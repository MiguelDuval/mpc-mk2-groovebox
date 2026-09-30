#pragma once
#include "MPC/MpcStudioMk2InputDecoder.h"
#include <cstdint>
#include <optional>

namespace mpc::studio {
enum class SemanticActionType : std::int32_t {
    None=0, NavigateMain=1, NavigateTrackView=2, NavigateGrid=3, NavigateWaveform=4,
    NavigateSampleEdit=5, NavigatePadMixer=6, NavigateTrackMixer=7, NavigateSequenceLauncher=8,
    NavigateBrowse=9, NavigateSampler=10, NavigateStepSequencer=11, BrowserUp=12,
    TrackSelectionContext=13, SequenceSelectionContext=14, ProgramSelectionContext=15,
    TrackTypeSelectionContext=16, DataDialDelta=17, DataDialPress=18, AdjustValueDelta=19,
    PadBankChanged=20, NoteRepeatState=21, FullLevelState=22, HalfLevelState=23,
    PadMuteModeState=24, TrackMuteModeState=25, TransportRecord=26, TransportOverdub=27,
    TransportStop=28, TransportPlay=29, TransportPlayStart=30, TransportReset=31,
    StepLeft=32, StepRight=33, BarLeft=34, BarRight=35, TapTempo=36, TouchStripValue=37,
    LocateState=38, LocatePad=39, SampleSelectContext=40, SampleStartContext=41,
    SampleEndContext=42, TuneContext=43, Quantize=44, TimingCorrectState=45, ZoomContext=46,
    CopyContext=47, Undo=48, AutomationContext=49, PadMuteTarget=50, TrackMuteTarget=51, SixteenLevelState=53, Reserved=52
};
struct SemanticAction final {
    SemanticActionType type=SemanticActionType::None;
    std::int32_t value0=0, value1=0, value2=0;
};
struct PadRoutingResult final {
    bool consumed=false;
    std::uint8_t velocity=0;
    std::uint8_t targetPadIndex=0xFF;
    std::optional<SemanticAction> action;
};
class MpcStudioMk2SemanticAdapter final {
public:
    std::optional<SemanticAction> handleControl(const InputEvent&) noexcept;
    PadRoutingResult handlePad(const InputEvent&) noexcept;
    [[nodiscard]] bool shiftHeld() const noexcept { return shiftHeld_; }
    [[nodiscard]] bool modeHeld() const noexcept { return modeHeld_; }
    [[nodiscard]] bool locateHeld() const noexcept { return locateHeld_; }
    [[nodiscard]] bool noteRepeatActive() const noexcept { return noteRepeatHeld_ || noteRepeatLatched_; }
    [[nodiscard]] bool noteRepeatLatched() const noexcept { return noteRepeatLatched_; }
    [[nodiscard]] bool fullLevel() const noexcept { return fullLevel_; }
    [[nodiscard]] bool halfLevel() const noexcept { return halfLevel_; }
    [[nodiscard]] bool padMuteMode() const noexcept { return padMuteMode_; }
    [[nodiscard]] bool trackMuteMode() const noexcept { return trackMuteMode_; }
    [[nodiscard]] bool sixteenLevel() const noexcept { return sixteenLevel_; }
    [[nodiscard]] std::uint8_t lastPadIndex() const noexcept { return lastPadIndex_; }
    [[nodiscard]] std::uint8_t padBank() const noexcept { return padBank_; }
private:
    std::optional<SemanticAction> handleButton(std::uint8_t,bool) noexcept;
    std::optional<SemanticAction> handleJog(std::uint8_t) noexcept;
    std::optional<SemanticAction> handleJogPress(bool) noexcept;
    std::optional<SemanticAction> modePadAction(std::uint8_t) const noexcept;
    bool shiftHeld_=false, modeHeld_=false, locateHeld_=false;
    bool noteRepeatHeld_=false, noteRepeatLatched_=false;
    bool fullLevel_=false, halfLevel_=false;
    bool padMuteMode_=false, trackMuteMode_=false, sixteenLevel_=false;
    std::uint8_t lastPadIndex_=0xFF;
    std::uint8_t padBank_=0;
};
} // namespace mpc::studio
