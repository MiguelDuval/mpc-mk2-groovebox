#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencerPlayback.h"
#include "MPC/Sequencer/MpcTrackPerformance.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <memory>
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

    bool start() {
        stop();

        const auto& sequence = projectState_.activeSequence();
        const auto& program = projectState_.activeDrumProgram();

        for (std::size_t trackIndex = 0;
             trackIndex < sequence.tracks.size();
             ++trackIndex) {
            const auto& track = sequence.tracks[trackIndex];
            if (track.patterns.empty()
                    || track.kind != domain::TrackKind::Drum) {
                continue;
            }

            playbacks_.push_back(std::make_unique<MpcSequencerPlayback>(
                    sequence,
                    track.patterns.front(),
                    program,
                    audio_.triggerQueue()));
            playbackTrackIndices_.push_back(trackIndex);
        }

        if (playbacks_.empty()) {
            return false;
        }

        for (auto& playback : playbacks_) {
            playback->reset();
            playback->start();
        }
        return true;
    }

    void stop() noexcept {
        for (auto& playback : playbacks_) {
            playback->stop();
        }
        playbacks_.clear();
        playbackTrackIndices_.clear();
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
        const auto& tracks = projectState_.activeSequence().tracks;
        const auto anySolo = anyTrackSoloed(
                std::span<const domain::Track>(tracks.data(), tracks.size()));

        for (std::size_t playbackIndex = 0;
             playbackIndex < playbacks_.size();
             ++playbackIndex) {
            const auto trackIndex = playbackTrackIndices_[playbackIndex];
            if (trackIndex >= tracks.size()
                    || !shouldScheduleTrack(tracks[trackIndex], anySolo)) {
                continue;
            }

            auto& playback = playbacks_[playbackIndex];
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

private:
    MpcProjectState& projectState_;
    audio::AudioEngine& audio_;
    std::vector<std::unique_ptr<MpcSequencerPlayback>> playbacks_;
    std::vector<std::size_t> playbackTrackIndices_;
};

} // namespace mpc::sequencer
