#include "Midi/TrackPerformanceMidiGesture.h"

#include <cassert>

int main() {
    mpc::midi::TrackPerformanceMidiGesture gesture;

    assert(!gesture.onPad(0u, true, 1'000));
    assert(!gesture.onPad(0u, false, 100'000'000));
    gesture.reset();

    assert(!gesture.onPad(3u, true, 1'000'000'000));
    const auto mute = gesture.onPad(3u, false, 1'400'000'000);
    assert(mute.has_value());
    assert(*mute == mpc::midi::TrackPerformanceGesture::Mute);

    assert(!gesture.onPad(3u, true, 2'000'000'000));
    const auto solo = gesture.onPad(3u, false, 2'500'000'000);
    assert(solo.has_value());
    assert(*solo == mpc::midi::TrackPerformanceGesture::Solo);

    assert(!gesture.onPad(16u, true, 3'000'000'000));
    assert(!gesture.onPad(16u, false, 3'600'000'000));

    gesture.reset();
    assert(!gesture.onPad(1u, true, 4'000'000'000));
    assert(!gesture.onPad(2u, true, 4'100'000'000));
    const auto secondPad = gesture.onPad(2u, false, 4'700'000'000);
    assert(secondPad.has_value());
    assert(*secondPad == mpc::midi::TrackPerformanceGesture::Solo);

    return 0;
}
