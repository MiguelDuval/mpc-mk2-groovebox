#include "WavSample.h"

#include <algorithm>
#include <cstring>
#include <limits>

namespace {

std::uint16_t readU16(const std::uint8_t* data) {
    return static_cast<std::uint16_t>(
        static_cast<std::uint16_t>(data[0])
        | (static_cast<std::uint16_t>(data[1]) << 8));
}

std::uint32_t readU32(const std::uint8_t* data) {
    return static_cast<std::uint32_t>(
        static_cast<std::uint32_t>(data[0])
        | (static_cast<std::uint32_t>(data[1]) << 8)
        | (static_cast<std::uint32_t>(data[2]) << 16)
        | (static_cast<std::uint32_t>(data[3]) << 24));
}

bool tagEquals(const std::uint8_t* data, const char* tag) {
    return std::memcmp(data, tag, 4) == 0;
}

} // namespace

namespace mpc::audio {

std::optional<SampleBuffer> decodeWav(std::span<const std::uint8_t> bytes) {
    if (bytes.size() < 12
            || !tagEquals(bytes.data(), "RIFF")
            || !tagEquals(bytes.data() + 8, "WAVE")) {
        return std::nullopt;
    }

    bool haveFormat = false;
    bool haveData = false;
    std::uint16_t format = 0;
    std::uint16_t channels = 0;
    std::uint16_t bitsPerSample = 0;
    std::uint32_t sampleRate = 0;
    std::size_t dataOffset = 0;
    std::size_t dataSize = 0;

    std::size_t offset = 12;
    while (offset + 8 <= bytes.size()) {
        const std::uint8_t* chunk = bytes.data() + offset;
        const std::uint32_t chunkSize = readU32(chunk + 4);
        const std::size_t payloadOffset = offset + 8;

        if (payloadOffset > bytes.size()) {
            return std::nullopt;
        }

        const std::size_t remaining = bytes.size() - payloadOffset;
        if (chunkSize > remaining) {
            return std::nullopt;
        }

        if (tagEquals(chunk, "fmt ")) {
            if (chunkSize < 16) {
                return std::nullopt;
            }

            const auto* payload = bytes.data() + payloadOffset;
            format = readU16(payload);
            channels = readU16(payload + 2);
            sampleRate = readU32(payload + 4);
            bitsPerSample = readU16(payload + 14);
            haveFormat = true;
        } else if (tagEquals(chunk, "data")) {
            dataOffset = payloadOffset;
            dataSize = chunkSize;
            haveData = true;
        }

        offset = payloadOffset + chunkSize + (chunkSize & 1u);
        if (offset > bytes.size()) {
            return std::nullopt;
        }
    }

    if (!haveFormat
            || !haveData
            || format != 1u
            || (channels != 1u && channels != 2u)
            || sampleRate == 0u
            || (bitsPerSample != 8u && bitsPerSample != 16u)) {
        return std::nullopt;
    }

    const std::size_t bytesPerSample = bitsPerSample / 8u;
    const std::size_t bytesPerFrame =
            static_cast<std::size_t>(channels) * bytesPerSample;

    if (bytesPerFrame == 0 || dataSize % bytesPerFrame != 0) {
        return std::nullopt;
    }

    const std::size_t frameCount = dataSize / bytesPerFrame;
    if (frameCount == 0
            || frameCount > std::numeric_limits<std::size_t>::max() / channels) {
        return std::nullopt;
    }

    SampleBuffer result;
    result.sampleRate = sampleRate;
    result.channelCount = channels;
    result.interleaved.resize(frameCount * channels);

    const auto* pcm = bytes.data() + dataOffset;
    for (std::size_t frame = 0; frame < frameCount; ++frame) {
        for (std::size_t channel = 0; channel < channels; ++channel) {
            const std::size_t index =
                    frame * bytesPerFrame + channel * bytesPerSample;

            float value = 0.0f;
            if (bitsPerSample == 16u) {
                const auto sample =
                        static_cast<std::int16_t>(readU16(pcm + index));
                value = static_cast<float>(sample) / 32768.0f;
            } else {
                value =
                    (static_cast<float>(pcm[index]) - 128.0f) / 128.0f;
            }

            result.interleaved[frame * channels + channel] =
                    std::clamp(value, -1.0f, 1.0f);
        }
    }

    return result;
}

} // namespace mpc::audio
