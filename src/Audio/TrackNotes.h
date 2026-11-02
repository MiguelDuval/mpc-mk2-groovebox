#pragma once

#include <cstdint>
#include <string>

namespace mpc {
class MpcProjectState;
} // namespace mpc

namespace mpc::audio {

/**
 * Platform-independent track note retrieval.
 * Formats all MIDI note events of a track as a semicolon-separated string:
 * "tick,dur,note,vel,prob,rat,nudge;tick,dur,note,vel,prob,rat,nudge;..."
 * No Oboe/Android dependency; safe to compile and test on any host.
 */
std::string getTrackNotesAsString(const mpc::MpcProjectState& project,
                                  std::uint8_t trackIndex) noexcept;

} // namespace mpc::audio
