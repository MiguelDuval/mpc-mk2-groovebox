#pragma once

#include "MPC/Domain/DrumProgram.h"

#include <cstddef>
#include <cstdint>
#include <optional>

namespace mpc::domain {

[[nodiscard]] inline std::optional<std::size_t> findPadIndexByMidiNote(
        const DrumProgram& program,
        std::uint8_t midiNote) noexcept {
    for (std::size_t padIndex = 0; padIndex < program.pads.size(); ++padIndex) {
        if (program.pads[padIndex].midiNote == midiNote) {
            return padIndex;
        }
    }
    return std::nullopt;
}

} // namespace mpc::domain
