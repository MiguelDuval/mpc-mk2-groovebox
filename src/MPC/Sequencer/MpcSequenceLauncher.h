#pragma once

#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequenceTransport.h"

#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

enum class SequenceLaunchAction : std::uint8_t {
    Unavailable,
    Selected,
    Queued,
    QueueCleared,
};

inline SequenceLaunchAction handleSequencePadPress(
        MpcProjectState& state,
        MpcSequenceTransportClock& transportClock,
        std::size_t bank,
        std::size_t padIndex,
        std::int64_t timestampNanos) noexcept {
    if (padIndex >= 16) {
        return SequenceLaunchAction::Unavailable;
    }

    const auto sequenceIndex = bank * 16u + padIndex;
    if (sequenceIndex >= state.sequenceCount()) {
        return SequenceLaunchAction::Unavailable;
    }

    const auto transport = transportClock.snapshot();
    if (transport.playing) {
        if (sequenceIndex == state.activeSequenceIndex()) {
            transportClock.clearQueuedSequence();
            return SequenceLaunchAction::QueueCleared;
        }

        transportClock.queueSequence(sequenceIndex);
        return SequenceLaunchAction::Queued;
    }

    if (!state.selectSequence(sequenceIndex)) {
        return SequenceLaunchAction::Unavailable;
    }

    transportClock.clearQueuedSequence();
    transportClock.update(
            state.activeSequence(),
            0,
            timestampNanos,
            false);
    return SequenceLaunchAction::Selected;
}

} // namespace mpc::sequencer
