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

        std::size_t expandedWritten = 0;
        for (std::size_t index = 0; index < schedule.written; ++index) {
            const auto& scheduled = scheduledEvents_[index];
            const auto count = scheduled.ratchetCount == 0
                    ? std::size_t{1}
                    : static_cast<std::size_t>(scheduled.ratchetCount);

            if (count == 1) {
                if (expandedWritten >= expandedScheduledEvents_.size()) {
                    result.schedulingTruncated = true;
                    continue;
                }
                expandedScheduledEvents_[expandedWritten++] = scheduled;
                continue;
            }

            const auto expanded = expandScheduledRatchets(
                    std::span<const ScheduledMidiEvent>(&scheduled, 1),
                    scheduled.durationTicks,
                    std::span<ScheduledMidiEvent>(
                            expandedScheduledEvents_.data() + expandedWritten,
                            expandedScheduledEvents_.size() - expandedWritten));
            result.invalid += expanded.invalid ? 1 : 0;
            result.schedulingTruncated =
                    result.schedulingTruncated || expanded.truncated;
            expandedWritten += expanded.written;
        }

        const auto route = routeScheduledEventsToPads(
                program_,
                std::span<const ScheduledMidiEvent>(
                        expandedScheduledEvents_.data(),
                        expandedWritten),
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
    std::array<ScheduledMidiEvent, kMaxPlaybackEvents> expandedScheduledEvents_{};
    std::array<ScheduledPadEvent, kMaxPlaybackEvents> routedEvents_{};
};

} // namespace mpc::sequencer
