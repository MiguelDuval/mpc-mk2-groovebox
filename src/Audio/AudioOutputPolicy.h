#pragma once

#include <oboe/Oboe.h>

namespace mpc::audio {

struct AudioOutputPolicy final {
    oboe::SharingMode preferredSharingMode = oboe::SharingMode::Shared;
    oboe::PerformanceMode performanceMode = oboe::PerformanceMode::LowLatency;
    bool floatFormat = true;
    bool allowFormatConversion = true;
    bool allowChannelConversion = true;
};

constexpr AudioOutputPolicy recommendedAudioOutputPolicy() noexcept {
    return {};
}

} // namespace mpc::audio
