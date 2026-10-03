#include "Audio/SampleLayerParameters.h"

#include <cassert>
#include <cmath>
#include <limits>

int main() {
    assert(mpc::audio::normalizeSampleLayerGainMilli(1.0f) == 1000);
    assert(mpc::audio::normalizeSampleLayerGainMilli(0.5f) == 500);
    assert(mpc::audio::normalizeSampleLayerGainMilli(-0.25f) == 0);
    assert(mpc::audio::normalizeSampleLayerGainMilli(1.25f) == 1000);
    assert(mpc::audio::normalizeSampleLayerGainMilli(
            std::numeric_limits<float>::quiet_NaN()) == 1000);

    assert(std::abs(
            mpc::audio::sampleLayerGainFromMilli(750) - 0.75f) < 0.0001f);
    assert(mpc::audio::sampleLayerGainFromMilli(-1) == 0.0f);
    assert(mpc::audio::sampleLayerGainFromMilli(1200) == 1.0f);

    return 0;
}
