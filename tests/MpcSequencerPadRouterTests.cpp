#include "MPC/Sequencer/MpcSequencerPadRouter.h"

#include <array>
#include <cassert>

int main() {
    mpc::domain::DrumProgram program;
    program.pads[0].midiNote = 36;
    program.pads[1].midiNote = 42;

    const std::array<mpc::sequencer::ScheduledMidiEvent, 3> input{{
            {10, 100, 36, 90, 1, 0},
            {20, 100, 99, 80, 2, 1},
            {30, 100, 42, 110, 1, 2}}};

    std::array<mpc::sequencer::ScheduledPadEvent, 2> output{};

    const auto result =
            mpc::sequencer::routeScheduledEventsToPads(
                    program, input, output);

    assert(result.written == 2);
    assert(result.routed == 2);
    assert(result.unmapped == 1);
    assert(!result.truncated);

    assert(output[0].offsetTicks == 10);
    assert(output[0].padIndex == 0);
    assert(output[0].velocity == 90);
    assert(output[0].sourceNoteIndex == 0);

    assert(output[1].offsetTicks == 30);
    assert(output[1].padIndex == 1);
    assert(output[1].velocity == 110);
    assert(output[1].sourceNoteIndex == 2);

    std::array<mpc::sequencer::ScheduledPadEvent, 1> tinyOutput{};
    const auto truncated =
            mpc::sequencer::routeScheduledEventsToPads(
                    program, input, tinyOutput);

    assert(truncated.written == 1);
    assert(truncated.routed == 1);
    assert(truncated.unmapped == 1);
    assert(truncated.truncated);

    return 0;
}
