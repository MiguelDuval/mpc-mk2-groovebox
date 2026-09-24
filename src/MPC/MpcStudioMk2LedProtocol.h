#pragma once

#include <array>
#include <cstdint>
#include <span>
#include <vector>

namespace mpc::studio {

struct Rgb {
    std::uint8_t red;
    std::uint8_t green;
    std::uint8_t blue;
};

std::array<std::uint8_t, 12> makePadLedSysEx(
    std::uint8_t pad,
    Rgb rgb);

inline constexpr std::uint16_t lcdWidth = 160;
inline constexpr std::uint16_t lcdHeight = 80;

struct LcdChunk {
    std::uint8_t x;
    std::uint8_t y;
    std::uint8_t width;
    std::uint8_t height;
};

inline constexpr std::array<LcdChunk, 6> lcdChunks{{
    {0, 0, 60, 60},
    {0, 60, 60, 20},
    {60, 0, 60, 60},
    {60, 60, 60, 20},
    {120, 0, 40, 60},
    {120, 60, 40, 20}
}};

std::vector<std::uint8_t> encodeLcdPayload(
    std::span<const std::uint8_t> pngBytes);

std::vector<std::uint8_t> makeLcdChunkSysEx(
    LcdChunk chunk,
    std::span<const std::uint8_t> pngBytes);

} // namespace mpc::studio
