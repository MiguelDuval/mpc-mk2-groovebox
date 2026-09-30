#include "Audio/AudioOutputPolicy.h"

#include <cassert>

int main() {
    const auto policy = mpc::audio::recommendedAudioOutputPolicy();

    assert(policy.preferredSharingMode == oboe::SharingMode::Exclusive);
    assert(policy.performanceMode == oboe::PerformanceMode::LowLatency);
    assert(policy.floatFormat);
    assert(policy.allowFormatConversion);
    assert(policy.allowChannelConversion);

    return 0;
}
