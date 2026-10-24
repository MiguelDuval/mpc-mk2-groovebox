#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequenceTransport.h"

#include <cassert>
#include <cstdint>
#include <limits>

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


void testLoopNormalizationHandlesMinimumSignedPosition() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    sequence.lengthTicks = 2880;
    sequence.loopEnabled = true;
    sequence.loopStartTicks = 480;
    sequence.loopEndTicks = 2400;

    mpc::sequencer::MpcSequenceTransportClock clock;
    clock.start(sequence, std::numeric_limits<std::int64_t>::min(), 1000000000);

    // The loop-relative position is normalized without subtracting 480 from
    // INT64_MIN, which would overflow signed arithmetic.
    assert(clock.snapshot().positionTicks == 1792);
}

void testInvalidFiniteTempoFallsBackBeforeTransportClockConversion() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    sequence.tempoBpm = std::numeric_limits<double>::max();
    sequence.lengthTicks = 1920;
    sequence.loopEnabled = false;

    mpc::sequencer::MpcSequenceTransportClock clock;
    clock.start(sequence, 1000, 1);

    // Finite is not the same as supported: avoid a floating-to-integer
    // overflow in store(), and keep playback within the defined tempo policy.
    assert(clock.snapshot().tempoMilliBpm == 120000);
    assert(clock.positionAtTimestamp(std::numeric_limits<std::int64_t>::max())
            == 1919);
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
    testLoopNormalizationHandlesMinimumSignedPosition();
    testInvalidFiniteTempoFallsBackBeforeTransportClockConversion();
    testStopFreezesPosition();
    return 0;
}
