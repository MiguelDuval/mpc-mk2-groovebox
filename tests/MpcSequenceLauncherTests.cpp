#include "MPC/Sequencer/MpcSequenceLauncher.h"

#include <cassert>
#include <cstddef>
#include <limits>

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

    // A huge bank index would wrap bank * 16 to zero on unsigned arithmetic,
    // falsely selecting or queueing sequence 0 unless checked beforehand.
    const auto wrappingBank =
            std::numeric_limits<std::size_t>::max() / 16u + 1u;
    action = mpc::sequencer::handleSequencePadPress(
            state, transport, wrappingBank, 0, 700);
    assert(action == mpc::sequencer::SequenceLaunchAction::Unavailable);
    assert(transport.queuedSequenceIndex() == -1);

    return 0;
}
