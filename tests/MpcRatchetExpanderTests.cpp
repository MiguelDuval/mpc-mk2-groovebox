#include "MPC/Sequencer/MpcPatternScheduler.h"

#include <array>
#include <cassert>
#include <cstdint>

int main() {
    const mpc::sequencer::ScheduledMidiEvent input{
            100, 200, 480, 36, 100, 4, 7};

    std::array<mpc::sequencer::ScheduledMidiEvent, 8> output{};
    const auto result = mpc::sequencer::expandScheduledRatchets(
            std::span<const mpc::sequencer::ScheduledMidiEvent>(&input, 1),
            480,
            output);

    assert(result.written == 4);
    assert(!result.truncated);
    assert(!result.invalid);

    assert(output[0].offsetTicks == 100);
    assert(output[1].offsetTicks == 220);
    assert(output[2].offsetTicks == 340);
    assert(output[3].offsetTicks == 460);

    for (std::size_t index = 0; index < result.written; ++index) {
        assert(output[index].patternTick == input.patternTick);
        assert(output[index].durationTicks == input.durationTicks);
        assert(output[index].note == input.note);
        assert(output[index].velocity == input.velocity);
        assert(output[index].ratchetCount == 1);
        assert(output[index].sourceNoteIndex == input.sourceNoteIndex);
    }

    const auto singleResult = mpc::sequencer::expandScheduledRatchets(
            std::span<const mpc::sequencer::ScheduledMidiEvent>(&input, 1),
            480,
            std::span<mpc::sequencer::ScheduledMidiEvent>(output.data(), 1));

    assert(singleResult.written == 1);
    assert(output[0].offsetTicks == 100);

    std::array<mpc::sequencer::ScheduledMidiEvent, 3> tinyOutput{};
    const auto truncated = mpc::sequencer::expandScheduledRatchets(
            std::span<const mpc::sequencer::ScheduledMidiEvent>(&input, 1),
            480,
            tinyOutput);

    assert(truncated.written == 3);
    assert(truncated.truncated);

    const mpc::sequencer::ScheduledMidiEvent clampedInput{
            0, 120, 80, 36, 90, 12, 2};
    const auto clamped = mpc::sequencer::expandScheduledRatchets(
            std::span<const mpc::sequencer::ScheduledMidiEvent>(
                    &clampedInput,
                    1),
            80,
            output);

    assert(clamped.written == 8);
    assert(output[0].offsetTicks == 0);
    assert(output[1].offsetTicks == 10);
    assert(output[7].offsetTicks == 70);

    const auto invalid = mpc::sequencer::expandScheduledRatchets(
            std::span<const mpc::sequencer::ScheduledMidiEvent>(&input, 1),
            0,
            output);

    assert(invalid.written == 0);
    assert(invalid.invalid);

    return 0;
}
