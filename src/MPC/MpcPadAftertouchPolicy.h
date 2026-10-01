#pragma once

#include <algorithm>
#include <array>
#include <cmath>

namespace mpc::studio::aftertouch {

inline constexpr float kMaximumFilterDepthOctaves = 3.0f;

inline float filterMultiplierForPressure(unsigned pressure) noexcept {
    static const auto table = [] {
        std::array<float, 128> values{};
        for (std::size_t i = 0; i < values.size(); ++i) {
            const float normalized =
                    static_cast<float>(i) / 127.0f;
            values[i] = std::pow(
                    2.0f,
                    -kMaximumFilterDepthOctaves * normalized);
        }
        return values;
    }();

    return table[std::min<std::size_t>(127u, pressure)];
}

inline float filterCutoffForPressure(
        float baseCutoffHz,
        unsigned pressure) noexcept {
    if (!std::isfinite(baseCutoffHz) || baseCutoffHz <= 0.0f) {
        return 0.0f;
    }
    return baseCutoffHz * filterMultiplierForPressure(pressure);
}

} // namespace mpc::studio::aftertouch
