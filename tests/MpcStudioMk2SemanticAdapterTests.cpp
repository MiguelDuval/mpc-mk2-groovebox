#include "MPC/MpcStudioMk2SemanticAdapter.h"
#include <cassert>
#include <cstdint>
using namespace mpc::studio;
InputEvent btn(std::uint8_t n,bool p=true){return {InputEventType::Button,0,n,static_cast<std::uint8_t>(p?127:0),0xFF,p};}
InputEvent btnAt(std::uint8_t n,bool p,std::int64_t timestamp){
  auto event=btn(n,p);
  event.timestampNanos=timestamp;
  return event;
}
InputEvent pad(std::uint8_t i){return {InputEventType::PadNote,9,37,112,i,true};}
InputEvent jog(std::uint8_t v){return {InputEventType::JogWheel,0,100,v};}
InputEvent strip(std::uint8_t v){return {InputEventType::TouchStrip,0,33,v};}
void typeOf(const std::optional<SemanticAction>& a,SemanticActionType t){assert(a);assert(a->type==t);}
int main(){
  MpcStudioMk2SemanticAdapter a;
  static_cast<void>(a.handleControl(btn(114))); auto r=a.handlePad(pad(1)); typeOf(r.action,SemanticActionType::NavigateGrid); assert(r.consumed);
  MpcStudioMk2SemanticAdapter b; static_cast<void>(b.handleControl(btn(49))); auto bank=b.handleControl(btn(36)); typeOf(bank,SemanticActionType::PadBankChanged); assert(bank->value0==5);
  auto source=b.handlePad(pad(4)); assert(source.targetPadIndex==4); static_cast<void>(b.handleControl(btn(49,false))); static_cast<void>(b.handleControl(btn(39))); assert(b.handlePad(pad(0)).velocity==127);
  static_cast<void>(b.handleControl(btn(49))); auto half=b.handleControl(btn(39)); typeOf(half,SemanticActionType::HalfLevelState); assert(half->value0==1); assert(b.handlePad(pad(0)).velocity==64);
  static_cast<void>(b.handlePad(pad(4))); static_cast<void>(b.handleControl(btn(49))); auto level16=b.handleControl(btn(40)); typeOf(level16,SemanticActionType::SixteenLevelState); assert(level16->value0==1 && level16->value1==4); auto low=b.handlePad(pad(0)); auto high=b.handlePad(pad(15)); assert(low.targetPadIndex==4 && low.velocity==1); assert(high.targetPadIndex==4 && high.velocity==127); auto level16Release=b.handlePad(InputEvent{InputEventType::PadNote,9,37,0,15,false}); assert(level16Release.targetPadIndex==4); static_cast<void>(b.handleControl(btn(40))); assert(!b.sixteenLevel());

  MpcStudioMk2SemanticAdapter c; typeOf(c.handleControl(btn(13)),SemanticActionType::TrackSelectionContext); auto d=c.handleControl(jog(1)); typeOf(d,SemanticActionType::DataDialDelta); assert(d->value0==1); auto plus=c.handleControl(btn(54)); typeOf(plus,SemanticActionType::AdjustValueDelta); assert(plus->value0==1);
  MpcStudioMk2SemanticAdapter e; typeOf(e.handleControl(btn(82)),SemanticActionType::TransportPlay); static_cast<void>(e.handleControl(btn(49))); typeOf(e.handleControl(btn(81)),SemanticActionType::TransportReset);
  MpcStudioMk2SemanticAdapter rate;
  typeOf(rate.handleControl(btn(11)),SemanticActionType::NoteRepeatState);
  auto rateLow=rate.handleControl(strip(0));
  typeOf(rateLow,SemanticActionType::NoteRepeatRateChanged);
  assert(rateLow->value0==0 && rateLow->value1==960);
  assert(!rate.handleControl(strip(0)).has_value());
  auto rateSixteenth=rate.handleControl(strip(40));
  typeOf(rateSixteenth,SemanticActionType::NoteRepeatRateChanged);
  assert(rateSixteenth->value0==2 && rateSixteenth->value1==240);
  auto rateTriplet=rate.handleControl(strip(96));
  typeOf(rateTriplet,SemanticActionType::NoteRepeatRateChanged);
  assert(rateTriplet->value0==6 && rateTriplet->value1==320);
    MpcStudioMk2SemanticAdapter f; auto nr=f.handleControl(btn(11)); typeOf(nr,SemanticActionType::NoteRepeatState); assert(nr->value0==1); auto repeated=f.handlePad(pad(2)); assert(repeated.repeating); assert(repeated.targetPadIndex==2); assert(repeated.velocity==112); auto repeatRelease=f.handlePad(InputEvent{InputEventType::PadNote,9,37,0,2,false}); assert(repeatRelease.repeating); assert(repeatRelease.targetPadIndex==2); static_cast<void>(f.handleControl(btn(11,false)));

  MpcStudioMk2SemanticAdapter latched; static_cast<void>(latched.handleControl(btn(49))); auto latch=latched.handleControl(btn(11)); typeOf(latch,SemanticActionType::NoteRepeatState); assert(latch->value0==1 && latch->value1==1); auto latchedPress=latched.handlePad(pad(3)); assert(latchedPress.repeating); auto latchedRelease=latched.handlePad(InputEvent{InputEventType::PadNote,9,37,0,3,false}); assert(latchedRelease.repeating); assert(latchedRelease.targetPadIndex==3); static_cast<void>(latched.handleControl(btn(11))); static_cast<void>(f.handleControl(btn(4))); auto tm=f.handlePad(pad(2)); typeOf(tm.action,SemanticActionType::TrackMuteTarget); static_cast<void>(f.handleControl(btn(4))); static_cast<void>(f.handleControl(btn(49))); static_cast<void>(f.handleControl(btn(4))); auto pm=f.handlePad(pad(2)); typeOf(pm.action,SemanticActionType::PadMuteTarget);

  MpcStudioMk2SemanticAdapter stripModes;
  auto stripMode = stripModes.handleControl(btn(0));
  typeOf(stripMode, SemanticActionType::TouchStripModeChanged);
  assert(stripMode->value0 == 1);
  static_cast<void>(stripModes.handleControl(btn(49)));
  auto configAction = stripModes.handleControl(btn(0));
  typeOf(configAction, SemanticActionType::TouchStripConfigContext);
  assert(configAction->value0 == 1);
  static_cast<void>(stripModes.handleControl(btn(49, false)));
  auto touchOn = stripModes.handleControl(
      InputEvent{InputEventType::TouchStripTouch, 0, 78, 127, 0xFF, true});
  typeOf(touchOn, SemanticActionType::TouchStripTouchState);
  assert(touchOn->value0 == 1 && stripModes.touchStripTouched());
  auto touchOff = stripModes.handleControl(
      InputEvent{InputEventType::TouchStripTouch, 0, 78, 0, 0xFF, false});
  typeOf(touchOff, SemanticActionType::TouchStripTouchState);
  assert(touchOff->value0 == 0 && !stripModes.touchStripTouched());

  MpcStudioMk2SemanticAdapter locate;
  auto locatePress = locate.handleControl(btnAt(70, true, 1'000'000'000));
  typeOf(locatePress, SemanticActionType::LocateState);
  assert(locatePress->value0 == 1 && locatePress->value1 == 0);
  auto store = locate.handlePad(pad(8));
  typeOf(store.action, SemanticActionType::LocatePad);
  assert(store.consumed && store.action->value0 == 0 && store.action->value1 == 1);
  auto locateRelease = locate.handleControl(btnAt(70, false, 1'200'000'000));
  typeOf(locateRelease, SemanticActionType::LocateState);
  assert(locateRelease->value0 == 1 && locateRelease->value1 == 1);
  auto jump = locate.handlePad(pad(0));
  typeOf(jump.action, SemanticActionType::LocatePad);
  assert(jump.consumed && jump.action->value0 == 0 && jump.action->value1 == 0);
  assert(locate.locateActive() && locate.locateLatched());

  auto modeExit = locate.handleControl(btn(52));
  typeOf(modeExit, SemanticActionType::NavigateMain);
  assert(!locate.locateActive());

  MpcStudioMk2SemanticAdapter momentaryLocate;
  static_cast<void>(momentaryLocate.handleControl(btnAt(70, true, 3'000'000'000)));
  auto temporaryStore = momentaryLocate.handlePad(pad(13));
  typeOf(temporaryStore.action, SemanticActionType::LocatePad);
  assert(temporaryStore.action->value0 == 5 && temporaryStore.action->value1 == 1);
  auto temporaryRelease = momentaryLocate.handleControl(btnAt(
      70, false, 3'400'000'000));
  typeOf(temporaryRelease, SemanticActionType::LocateState);
  assert(temporaryRelease->value0 == 0 && temporaryRelease->value1 == 0);
  assert(!momentaryLocate.locateActive());

  MpcStudioMk2SemanticAdapter locateNavigation;
  static_cast<void>(locateNavigation.handleControl(btnAt(
      70, true, 4'000'000'000)));
  static_cast<void>(locateNavigation.handleControl(btnAt(
      70, false, 4'200'000'000)));
  auto previousEvent = locateNavigation.handleControl(btn(68));
  typeOf(previousEvent, SemanticActionType::StepLeft);
  assert(previousEvent->value0 == 1);

  return 0;
}
