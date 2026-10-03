#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

namespace mpc::audio {

struct WaveformPeak final {
    float minimum = 0.0f;
    float maximum = 0.0f;
};

std::vector<WaveformPeak> buildWaveformPeaks(
        std::span<const float> interleaved,
        std::uint16_t channelCount,
        std::size_t pointCount);

} // namespace mpc::audio
