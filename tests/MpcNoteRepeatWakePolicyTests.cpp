#include "MPC/Sequencer/MpcNoteRepeatWakePolicy.h"

#include <cassert>

int main() {
    using mpc::sequencer::note_repeat_wake_policy::shouldWakeIdleWorker;

    // Changing the selected grid without an enabled, held Pad must leave the
    // worker asleep. Otherwise the idle predicate stays true forever because
    // generation changes aren't consumed on this branch.
    assert(!shouldWakeIdleWorker(false, false, false));
    assert(!shouldWakeIdleWorker(false, false, true));
    assert(!shouldWakeIdleWorker(false, true, false));

    // Active playback and shutdown must wake the idle worker.
    assert(shouldWakeIdleWorker(false, true, true));
    assert(shouldWakeIdleWorker(true, false, false));
    assert(shouldWakeIdleWorker(true, true, true));

    return 0;
}
