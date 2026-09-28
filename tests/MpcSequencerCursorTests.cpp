#include "MPC/Sequencer/MpcSequencerCursor.h"

#include <cassert>

int main() {
    mpc::domain::Sequence sequence;
    sequence.lengthTicks = 3840;

    mpc::sequencer::MpcSequencerCursor cursor(sequence);

    assert(!cursor.isPlaying());
    assert(cursor.positionTicks() == 0);
    assert(cursor.sequenceLengthTicks() == 3840);

    auto stopped = cursor.advanceTicks(960);
    assert(stopped.begin == 0);
    assert(stopped.end == 0);
    assert(stopped.completedCycles == 0);
    assert(cursor.positionTicks() == 0);

    cursor.start();

    auto first = cursor.advanceTicks(960);
    assert(first.begin == 0);
    assert(first.end == 960);
    assert(first.completedCycles == 0);
    assert(cursor.positionTicks() == 960);

    auto wrap = cursor.advanceTicks(3000);
    assert(wrap.begin == 960);
    assert(wrap.end == 120);
    assert(wrap.completedCycles == 1);
    assert(cursor.positionTicks() == 120);

    cursor.setPositionTicks(-1);
    assert(cursor.positionTicks() == 3839);

    cursor.setPositionTicks(7681);
    assert(cursor.positionTicks() == 1);

    cursor.stop();
    cursor.setPositionTicks(100);
    auto stoppedAgain = cursor.advanceTicks(1000);
    assert(stoppedAgain.end == 100);
    assert(cursor.positionTicks() == 100);

    sequence.lengthTicks = 0;
    mpc::sequencer::MpcSequencerCursor minimumLengthCursor(sequence);
    assert(minimumLengthCursor.sequenceLengthTicks() == 1);

    return 0;
}
