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
    // The lifetime contains frames [0, totalFrames). Beyond that window
    // the envelope is finished even when releaseMs is zero; otherwise a
    // zero-release voice can retain sustain gain in direct/helper callers.
    if (totalFrames == 0 || ageFrames >= totalFrames || sampleRate <= 0) {
        return 0.0f;
    }

    // Public callers may pass an unnormalized parameter aggregate. Normalize
    // at the DSP boundary so NaN, infinity, negative times, or out-of-range
    // sustain cannot reach rounding/conversion or escape the supported range.
    const auto normalized = normalizeSampleEnvelopeParameters(
            parameters.attackMs,
            parameters.decayMs,
            parameters.sustain,
            parameters.releaseMs);

    const auto timeToFrames = [sampleRate](float milliseconds) {
        // Use llround: on 32-bit targets a valid 2000 ms envelope at a high
        // integer sample rate can exceed LONG_MAX while still fitting size_t.
        return static_cast<std::size_t>(
                std::llround(
                        static_cast<double>(milliseconds)
                        * static_cast<double>(sampleRate)
                        / 1000.0));
    };

    const std::size_t attackFrames = std::min(
            timeToFrames(normalized.attackMs), totalFrames);
    const std::size_t decayFrames = std::min(
            timeToFrames(normalized.decayMs),
            totalFrames - attackFrames);
    const std::size_t releaseFrames = std::min(
            timeToFrames(normalized.releaseMs), totalFrames);
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
            return normalized.sustain;
        }
        const float amount =
                static_cast<float>(ageFrames - attackFrames)
                / static_cast<float>(decayFrames);
        return 1.0f + (normalized.sustain - 1.0f) * amount;
    }

    if (releaseFrames > 0 && ageFrames >= releaseStart) {
        if (releaseFrames <= 1) {
            return 0.0f;
        }
        const float amount =
                static_cast<float>(ageFrames - releaseStart)
                / static_cast<float>(releaseFrames);
        return normalized.sustain * std::max(0.0f, 1.0f - amount);
    }

    return normalized.sustain;
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
