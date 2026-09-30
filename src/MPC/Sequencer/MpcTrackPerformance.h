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

} // namespace mpc::sequencer
