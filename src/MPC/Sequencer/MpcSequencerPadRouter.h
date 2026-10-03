#pragma once

#include "MPC/Domain/DrumProgramOps.h"
#include "MPC/Sequencer/MpcPatternScheduler.h"

#include <cstddef>
#include <cstdint>
#include <span>

namespace mpc::sequencer {

struct ScheduledPadEvent final {
    std::int64_t offsetTicks = 0;
    std::size_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::size_t sourceNoteIndex = 0;
};

struct PadRouteResult final {
    std::size_t written = 0;
    std::size_t routed = 0;
    std::size_t unmapped = 0;
    bool truncated = false;
};

[[nodiscard]] inline PadRouteResult routeScheduledEventsToPads(
        const domain::DrumProgram& program,
        std::span<const ScheduledMidiEvent> input,
        std::span<ScheduledPadEvent> output) noexcept {
    PadRouteResult result;

    for (const auto& event : input) {
        const auto padIndex =
                domain::findPadIndexByMidiNote(program, event.note);
        if (!padIndex.has_value()) {
            ++result.unmapped;
            continue;
        }

        if (program.pad(*padIndex).muted) {
            continue;
        }

        if (result.written >= output.size()) {
            result.truncated = true;
            continue;
        }

        auto& routed = output[result.written++];
        routed.offsetTicks = event.offsetTicks;
        routed.padIndex = *padIndex;
        routed.velocity = event.velocity;
        routed.sourceNoteIndex = event.sourceNoteIndex;
        ++result.routed;
    }

    return result;
}

} // namespace mpc::sequencer
