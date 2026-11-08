#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencePlaybackSession.h"

#include <array>
#include <cassert>
#include <cstdint>

int main() {
    mpc::MpcProjectState state;
    auto& sequence = state.activeSequence();
    auto& track = sequence.tracks.front();
    auto& pattern = track.patterns.front();

    // Two source notes are clipped into an Arrangement instance that starts
    // later on the timeline and reads from a non-zero source offset.
    pattern.notes = {
            {120, 180, 36, 97, 127, 3, -11, 0},
            {600, 90, 42, 111, 127, 2, 7, 2500}
    };
    track.placements = {{
            "session-placement-01",
            pattern.id,
            480,
            720,
            120
    }};

    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);
    assert(session.start(0));
    assert(session.isPlaying());

    // At 120 BPM the first source note (tick 120, nudge -11) is flattened
    // to destination tick 480, then scheduled at effective tick 469.
    const auto first = session.advance(576, 0x53455131u, 48000);
    assert(first.scheduled == 1);
    assert(first.routed == 3);
    assert(first.queued == 3);
    assert(first.dropped == 0);
    assert(first.invalid == 0);
    assert(!first.wrapped);
    assert(session.positionTicks() == 576);

    std::array<mpc::audio::AudioTriggerEvent, 3> firstEvents{};
    for (auto& event : firstEvents) assert(queue.tryDequeue(event));
    assert(!queue.tryDequeue(firstEvents[0]));
    for (const auto& event : firstEvents) {
        assert(event.padIndex == 0);
        assert(event.velocity == 97);
        assert(event.tuningOffsetMilliSemitones == 0);
    }
    assert(firstEvents[0].offsetFrames == 11725);
    assert(firstEvents[1].offsetFrames == 13225);
    assert(firstEvents[2].offsetFrames == 14725);

    // The second note remains inside the same clip's source window. It must
    // arrive at timeline tick 967 (480 + 600 - 120 + nudge 7), then route to
    // the Program pad mapped to MIDI note 42.
    const auto second = session.advance(500, 0x53455131u, 48000);
    assert(second.scheduled == 1);
    assert(second.routed == 2);
    assert(second.queued == 2);
    assert(second.dropped == 0);
    assert(second.invalid == 0);
    assert(session.positionTicks() == 1076);

    std::array<mpc::audio::AudioTriggerEvent, 2> secondEvents{};
    for (auto& event : secondEvents) assert(queue.tryDequeue(event));
    assert(!queue.tryDequeue(secondEvents[0]));
    for (const auto& event : secondEvents) {
        assert(event.padIndex == 6);
        assert(event.velocity == 111);
        assert(event.tuningOffsetMilliSemitones == 2500);
    }
    assert(secondEvents[0].offsetFrames == 9775);
    assert(secondEvents[1].offsetFrames == 10900);

    session.stop();
    assert(!session.isPlaying());
    return 0;
}
