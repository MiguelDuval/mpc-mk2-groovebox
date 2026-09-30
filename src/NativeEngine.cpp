#include <jni.h>
#include "Audio/AudioEngine.h"

#include <algorithm>
#include <chrono>
#include <cstdint>
#include <span>
#include <limits>
#include <string>
#include <vector>

#include "MPC/MpcCore.h"
#include "MPC/Sequencer/MpcSequencePlaybackSession.h"
#include "MPC/Sequencer/MpcSequenceSettings.h"

namespace {

jstring toJString(JNIEnv* env, const std::string& text) {
    return env->NewStringUTF(text.c_str());
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

    mpc::audio::AudioEngine::instance().triggerPad(
            static_cast<std::uint8_t>(pad),
            static_cast<std::uint8_t>(velocity));
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
    sequenceSession().stop();
    if (mpc::MpcCore::instance().projectState().setSequenceTempo(tempo)) {
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
    sequenceSession().stop();
    if (mpc::MpcCore::instance().projectState().setSequenceBars(bars)) {
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
    sequenceSession().stop();
    if (mpc::MpcCore::instance().projectState().setSequenceTimeSignature(
            numerator, denominator)) {
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
    sequenceSession().stop();
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
    sequenceSession().stop();
    auto& state = mpc::MpcCore::instance().projectState();
    if (state.setSequenceLoop(
            state.activeSequence().loopEnabled,
            startBar,
            endBar)) {
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
    sequenceSession().stop();
    if (mpc::MpcCore::instance().projectState().setSequenceQuantizeGrid(ticks)) {
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
    sequenceSession().stop();
    if (mpc::MpcCore::instance().projectState().setSequenceSwing(percent)) {
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



extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsTrackMuted(
        JNIEnv* /* env */, jobject /* thiz */, jint trackIndex)
{
    const auto& tracks = mpc::MpcCore::instance().projectState().activeSequence().tracks;
    return trackIndex >= 0
            && static_cast<std::size_t>(trackIndex) < tracks.size()
            && tracks[static_cast<std::size_t>(trackIndex)].muted
            ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsTrackSoloed(
        JNIEnv* /* env */, jobject /* thiz */, jint trackIndex)
{
    const auto& tracks = mpc::MpcCore::instance().projectState().activeSequence().tracks;
    return trackIndex >= 0
            && static_cast<std::size_t>(trackIndex) < tracks.size()
            && tracks[static_cast<std::size_t>(trackIndex)].soloed
            ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTrackMuted(
        JNIEnv* env, jobject /* thiz */, jint trackIndex, jboolean muted)
{
    auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex) >= state.activeSequence().tracks.size()) {
        return toJString(env, "Track mute failed");
    }
    state.setTrackMuted(static_cast<std::size_t>(trackIndex), muted == JNI_TRUE);
    return toJString(env, state.trackStatus(static_cast<std::size_t>(trackIndex)));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetTrackSoloed(
        JNIEnv* env, jobject /* thiz */, jint trackIndex, jboolean soloed)
{
    auto& state = mpc::MpcCore::instance().projectState();
    if (trackIndex < 0
            || static_cast<std::size_t>(trackIndex) >= state.activeSequence().tracks.size()) {
        return toJString(env, "Track solo failed");
    }
    state.setTrackSoloed(static_cast<std::size_t>(trackIndex), soloed == JNI_TRUE);
    return toJString(env, state.trackStatus(static_cast<std::size_t>(trackIndex)));
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
    return static_cast<jint>(
            mpc::MpcCore::instance().projectState().sequenceCount());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsChainEnabled(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return mpc::MpcCore::instance().projectState().sequenceChainEnabled()
            ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceSetChainEnabled(
        JNIEnv* env, jobject /* thiz */, jboolean enabled)
{
    mpc::MpcCore::instance().projectState().setSequenceChainEnabled(
            enabled == JNI_TRUE);
    return toJString(
            env,
            enabled == JNI_TRUE ? "Sequence chain ON" : "Sequence chain OFF");
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceAddSequence(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    sequenceSession().stop();
    if (!core.projectState().addSequence()) {
        return toJString(env, "Sequence creation failed");
    }
    core.sequenceTransportClock().update(
            core.projectState().activeSequence(),
            0,
            monotonicNanos(),
            false);
    return toJString(env, core.projectState().sequenceStatus());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceNext(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    const bool wasPlaying = sequenceSession().isPlaying();
    const bool wasRecording = core.sequenceRecorder().active();
    if (wasRecording) {
        core.sequenceRecorder().finish(
                core.projectState(),
                core.sequenceRecordQueue(),
                sequenceSession().positionTicks());
    }
    sequenceSession().stop();
    if (!core.projectState().nextSequence()) {
        return toJString(env, "Next sequence unavailable");
    }
    if (wasPlaying) {
        const auto audioResult = core.audio().start();
        if (audioResult.rfind("Audio output", 0) != 0) {
            return toJString(env, "Next sequence audio failed: " + audioResult);
        }
        sequenceSession().start();
        if (wasRecording) {
            core.sequenceRecorder().begin(core.projectState());
        }
        core.sequenceTransportClock().start(
                core.projectState().activeSequence(),
                sequenceSession().positionTicks(),
                monotonicNanos());
    }
    return toJString(env, core.projectState().sequenceStatus());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceRecordToggle(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();

    if (core.sequenceRecorder().active()) {
        const auto recorded = core.sequenceRecorder().finish(
                state,
                core.sequenceRecordQueue(),
                sequenceSession().positionTicks());
        return toJString(
                env,
                "Recording stopped | notes=" + std::to_string(recorded));
    }

    bool armed = false;
    for (const auto& track : state.activeSequence().tracks) {
        armed = armed || track.recordArmed;
    }
    if (!armed) {
        return toJString(env, "REC: arm a track first");
    }

    if (!sequenceSession().isPlaying()) {
        const auto audioResult = core.audio().start();
        if (audioResult.rfind("Audio output", 0) != 0) {
            return toJString(env, "REC start failed: " + audioResult);
        }
        if (!sequenceSession().start()) {
            return toJString(env, "REC start failed: no sequence tracks");
        }
        core.sequenceTransportClock().start(
                state.activeSequence(),
                sequenceSession().positionTicks(),
                monotonicNanos());
    }

    core.sequenceRecorder().begin(state);
    return toJString(
            env,
            "Recording active | "
                    + state.trackStatus(state.activeTrackIndex()));
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceStart(
        JNIEnv* env, jobject /* thiz */)
{
    auto& core = mpc::MpcCore::instance();
    const auto audioResult = core.audio().start();
    if (audioResult.rfind("Audio output", 0) != 0) {
        return toJString(
                env,
                "Sequence start failed: " + audioResult);
    }

    const auto recordStartState =
            core.projectState().sequenceStatus();

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

    sequenceSession().advance(
            ticks,
            0x53455131u,
            core.audio().outputSampleRate());

    auto& state = core.projectState();
    if (sequenceSession().didWrap()
            && state.sequenceChainEnabled()
            && state.sequenceCount() > 1) {
        const bool wasRecording = core.sequenceRecorder().active();
        if (wasRecording) {
            core.sequenceRecorder().finish(
                    state,
                    core.sequenceRecordQueue(),
                    sequenceSession().positionTicks());
        }

        sequenceSession().stop();
        state.nextSequence();
        sequenceSession().start();

        if (wasRecording) {
            core.sequenceRecorder().begin(state);
        }
    }

    const auto& activeSequence = state.activeSequence();
    core.sequenceTransportClock().update(
            activeSequence,
            sequenceSession().positionTicks(),
            monotonicNanos(),
            sequenceSession().isPlaying());

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
    return static_cast<jlong>(sequenceSession().positionTicks());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceIsPlaying(
        JNIEnv* /* env */, jobject /* thiz */)
{
    return sequenceSession().isPlaying() ? JNI_TRUE : JNI_FALSE;
}



extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeSequenceCapturePadHit(
        JNIEnv* env, jobject /* thiz */, jint pad, jint velocity)
{
    auto& core = mpc::MpcCore::instance();
    auto& state = core.projectState();

    if (!core.sequenceRecorder().active()
            || !sequenceSession().isPlaying()
            || pad < 0 || pad >= 16
            || velocity <= 0 || velocity > 127) {
        return toJString(env, "capture=idle");
    }

    const auto trackIndex = state.activeTrackIndex();
    if (trackIndex >= state.activeSequence().tracks.size()) {
        return toJString(env, "capture=idle");
    }

    const auto& track = state.activeSequence().tracks[trackIndex];
    if (!track.recordArmed || track.kind != mpc::domain::TrackKind::Drum) {
        return toJString(env, "capture=idle");
    }

    const auto tick = sequenceSession().positionTicks();
    const auto trackValue = static_cast<std::uint8_t>(trackIndex);
    const auto padValue = static_cast<std::uint8_t>(pad);
    const auto velocityValue = static_cast<std::uint8_t>(velocity);

    const mpc::sequencer::SequenceRecordEvent press{
            tick, trackValue, padValue, velocityValue, 1u};
    const mpc::sequencer::SequenceRecordEvent release{
            tick, trackValue, padValue, 0u, 0u};

    const bool pressed = core.sequenceRecordQueue().tryEnqueue(press);
    const bool released = core.sequenceRecordQueue().tryEnqueue(release);
    return toJString(
            env,
            (pressed && released)
                    ? "capture=recorded"
                    : "capture=queue-full");
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
    const auto index = state.activeTrackIndex();
    if (index >= state.activeSequence().tracks.size()) {
        return toJString(env, "Track arm failed: no selected track");
    }

    if (core.sequenceRecorder().active()) {
        core.sequenceRecorder().finish(
                state,
                core.sequenceRecordQueue(),
                sequenceSession().positionTicks());
    }

    if (!state.setTrackArmed(index, armed == JNI_TRUE)) {
        return toJString(env, "Track arm failed");
    }

    if (armed == JNI_TRUE && sequenceSession().isPlaying()) {
        core.sequenceRecorder().begin(state);
        core.sequenceTransportClock().update(
                state.activeSequence(),
                sequenceSession().positionTicks(),
                monotonicNanos(),
                true);
    }

    return toJString(env, state.trackStatus(index));
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
