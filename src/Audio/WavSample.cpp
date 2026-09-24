#include "WavSample.h"

#include <algorithm>
#include <bit>
#include <cmath>
#include <cstring>
#include <limits>

namespace {

constexpr std::uint16_t kPcmFormat = 1;
constexpr std::uint16_t kFloatFormat = 3;
constexpr std::uint16_t kExtensibleFormat = 0xFFFE;
constexpr std::uint16_t kExtensiblePcm = kPcmFormat;
constexpr std::uint16_t kExtensibleFloat = kFloatFormat;

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

std::int32_t readS24(const std::uint8_t* data) {
    const std::uint32_t value =
            static_cast<std::uint32_t>(data[0])
            | (static_cast<std::uint32_t>(data[1]) << 8)
            | (static_cast<std::uint32_t>(data[2]) << 16);

    if ((value & 0x00800000u) != 0u) {
        return static_cast<std::int32_t>(value | 0xFF000000u);
    }

    return static_cast<std::int32_t>(value);
}

std::int32_t readS32(const std::uint8_t* data) {
    return static_cast<std::int32_t>(readU32(data));
}

float readFloat32(const std::uint8_t* data) {
    const std::uint32_t bits = readU32(data);
    return std::bit_cast<float>(bits);
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
    std::uint16_t blockAlign = 0;
    std::uint16_t validBitsPerSample = 0;
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
            blockAlign = readU16(payload + 12);
            bitsPerSample = readU16(payload + 14);

            if (format == kExtensibleFormat) {
                if (chunkSize < 40) {
                    return std::nullopt;
                }

                validBitsPerSample = readU16(payload + 18);

                // The first two bytes of the SubFormat GUID contain the
                // underlying PCM/IEEE-float format; the remaining GUID bytes
                // are fixed for standard WAV subtype GUIDs.
                const std::uint32_t subFormatCode = readU16(payload + 24);
                const std::uint32_t guidTail0 = readU32(payload + 28);
                const std::uint32_t guidTail1 = readU32(payload + 32);

                if (guidTail0 != 0x00000000u || guidTail1 != 0x00100000u) {
                    return std::nullopt;
                }

                format = static_cast<std::uint16_t>(subFormatCode);
            }

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
            || (format != kPcmFormat && format != kFloatFormat)
            || (channels != 1u && channels != 2u)
            || sampleRate == 0u) {
        return std::nullopt;
    }

    if (format == kFloatFormat && bitsPerSample != 32u) {
        return std::nullopt;
    }

    if (format == kPcmFormat
            && bitsPerSample != 8u
            && bitsPerSample != 16u
            && bitsPerSample != 24u
            && bitsPerSample != 32u) {
        return std::nullopt;
    }

    const std::size_t bytesPerSample = bitsPerSample / 8u;
    const std::size_t expectedBlockAlign =
            static_cast<std::size_t>(channels) * bytesPerSample;

    if (bytesPerSample == 0
            || blockAlign != expectedBlockAlign
            || dataSize % expectedBlockAlign != 0) {
        return std::nullopt;
    }

    if (format == kPcmFormat
            && validBitsPerSample != 0
            && validBitsPerSample > bitsPerSample) {
        return std::nullopt;
    }

    const std::size_t frameCount = dataSize / expectedBlockAlign;
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
                    frame * expectedBlockAlign + channel * bytesPerSample;

            float value = 0.0f;

            if (format == kFloatFormat) {
                value = readFloat32(pcm + index);
            } else if (bitsPerSample == 8u) {
                value =
                        (static_cast<float>(pcm[index]) - 128.0f) / 128.0f;
            } else if (bitsPerSample == 16u) {
                const auto sample =
                        static_cast<std::int16_t>(readU16(pcm + index));
                value = static_cast<float>(sample) / 32768.0f;
            } else if (bitsPerSample == 24u) {
                value =
                        static_cast<float>(readS24(pcm + index))
                        / 8388608.0f;
            } else {
                const auto sample = readS32(pcm + index);
                value =
                        static_cast<float>(
                            static_cast<double>(sample) / 2147483648.0);
            }

            if (!std::isfinite(value)) {
                value = 0.0f;
            }

            result.interleaved[frame * channels + channel] =
                    std::clamp(value, -1.0f, 1.0f);
        }
    }

    return result;
}

} // namespace mpc::audio
