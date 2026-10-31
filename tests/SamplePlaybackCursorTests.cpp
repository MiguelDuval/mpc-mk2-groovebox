#include "Audio/SamplePlaybackCursor.h"

#include <cassert>
#include <cmath>
#include <cstdint>
#include <limits>

int main() {
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(0u) == 1u);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(0u) == -1);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(1u) == 0);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(129u) == 128);
    const auto largestFrame = static_cast<std::size_t>(
            std::numeric_limits<std::uint32_t>::max() - 1u);
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(largestFrame)
            == std::numeric_limits<std::uint32_t>::max());
    assert(mpc::audio::encodeSamplePlaybackCursorFrame(
            static_cast<std::size_t>(
                    std::numeric_limits<std::uint32_t>::max())) == 0u);
    assert(mpc::audio::decodeSamplePlaybackCursorFrame(
            std::numeric_limits<std::uint32_t>::max())
            == static_cast<std::int64_t>(
                    std::numeric_limits<std::uint32_t>::max() - 1u));

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

    // A step that overshoots the selected region must stop at the earliest
    // region/sample boundary, not leave an invalid cursor for future indexing.
    double overshootRegionPosition = 1.25;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            overshootRegionPosition, 100000.0f, 7u, 12u));
    assert(overshootRegionPosition == 7.0);

    double overshootSamplePosition = 1.25;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            overshootSamplePosition, 100000.0f, 20u, 5u));
    assert(overshootSamplePosition == 5.0);

    double overshootStartPosition = 0.25;
    assert(!mpc::audio::advanceSamplePlaybackCursor(
            overshootStartPosition, -100000.0f, 10u, 12u));
    assert(overshootStartPosition == 0.0);

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
