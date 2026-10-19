#pragma once

#include <cstddef>
#include <cstdint>
#include <limits>

namespace mpc::audio {

// Callback-safe telemetry encoding: zero means inactive and frame N is N + 1.
// uint32_t keeps the read-only cursor snapshot lock-free on supported Android targets.
inline std::uint32_t encodeSamplePlaybackCursorFrame(std::size_t frame) noexcept {
    constexpr auto kInactive = std::numeric_limits<std::uint32_t>::max();
    if (frame >= static_cast<std::size_t>(kInactive)) return 0u;
    return static_cast<std::uint32_t>(frame + 1u);
}

inline std::int64_t decodeSamplePlaybackCursorFrame(std::uint32_t encoded) noexcept {
    return encoded == 0u ? -1 : static_cast<std::int64_t>(encoded - 1u);
}

inline bool advanceSamplePlaybackCursor(
        double& position,
        float positionStep,
        std::size_t regionEndFrame,
        std::size_t sampleFrameCount) noexcept {
    position += static_cast<double>(positionStep);
    return position < static_cast<double>(regionEndFrame)
            && position < static_cast<double>(sampleFrameCount);
}

} // namespace mpc::audio
