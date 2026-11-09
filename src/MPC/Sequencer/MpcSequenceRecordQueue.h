#pragma once

#include "Audio/AudioTriggerQueue.h"

#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

inline constexpr std::uint8_t kNoMidiNoteOverride = 0xFF;

struct SequenceRecordEvent final {
    std::int64_t tick = 0;
    std::uint8_t trackIndex = 0;
    std::uint8_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::uint8_t pressed = 0;
    // Optional performed MIDI pitch; 0xFF keeps Drum Program Pad mapping.
    std::uint8_t midiNoteOverride = kNoMidiNoteOverride;
};

inline constexpr std::size_t kSequenceRecordQueueCapacity = 1024;

using SequenceRecordQueue =
        audio::BoundedMpmcQueue<
                SequenceRecordEvent,
                kSequenceRecordQueueCapacity>;

} // namespace mpc::sequencer
