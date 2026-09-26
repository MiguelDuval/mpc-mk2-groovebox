#include <jni.h>
#include "Audio/AudioEngine.h"

#include <cstdint>
#include <span>
#include <string>
#include <vector>

namespace {

jstring toJString(JNIEnv* env, const std::string& text) {
    return env->NewStringUTF(text.c_str());
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
