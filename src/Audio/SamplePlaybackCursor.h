#pragma once

#include <algorithm>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <limits>

namespace mpc::audio {

/*
 * Audio callback -> UI telemetry encoding. Zero means idle; positive values
 * store frame + 1 so sample frame zero remains observable. Oversized indices
 * are rejected rather than wrapping into a misleading cursor position.
 */
inline std::uint32_t encodeSamplePlaybackCursorFrame(
        std::size_t frame) noexcept {
    constexpr auto maximum =
            static_cast<std::size_t>(
                    std::numeric_limits<std::uint32_t>::max());
    if (frame >= maximum) {
        return 0u;
    }
    return static_cast<std::uint32_t>(frame + 1u);
}

inline std::int64_t decodeSamplePlaybackCursorFrame(
        std::uint32_t encoded) noexcept {
    return encoded == 0u
            ? -1
            : static_cast<std::int64_t>(encoded - 1u);
}


inline bool advanceSamplePlaybackCursor(
        double& position,
        float positionStep,
        std::size_t regionEndFrame,
        std::size_t sampleFrameCount) noexcept {
    // A non-finite input can poison the playback cursor permanently and
    // make subsequent frame-to-index conversions unsafe. Leave state intact
    // when the caller supplies an invalid cursor or step.
    if (!std::isfinite(position) || !std::isfinite(positionStep)) {
        return false;
    }

    const double playbackEnd = std::min(
            static_cast<double>(regionEndFrame),
            static_cast<double>(sampleFrameCount));
    // Treat the first frame at or beyond either half-open boundary as end of
    // playback. Clamp overshoots so a large but finite step cannot leave the
    // persistent cursor at a huge out-of-range value.
    const double nextPosition = position + static_cast<double>(positionStep);
    if (!std::isfinite(nextPosition) || nextPosition >= playbackEnd) {
        position = playbackEnd;
        return false;
    }
    if (nextPosition < 0.0) {
        position = 0.0;
        return false;
    }
    position = nextPosition;
    return true;
}

} // namespace mpc::audio
