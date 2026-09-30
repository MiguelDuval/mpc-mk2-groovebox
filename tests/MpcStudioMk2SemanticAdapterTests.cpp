#include "MPC/MpcStudioMk2SemanticAdapter.h"
#include <cassert>
#include <cstdint>
using namespace mpc::studio;
InputEvent btn(std::uint8_t n,bool p=true){return {InputEventType::Button,0,n,static_cast<std::uint8_t>(p?127:0),0xFF,p};}
InputEvent pad(std::uint8_t i){return {InputEventType::PadNote,9,37,112,i,true};}
InputEvent jog(std::uint8_t v){return {InputEventType::JogWheel,0,100,v};}
void typeOf(const std::optional<SemanticAction>& a,SemanticActionType t){assert(a);assert(a->type==t);}
int main(){
  MpcStudioMk2SemanticAdapter a;
  static_cast<void>(a.handleControl(btn(114))); auto r=a.handlePad(pad(1)); typeOf(r.action,SemanticActionType::NavigateGrid); assert(r.consumed);
  MpcStudioMk2SemanticAdapter b; static_cast<void>(b.handleControl(btn(49))); auto bank=b.handleControl(btn(36)); typeOf(bank,SemanticActionType::PadBankChanged); assert(bank->value0==5);
  static_cast<void>(b.handleControl(btn(49,false))); static_cast<void>(b.handleControl(btn(39))); assert(b.handlePad(pad(0)).velocity==127);
  static_cast<void>(b.handleControl(btn(49))); auto half=b.handleControl(btn(39)); typeOf(half,SemanticActionType::HalfLevelState); assert(half->value0==1); assert(b.handlePad(pad(0)).velocity==64);
  MpcStudioMk2SemanticAdapter c; typeOf(c.handleControl(btn(13)),SemanticActionType::TrackSelectionContext); auto d=c.handleControl(jog(1)); typeOf(d,SemanticActionType::DataDialDelta); assert(d->value0==1);
  MpcStudioMk2SemanticAdapter e; typeOf(e.handleControl(btn(82)),SemanticActionType::TransportPlay); static_cast<void>(e.handleControl(btn(49))); typeOf(e.handleControl(btn(81)),SemanticActionType::TransportReset);
  MpcStudioMk2SemanticAdapter f; auto nr=f.handleControl(btn(11)); typeOf(nr,SemanticActionType::NoteRepeatState); assert(nr->value0==1); static_cast<void>(f.handleControl(btn(4))); auto tm=f.handlePad(pad(2)); typeOf(tm.action,SemanticActionType::TrackMuteTarget); static_cast<void>(f.handleControl(btn(4))); static_cast<void>(f.handleControl(btn(49))); static_cast<void>(f.handleControl(btn(4))); auto pm=f.handlePad(pad(2)); typeOf(pm.action,SemanticActionType::PadMuteTarget);
  return 0;
}
