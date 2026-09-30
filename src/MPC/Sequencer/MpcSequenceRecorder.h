#pragma once

#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcPatternOps.h"
#include "MPC/Sequencer/MpcSequenceRecordQueue.h"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <utility>

namespace mpc::sequencer {

class MpcSequenceRecorder final {
public:
    MpcSequenceRecorder() = default;

    MpcSequenceRecorder(const MpcSequenceRecorder&) = delete;
    MpcSequenceRecorder& operator=(const MpcSequenceRecorder&) = delete;

    void setMode(PatternRecordMode mode) noexcept {
        mode_ = mode;
    }

    [[nodiscard]] PatternRecordMode mode() const noexcept {
        return mode_;
    }

    void begin(MpcProjectState& state) {
        held_.fill({});
        startTicks_.fill({});
        replaceCleared_.fill(false);
        active_ = true;

        for (std::size_t index = 0;
             index < state.activeSequence().tracks.size()
                    && index < replaceCleared_.size();
             ++index) {
            auto& track = state.activeSequence().tracks[index];
            if (!track.recordArmed
                    || track.kind != domain::TrackKind::Drum
                    || track.patterns.empty()) {
                continue;
            }

            if (mode_ == PatternRecordMode::Replace) {
                track.patterns.front().notes.clear();
                replaceCleared_[index] = true;
            }
        }
    }

    [[nodiscard]] bool active() const noexcept {
        return active_;
    }

    std::size_t drain(
            MpcProjectState& state,
            SequenceRecordQueue& queue) {
        if (!active_) {
            return 0;
        }

        std::size_t recorded = 0;
        SequenceRecordEvent event{};

        while (queue.tryDequeue(event)) {
            if (event.trackIndex >= replaceCleared_.size()
                    || event.trackIndex >= state.activeSequence().tracks.size()
                    || event.padIndex >= domain::kMaxProgramPads) {
                continue;
            }

            auto& track = state.activeSequence().tracks[event.trackIndex];
            if (!track.recordArmed
                    || track.kind != domain::TrackKind::Drum
                    || track.patterns.empty()) {
                continue;
            }

            const auto trackIndex = static_cast<std::size_t>(event.trackIndex);
            const auto padIndex = static_cast<std::size_t>(event.padIndex);

            if (event.pressed != 0u) {
                if (!held_[trackIndex][padIndex]) {
                    held_[trackIndex][padIndex] = true;
                    startTicks_[trackIndex][padIndex] = event.tick;
                }
                continue;
            }

            if (!held_[trackIndex][padIndex]) {
                continue;
            }

            const auto startTick = startTicks_[trackIndex][padIndex];
            held_[trackIndex][padIndex] = false;

            auto& pattern = track.patterns.front();
            const auto length = std::max<std::int32_t>(1, pattern.lengthTicks);
            const auto loopLength = static_cast<std::int64_t>(length);

            auto normalizedStart = startTick % loopLength;
            if (normalizedStart < 0) {
                normalizedStart += loopLength;
            }

            auto normalizedEnd = event.tick % loopLength;
            if (normalizedEnd < 0) {
                normalizedEnd += loopLength;
            }

            auto duration = normalizedEnd - normalizedStart;
            if (duration <= 0) {
                duration = loopLength;
            }
            duration = std::clamp<std::int64_t>(
                    duration,
                    1,
                    loopLength);

            domain::MidiNoteEvent note;
            note.tick = static_cast<std::int32_t>(normalizedStart);
            note.durationTicks = static_cast<std::int32_t>(duration);
            note.note = state.activeDrumProgram().pad(padIndex).midiNote;
            note.velocity = std::clamp<std::uint8_t>(
                    event.velocity, 1, 127);
            note.probability = 127;
            note.ratchet = 1;

            quantizeAndSwing(
                    note,
                    state.activeSequence().quantizeGridTicks,
                    state.activeSequence().swingPercent);

            if (mode_ == PatternRecordMode::Replace
                    && !replaceCleared_[trackIndex]) {
                pattern.notes.clear();
                replaceCleared_[trackIndex] = true;
            }

            pattern.notes.push_back(note);
            ++recorded;
        }

        sortTouchedPatterns(state);
        return recorded;
    }

    std::size_t finish(
            MpcProjectState& state,
            SequenceRecordQueue& queue,
            std::int64_t currentTick) {
        if (!active_) {
            return 0;
        }

        std::size_t recorded = drain(state, queue);

        for (std::size_t trackIndex = 0;
             trackIndex < state.activeSequence().tracks.size()
                    && trackIndex < held_.size();
             ++trackIndex) {
            auto& track = state.activeSequence().tracks[trackIndex];
            if (!track.recordArmed
                    || track.kind != domain::TrackKind::Drum
                    || track.patterns.empty()) {
                continue;
            }

            const auto loopLength = static_cast<std::int64_t>(
                    std::max<std::int32_t>(
                            1,
                            track.patterns.front().lengthTicks));
            auto normalizedEnd = currentTick % loopLength;
            if (normalizedEnd < 0) {
                normalizedEnd += loopLength;
            }

            for (std::size_t padIndex = 0;
                 padIndex < domain::kMaxProgramPads;
                 ++padIndex) {
                if (!held_[trackIndex][padIndex]) {
                    continue;
                }

                const auto startTick =
                        startTicks_[trackIndex][padIndex];
                held_[trackIndex][padIndex] = false;

                auto normalizedStart = startTick % loopLength;
                if (normalizedStart < 0) {
                    normalizedStart += loopLength;
                }

                auto duration = normalizedEnd - normalizedStart;
                if (duration <= 0) {
                    duration = std::max<std::int64_t>(
                            1,
                            std::min<std::int64_t>(
                                    state.activeSequence().quantizeGridTicks,
                                    loopLength));
                }

                duration = std::clamp<std::int64_t>(
                        duration, 1, loopLength);

                domain::MidiNoteEvent note;
                note.tick = static_cast<std::int32_t>(normalizedStart);
                note.durationTicks = static_cast<std::int32_t>(duration);
                note.note = state.activeDrumProgram().pad(padIndex).midiNote;
                note.velocity = 100;
                note.probability = 127;
                note.ratchet = 1;

                quantizeAndSwing(
                        note,
                        state.activeSequence().quantizeGridTicks,
                        state.activeSequence().swingPercent);

                if (mode_ == PatternRecordMode::Replace
                        && !replaceCleared_[trackIndex]) {
                    track.patterns.front().notes.clear();
                    replaceCleared_[trackIndex] = true;
                }
                track.patterns.front().notes.push_back(note);
                ++recorded;
            }
        }

        sortTouchedPatterns(state);
        active_ = false;
        held_.fill({});
        startTicks_.fill({});
        replaceCleared_.fill(false);
        return recorded;
    }

private:
    static void quantizeAndSwing(
            domain::MidiNoteEvent& note,
            std::int32_t gridTicks,
            std::int32_t swingPercent) noexcept {
        if (gridTicks <= 0) {
            return;
        }

        const auto length =
                std::max<std::int64_t>(1, noteTickPatternLength_);
        const auto grid = static_cast<std::int64_t>(gridTicks);

        auto tick = static_cast<std::int64_t>(note.tick);
        tick = ((std::max<std::int64_t>(0, tick) + grid / 2) / grid) * grid;
        tick %= length;

        if (swingPercent > 0) {
            const auto step = tick / grid;
            if ((step & 1) != 0) {
                const auto maxDelay = grid / 2;
                const auto delay =
                        (maxDelay * std::clamp(swingPercent, 0, 100)) / 100;
                tick = (tick + delay) % length;
            }
        }

        note.tick = static_cast<std::int32_t>(tick);
    }

    static void quantizeAndSwingForPattern(
            domain::MidiNoteEvent& note,
            std::int32_t patternLength,
            std::int32_t gridTicks,
            std::int32_t swingPercent) noexcept {
        noteTickPatternLength_ = std::max<std::int64_t>(1, patternLength);
        quantizeAndSwing(note, gridTicks, swingPercent);
        noteTickPatternLength_ = 1;
    }

    void sortTouchedPatterns(MpcProjectState& state) noexcept {
        for (auto& track : state.activeSequence().tracks) {
            if (track.kind != domain::TrackKind::Drum
                    || track.patterns.empty()) {
                continue;
            }
            auto& notes = track.patterns.front().notes;
            std::stable_sort(
                    notes.begin(),
                    notes.end(),
                    [](const domain::MidiNoteEvent& lhs,
                       const domain::MidiNoteEvent& rhs) {
                        return lhs.tick < rhs.tick;
                    });
        }
    }

    std::array<std::array<bool, domain::kMaxProgramPads>,
               domain::kMaxSequenceTracks> held_{};
    std::array<std::array<std::int64_t, domain::kMaxProgramPads>,
               domain::kMaxSequenceTracks> startTicks_{};
    std::array<bool, domain::kMaxSequenceTracks> replaceCleared_{};
    PatternRecordMode mode_ = PatternRecordMode::Overdub;
    bool active_ = false;
    inline static std::int64_t noteTickPatternLength_ = 1;
};

} // namespace mpc::sequencer
