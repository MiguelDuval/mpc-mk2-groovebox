#include "NativeMidi.h"
#include "../Audio/AudioEngine.h"
#include "../MPC/MpcStudioMk2InputDecoder.h"
#include "../MPC/MpcStudioMk2LedProtocol.h"

#include <android/log.h>
#include <jni.h>

#include <array>
#include <cstddef>
#include <optional>
#include <vector>

namespace {

constexpr const char* kTag = "MpcMk2Groovebox";

const char* eventTypeName(mpc::studio::InputEventType type) {
    using Type = mpc::studio::InputEventType;

    switch (type) {
        case Type::PadNote: return "PAD";
        case Type::PadAftertouch: return "PAD_AFTERTOUCH";
        case Type::Button: return "BUTTON";
        case Type::JogWheel: return "JOG";
        case Type::JogPress: return "JOG_PRESS";
        case Type::TouchStrip: return "TOUCH_STRIP";
        case Type::ChannelAftertouch: return "CHANNEL_AFTERTOUCH";
    }

    return "UNKNOWN";
}

} // namespace

namespace mpc::midi {

std::optional<std::array<std::uint8_t, 12>> handleIncoming(
        std::span<const std::uint8_t> message,
        std::int64_t /*timestamp*/) {
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
        if (event->pressed) {
            mpc::audio::AudioEngine::instance().triggerPad(
                event->padIndex,
                event->value);
        }

        const std::uint8_t level = event->pressed
                ? static_cast<std::uint8_t>(
                        std::min<std::uint8_t>(event->value, 127u))
                : 0u;

        return mpc::studio::makePadLedSysEx(
                event->padIndex,
                mpc::studio::Rgb{level, level, level});
    }

    if (event->type == mpc::studio::InputEventType::Button
            || event->type == mpc::studio::InputEventType::JogPress) {
        __android_log_print(
            ANDROID_LOG_DEBUG,
            kTag,
            "MIDI %s number=%u value=%u pressed=%s channel=%u pad=%u",
            eventTypeName(event->type),
            static_cast<unsigned>(event->number),
            static_cast<unsigned>(event->value),
            event->pressed ? "true" : "false",
            static_cast<unsigned>(event->channel),
            static_cast<unsigned>(event->padIndex));
        return std::nullopt;
    }

    __android_log_print(
        ANDROID_LOG_DEBUG,
        kTag,
        "MIDI %s number=%u value=%u channel=%u pad=%u",
        eventTypeName(event->type),
        static_cast<unsigned>(event->number),
        static_cast<unsigned>(event->value),
        static_cast<unsigned>(event->channel),
        static_cast<unsigned>(event->padIndex));

    return std::nullopt;
}

} // namespace mpc::midi

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
