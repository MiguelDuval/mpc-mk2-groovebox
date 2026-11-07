#include "MPC/Domain/ListEditService.h"

#include <algorithm>
#include <cstdint>
#include <optional>
#include <unordered_set>

namespace mpc::domain {
namespace {

MidiNoteEvent* findByEventId(Pattern& pattern, std::uint32_t eventId) {
    if (eventId == 0) return nullptr;
    for (auto& event : pattern.notes) {
        if (event.eventId == eventId) return &event;
    }
    return nullptr;
}

void sortByTick(Pattern& pattern) {
    std::stable_sort(pattern.notes.begin(), pattern.notes.end(),
            [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                return lhs.tick < rhs.tick;
            });
}

bool takeNextAvailableId(
        const std::unordered_set<std::uint32_t>& usedIds,
        std::uint32_t& candidate,
        std::uint32_t& allocatedId) {
    if (candidate == 0) candidate = 1;

    // At most N+1 candidates are needed when N IDs are occupied.
    for (std::size_t attempts = 0; attempts <= usedIds.size(); ++attempts) {
        if (!usedIds.contains(candidate)) {
            allocatedId = candidate;
            ++candidate;
            if (candidate == 0) candidate = 1;
            return true;
        }
        ++candidate;
        if (candidate == 0) candidate = 1;
    }
    return false;
}

bool ensureEventIds(Pattern& pattern) {
    std::unordered_set<std::uint32_t> usedIds;
    usedIds.reserve(pattern.notes.size());

    // Keep the first occurrence; mark subsequent duplicate IDs for repair.
    for (auto& event : pattern.notes) {
        if (event.eventId != 0 && !usedIds.insert(event.eventId).second) {
            event.eventId = 0;
        }
    }

    std::uint32_t candidate = pattern.nextEventId == 0 ? 1 : pattern.nextEventId;
    for (auto& event : pattern.notes) {
        if (event.eventId != 0) continue;

        std::uint32_t allocatedId = 0;
        if (!takeNextAvailableId(usedIds, candidate, allocatedId)) return false;
        event.eventId = allocatedId;
        usedIds.insert(allocatedId);
    }

    pattern.nextEventId = candidate;
    return true;
}

bool allocateEventId(Pattern& pattern, std::uint32_t& eventId) {
    std::unordered_set<std::uint32_t> usedIds;
    usedIds.reserve(pattern.notes.size());
    for (const auto& event : pattern.notes) {
        if (event.eventId != 0) usedIds.insert(event.eventId);
    }

    std::uint32_t candidate = pattern.nextEventId == 0 ? 1 : pattern.nextEventId;
    if (!takeNextAvailableId(usedIds, candidate, eventId)) return false;
    pattern.nextEventId = candidate;
    return true;
}

} // namespace

ListEditService::AddResult ListEditService::addEvent(
        Pattern& pattern, MpcEditHistory& history, std::int32_t tick,
        std::int32_t durationTicks, std::uint8_t note, std::uint8_t velocity) {
    AddResult result;

    if (pattern.lengthTicks <= 0 || tick < 0
            || static_cast<std::int64_t>(tick) >= pattern.lengthTicks) {
        result.error = "tick out of range";
        return result;
    }
    if (durationTicks < 0
            || static_cast<std::int64_t>(durationTicks) > pattern.lengthTicks) {
        result.error = "duration out of range";
        return result;
    }
    if (note > 127) {
        result.error = "note out of range";
        return result;
    }
    if (velocity > 127) {
        result.error = "velocity out of range";
        return result;
    }
    if (static_cast<std::int64_t>(tick) + static_cast<std::int64_t>(durationTicks)
            > static_cast<std::int64_t>(pattern.lengthTicks)) {
        result.error = "event exceeds pattern length";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    std::uint32_t newEventId = 0;
    if (!allocateEventId(pattern, newEventId)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const MidiNoteEvent newEvent{tick, durationTicks, note, velocity, 127, 1, 0, newEventId};
    auto redo = [&pattern, newEvent]() -> bool {
        if (findByEventId(pattern, newEvent.eventId) != nullptr) return false;
        pattern.notes.push_back(newEvent);
        sortByTick(pattern);
        return true;
    };
    auto undo = [&pattern, newEventId]() -> bool {
        auto it = std::find_if(pattern.notes.begin(), pattern.notes.end(),
                [newEventId](const MidiNoteEvent& event) {
                    return event.eventId == newEventId;
                });
        if (it == pattern.notes.end()) return false;
        pattern.notes.erase(it);
        return true;
    };

    if (!redo()) {
        result.error = "event insertion failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Add Pattern Event", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }

    result.success = true;
    for (std::size_t i = 0; i < pattern.notes.size(); ++i) {
        if (pattern.notes[i].eventId == newEventId) {
            result.eventIndex = i;
            break;
        }
    }
    return result;
}

ListEditService::ModifyResult ListEditService::deleteEvent(
        Pattern& pattern, MpcEditHistory& history, std::size_t eventIndex) {
    ModifyResult result;
    if (eventIndex >= pattern.notes.size()) {
        result.error = "index out of range";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const MidiNoteEvent deletedEvent = pattern.notes[eventIndex];
    const std::uint32_t targetId = deletedEvent.eventId;
    const std::size_t deletedIndex = eventIndex;
    auto undo = [&pattern, deletedEvent, deletedIndex]() -> bool {
        if (findByEventId(pattern, deletedEvent.eventId) != nullptr) return false;

        // Undo must restore the exact original order, including the position
        // among otherwise identical notes that share the same tick.
        const std::size_t insertionIndex = std::min(deletedIndex, pattern.notes.size());
        pattern.notes.insert(
                pattern.notes.begin() + static_cast<std::ptrdiff_t>(insertionIndex),
                deletedEvent);
        return true;
    };
    auto redo = [&pattern, targetId]() -> bool {
        auto it = std::find_if(pattern.notes.begin(), pattern.notes.end(),
                [targetId](const MidiNoteEvent& event) { return event.eventId == targetId; });
        if (it == pattern.notes.end()) return false;
        pattern.notes.erase(it);
        return true;
    };

    if (!redo()) {
        result.error = "event deletion failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Delete Pattern Event", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setNote(
        Pattern& pattern, MpcEditHistory& history, std::size_t eventIndex,
        std::uint8_t note) {
    ModifyResult result;
    if (eventIndex >= pattern.notes.size()) {
        result.error = "index out of range";
        return result;
    }
    if (note > 127) {
        result.error = "note out of range";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const std::uint32_t id = pattern.notes[eventIndex].eventId;
    const std::uint8_t oldValue = pattern.notes[eventIndex].note;
    auto undo = [&pattern, id, oldValue]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->note = oldValue;
        return true;
    };
    auto redo = [&pattern, id, note]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->note = note;
        return true;
    };

    if (!redo()) {
        result.error = "event modification failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Note", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setVelocity(
        Pattern& pattern, MpcEditHistory& history, std::size_t eventIndex,
        std::uint8_t velocity) {
    ModifyResult result;
    if (eventIndex >= pattern.notes.size()) {
        result.error = "index out of range";
        return result;
    }
    if (velocity > 127) {
        result.error = "velocity out of range";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const std::uint32_t id = pattern.notes[eventIndex].eventId;
    const std::uint8_t oldValue = pattern.notes[eventIndex].velocity;
    auto undo = [&pattern, id, oldValue]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->velocity = oldValue;
        return true;
    };
    auto redo = [&pattern, id, velocity]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->velocity = velocity;
        return true;
    };

    if (!redo()) {
        result.error = "event modification failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Velocity", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setDuration(
        Pattern& pattern, MpcEditHistory& history, std::size_t eventIndex,
        std::int32_t durationTicks) {
    ModifyResult result;
    if (eventIndex >= pattern.notes.size()) {
        result.error = "index out of range";
        return result;
    }
    if (durationTicks < 0
            || static_cast<std::int64_t>(durationTicks) > pattern.lengthTicks) {
        result.error = "duration out of range";
        return result;
    }
    if (static_cast<std::int64_t>(pattern.notes[eventIndex].tick)
                    + static_cast<std::int64_t>(durationTicks)
            > static_cast<std::int64_t>(pattern.lengthTicks)) {
        result.error = "event exceeds pattern length";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const std::uint32_t id = pattern.notes[eventIndex].eventId;
    const std::int32_t oldValue = pattern.notes[eventIndex].durationTicks;
    auto undo = [&pattern, id, oldValue]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->durationTicks = oldValue;
        return true;
    };
    auto redo = [&pattern, id, durationTicks]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->durationTicks = durationTicks;
        return true;
    };

    if (!redo()) {
        result.error = "event modification failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Duration", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }
    result.success = true;
    return result;
}

ListEditService::ModifyResult ListEditService::setTick(
        Pattern& pattern, MpcEditHistory& history, std::size_t eventIndex,
        std::int32_t tick) {
    ModifyResult result;
    if (eventIndex >= pattern.notes.size()) {
        result.error = "index out of range";
        return result;
    }
    if (pattern.lengthTicks <= 0 || tick < 0
            || static_cast<std::int64_t>(tick) >= pattern.lengthTicks) {
        result.error = "tick out of range";
        return result;
    }
    if (static_cast<std::int64_t>(tick)
                    + static_cast<std::int64_t>(pattern.notes[eventIndex].durationTicks)
            > static_cast<std::int64_t>(pattern.lengthTicks)) {
        result.error = "event exceeds pattern length";
        return result;
    }
    if (!ensureEventIds(pattern)) {
        result.error = "event ID space exhausted";
        return result;
    }

    const std::uint32_t id = pattern.notes[eventIndex].eventId;
    const std::int32_t oldValue = pattern.notes[eventIndex].tick;
    const std::size_t oldIndex = eventIndex;
    auto undo = [&pattern, id, oldValue, oldIndex]() -> bool {
        auto it = std::find_if(pattern.notes.begin(), pattern.notes.end(),
                [id](const MidiNoteEvent& event) { return event.eventId == id; });
        if (it == pattern.notes.end()) return false;

        // Restore the event to its pre-edit position before leaving all
        // equal-tick neighbours in their original stable order.
        MidiNoteEvent restored = *it;
        pattern.notes.erase(it);
        restored.tick = oldValue;
        const std::size_t insertionIndex = std::min(oldIndex, pattern.notes.size());
        pattern.notes.insert(
                pattern.notes.begin() + static_cast<std::ptrdiff_t>(insertionIndex),
                restored);
        return true;
    };
    auto redo = [&pattern, id, tick]() -> bool {
        auto* event = findByEventId(pattern, id);
        if (!event) return false;
        event->tick = tick;
        sortByTick(pattern);
        return true;
    };

    if (!redo()) {
        result.error = "event modification failed";
        return result;
    }
    if (!history.record(mpc::MpcEditHistory::Command{
            "Change Pattern Event Tick", undo, redo})) {
        (void)undo();
        result.error = "history record rejected";
        return result;
    }
    result.success = true;
    return result;
}

std::size_t ListEditService::eventCount(const Pattern& pattern) noexcept {
    return pattern.notes.size();
}

std::optional<MidiNoteEvent> ListEditService::getEvent(
        const Pattern& pattern, std::size_t eventIndex) noexcept {
    if (eventIndex >= pattern.notes.size()) return std::nullopt;
    return pattern.notes[eventIndex];
}

} // namespace mpc::domain
