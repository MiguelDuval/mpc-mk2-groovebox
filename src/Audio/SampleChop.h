#pragma once

#include "SampleRegion.h"

#include <array>
#include <cstddef>

namespace mpc::audio {

constexpr bool isSupportedChopCount(std::size_t chopCount) noexcept {
    return chopCount == 4 || chopCount == 8 || chopCount == 16;
}

struct SampleChopPlan final {
    std::array<SampleRegion, 16> regions{};
    std::size_t count = 0;

    bool isValid() const noexcept {
        return count > 0;
    }
};

inline SampleChopPlan makeEvenChopPlan(
        std::size_t sampleFrameCount,
        std::size_t chopCount) noexcept {
    SampleChopPlan plan;

    if (!isSupportedChopCount(chopCount)
            || sampleFrameCount < chopCount) {
        return plan;
    }

    // Divide first so boundaries remain correct even near SIZE_MAX. Because
    // chopCount is restricted to 4/8/16, remainder * boundaryIndex is bounded
    // to a small value; quotient * boundaryIndex never exceeds sampleFrameCount.
    const std::size_t quotient = sampleFrameCount / chopCount;
    const std::size_t remainder = sampleFrameCount % chopCount;
    const auto boundary = [quotient, remainder, chopCount](
            std::size_t boundaryIndex) noexcept {
        return quotient * boundaryIndex
                + (remainder * boundaryIndex) / chopCount;
    };

    for (std::size_t index = 0; index < chopCount; ++index) {
        const std::size_t startFrame = boundary(index);
        const std::size_t endFrame = boundary(index + 1);

        if (startFrame >= endFrame) {
            return SampleChopPlan{};
        }

        plan.regions[index] = SampleRegion{startFrame, endFrame};
    }

    plan.count = chopCount;
    return plan;
}

} // namespace mpc::audio
