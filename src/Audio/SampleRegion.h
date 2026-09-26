#pragma once

#include <cstddef>

namespace mpc::audio {

// Non-destructive sample playback region.
// endFrame is exclusive, so a full region is [0, sampleFrameCount).
struct SampleRegion final {
    std::size_t startFrame = 0;
    std::size_t endFrame = 0;

    bool isValidFor(std::size_t sampleFrameCount) const noexcept {
        return startFrame < endFrame && endFrame <= sampleFrameCount;
    }

    std::size_t frameCount() const noexcept {
        return endFrame > startFrame ? endFrame - startFrame : 0;
    }
};

inline SampleRegion fullSampleRegion(std::size_t sampleFrameCount) noexcept {
    return SampleRegion{0, sampleFrameCount};
}

} // namespace mpc::audio
