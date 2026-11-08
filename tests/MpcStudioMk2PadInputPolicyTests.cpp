#include "MPC/MpcProjectState.h"
#include "MPC/MpcStudioMk2ControlMap.h"
#include "MPC/MpcStudioMk2InputDecoder.h"
#include "MPC/MpcStudioMk2SemanticAdapter.h"
#include "MPC/Sequencer/MpcSequenceRecorder.h"
#include "Midi/MpcStudioMk2PadInputPolicy.h"

#include <array>
#include <cassert>
#include <cstdint>

namespace {

mpc::studio::InputEvent decodePad(bool pressed, std::uint8_t velocity) {
    const auto note = mpc::studio::pads[0].midiNote;
    const std::array<std::uint8_t, 3> bytes{
            static_cast<std::uint8_t>(
                    (pressed ? 0x90u : 0x80u) | mpc::studio::padChannel),
            note,
            velocity};
    const auto event = mpc::studio::decodeInput(bytes);
    assert(event.has_value());
    assert(event->type == mpc::studio::InputEventType::PadNote);
    assert(event->padIndex == 0);
    assert(event->pressed == pressed);
    return *event;
}

void testHardwarePadVelocityRecordsIntoPattern() {
    mpc::MpcProjectState state;
    auto& track = state.activeSequence().tracks.front();
    track.recordArmed = true;
    state.activeSequence().timingCorrectEnabled = false;

    mpc::studio::MpcStudioMk2SemanticAdapter adapter;
    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.setMode(mpc::sequencer::PatternRecordMode::Overdub);
    recorder.begin(state);

    const auto press = decodePad(true, 115);
    const auto pressRoute = adapter.handlePad(press);
    const auto pressPlan =
            mpc::midi::makePadInputPlan(pressRoute, true, false);
    assert(pressPlan.hasTarget && pressPlan.triggerRequested);
    assert(pressPlan.targetPadIndex == 0 && pressPlan.velocity == 115);
    assert(!pressPlan.tuningMode);

    const auto noteOn = mpc::midi::makePadSequenceRecordEvent(
            pressPlan, true, true, 0, 1200, true);
    assert(noteOn.has_value());
    assert(noteOn->pressed == mpc::sequencer::kSequenceRecordNoteOn);
    assert(noteOn->padIndex == 0 && noteOn->velocity == 115);
    assert(queue.tryEnqueue(*noteOn));

    const auto release = decodePad(false, 0);
    const auto releaseRoute = adapter.handlePad(release);
    const auto releasePlan =
            mpc::midi::makePadInputPlan(releaseRoute, false, false);
    assert(releasePlan.hasTarget && !releasePlan.triggerRequested);
    assert(releasePlan.targetPadIndex == 0 && releasePlan.velocity == 115);
    const auto noteOff = mpc::midi::makePadSequenceRecordEvent(
            releasePlan, false, true, 0, 1480, false);
    assert(noteOff.has_value());
    assert(noteOff->pressed == mpc::sequencer::kSequenceRecordNoteOff);
    assert(noteOff->velocity == 0);
    assert(queue.tryEnqueue(*noteOff));

    assert(recorder.drain(state, queue) == 1);
    const auto& notes = track.patterns.front().notes;
    assert(notes.size() == 1);
    assert(notes.front().tick == 1200);
    assert(notes.front().durationTicks == 280);
    assert(notes.front().note == 36);
    assert(notes.front().velocity == 115);
}

void testMutedPadRemainsRecordableButDoesNotTriggerAudio() {
    mpc::studio::PadRoutingResult routing;
    routing.targetPadIndex = 3;
    routing.velocity = 92;
    const auto plan = mpc::midi::makePadInputPlan(routing, true, true);
    assert(plan.hasTarget && !plan.triggerRequested);
    const auto event = mpc::midi::makePadSequenceRecordEvent(
            plan, true, true, 0, 600, false);
    assert(event.has_value());
    assert(event->pressed == mpc::sequencer::kSequenceRecordNoteOn);
    assert(event->padIndex == 3 && event->velocity == 92);
}

void testSixteenLevelTuneRecordsOnlyAcceptedOneShotHits() {
    mpc::studio::MpcStudioMk2SemanticAdapter adapter;
    // Select the base Pad first, then enter 16 Levels and Tune. A Tune hit
    // targets that remembered Pad even when the physical pad differs.
    const mpc::studio::InputEvent basePress{
            mpc::studio::InputEventType::PadNote, mpc::studio::padChannel,
            mpc::studio::pads[5].midiNote, 100, 5, true};
    const mpc::studio::InputEvent baseRelease{
            mpc::studio::InputEventType::PadNote, mpc::studio::padChannel,
            mpc::studio::pads[5].midiNote, 0, 5, false};
    static_cast<void>(adapter.handlePad(basePress));
    static_cast<void>(adapter.handlePad(baseRelease));
    const mpc::studio::InputEvent level16{
            mpc::studio::InputEventType::Button, mpc::studio::buttonChannel,
            40, 127, 0xFF, true};
    static_cast<void>(adapter.handleControl(level16));
    adapter.setSixteenLevelTuneMode(true);

    const auto press = decodePad(true, 100);
    const auto routing = adapter.handlePad(press);
    assert(routing.tuningMode && routing.targetPadIndex == 5);
    const auto plan = mpc::midi::makePadInputPlan(routing, true, false);
    assert(plan.triggerRequested && plan.tuningMode);

    const auto rejected = mpc::midi::makePadSequenceRecordEvent(
            plan, true, true, 0, 360, false);
    assert(!rejected.has_value());

    const auto hit = mpc::midi::makePadSequenceRecordEvent(
            plan, true, true, 0, 360, true);
    assert(hit.has_value());
    assert(hit->pressed == mpc::sequencer::kSequenceRecordNoteHit);
    assert(hit->padIndex == plan.targetPadIndex);
    assert(hit->velocity == plan.velocity);
    assert(hit->tuningOffsetMilliSemitones
            == plan.tuningOffsetMilliSemitones);

    // The one-shot Tune hit is also committed by the same native Pattern
    // recorder, preserving its signed per-note pitch offset.
    mpc::MpcProjectState state;
    state.activeSequence().timingCorrectEnabled = false;
    state.activeSequence().tracks[0].recordArmed = true;
    mpc::sequencer::SequenceRecordQueue queue;
    mpc::sequencer::MpcSequenceRecorder recorder;
    recorder.begin(state);
    assert(queue.tryEnqueue(*hit));
    assert(recorder.drain(state, queue) == 1);
    const auto& recorded = state.activeSequence().tracks[0].patterns.front().notes;
    assert(recorded.size() == 1);
    assert(recorded.front().tick == 360);
    assert(recorded.front().note == state.activeDrumProgram().pad(5).midiNote);
    assert(recorded.front().velocity == plan.velocity);
    assert(recorded.front().tuningOffsetMilliSemitones
            == plan.tuningOffsetMilliSemitones);

    const auto release = adapter.handlePad(decodePad(false, 0));
    const auto releasePlan = mpc::midi::makePadInputPlan(release, false, false);
    assert(releasePlan.tuningMode);
    assert(!mpc::midi::makePadSequenceRecordEvent(
            releasePlan, false, true, 0, 480, false).has_value());
}

void testUnarmedOrStoppedTransportDoesNotRecord() {
    mpc::studio::PadRoutingResult routing;
    routing.targetPadIndex = 2;
    routing.velocity = 80;
    const auto plan = mpc::midi::makePadInputPlan(routing, true, false);
    assert(plan.triggerRequested);
    assert(!mpc::midi::makePadSequenceRecordEvent(
            plan, true, false, 0, 240, true).has_value());
    assert(!mpc::midi::makePadSequenceRecordEvent(
            plan, true, true, 0, -1, true).has_value());
}

void testInvalidOrConsumedTargetsFailClosed() {
    mpc::studio::PadRoutingResult invalid;
    invalid.targetPadIndex = 0xFF;
    invalid.velocity = 100;
    const auto noTarget = mpc::midi::makePadInputPlan(invalid, true, false);
    assert(!noTarget.hasTarget && !noTarget.triggerRequested);

    mpc::studio::PadRoutingResult consumed;
    consumed.targetPadIndex = 4;
    consumed.velocity = 100;
    consumed.consumed = true;
    const auto noTrigger = mpc::midi::makePadInputPlan(consumed, true, false);
    assert(!noTrigger.hasTarget && !noTrigger.triggerRequested);
}

} // namespace

int main() {
    testHardwarePadVelocityRecordsIntoPattern();
    testMutedPadRemainsRecordableButDoesNotTriggerAudio();
    testSixteenLevelTuneRecordsOnlyAcceptedOneShotHits();
    testUnarmedOrStoppedTransportDoesNotRecord();
    testInvalidOrConsumedTargetsFailClosed();
    return 0;
}
