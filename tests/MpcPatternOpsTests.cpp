#include "MPC/Sequencer/MpcPatternOps.h"

#include <cassert>

int main() {
    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes = {
            {110, 120, 42, 100, 127, 1},
            {520, 120, 43, 100, 127, 1},
            {3830, 120, 36, 110, 127, 1}};

    const auto changed = mpc::sequencer::quantizePattern(pattern, 480);

    assert(changed == 3);
    assert(pattern.notes.size() == 3);
    assert(pattern.notes[0].tick == 0);
    assert(pattern.notes[0].note == 36);
    assert(pattern.notes[1].tick == 0);
    assert(pattern.notes[1].note == 42);
    assert(pattern.notes[2].tick == 480);
    assert(pattern.notes[2].note == 43);

    assert(mpc::sequencer::quantizePattern(pattern, 0) == 0);
    assert(mpc::sequencer::quantizePattern(pattern, -120) == 0);

    pattern.lengthTicks = 0;
    assert(mpc::sequencer::quantizePattern(pattern, 120) == 0);

    return 0;
}
