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
        projectGeneration_ = projectState_.projectGeneration();
        sequenceIndex_ = projectState_.activeSequenceIndex();
        sequenceId_ = sequence.id;
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
    bool playbackContextMatches() const noexcept {
        if (!contextCaptured_
                || projectState_.projectGeneration() != projectGeneration_
                || projectState_.activeSequenceIndex() != sequenceIndex_) {
            return false;
        }
        const auto& sequences = projectState_.project().sequences;
        return sequenceIndex_ < sequences.size()
                && sequences[sequenceIndex_].id == sequenceId_;
    }

    MpcProjectState& projectState_;
    audio::AudioTriggerQueue& audioQueue_;
    std::uint64_t projectGeneration_ = 0;
    std::size_t sequenceIndex_ = 0;
    std::string sequenceId_;
    bool contextCaptured_ = false;
    std::vector<domain::Pattern> arrangementPatterns_;
    std::vector<std::unique_ptr<MpcSequencerPlayback>> playbacks_;
};

} // namespace mpc::sequencer
