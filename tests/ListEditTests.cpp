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

    return 0;
}
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
