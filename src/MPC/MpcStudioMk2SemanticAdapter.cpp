#include "MpcStudioMk2SemanticAdapter.h"
#include "MpcStudioMk2ControlMap.h"
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
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleControl(const InputEvent& e) noexcept {
    switch(e.type){
        case InputEventType::Button: return handleButton(e.number,e.pressed);
        case InputEventType::JogWheel: return handleJog(e.value);
        case InputEventType::JogPress: return handleJogPress(e.pressed);
        case InputEventType::TouchStrip: return make(Type::TouchStripValue,e.value);
        default: return std::nullopt;
    }
}
PadRoutingResult MpcStudioMk2SemanticAdapter::handlePad(const InputEvent& e) noexcept {
    PadRoutingResult r;
    r.velocity=e.value;
    r.targetPadIndex=e.padIndex;
    if(e.type!=InputEventType::PadNote) return r;
    if(!e.pressed){
        if(sixteenLevel_ && lastPadIndex_!=0xFF){
            r.targetPadIndex=lastPadIndex_;
        }
        return r;
    }
    if(modeHeld_){ r.consumed=true; r.action=modePadAction(e.padIndex); return r; }
    if(locateHeld_){ r.consumed=true; r.action=make(Type::LocatePad,e.padIndex); return r; }
    if(padMuteMode_){ r.consumed=true; r.action=make(Type::PadMuteTarget,e.padIndex); return r; }
    if(trackMuteMode_){ r.consumed=true; r.action=make(Type::TrackMuteTarget,e.padIndex); return r; }

    if(sixteenLevel_){
        if(lastPadIndex_==0xFF){
            r.consumed=true;
            r.action=make(Type::SixteenLevelState,0,-1);
            return r;
        }
        r.targetPadIndex=lastPadIndex_;
        r.velocity=static_cast<std::uint8_t>(
                1u + (static_cast<unsigned>(e.padIndex) * 126u) / 15u);
        return r;
    }

    lastPadIndex_=e.padIndex;
    if(fullLevel_) r.velocity=127; else if(halfLevel_) r.velocity=64;
    return r;
}
std::optional<SemanticAction> MpcStudioMk2SemanticAdapter::handleButton(std::uint8_t note,bool pressed) noexcept {
    const auto* b=findButton(note); if(!b) return std::nullopt;
    const std::string_view n(b->name);
    if(n=="Shift"){ shiftHeld_=pressed; return std::nullopt; }
    if(n=="Mode"){ modeHeld_=pressed; return std::nullopt; }
    if(n=="Locate"){ locateHeld_=pressed; return make(Type::LocateState,locateHeld_?1:0); }
    if(n=="NoteRepeat"){
        if(!pressed){ if(!noteRepeatLatched_) noteRepeatHeld_=false; return make(Type::NoteRepeatState,noteRepeatActive()?1:0,noteRepeatLatched()?1:0); }
        if(shiftHeld_){ noteRepeatLatched_=!noteRepeatLatched_; noteRepeatHeld_=false; } else noteRepeatHeld_=true;
        return make(Type::NoteRepeatState,noteRepeatActive()?1:0,noteRepeatLatched()?1:0);
    }
    if(!pressed) return std::nullopt;
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
    if(n=="Copy") return make(Type::CopyContext,shiftHeld_?1:0);
    if(n=="Undo") return make(Type::Undo,shiftHeld_?1:0);
    if(n=="AutomationReadWrite") return make(Type::AutomationContext,shiftHeld_?1:0);
    if(n=="Record") return make(Type::TransportRecord);
    if(n=="Overdub") return make(Type::TransportOverdub);
    if(n=="Stop") return make(shiftHeld_?Type::TransportReset:Type::TransportStop);
    if(n=="Play") return make(Type::TransportPlay);
    if(n=="PlayStart") return make(Type::TransportPlayStart);
    if(n=="TapTempo") return make(Type::TapTempo);
    if(n=="StepLeft") return make(Type::StepLeft,locateHeld_?1:0);
    if(n=="StepRight") return make(Type::StepRight,locateHeld_?1:0);
    if(n=="BarLeft") return make(Type::BarLeft,locateHeld_?1:0);
    if(n=="BarRight") return make(Type::BarRight,locateHeld_?1:0);
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
