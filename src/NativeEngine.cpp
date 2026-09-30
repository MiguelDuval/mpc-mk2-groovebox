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

    std::array<jint, 4> values{0, 0, 0, 0};
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
                    0,
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

    if (!sequenceSession().start()) {
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
            monotonicNanos());

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
