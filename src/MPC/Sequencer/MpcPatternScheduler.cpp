#include "MPC/Sequencer/MpcPatternScheduler.h"

#include <cstdint>
#include <limits>

namespace mpc::sequencer {
namespace {

std::uint32_t mixCycleSeed(
        std::uint32_t seed,
        std::uint64_t cycleIndex) noexcept {
    std::uint32_t state =
            seed ^
            static_cast<std::uint32_t>(cycleIndex) * 0x9E3779B9u ^
            static_cast<std::uint32_t>(cycleIndex >> 32) * 0x85EBCA6Bu;
    state ^= state >> 16;
    state *= 0x7FEB352Du;
    state ^= state >> 15;
    return state;
}

} // namespace

ScheduleResult schedulePatternWindow(
        const domain::Pattern& pattern,
        const TickWindow& window,
        std::uint32_t seed,
        std::span<ScheduledMidiEvent> output) noexcept {
    ScheduleResult result;

    if (pattern.lengthTicks <= 0) {
        return result;
    }

    const auto length = static_cast<std::int64_t>(pattern.lengthTicks);
    if (window.begin < 0 || window.end < 0 ||
        window.begin >= length || window.end >= length) {
        return result;
    }

    if (window.completedCycles == 0 && window.end < window.begin) {
        return result;
    }

    if (window.completedCycles >
        static_cast<std::uint64_t>(std::numeric_limits<std::int64_t>::max()) /
                static_cast<std::uint64_t>(length)) {
        return result;
    }

    const auto emitSegment = [&](std::int64_t segmentBegin,
                                 std::int64_t segmentEnd,
                                 std::int64_t baseOffset,
                                 std::uint64_t cycleIndex) {
        if (segmentBegin >= segmentEnd) {
            return;
        }

        const auto cycleSeed = mixCycleSeed(seed, cycleIndex);

        for (std::size_t noteIndex = 0;
             noteIndex < pattern.notes.size();
             ++noteIndex) {
            const auto& note = pattern.notes[noteIndex];
            if (note.tick < segmentBegin || note.tick >= segmentEnd) {
                continue;
            }

            if (!shouldTriggerNote(note, cycleSeed)) {
                continue;
            }

            ++result.eligible;
            if (result.written >= output.size()) {
                result.truncated = true;
                continue;
            }

            auto& scheduled = output[result.written++];
            scheduled.offsetTicks =
                    baseOffset + static_cast<std::int64_t>(note.tick) -
                    segmentBegin;
            scheduled.patternTick = note.tick;
            scheduled.durationTicks = note.durationTicks;
            scheduled.note = note.note;
            scheduled.velocity = note.velocity;
            scheduled.ratchetCount = static_cast<std::uint8_t>(
                    ratchetCount(note));
            scheduled.sourceNoteIndex = noteIndex;
        }
    };

    std::int64_t baseOffset = 0;

    if (window.completedCycles == 0) {
        emitSegment(window.begin, window.end, 0, 0);
        return result;
    }

    emitSegment(
            window.begin,
            length,
            baseOffset,
            0);
    baseOffset += length - window.begin;

    for (std::uint64_t cycle = 1;
         cycle < window.completedCycles;
         ++cycle) {
        emitSegment(0, length, baseOffset, cycle);
        baseOffset += length;
    }

    emitSegment(
            0,
            window.end,
            baseOffset,
            window.completedCycles);

    return result;
}

ScheduleResult schedulePatternWindowInRange(
        const domain::Pattern& pattern,
        const TickWindow& window,
        std::int64_t loopStartTicks,
        std::int64_t loopEndTicks,
        std::uint32_t seed,
        std::span<ScheduledMidiEvent> output) noexcept {
    ScheduleResult result;

    if (pattern.lengthTicks <= 0
            || loopStartTicks < 0
            || loopEndTicks <= loopStartTicks
            || loopEndTicks > pattern.lengthTicks
            || window.begin < loopStartTicks
            || window.begin >= loopEndTicks
            || window.end < loopStartTicks
            || window.end >= loopEndTicks) {
        return result;
    }

    const auto loopLength = loopEndTicks - loopStartTicks;

    const auto emitSegment = [&](std::int64_t segmentBegin,
                                 std::int64_t segmentEnd,
                                 std::int64_t baseOffset,
                                 std::uint64_t cycleIndex) {
        if (segmentBegin >= segmentEnd) {
            return;
        }

        const auto cycleSeed = mixCycleSeed(seed, cycleIndex);
        for (std::size_t noteIndex = 0;
             noteIndex < pattern.notes.size();
             ++noteIndex) {
            const auto& note = pattern.notes[noteIndex];
            if (note.tick < segmentBegin || note.tick >= segmentEnd) {
                continue;
            }

            if (!shouldTriggerNote(note, cycleSeed)) {
                continue;
            }

            ++result.eligible;
            if (result.written >= output.size()) {
                result.truncated = true;
                continue;
            }

            auto& scheduled = output[result.written++];
            scheduled.offsetTicks =
                    baseOffset + note.tick - segmentBegin;
            scheduled.patternTick = note.tick;
            scheduled.durationTicks = note.durationTicks;
            scheduled.note = note.note;
            scheduled.velocity = note.velocity;
            scheduled.ratchetCount = static_cast<std::uint8_t>(
                    ratchetCount(note));
            scheduled.sourceNoteIndex = noteIndex;
        }
    };

    if (window.completedCycles == 0) {
        if (window.end < window.begin) {
            return result;
        }
        emitSegment(window.begin, window.end, 0, 0);
        return result;
    }

    emitSegment(window.begin, loopEndTicks, 0, 0);

    std::int64_t baseOffset = loopEndTicks - window.begin;
    for (std::uint64_t cycle = 1;
         cycle < window.completedCycles;
         ++cycle) {
        emitSegment(loopStartTicks, loopEndTicks, baseOffset, cycle);
        if (baseOffset > std::numeric_limits<std::int64_t>::max() - loopLength) {
            result.truncated = true;
            return result;
        }
        baseOffset += loopLength;
    }

    emitSegment(loopStartTicks, window.end, baseOffset, window.completedCycles);
    return result;
}

} // namespace mpc::sequencer
