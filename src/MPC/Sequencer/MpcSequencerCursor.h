#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <cstdint>

namespace mpc::sequencer {

struct TickWindow final {
    std::int64_t begin = 0;
    std::int64_t end = 0;
    std::uint64_t completedCycles = 0;
};

class MpcSequencerCursor final {
public:
    explicit MpcSequencerCursor(const domain::Sequence& sequence) noexcept;

    void start() noexcept {
        playing_ = true;
    }

    void stop() noexcept {
        playing_ = false;
    }

    void reset() noexcept {
        positionTicks_ = sequence_.loopEnabled
                ? std::clamp<std::int64_t>(
                        sequence_.loopStartTicks, 0, sequenceLengthTicks_ - 1)
                : 0;
        playing_ = false;
    }

    bool isPlaying() const noexcept {
        return playing_;
    }

    std::int64_t positionTicks() const noexcept {
        return positionTicks_;
    }

    std::int32_t sequenceLengthTicks() const noexcept {
        return sequenceLengthTicks_;
    }

    std::int64_t loopStartTicks() const noexcept {
        if (!sequence_.loopEnabled) return 0;
        return std::clamp<std::int64_t>(
                sequence_.loopStartTicks, 0, sequenceLengthTicks_ - 1);
    }

    std::int64_t loopEndTicks() const noexcept {
        if (!sequence_.loopEnabled) return sequenceLengthTicks_;
        const auto start = loopStartTicks();
        return std::clamp<std::int64_t>(
                sequence_.loopEndTicks, start + 1, sequenceLengthTicks_);
    }

    void setPositionTicks(std::int64_t ticks) noexcept;

    TickWindow advanceTicks(std::int64_t deltaTicks) noexcept;

private:
    const domain::Sequence& sequence_;
    std::int32_t sequenceLengthTicks_ = 1;
    std::int64_t positionTicks_ = 0;
    bool playing_ = false;
};

} // namespace mpc::sequencer
