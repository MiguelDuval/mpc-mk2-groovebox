#include "MPC/MpcProjectPersistence.h"

#include <algorithm>
#include <cmath>
#include <cstdio>
#include <cstdint>
#include <fstream>
#include <iomanip>
#include <limits>
#include <set>
#include <sstream>
#include <string>
#include <utility>

namespace mpc::persistence {
namespace {

constexpr std::uint64_t kMaxCollectionItems = 1'000'000;
constexpr std::uint64_t kMaxSamples = 100'000;
constexpr std::uint64_t kMaxPrograms = 4'096;
constexpr std::uint64_t kMaxSequences = domain::kMaxSequences;
constexpr std::uint64_t kMaxTracks = domain::kMaxSequenceTracks;
constexpr std::uint64_t kMaxPatternsPerTrack = 4'096;
constexpr std::uint64_t kMaxPlacementsPerTrack = 65'536;
constexpr std::uint64_t kMaxNotesPerPattern = 1'000'000;
constexpr std::uint64_t kMaxProjectFileBytes = 128ULL * 1024ULL * 1024ULL;

bool setError(std::string* error, const std::string& message) {
    if (error != nullptr && error->empty()) *error = message;
    return false;
}

class Decoder final {
public:
    Decoder(std::istream& stream, std::string* error)
        : stream_(stream), error_(error) {}

    bool tag(std::string_view expected) {
        std::string actual;
        if (!(stream_ >> actual)) return fail("unexpected end of project before record " + std::string(expected));
        if (actual != expected) return fail("expected record " + std::string(expected) + ", got " + actual);
        return true;
    }

    template <typename T>
    bool value(T& result) {
        if (!(stream_ >> result)) return fail("invalid or truncated project value");
        return true;
    }

    bool text(std::string& result) {
        if (!(stream_ >> std::quoted(result))) return fail("invalid or truncated quoted text");
        return true;
    }

    bool boolean(bool& result) {
        int raw = 0;
        if (!value(raw)) return false;
        if (raw != 0 && raw != 1) return fail("boolean value must be 0 or 1");
        result = raw != 0;
        return true;
    }

    bool finiteFloat(float& result) {
        if (!value(result)) return false;
        if (!std::isfinite(result)) return fail("non-finite float in project");
        return true;
    }

    bool finiteDouble(double& result) {
        if (!value(result)) return false;
        if (!std::isfinite(result)) return fail("non-finite number in project");
        return true;
    }

    bool count(std::size_t& result, std::uint64_t maximum, const char* label) {
        std::uint64_t raw = 0;
        if (!value(raw)) return false;
        if (raw > maximum || raw > static_cast<std::uint64_t>(
                    std::numeric_limits<std::size_t>::max())) {
            return fail(std::string(label) + " count exceeds supported limit");
        }
        result = static_cast<std::size_t>(raw);
        return true;
    }

    bool finished() {
        stream_ >> std::ws;
        if (!stream_.eof()) return fail("unexpected data after END_PROJECT");
        return true;
    }

    bool fail(const std::string& message) {
        return setError(error_, message);
    }

private:
    std::istream& stream_;
    std::string* error_;
};

void writeLayer(std::ostream& out, const domain::SampleLayer& layer) {
    out << "LAYER " << layer.sample.value
        << ' ' << layer.region.startFrame << ' ' << layer.region.endFrame
        << ' ' << layer.gain << ' ' << layer.pan << ' ' << layer.tuningSemitones
        << ' ' << static_cast<unsigned int>(layer.velocityMinimum)
        << ' ' << static_cast<unsigned int>(layer.velocityMaximum)
        << ' ' << layer.loopStartSample << ' ' << layer.loopEndSample
        << ' ' << static_cast<unsigned int>(layer.enabled) << '\n';
}

bool readLayer(Decoder& in, domain::SampleLayer& layer) {
    std::uint32_t sampleId = 0;
    std::uint64_t startFrame = 0;
    std::uint64_t endFrame = 0;
    unsigned int velocityMinimum = 0;
    unsigned int velocityMaximum = 0;

    if (!in.tag("LAYER")
            || !in.value(sampleId)
            || !in.value(startFrame)
            || !in.value(endFrame)
            || !in.finiteFloat(layer.gain)
            || !in.finiteFloat(layer.pan)
            || !in.finiteFloat(layer.tuningSemitones)
            || !in.value(velocityMinimum)
            || !in.value(velocityMaximum)
            || !in.value(layer.loopStartSample)
            || !in.value(layer.loopEndSample)
            || !in.boolean(layer.enabled)) {
        return false;
    }
    if (startFrame > static_cast<std::uint64_t>(std::numeric_limits<std::size_t>::max())
            || endFrame > static_cast<std::uint64_t>(std::numeric_limits<std::size_t>::max())
            || velocityMinimum > 127 || velocityMaximum > 127) {
        return in.fail("sample-layer field is out of range");
    }

    layer.sample.value = sampleId;
    layer.region.startFrame = static_cast<std::size_t>(startFrame);
    layer.region.endFrame = static_cast<std::size_t>(endFrame);
    layer.velocityMinimum = static_cast<std::uint8_t>(velocityMinimum);
    layer.velocityMaximum = static_cast<std::uint8_t>(velocityMaximum);
    return true;
}

void writePad(std::ostream& out, const domain::Pad& pad) {
    out << "PAD " << pad.index << ' ' << std::quoted(pad.name)
        << ' ' << static_cast<unsigned int>(pad.midiNote)
        << ' ' << static_cast<unsigned int>(pad.triggerMode)
        << ' ' << pad.tuningSemitones << ' ' << pad.level << ' ' << pad.pan
        << ' ' << pad.velocityScale << ' ' << pad.envelopeAttackMs
        << ' ' << pad.envelopeDecayMs << ' ' << pad.envelopeSustain
        << ' ' << pad.envelopeReleaseMs << ' ' << pad.filterCutoffHz
        << ' ' << static_cast<unsigned int>(pad.muteGroup)
        << ' ' << static_cast<unsigned int>(pad.polyphony)
        << ' ' << static_cast<unsigned int>(pad.muted)
        << ' ' << static_cast<unsigned int>(pad.soloed) << '\n';
    for (const auto& layer : pad.layers) writeLayer(out, layer);
}

bool readPad(Decoder& in, domain::Pad& pad) {
    unsigned int index = 0;
    unsigned int midiNote = 0;
    unsigned int triggerMode = 0;
    unsigned int muteGroup = 0;
    unsigned int polyphony = 0;

    if (!in.tag("PAD")
            || !in.value(index)
            || !in.text(pad.name)
            || !in.value(midiNote)
            || !in.value(triggerMode)
            || !in.finiteFloat(pad.tuningSemitones)
            || !in.finiteFloat(pad.level)
            || !in.finiteFloat(pad.pan)
            || !in.finiteFloat(pad.velocityScale)
            || !in.finiteFloat(pad.envelopeAttackMs)
            || !in.finiteFloat(pad.envelopeDecayMs)
            || !in.finiteFloat(pad.envelopeSustain)
            || !in.finiteFloat(pad.envelopeReleaseMs)
            || !in.finiteFloat(pad.filterCutoffHz)
            || !in.value(muteGroup)
            || !in.value(polyphony)
            || !in.boolean(pad.muted)
            || !in.boolean(pad.soloed)) {
        return false;
    }
    if (index > std::numeric_limits<std::uint16_t>::max()
            || midiNote > 127 || triggerMode > 2
            || muteGroup > 255 || polyphony > 255) {
        return in.fail("Pad field is out of range");
    }

    pad.index = static_cast<std::uint16_t>(index);
    pad.midiNote = static_cast<std::uint8_t>(midiNote);
    pad.triggerMode = static_cast<domain::TriggerMode>(triggerMode);
    pad.muteGroup = static_cast<std::uint8_t>(muteGroup);
    pad.polyphony = static_cast<std::uint8_t>(polyphony);
    for (auto& layer : pad.layers) {
        if (!readLayer(in, layer)) return false;
    }
    return true;
}

bool readPattern(Decoder& in, domain::Pattern& pattern) {
    std::size_t noteCount = 0;
    if (!in.tag("PATTERN")
            || !in.text(pattern.id)
            || !in.text(pattern.name)
            || !in.value(pattern.lengthTicks)
            || !in.count(noteCount, kMaxNotesPerPattern, "Pattern note")) {
        return false;
    }
    pattern.notes.reserve(noteCount);
    for (std::size_t i = 0; i < noteCount; ++i) {
        domain::MidiNoteEvent note;
        unsigned int pitch = 0;
        unsigned int velocity = 0;
        unsigned int probability = 0;
        unsigned int ratchet = 0;
        if (!in.tag("NOTE")
                || !in.value(note.tick)
                || !in.value(note.durationTicks)
                || !in.value(pitch)
                || !in.value(velocity)
                || !in.value(probability)
                || !in.value(ratchet)
                || !in.value(note.nudgeTicks)) {
            return false;
        }
        if (pitch > 127 || velocity > 127 || probability > 127
                || ratchet < 1 || ratchet > 8) {
            return in.fail("MIDI note field is out of range");
        }
        note.note = static_cast<std::uint8_t>(pitch);
        note.velocity = static_cast<std::uint8_t>(velocity);
        note.probability = static_cast<std::uint8_t>(probability);
        note.ratchet = static_cast<std::uint8_t>(ratchet);
        pattern.notes.push_back(note);
    }
    return true;
}

bool validatePlacements(const domain::Project& project, std::string* error) {
    for (const auto& sequence : project.sequences) {
        for (const auto& track : sequence.tracks) {
            std::set<std::string> ids;
            for (std::size_t i = 0; i < track.placements.size(); ++i) {
                const auto& placement = track.placements[i];
                const auto pattern = std::find_if(
                        track.patterns.begin(), track.patterns.end(),
                        [&placement](const domain::Pattern& value) {
                            return value.id == placement.patternId;
                        });
                if (placement.id.empty() || !ids.insert(placement.id).second) {
                    return setError(error, "Pattern placement id is empty or duplicated");
                }
                if (pattern == track.patterns.end()) {
                    return setError(error, "Pattern placement references a missing Pattern");
                }
                const auto timelineEnd = static_cast<std::int64_t>(placement.startTick)
                        + static_cast<std::int64_t>(placement.lengthTicks);
                const auto sourceEnd = static_cast<std::int64_t>(placement.sourceOffsetTicks)
                        + static_cast<std::int64_t>(placement.lengthTicks);
                if (placement.startTick < 0 || placement.lengthTicks <= 0
                        || placement.sourceOffsetTicks < 0
                        || timelineEnd > sequence.lengthTicks
                        || sourceEnd > pattern->lengthTicks) {
                    return setError(error, "Pattern placement range is invalid");
                }
                for (std::size_t j = 0; j < i; ++j) {
                    const auto& other = track.placements[j];
                    const auto otherEnd = static_cast<std::int64_t>(other.startTick)
                            + static_cast<std::int64_t>(other.lengthTicks);
                    if (static_cast<std::int64_t>(placement.startTick) < otherEnd
                            && static_cast<std::int64_t>(other.startTick) < timelineEnd) {
                        return setError(error, "Pattern placements overlap on one Track");
                    }
                }
            }
        }
    }
    return true;
}

} // namespace

std::string serializeProject(const domain::Project& project, std::string* error) {
    if (error != nullptr) error->clear();
    if (!validatePlacements(project, error)) return {};
    std::ostringstream out;
    out << std::setprecision(std::numeric_limits<double>::max_digits10);
    out << "MPCMK2_PROJECT " << kCurrentProjectFormatVersion << '\n';
    out << "PROJECT " << std::quoted(project.id) << ' ' << std::quoted(project.name) << '\n';

    out << "SAMPLES " << project.samples.size() << '\n';
    for (const auto& sample : project.samples) {
        out << "SAMPLE " << std::quoted(sample.id) << ' '
            << std::quoted(sample.name) << ' ' << std::quoted(sample.path)
            << ' ' << sample.sampleRate << ' ' << sample.lengthSamples
            << ' ' << sample.assetId.value << '\n';
    }

    out << "PROGRAMS " << project.drumPrograms.size() << '\n';
    for (const auto& program : project.drumPrograms) {
        out << "PROGRAM " << std::quoted(program.id) << ' '
            << std::quoted(program.name) << ' '
            << static_cast<unsigned int>(program.type) << '\n';
        for (const auto& pad : program.pads) writePad(out, pad);
    }

    out << "SEQUENCES " << project.sequences.size() << '\n';
    for (const auto& sequence : project.sequences) {
        out << "SEQUENCE " << std::quoted(sequence.id) << ' '
            << std::quoted(sequence.name) << ' '
            << sequence.tempoBpm << ' '
            << sequence.numerator << ' ' << sequence.denominator << ' '
            << sequence.lengthTicks << ' '
            << static_cast<unsigned int>(sequence.loopEnabled) << ' '
            << sequence.loopStartTicks << ' ' << sequence.loopEndTicks << ' '
            << sequence.quantizeGridTicks << ' ' << sequence.swingPercent << ' '
            << static_cast<unsigned int>(sequence.timingCorrectEnabled) << ' '
            << static_cast<unsigned int>(sequence.metronomeEnabled) << ' '
            << static_cast<unsigned int>(sequence.countInEnabled);
        for (const auto tick : sequence.locatorTicks) out << ' ' << tick;
        out << ' ' << sequence.tracks.size() << '\n';

        for (const auto& track : sequence.tracks) {
            out << "TRACK " << std::quoted(track.id) << ' '
                << std::quoted(track.name) << ' '
                << static_cast<unsigned int>(track.type) << ' '
                << static_cast<unsigned int>(track.kind) << ' '
                << std::quoted(track.programId) << ' '
                << static_cast<unsigned int>(track.muted) << ' '
                << static_cast<unsigned int>(track.soloed) << ' '
                << static_cast<unsigned int>(track.recordArmed) << ' '
                << track.patterns.size() << ' '
                << track.placements.size() << '\n';

            for (const auto& pattern : track.patterns) {
                out << "PATTERN " << std::quoted(pattern.id) << ' '
                    << std::quoted(pattern.name) << ' '
                    << pattern.lengthTicks << ' ' << pattern.notes.size() << '\n';
                for (const auto& note : pattern.notes) {
                    out << "NOTE " << note.tick << ' ' << note.durationTicks << ' '
                        << static_cast<unsigned int>(note.note) << ' '
                        << static_cast<unsigned int>(note.velocity) << ' '
                        << static_cast<unsigned int>(note.probability) << ' '
                        << static_cast<unsigned int>(note.ratchet) << ' '
                        << note.nudgeTicks << '\n';
                }
            }
            for (const auto& placement : track.placements) {
                out << "PLACEMENT " << std::quoted(placement.id) << ' '
                    << std::quoted(placement.patternId) << ' '
                    << placement.startTick << ' '
                    << placement.lengthTicks << ' '
                    << placement.sourceOffsetTicks << '\n';
            }
        }
    }

    out << "END_PROJECT\n";
    return out.str();
}

bool deserializeProject(
        std::string_view bytes,
        domain::Project& project,
        std::string* error) {
    if (error != nullptr) error->clear();
    if (bytes.size() > kMaxProjectFileBytes) {
        return setError(error, "Project data exceeds the supported file-size limit");
    }

    std::istringstream stream{std::string(bytes)};
    Decoder in(stream, error);
    std::string magic;
    int version = 0;
    if (!in.value(magic) || magic != "MPCMK2_PROJECT" || !in.value(version)) {
        return setError(error, "Invalid project header");
    }
    if (version != 1 && version != kCurrentProjectFormatVersion) {
        return setError(error, "Unsupported project format version");
    }

    domain::Project candidate;
    if (!in.tag("PROJECT") || !in.text(candidate.id) || !in.text(candidate.name)) return false;

    std::size_t sampleCount = 0;
    if (!in.tag("SAMPLES") || !in.count(sampleCount, kMaxSamples, "Sample")) return false;
    candidate.samples.reserve(sampleCount);
    for (std::size_t i = 0; i < sampleCount; ++i) {
        domain::SampleRef sample;
        if (!in.tag("SAMPLE")
                || !in.text(sample.id)
                || !in.text(sample.name)
                || !in.text(sample.path)
                || !in.finiteDouble(sample.sampleRate)
                || !in.value(sample.lengthSamples)
                || !in.value(sample.assetId.value)) {
            return false;
        }
        candidate.samples.push_back(std::move(sample));
    }

    std::size_t programCount = 0;
    if (!in.tag("PROGRAMS") || !in.count(programCount, kMaxPrograms, "Program")) return false;
    candidate.drumPrograms.reserve(programCount);
    for (std::size_t i = 0; i < programCount; ++i) {
        domain::DrumProgram program;
        unsigned int type = 0;
        if (!in.tag("PROGRAM")
                || !in.text(program.id)
                || !in.text(program.name)
                || !in.value(type)) {
            return false;
        }
        if (type > 3) return in.fail("Program type is out of range");
        program.type = static_cast<domain::ProgramType>(type);
        for (auto& pad : program.pads) {
            if (!readPad(in, pad)) return false;
        }
        candidate.drumPrograms.push_back(std::move(program));
    }

    std::size_t sequenceCount = 0;
    if (!in.tag("SEQUENCES") || !in.count(sequenceCount, kMaxSequences, "Sequence")) return false;
    candidate.sequences.reserve(sequenceCount);
    for (std::size_t i = 0; i < sequenceCount; ++i) {
        domain::Sequence sequence;
        std::size_t trackCount = 0;
        if (!in.tag("SEQUENCE")
                || !in.text(sequence.id)
                || !in.text(sequence.name)
                || !in.finiteDouble(sequence.tempoBpm)
                || !in.value(sequence.numerator)
                || !in.value(sequence.denominator)
                || !in.value(sequence.lengthTicks)
                || !in.boolean(sequence.loopEnabled)
                || !in.value(sequence.loopStartTicks)
                || !in.value(sequence.loopEndTicks)
                || !in.value(sequence.quantizeGridTicks)
                || !in.value(sequence.swingPercent)
                || !in.boolean(sequence.timingCorrectEnabled)
                || !in.boolean(sequence.metronomeEnabled)
                || !in.boolean(sequence.countInEnabled)) {
            return false;
        }
        for (auto& tick : sequence.locatorTicks) {
            if (!in.value(tick)) return false;
        }
        if (!in.count(trackCount, kMaxTracks, "Track")) return false;
        sequence.tracks.reserve(trackCount);

        for (std::size_t trackIndex = 0; trackIndex < trackCount; ++trackIndex) {
            domain::Track track;
            unsigned int type = 0;
            unsigned int kind = 0;
            std::size_t patternCount = 0;
            std::size_t placementCount = 0;
            if (!in.tag("TRACK")
                    || !in.text(track.id)
                    || !in.text(track.name)
                    || !in.value(type)
                    || !in.value(kind)
                    || !in.text(track.programId)
                    || !in.boolean(track.muted)
                    || !in.boolean(track.soloed)
                    || !in.boolean(track.recordArmed)
                    || !in.count(patternCount, kMaxPatternsPerTrack, "Pattern")) {
                return false;
            }
            if (type > 3 || kind > 4) return in.fail("Track type is out of range");
            if (version >= 2
                    && !in.count(placementCount, kMaxPlacementsPerTrack, "Pattern placement")) {
                return false;
            }
            track.type = static_cast<domain::ProgramType>(type);
            track.kind = static_cast<domain::TrackKind>(kind);
            track.patterns.reserve(patternCount);
            for (std::size_t patternIndex = 0; patternIndex < patternCount; ++patternIndex) {
                domain::Pattern pattern;
                if (!readPattern(in, pattern)) return false;
                track.patterns.push_back(std::move(pattern));
            }
            track.placements.reserve(placementCount);
            for (std::size_t placementIndex = 0; placementIndex < placementCount; ++placementIndex) {
                domain::PatternPlacement placement;
                if (!in.tag("PLACEMENT")
                        || !in.text(placement.id)
                        || !in.text(placement.patternId)
                        || !in.value(placement.startTick)
                        || !in.value(placement.lengthTicks)
                        || !in.value(placement.sourceOffsetTicks)) {
                    return false;
                }
                track.placements.push_back(std::move(placement));
            }
            sequence.tracks.push_back(std::move(track));
        }
        candidate.sequences.push_back(std::move(sequence));
    }

    if (!in.tag("END_PROJECT") || !in.finished()) return false;
    if (!validatePlacements(candidate, error)) return false;

    project = std::move(candidate);
    return true;
}

bool saveProjectFile(
        const domain::Project& project,
        const std::string& path,
        std::string* error) {
    if (error != nullptr) error->clear();
    if (path.empty()) return setError(error, "Project path is empty");
    if (!validatePlacements(project, error)) return false;

    const auto bytes = serializeProject(project, error);
    if (bytes.empty()) return false;
    if (bytes.size() > kMaxProjectFileBytes) {
        return setError(error, "Serialized project exceeds the supported file-size limit");
    }

    const std::string temporaryPath = path + ".tmp";
    std::remove(temporaryPath.c_str());
    {
        std::ofstream file(temporaryPath, std::ios::binary | std::ios::trunc);
        if (!file) return setError(error, "Could not open temporary project file for writing");
        file.write(bytes.data(), static_cast<std::streamsize>(bytes.size()));
        file.flush();
        if (!file) {
            file.close();
            std::remove(temporaryPath.c_str());
            return setError(error, "Failed while writing project file");
        }
    }

    if (std::rename(temporaryPath.c_str(), path.c_str()) != 0) {
        std::remove(temporaryPath.c_str());
        return setError(error, "Could not atomically replace the project file");
    }
    return true;
}

bool loadProjectFile(
        const std::string& path,
        domain::Project& project,
        std::string* error) {
    if (error != nullptr) error->clear();
    if (path.empty()) return setError(error, "Project path is empty");

    std::ifstream file(path, std::ios::binary);
    if (!file) return setError(error, "Could not open project file for reading");

    file.seekg(0, std::ios::end);
    const auto length = file.tellg();
    if (length < 0 || static_cast<std::uint64_t>(length) > kMaxProjectFileBytes) {
        return setError(error, "Project file has an invalid or excessive size");
    }
    file.seekg(0, std::ios::beg);
    std::string bytes(static_cast<std::size_t>(length), '\0');
    if (!bytes.empty()) {
        file.read(bytes.data(), static_cast<std::streamsize>(bytes.size()));
        if (!file) return setError(error, "Failed while reading project file");
    }
    return deserializeProject(bytes, project, error);
}

} // namespace mpc::persistence
