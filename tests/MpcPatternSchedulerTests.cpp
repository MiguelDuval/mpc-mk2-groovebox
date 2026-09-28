#include "MPC/Sequencer/MpcPatternScheduler.h"

#include <array>
#include <cassert>

int main() {
    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 1000;
    pattern.notes = {
            {50, 30, 36, 100, 0, 1},
            {100, 120, 37, 110, 127, 1},
            {800, 240, 38, 90, 127, 4}};

    std::array<mpc::sequencer::ScheduledMidiEvent, 4> output{};

    const auto first = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 500, 0},
            1234u,
            output);

    assert(first.written == 1);
    assert(first.eligible == 1);
    assert(!first.truncated);
    assert(output[0].offsetTicks == 100);
    assert(output[0].patternTick == 100);
    assert(output[0].durationTicks == 120);
    assert(output[0].note == 37);
    assert(output[0].velocity == 110);
    assert(output[0].ratchetCount == 1);
    assert(output[0].sourceNoteIndex == 1);

    const auto wrapped = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{700, 200, 1},
            1234u,
            output);

    assert(wrapped.written == 2);
    assert(wrapped.eligible == 2);
    assert(!wrapped.truncated);

    assert(output[0].offsetTicks == 100);
    assert(output[0].patternTick == 800);
    assert(output[0].ratchetCount == 4);
    assert(output[0].sourceNoteIndex == 2);

    assert(output[1].offsetTicks == 400);
    assert(output[1].patternTick == 100);
    assert(output[1].ratchetCount == 1);
    assert(output[1].sourceNoteIndex == 1);

    const auto fullCycle = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 0, 1},
            1234u,
            output);

    assert(fullCycle.written == 2);
    assert(fullCycle.eligible == 2);
    assert(output[0].offsetTicks == 100);
    assert(output[1].offsetTicks == 800);

    std::array<mpc::sequencer::ScheduledMidiEvent, 1> tinyOutput{};
    const auto truncated = mpc::sequencer::schedulePatternWindow(
            pattern,
            mpc::sequencer::TickWindow{0, 999, 0},
            1234u,
            tinyOutput);

    assert(truncated.written == 1);
    assert(truncated.eligible == 2);
    assert(truncated.truncated);

    const auto deterministicAgain =
            mpc::sequencer::schedulePatternWindow(
                    pattern,
                    mpc::sequencer::TickWindow{700, 200, 1},
                    1234u,
                    output);
    assert(deterministicAgain.written == wrapped.written);
    assert(output[0].offsetTicks == 100);
    assert(output[1].offsetTicks == 400);

    assert(mpc::sequencer::schedulePatternWindow(
                   pattern,
                   mpc::sequencer::TickWindow{-1, 10, 0},
                   1234u,
                   output)
                   .written == 0);

    pattern.lengthTicks = 0;
    assert(mpc::sequencer::schedulePatternWindow(
                   pattern,
                   mpc::sequencer::TickWindow{0, 0, 0},
                   1234u,
                   output)
                   .written == 0);

    return 0;
}
