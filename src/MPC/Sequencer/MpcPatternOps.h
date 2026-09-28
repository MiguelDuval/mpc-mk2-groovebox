#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

inline constexpr std::int32_t kDefaultQuantizeGridTicks = 120;

inline std::size_t quantizePattern(
        domain::Pattern& pattern,
        std::int32_t gridTicks) {
    if (gridTicks <= 0 || pattern.lengthTicks <= 0) {
        return 0;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto grid = static_cast<std::int64_t>(gridTicks);
    std::size_t changed = 0;

    for (auto& note : pattern.notes) {
        const auto original = note.tick;
        const auto tick = std::max<std::int64_t>(0, original);
        const auto quantized =
                ((tick + grid / 2) / grid) * grid;
        note.tick = static_cast<std::int32_t>(quantized % length);
        if (note.tick != original) {
            ++changed;
        }
    }

    std::stable_sort(
            pattern.notes.begin(),
            pattern.notes.end(),
            [](const domain::MidiNoteEvent& lhs,
               const domain::MidiNoteEvent& rhs) {
                return lhs.tick < rhs.tick;
            });

    return changed;
}

} // namespace mpc::sequencer
