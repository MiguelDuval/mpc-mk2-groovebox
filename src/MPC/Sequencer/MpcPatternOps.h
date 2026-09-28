#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <span>

namespace mpc::sequencer {

inline constexpr std::int32_t kDefaultQuantizeGridTicks = 120;

enum class PatternRecordMode : std::uint8_t {
    Replace,
    Overdub
};

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

inline std::size_t recordNotes(
        domain::Pattern& pattern,
        std::span<const domain::MidiNoteEvent> captured,
        PatternRecordMode mode) {
    if (pattern.lengthTicks <= 0) {
        return 0;
    }

    std::size_t validCount = 0;
    for (const auto& source : captured) {
        if (source.durationTicks >= 0 && source.ratchet != 0) {
            ++validCount;
        }
    }

    if (validCount == 0) {
        return 0;
    }

    if (mode == PatternRecordMode::Replace) {
        pattern.notes.reserve(validCount);
        pattern.notes.clear();
    } else {
        pattern.notes.reserve(pattern.notes.size() + validCount);
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    std::size_t accepted = 0;

    for (const auto& source : captured) {
        if (source.durationTicks < 0 || source.ratchet == 0) {
            continue;
        }

        auto note = source;
        const auto normalized =
                static_cast<std::int64_t>(note.tick) % length;
        note.tick = static_cast<std::int32_t>(
                normalized < 0 ? normalized + length : normalized);
        pattern.notes.push_back(note);
        ++accepted;
    }

    std::stable_sort(
            pattern.notes.begin(),
            pattern.notes.end(),
            [](const domain::MidiNoteEvent& lhs,
               const domain::MidiNoteEvent& rhs) {
                return lhs.tick < rhs.tick;
            });

    return accepted;
}

} // namespace mpc::sequencer
