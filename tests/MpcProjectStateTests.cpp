#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcPatternOps.h"

#include <cassert>
#include <limits>

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
    assert(sequence.tracks[0].patterns.size() == 1);
    assert(sequence.tracks[0].placements.size() == 1);
    assert(sequence.tracks[0].placements.front().patternId
            == sequence.tracks[0].patterns.front().id);
    assert(sequence.tracks[0].placements.front().startTick == 0);
    assert(sequence.tracks[0].placements.front().lengthTicks == sequence.lengthTicks);

    auto& pattern = state.activeSequence().tracks[0].patterns.front();
    assert(mpc::sequencer::setStepNote(
            pattern, 0, 240, 36, 110, 120, 127, 1));
    assert(pattern.notes.size() == 1);
    assert(pattern.notes.front().tick == 0);
    assert(pattern.notes.front().durationTicks == 120);
    assert(pattern.notes.front().velocity == 110);
    assert(mpc::sequencer::setStepNote(
            pattern, 0, 240, 36, 90, 240, 127, 1));
    assert(pattern.notes.size() == 1);
    assert(pattern.notes.front().durationTicks == 240);
    assert(pattern.notes.front().velocity == 90);
    assert(mpc::sequencer::eraseStepNote(pattern, 0, 240, 36));
    assert(pattern.notes.empty());
    assert(!mpc::sequencer::eraseStepNote(pattern, 0, 240, 36));

    assert(state.setSequenceTempo(140.0));
    assert(state.activeSequence().tempoBpm == 140.0);
    assert(!state.setSequenceTempo(10.0));
    assert(!state.setSequenceBars(0));
    assert(!state.setSequenceBars(129));

    assert(state.setSequenceBars(8));
    assert(state.activeSequence().lengthTicks == 30720);
    assert(state.activeSequence().tracks[0].placements.front().lengthTicks == 30720);
    assert(state.activeSequence().loopEndTicks == 30720);

    assert(state.setSequenceLoop(true, 3, 6));
    const auto loopStart = state.activeSequence().loopStartTicks;
    const auto loopEnd = state.activeSequence().loopEndTicks;
    assert(loopStart == 7680);
    assert(loopEnd == 23040);

    assert(state.setSequenceTimeSignature(3, 4));
    assert(state.activeSequence().lengthTicks == 23040);
    assert(state.activeSequence().tracks[0].placements.front().lengthTicks == 23040);
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
    assert(state.activeSequence().tracks[1].placements.size() == 1);
    assert(state.activeSequence().tracks[1].placements.front().patternId
            == state.activeSequence().tracks[1].patterns.front().id);
    assert(state.activeSequence().tracks[1].placements.front().startTick == 0);
    assert(state.activeSequence().tracks[1].placements.front().lengthTicks
            == state.activeSequence().lengthTicks);
    assert(state.selectTrack(0));
    assert(state.activeTrackIndex() == 0);
    assert(state.activeProgramIndexForTrack(0) == 0);

    mpc::domain::DrumProgram secondProgram;
    secondProgram.id = "drum-program-2";
    secondProgram.name = "Drum Program 2";
    secondProgram.type = mpc::domain::ProgramType::Drum;
    for (std::size_t pad = 0; pad < mpc::domain::kMaxProgramPads; ++pad) {
        secondProgram.pads[pad].index = static_cast<std::uint16_t>(pad);
        secondProgram.pads[pad].midiNote =
                static_cast<std::uint8_t>(48 + pad);
        secondProgram.pads[pad].name = "Program 2 Pad " + std::to_string(pad + 1);
    }
    state.project().drumPrograms.push_back(std::move(secondProgram));

    assert(state.setTrackProgram(0, "drum-program-2"));
    assert(state.activeSequence().tracks[0].programId == "drum-program-2");
    assert(state.activeProgramIndexForTrack(0) == 1);

    assert(state.selectTrack(0));
    assert(state.activeDrumProgram().id == "drum-program-2");

    assert(state.addTrack(mpc::domain::TrackKind::Drum));
    assert(state.activeTrackIndex() == 2);
    assert(state.activeSequence().tracks[2].programId == "drum-program-2");
    assert(state.activeProgramIndexForTrack(2) == 1);

    assert(!state.setTrackProgram(0, "missing-program"));

    assert(!state.selectTrack(99));

    assert(state.sequenceCount() == 1);
    assert(state.addSequence());
    assert(state.sequenceCount() == 2);
    assert(state.activeSequenceIndex() == 1);
    assert(state.activeTrackIndex() == 0);
    assert(state.activeSequence().name == "Sequence 02");
    assert(state.activeSequence().tracks.size() == 1);
    assert(state.activeSequence().tracks[0].kind == mpc::domain::TrackKind::Drum);
    assert(state.activeSequence().tracks[0].patterns.size() == 1);
    assert(state.activeSequence().tracks[0].placements.size() == 1);
    assert(state.activeSequence().tracks[0].placements.front().patternId
            == state.activeSequence().tracks[0].patterns.front().id);
    assert(state.activeSequence().tracks[0].placements.front().startTick == 0);
    assert(state.activeSequence().tracks[0].placements.front().lengthTicks
            == state.activeSequence().lengthTicks);
    assert(state.activeSequence().tempoBpm == 120.0);
    assert(state.activeSequence().lengthTicks == 15360);

    assert(state.selectPreviousSequence());
    assert(state.activeSequenceIndex() == 0);
    assert(state.activeTrackIndex() == 0);
    assert(state.selectNextSequence());
    assert(state.activeSequenceIndex() == 1);
    assert(state.activeTrackIndex() == 0);
    assert(state.activeDrumProgram().id == "drum-program-1");
    assert(state.activeProgramIndexForTrack(0) == 0);
    assert(state.selectSequence(0));
    assert(state.activeSequenceIndex() == 0);
    assert(!state.selectSequence(99));


    // Project replacement must reset active indices and resolve the Track-owned Program.
    mpc::domain::Project opened;
    opened.id = "opened-project";
    opened.name = "Opened Project";
    mpc::domain::DrumProgram openedProgram;
    openedProgram.id = "opened-program";
    openedProgram.name = "Opened Drums";
    openedProgram.type = mpc::domain::ProgramType::Drum;
    openedProgram.pads[0].index = 0;
    openedProgram.pads[0].midiNote = 36;
    openedProgram.pads[0].name = "Loaded Kick";
    openedProgram.pads[0].level = 0.75f;
    openedProgram.pads[0].layer(0).sample.value = 41;
    mpc::domain::SampleRef openedSample;
    openedSample.id = "sample-41";
    openedSample.name = "Saved Kick";
    openedSample.sampleRate = 48000.0;
    openedSample.lengthSamples = 12000;
    openedSample.assetId.value = 41;
    opened.samples.push_back(openedSample);
    opened.drumPrograms.push_back(openedProgram);
    mpc::domain::Sequence openedSequence;
    openedSequence.id = "opened-sequence";
    openedSequence.name = "Opening";
    openedSequence.lengthTicks = 3840;
    mpc::domain::Track openedTrack;
    openedTrack.id = "opened-track";
    openedTrack.name = "DRUMS";
    openedTrack.kind = mpc::domain::TrackKind::Drum;
    openedTrack.type = mpc::domain::ProgramType::Drum;
    openedTrack.programId = openedProgram.id;
    mpc::domain::Pattern openedPattern;
    openedPattern.id = "opened-pattern";
    openedPattern.lengthTicks = 3840;
    openedPattern.notes = {{240, 120, 36, 105, 100, 2, -5}};
    openedTrack.patterns.push_back(openedPattern);
    openedSequence.tracks.push_back(openedTrack);
    opened.sequences.push_back(openedSequence);
    assert(mpc::MpcProjectState::canReplaceProject(opened));
    assert(state.replaceProject(opened));
    assert(state.project().id == "opened-project");
    assert(state.activeSequenceIndex() == 0);
    assert(state.activeTrackIndex() == 0);
    assert(state.activeDrumProgram().id == "opened-program");
    assert(state.activeSequence().tracks[0].patterns[0].notes[0].nudgeTicks == -5);
    const auto newSampleId = state.registerSample("New sample", "", 44100.0, 9000);
    assert(newSampleId.value == 42);

    // When the next ID is UINT32_MAX, registering it must not wrap to 1 and
    // duplicate an existing asset on the next registration.
    mpc::MpcProjectState wrapSampleIdState;
    auto wrapProject = wrapSampleIdState.project();
    mpc::domain::SampleRef lowSample;
    lowSample.id = "sample-low-id";
    lowSample.name = "Existing low ID";
    lowSample.sampleRate = 48000.0;
    lowSample.lengthSamples = 100;
    lowSample.assetId.value = 1;
    wrapProject.samples.push_back(lowSample);
    mpc::domain::SampleRef highSample;
    highSample.id = "sample-high-id";
    highSample.name = "Existing high ID";
    highSample.sampleRate = 48000.0;
    highSample.lengthSamples = 100;
    highSample.assetId.value = std::numeric_limits<std::uint32_t>::max() - 1;
    wrapProject.samples.push_back(highSample);
    assert(wrapSampleIdState.replaceProject(std::move(wrapProject)));

    const auto lastSampleId = wrapSampleIdState.registerSample(
            "Last sample ID", "", 48000.0, 100);
    assert(lastSampleId.value == std::numeric_limits<std::uint32_t>::max());
    const auto wrappedSampleId = wrapSampleIdState.registerSample(
            "First free after wrap", "", 48000.0, 100);
    assert(wrappedSampleId.value == 2);
    assert(wrapSampleIdState.project().samples.size() == 4);
    assert(wrapSampleIdState.findSample(mpc::domain::SampleId{1}) != nullptr);
    assert(wrapSampleIdState.findSample(lastSampleId) != nullptr);
    assert(wrapSampleIdState.findSample(wrappedSampleId) != nullptr);
    assert(lastSampleId.value != wrappedSampleId.value);
    mpc::domain::Project invalid;
    invalid.id = "invalid";
    assert(!mpc::MpcProjectState::canReplaceProject(invalid));
    assert(!state.replaceProject(std::move(invalid)));
    assert(state.project().id == "opened-project");

    // NEW PROJECT must restore the canonical model, not only clear the UI.
    // It also resets selection and sample-ID allocation for the new domain.
    assert(state.resetToDefault());
    assert(state.project().id == "project-1");
    assert(state.project().name == "Untitled");
    assert(state.project().samples.empty());
    assert(state.project().drumPrograms.size() == 1);
    assert(state.activeDrumProgram().id == "drum-program-1");
    assert(state.activeDrumProgram().pads[0].midiNote == 36);
    assert(state.activeSequenceIndex() == 0);
    assert(state.activeTrackIndex() == 0);
    assert(state.activeSequence().id == "sequence-1");
    assert(state.activeSequence().name == "Sequence 01");
    assert(state.activeSequence().tracks.size() == 1);
    assert(state.activeSequence().tracks[0].kind == mpc::domain::TrackKind::Drum);
    assert(state.activeSequence().tracks[0].patterns.size() == 1);
    assert(state.activeSequence().tracks[0].patterns[0].notes.empty());
    assert(state.activeSequence().tracks[0].placements.size() == 1);
    assert(state.activeSequence().tracks[0].placements[0].patternId
            == state.activeSequence().tracks[0].patterns[0].id);
    assert(state.activeSequence().tracks[0].placements[0].startTick == 0);
    assert(state.activeSequence().tracks[0].placements[0].lengthTicks
            == state.activeSequence().lengthTicks);
    assert(state.activeProgramIndexForTrack(0) == 0);
    const auto firstNewSample = state.registerSample(
            "First sample in new project", "", 48000.0, 1000);
    assert(firstNewSample.value == 1);
    return 0;
}
