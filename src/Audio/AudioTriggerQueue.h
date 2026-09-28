#pragma once

#include <array>
#include <atomic>
#include <cstddef>
#include <cstdint>
#include <type_traits>

namespace mpc::audio {

struct AudioTriggerEvent final {
    std::uint8_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::int32_t offsetFrames = 0;
};

template <typename T, std::size_t Capacity>
class BoundedMpmcQueue final {
    static_assert(Capacity >= 2 && (Capacity & (Capacity - 1)) == 0,
            "Queue capacity must be a power of two");
    static_assert(std::is_trivially_copyable_v<T>,
            "Queue payload must be trivially copyable");

    struct Cell final {
        std::atomic<std::uint32_t> sequence;
        T value{};
    };

public:
    BoundedMpmcQueue() noexcept {
        for (std::uint32_t index = 0;
                index < static_cast<std::uint32_t>(Capacity);
                ++index) {
            buffer_[index].sequence.store(
                    index,
                    std::memory_order_relaxed);
        }
    }

    BoundedMpmcQueue(const BoundedMpmcQueue&) = delete;
    BoundedMpmcQueue& operator=(const BoundedMpmcQueue&) = delete;

    [[nodiscard]] bool tryEnqueue(const T& value) noexcept {
        std::uint32_t position =
                enqueuePosition_.load(std::memory_order_relaxed);

        for (;;) {
            Cell& cell = buffer_[position & kMask];
            const std::uint32_t sequence =
                    cell.sequence.load(std::memory_order_acquire);
            const std::int32_t difference =
                    static_cast<std::int32_t>(sequence - position);

            if (difference == 0) {
                if (enqueuePosition_.compare_exchange_weak(
                            position,
                            position + 1,
                            std::memory_order_relaxed,
                            std::memory_order_relaxed)) {
                    cell.value = value;
                    cell.sequence.store(
                            position + 1,
                            std::memory_order_release);
                    return true;
                }
                continue;
            }

            if (difference < 0) {
                return false;
            }

            position = enqueuePosition_.load(std::memory_order_relaxed);
        }
    }

    [[nodiscard]] bool tryDequeue(T& value) noexcept {
        std::uint32_t position =
                dequeuePosition_.load(std::memory_order_relaxed);

        for (;;) {
            Cell& cell = buffer_[position & kMask];
            const std::uint32_t sequence =
                    cell.sequence.load(std::memory_order_acquire);
            const std::int32_t difference =
                    static_cast<std::int32_t>(
                            sequence - (position + 1));

            if (difference == 0) {
                if (dequeuePosition_.compare_exchange_weak(
                            position,
                            position + 1,
                            std::memory_order_relaxed,
                            std::memory_order_relaxed)) {
                    value = cell.value;
                    cell.sequence.store(
                            position + static_cast<std::uint32_t>(Capacity),
                            std::memory_order_release);
                    return true;
                }
                continue;
            }

            if (difference < 0) {
                return false;
            }

            position = dequeuePosition_.load(std::memory_order_relaxed);
        }
    }

private:
    static constexpr std::uint32_t kMask =
            static_cast<std::uint32_t>(Capacity - 1);

    std::array<Cell, Capacity> buffer_{};
    alignas(64) std::atomic<std::uint32_t> enqueuePosition_{0};
    alignas(64) std::atomic<std::uint32_t> dequeuePosition_{0};
};

inline constexpr std::size_t kAudioTriggerQueueCapacity = 256;
using AudioTriggerQueue =
        BoundedMpmcQueue<AudioTriggerEvent, kAudioTriggerQueueCapacity>;

} // namespace mpc::audio
