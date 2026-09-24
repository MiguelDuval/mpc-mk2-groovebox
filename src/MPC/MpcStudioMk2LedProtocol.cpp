#include "MpcStudioMk2LedProtocol.h"

namespace mpc::studio {

std::array<std::uint8_t, 12> makePadLedSysEx(
    std::uint8_t pad,
    Rgb rgb)
{
    return {
        0xF0, 0x47, 0x47, 0x4A, 0x65, 0x00,
        0x04, pad, rgb.red, rgb.green, rgb.blue, 0xF7
    };
}

} // namespace mpc::studio
