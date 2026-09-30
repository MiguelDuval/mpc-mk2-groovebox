#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequenceTransport.h"

#include <cassert>
#include <cstdint>

namespace {

void testLoopedPosition() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    sequence.tempoBpm = 120.0;
    sequence.loopEnabled = true;
    sequence.loopStartTicks = 960;
    sequence.loopEndTicks = 2880;

    mpc::sequencer::MpcSequenceTransportClock clock;
    clock.start(sequence, 960, 1000000000);

    assert(clock.positionAtTimestamp(1000000000) == 960);

    // 120 BPM at 960 PPQN = 1920 ticks/second.
    assert(clock.positionAtTimestamp(1500000000) == 1920);
    assert(clock.positionAtTimestamp(2000000000) == 960);
}

void testNonLoopPositionDoesNotWrap() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    sequence.tempoBpm = 120.0;
    sequence.lengthTicks = 1920;
    sequence.loopEnabled = false;

    mpc::sequencer::MpcSequenceTransportClock clock;
    clock.start(sequence, 0, 1000000000);

    assert(clock.positionAtTimestamp(1500000000) == 960);
    assert(clock.positionAtTimestamp(3000000000) == 1919);
}

void testStopFreezesPosition() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    sequence.tempoBpm = 120.0;

    mpc::sequencer::MpcSequenceTransportClock clock;
    clock.start(sequence, 0, 1000000000);
    clock.stop(sequence, 480, 1500000000);

    assert(clock.positionAtTimestamp(2000000000) == 480);
    assert(!clock.snapshot().playing);

    clock.queueSequence(2);
    assert(clock.snapshot().queuedSequenceIndex == 2);
    clock.clearQueuedSequence();
    assert(clock.snapshot().queuedSequenceIndex == -1);
}

} // namespace

int main() {
    testLoopedPosition();
    testNonLoopPositionDoesNotWrap();
    testStopFreezesPosition();
    return 0;
}
