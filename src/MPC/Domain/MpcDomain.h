#pragma once

#include <array>
#include <cstdint>
#include <string>
#include <vector>

namespace mpc::domain {

inline constexpr std::size_t kMaxProgramPads = 128;
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

struct SampleRef {
    std::string id;
    std::string name;
    std::string path;
    double sampleRate = 0.0;
    std::int64_t lengthSamples = 0;
};

struct SampleLayer {
    SampleRef sample;
    std::int64_t startSample = 0;
    std::int64_t endSample = 0;
    std::int64_t loopStartSample = 0;
    std::int64_t loopEndSample = 0;
    float volumeDb = 0.0f;
    float pan = 0.0f;
    float tuneSemitones = 0.0f;
    std::uint8_t velocityMin = 0;
    std::uint8_t velocityMax = 127;
    bool enabled = false;
};

struct Pad {
    std::uint16_t index = 0;
    std::string name;
    std::uint8_t midiNote = 0;
    TriggerMode triggerMode = TriggerMode::OneShot;

    float volumeDb = 0.0f;
    float pan = 0.0f;
    float tuneSemitones = 0.0f;
    float velocityScale = 1.0f;

    std::uint8_t muteGroup = 0;
    std::uint8_t polyphony = 32;
    bool muted = false;
    bool soloed = false;

    std::array<SampleLayer, kMaxSampleLayers> layers{};
};

struct DrumProgram {
    std::string id;
    std::string name;
    ProgramType type = ProgramType::Drum;
    std::array<Pad, kMaxProgramPads> pads{};
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

struct Track {
    std::string id;
    std::string name;
    ProgramType type = ProgramType::Drum;
    std::string programId;
    std::vector<Pattern> patterns;
};

struct Sequence {
    std::string id;
    std::string name;
    double tempoBpm = 120.0;
    std::int32_t numerator = 4;
    std::int32_t denominator = 4;
    std::int32_t lengthTicks = 3840;
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
