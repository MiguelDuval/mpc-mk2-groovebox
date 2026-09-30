#include "MPC/Sequencer/MpcNoteRepeatScheduler.h"

#include <cassert>

int main() {
    using Scheduler = mpc::sequencer::MpcNoteRepeatScheduler;

    // 1/16 at 120 BPM = 125 ms.
    assert(Scheduler::intervalNanos(240, 120000) == 125000000);

    // The scheduler accepts arbitrary boundary offsets, so 1 tick is
    // a real timing value rather than being quantized to a larger interval.
    assert(Scheduler::intervalNanos(1, 120000) == 3125000);

    assert(Scheduler::ticksToNextBoundary(0, 0, 3840, 240) == 240);
    assert(Scheduler::ticksToNextBoundary(120, 0, 3840, 240) == 120);
    assert(Scheduler::ticksToNextBoundary(240, 0, 3840, 240) == 240);
    assert(Scheduler::ticksToNextBoundary(3720, 0, 3840, 240) == 120);

    // Loop-relative alignment must work when the loop does not start at zero.
    assert(Scheduler::ticksToNextBoundary(600, 480, 2400, 240) == 120);
    assert(Scheduler::ticksToNextBoundary(480, 480, 2400, 240) == 240);

    return 0;
}
