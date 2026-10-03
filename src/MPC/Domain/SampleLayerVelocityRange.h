#pragma once

#include <cstdint>

namespace mpc::domain {

struct SampleLayerVelocityRange final {
    std::uint8_t minimum = 0;
    std::uint8_t maximum = 127;

    constexpr bool isValid() const noexcept {
        return minimum <= maximum;
    }

    constexpr bool contains(std::uint8_t velocity) const noexcept {
        return isValid() && velocity >= minimum && velocity <= maximum;
    }
};

} // namespace mpc::domain
