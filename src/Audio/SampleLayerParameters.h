#pragma once

#include <algorithm>
#include <cmath>
#include <cstdint>

namespace mpc::audio {

inline std::int32_t normalizeSampleLayerGainMilli(float gain) noexcept {
    if (!std::isfinite(gain)) {
        return 1000;
    }

    const float clamped = std::clamp(gain, 0.0f, 1.0f);
    return static_cast<std::int32_t>(std::lround(clamped * 1000.0f));
}

inline float sampleLayerGainFromMilli(std::int32_t milli) noexcept {
    const auto clamped = std::clamp(milli, 0, 1000);
    return static_cast<float>(clamped) / 1000.0f;
}

} // namespace mpc::audio
