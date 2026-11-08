#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcStudioMk2SemanticAdapter.h"
#include "MPC/Sequencer/MpcSequenceRecordQueue.h"

#include <array>
#include <atomic>
#include <cstdint>
#include <optional>

namespace mpc::midi {

// One immutable decision shared by realtime pad triggering and Pattern recording.
// The semantic adapter has already snapshotted press/release context; this policy
// adds the live mute/transport/Record state without consulting Android UI code.
struct PadInputPlan final {
    bool hasTarget = false;
    bool triggerRequested = false;
    bool tuningMode = false;
    std::uint8_t targetPadIndex = 0xFF;
    std::uint8_t velocity = 0;
    std::int32_t tuningOffsetMilliSemitones = 0;
};

[[nodiscard]] inline PadInputPlan makePadInputPlan(
        const studio::PadRoutingResult& routing,
        bool pressed,
        bool padMuted) noexcept {
    PadInputPlan plan;
    if (routing.consumed
            || routing.targetPadIndex >= domain::kMaxProgramPads
            || routing.velocity == 0
            || routing.velocity > 127) {
        return plan;
    }

    plan.hasTarget = true;
    plan.targetPadIndex = routing.targetPadIndex;
    plan.velocity = routing.velocity;
    plan.tuningMode = routing.tuningMode;
    plan.tuningOffsetMilliSemitones = routing.tuningOffsetMilliSemitones;
    plan.triggerRequested = pressed && !padMuted;
    return plan;
}

[[nodiscard]] inline std::optional<sequencer::SequenceRecordEvent>
makePadSequenceRecordEvent(
        const PadInputPlan& plan,
        bool pressed,
        bool recordableDrumTrack,
        std::uint8_t trackIndex,
        std::int64_t tick,
        bool triggerAccepted) noexcept {
    if (!plan.hasTarget || !recordableDrumTrack || tick < 0
            || trackIndex >= domain::kMaxSequenceTracks) {
        return std::nullopt;
    }

    if (plan.tuningMode) {
        // 16 Levels Tune is a one-shot pitch-shifted hit. A release never
        // closes a held source note, and a muted/rejected hit is not recorded.
        if (!pressed || !plan.triggerRequested || !triggerAccepted) {
            return std::nullopt;
        }
        return sequencer::SequenceRecordEvent{
                tick,
                trackIndex,
                plan.targetPadIndex,
                plan.velocity,
                sequencer::kSequenceRecordNoteHit,
                plan.tuningOffsetMilliSemitones};
    }

    // Normal Pad note-off is recorded even when the pad was muted at hit time,
    // matching the existing transport recording contract.
    return sequencer::SequenceRecordEvent{
            tick,
            trackIndex,
            plan.targetPadIndex,
            pressed ? plan.velocity : std::uint8_t{0},
            pressed ? sequencer::kSequenceRecordNoteOn
                    : sequencer::kSequenceRecordNoteOff,
            0};
}


struct PadRecordingTargetTracker final {
    // MIDI input and UI/JNI transport changes run on different threads.
    // Each physical Pad therefore owns one atomic packed state: bit 8 means
    // active; bits 0-7 contain the captured Track index.
    static constexpr std::uint16_t kActiveBit = 0x100u;

    void capture(
            std::uint8_t physicalPadIndex,
            bool recordableDrumTrack,
            std::uint8_t trackIndex) noexcept {
        if (physicalPadIndex >= held_.size()) return;
        const bool valid = recordableDrumTrack
                && trackIndex < domain::kMaxSequenceTracks;
        const auto packed = valid
                ? static_cast<std::uint16_t>(
                        kActiveBit | static_cast<std::uint16_t>(trackIndex))
                : std::uint16_t{0};
        held_[physicalPadIndex].store(packed, std::memory_order_release);
    }

    [[nodiscard]] std::optional<std::uint8_t> take(
            std::uint8_t physicalPadIndex) noexcept {
        if (physicalPadIndex >= held_.size()) return std::nullopt;
        const auto packed =
                held_[physicalPadIndex].exchange(0u, std::memory_order_acq_rel);
        if ((packed & kActiveBit) == 0u) return std::nullopt;
        return static_cast<std::uint8_t>(packed & 0xFFu);
    }

    void clear() noexcept {
        for (auto& slot : held_) {
            slot.store(0u, std::memory_order_release);
        }
    }

private:
    std::array<std::atomic<std::uint16_t>, domain::kMaxProgramPads> held_{};
};

} // namespace mpc::midi
