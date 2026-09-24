#include "NativeMidi.h"

#include <android/log.h>
#include <cstddef>
#include <cstdio>

namespace {
constexpr const char* kTag = "MpcMk2Groovebox";
}

namespace mpc::midi {
void handleIncoming(std::span<const std::uint8_t> message, std::int64_t /*timestamp*/) {
    if (message.empty()) return;
    __android_log_print(ANDROID_LOG_DEBUG, kTag,
                        "Native MIDI router received %zu-byte message",
                        message.size());
}
}

extern "C" JNIEXPORT void JNICALL
Java_com_miguelduval_mpcmk2groovebox_AndroidMidiBridge_nativeOnMidi(
        JNIEnv* env, jclass, jbyteArray data, jlong timestamp) {
    if (data == nullptr) return;

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) return;

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) return;

    auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
    mpc::midi::handleIncoming(
        std::span<const std::uint8_t>(raw, static_cast<std::size_t>(length)),
        static_cast<std::int64_t>(timestamp));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
}
