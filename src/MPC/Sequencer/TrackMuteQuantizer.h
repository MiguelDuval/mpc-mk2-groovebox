#pragma once

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <optional>
#include <span>

namespace mpc::sequencer {

enum class TrackMuteQuantizeMode : std::uint8_t {
    Off = 0,
    Sixteenth,
    Eighth,
    Quarter,
    Half,
    Bar,
    TwoBars
};

inline constexpr std::int64_t kTicksPerQuarter = 960;

inline constexpr std::int64_t trackMuteQuantizeTicks(
        TrackMuteQuantizeMode mode,
        std::int32_t numerator,
        std::int32_t denominator) noexcept {
    switch (mode) {
        case TrackMuteQuantizeMode::Off: return 0;
        case TrackMuteQuantizeMode::Sixteenth: return 240;
        case TrackMuteQuantizeMode::Eighth: return 480;
        case TrackMuteQuantizeMode::Quarter: return kTicksPerQuarter;
        case TrackMuteQuantizeMode::Half: return kTicksPerQuarter * 2;
        case TrackMuteQuantizeMode::Bar:
            if (numerator <= 0 || denominator <= 0) return 0;
            return (kTicksPerQuarter * 4 * numerator) / denominator;
        case TrackMuteQuantizeMode::TwoBars:
            if (numerator <= 0 || denominator <= 0) return 0;
            return 2 * ((kTicksPerQuarter * 4 * numerator) / denominator);
    }
    return 0;
}

inline constexpr const char* trackMuteQuantizeLabel(
        TrackMuteQuantizeMode mode) noexcept {
    switch (mode) {
        case TrackMuteQuantizeMode::Off: return "OFF";
        case TrackMuteQuantizeMode::Sixteenth: return "1/16";
        case TrackMuteQuantizeMode::Eighth: return "1/8";
        case TrackMuteQuantizeMode::Quarter: return "1/4";
        case TrackMuteQuantizeMode::Half: return "1/2";
        case TrackMuteQuantizeMode::Bar: return "1 BAR";
        case TrackMuteQuantizeMode::TwoBars: return "2 BAR";
    }
    return "OFF";
}

class TrackMuteQuantizer final {
public:
    static constexpr std::size_t kCapacity = 32;

    struct Command final {
        std::size_t trackIndex = 0;
        bool targetMuted = false;
        std::int64_t ticksUntilApply = 0;
    };

    void setMode(TrackMuteQuantizeMode mode) noexcept { mode_ = mode; }
    [[nodiscard]] TrackMuteQuantizeMode mode() const noexcept { return mode_; }

    [[nodiscard]] bool enqueue(
            std::size_t trackIndex,
            bool targetMuted,
            std::int64_t currentPositionTicks,
            std::int64_t loopStartTicks,
            std::int64_t loopEndTicks,
            std::int64_t resolutionTicks) noexcept {
        if (resolutionTicks <= 0 || loopEndTicks <= loopStartTicks) return false;

        const auto position = std::clamp(
                currentPositionTicks,
                loopStartTicks,
                loopEndTicks - 1);
        const auto relative = position - loopStartTicks;
        const auto remainder = relative % resolutionTicks;
        const auto boundaryDelta = remainder == 0
                ? resolutionTicks
                : resolutionTicks - remainder;
        const auto ticksUntilApply =
                std::max<std::int64_t>(1, boundaryDelta);

        // Live Track Mute is a single pending intent per track. Repeated
        // taps before the boundary change the target but never move the
        // already-committed musical boundary.
        for (std::size_t i = 0; i < size_; ++i) {
            if (commands_[i].trackIndex == trackIndex) {
                commands_[i].targetMuted = targetMuted;
                return true;
            }
        }

        if (size_ >= commands_.size()) return false;
        commands_[size_++] = Command{trackIndex, targetMuted, ticksUntilApply};
        return true;
    }

    void advance(std::int64_t deltaTicks) noexcept {
        if (deltaTicks <= 0) return;
        for (std::size_t i = 0; i < size_; ++i) {
            commands_[i].ticksUntilApply = std::max<std::int64_t>(
                    0,
                    commands_[i].ticksUntilApply - deltaTicks);
        }
    }

    [[nodiscard]] std::int64_t nextDueTicks() const noexcept {
        if (size_ == 0) return std::numeric_limits<std::int64_t>::max();
        auto result = std::numeric_limits<std::int64_t>::max();
        for (std::size_t i = 0; i < size_; ++i) {
            result = std::min(result, commands_[i].ticksUntilApply);
        }
        return result;
    }

    [[nodiscard]] std::size_t takeDue(std::span<Command> output) noexcept {
        std::size_t written = 0;
        std::size_t i = 0;
        while (i < size_) {
            if (commands_[i].ticksUntilApply > 0) {
                ++i;
                continue;
            }
            if (written < output.size()) output[written++] = commands_[i];
            for (std::size_t j = i + 1; j < size_; ++j) {
                commands_[j - 1] = commands_[j];
            }
            --size_;
        }
        return written;
    }

    [[nodiscard]] std::optional<bool> pendingTargetForTrack(
            std::size_t trackIndex) const noexcept {
        std::optional<bool> result;
        auto earliest = std::numeric_limits<std::int64_t>::max();
        for (std::size_t i = 0; i < size_; ++i) {
            const auto& command = commands_[i];
            if (command.trackIndex != trackIndex
                    || command.ticksUntilApply >= earliest) continue;
            earliest = command.ticksUntilApply;
            result = command.targetMuted;
        }
        return result;
    }

    void clear() noexcept { size_ = 0; }

private:
    TrackMuteQuantizeMode mode_ = TrackMuteQuantizeMode::Off;
    std::array<Command, kCapacity> commands_{};
    std::size_t size_ = 0;
};

} // namespace mpc::sequencer
