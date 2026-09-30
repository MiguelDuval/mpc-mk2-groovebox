#include "MPC/MpcProjectState.h"

#include <cassert>

int main() {
    mpc::MpcProjectState state;

    assert(state.project().drumPrograms.size() == 1);
    assert(state.activeDrumProgram().pads.size() == mpc::domain::kMaxProgramPads);
    assert(state.activeDrumProgram().pad(0).midiNote == 36);

    const auto sampleId = state.registerSample(
            "Kick",
            "",
            48000.0,
            24000);

    assert(sampleId.isAssigned());
    assert(state.project().samples.size() == 1);
    assert(state.findSample(sampleId) != nullptr);
    assert(state.findSample(sampleId)->name == "Kick");
    assert(state.findSample(sampleId)->sampleRate == 48000.0);
    assert(state.findSample(sampleId)->lengthSamples == 24000);

    state.activeDrumProgram().pad(0).layer(0).sample = sampleId;
    assert(state.activeDrumProgram().pad(0).layer(0).isAssigned());

    assert(state.project().sequences.size() == 1);
    const auto& sequence = state.activeSequence();
    assert(sequence.tempoBpm == 120.0);
    assert(sequence.numerator == 4);
    assert(sequence.denominator == 4);
    assert(sequence.lengthTicks == 15360);
    assert(sequence.loopEnabled);
    assert(sequence.loopStartTicks == 0);
    assert(sequence.loopEndTicks == sequence.lengthTicks);
    assert(sequence.tracks.size() == 1);
    assert(sequence.tracks[0].kind == mpc::domain::TrackKind::Drum);

    assert(state.setSequenceTempo(140.0));
    assert(state.activeSequence().tempoBpm == 140.0);
    assert(!state.setSequenceTempo(10.0));
    assert(!state.setSequenceBars(0));
    assert(!state.setSequenceBars(129));

    assert(state.setSequenceBars(8));
    assert(state.activeSequence().lengthTicks == 30720);
    assert(state.activeSequence().loopEndTicks == 30720);

    assert(state.setSequenceLoop(true, 3, 6));
    const auto loopStart = state.activeSequence().loopStartTicks;
    const auto loopEnd = state.activeSequence().loopEndTicks;
    assert(loopStart == 7680);
    assert(loopEnd == 23040);

    assert(state.setSequenceTimeSignature(3, 4));
    assert(state.activeSequence().lengthTicks == 23040);
    assert(state.activeSequence().loopStartTicks == 5760);
    assert(state.activeSequence().loopEndTicks == 17280);

    assert(state.setSequenceQuantizeGrid(240));
    assert(state.activeSequence().quantizeGridTicks == 240);
    assert(!state.setSequenceQuantizeGrid(241));
    assert(state.setSequenceSwing(35));
    assert(state.activeSequence().swingPercent == 35);
    assert(!state.setSequenceSwing(101));

    assert(state.addTrack(mpc::domain::TrackKind::Audio));
    assert(state.activeSequence().tracks.size() == 2);
    assert(state.activeTrackIndex() == 1);

    assert(state.setTrackLengthBars(0, 1));
    assert(state.activeSequence().tracks[0].lengthTicks == 2880);
    assert(state.trackLengthTicks(0) == 2880);
    assert(state.setTrackLengthBars(0, 0));
    assert(state.activeSequence().tracks[0].lengthTicks == 0);
    assert(state.trackLengthTicks(0) == state.activeSequence().lengthTicks);

    assert(state.selectTrack(0));
    assert(state.activeTrackIndex() == 0);
    assert(!state.selectTrack(99));

    assert(state.sequenceCount() == 1);
    assert(state.addSequence({}, 2));
    assert(state.sequenceCount() == 2);
    assert(state.activeSequenceIndex() == 1);
    assert(state.activeSequence().lengthTicks == 7680);
    assert(state.activeSequence().tracks.size() == 1);
    assert(state.activeSequence().tracks[0].kind == mpc::domain::TrackKind::Drum);
    assert(state.selectSequence(0));
    assert(state.activeSequenceIndex() == 0);
    assert(state.activeTrackIndex() == 0);
    assert(!state.selectSequence(99));

    return 0;
}
