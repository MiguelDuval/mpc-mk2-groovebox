#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <span>

namespace mpc::sequencer {

inline constexpr std::int32_t kDefaultQuantizeGridTicks = 120;
inline constexpr std::int32_t kMaxStepNudgeTicks = 960;

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


inline std::size_t applySwing(
        domain::Pattern& pattern,
        std::int32_t gridTicks,
        std::int32_t swingPercent) {
    if (gridTicks <= 0 || pattern.lengthTicks <= 0 ||
        pattern.notes.empty()) {
        return 0;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto grid = static_cast<std::int64_t>(gridTicks);
    const auto clampedSwing = std::clamp(swingPercent, 0, 100);
    if (clampedSwing == 0) {
        return 0;
    }

    const auto maxDelay = grid / 2;
    const auto delay =
            (maxDelay * static_cast<std::int64_t>(clampedSwing)) / 100;

    std::size_t changed = 0;
    for (auto& note : pattern.notes) {
        if (note.tick < 0 || note.tick % grid != 0) {
            continue;
        }

        const auto step = static_cast<std::int64_t>(note.tick) / grid;
        if ((step & 1) == 0) {
            continue;
        }

        const auto shifted =
                (static_cast<std::int64_t>(note.tick) + delay) % length;
        note.tick = static_cast<std::int32_t>(shifted);
        ++changed;
    }

    if (changed != 0) {
        std::stable_sort(
                pattern.notes.begin(),
                pattern.notes.end(),
                [](const domain::MidiNoteEvent& lhs,
                   const domain::MidiNoteEvent& rhs) {
                    return lhs.tick < rhs.tick;
                });
    }

    return changed;
}


inline std::uint32_t deterministicProbabilityRoll(
        const domain::MidiNoteEvent& note,
        std::uint32_t seed) noexcept {
    std::uint32_t state = seed ^ (static_cast<std::uint32_t>(note.tick) * 0x9E3779B9u);
    state ^= static_cast<std::uint32_t>(note.note) * 0x85EBCA6Bu;
    state ^= static_cast<std::uint32_t>(note.velocity) * 0xC2B2AE35u;
    state ^= state >> 16;
    state *= 0x7FEB352Du;
    state ^= state >> 15;
    return state & 127u;
}

inline bool shouldTriggerNote(
        const domain::MidiNoteEvent& note,
        std::uint32_t seed) noexcept {
    if (note.probability == 0 || note.velocity == 0) {
        return false;
    }
    if (note.probability >= 127) {
        return true;
    }
    return deterministicProbabilityRoll(note, seed) < note.probability;
}

inline std::size_t ratchetCount(
        const domain::MidiNoteEvent& note) noexcept {
    return std::clamp<std::size_t>(note.ratchet, 1, 8);
}

inline bool setStepNote(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber,
        std::uint8_t velocity,
        std::int32_t durationTicks = 0,
        std::uint8_t probability = 127,
        std::uint8_t ratchet = 1) {
    if (stepIndex < 0 || gridTicks <= 0 || pattern.lengthTicks <= 0 ||
        velocity == 0 || ratchet == 0 || probability > 127) {
        return false;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto tick64 =
            static_cast<std::int64_t>(stepIndex) *
            static_cast<std::int64_t>(gridTicks);
    if (tick64 >= length) {
        return false;
    }

    const auto tick = static_cast<std::int32_t>(tick64);
    for (auto& note : pattern.notes) {
        if (note.tick == tick && note.note == noteNumber) {
            note.durationTicks = durationTicks;
            note.velocity = velocity;
            note.probability = probability;
            note.ratchet = ratchet;
            return true;
        }
    }

    pattern.notes.push_back({
            tick,
            durationTicks,
            noteNumber,
            velocity,
            probability,
            ratchet});

    std::stable_sort(
            pattern.notes.begin(),
            pattern.notes.end(),
            [](const domain::MidiNoteEvent& lhs,
               const domain::MidiNoteEvent& rhs) {
                return lhs.tick < rhs.tick;
            });
    return true;
}

inline domain::MidiNoteEvent* findStepNote(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber) noexcept {
    if (stepIndex < 0 || gridTicks <= 0 || pattern.lengthTicks <= 0) {
        return nullptr;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto tick64 =
            static_cast<std::int64_t>(stepIndex)
            * static_cast<std::int64_t>(gridTicks);
    if (tick64 < 0 || tick64 >= length) {
        return nullptr;
    }

    const auto tick = static_cast<std::int32_t>(tick64);
    for (auto& note : pattern.notes) {
        if (note.tick == tick && note.note == noteNumber) {
            return &note;
        }
    }
    return nullptr;
}

inline const domain::MidiNoteEvent* findStepNote(
        const domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber) noexcept {
    if (stepIndex < 0 || gridTicks <= 0 || pattern.lengthTicks <= 0) {
        return nullptr;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto tick64 =
            static_cast<std::int64_t>(stepIndex)
            * static_cast<std::int64_t>(gridTicks);
    if (tick64 < 0 || tick64 >= length) {
        return nullptr;
    }

    const auto tick = static_cast<std::int32_t>(tick64);
    for (const auto& note : pattern.notes) {
        if (note.tick == tick && note.note == noteNumber) {
            return &note;
        }
    }
    return nullptr;
}

inline bool setStepNoteVelocity(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber,
        std::uint8_t velocity) noexcept {
    if (velocity == 0) {
        return false;
    }
    auto* note = findStepNote(
            pattern, stepIndex, gridTicks, noteNumber);
    if (note == nullptr) {
        return false;
    }
    note->velocity = velocity;
    return true;
}

inline bool setStepNoteProbability(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber,
        std::uint8_t probability) noexcept {
    auto* note = findStepNote(
            pattern, stepIndex, gridTicks, noteNumber);
    if (note == nullptr || probability > 127) {
        return false;
    }
    note->probability = probability;
    return true;
}

inline bool setStepNoteNudge(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber,
        std::int32_t nudgeTicks) noexcept {
    if (nudgeTicks < -kMaxStepNudgeTicks
            || nudgeTicks > kMaxStepNudgeTicks) {
        return false;
    }
    auto* note = findStepNote(
            pattern, stepIndex, gridTicks, noteNumber);
    if (note == nullptr) {
        return false;
    }
    note->nudgeTicks = nudgeTicks;
    return true;
}

inline bool setStepNoteRatchet(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber,
        std::uint8_t ratchet) noexcept {
    if (ratchet == 0 || ratchet > 8) {
        return false;
    }
    auto* note = findStepNote(
            pattern, stepIndex, gridTicks, noteNumber);
    if (note == nullptr) {
        return false;
    }
    note->ratchet = ratchet;
    if (note->durationTicks <= 0) {
        note->durationTicks = gridTicks;
    }
    return true;
}

inline bool eraseStepNote(
        domain::Pattern& pattern,
        std::int32_t stepIndex,
        std::int32_t gridTicks,
        std::uint8_t noteNumber) {
    if (stepIndex < 0 || gridTicks <= 0 || pattern.lengthTicks <= 0) {
        return false;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    const auto tick64 =
            static_cast<std::int64_t>(stepIndex) *
            static_cast<std::int64_t>(gridTicks);
    if (tick64 >= length) {
        return false;
    }

    const auto tick = static_cast<std::int32_t>(tick64);
    const auto before = pattern.notes.size();
    pattern.notes.erase(
            std::remove_if(
                    pattern.notes.begin(),
                    pattern.notes.end(),
                    [tick, noteNumber](const domain::MidiNoteEvent& note) {
                        return note.tick == tick && note.note == noteNumber;
                    }),
            pattern.notes.end());
    return pattern.notes.size() != before;
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
