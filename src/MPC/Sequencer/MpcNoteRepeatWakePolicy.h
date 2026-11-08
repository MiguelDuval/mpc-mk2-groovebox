#pragma once

namespace mpc::sequencer::note_repeat_wake_policy {

// Sleep while Note Repeat is disabled or no Pad is held. Grid metadata changes
// are not worker work in that state; waking on generation alone leaves the
// wait predicate permanently true and spins the idle worker.
[[nodiscard]] inline bool shouldWakeIdleWorker(
        bool stopping,
        bool enabled,
        bool padHeld) noexcept {
    return stopping || (enabled && padHeld);
}

} // namespace mpc::sequencer::note_repeat_wake_policy
