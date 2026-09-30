#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencerPlayback.h"

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

        positionTicks_ = 0;
        playbacks_.clear();

        for (std::size_t trackIndex = 0;
             trackIndex < sequence.tracks.size();
             ++trackIndex) {
            const auto& track = sequence.tracks[trackIndex];
            if (track.kind != domain::TrackKind::Drum || track.patterns.empty()) {
                continue;
            }

            TrackPlayback trackPlayback;
            trackPlayback.trackIndex = trackIndex;
            trackPlayback.playback = std::make_unique<MpcSequencerPlayback>(
                    sequence,
                    track.patterns.front(),
                    program,
                    audio_.triggerQueue());
            trackPlayback.playing = false;
            playbacks_.push_back(std::move(trackPlayback));
        }

        if (playbacks_.empty()) {
            return false;
        }

        playing_ = true;
        syncTrackStates();
        return true;
    }

    void stop() noexcept {
        for (auto& trackPlayback : playbacks_) {
            if (trackPlayback.playback != nullptr) {
                trackPlayback.playback->stop();
            }
            trackPlayback.playing = false;
        }
        playbacks_.clear();
        playing_ = false;
    }

    void reset() noexcept {
        positionTicks_ = 0;
        for (auto& trackPlayback : playbacks_) {
            if (trackPlayback.playback == nullptr) {
                continue;
            }
            trackPlayback.playback->reset();
            trackPlayback.playback->setPositionTicks(0);
        }
        if (playing_) {
            syncTrackStates();
        }
    }

    bool isPlaying() const noexcept {
        return playing_;
    }

    bool didWrap() const noexcept {
        return wrapped_;
    }

    SequencePlaybackAggregate advance(
            std::int64_t deltaTicks,
            std::uint32_t seed,
            std::int32_t sampleRate) noexcept {
        SequencePlaybackAggregate result;
        if (!playing_ || deltaTicks <= 0) {
            return result;
        }

        wrapped_ = false;
        syncTrackStates();

        for (auto& trackPlayback : playbacks_) {
            if (!trackPlayback.playing || trackPlayback.playback == nullptr) {
                continue;
            }
            const auto value = trackPlayback.playback->advance(
                    deltaTicks,
                    seed,
                    sampleRate);
            result.scheduled += value.scheduled;
            result.routed += value.routed;
            result.queued += value.queued;
            result.dropped += value.dropped;
            result.invalid += value.invalid;
        }

        const auto& sequence = projectState_.activeSequence();
        const auto length = std::max<std::int64_t>(1, sequence.lengthTicks);
        if (sequence.loopEnabled) {
            const auto advancedPosition = positionTicks_ + deltaTicks;
            wrapped_ = advancedPosition >= length;
            positionTicks_ = advancedPosition % length;
        } else {
            positionTicks_ = std::min(length - 1, positionTicks_ + deltaTicks);
            if (positionTicks_ >= length - 1) {
                playing_ = false;
                for (auto& trackPlayback : playbacks_) {
                    if (trackPlayback.playback != nullptr) {
                        trackPlayback.playback->stop();
                    }
                    trackPlayback.playing = false;
                }
            }
        }

        for (auto& trackPlayback : playbacks_) {
            if (!trackPlayback.playing && trackPlayback.playback != nullptr) {
                trackPlayback.playback->setPositionTicks(positionTicks_);
            }
        }

        return result;
    }

    std::int64_t positionTicks() const noexcept {
        return positionTicks_;
    }

private:
    struct TrackPlayback final {
        std::size_t trackIndex = 0;
        std::unique_ptr<MpcSequencerPlayback> playback;
        bool playing = false;
    };

    bool audibleTrack(const domain::Track& track, bool anySolo) const noexcept {
        if (track.muted) {
            return false;
        }
        if (anySolo && !track.soloed) {
            return false;
        }
        return true;
    }

    void syncTrackStates() noexcept {
        bool anySolo = false;
        for (const auto& track : projectState_.activeSequence().tracks) {
            anySolo = anySolo || track.soloed;
        }

        for (auto& trackPlayback : playbacks_) {
            if (trackPlayback.playback == nullptr
                    || trackPlayback.trackIndex >= projectState_.activeSequence().tracks.size()) {
                continue;
            }

            const auto& track =
                    projectState_.activeSequence().tracks[trackPlayback.trackIndex];
            const bool shouldPlay = audibleTrack(track, anySolo);

            if (shouldPlay && !trackPlayback.playing) {
                trackPlayback.playback->setPositionTicks(positionTicks_);
                trackPlayback.playback->start();
                trackPlayback.playing = true;
            } else if (!shouldPlay && trackPlayback.playing) {
                trackPlayback.playback->stop();
                trackPlayback.playing = false;
                trackPlayback.playback->setPositionTicks(positionTicks_);
            }
        }
    }

    MpcProjectState& projectState_;
    audio::AudioEngine& audio_;
    std::vector<TrackPlayback> playbacks_;
    std::int64_t positionTicks_ = 0;
    bool playing_ = false;
    bool wrapped_ = false;
};

} // namespace mpc::sequencer
