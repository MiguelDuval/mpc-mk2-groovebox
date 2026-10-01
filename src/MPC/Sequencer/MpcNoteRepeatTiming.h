#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>
#include <limits>

namespace mpc::sequencer::note_repeat_timing {

inline constexpr std::int32_t kMinGridTicks = 40;
inline constexpr std::int32_t kMaxGridTicks = 960;
inline constexpr std::int64_t kMinTempoMilliBpm = 20000;
inline constexpr std::int64_t kMaxTempoMilliBpm = 300000;

struct RepeatRate final {
    std::int32_t ticks;
    const char* label;
    bool triplet;
};

inline constexpr std::array<RepeatRate, 8> kRepeatRates{{
    {960, "1/4", false},
    {480, "1/8", false},
    {240, "1/16", false},
    {120, "1/32", false},
    {60, "1/64", false},
    {640, "1/4T", true},
    {320, "1/8T", true},
    {160, "1/16T", true},
}};

[[nodiscard]] inline std::int32_t repeatRateTicksForIndex(
        std::int32_t index) noexcept {
    const auto clamped = std::clamp<std::int32_t>(
            index, 0, static_cast<std::int32_t>(kRepeatRates.size()) - 1);
    return kRepeatRates[static_cast<std::size_t>(clamped)].ticks;
}

[[nodiscard]] inline std::int32_t repeatRateIndexForTicks(
        std::int32_t ticks) noexcept {
    std::int32_t bestIndex = 0;
    auto bestDistance = std::numeric_limits<std::int32_t>::max();
    for (std::size_t i = 0; i < kRepeatRates.size(); ++i) {
        const auto distance = std::abs(
                kRepeatRates[i].ticks - ticks);
        if (distance < bestDistance) {
            bestDistance = distance;
            bestIndex = static_cast<std::int32_t>(i);
        }
    }
    return bestIndex;
}

[[nodiscard]] inline std::int32_t repeatRateIndexForTouch(
        std::uint8_t value) noexcept {
    const auto count =
            static_cast<std::int32_t>(kRepeatRates.size());
    return std::min<std::int32_t>(
            count - 1,
            (static_cast<std::int32_t>(value) * count) / 128);
}


[[nodiscard]] inline std::int32_t clampGrid(
        std::int32_t ticks) noexcept {
    return std::clamp(ticks, kMinGridTicks, kMaxGridTicks);
}

[[nodiscard]] inline std::int64_t clampTempoMilliBpm(
        std::int64_t tempoMilliBpm) noexcept {
    return std::clamp(
            tempoMilliBpm,
            kMinTempoMilliBpm,
            kMaxTempoMilliBpm);
}

[[nodiscard]] inline std::int64_t intervalNanos(
        std::int64_t ticks,
        std::int64_t tempoMilliBpm) noexcept {
    const auto safeTicks = std::max<std::int64_t>(1, ticks);
    const auto bpm =
            static_cast<double>(
                    clampTempoMilliBpm(tempoMilliBpm)) / 1000.0;
    const auto seconds =
            (static_cast<double>(safeTicks) / 960.0)
            * (60.0 / bpm);
    const auto nanos = seconds * 1000000000.0;

    if (!std::isfinite(nanos)
            || nanos <= 0.0
            || nanos >= static_cast<double>(
                    std::numeric_limits<std::int64_t>::max())) {
        return 1;
    }

    return std::max<std::int64_t>(
            1,
            static_cast<std::int64_t>(std::llround(nanos)));
}

[[nodiscard]] inline std::int64_t ticksToNextBoundary(
        std::int64_t positionTicks,
        std::int64_t loopStartTicks,
        std::int64_t loopEndTicks,
        std::int32_t gridTicks) noexcept {
    if (loopEndTicks <= loopStartTicks) {
        return clampGrid(gridTicks);
    }

    const auto loopLength = loopEndTicks - loopStartTicks;
    const auto grid =
            static_cast<std::int64_t>(clampGrid(gridTicks));
    const auto relative =
            ((positionTicks - loopStartTicks) % loopLength
                    + loopLength)
            % loopLength;
    const auto remainder = relative % grid;

    return remainder == 0 ? grid : grid - remainder;
}

} // namespace mpc::sequencer::note_repeat_timing
