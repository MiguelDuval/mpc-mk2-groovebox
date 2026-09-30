#pragma once

#include <array>
#include <cstdint>
#include <optional>
#include <span>

namespace mpc::midi {
std::optional<std::array<std::uint8_t, 12>> handleIncoming(
        std::span<const std::uint8_t> message,
        std::int64_t timestamp);
}
