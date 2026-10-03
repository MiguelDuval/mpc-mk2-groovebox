#include "Audio/SampleLayerParameters.h"

#include <cassert>
#include <cmath>
#include <limits>

int main() {
    assert(mpc::audio::normalizeSampleLayerPanMilli(0.0f) == 0);
    assert(mpc::audio::normalizeSampleLayerPanMilli(-0.5f) == -500);
    assert(mpc::audio::normalizeSampleLayerPanMilli(0.5f) == 500);
    assert(mpc::audio::normalizeSampleLayerPanMilli(-2.0f) == -1000);
    assert(mpc::audio::normalizeSampleLayerPanMilli(2.0f) == 1000);
    assert(mpc::audio::normalizeSampleLayerPanMilli(
            std::numeric_limits<float>::quiet_NaN()) == 0);

    assert(std::abs(
            mpc::audio::sampleLayerPanFromMilli(-750) + 0.75f) < 0.0001f);
    assert(mpc::audio::sampleLayerPanFromMilli(-1200) == -1.0f);
    assert(mpc::audio::sampleLayerPanFromMilli(1200) == 1.0f);

    return 0;
}
