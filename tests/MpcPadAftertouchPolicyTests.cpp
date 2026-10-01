#include "MPC/MpcPadAftertouchPolicy.h"

#include <cassert>
#include <cmath>

int main() {
    using namespace mpc::studio::aftertouch;

    assert(std::abs(filterMultiplierForPressure(0) - 1.0f) < 0.000001f);
    assert(std::abs(filterMultiplierForPressure(127) - 0.125f) < 0.000001f);
    assert(std::abs(filterMultiplierForPressure(255) - 0.125f) < 0.000001f);
    assert(std::abs(filterCutoffForPressure(8000.0f, 0) - 8000.0f) < 0.01f);
    assert(std::abs(filterCutoffForPressure(8000.0f, 127) - 1000.0f) < 0.01f);
    assert(std::abs(filterCutoffForPressure(8000.0f, 255) - 1000.0f) < 0.01f);
    assert(filterCutoffForPressure(0.0f, 127) == 0.0f);
    assert(filterCutoffForPressure(NAN, 127) == 0.0f);
    return 0;
}
