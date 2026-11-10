#include "Audio/SamplePlaybackCursor.h"

#include <cassert>
#include <cmath>
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

    // Invalid numeric inputs must not corrupt the last valid cursor.
    double preservedPosition = 1.25;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            preservedPosition, std::numeric_limits<float>::quiet_NaN(), 4u, 4u));
    assert(preservedPosition == 1.25);
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            preservedPosition, std::numeric_limits<float>::infinity(), 4u, 4u));
    assert(preservedPosition == 1.25);
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            preservedPosition, -std::numeric_limits<float>::infinity(), 4u, 4u));
    assert(preservedPosition == 1.25);

    double invalidPosition = std::numeric_limits<double>::quiet_NaN();
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            invalidPosition, 1.0f, 4u, 4u));
    assert(std::isnan(invalidPosition));
    double infinitePosition = std::numeric_limits<double>::infinity();
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            infinitePosition, 1.0f, 4u, 4u));
    assert(std::isinf(infinitePosition));

    return 0;
}
