#include "Audio/SampleCrop.h"

#include <cassert>

namespace {

mpc::audio::SampleBuffer makeStereoSample() {
    mpc::audio::SampleBuffer sample;
    sample.sampleRate = 48000;
    sample.channelCount = 2;
    sample.interleaved = {
        0.0f, 0.1f,
        1.0f, 1.1f,
        2.0f, 2.1f,
        3.0f, 3.1f,
        4.0f, 4.1f
    };
    return sample;
}

void testCropPreservesFormatAndSelectedFrames() {
    const auto source = makeStereoSample();
    const auto cropped = mpc::audio::cropSampleToRegion(
            source,
            mpc::audio::SampleRegion{1, 4});

    assert(cropped.has_value());
    assert(cropped->sampleRate == 48000);
    assert(cropped->channelCount == 2);
    assert(cropped->frameCount() == 3);
    assert(cropped->sampleAt(0, 0) == 1.0f);
    assert(cropped->sampleAt(0, 1) == 1.1f);
    assert(cropped->sampleAt(2, 0) == 3.0f);
    assert(cropped->sampleAt(2, 1) == 3.1f);

    assert(source.frameCount() == 5);
    assert(source.sampleAt(0, 0) == 0.0f);
    assert(source.sampleAt(4, 1) == 4.1f);
}

void testInvalidCropIsRejected() {
    const auto source = makeStereoSample();

    assert(!mpc::audio::cropSampleToRegion(
            source,
            mpc::audio::SampleRegion{3, 3}).has_value());
    assert(!mpc::audio::cropSampleToRegion(
            source,
            mpc::audio::SampleRegion{4, 7}).has_value());
}

} // namespace

int main() {
    testCropPreservesFormatAndSelectedFrames();
    testInvalidCropIsRejected();
    return 0;
}
