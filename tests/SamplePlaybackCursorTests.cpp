#include "Audio/SamplePlaybackCursor.h"

#include <cassert>

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

    return 0;
}
