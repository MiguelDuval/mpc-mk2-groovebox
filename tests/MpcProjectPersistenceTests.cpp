#include "MPC/MpcEditHistory.h"
#include "MPC/MpcProjectPersistence.h"
#include "MPC/Sequencer/MpcArrangementOps.h"

#include <cassert>
#include <cmath>
#include <cstdio>
#include <filesystem>
#include <fstream>
#include <string>
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
                || a.nudgeTicks != b.nudgeTicks) {
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
    project.drumPrograms.push_back(program);

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
        MidiNoteEvent{120, 180, 36, 97, 88, 3, -11},
        MidiNoteEvent{600, 90, 42, 111, 123, 2, 7}
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
    assert(serialized.find("MPCMK2_PROJECT 2") == 0);
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

    assert(loaded.drumPrograms.size() == 1);
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

    const std::string validNoteRecord = "NOTE 120 180 36 97 88 3 -11";
    const auto noteRecordPosition = serialized.find(validNoteRecord);
    assert(noteRecordPosition != std::string::npos);
    std::string malformedNoteBytes = serialized;
    malformedNoteBytes.replace(
            noteRecordPosition,
            validNoteRecord.size(),
            "NOTE -1 180 36 97 88 3 -11");
    error.clear();
    assert(!persistence::deserializeProject(malformedNoteBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

    malformedNoteBytes = serialized;
    malformedNoteBytes.replace(
            noteRecordPosition,
            validNoteRecord.size(),
            "NOTE 120 -1 36 97 88 3 -11");
    error.clear();
    assert(!persistence::deserializeProject(malformedNoteBytes, loaded, &error));
    assert(!error.empty());
    assert(persistence::serializeProject(loaded) == loadedSerializedBeforeFailure);

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
    error.clear();
    assert(persistence::serializeProject(tooManySamples, &error).empty());
    assert(!error.empty());

    std::remove(filePath.c_str());
    std::remove((filePath + ".tmp").c_str());
    return 0;
}
