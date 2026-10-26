#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcPatternOps.h"
#include "MPC/Sequencer/MpcSequenceSettings.h"

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
    // Malformed/legacy-sized sequences can approach INT32_MAX. Changing
    // the time signature must not overflow loop-bar rounding or loop tick
    // multiplication, and the resulting half-open range must stay in bounds.
    mpc::MpcProjectState extremeTimeSignatureState;
    auto& extremeSequence = extremeTimeSignatureState.activeSequence();
    const auto maximumTicks = std::numeric_limits<std::int32_t>::max();
    extremeSequence.lengthTicks = maximumTicks;
    extremeSequence.loopStartTicks = maximumTicks - 3840;
    extremeSequence.loopEndTicks = maximumTicks;
    extremeSequence.tracks.front().patterns.front().lengthTicks = maximumTicks;
    extremeSequence.tracks.front().placements.front().lengthTicks = maximumTicks;
    assert(extremeTimeSignatureState.setSequenceTimeSignature(16, 4));
    assert(extremeSequence.lengthTicks == maximumTicks);
    assert(extremeSequence.loopStartTicks >= 0);
    assert(extremeSequence.loopStartTicks < extremeSequence.loopEndTicks);
    assert(extremeSequence.loopEndTicks <= extremeSequence.lengthTicks);
    assert(extremeSequence.loopStartTicks == maximumTicks - 1);
    assert(extremeSequence.loopEndTicks == maximumTicks);


    // Shortening a Sequence must not leave Pattern notes or Arrangement
    // placements beyond their newly shortened timeline/source bounds.
    mpc::MpcProjectState barResizeState;
    assert(barResizeState.setSequenceBars(8));
    auto& barResizeSequence = barResizeState.activeSequence();
    auto& barResizeTrack = barResizeSequence.tracks.front();
    auto& barResizePattern = barResizeTrack.patterns.front();
    barResizePattern.notes = {
        {100, 120, 36, 100, 127, 1, 0},
        {3600, 5000, 38, 90, 127, 1, 0},
        {4000, 240, 42, 80, 127, 1, 0}
    };
    barResizeTrack.placements.clear();
    barResizeTrack.placements = {
        {"clip-a", barResizePattern.id, 100, 500, 0},
        {"clip-b", barResizePattern.id, 3600, 500, 3500},
        {"clip-c", barResizePattern.id, 5000, 300, 0}
    };
    assert(barResizeState.setSequenceBars(1));
    assert(barResizeSequence.lengthTicks == 3840);
    assert(barResizePattern.lengthTicks == 3840);
    assert(barResizePattern.notes.size() == 2);
    assert(barResizePattern.notes[0].tick == 100);
    assert(barResizePattern.notes[0].durationTicks == 120);
    assert(barResizePattern.notes[1].tick == 3600);
    assert(barResizePattern.notes[1].durationTicks == 3840);
    assert(barResizeTrack.placements.size() == 2);
    assert(barResizeTrack.placements[0].id == "clip-a");
    assert(barResizeTrack.placements[0].startTick == 100);
    assert(barResizeTrack.placements[0].lengthTicks == 500);
    assert(barResizeTrack.placements[1].id == "clip-b");
    assert(barResizeTrack.placements[1].startTick == 3600);
    assert(barResizeTrack.placements[1].lengthTicks == 240);
    for (const auto& placement : barResizeTrack.placements) {
        assert(placement.startTick >= 0);
        assert(placement.lengthTicks > 0);
        assert(static_cast<std::int64_t>(placement.startTick) + placement.lengthTicks
                <= barResizeSequence.lengthTicks);
        assert(placement.sourceOffsetTicks >= 0);
        assert(static_cast<std::int64_t>(placement.sourceOffsetTicks) + placement.lengthTicks
                <= barResizePattern.lengthTicks);
    }

    // Changing meter can also shorten the resulting Sequence; the same bounds
    // repair must apply when the time-signature setter resizes its Patterns.
    mpc::MpcProjectState meterResizeState;
    assert(meterResizeState.setSequenceBars(8));
    auto& meterResizeSequence = meterResizeState.activeSequence();
    auto& meterResizeTrack = meterResizeSequence.tracks.front();
    auto& meterResizePattern = meterResizeTrack.patterns.front();
    meterResizePattern.notes = {
        {100, 120, 36, 100, 127, 1, 0},
        {25000, 25000, 38, 90, 127, 1, 0}
    };
    meterResizeTrack.placements.clear();
    meterResizeTrack.placements = {
        {"meter-clip-a", meterResizePattern.id, 100, 500, 0},
        {"meter-clip-b", meterResizePattern.id, 24000, 1000, 24000}
    };
    assert(meterResizeState.setSequenceTimeSignature(3, 4));
    assert(meterResizeSequence.lengthTicks == 23040);
    assert(meterResizePattern.lengthTicks == 23040);
    assert(meterResizePattern.notes.size() == 1);
    assert(meterResizePattern.notes.front().tick == 100);
    assert(meterResizeTrack.placements.size() == 1);
    assert(meterResizeTrack.placements.front().id == "meter-clip-a");
    assert(meterResizeTrack.placements.front().lengthTicks == 500);

    // A malformed/legacy loop whose end precedes its start must be repaired
    // when the Sequence resize API next runs, rather than exposing an inverted
    // half-open range to the transport.
    mpc::MpcProjectState invalidLoopResizeState;
    auto& invalidLoopResizeSequence = invalidLoopResizeState.activeSequence();
    invalidLoopResizeSequence.loopEnabled = true;
    invalidLoopResizeSequence.loopStartTicks = 7000;
    invalidLoopResizeSequence.loopEndTicks = 4000;
    assert(invalidLoopResizeState.setSequenceBars(4));
    assert(invalidLoopResizeSequence.loopStartTicks == 7000);
    assert(invalidLoopResizeSequence.loopEndTicks == 7001);
    assert(invalidLoopResizeSequence.loopStartTicks
            < invalidLoopResizeSequence.loopEndTicks);
    assert(invalidLoopResizeSequence.loopEndTicks
            <= invalidLoopResizeSequence.lengthTicks);


    // Bar-to-tick loop mapping must clamp in int64 before narrowing when a
    // legacy Sequence is at INT32_MAX and the final bar rounds above the limit.
    mpc::MpcProjectState extremeLoopState;
    auto& extremeLoopSequence = extremeLoopState.activeSequence();
    extremeLoopSequence.lengthTicks = std::numeric_limits<std::int32_t>::max();
    extremeLoopSequence.loopStartTicks = 0;
    extremeLoopSequence.loopEndTicks = extremeLoopSequence.lengthTicks;
    extremeLoopSequence.tracks.front().patterns.front().lengthTicks =
            extremeLoopSequence.lengthTicks;
    extremeLoopSequence.tracks.front().placements.front().lengthTicks =
            extremeLoopSequence.lengthTicks;
    const auto extremeBars = mpc::sequencer::sequenceBars(extremeLoopSequence);
    assert(extremeLoopState.setSequenceLoop(false, extremeBars, extremeBars));
    assert(!extremeLoopSequence.loopEnabled);
    assert(extremeLoopSequence.loopStartTicks >= 0);
    assert(extremeLoopSequence.loopStartTicks < extremeLoopSequence.loopEndTicks);
    assert(extremeLoopSequence.loopEndTicks == extremeLoopSequence.lengthTicks);

    // A rejected edit is transactional: an invalid sequence length must not
    // partially change the currently stored loop-enable/range state.
    mpc::MpcProjectState invalidLoopState;
    auto& invalidLoopSequence = invalidLoopState.activeSequence();
    invalidLoopSequence.lengthTicks = 0;
    invalidLoopSequence.loopEnabled = true;
    invalidLoopSequence.loopStartTicks = 123;
    invalidLoopSequence.loopEndTicks = 456;
    assert(!invalidLoopState.setSequenceLoop(false, 1, 1));
    assert(invalidLoopSequence.loopEnabled);
    assert(invalidLoopSequence.loopStartTicks == 123);
    assert(invalidLoopSequence.loopEndTicks == 456);

    return 0;
}
