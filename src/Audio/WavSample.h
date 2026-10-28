#pragma once

#include <cstddef>
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

// Encode a non-empty mono/stereo SampleBuffer as RIFF IEEE-float 32-bit PCM.
// Returns an empty vector when the sample cannot be represented by RIFF/WAV.
std::vector<std::uint8_t> encodeWav(const SampleBuffer& sample);

} // namespace mpc::audio
