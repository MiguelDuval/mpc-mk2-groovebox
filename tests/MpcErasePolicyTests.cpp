#include "MPC/Sequencer/MpcErasePolicy.h"

#include <array>
#include <cassert>

using mpc::domain::MidiNoteEvent;
using mpc::domain::Pattern;

int main() {
    Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes = {
        MidiNoteEvent{0, 240, 36, 100, 127, 1, 0},
        MidiNoteEvent{960, 240, 36, 110, 127, 1, 0},
        MidiNoteEvent{1920, 240, 38, 120, 127, 1, 60},
        MidiNoteEvent{3780, 240, 36, 90, 127, 1, 0}
    };

    using namespace mpc::sequencer::erase;

    assert(normalizeTick(-60, 3840) == 3780);
    assert(circularDistance(0, 3780, 3840) == 60);
    assert(circularDistance(3780, 0, 3840) == 60);

    const auto first = nearestEventIndex(
        std::span<const MidiNoteEvent>(pattern.notes.data(), pattern.notes.size()),
        36, 80, 3840, 120);
    assert(first.has_value() && *first == 0);

    assert(eraseNearestEvent(pattern, 36, 80, 120));
    assert(pattern.notes.size() == 3);
    assert(pattern.notes[0].tick == 960);

    assert(eraseNearestEvent(pattern, 36, 3785, 100));
    assert(pattern.notes.size() == 2);
    assert(pattern.notes[1].note == 38);

    assert(!eraseNearestEvent(pattern, 36, 3000, 100));

    return 0;
}
