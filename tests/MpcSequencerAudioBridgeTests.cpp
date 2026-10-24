#include "MPC/Sequencer/MpcSequencerAudioBridge.h"

#include <array>
#include <cassert>
#include <cstdint>

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

    const std::array<mpc::sequencer::ScheduledPadEvent, 4> invalidInput{{
            {0, 0, 100, 0},
            {0, 255, 100, 1},
            {0, 2, 128, 2},
            {0, mpc::domain::kMaxProgramPads, 100, 3}}};
    const std::array<std::int32_t, 4> invalidOffsets{{-1, 12, 12, 12}};

    const auto invalid =
            mpc::sequencer::enqueueScheduledPadEventsAsFrames(
                    queue, invalidInput, invalidOffsets);

    assert(invalid.input == 4);
    assert(invalid.written == 0);
    assert(invalid.dropped == 0);
    assert(invalid.invalid == 4);

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
                    static_cast<std::int32_t>(index)}));
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

    mpc::audio::AudioTriggerQueue timingQueue;
    const auto timingInput = std::array<mpc::sequencer::ScheduledPadEvent, 2>{{
            {960, 3, 100, 0},
            {1920, 4, 110, 1}}};

    const auto timing =
            mpc::sequencer::enqueueScheduledPadEvents(
                    timingQueue,
                    timingInput,
                    120.0,
                    48000);

    assert(timing.input == 2);
    assert(timing.written == 2);
    assert(timing.dropped == 0);
    assert(timing.invalid == 0);

    mpc::audio::AudioTriggerEvent firstTiming;
    mpc::audio::AudioTriggerEvent secondTiming;
    assert(timingQueue.tryDequeue(firstTiming));
    assert(timingQueue.tryDequeue(secondTiming));
    assert(firstTiming.offsetFrames == 24000);
    assert(secondTiming.offsetFrames == 48000);
    assert(firstTiming.padIndex == 3);
    assert(secondTiming.padIndex == 4);

    const auto invalidTiming =
            mpc::sequencer::enqueueScheduledPadEvents(
                    timingQueue,
                    timingInput,
                    0.0,
                    48000);
    assert(invalidTiming.input == 2);
    assert(invalidTiming.written == 0);
    assert(invalidTiming.dropped == 0);
    assert(invalidTiming.invalid == 2);

    const std::array<mpc::sequencer::ScheduledPadEvent, 3> malformedTimingInput{{
            {960, mpc::domain::kMaxProgramPads, 100, 0},
            {1920, 2, 128, 1},
            {2400, 3, 0, 2}}};
    const auto malformedTiming =
            mpc::sequencer::enqueueScheduledPadEvents(
                    timingQueue, malformedTimingInput, 120.0, 48000);
    assert(malformedTiming.input == 3);
    assert(malformedTiming.written == 0);
    assert(malformedTiming.dropped == 0);
    assert(malformedTiming.invalid == 3);

    return 0;
}
