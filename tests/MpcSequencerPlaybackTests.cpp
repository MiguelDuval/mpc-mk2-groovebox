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
    assert(second.wrapped);
    assert(second.scheduled == 1);
    assert(second.routed == 1);
    assert(second.queued == 1);
    assert(playback.positionTicks() == 0);

    assert(queue.tryDequeue(events[0]));
    assert(events[0].padIndex == 2);
    assert(events[0].velocity == 90);
    assert(events[0].offsetFrames == 0);
    assert(!queue.tryDequeue(events[1]));

    const auto third = playback.advance(960, 99u, 48000);
    assert(third.scheduled == 1);
    assert(third.routed == 1);
    assert(third.queued == 1);
    assert(playback.positionTicks() == 960);

    assert(queue.tryDequeue(events[0]));
    assert(events[0].padIndex == 0);
    assert(events[0].velocity == 100);
    assert(events[0].offsetFrames == 0);
    assert(!queue.tryDequeue(events[1]));

    playback.stop();
    const auto stoppedAgain = playback.advance(960, 99u, 48000);
    assert(stoppedAgain.scheduled == 0);
    assert(stoppedAgain.queued == 0);
    assert(playback.positionTicks() == 960);

    mpc::domain::Pattern ratchetPattern;
    ratchetPattern.lengthTicks = 3840;
    ratchetPattern.notes = {
            {0, 480, 36, 100, 127, 4}};

    mpc::audio::AudioTriggerQueue ratchetQueue;
    mpc::sequencer::MpcSequencerPlayback ratchetPlayback(
            sequence,
            ratchetPattern,
            program,
            ratchetQueue);

    ratchetPlayback.start();
    const auto ratchetResult =
            ratchetPlayback.advance(960, 99u, 48000);

    assert(ratchetResult.scheduled == 1);
    assert(ratchetResult.routed == 4);
    assert(ratchetResult.queued == 4);

    std::array<mpc::audio::AudioTriggerEvent, 4> ratchetEvents{};
    for (std::size_t index = 0; index < ratchetEvents.size(); ++index) {
        assert(ratchetQueue.tryDequeue(ratchetEvents[index]));
        assert(ratchetEvents[index].padIndex == 0);
        assert(ratchetEvents[index].velocity == 100);
    }
    assert(ratchetEvents[0].offsetFrames == 0);
    assert(ratchetEvents[1].offsetFrames == 3000);
    assert(ratchetEvents[2].offsetFrames == 6000);
    assert(ratchetEvents[3].offsetFrames == 9000);
    assert(!ratchetQueue.tryDequeue(ratchetEvents[0]));

    // A zero duration is a valid step-note default-duration sentinel. Ratchet
    // playback must fall back to the Sequence quantize grid instead of dropping
    // the note as an invalid zero-span expansion.
    auto defaultDurationSequence = sequence;
    defaultDurationSequence.quantizeGridTicks = 240;
    mpc::domain::Pattern defaultDurationPattern;
    defaultDurationPattern.lengthTicks = 3840;
    defaultDurationPattern.notes = {
            {0, 0, 36, 100, 127, 4}};
    mpc::audio::AudioTriggerQueue defaultDurationQueue;
    mpc::sequencer::MpcSequencerPlayback defaultDurationPlayback(
            defaultDurationSequence,
            defaultDurationPattern,
            program,
            defaultDurationQueue);
    defaultDurationPlayback.start();
    const auto defaultDurationResult =
            defaultDurationPlayback.advance(960, 99u, 48000);
    assert(defaultDurationResult.scheduled == 1);
    assert(defaultDurationResult.routed == 4);
    assert(defaultDurationResult.queued == 4);
    assert(defaultDurationResult.invalid == 0);
    std::array<mpc::audio::AudioTriggerEvent, 4> defaultDurationEvents{};
    for (auto& event : defaultDurationEvents) assert(defaultDurationQueue.tryDequeue(event));
    assert(defaultDurationEvents[0].offsetFrames == 0);
    assert(defaultDurationEvents[1].offsetFrames == 1500);
    assert(defaultDurationEvents[2].offsetFrames == 3000);
    assert(defaultDurationEvents[3].offsetFrames == 4500);
    assert(!defaultDurationQueue.tryDequeue(defaultDurationEvents[0]));

    // Track Solo must gate the actual audio-trigger queue, not just a UI label.
    mpc::domain::Pattern soloPattern;
    soloPattern.lengthTicks = 3840;
    soloPattern.notes = {
            {0, 240, 36, 100, 127, 1},
            {960, 240, 37, 110, 127, 1},
            {1920, 240, 38, 90, 127, 1},
            {2880, 240, 36, 105, 127, 1}};
    auto soloSequence = sequence;
    soloSequence.loopEnabled = true;
    soloSequence.loopStartTicks = 0;
    soloSequence.loopEndTicks = 3840;

    bool mutedTrack = false;
    bool selectedTrackSoloed = false;
    bool anyTrackSoloed = false;
    mpc::audio::AudioTriggerQueue soloQueue;
    mpc::sequencer::MpcSequencerPlayback soloPlayback(
            soloSequence, soloPattern, program, soloQueue,
            &mutedTrack, &selectedTrackSoloed, &anyTrackSoloed);
    soloPlayback.start();

    const auto noSolo = soloPlayback.advance(960, 99u, 48000);
    assert(noSolo.scheduled == 1);
    assert(noSolo.routed == 1);
    assert(noSolo.queued == 1);
    mpc::audio::AudioTriggerEvent soloEvent{};
    assert(soloQueue.tryDequeue(soloEvent));
    assert(soloEvent.padIndex == 0);
    assert(!soloQueue.tryDequeue(soloEvent));

    // A different Track is Soloed: this non-soloed Track must advance in time
    // but must not enqueue audio events.
    anyTrackSoloed = true;
    const auto excluded = soloPlayback.advance(960, 99u, 48000);
    assert(excluded.scheduled == 1);
    assert(excluded.routed == 0);
    assert(excluded.queued == 0);
    assert(!soloQueue.tryDequeue(soloEvent));

    // Soloing this Track live immediately allows its events again.
    selectedTrackSoloed = true;
    const auto included = soloPlayback.advance(960, 99u, 48000);
    assert(included.scheduled == 1);
    assert(included.routed == 1);
    assert(included.queued == 1);
    assert(soloQueue.tryDequeue(soloEvent));
    assert(soloEvent.padIndex == 2);
    assert(!soloQueue.tryDequeue(soloEvent));

    // Mute has precedence even when the Track is Soloed.
    mutedTrack = true;
    const auto mutedSolo = soloPlayback.advance(960, 99u, 48000);
    assert(mutedSolo.scheduled == 1);
    assert(mutedSolo.routed == 0);
    assert(mutedSolo.queued == 0);
    assert(!soloQueue.tryDequeue(soloEvent));

    // Turning this Solo off while another Solo remains suppresses this Track.
    mutedTrack = false;
    selectedTrackSoloed = false;
    const auto liveSoloOff = soloPlayback.advance(960, 99u, 48000);
    assert(liveSoloOff.scheduled == 1);
    assert(liveSoloOff.routed == 0);
    assert(liveSoloOff.queued == 0);
    assert(!soloQueue.tryDequeue(soloEvent));

    // Removing the last Solo restores ordinary playback without rebuilding
    // the sequencer; positions continue and the next scheduled note is audible.
    anyTrackSoloed = false;
    const auto ordinaryPlaybackRestored =
            soloPlayback.advance(960, 99u, 48000);
    assert(ordinaryPlaybackRestored.scheduled == 1);
    assert(ordinaryPlaybackRestored.routed == 1);
    assert(ordinaryPlaybackRestored.queued == 1);
    assert(soloQueue.tryDequeue(soloEvent));
    assert(soloEvent.padIndex == 1);
    assert(!soloQueue.tryDequeue(soloEvent));

    // The session's eligible-playback list defines Solo scope per Sequence.
    // Switching that list must recompute the "any Solo" gate from new state.
    mpc::domain::Track sequenceOneSolo{};
    mpc::domain::Track sequenceOneOther{};
    mpc::domain::Track sequenceTwoTrack{};
    sequenceOneSolo.soloed = true;
    sequenceOneOther.soloed = false;
    sequenceTwoTrack.soloed = false;
    const std::array<const mpc::domain::Track*, 2> sequenceOnePlaybacks{
            &sequenceOneSolo, &sequenceOneOther};
    const std::array<const mpc::domain::Track*, 1> sequenceTwoPlaybacks{
            &sequenceTwoTrack};
    assert(mpc::sequencer::hasSoloedPlaybackTrack(sequenceOnePlaybacks));
    assert(!mpc::sequencer::hasSoloedPlaybackTrack(sequenceTwoPlaybacks));
    assert(mpc::sequencer::shouldRouteTrackForSolo(false, false, false));
    assert(!mpc::sequencer::shouldRouteTrackForSolo(false, false, true));
    assert(mpc::sequencer::shouldRouteTrackForSolo(false, true, true));
    assert(!mpc::sequencer::shouldRouteTrackForSolo(true, true, true));

    // Multiple simultaneous Solos remain audible; only non-soloed Tracks are
    // excluded. Each playback consumes the same live Sequence-wide Solo flag.
    bool firstTrackMuted = false;
    bool secondTrackMuted = false;
    bool firstTrackSoloed = true;
    bool secondTrackSoloed = true;
    bool sharedSoloMode = true;
    mpc::audio::AudioTriggerQueue firstSoloQueue;
    mpc::audio::AudioTriggerQueue secondSoloQueue;
    mpc::audio::AudioTriggerQueue nonSoloQueue;
    mpc::sequencer::MpcSequencerPlayback firstSoloPlayback(
            soloSequence, soloPattern, program, firstSoloQueue,
            &firstTrackMuted, &firstTrackSoloed, &sharedSoloMode);
    mpc::sequencer::MpcSequencerPlayback secondSoloPlayback(
            soloSequence, soloPattern, program, secondSoloQueue,
            &secondTrackMuted, &secondTrackSoloed, &sharedSoloMode);
    mpc::sequencer::MpcSequencerPlayback nonSoloPlayback(
            soloSequence, soloPattern, program, nonSoloQueue,
            &mutedTrack, &selectedTrackSoloed, &sharedSoloMode);
    firstSoloPlayback.start();
    secondSoloPlayback.start();
    nonSoloPlayback.start();
    assert(firstSoloPlayback.advance(960, 99u, 48000).queued == 1);
    assert(secondSoloPlayback.advance(960, 99u, 48000).queued == 1);
    assert(nonSoloPlayback.advance(960, 99u, 48000).queued == 0);
    assert(firstSoloQueue.tryDequeue(soloEvent));
    assert(secondSoloQueue.tryDequeue(soloEvent));
    assert(!nonSoloQueue.tryDequeue(soloEvent));

    // Invalid stored BPM must use the same normalized tempo for event-frame
    // offsets as the transport clock; otherwise extreme BPM collapses later
    // events onto frame zero while the transport advances at a bounded rate.
    auto extremeTempoSequence = sequence;
    extremeTempoSequence.loopEnabled = false;
    extremeTempoSequence.tempoBpm = std::numeric_limits<double>::max();
    mpc::audio::AudioTriggerQueue extremeTempoQueue;
    mpc::sequencer::MpcSequencerPlayback extremeTempoPlayback(
            extremeTempoSequence,
            pattern,
            program,
            extremeTempoQueue);
    extremeTempoPlayback.start();
    const auto extremeTempoResult =
            extremeTempoPlayback.advance(1920, 99u, 48000);
    assert(extremeTempoResult.scheduled == 2);
    assert(extremeTempoResult.queued == 2);
    std::array<mpc::audio::AudioTriggerEvent, 2> extremeTempoEvents{};
    assert(extremeTempoQueue.tryDequeue(extremeTempoEvents[0]));
    assert(extremeTempoQueue.tryDequeue(extremeTempoEvents[1]));
    assert(extremeTempoEvents[0].offsetFrames == 0);
    assert(extremeTempoEvents[1].offsetFrames == 9600);
    assert(!extremeTempoQueue.tryDequeue(extremeTempoEvents[0]));

    return 0;
}
