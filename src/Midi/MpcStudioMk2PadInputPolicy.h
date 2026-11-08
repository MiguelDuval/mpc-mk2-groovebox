#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcStudioMk2SemanticAdapter.h"
#include "MPC/Sequencer/MpcSequenceRecordQueue.h"

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

} // namespace mpc::midi
