#pragma once

#include "Audio/AudioTriggerQueue.h"

#include <cstddef>
#include <cstdint>

namespace mpc::sequencer {

inline constexpr std::uint8_t kNoMidiNoteOverride = 0xFF;
inline constexpr std::uint8_t kNoSequenceRecordKey = 0xFF;
inline constexpr std::size_t kSequenceRecordKeyCount = 64;

struct SequenceRecordEvent final {
    std::int64_t tick = 0;
    std::uint8_t trackIndex = 0;
    std::uint8_t padIndex = 0;
    std::uint8_t velocity = 0;
    std::uint8_t pressed = 0;
    // Optional performed MIDI pitch; 0xFF keeps Drum Program Pad mapping.
    std::uint8_t midiNoteOverride = kNoMidiNoteOverride;
    // Separate held-note slot for polyphonic performance maps (16 pads × 4 voices).
    std::uint8_t recordKey = kNoSequenceRecordKey;
};

inline constexpr std::size_t kSequenceRecordQueueCapacity = 1024;

using SequenceRecordQueue =
        audio::BoundedMpmcQueue<
                SequenceRecordEvent,
                kSequenceRecordQueueCapacity>;

} // namespace mpc::sequencer
