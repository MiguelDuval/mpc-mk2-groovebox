#include "Audio/SamplePlaybackCursor.h"

#include <cassert>
#include <cstdint>
#include <limits>

int main() {
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(0u) == 1u);
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(128u) == 129u);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(0u) == -1);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(129u) == 128);
    const auto lastFrame = static_cast<std::size_t>(
            std::numeric_limits<std::uint32_t>::max()) - 1u;
    const auto encodedLastFrame = mpc::audio::encodeSamplePlaybackCursorFrame(lastFrame);
    assert(encodedLastFrame == std::numeric_limits<std::uint32_t>::max());
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(encodedLastFrame)
            == static_cast<std::int64_t>(lastFrame));
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(static_cast<std::size_t>(
            std::numeric_limits<std::uint32_t>::max())) == 0u);

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
