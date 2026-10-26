#pragma once

#include <algorithm>
#include <cmath>
#include <cstddef>

namespace mpc::audio {

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
    position = std::max(0.0, nextPosition);
    return position < playbackEnd;
}

} // namespace mpc::audio
