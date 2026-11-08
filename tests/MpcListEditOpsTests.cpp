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


    // Duplicate/move preserve the complete MIDI event, including per-note pitch
    // offsets and microtiming; transpose is all-or-nothing at MIDI boundaries.
    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes.push_back(MidiNoteEvent{480,360,60,91,87,3,-17,1250});
    assert(mpc::sequencer::list_edit::duplicatePatternEvent(pattern,0,240));
    assert(pattern.notes.size()==2 && pattern.notes[1].tick==720);
    assert(pattern.notes[1].durationTicks==360 && pattern.notes[1].velocity==91);
    assert(pattern.notes[1].probability==87 && pattern.notes[1].ratchet==3);
    assert(pattern.notes[1].nudgeTicks==-17);
    assert(pattern.notes[1].tuningOffsetMilliSemitones==1250);
    assert(mpc::sequencer::list_edit::movePatternEvent(pattern,1,120));
    assert(pattern.notes[1].tick==840);
    assert(!mpc::sequencer::list_edit::movePatternEvent(pattern,1,100000));
    // Event field edits reject ambiguous same-tick MIDI identities.
    mpc::domain::Pattern noteCollision;
    noteCollision.lengthTicks = 3840;
    noteCollision.notes = {
        MidiNoteEvent{480, 120, 60, 100, 127, 1, 0, 0},
        MidiNoteEvent{480, 120, 61, 100, 127, 1, 0, 0}};
    const auto beforeNoteCollision = noteCollision.notes;
    assert(!mpc::sequencer::list_edit::setPatternEventNote(noteCollision, 1, 60));
    assert(noteCollision.notes[1].note == beforeNoteCollision[1].note);

    mpc::domain::Pattern tickCollision;
    tickCollision.lengthTicks = 3840;
    tickCollision.notes = {
        MidiNoteEvent{480, 120, 60, 100, 127, 1, 0, 0},
        MidiNoteEvent{600, 120, 60, 100, 127, 1, 0, 0}};
    const auto beforeTickCollision = tickCollision.notes;
    assert(!mpc::sequencer::list_edit::setPatternEventTick(tickCollision, 1, 480));
    assert(tickCollision.notes[1].tick == beforeTickCollision[1].tick);

    const auto beforeTranspose=pattern.notes;
    assert(mpc::sequencer::list_edit::transposePattern(pattern,12));
    assert(pattern.notes[0].note==72 && pattern.notes[1].note==72);
    assert(pattern.notes[0].nudgeTicks==beforeTranspose[0].nudgeTicks);
    assert(pattern.notes[0].tuningOffsetMilliSemitones==beforeTranspose[0].tuningOffsetMilliSemitones);
    pattern.notes[0].note=120;
    const auto beforeRejected=pattern.notes;
    assert(!mpc::sequencer::list_edit::transposePattern(pattern,12));
    assert(pattern.notes[0].note==beforeRejected[0].note);
    assert(pattern.notes[1].note==beforeRejected[1].note);
    return 0;
}
