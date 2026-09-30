#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstdint>
#include <limits>

namespace mpc::sequencer {

inline constexpr std::int32_t kTicksPerQuarterNote = 960;

[[nodiscard]] inline bool isValidTimeSignature(
        std::int32_t numerator,
        std::int32_t denominator) noexcept {
    return numerator >= 1 && numerator <= 16
            && (denominator == 4
                || denominator == 8
                || denominator == 16
                || denominator == 32);
}

[[nodiscard]] inline std::int32_t barLengthTicks(
        std::int32_t numerator,
        std::int32_t denominator) noexcept {
    if (!isValidTimeSignature(numerator, denominator)) {
        return 0;
    }

    return static_cast<std::int32_t>(
            (static_cast<std::int64_t>(numerator)
             * 4
             * kTicksPerQuarterNote)
            / denominator);
}

[[nodiscard]] inline std::int32_t sequenceLengthForBars(
        std::int32_t bars,
        std::int32_t numerator,
        std::int32_t denominator) noexcept {
    if (bars <= 0) {
        return 0;
    }

    const auto perBar = barLengthTicks(numerator, denominator);
    if (perBar <= 0) {
        return 0;
    }

    const auto length = static_cast<std::int64_t>(bars) * perBar;
    return length > static_cast<std::int64_t>(
            std::numeric_limits<std::int32_t>::max())
            ? std::numeric_limits<std::int32_t>::max()
            : static_cast<std::int32_t>(length);
}

[[nodiscard]] inline std::int32_t sequenceBars(
        const domain::Sequence& sequence) noexcept {
    const auto perBar = barLengthTicks(
            sequence.numerator,
            sequence.denominator);
    if (perBar <= 0) {
        return 1;
    }

    return std::max<std::int32_t>(
            1,
            static_cast<std::int32_t>(
                    (static_cast<std::int64_t>(sequence.lengthTicks)
                     + perBar - 1)
                    / perBar));
}

[[nodiscard]] inline std::int32_t clampLoopBar(
        std::int32_t bar,
        std::int32_t bars) noexcept {
    return std::clamp(bar, 1, std::max<std::int32_t>(1, bars));
}

} // namespace mpc::sequencer
