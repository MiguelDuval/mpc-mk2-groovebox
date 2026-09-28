#pragma once

#include "Audio/AudioTriggerQueue.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/Sequencer/MpcSequencerAudioBridge.h"
#include "MPC/Sequencer/MpcSequencerPadRouter.h"
#include "MPC/Sequencer/MpcSequencerRuntime.h"

#include <array>
#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

struct SequencerPlaybackResult final {
    std::size_t scheduled = 0;
    std::size_t routed = 0;
    std::size_t unmapped = 0;
    std::size_t queued = 0;
    std::size_t dropped = 0;
    std::size_t invalid = 0;
    bool schedulingTruncated = false;
};

class MpcSequencerPlayback final {
public:
    MpcSequencerPlayback(
            const domain::Sequence& sequence,
            const domain::Pattern& pattern,
            const domain::DrumProgram& program,
            audio::AudioTriggerQueue& audioQueue) noexcept
            : sequence_(sequence),
              program_(program),
              runtime_(sequence, pattern),
              audioQueue_(audioQueue) {
    }

    MpcSequencerPlayback(const MpcSequencerPlayback&) = delete;
    MpcSequencerPlayback& operator=(const MpcSequencerPlayback&) = delete;

    void start() noexcept {
        runtime_.start();
    }

    void stop() noexcept {
        runtime_.stop();
    }

    void reset() noexcept {
        runtime_.reset();
    }

    [[nodiscard]] bool isPlaying() const noexcept {
        return runtime_.isPlaying();
    }

    [[nodiscard]] std::int64_t positionTicks() const noexcept {
        return runtime_.positionTicks();
    }

    SequencerPlaybackResult advance(
            std::int64_t deltaTicks,
            std::uint32_t seed,
            std::int32_t sampleRate) noexcept {
        SequencerPlaybackResult result;

        if (sampleRate <= 0) {
            result.invalid = 1;
            return result;
        }

        const auto schedule = runtime_.advance(
                deltaTicks,
                seed,
                scheduledEvents_);
        result.scheduled = schedule.written;
        result.schedulingTruncated = schedule.truncated;

        const auto route = routeScheduledEventsToPads(
                program_,
                std::span<const ScheduledMidiEvent>(
                        scheduledEvents_.data(),
                        schedule.written),
                routedEvents_);
        result.routed = route.routed;
        result.unmapped = route.unmapped;

        const auto bridge = enqueueScheduledPadEvents(
                audioQueue_,
                std::span<const ScheduledPadEvent>(
                        routedEvents_.data(),
                        route.written),
                sequence_.tempoBpm,
                sampleRate);
        result.queued = bridge.written;
        result.dropped = bridge.dropped;
        result.invalid += bridge.invalid;

        return result;
    }

private:
    static constexpr std::size_t kMaxPlaybackEvents =
            audio::kAudioTriggerQueueCapacity;

    const domain::Sequence& sequence_;
    const domain::DrumProgram& program_;
    MpcSequencerRuntime runtime_;
    audio::AudioTriggerQueue& audioQueue_;
    std::array<ScheduledMidiEvent, kMaxPlaybackEvents> scheduledEvents_{};
    std::array<ScheduledPadEvent, kMaxPlaybackEvents> routedEvents_{};
};

} // namespace mpc::sequencer
