#include "MPC/Sequencer/MpcNoteRepeatWakePolicy.h"

#include <cassert>

int main() {
    using mpc::sequencer::note_repeat_wake_policy::shouldWakeIdleWorker;

    // A grid change while disabled or with no held Pad must leave the worker
    // asleep. The changed grid is observed on the next real activation.
    assert(!shouldWakeIdleWorker(false, false, false));
    assert(!shouldWakeIdleWorker(false, false, true));
    assert(!shouldWakeIdleWorker(false, true, false));

    // Active repeat and shutdown must wake the worker.
    assert(shouldWakeIdleWorker(false, true, true));
    assert(shouldWakeIdleWorker(true, false, false));
    assert(shouldWakeIdleWorker(true, true, true));
    return 0;
}
