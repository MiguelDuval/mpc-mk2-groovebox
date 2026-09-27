#include "Audio/SampleLayerParameters.h"

#include <cassert>
#include <cmath>
#include <limits>

int main() {
    assert(mpc::audio::normalizeSampleLayerTuningMilli(0.0f) == 0);
    assert(mpc::audio::normalizeSampleLayerTuningMilli(1.25f) == 1250);
    assert(mpc::audio::normalizeSampleLayerTuningMilli(-2.5f) == -2500);
    assert(mpc::audio::normalizeSampleLayerTuningMilli(30.0f) == 24000);
    assert(mpc::audio::normalizeSampleLayerTuningMilli(-30.0f) == -24000);
    assert(mpc::audio::normalizeSampleLayerTuningMilli(
            std::numeric_limits<float>::quiet_NaN()) == 0);

    assert(std::abs(
            mpc::audio::sampleLayerTuningFromMilli(1250) - 1.25f) < 0.0001f);
    assert(mpc::audio::sampleLayerTuningFromMilli(30000) == 24.0f);
    assert(mpc::audio::sampleLayerTuningFromMilli(-30000) == -24.0f);

    return 0;
}
