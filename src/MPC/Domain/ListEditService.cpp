#include "MPC/Domain/ListEditService.h"

#include <algorithm>
#include <limits>
#include <optional>
#include <string>
#include <utility>

namespace mpc::domain {

ListEditService::AddResult ListEditService::addEvent(
        Pattern& pattern,
        MpcEditHistory& history,
        std::int32_t tick,
        std::int32_t durationTicks,
        std::uint8_t note,
        std::uint8_t velocity) {
    AddResult result;

    auto addRedo = [&pattern, tick, durationTicks, note, velocity]() mutable {
        auto& notes = pattern.notes;
        notes.erase(
                std::remove_if(
                        notes.begin(), notes.end(),
                        [tick, note](const MidiNoteEvent& n) {
                            return n.tick == tick &&
                                   n.note == static_cast<std::uint8_t>(note);
                        }),
                notes.end());
        notes.push_back(
                MidiNoteEvent{tick, durationTicks, note, velocity, 127, 1, 0});
        std::stable_sort(
                notes.begin(), notes.end(),
                [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                    return lhs.tick < rhs.tick;
                });
    };

    auto addUndo = [&pattern, tick, note]() mutable {
        auto& notes = pattern.notes;
        notes.erase(
                std::remove_if(
                        notes.begin(), notes.end(),
                        [tick, note](const MidiNoteEvent& n) {
                            return n.tick == tick &&
                                   n.note == static_cast<std::uint8_t>(note);
                        }),
                notes.end());
    };

    if (!history.record("Add Pattern Event", std::move(addUndo),
                        std::move(addRedo))) {
        result.success = false;
        result.error = "history record rejected";
        return result;
    }

    addRedo();
    result.success = true;

    const auto& notes = pattern.notes;
    for (std::size_t i = 0; i < notes.size(); ++i) {
        if (notes[i].tick == tick && notes[i].note == note &&
            notes[i].velocity == velocity &&
            notes[i].durationTicks == durationTicks) {
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

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const auto deletedNote = notes[eventIndex];

    auto delUndo = [&pattern, eventIndex, deletedNote]() mutable {
        auto& n = pattern.notes;
        n.insert(n.begin() + static_cast<std::ptrdiff_t>(eventIndex),
                 deletedNote);
        std::stable_sort(
                n.begin(), n.end(),
                [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                    return lhs.tick < rhs.tick;
                });
    };

    auto delRedo = [&pattern, eventIndex]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n.erase(n.begin() + static_cast<std::ptrdiff_t>(eventIndex));
        }
    };

    if (!history.record("Delete Pattern Event", std::move(delUndo),
                        std::move(delRedo))) {
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

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const auto oldNote = notes[eventIndex].note;

    auto undo = [&pattern, eventIndex, oldNote]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].note = oldNote;
        }
    };
    auto redo = [&pattern, eventIndex, note]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].note = note;
        }
    };

    if (!history.record("Change Pattern Event Note", std::move(undo),
                        std::move(redo))) {
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

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const auto oldVelocity = notes[eventIndex].velocity;

    auto undo = [&pattern, eventIndex, oldVelocity]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].velocity = oldVelocity;
        }
    };
    auto redo = [&pattern, eventIndex, velocity]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].velocity = velocity;
        }
    };

    if (!history.record("Change Pattern Event Velocity", std::move(undo),
                        std::move(redo))) {
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

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const auto oldDuration = notes[eventIndex].durationTicks;

    auto undo = [&pattern, eventIndex, oldDuration]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].durationTicks = oldDuration;
        }
    };
    auto redo = [&pattern, eventIndex, durationTicks]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].durationTicks = durationTicks;
        }
    };

    if (!history.record("Change Pattern Event Duration", std::move(undo),
                        std::move(redo))) {
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

    auto& notes = pattern.notes;
    if (eventIndex >= notes.size()) {
        result.success = false;
        result.error = "index out of range";
        return result;
    }

    const auto oldTick = notes[eventIndex].tick;
    const auto oldNote = notes[eventIndex].note;
    const auto oldVelocity = notes[eventIndex].velocity;
    const auto oldDuration = notes[eventIndex].durationTicks;

    auto undo = [&pattern, oldTick, oldNote, oldVelocity, oldDuration]()
            mutable {
        auto& n = pattern.notes;
        auto it = std::find_if(
                n.begin(), n.end(),
                [oldTick, oldNote](const MidiNoteEvent& e) {
                    return e.tick == oldTick && e.note == oldNote;
                });
        if (it != n.end()) {
            it->tick = oldTick;
            it->note = oldNote;
            it->velocity = oldVelocity;
            it->durationTicks = oldDuration;
            std::stable_sort(
                    n.begin(), n.end(),
                    [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    };

    auto redo = [&pattern, eventIndex, tick, oldNote, oldVelocity,
                 oldDuration]() mutable {
        auto& n = pattern.notes;
        if (eventIndex < n.size()) {
            n[eventIndex].tick = tick;
            n[eventIndex].note = oldNote;
            n[eventIndex].velocity = oldVelocity;
            n[eventIndex].durationTicks = oldDuration;
            std::stable_sort(
                    n.begin(), n.end(),
                    [](const MidiNoteEvent& lhs, const MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    };

    if (!history.record("Change Pattern Event Tick", std::move(undo),
                        std::move(redo))) {
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
