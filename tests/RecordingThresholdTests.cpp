#include "Audio/RecordingThreshold.h"

#include <cassert>
#include <limits>

int main() {
    assert(!mpc::audio::recordingThresholdCrossed(0.24f, 0.25f));
    assert(mpc::audio::recordingThresholdCrossed(0.25f, 0.25f));
    assert(mpc::audio::recordingThresholdCrossed(-0.30f, 0.25f));
    assert(mpc::audio::recordingThresholdCrossed(0.01f, 0.0f));
    assert(!mpc::audio::recordingThresholdCrossed(0.999f, 1.0f));
    assert(mpc::audio::recordingThresholdCrossed(1.0f, 1.0f));
    assert(!mpc::audio::recordingThresholdCrossed(
            0.5f,
            std::numeric_limits<float>::quiet_NaN()));
    return 0;
}
