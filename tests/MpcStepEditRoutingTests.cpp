#include "MPC/Sequencer/MpcStepEditRouting.h"

#include <cassert>

int main() {
    assert(mpc::sequencer::step_edit::stepIndexForPad(0, 0) == 0);
    assert(mpc::sequencer::step_edit::stepIndexForPad(0, 15) == 15);
    assert(mpc::sequencer::step_edit::stepIndexForPad(1, 0) == 16);
    assert(mpc::sequencer::step_edit::stepIndexForPad(3, 15) == 63);
    assert(mpc::sequencer::step_edit::stepIndexForPad(3, 99) == 63);
    return 0;
}
