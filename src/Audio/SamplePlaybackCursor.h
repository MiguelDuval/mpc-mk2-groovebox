#pragma once

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

    position += static_cast<double>(positionStep);
    return position < static_cast<double>(regionEndFrame)
            && position < static_cast<double>(sampleFrameCount);
}

} // namespace mpc::audio
