#include "MPC/Sequencer/MpcNoteRepeatTiming.h"

#include <cassert>

int main() {
    using namespace mpc::sequencer::note_repeat_timing;

    // 1/16 at 120 BPM = 125 ms.
    assert(intervalNanos(240, 120000) == 125000000);

    // The timing primitive also handles a one-tick boundary exactly.
    assert(intervalNanos(1, 120000) == 520833);

    assert(ticksToNextBoundary(0, 0, 3840, 240) == 240);
    assert(ticksToNextBoundary(120, 0, 3840, 240) == 120);
    assert(ticksToNextBoundary(240, 0, 3840, 240) == 240);
    assert(ticksToNextBoundary(3720, 0, 3840, 240) == 120);

    // Loop-relative alignment must work when the loop does not start at zero.
    assert(ticksToNextBoundary(600, 480, 2400, 240) == 120);
    assert(ticksToNextBoundary(480, 480, 2400, 240) == 240);

    // Repeat timing also supports MPC-style triplet divisions.
    assert(repeatRateTicksForIndex(0) == 960);
    assert(repeatRateTicksForIndex(2) == 240);
    assert(repeatRateTicksForIndex(5) == 640);
    assert(repeatRateTicksForIndex(7) == 160);
    assert(repeatRateIndexForTicks(240) == 2);
    assert(repeatRateIndexForTicks(640) == 5);
    assert(repeatRateIndexForTouch(0) == 0);
    assert(repeatRateIndexForTouch(31) == 1);
    assert(repeatRateIndexForTouch(32) == 2);
    assert(repeatRateIndexForTouch(127) == 7);

    // Keep the scheduler bounds wide enough for the fastest supported triplet.
    assert(clampGrid(1) == 40);
    assert(clampGrid(40) == 40);
    assert(clampGrid(960) == 960);
    assert(clampGrid(5000) == 960);

    return 0;
}
