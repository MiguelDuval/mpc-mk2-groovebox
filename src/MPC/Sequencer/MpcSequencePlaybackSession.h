#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencerPlayback.h"

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

    bool start(std::int64_t startPositionTicks = 0) {
        stop();

        const auto& sequence = projectState_.activeSequence();
        const auto& program = projectState_.activeDrumProgram();

        for (const auto& track : sequence.tracks) {
            if (track.patterns.empty()) {
                continue;
            }
            if (track.kind != domain::TrackKind::Drum) {
                continue;
            }

            playbacks_.push_back(std::make_unique<MpcSequencerPlayback>(
                    sequence,
                    track.patterns.front(),
                    program,
                    audio_.triggerQueue(),
                    &track.muted));
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
    std::vector<std::unique_ptr<MpcSequencerPlayback>> playbacks_;
};

} // namespace mpc::sequencer
