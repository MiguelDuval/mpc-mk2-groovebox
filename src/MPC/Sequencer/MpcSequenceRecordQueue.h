#pragma once

#include "Audio/AudioTriggerQueue.h"

#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

struct SequenceRecordEvent final {
    std::int64_t tick = 0;
    std::uint8_t trackIndex = 0;
    std::uint8_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::uint8_t pressed = 0;
};

inline constexpr std::size_t kSequenceRecordQueueCapacity = 1024;

using SequenceRecordQueue =
        audio::BoundedMpmcQueue<
                SequenceRecordEvent,
                kSequenceRecordQueueCapacity>;

} // namespace mpc::sequencer
