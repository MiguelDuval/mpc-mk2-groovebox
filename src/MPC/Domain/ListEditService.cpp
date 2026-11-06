#include "MPC/Domain/ListEditService.h"

#include <algorithm>
#include <cstdint>
#include <optional>
#include <string>
#include <unordered_set>
#include <utility>

namespace mpc::domain {

// Helper: find event by stable eventId
static MidiNoteEvent* findByEventId(Pattern& pattern, std::uint32_t eventId) {
    if (eventId == 0) return nullptr;
    for (auto& e : pattern.notes) {
        if (e.eventId == eventId) return &e;
    }
    return nullptr;
}

static const MidiNoteEvent* findByEventId(const Pattern& pattern, std::uint32_t eventId) {
    if (eventId == 0) return nullptr;
    for (const auto& e : pattern.notes) {
        if (e.eventId == eventId) return &e;
    }
    return nullptr;
}

// Helper: assign eventId if not already assigned (for legacy/deserialized events)
// Uses a robust allocator that ensures unique IDs within the pattern.
static void ensureEventId(Pattern& pattern) {
    // First pass: collect all existing non-zero eventIds
    std::unordered_set<std::uint32_t> usedIds;
    usedIds.reserve(pattern.notes.size() * 2);
    for (const auto& e : pattern.notes) {
        if (e.eventId != 0) {
            usedIds.insert(e.eventId);
        }
    }

    // Update nextEventId to be greater than any existing ID
    for (const auto id : usedIds) {
        if (id >= pattern.nextEventId) {
            pattern.nextEventId = id + 1;
        }
    }

    // Second pass: assign IDs to zero-ID events
    for (auto& e : pattern.notes) {
        if (e.eventId == 0) {
            // Find the next available ID
            while (usedIds.contains(pattern.nextEventId)) {
                ++pattern.nextEventId;
                // Handle overflow: if we wrap around, search from 1
                if (pattern.nextEventId == 0) {
                    pattern.nextEventId = 1;
                }
            }
            e.eventId = pattern.nextEventId;
            usedIds.insert(pattern.nextEventId);
            ++pattern.nextEventId;
            if (pattern.nextEventId == 0) {
                pattern.nextEventId = 1;
            }
        }
    }

    // Verify all IDs are unique (debug check)
    assert(usedIds.size() == pattern.notes.size());
}

// Helper: allocate a new unique eventId for a newly created event
static std::uint32_t allocateEventId(Pattern& pattern) {
    // Ensure existing IDs are unique first
    std::unordered_set<std::uint32_t> usedIds;
    usedIds.reserve(pattern.notes.size() * 2);
    for (const auto& e : pattern.notes) {
        if (e.eventId != 0) {
            usedIds.insert(e.eventId);
        }
    }

    // Update nextEventId to be greater than any existing ID
    for (const auto id : usedIds) {
        if (id >= pattern.nextEventId) {
            pattern.nextEventId = id + 1;
        }
    }

    // Find next available ID
    while (usedIds.contains(pattern.nextEventId)) {
        ++pattern.nextEventId;
        if (pattern.nextEventId == 0) {
            pattern.nextEventId = 1;
        }
    }

    const std::uint32_t newId = pattern.nextEventId;
    ++pattern.nextEventId;
    if (pattern.nextEventId == 0) {
        pattern.nextEventId = 1;
    }
    return newId;
}

ListEditService::AddResult ListEditService::addEvent(
        Pattern& pattern,
        MpcEditHistory& history,
        std::int32_t tick,
        std::int32_t durationTicks,
        std::uint8_t note,
        std::uint8_t velocity) {
    AddResult result;

    // Validate input values
    if (tick < 0 || static_cast<std::int64_t>(tick) >= pattern.lengthTicks) {
        result.success = false;
        result.error = "tick out of range";
        return result;
    }
    if (durationTicks < 0 || static_cast<std::int64_t>(durationTicks) > pattern.lengthTicks) {
        result.success = false;
        result.error = "duration out of range";
        return result;
    }
    if (velocity > 127) {
        result.success = false;
        result.error = "velocity out of range";
        return result;
    }
    if (tick + durationTicks > pattern.lengthTicks) {
        result.success = false;
        result.error = "event exceeds pattern length";
        return result;
    }

    // Ensure all existing events have IDs
    ensureEventId(pattern);

    const std::uint32_t newEventId = allocateEventId(pattern);

    auto addRedo = [&pattern, tick, durationTicks, note, velocity, newEventId]() mutable {
        auto& notes = pattern.notes;
        notes.push_back(MidiNoteEvent{tick, durationTicks, note, velocity, 127, 1, 0, newEventId});
        std::stable_sort(
                notes.begin(), notes.end(),
                [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                    return lhs.tick < rhs.tick;
                });
    };

    auto addUndo = [&pattern, newEventId]() mutable {
        auto& notes = pattern.notes;
        notes.erase(
                std::remove_if(
                        notes.begin(), notes.end(),
                        [newEventId](const MidiNoteEvent& n) {
                            return n.eventId == newEventId;
                        }),
                notes.end());
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Add Pattern Event",
            std::move(addUndo),
            std::move(addRedo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    addRedo();
    result.success = true;

    const auto& notes = pattern.notes;
    for (std::size_t i = 0; i < notes.size(); ++i) {
        if (notes[i].eventId == newEventId) {
            result.eventIndex = i;
            break;
        }
    }

    return result;
}

ListEditService::ModifyResult ListEditService::deleteEvent(
        Pattern& pattern,
        MpcEditHistory& history,
        std::size_t eventIndex) {
    ModifyResult result;

    ensureEventId(pattern);

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const std::uint32_t targetEventId = notes[eventIndex].eventId;
    const MidiNoteEvent deletedEvent = notes[eventIndex];

    auto delUndo = [&pattern, targetEventId, deletedEvent]() mutable {
        auto& n = pattern.notes;
        if (!findByEventId(pattern, targetEventId)) {
            n.push_back(deletedEvent);
            std::stable_sort(
                    n.begin(), n.end(),
                    [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    };

    auto delRedo = [&pattern, targetEventId]() mutable {
        auto& n = pattern.notes;
        n.erase(
                std::remove_if(
                        n.begin(), n.end(),
                        [targetEventId](const MidiNoteEvent& e) {
                            return e.eventId == targetEventId;
                        }),
                n.end());
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Delete Pattern Event",
            std::move(delUndo),
            std::move(delRedo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    delRedo();
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setNote(
        Pattern& pattern,
        MpcEditHistory& history,
        std::size_t eventIndex,
        std::uint8_t note) {
    ModifyResult result;

    ensureEventId(pattern);

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    // Note value is already constrained to 0-127 by uint8_t parameter
    // but we can add explicit check for clarity
    if (note > 127) {
        result.success = false;
        result.error = "note out of range";
        return result;
    }

    const std::uint32_t targetEventId = notes[eventIndex].eventId;
    const std::uint8_t oldNote = notes[eventIndex].note;

    auto undo = [&pattern, targetEventId, oldNote]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->note = oldNote;
        }
    };
    auto redo = [&pattern, targetEventId, note]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->note = note;
        }
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Note",
            std::move(undo),
            std::move(redo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    redo();
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setVelocity(
        Pattern& pattern,
        MpcEditHistory& history,
        std::size_t eventIndex,
        std::uint8_t velocity) {
    ModifyResult result;

    ensureEventId(pattern);

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    // Velocity value is already constrained to 0-127 by uint8_t parameter
    // but we can add explicit check for clarity
    if (velocity > 127) {
        result.success = false;
        result.error = "velocity out of range";
        return result;
    }

    const std::uint32_t targetEventId = notes[eventIndex].eventId;
    const std::uint8_t oldVelocity = notes[eventIndex].velocity;

    auto undo = [&pattern, targetEventId, oldVelocity]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->velocity = oldVelocity;
        }
    };
    auto redo = [&pattern, targetEventId, velocity]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->velocity = velocity;
        }
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Velocity",
            std::move(undo),
            std::move(redo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    redo();
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setDuration(
        Pattern& pattern,
        MpcEditHistory& history,
        std::size_t eventIndex,
        std::int32_t durationTicks) {
    ModifyResult result;

    ensureEventId(pattern);

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    // Validate duration value
    if (durationTicks < 0 || static_cast<std::int64_t>(durationTicks) > pattern.lengthTicks) {
        result.success = false;
        result.error = "duration out of range";
        return result;
    }

    const std::uint32_t targetEventId = notes[eventIndex].eventId;
    const std::int32_t oldDuration = notes[eventIndex].durationTicks;

    auto undo = [&pattern, targetEventId, oldDuration]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->durationTicks = oldDuration;
        }
    };
    auto redo = [&pattern, targetEventId, durationTicks]() mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->durationTicks = durationTicks;
        }
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Duration",
            std::move(undo),
            std::move(redo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    redo();
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setTick(
        Pattern& pattern,
        MpcEditHistory& history,
        std::size_t eventIndex,
        std::int32_t tick) {
    ModifyResult result;

    ensureEventId(pattern);

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    // Validate tick value
    if (tick < 0 || static_cast<std::int64_t>(tick) >= pattern.lengthTicks) {
        result.success = false;
        result.error = "tick out of range";
        return result;
    }

    const std::uint32_t targetEventId = notes[eventIndex].eventId;
    const std::int32_t oldTick = notes[eventIndex].tick;
    const std::uint8_t oldNote = notes[eventIndex].note;
    const std::uint8_t oldVelocity = notes[eventIndex].velocity;
    const std::int32_t oldDuration = notes[eventIndex].durationTicks;

    auto undo = [&pattern, targetEventId, oldTick, oldNote, oldVelocity, oldDuration]()
            mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->tick = oldTick;
            e->note = oldNote;
            e->velocity = oldVelocity;
            e->durationTicks = oldDuration;
            std::stable_sort(
                    pattern.notes.begin(), pattern.notes.end(),
                    [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    };

    auto redo = [&pattern, targetEventId, tick, oldNote, oldVelocity, oldDuration]()
            mutable {
        if (auto* e = findByEventId(pattern, targetEventId)) {
            e->tick = tick;
            e->note = oldNote;
            e->velocity = oldVelocity;
            e->durationTicks = oldDuration;
            std::stable_sort(
                    pattern.notes.begin(), pattern.notes.end(),
                    [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    };

    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Tick",
            std::move(undo),
            std::move(redo)})) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    redo();
    result.success = true;
    return result;
}

std::size_t ListEditService::eventCount(const Pattern& pattern) noexcept {
    return pattern.notes.size();
}

std::optional<MidiNoteEvent> ListEditService::getEvent(
        const Pattern& pattern,
        std::size_t eventIndex) noexcept {
    if (eventIndex >= pattern.notes.size()) {
        return std::nullopt;
    }
    return pattern.notes[eventIndex];
}

} // namespace mpc::domain
