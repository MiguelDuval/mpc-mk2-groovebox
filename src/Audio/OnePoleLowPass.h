#pragma once

#include <algorithm>
#include <cmath>
#include <numbers>

namespace mpc::audio {

class OnePoleLowPass final {
public:
    void configure(float cutoffHz, int sampleRate) noexcept {
        initialized_ = false;
        state_ = 0.0f;
        setCutoff(cutoffHz, sampleRate);
    }

    // Update the coefficient without clearing the running filter state.
    void setCutoff(float cutoffHz, int sampleRate) noexcept {
        if (!std::isfinite(cutoffHz) || cutoffHz <= 0.0f || sampleRate <= 0) {
            bypass_ = true;
            alpha_ = 1.0f;
            return;
        }

        const float maxCutoff = static_cast<float>(sampleRate) * 0.49f;
        const float clampedCutoff = std::clamp(cutoffHz, 0.0f, maxCutoff);
        if (clampedCutoff >= maxCutoff) {
            bypass_ = true;
            alpha_ = 1.0f;
            return;
        }

        const float normalized =
                2.0f * std::numbers::pi_v<float> * clampedCutoff
                / static_cast<float>(sampleRate);
        alpha_ = 1.0f - std::exp(-normalized);
        bypass_ = false;
    }

    void reset() noexcept {
        initialized_ = false;
        state_ = 0.0f;
    }

    float process(float input) noexcept {
        // Never allow non-finite source samples to enter the filter state or
        // propagate to the audio output, even while the filter is bypassed.
        if (!std::isfinite(input)) {
            state_ = 0.0f;
            initialized_ = false;
            return 0.0f;
        }

        // Defend state/coefficient invariants if a previous input was extreme.
        if (!std::isfinite(state_)) {
            state_ = 0.0f;
            initialized_ = false;
        }
        if (!std::isfinite(alpha_)) {
            alpha_ = 1.0f;
            bypass_ = true;
        }

        if (bypass_) {
            // Keep continuity state warm while bypassed. Re-enabling a filter
            // starts from the most recent bypassed sample, not stale state.
            state_ = input;
            initialized_ = true;
            return input;
        }

        if (!initialized_) {
            state_ = input;
            initialized_ = true;
            return input;
        }

        const float nextState = state_ + alpha_ * (input - state_);
        if (!std::isfinite(nextState)) {
            state_ = 0.0f;
            initialized_ = false;
            return 0.0f;
        }

        state_ = nextState;
        return state_;
    }

private:
    float alpha_ = 1.0f;
    float state_ = 0.0f;
    bool initialized_ = false;
    bool bypass_ = true;
};

} // namespace mpc::audio
