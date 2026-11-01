#include "MPC/Sequencer/MpcSequencerRuntime.h"

#include <array>
#include <cassert>

int main() {
    mpc::domain::Sequence sequence;
    sequence.lengthTicks = 1000;

    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 1000;
    pattern.notes = {
            {100, 60, 36, 100, 127, 1},
            {800, 60, 37, 110, 127, 1}};

    mpc::sequencer::MpcSequencerRuntime runtime(sequence, pattern);
    std::array<mpc::sequencer::ScheduledMidiEvent, 4> output{};

    const auto stopped = runtime.advance(200, 1234u, output);
    assert(stopped.written == 0);
    assert(runtime.positionTicks() == 0);

    runtime.start();

    const auto first = runtime.advance(200, 1234u, output);
    assert(first.written == 1);
    assert(first.eligible == 1);
    assert(!first.truncated);
    assert(output[0].offsetTicks == 100);
    assert(output[0].patternTick == 100);
    assert(output[0].note == 36);

    const auto wrapped = runtime.advance(800, 1234u, output);
    assert(wrapped.written == 1);
    assert(wrapped.eligible == 1);
    assert(output[0].offsetTicks == 600);
    assert(output[0].patternTick == 800);
    assert(output[0].note == 37);
    assert(runtime.positionTicks() == 0);

    runtime.stop();
    const auto stoppedAgain = runtime.advance(200, 1234u, output);
    assert(stoppedAgain.written == 0);
    assert(runtime.positionTicks() == 0);

    runtime.reset();
    assert(!runtime.isPlaying());
    assert(runtime.positionTicks() == 0);

    return 0;
}
