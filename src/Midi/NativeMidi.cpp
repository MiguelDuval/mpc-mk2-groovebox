#include "NativeMidi.h"
#include "../MPC/MpcStudioMk2InputDecoder.h"
#include "../MPC/MpcStudioMk2LedProtocol.h"

#include <android/log.h>
#include <jni.h>

#include <cstddef>

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

void handleIncoming(std::span<const std::uint8_t> message, std::int64_t /*timestamp*/) {
    if (message.empty()) {
        return;
    }

    const auto event = mpc::studio::decodeInput(message);

    if (!event) {
        __android_log_print(
            ANDROID_LOG_DEBUG,
            kTag,
            "MIDI %zu-byte message: unrecognized",
            message.size());
        return;
    }

    if (event->type == mpc::studio::InputEventType::PadNote
            || event->type == mpc::studio::InputEventType::Button
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
        return;
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
}

} // namespace mpc::midi

extern "C" JNIEXPORT void JNICALL
Java_com_miguelduval_mpcmk2groovebox_AndroidMidiBridge_nativeOnMidi(
        JNIEnv* env, jclass, jbyteArray data, jlong timestamp) {
    if (data == nullptr) {
        return;
    }

    const jsize length = env->GetArrayLength(data);
    if (length <= 0) {
        return;
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes == nullptr) {
        return;
    }

    auto* raw = reinterpret_cast<const std::uint8_t*>(bytes);
    mpc::midi::handleIncoming(
        std::span<const std::uint8_t>(raw, static_cast<std::size_t>(length)),
        static_cast<std::int64_t>(timestamp));

    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
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
