#include "MPC/Sequencer/MpcLocatePolicy.h"

#include <cassert>

int main() {
    using namespace mpc::sequencer::locate;

    assert(kLocatorCount == 6u);
    for (std::int32_t slot = 0; slot < 6; ++slot) {
        assert(locatorSlotForJumpPad(slot) == slot);
        assert(locatorSlotForStorePad(8 + slot) == slot);
        assert(storePadForLocatorSlot(slot) == 8 + slot);
    }

    assert(locatorSlotForJumpPad(6) == -1);
    assert(locatorSlotForStorePad(14) == -1);
    assert(storePadForLocatorSlot(6) == -1);

    assert(clampTick(-1, 3840) == 0);
    assert(clampTick(3840, 3840) == 3839);
    assert(clampTick(120, 3840) == 120);

    assert(!isMomentaryHold(1'000'000'000, 1'300'000'000));
    assert(isMomentaryHold(1'000'000'000, 1'350'000'000));
    assert(isMomentaryHold(1'000'000'000, 2'000'000'000));
    assert(!isMomentaryHold(0, 2'000'000'000));

    return 0;
}
