#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <limits>
#include <optional>

namespace mpc::sequencer {

inline constexpr std::int32_t kTicksPerQuarterNote = 960;

// Keep transport timing and audio-frame scheduling on the same supported BPM
// range even when a legacy or malformed editable Sequence contains bad data.
[[nodiscard]] inline double sanitizeTempoBpm(double tempoBpm) noexcept {
    return std::isfinite(tempoBpm)
            ? std::clamp(tempoBpm, 20.0, 300.0)
            : 120.0;
}

[[nodiscard]] inline std::optional<std::int32_t> ticksToFrames(
        std::int64_t ticks,
        double tempoBpm,
        std::int32_t sampleRate) noexcept {
    if (ticks < 0
            || !std::isfinite(tempoBpm)
            || tempoBpm <= 0.0
            || sampleRate <= 0) {
        return std::nullopt;
    }

    const long double framesPerTick =
            static_cast<long double>(sampleRate) * 60.0L
            / (static_cast<long double>(tempoBpm)
                    * static_cast<long double>(kTicksPerQuarterNote));
    const long double frames =
            static_cast<long double>(ticks) * framesPerTick;

    if (!std::isfinite(frames)
            || frames >= static_cast<long double>(
                    std::numeric_limits<std::int32_t>::max())) {
        return std::nullopt;
    }

    return static_cast<std::int32_t>(
            std::llround(frames));
}

} // namespace mpc::sequencer
