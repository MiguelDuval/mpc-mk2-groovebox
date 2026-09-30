#include "MpcProjectState.h"

#include <algorithm>
#include <cmath>
#include <iomanip>
#include <iterator>
#include <sstream>

namespace mpc {

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

    domain::Sequence sequence;
    sequence.id = "sequence-1";
    sequence.name = "Sequence 01";
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

    constexpr const char* kDefaultTrackNames[] = {
            "DRUM KIT", "SYNTH 01", "MIDI EXT 01", "AUDIO 01"
    };
    constexpr domain::TrackKind kDefaultTrackKinds[] = {
            domain::TrackKind::Drum,
            domain::TrackKind::Plugin,
            domain::TrackKind::Midi,
            domain::TrackKind::Audio
    };

    for (std::size_t trackIndex = 0;
            trackIndex < std::size(kDefaultTrackNames);
            ++trackIndex) {
        domain::Track track;
        track.id = "track-" + std::to_string(trackIndex + 1);
        track.name = kDefaultTrackNames[trackIndex];
        track.kind = kDefaultTrackKinds[trackIndex];

        switch (track.kind) {
            case domain::TrackKind::Drum:
                track.type = domain::ProgramType::Drum;
                track.programId = "drum-program-1";
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
        sequence.tracks.push_back(std::move(track));
    }

    project_.sequences.push_back(std::move(sequence));
}

bool MpcProjectState::setTrackMuted(
        std::size_t trackIndex,
        bool muted) noexcept {
    if (trackIndex >= activeSequence().tracks.size()) {
        return false;
    }
    activeSequence().tracks[trackIndex].muted = muted;
    return true;
}

bool MpcProjectState::setTrackSoloed(
        std::size_t trackIndex,
        bool soloed) noexcept {
    if (trackIndex >= activeSequence().tracks.size()) {
        return false;
    }

    auto& tracks = activeSequence().tracks;
    if (soloed) {
        for (auto& track : tracks) {
            track.soloed = false;
        }
    }
    tracks[trackIndex].soloed = soloed;
    return true;
}

bool MpcProjectState::setTrackArmed(
        std::size_t trackIndex,
        bool armed) noexcept {
    if (trackIndex >= activeSequence().tracks.size()) {
        return false;
    }

    auto& tracks = activeSequence().tracks;
    if (armed) {
        for (auto& track : tracks) {
            track.recordArmed = false;
        }
    }
    tracks[trackIndex].recordArmed = armed;
    return true;
}

bool MpcProjectState::addSequence(std::string name) {
    if (project_.sequences.size() >= domain::kMaxSequences) {
        return false;
    }

    const auto& source = activeSequence();
    domain::Sequence sequence;
    const auto number = project_.sequences.size() + 1;
    sequence.id = "sequence-" + std::to_string(number);
    sequence.name = name.empty()
            ? (std::string("Sequence ") + (number < 10 ? "0" : "") + std::to_string(number))
            : std::move(name);
    sequence.tempoBpm = source.tempoBpm;
    sequence.numerator = source.numerator;
    sequence.denominator = source.denominator;
    sequence.lengthTicks = source.lengthTicks;
    sequence.loopEnabled = source.loopEnabled;
    sequence.loopStartTicks = source.loopStartTicks;
    sequence.loopEndTicks = source.loopEndTicks;
    sequence.quantizeGridTicks = source.quantizeGridTicks;
    sequence.swingPercent = source.swingPercent;
    sequence.metronomeEnabled = source.metronomeEnabled;
    sequence.countInEnabled = source.countInEnabled;

    for (const auto& sourceTrack : source.tracks) {
        domain::Track track = sourceTrack;
        track.muted = false;
        track.soloed = false;
        track.recordArmed = false;
        for (auto& pattern : track.patterns) {
            pattern.notes.clear();
        }
        sequence.tracks.push_back(std::move(track));
    }

    project_.sequences.push_back(std::move(sequence));
    activeSequenceIndex_ = project_.sequences.size() - 1;
    activeTrackIndex_ = 0;
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

    for (auto& track : mutableSequence.tracks) {
        for (auto& pattern : track.patterns) {
            pattern.lengthTicks = length;
        }
    }

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
    const auto bars = sequencer::sequenceBars(oldSequence);
    const auto oldPerBar = std::max(
            1,
            sequencer::barLengthTicks(
                    oldSequence.numerator,
                    oldSequence.denominator));
    const auto startBar = std::clamp(
            oldSequence.loopStartTicks / oldPerBar + 1,
            1,
            bars);
    const auto endBar = std::clamp(
            (oldSequence.loopEndTicks + oldPerBar - 1) / oldPerBar,
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

    const auto newPerBar = sequencer::barLengthTicks(numerator, denominator);
    sequence.loopStartTicks = (startBar - 1) * newPerBar;
    sequence.loopEndTicks = std::min(
            length,
            endBar * newPerBar);

    for (auto& track : sequence.tracks) {
        for (auto& pattern : track.patterns) {
            pattern.lengthTicks = length;
        }
    }
    return true;
}

bool MpcProjectState::setSequenceLoop(
        bool enabled,
        std::int32_t startBar,
        std::int32_t endBar) noexcept {
    auto& sequence = activeSequence();
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

    sequence.loopEnabled = enabled;
    sequence.loopStartTicks = (start - 1) * perBar;
    sequence.loopEndTicks =
            std::min(
                    sequence.lengthTicks,
                    end * perBar);
    if (sequence.loopEndTicks <= sequence.loopStartTicks) {
        return false;
    }
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
            track.programId = "drum-program-1";
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

    sequence.tracks.push_back(std::move(track));
    activeTrackIndex_ = sequence.tracks.size() - 1;
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
    out << "SEQ " << (activeSequenceIndex_ + 1) << "  " << sequence.name
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
        case domain::TrackKind::Drum: kind = "SAMPLE"; break;
        case domain::TrackKind::Keygroup: kind = "SYNTH"; break;
        case domain::TrackKind::Plugin: kind = "SYNTH"; break;
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
