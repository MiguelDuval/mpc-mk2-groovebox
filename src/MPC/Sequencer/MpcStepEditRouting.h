#pragma once

#include <cstddef>

namespace mpc::sequencer::step_edit {

inline constexpr std::size_t kStepsPerPage = 16;

[[nodiscard]] inline std::size_t stepIndexForPad(
        std::size_t page,
        std::size_t padIndex) noexcept {
    const auto clampedPad =
            padIndex < kStepsPerPage ? padIndex : kStepsPerPage - 1;
    return page * kStepsPerPage + clampedPad;
}

} // namespace mpc::sequencer::step_edit
