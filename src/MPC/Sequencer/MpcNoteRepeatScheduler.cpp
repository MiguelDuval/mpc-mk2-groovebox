#include "MpcNoteRepeatScheduler.h"
#include "MpcNoteRepeatTiming.h"

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>

namespace mpc::sequencer {

namespace {
std::int64_t nowNanos() noexcept {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(
            std::chrono::steady_clock::now().time_since_epoch()).count();
}
} // namespace

MpcNoteRepeatScheduler::MpcNoteRepeatScheduler(
        audio::AudioEngine& audio,
        MpcSequenceTransportClock& clock) noexcept
        : audio_(audio),
          clock_(clock),
          worker_(&MpcNoteRepeatScheduler::run, this) {
}

MpcNoteRepeatScheduler::~MpcNoteRepeatScheduler() {
    stopping_.store(true, std::memory_order_release);
    wake();
    if (worker_.joinable()) {
        worker_.join();
    }
}

void MpcNoteRepeatScheduler::setEnabled(bool enabled) noexcept {
    enabled_.store(enabled, std::memory_order_release);
    if (!enabled) {
        padHeld_.store(false, std::memory_order_release);
    }
    wake();
}

void MpcNoteRepeatScheduler::setPad(
        std::uint8_t padIndex,
        std::uint8_t velocity) noexcept {
    if (!enabled_.load(std::memory_order_acquire)
            || padIndex >= domain::kMaxProgramPads
            || velocity == 0) {
        return;
    }

    padIndex_.store(padIndex, std::memory_order_release);
    velocity_.store(velocity, std::memory_order_release);
    generation_.fetch_add(1, std::memory_order_acq_rel);
    padHeld_.store(true, std::memory_order_release);

    // The first hit is immediate. Subsequent hits are scheduled against
    // the same transport clock/grid instead of the Android UI timer.
    audio_.triggerPad(padIndex, velocity);
    wake();
}

void MpcNoteRepeatScheduler::clearPad() noexcept {
    padHeld_.store(false, std::memory_order_release);
    wake();
}

void MpcNoteRepeatScheduler::setRepeatGridIndex(
        std::int32_t index) noexcept {
    const auto clamped = std::clamp<std::int32_t>(
            index,
            0,
            static_cast<std::int32_t>(
                    note_repeat_timing::kRepeatRates.size()) - 1);
    const auto ticks =
            note_repeat_timing::repeatRateTicksForIndex(clamped);

    const auto oldIndex =
            repeatGridIndex_.exchange(
                    clamped,
                    std::memory_order_acq_rel);
    repeatGridTicks_.store(ticks, std::memory_order_release);

    if (oldIndex != clamped) {
        generation_.fetch_add(1, std::memory_order_acq_rel);
        wake();
    }
}

std::int64_t MpcNoteRepeatScheduler::intervalNanos(
        std::int64_t ticks,
        std::int64_t tempoMilliBpm) noexcept {
    const auto safeTicks = std::max<std::int64_t>(1, ticks);
    const auto bpm =
            static_cast<double>(clampTempoMilliBpm(tempoMilliBpm))
            / 1000.0;
    const auto seconds =
            (static_cast<double>(safeTicks) / 960.0)
            * (60.0 / bpm);
    const auto nanos = seconds * 1000000000.0;

    if (!std::isfinite(nanos)
            || nanos <= 0.0
            || nanos >= static_cast<double>(
                    std::numeric_limits<std::int64_t>::max())) {
        return 1;
    }

    return std::max<std::int64_t>(
            1,
            static_cast<std::int64_t>(std::llround(nanos)));
}

std::int64_t MpcNoteRepeatScheduler::ticksToNextBoundary(
        std::int64_t positionTicks,
        std::int64_t loopStartTicks,
        std::int64_t loopEndTicks,
        std::int32_t gridTicks) noexcept {
    return note_repeat_timing::ticksToNextBoundary(
            positionTicks,
            loopStartTicks,
            loopEndTicks,
            gridTicks);
}

MpcNoteRepeatScheduler::Clock::time_point
MpcNoteRepeatScheduler::nextDueFromTransport(
        Clock::time_point now,
        const SequenceTransportSnapshot& snapshot) const noexcept {
    const auto grid = repeatGridTicks();

    if (!snapshot.playing
            || snapshot.loopEndTicks <= snapshot.loopStartTicks) {
        return now + std::chrono::nanoseconds(
                intervalNanos(grid, snapshot.tempoMilliBpm));
    }

    const auto currentPosition =
            clock_.positionAtTimestamp(nowNanos());
    const auto ticks =
            ticksToNextBoundary(
                    currentPosition,
                    snapshot.loopStartTicks,
                    snapshot.loopEndTicks,
                    grid);

    return now + std::chrono::nanoseconds(
            intervalNanos(ticks, snapshot.tempoMilliBpm));
}

void MpcNoteRepeatScheduler::wake() noexcept {
    wakeCv_.notify_one();
}

void MpcNoteRepeatScheduler::run() noexcept {
    std::uint32_t seenGeneration = generation_.load(
            std::memory_order_acquire);
    auto nextDue = Clock::now();

    while (!stopping_.load(std::memory_order_acquire)) {
        if (!enabled_.load(std::memory_order_acquire)
                || !padHeld_.load(std::memory_order_acquire)) {
            std::unique_lock lock(wakeMutex_);
            wakeCv_.wait(lock, [this, seenGeneration] {
                return stopping_.load(std::memory_order_acquire)
                        || (enabled_.load(std::memory_order_acquire)
                            && padHeld_.load(std::memory_order_acquire))
                        || generation_.load(std::memory_order_acquire)
                                != seenGeneration;
            });
            nextDue = Clock::now();
            continue;
        }

        const auto generation =
                generation_.load(std::memory_order_acquire);
        if (generation != seenGeneration) {
            seenGeneration = generation;
            nextDue = nextDueFromTransport(
                    Clock::now(),
                    clock_.snapshot());
        }

        const auto snapshot = clock_.snapshot();
        const auto now = Clock::now();

        if (now >= nextDue) {
            if (enabled_.load(std::memory_order_acquire)
                    && padHeld_.load(std::memory_order_acquire)) {
                audio_.triggerPad(
                        padIndex_.load(std::memory_order_acquire),
                        velocity_.load(std::memory_order_acquire));
            }

            nextDue = nextDueFromTransport(Clock::now(), snapshot);
            continue;
        }

        std::unique_lock lock(wakeMutex_);
        const auto wakeAt = std::min(
                nextDue,
                now + std::chrono::milliseconds(8));
        wakeCv_.wait_until(lock, wakeAt);
    }
}

} // namespace mpc::sequencer
