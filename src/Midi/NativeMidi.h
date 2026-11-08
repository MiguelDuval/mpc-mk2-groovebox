#pragma once

#include <array>
#include <cstdint>
#include <optional>
#include <vector>
#include <span>

namespace mpc::midi {
std::optional<std::vector<std::uint8_t>> handleIncoming(
        std::span<const std::uint8_t> message,
        std::int64_t timestamp);

// Closes the Note Repeat record producer and waits for any in-flight bounded
// queue write before a take/Track/Sequence transition drains recorder state.
void invalidateNoteRepeatRecordingTarget() noexcept;
void invalidatePadRecordingTargets() noexcept;
}
