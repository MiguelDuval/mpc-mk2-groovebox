#pragma once

#include <algorithm>
#include <cmath>

namespace mpc::studio::aftertouch {

inline constexpr float kMaximumFilterDepthOctaves = 3.0f;

inline float filterCutoffForPressure(
        float baseCutoffHz,
        unsigned pressure) noexcept {
    if (!std::isfinite(baseCutoffHz) || baseCutoffHz <= 0.0f) {
        return 0.0f;
    }

    const auto clampedPressure = std::min(127u, pressure);
    const float normalized =
            static_cast<float>(clampedPressure) / 127.0f;

    return baseCutoffHz * std::pow(
            2.0f,
            -kMaximumFilterDepthOctaves * normalized);
}

} // namespace mpc::studio::aftertouch
