#pragma once

#include "SampleRegion.h"
#include "WavSample.h"

#include <cstddef>
#include <limits>
#include <optional>

namespace mpc::audio {

// Creates a new PCM buffer containing only the selected region.
// The source buffer is never modified.
inline std::optional<SampleBuffer> cropSampleToRegion(
        const SampleBuffer& source,
        SampleRegion region) {
    if (source.channelCount == 0
            || !region.isValidFor(source.frameCount())) {
        return std::nullopt;
    }

    const std::size_t frameCount = region.frameCount();
    if (frameCount > (std::numeric_limits<std::size_t>::max()
            / static_cast<std::size_t>(source.channelCount))) {
        return std::nullopt;
    }

    SampleBuffer cropped;
    cropped.sampleRate = source.sampleRate;
    cropped.channelCount = source.channelCount;
    cropped.interleaved.resize(
            frameCount * static_cast<std::size_t>(source.channelCount));

    for (std::size_t frame = 0; frame < frameCount; ++frame) {
        for (std::size_t channel = 0;
                channel < static_cast<std::size_t>(source.channelCount);
                ++channel) {
            cropped.interleaved[
                    frame * static_cast<std::size_t>(source.channelCount)
                    + channel] =
                    source.sampleAt(region.startFrame + frame, channel);
        }
    }

    return cropped;
}

} // namespace mpc::audio
