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
    assert(state.activeSequence().tracks.size() == 4);
    assert(state.activeSequence().tracks[0].name == "SAMPLE 01");
    assert(state.activeSequence().tracks[1].name == "SYNTH 01");
    assert(state.activeSequence().tracks[2].name == "MIDI EXT 01");
    assert(state.activeSequence().tracks[3].name == "AUDIO 01");
    assert(state.activeSequence().tracks[0].kind == mpc::domain::TrackKind::Drum);
    assert(state.activeSequence().tracks[1].kind == mpc::domain::TrackKind::Plugin);
    assert(state.activeSequence().tracks[2].kind == mpc::domain::TrackKind::Midi);
    assert(state.activeSequence().tracks[3].kind == mpc::domain::TrackKind::Audio);

    assert(state.selectTrack(0));
    assert(state.setTrackMuted(0, true));
    assert(state.activeSequence().tracks[0].muted);
    assert(state.setTrackSoloed(1, true));
    assert(!state.activeSequence().tracks[0].soloed);
    assert(state.activeSequence().tracks[1].soloed);
    assert(state.setTrackArmed(1, true));
    assert(!state.activeSequence().tracks[0].recordArmed);
    assert(state.activeSequence().tracks[1].recordArmed);

    assert(state.addSequence());
    assert(state.sequenceCount() == 2);
    assert(state.activeSequenceIndex() == 1);
    assert(state.activeSequence().tracks.size() == 4);
    assert(!state.activeSequence().tracks[0].muted);
    assert(!state.activeSequence().tracks[1].soloed);
    assert(!state.activeSequence().tracks[1].recordArmed);
    assert(state.nextSequence());
    assert(state.activeSequenceIndex() == 0);
    assert(state.activeSequence().tracks[0].muted);

    return 0;
}
