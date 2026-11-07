#include "MpcStudioMk2SemanticAdapter.h"
#include "MpcStudioMk2ControlMap.h"
#include "MPC/Sequencer/MpcNoteRepeatTiming.h"
#include "MPC/Sequencer/MpcLocatePolicy.h"
#include "MPC/Sequencer/MpcTouchStripPolicy.h"
#include <array>
#include <string_view>

namespace {
using Action=mpc::studio::SemanticAction;
using Type=mpc::studio::SemanticActionType;
const mpc::studio::ButtonDefinition* findButton(std::uint8_t note) noexcept {
    for (const auto& b: mpc::studio::buttons) if (b.midiNote==note) return &b;
    return nullptr;
}
Action make(Type t,std::int32_t a=0,std::int32_t b=0,std::int32_t c=0) noexcept { return {t,a,b,c}; }
}
namespace mpc::studio {
void MpcStudioMk2SemanticAdapter::setSixteenLevelTuneMode(bool enabled) noexcept {
    sixteenLevelTuneMode_.store(enabled, std::memory_order_release);
}

std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleControl(const InputEvent& e) noexcept {
    switch(e.type){
        case InputEventType::Button: return handleButton(e.number,e.pressed,e.timestampNanos);
        case InputEventType::JogWheel: return handleJog(e.value);
        case InputEventType::JogPress: return handleJogPress(e.pressed);
        case InputEventType::PadAftertouch:
            if (e.padIndex >= 16 || e.value > 127) {
                return std::nullopt;
            }
            return make(
                    Type::PadAftertouch,
                    static_cast<std::int32_t>(e.padIndex),
                    static_cast<std::int32_t>(e.value));
        case InputEventType::TouchStripTouch:
            touchStripTouched_ = e.pressed;
            return make(Type::TouchStripTouchState, e.pressed ? 1 : 0, static_cast<std::int32_t>(touchStripMode_));
        case InputEventType::TouchStrip:
            if (!noteRepeatActive()) {
                return make(Type::TouchStripValue,e.value);
            }
            {
                const auto index =
                        mpc::sequencer::note_repeat_timing::repeatRateIndexForTouch(
                                e.value);
                if (index == static_cast<std::int32_t>(noteRepeatRateIndex_)) {
                    return std::nullopt;
                }
                noteRepeatRateIndex_ = static_cast<std::uint8_t>(index);
                return make(
                        Type::NoteRepeatRateChanged,
                        index,
                        mpc::sequencer::note_repeat_timing::repeatRateTicksForIndex(
                                index));
            }
        default: return std::nullopt;
    }
}
std::uint8_t MpcStudioMk2SemanticAdapter::padTargetForPhysicalPad(
        std::uint8_t physicalPadIndex) const noexcept {
    if (physicalPadIndex >= padPresses_.size()) {
        return 0xFF;
    }

    const auto& snapshot = padPresses_[physicalPadIndex];
    return snapshot.hasTarget
            ? snapshot.targetPadIndex
            : physicalPadIndex;
}

std::optional<PadRoutingResult>
MpcStudioMk2SemanticAdapter::heldRepeatPad() const noexcept {
    const PadPressSnapshot* latest = nullptr;
    for (const auto& snapshot : padPresses_) {
        if (snapshot.active
                && snapshot.repeating
                && (latest == nullptr || snapshot.order > latest->order)) {
            latest = &snapshot;
        }
    }

    if (latest == nullptr) {
        return std::nullopt;
    }

    PadRoutingResult result;
    result.repeating = true;
    result.velocity = latest->velocity;
    result.targetPadIndex = latest->targetPadIndex;
    return result;
}

PadRoutingResult MpcStudioMk2SemanticAdapter::handlePad(const InputEvent& e) noexcept {
    PadRoutingResult r;
    if (e.type != InputEventType::PadNote) {
        r.velocity = e.value;
        r.targetPadIndex = e.padIndex;
        return r;
    }

    if (e.padIndex >= padPresses_.size()
            || e.value > 127
            || (e.pressed && e.value == 0)) {
        r.consumed = true;
        if (e.pressed && e.padIndex < padPresses_.size()) {
            auto& snapshot = padPresses_[e.padIndex];
            snapshot.active = true;
            snapshot.consumed = true;
            snapshot.repeating = false;
            snapshot.hasTarget = false;
            snapshot.targetPadIndex = 0xFF;
            snapshot.velocity = 0;
            snapshot.tuningMode = false;
            snapshot.tuningOffsetMilliSemitones = 0;
            snapshot.order = ++padPressOrder_;
        }
        return r;
    }

    r.velocity = e.value;
    r.targetPadIndex = e.padIndex;

    if (!e.pressed) {
        auto& snapshot = padPresses_[e.padIndex];
        if (!snapshot.active) {
            // An orphan Note Off must never be reinterpreted using the current
            // 16 Levels/Repeat/Copy context or close a different held note.
            r.consumed = true;
            return r;
        }

        r.consumed = snapshot.consumed;
        r.repeating = snapshot.repeating;
        r.velocity = snapshot.velocity;
        r.targetPadIndex = snapshot.targetPadIndex;
        r.tuningMode = snapshot.tuningMode;
        r.tuningOffsetMilliSemitones = snapshot.tuningOffsetMilliSemitones;
        snapshot.active = false;
        return r;
    }

    const auto rememberPress = [this, &e](PadRoutingResult result) noexcept {
        auto& snapshot = padPresses_[e.padIndex];
        snapshot.active = true;
        snapshot.consumed = result.consumed;
        snapshot.repeating = result.repeating;
        snapshot.hasTarget = !result.consumed
                && result.targetPadIndex < padPresses_.size();
        snapshot.targetPadIndex = result.targetPadIndex;
        snapshot.velocity = result.velocity;
        snapshot.tuningMode = result.tuningMode;
        snapshot.tuningOffsetMilliSemitones = result.tuningOffsetMilliSemitones;
        snapshot.order = ++padPressOrder_;
        return result;
    };

    if (modeHeld_) {
        r.consumed = true;
        r.action = modePadAction(e.padIndex);
        return rememberPress(r);
    }

    if (locateActive()) {
        r.consumed = true;
        if (const auto jumpSlot =
                    mpc::sequencer::locate::locatorSlotForJumpPad(e.padIndex);
                jumpSlot >= 0) {
            r.action = make(Type::LocatePad, jumpSlot, 0);
            return rememberPress(r);
        }
        if (const auto storeSlot =
                    mpc::sequencer::locate::locatorSlotForStorePad(e.padIndex);
                storeSlot >= 0) {
            r.action = make(Type::LocatePad, storeSlot, 1);
            return rememberPress(r);
        }
        r.action = make(Type::LocatePad, -1, 0);
        return rememberPress(r);
    }

    if (eraseHeld_) {
        r.consumed = true;
        r.action = make(Type::ErasePadTarget, e.padIndex);
        return rememberPress(r);
    }

    if (copyMode_ != CopyMode::None) {
        r.consumed = true;
        if (copyMode_ == CopyMode::Copy) {
            if (copySourcePad_ == 0xFF) {
                copySourcePad_ = e.padIndex;
                r.action = make(
                        Type::CopyPadSelection,
                        0,
                        static_cast<std::int32_t>(e.padIndex),
                        1);
                return rememberPress(r);
            }
            if (e.padIndex == copySourcePad_) {
                r.action = make(
                        Type::CopyPadSelection,
                        0,
                        static_cast<std::int32_t>(e.padIndex),
                        0);
                return rememberPress(r);
            }
            const auto bit = static_cast<std::uint16_t>(1u << e.padIndex);
            copyDestinationMask_ ^= bit;
            r.action = make(
                    Type::CopyPadSelection,
                    0,
                    static_cast<std::int32_t>(e.padIndex),
                    (copyDestinationMask_ & bit) != 0u ? 1 : 0);
            return rememberPress(r);
        }

        const auto bit = static_cast<std::uint16_t>(1u << e.padIndex);
        deletePadMask_ ^= bit;
        r.action = make(
                Type::CopyPadSelection,
                1,
                static_cast<std::int32_t>(e.padIndex),
                (deletePadMask_ & bit) != 0u ? 1 : 0);
        return rememberPress(r);
    }

    if (padMuteMode_) {
        r.consumed = true;
        r.action = make(Type::PadMuteTarget, e.padIndex);
        return rememberPress(r);
    }
    if (trackMuteMode_) {
        r.consumed = true;
        r.action = make(Type::TrackMuteTarget, e.padIndex);
        return rememberPress(r);
    }

    if (noteRepeatActive()) {
        lastPadIndex_ = e.padIndex;
        r.repeating = true;
        if (fullLevel_) {
            r.velocity = 127;
        } else if (halfLevel_) {
            r.velocity = 64;
        }
        return rememberPress(r);
    }

    if (sixteenLevel_) {
        if (lastPadIndex_ == 0xFF) {
            r.consumed = true;
            r.action = make(Type::SixteenLevelState, 0, -1);
            return rememberPress(r);
        }
        r.targetPadIndex = lastPadIndex_;
        if (sixteenLevelTuneMode_.load(std::memory_order_acquire)) {
            r.tuningMode = true;
            r.tuningOffsetMilliSemitones =
                    static_cast<std::int32_t>(e.padIndex) * 1000;
            r.velocity = 100;
        } else {
            r.velocity = static_cast<std::uint8_t>(
                    1u + (static_cast<unsigned>(e.padIndex) * 126u) / 15u);
        }
        return rememberPress(r);
    }

    lastPadIndex_ = e.padIndex;
    if (fullLevel_) {
        r.velocity = 127;
    } else if (halfLevel_) {
        r.velocity = 64;
    }
    return rememberPress(r);
}
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleButton(std::uint8_t note,bool pressed,std::int64_t timestampNanos) noexcept {
    const auto* b=findButton(note); if(!b) return std::nullopt;
    const std::string_view n(b->name);

    // An unfinished Copy/Delete gesture must never survive an unrelated
    // control press. Shift itself is allowed to remain held for Shift+Copy.
    if (pressed
            && copyMode_ != CopyMode::None
            && n != "Copy"
            && n != "Shift") {
        copyMode_ = CopyMode::None;
        copySourcePad_ = 0xFF;
        copyDestinationMask_ = 0;
        deletePadMask_ = 0;
    }

    if (pressed && n != "Locate") {
        const bool exitsLocate =
                n == "Main"
                || n == "Browse"
                || n == "TrackSelect"
                || n == "ProgramSelect"
                || n == "SampleSelect"
                || n == "SampleStart"
                || n == "SampleEnd"
                || n == "Tune"
                || n == "Quantize"
                || n == "TCOnOff"
                || n == "Zoom"
                || n == "Copy"
                || n == "Undo"
                || n == "AutomationReadWrite"
                || n == "TouchStripButton"
                || n == "Erase";
        if (exitsLocate && locateLatched_) {
            locateLatched_ = false;
        }
    }
    if(n=="Shift"){ shiftHeld_=pressed; return std::nullopt; }
    if(n=="Erase"){
        eraseHeld_=pressed;
        return make(Type::EraseState, pressed ? 1 : 0);
    }
    if(n=="TouchStripButton"){
        if(!pressed) return std::nullopt;
        if(shiftHeld_) return make(Type::TouchStripConfigContext, static_cast<std::int32_t>(touchStripMode_), 0);
        touchStripMode_ = mpc::sequencer::touch_strip::nextMode(touchStripMode_);
        return make(Type::TouchStripModeChanged, static_cast<std::int32_t>(touchStripMode_));
    }
    if(n=="Mode"){ modeHeld_=pressed; return std::nullopt; }
    if(n=="Locate"){
        if (pressed) {
            locateHeld_ = true;
            locatePressTimestampNanos_ = timestampNanos;
            return make(Type::LocateState, 1, locateLatched_ ? 1 : 0);
        }

        const auto held = locatePressTimestampNanos_;
        locateHeld_ = false;
        locatePressTimestampNanos_ = 0;
        if (!mpc::sequencer::locate::isMomentaryHold(
                    held, timestampNanos)) {
            locateLatched_ = !locateLatched_;
        }
        return make(
                Type::LocateState,
                locateActive() ? 1 : 0,
                locateLatched_ ? 1 : 0);
    }
    if(n=="NoteRepeat"){
        if(!pressed){
            if(!noteRepeatLatched_) noteRepeatHeld_=false;
            return make(Type::NoteRepeatState,
                    noteRepeatActive()?1:0,
                    noteRepeatLatched()?1:0);
        }
        if(shiftHeld_){
            noteRepeatLatched_=!noteRepeatLatched_;
            noteRepeatHeld_=false;
        } else {
            noteRepeatHeld_=true;
        }
        if(noteRepeatActive()) {
            sixteenLevel_=false;
        }
        return make(Type::NoteRepeatState,noteRepeatActive()?1:0,noteRepeatLatched()?1:0);
    }
    if(n=="FullLevel"){
        if(shiftHeld_){ halfLevel_=!halfLevel_; fullLevel_=false; if(halfLevel_) sixteenLevel_=false; return make(Type::HalfLevelState,halfLevel_?1:0); }
        fullLevel_=!fullLevel_; if(fullLevel_) { halfLevel_=false; sixteenLevel_=false; } return make(Type::FullLevelState,fullLevel_?1:0);
    }
    if(n=="Level16"){
        if(sixteenLevel_){
            sixteenLevel_=false;
            return make(Type::SixteenLevelState,0);
        }
        if(lastPadIndex_==0xFF){
            return make(Type::SixteenLevelState,0,-1);
        }
        sixteenLevel_=true;
        fullLevel_=false;
        halfLevel_=false;
        return make(Type::SixteenLevelState,1,lastPadIndex_);
    }
    if(n=="PadMute"){
        if(shiftHeld_){ padMuteMode_=!padMuteMode_; trackMuteMode_=false; return make(Type::PadMuteModeState,padMuteMode_?1:0); }
        trackMuteMode_=!trackMuteMode_; padMuteMode_=false; return make(Type::TrackMuteModeState,trackMuteMode_?1:0);
    }
    if(n=="PadBankAE"||n=="PadBankBF"||n=="PadBankCG"||n=="PadBankDH"){
        std::uint8_t p= n=="PadBankBF"?1:n=="PadBankCG"?2:n=="PadBankDH"?3:0;
        padBank_=static_cast<std::uint8_t>(p+(shiftHeld_?4:0)); return make(Type::PadBankChanged,padBank_);
    }
    if(n=="Main") return make(shiftHeld_?Type::NavigateTrackView:Type::NavigateMain);
    if(n=="Browse") return make(shiftHeld_?Type::BrowserUp:Type::NavigateBrowse);
    if(n=="TrackSelect") return make(shiftHeld_?Type::SequenceSelectionContext:Type::TrackSelectionContext);
    if(n=="ProgramSelect") return make(shiftHeld_?Type::TrackTypeSelectionContext:Type::ProgramSelectionContext);
    if(n=="Plus") return make(Type::AdjustValueDelta,1,shiftHeld_?1:0);
    if(n=="Minus") return make(Type::AdjustValueDelta,-1,shiftHeld_?1:0);
    if(n=="SampleSelect") return make(Type::SampleSelectContext,shiftHeld_?1:0);
    if(n=="SampleStart") return make(Type::SampleStartContext,shiftHeld_?1:0);
    if(n=="SampleEnd") return make(Type::SampleEndContext,shiftHeld_?1:0);
    if(n=="Tune") return make(Type::TuneContext,shiftHeld_?1:0);
    if(n=="Quantize") return make(Type::Quantize,shiftHeld_?1:0);
    if(n=="TCOnOff") return make(Type::TimingCorrectState,shiftHeld_?2:1);
    if(n=="Zoom") return make(Type::ZoomContext,shiftHeld_?1:0);
    if(n=="Copy"){
        if (pressed) {
            copyMode_ = shiftHeld_
                    ? CopyMode::Delete
                    : CopyMode::Copy;
            copySourcePad_ = 0xFF;
            copyDestinationMask_ = 0;
            deletePadMask_ = 0;
            return make(
                    Type::CopyContext,
                    copyMode_ == CopyMode::Delete ? 1 : 0,
                    1,
                    -1);
        }

        if (copyMode_ == CopyMode::None) {
            return std::nullopt;
        }

        const auto mode = copyMode_;
        const auto source = copySourcePad_;
        const auto mask = mode == CopyMode::Copy
                ? copyDestinationMask_
                : deletePadMask_;

        copyMode_ = CopyMode::None;
        copySourcePad_ = 0xFF;
        copyDestinationMask_ = 0;
        deletePadMask_ = 0;

        if (mode == CopyMode::Copy && source == 0xFF) {
            return make(Type::CopyContext, 0, 0, -1);
        }
        if (mask == 0u) {
            return make(
                    Type::CopyContext,
                    mode == CopyMode::Delete ? 1 : 0,
                    0,
                    source == 0xFF ? -1 : static_cast<std::int32_t>(source));
        }

        return make(
                Type::CopyPadCommit,
                mode == CopyMode::Delete ? 1 : 0,
                source == 0xFF ? -1 : static_cast<std::int32_t>(source),
                static_cast<std::int32_t>(mask));
    }
    if(!pressed) return std::nullopt;
    if(n=="Undo") return make(Type::Undo,shiftHeld_?1:0);
    if(n=="AutomationReadWrite") return make(Type::AutomationContext,shiftHeld_?1:0);
    if(n=="Record") return make(Type::TransportRecord);
    if(n=="Overdub") return make(Type::TransportOverdub);
    if(n=="Stop") return make(shiftHeld_?Type::TransportReset:Type::TransportStop);
    if(n=="Play") return make(Type::TransportPlay);
    if(n=="PlayStart") return make(Type::TransportPlayStart);
    if(n=="TapTempo") return make(Type::TapTempo);
    if(n=="StepLeft") return make(Type::StepLeft,locateActive()?1:0);
    if(n=="StepRight") return make(Type::StepRight,locateActive()?1:0);
    if(n=="BarLeft") return make(Type::BarLeft,locateActive()?1:0);
    if(n=="BarRight") return make(Type::BarRight,locateActive()?1:0);
    return std::nullopt;
}
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleJog(std::uint8_t v) noexcept {
    if(v==1) return make(Type::DataDialDelta,1,shiftHeld_?1:0);
    if(v==127) return make(Type::DataDialDelta,-1,shiftHeld_?1:0);
    return std::nullopt;
}
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleJogPress(bool p) noexcept {
    if (p) return make(Type::DataDialPress);
    return std::nullopt;
}
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::modePadAction(std::uint8_t i) const noexcept {
    if(i>=16) return std::nullopt;
    constexpr std::array<Type,16> a{
        Type::NavigateTrackView,Type::NavigateGrid,Type::NavigateWaveform,Type::Reserved,
        Type::NavigateSampleEdit,Type::Reserved,Type::NavigatePadMixer,Type::NavigateTrackMixer,
        Type::NavigateSequenceLauncher,Type::Reserved,Type::Reserved,Type::NavigateBrowse,
        Type::NavigateSampler,Type::Reserved,Type::NavigateStepSequencer,Type::Reserved};
    return make(a[i]);
}
} // namespace mpc::studio
