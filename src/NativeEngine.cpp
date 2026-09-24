#include <jni.h>
#include "Audio/AudioEngine.h"

#include <memory>
#include <string>

namespace {

std::shared_ptr<mpc::audio::AudioEngine> audioEngine() {
    static const auto engine = std::make_shared<mpc::audio::AudioEngine>();
    return engine;
}

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
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStart(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, audioEngine()->start());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStop(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, audioEngine()->stop());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeAudioStatus(
        JNIEnv* env, jobject /* thiz */)
{
    return toJString(env, audioEngine()->status());
}
