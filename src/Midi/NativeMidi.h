#pragma once

#include <cstdint>
#include <span>

namespace mpc::midi {
void handleIncoming(std::span<const std::uint8_t> message, std::int64_t timestamp);
}
