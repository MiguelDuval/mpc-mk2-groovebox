#include "Audio/SampleLoopRegionPolicy.h"

#include <cassert>
#include <cstddef>
#include <cstdint>
#include <limits>

int main() {
    using mpc::audio::clipSampleLoopToRegion;
    using mpc::audio::rebaseSampleLoopForCrop;

    assert(clipSampleLoopToRegion(0, 0, 0, 100).isDisabled());

    const auto preserved = clipSampleLoopToRegion(120, 360, 100, 400);
    assert(preserved.startFrame == 120);
    assert(preserved.endFrame == 360);

    const auto clippedAtBothEnds = clipSampleLoopToRegion(20, 500, 100, 400);
    assert(clippedAtBothEnds.startFrame == 100);
    assert(clippedAtBothEnds.endFrame == 400);

    const auto clippedStart = clipSampleLoopToRegion(20, 250, 100, 400);
    assert(clippedStart.startFrame == 100);
    assert(clippedStart.endFrame == 250);

    const auto clippedEnd = clipSampleLoopToRegion(250, 500, 100, 400);
    assert(clippedEnd.startFrame == 250);
    assert(clippedEnd.endFrame == 400);

    // Half-open ranges that merely touch do not overlap.
    assert(clipSampleLoopToRegion(20, 100, 100, 400).isDisabled());
    assert(clipSampleLoopToRegion(400, 500, 100, 400).isDisabled());

    // Corrupt loop values and invalid regions are made safe rather than
    // propagating metadata that persistence would reject.
    assert(clipSampleLoopToRegion(-1, 50, 0, 100).isDisabled());
    assert(clipSampleLoopToRegion(20, 20, 0, 100).isDisabled());
    assert(clipSampleLoopToRegion(20, 10, 0, 100).isDisabled());
    assert(clipSampleLoopToRegion(20, 80, 10, 10).isDisabled());

    // Cropping preserves loop overlap and changes its frame origin.
    const auto rebased = rebaseSampleLoopForCrop(600, 1400, 500, 1500);
    assert(rebased.startFrame == 100);
    assert(rebased.endFrame == 900);

    const auto rebasedClipped = rebaseSampleLoopForCrop(100, 800, 300, 700);
    assert(rebasedClipped.startFrame == 0);
    assert(rebasedClipped.endFrame == 400);

    assert(rebaseSampleLoopForCrop(100, 200, 200, 400).isDisabled());
    assert(rebaseSampleLoopForCrop(0, 0, 100, 200).isDisabled());

    // size_t may represent coordinates outside the signed loop-frame domain.
    if (static_cast<std::uintmax_t>(std::numeric_limits<std::size_t>::max())
            > static_cast<std::uintmax_t>(std::numeric_limits<std::int64_t>::max())) {
        const auto huge = std::numeric_limits<std::size_t>::max();
        assert(clipSampleLoopToRegion(1, 100, huge - 1, huge).isDisabled());
        assert(rebaseSampleLoopForCrop(1, 100, huge - 1, huge).isDisabled());
    }

    return 0;
}
