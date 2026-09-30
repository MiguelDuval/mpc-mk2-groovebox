#include "MPC/Sequencer/TrackMuteQuantizer.h"

#include <array>
#include <cassert>

int main() {
    using Mode = mpc::sequencer::TrackMuteQuantizeMode;
    using Quantizer = mpc::sequencer::TrackMuteQuantizer;

    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Off, 4, 4) == 0);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Sixteenth, 4, 4) == 240);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Eighth, 4, 4) == 480);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Quarter, 4, 4) == 960);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Half, 4, 4) == 1920);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::Bar, 3, 4) == 2880);
    assert(mpc::sequencer::trackMuteQuantizeTicks(Mode::TwoBars, 4, 4) == 7680);

    Quantizer q;
    q.setMode(Mode::Sixteenth);
    assert(q.enqueue(2, true, 100, 0, 3840, 240));
    assert(q.nextDueTicks() == 140);
    assert(q.pendingTargetForTrack(2).has_value());
    assert(*q.pendingTargetForTrack(2));

    q.advance(139);
    assert(q.nextDueTicks() == 1);
    q.advance(1);

    std::array<Quantizer::Command, Quantizer::kCapacity> due{};
    assert(q.takeDue(due) == 1);
    assert(q.nextDueTicks() > 1'000'000'000);

    assert(q.enqueue(4, true, 240, 0, 3840, 240));
    assert(q.nextDueTicks() == 240);
    assert(q.enqueue(4, false, 300, 0, 3840, 240));
    assert(q.nextDueTicks() == 240);
    assert(q.pendingTargetForTrack(4).has_value());
    assert(!*q.pendingTargetForTrack(4));

    q.clear();

    assert(q.enqueue(5, true, 3800, 0, 3840, 960));
    assert(q.nextDueTicks() == 40);

    q.clear();
    q.setMode(Mode::TwoBars);
    assert(q.enqueue(6, true, 3800, 0, 3840, 7680));
    assert(q.nextDueTicks() == 3880);
    q.advance(3840);
    assert(q.nextDueTicks() == 40);
    q.advance(40);
    assert(q.takeDue(due) == 1);
    assert(q.nextDueTicks() > 1'000'000'000);

    q.clear();
    assert(q.nextDueTicks() > 1'000'000'000);
    return 0;
}
