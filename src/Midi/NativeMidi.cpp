#include "NativeMidi.h"
#include "../Audio/AudioEngine.h"
#include "../MPC/MpcCore.h"
#include "../MPC/MpcStudioMk2InputDecoder.h"
#include "../MPC/MpcStudioMk2SemanticAdapter.h"
#include "../MPC/Sequencer/MpcNoteRepeatScheduler.h"
#include "../MPC/Sequencer/MpcNoteRepeatTiming.h"
#include "../MPC/MpcStudioMk2LedProtocol.h"

#include <android/log.h>
#include <jni.h>

#include <array>
#include <atomic>
#include <cstddef>
#include <optional>
#include <vector>
#include <memory>

namespace {

constexpr const char* kTag = "MpcMk2Groovebox";

std::atomic<bool> sequenceLauncherEnabled{false};
std::atomic<std::size_t> sequenceLauncherBank{0};
std::atomic<int> pendingSequenceLauncherPad{-1};

constexpr std::size_t kSemanticActionQueueCapacity = 128;
std::array<mpc::studio::SemanticAction, kSemanticActionQueueCapacity>
        semanticActionQueue{};
std::atomic<std::size_t> semanticActionHead{0};
std::atomic<std::size_t> semanticActionTail{0};
mpc::studio::MpcStudioMk2SemanticAdapter semanticAdapter;
std::unique_ptr<mpc::sequencer::MpcNoteRepeatScheduler> noteRepeatScheduler;

std::vector<std::uint8_t> makeNoteRepeatRateFeedback(
        bool enabled,
        std::size_t selectedIndex) {
    std::vector<std::uint8_t> feedback;
    feedback.reserve(
            mpc::sequencer::note_repeat_timing::kRepeatRates.size() * 3u);
    for (std::size_t i = 0;
         i < mpc::sequencer::note_repeat_timing::kRepeatRates.size();
         ++i) {
        const auto brightness =
                enabled && i == selectedIndex ? std::uint8_t{127} : std::uint8_t{0};
        const auto message =
                mpc::studio::makeNoteRepeatLed(i, brightness);
        if (message.has_value()) {
            feedback.insert(feedback.end(), message->begin(), message->end());
        }
    }
    return feedback;
}

mpc::sequencer::MpcNoteRepeatScheduler& repeatScheduler() {
    if (!noteRepeatScheduler) {
        auto& core = mpc::MpcCore::instance();
        noteRepeatScheduler =
                std::make_unique<mpc::sequencer::MpcNoteRepeatScheduler>(
                        core.audio(),
                        core.sequenceTransportClock());
    }
    return *noteRepeatScheduler;
}

bool enqueueSemanticAction(
        const mpc::studio::SemanticAction& action) noexcept {
    const auto head =
            semanticActionHead.load(std::memory_order_relaxed);
    const auto next =
            (head + 1u) % kSemanticActionQueueCapacity;
    if (next == semanticActionTail.load(std::memory_order_acquire)) {
        __android_log_print(
                ANDROID_LOG_WARN,
                kTag,
                "MPC semantic action queue full");
        return false;
    }
    semanticActionQueue[head] = action;
    semanticActionHead.store(next, std::memory_order_release);
    return true;
}

std::optional<mpc::studio::SemanticAction> dequeueSemanticAction() noexcept {
    const auto tail =
            semanticActionTail.load(std::memory_order_relaxed);
    if (tail == semanticActionHead.load(std::memory_order_acquire)) {
        return std::nullopt;
    }
    const auto action = semanticActionQueue[tail];
    semanticActionTail.store(
            (tail + 1u) % kSemanticActionQueueCapacity,
            std::memory_order_release);
    return action;
}

} // namespace

namespace mpc::midi {

std::optional<std::vector<std::uint8_t>> handleIncoming(
        std::span<const std::uint8_t> message,
        std::int64_t timestamp) {
    if (message.empty()) {
        return std::nullopt;
    }

    const auto event = mpc::studio::decodeInput(message);

    if (!event) {
        __android_log_print(
            ANDROID_LOG_DEBUG,
            kTag,
            "MIDI %zu-byte message: unrecognized",
            message.size());
        return std::nullopt;
    }

    if (event->type == mpc::studio::InputEventType::PadNote) {
        auto& core = mpc::MpcCore::instance();
        const auto padRouting = semanticAdapter.handlePad(*event);
        if (padRouting.action.has_value()) {
            enqueueSemanticAction(*padRouting.action);
        }
        if (padRouting.consumed) {
            return std::nullopt;
        }

        if (sequenceLauncherEnabled.load(std::memory_order_acquire)) {
            if (event->pressed) {
                pendingSequenceLauncherPad.store(
                        static_cast<int>(event->padIndex),
                        std::memory_order_release);
            }

            if (event->pressed) {
                const auto led = mpc::studio::makePadLedSysEx(
                        event->padIndex,
                        mpc::studio::Rgb{24u, 72u, 24u});
                return std::vector<std::uint8_t>(led.begin(), led.end());
            }
            return std::nullopt;
        }

        if (padRouting.repeating) {
            auto& scheduler = repeatScheduler();
            if (event->pressed) {
                if (padRouting.targetPadIndex < mpc::domain::kMaxProgramPads) {
                    const auto& pad = core.projectState()
                            .activeDrumProgram()
                            .pad(padRouting.targetPadIndex);
                    if (!pad.muted) {
                        scheduler.setPad(
                                padRouting.targetPadIndex,
                                padRouting.velocity);
                    } else {
                        scheduler.clearPad();
                    }
                }
            } else if (!semanticAdapter.noteRepeatLatched()) {
                scheduler.clearPad();
            }

            const std::uint8_t level =
                    event->pressed ? padRouting.velocity : 0u;
            const auto led = mpc::studio::makePadLedSysEx(
                    event->padIndex,
                    mpc::studio::Rgb{level, level, level});
            return std::vector<std::uint8_t>(led.begin(), led.end());
        }

        if (event->pressed && padRouting.targetPadIndex != 0xFF) {
            const auto& pad =
                    core.projectState().activeDrumProgram()
                            .pad(padRouting.targetPadIndex);
            if (!pad.muted) {
                core.audio().triggerPad(
                        padRouting.targetPadIndex,
                        padRouting.velocity);
            }
        }

        const auto& state = core.projectState();
        const auto trackIndex = state.activeTrackIndex();
        const auto& tracks = state.activeSequence().tracks;

        if (trackIndex < tracks.size()
                && tracks[trackIndex].recordArmed
                && tracks[trackIndex].kind == mpc::domain::TrackKind::Drum
                && core.sequenceTransportClock().snapshot().playing) {
            const auto tick =
                    core.sequenceTransportClock().positionAtTimestamp(timestamp);
            const mpc::sequencer::SequenceRecordEvent captured{
                    tick,
                    static_cast<std::uint8_t>(trackIndex),
                    padRouting.targetPadIndex,
                    event->pressed
                            ? padRouting.velocity
                            : static_cast<std::uint8_t>(0),
                    event->pressed ? static_cast<std::uint8_t>(1) : static_cast<std::uint8_t>(0)};

            if (!core.sequenceRecordQueue().tryEnqueue(captured)) {
                __android_log_print(
                        ANDROID_LOG_WARN,
                        kTag,
                        "Sequence record queue full");
            }
        }

        const std::uint8_t level = event->pressed ? padRouting.velocity : 0u;

        const auto led = mpc::studio::makePadLedSysEx(
                event->padIndex,
                mpc::studio::Rgb{level, level, level});
        return std::vector<std::uint8_t>(led.begin(), led.end());
    }
    if (event->type == mpc::studio::InputEventType::Button
            || event->type == mpc::studio::InputEventType::JogPress
            || event->type == mpc::studio::InputEventType::JogWheel
            || event->type == mpc::studio::InputEventType::TouchStrip) {
        const auto action = semanticAdapter.handleControl(*event);
        if (action.has_value()) {
            if (action->type
                    == mpc::studio::SemanticActionType::NoteRepeatState) {
                auto& scheduler = repeatScheduler();
                scheduler.setEnabled(action->value0 != 0);
                const auto feedback = makeNoteRepeatRateFeedback(
                        action->value0 != 0,
                        scheduler.repeatGridIndex());
                enqueueSemanticAction(*action);
                return feedback.empty()
                        ? std::nullopt
                        : std::optional<std::vector<std::uint8_t>>(feedback);
            }
            if (action->type
                    == mpc::studio::SemanticActionType::NoteRepeatRateChanged) {
                auto& scheduler = repeatScheduler();
                scheduler.setRepeatGridIndex(action->value0);
                const auto feedback = makeNoteRepeatRateFeedback(
                        true,
                        scheduler.repeatGridIndex());
                enqueueSemanticAction(*action);
                return feedback.empty()
                        ? std::nullopt
                        : std::optional<std::vector<std::uint8_t>>(feedback);
            }
            enqueueSemanticAction(*action);
        }
        return std::nullopt;
    }

    return std::nullopt;
}

} // namespace mpc::midi

template <std::size_t Size>
jbyteArray toJavaByteArray(
        JNIEnv* env,
        const std::array<std::uint8_t, Size>& bytes) {
    auto result = env->NewByteArray(static_cast<jsize>(Size));
    if (result == nullptr) {
        return nullptr;
    }

    env->SetByteArrayRegion(
        result,
        0,
        static_cast<jsize>(Size),
        reinterpret_cast<const jbyte*>(bytes.data()));
    return result;
}

jbyteArray toJavaByteArray(
        JNIEnv* env,
        const std::vector<std::uint8_t>& bytes) {
    auto result = env->NewByteArray(static_cast<jsize>(bytes.size()));
    if (result == nullptr) {
        return nullptr;
    }

    env->SetByteArrayRegion(
        result,
        0,
        static_cast<jsize>(bytes.size()),
        reinterpret_cast<const jbyte*>(bytes.data()));
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_AndroidMidiBridge_nativeConsumeHardwareAction(
        JNIEnv* env, jclass /* clazz */) {
    if (env == nullptr) {
        return nullptr;
    }

    const auto action = dequeueSemanticAction();
    if (!action.has_value()) {
        return env->NewIntArray(0);
    }

    const std::array<jint, 4> values{{
        static_cast<jint>(action->type),
        static_cast<jint>(action->value0),
        static_cast<jint>(action->value1),
        static_cast<jint>(action->value2)
    }};
    auto result = env->NewIntArray(static_cast<jsize>(values.size()));
    if (result == nullptr) {
        return nullptr;
    }
    env->SetIntArrayRegion(
            result, 0,
            static_cast<jsize>(values.size()),
            values.data());
    return result;
}

extern "C" JNIEXPORT void JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetLauncherContext(
        JNIEnv* /* env */,
        jclass /* clazz */,
        jboolean enabled,
        jint bank)
{
    sequenceLauncherEnabled.store(
            enabled == JNI_TRUE,
            std::memory_order_release);
    sequenceLauncherBank.store(
            bank < 0 ? 0u : static_cast<std::size_t>(bank),
            std::memory_order_release);
    if (enabled != JNI_TRUE) {
        pendingSequenceLauncherPad.store(
                -1,
                std::memory_order_release);
        if (noteRepeatScheduler) {
            noteRepeatScheduler->setEnabled(false);
        }
    }
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_AndroidMidiBridge_nativeConsumeSequenceLauncherPad(
        JNIEnv* /* env */,
        jclass /* clazz */)
{
    return static_cast<jint>(
            pendingSequenceLauncherPad.exchange(
                    -1,
                    std::memory_order_acq_rel));
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_AndroidMidiBridge_nativeOnMidi(
        JNIEnv* env, jclass, jbyteArray data, jlong timestamp) {
    if (env == nullptr || data == nullptr) {
        return nullptr;
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return nullptr;
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return nullptr;
    }

    auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
    const auto feedback = mpc::midi::handleIncoming(
        std::span<const std::uint8_t>(raw, static_cast<std::size_t>(length)),
        static_cast<std::int64_t>(timestamp));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    if (!feedback.has_value()) {
        return nullptr;
    }

    return toJavaByteArray(env, *feedback);
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MpcStudioMk2MidiMessages_nativePadRgb(
        JNIEnv* env, jclass, jint pad, jint red, jint green, jint blue) {
    if (env == nullptr || pad < 0 || red < 0 || green < 0 || blue < 0) {
        return nullptr;
    }

    return toJavaByteArray(
        env,
        mpc::studio::makePadLedSysEx(
            static_cast<std::uint8_t>(pad),
            mpc::studio::Rgb{
                static_cast<std::uint8_t>(red),
                static_cast<std::uint8_t>(green),
                static_cast<std::uint8_t>(blue)
            }));
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MpcStudioMk2MidiMessages_nativeButtonLed(
        JNIEnv* env, jclass, jint cc, jint value) {
    if (env == nullptr || cc < 0 || value < 0) {
        return nullptr;
    }

    return toJavaByteArray(
        env,
        mpc::studio::makeCcMessage(
            static_cast<std::uint8_t>(cc),
            static_cast<std::uint8_t>(value)));
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MpcStudioMk2MidiMessages_nativeTouchStripLedSegment(
        JNIEnv* env, jclass, jint segment, jint brightness) {
    if (env == nullptr || segment < 0 || brightness < 0) {
        return nullptr;
    }

    const auto message = mpc::studio::makeTouchStripLedSegment(
        static_cast<std::size_t>(segment),
        static_cast<std::uint8_t>(brightness));
    return message ? toJavaByteArray(env, *message) : nullptr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MpcStudioMk2MidiMessages_nativeNoteRepeatLed(
        JNIEnv* env, jclass, jint index, jint brightness) {
    if (env == nullptr || index < 0 || brightness < 0) {
        return nullptr;
    }

    const auto message = mpc::studio::makeNoteRepeatLed(
        static_cast<std::size_t>(index),
        static_cast<std::uint8_t>(brightness));
    return message ? toJavaByteArray(env, *message) : nullptr;
}

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MpcStudioMk2MidiMessages_nativeLcdChunk(
        JNIEnv* env, jclass, jint x, jint y, jbyteArray pngBytes) {
    if (env == nullptr || pngBytes == nullptr || x < 0 || x > 255 || y < 0 || y > 255) {
        return nullptr;
    }

    const jsize length = env->GetArrayLength(pngBytes);
    if (length < 0) {
        return nullptr;
    }

    jbyte* bytes = env->GetByteArrayElements(pngBytes, nullptr);
    if (bytes == nullptr && length > 0) {
        return nullptr;
    }

    std::vector<std::uint8_t> message;
    if (length == 0) {
        message = mpc::studio::makeLcdChunkSysEx(
            mpc::studio::LcdChunk{
                static_cast<std::uint8_t>(x),
                static_cast<std::uint8_t>(y),
                0,
                0
            },
            {});
    } else {
        const auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
        message = mpc::studio::makeLcdChunkSysEx(
            mpc::studio::LcdChunk{
                static_cast<std::uint8_t>(x),
                static_cast<std::uint8_t>(y),
                0,
                0
            },
            std::span<const std::uint8_t>(raw, static_cast<std::size_t>(length)));
    }

    if (bytes != nullptr) {
        env->ReleaseByteArrayElements(pngBytes, bytes, JNI_ABORT);
    }

    auto result = env->NewByteArray(static_cast<jsize>(message.size()));
    if (result == nullptr) {
        return nullptr;
    }

    env->SetByteArrayRegion(
        result,
        0,
        static_cast<jsize>(message.size()),
        reinterpret_cast<const jbyte*>(message.data()));
    return result;
}
