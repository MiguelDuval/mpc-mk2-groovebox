#pragma once

#include <oboe/Oboe.h>

namespace mpc::audio {

struct AudioOutputPolicy final {
    // Shared mode is the portable Android baseline. Exclusive output is not\n    // guaranteed on physical devices and can destabilize startup when the\n    // audio service cannot satisfy an exclusive stream request.\n    oboe::SharingMode preferredSharingMode = oboe::SharingMode::Shared;
    oboe::PerformanceMode performanceMode = oboe::PerformanceMode::LowLatency;
    bool floatFormat = true;
    bool allowFormatConversion = true;
    bool allowChannelConversion = true;
};

constexpr AudioOutputPolicy recommendedAudioOutputPolicy() noexcept {
    return {};
}

} // namespace mpc::audio
