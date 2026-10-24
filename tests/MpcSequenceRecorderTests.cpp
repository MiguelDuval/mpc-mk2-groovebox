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

void testSequenceSwitchAbortsRecordingWithoutCrossSequenceNotes() {
    mpc::MpcProjectState state;
    state.activeSequence().tracks[0].recordArmed = true;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Replace);
    recorder.begin(state);

    // Hold a press in Sequence 01; its release must never be replayed against
    // Sequence 02 after track index 0 acquires a different musical meaning.
    assert(queue.tryEnqueue({100, 0, 0, 100, 1}));
    assert(recorder.drain(state, queue) == 0);
    assert(recorder.active());

    assert(state.addSequence("Sequence 02"));
    auto& secondSequence = state.activeSequence();
    assert(secondSequence.id != "sequence-01");
    secondSequence.tracks[0].recordArmed = true;

    assert(queue.tryEnqueue({220, 0, 0, 0, 0}));
    assert(queue.tryEnqueue({240, 0, 0, 110, 1}));
    assert(recorder.drain(state, queue) == 0);
    assert(!recorder.active());
    assert(secondSequence.tracks[0].patterns.front().notes.empty());

    mpc::sequencer::SequenceRecordEvent stale{};
    assert(!queue.tryDequeue(stale));
    assert(recorder.finish(state, queue, 480) == 0);
    assert(secondSequence.tracks[0].patterns.front().notes.empty());
}

void testProjectReplacementAbortsEvenWhenStableIdsMatch() {
    mpc::MpcProjectState state;
    state.activeSequence().tracks[0].recordArmed = true;

    const auto originalProjectId = state.project().id;
    const auto originalSequenceId = state.activeSequence().id;
    const auto originalGeneration = state.projectGeneration();

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    assert(queue.tryEnqueue({100, 0, 0, 100, 1}));
    assert(recorder.drain(state, queue) == 0);
    assert(recorder.active());

    // Simulate opening a document that legitimately reuses the same persistent
    // Project/Sequence IDs. IDs alone cannot identify the active recording
    // session because its in-memory Project has been replaced.
    assert(state.replaceProject(state.project()));
    assert(state.project().id == originalProjectId);
    assert(state.activeSequence().id == originalSequenceId);
    assert(state.projectGeneration() != originalGeneration);
    assert(state.activeSequence().tracks[0].recordArmed);

    assert(queue.tryEnqueue({220, 0, 0, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(!recorder.active());
    assert(state.activeSequence().tracks[0].patterns.front().notes.empty());

    mpc::sequencer::SequenceRecordEvent stale{};
    assert(!queue.tryDequeue(stale));
    assert(recorder.finish(state, queue, 480) == 0);
    assert(state.activeSequence().tracks[0].patterns.front().notes.empty());
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

void testRejectsMalformedQueuedMidiValues() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.quantizeGridTicks = 120;
    sequence.swingPercent = 0;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    // Invalid velocity must not create a note that later fails project
    // persistence. A non-boolean pressed byte must not arm a held note.
    assert(queue.tryEnqueue({100, 0, 0, 255, 1}));
    assert(queue.tryEnqueue({200, 0, 0, 0, 0}));
    assert(queue.tryEnqueue({300, 0, 1, 100, 2}));
    assert(queue.tryEnqueue({420, 0, 1, 0, 0}));

    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());
    assert(recorder.finish(state, queue, 600) == 0);
    assert(track.patterns.front().notes.empty());
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
    testRejectsMalformedQueuedMidiValues();
    testSequenceSwitchAbortsRecordingWithoutCrossSequenceNotes();
    testProjectReplacementAbortsEvenWhenStableIdsMatch();
    testTimingCorrectCanBeDisabled();
    return 0;
}
