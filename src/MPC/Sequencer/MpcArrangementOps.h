#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <limits>
#include <string>
#include <utility>

namespace mpc::sequencer {

inline const domain::Pattern* findPlacementPattern(
        const domain::Track& track, const std::string& patternId) noexcept {
    const domain::Pattern* found = nullptr;
    for (const auto& pattern : track.patterns) {
        if (pattern.id != patternId) continue;
        if (found != nullptr) return nullptr;
        found = &pattern;
    }
    return found;
}

inline std::size_t patternPlacementMatches(
        const domain::Track& track, const std::string& placementId) noexcept {
    return static_cast<std::size_t>(std::count_if(
            track.placements.begin(), track.placements.end(),
            [&placementId](const domain::PatternPlacement& placement) {
                return placement.id == placementId;
            }));
}

inline domain::PatternPlacement* findUniquePatternPlacement(
        domain::Track& track, const std::string& placementId) noexcept {
    if (placementId.empty() || patternPlacementMatches(track, placementId) != 1) {
        return nullptr;
    }
    const auto found = std::find_if(
            track.placements.begin(), track.placements.end(),
            [&placementId](const domain::PatternPlacement& placement) {
                return placement.id == placementId;
            });
    return found == track.placements.end() ? nullptr : &*found;
}

inline bool validPatternPlacementRange(
        const domain::Sequence& sequence,
        const domain::Track& track,
        const domain::PatternPlacement& candidate,
        const domain::PatternPlacement* ignored = nullptr) noexcept {
    if (candidate.id.empty() || candidate.patternId.empty()
            || candidate.startTick < 0 || candidate.lengthTicks <= 0
            || candidate.sourceOffsetTicks < 0 || sequence.lengthTicks <= 0) {
        return false;
    }
    const auto* pattern = findPlacementPattern(track, candidate.patternId);
    if (pattern == nullptr || pattern->lengthTicks <= 0
            || static_cast<std::int64_t>(candidate.sourceOffsetTicks)
                    + candidate.lengthTicks > pattern->lengthTicks) {
        return false;
    }
    const auto candidateEnd = static_cast<std::int64_t>(candidate.startTick)
            + candidate.lengthTicks;
    if (candidateEnd > sequence.lengthTicks) return false;

    for (const auto& existing : track.placements) {
        if (&existing == ignored) continue;
        const auto existingEnd = static_cast<std::int64_t>(existing.startTick)
                + existing.lengthTicks;
        if (candidate.startTick < existingEnd && existing.startTick < candidateEnd) {
            return false;
        }
    }
    return true;
}

inline void sortPatternPlacements(domain::Track& track) {
    std::stable_sort(
            track.placements.begin(), track.placements.end(),
            [](const domain::PatternPlacement& lhs,
               const domain::PatternPlacement& rhs) {
                if (lhs.startTick != rhs.startTick) return lhs.startTick < rhs.startTick;
                return lhs.id < rhs.id;
            });
}

// A placement refers to an existing Pattern; adding an Arrangement clip does not
// duplicate or mutate the note data stored by that Pattern.
inline bool addPatternPlacement(
        domain::Sequence& sequence,
        std::size_t trackIndex,
        std::string placementId,
        std::string patternId,
        std::int64_t startTick,
        std::int32_t lengthTicks,
        std::int64_t sourceOffsetTicks = 0) {
    if (trackIndex >= sequence.tracks.size()
            || startTick < 0
            || startTick > std::numeric_limits<std::int32_t>::max()
            || sourceOffsetTicks < 0
            || sourceOffsetTicks > std::numeric_limits<std::int32_t>::max()) {
        return false;
    }
    auto& track = sequence.tracks[trackIndex];
    if (patternPlacementMatches(track, placementId) != 0) return false;
    domain::PatternPlacement candidate;
    candidate.id = std::move(placementId);
    candidate.patternId = std::move(patternId);
    candidate.startTick = static_cast<std::int32_t>(startTick);
    candidate.lengthTicks = lengthTicks;
    candidate.sourceOffsetTicks = static_cast<std::int32_t>(sourceOffsetTicks);
    if (!validPatternPlacementRange(sequence, track, candidate)) return false;

    track.placements.push_back(std::move(candidate));
    sortPatternPlacements(track);
    return true;
}

// Move one unique placement without changing its source Pattern, duration or offset.
inline bool movePatternPlacement(
        domain::Sequence& sequence,
        std::size_t trackIndex,
        const std::string& placementId,
        std::int64_t targetStartTick) {
    if (trackIndex >= sequence.tracks.size()
            || targetStartTick < 0 || targetStartTick > std::numeric_limits<std::int32_t>::max()) {
        return false;
    }
    auto& track = sequence.tracks[trackIndex];
    auto* placement = findUniquePatternPlacement(track, placementId);
    if (placement == nullptr) return false;

    auto candidate = *placement;
    candidate.startTick = static_cast<std::int32_t>(targetStartTick);
    if (!validPatternPlacementRange(sequence, track, candidate, placement)) return false;

    placement->startTick = candidate.startTick;
    sortPatternPlacements(track);
    return true;
}

// Resize a placement window into its source Pattern. This first model intentionally
// disallows stretching/repeating past the referenced Pattern's source range.
inline bool resizePatternPlacement(
        domain::Sequence& sequence,
        std::size_t trackIndex,
        const std::string& placementId,
        std::int32_t newLengthTicks) {
    if (trackIndex >= sequence.tracks.size()) return false;
    auto& track = sequence.tracks[trackIndex];
    auto* placement = findUniquePatternPlacement(track, placementId);
    if (placement == nullptr) return false;

    auto candidate = *placement;
    candidate.lengthTicks = newLengthTicks;
    if (!validPatternPlacementRange(sequence, track, candidate, placement)) return false;

    placement->lengthTicks = newLengthTicks;
    sortPatternPlacements(track);
    return true;
}

// Flatten the visible Pattern placements into one sequence-length playback Pattern.
// This keeps the existing sequencer scheduler intact while making its event timeline
// honor each clip's destination, source offset and visible range.
inline bool buildArrangementPlaybackPattern(
        const domain::Sequence& sequence,
        const domain::Track& track,
        domain::Pattern& output) {
    if (track.patterns.empty() || sequence.lengthTicks <= 0) return false;

    domain::Pattern arranged;
    arranged.id = track.id + "-arrangement-playback";
    arranged.name = track.name + " Arrangement";
    arranged.lengthTicks = sequence.lengthTicks;

    if (track.placements.empty()) {
        // Legacy projects with no explicit placements retain their original Pattern
        // playback length and event coordinates.
        output = track.patterns.front();
        return true;
    }

    for (const auto& placement : track.placements) {
        if (!validPatternPlacementRange(
                sequence, track, placement, &placement)) {
            return false;
        }
        const auto* source = findPlacementPattern(track, placement.patternId);
        if (source == nullptr) return false;

        const auto sourceStart = static_cast<std::int64_t>(placement.sourceOffsetTicks);
        const auto sourceEnd = sourceStart + placement.lengthTicks;
        for (const auto& sourceEvent : source->notes) {
            if (sourceEvent.tick < sourceStart || sourceEvent.tick >= sourceEnd) {
                continue;
            }
            const auto destinationTick = static_cast<std::int64_t>(placement.startTick)
                    + sourceEvent.tick - sourceStart;
            if (destinationTick < 0 || destinationTick >= sequence.lengthTicks) {
                return false;
            }
            const auto visibleDuration = std::min<std::int64_t>(
                    std::max<std::int32_t>(1, sourceEvent.durationTicks),
                    sourceEnd - sourceEvent.tick);
            if (visibleDuration <= 0) continue;

            auto mappedEvent = sourceEvent;
            mappedEvent.tick = static_cast<std::int32_t>(destinationTick);
            mappedEvent.durationTicks = static_cast<std::int32_t>(visibleDuration);
            arranged.notes.push_back(mappedEvent);
        }
    }

    std::stable_sort(
            arranged.notes.begin(), arranged.notes.end(),
            [](const domain::MidiNoteEvent& lhs,
               const domain::MidiNoteEvent& rhs) {
                if (lhs.tick != rhs.tick) return lhs.tick < rhs.tick;
                return lhs.note < rhs.note;
            });
    output = std::move(arranged);
    return true;
}

// Split one placement into two adjacent views of the same source Pattern.
// Note data remains shared in the Pattern; only timeline placement ranges change.
inline bool splitPatternPlacement(
        domain::Sequence& sequence,
        std::size_t trackIndex,
        const std::string& placementId,
        std::int64_t splitTick,
        const std::string& rightPlacementId) {
    if (trackIndex >= sequence.tracks.size()
            || splitTick < 0
            || splitTick > std::numeric_limits<std::int32_t>::max()
            || rightPlacementId.empty()) {
        return false;
    }
    auto& track = sequence.tracks[trackIndex];
    auto* placement = findUniquePatternPlacement(track, placementId);
    if (placement == nullptr || placementId == rightPlacementId
            || patternPlacementMatches(track, rightPlacementId) != 0) {
        return false;
    }

    const auto original = *placement;
    const auto originalEnd = static_cast<std::int64_t>(original.startTick)
            + original.lengthTicks;
    if (splitTick <= original.startTick || splitTick >= originalEnd) {
        return false;
    }

    const auto leftLength = splitTick - original.startTick;
    const auto rightLength = originalEnd - splitTick;
    const auto rightSourceOffset =
            static_cast<std::int64_t>(original.sourceOffsetTicks) + leftLength;
    if (leftLength <= 0 || leftLength > std::numeric_limits<std::int32_t>::max()
            || rightLength <= 0 || rightLength > std::numeric_limits<std::int32_t>::max()
            || rightSourceOffset < 0
            || rightSourceOffset > std::numeric_limits<std::int32_t>::max()) {
        return false;
    }

    auto left = original;
    left.lengthTicks = static_cast<std::int32_t>(leftLength);
    auto right = original;
    right.id = rightPlacementId;
    right.startTick = static_cast<std::int32_t>(splitTick);
    right.lengthTicks = static_cast<std::int32_t>(rightLength);
    right.sourceOffsetTicks = static_cast<std::int32_t>(rightSourceOffset);
    if (!validPatternPlacementRange(sequence, track, left, placement)
            || !validPatternPlacementRange(sequence, track, right, placement)) {
        return false;
    }

    *placement = std::move(left);
    track.placements.push_back(std::move(right));
    sortPatternPlacements(track);
    return true;
}

// Removing a placement leaves its referenced Pattern and notes intact.
inline bool erasePatternPlacement(
        domain::Sequence& sequence,
        std::size_t trackIndex,
        const std::string& placementId) {
    if (trackIndex >= sequence.tracks.size()) return false;
    auto& placements = sequence.tracks[trackIndex].placements;
    if (patternPlacementMatches(sequence.tracks[trackIndex], placementId) != 1) {
        return false;
    }
    const auto found = std::find_if(
            placements.begin(), placements.end(),
            [&placementId](const domain::PatternPlacement& placement) {
                return placement.id == placementId;
            });
    if (found == placements.end()) return false;
    placements.erase(found);
    return true;
}

} // namespace mpc::sequencer
