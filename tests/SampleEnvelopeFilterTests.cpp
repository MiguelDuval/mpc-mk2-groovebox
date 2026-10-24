#include "Audio/OnePoleLowPass.h"
#include "Audio/SampleEnvelope.h"

#include <cassert>
#include <cmath>
#include <cstddef>
#include <limits>
#include <limits>

namespace {

void expectNear(float actual, float expected, float epsilon = 0.0001f) {
    assert(std::abs(actual - expected) <= epsilon);
}

} // namespace

int main() {
    {
        const auto parameters =
                mpc::audio::normalizeSampleEnvelopeParameters(
                        -10.0f, 2500.0f, 2.0f, 100.0f);
        expectNear(parameters.attackMs, 0.0f);
        expectNear(parameters.decayMs, 2000.0f);
        expectNear(parameters.sustain, 1.0f);
        expectNear(parameters.releaseMs, 100.0f);
    }

    {
        const mpc::audio::SampleEnvelopeParameters flat{};
        for (std::size_t age = 0; age < 1000; ++age) {
            expectNear(mpc::audio::sampleEnvelopeGain(
                    age, 1000, 1000, flat), 1.0f);
        }
    }

    {
        const auto parameters =
                mpc::audio::normalizeSampleEnvelopeParameters(
                        100.0f, 100.0f, 0.5f, 200.0f);

        expectNear(mpc::audio::sampleEnvelopeGain(
                0, 1000, 1000, parameters), 0.0f);
        expectNear(mpc::audio::sampleEnvelopeGain(
                50, 1000, 1000, parameters), 0.5f);
        expectNear(mpc::audio::sampleEnvelopeGain(
                150, 1000, 1000, parameters), 0.75f);
        expectNear(mpc::audio::sampleEnvelopeGain(
                500, 1000, 1000, parameters), 0.5f);
        assert(mpc::audio::sampleEnvelopeGain(
                900, 1000, 1000, parameters) < 0.3f);
    }

    {
        // The DSP helper is also a public entry point: it must not assume
        // every caller has already passed values through the normalizer.
        const mpc::audio::SampleEnvelopeParameters raw{
                -10.0f, -5.0f, 2.0f, -1.0f};
        expectNear(mpc::audio::sampleEnvelopeGain(
                100, 1000, 1000, raw), 1.0f);

        const mpc::audio::SampleEnvelopeParameters nonFinite{
                std::numeric_limits<float>::quiet_NaN(),
                std::numeric_limits<float>::infinity(),
                std::numeric_limits<float>::quiet_NaN(),
                -std::numeric_limits<float>::infinity()};
        const auto gain = mpc::audio::sampleEnvelopeGain(
                500, 1000, 1000, nonFinite);
        assert(std::isfinite(gain));
        expectNear(gain, 1.0f);

        // The maximum normalized duration can exceed LONG_MAX frames on a
        // 32-bit target, although the valid frame count fits size_t.
        const mpc::audio::SampleEnvelopeParameters longAttack{
                2000.0f, 0.0f, 1.0f, 0.0f};
        const auto highRateGain = mpc::audio::sampleEnvelopeGain(
                100, std::numeric_limits<std::size_t>::max(),
                std::numeric_limits<int>::max(), longAttack);
        assert(std::isfinite(highRateGain));
        assert(highRateGain >= 0.0f && highRateGain <= 1.0f);
    }

    {
        mpc::audio::OnePoleLowPass filter;
        filter.configure(0.0f, 48000);
        expectNear(filter.process(0.75f), 0.75f);

        // Bypass must follow the live signal so re-enabling the filter does
        // not restart from a stale pre-bypass sample.
        filter.configure(1000.0f, 48000);
        expectNear(filter.process(1.0f), 1.0f);
        filter.setCutoff(0.0f, 48000);
        expectNear(filter.process(0.0f), 0.0f);
        filter.setCutoff(1000.0f, 48000);
        expectNear(filter.process(0.0f), 0.0f);

        const float first = filter.process(1.0f);
        const float second = filter.process(0.0f);
        expectNear(first, 1.0f);
        assert(second > 0.0f);
        assert(second < 1.0f);

        // NaN and infinity must never leak to output or poison the subsequent
        // running state, including when the filter is bypassed.
        const auto notANumber = std::numeric_limits<float>::quiet_NaN();
        const auto positiveInfinity = std::numeric_limits<float>::infinity();
        assert(filter.process(notANumber) == 0.0f);
        assert(std::isfinite(filter.process(0.25f)));
        filter.setCutoff(0.0f, 48000);
        assert(filter.process(positiveInfinity) == 0.0f);
        expectNear(filter.process(0.25f), 0.25f);

        filter.setCutoff(1000.0f, 48000);
        expectNear(filter.process(0.25f), 0.25f);
    }

    return 0;
}
