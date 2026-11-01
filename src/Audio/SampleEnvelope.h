#pragma once

#include <algorithm>
#include <cmath>
#include <cstddef>

namespace mpc::audio {

struct SampleEnvelopeParameters final {
    float attackMs = 0.0f;
    float decayMs = 0.0f;
    float sustain = 1.0f;
    float releaseMs = 0.0f;
};

inline constexpr float kMaxSampleEnvelopeTimeMs = 2000.0f;

inline float normalizeSampleEnvelopeTimeMs(float milliseconds) noexcept {
    if (!std::isfinite(milliseconds)) {
        return 0.0f;
    }
    return std::clamp(milliseconds, 0.0f, kMaxSampleEnvelopeTimeMs);
}

inline float normalizeSampleEnvelopeSustain(float sustain) noexcept {
    if (!std::isfinite(sustain)) {
        return 1.0f;
    }
    return std::clamp(sustain, 0.0f, 1.0f);
}

inline SampleEnvelopeParameters normalizeSampleEnvelopeParameters(
        float attackMs,
        float decayMs,
        float sustain,
        float releaseMs) noexcept {
    return {
            normalizeSampleEnvelopeTimeMs(attackMs),
            normalizeSampleEnvelopeTimeMs(decayMs),
            normalizeSampleEnvelopeSustain(sustain),
            normalizeSampleEnvelopeTimeMs(releaseMs)};
}

inline float sampleEnvelopeGain(
        std::size_t ageFrames,
        std::size_t totalFrames,
        int sampleRate,
        const SampleEnvelopeParameters& parameters) noexcept {
    if (totalFrames == 0 || sampleRate <= 0) {
        return 0.0f;
    }

    const auto timeToFrames = [sampleRate](float milliseconds) {
        return static_cast<std::size_t>(
                std::lround(
                        static_cast<double>(milliseconds)
                        * static_cast<double>(sampleRate)
                        / 1000.0));
    };

    const std::size_t attackFrames = std::min(
            timeToFrames(parameters.attackMs), totalFrames);
    const std::size_t decayFrames = std::min(
            timeToFrames(parameters.decayMs),
            totalFrames - attackFrames);
    const std::size_t releaseFrames = std::min(
            timeToFrames(parameters.releaseMs), totalFrames);
    const std::size_t releaseStart = totalFrames - releaseFrames;

    if (ageFrames < attackFrames) {
        return attackFrames == 0
                ? 1.0f
                : static_cast<float>(ageFrames)
                        / static_cast<float>(attackFrames);
    }

    const std::size_t decayEnd = attackFrames + decayFrames;
    if (ageFrames < decayEnd) {
        if (decayFrames == 0) {
            return parameters.sustain;
        }
        const float amount =
                static_cast<float>(ageFrames - attackFrames)
                / static_cast<float>(decayFrames);
        return 1.0f + (parameters.sustain - 1.0f) * amount;
    }

    if (releaseFrames > 0 && ageFrames >= releaseStart) {
        if (releaseFrames <= 1) {
            return 0.0f;
        }
        const float amount =
                static_cast<float>(ageFrames - releaseStart)
                / static_cast<float>(releaseFrames);
        return parameters.sustain * std::max(0.0f, 1.0f - amount);
    }

    return parameters.sustain;
}

inline int normalizeSampleFilterCutoffMilliHz(float cutoffHz) noexcept {
    if (!std::isfinite(cutoffHz)) {
        return 20000000;
    }
    return static_cast<int>(
            std::lround(std::clamp(cutoffHz, 0.0f, 20000.0f) * 1000.0f));
}

inline float sampleFilterCutoffFromMilliHz(int milliHz) noexcept {
    return static_cast<float>(std::clamp(milliHz, 0, 20000000)) / 1000.0f;
}

} // namespace mpc::audio
