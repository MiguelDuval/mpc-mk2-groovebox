#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/Sequencer/MpcSequenceTransport.h"

#include <atomic>
#include <chrono>
#include <condition_variable>
#include <cstdint>
#include <mutex>
#include <thread>

namespace mpc::sequencer {

class MpcNoteRepeatScheduler final {
public:
    MpcNoteRepeatScheduler(
            audio::AudioEngine& audio,
            MpcSequenceTransportClock& clock) noexcept;

    ~MpcNoteRepeatScheduler();

    MpcNoteRepeatScheduler(const MpcNoteRepeatScheduler&) = delete;
    MpcNoteRepeatScheduler& operator=(const MpcNoteRepeatScheduler&) = delete;

    void setEnabled(bool enabled) noexcept;
    void setPad(std::uint8_t padIndex, std::uint8_t velocity) noexcept;
    void clearPad() noexcept;

    void setRepeatGridIndex(std::int32_t index) noexcept;
    [[nodiscard]] std::int32_t repeatGridIndex() const noexcept {
        return repeatGridIndex_.load(std::memory_order_acquire);
    }
    [[nodiscard]] std::int32_t repeatGridTicks() const noexcept {
        return repeatGridTicks_.load(std::memory_order_acquire);
    }

    [[nodiscard]] static std::int64_t intervalNanos(
            std::int64_t ticks,
            std::int64_t tempoMilliBpm) noexcept;

    [[nodiscard]] static std::int64_t ticksToNextBoundary(
            std::int64_t positionTicks,
            std::int64_t loopStartTicks,
            std::int64_t loopEndTicks,
            std::int32_t gridTicks) noexcept;

private:
    using Clock = std::chrono::steady_clock;

    void wake() noexcept;
    void run() noexcept;
    [[nodiscard]] Clock::time_point nextDueFromTransport(
            Clock::time_point now,
            const SequenceTransportSnapshot& snapshot) const noexcept;

    audio::AudioEngine& audio_;
    MpcSequenceTransportClock& clock_;

    std::atomic<bool> enabled_{false};
    std::atomic<bool> padHeld_{false};
    std::atomic<std::uint8_t> padIndex_{0};
    std::atomic<std::uint8_t> velocity_{0};
    std::atomic<std::int32_t> repeatGridIndex_{2};
    std::atomic<std::int32_t> repeatGridTicks_{240};
    std::atomic<std::uint32_t> generation_{0};
    std::atomic<bool> stopping_{false};

    std::mutex wakeMutex_;
    std::condition_variable wakeCv_;
    std::thread worker_;
};

} // namespace mpc::sequencer
