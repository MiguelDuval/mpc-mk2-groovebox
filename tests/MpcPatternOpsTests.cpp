#include "MPC/MpcEditHistory.h"
#include "MPC/Sequencer/MpcArrangementOps.h"
#include "MPC/Sequencer/MpcPatternOps.h"

#include <array>
#include <cassert>

int main() {
    // Explicit Arrangement Pattern placements reference Pattern data without copying it.
    mpc::domain::Sequence placementSequence;
    placementSequence.id = "sequence-placement-test";
    placementSequence.lengthTicks = 3840;
    mpc::domain::Track placementTrack;
    placementTrack.id = "track-placement-test";
    mpc::domain::Pattern placementPattern;
    placementPattern.id = "pattern-placement-test";
    placementPattern.lengthTicks = 1920;
    placementPattern.notes = {{0, 120, 36, 97, 88, 3, -11}};
    placementTrack.patterns.push_back(placementPattern);
    placementSequence.tracks.push_back(placementTrack);

    assert(mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-01", "pattern-placement-test", 0, 960));
    assert(mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-02", "pattern-placement-test", 960, 960));
    assert(mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-03", "pattern-placement-test", 1920, 960, 960));
    const auto& placementTrackAfterAdd = placementSequence.tracks[0];
    assert(placementTrackAfterAdd.placements.size() == 3);
    assert(placementTrackAfterAdd.placements[0].id == "clip-01");
    assert(placementTrackAfterAdd.placements[0].startTick == 0);
    assert(placementTrackAfterAdd.placements[1].id == "clip-02");
    assert(placementTrackAfterAdd.placements[1].startTick == 960);
    assert(placementTrackAfterAdd.placements[2].id == "clip-03");
    assert(placementTrackAfterAdd.placements[2].sourceOffsetTicks == 960);

    // Duplicate creates a new clip instance that reuses the exact source range.
    mpc::domain::Sequence duplicatePlacementSequence;
    duplicatePlacementSequence.id = "duplicate-placement-sequence";
    duplicatePlacementSequence.lengthTicks = 3840;
    mpc::domain::Track duplicatePlacementTrack;
    duplicatePlacementTrack.id = "duplicate-placement-track";
    mpc::domain::Pattern duplicatePlacementPattern;
    duplicatePlacementPattern.id = "duplicate-placement-pattern";
    duplicatePlacementPattern.lengthTicks = 1920;
    duplicatePlacementPattern.notes = {{240, 180, 36, 97, 88, 3, -11}};
    duplicatePlacementTrack.patterns.push_back(duplicatePlacementPattern);
    duplicatePlacementSequence.tracks.push_back(duplicatePlacementTrack);
    assert(mpc::sequencer::addPatternPlacement(
            duplicatePlacementSequence, 0, "clip-source",
            "duplicate-placement-pattern", 0, 960, 240));
    const auto duplicateBaselineNotes =
            duplicatePlacementSequence.tracks[0].patterns[0].notes;
    const auto notesEqual = [](
            const std::vector<mpc::domain::MidiNoteEvent>& lhs,
            const std::vector<mpc::domain::MidiNoteEvent>& rhs) {
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
    };
    assert(mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "clip-source", "clip-copy", 960));
    auto findPlacement = [&duplicatePlacementSequence](const std::string& id)
            -> const mpc::domain::PatternPlacement* {
        const auto& placements = duplicatePlacementSequence.tracks[0].placements;
        const auto found = std::find_if(
                placements.begin(), placements.end(),
                [&id](const mpc::domain::PatternPlacement& placement) {
                    return placement.id == id;
                });
        return found == placements.end() ? nullptr : &*found;
    };
    const auto* duplicatedPlacement = findPlacement("clip-copy");
    const auto* sourcePlacement = findPlacement("clip-source");
    assert(duplicatedPlacement != nullptr && sourcePlacement != nullptr);
    assert(duplicatedPlacement->id != sourcePlacement->id);
    assert(duplicatedPlacement->patternId == sourcePlacement->patternId);
    assert(duplicatedPlacement->startTick == 960);
    assert(duplicatedPlacement->lengthTicks == sourcePlacement->lengthTicks);
    assert(duplicatedPlacement->sourceOffsetTicks == sourcePlacement->sourceOffsetTicks);
    assert(duplicatePlacementSequence.tracks[0].placements.size() == 2);
    assert(notesEqual(duplicatePlacementSequence.tracks[0].patterns[0].notes, duplicateBaselineNotes));
    assert(!mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "clip-source", "clip-id-collision", 960));
    assert(!mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "clip-source", "clip-copy", 1920));
    assert(!mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "missing-clip", "clip-missing-source", 1920));
    assert(!mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "clip-source", "clip-overlap", 480));
    assert(!mpc::sequencer::duplicatePatternPlacement(
            duplicatePlacementSequence, 0, "clip-source", "clip-outside", 3000));
    assert(duplicatePlacementSequence.tracks[0].placements.size() == 2);

    mpc::MpcEditHistory duplicatePlacementHistory;
    const auto undoPlacementDuplicate = [&duplicatePlacementSequence]() {
        return mpc::sequencer::erasePatternPlacement(
                duplicatePlacementSequence, 0, "clip-copy");
    };
    const auto redoPlacementDuplicate = [&duplicatePlacementSequence]() {
        return mpc::sequencer::duplicatePatternPlacement(
                duplicatePlacementSequence, 0, "clip-source", "clip-copy", 960);
    };
    assert(duplicatePlacementHistory.record({
            "Duplicate Arrangement Pattern placement",
            undoPlacementDuplicate, redoPlacementDuplicate}));
    assert(duplicatePlacementHistory.undo());
    assert(duplicatePlacementSequence.tracks[0].placements.size() == 1);
    assert(duplicatePlacementSequence.tracks[0].placements[0].id == "clip-source");
    assert(notesEqual(duplicatePlacementSequence.tracks[0].patterns[0].notes, duplicateBaselineNotes));
    assert(duplicatePlacementHistory.redo());
    assert(duplicatePlacementSequence.tracks[0].placements.size() == 2);
    assert(findPlacement("clip-copy") != nullptr);
    assert(notesEqual(duplicatePlacementSequence.tracks[0].patterns[0].notes, duplicateBaselineNotes));
    assert(duplicatePlacementHistory.undo());
    assert(duplicatePlacementSequence.tracks[0].placements.size() == 1);
    assert(duplicatePlacementSequence.tracks[0].placements[0].id == "clip-source");
    assert(notesEqual(duplicatePlacementSequence.tracks[0].patterns[0].notes, duplicateBaselineNotes));

    // Invalid Pattern references, duplicate ids, overlaps and out-of-range clips fail
    // without changing the placement list.
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-04", "missing-pattern", 2880, 960));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-01", "pattern-placement-test", 2880, 960));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-overlap", "pattern-placement-test", 480, 240));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-negative", "pattern-placement-test", -240, 240));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-outside", "pattern-placement-test", 3360, 960));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-too-long", "pattern-placement-test", 2880, 1200));
    assert(!mpc::sequencer::addPatternPlacement(
            placementSequence, 0, "clip-bad-offset", "pattern-placement-test", 2880, 240, 1800));
    assert(placementSequence.tracks[0].placements.size() == 3);

    // Move and resize operations reject collisions atomically and preserve identity.
    assert(!mpc::sequencer::movePatternPlacement(
            placementSequence, 0, "clip-02", 0));
    assert(!mpc::sequencer::movePatternPlacement(
            placementSequence, 0, "clip-02", 1440));
    assert(placementSequence.tracks[0].placements[1].startTick == 960);
    assert(!mpc::sequencer::resizePatternPlacement(
            placementSequence, 0, "clip-02", 1920));
    assert(placementSequence.tracks[0].placements[1].lengthTicks == 960);

    // Editing a placement never duplicates/removes the underlying note data.
    assert(mpc::sequencer::erasePatternPlacement(placementSequence, 0, "clip-03"));
    assert(placementSequence.tracks[0].patterns.size() == 1);
    assert(placementSequence.tracks[0].patterns[0].notes.size() == 1);
    assert(mpc::sequencer::movePatternPlacement(
            placementSequence, 0, "clip-02", 1920));
    assert(placementSequence.tracks[0].placements[1].id == "clip-02");
    assert(placementSequence.tracks[0].placements[1].patternId
            == "pattern-placement-test");
    assert(placementSequence.tracks[0].placements[1].startTick == 1920);

    // The existing history mechanism restores exact placement state on Undo/Redo.
    mpc::MpcEditHistory placementHistory;
    assert(placementHistory.record({
            "Move Pattern placement",
            [&placementSequence]() {
                return mpc::sequencer::movePatternPlacement(
                        placementSequence, 0, "clip-02", 960);
            },
            [&placementSequence]() {
                return mpc::sequencer::movePatternPlacement(
                        placementSequence, 0, "clip-02", 1920);
            }}));
    assert(placementHistory.undo());
    assert(placementSequence.tracks[0].placements[1].startTick == 960);
    assert(placementHistory.redo());
    assert(placementSequence.tracks[0].placements[1].startTick == 1920);

    assert(mpc::sequencer::resizePatternPlacement(
            placementSequence, 0, "clip-02", 1920));
    assert(placementSequence.tracks[0].placements[1].lengthTicks == 1920);
    assert(!mpc::sequencer::movePatternPlacement(
            placementSequence, 0, "clip-02", 2880));
    assert(placementSequence.tracks[0].placements[1].startTick == 1920);
    assert(placementSequence.tracks[0].patterns[0].notes[0].tick == 0);
    assert(placementSequence.tracks[0].patterns[0].notes[0].durationTicks == 120);
    assert(placementSequence.tracks[0].patterns[0].notes[0].velocity == 97);
    assert(placementSequence.tracks[0].patterns[0].notes[0].probability == 88);
    assert(placementSequence.tracks[0].patterns[0].notes[0].ratchet == 3);
    assert(placementSequence.tracks[0].patterns[0].notes[0].nudgeTicks == -11);

    // Split an existing timeline placement without changing its Pattern data.
    mpc::domain::Sequence splitSequence;
    splitSequence.id = "split-placement-sequence";
    splitSequence.lengthTicks = 3840;
    mpc::domain::Track splitTrack;
    splitTrack.id = "split-placement-track";
    mpc::domain::Pattern splitPattern;
    splitPattern.id = "split-placement-pattern";
    splitPattern.lengthTicks = 3840;
    splitPattern.notes = {{240, 120, 36, 97, 88, 3, -11}};
    splitTrack.patterns.push_back(splitPattern);
    splitSequence.tracks.push_back(splitTrack);
    assert(mpc::sequencer::addPatternPlacement(
            splitSequence, 0, "clip-left", "split-placement-pattern", 0, 3840));
    assert(!mpc::sequencer::splitPatternPlacement(
            splitSequence, 0, "clip-left", 0, "clip-right"));
    assert(!mpc::sequencer::splitPatternPlacement(
            splitSequence, 0, "clip-left", 3840, "clip-right"));
    assert(!mpc::sequencer::splitPatternPlacement(
            splitSequence, 0, "missing-clip", 1920, "clip-right"));
    assert(mpc::sequencer::splitPatternPlacement(
            splitSequence, 0, "clip-left", 1920, "clip-right"));
    assert(splitSequence.tracks[0].placements.size() == 2);
    assert(splitSequence.tracks[0].placements[0].id == "clip-left");
    assert(splitSequence.tracks[0].placements[0].startTick == 0);
    assert(splitSequence.tracks[0].placements[0].lengthTicks == 1920);
    assert(splitSequence.tracks[0].placements[0].sourceOffsetTicks == 0);
    assert(splitSequence.tracks[0].placements[1].id == "clip-right");
    assert(splitSequence.tracks[0].placements[1].startTick == 1920);
    assert(splitSequence.tracks[0].placements[1].lengthTicks == 1920);
    assert(splitSequence.tracks[0].placements[1].sourceOffsetTicks == 1920);
    assert(splitSequence.tracks[0].patterns.size() == 1);
    assert(splitSequence.tracks[0].patterns[0].notes.size() == 1);
    assert(splitSequence.tracks[0].patterns[0].notes[0].tick == 240);
    assert(splitSequence.tracks[0].patterns[0].notes[0].velocity == 97);
    const auto splitUndo = [&splitSequence]() {
        if (!mpc::sequencer::erasePatternPlacement(
                splitSequence, 0, "clip-right")) return false;
        return mpc::sequencer::resizePatternPlacement(
                splitSequence, 0, "clip-left", 3840);
    };
    const auto splitRedo = [&splitSequence]() {
        return mpc::sequencer::splitPatternPlacement(
                splitSequence, 0, "clip-left", 1920, "clip-right");
    };
    mpc::MpcEditHistory splitHistory;
    assert(splitHistory.record({"Split Arrangement Pattern placement", splitUndo, splitRedo}));
    assert(splitHistory.undo());
    assert(splitSequence.tracks[0].placements.size() == 1);
    assert(splitSequence.tracks[0].placements[0].id == "clip-left");
    assert(splitSequence.tracks[0].placements[0].startTick == 0);
    assert(splitSequence.tracks[0].placements[0].lengthTicks == 3840);
    assert(splitHistory.redo());
    assert(splitSequence.tracks[0].placements.size() == 2);
    assert(splitSequence.tracks[0].placements[1].sourceOffsetTicks == 1920);
    assert(splitHistory.undo());
    assert(splitSequence.tracks[0].placements.size() == 1);
    assert(splitSequence.tracks[0].placements[0].lengthTicks == 3840);
    assert(splitSequence.tracks[0].patterns[0].notes.size() == 1);

    // Playback projection follows the placement model without mutating shared Pattern notes.
    splitSequence.tracks[0].patterns[0].notes.push_back({2400, 180, 38, 71, 91, 2, 9});
    assert(mpc::sequencer::splitPatternPlacement(
            splitSequence, 0, "clip-left", 1920, "clip-right"));
    mpc::domain::Pattern renderedArrangement;
    assert(mpc::sequencer::buildArrangementPlaybackPattern(
            splitSequence, splitSequence.tracks[0], renderedArrangement));
    assert(renderedArrangement.lengthTicks == splitSequence.lengthTicks);
    assert(renderedArrangement.notes.size() == 2);
    assert(renderedArrangement.notes[0].tick == 240);
    assert(renderedArrangement.notes[0].durationTicks == 120);
    assert(renderedArrangement.notes[0].note == 36);
    assert(renderedArrangement.notes[0].velocity == 97);
    assert(renderedArrangement.notes[0].probability == 88);
    assert(renderedArrangement.notes[0].ratchet == 3);
    assert(renderedArrangement.notes[0].nudgeTicks == -11);
    assert(renderedArrangement.notes[1].tick == 2400);
    assert(renderedArrangement.notes[1].durationTicks == 180);
    assert(renderedArrangement.notes[1].note == 38);
    assert(renderedArrangement.notes[1].velocity == 71);
    assert(renderedArrangement.notes[1].probability == 91);
    assert(renderedArrangement.notes[1].ratchet == 2);
    assert(renderedArrangement.notes[1].nudgeTicks == 9);
    splitSequence.tracks[0].placements[1].startTick = 2160;
    splitSequence.tracks[0].placements[1].lengthTicks = 1680;
    assert(mpc::sequencer::buildArrangementPlaybackPattern(
            splitSequence, splitSequence.tracks[0], renderedArrangement));
    assert(renderedArrangement.notes.size() == 2);
    assert(renderedArrangement.notes[1].tick == 2640);
    splitSequence.tracks[0].placements[1].sourceOffsetTicks = 3000;
    assert(!mpc::sequencer::buildArrangementPlaybackPattern(
            splitSequence, splitSequence.tracks[0], renderedArrangement));
    assert(splitSequence.tracks[0].patterns[0].notes[1].tick == 2400);
    assert(splitSequence.tracks[0].patterns[0].notes[1].nudgeTicks == 9);

    mpc::domain::Pattern pattern;
    pattern.lengthTicks = 3840;
    pattern.notes = {
            {110, 120, 42, 100, 127, 1},
            {520, 120, 43, 100, 127, 1},
            {3830, 120, 36, 110, 127, 1}};

    const auto changed = mpc::sequencer::quantizePattern(pattern, 480);

    assert(changed == 3);
    assert(pattern.notes.size() == 3);
    assert(pattern.notes[0].tick == 0);
    assert(pattern.notes[0].note == 42);
    assert(pattern.notes[1].tick == 0);
    assert(pattern.notes[1].note == 36);
    assert(pattern.notes[2].tick == 480);
    assert(pattern.notes[2].note == 43);

    assert(mpc::sequencer::quantizePattern(pattern, 0) == 0);
    assert(mpc::sequencer::quantizePattern(pattern, -120) == 0);

    std::array<mpc::domain::MidiNoteEvent, 4> captured{{
            {-120, 60, 50, 90, 127, 1},
            {481, 120, 51, 100, 127, 2},
            {960, -1, 52, 100, 127, 1},
            {1440, 120, 53, 100, 127, 0}}};

    pattern.notes = {{720, 120, 40, 100, 127, 1}};
    const auto recorded = mpc::sequencer::recordNotes(
            pattern,
            captured,
            mpc::sequencer::PatternRecordMode::Replace);

    assert(recorded == 2);
    assert(pattern.notes.size() == 2);
    assert(pattern.notes[0].tick == 481);
    assert(pattern.notes[0].note == 51);
    assert(pattern.notes[1].tick == 3720);
    assert(pattern.notes[1].note == 50);

    std::array<mpc::domain::MidiNoteEvent, 1> overdub{{
            {240, 120, 54, 110, 127, 1}}};

    const auto overdubbed = mpc::sequencer::recordNotes(
            pattern,
            overdub,
            mpc::sequencer::PatternRecordMode::Overdub);

    assert(overdubbed == 1);
    assert(pattern.notes.size() == 3);
    assert(pattern.notes[0].tick == 240);
    assert(pattern.notes[1].tick == 481);
    assert(pattern.notes[2].tick == 3720);

    mpc::domain::Pattern stepPattern;
    stepPattern.lengthTicks = 1920;
    assert(mpc::sequencer::setStepNote(
                   stepPattern, 0, 120, 36, 110, 120, 127, 1));
    assert(mpc::sequencer::setStepNote(
                   stepPattern, 4, 120, 38, 100, 120, 96, 2));
    assert(stepPattern.notes.size() == 2);
    assert(stepPattern.notes[0].tick == 0);
    assert(stepPattern.notes[0].note == 36);
    assert(stepPattern.notes[1].tick == 480);
    assert(stepPattern.notes[1].probability == 96);
    assert(stepPattern.notes[1].ratchet == 2);

    assert(mpc::sequencer::setStepNote(
                   stepPattern, 4, 120, 38, 90, 240, 64, 3));
    assert(stepPattern.notes.size() == 2);
    assert(stepPattern.notes[1].velocity == 90);
    assert(stepPattern.notes[1].durationTicks == 240);

    const auto* selectedNote = mpc::sequencer::findStepNote(
            stepPattern, 4, 120, 38);
    assert(selectedNote != nullptr);
    assert(selectedNote->probability == 64);
    assert(selectedNote->ratchet == 3);

    assert(mpc::sequencer::setStepNoteVelocity(
            stepPattern, 4, 120, 38, 72));
    assert(mpc::sequencer::setStepNoteProbability(
            stepPattern, 4, 120, 38, 80));
    assert(mpc::sequencer::setStepNoteRatchet(
            stepPattern, 4, 120, 38, 4));

    const auto* editedNote = mpc::sequencer::findStepNote(
            stepPattern, 4, 120, 38);
    assert(editedNote != nullptr);
    assert(editedNote->velocity == 72);
    assert(editedNote->probability == 80);
    assert(editedNote->ratchet == 4);
    assert(editedNote->nudgeTicks == 0);

    assert(mpc::sequencer::setStepNoteNudge(
            stepPattern, 4, 120, 38, 120));
    assert(mpc::sequencer::setStepNoteDuration(
            stepPattern, 4, 120, 38, 30));
    assert(stepPattern.notes[1].durationTicks == 30);
    assert(mpc::sequencer::setStepNoteDuration(
            stepPattern, 4, 120, 38, 480));
    assert(stepPattern.notes[1].durationTicks == 480);
    assert(!mpc::sequencer::setStepNoteDuration(
            stepPattern, 4, 120, 38, 29));
    assert(!mpc::sequencer::setStepNoteDuration(
            stepPattern, 4, 120, 38, 481));
    assert(stepPattern.notes[1].nudgeTicks == 120);
    assert(mpc::sequencer::setStepNoteNudge(
            stepPattern, 4, 120, 38, -120));
    assert(stepPattern.notes[1].nudgeTicks == -120);
    assert(!mpc::sequencer::setStepNoteNudge(
            stepPattern, 4, 120, 38, 961));
    assert(!mpc::sequencer::setStepNoteNudge(
            stepPattern, 4, 120, 38, -961));

    // Re-writing the same step updates musical parameters without erasing
    // its separate microtiming offset.
    assert(mpc::sequencer::setStepNote(
            stepPattern, 4, 120, 38, 90, 240, 64, 3));
    assert(stepPattern.notes[1].nudgeTicks == -120);

    mpc::domain::Pattern ratchetDurationPattern;
    ratchetDurationPattern.lengthTicks = 1920;
    assert(mpc::sequencer::setStepNote(
            ratchetDurationPattern, 0, 120, 36, 100));
    assert(ratchetDurationPattern.notes.front().durationTicks == 0);
    assert(mpc::sequencer::setStepNoteRatchet(
            ratchetDurationPattern, 0, 120, 36, 2));
    assert(ratchetDurationPattern.notes.front().durationTicks == 120);

    assert(!mpc::sequencer::setStepNoteVelocity(
            stepPattern, 7, 120, 38, 100));
    assert(!mpc::sequencer::setStepNoteProbability(
            stepPattern, 4, 120, 38, 128));
    assert(!mpc::sequencer::setStepNoteRatchet(
            stepPattern, 4, 120, 38, 9));

    assert(mpc::sequencer::eraseStepNote(stepPattern, 4, 120, 38));
    assert(stepPattern.notes.size() == 1);
    assert(!mpc::sequencer::eraseStepNote(stepPattern, 4, 120, 38));

    assert(!mpc::sequencer::setStepNote(
            stepPattern, 16, 120, 40, 100));
    assert(!mpc::sequencer::setStepNote(
            stepPattern, -1, 120, 40, 100));
    assert(!mpc::sequencer::setStepNote(
            stepPattern, 1, 0, 40, 100));
    assert(!mpc::sequencer::setStepNote(
            stepPattern, 1, 120, 40, 0));

    // Arrangement moves change only event position and reject collisions.
    mpc::domain::Pattern arrangementPattern;
    arrangementPattern.lengthTicks = 1920;
    arrangementPattern.notes = {
            {240, 120, 36, 100, 73, 2, -12},
            {480, 240, 38, 91, 64, 3, 24},
            {960, 120, 42, 88, 127, 1, 0}};
    assert(mpc::sequencer::movePatternNoteTick(arrangementPattern, 240, 36, 360));
    assert(arrangementPattern.notes.size() == 3);
    assert(arrangementPattern.notes[0].tick == 360);
    assert(arrangementPattern.notes[0].note == 36);
    assert(arrangementPattern.notes[0].durationTicks == 120);
    assert(arrangementPattern.notes[0].velocity == 100);
    assert(arrangementPattern.notes[0].probability == 73);
    assert(arrangementPattern.notes[0].ratchet == 2);
    assert(arrangementPattern.notes[0].nudgeTicks == -12);
    assert(mpc::sequencer::movePatternNoteTick(arrangementPattern, 360, 36, 240));
    assert(arrangementPattern.notes[0].tick == 240);
    arrangementPattern.notes[1].note = 36;
    assert(!mpc::sequencer::movePatternNoteTick(arrangementPattern, 240, 36, 480));
    assert(arrangementPattern.notes[0].tick == 240);
    assert(arrangementPattern.notes[1].tick == 480);
    arrangementPattern.notes[1].note = 38;
    assert(!mpc::sequencer::movePatternNoteTick(arrangementPattern, 240, 36, 240));
    assert(!mpc::sequencer::movePatternNoteTick(arrangementPattern, 999, 36, 720));
    assert(!mpc::sequencer::movePatternNoteTick(arrangementPattern, 240, 36, -120));
    assert(!mpc::sequencer::movePatternNoteTick(arrangementPattern, 240, 36, 1920));

    // Arrangement duplicate is non-destructive, preserves every musical field,
    // rejects same-pitch collisions/boundaries, and can be undone/redone exactly.
    mpc::domain::Pattern duplicatePattern;
    duplicatePattern.lengthTicks = 1920;
    duplicatePattern.notes = {
            {240, 180, 36, 97, 88, 3, -11},
            {480, 120, 36, 80, 127, 1, 0},
            {720, 90, 38, 75, 66, 2, 12}};
    assert(mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, 360));
    assert(duplicatePattern.notes.size() == 4);
    const auto sourceAt = std::find_if(
            duplicatePattern.notes.begin(), duplicatePattern.notes.end(),
            [](const mpc::domain::MidiNoteEvent& event) {
                return event.tick == 240 && event.note == 36;
            });
    const auto duplicateAt = std::find_if(
            duplicatePattern.notes.begin(), duplicatePattern.notes.end(),
            [](const mpc::domain::MidiNoteEvent& event) {
                return event.tick == 360 && event.note == 36;
            });
    assert(sourceAt != duplicatePattern.notes.end());
    assert(duplicateAt != duplicatePattern.notes.end());
    assert(sourceAt->durationTicks == duplicateAt->durationTicks);
    assert(sourceAt->note == duplicateAt->note);
    assert(sourceAt->velocity == duplicateAt->velocity);
    assert(sourceAt->probability == duplicateAt->probability);
    assert(sourceAt->ratchet == duplicateAt->ratchet);
    assert(sourceAt->nudgeTicks == duplicateAt->nudgeTicks);
    assert(sourceAt->velocity == 97 && sourceAt->probability == 88);
    assert(sourceAt->ratchet == 3 && sourceAt->nudgeTicks == -11);
    assert(!mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, 480));
    assert(!mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, 240));
    assert(!mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, -120));
    assert(!mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, 1920));
    assert(!mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 999, 36, 1080));
    assert(mpc::sequencer::erasePatternNoteTick(duplicatePattern, 360, 36));
    assert(duplicatePattern.notes.size() == 3);
    assert(!mpc::sequencer::erasePatternNoteTick(duplicatePattern, 360, 36));
    assert(mpc::sequencer::duplicatePatternNoteTick(duplicatePattern, 240, 36, 360));
    assert(duplicatePattern.notes.size() == 4);
    assert(mpc::sequencer::erasePatternNoteTick(duplicatePattern, 360, 36));
    assert(duplicatePattern.notes.size() == 3);

    // Arrangement clipboard copies a complete event and pastes it non-destructively.
    mpc::domain::Pattern clipboardPattern;
    clipboardPattern.lengthTicks = 1920;
    clipboardPattern.notes = {
            {240, 180, 36, 97, 88, 3, -11},
            {720, 90, 38, 75, 66, 2, 12}};
    mpc::domain::MidiNoteEvent copiedEvent{};
    assert(mpc::sequencer::copyPatternNoteTick(
            clipboardPattern, 240, 36, copiedEvent));
    assert(copiedEvent.tick == 240 && copiedEvent.note == 36);
    assert(!mpc::sequencer::copyPatternNoteTick(
            clipboardPattern, 480, 36, copiedEvent));
    clipboardPattern.notes.push_back({240, 60, 36, 80, 64, 1, 0});
    assert(!mpc::sequencer::copyPatternNoteTick(
            clipboardPattern, 240, 36, copiedEvent));
    clipboardPattern.notes.pop_back();
    assert(copiedEvent.tick == 240 && copiedEvent.velocity == 97
            && copiedEvent.probability == 88 && copiedEvent.ratchet == 3
            && copiedEvent.nudgeTicks == -11);
    assert(mpc::sequencer::pastePatternNoteTick(clipboardPattern, copiedEvent, 360));
    assert(clipboardPattern.notes.size() == 3);
    const auto clipboardPasted = std::find_if(
            clipboardPattern.notes.begin(), clipboardPattern.notes.end(),
            [](const mpc::domain::MidiNoteEvent& event) {
                return event.tick == 360 && event.note == 36;
            });
    assert(clipboardPasted != clipboardPattern.notes.end());
    assert(clipboardPasted->durationTicks == copiedEvent.durationTicks);
    assert(clipboardPasted->note == copiedEvent.note);
    assert(clipboardPasted->velocity == copiedEvent.velocity);
    assert(clipboardPasted->probability == copiedEvent.probability);
    assert(clipboardPasted->ratchet == copiedEvent.ratchet);
    assert(clipboardPasted->nudgeTicks == copiedEvent.nudgeTicks);
    assert(!mpc::sequencer::pastePatternNoteTick(clipboardPattern, copiedEvent, 360));
    assert(!mpc::sequencer::pastePatternNoteTick(clipboardPattern, copiedEvent, -1));
    assert(!mpc::sequencer::pastePatternNoteTick(clipboardPattern, copiedEvent, 1920));
    assert(clipboardPattern.notes.size() == 3);
    assert(mpc::sequencer::erasePatternNoteTick(clipboardPattern, 360, 36));
    assert(clipboardPattern.notes.size() == 2);
    assert(mpc::sequencer::pastePatternNoteTick(clipboardPattern, copiedEvent, 360));
    assert(clipboardPattern.notes.size() == 3);
    assert(mpc::sequencer::erasePatternNoteTick(clipboardPattern, 360, 36));

    // CUT returns the full source event while deleting only its unique Pattern entry.
    mpc::domain::Pattern cutPattern;
    cutPattern.lengthTicks = 1920;
    cutPattern.notes = {
            {240, 180, 36, 97, 88, 3, -11},
            {720, 90, 38, 75, 66, 2, 12}};
    mpc::domain::MidiNoteEvent cutEvent{};
    assert(mpc::sequencer::cutPatternNoteTick(cutPattern, 240, 36, cutEvent));
    assert(cutPattern.notes.size() == 1);
    assert(cutEvent.tick == 240 && cutEvent.durationTicks == 180);
    assert(cutEvent.note == 36 && cutEvent.velocity == 97
            && cutEvent.probability == 88 && cutEvent.ratchet == 3
            && cutEvent.nudgeTicks == -11);
    assert(!mpc::sequencer::cutPatternNoteTick(cutPattern, 240, 36, cutEvent));
    assert(!mpc::sequencer::cutPatternNoteTick(cutPattern, 1920, 36, cutEvent));
    assert(cutPattern.notes.size() == 1);
    assert(mpc::sequencer::pastePatternNoteTick(cutPattern, cutEvent, cutEvent.tick));
    assert(cutPattern.notes.size() == 2);
    const auto cutRestored = std::find_if(
            cutPattern.notes.begin(), cutPattern.notes.end(),
            [](const mpc::domain::MidiNoteEvent& event) {
                return event.tick == 240 && event.note == 36;
            });
    assert(cutRestored != cutPattern.notes.end());
    assert(cutRestored->durationTicks == 180 && cutRestored->velocity == 97
            && cutRestored->probability == 88 && cutRestored->ratchet == 3
            && cutRestored->nudgeTicks == -11);
    cutPattern.notes.push_back({240, 60, 36, 80, 64, 1, 0});
    assert(!mpc::sequencer::cutPatternNoteTick(cutPattern, 240, 36, cutEvent));
    assert(cutPattern.notes.size() == 3);
    cutPattern.notes.pop_back();

    mpc::domain::MidiNoteEvent probabilityNote{0, 120, 36, 100, 64, 4};
    assert(mpc::sequencer::shouldTriggerNote(probabilityNote, 1234u) ==
           mpc::sequencer::shouldTriggerNote(probabilityNote, 1234u));
    assert(mpc::sequencer::shouldTriggerNote(
                   mpc::domain::MidiNoteEvent{0, 120, 36, 100, 0, 1},
                   1234u) == false);
    assert(mpc::sequencer::shouldTriggerNote(
                   mpc::domain::MidiNoteEvent{0, 120, 36, 100, 127, 1},
                   1234u) == true);
    assert(mpc::sequencer::ratchetCount(probabilityNote) == 4);
    assert(mpc::sequencer::ratchetCount(
                   mpc::domain::MidiNoteEvent{0, 120, 36, 100, 127, 0}) == 1);
    assert(mpc::sequencer::ratchetCount(
                   mpc::domain::MidiNoteEvent{0, 120, 36, 100, 127, 12}) == 8);

    pattern.lengthTicks = 3840;
    pattern.notes = {{960, 120, 60, 100, 127, 1}};
    const std::array<mpc::domain::MidiNoteEvent, 1> invalidOnly{{{
            0, -1, 61, 100, 127, 1}}};
    assert(mpc::sequencer::recordNotes(
                   pattern,
                   invalidOnly,
                   mpc::sequencer::PatternRecordMode::Replace) == 0);
    assert(pattern.notes.size() == 1);
    assert(pattern.notes[0].tick == 960);

    mpc::domain::Pattern swingPattern;
    swingPattern.lengthTicks = 3840;
    swingPattern.notes = {
            {0, 120, 36, 100, 127, 1},
            {480, 120, 38, 100, 127, 1},
            {960, 120, 40, 100, 127, 1},
            {1440, 120, 42, 100, 127, 1},
            {720, 120, 44, 100, 127, 1}};

    assert(mpc::sequencer::applySwing(swingPattern, 480, 50) == 2);
    assert(swingPattern.notes[0].tick == 0);
    assert(swingPattern.notes[1].tick == 600);
    assert(swingPattern.notes[2].tick == 720);
    assert(swingPattern.notes[3].tick == 960);
    assert(swingPattern.notes[4].tick == 1560);

    assert(mpc::sequencer::applySwing(swingPattern, 0, 50) == 0);
    assert(mpc::sequencer::applySwing(swingPattern, 480, 0) == 0);
    assert(swingPattern.notes[0].tick == 0);
    assert(swingPattern.notes[1].tick == 600);
    assert(swingPattern.notes[2].tick == 720);
    assert(swingPattern.notes[3].tick == 960);
    assert(swingPattern.notes[4].tick == 1560);

    mpc::domain::Pattern wrapPattern;
    wrapPattern.lengthTicks = 600;
    wrapPattern.notes = {{400, 120, 50, 100, 127, 1}};
    assert(mpc::sequencer::applySwing(wrapPattern, 400, 100) == 1);
    assert(wrapPattern.notes[0].tick == 0);

    pattern.lengthTicks = 0;
    assert(mpc::sequencer::recordNotes(
                   pattern,
                   overdub,
                   mpc::sequencer::PatternRecordMode::Overdub) == 0);

    return 0;
}
