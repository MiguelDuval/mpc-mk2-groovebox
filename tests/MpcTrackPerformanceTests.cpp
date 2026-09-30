#include "MPC/Sequencer/MpcTrackPerformance.h"

#include <array>
#include <cassert>

int main() {
    std::array<mpc::domain::Track, 4> tracks{};

    assert(!mpc::sequencer::anyTrackSoloed(tracks));
    assert(mpc::sequencer::shouldScheduleTrack(tracks[0], false));
    assert(!mpc::sequencer::shouldAdvanceTrackSilently(tracks[0], false));

    tracks[0].muted = true;
    assert(!mpc::sequencer::shouldScheduleTrack(tracks[0], false));
    assert(mpc::sequencer::shouldAdvanceTrackSilently(tracks[0], false));

    tracks[0].muted = false;
    tracks[1].soloed = true;
    assert(mpc::sequencer::anyTrackSoloed(tracks));
    assert(mpc::sequencer::shouldScheduleTrack(tracks[1], true));
    assert(!mpc::sequencer::shouldScheduleTrack(tracks[0], true));
    assert(!mpc::sequencer::shouldAdvanceTrackSilently(tracks[1], true));
    assert(mpc::sequencer::shouldAdvanceTrackSilently(tracks[0], true));

    tracks[1].muted = true;
    assert(mpc::sequencer::anyTrackSoloed(tracks));
    assert(!mpc::sequencer::shouldScheduleTrack(tracks[1], true));
    assert(mpc::sequencer::shouldAdvanceTrackSilently(tracks[1], false));
    assert(mpc::sequencer::shouldAdvanceTrackSilently(tracks[1], true));

    return 0;
}
