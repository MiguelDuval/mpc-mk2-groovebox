#pragma once

namespace mpc::sequencer::note_repeat_wake_policy {

// The worker should sleep while Note Repeat is disabled or no Pad is held.
// A changed rate is only metadata in that state; waking because of it while
// leaving the generation unconsumed makes a condition-variable predicate
// immediately true again and spins the worker thread.
[[nodiscard]] inline bool shouldWakeIdleWorker(
        bool stopping,
        bool enabled,
        bool padHeld) noexcept {
    return stopping || (enabled && padHeld);
}

} // namespace mpc::sequencer::note_repeat_wake_policy
