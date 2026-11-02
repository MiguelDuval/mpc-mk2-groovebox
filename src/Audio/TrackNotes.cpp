#include "TrackNotes.h"

#include "MPC/MpcProjectState.h"
#include "MPC/Domain/MpcDomain.h"

#include <string>

namespace mpc::audio {

std::string getTrackNotesAsString(const mpc::MpcProjectState& project,
                                  std::uint8_t trackIndex) {
    const auto& sequence = project.activeSequence();
    if (trackIndex >= static_cast<std::uint8_t>(sequence.tracks.size())) {
        return "";
    }

    const auto& track = sequence.tracks[trackIndex];
    std::string result;

    for (const auto& pattern : track.patterns) {
        for (const auto& event : pattern.notes) {
            if (!result.empty()) {
                result += ";";
            }
            result += std::to_string(event.tick) + ","
                    + std::to_string(event.durationTicks) + ","
                    + std::to_string(event.note) + ","
                    + std::to_string(event.velocity) + ","
                    + std::to_string(event.probability) + ","
                    + std::to_string(event.ratchet) + ","
                    + std::to_string(event.nudgeTicks);
        }
    }

    return result;
}

} // namespace mpc::audio
