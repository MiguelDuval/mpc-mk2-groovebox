#include "Audio/WavSample.h"

#include <cassert>
#include <cstdint>
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

std::vector<std::uint8_t> makeStereo16Wav() {
    const std::vector<std::int16_t> pcm{-32768, 32767, -16384, 16384,
                                        0,       0,       16384, -16384};
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
    appendU16(bytes, 2);
    appendU32(bytes, 44100);
    appendU32(bytes, 44100 * 4);
    appendU16(bytes, 4);
    appendU16(bytes, 16);

    appendTag(bytes, "data");
    appendU32(bytes, dataSize);
    for (const auto sample : pcm) {
        appendU16(bytes, static_cast<std::uint16_t>(sample));
    }

    return bytes;
}

void testRoundTripMono() {
    const auto encoded = makeMono16Wav();
    const auto decoded = mpc::audio::decodeWav(encoded);

    assert(decoded.has_value());
    assert(decoded->sampleRate == 16000);
    assert(decoded->channelCount == 1);
    assert(decoded->frameCount() == 5);

    const auto reEncoded = mpc::audio::encodeWav(*decoded);
    assert(!reEncoded.empty());

    const auto reDecoded = mpc::audio::decodeWav(reEncoded);
    assert(reDecoded.has_value());
    assert(reDecoded->sampleRate == 16000);
    assert(reDecoded->channelCount == 1);
    assert(reDecoded->frameCount() == 5);
    assert(reDecoded->sampleAt(0, 0) < -0.99f);
    assert(reDecoded->sampleAt(2, 0) == 0.0f);
    assert(reDecoded->sampleAt(4, 0) > 0.99f);
}

void testRoundTripStereo() {
    const auto encoded = makeStereo16Wav();
    const auto decoded = mpc::audio::decodeWav(encoded);

    assert(decoded.has_value());
    assert(decoded->sampleRate == 44100);
    assert(decoded->channelCount == 2);
    assert(decoded->frameCount() == 4);

    const auto reEncoded = mpc::audio::encodeWav(*decoded);
    assert(!reEncoded.empty());

    const auto reDecoded = mpc::audio::decodeWav(reEncoded);
    assert(reDecoded.has_value());
    assert(reDecoded->sampleRate == 44100);
    assert(reDecoded->channelCount == 2);
    assert(reDecoded->frameCount() == 4);
    assert(reDecoded->sampleAt(0, 0) < -0.99f);
    assert(reDecoded->sampleAt(0, 1) > 0.99f);
}

void testEncodeEmptyBuffer() {
    mpc::audio::SampleBuffer empty;
    empty.sampleRate = 44100;
    empty.channelCount = 1;
    empty.interleaved.clear();

    const auto encoded = mpc::audio::encodeWav(empty);
    assert(encoded.empty());
}

void testEncodeInvalidChannelCount() {
    mpc::audio::SampleBuffer invalid;
    invalid.sampleRate = 44100;
    invalid.channelCount = 8; // unsupported
    invalid.interleaved.assign(8 * 100, 0.0f);

    const auto encoded = mpc::audio::encodeWav(invalid);
    assert(encoded.empty());
}

void testEncodeBoundaryValues() {
    // Test full-scale positive and negative values
    mpc::audio::SampleBuffer buf;
    buf.sampleRate = 44100;
    buf.channelCount = 1;
    buf.interleaved = {-1.0f, -0.5f, 0.0f, 0.5f, 1.0f};

    const auto encoded = mpc::audio::encodeWav(buf);
    assert(!encoded.empty());

    const auto decoded = mpc::audio::decodeWav(encoded);
    assert(decoded.has_value());
    assert(decoded->frameCount() == 5);
    assert(decoded->sampleAt(0, 0) <= -0.99f);
    assert(decoded->sampleAt(4, 0) >= 0.99f);
}

void testEncodeSilence() {
    mpc::audio::SampleBuffer buf;
    buf.sampleRate = 22050;
    buf.channelCount = 2;
    buf.interleaved.assign(100, 0.0f);

    const auto encoded = mpc::audio::encodeWav(buf);
    assert(!encoded.empty());

    const auto decoded = mpc::audio::decodeWav(encoded);
    assert(decoded.has_value());
    assert(decoded->sampleRate == 22050);
    assert(decoded->channelCount == 2);
    assert(decoded->frameCount() == 50);
    for (std::size_t f = 0; f < decoded->frameCount(); ++f) {
        assert(decoded->sampleAt(f, 0) == 0.0f);
        assert(decoded->sampleAt(f, 1) == 0.0f);
    }
}

void testEncodeClipping() {
    // Values outside [-1, 1] should be clamped
    mpc::audio::SampleBuffer buf;
    buf.sampleRate = 44100;
    buf.channelCount = 1;
    buf.interleaved = {-2.0f, 2.0f, 0.0f};

    const auto encoded = mpc::audio::encodeWav(buf);
    assert(!encoded.empty());

    const auto decoded = mpc::audio::decodeWav(encoded);
    assert(decoded.has_value());
    assert(decoded->sampleAt(0, 0) <= -0.99f);
    assert(decoded->sampleAt(1, 0) >= 0.99f);
    assert(decoded->sampleAt(2, 0) == 0.0f);
}

} // namespace

int main() {
    testRoundTripMono();
    testRoundTripStereo();
    testEncodeEmptyBuffer();
    testEncodeInvalidChannelCount();
    testEncodeBoundaryValues();
    testEncodeSilence();
    testEncodeClipping();
    return 0;
}