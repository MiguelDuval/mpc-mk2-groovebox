#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <limits>

namespace mpc::audio {

struct SampleLoopRange final {
    std::int64_t startFrame = 0;
    std::int64_t endFrame = 0;

    [[nodiscard]] constexpr bool isDisabled() const noexcept {
        return startFrame == 0 && endFrame == 0;
    }
};

[[nodiscard]] inline bool sampleLoopFrameFitsInt64(std::size_t frame) noexcept {
    return static_cast<std::uintmax_t>(frame)
            <= static_cast<std::uintmax_t>(std::numeric_limits<std::int64_t>::max());
}

// Loop metadata uses the source-frame coordinate system of its SampleRegion.
// Return [0,0) for disabled, malformed, unrepresentable, or non-intersecting
// loops so edits cannot leave a project in an unsaveable state.
[[nodiscard]] inline SampleLoopRange clipSampleLoopToRegion(
        std::int64_t loopStartSample,
        std::int64_t loopEndSample,
        std::size_t regionStartFrame,
        std::size_t regionEndFrame) noexcept {
    if (loopStartSample == 0 && loopEndSample == 0) {
        return {};
    }
    if (loopStartSample < 0
            || loopEndSample <= loopStartSample
            || regionEndFrame <= regionStartFrame
            || !sampleLoopFrameFitsInt64(regionStartFrame)
            || !sampleLoopFrameFitsInt64(regionEndFrame)) {
        return {};
    }

    const auto regionStart = static_cast<std::int64_t>(regionStartFrame);
    const auto regionEnd = static_cast<std::int64_t>(regionEndFrame);
    const auto clippedStart = std::max(loopStartSample, regionStart);
    const auto clippedEnd = std::min(loopEndSample, regionEnd);
    if (clippedEnd <= clippedStart) {
        return {};
    }
    return {clippedStart, clippedEnd};
}

// Cropping changes the frame origin. Intersect the source loop with the crop,
// then rebase it so frame zero is the first frame of the cropped buffer.
[[nodiscard]] inline SampleLoopRange rebaseSampleLoopForCrop(
        std::int64_t loopStartSample,
        std::int64_t loopEndSample,
        std::size_t cropStartFrame,
        std::size_t cropEndFrame) noexcept {
    const auto clipped = clipSampleLoopToRegion(
            loopStartSample, loopEndSample, cropStartFrame, cropEndFrame);
    if (clipped.isDisabled() || !sampleLoopFrameFitsInt64(cropStartFrame)) {
        return {};
    }
    const auto cropStart = static_cast<std::int64_t>(cropStartFrame);
    return {clipped.startFrame - cropStart, clipped.endFrame - cropStart};
}

} // namespace mpc::audio
