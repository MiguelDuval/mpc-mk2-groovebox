#include "Audio/WavSample.h"
#include "Audio/SampleExportPolicy.h"

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

    // RIFF chunks with an odd payload size are padded to an even boundary.
    bytes.push_back(0);

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


void testDecodeExtensiblePcm() {
    std::vector<std::uint8_t> bytes;
    const std::int16_t pcm = 16384;
    const std::uint32_t dataSize = sizeof(pcm);
    const std::uint32_t riffSize = 62u;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");
    appendTag(bytes, "fmt ");
    appendU32(bytes, 40);
    appendU16(bytes, 0xFFFE);
    appendU16(bytes, 1);
    appendU32(bytes, 16000);
    appendU32(bytes, 32000);
    appendU16(bytes, 2);
    appendU16(bytes, 16);
    appendU16(bytes, 22);
    appendU16(bytes, 16);
    appendU32(bytes, 0);
    appendU16(bytes, 1);
    appendU16(bytes, 0);
    appendU16(bytes, 0);
    appendU16(bytes, 0x0010);
    bytes.push_back(0x80);
    bytes.push_back(0x00);
    bytes.push_back(0x00);
    bytes.push_back(0xAA);
    bytes.push_back(0x00);
    bytes.push_back(0x38);
    bytes.push_back(0x9B);
    bytes.push_back(0x71);
    appendTag(bytes, "data");
    appendU32(bytes, dataSize);
    appendU16(bytes, static_cast<std::uint16_t>(pcm));

    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 16000);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 1);
    assert(sample->sampleAt(0, 0) > 0.49f);
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


void testSampleExportPolicy() {
    assert(mpc::audio::isValidSampleExportAddress(0, 0, 16, 8));
    assert(mpc::audio::isValidSampleExportAddress(15, 7, 16, 8));
    assert(!mpc::audio::isValidSampleExportAddress(16, 0, 16, 8));
    assert(!mpc::audio::isValidSampleExportAddress(0, 8, 16, 8));
    assert(!mpc::audio::isValidSampleExportAddress(16, 8, 16, 8));

    assert(!mpc::audio::hasExportableSample(nullptr));
    mpc::audio::SampleBuffer empty;
    assert(!mpc::audio::hasExportableSample(&empty));

    mpc::audio::SampleBuffer loaded;
    loaded.sampleRate = 16000;
    loaded.channelCount = 1;
    loaded.interleaved = {0.0f};
    assert(mpc::audio::hasExportableSample(&loaded));
}

void testEncodeWavRejectsInvalidBuffers() {
    mpc::audio::SampleBuffer empty;
    assert(mpc::audio::encodeWav(empty).empty());

    mpc::audio::SampleBuffer invalidChannels;
    invalidChannels.sampleRate = 16000;
    invalidChannels.channelCount = 3;
    invalidChannels.interleaved = {0.0f};
    assert(mpc::audio::encodeWav(invalidChannels).empty());

    mpc::audio::SampleBuffer invalidRate;
    invalidRate.channelCount = 1;
    invalidRate.interleaved = {0.0f};
    assert(mpc::audio::encodeWav(invalidRate).empty());

    mpc::audio::SampleBuffer incompleteFrame;
    incompleteFrame.sampleRate = 16000;
    incompleteFrame.channelCount = 2;
    incompleteFrame.interleaved = {0.0f};
    assert(mpc::audio::encodeWav(incompleteFrame).empty());

    // RIFF stores byteRate in 32 bits; reject rates whose channel/sample width
    // would overflow that field instead of silently wrapping the header.
    mpc::audio::SampleBuffer overflowingByteRate;
    overflowingByteRate.sampleRate = UINT32_MAX;
    overflowingByteRate.channelCount = 2;
    overflowingByteRate.interleaved = {0.0f, 0.0f};
    assert(mpc::audio::encodeWav(overflowingByteRate).empty());
}

void testEncodeWavStereoRoundTrip() {
    mpc::audio::SampleBuffer source;
    source.sampleRate = 48000;
    source.channelCount = 2;
    source.interleaved = {-1.0f, 0.25f, 0.5f, 1.0f};

    const auto encoded = mpc::audio::encodeWav(source);
    assert(encoded.size() == 44 + 4 * sizeof(float));

    // Stereo IEEE-float header: 48 kHz, 384000 bytes/sec, 8-byte frames.
    assert(encoded[20] == 3 && encoded[21] == 0);
    assert(encoded[22] == 2 && encoded[23] == 0);
    assert(encoded[24] == 0x80 && encoded[25] == 0xBB
            && encoded[26] == 0 && encoded[27] == 0);
    assert(encoded[28] == 0 && encoded[29] == 0xDC
            && encoded[30] == 0x05 && encoded[31] == 0);
    assert(encoded[32] == 8 && encoded[33] == 0);
    assert(encoded[40] == 16 && encoded[41] == 0
            && encoded[42] == 0 && encoded[43] == 0);

    const auto decoded = mpc::audio::decodeWav(encoded);
    assert(decoded.has_value());
    assert(decoded->sampleRate == 48000);
    assert(decoded->channelCount == 2);
    assert(decoded->frameCount() == 2);
    assert(decoded->sampleAt(0, 0) == -1.0f);
    assert(decoded->sampleAt(0, 1) == 0.25f);
    assert(decoded->sampleAt(1, 0) == 0.5f);
    assert(decoded->sampleAt(1, 1) == 1.0f);
}

void testEncodeWavRoundTrip() {
    const auto source = mpc::audio::decodeWav(makeMono16Wav());
    assert(source.has_value());

    const auto exported = mpc::audio::encodeWav(*source);
    assert(exported.size() == 44 + 5 * sizeof(float));

    // Verify RIFF/WAVE and the IEEE-float fmt chunk.
    assert(exported[0] == 'R');
    assert(exported[1] == 'I');
    assert(exported[2] == 'F');
    assert(exported[3] == 'F');
    assert(exported[8] == 'W');
    assert(exported[9] == 'A');
    assert(exported[10] == 'V');
    assert(exported[11] == 'E');
    assert(exported[4] == 56 && exported[5] == 0
            && exported[6] == 0 && exported[7] == 0);
    assert(exported[20] == 3 && exported[21] == 0);
    assert(exported[22] == 1 && exported[23] == 0);
    assert(exported[24] == 0x80 && exported[25] == 0x3E
            && exported[26] == 0 && exported[27] == 0);
    assert(exported[28] == 0 && exported[29] == 0xFA
            && exported[30] == 0 && exported[31] == 0);
    assert(exported[32] == 4 && exported[33] == 0);
    assert(exported[34] == 32 && exported[35] == 0);
    assert(exported[40] == 20 && exported[41] == 0
            && exported[42] == 0 && exported[43] == 0);

    // The first sample is -1.0f and must be serialized little-endian.
    assert(exported[44] == 0x00);
    assert(exported[45] == 0x00);
    assert(exported[46] == 0x80);
    assert(exported[47] == 0xBF);

    const auto decoded = mpc::audio::decodeWav(exported);
    assert(decoded.has_value());
    assert(decoded->sampleRate == 16000);
    assert(decoded->channelCount == 1);
    assert(decoded->frameCount() == 5);
    assert(decoded->sampleAt(0, 0) < -0.99f);
    assert(decoded->sampleAt(2, 0) == 0.0f);
    assert(decoded->sampleAt(4, 0) > 0.99f);
}

} // namespace

int main() {
    testDecode();
    testDecode24BitPcm();
    testDecode32BitPcm();
    testDecode32BitFloat();
    testDecodeExtensiblePcm();
    testInvalidHeader();
    testUnsupportedFormat();
    testSampleExportPolicy();
    testEncodeWavRejectsInvalidBuffers();
    testEncodeWavStereoRoundTrip();
    testEncodeWavRoundTrip();
    return 0;
}
