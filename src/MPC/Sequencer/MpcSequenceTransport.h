#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <atomic>
#include <cmath>
#include <cstdint>
#include <limits>

namespace mpc::sequencer {

struct SequenceTransportSnapshot final {
    std::int64_t positionTicks = 0;
    std::int64_t timestampNanos = 0;
    std::int64_t loopStartTicks = 0;
    std::int64_t loopEndTicks = 1;
    std::int64_t tempoMilliBpm = 120000;
    std::int32_t quantizeGridTicks = 240;
    bool loopEnabled = true;
    bool playing = false;
    std::int32_t queuedSequenceIndex = -1;
};

class MpcSequenceTransportClock final {
public:
    void start(
            const domain::Sequence& sequence,
            std::int64_t positionTicks,
            std::int64_t timestampNanos) noexcept {
        store(sequence, positionTicks, timestampNanos, true);
    }

    void stop(
            const domain::Sequence& sequence,
            std::int64_t positionTicks,
            std::int64_t timestampNanos) noexcept {
        store(sequence, positionTicks, timestampNanos, false);
    }

    void update(
            const domain::Sequence& sequence,
            std::int64_t positionTicks,
            std::int64_t timestampNanos,
            bool playing) noexcept {
        store(sequence, positionTicks, timestampNanos, playing);
    }

    void queueSequence(std::size_t sequenceIndex) noexcept {
        queuedSequenceIndex_.store(
                sequenceIndex > static_cast<std::size_t>(
                        std::numeric_limits<std::int32_t>::max())
                        ? -1
                        : static_cast<std::int32_t>(sequenceIndex),
                std::memory_order_release);
    }

    void clearQueuedSequence() noexcept {
        queuedSequenceIndex_.store(-1, std::memory_order_release);
    }

    [[nodiscard]] std::int32_t queuedSequenceIndex() const noexcept {
        return queuedSequenceIndex_.load(std::memory_order_acquire);
    }

    [[nodiscard]] SequenceTransportSnapshot snapshot() const noexcept {
        SequenceTransportSnapshot result;
        result.positionTicks =
                positionTicks_.load(std::memory_order_acquire);
        result.timestampNanos =
                timestampNanos_.load(std::memory_order_acquire);
        result.loopStartTicks =
                loopStartTicks_.load(std::memory_order_acquire);
        result.loopEndTicks =
                loopEndTicks_.load(std::memory_order_acquire);
        result.tempoMilliBpm =
                tempoMilliBpm_.load(std::memory_order_acquire);
        result.quantizeGridTicks =
                quantizeGridTicks_.load(std::memory_order_acquire);
        result.loopEnabled =
                loopEnabled_.load(std::memory_order_acquire);
        result.playing =
                playing_.load(std::memory_order_acquire);
        result.queuedSequenceIndex =
                queuedSequenceIndex_.load(std::memory_order_acquire);
        return result;
    }

    [[nodiscard]] std::int64_t positionAtTimestamp(
            std::int64_t timestampNanos) const noexcept {
        const auto state = snapshot();
        if (!state.playing || state.timestampNanos <= 0
                || timestampNanos <= state.timestampNanos) {
            return normalize(
                    state.positionTicks,
                    state.loopStartTicks,
                    state.loopEndTicks);
        }

        // Both timestamps are positive here and timestampNanos is newer, so
        // their difference is representable as a signed 64-bit duration.
        const auto elapsedNanos = timestampNanos - state.timestampNanos;
        const auto ticksPerSecond =
                static_cast<long double>(state.tempoMilliBpm) * 16.0L / 1000.0L;
        const auto exactDeltaTicks =
                static_cast<long double>(elapsedNanos) * ticksPerSecond
                / 1000000000.0L;

        std::int64_t deltaTicks = 0;
        const auto maxTicks = std::numeric_limits<std::int64_t>::max();
        if (exactDeltaTicks >= static_cast<long double>(maxTicks)) {
            // Avoid llround outside its representable range for hostile tempo
            // or timestamp input. The eventual transport position saturates.
            deltaTicks = maxTicks;
        } else if (std::isfinite(exactDeltaTicks) && exactDeltaTicks > 0.0L) {
            deltaTicks = std::llround(exactDeltaTicks);
        }

        const auto basePosition = std::max<std::int64_t>(0, state.positionTicks);
        const auto positiveDelta = std::max<std::int64_t>(0, deltaTicks);
        const auto advanced = positiveDelta > maxTicks - basePosition
                ? maxTicks
                : basePosition + positiveDelta;
        if (!state.loopEnabled) {
            return std::clamp<std::int64_t>(
                    advanced,
                    0,
                    state.loopEndTicks - 1);
        }

        return normalize(
                advanced,
                state.loopStartTicks,
                state.loopEndTicks);
    }

private:
    void store(
            const domain::Sequence& sequence,
            std::int64_t positionTicks,
            std::int64_t timestampNanos,
            bool playing) noexcept {
        const auto length = std::max<std::int64_t>(
                1,
                sequence.lengthTicks);
        auto loopStart = sequence.loopEnabled
                ? std::clamp<std::int64_t>(
                        sequence.loopStartTicks, 0, length - 1)
                : 0;
        auto loopEnd = sequence.loopEnabled
                ? std::clamp<std::int64_t>(
                        sequence.loopEndTicks, loopStart + 1, length)
                : length;
        if (loopEnd <= loopStart) {
            loopStart = 0;
            loopEnd = length;
        }

        loopStartTicks_.store(loopStart, std::memory_order_release);
        loopEndTicks_.store(loopEnd, std::memory_order_release);
        loopEnabled_.store(sequence.loopEnabled, std::memory_order_release);
        // Project loading validates finite numbers, while legacy or malformed
        // project data may still contain a finite tempo outside the supported
        // 20–300 BPM range. Never llround an out-of-range float into int64_t.
        const auto safeTempoBpm = std::isfinite(sequence.tempoBpm)
                        && sequence.tempoBpm >= 20.0
                        && sequence.tempoBpm <= 300.0
                ? sequence.tempoBpm
                : 120.0;
        tempoMilliBpm_.store(
                static_cast<std::int64_t>(
                        std::llround(safeTempoBpm * 1000.0)),
                std::memory_order_release);
        quantizeGridTicks_.store(
                std::clamp(sequence.quantizeGridTicks, 1, 3840),
                std::memory_order_release);
        const auto storedPosition = sequence.loopEnabled
                ? normalize(positionTicks, loopStart, loopEnd)
                : std::clamp<std::int64_t>(
                        positionTicks, 0, loopEnd - 1);
        positionTicks_.store(
                storedPosition,
                std::memory_order_release);
        timestampNanos_.store(timestampNanos, std::memory_order_release);
        playing_.store(playing, std::memory_order_release);
    }

    static std::int64_t normalize(
            std::int64_t ticks,
            std::int64_t loopStart,
            std::int64_t loopEnd) noexcept {
        if (loopEnd <= loopStart) {
            return loopStart;
        }

        const auto length = loopEnd - loopStart;
        // Compute (ticks - loopStart) mod length without the potentially
        // overflowing subtraction when ticks is INT64_MIN.
        const auto positionRemainder = ticks % length;
        const auto normalizedPosition = positionRemainder < 0
                ? positionRemainder + length
                : positionRemainder;
        const auto startRemainder = loopStart % length;
        const auto relative = normalizedPosition >= startRemainder
                ? normalizedPosition - startRemainder
                : length - (startRemainder - normalizedPosition);
        return loopStart + relative;
    }

    std::atomic<std::int64_t> positionTicks_{0};
    std::atomic<std::int64_t> timestampNanos_{0};
    std::atomic<std::int64_t> loopStartTicks_{0};
    std::atomic<std::int64_t> loopEndTicks_{1};
    std::atomic<std::int64_t> tempoMilliBpm_{120000};
    std::atomic<std::int32_t> quantizeGridTicks_{240};
    std::atomic<bool> loopEnabled_{true};
    std::atomic<bool> playing_{false};
    std::atomic<std::int32_t> queuedSequenceIndex_{-1};
};

} // namespace mpc::sequencer
