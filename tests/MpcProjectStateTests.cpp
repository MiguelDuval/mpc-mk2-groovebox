#include "MPC/MpcProjectState.h"

#include <cassert>

int main() {
    mpc::MpcProjectState state;

    assert(state.project().drumPrograms.size() == 1);
    assert(state.activeDrumProgram().pads.size() == mpc::domain::kMaxProgramPads);
    assert(state.activeDrumProgram().pad(0).midiNote == 36);

    const auto sampleId = state.registerSample(
            "Kick",
            "",
            48000.0,
            24000);

    assert(sampleId.isAssigned());
    assert(state.project().samples.size() == 1);
    assert(state.findSample(sampleId) != nullptr);
    assert(state.findSample(sampleId)->name == "Kick");
    assert(state.findSample(sampleId)->sampleRate == 48000.0);
    assert(state.findSample(sampleId)->lengthSamples == 24000);

    state.activeDrumProgram().pad(0).layer(0).sample = sampleId;
    assert(state.activeDrumProgram().pad(0).layer(0).isAssigned());

    return 0;
}
