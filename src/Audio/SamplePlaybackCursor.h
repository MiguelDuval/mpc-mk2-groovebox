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
    // A sampler cursor only advances forward through a non-empty region.
    // Reject malformed state/coefficient inputs before converting the cursor
    // to a frame index on the audio callback's next iteration.
    if (!std::isfinite(position)
            || position < 0.0
            || !std::isfinite(positionStep)
            || positionStep <= 0.0f
            || regionEndFrame == 0
            || sampleFrameCount == 0) {
        return false;
    }

    const auto limit = static_cast<double>(
            std::min(regionEndFrame, sampleFrameCount));
    if (position >= limit) {
        return false;
    }

    const auto nextPosition = position + static_cast<double>(positionStep);
    // A positive step can still fail to advance when rounded away at extreme
    // magnitudes. Stop that voice instead of pinning it to one sample forever.
    if (!std::isfinite(nextPosition) || nextPosition <= position) {
        return false;
    }

    position = nextPosition;
    return position < limit;
}

} // namespace mpc::audio
