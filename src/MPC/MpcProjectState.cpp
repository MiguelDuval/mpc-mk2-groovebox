#include "MpcProjectState.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <limits>
#include <sstream>

namespace mpc {

namespace {

domain::Sequence makeDefaultSequence(std::size_t sequenceNumber) {
    domain::Sequence sequence;
    sequence.id = "sequence-" + std::to_string(sequenceNumber);
    sequence.name = "Sequence " + (
            sequenceNumber < 10
                    ? "0" + std::to_string(sequenceNumber)
                    : std::to_string(sequenceNumber));
    sequence.tempoBpm = 120.0;
    sequence.numerator = 4;
    sequence.denominator = 4;
    sequence.lengthTicks = sequencer::sequenceLengthForBars(
            4, sequence.numerator, sequence.denominator);
    sequence.loopEnabled = true;
    sequence.loopStartTicks = 0;
    sequence.loopEndTicks = sequence.lengthTicks;
    sequence.quantizeGridTicks = 240;
    sequence.swingPercent = 0;

    domain::Track drumTrack;
    drumTrack.id = sequence.id + "-track-1";
    drumTrack.name = "DRUMS";
    drumTrack.type = domain::ProgramType::Drum;
    drumTrack.kind = domain::TrackKind::Drum;
    drumTrack.programId = "drum-program-1";

    domain::Pattern pattern;
    pattern.id = drumTrack.id + "-pattern-1";
    pattern.name = "Pattern 01";
    pattern.lengthTicks = sequence.lengthTicks;
    drumTrack.patterns.push_back(std::move(pattern));
    domain::PatternPlacement initialPlacement;
    initialPlacement.id = drumTrack.id + "-placement-1";
    initialPlacement.patternId = drumTrack.patterns.front().id;
    initialPlacement.startTick = 0;
    initialPlacement.lengthTicks = sequence.lengthTicks;
    drumTrack.placements.push_back(std::move(initialPlacement));
    sequence.tracks.push_back(std::move(drumTrack));

    return sequence;
}


// Keep the native model internally valid when a user changes Sequence length.
// Notes beyond the new Pattern end are removed; note durations are bounded by
// the Pattern length. Placements that no longer intersect both the Sequence
// timeline and their source Pattern are removed or clipped, never left for the
// serializer or playback path to discover as invalid state.
void resizeSequenceContent(
        domain::Sequence& sequence,
        std::int32_t previousLength,
        std::int32_t newLength) {
    for (auto& track : sequence.tracks) {
        for (auto& pattern : track.patterns) {
            pattern.lengthTicks = newLength;
            auto& notes = pattern.notes;
            notes.erase(
                    std::remove_if(
                            notes.begin(),
                            notes.end(),
                            [newLength](const domain::MidiNoteEvent& note) {
                                return note.tick < 0 || note.tick >= newLength;
                            }),
                    notes.end());
            for (auto& note : notes) {
                if (note.durationTicks < 0) note.durationTicks = 0;
                if (note.durationTicks > newLength) note.durationTicks = newLength;
            }
        }

        for (auto& placement : track.placements) {
            if (placement.startTick == 0
                    && placement.lengthTicks == previousLength) {
                placement.lengthTicks = newLength;
            }
        }

        auto& placements = track.placements;
        placements.erase(
                std::remove_if(
                        placements.begin(),
                        placements.end(),
                        [&track, newLength](const domain::PatternPlacement& placement) {
                            if (placement.startTick < 0
                                    || placement.startTick >= newLength
                                    || placement.lengthTicks <= 0
                                    || placement.sourceOffsetTicks < 0) {
                                return true;
                            }
                            const auto pattern = std::find_if(
                                    track.patterns.begin(),
                                    track.patterns.end(),
                                    [&placement](const domain::Pattern& value) {
                                        return value.id == placement.patternId;
                                    });
                            return pattern == track.patterns.end()
                                    || pattern->lengthTicks <= 0
                                    || placement.sourceOffsetTicks >= pattern->lengthTicks;
                        }),
                placements.end());

        for (auto& placement : placements) {
            const auto pattern = std::find_if(
                    track.patterns.begin(),
                    track.patterns.end(),
                    [&placement](const domain::Pattern& value) {
                        return value.id == placement.patternId;
                    });
            // Invalid/missing-source placements have already been removed.
            if (pattern == track.patterns.end()) continue;
            const auto timelineAvailable = static_cast<std::int64_t>(newLength)
                    - placement.startTick;
            const auto sourceAvailable = static_cast<std::int64_t>(pattern->lengthTicks)
                    - placement.sourceOffsetTicks;
            placement.lengthTicks = static_cast<std::int32_t>(
                    std::min<std::int64_t>(
                            placement.lengthTicks,
                            std::min(timelineAvailable, sourceAvailable)));
        }
    }
}

} // namespace

MpcProjectState::MpcProjectState() {
    project_.id = "project-1";
    project_.name = "Untitled";

    domain::DrumProgram program;
    program.id = "drum-program-1";
    program.name = "Drum Program 1";
    program.type = domain::ProgramType::Drum;

    for (std::size_t pad = 0; pad < domain::kMaxProgramPads; ++pad) {
        program.pads[pad].index = static_cast<std::uint16_t>(pad);
        program.pads[pad].midiNote = static_cast<std::uint8_t>(36 + pad);
        program.pads[pad].name = "Pad " + std::to_string(pad + 1);
    }

    project_.drumPrograms.push_back(std::move(program));
    project_.sequences.push_back(makeDefaultSequence(1));
}


bool MpcProjectState::resetToDefault() {
    MpcProjectState defaults;
    return replaceProject(std::move(defaults.project_));
}

bool MpcProjectState::canReplaceProject(const domain::Project& project) noexcept {
    if (project.id.empty() || project.sequences.empty() || project.drumPrograms.empty()) {
        return false;
    }
    for (const auto& sequence : project.sequences) {
        if (sequence.id.empty() || sequence.tracks.empty()) return false;
        for (const auto& track : sequence.tracks) {
            if (track.id.empty() || track.patterns.empty()) return false;
        }
    }
    return true;
}

bool MpcProjectState::replaceProject(domain::Project project) {
    if (!canReplaceProject(project)) return false;
    project_ = std::move(project);
    activeSequenceIndex_ = 0;
    activeTrackIndex_ = 0;
    activeDrumProgramIndex_ = 0;
    const auto programIndex = activeProgramIndexForTrack(activeTrackIndex_);
    if (programIndex < project_.drumPrograms.size()) activeDrumProgramIndex_ = programIndex;

    std::uint32_t maximumId = 0;
    for (const auto& sample : project_.samples) maximumId = std::max(maximumId, sample.assetId.value);
    for (const auto& program : project_.drumPrograms) {
        for (const auto& pad : program.pads) {
            for (const auto& layer : pad.layers) maximumId = std::max(maximumId, layer.sample.value);
        }
    }
    if (maximumId != std::numeric_limits<std::uint32_t>::max()) {
        nextSampleId_ = maximumId + 1;
    } else {
        nextSampleId_ = 1;
        const auto used = [this](std::uint32_t candidate) {
            for (const auto& sample : project_.samples) if (sample.assetId.value == candidate) return true;
            for (const auto& program : project_.drumPrograms) {
                for (const auto& pad : program.pads) {
                    for (const auto& layer : pad.layers) if (layer.sample.value == candidate) return true;
                }
            }
            return false;
        };
        while (nextSampleId_ != 0 && used(nextSampleId_)) ++nextSampleId_;
    }
    return true;
}

bool MpcProjectState::selectSequence(std::size_t sequenceIndex) noexcept {
    if (sequenceIndex >= project_.sequences.size()) {
        return false;
    }

    activeSequenceIndex_ = sequenceIndex;
    activeTrackIndex_ = activeSequence().tracks.empty()
            ? 0
            : std::min(activeTrackIndex_, activeSequence().tracks.size() - 1);

    if (!activeSequence().tracks.empty()) {
        selectTrack(activeTrackIndex_);
    }
    return true;
}

bool MpcProjectState::selectTrack(std::size_t trackIndex) noexcept {
    if (trackIndex >= activeSequence().tracks.size()) {
        return false;
    }

    activeTrackIndex_ = trackIndex;
    const auto& track = activeSequence().tracks[trackIndex];
    if (track.kind == domain::TrackKind::Drum) {
        const auto index = activeProgramIndexForTrack(trackIndex);
        if (index < project_.drumPrograms.size()) {
            activeDrumProgramIndex_ = index;
        }
    }
    return true;
}

bool MpcProjectState::setTrackProgram(
        std::size_t trackIndex,
        std::string programId) {
    if (trackIndex >= activeSequence().tracks.size()) {
        return false;
    }

    const auto* program = findDrumProgram(programId);
    if (program == nullptr
            || program->type != domain::ProgramType::Drum) {
        return false;
    }

    auto& track = activeSequence().tracks[trackIndex];
    if (track.kind != domain::TrackKind::Drum
            || track.type != domain::ProgramType::Drum) {
        return false;
    }

    track.programId = program->id;
    if (trackIndex == activeTrackIndex_) {
        for (std::size_t i = 0; i < project_.drumPrograms.size(); ++i) {
            if (project_.drumPrograms[i].id == program->id) {
                activeDrumProgramIndex_ = i;
                break;
            }
        }
    }
    return true;
}

domain::DrumProgram* MpcProjectState::findDrumProgram(
        const std::string& programId) noexcept {
    for (auto& program : project_.drumPrograms) {
        if (program.id == programId) {
            return &program;
        }
    }
    return nullptr;
}

const domain::DrumProgram* MpcProjectState::findDrumProgram(
        const std::string& programId) const noexcept {
    for (const auto& program : project_.drumPrograms) {
        if (program.id == programId) {
            return &program;
        }
    }
    return nullptr;
}

std::size_t MpcProjectState::activeProgramIndexForTrack(
        std::size_t trackIndex) const noexcept {
    if (trackIndex >= activeSequence().tracks.size()) {
        return project_.drumPrograms.size();
    }

    const auto& track = activeSequence().tracks[trackIndex];
    if (track.programId.empty()) {
        return project_.drumPrograms.size();
    }

    for (std::size_t i = 0; i < project_.drumPrograms.size(); ++i) {
        if (project_.drumPrograms[i].id == track.programId) {
            return i;
        }
    }
    return project_.drumPrograms.size();
}

bool MpcProjectState::selectNextSequence() noexcept {
    if (project_.sequences.empty()) {
        return false;
    }
    return selectSequence(
            (activeSequenceIndex_ + 1) % project_.sequences.size());
}

bool MpcProjectState::selectPreviousSequence() noexcept {
    if (project_.sequences.empty()) {
        return false;
    }
    const auto count = project_.sequences.size();
    const auto previous = activeSequenceIndex_ == 0
            ? count - 1
            : activeSequenceIndex_ - 1;
    return selectSequence(previous);
}

bool MpcProjectState::addSequence(std::string name) {
    if (project_.sequences.size() >= domain::kMaxSequences) {
        return false;
    }

    const auto number = project_.sequences.size() + 1;
    auto sequence = makeDefaultSequence(number);
    if (!name.empty()) {
        sequence.name = std::move(name);
    }

    project_.sequences.push_back(std::move(sequence));
    activeSequenceIndex_ = project_.sequences.size() - 1;
    activeTrackIndex_ = 0;
    if (!activeSequence().tracks.empty()) {
        selectTrack(activeTrackIndex_);
    }
    return true;
}

bool MpcProjectState::setSequenceTempo(double tempoBpm) noexcept {
    if (!std::isfinite(tempoBpm) || tempoBpm < 20.0 || tempoBpm > 300.0) {
        return false;
    }
    activeSequence().tempoBpm = tempoBpm;
    return true;
}

bool MpcProjectState::setSequenceBars(std::int32_t bars) noexcept {
    if (bars < 1 || bars > 128) {
        return false;
    }

    const auto& sequence = activeSequence();
    const auto length = sequencer::sequenceLengthForBars(
            bars, sequence.numerator, sequence.denominator);
    if (length <= 0) {
        return false;
    }

    const auto previousLength = sequence.lengthTicks;
    const bool loopWasFullLength =
            sequence.loopStartTicks == 0
            && sequence.loopEndTicks == sequence.lengthTicks;

    auto& mutableSequence = activeSequence();
    mutableSequence.lengthTicks = length;
    if (loopWasFullLength
            || mutableSequence.loopEndTicks > length
            || mutableSequence.loopEndTicks <= 0) {
        mutableSequence.loopStartTicks = 0;
        mutableSequence.loopEndTicks = length;
    }
    mutableSequence.loopStartTicks =
            std::clamp(mutableSequence.loopStartTicks, 0, length - 1);
    // Malformed/legacy state can already contain a loop end before its start.
    // Keep the half-open range valid after resizing instead of carrying that
    // invalid range into the transport clock.
    if (mutableSequence.loopEndTicks <= mutableSequence.loopStartTicks) {
        mutableSequence.loopEndTicks = mutableSequence.loopStartTicks + 1;
    }

    resizeSequenceContent(mutableSequence, previousLength, length);

    if (activeTrackIndex_ >= mutableSequence.tracks.size()) {
        activeTrackIndex_ = mutableSequence.tracks.empty()
                ? 0
                : mutableSequence.tracks.size() - 1;
    }
    return true;
}

bool MpcProjectState::setSequenceTimeSignature(
        std::int32_t numerator,
        std::int32_t denominator) noexcept {
    if (!sequencer::isValidTimeSignature(numerator, denominator)) {
        return false;
    }

    const auto& oldSequence = activeSequence();
    const auto previousLength = oldSequence.lengthTicks;
    const auto bars = sequencer::sequenceBars(oldSequence);
    const auto oldPerBar = std::max(
            1,
            sequencer::barLengthTicks(
                    oldSequence.numerator,
                    oldSequence.denominator));
    const auto startBar = std::clamp<std::int64_t>(
            static_cast<std::int64_t>(oldSequence.loopStartTicks) / oldPerBar + 1,
            1,
            bars);
    // Promote before adding the bar-rounding offset. loopEndTicks can be near
    // INT32_MAX in a malformed/legacy Project, so int32 addition would overflow.
    const auto roundedEndBar =
            (static_cast<std::int64_t>(oldSequence.loopEndTicks)
                    + oldPerBar - 1) / oldPerBar;
    const auto endBar = std::clamp<std::int64_t>(
            roundedEndBar,
            startBar,
            bars);

    const auto length = sequencer::sequenceLengthForBars(
            bars, numerator, denominator);
    if (length <= 0) {
        return false;
    }

    auto& sequence = activeSequence();
    sequence.numerator = numerator;
    sequence.denominator = denominator;
    sequence.lengthTicks = length;

    const auto newPerBar = static_cast<std::int64_t>(
            sequencer::barLengthTicks(numerator, denominator));
    // Time-signature changes can expand the mapped loop beyond the clamped
    // int32 sequence length. Keep all products in int64 and clamp the resulting
    // half-open loop range back into the new Sequence.
    const auto proposedLoopStart = (startBar - 1) * newPerBar;
    const auto proposedLoopEnd = endBar * newPerBar;
    const auto newLoopStart = std::clamp<std::int64_t>(
            proposedLoopStart, 0, static_cast<std::int64_t>(length) - 1);
    const auto newLoopEnd = std::clamp<std::int64_t>(
            proposedLoopEnd, newLoopStart + 1, length);
    sequence.loopStartTicks = static_cast<std::int32_t>(newLoopStart);
    sequence.loopEndTicks = static_cast<std::int32_t>(newLoopEnd);

    resizeSequenceContent(sequence, previousLength, length);
    return true;
}

bool MpcProjectState::setSequenceLoop(
        bool enabled,
        std::int32_t startBar,
        std::int32_t endBar) noexcept {
    auto& sequence = activeSequence();
    if (sequence.lengthTicks <= 0) {
        return false;
    }

    const auto bars = sequencer::sequenceBars(sequence);
    const auto start = sequencer::clampLoopBar(startBar, bars);
    const auto end = sequencer::clampLoopBar(endBar, bars);
    if (start > end) {
        return false;
    }

    const auto perBar = sequencer::barLengthTicks(
            sequence.numerator,
            sequence.denominator);
    if (perBar <= 0) {
        return false;
    }

    // Compute before mutating state and promote before bar-to-tick products.
    // The last bar can extend a few ticks beyond INT32_MAX when a legacy
    // Sequence was clamped to that maximum length.
    const auto length = static_cast<std::int64_t>(sequence.lengthTicks);
    const auto proposedStart = static_cast<std::int64_t>(start - 1) * perBar;
    const auto proposedEnd = static_cast<std::int64_t>(end) * perBar;
    const auto loopStart = std::clamp<std::int64_t>(
            proposedStart, 0, length - 1);
    const auto loopEnd = std::clamp<std::int64_t>(
            proposedEnd, loopStart + 1, length);

    sequence.loopStartTicks = static_cast<std::int32_t>(loopStart);
    sequence.loopEndTicks = static_cast<std::int32_t>(loopEnd);
    sequence.loopEnabled = enabled;
    return true;
}

bool MpcProjectState::setSequenceQuantizeGrid(
        std::int32_t gridTicks) noexcept {
    switch (gridTicks) {
        case 60:
        case 120:
        case 240:
        case 480:
        case 960:
            activeSequence().quantizeGridTicks = gridTicks;
            return true;
        default:
            return false;
    }
}

bool MpcProjectState::setSequenceSwing(std::int32_t swingPercent) noexcept {
    if (swingPercent < 0 || swingPercent > 100) {
        return false;
    }
    activeSequence().swingPercent = swingPercent;
    return true;
}

bool MpcProjectState::addTrack(
        domain::TrackKind kind,
        std::string name) {
    auto& sequence = activeSequence();
    if (sequence.tracks.size() >= domain::kMaxSequenceTracks) {
        return false;
    }
    domain::Track track;
    const auto number = sequence.tracks.size() + 1;
    track.id = "track-" + std::to_string(number);
    track.name = name.empty()
            ? ("TRACK " + std::to_string(number))
            : std::move(name);
    track.kind = kind;

    switch (kind) {
        case domain::TrackKind::Drum:
            track.type = domain::ProgramType::Drum;
            if (!project_.drumPrograms.empty()) {
                const auto index = std::min(
                        activeDrumProgramIndex_,
                        project_.drumPrograms.size() - 1);
                track.programId = project_.drumPrograms[index].id;
            }
            break;
        case domain::TrackKind::Keygroup:
            track.type = domain::ProgramType::Keygroup;
            break;
        case domain::TrackKind::Plugin:
            track.type = domain::ProgramType::Plugin;
            break;
        case domain::TrackKind::Midi:
            track.type = domain::ProgramType::Audio;
            break;
        case domain::TrackKind::Audio:
            track.type = domain::ProgramType::Audio;
            break;
    }

    domain::Pattern pattern;
    pattern.id = track.id + "-pattern-1";
    pattern.name = "Pattern 01";
    pattern.lengthTicks = sequence.lengthTicks;
    track.patterns.push_back(std::move(pattern));
    domain::PatternPlacement initialPlacement;
    initialPlacement.id = track.id + "-placement-1";
    initialPlacement.patternId = track.patterns.front().id;
    initialPlacement.startTick = 0;
    initialPlacement.lengthTicks = sequence.lengthTicks;
    track.placements.push_back(std::move(initialPlacement));

    sequence.tracks.push_back(std::move(track));
    activeTrackIndex_ = sequence.tracks.size() - 1;
    selectTrack(activeTrackIndex_);
    return true;
}

std::string MpcProjectState::sequenceStatus() const {
    const auto& sequence = activeSequence();
    const auto bars = sequencer::sequenceBars(sequence);
    const auto loopStart = sequence.loopStartTicks
            / std::max(1, sequencer::barLengthTicks(
                    sequence.numerator, sequence.denominator));
    const auto loopEnd = (sequence.loopEndTicks
            + std::max(1, sequencer::barLengthTicks(
                    sequence.numerator, sequence.denominator)) - 1)
            / std::max(1, sequencer::barLengthTicks(
                    sequence.numerator, sequence.denominator));

    std::ostringstream out;
    out << "SEQ " << (activeSequenceIndex_ + 1) << "/" << project_.sequences.size()
        << "  " << sequence.name
        << "  | " << std::fixed << std::setprecision(1)
        << sequence.tempoBpm << " BPM"
        << "  | " << sequence.numerator << "/" << sequence.denominator
        << "  | " << bars << (bars == 1 ? " bar" : " bars")
        << "  | LOOP " << (sequence.loopEnabled ? "ON" : "OFF")
        << " " << (loopStart + 1) << "-" << std::max(loopEnd, loopStart + 1);
    return out.str();
}

std::string MpcProjectState::trackStatus(std::size_t trackIndex) const {
    const auto& tracks = activeSequence().tracks;
    if (trackIndex >= tracks.size()) {
        return "Track unavailable";
    }

    const auto& track = tracks[trackIndex];
    std::string kind;
    switch (track.kind) {
        case domain::TrackKind::Drum: kind = "DRUM"; break;
        case domain::TrackKind::Keygroup: kind = "KEYGROUP"; break;
        case domain::TrackKind::Plugin: kind = "PLUGIN"; break;
        case domain::TrackKind::Midi: kind = "MIDI"; break;
        case domain::TrackKind::Audio: kind = "AUDIO"; break;
    }

    const auto eventCount = track.patterns.empty()
            ? std::size_t{0}
            : track.patterns.front().notes.size();

    return kind + "  " + track.name
            + "  | events=" + std::to_string(eventCount)
            + "  | " + (track.recordArmed ? "ARM" : "—");
}

domain::SampleId MpcProjectState::registerSample(
        std::string name,
        std::string path,
        double sampleRate,
        std::int64_t lengthSamples) {
    if (nextSampleId_ == 0) {
        nextSampleId_ = 1;
    }

    const auto id = domain::SampleId{nextSampleId_++};

    domain::SampleRef ref;
    ref.id = "sample-" + std::to_string(id.value);
    ref.name = name.empty()
            ? ("Sample " + std::to_string(id.value))
            : std::move(name);
    ref.path = std::move(path);
    ref.sampleRate = sampleRate;
    ref.lengthSamples = lengthSamples;
    ref.assetId = id;
    project_.samples.push_back(std::move(ref));

    return id;
}

const domain::SampleRef* MpcProjectState::findSample(
        domain::SampleId id) const noexcept {
    if (!id.isAssigned()) {
        return nullptr;
    }

    for (const auto& sample : project_.samples) {
        if (sample.assetId.value == id.value) {
            return &sample;
        }
    }

    return nullptr;
}

} // namespace mpc
