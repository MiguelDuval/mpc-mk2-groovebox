#include "MPC/Domain/DrumProgram.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/Domain/DrumProgramOps.h"

#include <cassert>
#include <cstddef>
#include <cstdint>

int main() {
    mpc::domain::DrumProgram program;
    program.pads[0].midiNote = 36;
    program.pads[1].midiNote = 42;
    program.pads[2].midiNote = 127;

    const auto first = mpc::domain::findPadIndexByMidiNote(program, 36);
    assert(first.has_value());
    assert(*first == 0);

    const auto middle = mpc::domain::findPadIndexByMidiNote(program, 42);
    assert(middle.has_value());
    assert(*middle == 1);

    const auto highest = mpc::domain::findPadIndexByMidiNote(program, 127);
    assert(highest.has_value());
    assert(*highest == 2);

    assert(!mpc::domain::findPadIndexByMidiNote(program, 99).has_value());

    program.pads[5].midiNote = 42;
    const auto duplicate = mpc::domain::findPadIndexByMidiNote(program, 42);
    assert(duplicate.has_value());
    assert(*duplicate == 1);

    return 0;
}
