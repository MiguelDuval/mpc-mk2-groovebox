#include <jni.h>
#include "Audio/AudioEngine.h"

#include <array>
#include <algorithm>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <span>
#include <limits>
#include <memory>
#include <string>
#include <vector>

#include "MPC/MpcCore.h"
#include "MPC/Sequencer/MpcPatternOps.h"
#include "MPC/Sequencer/MpcSequenceLauncher.h"
#include "MPC/Sequencer/MpcSequencePlaybackSession.h"
#include "MPC/Sequencer/MpcSequenceSettings.h"
#include "MPC/Sequencer/MpcLocatePolicy.h"
#include "MPC/Sequencer/MpcErasePolicy.h"

namespace {

std::unique_ptr<mpc::sequencer::MpcSequencePlaybackSession> sequencePlayback;

mpc::sequencer::MpcSequencePlaybackSession& sequenceSession() {
    if (!sequencePlayback) {
        auto& core = mpc::MpcCore::instance();
        sequencePlayback =
                std::make_unique<mpc::sequencer::MpcSequencePlaybackSession>(
                        core.projectState(),
                        core.audio());
    }
    return *sequencePlayback;
}

std::int64_t monotonicNanos() noexcept {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
}

jstring toJString(JNIEnv* env, const std::string& text) {
    return env->NewStringUTF(text.c_str());
}

void stopSequenceForMutation() {
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();
    const auto position = sequenceSession().positionTicks();

    if (core.sequenceRecorder().active()) {
        core.sequenceRecorder().finish(
                state,
                core.sequenceRecordQueue(),
                position);
    }

    sequenceSession().stop();
    core.sequenceTransportClock().clearQueuedSequence();
    core.sequenceTransportClock().stop(
            state.activeSequence(),
            position,
            monotonicNanos());
}

void syncSequenceTransportStopped() {
    auto& core = mpc::MpcCore::instance();
    core.sequenceTransportClock().stop(
            core.projectState().activeSequence(),
            sequenceSession().positionTicks(),
            monotonicNanos());
}

jfloatArray toJFloatArray(
        JNIEnv* env,
        const std::vector<mpc::audio::WaveformPeak>& peaks) {
    if (env == nullptr) {
        return nullptr;
    }

    const std::size_t valueCount = peaks.size() * 2u;
    if (valueCount > static_cast<std::size_t>(std::numeric_limits<jsize>::max())) {
        return nullptr;
    }

    const auto length = static_cast<jsize>(valueCount);
    jfloatArray result = env->NewFloatArray(length);
    if (result == nullptr) {
        return nullptr;
    }

    std::vector<jfloat> values(valueCount);
    for (std::size_t i = 0; i < peaks.size(); ++i) {
        values[i * 2u] = peaks[i].minimum;
        values[i * 2u + 1u] = peaks[i].maximum;
    }

    env->SetFloatArrayRegion(result, 0, length, values.data());
    return result;
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeEngineInfo(
        JNIEnv* env, jobject /* thiz */)
{
    constexpr auto* message =
        "Native foundation loaded: C++20 + Oboe. "
        "MPC Studio MkII hardware layer is ready.";

    return toJString(env, message);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioLoadSample(
        JNIEnv* env, jobject /* thiz */, jbyteArray data)
{
    if (env == nullptr || data == nullptr) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return toJString(env, "Sample load failed: JNI access error");
    }

    const auto* raw =
        reinterpret_cast<const std::uint8_t*>(bytes);

    const auto result = mpc::audio::AudioEngine::instance().loadSample(
        std::span<const std::uint8_t>(
            raw,
            static_cast<std::size_t>(length)));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioLoadSampleForPad(
        JNIEnv* env, jobject /* thiz */, jbyteArray data, jint pad)
{
    if (env == nullptr || data == nullptr) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    if (pad < 0 || pad >= 16) {
        return toJString(env, "Sample load failed: invalid pad");
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return toJString(env, "Sample load failed: JNI access error");
    }

    const auto* raw =
            reinterpret_cast<const std::uint8_t*>(bytes);

    const auto result =
            mpc::audio::AudioEngine::instance().loadSampleForPad(
                    std::span<const std::uint8_t>(
                            raw,
                            static_cast<std::size_t>(length)),
                    static_cast<std::uint8_t>(pad));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioLoadSampleForPadLayer(
        JNIEnv* env, jobject /* thiz */, jbyteArray data, jint pad, jint layer)
{
    if (env == nullptr || data == nullptr) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Sample load failed: invalid pad or layer");
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return toJString(env, "Sample load failed: empty byte array");
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return toJString(env, "Sample load failed: JNI access error");
    }

    const auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
    const auto result =
            mpc::audio::AudioEngine::instance().loadSampleForPadLayer(
                    std::span<const std::uint8_t>(
                            raw,
                            static_cast<std::size_t>(length)),
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadSampleRegion(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer,
        jlong startFrame, jlong endFrame)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8
            || startFrame < 0 || endFrame < 0) {
        return toJString(env, "Sample region change failed: invalid pad, layer or frame");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    static_cast<std::size_t>(startFrame),
                    static_cast<std::size_t>(endFrame)));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleRegionStart(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).startFrame);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleRegionEnd(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).endFrame);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleFrameCount(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jlong>(
            mpc::audio::AudioEngine::instance().padSampleFrameCount(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleName(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "");
    }

    const auto& state = mpc::MpcCore::instance().projectState();
    const auto& sampleLayer =
            state.activeDrumProgram()
                    .pad(static_cast<std::size_t>(pad))
                    .layer(static_cast<std::size_t>(layer));
    const auto* sample = state.findSample(sampleLayer.sample);
    if (sample == nullptr || sample->name.empty()) {
        return toJString(env, "");
    }

    return toJString(env, sample->name);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadSampleName(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jstring name)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(
                env,
                "Sample name update failed: invalid pad or layer");
    }

    if (name == nullptr) {
        return toJString(
                env,
                "Sample name update failed: empty name");
    }

    const char* utfName = env->GetStringUTFChars(name, nullptr);
    if (utfName == nullptr) {
        return toJString(
                env,
                "Sample name update failed: JNI access error");
    }

    std::string sampleName(utfName);
    env->ReleaseStringUTFChars(name, utfName);

    auto& state = mpc::MpcCore::instance().projectState();
    const auto& layerState =
            state.activeDrumProgram()
                    .pad(static_cast<std::size_t>(pad))
                    .layer(static_cast<std::size_t>(layer));
    if (!layerState.sample.isAssigned()) {
        return toJString(
                env,
                "Sample name update failed: no sample assigned");
    }

    mpc::domain::SampleRef* sample = nullptr;
    for (auto& candidate : state.project().samples) {
        if (candidate.assetId.value == layerState.sample.value) {
            sample = &candidate;
            break;
        }
    }
    if (sample == nullptr) {
        return toJString(
                env,
                "Sample name update failed: sample metadata missing");
    }

    if (sampleName.empty()) {
        sampleName = "Imported sample";
    }

    sample->name = std::move(sampleName);
    return toJString(env, "Sample name updated");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioChopPadSampleToPads(
        JNIEnv* env, jobject /* thiz */, jint sourcePad, jint sourceLayer, jint chopCount)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (sourcePad < 0 || sourcePad >= 16
            || sourceLayer < 0 || sourceLayer >= 8
            || chopCount < 0 || chopCount > 255) {
        return toJString(env, "Chop failed: invalid source or chop count");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().chopPadSampleToPads(
                    static_cast<std::uint8_t>(sourcePad),
                    static_cast<std::uint8_t>(sourceLayer),
                    static_cast<std::uint8_t>(chopCount)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioCropPadSampleRegion(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Crop failed: invalid pad or layer");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().cropPadSampleRegion(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadLayerGain(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jfloat gain)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Layer gain change failed: invalid pad or layer");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadLayerGain(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    gain));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLayerGain(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 1.0f;
    }

    return mpc::audio::AudioEngine::instance().padLayerGain(
            static_cast<std::uint8_t>(pad),
            static_cast<std::uint8_t>(layer));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadLayerTuning(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jfloat semitones)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Layer tuning change failed: invalid pad or layer");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadLayerTuningSemitones(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    semitones));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLayerTuning(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0.0f;
    }

    return mpc::audio::AudioEngine::instance().padLayerTuningSemitones(
            static_cast<std::uint8_t>(pad),
            static_cast<std::uint8_t>(layer));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadLayerPan(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jfloat pan)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Layer pan change failed: invalid pad or layer");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadLayerPan(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    pan));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLayerPan(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0.0f;
    }

    return mpc::audio::AudioEngine::instance().padLayerPan(
            static_cast<std::uint8_t>(pad),
            static_cast<std::uint8_t>(layer));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadLayerVelocityRange(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jint minimum, jint maximum)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8
            || minimum < 0 || minimum > 127
            || maximum < 0 || maximum > 127) {
        return toJString(env, "Layer velocity range failed: invalid value");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadLayerVelocityRange(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer),
                    static_cast<std::uint8_t>(minimum),
                    static_cast<std::uint8_t>(maximum)));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLayerVelocityMin(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jint>(
            mpc::audio::AudioEngine::instance().padLayerVelocityRange(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).minimum);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLayerVelocityMax(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 127;
    }

    return static_cast<jint>(
            mpc::audio::AudioEngine::instance().padLayerVelocityRange(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)).maximum);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadTuning(
        JNIEnv* env, jobject /* thiz */, jint pad, jfloat semitones)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16) {
        return toJString(env, "Tuning change failed: invalid pad");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadTuningSemitones(
                    static_cast<std::uint8_t>(pad),
                    semitones));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadTuning(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) {
        return 0.0f;
    }

    return mpc::audio::AudioEngine::instance().padTuningSemitones(
            static_cast<std::uint8_t>(pad));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadLevel(
        JNIEnv* env, jobject /* thiz */, jint pad, jfloat level)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16) {
        return toJString(env, "Level change failed: invalid pad");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadLevel(
                    static_cast<std::uint8_t>(pad),
                    level));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadLevel(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) {
        return 1.0f;
    }

    return mpc::audio::AudioEngine::instance().padLevel(
            static_cast<std::uint8_t>(pad));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadPan(
        JNIEnv* env, jobject /* thiz */, jint pad, jfloat pan)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16) {
        return toJString(env, "Pan change failed: invalid pad");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadPan(
                    static_cast<std::uint8_t>(pad),
                    pan));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadPan(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) {
        return 0.0f;
    }

    return mpc::audio::AudioEngine::instance().padPan(
            static_cast<std::uint8_t>(pad));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadEnvelope(
        JNIEnv* env,
        jobject /* thiz */,
        jint pad,
        jfloat attackMs,
        jfloat decayMs,
        jfloat sustain,
        jfloat releaseMs)
{
    if (env == nullptr) {
        return nullptr;
    }
    if (pad < 0 || pad >= 16) {
        return toJString(env, "Envelope change failed: invalid pad");
    }
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadEnvelopeParameters(
                    static_cast<std::uint8_t>(pad),
                    attackMs,
                    decayMs,
                    sustain,
                    releaseMs));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadEnvelopeAttack(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) return 0.0f;
    return mpc::audio::AudioEngine::instance().padEnvelopeParameters(
            static_cast<std::uint8_t>(pad)).attackMs;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadEnvelopeDecay(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) return 0.0f;
    return mpc::audio::AudioEngine::instance().padEnvelopeParameters(
            static_cast<std::uint8_t>(pad)).decayMs;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadEnvelopeSustain(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) return 1.0f;
    return mpc::audio::AudioEngine::instance().padEnvelopeParameters(
            static_cast<std::uint8_t>(pad)).sustain;
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadEnvelopeRelease(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) return 0.0f;
    return mpc::audio::AudioEngine::instance().padEnvelopeParameters(
            static_cast<std::uint8_t>(pad)).releaseMs;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetPadFilterCutoff(
        JNIEnv* env, jobject /* thiz */, jint pad, jfloat cutoffHz)
{
    if (env == nullptr) {
        return nullptr;
    }
    if (pad < 0 || pad >= 16) {
        return toJString(env, "Filter cutoff change failed: invalid pad");
    }
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setPadFilterCutoff(
                    static_cast<std::uint8_t>(pad), cutoffHz));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadFilterCutoff(
        JNIEnv* /* env */, jobject /* thiz */, jint pad)
{
    if (pad < 0 || pad >= 16) return 20000.0f;
    return mpc::audio::AudioEngine::instance().padFilterCutoff(
            static_cast<std::uint8_t>(pad));
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadWaveformPeaks(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer, jint points)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8
            || points <= 0 || points > 2048) {
        return env == nullptr ? nullptr : env->NewFloatArray(0);
    }

    const auto peaks = mpc::audio::AudioEngine::instance().padWaveformPeaks(
            static_cast<std::uint8_t>(pad),
            static_cast<std::uint8_t>(layer),
            static_cast<std::size_t>(points));
    return toJFloatArray(env, peaks);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetPadSampleRate(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint layer)
{
    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return 0;
    }

    return static_cast<jint>(
            mpc::audio::AudioEngine::instance().padSampleRate(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)));
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingWaveformPeaks(
        JNIEnv* env, jobject /* thiz */, jint points)
{
    if (points <= 0 || points > 2048) {
        return env == nullptr ? nullptr : env->NewFloatArray(0);
    }

    const auto peaks =
            mpc::audio::AudioEngine::instance().recordingWaveformPeaks(
                    static_cast<std::size_t>(points));
    return toJFloatArray(env, peaks);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingFrameCount(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::audio::AudioEngine::instance().recordingFrameCount());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingSampleRate(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::audio::AudioEngine::instance().recordingSampleRate());
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingPeak(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return mpc::audio::AudioEngine::instance().recordingPeak();
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingFrameCapacity(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return 960000;
}

extern "C" JNIEXPORT void JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioTriggerPad(
        JNIEnv* /* env */, jobject /* thiz */, jint pad, jint velocity)
{
    if (pad < 0 || pad >= 16 || velocity <= 0 || velocity > 127) {
        return;
    }

    auto& core = mpc::MpcCore::instance();
    const auto padIndex = static_cast<std::uint8_t>(pad);
    const auto velocityValue = static_cast<std::uint8_t>(velocity);

    core.audio().triggerPad(padIndex, velocityValue);

    const auto state = core.sequenceTransportClock().snapshot();
    const auto& projectState = core.projectState();
    const auto trackIndex = projectState.activeTrackIndex();
    const auto& tracks = projectState.activeSequence().tracks;

    if (state.playing
            && trackIndex < tracks.size()
            && tracks[trackIndex].recordArmed
            && tracks[trackIndex].kind == mpc::domain::TrackKind::Drum) {
        const auto tick = core.sequenceTransportClock().positionAtTimestamp(
                monotonicNanos());
        core.sequenceRecordQueue().tryEnqueue(
                mpc::sequencer::SequenceRecordEvent{
                        tick,
                        static_cast<std::uint8_t>(trackIndex),
                        padIndex,
                        velocityValue,
                        1u});
        core.sequenceRecordQueue().tryEnqueue(
                mpc::sequencer::SequenceRecordEvent{
                        tick,
                        static_cast<std::uint8_t>(trackIndex),
                        padIndex,
                        0u,
                        0u});
    }
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioConfigureOutput(
        JNIEnv* env,
        jobject /* thiz */,
        jint deviceId,
        jint sampleRate,
        jint bufferSizeFrames,
        jboolean exclusive,
        jboolean lowLatency)
{
    if (env == nullptr) {
        return nullptr;
    }

    mpc::audio::AudioEngine::OutputConfiguration configuration;
    configuration.deviceId = static_cast<int>(deviceId);
    configuration.sampleRate = static_cast<int>(sampleRate);
    configuration.bufferSizeFrames = static_cast<int>(bufferSizeFrames);
    configuration.exclusive = exclusive == JNI_TRUE;
    configuration.lowLatency = lowLatency == JNI_TRUE;

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().configureOutput(configuration));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioConfigureInputDevice(
        JNIEnv* env,
        jobject /* thiz */,
        jint deviceId)
{
    if (env == nullptr) {
        return nullptr;
    }

    mpc::audio::AudioEngine::InputConfiguration configuration;
    configuration.deviceId = static_cast<int>(deviceId);

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().configureInputDevice(configuration));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioTestOutput(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().testOutputTone());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStart(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().start());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStop(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().stop());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioSetRecordingThreshold(
        JNIEnv* env, jobject /* thiz */, jfloat threshold)
{
    if (env == nullptr) {
        return nullptr;
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().setRecordingThreshold(
                    threshold));
}

extern "C" JNIEXPORT jfloat JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioGetRecordingThreshold(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return mpc::audio::AudioEngine::instance().recordingThreshold();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStartRecording(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().startRecording());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStopRecording(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().stopRecording());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStartMonitor(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().startMonitor());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStopMonitor(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().stopMonitor());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioAssignRecordingToPadLayer(
        JNIEnv* env, jobject /* thiz */, jint pad, jint layer)
{
    if (env == nullptr) {
        return nullptr;
    }

    if (pad < 0 || pad >= 16 || layer < 0 || layer >= 8) {
        return toJString(env, "Recording assign failed: invalid pad or layer");
    }

    return toJString(
            env,
            mpc::audio::AudioEngine::instance().assignRecordingToPadLayer(
                    static_cast<std::uint8_t>(pad),
                    static_cast<std::uint8_t>(layer)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioRecordingStatus(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(
            env,
            mpc::audio::AudioEngine::instance().recordingStatus());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStatus(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().status());
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceStatus(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(
            env,
            mpc::MpcCore::instance().projectState().sequenceStatus());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetIndex(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequenceIndex());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetCount(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto count = mpc::MpcCore::instance().projectState().sequenceCount();
    return static_cast<jint>(
            std::min<std::size_t>(count, std::numeric_limits<jint>::max()));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetQueuedIndex(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().sequenceTransportClock()
                    .queuedSequenceIndex());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsGridEditable(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return JNI_FALSE;
    }

    const auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    const auto& tracks = state.activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        return JNI_FALSE;
    }

    const auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum
            || track.patterns.empty()
            || track.patterns.front().lengthTicks <= 0) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetGridVelocities(
        JNIEnv* env, jobject /* thiz */, jint firstStep, jint gridTicks)
{
    if (env == nullptr) {
        return nullptr;
    }

    constexpr std::size_t kGridCells = 16u * 16u;
    jintArray result = env->NewIntArray(static_cast<jsize>(kGridCells));
    if (result == nullptr) {
        return nullptr;
    }

    std::vector<jint> values(kGridCells, 0);

    if (firstStep < 0 || gridTicks <= 0) {
        env->SetIntArrayRegion(
                result, 0, static_cast<jsize>(values.size()), values.data());
        return result;
    }

    const auto& state = mpc::MpcCore::instance().projectState();
    const auto trackIndex = state.activeTrackIndex();
    const auto& tracks = state.activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        env->SetIntArrayRegion(
                result, 0, static_cast<jsize>(values.size()), values.data());
        return result;
    }

    const auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum
            || track.patterns.empty()) {
        env->SetIntArrayRegion(
                result, 0, static_cast<jsize>(values.size()), values.data());
        return result;
    }

    const auto& pattern = track.patterns.front();
    const auto& program = state.activeDrumProgram();

    std::array<int, 128> noteToPad{};
    noteToPad.fill(-1);
    for (std::size_t pad = 0; pad < mpc::domain::kMaxProgramPads; ++pad) {
        noteToPad[program.pads[pad].midiNote] = static_cast<int>(pad);
    }

    for (const auto& note : pattern.notes) {
        if (note.note >= noteToPad.size() || note.velocity == 0) {
            continue;
        }

        const auto pad = noteToPad[note.note];
        if (pad < 0) {
            continue;
        }

        if (note.tick < 0 || gridTicks <= 0
                || note.tick % gridTicks != 0) {
            continue;
        }

        const auto absoluteStep =
                static_cast<std::int64_t>(note.tick)
                / static_cast<std::int64_t>(gridTicks);
        const auto column = absoluteStep
                - static_cast<std::int64_t>(firstStep);
        if (column < 0 || column >= 16) {
            continue;
        }

        auto& velocity = values[
                static_cast<std::size_t>(pad) * 16u
                + static_cast<std::size_t>(column)];
        velocity = std::max(
                velocity,
                static_cast<jint>(note.velocity));
    }

    env->SetIntArrayRegion(
            result, 0, static_cast<jsize>(values.size()), values.data());
    return result;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetStepParameters(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks)
{
    if (env == nullptr) {
        return nullptr;
    }

    std::array<jint, 5> values{0, 0, 0, 0, 0};
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0) {
        auto* result = env->NewIntArray(static_cast<jsize>(values.size()));
        if (result != nullptr) {
            env->SetIntArrayRegion(
                    result, 0, static_cast<jsize>(values.size()), values.data());
        }
        return result;
    }

    const auto& state = mpc::MpcCore::instance().projectState();
    const auto trackIndex = state.activeTrackIndex();
    const auto& tracks = state.activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        auto* result = env->NewIntArray(static_cast<jsize>(values.size()));
        if (result != nullptr) {
            env->SetIntArrayRegion(
                    result, 0, static_cast<jsize>(values.size()), values.data());
        }
        return result;
    }

    const auto& track = tracks[trackIndex];
    if (track.kind == mpc::domain::TrackKind::Drum
            && !track.patterns.empty()) {
        const auto noteNumber = state.activeDrumProgram()
                .pads[static_cast<std::size_t>(padIndex)].midiNote;
        const auto* note = mpc::sequencer::findStepNote(
                track.patterns.front(),
                stepIndex,
                gridTicks,
                noteNumber);
        if (note != nullptr) {
            values[0] = static_cast<jint>(note->velocity);
            values[1] = static_cast<jint>(note->probability);
            values[2] = static_cast<jint>(note->ratchet);
            values[3] = static_cast<jint>(note->nudgeTicks);
            values[4] = static_cast<jint>(
                    note->durationTicks > 0
                            ? note->durationTicks
                            : gridTicks);
        }
    }

    auto* result = env->NewIntArray(static_cast<jsize>(values.size()));
    if (result != nullptr) {
        env->SetIntArrayRegion(
                result, 0, static_cast<jsize>(values.size()), values.data());
    }
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepVelocity(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks,
        jint velocity)
{
    if (velocity < 1 || velocity > 127) {
        return toJString(env, "Step velocity failed: use 1–127");
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Step edit blocked: stop playback first");
    }
    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0
            || trackIndex >= tracks.size()) {
        return toJString(env, "Step velocity failed: invalid target");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Step velocity failed: selected track is not DRUM");
    }

    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;
    if (!mpc::sequencer::setStepNoteVelocity(
            track.patterns.front(),
            stepIndex,
            gridTicks,
            noteNumber,
            static_cast<std::uint8_t>(velocity))) {
        return toJString(env, "Step velocity failed: no event");
    }

    return toJString(env, "Step velocity " + std::to_string(velocity));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepProbability(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks,
        jint probability)
{
    if (probability < 0 || probability > 127) {
        return toJString(env, "Step probability failed: use 0–127");
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Step edit blocked: stop playback first");
    }
    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0
            || trackIndex >= tracks.size()) {
        return toJString(env, "Step probability failed: invalid target");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Step probability failed: selected track is not DRUM");
    }

    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;
    if (!mpc::sequencer::setStepNoteProbability(
            track.patterns.front(),
            stepIndex,
            gridTicks,
            noteNumber,
            static_cast<std::uint8_t>(probability))) {
        return toJString(env, "Step probability failed: no event");
    }

    return toJString(
            env,
            "Step probability " + std::to_string(probability));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepRatchet(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks,
        jint ratchet)
{
    if (ratchet < 1 || ratchet > 8) {
        return toJString(env, "Step ratchet failed: use 1–8");
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Step edit blocked: stop playback first");
    }
    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0
            || trackIndex >= tracks.size()) {
        return toJString(env, "Step ratchet failed: invalid target");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Step ratchet failed: selected track is not DRUM");
    }

    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;
    if (!mpc::sequencer::setStepNoteRatchet(
            track.patterns.front(),
            stepIndex,
            gridTicks,
            noteNumber,
            static_cast<std::uint8_t>(ratchet))) {
        return toJString(env, "Step ratchet failed: no event");
    }

    return toJString(env, "Step ratchet " + std::to_string(ratchet) + "x");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepDuration(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks,
        jint durationTicks)
{
    if (gridTicks <= 0
            || durationTicks < std::max(1, gridTicks / 4)
            || durationTicks > gridTicks * 4) {
        return toJString(
                env,
                "Step duration failed: use "
                        + std::to_string(std::max(1, gridTicks / 4))
                        + "–"
                        + std::to_string(std::max(1, gridTicks) * 4)
                        + " ticks");
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Step edit blocked: stop playback first");
    }
    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0
            || trackIndex >= tracks.size()) {
        return toJString(env, "Step duration failed: invalid target");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Step duration failed: selected track is not DRUM");
    }

    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;
    if (!mpc::sequencer::setStepNoteDuration(
            track.patterns.front(),
            stepIndex,
            gridTicks,
            noteNumber,
            static_cast<std::int32_t>(durationTicks))) {
        return toJString(env, "Step duration failed: no event");
    }

    return toJString(
            env,
            "Step duration " + std::to_string(durationTicks) + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetStepNudge(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks,
        jint nudgeTicks)
{
    if (nudgeTicks < -mpc::sequencer::kMaxStepNudgeTicks
            || nudgeTicks > mpc::sequencer::kMaxStepNudgeTicks) {
        return toJString(
                env,
                "Step nudge failed: use "
                        + std::to_string(-mpc::sequencer::kMaxStepNudgeTicks)
                        + "–"
                        + std::to_string(mpc::sequencer::kMaxStepNudgeTicks));
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Step edit blocked: stop playback first");
    }
    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0
            || trackIndex >= tracks.size()) {
        return toJString(env, "Step nudge failed: invalid target");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Step nudge failed: selected track is not DRUM");
    }

    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;
    if (!mpc::sequencer::setStepNoteNudge(
            track.patterns.front(),
            stepIndex,
            gridTicks,
            noteNumber,
            static_cast<std::int32_t>(nudgeTicks))) {
        return toJString(env, "Step nudge failed: no event");
    }

    const std::string sign = nudgeTicks > 0 ? "+" : "";
    return toJString(
            env,
            std::string("Step nudge ")
                    + sign
                    + std::to_string(nudgeTicks)
                    + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceToggleGridStep(
        JNIEnv* env,
        jobject /* thiz */,
        jint padIndex,
        jint stepIndex,
        jint gridTicks)
{
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)
            || stepIndex < 0
            || gridTicks <= 0) {
        return toJString(env, "Grid edit failed: invalid step");
    }

    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(env, "Grid edit blocked: stop playback first");
    }

    stopSequenceForMutation();

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        return toJString(env, "Grid edit failed: no selected track");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum
            || track.patterns.empty()) {
        return toJString(env, "Grid edit failed: selected track is not DRUM");
    }

    auto& pattern = track.patterns.front();
    const auto noteNumber = state.activeDrumProgram()
            .pads[static_cast<std::size_t>(padIndex)].midiNote;

    const auto tick = static_cast<std::int64_t>(stepIndex)
            * static_cast<std::int64_t>(gridTicks);
    if (tick < 0 || tick >= pattern.lengthTicks) {
        return toJString(env, "Grid edit failed: step is outside pattern");
    }

    bool occupied = false;
    for (const auto& note : pattern.notes) {
        if (note.tick == tick && note.note == noteNumber) {
            occupied = true;
            break;
        }
    }

    const bool changed = occupied
            ? mpc::sequencer::eraseStepNote(
                    pattern, stepIndex, gridTicks, noteNumber)
            : mpc::sequencer::setStepNote(
                    pattern,
                    stepIndex,
                    gridTicks,
                    noteNumber,
                    100,
                    gridTicks,
                    127,
                    1);

    if (!changed) {
        return toJString(env, "Grid edit failed: event unchanged");
    }

    return toJString(
            env,
            std::string("STEP ")
                    + std::to_string(stepIndex + 1)
                    + (occupied ? " OFF" : " ON")
                    + " | PAD "
                    + std::to_string(padIndex + 1));
}

namespace {

jstring selectSequenceForUi(
        JNIEnv* env,
        std::size_t sequenceIndex) {
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();

    if (sequenceIndex >= state.sequenceCount()) {
        return toJString(env, "Sequence unavailable");
    }

    if (sequenceSession().isPlaying()) {
        if (sequenceIndex == state.activeSequenceIndex()) {
            core.sequenceTransportClock().clearQueuedSequence();
            return toJString(
                    env,
                    "Sequence queue cleared | " + state.sequenceStatus());
        }
        core.sequenceTransportClock().queueSequence(sequenceIndex);
        return toJString(
                env,
                "Sequence queued: " + std::to_string(sequenceIndex + 1)
                        + " | " + state.sequenceStatus());
    }

    stopSequenceForMutation();

    if (!state.selectSequence(sequenceIndex)) {
        return toJString(env, "Sequence selection failed");
    }

    core.sequenceTransportClock().clearQueuedSequence();
    core.sequenceTransportClock().update(
            state.activeSequence(),
            sequenceSession().positionTicks(),
            monotonicNanos(),
            false);

    return toJString(env, state.sequenceStatus());
}

} // namespace

namespace {

jstring selectSequenceFromPadForUi(
        JNIEnv* env,
        jint bank,
        jint padIndex) {
    if (bank < 0 || padIndex < 0 || padIndex >= 16) {
        return toJString(env, "Sequence launch failed: invalid pad");
    }

    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();
    const auto sequenceIndex =
            static_cast<std::size_t>(bank) * 16u
            + static_cast<std::size_t>(padIndex);
    if (sequenceIndex >= state.sequenceCount()) {
        return toJString(env, "Sequence launch failed: empty pad");
    }

    if (sequenceSession().isPlaying()) {
        const auto action =
                mpc::sequencer::handleSequencePadPress(
                        state,
                        core.sequenceTransportClock(),
                        static_cast<std::size_t>(bank),
                        static_cast<std::size_t>(padIndex),
                        monotonicNanos());
        switch (action) {
            case mpc::sequencer::SequenceLaunchAction::Queued:
                return toJString(
                        env,
                        "Sequence queued: "
                                + std::to_string(sequenceIndex + 1));
            case mpc::sequencer::SequenceLaunchAction::QueueCleared:
                return toJString(env, "Sequence queue cleared");
            default:
                return toJString(env, "Sequence launch failed");
        }
    }

    stopSequenceForMutation();
    const auto action =
            mpc::sequencer::handleSequencePadPress(
                    state,
                    core.sequenceTransportClock(),
                    static_cast<std::size_t>(bank),
                    static_cast<std::size_t>(padIndex),
                    monotonicNanos());
    if (action != mpc::sequencer::SequenceLaunchAction::Selected) {
        return toJString(env, "Sequence launch failed");
    }
    return toJString(
            env,
            "Sequence selected: " + std::to_string(sequenceIndex + 1)
                    + " | " + state.sequenceStatus());
}

} // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceLaunchPad(
        JNIEnv* env,
        jobject /* thiz */,
        jint bank,
        jint padIndex)
{
    return selectSequenceFromPadForUi(env, bank, padIndex);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSelect(
        JNIEnv* env, jobject /* thiz */, jint sequenceIndex)
{
    if (sequenceIndex < 0) {
        return toJString(env, "Sequence selection failed: invalid index");
    }
    return selectSequenceForUi(
            env,
            static_cast<std::size_t>(sequenceIndex));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceNext(
        JNIEnv* env, jobject /* thiz */)
{
    auto& state = mpc::MpcCore::instance().projectState();
    if (state.sequenceCount() == 0) {
        return toJString(env, "Sequence next failed: no sequences");
    }
    const auto queuedIndex =
            mpc::MpcCore::instance().sequenceTransportClock().queuedSequenceIndex();
    const auto baseIndex = queuedIndex >= 0
            && static_cast<std::size_t>(queuedIndex) < state.sequenceCount()
            ? static_cast<std::size_t>(queuedIndex)
            : state.activeSequenceIndex();
    const auto nextIndex =
            (baseIndex + 1) % state.sequenceCount();
    return selectSequenceForUi(env, nextIndex);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequencePrevious(
        JNIEnv* env, jobject /* thiz */)
{
    auto& state = mpc::MpcCore::instance().projectState();
    if (state.sequenceCount() == 0) {
        return toJString(env, "Sequence previous failed: no sequences");
    }
    const auto count = state.sequenceCount();
    const auto queuedIndex =
            mpc::MpcCore::instance().sequenceTransportClock().queuedSequenceIndex();
    const auto baseIndex = queuedIndex >= 0
            && static_cast<std::size_t>(queuedIndex) < count
            ? static_cast<std::size_t>(queuedIndex)
            : state.activeSequenceIndex();
    const auto previous =
            baseIndex == 0
                    ? count - 1
                    : baseIndex - 1;
    return selectSequenceForUi(env, previous);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceAddSequence(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();
    if (state.sequenceCount() >= mpc::domain::kMaxSequences) {
        return toJString(env, "Sequence add failed: maximum reached");
    }

    stopSequenceForMutation();

    if (!state.addSequence()) {
        return toJString(env, "Sequence add failed");
    }

    core.sequenceTransportClock().update(
            state.activeSequence(),
            sequenceSession().positionTicks(),
            monotonicNanos(),
            false);
    return toJString(env, state.sequenceStatus());
}

extern "C" JNIEXPORT jdouble JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTempo(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jdouble>(
            mpc::MpcCore::instance().projectState().activeSequence().tempoBpm);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTempo(
        JNIEnv* env, jobject /* thiz */, jdouble tempo)
{
    stopSequenceForMutation();
    if (mpc::MpcCore::instance().projectState().setSequenceTempo(tempo)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence tempo updated");
    }
    return toJString(env, "Sequence tempo failed: use 20–300 BPM");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetBars(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    return static_cast<jint>(
            mpc::sequencer::sequenceBars(state.activeSequence()));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetBars(
        JNIEnv* env, jobject /* thiz */, jint bars)
{
    stopSequenceForMutation();
    if (mpc::MpcCore::instance().projectState().setSequenceBars(bars)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence length updated");
    }
    return toJString(env, "Sequence length failed: invalid bar count");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetNumerator(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequence().numerator);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetDenominator(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequence().denominator);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTimeSignature(
        JNIEnv* env, jobject /* thiz */, jint numerator, jint denominator)
{
    stopSequenceForMutation();
    if (mpc::MpcCore::instance().projectState().setSequenceTimeSignature(
            numerator, denominator)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence time signature updated");
    }
    return toJString(env, "Time signature failed: 1–16 / 4, 8, 16 or 32");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsLoopEnabled(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return mpc::MpcCore::instance().projectState().activeSequence().loopEnabled
            ? JNI_TRUE
            : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetLoopEnabled(
        JNIEnv* env, jobject /* thiz */, jboolean enabled)
{
    stopSequenceForMutation();
    auto& state = mpc::MpcCore::instance().projectState();
    const auto& sequence = state.activeSequence();
    const auto bars = mpc::sequencer::sequenceBars(sequence);
    const auto perBar = mpc::sequencer::barLengthTicks(
            sequence.numerator, sequence.denominator);

    const auto startBar = sequence.loopStartTicks / std::max(1, perBar) + 1;
    const auto endBar = std::max(
            startBar,
            (sequence.loopEndTicks + std::max(1, perBar) - 1)
                    / std::max(1, perBar));

    if (state.setSequenceLoop(
            enabled == JNI_TRUE,
            startBar,
            std::min(bars, endBar))) {
        syncSequenceTransportStopped();
        return toJString(
                env,
                enabled == JNI_TRUE ? "Sequence loop ON" : "Sequence loop OFF");
    }
    return toJString(env, "Sequence loop change failed");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetLoopStartBar(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    const auto perBar = std::max(
            1,
            mpc::sequencer::barLengthTicks(
                    state.activeSequence().numerator,
                    state.activeSequence().denominator));
    return static_cast<jint>(
            state.activeSequence().loopStartTicks / perBar + 1);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetLoopEndBar(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    const auto perBar = std::max(
            1,
            mpc::sequencer::barLengthTicks(
                    state.activeSequence().numerator,
                    state.activeSequence().denominator));
    return static_cast<jint>(
            (state.activeSequence().loopEndTicks + perBar - 1) / perBar);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetLoopBars(
        JNIEnv* env, jobject /* thiz */, jint startBar, jint endBar)
{
    stopSequenceForMutation();
    auto& state = mpc::MpcCore::instance().projectState();
    if (state.setSequenceLoop(
            state.activeSequence().loopEnabled,
            startBar,
            endBar)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence loop range updated");
    }
    return toJString(env, "Sequence loop range failed");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetQuantizeGrid(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequence().quantizeGridTicks);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetQuantizeGrid(
        JNIEnv* env, jobject /* thiz */, jint ticks)
{
    stopSequenceForMutation();
    if (mpc::MpcCore::instance().projectState().setSequenceQuantizeGrid(ticks)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence quantize grid updated");
    }
    return toJString(env, "Quantize grid failed");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceQuantizeSelectedTrack(
        JNIEnv* env, jobject /* thiz */)
{
    stopSequenceForMutation();

    auto& state = mpc::MpcCore::instance().projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;

    if (trackIndex >= tracks.size()) {
        return toJString(env, "QUANTIZE failed: no selected track");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum
            || track.patterns.empty()) {
        return toJString(
                env,
                "QUANTIZE unavailable: selected track has no Drum pattern");
    }

    auto& pattern = track.patterns.front();
    const auto gridTicks =
            state.activeSequence().quantizeGridTicks;
    const auto changed =
            mpc::sequencer::quantizePattern(pattern, gridTicks);

    syncSequenceTransportStopped();

    return toJString(
            env,
            "QUANTIZE " + std::to_string(changed)
                    + " events • grid=" + std::to_string(gridTicks));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsTimingCorrectEnabled(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return mpc::MpcCore::instance().projectState()
            .activeSequence().timingCorrectEnabled
            ? JNI_TRUE
            : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTimingCorrectEnabled(
        JNIEnv* env, jobject /* thiz */, jboolean enabled)
{
    stopSequenceForMutation();
    auto& state = mpc::MpcCore::instance().projectState();
    state.activeSequence().timingCorrectEnabled = enabled == JNI_TRUE;
    syncSequenceTransportStopped();
    return toJString(
            env,
            state.activeSequence().timingCorrectEnabled
                    ? "TIMING CORRECT: ON"
                    : "TIMING CORRECT: OFF");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetSwing(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequence().swingPercent);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetSwing(
        JNIEnv* env, jobject /* thiz */, jint percent)
{
    stopSequenceForMutation();
    if (mpc::MpcCore::instance().projectState().setSequenceSwing(percent)) {
        syncSequenceTransportStopped();
        return toJString(env, "Sequence swing updated");
    }
    return toJString(env, "Swing failed: use 0–100%");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTrackCount(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeSequence().tracks.size());
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetSelectedTrack(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().activeTrackIndex());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTrackArrangementData(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex)
                    >= state.activeSequence().tracks.size()) {
        return toJString(env, "1|");
    }

    const auto& track =
            state.activeSequence().tracks[static_cast<std::size_t>(trackIndex)];
    if (track.patterns.empty()) {
        return toJString(env, "1|");
    }

    const auto& pattern = track.patterns.front();
    std::string result = std::to_string(std::max<std::int32_t>(
            1, pattern.lengthTicks));
    result.push_back('|');

    constexpr std::size_t kMaxArrangementEvents = 128;
    const auto limit = std::min(
            kMaxArrangementEvents,
            pattern.notes.size());
    for (std::size_t index = 0; index < limit; ++index) {
        const auto& note = pattern.notes[index];
        if (index > 0) result.push_back(';');
        result += std::to_string(std::max<std::int32_t>(0, note.tick));
        result.push_back(',');
        result += std::to_string(std::max<std::int32_t>(
                1, note.durationTicks));
    }
    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTrackType(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex)
                    >= state.activeSequence().tracks.size()) {
        return toJString(env, "NONE");
    }

    const auto& track =
            state.activeSequence().tracks[static_cast<std::size_t>(trackIndex)];
    switch (track.kind) {
        case mpc::domain::TrackKind::Drum: return toJString(env, "DRUM");
        case mpc::domain::TrackKind::Keygroup: return toJString(env, "KEYGROUP");
        case mpc::domain::TrackKind::Plugin: return toJString(env, "PLUGIN");
        case mpc::domain::TrackKind::Midi: return toJString(env, "MIDI");
        case mpc::domain::TrackKind::Audio: return toJString(env, "AUDIO");
    }
    return toJString(env, "NONE");
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsTrackMuted(
        JNIEnv* /* env */, jobject /* thiz */, jint trackIndex)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex)
                    >= state.activeSequence().tracks.size()) {
        return JNI_FALSE;
    }

    return state.activeSequence().tracks[
            static_cast<std::size_t>(trackIndex)].muted
            ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTrackProgram(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex)
                    >= state.activeSequence().tracks.size()) {
        return toJString(env, "PROGRAM • NONE");
    }

    const auto& track =
            state.activeSequence().tracks[static_cast<std::size_t>(trackIndex)];
    if (track.programId.empty()) {
        return toJString(env, "PROGRAM • NONE");
    }

    for (const auto& program : state.project().drumPrograms) {
        if (program.id == track.programId) {
            return toJString(
                    env,
                    "PROGRAM • " + (program.name.empty()
                            ? program.id : program.name));
        }
    }

    return toJString(
            env,
            "PROGRAM • " + track.programId);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetDrumProgramCount(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().project().drumPrograms.size());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetDrumProgramName(
        JNIEnv* env, jobject /* thiz */, jint programIndex)
{
    const auto& programs =
            mpc::MpcCore::instance().projectState().project().drumPrograms;
    if (programIndex < 0
            || static_cast<std::size_t>(programIndex) >= programs.size()) {
        return toJString(env, "NONE");
    }
    const auto& program = programs[static_cast<std::size_t>(programIndex)];
    return toJString(
            env,
            program.name.empty() ? program.id : program.name);
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetTrackProgramIndex(
        JNIEnv* /* env */, jobject /* thiz */, jint trackIndex)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0) {
        return -1;
    }
    return static_cast<jint>(
            state.activeProgramIndexForTrack(
                    static_cast<std::size_t>(trackIndex)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTrackProgram(
        JNIEnv* env, jobject /* thiz */, jint trackIndex, jint programIndex)
{
    if (trackIndex < 0 || programIndex < 0) {
        return toJString(env, "Program selection failed");
    }

    auto& state = mpc::MpcCore::instance().projectState();
    const auto& programs = state.project().drumPrograms;
    if (static_cast<std::size_t>(programIndex) >= programs.size()) {
        return toJString(env, "Program selection failed: invalid program");
    }

    const std::string programId =
            programs[static_cast<std::size_t>(programIndex)].id;
    if (!state.setTrackProgram(
            static_cast<std::size_t>(trackIndex), programId)) {
        return toJString(
                env,
                "Program selection failed: Track requires a Drum Program");
    }

    return toJString(
            env,
            "Program • " + programs[static_cast<std::size_t>(programIndex)].name);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSelectTrack(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    if (trackIndex < 0) {
        return toJString(env, "Track selection failed");
    }
    auto& state = mpc::MpcCore::instance().projectState();
    if (state.selectTrack(static_cast<std::size_t>(trackIndex))) {
        return toJString(
                env,
                state.trackStatus(static_cast<std::size_t>(trackIndex)));
    }
    return toJString(env, "Track selection failed");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceAddTrack(
        JNIEnv* env, jobject /* thiz */, jint kind)
{
    if (kind < 0 || kind > 4) {
        return toJString(env, "Track creation failed: invalid type");
    }

    auto& state = mpc::MpcCore::instance().projectState();
    if (!state.addTrack(
            static_cast<mpc::domain::TrackKind>(kind))) {
        return toJString(env, "Track creation failed");
    }
    return toJString(
            env,
            state.trackStatus(state.activeTrackIndex()));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceTrackStatus(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    if (trackIndex < 0) {
        return toJString(env, "Track unavailable");
    }
    return toJString(
            env,
            mpc::MpcCore::instance().projectState().trackStatus(
                    static_cast<std::size_t>(trackIndex)));
}


extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceStart(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    core.sequenceTransportClock().clearQueuedSequence();
    const auto audioResult = core.audio().start();
    if (audioResult.rfind("Audio output", 0) != 0) {
        return toJString(
                env,
                "Sequence start failed: " + audioResult);
    }

    if (core.sequenceRecorder().active()) {
        core.sequenceRecorder().finish(
                core.projectState(),
                core.sequenceRecordQueue(),
                sequenceSession().positionTicks());
    }

    const auto recordStartState =
            core.projectState().sequenceStatus();

    core.sequenceRecorder().begin(core.projectState());

    const auto transportNow = monotonicNanos();
    const auto startPosition =
            core.sequenceTransportClock().positionAtTimestamp(transportNow);

    if (!sequenceSession().start(startPosition)) {
        core.sequenceRecorder().finish(
                core.projectState(),
                core.sequenceRecordQueue(),
                0);
        return toJString(
                env,
                "Sequence start failed: no playable Drum Track");
    }

    const auto& sequence = core.projectState().activeSequence();
    core.sequenceTransportClock().start(
            sequence,
            sequenceSession().positionTicks(),
            transportNow);

    return toJString(
            env,
            recordStartState
                    + " | " + core.projectState().sequenceStatus()
                    + " | playing");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceStop(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    core.sequenceTransportClock().clearQueuedSequence();
    const auto position = sequenceSession().positionTicks();
    const auto recorded = core.sequenceRecorder().finish(
            core.projectState(),
            core.sequenceRecordQueue(),
            position);

    sequenceSession().stop();
    core.sequenceTransportClock().stop(
            core.projectState().activeSequence(),
            position,
            monotonicNanos());

    return toJString(
            env,
            "Sequence stopped"
                    + std::string(" | recorded=")
                    + std::to_string(recorded)
                    + " | "
                    + core.projectState().sequenceStatus());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceReset(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    core.sequenceTransportClock().clearQueuedSequence();
    const auto position = sequenceSession().positionTicks();
    core.sequenceRecorder().finish(
            core.projectState(),
            core.sequenceRecordQueue(),
            position);
    sequenceSession().reset();

    const auto& sequence = core.projectState().activeSequence();
    core.sequenceTransportClock().update(
            sequence,
            sequenceSession().positionTicks(),
            monotonicNanos(),
            sequenceSession().isPlaying());

    return toJString(env, "Sequence position reset");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceAdvance(
        JNIEnv* /* env */, jobject /* thiz */, jlong milliseconds)
{
    if (milliseconds <= 0) {
        return 0;
    }

    auto& core = mpc::MpcCore::instance();
    const auto& sequence = core.projectState().activeSequence();
    const auto ticksPerMillisecond =
            sequence.tempoBpm * 960.0 / 60000.0;
    const auto ticks = static_cast<std::int64_t>(
            std::llround(
                    static_cast<double>(milliseconds)
                    * ticksPerMillisecond));

    if (ticks <= 0) {
        return 0;
    }

    const auto advanceResult = sequenceSession().advance(
            ticks,
            0x53455131u,
            core.audio().outputSampleRate());

    auto& state = core.projectState();
    const auto queuedIndex =
            core.sequenceTransportClock().queuedSequenceIndex();

    if (advanceResult.wrapped
            && queuedIndex >= 0
            && static_cast<std::size_t>(queuedIndex) < state.sequenceCount()) {
        const bool wasRecording = core.sequenceRecorder().active();
        if (wasRecording) {
            core.sequenceRecorder().finish(
                    state,
                    core.sequenceRecordQueue(),
                    sequenceSession().positionTicks());
        }

        sequenceSession().stop();
        core.sequenceTransportClock().clearQueuedSequence();

        if (state.selectSequence(static_cast<std::size_t>(queuedIndex))
                && sequenceSession().start()) {
            if (wasRecording) {
                core.sequenceRecorder().begin(state);
            }
            core.sequenceTransportClock().start(
                    state.activeSequence(),
                    sequenceSession().positionTicks(),
                    monotonicNanos());
        } else {
            core.sequenceTransportClock().stop(
                    state.activeSequence(),
                    sequenceSession().positionTicks(),
                    monotonicNanos());
        }
    } else {
        core.sequenceTransportClock().update(
                state.activeSequence(),
                sequenceSession().positionTicks(),
                monotonicNanos(),
                sequenceSession().isPlaying());
    }

    core.sequenceRecorder().drain(
            state,
            core.sequenceRecordQueue());

    return static_cast<jint>(
            std::min<std::int64_t>(
                    std::numeric_limits<jint>::max(),
                    sequenceSession().positionTicks()));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceCopyPadToPads(
        JNIEnv* env,
        jobject /* thiz */,
        jint sourcePad,
        jint destinationMask)
{
    if (sourcePad < 0
            || sourcePad >= static_cast<jint>(mpc::domain::kMaxProgramPads)) {
        return toJString(env, "COPY PAD failed: invalid source pad");
    }

    auto& core = mpc::MpcCore::instance();
    auto& audio = core.audio();
    auto& history = core.editHistory();

    const auto mask =
            static_cast<std::uint16_t>(destinationMask & 0xFFFF);
    std::array<mpc::audio::AudioEngine::PadEditSnapshot,
               mpc::domain::kMaxProgramPads> before{};
    std::array<mpc::audio::AudioEngine::PadEditSnapshot,
               mpc::domain::kMaxProgramPads> after{};
    std::vector<std::uint8_t> destinations;

    for (std::size_t pad = 0;
            pad < mpc::domain::kMaxProgramPads;
            ++pad) {
        if ((mask & (std::uint16_t{1u} << pad)) != 0u
                && pad != static_cast<std::size_t>(sourcePad)) {
            destinations.push_back(static_cast<std::uint8_t>(pad));
            before[pad] = audio.capturePadEditSnapshot(
                    static_cast<std::uint8_t>(pad));
        }
    }

    if (destinations.empty()) {
        return toJString(env, "COPY PAD failed: select a destination");
    }

    const auto result = audio.copyPadToPads(
            static_cast<std::uint8_t>(sourcePad), mask);
    if (result.rfind("Copy Pad failed:", 0) == 0
            || result == "Copy Pad requires stopped playback") {
        return toJString(env, result);
    }

    for (const auto pad : destinations) {
        after[pad] = audio.capturePadEditSnapshot(pad);
    }

    const auto label =
            "Copy Pad • PAD "
            + std::to_string(sourcePad + 1)
            + " → "
            + std::to_string(destinations.size())
            + (destinations.size() == 1 ? " pad" : " pads");

    const bool recorded = history.record({
            label,
            [&audio, destinations, before]() mutable {
                for (const auto pad : destinations) {
                    if (!audio.applyPadEditSnapshot(pad, before[pad])) {
                        return false;
                    }
                }
                return true;
            },
            [&audio, destinations, after]() mutable {
                for (const auto pad : destinations) {
                    if (!audio.applyPadEditSnapshot(pad, after[pad])) {
                        return false;
                    }
                }
                return true;
            }});

    if (!recorded) {
        for (const auto pad : destinations) {
            static_cast<void>(audio.applyPadEditSnapshot(pad, before[pad]));
        }
        return toJString(env, "COPY PAD failed: history unavailable");
    }

    return toJString(env, result);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceDeletePadAssignments(
        JNIEnv* env,
        jobject /* thiz */,
        jint padMask)
{
    auto& core = mpc::MpcCore::instance();
    auto& audio = core.audio();
    auto& history = core.editHistory();
    const auto mask =
            static_cast<std::uint16_t>(padMask & 0xFFFF);

    if (mask == 0u) {
        return toJString(env, "DELETE PAD failed: select at least one pad");
    }

    std::array<mpc::audio::AudioEngine::PadEditSnapshot,
               mpc::domain::kMaxProgramPads> before{};
    std::array<mpc::audio::AudioEngine::PadEditSnapshot,
               mpc::domain::kMaxProgramPads> after{};
    std::vector<std::uint8_t> pads;

    for (std::size_t pad = 0;
            pad < mpc::domain::kMaxProgramPads;
            ++pad) {
        if ((mask & (std::uint16_t{1u} << pad)) != 0u) {
            pads.push_back(static_cast<std::uint8_t>(pad));
            before[pad] = audio.capturePadEditSnapshot(
                    static_cast<std::uint8_t>(pad));
        }
    }

    const auto result = audio.deletePadAssignments(mask);
    if (result.rfind("Delete Pad failed:", 0) == 0
            || result == "Delete Pad requires stopped playback") {
        return toJString(env, result);
    }

    for (const auto pad : pads) {
        after[pad] = audio.capturePadEditSnapshot(pad);
    }

    const auto label =
            "Delete Pad • "
            + std::to_string(pads.size())
            + (pads.size() == 1 ? " pad" : " pads");

    const bool recorded = history.record({
            label,
            [&audio, pads, before]() mutable {
                for (const auto pad : pads) {
                    if (!audio.applyPadEditSnapshot(pad, before[pad])) {
                        return false;
                    }
                }
                return true;
            },
            [&audio, pads, after]() mutable {
                for (const auto pad : pads) {
                    if (!audio.applyPadEditSnapshot(pad, after[pad])) {
                        return false;
                    }
                }
                return true;
            }});

    if (!recorded) {
        for (const auto pad : pads) {
            static_cast<void>(audio.applyPadEditSnapshot(pad, before[pad]));
        }
        return toJString(env, "DELETE PAD failed: history unavailable");
    }

    return toJString(env, result);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceCanUndo(
        JNIEnv* /* env */,
        jobject /* thiz */)
{
    return mpc::MpcCore::instance().editHistory().canUndo()
            ? JNI_TRUE
            : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceCanRedo(
        JNIEnv* /* env */,
        jobject /* thiz */)
{
    return mpc::MpcCore::instance().editHistory().canRedo()
            ? JNI_TRUE
            : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceUndo(
        JNIEnv* env,
        jobject /* thiz */)
{
    auto& history = mpc::MpcCore::instance().editHistory();
    if (!history.canUndo()) {
        return toJString(env, "UNDO • nothing to undo");
    }

    const auto label = history.nextUndoLabel();
    if (!history.undo()) {
        return toJString(
                env,
                "UNDO • unavailable while playback is running");
    }

    return toJString(env, "UNDO • " + label);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceRedo(
        JNIEnv* env,
        jobject /* thiz */)
{
    auto& history = mpc::MpcCore::instance().editHistory();
    if (!history.canRedo()) {
        return toJString(env, "REDO • nothing to redo");
    }

    const auto label = history.nextRedoLabel();
    if (!history.redo()) {
        return toJString(
                env,
                "REDO • unavailable while playback is running");
    }

    return toJString(env, "REDO • " + label);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceErasePadAtPlayhead(
        JNIEnv* env, jobject /* thiz */, jint padIndex)
{
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)) {
        return toJString(env, "ERASE failed: invalid pad");
    }

    auto& core = mpc::MpcCore::instance();
    if (!sequenceSession().isPlaying()) {
        return toJString(
                env,
                "ERASE • playback required for live Erase + Pad");
    }

    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    auto& tracks = state.activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        return toJString(env, "ERASE failed: invalid selected track");
    }

    auto& track = tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum
            || track.patterns.empty()) {
        return toJString(env, "ERASE • selected track is not DRUM");
    }

    auto& pattern = track.patterns.front();
    const auto noteNumber =
            state.activeDrumProgram()
                    .pads[static_cast<std::size_t>(padIndex)]
                    .midiNote;
    const auto now = monotonicNanos();
    const auto playhead =
            core.sequenceTransportClock().positionAtTimestamp(now);
    const auto gridTicks = std::max<std::int64_t>(
            1,
            state.activeSequence().quantizeGridTicks);
    const auto maxDistanceTicks = std::max<std::int64_t>(
            1,
            gridTicks / 2);

    if (!mpc::sequencer::erase::eraseNearestEvent(
                pattern,
                noteNumber,
                playhead,
                maxDistanceTicks)) {
        return toJString(
                env,
                "ERASE • no matching event near playhead");
    }

    return toJString(
            env,
            "ERASE • PAD "
                    + std::to_string(padIndex + 1)
                    + " EVENT REMOVED");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetLocator(
        JNIEnv* env, jobject /* thiz */, jint slot)
{
    if (slot < 0
            || slot >= static_cast<jint>(
                    mpc::sequencer::locate::kLocatorCount)) {
        return toJString(env, "Locator set failed: invalid slot");
    }

    auto& core = mpc::MpcCore::instance();
    auto& sequence = core.projectState().activeSequence();
    const auto now = monotonicNanos();
    const auto position =
            mpc::sequencer::locate::clampTick(
                    core.sequenceTransportClock().positionAtTimestamp(now),
                    sequence.lengthTicks);
    sequence.locatorTicks[static_cast<std::size_t>(slot)] = position;

    return toJString(
            env,
            "LOCATOR "
                    + std::to_string(slot + 1)
                    + " SET • "
                    + std::to_string(position)
                    + " ticks");
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetLocator(
        JNIEnv* /* env */, jobject /* thiz */, jint slot)
{
    if (slot < 0
            || slot >= static_cast<jint>(
                    mpc::sequencer::locate::kLocatorCount)) {
        return -1;
    }

    return static_cast<jlong>(
            mpc::MpcCore::instance()
                    .projectState()
                    .activeSequence()
                    .locatorTicks[static_cast<std::size_t>(slot)]);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceJumpToLocator(
        JNIEnv* env, jobject /* thiz */, jint slot)
{
    if (slot < 0
            || slot >= static_cast<jint>(
                    mpc::sequencer::locate::kLocatorCount)) {
        return toJString(env, "Locator jump failed: invalid slot");
    }

    auto& core = mpc::MpcCore::instance();
    const auto& sequence = core.projectState().activeSequence();
    const auto target =
            sequence.locatorTicks[static_cast<std::size_t>(slot)];
    if (target < 0) {
        return toJString(
                env,
                "LOCATOR "
                        + std::to_string(slot + 1)
                        + " EMPTY");
    }

    const auto now = monotonicNanos();
    const auto requested = mpc::sequencer::locate::clampTick(
            target,
            sequence.lengthTicks);

    sequenceSession().setPositionTicks(requested);
    core.sequenceTransportClock().update(
            sequence,
            requested,
            now,
            sequenceSession().isPlaying());
    const auto normalized =
            core.sequenceTransportClock().snapshot().positionTicks;
    sequenceSession().setPositionTicks(normalized);

    return toJString(
            env,
            "LOCATOR "
                    + std::to_string(slot + 1)
                    + " • JUMP "
                    + std::to_string(normalized)
                    + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceLocateMoveTicks(
        JNIEnv* env, jobject /* thiz */, jlong deltaTicks)
{
    auto& core = mpc::MpcCore::instance();
    const auto& sequence = core.projectState().activeSequence();
    const auto now = monotonicNanos();
    const auto current =
            core.sequenceTransportClock().positionAtTimestamp(now);

    if (deltaTicks > 0
            && current > std::numeric_limits<std::int64_t>::max() - deltaTicks) {
        return toJString(env, "Locate move failed: tick overflow");
    }
    if (deltaTicks < 0
            && current < std::numeric_limits<std::int64_t>::min() - deltaTicks) {
        return toJString(env, "Locate move failed: tick overflow");
    }

    const auto requested = mpc::sequencer::locate::clampTick(
            current + static_cast<std::int64_t>(deltaTicks),
            sequence.lengthTicks);
    sequenceSession().setPositionTicks(requested);
    core.sequenceTransportClock().update(
            sequence,
            requested,
            now,
            sequenceSession().isPlaying());
    const auto normalized =
            core.sequenceTransportClock().snapshot().positionTicks;
    sequenceSession().setPositionTicks(normalized);

    return toJString(
            env,
            "LOCATE • PLAYHEAD "
                    + std::to_string(normalized)
                    + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceMoveToLocateBoundary(
        JNIEnv* env, jobject /* thiz */, jint direction)
{
    if (direction == 0) {
        return toJString(env, "Locate boundary failed: invalid direction");
    }

    auto& core = mpc::MpcCore::instance();
    const auto& sequence = core.projectState().activeSequence();
    const auto target = direction < 0
            ? std::int64_t{0}
            : mpc::sequencer::locate::clampTick(
                    std::max<std::int64_t>(0, sequence.lengthTicks - 1),
                    sequence.lengthTicks);
    const auto now = monotonicNanos();

    sequenceSession().setPositionTicks(target);
    core.sequenceTransportClock().update(
            sequence,
            target,
            now,
            sequenceSession().isPlaying());
    const auto normalized =
            core.sequenceTransportClock().snapshot().positionTicks;
    sequenceSession().setPositionTicks(normalized);

    return toJString(
            env,
            direction < 0
                    ? "LOCATE • SEQUENCE START"
                    : "LOCATE • SEQUENCE END");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceMoveToPreviousOrNextEvent(
        JNIEnv* env, jobject /* thiz */, jint direction)
{
    if (direction == 0) {
        return toJString(env, "Locate event failed: invalid direction");
    }

    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();
    const auto trackIndex = state.activeTrackIndex();
    if (trackIndex >= state.activeSequence().tracks.size()) {
        return toJString(env, "Locate event failed: no selected track");
    }

    auto& track = state.activeSequence().tracks[trackIndex];
    if (track.kind != mpc::domain::TrackKind::Drum || track.patterns.empty()) {
        return toJString(env, "Locate event failed: selected track has no Drum pattern");
    }

    const auto& pattern = track.patterns.front();
    const auto now = monotonicNanos();
    const auto current =
            core.sequenceTransportClock().positionAtTimestamp(now);

    bool found = false;
    std::int64_t target = direction < 0
            ? std::numeric_limits<std::int64_t>::min()
            : std::numeric_limits<std::int64_t>::max();
    for (const auto& note : pattern.notes) {
        const auto eventTick = std::clamp<std::int64_t>(
                static_cast<std::int64_t>(note.tick) + note.nudgeTicks,
                0,
                std::max<std::int64_t>(0, pattern.lengthTicks - 1));
        if (direction < 0) {
            if (eventTick < current && (!found || eventTick > target)) {
                target = eventTick;
                found = true;
            }
        } else if (eventTick > current && (!found || eventTick < target)) {
            target = eventTick;
            found = true;
        }
    }

    if (!found) {
        return toJString(
                env,
                direction < 0
                        ? "LOCATE • no previous event"
                        : "LOCATE • no next event");
    }

    sequenceSession().setPositionTicks(target);
    core.sequenceTransportClock().update(
            state.activeSequence(),
            target,
            now,
            sequenceSession().isPlaying());
    const auto normalized =
            core.sequenceTransportClock().snapshot().positionTicks;
    sequenceSession().setPositionTicks(normalized);

    return toJString(
            env,
            std::string("LOCATE • EVENT ")
                    + std::to_string(normalized)
                    + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceMovePlayheadTicks(
        JNIEnv* env, jobject /* thiz */, jlong deltaTicks)
{
    auto& core = mpc::MpcCore::instance();
    if (sequenceSession().isPlaying()) {
        return toJString(
                env,
                "Playhead move blocked: stop playback first");
    }

    const auto now = monotonicNanos();
    const auto current =
            core.sequenceTransportClock().positionAtTimestamp(now);

    std::int64_t delta = static_cast<std::int64_t>(deltaTicks);
    std::int64_t target = current;
    if (delta > 0
            && current > std::numeric_limits<std::int64_t>::max() - delta) {
        target = std::numeric_limits<std::int64_t>::max();
    } else if (delta < 0
            && current < std::numeric_limits<std::int64_t>::min() - delta) {
        target = std::numeric_limits<std::int64_t>::min();
    } else {
        target += delta;
    }

    core.sequenceTransportClock().stop(
            core.projectState().activeSequence(),
            target,
            now);

    return toJString(
            env,
            "Playhead "
                    + std::to_string(
                            core.sequenceTransportClock()
                                    .snapshot()
                                    .positionTicks)
                    + " ticks");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceTogglePadMute(
        JNIEnv* env, jobject /* thiz */, jint padIndex)
{
    if (padIndex < 0
            || padIndex >= static_cast<jint>(mpc::domain::kMaxProgramPads)) {
        return toJString(env, "Pad mute failed: invalid pad");
    }

    auto& pad = mpc::MpcCore::instance()
            .projectState()
            .activeDrumProgram()
            .pad(static_cast<std::size_t>(padIndex));
    pad.muted = !pad.muted;

    return toJString(
            env,
            "PAD " + std::to_string(padIndex + 1)
                    + " MUTE " + (pad.muted ? "ON" : "OFF"));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceToggleTrackMute(
        JNIEnv* env, jobject /* thiz */, jint trackIndex)
{
    auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || trackIndex >= static_cast<jint>(
                    state.activeSequence().tracks.size())) {
        return toJString(env, "Track mute failed: invalid track");
    }

    auto& track =
            state.activeSequence().tracks[static_cast<std::size_t>(trackIndex)];
    track.muted = !track.muted;

    return toJString(
            env,
            "TRACK " + std::to_string(trackIndex + 1)
                    + " MUTE " + (track.muted ? "ON" : "OFF"));
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequencePositionTicks(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jlong>(
            mpc::MpcCore::instance().sequenceTransportClock()
                    .positionAtTimestamp(monotonicNanos()));
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsPlaying(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return sequenceSession().isPlaying() ? JNI_TRUE : JNI_FALSE;
}


extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsSelectedTrackArmed(
        JNIEnv* /* env */, jobject /* thiz */)
{
    const auto& state = mpc::MpcCore::instance().projectState();
    const auto index = state.activeTrackIndex();
    const auto& tracks = state.activeSequence().tracks;
    return index < tracks.size() && tracks[index].recordArmed
            ? JNI_TRUE
            : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetSelectedTrackArmed(
        JNIEnv* env, jobject /* thiz */, jboolean armed)
{
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();
    auto& tracks = state.activeSequence().tracks;
    const auto index = state.activeTrackIndex();
    if (index >= tracks.size()) {
        return toJString(env, "Track arm failed: no selected track");
    }

    if (core.sequenceRecorder().active()) {
        core.sequenceRecorder().finish(
                state,
                core.sequenceRecordQueue(),
                sequenceSession().positionTicks());
    }

    for (auto& track : tracks) {
        track.recordArmed = false;
    }
    tracks[index].recordArmed = armed == JNI_TRUE;

    if (armed == JNI_TRUE && sequenceSession().isPlaying()) {
        core.sequenceRecorder().begin(state);
        core.sequenceTransportClock().update(
                state.activeSequence(),
                sequenceSession().positionTicks(),
                monotonicNanos(),
                true);
    }

    return toJString(
            env,
            state.trackStatus(index));
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceGetRecordMode(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return static_cast<jint>(
            mpc::MpcCore::instance().sequenceRecorder().mode()
                    == mpc::sequencer::PatternRecordMode::Replace
            ? 0
            : 1);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetRecordMode(
        JNIEnv* env, jobject /* thiz */, jint mode)
{
    if (mode != 0 && mode != 1) {
        return toJString(env, "Record mode failed");
    }

    auto& core = mpc::MpcCore::instance();
    core.sequenceRecorder().setMode(
            mode == 0
                    ? mpc::sequencer::PatternRecordMode::Replace
                    : mpc::sequencer::PatternRecordMode::Overdub);

    return toJString(
            env,
            mode == 0 ? "REC MODE: REPLACE" : "REC MODE: OVERDUB");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceDrainRecordEvents(
        JNIEnv* /* env */, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    return static_cast<jint>(
            core.sequenceRecorder().drain(
                    core.projectState(),
                    core.sequenceRecordQueue()));
}