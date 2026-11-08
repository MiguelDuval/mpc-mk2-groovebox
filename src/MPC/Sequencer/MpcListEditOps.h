#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstddef>
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



[[nodiscard]] inline bool setPatternEventNote(
        domain::Pattern& pattern, std::size_t eventIndex, std::int32_t noteNumber) {
    if (eventIndex >= pattern.notes.size() || noteNumber < 0 || noteNumber > 127) return false;
    const auto tick = pattern.notes[eventIndex].tick;
    for (std::size_t i = 0; i < pattern.notes.size(); ++i) {
        if (i != eventIndex && pattern.notes[i].tick == tick
                && pattern.notes[i].note == static_cast<std::uint8_t>(noteNumber)) return false;
    }
    pattern.notes[eventIndex].note = static_cast<std::uint8_t>(noteNumber);
    std::stable_sort(pattern.notes.begin(), pattern.notes.end(),
            [](const domain::MidiNoteEvent& a, const domain::MidiNoteEvent& b) {
                return a.tick < b.tick;
            });
    return true;
}

[[nodiscard]] inline bool setPatternEventTick(
        domain::Pattern& pattern, std::size_t eventIndex, std::int32_t tick) {
    if (eventIndex >= pattern.notes.size() || tick < 0 || tick >= pattern.lengthTicks) return false;
    const auto note = pattern.notes[eventIndex].note;
    for (std::size_t i = 0; i < pattern.notes.size(); ++i) {
        if (i != eventIndex && pattern.notes[i].tick == tick
                && pattern.notes[i].note == note) return false;
    }
    pattern.notes[eventIndex].tick = tick;
    std::stable_sort(pattern.notes.begin(), pattern.notes.end(),
            [](const domain::MidiNoteEvent& a, const domain::MidiNoteEvent& b) {
                return a.tick < b.tick;
            });
    return true;
}

[[nodiscard]] inline bool movePatternEvent(
        domain::Pattern& pattern, std::size_t eventIndex,
        std::int64_t deltaTicks) {
    if (eventIndex >= pattern.notes.size() || pattern.lengthTicks <= 0) return false;
    const auto target = static_cast<std::int64_t>(pattern.notes[eventIndex].tick) + deltaTicks;
    if (target < 0 || target >= pattern.lengthTicks) return false;
    const auto note = pattern.notes[eventIndex].note;
    for (std::size_t i = 0; i < pattern.notes.size(); ++i) {
        if (i != eventIndex && pattern.notes[i].tick == target
                && pattern.notes[i].note == note) return false;
    }
    pattern.notes[eventIndex].tick = static_cast<std::int32_t>(target);
    std::stable_sort(pattern.notes.begin(), pattern.notes.end(),
            [](const domain::MidiNoteEvent& a, const domain::MidiNoteEvent& b) {
                return a.tick < b.tick;
            });
    return true;
}

[[nodiscard]] inline bool duplicatePatternEvent(
        domain::Pattern& pattern, std::size_t eventIndex,
        std::int64_t deltaTicks) {
    if (eventIndex >= pattern.notes.size() || pattern.lengthTicks <= 0 || deltaTicks == 0) return false;
    const auto source = pattern.notes[eventIndex];
    const auto target = static_cast<std::int64_t>(source.tick) + deltaTicks;
    if (target < 0 || target >= pattern.lengthTicks) return false;
    if (std::any_of(pattern.notes.begin(), pattern.notes.end(),
            [target, &source](const domain::MidiNoteEvent& value) {
                return value.tick == target && value.note == source.note;
            })) return false;
    auto duplicate = source;
    duplicate.tick = static_cast<std::int32_t>(target);
    pattern.notes.push_back(duplicate);
    std::stable_sort(pattern.notes.begin(), pattern.notes.end(),
            [](const domain::MidiNoteEvent& a, const domain::MidiNoteEvent& b) {
                return a.tick < b.tick;
            });
    return true;
}

[[nodiscard]] inline bool transposePattern(
        domain::Pattern& pattern, std::int32_t semitones) {
    if (semitones == 0 || semitones < -127 || semitones > 127 || pattern.notes.empty()) return false;
    for (const auto& event : pattern.notes) {
        const auto next = static_cast<std::int32_t>(event.note) + semitones;
        if (next < 0 || next > 127) return false;
    }
    for (auto& event : pattern.notes) {
        event.note = static_cast<std::uint8_t>(static_cast<std::int32_t>(event.note) + semitones);
    }
    return true;
}

} // namespace mpc::sequencer::list_edit
