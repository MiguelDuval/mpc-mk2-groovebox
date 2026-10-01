#pragma once

#include <cstdint>

namespace mpc::sequencer::step_edit {

enum class Parameter : std::uint8_t {
    Velocity,
    Probability,
    Ratchet,
    Nudge,
    Duration,
};

[[nodiscard]] constexpr Parameter nextParameter(Parameter parameter) noexcept {
    switch (parameter) {
        case Parameter::Velocity: return Parameter::Probability;
        case Parameter::Probability: return Parameter::Ratchet;
        case Parameter::Ratchet: return Parameter::Nudge;
        case Parameter::Nudge: return Parameter::Duration;
        case Parameter::Duration: return Parameter::Velocity;
    }
    return Parameter::Velocity;
}

[[nodiscard]] constexpr std::int32_t deltaFor(
        Parameter parameter,
        std::int32_t gridTicks,
        std::int32_t direction,
        bool fine) noexcept {
    if (direction == 0) return 0;
    const auto sign = direction > 0 ? 1 : -1;
    const auto grid = gridTicks > 0 ? gridTicks : 1;

    switch (parameter) {
        case Parameter::Velocity:
        case Parameter::Probability:
        case Parameter::Ratchet:
            return sign;
        case Parameter::Nudge:
            return sign * (fine ? 10 : 60);
        case Parameter::Duration: {
            const auto increment = fine
                    ? (grid / 16 > 0 ? grid / 16 : 1)
                    : (grid / 4 > 0 ? grid / 4 : 1);
            return sign * increment;
        }
    }
    return 0;
}

} // namespace mpc::sequencer::step_edit
