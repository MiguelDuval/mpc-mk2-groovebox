#pragma once

#include "Audio/AudioTriggerQueue.h"
#include "MPC/Sequencer/MpcSequencerPadRouter.h"
#include "MPC/Sequencer/MpcTickFrameConverter.h"

#include <cstddef>
#include <cstdint>
#include <limits>
#include <span>

namespace mpc::sequencer {

struct AudioBridgeResult final {
    std::size_t input = 0;
    std::size_t written = 0;
    std::size_t dropped = 0;
    std::size_t invalid = 0;
};

[[nodiscard]] inline AudioBridgeResult enqueueScheduledPadEventsAsFrames(
        audio::AudioTriggerQueue& queue,
        std::span<const ScheduledPadEvent> input,
        std::span<const std::int32_t> offsetFrames) noexcept {
    AudioBridgeResult result;
    result.input = input.size();

    if (offsetFrames.size() != input.size()) {
        result.invalid = input.size();
        return result;
    }

    for (std::size_t index = 0; index < input.size(); ++index) {
        const auto& event = input[index];
        const auto frameOffset = offsetFrames[index];

        if (frameOffset < 0
                || event.velocity == 0
                || event.padIndex > std::numeric_limits<std::uint8_t>::max()) {
            ++result.invalid;
            continue;
        }

        const audio::AudioTriggerEvent trigger{
            static_cast<std::uint8_t>(event.padIndex),
            event.velocity,
            frameOffset};

        if (queue.tryEnqueue(trigger)) {
            ++result.written;
        } else {
            ++result.dropped;
        }
    }

    return result;
}



[[nodiscard]] inline AudioBridgeResult enqueueScheduledPadEvents(
        audio::AudioTriggerQueue& queue,
        std::span<const ScheduledPadEvent> input,
        double tempoBpm,
        std::int32_t sampleRate) noexcept {
    AudioBridgeResult result;
    result.input = input.size();

    for (const auto& event : input) {
        const auto frameOffset =
                ticksToFrames(event.offsetTicks, tempoBpm, sampleRate);

        if (!frameOffset.has_value()
                || event.velocity == 0
                || event.padIndex > std::numeric_limits<std::uint8_t>::max()) {
            ++result.invalid;
            continue;
        }

        const audio::AudioTriggerEvent trigger{
            static_cast<std::uint8_t>(event.padIndex),
            event.velocity,
            *frameOffset};

        if (queue.tryEnqueue(trigger)) {
            ++result.written;
        } else {
            ++result.dropped;
        }
    }

    return result;
}

} // namespace mpc::sequencer
