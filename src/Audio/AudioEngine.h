#pragma once

#include "SampleLayerParameters.h"
#include "SampleLayerVelocityRange.h"
#include "SampleRegion.h"
#include "WavSample.h"
#include "AudioTriggerQueue.h"
#include "MPC/Domain/DrumProgram.h"
#include "MPC/MpcProjectState.h"

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
    explicit AudioEngine(mpc::MpcProjectState& projectState);
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
    std::string setPadSampleRegion(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            std::size_t startFrame,
            std::size_t endFrame);
    std::string chopPadSampleToPads(
            std::uint8_t sourcePadIndex,
            std::uint8_t sourceLayerIndex,
            std::uint8_t chopCount);
    std::string cropPadSampleRegion(
            std::uint8_t padIndex,
            std::uint8_t layerIndex);
    SampleRegion padSampleRegion(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::size_t padSampleFrameCount(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::string setPadLayerGain(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            float gain);
    float padLayerGain(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::string setPadLayerTuningSemitones(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            float semitones);
    float padLayerTuningSemitones(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::string setPadLayerPan(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            float pan);
    float padLayerPan(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::string setPadLayerVelocityRange(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            std::uint8_t minimum,
            std::uint8_t maximum);
    SampleLayerVelocityRange padLayerVelocityRange(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;

    // Control-thread snapshot for future sequencer, browser and project-state code.
    mpc::domain::DrumProgram drumProgramSnapshot() const;

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
    std::string setRecordingThreshold(float threshold);
    float recordingThreshold() const;
    std::string startRecording();
    std::string stopRecording();
    // Enables live microphone monitoring independently from recording.
    std::string startMonitor();
    std::string stopMonitor();
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
    static constexpr std::size_t kPadCount = mpc::domain::kMaxProgramPads;
    static constexpr std::size_t kSampleLayerCount = mpc::domain::kMaxSampleLayers;
    using SampleLayerGrid =
            std::array<std::array<std::shared_ptr<const SampleBuffer>, kSampleLayerCount>,
                    kPadCount>;
    using SampleRegionGrid =
            std::array<std::array<SampleRegion, kSampleLayerCount>, kPadCount>;

    class OutputCallback;
    class InputCallback;

    std::string openInputStream();
    mpc::domain::SampleId allocateSampleId();
    std::string stopInputStream();
    std::string stopOutputStream();

    static constexpr std::size_t kMaxRecordingFrames = 960000;
    static constexpr std::size_t kMonitorBufferFrames = 8192;

    std::shared_ptr<const SampleBuffer> sample_;
    std::string sampleDescription_;
    SampleLayerGrid padSamples_{};
    std::array<std::array<std::string, kSampleLayerCount>, kPadCount>
            padSampleDescriptions_{};
    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStream> stream_;
    AudioTriggerQueue triggerQueue_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padTuningMilliSemitones_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padLevelMilli_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padPanMilli_{};
    mpc::MpcProjectState& projectState_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>
            padLayerGainMilli_{};
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>
            padLayerTuningMilliSemitones_{};
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>
            padLayerPanMilli_{};
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>
            padLayerVelocityMin_{};
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>
            padLayerVelocityMax_{};

    std::array<float, kMonitorBufferFrames> monitorSamples_{};
    std::atomic<std::uint32_t> monitorWriteSequence_{0};
    std::atomic<bool> monitorEnabled_{false};
    std::atomic<bool> recordingEnabled_{false};
    std::atomic<bool> recordingArmed_{false};
    std::atomic<std::int32_t> recordingThresholdMilli_{0};

    std::vector<float> recordedSamples_;
    std::atomic<std::uint32_t> recordedFrameCount_{0};
    std::atomic<std::uint32_t> recordingPeakMilli_{0};
    std::atomic<std::int32_t> recordingSampleRate_{0};
    std::atomic<bool> recordingOverflowed_{false};
    std::shared_ptr<InputCallback> inputCallback_;
    std::shared_ptr<oboe::AudioStream> inputStream_;
};

} // namespace mpc::audio
