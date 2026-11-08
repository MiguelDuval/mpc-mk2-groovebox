#include "MPC/Sequencer/MpcListEditOps.h"

#include <cassert>
#include <cstdint>
#include <vector>

int main() {
    using mpc::domain::MidiNoteEvent;
    using mpc::sequencer::list_edit::PadNoteEventInsertResult;
    using mpc::sequencer::list_edit::appendPadNoteEvent;

    std::vector<MidiNoteEvent> notes{
            {240, 120, 42, 81, 127, 1, 0, 15000},
            {0, 120, 36, 100, 127, 1, 0, 0}};

    assert(appendPadNoteEvent(notes, 3840, 38, 120, 105, 480)
            == PadNoteEventInsertResult::Added);
    assert(notes.size() == 3);
    assert(notes[0].tick == 0 && notes[0].note == 36);
    assert(notes[1].tick == 120);
    assert(notes[1].note == 38);
    assert(notes[1].velocity == 105);
    assert(notes[1].durationTicks == 480);
    assert(notes[1].probability == 127);
    assert(notes[1].ratchet == 1);
    assert(notes[1].nudgeTicks == 0);
    assert(notes[1].tuningOffsetMilliSemitones == 0);
    assert(notes[2].tick == 240);
    assert(notes[2].tuningOffsetMilliSemitones == 15000);

    // The Drum Program owns the non-sequential Pad → MIDI-note map.
    mpc::domain::DrumProgram program;
    program.type = mpc::domain::ProgramType::Drum;
    program.pad(5).midiNote = 53;
    const auto beforeMappedInsert = notes.size();
    assert(appendPadNoteEvent(
            notes, 3840, program, 5, 360, 99, 240)
            == PadNoteEventInsertResult::Added);
    assert(notes.size() == beforeMappedInsert + 1);
    const auto mappedEvent = std::find_if(
            notes.begin(), notes.end(),
            [](const MidiNoteEvent& event) {
                return event.tick == 360 && event.note == 53;
            });
    assert(mappedEvent != notes.end());
    assert(mappedEvent->velocity == 99);
    assert(appendPadNoteEvent(
            notes, 3840, program, 16, 480, 90, 120)
            == PadNoteEventInsertResult::InvalidPad);
    program.type = mpc::domain::ProgramType::Plugin;
    assert(appendPadNoteEvent(
            notes, 3840, program, 5, 480, 90, 120)
            == PadNoteEventInsertResult::InvalidPad);

    program.type = mpc::domain::ProgramType::Drum;
    program.pad(5).midiNote = 200;
    assert(appendPadNoteEvent(
            notes, 3840, program, 5, 480, 90, 120)
            == PadNoteEventInsertResult::InvalidMidiNote);
    assert(notes.size() == beforeMappedInsert + 1);

    const auto beforeCollision = notes;
    assert(appendPadNoteEvent(notes, 3840, 38, 120, 90, 240)
            == PadNoteEventInsertResult::SamePitchCollision);
    assert(notes.size() == beforeCollision.size());
    assert(notes[1].velocity == beforeCollision[1].velocity);

    assert(appendPadNoteEvent(notes, 0, 38, 0, 100, 120)
            == PadNoteEventInsertResult::InvalidPattern);
    assert(appendPadNoteEvent(notes, 3840, 38, -1, 100, 120)
            == PadNoteEventInsertResult::InvalidTick);
    assert(appendPadNoteEvent(notes, 3840, 38, 3840, 100, 120)
            == PadNoteEventInsertResult::InvalidTick);
    assert(appendPadNoteEvent(notes, 3840, 38, 0, 0, 120)
            == PadNoteEventInsertResult::InvalidVelocity);
    assert(appendPadNoteEvent(notes, 3840, 38, 0, 128, 120)
            == PadNoteEventInsertResult::InvalidVelocity);
    assert(appendPadNoteEvent(notes, 3840, 38, 0, 100, -1)
            == PadNoteEventInsertResult::InvalidDuration);
    assert(appendPadNoteEvent(notes, 3840, 38, 0, 100, 3841)
            == PadNoteEventInsertResult::InvalidDuration);
    assert(notes.size() == beforeCollision.size());

    return 0;
}
