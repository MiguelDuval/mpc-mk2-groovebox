#include "MPC/Domain/DrumProgram.h"

#include <cassert>

int main() {
    mpc::domain::DrumProgram program;

    assert(program.pads.size() == mpc::domain::padCount);
    assert(program.pad(0).layers.size() == mpc::domain::sampleLayersPerPad);
    assert(!program.pad(0).layer(0).isAssigned());
    assert(program.pad(0).layer(0).velocityRange().contains(0));
    assert(program.pad(0).layer(0).velocityRange().contains(127));
    assert(program.pad(0).level == 1.0f);

    program.pad(0).layer(0).sample = {1};
    program.pad(0).layer(0).velocityMinimum = 64;
    program.pad(0).layer(0).velocityMaximum = 127;
    program.pad(0).layer(0).gain = 0.75f;
    program.pad(0).layer(0).tuningSemitones = -2.0f;
    program.pad(0).layer(0).pan = 0.25f;
    program.pad(0).layer(0).region = {100, 900};
    program.pad(0).tuningSemitones = 3.0f;
    program.pad(0).level = 0.5f;
    program.pad(0).pan = -0.25f;

    assert(program.pad(0).layer(0).isAssigned());
    assert(!program.pad(0).layer(0).velocityRange().contains(63));
    assert(program.pad(0).layer(0).velocityRange().contains(64));
    assert(program.pad(0).layer(0).gain == 0.75f);
    assert(program.pad(0).layer(0).tuningSemitones == -2.0f);
    assert(program.pad(0).layer(0).pan == 0.25f);
    assert(program.pad(0).layer(0).region.isValidFor(1000));
    assert(program.pad(0).layer(0).region.frameCount() == 800);
    assert(program.pad(0).tuningSemitones == 3.0f);
    assert(program.pad(0).level == 0.5f);
    assert(program.pad(0).pan == -0.25f);

    program.pad(0).layer(0).sample = {};
    assert(!program.pad(0).layer(0).isAssigned());
    assert(!program.pad(1).layer(0).isAssigned());

    return 0;
}
