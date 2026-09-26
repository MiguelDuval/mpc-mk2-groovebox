#include "Audio/SampleChop.h"

#include <cassert>

namespace {

void testFourEvenSlices() {
    const auto plan = mpc::audio::makeEvenChopPlan(16, 4);
    assert(plan.isValid());
    assert(plan.count == 4);
    assert(plan.regions[0].startFrame == 0);
    assert(plan.regions[0].endFrame == 4);
    assert(plan.regions[3].startFrame == 12);
    assert(plan.regions[3].endFrame == 16);
}

void testUnevenDistributionRemainsContiguous() {
    const auto plan = mpc::audio::makeEvenChopPlan(10, 4);
    assert(plan.isValid());
    assert(plan.count == 4);

    assert(plan.regions[0].startFrame == 0);
    assert(plan.regions[0].endFrame == 2);
    assert(plan.regions[1].startFrame == 2);
    assert(plan.regions[1].endFrame == 5);
    assert(plan.regions[2].startFrame == 5);
    assert(plan.regions[2].endFrame == 7);
    assert(plan.regions[3].startFrame == 7);
    assert(plan.regions[3].endFrame == 10);
}

void testEightAndSixteenSlices() {
    const auto eight = mpc::audio::makeEvenChopPlan(800, 8);
    const auto sixteen = mpc::audio::makeEvenChopPlan(1600, 16);

    assert(eight.isValid());
    assert(eight.count == 8);
    assert(eight.regions[7].endFrame == 800);

    assert(sixteen.isValid());
    assert(sixteen.count == 16);
    assert(sixteen.regions[15].endFrame == 1600);
}

void testRejectUnsupportedOrTooShortPlans() {
    assert(!mpc::audio::makeEvenChopPlan(100, 3).isValid());
    assert(!mpc::audio::makeEvenChopPlan(3, 4).isValid());
}

} // namespace

int main() {
    testFourEvenSlices();
    testUnevenDistributionRemainsContiguous();
    testEightAndSixteenSlices();
    testRejectUnsupportedOrTooShortPlans();
    return 0;
}
