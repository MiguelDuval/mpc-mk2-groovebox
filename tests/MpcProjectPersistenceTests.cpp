#include "MPC/MpcEditHistory.h"
#include "MPC/MpcProjectPersistence.h"
#include "Audio/WavSample.h"
#include "MPC/Sequencer/MpcArrangementOps.h"

#include <cassert>
#include <cstdint>
#include <memory>
#include <cmath>
#include <cstdio>
#include <filesystem>
#include <fstream>
#include <string>
#include <sstream>
#include <vector>

namespace {

bool notesEqual(
        const std::vector<mpc::domain::MidiNoteEvent>& lhs,
        const std::vector<mpc::domain::MidiNoteEvent>& rhs) {
    if (lhs.size() != rhs.size()) return false;
    for (std::size_t i = 0; i < lhs.size(); ++i) {
        const auto& a = lhs[i];
        const auto& b = rhs[i];
        if (a.tick != b.tick || a.durationTicks != b.durationTicks
                || a.note != b.note || a.velocity != b.velocity
                || a.probability != b.probability || a.ratchet != b.ratchet
                || a.nudgeTicks != b.nudgeTicks
                || a.tuningOffsetMilliSemitones != b.tuningOffsetMilliSemitones) {
            return false;
        }
    }
    return true;
}

const mpc::domain::PatternPlacement* findPlacement(
        const mpc::domain::Track& track,
        const std::string& id) {
    for (const auto& placement : track.placements) {
        if (placement.id == id) return &placement;
    }
    return nullptr;
}

mpc::domain::Project makeProject() {
    using namespace mpc::domain;
    Project project;
    project.id = "project-save-load-01";
    project.name = "Arrangement Persistence Test";

    SampleRef sample;
    sample.id = "sample-kick-01";
    sample.name = "Kick";
    sample.path = "/fixtures/kick.wav";
    sample.sampleRate = 48000.0;
    sample.lengthSamples = 24000;
    sample.assetId.value = 101;
    auto pcm = std::make_shared<mpc::audio::SampleBuffer>();
    pcm->sampleRate = 48000;
    pcm->channelCount = 1;
    pcm->interleaved.assign(24000, 0.125f);
    sample.pcm = pcm;
    project.samples.push_back(sample);

    DrumProgram program;
    program.id = "program-drums-01";
    program.name = "Drum Program";
    program.type = ProgramType::Drum;
    program.pads[0].index = 0;
    program.pads[0].name = "Kick";
    program.pads[0].midiNote = 36;
    program.pads[0].triggerMode = TriggerMode::Loop;
    program.pads[0].tuningSemitones = -1.25F;
    program.pads[0].level = 0.875F;
    program.pads[0].pan = -0.25F;
    program.pads[0].velocityScale = 0.9F;
    program.pads[0].envelopeAttackMs = 3.5F;
    program.pads[0].envelopeDecayMs = 120.25F;
    program.pads[0].envelopeSustain = 0.75F;
    program.pads[0].envelopeReleaseMs = 44.5F;
    program.pads[0].filterCutoffHz = 12500.0F;
    program.pads[0].muteGroup = 2;
    program.pads[0].polyphony = 12;
    auto& layer = program.pads[0].layer(0);
    layer.sample.value = 101;
    layer.region = SampleRegion{17, 1777};
    layer.gain = 0.625F;
    layer.pan = 0.125F;
    layer.tuningSemitones = -3.5F;
    layer.velocityMinimum = 7;
    layer.velocityMaximum = 119;
    layer.loopStartSample = 100;
    layer.loopEndSample = 1700;
    layer.enabled = true;
    program.pads[1].index = 1;
    program.pads[1].name = "Snare";
    program.pads[1].midiNote = 37;
    program.pads[1].layer(0) = layer; // Two Pads share one immutable asset.
    project.drumPrograms.push_back(program);
    auto alternateProgram = program;
    alternateProgram.id = "program-drums-02";
    alternateProgram.name = "Alternate Drum Program";
    alternateProgram.pads[0].name = "Alternate Kick";
    project.drumPrograms.push_back(alternateProgram);

    Sequence sequence;
    sequence.id = "sequence-main-01";
    sequence.name = "Main Sequence";
    sequence.tempoBpm = 137.125;
    sequence.numerator = 4;
    sequence.denominator = 4;
    sequence.lengthTicks = 3840;
    sequence.loopEnabled = true;
    sequence.loopStartTicks = 240;
    sequence.loopEndTicks = 3840;
    sequence.quantizeGridTicks = 240;
    sequence.swingPercent = 37;
    sequence.timingCorrectEnabled = true;
    sequence.metronomeEnabled = true;
    sequence.countInEnabled = false;
    sequence.locatorTicks = {0, 240, 960, -1, -1, 3600};

    Track track;
    track.id = "track-drums-01";
    track.name = "DRUMS";
    track.type = ProgramType::Drum;
    track.kind = TrackKind::Drum;
    track.programId = program.id;
    track.muted = false;
    track.soloed = true;
    track.recordArmed = false;

    Pattern pattern;
    pattern.id = "pattern-shared-01";
    pattern.name = "Shared Pattern";
    pattern.lengthTicks = 1920;
    pattern.notes = {
        MidiNoteEvent{120, 180, 36, 97, 88, 3, -11, 8500},
        MidiNoteEvent{600, 90, 42, 111, 123, 2, 7, -4250}
    };
    track.patterns.push_back(pattern);
    sequence.tracks.push_back(track);
    project.sequences.push_back(sequence);

    assert(mpc::sequencer::addPatternPlacement(
            project.sequences[0], 0, "clip-a", pattern.id, 0, 480, 0));
    assert(mpc::sequencer::addPatternPlacement(
            project.sequences[0], 0, "clip-b", pattern.id, 960, 720, 120));
    assert(mpc::sequencer::addPatternPlacement(
            project.sequences[0], 0, "clip-c", pattern.id, 1920, 960, 360));
    // Moving a real placement by one 240-tick quantize interval changes its
    // timeline start only; the linked Pattern remains the same object/data.
    assert(mpc::sequencer::movePatternPlacement(
            project.sequences[0], 0, "clip-b", 1200));
    return project;
}

} // namespace

int main() {
    using namespace mpc;
    domain::Project source = makeProject();
    const auto baselineNotes = source.sequences[0].tracks[0].patterns[0].notes;
    const auto serialized = persistence::serializeProject(source);
    assert(serialized.find("MPCMK2_PROJECT 4") == 0);
    assert(serialized.find("PCM_ASSETS 1\n") != std::string::npos);
    assert(serialized.find("PLACEMENT \"clip-b\" \"pattern-shared-01\" 1200 720 120")
            != std::string::npos);

    const auto filePath = (std::filesystem::temp_directory_path()
            / "mpc-project-persistence-test.mpcproj").string();
    std::remove(filePath.c_str());
    std::remove((filePath + ".tmp").c_str());

    std::string error;
    assert(persistence::saveProjectFile(source, filePath, &error));
    assert(error.empty());

    // Load from disk into a distinct project instance. This is a real file
    // round trip, not a re-render of the original in-memory model.
    domain::Project loaded;
    loaded.id = "pre-load-sentinel";
    assert(persistence::loadProjectFile(filePath, loaded, &error));
    assert(error.empty());
    assert(loaded.id == source.id);
    assert(loaded.name == source.name);
    assert(loaded.samples.size() == 1);
    assert(loaded.samples[0].id == "sample-kick-01");
    assert(loaded.samples[0].path == "/fixtures/kick.wav");
    assert(loaded.samples[0].sampleRate == 48000.0);
    assert(loaded.samples[0].lengthSamples == 24000);
    assert(loaded.samples[0].assetId.value == 101);
    assert(loaded.samples[0].pcm != nullptr);
    assert(loaded.samples[0].pcm->sampleRate == 48000);
    assert(loaded.samples[0].pcm->channelCount == 1);
    assert(loaded.samples[0].pcm->frameCount() == 24000);
    assert(std::abs(loaded.samples[0].pcm->interleaved[0] - 0.125f) < 0.000001f);
    assert(loaded.drumPrograms[0].pads[1].layer(0).sample.value == 101);
    assert(loaded.drumPrograms[0].pads[0].layer(0).sample.value
            == loaded.drumPrograms[0].pads[1].layer(0).sample.value);

    assert(loaded.drumPrograms.size() == 2);
    assert(loaded.drumPrograms[1].id == "program-drums-02");
    assert(loaded.drumPrograms[1].pads[0].layer(0).sample.value == 101);
    const auto& loadedPad = loaded.drumPrograms[0].pads[0];
    assert(loadedPad.index == 0);
    assert(loadedPad.name == "Kick");
    assert(loadedPad.midiNote == 36);
    assert(loadedPad.triggerMode == domain::TriggerMode::Loop);
    assert(loadedPad.tuningSemitones == -1.25F);
    assert(loadedPad.level == 0.875F);
    assert(loadedPad.muteGroup == 2);
    assert(loadedPad.polyphony == 12);
    const auto& loadedLayer = loadedPad.layer(0);
    assert(loadedLayer.sample.value == 101);
    assert(loadedLayer.region.startFrame == 17);
    assert(loadedLayer.region.endFrame == 1777);
    assert(loadedLayer.gain == 0.625F);
    assert(loadedLayer.pan == 0.125F);
    assert(loadedLayer.tuningSemitones == -3.5F);
    assert(loadedLayer.velocityMinimum == 7);
    assert(loadedLayer.velocityMaximum == 119);
    assert(loadedLayer.loopStartSample == 100);
    assert(loadedLayer.loopEndSample == 1700);

    assert(loaded.sequences.size() == 1);
    const auto& loadedSequence = loaded.sequences[0];
    assert(loadedSequence.id == "sequence-main-01");
    assert(loadedSequence.tempoBpm == 137.125);
    assert(loadedSequence.lengthTicks == 3840);
    assert(loadedSequence.loopStartTicks == 240);
    assert(loadedSequence.loopEndTicks == 3840);
    assert(loadedSequence.quantizeGridTicks == 240);
    assert(loadedSequence.swingPercent == 37);
    assert(loadedSequence.metronomeEnabled);
    assert(loadedSequence.locatorTicks[2] == 960);
    assert(loadedSequence.tracks.size() == 1);

    const auto& loadedTrack = loadedSequence.tracks[0];
    assert(loadedTrack.id == "track-drums-01");
    assert(loadedTrack.name == "DRUMS");
    assert(loadedTrack.programId == "program-drums-01");
    assert(loadedTrack.soloed);
    assert(loadedTrack.patterns.size() == 1);
    assert(loadedTrack.placements.size() == 3);
    const auto* clipA = findPlacement(loadedTrack, "clip-a");
    const auto* clipB = findPlacement(loadedTrack, "clip-b");
    const auto* clipC = findPlacement(loadedTrack, "clip-c");
    assert(clipA != nullptr && clipB != nullptr && clipC != nullptr);
    assert(clipA->patternId == "pattern-shared-01");
    assert(clipA->startTick == 0 && clipA->lengthTicks == 480);
    assert(clipA->sourceOffsetTicks == 0);
    assert(clipB->patternId == "pattern-shared-01");
    assert(clipB->startTick == 1200 && clipB->lengthTicks == 720);
    assert(clipB->sourceOffsetTicks == 120);
    assert(clipC->patternId == "pattern-shared-01");
    assert(clipC->startTick == 1920 && clipC->lengthTicks == 960);
    assert(clipC->sourceOffsetTicks == 360);
    assert(notesEqual(loadedTrack.patterns[0].notes, baselineNotes));

    // Delete a clip after loading, then use the existing edit-history mechanism
    // to restore/remove/restore it. Pattern musical data must remain identical.
    const domain::PatternPlacement savedClip = *clipB;
    assert(sequencer::erasePatternPlacement(
            loaded.sequences[0], 0, savedClip.id));
    MpcEditHistory history;
    const auto undoDelete = [&loaded, savedClip]() {
        return sequencer::addPatternPlacement(
                loaded.sequences[0], 0, savedClip.id, savedClip.patternId,
                savedClip.startTick, savedClip.lengthTicks, savedClip.sourceOffsetTicks);
    };
    const auto redoDelete = [&loaded, savedClip]() {
        return sequencer::erasePatternPlacement(
                loaded.sequences[0], 0, savedClip.id);
    };
    assert(history.record({"Delete restored Pattern placement", undoDelete, redoDelete}));
    assert(loaded.sequences[0].tracks[0].placements.size() == 2);
    assert(history.undo());
    assert(loaded.sequences[0].tracks[0].placements.size() == 3);
    assert(findPlacement(loaded.sequences[0].tracks[0], "clip-b") != nullptr);
    assert(history.redo());
    assert(loaded.sequences[0].tracks[0].placements.size() == 2);
    assert(history.undo());
    assert(loaded.sequences[0].tracks[0].placements.size() == 3);
    assert(notesEqual(loaded.sequences[0].tracks[0].patterns[0].notes, baselineNotes));

    // Version 1 represented a legacy project with Patterns but no placement
    // section. It still loads, and leaves placements empty for the existing
    // legacy Pattern projection/playback fallback.
    const std::string legacyV1 =
            "MPCMK2_PROJECT 1\n"
            "PROJECT \"legacy-project\" \"Legacy Project\"\n"
            "SAMPLES 0\n"
            "PROGRAMS 0\n"
            "SEQUENCES 1\n"
            "SEQUENCE \"sequence-legacy\" \"Sequence 01\" 120 4 4 3840 1 0 3840 240 0 1 0 0 -1 -1 -1 -1 -1 -1 1\n"
            "TRACK \"track-legacy\" \"Drums\" 0 0 \"\" 0 0 0 1\n"
            "PATTERN \"pattern-legacy\" \"Pattern 01\" 3840 1\n"
            "NOTE 120 240 36 101 99 2 -3\n"
            "END_PROJECT\n";
    domain::Project legacy;
    assert(persistence::deserializeProject(legacyV1, legacy, &error));
    assert(error.empty());
    assert(legacy.id == "legacy-project");
    assert(legacy.sequences.size() == 1);
    assert(legacy.sequences[0].tracks.size() == 1);
    assert(legacy.sequences[0].tracks[0].placements.empty());
    const auto& legacyNote = legacy.sequences[0].tracks[0].patterns[0].notes[0];
    assert(legacyNote.tick == 120);
    assert(legacyNote.durationTicks == 240);
    assert(legacyNote.note == 36);
    assert(legacyNote.velocity == 101);
    assert(legacyNote.probability == 99);
    assert(legacyNote.ratchet == 2);
    assert(legacyNote.nudgeTicks == -3);
    assert(legacyNote.tuningOffsetMilliSemitones == 0);

    // Version 2 adds placement counts but predates per-note pitch offsets.
    std::string legacyV2 = legacyV1;
    legacyV2.replace(legacyV2.find("MPCMK2_PROJECT 1"), 16, "MPCMK2_PROJECT 2");
    const auto legacyTrack = legacyV2.find("TRACK ");
    assert(legacyTrack != std::string::npos);
    const auto legacyTrackEnd = legacyV2.find('\n', legacyTrack);
    assert(legacyTrackEnd != std::string::npos);
    legacyV2.insert(legacyTrackEnd, " 0");
    domain::Project legacyV2Project;
    assert(persistence::deserializeProject(legacyV2, legacyV2Project, &error));
    assert(legacyV2Project.sequences[0].tracks[0].placements.empty());
    assert(legacyV2Project.sequences[0].tracks[0].patterns[0]
            .notes[0].tuningOffsetMilliSemitones == 0);

    // Version 3 adds the per-note tuning-offset field but has no PCM chunks.
    std::string legacyV3 = legacyV2;
    legacyV3.replace(legacyV3.find("MPCMK2_PROJECT 2"), 16, "MPCMK2_PROJECT 3");
    const std::string oldNoteLine = "NOTE 120 240 36 101 99 2 -3\n";
    const auto legacyNoteLine = legacyV3.find(oldNoteLine);
    assert(legacyNoteLine != std::string::npos);
    legacyV3.replace(legacyNoteLine, oldNoteLine.size(),
            "NOTE 120 240 36 101 99 2 -3 0\n");
    domain::Project legacyV3Project;
    assert(persistence::deserializeProject(legacyV3, legacyV3Project, &error));
    assert(legacyV3Project.sequences[0].tracks[0].patterns[0]
            .notes[0].tuningOffsetMilliSemitones == 0);

    // An actual v3 project with assigned sample metadata remains readable.
    // It restores the musical references but reports PCM missing, so v4 Save
    // refuses to produce a project that silently drops the assigned sound.
    std::string legacyAssignedV3 = serialized;
    legacyAssignedV3.replace(
            legacyAssignedV3.find("MPCMK2_PROJECT 4"), 16,
            "MPCMK2_PROJECT 3");
    const auto assignedPcmSection = legacyAssignedV3.find("PCM_ASSETS 1\\n");
    assert(assignedPcmSection != std::string::npos);
    const auto assignedPcmHeaderEnd = legacyAssignedV3.find('\\n', assignedPcmSection);
    assert(assignedPcmHeaderEnd != std::string::npos);
    std::istringstream pcmHeaderFields(legacyAssignedV3.substr(
            assignedPcmSection, assignedPcmHeaderEnd - assignedPcmSection));
    std::string pcmTag;
    std::uint32_t pcmSampleId = 0;
    std::uint32_t pcmRate = 0;
    unsigned int pcmChannels = 0;
    std::uint64_t pcmFrames = 0;
    std::uint64_t pcmBytes = 0;
    std::uint32_t pcmChecksum = 0;
    pcmHeaderFields >> pcmTag >> pcmSampleId >> pcmRate >> pcmChannels
            >> pcmFrames >> pcmBytes >> pcmChecksum;
    assert(pcmTag == "PCM_ASSETS");
    const auto pcmPayloadStart = assignedPcmHeaderEnd + 1;
    const auto pcmPayloadEnd = pcmPayloadStart
            + static_cast<std::size_t>(pcmBytes) + 1;
    assert(pcmPayloadEnd <= legacyAssignedV3.size());
    legacyAssignedV3.erase(
            assignedPcmSection, pcmPayloadEnd - assignedPcmSection);
    domain::Project legacyV3WithAssignments;
    assert(persistence::deserializeProject(
            legacyAssignedV3, legacyV3WithAssignments, &error));
    assert(legacyV3WithAssignments.samples.size() == 1);
    assert(legacyV3WithAssignments.samples[0].pcm == nullptr);
    assert(legacyV3WithAssignments.drumPrograms[0].pads[0]
            .layer(0).sample.value == 101);
    error.clear();
    assert(persistence::serializeProject(
            legacyV3WithAssignments, &error).empty());
    assert(error.find("has no PCM data") != std::string::npos);


    // A failed parse must leave every field of the already-open Project intact,
    // not merely preserve its identity. The truncated v2 payload is deliberately
    // valid through all serialized model records and fails only at END_PROJECT,
    // after a large candidate has already been decoded.
    const std::string loadedSerializedBeforeFailure =
            persistence::serializeProject(loaded, &error);
    assert(!loadedSerializedBeforeFailure.empty());
    assert(error.empty());

    const auto endProjectMarker = serialized.find("END_PROJECT\n");
    assert(endProjectMarker != std::string::npos);
    const std::string truncatedAfterRecords = serialized.substr(0, endProjectMarker);
    assert(!persistence::deserializeProject(truncatedAfterRecords, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    // Damaged PCM bytes are rejected by checksum and cannot replace live state.
    const auto pcmHeader = serialized.find("PCM_ASSET 101 ");
    assert(pcmHeader != std::string::npos);
    const auto pcmHeaderEnd = serialized.find('\n', pcmHeader);
    assert(pcmHeaderEnd != std::string::npos && pcmHeaderEnd + 1 < serialized.size());
    std::string corruptedPcm = serialized;
    corruptedPcm[pcmHeaderEnd + 1] ^= 1;
    error.clear();
    assert(!persistence::deserializeProject(corruptedPcm, loaded, &error));
    assert(error == "PCM asset checksum mismatch");
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    auto missingPcm = source;
    missingPcm.samples.front().pcm.reset();
    error.clear();
    assert(persistence::serializeProject(missingPcm, &error).empty());
    assert(error.find("has no PCM data") != std::string::npos);

    // Unsupported format versions must obey the same all-or-nothing contract.
    error.clear();
    const auto loadedIdBeforeFailure = loaded.id;
    assert(!persistence::deserializeProject(
            "MPCMK2_PROJECT 99\n", loaded, &error));
    assert(!error.empty());
    assert(loaded.id == loadedIdBeforeFailure);
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    // Serialized Pattern timing must be valid on both sides of the contract.
    // A malformed in-memory note must not be written into a file the decoder
    // would reject, and a malformed payload must not replace an open Project.
    domain::Project invalidDurationProject = source;
    invalidDurationProject.sequences[0].tracks[0].patterns[0]
            .notes[0].durationTicks = -1;
    error.clear();
    assert(persistence::serializeProject(invalidDurationProject, &error).empty());
    assert(!error.empty());

    const std::string validNoteRecord = "NOTE 120 180 36 97 88 3 -11 8500";
    const auto noteRecordPosition = serialized.find(validNoteRecord);
    assert(noteRecordPosition != std::string::npos);
    std::string malformedNoteBytes = serialized;
    malformedNoteBytes.replace(
            noteRecordPosition,
            validNoteRecord.size(),
            "NOTE -1 180 36 97 88 3 -11 8500");
    error.clear();
    assert(!persistence::deserializeProject(malformedNoteBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    malformedNoteBytes = serialized;
    malformedNoteBytes.replace(
            noteRecordPosition,
            validNoteRecord.size(),
            "NOTE 120 -1 36 97 88 3 -11 8500");
    error.clear();
    assert(!persistence::deserializeProject(malformedNoteBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    malformedNoteBytes = serialized;
    malformedNoteBytes.replace(
            noteRecordPosition,
            validNoteRecord.size(),
            "NOTE 120 180 36 97 88 3 -11 48001");
    error.clear();
    assert(!persistence::deserializeProject(malformedNoteBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    domain::Project invalidTuningOffset = source;
    invalidTuningOffset.sequences[0].tracks[0].patterns[0]
            .notes[0].tuningOffsetMilliSemitones = 48001;
    error.clear();
    assert(persistence::serializeProject(invalidTuningOffset, &error).empty());
    assert(!error.empty());

    // Keep serialization symmetric with the decoder: malformed numeric fields
    // that the reader rejects must never be written into an unreadable project.
    domain::Project invalidSampleRate = source;
    invalidSampleRate.samples.front().sampleRate =
            std::numeric_limits<double>::quiet_NaN();
    error.clear();
    assert(persistence::serializeProject(invalidSampleRate, &error).empty());
    assert(!error.empty());

    domain::Project invalidPadGain = source;
    invalidPadGain.drumPrograms.front().pads[0].level =
            std::numeric_limits<float>::infinity();
    error.clear();
    assert(persistence::serializeProject(invalidPadGain, &error).empty());
    assert(!error.empty());

    domain::Project invalidPadMidiNote = source;
    invalidPadMidiNote.drumPrograms.front().pads[0].midiNote = 128;
    error.clear();
    assert(persistence::serializeProject(invalidPadMidiNote, &error).empty());
    assert(!error.empty());

    domain::Project invalidTriggerMode = source;
    invalidTriggerMode.drumPrograms.front().pads[0].triggerMode =
            static_cast<domain::TriggerMode>(3);
    error.clear();
    assert(persistence::serializeProject(invalidTriggerMode, &error).empty());
    assert(!error.empty());

    domain::Project invalidLayerVelocity = source;
    invalidLayerVelocity.drumPrograms.front().pads[0].layer(0).velocityMaximum = 200;
    error.clear();
    assert(persistence::serializeProject(invalidLayerVelocity, &error).empty());
    assert(!error.empty());


    // SampleRef metadata and assigned SampleLayer regions/loops must be valid
    // before serialization; malformed external files must also preserve the
    // currently open Project on load failure.
    const auto assertInvalidSampleLayerRejected =
            [](auto mutateLayer, const std::string& expectedError) {
        auto invalid = makeProject();
        mutateLayer(invalid.drumPrograms[0].pads[0].layer(0));
        std::string validationError;
        assert(persistence::serializeProject(invalid, &validationError).empty());
        assert(validationError == expectedError);
    };
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.sample.value = 999; },
            "Sample layer references a missing SampleRef");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.region = domain::SampleRegion{1777, 1777}; },
            "Sample region is empty or reversed");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.region = domain::SampleRegion{17, 24001}; },
            "Sample region exceeds the referenced sample");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.loopStartSample = -1; },
            "Sample loop bounds are negative");
    assertInvalidSampleLayerRejected(
            [](auto& layer) {
                layer.loopStartSample = 1700;
                layer.loopEndSample = 100;
            },
            "Sample loop range is empty or reversed");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.loopEndSample = 24001; },
            "Sample loop exceeds the referenced sample");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.loopStartSample = 0; },
            "Sample loop is outside the playback region");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.loopEndSample = 1800; },
            "Sample loop is outside the playback region");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.gain = 1.25f; },
            "Sample layer gain is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.pan = -1.25f; },
            "Sample layer pan is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.tuningSemitones = 24.5f; },
            "Sample layer tuning is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) {
                layer.velocityMinimum = 119;
                layer.velocityMaximum = 7;
            },
            "Sample layer velocity range is reversed");

    auto negativeSampleRate = makeProject();
    negativeSampleRate.samples.front().sampleRate = -48000.0;
    std::string sampleMetadataError;
    assert(persistence::serializeProject(negativeSampleRate, &sampleMetadataError).empty());
    assert(sampleMetadataError == "Sample rate is out of range");
    auto negativeSampleLength = makeProject();
    negativeSampleLength.samples.front().lengthSamples = -1;
    sampleMetadataError.clear();
    assert(persistence::serializeProject(negativeSampleLength, &sampleMetadataError).empty());
    assert(sampleMetadataError == "Sample length is out of range");

    auto disabledLoopProject = makeProject();
    disabledLoopProject.drumPrograms[0].pads[0].layer(0).loopStartSample = 0;
    disabledLoopProject.drumPrograms[0].pads[0].layer(0).loopEndSample = 0;
    std::string disabledLoopError;
    const auto disabledLoopBytes =
            persistence::serializeProject(disabledLoopProject, &disabledLoopError);
    assert(!disabledLoopBytes.empty());
    domain::Project disabledLoopRoundTrip;
    assert(persistence::deserializeProject(
            disabledLoopBytes, disabledLoopRoundTrip, &disabledLoopError));
    assert(disabledLoopRoundTrip.drumPrograms[0].pads[0].layer(0).loopStartSample == 0);
    assert(disabledLoopRoundTrip.drumPrograms[0].pads[0].layer(0).loopEndSample == 0);

    const std::string validLayerRecord =
            "LAYER 101 17 1777 0.625 0.125 -3.5 7 119 100 1700 1\n";
    const auto assertInvalidSerializedLayerRejected =
            [&](const std::string& from, const std::string& to,
                    const std::string& expectedError) {
        std::string malformedBytes = serialized;
        const auto position = malformedBytes.find(validLayerRecord);
        assert(position != std::string::npos);
        auto malformedRecord = validLayerRecord;
        const auto field = malformedRecord.find(from);
        assert(field != std::string::npos);
        malformedRecord.replace(field, from.size(), to);
        malformedBytes.replace(position, validLayerRecord.size(), malformedRecord);
        const auto loadedBeforeFailure = persistence::serializeProject(loaded);
        assert(!loadedBeforeFailure.empty());
        std::string loadError;
        assert(!persistence::deserializeProject(malformedBytes, loaded, &loadError));
        assert(loadError == expectedError);
        assert(persistence::serializeProject(loaded) == loadedBeforeFailure);
    };
    assertInvalidSerializedLayerRejected(
            "LAYER 101 ", "LAYER 999 ",
            "Sample layer references a missing SampleRef");
    assertInvalidSerializedLayerRejected(
            " 17 1777 ", " 1777 1777 ",
            "Sample region is empty or reversed");
    assertInvalidSerializedLayerRejected(
            " 17 1777 ", " 17 24001 ",
            "Sample region exceeds the referenced sample");
    assertInvalidSerializedLayerRejected(
            " 100 1700 ", " -1 1700 ",
            "Sample loop bounds are negative");
    assertInvalidSerializedLayerRejected(
            " 100 1700 ", " 1700 100 ",
            "Sample loop range is empty or reversed");
    assertInvalidSerializedLayerRejected(
            " 100 1700 ", " 100 24001 ",
            "Sample loop exceeds the referenced sample");
    assertInvalidSerializedLayerRejected(
            " 100 1700 ", " 0 1700 ",
            "Sample loop is outside the playback region");
    assertInvalidSerializedLayerRejected(
            " 100 1700 ", " 100 1800 ",
            "Sample loop is outside the playback region");

    domain::Project invalidTrackKind = source;
    invalidTrackKind.sequences.front().tracks.front().kind =
            static_cast<domain::TrackKind>(5);
    error.clear();
    assert(persistence::serializeProject(invalidTrackKind, &error).empty());
    assert(!error.empty());

    domain::Project invalidTempo = source;
    invalidTempo.sequences.front().tempoBpm = 301.0;
    error.clear();
    assert(persistence::serializeProject(invalidTempo, &error).empty());
    assert(!error.empty());

    domain::Project invalidMeter = source;
    invalidMeter.sequences.front().numerator = 17;
    error.clear();
    assert(persistence::serializeProject(invalidMeter, &error).empty());
    assert(!error.empty());

    domain::Project invalidLoop = source;
    invalidLoop.sequences.front().loopStartTicks = 3000;
    invalidLoop.sequences.front().loopEndTicks = 2500;
    error.clear();
    assert(persistence::serializeProject(invalidLoop, &error).empty());
    assert(!error.empty());

    domain::Project invalidGrid = source;
    invalidGrid.sequences.front().quantizeGridTicks = 241;
    error.clear();
    assert(persistence::serializeProject(invalidGrid, &error).empty());
    assert(!error.empty());

    domain::Project invalidSwing = source;
    invalidSwing.sequences.front().swingPercent = 101;
    error.clear();
    assert(persistence::serializeProject(invalidSwing, &error).empty());
    assert(!error.empty());

    // Deserialization rejects values that the interactive Sequence setters
    // cannot produce, and must leave the currently-open Project untouched.
    const auto sequenceSettingsBeforeFailure =
            persistence::serializeProject(loaded, &error);
    std::string invalidSequenceBytes = serialized;
    const auto tempoToken = invalidSequenceBytes.find("137.125 4 4 3840");
    assert(tempoToken != std::string::npos);
    invalidSequenceBytes.replace(tempoToken, std::string("137.125").size(), "301");
    error.clear();
    assert(!persistence::deserializeProject(invalidSequenceBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == sequenceSettingsBeforeFailure);

    // The decoder imposes per-collection limits. Serialization rejects a
    // payload above the same limit before emitting an unreadable project.
    domain::Project tooManySamples = source;
    tooManySamples.samples.resize(100001);
    for (std::size_t index = 0; index < tooManySamples.samples.size(); ++index) {
        tooManySamples.samples[index].id =
                "oversized-sample-" + std::to_string(index);
        tooManySamples.samples[index].assetId.value =
                static_cast<std::uint32_t>(index + 1);
    }
    error.clear();
    assert(persistence::serializeProject(tooManySamples, &error).empty());
    assert(!error.empty());

    domain::Project duplicatePatternId = source;
    auto duplicatePattern = duplicatePatternId.sequences.front().tracks.front().patterns.front();
    duplicatePattern.name = "Duplicate identifier must be rejected";
    duplicatePatternId.sequences.front().tracks.front().patterns.push_back(duplicatePattern);
    error.clear();
    assert(persistence::serializeProject(duplicatePatternId, &error).empty());
    assert(!error.empty());

    domain::Project validTwoPatterns = source;
    auto secondPattern = validTwoPatterns.sequences.front().tracks.front().patterns.front();
    secondPattern.id = "pattern-distinct-02";
    secondPattern.name = "Second unplaced Pattern";
    validTwoPatterns.sequences.front().tracks.front().patterns.push_back(secondPattern);
    auto malformedDuplicatePatternId = persistence::serializeProject(validTwoPatterns);
    const std::string secondPatternTag = R"(PATTERN "pattern-distinct-02")";
    const auto secondPatternPos = malformedDuplicatePatternId.find(secondPatternTag);
    assert(secondPatternPos != std::string::npos);
    malformedDuplicatePatternId.replace(
            secondPatternPos,
            secondPatternTag.size(),
            R"(PATTERN "pattern-shared-01")");
    domain::Project untouchedAfterDuplicateId = source;
    const auto beforeDuplicateIdFailure = persistence::serializeProject(untouchedAfterDuplicateId);
    error.clear();
    assert(!persistence::deserializeProject(
            malformedDuplicatePatternId, untouchedAfterDuplicateId, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(untouchedAfterDuplicateId)
            == beforeDuplicateIdFailure);

    domain::Project duplicateAssetIds = source;
    auto secondSample = duplicateAssetIds.samples.front();
    secondSample.id = "sample-distinct-02";
    duplicateAssetIds.samples.push_back(secondSample);
    error.clear();
    assert(persistence::serializeProject(duplicateAssetIds, &error).empty());
    assert(!error.empty());

    // Every serialized entity identity is required to be nonempty and
    // unique in its documented scope, independently of the other validators.
    const auto assertDuplicateIdentityRejected =
            [&error](const domain::Project& invalid, const std::string& expected) {
        error.clear();
        assert(persistence::serializeProject(invalid, &error).empty());
        assert(error == expected);
    };

    auto emptyProjectId = source;
    emptyProjectId.id.clear();
    assertDuplicateIdentityRejected(
            emptyProjectId, "Project identifier must not be empty");

    auto duplicateSampleStringId = source;
    auto duplicateSampleString = duplicateSampleStringId.samples.front();
    duplicateSampleString.assetId.value += 100;
    duplicateSampleStringId.samples.push_back(duplicateSampleString);
    assertDuplicateIdentityRejected(
            duplicateSampleStringId,
            "Sample identifiers must be nonempty and unique");

    auto duplicateProgramId = source;
    duplicateProgramId.drumPrograms.push_back(
            duplicateProgramId.drumPrograms.front());
    assertDuplicateIdentityRejected(
            duplicateProgramId,
            "Drum Program identifiers must be nonempty and unique");

    auto duplicateSequenceId = source;
    duplicateSequenceId.sequences.push_back(
            duplicateSequenceId.sequences.front());
    assertDuplicateIdentityRejected(
            duplicateSequenceId,
            "Sequence identifiers must be nonempty and unique");

    auto duplicateTrackId = source;
    duplicateTrackId.sequences.front().tracks.push_back(
            duplicateTrackId.sequences.front().tracks.front());
    assertDuplicateIdentityRejected(
            duplicateTrackId,
            "Track identifiers must be nonempty and unique within a Sequence");

    auto duplicatePlacementId = source;
    duplicatePlacementId.sequences.front().tracks.front().placements.push_back(
            duplicatePlacementId.sequences.front().tracks.front().placements.front());
    assertDuplicateIdentityRejected(
            duplicatePlacementId,
            "Placement identifiers must be nonempty and unique within a Track");

    // A large valid Arrangement must remain practical to validate. Placements
    // are deliberately stored in reverse timeline order to exercise sorting,
    // not a best-case already-sorted fast path.
    domain::Project largeArrangement = source;
    auto& largeSequence = largeArrangement.sequences.front();
    auto& largeTrack = largeSequence.tracks.front();
    constexpr std::int32_t kPlacementCount = 12000;
    constexpr std::int32_t kPatternCount = 4096;
    largeSequence.lengthTicks = kPlacementCount * 2;
    largeTrack.patterns.clear();
    largeTrack.patterns.reserve(kPatternCount);
    for (std::int32_t index = 0; index < kPatternCount; ++index) {
        mpc::domain::Pattern pattern;
        pattern.id = "large-pattern-" + std::to_string(index);
        pattern.name = pattern.id;
        pattern.lengthTicks = largeSequence.lengthTicks;
        largeTrack.patterns.push_back(std::move(pattern));
    }
    largeTrack.placements.clear();
    largeTrack.placements.reserve(kPlacementCount);
    for (std::int32_t index = kPlacementCount - 1; index >= 0; --index) {
        const auto& pattern = largeTrack.patterns[
                static_cast<std::size_t>(index % kPatternCount)];
        largeTrack.placements.push_back({
                "large-clip-" + std::to_string(index),
                pattern.id,
                index * 2,
                1,
                0
        });
    }
    error.clear();
    const auto largeSerialized =
            persistence::serializeProject(largeArrangement, &error);
    assert(!largeSerialized.empty());
    assert(error.empty());

    // Scrambled input order must not hide an actual timeline overlap.
    largeTrack.placements.back().startTick = 2;
    error.clear();
    assert(persistence::serializeProject(largeArrangement, &error).empty());
    assert(!error.empty());

    // File APIs must have the same all-or-nothing guarantees as the
    // in-memory decoder: a bad save keeps the previous file, and every
    // failed read keeps every field in the active destination Project.
    domain::Project atomicOpenProject = source;
    const std::string atomicOpenBaseline =
            persistence::serializeProject(atomicOpenProject, &error);
    assert(!atomicOpenBaseline.empty());
    assert(error.empty());

    domain::Project invalidForSave = source;
    bool invalidatedPlacement = false;
    for (auto& placement : invalidForSave.sequences[0].tracks[0].placements) {
        if (placement.id == "clip-b") {
            placement.patternId = "pattern-missing-01";
            invalidatedPlacement = true;
            break;
        }
    }
    assert(invalidatedPlacement);
    error.clear();
    assert(!persistence::saveProjectFile(invalidForSave, filePath, &error));
    assert(!error.empty());
    {
        std::ifstream savedFile(filePath, std::ios::binary);
        assert(savedFile.good());
        savedFile.seekg(0, std::ios::end);
        const auto fileSize = savedFile.tellg();
        assert(fileSize >= 0);
        savedFile.seekg(0, std::ios::beg);
        std::string preservedBytes(
                static_cast<std::size_t>(fileSize), '\0');
        if (!preservedBytes.empty()) {
            savedFile.read(
                    preservedBytes.data(),
                    static_cast<std::streamsize>(preservedBytes.size()));
            assert(savedFile.good());
        }
        assert(preservedBytes == serialized);
    }

    const std::string truncatedV2 =
            "MPCMK2_PROJECT 2\n"
            "PROJECT \"partial-project\" \"Partial Project\"\n"
            "SAMPLES 1\n";
    const std::string malformedFilePath = filePath + ".malformed";
    {
        std::ofstream malformedFile(
                malformedFilePath, std::ios::binary | std::ios::trunc);
        assert(malformedFile.good());
        malformedFile << truncatedV2;
        assert(malformedFile.good());
    }
    error.clear();
    assert(!persistence::loadProjectFile(
            malformedFilePath, atomicOpenProject, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(atomicOpenProject)
            == atomicOpenBaseline);

    std::string invalidPlacementBytes = serialized;
    const std::string validPlacement =
            "PLACEMENT \"clip-b\" \"pattern-shared-01\" 1200 720 120";
    const auto placementPosition = invalidPlacementBytes.find(validPlacement);
    assert(placementPosition != std::string::npos);
    invalidPlacementBytes.replace(
            placementPosition, validPlacement.size(),
            "PLACEMENT \"clip-b\" \"pattern-missing-01\" 1200 720 120");
    const std::string invalidPlacementFilePath =
            filePath + ".invalid-placement";
    {
        std::ofstream invalidFile(
                invalidPlacementFilePath, std::ios::binary | std::ios::trunc);
        assert(invalidFile.good());
        invalidFile << invalidPlacementBytes;
        assert(invalidFile.good());
    }
    error.clear();
    assert(!persistence::loadProjectFile(
            invalidPlacementFilePath, atomicOpenProject, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(atomicOpenProject)
            == atomicOpenBaseline);

    const std::string missingFilePath = filePath + ".does-not-exist";
    std::remove(missingFilePath.c_str());
    error.clear();
    assert(!persistence::loadProjectFile(
            missingFilePath, atomicOpenProject, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(atomicOpenProject)
            == atomicOpenBaseline);

    std::remove(malformedFilePath.c_str());
    std::remove((malformedFilePath + ".tmp").c_str());
    std::remove(invalidPlacementFilePath.c_str());
    std::remove((invalidPlacementFilePath + ".tmp").c_str());

    std::remove(filePath.c_str());
    std::remove((filePath + ".tmp").c_str());
    return 0;
}
