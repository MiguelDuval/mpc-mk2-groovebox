#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

namespace mpc::domain {

// Immutable-by-convention interleaved PCM owned by a SampleRef's shared asset
// handle. This value type has no decoder, device or realtime-engine dependency.
struct SamplePcmAsset final {
    std::uint32_t sampleRate = 0;
    std::uint16_t channelCount = 0;
    std::vector<float> interleaved;

    std::size_t frameCount() const noexcept {
        return channelCount == 0 ? 0 : interleaved.size() / channelCount;
    }

    float sampleAt(std::size_t frame, std::size_t channel) const noexcept {
        if (channel >= channelCount || frame >= frameCount()) return 0.0f;
        return interleaved[frame * channelCount + channel];
    }
};

} // namespace mpc::domain
