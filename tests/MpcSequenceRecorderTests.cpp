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
    return 0;
}
