#include "NativeMidi.h"
#include "../MPC/MpcStudioMk2InputDecoder.h"

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
