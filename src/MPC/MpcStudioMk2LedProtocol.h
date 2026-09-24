#pragma once

#include <array>
#include <cstdint>

namespace mpc::studio {

struct Rgb {
    std::uint8_t red;
    std::uint8_t green;
    std::uint8_t blue;
};

std::array<std::uint8_t, 12> makePadLedSysEx(
    std::uint8_t pad,
    Rgb rgb);

} // namespace mpc::studio
