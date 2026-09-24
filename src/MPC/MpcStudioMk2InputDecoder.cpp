#include "MpcStudioMk2InputDecoder.h"
#include "MpcStudioMk2ControlMap.h"

namespace {

const mpc::studio::PadDefinition* findPad(std::uint8_t note) {
    for (const auto& pad : mpc::studio::pads) {
        if (pad.midiNote == note) {
            return &pad;
        }
    }
    return nullptr;
}

const mpc::studio::ButtonDefinition* findButton(std::uint8_t note) {
    for (const auto& button : mpc::studio::buttons) {
        if (button.midiNote == note) {
            return &button;
        }
    }
    return nullptr;
}

} // namespace

namespace mpc::studio {

std::optional<InputEvent> decodeInput(std::span<const std::uint8_t> message) {
    if (message.size() < 2) {
        return std::nullopt;
    }

    const std::uint8_t status = message[0];
    const std::uint8_t messageType =
        static_cast<std::uint8_t>(status & 0xF0);
    const std::uint8_t channel =
        static_cast<std::uint8_t>(status & 0x0F);
    const std::uint8_t number = message[1];

    switch (messageType) {
        case 0x80:
        case 0x90: {
            if (message.size() < 3) {
                return std::nullopt;
            }

            const auto velocity = message[2];
            const bool pressed = messageType == 0x90 && velocity != 0;

            if (channel == padChannel) {
                if (const auto* pad = findPad(number)) {
                    return InputEvent{
                        InputEventType::PadNote,
                        channel,
                        number,
                        velocity,
                        pad->physicalIndex,
                        pressed
                    };
                }
            }

            if (channel == buttonChannel) {
                if (number == jogWheelPressNote) {
                    return InputEvent{
                        InputEventType::JogPress,
                        channel,
                        number,
                        velocity,
                        0xFF,
                        pressed
                    };
                }

                if (findButton(number) != nullptr) {
                    return InputEvent{
                        InputEventType::Button,
                        channel,
                        number,
                        velocity,
                        0xFF,
                        pressed
                    };
                }
            }

            return std::nullopt;
        }

        case 0xA0:
            if (message.size() < 3 || channel != padChannel) {
                return std::nullopt;
            }

            if (const auto* pad = findPad(number)) {
                return InputEvent{
                    InputEventType::PadAftertouch,
                    channel,
                    number,
                    message[2],
                    pad->physicalIndex,
                    false
                };
            }

            return std::nullopt;

        case 0xB0:
            if (message.size() < 3) {
                return std::nullopt;
            }

            if (number == jogWheelCc) {
                return InputEvent{
                    InputEventType::JogWheel,
                    channel,
                    number,
                    message[2]
                };
            }

            if (number == touchStripCc) {
                return InputEvent{
                    InputEventType::TouchStrip,
                    channel,
                    number,
                    message[2]
                };
            }

            return std::nullopt;

        case 0xD0:
            return InputEvent{
                InputEventType::ChannelAftertouch,
                channel,
                0,
                number
            };

        default:
            return std::nullopt;
    }
}

} // namespace mpc::studio
