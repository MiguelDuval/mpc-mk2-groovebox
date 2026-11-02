#include "Audio/AudioEngine.h"
#include "MPC/Domain/MpcDomain.h"
#include "MPC/MpcProjectState.h"

#include <cassert>
#include <cmath>
#include <cstdint>
#include <string>

int main() {
    // Construct project state and engine directly (control-thread setup).
    mpc::MpcProjectState projectState;
    mpc::audio::AudioEngine engine(projectState);

    // --- Initial snapshots reflect active sequence ---
    const auto& initialSeq = projectState.activeSequence();
    assert(std::abs(engine.metronomeTempo() - initialSeq.tempoBpm) < 0.001);
    assert(engine.metronomeTimeSigNumerator() == initialSeq.numerator);
    assert(engine.metronomeTimeSigDenominator() == initialSeq.denominator);

    // --- Metronome enable/disable ---
    // Initially disabled.
    assert(!engine.metronomeEnabled());

    // Enable.
    const std::string enableResult =
            engine.setMetronomeEnabled(true);
    assert(engine.metronomeEnabled());
    assert(enableResult.find("ON") != std::string::npos);

    // Disable (requests reset; callback consumes on next invocation).
    const std::string disableResult =
            engine.setMetronomeEnabled(false);
    assert(!engine.metronomeEnabled());
    assert(disableResult.find("OFF") != std::string::npos);

    // Re-enable.
    engine.setMetronomeEnabled(true);
    assert(engine.metronomeEnabled());

    // --- Volume ---
    assert(std::abs(engine.metronomeVolume() - 0.8f) < 0.001f);
    const std::string volResult = engine.setMetronomeVolume(0.5f);
    assert(std::abs(engine.metronomeVolume() - 0.5f) < 0.001f);
    assert(volResult.find("VOLUME") != std::string::npos);
    // Clamping: values above 1.0 clamp to 1.0.
    engine.setMetronomeVolume(2.0f);
    assert(std::abs(engine.metronomeVolume() - 1.0f) < 0.001f);
    // Clamping: values below 0.0 clamp to 0.0.
    engine.setMetronomeVolume(-0.5f);
    assert(std::abs(engine.metronomeVolume() - 0.0f) < 0.001f);
    // Invalid values do not corrupt valid configuration:
    engine.setMetronomeVolume(0.75f);
    assert(std::abs(engine.metronomeVolume() - 0.75f) < 0.001f);

    // --- Sound modes ---
    assert(engine.metronomeSound() == 0); // click
    assert(engine.setMetronomeSound(1).find("beep") != std::string::npos);
    assert(engine.metronomeSound() == 1);
    assert(engine.setMetronomeSound(2).find("wood") != std::string::npos);
    assert(engine.metronomeSound() == 2);
    assert(engine.setMetronomeSound(3).find("none") != std::string::npos);
    assert(engine.metronomeSound() == 3);
    // Invalid index rejected.
    assert(engine.setMetronomeSound(4).find("failed") != std::string::npos);
    assert(engine.metronomeSound() == 3); // unchanged

    // --- Tempo snapshots ---
    engine.setMetronomeTempo(120.0);
    assert(std::abs(engine.metronomeTempo() - 120.0) < 0.001);
    engine.setMetronomeTempo(60.0);
    assert(std::abs(engine.metronomeTempo() - 60.0) < 0.001);
    // Clamping: tempo below 20 clamps to 20.
    engine.setMetronomeTempo(10.0);
    assert(std::abs(engine.metronomeTempo() - 20.0) < 0.001);
    // Clamping: tempo above 300 clamps to 300.
    engine.setMetronomeTempo(400.0);
    assert(std::abs(engine.metronomeTempo() - 300.0) < 0.001);
    // Invalid values do not corrupt valid configuration:
    engine.setMetronomeTempo(96.0);
    assert(std::abs(engine.metronomeTempo() - 96.0) < 0.001);

    // --- Time-signature snapshots ---
    engine.setMetronomeTimeSignature(4, 4);
    assert(engine.metronomeTimeSigNumerator() == 4);
    assert(engine.metronomeTimeSigDenominator() == 4);
    engine.setMetronomeTimeSignature(7, 8);
    assert(engine.metronomeTimeSigNumerator() == 7);
    assert(engine.metronomeTimeSigDenominator() == 8);
    // Invalid (zero) values are ignored.
    engine.setMetronomeTimeSignature(0, 0);
    assert(engine.metronomeTimeSigNumerator() == 7);
    assert(engine.metronomeTimeSigDenominator() == 8);
    // Valid values after invalid ones:
    engine.setMetronomeTimeSignature(3, 4);
    assert(engine.metronomeTimeSigNumerator() == 3);
    assert(engine.metronomeTimeSigDenominator() == 4);

    // --- Track note retrieval ---
    // Invalid track index returns empty string.
    assert(engine.getTrackNotesAsString(255).empty());
    assert(engine.getTrackNotesAsString(99).empty());

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

    const std::string notes = engine.getTrackNotesAsString(
            static_cast<std::uint8_t>(sequence.tracks.size() - 1));
    // Format: tick,dur,note,vel,prob,rat,nudge;tick,dur,note,vel,prob,rat,nudge
    assert(notes.find("120,180,36,100,88,2,-11") != std::string::npos);
    assert(notes.find("600,90,42,111,123,1,7") != std::string::npos);
    assert(notes.find(";") != std::string::npos); // separator present

    // Track 0 (if it exists) may have no notes, but must not crash.
    if (sequence.tracks.size() > 1) {
        const std::string notes0 = engine.getTrackNotesAsString(0);
        (void)notes0;
    }

    return 0;
}