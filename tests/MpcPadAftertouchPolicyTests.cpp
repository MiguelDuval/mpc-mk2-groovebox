#include "MPC/MpcPadAftertouchPolicy.h"

#include <cassert>
#include <cmath>

int main() {
    using namespace mpc::studio::aftertouch;

    assert(std::abs(filterCutoffForPressure(8000.0f, 0) - 8000.0f) < 0.01f);
    assert(std::abs(filterCutoffForPressure(8000.0f, 127) - 1000.0f) < 0.01f);
    assert(std::abs(filterCutoffForPressure(8000.0f, 255) - 1000.0f) < 0.01f);
    assert(filterCutoffForPressure(0.0f, 127) == 0.0f);
    assert(filterCutoffForPressure(NAN, 127) == 0.0f);
    return 0;
}
