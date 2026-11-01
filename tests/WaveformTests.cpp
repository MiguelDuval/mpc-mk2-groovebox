#include "Audio/Waveform.h"

#include <cassert>
#include <cmath>
#include <vector>

int main() {
    const std::vector<float> mono = {
        -0.25f, 0.5f,
        -1.0f, 0.75f,
        0.1f, -0.4f,
        0.0f, 0.9f
    };

    const auto peaks = mpc::audio::buildWaveformPeaks(mono, 1, 4);
    assert(peaks.size() == 4);
    assert(std::abs(peaks[0].minimum + 0.25f) < 0.0001f);
    assert(std::abs(peaks[0].maximum - 0.5f) < 0.0001f);
    assert(std::abs(peaks[1].minimum + 1.0f) < 0.0001f);
    assert(std::abs(peaks[1].maximum - 0.75f) < 0.0001f);

    const std::vector<float> stereo = {
        -1.0f, 1.0f,
        0.0f, 0.5f,
        -0.5f, 0.25f,
        0.25f, -0.25f
    };
    const auto stereoPeaks =
            mpc::audio::buildWaveformPeaks(stereo, 2, 2);
    assert(stereoPeaks.size() == 2);
    assert(std::abs(stereoPeaks[0].minimum + 1.0f) < 0.0001f);
    assert(std::abs(stereoPeaks[0].maximum - 1.0f) < 0.0001f);
    assert(std::abs(stereoPeaks[1].minimum + 0.5f) < 0.0001f);
    assert(std::abs(stereoPeaks[1].maximum - 0.25f) < 0.0001f);

    assert(mpc::audio::buildWaveformPeaks({}, 1, 16).empty());
    assert(mpc::audio::buildWaveformPeaks(mono, 0, 4).empty());
    assert(mpc::audio::buildWaveformPeaks(mono, 1, 0).empty());

    return 0;
}
