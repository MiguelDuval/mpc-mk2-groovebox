#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

#include "MPC/Domain/SampleLayerVelocityRange.h"
#include "MPC/Domain/SampleRegion.h"

namespace mpc::domain {

inline constexpr std::size_t padCount = 16;
inline constexpr std::size_t sampleLayersPerPad = 8;

struct SampleId final {
    std::uint32_t value = 0;

    constexpr bool isAssigned() const noexcept {
        return value != 0;
    }
};

struct SampleLayer final {
    SampleId sample{};
    std::uint8_t velocityMinimum = 0;
    std::uint8_t velocityMaximum = 127;
    SampleRegion region{};
    float gain = 1.0f;
    float tuningSemitones = 0.0f;
    float pan = 0.0f;

    constexpr SampleLayerVelocityRange velocityRange() const noexcept {
        return {velocityMinimum, velocityMaximum};
    }

    constexpr bool isAssigned() const noexcept {
        return sample.isAssigned();
    }
};

struct Pad final {
    std::array<SampleLayer, sampleLayersPerPad> layers{};
    float tuningSemitones = 0.0f;
    float level = 1.0f;
    float pan = 0.0f;

    constexpr SampleLayer& layer(std::size_t index) noexcept {
        return layers[index];
    }

    constexpr const SampleLayer& layer(std::size_t index) const noexcept {
        return layers[index];
    }
};

struct DrumProgram final {
    std::array<Pad, padCount> pads{};

    constexpr Pad& pad(std::size_t index) noexcept {
        return pads[index];
    }

    constexpr const Pad& pad(std::size_t index) const noexcept {
        return pads[index];
    }
};

} // namespace mpc::domain
