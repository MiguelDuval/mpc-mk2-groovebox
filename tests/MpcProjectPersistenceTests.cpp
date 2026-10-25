#include "MPC/MpcEditHistory.h"
#include "MPC/MpcProjectPersistence.h"
#include "MPC/Sequencer/MpcArrangementOps.h"

#include <cassert>
#include <cmath>
#include <cstdint>
#include <cstdio>
#include <filesystem>
#include <fstream>
#include <limits>
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

    // Arrangement playback resolves source Patterns by track-local ID. The
    // serializer/reader must reject ambiguous IDs rather than emit a file that
    // loads successfully but cannot be flattened for playback.
    const auto assertPatternIdsRejected = [](bool makeEmptyId) {
        auto invalid = makeProject();
        auto& track = invalid.sequences[0].tracks[0];
        auto duplicate = track.patterns.front();
        if (makeEmptyId) {
            track.patterns.front().id.clear();
        } else {
            duplicate.name = "Duplicate Pattern";
            track.patterns.push_back(std::move(duplicate));
        }
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError
                == "Pattern ids must be non-empty and unique on a Track with placements");
    };
    assertPatternIdsRejected(false);
    assertPatternIdsRejected(true);

    // The reader must apply the same ambiguity check to externally supplied
    // files, not just projects serialized by this build.
    auto duplicatePatternBytes = serialized;
    const std::string originalTrackHeader =
            "TRACK \"track-drums-01\" \"DRUMS\" 0 0 \"program-drums-01\" 0 1 0 1 3\n";
    const auto trackHeaderPos = duplicatePatternBytes.find(originalTrackHeader);
    assert(trackHeaderPos != std::string::npos);
    auto duplicateTrackHeader = originalTrackHeader;
    const auto patternCountPos = duplicateTrackHeader.rfind(" 1 3\n");
    assert(patternCountPos != std::string::npos);
    duplicateTrackHeader.replace(patternCountPos, 5, " 2 3\n");
    duplicatePatternBytes.replace(
            trackHeaderPos, originalTrackHeader.size(), duplicateTrackHeader);
    const auto patternRecordPos = duplicatePatternBytes.find("PATTERN \"pattern-shared-01\"");
    const auto firstPlacementPos = duplicatePatternBytes.find("PLACEMENT \"clip-a\"");
    assert(patternRecordPos != std::string::npos);
    assert(firstPlacementPos != std::string::npos);
    const auto duplicateRecord = duplicatePatternBytes.substr(
            patternRecordPos, firstPlacementPos - patternRecordPos);
    duplicatePatternBytes.insert(firstPlacementPos, duplicateRecord);
    std::string duplicatePatternError;
    assert(!persistence::deserializeProject(
            duplicatePatternBytes, source, &duplicatePatternError));
    assert(duplicatePatternError
            == "Pattern ids must be non-empty and unique on a Track with placements");

    // Assigned layers need a non-empty playback region, and any known sample
    // length bounds the region's exclusive end frame.
    const auto assertInvalidSampleLayerRejected =
            [](auto mutateLayer, const std::string& expectedError) {
        auto invalid = makeProject();
        mutateLayer(invalid.drumPrograms[0].pads[0].layer(0));
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == expectedError);
    };
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.region = domain::SampleRegion{1777, 1777}; },
            "Sample region is empty or reversed");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.region = domain::SampleRegion{1777, 17}; },
            "Sample region is empty or reversed");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.region = domain::SampleRegion{17, 24001}; },
            "Sample region exceeds the referenced sample");
    assertInvalidSampleLayerRejected(
            [](auto& layer) {
                layer.velocityMinimum = 119;
                layer.velocityMaximum = 7;
            },
            "Sample layer velocity range is reversed");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.velocityMaximum = 128; },
            "Sample layer velocity range is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.gain = 1.25f; },
            "Sample layer gain is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.gain = std::numeric_limits<float>::quiet_NaN(); },
            "Sample layer gain is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.pan = -1.25f; },
            "Sample layer pan is out of range");
    assertInvalidSampleLayerRejected(
            [](auto& layer) { layer.tuningSemitones = 24.5f; },
            "Sample layer tuning is out of range");

    const std::string validLayerRecord =
            "LAYER 101 17 1777 0.625 0.125 -3.5 7 119 100 1700 1\n";
    const auto assertInvalidSerializedSampleLayerRejected =
            [&](const std::string& from, const std::string& to,
                    const std::string& expectedError) {
        std::string invalidBytes = serialized;
        const auto position = invalidBytes.find(validLayerRecord);
        assert(position != std::string::npos);
        auto invalidRecord = validLayerRecord;
        const auto fieldPosition = invalidRecord.find(from);
        assert(fieldPosition != std::string::npos);
        invalidRecord.replace(fieldPosition, from.size(), to);
        invalidBytes.replace(position, validLayerRecord.size(), invalidRecord);

        const auto sourceBeforeFailure = persistence::serializeProject(source);
        assert(!sourceBeforeFailure.empty());
        std::string parseError;
        assert(!persistence::deserializeProject(invalidBytes, source, &parseError));
        assert(parseError == expectedError);
        assert(persistence::serializeProject(source) == sourceBeforeFailure);
    };
    assertInvalidSerializedSampleLayerRejected(
            " 17 1777 ", " 1777 1777 ",
            "Sample region is empty or reversed");
    assertInvalidSerializedSampleLayerRejected(
            " 17 1777 ", " 17 24001 ",
            "Sample region exceeds the referenced sample");
    assertInvalidSerializedSampleLayerRejected(
            " 7 119 ", " 119 7 ",
            "Sample layer velocity range is reversed");
    assertInvalidSerializedSampleLayerRejected(
            " 7 119 ", " 7 128 ",
            "sample-layer field is out of range");
    assertInvalidSerializedSampleLayerRejected(
            " 0.625 ", " 1.25 ",
            "Sample layer gain is out of range");
    assertInvalidSerializedSampleLayerRejected(
            " 0.125 ", " -1.25 ",
            "Sample layer pan is out of range");
    assertInvalidSerializedSampleLayerRejected(
            " -3.5 ", " 24.5 ",
            "Sample layer tuning is out of range");

    // Sample rate and frame count are optional metadata when zero, but
    // negative values cannot describe a valid sample and must not silently
    // disable the sample-region upper-bound check.
    const auto assertInvalidSampleReferenceRejected =
            [](auto mutateSample, const std::string& expectedError) {
        auto invalid = makeProject();
        mutateSample(invalid.samples[0]);
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == expectedError);
    };
    assertInvalidSampleReferenceRejected(
            [](auto& sample) { sample.sampleRate = -48000.0; },
            "Sample rate is out of range");
    assertInvalidSampleReferenceRejected(
            [](auto& sample) { sample.sampleRate = std::numeric_limits<double>::quiet_NaN(); },
            "Sample rate is out of range");
    assertInvalidSampleReferenceRejected(
            [](auto& sample) { sample.lengthSamples = -1; },
            "Sample length is out of range");

    const std::string validSampleRecord =
            "SAMPLE \"sample-kick-01\" \"Kick\" \"/fixtures/kick.wav\" 48000 24000 101\n";
    const auto assertInvalidSerializedSampleReferenceRejected =
            [&](const std::string& from, const std::string& to,
                    const std::string& expectedError) {
        std::string invalidBytes = serialized;
        const auto position = invalidBytes.find(validSampleRecord);
        assert(position != std::string::npos);
        auto invalidRecord = validSampleRecord;
        const auto fieldPosition = invalidRecord.find(from);
        assert(fieldPosition != std::string::npos);
        invalidRecord.replace(fieldPosition, from.size(), to);
        invalidBytes.replace(position, validSampleRecord.size(), invalidRecord);

        const auto sourceBeforeFailure = persistence::serializeProject(source);
        assert(!sourceBeforeFailure.empty());
        std::string parseError;
        assert(!persistence::deserializeProject(invalidBytes, source, &parseError));
        assert(parseError == expectedError);
        assert(persistence::serializeProject(source) == sourceBeforeFailure);
    };
    assertInvalidSerializedSampleReferenceRejected(
            " 48000 ", " -48000 ", "Sample rate is out of range");
    assertInvalidSerializedSampleReferenceRejected(
            " 24000 101", " -1 101", "Sample length is out of range");

    // Pad controls are clamped by the live audio engine. Persisting values
    // outside those bounds would produce a Project whose UI state and runtime
    // projection disagree, and could overflow milli-unit conversions.
    const auto assertInvalidPadParameterRejected =
            [](auto mutatePad, const std::string& expectedError) {
        auto invalid = makeProject();
        mutatePad(invalid.drumPrograms[0].pads[0]);
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == expectedError);
    };
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.tuningSemitones = 24.5f; },
            "Pad tuning is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.level = 1.25f; },
            "Pad level is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.level = std::numeric_limits<float>::quiet_NaN(); },
            "Pad level is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.pan = -1.25f; },
            "Pad pan is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.envelopeAttackMs = 2001.0f; },
            "Pad envelope time is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.envelopeSustain = 1.25f; },
            "Pad envelope sustain is out of range");
    assertInvalidPadParameterRejected(
            [](auto& pad) { pad.filterCutoffHz = 20001.0f; },
            "Pad filter cutoff is out of range");

    const std::string originalPadPrefix = "PAD 0 \"Kick\"";
    const auto assertInvalidSerializedPadParameterRejected =
            [&](const std::string& from, const std::string& to,
                    const std::string& expectedError) {
        std::string invalidBytes = serialized;
        const auto padStart = invalidBytes.find(originalPadPrefix);
        assert(padStart != std::string::npos);
        const auto padEnd = invalidBytes.find('\n', padStart);
        assert(padEnd != std::string::npos);
        auto padRecord = invalidBytes.substr(padStart, padEnd - padStart);
        const auto fieldPosition = padRecord.find(from);
        assert(fieldPosition != std::string::npos);
        padRecord.replace(fieldPosition, from.size(), to);
        invalidBytes.replace(padStart, padEnd - padStart, padRecord);

        const auto sourceBeforeFailure = persistence::serializeProject(source);
        assert(!sourceBeforeFailure.empty());
        std::string parseError;
        assert(!persistence::deserializeProject(invalidBytes, source, &parseError));
        assert(parseError == expectedError);
        assert(persistence::serializeProject(source) == sourceBeforeFailure);
    };
    assertInvalidSerializedPadParameterRejected(
            " -1.25 ", " 24.5 ", "Pad tuning is out of range");
    assertInvalidSerializedPadParameterRejected(
            " 0.875 ", " 1.25 ", "Pad level is out of range");
    assertInvalidSerializedPadParameterRejected(
            " -0.25 ", " 1.25 ", "Pad pan is out of range");
    assertInvalidSerializedPadParameterRejected(
            " 3.5 ", " 2001 ", "Pad envelope time is out of range");
    assertInvalidSerializedPadParameterRejected(
            " 0.75 ", " 1.25 ", "Pad envelope sustain is out of range");
    assertInvalidSerializedPadParameterRejected(
            " 12500 ", " 20001 ", "Pad filter cutoff is out of range");

    // Sequence fields are part of the persisted project contract. Reject the
    // same invalid values at serialization and deserialization boundaries
    // rather than opening a project with transport settings the UI setters
    // would never allow.
    const auto assertInvalidSequenceSettingRejected =
            [](auto mutateSequence, const std::string& expectedError) {
        auto invalid = makeProject();
        mutateSequence(invalid.sequences[0]);
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == expectedError);
    };
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.tempoBpm = 19.0; },
            "Sequence tempo is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) {
                sequence.tempoBpm = std::numeric_limits<double>::quiet_NaN();
            },
            "Sequence tempo is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.numerator = 0; },
            "Sequence time signature is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.lengthTicks = 0; },
            "Sequence length must be positive");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.loopStartTicks = sequence.lengthTicks; },
            "Sequence loop range is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.quantizeGridTicks = 241; },
            "Sequence quantize grid is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.swingPercent = 101; },
            "Sequence swing is out of range");
    assertInvalidSequenceSettingRejected(
            [](auto& sequence) { sequence.locatorTicks[5] = sequence.lengthTicks + 1LL; },
            "Sequence locator is outside the Sequence");

    const std::string validSequenceRecord =
            "SEQUENCE \"sequence-main-01\" \"Main Sequence\" 137.125 4 4 3840 1 240 3840 240 37 1 1 0 0 240 960 -1 -1 3600 1\n";
    const auto assertInvalidSerializedSequenceRejected =
            [&](const std::string& from, const std::string& to,
                    const std::string& expectedError) {
        std::string invalidBytes = serialized;
        const auto position = invalidBytes.find(validSequenceRecord);
        assert(position != std::string::npos);
        auto invalidRecord = validSequenceRecord;
        const auto fieldPosition = invalidRecord.find(from);
        assert(fieldPosition != std::string::npos);
        invalidRecord.replace(fieldPosition, from.size(), to);
        invalidBytes.replace(position, validSequenceRecord.size(), invalidRecord);

        const auto sourceBeforeFailure = persistence::serializeProject(source);
        assert(!sourceBeforeFailure.empty());
        std::string parseError;
        assert(!persistence::deserializeProject(invalidBytes, source, &parseError));
        assert(parseError == expectedError);
        assert(persistence::serializeProject(source) == sourceBeforeFailure);
    };
    assertInvalidSerializedSequenceRejected(
            "137.125", "19", "Sequence tempo is out of range");
    assertInvalidSerializedSequenceRejected(
            " 4 4 3840 ", " 0 4 3840 ",
            "Sequence time signature is out of range");
    assertInvalidSerializedSequenceRejected(
            " 4 4 3840 ", " 4 4 0 ",
            "Sequence length must be positive");
    assertInvalidSerializedSequenceRejected(
            " 1 240 3840 ", " 1 3840 3840 ",
            "Sequence loop range is out of range");
    assertInvalidSerializedSequenceRejected(
            " 3840 1 240 3840 240 37 ",
            " 3840 1 240 3840 241 37 ",
            "Sequence quantize grid is out of range");
    assertInvalidSerializedSequenceRejected(
            " 240 37 1 1 0 ", " 240 101 1 1 0 ",
            "Sequence swing is out of range");
    assertInvalidSerializedSequenceRejected(
            " 0 240 960 -1 -1 3600 1\n",
            " 0 240 960 -1 -1 3841 1\n",
            "Sequence locator is outside the Sequence");

    // Every persisted MIDI field must satisfy the decoder's own constraints.
    // Reject the in-memory Project before producing bytes that cannot be loaded.
    const auto assertInvalidNoteRejected = [](auto mutateNote) {
        auto invalid = makeProject();
        auto& note = invalid.sequences[0].tracks[0].patterns[0].notes.front();
        mutateNote(note);
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == "MIDI note field is out of range");
    };
    assertInvalidNoteRejected([](auto& note) { note.note = 128; });
    assertInvalidNoteRejected([](auto& note) { note.velocity = 128; });
    assertInvalidNoteRejected([](auto& note) { note.probability = 128; });
    assertInvalidNoteRejected([](auto& note) { note.ratchet = 0; });
    assertInvalidNoteRejected([](auto& note) { note.ratchet = 9; });

    const auto assertInvalidNoteTimingRejected = [](auto mutateNote) {
        auto invalid = makeProject();
        auto& note = invalid.sequences[0].tracks[0].patterns[0].notes.front();
        mutateNote(note);
        std::string validationError;
        const auto bytes = persistence::serializeProject(invalid, &validationError);
        assert(bytes.empty());
        assert(validationError == "MIDI note timing is out of range");
    };
    assertInvalidNoteTimingRejected([](auto& note) { note.tick = -1; });
    assertInvalidNoteTimingRejected([](auto& note) { note.tick = 1920; });
    assertInvalidNoteTimingRejected([](auto& note) { note.durationTicks = -1; });
    assertInvalidNoteTimingRejected([](auto& note) { note.nudgeTicks = 961; });
    assertInvalidNoteTimingRejected([](auto& note) { note.nudgeTicks = -961; });

    // Velocity zero remains representable for compatibility with existing
    // silent note data because readPattern() accepts zero as an in-range value.
    auto silentNoteProject = makeProject();
    silentNoteProject.sequences[0].tracks[0].patterns[0].notes[0].velocity = 0;
    std::string silentNoteError;
    const auto silentNoteBytes =
            persistence::serializeProject(silentNoteProject, &silentNoteError);
    assert(!silentNoteBytes.empty());
    assert(silentNoteError.empty());
    domain::Project silentNoteRoundTrip;
    assert(persistence::deserializeProject(
            silentNoteBytes, silentNoteRoundTrip, &silentNoteError));
    assert(silentNoteRoundTrip.sequences[0].tracks[0].patterns[0].notes[0].velocity == 0);

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

    // The reader must reject the same timing violations as the serializer and
    // preserve the already-open Project on every malformed payload.
    const std::string validNoteRecord = "NOTE 120 180 36 97 88 3 -11\n";
    const std::string projectBeforeMalformedNotes = persistence::serializeProject(loaded);
    assert(!projectBeforeMalformedNotes.empty());
    const auto assertInvalidSerializedNoteRejected = [&](const std::string& invalidRecord) {
        std::string invalidBytes = serialized;
        const auto position = invalidBytes.find(validNoteRecord);
        assert(position != std::string::npos);
        invalidBytes.replace(position, validNoteRecord.size(), invalidRecord);
        std::string parseError;
        assert(!persistence::deserializeProject(invalidBytes, loaded, &parseError));
        assert(!parseError.empty());
        assert(persistence::serializeProject(loaded) == projectBeforeMalformedNotes);
    };
    assertInvalidSerializedNoteRejected("NOTE -1 180 36 97 88 3 -11\n");
    assertInvalidSerializedNoteRejected("NOTE 1920 180 36 97 88 3 -11\n");
    assertInvalidSerializedNoteRejected("NOTE 120 -1 36 97 88 3 -11\n");
    assertInvalidSerializedNoteRejected("NOTE 120 180 36 97 88 3 961\n");
    assertInvalidSerializedNoteRejected("NOTE 120 180 36 97 88 3 -961\n");

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

    std::remove(filePath.c_str());
    std::remove((filePath + ".tmp").c_str());
    return 0;
}
