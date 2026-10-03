#include "Audio/SampleLayerVelocityRange.h"

#include <cassert>

int main() {
    using mpc::audio::SampleLayerVelocityRange;

    constexpr SampleLayerVelocityRange full{};
    static_assert(full.isValid());
    static_assert(full.contains(0));
    static_assert(full.contains(64));
    static_assert(full.contains(127));

    constexpr SampleLayerVelocityRange low{0, 63};
    static_assert(low.contains(0));
    static_assert(low.contains(63));
    static_assert(!low.contains(64));

    constexpr SampleLayerVelocityRange high{64, 127};
    static_assert(!high.contains(63));
    static_assert(high.contains(64));
    static_assert(high.contains(127));

    constexpr SampleLayerVelocityRange invalid{100, 99};
    assert(!invalid.isValid());
    assert(!invalid.contains(110));
    return 0;
}
