#include "MPC/Sequencer/MpcStepEditParameterPolicy.h"

#include <cassert>

int main() {
    using mpc::sequencer::step_edit::Parameter;
    using mpc::sequencer::step_edit::nextParameter;
    using mpc::sequencer::step_edit::deltaFor;

    assert(nextParameter(Parameter::Velocity) == Parameter::Probability);
    assert(nextParameter(Parameter::Probability) == Parameter::Ratchet);
    assert(nextParameter(Parameter::Ratchet) == Parameter::Nudge);
    assert(nextParameter(Parameter::Nudge) == Parameter::Duration);
    assert(nextParameter(Parameter::Duration) == Parameter::Velocity);

    assert(deltaFor(Parameter::Velocity, 960, 1, false) == 1);
    assert(deltaFor(Parameter::Velocity, 960, -1, true) == -1);
    assert(deltaFor(Parameter::Probability, 960, 1, false) == 1);
    assert(deltaFor(Parameter::Ratchet, 960, -1, false) == -1);

    assert(deltaFor(Parameter::Nudge, 960, 1, false) == 60);
    assert(deltaFor(Parameter::Nudge, 960, 1, true) == 10);
    assert(deltaFor(Parameter::Nudge, 960, -1, true) == -10);

    assert(deltaFor(Parameter::Duration, 960, 1, false) == 240);
    assert(deltaFor(Parameter::Duration, 960, 1, true) == 60);
    assert(deltaFor(Parameter::Duration, 96, -1, false) == -24);

    return 0;
}
