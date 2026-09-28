#include "MPC/Sequencer/MpcPatternOps.h"

#include <array>
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
    assert(pattern.notes[0].note == 42);
    assert(pattern.notes[1].tick == 0);
    assert(pattern.notes[1].note == 36);
    assert(pattern.notes[2].tick == 480);
    assert(pattern.notes[2].note == 43);

    assert(mpc::sequencer::quantizePattern(pattern, 0) == 0);
    assert(mpc::sequencer::quantizePattern(pattern, -120) == 0);

    std::array<mpc::domain::MidiNoteEvent, 4> captured{{
            {-120, 60, 50, 90, 127, 1},
            {481, 120, 51, 100, 127, 2},
            {960, -1, 52, 100, 127, 1},
            {1440, 120, 53, 100, 127, 0}}};

    pattern.notes = {{720, 120, 40, 100, 127, 1}};
    const auto recorded = mpc::sequencer::recordNotes(
            pattern,
            captured,
            mpc::sequencer::PatternRecordMode::Replace);

    assert(recorded == 2);
    assert(pattern.notes.size() == 2);
    assert(pattern.notes[0].tick == 3720);
    assert(pattern.notes[0].note == 50);
    assert(pattern.notes[1].tick == 481);
    assert(pattern.notes[1].note == 51);

    std::array<mpc::domain::MidiNoteEvent, 1> overdub{{
            {240, 120, 54, 110, 127, 1}}};

    const auto overdubbed = mpc::sequencer::recordNotes(
            pattern,
            overdub,
            mpc::sequencer::PatternRecordMode::Overdub);

    assert(overdubbed == 1);
    assert(pattern.notes.size() == 3);
    assert(pattern.notes[0].tick == 240);
    assert(pattern.notes[1].tick == 481);
    assert(pattern.notes[2].tick == 3720);

    pattern.lengthTicks = 0;
    assert(mpc::sequencer::recordNotes(
                   pattern,
                   overdub,
                   mpc::sequencer::PatternRecordMode::Overdub) == 0);

    return 0;
}
