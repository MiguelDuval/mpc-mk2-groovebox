#include "Waveform.h"

#include <algorithm>
#include <cmath>
#include <limits>

namespace mpc::audio {

std::vector<WaveformPeak> buildWaveformPeaks(
        std::span<const float> interleaved,
        std::uint16_t channelCount,
        std::size_t pointCount) {
    if (channelCount == 0 || pointCount == 0 || interleaved.empty()) {
        return {};
    }

    const std::size_t frameCount =
            interleaved.size() / static_cast<std::size_t>(channelCount);
    if (frameCount == 0) {
        return {};
    }

    pointCount = std::min(pointCount, frameCount);
    std::vector<WaveformPeak> result(pointCount);

    for (std::size_t point = 0; point < pointCount; ++point) {
        const std::size_t frameBegin =
                point * frameCount / pointCount;
        const std::size_t frameEnd =
                std::max(frameBegin + 1, (point + 1) * frameCount / pointCount);

        float minimum = 1.0f;
        float maximum = -1.0f;
        bool haveValue = false;

        for (std::size_t frame = frameBegin;
                frame < std::min(frameEnd, frameCount);
                ++frame) {
            const std::size_t base =
                    frame * static_cast<std::size_t>(channelCount);
            for (std::size_t channel = 0; channel < channelCount; ++channel) {
                float value = interleaved[base + channel];
                if (!std::isfinite(value)) {
                    value = 0.0f;
                }
                value = std::clamp(value, -1.0f, 1.0f);
                minimum = std::min(minimum, value);
                maximum = std::max(maximum, value);
                haveValue = true;
            }
        }

        if (haveValue) {
            result[point] = WaveformPeak{minimum, maximum};
        }
    }

    return result;
}

} // namespace mpc::audio
