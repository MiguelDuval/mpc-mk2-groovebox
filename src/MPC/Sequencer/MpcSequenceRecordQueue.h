#pragma once

#include "Audio/AudioTriggerQueue.h"

#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

inline constexpr std::uint8_t kSequenceRecordNoteOff = 0;
inline constexpr std::uint8_t kSequenceRecordNoteOn = 1;
// One-shot drum hits are produced by clocked performance generators such as Note Repeat.
inline constexpr std::uint8_t kSequenceRecordNoteHit = 2;

struct SequenceRecordEvent final {
    std::int64_t tick = 0;
    std::uint8_t trackIndex = 0;
    std::uint8_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::uint8_t pressed = kSequenceRecordNoteOff;
};

inline constexpr std::size_t kSequenceRecordQueueCapacity = 1024;

using SequenceRecordQueue =
        audio::BoundedMpmcQueue<
                SequenceRecordEvent,
                kSequenceRecordQueueCapacity>;

} // namespace mpc::sequencer
