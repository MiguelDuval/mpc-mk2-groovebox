#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/Sequencer/MpcPatternScheduler.h"
#include "MPC/Sequencer/MpcSequencerCursor.h"

#include <cstdint>
#include <span>

namespace mpc::sequencer {

class MpcSequencerRuntime final {
public:
    MpcSequencerRuntime(
            const domain::Sequence& sequence,
            const domain::Pattern& pattern) noexcept
            : cursor_(sequence),
              pattern_(pattern) {
    }

    MpcSequencerRuntime(const MpcSequencerRuntime&) = delete;
    MpcSequencerRuntime& operator=(const MpcSequencerRuntime&) = delete;

    void start() noexcept {
        cursor_.start();
    }

    void stop() noexcept {
        cursor_.stop();
    }

    void reset() noexcept {
        cursor_.reset();
    }

    bool isPlaying() const noexcept {
        return cursor_.isPlaying();
    }

    std::int64_t positionTicks() const noexcept {
        return cursor_.positionTicks();
    }

    void setPositionTicks(std::int64_t ticks) noexcept {
        cursor_.setPositionTicks(ticks);
    }

    TickWindow advancePosition(
            std::int64_t deltaTicks) noexcept {
        return cursor_.advanceTicks(deltaTicks);
    }

    ScheduleResult advance(
            std::int64_t deltaTicks,
            std::uint32_t seed,
            std::span<ScheduledMidiEvent> output) noexcept {
        const auto window = cursor_.advanceTicks(deltaTicks);
        if (!cursor_.isPlaying() && window.begin == window.end) {
            return {};
        }

        return schedulePatternWindowInRange(
                pattern_,
                window,
                cursor_.loopStartTicks(),
                cursor_.loopEndTicks(),
                seed,
                output);
    }

private:
    MpcSequencerCursor cursor_;
    const domain::Pattern& pattern_;
};

} // namespace mpc::sequencer
