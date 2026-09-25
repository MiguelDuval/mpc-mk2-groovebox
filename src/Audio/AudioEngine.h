#pragma once

#include "WavSample.h"

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <span>
#include <string>

#include <oboe/Oboe.h>

namespace mpc::audio {

class AudioEngine final {
public:
    AudioEngine();
    ~AudioEngine();

    AudioEngine(const AudioEngine&) = delete;
    AudioEngine& operator=(const AudioEngine&) = delete;

    static AudioEngine& instance();

    std::string loadSample(std::span<const std::uint8_t> bytes);
    std::string loadSampleForPad(
            std::span<const std::uint8_t> bytes,
            std::uint8_t padIndex);
    void setPadTuningSemitones(std::uint8_t padIndex, float semitones);
    std::string start();
    std::string stop();
    std::string status() const;

    // Queues a single-shot musical trigger for one physical MPC pad.
    // The request is consumed by the realtime audio callback without locks.
    void triggerPad(std::uint8_t padIndex, std::uint8_t velocity);

private:
    static constexpr std::size_t kPadCount = 16;

    class OutputCallback;

    std::shared_ptr<const SampleBuffer> sample_;
    std::string sampleDescription_;
    std::array<std::shared_ptr<const SampleBuffer>, kPadCount> padSamples_{};
    std::array<std::string, kPadCount> padSampleDescriptions_{};
    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStream> stream_;
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerSequence_{};
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerVelocity_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padTuningMilliSemitones_{};
};

} // namespace mpc::audio
