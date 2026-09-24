#pragma once

#include <cstdint>
#include <optional>
#include <span>

namespace mpc::studio {

enum class InputEventType : std::uint8_t {
    PadNote,
    PadAftertouch,
    Button,
    JogWheel,
    JogPress,
    TouchStrip,
    ChannelAftertouch,
};

struct InputEvent {
    InputEventType type{};
    std::uint8_t channel = 0;
    std::uint8_t number = 0;
    std::uint8_t value = 0;
    std::uint8_t padIndex = 0xFF;
    bool pressed = false;
};

std::optional<InputEvent> decodeInput(std::span<const std::uint8_t> message);

} // namespace mpc::studio
