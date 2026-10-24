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

    // Opening a document can reuse all serialized IDs; the in-memory Project
    // generation still changes and invalidates the held press.
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

void testDisarmedTrackClearsHeldNotesBeforeRearm() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.timingCorrectEnabled = false;
    sequence.quantizeGridTicks = 120;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    // A note-on is held while this track is armed.
    assert(queue.tryEnqueue({100, 0, 0, 100, 1}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());

    // The track is disarmed before the release reaches the recorder.
    track.recordArmed = false;
    // No MIDI event arrives during the disarmed interval. drain() still has
    // to invalidate the held press based on the authoritative track state.
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());

    // Once re-armed, a delayed release must not close the old tick-100 press.
    track.recordArmed = true;
    assert(queue.tryEnqueue({200, 0, 0, 0, 0}));
    assert(queue.tryEnqueue({300, 0, 0, 110, 1}));
    assert(queue.tryEnqueue({420, 0, 0, 0, 0}));
    assert(recorder.drain(state, queue) == 1);

    const auto& notes = track.patterns.front().notes;
    assert(notes.size() == 1);
    assert(notes.front().tick == 300);
    assert(notes.front().durationTicks == 120);
    assert(notes.front().velocity == 110);
}

void testRejectsZeroVelocityPressFromSyntheticQueue() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks[0];
    track.recordArmed = true;
    sequence.timingCorrectEnabled = false;

    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    // A zero-velocity Note On must not become a minimum-velocity note when a
    // malformed/synthetic producer bypasses the MIDI decoder.
    assert(queue.tryEnqueue({100, 0, 0, 0, 1}));
    assert(queue.tryEnqueue({220, 0, 0, 0, 0}));
    assert(recorder.drain(state, queue) == 0);
    assert(track.patterns.front().notes.empty());
    assert(recorder.finish(state, queue, 360) == 0);
    assert(track.patterns.front().notes.empty());
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


void testNonActiveArmedTrackUsesItsOwnDrumProgram() {
    const auto configure = [](mpc::MpcProjectState& state) {
        mpc::domain::DrumProgram secondProgram;
        secondProgram.id = "drum-program-2";
        secondProgram.name = "Second Drum Program";
        secondProgram.type = mpc::domain::ProgramType::Drum;
        for (std::size_t pad = 0; pad < mpc::domain::kMaxProgramPads; ++pad) {
            secondProgram.pads[pad].index = static_cast<std::uint16_t>(pad);
            secondProgram.pads[pad].name = "Second Pad " + std::to_string(pad + 1);
            secondProgram.pads[pad].midiNote = static_cast<std::uint8_t>(72 + pad);
        }
        state.project().drumPrograms.push_back(secondProgram);
        assert(state.addTrack(mpc::domain::TrackKind::Drum));
        assert(state.setTrackProgram(1, "drum-program-2"));
        // Keep track 0 selected: the recorder must not use its activeProgram
        // mapping when handling events destined for the separately-armed track.
        assert(state.selectTrack(0));
        state.activeSequence().tracks[0].recordArmed = false;
        state.activeSequence().tracks[1].recordArmed = true;
    };

    // The drain path records a completed press/release on non-active track 1.
    mpc::MpcProjectState drainState;
    configure(drainState);
    mpc::sequencer::SequenceRecordQueue drainQueue;
    mpc::sequencer::MpcSequenceRecorder drainRecorder;
    drainRecorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    drainRecorder.begin(drainState);
    assert(drainQueue.tryEnqueue({100, 1, 0, 100, 1}));
    assert(drainQueue.tryEnqueue({220, 1, 0, 0, 0}));
    assert(drainRecorder.drain(drainState, drainQueue) == 1);
    const auto& drainedNotes = drainState.activeSequence().tracks[1].patterns.front().notes;
    assert(drainedNotes.size() == 1);
    assert(drainedNotes.front().note == 72);
    assert(drainedNotes.front().velocity == 100);

    // The finish path must use the same track-specific mapping when closing a
    // held note without an explicit release event.
    mpc::MpcProjectState finishState;
    configure(finishState);
    mpc::sequencer::SequenceRecordQueue finishQueue;
    mpc::sequencer::MpcSequenceRecorder finishRecorder;
    finishRecorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    finishRecorder.begin(finishState);
    assert(finishQueue.tryEnqueue({300, 1, 0, 96, 1}));
    assert(finishRecorder.finish(finishState, finishQueue, 420) == 1);
    const auto& finishedNotes = finishState.activeSequence().tracks[1].patterns.front().notes;
    assert(finishedNotes.size() == 1);
    assert(finishedNotes.front().note == 72);
    assert(finishedNotes.front().velocity == 96);
}

} // namespace

int main() {
    testReplaceAndQuantize();
    testOverdubPreservesExistingNotes();
    testFinishClosesHeldNote();
    testDisarmedTrackClearsHeldNotesBeforeRearm();
    testRejectsZeroVelocityPressFromSyntheticQueue();
    testZeroLengthTapUsesGridDuration();
    testRejectsMalformedQueuedMidiValues();
    testSequenceSwitchAbortsRecordingWithoutCrossSequenceNotes();
    testNonActiveArmedTrackUsesItsOwnDrumProgram();
    testProjectReplacementAbortsEvenWhenStableIdsMatch();
    testTimingCorrectCanBeDisabled();
    return 0;
}
