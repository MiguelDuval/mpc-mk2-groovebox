#pragma once

#include <algorithm>
#include <cmath>

namespace mpc::audio {

inline bool recordingThresholdCrossed(
        float sample,
        float threshold) noexcept {
    if (!std::isfinite(sample) || !std::isfinite(threshold)) {
        return false;
    }

    const float clampedThreshold =
            std::clamp(threshold, 0.0f, 1.0f);
    return std::abs(sample) >= clampedThreshold;
}

} // namespace mpc::audio
