#include "MPC/Sequencer/MpcSequenceLauncher.h"

#include <cassert>

int main() {
    mpc::MpcProjectState state;
    assert(state.addSequence());

    mpc::sequencer::MpcSequenceTransportClock transport;

    auto action = mpc::sequencer::handleSequencePadPress(
            state, transport, 0, 0, 100);
    assert(action == mpc::sequencer::SequenceLaunchAction::Selected);
    assert(state.activeSequenceIndex() == 0);
    assert(transport.queuedSequenceIndex() == -1);

    action = mpc::sequencer::handleSequencePadPress(
            state, transport, 0, 1, 200);
    assert(action == mpc::sequencer::SequenceLaunchAction::Selected);
    assert(state.activeSequenceIndex() == 1);

    transport.start(state.activeSequence(), 0, 300);
    action = mpc::sequencer::handleSequencePadPress(
            state, transport, 0, 0, 400);
    assert(action == mpc::sequencer::SequenceLaunchAction::Queued);
    assert(state.activeSequenceIndex() == 1);
    assert(transport.queuedSequenceIndex() == 0);

    action = mpc::sequencer::handleSequencePadPress(
            state, transport, 0, 1, 500);
    assert(action == mpc::sequencer::SequenceLaunchAction::QueueCleared);
    assert(transport.queuedSequenceIndex() == -1);

    action = mpc::sequencer::handleSequencePadPress(
            state, transport, 0, 15, 600);
    assert(action == mpc::sequencer::SequenceLaunchAction::Unavailable);

    return 0;
}
