#include "MPC/Sequencer/MpcTouchStripPolicy.h"
#include <cassert>
int main(){
 using namespace mpc::sequencer::touch_strip;
 assert(nextMode(Mode::Level)==Mode::Pan);
 assert(nextMode(Mode::Pan)==Mode::Tune);
 assert(nextMode(Mode::Tune)==Mode::SampleStart);
 assert(nextMode(Mode::SampleStart)==Mode::SampleEnd);
 assert(nextMode(Mode::SampleEnd)==Mode::Level);
 assert(modeIndicatorSegment(Mode::Level)==0);
 assert(modeIndicatorSegment(Mode::Pan)==2);
 assert(modeIndicatorSegment(Mode::Tune)==4);
 assert(modeIndicatorSegment(Mode::SampleStart)==6);
 assert(modeIndicatorSegment(Mode::SampleEnd)==8);
 assert(valueIndicatorSegment(0)==0);
 assert(valueIndicatorSegment(63)==4);
 assert(valueIndicatorSegment(127)==8);
 return 0;
}
