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

// Encodes mono 16-bit PCM as a RIFF/WAVE byte buffer. Mirrors the format
// written by AudioEngine::exportSampleToWav but does not depend on the
// AudioEngine class or the oboe library.
std::vector<std::uint8_t> encodeMono16Wav(const std::vector<std::int16_t>& pcm,
                                          std::uint32_t sampleRate) {
    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * sizeof(std::int16_t));
    const std::uint32_t riffSize = 36u + dataSize;
    const std::uint32_t byteRate = sampleRate * 2;

    std::vector<std::uint8_t> bytes;
    bytes.reserve(36 + dataSize);

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");

    appendTag(bytes, "fmt ");
    appendU32(bytes, 16);
    appendU16(bytes, 1);     // PCM
    appendU16(bytes, 1);     // mono
    appendU32(bytes, sampleRate);
    appendU32(bytes, byteRate);
    appendU16(bytes, 2);     // block align
    appendU16(bytes, 16);    // bits per sample

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

    const std::vector<std::int16_t> pcm{-100, 0, 100};
    const std::uint32_t dataSize =
            static_cast<std::uint32_t>(pcm.size() * sizeof(std::int16_t));
    const std::uint32_t riffSize = 36u + dataSize + 24;

    appendTag(bytes, "RIFF");
    appendU32(bytes, riffSize);
    appendTag(bytes, "WAVE");

    // fmt chunk (extensible, 40 bytes)
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
    // Subformat GUID: 01000000-0000-0010-8000-00AA00389B71 (PCM)
    const std::uint8_t subformat[16] = {
        0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00,
        0x80, 0x00, 0x00, 0xAA, 0x00, 0x38, 0x9B, 0x71};
    bytes.insert(bytes.end(), subformat, subformat + 16);

    appendTag(bytes, "data");
    appendU32(bytes, dataSize);
    for (const auto sample : pcm) {
        appendU16(bytes, static_cast<std::uint16_t>(sample));
    }

    const auto sample = mpc::audio::decodeWav(bytes);

    assert(sample.has_value());
    assert(sample->sampleRate == 16000);
    assert(sample->channelCount == 1);
    assert(sample->frameCount() == 3);
    assert(sample->sampleAt(0, 0) > -0.01f);
    assert(sample->sampleAt(0, 0) < 0.0f);
    assert(sample->sampleAt(1, 0) == 0.0f);
    assert(sample->sampleAt(2, 0) > 0.0f);
    assert(sample->sampleAt(2, 0) < 0.01f);
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

void testExportWavEmpty() {
    // Exporting an empty PCM buffer produces a WAV with an empty data chunk.
    const std::vector<std::int16_t> pcm{};
    const auto wavBytes = encodeMono16Wav(pcm, 16000);

    assert(!wavBytes.empty());
    assert(wavBytes[0] == 'R');
    assert(wavBytes[1] == 'I');
    assert(wavBytes[2] == 'F');
    assert(wavBytes[3] == 'F');
    assert(wavBytes[8] == 'W');
    assert(wavBytes[9] == 'A');
    assert(wavBytes[10] == 'V');
    assert(wavBytes[11] == 'E');

    const auto decoded = mpc::audio::decodeWav(wavBytes);
    // decodeWav rejects empty data (frameCount == 0), which is expected.
    // The export format itself is structurally valid (verified above).
    assert(!decoded.has_value());
}

void testExportWavRoundTrip() {
    // Encode mono 16-bit PCM as WAV, then decode and verify round-trip.
    const std::vector<std::int16_t> pcm{-32768, -16384, 0, 16384, 32767};
    const auto wavBytes = encodeMono16Wav(pcm, 16000);

    assert(!wavBytes.empty());
    assert(wavBytes[0] == 'R');
    assert(wavBytes[1] == 'I');
    assert(wavBytes[2] == 'F');
    assert(wavBytes[3] == 'F');
    assert(wavBytes[8] == 'W');
    assert(wavBytes[9] == 'A');
    assert(wavBytes[10] == 'V');
    assert(wavBytes[11] == 'E');

    // Verify fmt chunk: PCM (format tag 1)
    assert(wavBytes[20] == 1);

    // Decode the exported WAV and verify round-trip.
    const auto decoded = mpc::audio::decodeWav(wavBytes);
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
    testExportWavEmpty();
    testExportWavRoundTrip();
    return 0;
}
