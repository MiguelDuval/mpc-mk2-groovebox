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

[[nodiscard]] ScheduleResult schedulePatternWindow(
        const domain::Pattern& pattern,
        const TickWindow& window,
        std::uint32_t seed,
        std::span<ScheduledMidiEvent> output) noexcept;

} // namespace mpc::sequencer
