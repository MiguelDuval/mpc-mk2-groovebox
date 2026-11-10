#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequenceRecordQueue.h"
#include "MPC/Sequencer/MpcSequenceRecorder.h"

#include <cassert>
#include <cstdint>

namespace {

void testReplaceAndQuantize() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 60;
    sequence.swingPercent = 0;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Replace);
    recorder.begin(state);

    assert(queue.tryEnqueue({
            90, 0, 0, 100, 1}));
    assert(queue.tryEnqueue({
            150, 0, 0, 0, 0}));

    const auto recorded = recorder.drain(state, queue);
    assert(recorded == 1);
    assert(track.patterns.front().notes.size() == 1);

    const auto& note = track.patterns.front().notes.front();
    assert(note.tick == 120);
    assert(note.durationTicks == 60);
    assert(note.note == state.activeDrumProgram().pad(0).midiNote);
    assert(note.velocity == 100);
}

void testOverdubPreservesExistingNotes() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 60;
    sequence.swingPercent = 0;

    track.patterns.front().notes.push_back({
            0, 60, state.activeDrumProgram().pad(1).midiNote, 80, 127, 1});

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    assert(queue.tryEnqueue({
            300, 0, 2, 110, 1}));
    assert(queue.tryEnqueue({
            360, 0, 2, 0, 0}));

    assert(recorder.drain(state, queue) == 1);
    assert(track.patterns.front().notes.size() == 2);
    assert(track.patterns.front().notes[0].note
            == state.activeDrumProgram().pad(1).midiNote);
    assert(track.patterns.front().notes[1].note
            == state.activeDrumProgram().pad(2).midiNote);
}

void testFinishClosesHeldNote() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 120;
    sequence.swingPercent = 0;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Replace);
    recorder.begin(state);

    assert(queue.tryEnqueue({
            480, 0, 3, 90, 1}));

    assert(recorder.finish(state, queue, 600) == 1);
    assert(!recorder.active());
    assert(track.patterns.front().notes.size() == 1);
    assert(track.patterns.front().notes.front().tick == 480);
    assert(track.patterns.front().notes.front().durationTicks == 120);
}

void testRejectsMalformedQueuedEvents() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.timingCorrectEnabled = false;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    // A negative absolute tick is not a valid captured transport position.
    // It must not wrap to the end of the Pattern and become an audible note.
    assert(queue.tryEnqueue({-1, 0, 0, 100, 1}));
    assert(queue.tryEnqueue({120, 0, 0, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());

    // Values above MIDI's 7-bit velocity range would be serialized into a
    // Project which the strict persistence reader later rejects.
    assert(queue.tryEnqueue({120, 0, 1, 255, 1}));
    assert(queue.tryEnqueue({240, 0, 1, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());

    // pressed is a boolean field, not a truthy flag; do not interpret 2 as a
    // second spelling of note-on.
    assert(queue.tryEnqueue({120, 0, 2, 90, 2}));
    assert(queue.tryEnqueue({240, 0, 2, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());
}

void testInvalidReleaseLeavesValidHeldNoteForFinish() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.timingCorrectEnabled = false;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    assert(queue.tryEnqueue({120, 0, 0, 100, 1}));
    assert(queue.tryEnqueue({-1, 0, 0, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());

    // Ignoring the malformed release must not clear the valid held press;
    // finishing at a valid transport tick closes it with the proper duration.
    assert(recorder.finish(state, queue, 240) == 1);
    assert(track.patterns.front().notes.size() == 1);
    assert(track.patterns.front().notes.front().tick == 120);
    assert(track.patterns.front().notes.front().durationTicks == 120);
    assert(track.patterns.front().notes.front().velocity == 100);
}

void testTimingCorrectCanBeDisabled() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 120;
    sequence.swingPercent = 50;
    sequence.timingCorrectEnabled = false;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    assert(queue.tryEnqueue({485, 0, 4, 100, 1}));
    assert(queue.tryEnqueue({545, 0, 4, 0, 0}));

    assert(recorder.drain(state, queue) == 1);
    const auto& note = track.patterns.front().notes.front();
    assert(note.tick == 485);
    assert(note.durationTicks == 60);
}

void testZeroLengthTapUsesGridDuration() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 240;
    sequence.swingPercent = 0;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    assert(queue.tryEnqueue({
            900, 0, 4, 100, 1}));
    assert(queue.tryEnqueue({
            900, 0, 4, 0, 0}));

    assert(recorder.drain(state, queue) == 1);
    assert(track.patterns.front().notes.back().durationTicks == 240);
}

} // namespace

int main() {
    testReplaceAndQuantize();
    testOverdubPreservesExistingNotes();
    testFinishClosesHeldNote();
    testZeroLengthTapUsesGridDuration();
    testTimingCorrectCanBeDisabled();
    testRejectsMalformedQueuedEvents();
    testInvalidReleaseLeavesValidHeldNoteForFinish();
    return 0;
}
