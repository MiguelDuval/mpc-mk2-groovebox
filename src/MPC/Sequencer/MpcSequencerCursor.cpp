#include "MPC/Sequencer/MpcSequencerCursor.h"
#include "MPC/Sequencer/MpcSequenceSettings.h"

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
    if (length <= 0) {
        positionTicks_ = 0;
        return;
    }

    if (!sequence_.loopEnabled) {
        positionTicks_ = std::clamp<std::int64_t>(ticks, 0, length - 1);
        return;
    }

    const auto loopStart = std::clamp<std::int64_t>(
            sequence_.loopStartTicks, 0, length - 1);
    const auto loopEnd = std::clamp<std::int64_t>(
            sequence_.loopEndTicks, loopStart + 1, length);
    const auto loopLength = loopEnd - loopStart;

    if (loopLength <= 0) {
        positionTicks_ = loopStart;
        return;
    }

    const auto relative = (ticks - loopStart) % loopLength;
    positionTicks_ = loopStart + (relative < 0 ? relative + loopLength : relative);
}

TickWindow MpcSequencerCursor::advanceTicks(
        std::int64_t deltaTicks) noexcept {
    const TickWindow stoppedWindow{
            positionTicks_,
            positionTicks_,
            0};

    if (!playing_ || deltaTicks <= 0) {
        return stoppedWindow;
    }

    const auto length = static_cast<std::int64_t>(sequenceLengthTicks_);

    if (!sequence_.loopEnabled) {
        const auto begin = positionTicks_;
        const auto end = std::min<std::int64_t>(
                length,
                begin + deltaTicks);
        positionTicks_ = std::min<std::int64_t>(end, length - 1);

        TickWindow result{begin, positionTicks_, 0};
        if (end >= length) {
            playing_ = false;
        }
        return result;
    }

    const auto loopStart = std::clamp<std::int64_t>(
            sequence_.loopStartTicks, 0, length - 1);
    const auto loopEnd = std::clamp<std::int64_t>(
            sequence_.loopEndTicks, loopStart + 1, length);
    const auto loopLength = loopEnd - loopStart;

    positionTicks_ = std::clamp<std::int64_t>(
            positionTicks_, loopStart, loopEnd - 1);

    const auto begin = positionTicks_;
    const auto distance = static_cast<std::int64_t>(
            positionTicks_ - loopStart);
    const auto total = distance + deltaTicks;

    TickWindow result;
    result.begin = begin;
    result.completedCycles =
            static_cast<std::uint64_t>(total / loopLength);
    result.end = loopStart + (total % loopLength);
    positionTicks_ = result.end;
    return result;
}

} // namespace mpc::sequencer
