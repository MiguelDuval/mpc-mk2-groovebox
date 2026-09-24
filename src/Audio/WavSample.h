#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <vector>

namespace mpc::audio {

struct SampleBuffer final {
    std::uint32_t sampleRate = 0;
    std::uint16_t channelCount = 0;
    std::vector<float> interleaved;

    std::size_t frameCount() const noexcept {
        return channelCount == 0 ? 0 : interleaved.size() / channelCount;
    }

    float sampleAt(std::size_t frame, std::size_t channel) const noexcept {
        if (channel >= channelCount || frame >= frameCount()) {
            return 0.0f;
        }
        return interleaved[frame * channelCount + channel];
    }
};

std::optional<SampleBuffer> decodeWav(std::span<const std::uint8_t> bytes);

} // namespace mpc::audio
