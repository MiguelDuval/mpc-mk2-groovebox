#pragma once

#include <cstddef>

namespace mpc::audio {

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
