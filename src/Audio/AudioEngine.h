#pragma once

#include "WavSample.h"

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <span>
#include <string>
#include <vector>

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
    std::string loadSampleForPadLayer(
            std::span<const std::uint8_t> bytes,
            std::uint8_t padIndex,
            std::uint8_t layerIndex);
    std::string setPadTuningSemitones(std::uint8_t padIndex, float semitones);
    float padTuningSemitones(std::uint8_t padIndex) const;
    std::string setPadLevel(std::uint8_t padIndex, float level);
    float padLevel(std::uint8_t padIndex) const;
    std::string setPadPan(std::uint8_t padIndex, float pan);
    float padPan(std::uint8_t padIndex) const;
    std::string start();
    std::string stop();
    std::string status() const;

    // Starts a bounded microphone capture buffer in RAM.
    // The capture callback writes only into preallocated storage.
    std::string startRecording();
    std::string stopRecording();
    // Promotes the last stopped RAM recording into the selected pad/layer.
    // The operation is control-thread only; realtime callbacks never resize/copy this buffer.
    std::string assignRecordingToPadLayer(
            std::uint8_t padIndex,
            std::uint8_t layerIndex);
    std::string recordingStatus() const;

    // Queues a single-shot musical trigger for one physical MPC pad.
    // The request is consumed by the realtime audio callback without locks.
    void triggerPad(std::uint8_t padIndex, std::uint8_t velocity);

private:
    static constexpr std::size_t kPadCount = 16;
    static constexpr std::size_t kSampleLayerCount = 8;
    using SampleLayerGrid =
            std::array<std::array<std::shared_ptr<const SampleBuffer>, kSampleLayerCount>,
                    kPadCount>;

    class OutputCallback;
    class InputCallback;

    static constexpr std::size_t kMaxRecordingFrames = 960000;
    static constexpr std::size_t kMonitorBufferFrames = 8192;

    std::shared_ptr<const SampleBuffer> sample_;
    std::string sampleDescription_;
    SampleLayerGrid padSamples_{};
    std::array<std::array<std::string, kSampleLayerCount>, kPadCount>
            padSampleDescriptions_{};
    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStream> stream_;
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerSequence_{};
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerVelocity_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padTuningMilliSemitones_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padLevelMilli_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padPanMilli_{};

    std::array<float, kMonitorBufferFrames> monitorSamples_{};
    std::atomic<std::uint32_t> monitorWriteSequence_{0};
    std::atomic<bool> monitorEnabled_{false};

    std::vector<float> recordedSamples_;
    std::atomic<std::uint32_t> recordedFrameCount_{0};
    std::atomic<std::uint32_t> recordingPeakMilli_{0};
    std::atomic<std::int32_t> recordingSampleRate_{0};
    std::atomic<bool> recordingOverflowed_{false};
    std::shared_ptr<InputCallback> inputCallback_;
    std::shared_ptr<oboe::AudioStream> inputStream_;
};

} // namespace mpc::audio
