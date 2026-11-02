#pragma once

#include "SampleLayerParameters.h"
#include "SampleEnvelope.h"
#include "SampleLayerVelocityRange.h"
#include "SampleRegion.h"
#include "WavSample.h"
#include "Waveform.h"
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
    // Lock-free snapshot of the active voice position; -1 means idle.
    std::int64_t padSamplePlaybackCursorFrame(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const noexcept;
    std::size_t padSampleFrameCount(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::uint32_t padSampleRate(
            std::uint8_t padIndex,
            std::uint8_t layerIndex) const;
    std::vector<WaveformPeak> padWaveformPeaks(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            std::size_t pointCount) const;
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

    // Track mixer level/pan (Channel Mixer). Thread-safe control-thread access.
    std::string setTrackLevel(std::uint8_t trackIndex, float level);
    float trackLevel(std::uint8_t trackIndex) const;
    std::string setTrackPan(std::uint8_t trackIndex, float pan);
    float trackPan(std::uint8_t trackIndex) const;

    // Realtime expression from the pressure-sensitive MkII pads.
    // Ephemeral only: does not modify project state.
    void setPadAftertouch(
            std::uint8_t padIndex,
            std::uint8_t pressure) noexcept;
    std::string setPadEnvelopeParameters(
            std::uint8_t padIndex,
            float attackMs,
            float decayMs,
            float sustain,
            float releaseMs);
    SampleEnvelopeParameters padEnvelopeParameters(
            std::uint8_t padIndex) const;
    std::string setPadFilterCutoff(
            std::uint8_t padIndex,
            float cutoffHz);
    float padFilterCutoff(
            std::uint8_t padIndex) const;
    struct OutputConfiguration final {
        int deviceId = -1;
        int sampleRate = 0;
        int bufferSizeFrames = 0;
        bool exclusive = false;
        bool lowLatency = true;
    };

    struct InputConfiguration final {
        int deviceId = -1;
    };

    struct PadEditSnapshot final {
        domain::Pad state{};
        std::array<std::shared_ptr<const SampleBuffer>, mpc::domain::kMaxSampleLayers> samples{};
        std::array<std::string, mpc::domain::kMaxSampleLayers> descriptions{};
    };

    PadEditSnapshot capturePadEditSnapshot(
            std::uint8_t padIndex) const;
    bool applyPadEditSnapshot(
            std::uint8_t padIndex,
            const PadEditSnapshot& snapshot);
    std::string copyPadToPads(
            std::uint8_t sourcePadIndex,
            std::uint16_t destinationMask);
    // Exports a pad/layer sample to PCM 16-bit WAV bytes.
    // Returns empty string on success with outBytes populated, or an error
    // message string on failure (invalid pad/layer, no sample assigned).
    std::string exportPadSampleToWav(
            std::uint8_t padIndex,
            std::uint8_t layerIndex,
            std::vector<std::uint8_t>& outBytes);
    // Exports the last stopped RAM recording to PCM 16-bit WAV bytes.
    // Returns empty string on success with outBytes populated, or an error
    // message string on failure (no recording available).
    std::string exportRecordingToWav(
            std::vector<std::uint8_t>& outBytes);
    std::string deletePadAssignments(
            std::uint16_t padMask);

    std::string configureOutput(const OutputConfiguration& configuration);
    std::string configureInputDevice(const InputConfiguration& configuration);
    OutputConfiguration outputConfiguration() const;
    InputConfiguration inputConfiguration() const;
    std::string testOutputTone();
    std::string start();
    std::string stop();
    std::string status() const;
    bool clearRuntimeSamplesForProjectReplacement() noexcept;
    void refreshActiveProgramRuntimeProjection() noexcept;

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
    std::uint32_t recordingFrameCount() const;
    std::uint32_t recordingSampleRate() const;
    float recordingPeak() const;
    std::vector<WaveformPeak> recordingWaveformPeaks(
            std::size_t pointCount) const;
    // Clears any recorded audio data from the last recording session.
    // Safe to call from the control thread at any time.
    std::string clearRecording();

    // Queues a single-shot musical trigger for one physical MPC pad.
    // offsetFrames is relative to the next output callback.
    void triggerPad(std::uint8_t padIndex, std::uint8_t velocity);
    void triggerPadAtOffset(
            std::uint8_t padIndex,
            std::uint8_t velocity,
            std::int32_t offsetFrames);

    AudioTriggerQueue& triggerQueue() noexcept {
        return triggerQueue_;
    }

    std::int32_t outputSampleRate() const noexcept {
        if (stream_ != nullptr) {
            return stream_->getSampleRate();
        }
        return outputConfiguration_.sampleRate > 0
                ? outputConfiguration_.sampleRate
                : 48000;
    }

    // Metronome (VIS-076 / G-006). Public control-thread API; realtime
    // callback reads atomic snapshots only.
    std::string setMetronomeEnabled(bool enabled);
    bool metronomeEnabled() const;
    std::string setMetronomeVolume(float volume);   // 0.0..1.0
    float metronomeVolume() const;
    std::string setMetronomeSound(int soundIndex);  // 0=click,1=beep,2=wood,3=none
    int metronomeSound() const;
    // Snapshot tempo from the control thread; read atomically in the callback.
    void setMetronomeTempo(double bpm) noexcept;
    double metronomeTempo() const noexcept;
    // Snapshot time signature from the control thread; read atomically in the callback.
    void setMetronomeTimeSignature(int numerator, int denominator) noexcept;
    int metronomeTimeSigNumerator() const noexcept;
    int metronomeTimeSigDenominator() const noexcept;

    // List Edit (VIS-026): get all MIDI note events for a track as a
    // semicolon-separated string: "tick,dur,note,vel,prob,rat,nudge;..."
    std::string getTrackNotesAsString(std::uint8_t trackIndex) const;

private:
    static constexpr std::size_t kPadCount = mpc::domain::kMaxProgramPads;
    static constexpr std::size_t kSampleLayerCount = mpc::domain::kMaxSampleLayers;
    using SampleLayerGrid =
            std::array<std::array<std::shared_ptr<const SampleBuffer>, kSampleLayerCount>,
                    kPadCount>;
    using SampleRegionGrid =
            std::array<std::array<SampleRegion, kSampleLayerCount>, kPadCount>;
    using SamplePlaybackCursorGrid =
            std::array<std::array<std::atomic<std::uint32_t>, kSampleLayerCount>, kPadCount>;

    class OutputCallback;
    class InputCallback;

    std::string openInputStream();
    mpc::domain::SampleId allocateSampleId();
    std::string stopInputStream();
    std::string stopOutputStream();
    void syncPadRuntimeProjection(
            std::uint8_t padIndex) noexcept;
    static std::string validateOutputConfiguration(
            const OutputConfiguration& configuration);
    static std::string validateInputConfiguration(
            const InputConfiguration& configuration);

    static constexpr std::size_t kMaxRecordingFrames = 960000;
    static constexpr std::size_t kMonitorBufferFrames = 8192;
    static constexpr std::size_t kRecordingWaveformBinCount = 2048;

    std::shared_ptr<const SampleBuffer> sample_;
    std::string sampleDescription_;
    SampleLayerGrid padSamples_{};
    std::array<std::array<std::string, kSampleLayerCount>, kPadCount>
            padSampleDescriptions_{};
    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStreamErrorCallback> outputErrorCallback_;
    std::shared_ptr<oboe::AudioStream> stream_;
    std::atomic<std::uint64_t> outputCallbackCount_{0};
    std::atomic<std::uint64_t> outputCallbackFrames_{0};
    SamplePlaybackCursorGrid samplePlaybackCursorFrames_{};
    std::atomic<std::uint32_t> outputPeakMilli_{0};
    std::atomic<std::uint32_t> testToneRequestFrames_{0};
    std::atomic<std::int32_t> outputLastErrorCode_{
            static_cast<std::int32_t>(oboe::Result::OK)};
    AudioTriggerQueue triggerQueue_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padTuningMilliSemitones_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padLevelMilli_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padPanMilli_{};
    std::array<std::atomic<std::uint8_t>, kPadCount> padAftertouch_{};
    std::array<std::atomic<std::uint32_t>, kPadCount> padAftertouchFilterMicro_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padEnvelopeAttackMilliMs_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padEnvelopeDecayMilliMs_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padEnvelopeSustainMilli_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padEnvelopeReleaseMilliMs_{};
    std::array<std::atomic<std::int32_t>, kPadCount> padFilterCutoffMilliHz_{};
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
    // Metronome state (VIS-076 / G-006). Control-thread safe; read in the
    // realtime output callback to generate click accents on beat 1.
    std::atomic<bool> metronomeEnabled_{false};
    std::atomic<std::int32_t> metronomeVolumeMilli_{800};   // 0..1000 -> 0..1.0
    std::atomic<std::int32_t> metronomeSound_{0};           // 0=click,1=beep,2=wood,3=none
    // Atomic snapshots of tempo/time-signature for the realtime callback.
    // Written on the control thread; read atomically in the callback.
    std::atomic<double> metronomeTempoBpm_{120.0};
    std::atomic<int> metronomeTimeSigNumerator_{4};
    std::atomic<int> metronomeTimeSigDenominator_{4};
    // Atomic reset request from the control thread; consumed by the realtime
    // callback to reset its owned metronome phase/beat/frame state.
    std::atomic<bool> metronomeResetRequested_{false};
    std::atomic<bool> recordingEnabled_{false};
    std::atomic<bool> recordingArmed_{false};
    std::atomic<std::int32_t> recordingThresholdMilli_{0};
    std::array<std::atomic<std::int32_t>, kRecordingWaveformBinCount>
            recordingWaveformPeakMilli_{};

    std::vector<float> recordedSamples_;
    std::atomic<std::uint32_t> recordedFrameCount_{0};
    std::atomic<std::uint32_t> recordingPeakMilli_{0};
    std::atomic<std::int32_t> recordingSampleRate_{0};
    std::atomic<bool> recordingOverflowed_{false};
    std::shared_ptr<InputCallback> inputCallback_;
    std::shared_ptr<oboe::AudioStream> inputStream_;

    OutputConfiguration outputConfiguration_{};
    InputConfiguration inputConfiguration_{};
};

} // namespace mpc::audio
