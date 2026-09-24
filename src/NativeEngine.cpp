#include <jni.h>
#include <oboe/Oboe.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_miguelduval_mpcmk2groovebox_MainActivity_nativeEngineInfo(
        JNIEnv* env, jobject /* thiz */)
{
    (void) static_cast<int>(oboe::Result::OK);

    constexpr auto* message =
        "Native foundation loaded: C++20 + Oboe. "
        "MPC Studio MkII hardware layer is next.";

    return env->NewStringUTF(message);
}
