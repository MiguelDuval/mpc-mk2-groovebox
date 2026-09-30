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

    bool start() {
        stop();

        const auto& sequence = projectState_.activeSequence();
        const auto& program = projectState_.activeDrumProgram();

        for (const auto& track : sequence.tracks) {
            if (track.muted || track.patterns.empty()) {
                continue;
            }
            if (track.kind != domain::TrackKind::Drum) {
                continue;
            }

            playbacks_.push_back(std::make_unique<MpcSequencerPlayback>(
                    sequence,
                    track.patterns.front(),
                    program,
                    audio_.triggerQueue()));
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
        if (!playbacks_.empty()
                && deltaTicks > 0
                && playbacks_.front()->isPlaying()) {
            const auto& sequence = projectState_.activeSequence();
            if (sequence.loopEnabled) {
                const auto loopLength =
                        std::max<std::int64_t>(
                                1,
                                static_cast<std::int64_t>(
                                        sequence.loopEndTicks)
                                - static_cast<std::int64_t>(
                                        sequence.loopStartTicks));
                const auto position = playbacks_.front()->positionTicks();
                const auto loopEnd = std::clamp<std::int64_t>(
                        sequence.loopEndTicks,
                        sequence.loopStartTicks + 1,
                        std::max<std::int64_t>(1, sequence.lengthTicks));
                result.wrapped =
                        deltaTicks >= std::max<std::int64_t>(
                                1,
                                loopEnd - position)
                        || deltaTicks >= loopLength;
            }
        }

        for (auto& playback : playbacks_) {
            const auto value = playback->advance(
                    deltaTicks, seed, sampleRate);
            result.scheduled += value.scheduled;
            result.routed += value.routed;
            result.queued += value.queued;
            result.dropped += value.dropped;
            result.invalid += value.invalid;
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
};

} // namespace mpc::sequencer
