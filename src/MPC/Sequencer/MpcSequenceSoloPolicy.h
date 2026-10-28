#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"

#include <span>

namespace mpc::sequencer {

// Track Solo is supported only where the current sequencer can produce audio
// from the Track. This keeps unsupported MIDI/Audio/Plugin routes truthful.
[[nodiscard]] inline bool canSoloTrackPlayback(
        const MpcProjectState& projectState,
        const domain::Track& track) noexcept {
    return track.kind == domain::TrackKind::Drum
            && !track.patterns.empty()
            && projectState.findDrumProgram(track.programId) != nullptr;
}

// The session builds this list from successfully-created playback instances.
// Re-evaluating its live flags lets users change Solo during playback without
// allocating or touching the realtime audio callback.
[[nodiscard]] inline bool hasSoloedPlaybackTrack(
        std::span<const domain::Track* const> playbackTracks) noexcept {
    for (const auto* track : playbackTracks) {
        if (track != nullptr && track->soloed) {
            return true;
        }
    }
    return false;
}

// Mute always wins on the same Track. With no active Solo, all unmuted tracks
// remain audible; with any active Solo, only tracks carrying Solo pass.
[[nodiscard]] inline bool shouldRouteTrackForSolo(
        bool trackMuted,
        bool trackSoloed,
        bool anyTrackSoloed) noexcept {
    return !trackMuted && (!anyTrackSoloed || trackSoloed);
}

} // namespace mpc::sequencer
