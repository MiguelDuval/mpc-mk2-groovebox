#pragma once

#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include "MPC/Domain/SampleLayerVelocityRange.h"
#include "MPC/Domain/SampleRegion.h"

namespace mpc::domain {

inline constexpr std::size_t kMaxProgramPads = 16;
inline constexpr std::size_t kMaxSampleLayers = 8;

enum class ProgramType : std::uint8_t {
    Drum,
    Keygroup,
    Plugin,
    Audio
};

enum class TriggerMode : std::uint8_t {
    OneShot,
    NoteOff,
    Loop
};

struct SampleId final {
    std::uint32_t value = 0;

    constexpr bool isAssigned() const noexcept {
        return value != 0;
    }
};

struct SampleRef {
    std::string id;
    std::string name;
    std::string path;
    double sampleRate = 0.0;
    std::int64_t lengthSamples = 0;
    SampleId assetId{};
};

struct SampleLayer {
    SampleId sample{};
    SampleRegion region{};
    float gain = 1.0f;
    float pan = 0.0f;
    float tuningSemitones = 0.0f;
    std::uint8_t velocityMinimum = 0;
    std::uint8_t velocityMaximum = 127;

    // Project-facing sampler metadata retained for later loop/envelope work.
    std::int64_t loopStartSample = 0;
    std::int64_t loopEndSample = 0;
    bool enabled = true;

    constexpr SampleLayerVelocityRange velocityRange() const noexcept {
        return {velocityMinimum, velocityMaximum};
    }

    constexpr bool isAssigned() const noexcept {
        return sample.isAssigned();
    }
};

struct Pad {
    std::uint16_t index = 0;
    std::string name;
    std::uint8_t midiNote = 0;
    TriggerMode triggerMode = TriggerMode::OneShot;

    float tuningSemitones = 0.0f;
    float level = 1.0f;
    float pan = 0.0f;
    float velocityScale = 1.0f;

    float envelopeAttackMs = 0.0f;
    float envelopeDecayMs = 0.0f;
    float envelopeSustain = 1.0f;
    float envelopeReleaseMs = 0.0f;
    float filterCutoffHz = 20000.0f;

    std::uint8_t muteGroup = 0;
    std::uint8_t polyphony = 32;
    bool muted = false;
    bool soloed = false;

    std::array<SampleLayer, kMaxSampleLayers> layers{};

    constexpr SampleLayer& layer(std::size_t layerIndex) noexcept {
        return layers[layerIndex];
    }

    constexpr const SampleLayer& layer(std::size_t layerIndex) const noexcept {
        return layers[layerIndex];
    }
};

struct DrumProgram {
    std::string id;
    std::string name;
    ProgramType type = ProgramType::Drum;
    std::array<Pad, kMaxProgramPads> pads{};

    constexpr Pad& pad(std::size_t padIndex) noexcept {
        return pads[padIndex];
    }

    constexpr const Pad& pad(std::size_t padIndex) const noexcept {
        return pads[padIndex];
    }
};

struct MidiNoteEvent {
    std::int32_t tick = 0;
    std::int32_t durationTicks = 0;
    std::uint8_t note = 0;
    std::uint8_t velocity = 0;
    std::uint8_t probability = 127;
    std::uint8_t ratchet = 1;
};

struct Pattern {
    std::string id;
    std::string name;
    std::int32_t lengthTicks = 3840;
    std::vector<MidiNoteEvent> notes;
};

enum class TrackKind : std::uint8_t {
    Drum,
    Keygroup,
    Plugin,
    Midi,
    Audio
};

struct Track {
    std::string id;
    std::string name;
    ProgramType type = ProgramType::Drum;
    TrackKind kind = TrackKind::Drum;
    std::string programId;
    bool muted = false;
    bool soloed = false;
    bool recordArmed = false;
    std::vector<Pattern> patterns;
};

struct Sequence {
    std::string id;
    std::string name;
    double tempoBpm = 120.0;
    std::int32_t numerator = 4;
    std::int32_t denominator = 4;
    std::int32_t lengthTicks = 3840;
    bool loopEnabled = true;
    std::int32_t loopStartTicks = 0;
    std::int32_t loopEndTicks = 3840;
    std::int32_t quantizeGridTicks = 240;
    std::int32_t swingPercent = 0;
    bool metronomeEnabled = false;
    bool countInEnabled = false;
    std::vector<Track> tracks;
};

struct Project {
    std::string id;
    std::string name;
    std::vector<Sequence> sequences;
    std::vector<DrumProgram> drumPrograms;
    std::vector<SampleRef> samples;
};

} // namespace mpc::domain
