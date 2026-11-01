#include "MPC/Sequencer/MpcSequencerPlayback.h"
#include "Audio/AudioTriggerQueue.h"

#include <cassert>

using mpc::domain::DrumProgram;
using mpc::domain::MidiNoteEvent;
using mpc::domain::Pattern;
using mpc::domain::Sequence;
using mpc::audio::AudioTriggerQueue;
using mpc::sequencer::MpcSequencerPlayback;

int main() {
    Sequence sequence;
    sequence.tempoBpm = 120.0;
    sequence.numerator = 4;
    sequence.denominator = 4;
    sequence.lengthTicks = 3840;
    sequence.loopEnabled = false;

    Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes = {
        MidiNoteEvent{0, 240, 36, 100, 127, 1, 0}
    };

    DrumProgram program;
    AudioTriggerQueue queue;

    {
        bool muted = false;
        bool soloed = false;
        bool anySolo = false;
        MpcSequencerPlayback playback(
            sequence, pattern, program, queue, &muted, &soloed, anySolo);
        playback.start();
        auto result = playback.advance(1, 1, 44100);
        assert(result.routed > 0 && "note should be routed when nothing silences it");
    }

    {
        bool muted = true;
        bool soloed = false;
        bool anySolo = false;
        MpcSequencerPlayback playback(
            sequence, pattern, program, queue, &muted, &soloed, anySolo);
        playback.start();
        auto result = playback.advance(1, 1, 44100);
        assert(result.routed == 0 && "muted track must not route events");
    }

    {
        bool muted = false;
        bool soloed = false;
        bool anySolo = true;
        MpcSequencerPlayback playback(
            sequence, pattern, program, queue, &muted, &soloed, anySolo);
        playback.start();
        auto result = playback.advance(1, 1, 44100);
        assert(result.routed == 0 && "non-soloed track must be silent when another track is soloed");
    }

    {
        bool muted = false;
        bool soloed = true;
        bool anySolo = true;
        MpcSequencerPlayback playback(
            sequence, pattern, program, queue, &muted, &soloed, anySolo);
        playback.start();
        auto result = playback.advance(1, 1, 44100);
        assert(result.routed > 0 && "soloed track must route events");
    }

    {
        bool muted = true;
        bool soloed = true;
        bool anySolo = true;
        MpcSequencerPlayback playback(
            sequence, pattern, program, queue, &muted, &soloed, anySolo);
        playback.start();
        auto result = playback.advance(1, 1, 44100);
        assert(result.routed == 0 && "mute takes precedence over solo");
    }

    return 0;
}