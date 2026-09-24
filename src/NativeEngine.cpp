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
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStatus(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, mpc::audio::AudioEngine::instance().status());
}
