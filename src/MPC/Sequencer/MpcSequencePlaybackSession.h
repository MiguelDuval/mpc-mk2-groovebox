#pragma once

#include "Audio/AudioTriggerQueue.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencerPlayback.h"
#include "MPC/Sequencer/MpcArrangementOps.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <string>
#include <utility>
#include <vector>

namespace mpc::sequencer {

struct SequencePlaybackAggregate final {
    std::size_t scheduled = 0;
    std::size_t routed = 0;
    std::size_t queued = 0;
    std::size_t dropped = 0;
    std::size_t invalid = 0;
    bool wrapped = false;
};

class MpcSequencePlaybackSession final {
public:
    MpcSequencePlaybackSession(
            MpcProjectState& projectState,
            audio::AudioTriggerQueue& audioQueue) noexcept
            : projectState_(projectState),
              audioQueue_(audioQueue) {
    }

    MpcSequencePlaybackSession(const MpcSequencePlaybackSession&) = delete;
    MpcSequencePlaybackSession& operator=(const MpcSequencePlaybackSession&) = delete;

    bool start(std::int64_t startPositionTicks = 0) {
        stop();

        const auto& sequence = projectState_.activeSequence();
        const auto& project = projectState_.project();
        projectGeneration_ = projectState_.projectGeneration();
        sequenceIndex_ = projectState_.activeSequenceIndex();
        sequenceId_ = sequence.id;

        // Playback objects hold references into the Sequence, DrumProgram and
        // Track vectors. Project replacement is covered by projectGeneration_,
        // but adding a Track or DrumProgram can reallocate a vector without
        // replacing the Project. Capture the storage and the relevant shape so
        // advance() invalidates the session before touching any stale reference.
        sequenceStorage_ = project.sequences.data();
        sequenceCount_ = project.sequences.size();
        trackStorage_ = sequence.tracks.data();
        trackCount_ = sequence.tracks.size();
        programStorage_ = project.drumPrograms.data();
        programCount_ = project.drumPrograms.size();
        sequenceLengthTicks_ = sequence.lengthTicks;
        sequenceNumerator_ = sequence.numerator;
        sequenceDenominator_ = sequence.denominator;
        sequenceLoopEnabled_ = sequence.loopEnabled;
        sequenceLoopStartTicks_ = sequence.loopStartTicks;
        sequenceLoopEndTicks_ = sequence.loopEndTicks;
        trackSignatures_.clear();
        trackSignatures_.reserve(sequence.tracks.size());
        for (const auto& track : sequence.tracks) {
            trackSignatures_.push_back(TrackSignature{
                    track.id,
                    track.programId,
                    track.kind,
                    track.patterns.size(),
                    track.placements.size()});
        }
        contextCaptured_ = true;
        // Reserve before creating playback sessions: each session references
        // one stable sequence-length Pattern assembled from the track's clips.
        arrangementPatterns_.reserve(sequence.tracks.size());
        playbacks_.reserve(sequence.tracks.size());

        for (const auto& track : sequence.tracks) {
            if (track.patterns.empty()) {
                continue;
            }
            if (track.kind != domain::TrackKind::Drum) {
                continue;
            }

            const auto* program =
                    projectState_.findDrumProgram(track.programId);
            if (program == nullptr) {
                continue;
            }

            domain::Pattern arrangedPattern;
            if (!buildArrangementPlaybackPattern(
                    sequence, track, arrangedPattern)) {
                continue;
            }
            arrangementPatterns_.push_back(std::move(arrangedPattern));
            playbacks_.push_back(std::make_unique<MpcSequencerPlayback>(
                    sequence,
                    arrangementPatterns_.back(),
                    *program,
                    audioQueue_,
                    &track.muted));
        }

        if (playbacks_.empty()) {
            stop();
            return false;
        }

        for (auto& playback : playbacks_) {
            playback->reset();
            playback->setPositionTicks(startPositionTicks);
            playback->start();
        }
        return true;
    }

    void stop() noexcept {
        for (auto& playback : playbacks_) {
            playback->stop();
        }
        playbacks_.clear();
        arrangementPatterns_.clear();
        contextCaptured_ = false;
        projectGeneration_ = 0;
        sequenceIndex_ = 0;
        sequenceId_.clear();
        sequenceStorage_ = nullptr;
        sequenceCount_ = 0;
        trackStorage_ = nullptr;
        trackCount_ = 0;
        programStorage_ = nullptr;
        programCount_ = 0;
        sequenceLengthTicks_ = 0;
        sequenceNumerator_ = 0;
        sequenceDenominator_ = 0;
        sequenceLoopEnabled_ = false;
        sequenceLoopStartTicks_ = 0;
        sequenceLoopEndTicks_ = 0;
        trackSignatures_.clear();
    }

    void reset() noexcept {
        if (!playbackContextMatches()) {
            stop();
            return;
        }
        for (auto& playback : playbacks_) {
            playback->reset();
        }
    }

    bool isPlaying() const noexcept {
        if (!playbackContextMatches()) {
            return false;
        }
        for (const auto& playback : playbacks_) {
            if (playback->isPlaying()) {
                return true;
            }
        }
        return false;
    }

    SequencePlaybackAggregate advance(
            std::int64_t deltaTicks,
            std::uint32_t seed,
            std::int32_t sampleRate) noexcept {
        SequencePlaybackAggregate result;
        if (!playbackContextMatches()) {
            stop();
            return result;
        }

        for (auto& playback : playbacks_) {
            const auto value = playback->advance(
                    deltaTicks, seed, sampleRate);
            result.scheduled += value.scheduled;
            result.routed += value.routed;
            result.queued += value.queued;
            result.dropped += value.dropped;
            result.invalid += value.invalid;
            result.wrapped = result.wrapped || value.wrapped;
        }
        return result;
    }

    std::int64_t positionTicks() const noexcept {
        if (!playbackContextMatches() || playbacks_.empty()) {
            return 0;
        }
        return playbacks_.front()->positionTicks();
    }

    void setPositionTicks(std::int64_t ticks) noexcept {
        if (!playbackContextMatches()) {
            stop();
            return;
        }
        for (auto& playback : playbacks_) {
            playback->setPositionTicks(ticks);
        }
    }

private:
    struct TrackSignature final {
        std::string id;
        std::string programId;
        domain::TrackKind kind;
        std::size_t patternCount;
        std::size_t placementCount;
    };

    bool playbackContextMatches() const noexcept {
        if (!contextCaptured_
                || projectState_.projectGeneration() != projectGeneration_
                || projectState_.activeSequenceIndex() != sequenceIndex_) {
            return false;
        }

        const auto& project = projectState_.project();
        const auto& sequences = project.sequences;
        const auto& programs = project.drumPrograms;
        if (sequences.data() != sequenceStorage_
                || sequences.size() != sequenceCount_
                || programs.data() != programStorage_
                || programs.size() != programCount_
                || sequenceIndex_ >= sequences.size()) {
            return false;
        }

        const auto& sequence = sequences[sequenceIndex_];
        if (sequence.id != sequenceId_
                || sequence.tracks.data() != trackStorage_
                || sequence.tracks.size() != trackCount_
                || sequence.lengthTicks != sequenceLengthTicks_
                || sequence.numerator != sequenceNumerator_
                || sequence.denominator != sequenceDenominator_
                || sequence.loopEnabled != sequenceLoopEnabled_
                || sequence.loopStartTicks != sequenceLoopStartTicks_
                || sequence.loopEndTicks != sequenceLoopEndTicks_
                || trackSignatures_.size() != sequence.tracks.size()) {
            return false;
        }

        for (std::size_t index = 0; index < trackSignatures_.size(); ++index) {
            const auto& track = sequence.tracks[index];
            const auto& signature = trackSignatures_[index];
            if (track.id != signature.id
                    || track.programId != signature.programId
                    || track.kind != signature.kind
                    || track.patterns.size() != signature.patternCount
                    || track.placements.size() != signature.placementCount) {
                return false;
            }
        }
        return true;
    }

    MpcProjectState& projectState_;
    audio::AudioTriggerQueue& audioQueue_;
    std::uint64_t projectGeneration_ = 0;
    std::size_t sequenceIndex_ = 0;
    std::string sequenceId_;
    const domain::Sequence* sequenceStorage_ = nullptr;
    std::size_t sequenceCount_ = 0;
    const domain::Track* trackStorage_ = nullptr;
    std::size_t trackCount_ = 0;
    const domain::DrumProgram* programStorage_ = nullptr;
    std::size_t programCount_ = 0;
    std::int32_t sequenceLengthTicks_ = 0;
    std::int32_t sequenceNumerator_ = 0;
    std::int32_t sequenceDenominator_ = 0;
    bool sequenceLoopEnabled_ = false;
    std::int32_t sequenceLoopStartTicks_ = 0;
    std::int32_t sequenceLoopEndTicks_ = 0;
    std::vector<TrackSignature> trackSignatures_;
    bool contextCaptured_ = false;
    std::vector<domain::Pattern> arrangementPatterns_;
    std::vector<std::unique_ptr<MpcSequencerPlayback>> playbacks_;
};

} // namespace mpc::sequencer
