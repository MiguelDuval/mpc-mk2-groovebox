#pragma once

#include "Audio/WavSample.h"

#include <cstddef>

namespace mpc::audio {

// Keep the address and sample-presence guards independently testable without
// pulling the Oboe-backed AudioEngine into the host-native WAV codec test.
constexpr bool isValidSampleExportAddress(
        std::size_t padIndex,
        std::size_t layerIndex,
        std::size_t padCount,
        std::size_t layerCount) noexcept {
    return padIndex < padCount && layerIndex < layerCount;
}

inline bool hasExportableSample(const SampleBuffer* sample) noexcept {
    return sample != nullptr && sample->frameCount() != 0;
}

} // namespace mpc::audio
