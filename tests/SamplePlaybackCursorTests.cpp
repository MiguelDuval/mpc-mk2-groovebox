#include "Audio/SamplePlaybackCursor.h"

#include <cassert>
#include <limits>

int main() {
    double position = 0.0;

    assert(mpc::audio::advanceSamplePlaybackCursor(
            position, 1.0f, 4u, 4u));
    assert(position == 1.0);

    assert(mpc::audio::advanceSamplePlaybackCursor(
            position, 1.5f, 4u, 4u));
    assert(position == 2.5);

    assert(!mpc::audio::advanceSamplePlaybackCursor(
            position, 1.5f, 4u, 4u));
    assert(position == 4.0);

    double shorterSamplePosition = 2.0;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            shorterSamplePosition, 1.0f, 8u, 3u));
    assert(shorterSamplePosition == 3.0);

    double negativePosition = -1.0;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            negativePosition, 1.0f, 8u, 8u));
    assert(negativePosition == -1.0);

    double nonFinitePosition = std::numeric_limits<double>::quiet_NaN();
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            nonFinitePosition, 1.0f, 8u, 8u));

    double validPosition = 1.0;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            validPosition, 0.0f, 8u, 8u));
    assert(validPosition == 1.0);
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            validPosition, -1.0f, 8u, 8u));
    assert(validPosition == 1.0);
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            validPosition, std::numeric_limits<float>::infinity(), 8u, 8u));
    assert(validPosition == 1.0);

    double exhaustedPosition = 4.0;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            exhaustedPosition, 1.0f, 4u, 8u));
    assert(exhaustedPosition == 4.0);

    return 0;
}
