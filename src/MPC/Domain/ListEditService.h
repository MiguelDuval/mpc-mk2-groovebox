#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcEditHistory.h"

#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

namespace mpc::domain {

// Domain-level List Edit operations for MIDI note events within a Pattern.
// All operations record undo/redo commands in the supplied MpcEditHistory.
//
// Design notes:
// - Pattern must outlive any recorded command (commands capture Pattern*).
// - After changing a note's tick, the pattern's note vector is stable-sorted
//   by tick. Undo/redo must therefore restore the event by stable identity
//   (tick + note + velocity + duration), not by vector index, because sorting
//   can change the position of an event.
//
// This is the production code path used by both JNI and unit tests.
class ListEditService final {
public:
    struct AddResult {
        bool success = false;
        std::size_t eventIndex = 0;
        std::string error;
    };

    struct ModifyResult {
        bool success = false;
        std::string error;
    };

    // Adds a new note event to the pattern, then stable-sorts by tick.
    static AddResult addEvent(
            Pattern& pattern,
            MpcEditHistory& history,
            std::int32_t tick,
            std::int32_t durationTicks,
            std::uint8_t note,
            std::uint8_t velocity);

    // Deletes the event at the given index. The deleted event is restored by
    // undo at its original stable position (re-sorted on redo).
    static ModifyResult deleteEvent(
            Pattern& pattern,
            MpcEditHistory& history,
            std::size_t eventIndex);

    // Changes the note of the event at the given index.
    static ModifyResult setNote(
            Pattern& pattern,
            MpcEditHistory& history,
            std::size_t eventIndex,
            std::uint8_t note);

    // Changes the velocity of the event at the given index.
    static ModifyResult setVelocity(
            Pattern& pattern,
            MpcEditHistory& history,
            std::size_t eventIndex,
            std::uint8_t velocity);

    // Changes the duration of the event at the given index.
    static ModifyResult setDuration(
            Pattern& pattern,
            MpcEditHistory& history,
            std::size_t eventIndex,
            std::int32_t durationTicks);

    // Changes the tick of the event at the given index, then re-sorts.
    static ModifyResult setTick(
            Pattern& pattern,
            MpcEditHistory& history,
            std::size_t eventIndex,
            std::int32_t tick);

    // Returns the number of events in the pattern.
    static std::size_t eventCount(const Pattern& pattern) noexcept;

    // Returns the event at the given index, or std::nullopt if out of range.
    static std::optional<MidiNoteEvent> getEvent(
            const Pattern& pattern,
            std::size_t eventIndex) noexcept;
};

} // namespace mpc::domain