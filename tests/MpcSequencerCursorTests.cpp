#include "MPC/Sequencer/MpcSequencerCursor.h"

#include <cassert>
#include <cstdint>
#include <limits>

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

    mpc::domain::Sequence rangedSequence;
    rangedSequence.lengthTicks = 3840;
    rangedSequence.loopEnabled = true;
    rangedSequence.loopStartTicks = 960;
    rangedSequence.loopEndTicks = 2880;
    mpc::sequencer::MpcSequencerCursor rangedCursor(rangedSequence);
    rangedCursor.setPositionTicks(960);
    rangedCursor.start();

    const auto ranged = rangedCursor.advanceTicks(1920);
    assert(ranged.begin == 960);
    assert(ranged.end == 960);
    assert(ranged.completedCycles == 1);
    assert(rangedCursor.positionTicks() == 960);

    rangedCursor.setPositionTicks(100);
    assert(rangedCursor.positionTicks() == 2020);

    mpc::domain::Sequence nonLoopSequence;
    nonLoopSequence.lengthTicks = 1920;
    nonLoopSequence.loopEnabled = false;
    mpc::sequencer::MpcSequencerCursor nonLoopCursor(nonLoopSequence);
    nonLoopCursor.start();
    const auto finalWindow = nonLoopCursor.advanceTicks(2400);
    assert(finalWindow.begin == 0);
    assert(finalWindow.end == 1919);
    assert(!nonLoopCursor.isPlaying());
    assert(nonLoopCursor.positionTicks() == 1919);

    sequence.lengthTicks = 0;
    mpc::sequencer::MpcSequencerCursor minimumLengthCursor(sequence);
    assert(minimumLengthCursor.sequenceLengthTicks() == 1);

    mpc::domain::Sequence hugeDeltaSequence;
    hugeDeltaSequence.lengthTicks = 3840;
    mpc::sequencer::MpcSequencerCursor hugeDeltaCursor(hugeDeltaSequence);
    hugeDeltaCursor.start();
    hugeDeltaCursor.setPositionTicks(3839);

    constexpr std::int64_t hugeDelta = std::numeric_limits<std::int64_t>::max();
    const auto hugeWindow = hugeDeltaCursor.advanceTicks(hugeDelta);

    const auto length = static_cast<std::int64_t>(
            hugeDeltaSequence.lengthTicks);
    const auto fullCycles = static_cast<std::uint64_t>(hugeDelta / length);
    const auto remainder = hugeDelta % length;
    const auto advanced = static_cast<std::int64_t>(3839) + remainder;

    assert(hugeWindow.begin == 3839);
    assert(hugeWindow.completedCycles ==
           fullCycles + static_cast<std::uint64_t>(advanced >= length));
    assert(hugeWindow.end ==
           (advanced >= length ? advanced - length : advanced));
    assert(hugeDeltaCursor.positionTicks() == hugeWindow.end);

    return 0;
}
