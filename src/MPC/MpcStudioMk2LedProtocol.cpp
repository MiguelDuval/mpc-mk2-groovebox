#include "MpcStudioMk2LedProtocol.h"

#include <algorithm>
#include <tuple>

namespace {

std::pair<std::uint8_t, std::uint8_t> splitU16(std::size_t value) {
    const auto v = static_cast<std::uint16_t>(value & 0xFFFFu);
    return {
        static_cast<std::uint8_t>((v >> 8) & 0xFFu),
        static_cast<std::uint8_t>(v & 0xFFu)
    };
}

std::int64_t lcdMagicNumber(std::size_t encodedLength) {
    return static_cast<std::int64_t>((encodedLength / 128u) * 128u) - 8;
}

} // namespace

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

std::vector<std::uint8_t> encodeLcdPayload(
    std::span<const std::uint8_t> pngBytes)
{
    // The MPC Studio MkII LCD transport packs each group of 7 source bytes
    // behind one control byte. Each control bit records whether the
    // corresponding source byte had its high bit set.
    std::vector<std::uint8_t> encoded;
    encoded.reserve(pngBytes.size() + (pngBytes.size() + 6u) / 7u);

    for (std::size_t offset = 0; offset < pngBytes.size(); offset += 7u) {
        const auto count = std::min<std::size_t>(7u, pngBytes.size() - offset);
        std::uint8_t control = 0;

        encoded.push_back(0);

        for (std::size_t i = 0; i < count; ++i) {
            const auto source = pngBytes[offset + i];
            auto value = static_cast<std::uint8_t>(source & 0x7Fu);
            if ((source & 0x80u) != 0u) {
                control = static_cast<std::uint8_t>(
                    control | static_cast<std::uint8_t>(1u << i));
            }
            encoded.push_back(value);
        }

        encoded[encoded.size() - count - 1u] = control;
    }

    return encoded;
}

std::vector<std::uint8_t> makeLcdChunkSysEx(
    LcdChunk chunk,
    std::span<const std::uint8_t> pngBytes)
{
    const auto encoded = encodeLcdPayload(pngBytes);

    std::vector<std::uint8_t> message;
    message.reserve(16u + encoded.size());

    message.push_back(0xF0);
    message.push_back(0x47);
    message.push_back(0x7F);
    message.push_back(0x4A);
    message.push_back(0x04);

    const auto totalSize = static_cast<std::int64_t>(encoded.size())
        + 16 + lcdMagicNumber(encoded.size());
    const auto [sizeHigh, sizeLow] =
        splitU16(static_cast<std::size_t>(totalSize));
    message.push_back(sizeHigh);
    message.push_back(sizeLow);

    auto [pngSizeHigh, pngSizeLow] = splitU16(pngBytes.size());
    if (pngSizeLow >= 128u) {
        message.push_back(0x20);
        message.push_back(0x20);
        std::tie(pngSizeHigh, pngSizeLow) =
            splitU16(pngBytes.size() - 128u);
    } else {
        message.push_back(0x00);
        message.push_back(0x20);
    }

    message.push_back(chunk.x);
    message.push_back(0x00);
    message.push_back(chunk.y);
    message.push_back(0x00);

    message.push_back(pngSizeHigh);
    message.push_back(pngSizeLow);

    message.insert(message.end(), encoded.begin(), encoded.end());
    message.push_back(0xF7);

    return message;
}

} // namespace mpc::studio
