#pragma once

#include <array>
#include <cstdint>
#include <optional>

namespace mpc::midi {

enum class TrackPerformanceGesture : std::uint8_t {
    Mute,
    Solo,
};

class TrackPerformanceMidiGesture {
public:
    static constexpr std::int64_t kLongPressNanos = 500'000'000;

    TrackPerformanceMidiGesture() {
        reset();
    }

    void reset() noexcept {
        pressed_.fill(false);
        pressTimestamps_.fill(-1);
    }

    std::optional<TrackPerformanceGesture> onPad(
            std::uint8_t padIndex,
            bool pressed,
            std::int64_t timestamp) noexcept {
        if (padIndex >= pressed_.size()) {
            return std::nullopt;
        }

        if (pressed) {
            if (!pressed_[padIndex]) {
                pressed_[padIndex] = true;
                pressTimestamps_[padIndex] = timestamp;
            }
            return std::nullopt;
        }

        if (!pressed_[padIndex]) {
            return std::nullopt;
        }

        const auto started = pressTimestamps_[padIndex];
        pressed_[padIndex] = false;
        pressTimestamps_[padIndex] = -1;

        if (started >= 0
                && timestamp >= started
                && timestamp - started >= kLongPressNanos) {
            return TrackPerformanceGesture::Solo;
        }

        return TrackPerformanceGesture::Mute;
    }

private:
    std::array<bool, 16> pressed_{};
    std::array<std::int64_t, 16> pressTimestamps_{};
};

} // namespace mpc::midi
