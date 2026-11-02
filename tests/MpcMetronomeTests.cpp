#include "Audio/Metronome.h"
#include "Audio/TrackNotes.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"

#include <cassert>
#include <cmath>
#include <cstdint>
#include <string>

int main() {
    // Construct project state and metronome directly (control-thread setup).
    mpc::MpcProjectState projectState;
    mpc::audio::Metronome metro;

    // --- Initial snapshots reflect active sequence ---
    const auto& initialSeq = projectState.activeSequence();
    assert(std::abs(metro.tempo() - initialSeq.tempoBpm) < 0.001);
    assert(metro.timeSigNumerator() == initialSeq.numerator);
    assert(metro.timeSigDenominator() == initialSeq.denominator);

    // --- Metronome enable/disable ---
    // Initially disabled.
    assert(!metro.enabled());

    // Enable.
    const std::string enableResult = metro.setEnabled(true);
    assert(metro.enabled());
    assert(enableResult.find("ON") != std::string::npos);

    // Disable (requests reset; callback consumes on next invocation).
    const std::string disableResult = metro.setEnabled(false);
    assert(!metro.enabled());
    assert(disableResult.find("OFF") != std::string::npos);

    // Re-enable.
    metro.setEnabled(true);
    assert(metro.enabled());

    // --- Volume ---
    assert(std::abs(metro.volume() - 0.8f) < 0.001f);
    const std::string volResult = metro.setVolume(0.5f);
    assert(std::abs(metro.volume() - 0.5f) < 0.001f);
    assert(volResult.find("VOLUME") != std::string::npos);
    // Clamping: values above 1.0 clamp to 1.0.
    metro.setVolume(2.0f);
    assert(std::abs(metro.volume() - 1.0f) < 0.001f);
    // Clamping: values below 0.0 clamp to 0.0.
    metro.setVolume(-0.5f);
    assert(std::abs(metro.volume() - 0.0f) < 0.001f);
    // Invalid values do not corrupt valid configuration:
    metro.setVolume(0.75f);
    assert(std::abs(metro.volume() - 0.75f) < 0.001f);

    // --- Sound modes ---
    assert(metro.sound() == 0); // click
    assert(metro.setSound(1).find("beep") != std::string::npos);
    assert(metro.sound() == 1);
    assert(metro.setSound(2).find("wood") != std::string::npos);
    assert(metro.sound() == 2);
    assert(metro.setSound(3).find("none") != std::string::npos);
    assert(metro.sound() == 3);
    // Invalid index rejected.
    assert(metro.setSound(4).find("failed") != std::string::npos);
    assert(metro.sound() == 3); // unchanged

    // --- Tempo snapshots ---
    metro.setTempo(120.0);
    assert(std::abs(metro.tempo() - 120.0) < 0.001);
    metro.setTempo(60.0);
    assert(std::abs(metro.tempo() - 60.0) < 0.001);
    // Clamping: tempo below 20 clamps to 20.
    metro.setTempo(10.0);
    assert(std::abs(metro.tempo() - 20.0) < 0.001);
    // Clamping: tempo above 300 clamps to 300.
    metro.setTempo(400.0);
    assert(std::abs(metro.tempo() - 300.0) < 0.001);
    // Invalid values do not corrupt valid configuration:
    metro.setTempo(96.0);
    assert(std::abs(metro.tempo() - 96.0) < 0.001);

    // --- Time-signature snapshots ---
    metro.setTimeSignature(4, 4);
    assert(metro.timeSigNumerator() == 4);
    assert(metro.timeSigDenominator() == 4);
    metro.setTimeSignature(7, 8);
    assert(metro.timeSigNumerator() == 7);
    assert(metro.timeSigDenominator() == 8);
    // Invalid (zero) values are ignored.
    metro.setTimeSignature(0, 0);
    assert(metro.timeSigNumerator() == 7);
    assert(metro.timeSigDenominator() == 8);
    // Valid values after invalid ones:
    metro.setTimeSignature(3, 4);
    assert(metro.timeSigNumerator() == 3);
    assert(metro.timeSigDenominator() == 4);

    // --- Track note retrieval ---
    // Invalid track index returns empty string.
    assert(mpc::audio::getTrackNotesAsString(projectState, 255).empty());
    assert(mpc::audio::getTrackNotesAsString(projectState, 99).empty());

    // Add a track with a known MIDI note event to the active sequence.
    const auto trackId = projectState.addTrack(
            mpc::domain::TrackKind::Midi, "Test Track");
    assert(trackId != 0);

    auto& sequence = projectState.activeSequence();
    assert(sequence.tracks.size() > 0);

    auto& track = sequence.tracks.back();
    mpc::domain::Pattern pattern;
    pattern.id = "test-pattern-1";
    pattern.name = "Test Pattern";
    pattern.lengthTicks = 3840;
    pattern.notes = {
            mpc::domain::MidiNoteEvent{120, 180, 36, 100, 88, 2, -11},
            mpc::domain::MidiNoteEvent{600, 90, 42, 111, 123, 1, 7},
    };
    track.patterns.push_back(std::move(pattern));

    const std::string notes = mpc::audio::getTrackNotesAsString(
            projectState,
            static_cast<std::uint8_t>(sequence.tracks.size() - 1));
    // Format: tick,dur,note,vel,prob,rat,nudge;tick,dur,note,vel,prob,rat,nudge
    assert(notes.find("120,180,36,100,88,2,-11") != std::string::npos);
    assert(notes.find("600,90,42,111,123,1,7") != std::string::npos);
    assert(notes.find(";") != std::string::npos); // separator present

    // Track 0 (if it exists) may have no notes, but must not crash.
    if (sequence.tracks.size() > 1) {
        const std::string notes0 = mpc::audio::getTrackNotesAsString(projectState, 0);
        (void)notes0;
    }

    return 0;
}