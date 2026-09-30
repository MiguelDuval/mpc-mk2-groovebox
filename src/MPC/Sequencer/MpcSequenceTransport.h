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

        const auto elapsedNanos = timestampNanos - state.timestampNanos;
        const auto bpm = static_cast<double>(state.tempoMilliBpm) / 1000.0;
        const auto ticksPerSecond =
                bpm * 960.0 / 60.0;
        const auto deltaTicks = static_cast<std::int64_t>(
                std::llround(
                        static_cast<double>(elapsedNanos)
                        * ticksPerSecond
                        / 1000000000.0));

        const auto advanced =
                state.positionTicks + std::max<std::int64_t>(0, deltaTicks);
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
        tempoMilliBpm_.store(
                static_cast<std::int64_t>(
                        std::llround(sequence.tempoBpm * 1000.0)),
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
        const auto relative = (ticks - loopStart) % length;
        return loopStart + (relative < 0 ? relative + length : relative);
    }

    std::atomic<std::int64_t> positionTicks_{0};
    std::atomic<std::int64_t> timestampNanos_{0};
    std::atomic<std::int64_t> loopStartTicks_{0};
    std::atomic<std::int64_t> loopEndTicks_{1};
    std::atomic<std::int64_t> tempoMilliBpm_{120000};
    std::atomic<bool> loopEnabled_{true};
    std::atomic<bool> playing_{false};
    std::atomic<std::int32_t> queuedSequenceIndex_{-1};
};

} // namespace mpc::sequencer
