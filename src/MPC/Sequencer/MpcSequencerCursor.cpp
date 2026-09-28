#include "MPC/Sequencer/MpcSequencerCursor.h"

#include <algorithm>

namespace mpc::sequencer {

MpcSequencerCursor::MpcSequencerCursor(
        const domain::Sequence& sequence) noexcept
        : sequence_(sequence),
          sequenceLengthTicks_(std::max<std::int32_t>(
                  1,
                  sequence.lengthTicks)) {
}

void MpcSequencerCursor::setPositionTicks(std::int64_t ticks) noexcept {
    const auto length = static_cast<std::int64_t>(sequenceLengthTicks_);
    const auto normalized = ticks % length;
    positionTicks_ = normalized < 0 ? normalized + length : normalized;
}

TickWindow MpcSequencerCursor::advanceTicks(
        std::int64_t deltaTicks) noexcept {
    const TickWindow window{
            positionTicks_,
            positionTicks_,
            0};

    if (!playing_ || deltaTicks <= 0) {
        return window;
    }

    const auto length = static_cast<std::int64_t>(sequenceLengthTicks_);
    const auto fullCycles =
            static_cast<std::uint64_t>(deltaTicks / length);
    const auto remainder = deltaTicks % length;
    const auto advanced = positionTicks_ + remainder;

    TickWindow result;
    result.begin = positionTicks_;
    result.completedCycles = fullCycles
            + static_cast<std::uint64_t>(advanced >= length);
    result.end = advanced >= length ? advanced - length : advanced;

    positionTicks_ = result.end;
    return result;
}

} // namespace mpc::sequencer
