#include "Audio/AudioTriggerQueue.h"

#include <array>
#include <cassert>
#include <cstdint>
#include <thread>
#include <vector>

int main() {
    mpc::audio::AudioTriggerQueue queue;

    mpc::audio::AudioTriggerEvent initiallyEmpty;
    assert(!queue.tryDequeue(initiallyEmpty));

    for (std::uint8_t index = 0; index < 16; ++index) {
        assert(queue.tryEnqueue(
                mpc::audio::AudioTriggerEvent{
                    index,
                    static_cast<std::uint8_t>(80 + index),
                    static_cast<std::int32_t>(index * 3)}));
    }

    for (std::uint8_t index = 0; index < 16; ++index) {
        mpc::audio::AudioTriggerEvent event;
        assert(queue.tryDequeue(event));
        assert(event.padIndex == index);
        assert(event.velocity == static_cast<std::uint8_t>(80 + index));
        assert(event.offsetFrames == static_cast<std::int32_t>(index * 3));
    }

    mpc::audio::AudioTriggerEvent empty;
    assert(!queue.tryDequeue(empty));

    constexpr std::size_t capacity = mpc::audio::kAudioTriggerQueueCapacity;
    for (std::size_t index = 0; index < capacity; ++index) {
        assert(queue.tryEnqueue(
                mpc::audio::AudioTriggerEvent{
                    static_cast<std::uint8_t>(index & 0x0Fu),
                    static_cast<std::uint8_t>(index & 0x7Fu)}));
    }
    assert(!queue.tryEnqueue(mpc::audio::AudioTriggerEvent{1, 127}));

    for (std::size_t index = 0; index < capacity; ++index) {
        mpc::audio::AudioTriggerEvent event;
        assert(queue.tryDequeue(event));
        assert(event.padIndex == static_cast<std::uint8_t>(index & 0x0Fu));
        assert(event.velocity == static_cast<std::uint8_t>(index & 0x7Fu));
    }
    assert(!queue.tryDequeue(empty));

    mpc::audio::AudioTriggerQueue concurrentQueue;
    constexpr std::size_t producerCount = 4;
    constexpr std::size_t eventsPerProducer = 32;
    std::vector<std::thread> producers;
    producers.reserve(producerCount);

    for (std::size_t producer = 0; producer < producerCount; ++producer) {
        producers.emplace_back([&concurrentQueue, producer] {
            for (std::size_t index = 0; index < eventsPerProducer; ++index) {
                while (!concurrentQueue.tryEnqueue(
                        mpc::audio::AudioTriggerEvent{
                            static_cast<std::uint8_t>(producer),
                            static_cast<std::uint8_t>(index)})) {
                    std::this_thread::yield();
                }
            }
        });
    }

    std::array<std::array<bool, eventsPerProducer>, producerCount> seen{};
    std::size_t received = 0;
    while (received < producerCount * eventsPerProducer) {
        mpc::audio::AudioTriggerEvent event;
        if (!concurrentQueue.tryDequeue(event)) {
            std::this_thread::yield();
            continue;
        }

        assert(event.padIndex < producerCount);
        assert(event.velocity < eventsPerProducer);
        assert(!seen[event.padIndex][event.velocity]);
        seen[event.padIndex][event.velocity] = true;
        ++received;
    }

    for (auto& producer : producers) {
        producer.join();
    }

    for (const auto& producerSeen : seen) {
        for (const bool value : producerSeen) {
            assert(value);
        }
    }

    return 0;
}
