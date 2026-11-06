#include "MPC/Domain/MpcDomain.h"
#include "MPC/Domain/ListEditService.h"
#include "MPC/MpcEditHistory.h"

#include <algorithm>
#include <cassert>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

using mpc::domain::ListEditService;
using mpc::domain::MidiNoteEvent;
using mpc::domain::Pattern;
using mpc::domain::Sequence;
using mpc::domain::Track;
using mpc::MpcEditHistory;

static Track makeDrumTrack() {
    Track track;
    track.id = "track-drum";
    track.name = "DRUMS";
    track.type = mpc::domain::ProgramType::Drum;
    track.kind = mpc::domain::TrackKind::Drum;
    track.programId = "drum-program-1";

    Pattern pattern;
    pattern.id = "pattern-01";
    pattern.name = "Pattern 01";
    pattern.lengthTicks = 1920;

    track.patterns.push_back(pattern);
    return track;
}

static Sequence makeSequence() {
    Sequence sequence;
    sequence.id = "sequence-01";
    sequence.name = "Sequence 01";
    sequence.tempoBpm = 120.0;
    sequence.lengthTicks = 3840;
    sequence.loopEnabled = true;
    sequence.loopStartTicks = 0;
    sequence.loopEndTicks = 3840;
    sequence.quantizeGridTicks = 240;
    sequence.swingPercent = 0;

    sequence.tracks.push_back(makeDrumTrack());
    return sequence;
}

static bool notesEqual(
        const std::vector<MidiNoteEvent>& lhs,
        const std::vector<MidiNoteEvent>& rhs) {
    if (lhs.size() != rhs.size()) return false;
    for (std::size_t i = 0; i < lhs.size(); ++i) {
        const auto& a = lhs[i];
        const auto& b = rhs[i];
        if (a.tick != b.tick || a.durationTicks != b.durationTicks
                || a.note != b.note || a.velocity != b.velocity
                || a.probability != b.probability || a.ratchet != b.ratchet
                || a.nudgeTicks != b.nudgeTicks) return false;
    }
    return true;
}

int main() {
    // ---- Test 1: Add event -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;

        auto result = ListEditService::addEvent(
                pattern, history, 0, 480, 60, 100);
        assert(result.success);
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[0].velocity == 100);
        assert(pattern.notes[0].durationTicks == 480);
        assert(pattern.notes[0].tick == 0);

        assert(history.canUndo());
        assert(!history.canRedo());

        assert(history.undo());
        assert(pattern.notes.empty());

        assert(history.redo());
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].note == 60);
    }

    // ---- Test 2: Delete event -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({100, 480, 64, 90, 127, 1, 0});
        pattern.notes.push_back({200, 480, 67, 110, 127, 1, 0});

        MpcEditHistory history;

        auto result = ListEditService::deleteEvent(
                pattern, history, 0);
        assert(result.success);
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].note == 67);

        assert(history.undo());
        assert(pattern.notes.size() == 2);

        assert(history.redo());
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].note == 67);
    }

    // ---- Test 3: Change Note -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});

        MpcEditHistory history;

        auto result = ListEditService::setNote(
                pattern, history, 0, 72);
        assert(result.success);
        assert(pattern.notes[0].note == 72);

        assert(history.undo());
        assert(pattern.notes[0].note == 60);

        assert(history.redo());
        assert(pattern.notes[0].note == 72);
    }

    // ---- Test 4: Change Velocity -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});

        MpcEditHistory history;

        auto result = ListEditService::setVelocity(
                pattern, history, 0, 110);
        assert(result.success);
        assert(pattern.notes[0].velocity == 110);

        assert(history.undo());
        assert(pattern.notes[0].velocity == 100);

        assert(history.redo());
        assert(pattern.notes[0].velocity == 110);
    }

    // ---- Test 5: Change Tick with sort -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({200, 480, 64, 100, 127, 1, 0});

        MpcEditHistory history;

        auto result = ListEditService::setTick(
                pattern, history, 0, 300);
        assert(result.success);
        assert(pattern.notes.size() == 2);
        assert(pattern.notes[0].tick == 200);
        assert(pattern.notes[0].note == 64);
        assert(pattern.notes[1].tick == 300);
        assert(pattern.notes[1].note == 60);

        assert(history.undo());
        assert(pattern.notes[0].tick == 0);
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[1].tick == 200);
        assert(pattern.notes[1].note == 64);

        assert(history.redo());
        assert(pattern.notes[0].tick == 200);
        assert(pattern.notes[0].note == 64);
        assert(pattern.notes[1].tick == 300);
        assert(pattern.notes[1].note == 60);
    }

    // ---- Test 6: Change Duration -> Undo -> Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});

        MpcEditHistory history;

        auto result = ListEditService::setDuration(
                pattern, history, 0, 960);
        assert(result.success);
        assert(pattern.notes[0].durationTicks == 960);

        assert(history.undo());
        assert(pattern.notes[0].durationTicks == 480);

        assert(history.redo());
        assert(pattern.notes[0].durationTicks == 960);
    }

    // ---- Test 7: Multiple events on same Tick ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({100, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({100, 480, 64, 110, 127, 1, 0});
        pattern.notes.push_back({100, 480, 67, 120, 127, 1, 0});

        assert(pattern.notes.size() == 3);
        assert(notesEqual(pattern.notes, {
            {100, 480, 60, 100, 127, 1, 0},
            {100, 480, 64, 110, 127, 1, 0},
            {100, 480, 67, 120, 127, 1, 0},
        }));
    }
    // ---- Test 9: Invalid event index rejected ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        assert(pattern.notes.empty());

        MpcEditHistory history;
        auto result = ListEditService::setNote(pattern, history, 0, 72);
        assert(!result.success);
        result = ListEditService::setVelocity(pattern, history, 0, 110);
        assert(!result.success);
        result = ListEditService::setDuration(pattern, history, 0, 960);
        assert(!result.success);
        result = ListEditService::setTick(pattern, history, 0, 300);
        assert(!result.success);
        result = ListEditService::deleteEvent(pattern, history, 0);
        assert(!result.success);
    }

    // ---- Test 10: Multiple Patterns on one Track ----
    {
        Sequence seq = makeSequence();
        auto& track = seq.tracks[0];

        Pattern patternB;
        patternB.id = "pattern-02";
        patternB.name = "Pattern 02";
        patternB.lengthTicks = 1920;
        track.patterns.push_back(patternB);

        assert(track.selectPattern("pattern-02"));
        auto* selected = track.selectedPattern();
        assert(selected != nullptr);
        assert(selected->id == "pattern-02");

        MpcEditHistory history;
        auto result = ListEditService::addEvent(
                *selected, history, 0, 480, 60, 100);
        assert(result.success);
        assert(selected->notes.size() == 1);
        assert(track.patterns[0].notes.empty());
    }

    // ---- Test 11: Multiple Tracks ----
    {
        Sequence seq = makeSequence();

        Track trackB;
        trackB.id = "track-2";
        trackB.name = "BASS";
        trackB.type = mpc::domain::ProgramType::Drum;
        trackB.kind = mpc::domain::TrackKind::Drum;
        trackB.programId = "drum-program-1";
        Pattern patternB;
        patternB.id = "pattern-bass-01";
        patternB.name = "Bass Pattern 01";
        patternB.lengthTicks = 1920;
        trackB.patterns.push_back(patternB);
        seq.tracks.push_back(std::move(trackB));

        MpcEditHistory history;
        auto result = ListEditService::addEvent(
                seq.tracks[0].patterns[0], history, 0, 480, 36, 120);
        assert(result.success);
        assert(seq.tracks[0].patterns[0].notes.size() == 1);
        assert(seq.tracks[1].patterns[0].notes.empty());
    }

    // ---- Test 12: Stable sorting with equal Tick ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({100, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({100, 480, 64, 110, 127, 1, 0});
        pattern.notes.push_back({100, 480, 67, 120, 127, 1, 0});

        MpcEditHistory history;
        auto result = ListEditService::setTick(pattern, history, 1, 50);
        assert(result.success);
        assert(pattern.notes[0].tick == 50);
        assert(pattern.notes[0].note == 64);
        assert(pattern.notes[1].tick == 100);
        assert(pattern.notes[1].note == 60);
        assert(pattern.notes[2].tick == 100);
        assert(pattern.notes[2].note == 67);

        assert(history.undo());
        assert(pattern.notes[0].tick == 100);
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[1].tick == 100);
        assert(pattern.notes[1].note == 64);
        assert(pattern.notes[2].tick == 100);
        assert(pattern.notes[2].note == 67);

        assert(history.redo());
        assert(pattern.notes[0].tick == 50);
        assert(pattern.notes[0].note == 64);
    }

    // ---- Test 13: Preserve other events when modifying one ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({200, 480, 64, 110, 127, 1, 0});
        pattern.notes.push_back({400, 480, 67, 120, 127, 1, 0});

        MpcEditHistory history;
        auto result = ListEditService::setVelocity(pattern, history, 1, 50);
        assert(result.success);
        assert(pattern.notes[0].velocity == 100);
        assert(pattern.notes[1].velocity == 50);
        assert(pattern.notes[2].velocity == 120);

        assert(history.undo());
        assert(pattern.notes[1].velocity == 110);

        assert(history.redo());
        assert(pattern.notes[1].velocity == 50);
    }

    // ---- Test 14: Repeated Undo/Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        ListEditService::addEvent(pattern, history, 100, 480, 64, 110);
        ListEditService::addEvent(pattern, history, 200, 480, 67, 120);

        assert(pattern.notes.size() == 3);

        assert(history.undo());
        assert(pattern.notes.size() == 2);
        assert(history.undo());
        assert(pattern.notes.size() == 1);
        assert(history.undo());
        assert(pattern.notes.empty());

        assert(history.redo());
        assert(pattern.notes.size() == 1);
        assert(history.redo());
        assert(pattern.notes.size() == 2);
        assert(history.redo());
        assert(pattern.notes.size() == 3);
    }

    return 0;
}

    // ---- Test 8: Multiple events with same MIDI note ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({200, 480, 60, 110, 127, 1, 0});
        pattern.notes.push_back({400, 480, 60, 120, 127, 1, 0});

        assert(pattern.notes.size() == 3);
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[1].note == 60);
        assert(pattern.notes[2].note == 60);
        assert(pattern.notes[0].tick == 0);
        assert(pattern.notes[1].tick == 200);
        assert(pattern.notes[2].tick == 400);
    }

    // ---- Test 9: Invalid event index rejected ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        assert(pattern.notes.empty());

        MpcEditHistory history;
        auto result = ListEditService::setNote(pattern, history, 0, 72);
        assert(!result.success);
        result = ListEditService::setVelocity(pattern, history, 0, 110);
        assert(!result.success);
        result = ListEditService::setDuration(pattern, history, 0, 960);
        assert(!result.success);
        result = ListEditService::setTick(pattern, history, 0, 300);
        assert(!result.success);
        result = ListEditService::deleteEvent(pattern, history, 0);
        assert(!result.success);
    }

    // ---- Test 10: Multiple Patterns on one Track ----
    {
        Sequence seq = makeSequence();
        auto& track = seq.tracks[0];

        Pattern patternB;
        patternB.id = "pattern-02";
        patternB.name = "Pattern 02";
        patternB.lengthTicks = 1920;
        track.patterns.push_back(patternB);

        assert(track.selectPattern("pattern-02"));
        auto* selected = track.selectedPattern();
        assert(selected != nullptr);
        assert(selected->id == "pattern-02");

        MpcEditHistory history;
        auto result = ListEditService::addEvent(
                *selected, history, 0, 480, 60, 100);
        assert(result.success);
        assert(selected->notes.size() == 1);
        assert(track.patterns[0].notes.empty());
    }

    // ---- Test 11: Multiple Tracks ----
    {
        Sequence seq = makeSequence();

        Track trackB;
        trackB.id = "track-2";
        trackB.name = "BASS";
        trackB.type = mpc::domain::ProgramType::Drum;
        trackB.kind = mpc::domain::TrackKind::Drum;
        trackB.programId = "drum-program-1";
        Pattern patternB;
        patternB.id = "pattern-bass-01";
        patternB.name = "Bass Pattern 01";
        patternB.lengthTicks = 1920;
        trackB.patterns.push_back(patternB);
        seq.tracks.push_back(std::move(trackB));

        MpcEditHistory history;
        auto result = ListEditService::addEvent(
                seq.tracks[0].patterns[0], history, 0, 480, 36, 120);
// ---- Test 12: Stable sorting with equal Tick ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({100, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({100, 480, 64, 110, 127, 1, 0});
        pattern.notes.push_back({100, 480, 67, 120, 127, 1, 0});

        MpcEditHistory history;
        auto result = ListEditService::setTick(pattern, history, 1, 50);
        assert(result.success);
        assert(pattern.notes[0].tick == 50);
        assert(pattern.notes[0].note == 64);
        assert(pattern.notes[1].tick == 100);
        assert(pattern.notes[1].note == 60);
        assert(pattern.notes[2].tick == 100);
        assert(pattern.notes[2].note == 67);

        assert(history.undo());
        assert(pattern.notes[0].tick == 100);
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[1].tick == 100);
        assert(pattern.notes[1].note == 64);
        assert(pattern.notes[2].tick == 100);
        assert(pattern.notes[2].note == 67);

        assert(history.redo());
        assert(pattern.notes[0].tick == 50);
        assert(pattern.notes[0].note == 64);
    }

    // ---- Test 13: Preserve other events when modifying one ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0});
        pattern.notes.push_back({200, 480, 64, 110, 127, 1, 0});
        pattern.notes.push_back({400, 480, 67, 120, 127, 1, 0});

        MpcEditHistory history;
        auto result = ListEditService::setVelocity(pattern, history, 1, 50);
        assert(result.success);
        assert(pattern.notes[0].velocity == 100);
        assert(pattern.notes[1].velocity == 50);
        assert(pattern.notes[2].velocity == 120);

        assert(history.undo());
        assert(pattern.notes[1].velocity == 110);

        assert(history.redo());
        assert(pattern.notes[1].velocity == 50);
    }

    // ---- Test 14: Repeated Undo/Redo ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        ListEditService::addEvent(pattern, history, 100, 480, 64, 110);
        ListEditService::addEvent(pattern, history, 200, 480, 67, 120);

        assert(pattern.notes.size() == 3);

        assert(history.undo());
        assert(pattern.notes.size() == 2);
        assert(history.undo());
        assert(pattern.notes.size() == 1);
        assert(history.undo());
        assert(pattern.notes.empty());

        assert(history.redo());
        assert(pattern.notes.size() == 1);
        assert(history.redo());
        assert(pattern.notes.size() == 2);
        assert(history.redo());
        assert(pattern.notes.size() == 3);
    }

    // ---- Test 15: eventId assigned on addEvent ----
// ---- Test 15: eventId assigned on addEvent ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        auto result = ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        assert(result.success);
        assert(pattern.notes[0].eventId != 0);
        assert(pattern.nextEventId == 2);

        // Undo should not affect nextEventId counter
        assert(history.undo());
        assert(pattern.notes.empty());

        // Redo should restore the event with same eventId
        assert(history.redo());
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].eventId != 0);
    }

    // ---- Test 16: eventId survives multiple modifications ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        const std::uint32_t eventId = pattern.notes[0].eventId;
        assert(eventId != 0);

        // Modify note
        ListEditService::setNote(pattern, history, 0, 72);
        assert(pattern.notes[0].eventId == eventId);
        assert(pattern.notes[0].note == 72);

        // Modify velocity
        ListEditService::setVelocity(pattern, history, 0, 50);
        assert(pattern.notes[0].eventId == eventId);
        assert(pattern.notes[0].velocity == 50);

        // Modify duration
        ListEditService::setDuration(pattern, history, 0, 960);
        assert(pattern.notes[0].eventId == eventId);
        assert(pattern.notes[0].durationTicks == 960);

        // Modify tick (with re-sort)
        ListEditService::setTick(pattern, history, 0, 200);
        assert(pattern.notes[0].eventId == eventId);
        assert(pattern.notes[0].tick == 200);

        // Full Undo/Redo cycle
        assert(history.undo()); // undo setTick
        assert(pattern.notes[0].tick == 0);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.undo()); // undo setDuration
        assert(pattern.notes[0].durationTicks == 480);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.undo()); // undo setVelocity
        assert(pattern.notes[0].velocity == 100);
        assert(pattern.notes[0].eventId == eventId);

// ---- Test 18: eventId assigned to legacy events on first edit ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        // Manually add events without eventId (simulating deserialized project)
        pattern.notes.push_back({0, 480, 60, 100, 127, 1, 0}); // eventId = 0
        pattern.notes.push_back({100, 480, 64, 110, 127, 1, 0}); // eventId = 0
        assert(pattern.notes[0].eventId == 0);
        assert(pattern.notes[1].eventId == 0);
        assert(pattern.nextEventId == 1);

        MpcEditHistory history;
        // First edit should assign eventIds
        auto result = ListEditService::setVelocity(pattern, history, 0, 50);
        assert(result.success);
        assert(pattern.notes[0].eventId != 0);
        assert(pattern.notes[1].eventId != 0);
        assert(pattern.notes[0].eventId != pattern.notes[1].eventId);

        // Undo should work with assigned eventIds
        assert(history.undo());
        assert(pattern.notes[0].velocity == 100);
        assert(pattern.notes[0].eventId != 0);
    }

    // ---- Test 19: Multiple modifications then full undo stack ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        ListEditService::addEvent(pattern, history, 0, 480, 60, 100);   // 1
        ListEditService::addEvent(pattern, history, 100, 480, 64, 110);  // 2
        ListEditService::addEvent(pattern, history, 200, 480, 67, 120);  // 3
        ListEditService::setNote(pattern, history, 1, 72);               // modify #2
        ListEditService::setVelocity(pattern, history, 0, 50);           // modify #1
        ListEditService::setTick(pattern, history, 2, 300);              // modify #3 with sort

        assert(pattern.notes.size() == 3);
        // Note order after sort: #1(tick 0), #2(tick 100), #3(tick 300)

        // Undo all
        assert(history.undo()); // undo setTick
        assert(pattern.notes[2].tick == 200);

        assert(history.undo()); // undo setVelocity
        assert(pattern.notes[0].velocity == 100);

        assert(history.undo()); // undo setNote
        assert(pattern.notes[1].note == 64);

        assert(history.undo()); // undo add #3
        assert(pattern.notes.size() == 2);

        assert(history.undo()); // undo add #2
        assert(pattern.notes.size() == 1);

        assert(history.undo()); // undo add #1
        assert(pattern.notes.empty());

        // Redo all
        assert(history.redo()); // redo add #1
        assert(pattern.notes.size() == 1);

        assert(history.redo()); // redo add #2
        assert(pattern.notes.size() == 2);

        assert(history.redo()); // redo add #3
        assert(pattern.notes.size() == 3);

        assert(history.redo()); // redo setNote
        assert(pattern.notes[1].note == 72);

        assert(history.redo()); // redo setVelocity
        assert(pattern.notes[0].velocity == 50);

        assert(history.redo()); // redo setTick
        assert(pattern.notes[2].tick == 300);
    }

    // ---- Test 20: Delete one of multiple identical events ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        // Add three events with same tick and note, different velocities
        ListEditService::addEvent(pattern, history, 100, 480, 60, 100);
        ListEditService::addEvent(pattern, history, 100, 480, 60, 110);
        ListEditService::addEvent(pattern, history, 100, 480, 60, 120);
        assert(pattern.notes.size() == 3);

        const std::uint32_t idToDelete = pattern.notes[1].eventId; // middle one, velocity 110

        // Delete by index
        auto result = ListEditService::deleteEvent(pattern, history, 1);
        assert(result.success);
        assert(pattern.notes.size() == 2);

        // Verify the correct event was deleted
        bool found = false;
        for (const auto& n : pattern.notes) {
            if (n.eventId == idToDelete) found = true;
        }
        assert(!found);

        // Undo should restore the exact deleted event
        assert(history.undo());
        assert(pattern.notes.size() == 3);
        found = false;
        for (const auto& n : pattern.notes) {
            if (n.eventId == idToDelete) found = true;
        }
        assert(found);

        // Redo should delete it again
        assert(history.redo());
        assert(pattern.notes.size() == 2);
        found = false;
        for (const auto& n : pattern.notes) {
            if (n.eventId == idToDelete) found = true;
        }
        assert(!found);
    }

    // ---- Test 21: Undo history cleared on project replacement ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        ListEditService::addEvent(pattern, history, 100, 480, 64, 110);
        assert(history.canUndo());
        assert(history.canRedo() == false);

        // Clear history (simulates project replacement)
        history.clear();
        assert(!history.canUndo());
        assert(!history.canRedo());

        // New operations on cleared history
        ListEditService::addEvent(pattern, history, 200, 480, 67, 120);
        assert(history.canUndo());
        assert(history.undo());
        assert(pattern.notes.size() == 2); // Original 2 events remain
    }

    return 0;
}
        assert(history.undo()); // undo setNote
        assert(pattern.notes[0].note == 60);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.redo()); // redo setNote
        assert(pattern.notes[0].note == 72);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.redo()); // redo setVelocity
        assert(pattern.notes[0].velocity == 50);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.redo()); // redo setDuration
        assert(pattern.notes[0].durationTicks == 960);
        assert(pattern.notes[0].eventId == eventId);

        assert(history.redo()); // redo setTick
        assert(pattern.notes[0].tick == 200);
        assert(pattern.notes[0].eventId == eventId);
    }

    // ---- Test 17: Add duplicate tick+note with different velocity preserves both ----
    {
        Sequence seq = makeSequence();
        auto& pattern = seq.tracks[0].patterns[0];

        MpcEditHistory history;
        // Add first event at tick 0, note 60, velocity 100
        auto result1 = ListEditService::addEvent(pattern, history, 0, 480, 60, 100);
        assert(result1.success);
        assert(pattern.notes.size() == 1);
        const std::uint32_t id1 = pattern.notes[0].eventId;

        // Add second event at same tick, same note, different velocity
        auto result2 = ListEditService::addEvent(pattern, history, 0, 480, 60, 80);
        assert(result2.success);
        assert(pattern.notes.size() == 2);

        // Both events should exist with different velocities
        bool found1 = false, found2 = false;
        for (const auto& n : pattern.notes) {
            if (n.eventId == id1 && n.velocity == 100) found1 = true;
            if (n.eventId != id1 && n.velocity == 80) found2 = true;
        }
        assert(found1 && found2);

        // Undo second add should only remove the second event
        assert(history.undo());
        assert(pattern.notes.size() == 1);
        assert(pattern.notes[0].eventId == id1);
        assert(pattern.notes[0].velocity == 100);

        assert(history.redo());
        assert(pattern.notes.size() == 2);
    }
