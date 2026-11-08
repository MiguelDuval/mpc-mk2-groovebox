#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstdint>
#include <vector>

namespace mpc::sequencer::list_edit {

enum class PadNoteEventInsertResult : std::uint8_t {
    Added,
    InvalidPattern,
    InvalidTick,
    InvalidVelocity,
    InvalidDuration,
    SamePitchCollision,
    InvalidPad,
    InvalidMidiNote
};

[[nodiscard]] inline PadNoteEventInsertResult appendPadNoteEvent(
        std::vector<domain::MidiNoteEvent>& notes,
        std::int32_t patternLengthTicks,
        std::uint8_t mappedMidiNote,
        std::int32_t tick,
        std::int32_t velocity,
        std::int32_t durationTicks) {
    if (patternLengthTicks <= 0) return PadNoteEventInsertResult::InvalidPattern;
    if (tick < 0 || tick >= patternLengthTicks) return PadNoteEventInsertResult::InvalidTick;
    if (velocity < 1 || velocity > 127) return PadNoteEventInsertResult::InvalidVelocity;
    if (durationTicks < 0 || durationTicks > patternLengthTicks) {
        return PadNoteEventInsertResult::InvalidDuration;
    }
    if (mappedMidiNote > 127u) return PadNoteEventInsertResult::InvalidMidiNote;
    const auto duplicate = std::any_of(
            notes.begin(), notes.end(),
            [tick, mappedMidiNote](const domain::MidiNoteEvent& event) {
                return event.tick == tick && event.note == mappedMidiNote;
            });
    if (duplicate) return PadNoteEventInsertResult::SamePitchCollision;

    domain::MidiNoteEvent event;
    event.tick = tick;
    event.durationTicks = durationTicks;
    event.note = mappedMidiNote;
    event.velocity = static_cast<std::uint8_t>(velocity);
    event.probability = 127;
    event.ratchet = 1;
    event.nudgeTicks = 0;
    event.tuningOffsetMilliSemitones = 0;
    notes.push_back(event);
    std::stable_sort(
            notes.begin(), notes.end(),
            [](const domain::MidiNoteEvent& lhs,
               const domain::MidiNoteEvent& rhs) {
                return lhs.tick < rhs.tick;
            });
    return PadNoteEventInsertResult::Added;
}

[[nodiscard]] inline PadNoteEventInsertResult appendPadNoteEvent(
        std::vector<domain::MidiNoteEvent>& notes,
        std::int32_t patternLengthTicks,
        const domain::DrumProgram& program,
        std::size_t padIndex,
        std::int32_t tick,
        std::int32_t velocity,
        std::int32_t durationTicks) {
    if (program.type != domain::ProgramType::Drum
            || padIndex >= domain::kMaxProgramPads) {
        return PadNoteEventInsertResult::InvalidPad;
    }
    return appendPadNoteEvent(
            notes,
            patternLengthTicks,
            program.pad(padIndex).midiNote,
            tick,
            velocity,
            durationTicks);
}

} // namespace mpc::sequencer::list_edit
