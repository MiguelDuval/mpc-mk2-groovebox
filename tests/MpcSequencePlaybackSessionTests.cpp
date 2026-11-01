#include "Audio/AudioTriggerQueue.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequencePlaybackSession.h"

#include <cassert>
#include <cstdint>
#include <utility>

namespace {

void prepareAudibleDefaultPattern(mpc::MpcProjectState& state) {
    auto& pattern = state.activeSequence().tracks.front().patterns.front();
    pattern.notes.push_back({0, 120, 36, 100, 127, 1});
}

void assertNoStalePlayback(
        mpc::sequencer::MpcSequencePlaybackSession& session,
        mpc::audio::AudioTriggerQueue& queue) {
    const auto result = session.advance(960, 73u, 48000);
    assert(result.scheduled == 0);
    assert(result.queued == 0);
    assert(!session.isPlaying());
    assert(session.positionTicks() == 0);
    mpc::audio::AudioTriggerEvent stale{};
    assert(!queue.tryDequeue(stale));
}

void testTrackVectorMutationStopsBeforeDereferencingTrackReferences() {
    mpc::MpcProjectState state;
    prepareAudibleDefaultPattern(state);
    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);

    assert(session.start());
    assert(session.isPlaying());
    assert(state.addTrack(mpc::domain::TrackKind::Audio));
    assertNoStalePlayback(session, queue);
}

void testDrumProgramVectorMutationStopsBeforeDereferencingProgramReferences() {
    mpc::MpcProjectState state;
    prepareAudibleDefaultPattern(state);
    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);

    assert(session.start());
    assert(session.isPlaying());
    auto extraProgram = state.project().drumPrograms.front();
    extraProgram.id = "drum-program-extra";
    extraProgram.name = "Extra Drum Program";
    state.project().drumPrograms.push_back(std::move(extraProgram));
    assertNoStalePlayback(session, queue);
}

void testSequenceShapeMutationInvalidatesPlaybackSnapshot() {
    mpc::MpcProjectState state;
    prepareAudibleDefaultPattern(state);
    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);

    assert(session.start());
    assert(session.isPlaying());
    assert(state.setSequenceBars(8));
    assertNoStalePlayback(session, queue);
}

void testTrackRetargetInvalidatesPlaybackSnapshot() {
    mpc::MpcProjectState state;
    prepareAudibleDefaultPattern(state);
    auto extraProgram = state.project().drumPrograms.front();
    extraProgram.id = "drum-program-extra";
    extraProgram.name = "Extra Drum Program";
    state.project().drumPrograms.push_back(std::move(extraProgram));
    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);

    assert(session.start());
    assert(session.isPlaying());
    assert(state.setTrackProgram(0, "drum-program-extra"));
    assertNoStalePlayback(session, queue);
}

void testProjectReplacementInvalidatesPlaybackGeneration() {
    mpc::MpcProjectState state;
    prepareAudibleDefaultPattern(state);
    mpc::audio::AudioTriggerQueue queue;
    mpc::sequencer::MpcSequencePlaybackSession session(state, queue);

    assert(session.start());
    assert(session.isPlaying());
    auto sameIdProject = state.project();
    assert(state.replaceProject(std::move(sameIdProject)));
    assertNoStalePlayback(session, queue);
}

} // namespace

int main() {
    testTrackVectorMutationStopsBeforeDereferencingTrackReferences();
    testDrumProgramVectorMutationStopsBeforeDereferencingProgramReferences();
    testSequenceShapeMutationInvalidatesPlaybackSnapshot();
    testTrackRetargetInvalidatesPlaybackSnapshot();
    testProjectReplacementInvalidatesPlaybackGeneration();
    return 0;
}
