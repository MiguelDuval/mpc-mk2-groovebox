#include "MPC/Sequencer/MpcSequencerAudioBridge.h"

#include <array>
#include <cassert>
#include <cstdint>
#include <limits>

int main() {
    mpc::audio::AudioTriggerQueue queue;

    const std::array<mpc::sequencer::ScheduledPadEvent, 3> input{{
            {10, 0, 100, 0},
            {20, 1, 110, 1},
            {30, 2, 120, 2}}};

    const std::array<std::int32_t, 3> offsets{{0, 64, 127}};

    const auto result =
            mpc::sequencer::enqueueScheduledPadEventsAsFrames(
                    queue, input, offsets);

    assert(result.input == 3);
    assert(result.written == 3);
    assert(result.dropped == 0);
    assert(result.invalid == 0);

    for (std::size_t index = 0; index < input.size(); ++index) {
        mpc::audio::AudioTriggerEvent event;
        assert(queue.tryDequeue(event));
        assert(event.padIndex == input[index].padIndex);
        assert(event.velocity == input[index].velocity);
        assert(event.offsetFrames == offsets[index]);
    }

    const std::array<mpc::sequencer::ScheduledPadEvent, 2> invalidInput{{
            {0, 0, 100, 0},
            {0, 255, 0, 1}}};
    const std::array<std::int32_t, 2> invalidOffsets{{-1, 12}};

    const auto invalid =
            mpc::sequencer::enqueueScheduledPadEventsAsFrames(
                    queue, invalidInput, invalidOffsets);

    assert(invalid.input == 2);
    assert(invalid.written == 0);
    assert(invalid.dropped == 0);
    assert(invalid.invalid == 2);

    const std::array<std::int32_t, 1> mismatchedOffsets{{8}};
    const auto mismatched =
            mpc::sequencer::enqueueScheduledPadEventsAsFrames(
                    queue, input, mismatchedOffsets);
    assert(mismatched.input == 3);
    assert(mismatched.written == 0);
    assert(mismatched.dropped == 0);
    assert(mismatched.invalid == 3);

    mpc::audio::AudioTriggerQueue fullQueue;
    for (std::size_t index = 0;
            index < mpc::audio::kAudioTriggerQueueCapacity;
            ++index) {
        assert(fullQueue.tryEnqueue(
                mpc::audio::AudioTriggerEvent{
                    0,
                    100,
                    static_cast<std::int32_t>(
                            std::min<std::size_t>(
                                    index,
                                    static_cast<std::size_t>(
                                            std::numeric_limits<std::int32_t>::max())))}));
    }

    const auto dropped =
            mpc::sequencer::enqueueScheduledPadEventsAsFrames(
                    fullQueue,
                    input,
                    offsets);
    assert(dropped.input == 3);
    assert(dropped.written == 0);
    assert(dropped.dropped == 3);
    assert(dropped.invalid == 0);

    return 0;
}
