#pragma once

#include "MPC/Domain/MpcDomain.h"

#include <span>

namespace mpc::sequencer {

inline bool anyTrackSoloed(
        std::span<const domain::Track> tracks) noexcept {
    for (const auto& track : tracks) {
        if (track.soloed) {
            return true;
        }
    }
    return false;
}

inline bool shouldScheduleTrack(
        const domain::Track& track,
        bool anySolo) noexcept {
    if (track.muted) {
        return false;
    }
    if (anySolo && !track.soloed) {
        return false;
    }
    return true;
}

// Live MUTE/SOLO changes the audible layer, not musical time.
// A track that is currently inaudible must keep its playback cursor moving
// so unmuting/unsoloing returns it at the same musical position as the rest.
inline bool shouldAdvanceTrackSilently(
        const domain::Track& track,
        bool anySolo) noexcept {
    return !shouldScheduleTrack(track, anySolo);
}

} // namespace mpc::sequencer
