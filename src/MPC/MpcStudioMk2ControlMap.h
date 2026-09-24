#pragma once

#include <array>
#include <cstdint>

namespace mpc::studio {

struct ButtonDefinition {
    const char* name;
    std::uint8_t midiNote;
    std::uint8_t ledCc;
};

struct PadDefinition {
    std::uint8_t physicalIndex;
    std::uint8_t midiNote;
};

inline constexpr std::array<PadDefinition, 16> pads{{
    {0, 37}, {1, 36}, {2, 42}, {3, 82},
    {4, 40}, {5, 38}, {6, 46}, {7, 44},
    {8, 48}, {9, 47}, {10, 45}, {11, 43},
    {12, 49}, {13, 55}, {14, 51}, {15, 53}
}};

inline constexpr std::uint8_t buttonChannel = 0;
inline constexpr std::uint8_t padChannel = 9;
inline constexpr std::uint8_t jogWheelCc = 100;
inline constexpr std::uint8_t jogWheelPressNote = 111;
inline constexpr std::uint8_t touchStripCc = 33;

} // namespace mpc::studio
