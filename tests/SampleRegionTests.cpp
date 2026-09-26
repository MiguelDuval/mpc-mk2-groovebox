#include "Audio/SampleRegion.h"

#include <cassert>

namespace {

void testFullRegion() {
    const auto region = mpc::audio::fullSampleRegion(128);
    assert(region.startFrame == 0);
    assert(region.endFrame == 128);
    assert(region.frameCount() == 128);
    assert(region.isValidFor(128));
}

void testPartialRegion() {
    const mpc::audio::SampleRegion region{16, 48};
    assert(region.frameCount() == 32);
    assert(region.isValidFor(48));
    assert(!region.isValidFor(47));
}

void testInvalidRegions() {
    const mpc::audio::SampleRegion empty{0, 0};
    const mpc::audio::SampleRegion reversed{8, 4};
    const mpc::audio::SampleRegion outOfBounds{2, 11};

    assert(!empty.isValidFor(10));
    assert(!reversed.isValidFor(10));
    assert(!outOfBounds.isValidFor(10));
}

} // namespace

int main() {
    testFullRegion();
    testPartialRegion();
    testInvalidRegions();
    return 0;
}
