#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include "MPC/Domain/SamplePcmAsset.h"

namespace mpc::audio {

// Preserve the existing audio API while keeping the actual immutable PCM
// storage type in the domain layer, below AudioEngine and WAV decoding.
using SampleBuffer = mpc::domain::SamplePcmAsset;

std::optional<SampleBuffer> decodeWav(std::span<const std::uint8_t> bytes);

} // namespace mpc::audio
