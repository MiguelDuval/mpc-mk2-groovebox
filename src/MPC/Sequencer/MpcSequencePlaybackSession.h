#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencerPlayback.h"
#include "MPC/Sequencer/MpcSequenceSoloPolicy.h"
#include "MPC/Sequencer/MpcArrangementOps.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <memory>
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
            audio::AudioEngine& audio) noexcept
            : projectState_(projectState),
              audio_(audio) {
    }

    MpcSequencePlaybackSession(const MpcSequencePlaybackSession&) = delete;
    MpcSequencePlaybackSession& operator=(const MpcSequencePlaybackSession&) = delete;

    bool start(std::int64_t startPositionTicks = 0) {
        stop();

        const auto& sequence = projectState_.activeSequence();
        // Reserve before creating playback sessions: each session references
        // one stable sequence-length Pattern assembled from the track's clips.
        arrangementPatterns_.reserve(sequence.tracks.size());
        playbackTracks_.reserve(sequence.tracks.size());

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
                    audio_.triggerQueue(),
                    &track.muted,
                    &track.soloed,
                    &anySoloedPlayableTrack_));
            playbackTracks_.push_back(&track);
        }

        if (playbacks_.empty()) {
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
        playbackTracks_.clear();
        anySoloedPlayableTrack_ = false;
    }

    void reset() noexcept {
        for (auto& playback : playbacks_) {
            playback->reset();
        }
    }

    bool isPlaying() const noexcept {
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

        if (!playbacks_.empty()) {
            anySoloedPlayableTrack_ = hasSoloedPlaybackTrack(
                    std::span<const domain::Track* const>(
                            playbackTracks_.data(), playbackTracks_.size()));
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
        if (playbacks_.empty()) {
            return 0;
        }
        return playbacks_.front()->positionTicks();
    }

    void setPositionTicks(std::int64_t ticks) noexcept {
        for (auto& playback : playbacks_) {
            playback->setPositionTicks(ticks);
        }
    }

private:
    MpcProjectState& projectState_;
    audio::AudioEngine& audio_;
    std::vector<domain::Pattern> arrangementPatterns_;
    std::vector<const domain::Track*> playbackTracks_;
    std::vector<std::unique_ptr<MpcSequencerPlayback>> playbacks_;
    bool anySoloedPlayableTrack_ = false;
};

} // namespace mpc::sequencer
