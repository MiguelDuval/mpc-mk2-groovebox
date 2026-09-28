#include "MPC/Sequencer/MpcSequencerPlayback.h"

#include <array>
#include <cassert>
#include <cstdint>

int main() {
    mpc::domain::Sequence sequence;
    sequence.lengthTicks = 3840;
    sequence.tempoBpm = 120.0;

    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes = {
            {0, 120, 36, 100, 127, 1},
            {960, 120, 37, 110, 127, 1},
            {1920, 120, 38, 90, 127, 1}};

    mpc::domain::DrumProgram program;
    program.pads[0].midiNote = 36;
    program.pads[1].midiNote = 37;
    program.pads[2].midiNote = 38;

    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencerPlayback playback(
            sequence,
            pattern,
            program,
            queue);

    const auto stopped = playback.advance(3840, 99u, 48000);
    assert(stopped.scheduled == 0);
    assert(stopped.queued == 0);
    assert(playback.positionTicks() == 0);

    playback.start();

    const auto first = playback.advance(1920, 99u, 48000);
    assert(first.scheduled == 2);
    assert(first.routed == 2);
    assert(first.unmapped == 0);
    assert(first.queued == 2);
    assert(first.dropped == 0);
    assert(first.invalid == 0);
    assert(!first.schedulingTruncated);
    assert(playback.positionTicks() == 1920);

    std::array<mpc::audio::AudioTriggerEvent, 2> events{};
    assert(queue.tryDequeue(events[0]));
    assert(queue.tryDequeue(events[1]));

    assert(events[0].padIndex == 0);
    assert(events[0].velocity == 100);
    assert(events[0].offsetFrames == 0);

    assert(events[1].padIndex == 1);
    assert(events[1].velocity == 110);
    assert(events[1].offsetFrames == 24000);

    const auto second = playback.advance(1920, 99u, 48000);
    assert(second.scheduled == 2);
    assert(second.routed == 2);
    assert(second.queued == 2);
    assert(playback.positionTicks() == 0);

    assert(queue.tryDequeue(events[0]));
    assert(queue.tryDequeue(events[1]));

    assert(events[0].padIndex == 2);
    assert(events[0].velocity == 90);
    assert(events[0].offsetFrames == 0);

    assert(events[1].padIndex == 0);
    assert(events[1].velocity == 100);
    assert(events[1].offsetFrames == 0);

    playback.stop();
    const auto stoppedAgain = playback.advance(960, 99u, 48000);
    assert(stoppedAgain.scheduled == 0);
    assert(stoppedAgain.queued == 0);
    assert(playback.positionTicks() == 0);

    return 0;
}
