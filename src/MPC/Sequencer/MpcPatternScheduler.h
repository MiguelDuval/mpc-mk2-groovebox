#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/Sequencer/MpcPatternOps.h"
#include "MPC/Sequencer/MpcSequencerCursor.h"

#include <cstddef>
#include <cstdint>
#include <span>

namespace mpc::sequencer {

struct ScheduledMidiEvent final {
    std::int64_t offsetTicks = 0;
    std::int32_t patternTick = 0;
    std::int32_t durationTicks = 0;
    std::uint8_t note = 0;
    std::uint8_t velocity = 0;
    std::uint8_t ratchetCount = 1;
    std::size_t sourceNoteIndex = 0;
};

struct ScheduleResult final {
    std::size_t written = 0;
    std::size_t eligible = 0;
    bool truncated = false;
};

struct RatchetExpansionResult final {
    std::size_t written = 0;
    bool truncated = false;
    bool invalid = false;
};

[[nodiscard]] inline RatchetExpansionResult expandScheduledRatchets(
        std::span<const ScheduledMidiEvent> input,
        std::int64_t spanTicks,
        std::span<ScheduledMidiEvent> output) noexcept {
    RatchetExpansionResult result;

    if (spanTicks <= 0) {
        result.invalid = true;
        return result;
    }

    for (const auto& event : input) {
        const auto count = event.ratchetCount == 0
                ? std::size_t{1}
                : event.ratchetCount > 8
                    ? std::size_t{8}
                    : static_cast<std::size_t>(event.ratchetCount);

        if (spanTicks < static_cast<std::int64_t>(count)) {
            result.invalid = true;
            continue;
        }

        for (std::size_t index = 0; index < count; ++index) {
            if (result.written >= output.size()) {
                result.truncated = true;
                continue;
            }

            auto& expanded = output[result.written++];
            expanded = event;
            expanded.ratchetCount = 1;

            const auto relativeOffset = static_cast<std::int64_t>(
                    (spanTicks * static_cast<std::int64_t>(index)) /
                    static_cast<std::int64_t>(count));
            expanded.offsetTicks = event.offsetTicks + relativeOffset;
        }
    }

    return result;
}

[[nodiscard]] ScheduleResult schedulePatternWindow(
        const domain::Pattern& pattern,
        const TickWindow& window,
        std::uint32_t seed,
        std::span<ScheduledMidiEvent> output) noexcept;

} // namespace mpc::sequencer
