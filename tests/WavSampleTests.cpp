#include "Audio/WavSample.h"

#include <cassert>
#include <cstdint>
#include <initializer_list>
#include <cstring>
#include <vector>

namespace {

void appendU16(std::vector<std::uint8_t>& bytes, std::uint16_t value) {
    bytes.push_back(static_cast<std::uint8_t>(value & 0xFF));
    bytes.push_back(static_cast<std::uint8_t>((value >> 8) & 0xFF));
}

void appendU32(std::vector<std::uint8_t>& bytes, std::uint32_t value) {
    bytes.push_back(static_cast<std::uint8_t>(value & 0xFF));
    bytes.push_back(static_cast<std::uint8_t>((value >> 8) & 0xFF));
    bytes.push_back(static_cast<std::uint8_t>((value >> 16) & 0xFF));
    bytes.push_back(static_cast<std::uint8_t>((value >> 24) & 0xFF));
}

void appendTag(std::vector<std::uint8_t>& bytes, const char* tag) {
    bytes.insert(bytes.end(), tag, tag + 4);
}

std::vector<std::uint8_t> makeMono16Wav() {
    const std::vector<std::int16_t> pcm{-32768, -16384, 0, 16384, 32767};
    std::vector<std::uint8_t> bytes;

    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * sizeof(std::int16_t));
    const std::uint32_t riffSize = 36u + dataSize;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");

    appendTag(bytes, "fmt ");
    appendU32(bytes, 16);
    appendU16(bytes, 1);
    appendU16(bytes, 1);
    appendU32(bytes, 16000);
    appendU32(bytes, 16000 * 2);
    appendU16(bytes, 2);
    appendU16(bytes, 16);

    appendTag(bytes, "data");
    appendU32(bytes, dataSize);
    for (const auto sample : pcm) {
        appendU16(bytes, static_cast<std::uint16_t>(sample));
    }

    return bytes;
}

void testDecode() {
    const auto bytes = makeMono16Wav();
    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 16000);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 5);
    assert(sample->sampleAt(0, 0) < -0.99f);
    assert(sample->sampleAt(2, 0) == 0.0f);
    assert(sample->sampleAt(4, 0) > 0.99f);
}


void testDecode24BitPcm() {
    std::vector<std::uint8_t> bytes;

    const std::vector<std::int32_t> pcm{-8388608, 0, 8388607};
    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * 3);
    const std::uint32_t riffSize = 36u + dataSize;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");
    appendTag(bytes, "fmt ");
    appendU32(bytes, 16);
    appendU16(bytes, 1);
    appendU16(bytes, 1);
    appendU32(bytes, 44100);
    appendU32(bytes, 44100 * 3);
    appendU16(bytes, 3);
    appendU16(bytes, 24);
    appendTag(bytes, "data");
    appendU32(bytes, dataSize);

    for (const auto sample : pcm) {
        const auto value = static_cast<std::uint32_t>(sample) & 0x00FFFFFFu;
        bytes.push_back(static_cast<std::uint8_t>(value & 0xFF));
        bytes.push_back(static_cast<std::uint8_t>((value >> 8) & 0xFF));
        bytes.push_back(static_cast<std::uint8_t>((value >> 16) & 0xFF));
    }

    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 44100);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 3);
    assert(sample->sampleAt(0, 0) < -0.99f);
    assert(sample->sampleAt(1, 0) == 0.0f);
    assert(sample->sampleAt(2, 0) > 0.99f);
}

void testDecode32BitPcm() {
    std::vector<std::uint8_t> bytes;

    const std::vector<std::int32_t> pcm{-2147483648, 0, 2147483647};
    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * sizeof(std::int32_t));
    const std::uint32_t riffSize = 36u + dataSize;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");
    appendTag(bytes, "fmt ");
    appendU32(bytes, 16);
    appendU16(bytes, 1);
    appendU16(bytes, 1);
    appendU32(bytes, 48000);
    appendU32(bytes, 48000 * 4);
    appendU16(bytes, 4);
    appendU16(bytes, 32);
    appendTag(bytes, "data");
    appendU32(bytes, dataSize);

    for (const auto sample : pcm) {
        appendU32(bytes, static_cast<std::uint32_t>(sample));
    }

    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 48000);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 3);
    assert(sample->sampleAt(0, 0) < -0.99f);
    assert(sample->sampleAt(1, 0) == 0.0f);
    assert(sample->sampleAt(2, 0) > 0.99f);
}

void appendF32(std::vector<std::uint8_t>& bytes, float value) {
    std::uint32_t bits = 0;
    static_assert(sizeof(bits) == sizeof(value));
    std::memcpy(&bits, &value, sizeof(bits));
    appendU32(bytes, bits);
}

void testDecode32BitFloat() {
    std::vector<std::uint8_t> bytes;
    const std::vector<float> pcm{-1.0f, -0.25f, 0.5f, 1.0f};
    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * sizeof(float));
    const std::uint32_t riffSize = 36u + dataSize;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");
    appendTag(bytes, "fmt ");
    appendU32(bytes, 16);
    appendU16(bytes, 3);
    appendU16(bytes, 1);
    appendU32(bytes, 44100);
    appendU32(bytes, 44100 * 4);
    appendU16(bytes, 4);
    appendU16(bytes, 32);
    appendTag(bytes, "data");
    appendU32(bytes, dataSize);

    for (const auto sample : pcm) {
        appendF32(bytes, sample);
    }

    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 44100);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 4);
    assert(sample->sampleAt(0, 0) == -1.0f);
    assert(sample->sampleAt(1, 0) == -0.25f);
    assert(sample->sampleAt(2, 0) == 0.5f);
    assert(sample->sampleAt(3, 0) == 1.0f);
}

void testInvalidHeader() {
    const std::vector<std::uint8_t> bytes{'N', 'O', 'P', 'E'};
    assert(!mpc::audio::decodeWav(bytes).has_value());
}

void testUnsupportedFormat() {
    auto bytes = makeMono16Wav();
    bytes[20] = 3;
    bytes[21] = 0;
    assert(!mpc::audio::decodeWav(bytes).has_value());
}

} // namespace

int main() {
    testDecode();
    testDecode24BitPcm();
    testDecode32BitPcm();
    testDecode32BitFloat();
    testInvalidHeader();
    testUnsupportedFormat();
    return 0;
}
