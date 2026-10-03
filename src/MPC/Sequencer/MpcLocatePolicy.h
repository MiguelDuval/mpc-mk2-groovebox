#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>

namespace mpc::sequencer::locate {

inline constexpr std::size_t kLocatorCount = 6;
inline constexpr std::int32_t kJumpPadFirst = 0;
inline constexpr std::int32_t kStorePadFirst = 8;
inline constexpr std::int64_t kMomentaryHoldThresholdNanos = 350'000'000;

[[nodiscard]] constexpr std::int32_t locatorSlotForJumpPad(
        std::int32_t padIndex) noexcept {
    return padIndex >= kJumpPadFirst
            && padIndex < kJumpPadFirst
                    + static_cast<std::int32_t>(kLocatorCount)
            ? padIndex - kJumpPadFirst
            : -1;
}

[[nodiscard]] constexpr std::int32_t locatorSlotForStorePad(
        std::int32_t padIndex) noexcept {
    return padIndex >= kStorePadFirst
            && padIndex < kStorePadFirst
                    + static_cast<std::int32_t>(kLocatorCount)
            ? padIndex - kStorePadFirst
            : -1;
}

[[nodiscard]] constexpr std::int32_t storePadForLocatorSlot(
        std::int32_t slot) noexcept {
    return slot >= 0
            && slot < static_cast<std::int32_t>(kLocatorCount)
            ? kStorePadFirst + slot
            : -1;
}

[[nodiscard]] constexpr std::int64_t clampTick(
        std::int64_t tick,
        std::int64_t sequenceLengthTicks) noexcept {
    const auto length = std::max<std::int64_t>(1, sequenceLengthTicks);
    return std::clamp<std::int64_t>(tick, 0, length - 1);
}

[[nodiscard]] constexpr bool isMomentaryHold(
        std::int64_t pressTimestampNanos,
        std::int64_t releaseTimestampNanos) noexcept {
    return pressTimestampNanos > 0
            && releaseTimestampNanos > pressTimestampNanos
            && releaseTimestampNanos - pressTimestampNanos
                    >= kMomentaryHoldThresholdNanos;
}

} // namespace mpc::sequencer::locate
