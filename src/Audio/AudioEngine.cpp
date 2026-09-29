#include "AudioEngine.h"
#include "MPC/MpcCore.h"
#include "RecordingThreshold.h"
#include "SampleChop.h"
#include "SampleLayerParameters.h"
#include "SampleCrop.h"
#include "SampleEnvelope.h"
#include "OnePoleLowPass.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <utility>

namespace {

constexpr std::size_t kPadCount = mpc::domain::kMaxProgramPads;
constexpr std::size_t kSampleLayerCount = mpc::domain::kMaxSampleLayers;
constexpr std::uint32_t kMaxRecordingFrames = 960000u;
constexpr std::size_t kMonitorBufferFrames = 8192;
constexpr float kMonitorGain = 0.65f;
constexpr float kPadAmplitude = 0.85f;

const char* resultText(oboe::Result result) {
    return oboe::convertToText(result);
}

const char* streamStateText(oboe::StreamState state) {
    return oboe::convertToText(state);
}

} // namespace

namespace mpc::audio {

class AudioEngine::InputCallback final : public oboe::AudioStreamDataCallback {
public:
    explicit InputCallback(AudioEngine& owner)
            : owner_(owner) {
    }

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* audioStream,
            void* audioData,
            int32_t numFrames) override {
        if (audioStream == nullptr || audioData == nullptr || numFrames <= 0) {
            return oboe::DataCallbackResult::Continue;
        }

        const int32_t channelCount = audioStream->getChannelCount();
        if (channelCount <= 0) {
            return oboe::DataCallbackResult::Continue;
        }

        bool recordingEnabled =
                owner_.recordingEnabled_.load(std::memory_order_acquire);
        const bool recordingArmed =
                owner_.recordingArmed_.load(std::memory_order_acquire);
        const float recordingThreshold =
                static_cast<float>(
                    owner_.recordingThresholdMilli_.load(
                        std::memory_order_relaxed))
                / 1000.0f;
        const bool monitorEnabled =
                owner_.monitorEnabled_.load(std::memory_order_acquire);
        const auto* input = static_cast<const float*>(audioData);

        auto recordingFrameCount =
                owner_.recordedFrameCount_.load(std::memory_order_relaxed);

        const auto monitorFrameStart =
                monitorEnabled
                    ? owner_.monitorWriteSequence_.load(
                            std::memory_order_relaxed)
                    : 0u;

        std::uint32_t peakMilli =
                recordingEnabled
                    ? owner_.recordingPeakMilli_.load(
                            std::memory_order_relaxed)
                    : 0u;
        bool recordingOverflowed = false;

        for (std::uint32_t frame = 0;
                frame < static_cast<std::uint32_t>(numFrames);
                ++frame) {
            float mono = 0.0f;
            for (int32_t channel = 0; channel < channelCount; ++channel) {
                mono += input[
                        static_cast<std::size_t>(frame) * channelCount
                        + static_cast<std::size_t>(channel)];
            }
            mono /= static_cast<float>(channelCount);
            mono = std::clamp(mono, -1.0f, 1.0f);

            if (!recordingEnabled
                    && recordingArmed
                    && recordingThresholdCrossed(mono, recordingThreshold)) {
                recordingEnabled = true;
                owner_.recordingArmed_.store(
                        false,
                        std::memory_order_release);
                owner_.recordingEnabled_.store(
                        true,
                        std::memory_order_release);
            }

            if (recordingEnabled) {
                if (recordingFrameCount < kMaxRecordingFrames) {
                    owner_.recordedSamples_[recordingFrameCount] = mono;

                    const auto absValue =
                            std::min(1.0f, std::abs(mono));
                    const auto samplePeakMilli =
                            static_cast<std::uint32_t>(
                                    std::lround(absValue * 1000.0f));
                    peakMilli = std::max(peakMilli, samplePeakMilli);

                    const auto waveformBin = std::min(
                            static_cast<std::size_t>(
                                    recordingFrameCount
                                    * AudioEngine::kRecordingWaveformBinCount
                                    / kMaxRecordingFrames),
                            AudioEngine::kRecordingWaveformBinCount - 1);
                    auto& waveformPeak =
                            owner_.recordingWaveformPeakMilli_[waveformBin];
                    const auto previousPeak =
                            waveformPeak.load(std::memory_order_relaxed);
                    if (samplePeakMilli > previousPeak) {
                        waveformPeak.store(
                                static_cast<std::int32_t>(samplePeakMilli),
                                std::memory_order_relaxed);
                    }

                    ++recordingFrameCount;
                } else {
                    recordingOverflowed = true;
                }
            }

            if (monitorEnabled) {
                const auto monitorIndex =
                        (monitorFrameStart + frame) % kMonitorBufferFrames;
                owner_.monitorSamples_[monitorIndex] = mono;
            }
        }

        if (recordingEnabled) {
            owner_.recordingPeakMilli_.store(
                    peakMilli,
                    std::memory_order_relaxed);
            owner_.recordedFrameCount_.store(
                    recordingFrameCount,
                    std::memory_order_release);

            if (recordingOverflowed) {
                owner_.recordingOverflowed_.store(
                        true,
                        std::memory_order_release);
            }
        }

        if (monitorEnabled) {
            owner_.monitorWriteSequence_.store(
                    monitorFrameStart + static_cast<std::uint32_t>(numFrames),
                    std::memory_order_release);
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    AudioEngine& owner_;
};

class AudioEngine::OutputCallback final : public oboe::AudioStreamDataCallback {
public:
    OutputCallback(
            AudioEngine::SampleLayerGrid samples,
            AudioEngine::SampleRegionGrid regions,
            AudioTriggerQueue& triggerQueue,
            std::array<std::atomic<std::int32_t>, kPadCount>& tuningMilliSemitones,
            std::array<std::atomic<std::int32_t>, kPadCount>& levelMilli,
            std::array<std::atomic<std::int32_t>, kPadCount>& panMilli,
            std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerGainMilli,
            std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerTuningMilliSemitones,
            std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerPanMilli,
            std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerVelocityMin,
            std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerVelocityMax,
            std::array<std::atomic<std::int32_t>, kPadCount>& envelopeAttackMilliMs,
            std::array<std::atomic<std::int32_t>, kPadCount>& envelopeDecayMilliMs,
            std::array<std::atomic<std::int32_t>, kPadCount>& envelopeSustainMilli,
            std::array<std::atomic<std::int32_t>, kPadCount>& envelopeReleaseMilliMs,
            std::array<std::atomic<std::int32_t>, kPadCount>& filterCutoffMilliHz,
            std::array<float, kMonitorBufferFrames>& monitorSamples,
            std::atomic<std::uint32_t>& monitorWriteSequence,
            std::atomic<bool>& monitorEnabled)
            : samples_(std::move(samples)),
              regions_(std::move(regions)),
              triggerQueue_(triggerQueue),
              tuningMilliSemitones_(tuningMilliSemitones),
              levelMilli_(levelMilli),
              panMilli_(panMilli),
              layerGainMilli_(layerGainMilli),
              layerTuningMilliSemitones_(layerTuningMilliSemitones),
              layerPanMilli_(layerPanMilli),
              layerVelocityMin_(layerVelocityMin),
              layerVelocityMax_(layerVelocityMax),
              envelopeAttackMilliMs_(envelopeAttackMilliMs),
              envelopeDecayMilliMs_(envelopeDecayMilliMs),
              envelopeSustainMilli_(envelopeSustainMilli),
              envelopeReleaseMilliMs_(envelopeReleaseMilliMs),
              filterCutoffMilliHz_(filterCutoffMilliHz),
              monitorSamples_(monitorSamples),
              monitorWriteSequence_(monitorWriteSequence),
              monitorEnabled_(monitorEnabled) {
    }

    oboe::DataCallbackResult onAudioReady(
            oboe::AudioStream* audioStream,
            void* audioData,
            int32_t numFrames) override {
        if (audioStream == nullptr || audioData == nullptr || numFrames <= 0) {
            return oboe::DataCallbackResult::Continue;
        }

        const int32_t channelCount = audioStream->getChannelCount();
        const int32_t sampleRate = audioStream->getSampleRate();

        if (channelCount <= 0 || sampleRate <= 0) {
            return oboe::DataCallbackResult::Continue;
        }

        auto* output = static_cast<float*>(audioData);

        while (pendingTriggerCount_ < pendingTriggerEvents_.size()
                && triggerQueue_.tryDequeue(
                        pendingTriggerEvents_[pendingTriggerCount_])) {
            auto& pending =
                    pendingTriggerEvents_[pendingTriggerCount_];
            pending.offsetFrames = std::max(pending.offsetFrames, 0);
            ++pendingTriggerCount_;
        }

        const bool monitorEnabled =
                monitorEnabled_.load(std::memory_order_acquire);
        if (monitorEnabled && !monitorWasEnabled_) {
            monitorReadSequence_ =
                    monitorWriteSequence_.load(std::memory_order_acquire);
        }
        monitorWasEnabled_ = monitorEnabled;

        if (monitorEnabled) {
            const std::uint32_t writeSequence =
                    monitorWriteSequence_.load(std::memory_order_acquire);
            const std::uint32_t available =
                    writeSequence - monitorReadSequence_;
            if (available > kMonitorBufferFrames) {
                monitorReadSequence_ =
                        writeSequence - kMonitorBufferFrames;
            }
        }

        for (int32_t frame = 0; frame < numFrames; ++frame) {
            for (std::size_t index = 0; index < pendingTriggerCount_;) {
                auto& pending = pendingTriggerEvents_[index];
                if (pending.offsetFrames > frame) {
                    ++index;
                    continue;
                }

                if (pending.padIndex < kPadCount && pending.velocity != 0) {
                    startVoice(
                            pending.padIndex,
                            pending.velocity,
                            sampleRate);
                }

                --pendingTriggerCount_;
                if (index != pendingTriggerCount_) {
                    pendingTriggerEvents_[index] =
                            pendingTriggerEvents_[pendingTriggerCount_];
                }
            }

            float left = 0.0f;
            float right = 0.0f;

            if (monitorEnabled) {
                const std::uint32_t writeSequence =
                        monitorWriteSequence_.load(std::memory_order_acquire);
                const std::uint32_t available =
                        writeSequence - monitorReadSequence_;
                if (available > 0u && available <= kMonitorBufferFrames) {
                    const float monitorSample =
                            monitorSamples_[monitorReadSequence_
                                            % kMonitorBufferFrames];
                    left += monitorSample * kMonitorGain;
                    right += monitorSample * kMonitorGain;
                    ++monitorReadSequence_;
                } else if (available > kMonitorBufferFrames) {
                    monitorReadSequence_ =
                            writeSequence - kMonitorBufferFrames;
                }
            }

            for (std::size_t pad = 0; pad < kPadCount; ++pad) {
                for (std::size_t voiceIndex = 0;
                        voiceIndex < kMaxPadVoices;
                        ++voiceIndex) {
                    auto& voice = voices_[pad][voiceIndex];
                    if (!voice.active) {
                        continue;
                    }

                    bool anyActiveLayer = false;

                    for (std::size_t layer = 0;
                            layer < kSampleLayerCount;
                            ++layer) {
                        auto& layerVoice = voice.layers[layer];
                        if (!layerVoice.active) {
                            continue;
                        }

                        const auto& sample = samples_[pad][layer];
                        const auto& region = regions_[pad][layer];
                        if (sample == nullptr || sample->frameCount() == 0
                                || sample->channelCount == 0) {
                            layerVoice.active = false;
                            continue;
                        }

                        const std::size_t sourceFrame =
                                static_cast<std::size_t>(layerVoice.position);

                        if (sourceFrame >= region.endFrame
                                || sourceFrame >= sample->frameCount()) {
                            layerVoice.active = false;
                            continue;
                        }

                        const std::size_t nextFrame =
                                std::min(sourceFrame + 1, region.endFrame - 1);
                        const float fraction =
                                static_cast<float>(
                                    layerVoice.position
                                    - static_cast<double>(sourceFrame));

                        const SampleEnvelopeParameters envelope{
                                static_cast<float>(
                                        envelopeAttackMilliMs_[pad]
                                                .load(std::memory_order_relaxed))
                                        / 1000.0f,
                                static_cast<float>(
                                        envelopeDecayMilliMs_[pad]
                                                .load(std::memory_order_relaxed))
                                        / 1000.0f,
                                static_cast<float>(
                                        envelopeSustainMilli_[pad]
                                                .load(std::memory_order_relaxed))
                                        / 1000.0f,
                                static_cast<float>(
                                        envelopeReleaseMilliMs_[pad]
                                                .load(std::memory_order_relaxed))
                                        / 1000.0f};
                        const float envelopeGain =
                                sampleEnvelopeGain(
                                        static_cast<std::size_t>(layerVoice.ageFrames),
                                        static_cast<std::size_t>(layerVoice.lifeFrames),
                                        sampleRate,
                                        envelope);

                        if (sample->channelCount == 1) {
                            const float sample0 =
                                    sample->sampleAt(sourceFrame, 0);
                            const float sample1 =
                                    sample->sampleAt(nextFrame, 0);
                            const float filteredValue =
                                    layerVoice.filterLeft.process(
                                            sample0 + (sample1 - sample0) * fraction);
                            const float value =
                                    filteredValue * envelopeGain;
                            left += value * voice.gain * layerVoice.gain
                                    * voice.leftGain * layerVoice.leftGain;
                            right += value * voice.gain * layerVoice.gain
                                    * voice.rightGain * layerVoice.rightGain;
                        } else {
                            const float left0 =
                                    sample->sampleAt(sourceFrame, 0);
                            const float left1 =
                                    sample->sampleAt(nextFrame, 0);
                            const float right0 =
                                    sample->sampleAt(sourceFrame, 1);
                            const float right1 =
                                    sample->sampleAt(nextFrame, 1);

                            const float filteredLeft =
                                    layerVoice.filterLeft.process(
                                            left0 + (left1 - left0) * fraction);
                            const float filteredRight =
                                    layerVoice.filterRight.process(
                                            right0 + (right1 - right0) * fraction);

                            left += filteredLeft * envelopeGain
                                    * voice.gain * layerVoice.gain
                                    * voice.leftGain;
                            right += filteredRight * envelopeGain
                                    * voice.gain * layerVoice.gain
                                    * voice.rightGain;
                        }

                        ++layerVoice.ageFrames;
                        anyActiveLayer = true;
                    }

                    voice.active = anyActiveLayer;
                }
            }

            const float mono =
                    std::clamp((left + right) * 0.5f, -0.98f, 0.98f);

            for (int32_t channel = 0; channel < channelCount; ++channel) {
                if (channel == 0) {
                    output[frame * channelCount + channel] =
                            std::clamp(left, -0.98f, 0.98f);
                } else if (channel == 1) {
                    output[frame * channelCount + channel] =
                            std::clamp(right, -0.98f, 0.98f);
                } else {
                    output[frame * channelCount + channel] = mono;
                }
            }
        }

        for (std::size_t index = 0; index < pendingTriggerCount_; ++index) {
            pendingTriggerEvents_[index].offsetFrames -= numFrames;
        }

        return oboe::DataCallbackResult::Continue;
    }
private:
    static constexpr std::size_t kMaxPadVoices = 8;

    void startVoice(
            std::uint8_t padIndex,
            std::uint8_t velocity,
            int32_t sampleRate) noexcept {
        std::size_t selectedIndex = nextVoiceIndex_[padIndex];
        for (std::size_t offset = 0; offset < kMaxPadVoices; ++offset) {
            const std::size_t candidate =
                    (selectedIndex + offset) % kMaxPadVoices;
            if (!voices_[padIndex][candidate].active) {
                selectedIndex = candidate;
                break;
            }
        }

        nextVoiceIndex_[padIndex] =
                static_cast<std::uint8_t>((selectedIndex + 1) % kMaxPadVoices);

        auto& voice = voices_[padIndex][selectedIndex];
        const float velocityGain =
                static_cast<float>(std::min<std::uint8_t>(velocity, 127u))
                / 127.0f;
        const float level =
                static_cast<float>(
                    levelMilli_[padIndex].load(std::memory_order_relaxed))
                / 1000.0f;
        const float pan =
                static_cast<float>(
                    panMilli_[padIndex].load(std::memory_order_relaxed))
                / 1000.0f;
        const float filterCutoffHz =
                static_cast<float>(
                        filterCutoffMilliHz_[padIndex]
                                .load(std::memory_order_relaxed))
                / 1000.0f;

        voice.gain = velocityGain * level * kPadAmplitude;
        voice.leftGain = pan > 0.0f ? 1.0f - pan : 1.0f;
        voice.rightGain = pan < 0.0f ? 1.0f + pan : 1.0f;

        bool anyLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            auto& layerVoice = voice.layers[layer];
            const auto& sample = samples_[padIndex][layer];
            const auto& region = regions_[padIndex][layer];

            layerVoice.position = static_cast<double>(region.startFrame);
            layerVoice.gain = sampleLayerGainFromMilli(
                    layerGainMilli_[padIndex][layer].load(
                            std::memory_order_relaxed));
            layerVoice.tuningSemitones = sampleLayerTuningFromMilli(
                    layerTuningMilliSemitones_[padIndex][layer].load(
                            std::memory_order_relaxed));
            const float layerPan = sampleLayerPanFromMilli(
                    layerPanMilli_[padIndex][layer].load(
                            std::memory_order_relaxed));
            layerVoice.leftGain =
                    layerPan > 0.0f ? 1.0f - layerPan : 1.0f;
            layerVoice.rightGain =
                    layerPan < 0.0f ? 1.0f + layerPan : 1.0f;
            const auto velocityMin =
                    static_cast<std::uint8_t>(
                            std::clamp(
                                    layerVelocityMin_[padIndex][layer].load(
                                            std::memory_order_relaxed),
                                    0,
                                    127));
            const auto velocityMax =
                    static_cast<std::uint8_t>(
                            std::clamp(
                                    layerVelocityMax_[padIndex][layer].load(
                                            std::memory_order_relaxed),
                                    0,
                                    127));
            const SampleLayerVelocityRange velocityRange{
                    velocityMin,
                    velocityMax};
            const auto clampedVelocity =
                    static_cast<std::uint8_t>(std::min<std::uint8_t>(velocity, 127u));
            layerVoice.active = sample != nullptr
                    && sample->frameCount() > 0
                    && sample->channelCount > 0
                    && region.isValidFor(sample->frameCount())
                    && velocityRange.contains(clampedVelocity);
            anyLayer = anyLayer || layerVoice.active;
        }

        if (!anyLayer) {
            voice.active = false;
            return;
        }

        const float tuningSemitones =
                static_cast<float>(
                    tuningMilliSemitones_[padIndex].load(
                        std::memory_order_relaxed))
                / 1000.0f;
        const float semitoneRatio =
                std::pow(2.0f, tuningSemitones / 12.0f);

        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            const auto& sample = samples_[padIndex][layer];
            auto& layerVoice = voice.layers[layer];
            if (!layerVoice.active || sample == nullptr) {
                continue;
            }

            const float sampleToOutputRate =
                    static_cast<float>(sample->sampleRate)
                    / static_cast<float>(sampleRate);
            const float layerSemitoneRatio =
                    std::pow(2.0f, layerVoice.tuningSemitones / 12.0f);
            layerVoice.positionStep =
                    sampleToOutputRate * semitoneRatio * layerSemitoneRatio;
            layerVoice.ageFrames = 0;
            layerVoice.lifeFrames = static_cast<std::uint64_t>(
                    std::max(
                            1.0,
                            std::ceil(
                                    static_cast<double>(regions_[padIndex][layer].frameCount())
                                    / static_cast<double>(
                                            std::max(
                                                    0.000001f,
                                                    layerVoice.positionStep)))));
            layerVoice.filterLeft.configure(filterCutoffHz, sampleRate);
            layerVoice.filterRight.configure(filterCutoffHz, sampleRate);
            layerVoice.filterLeft.reset();
            layerVoice.filterRight.reset();
        }

        voice.active = true;
    }

    struct PadVoice {
        struct LayerVoice {
            double position = 0.0;
            float positionStep = 0.0f;
            float gain = 1.0f;
            float tuningSemitones = 0.0f;
            float leftGain = 1.0f;
            float rightGain = 1.0f;
            std::uint64_t ageFrames = 0;
            std::uint64_t lifeFrames = 1;
            OnePoleLowPass filterLeft;
            OnePoleLowPass filterRight;
            bool active = false;
        };

        std::array<LayerVoice, kSampleLayerCount> layers{};
        float gain = 0.0f;
        float leftGain = 1.0f;
        float rightGain = 1.0f;
        bool active = false;
    };

    AudioEngine::SampleLayerGrid samples_;
    AudioEngine::SampleRegionGrid regions_;
    AudioTriggerQueue& triggerQueue_;
    std::array<std::atomic<std::int32_t>, kPadCount>& tuningMilliSemitones_;
    std::array<std::atomic<std::int32_t>, kPadCount>& levelMilli_;
    std::array<std::atomic<std::int32_t>, kPadCount>& panMilli_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerGainMilli_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerTuningMilliSemitones_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerPanMilli_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerVelocityMin_;
    std::array<std::array<std::atomic<std::int32_t>, kSampleLayerCount>, kPadCount>& layerVelocityMax_;
    std::array<std::atomic<std::int32_t>, kPadCount>& envelopeAttackMilliMs_;
    std::array<std::atomic<std::int32_t>, kPadCount>& envelopeDecayMilliMs_;
    std::array<std::atomic<std::int32_t>, kPadCount>& envelopeSustainMilli_;
    std::array<std::atomic<std::int32_t>, kPadCount>& envelopeReleaseMilliMs_;
    std::array<std::atomic<std::int32_t>, kPadCount>& filterCutoffMilliHz_;
    std::array<float, kMonitorBufferFrames>& monitorSamples_;
    std::atomic<std::uint32_t>& monitorWriteSequence_;
    std::atomic<bool>& monitorEnabled_;
    std::uint32_t monitorReadSequence_ = 0;
    bool monitorWasEnabled_ = false;
    std::array<std::array<PadVoice, kMaxPadVoices>, kPadCount> voices_{};
    std::array<std::uint8_t, kPadCount> nextVoiceIndex_{};
    std::array<AudioTriggerEvent, kAudioTriggerQueueCapacity> pendingTriggerEvents_{};
    std::size_t pendingTriggerCount_ = 0;
};

AudioEngine::AudioEngine(mpc::MpcProjectState& projectState)
        : projectState_(projectState) {
    recordedSamples_.resize(kMaxRecordingFrames);
    recordingThresholdMilli_.store(0, std::memory_order_relaxed);
    recordingArmed_.store(false, std::memory_order_relaxed);

    for (std::size_t pad = 0; pad < kPadCount; ++pad) {
        padTuningMilliSemitones_[pad].store(
                static_cast<std::int32_t>(
                    std::lround(projectState_.activeDrumProgram().pad(pad).tuningSemitones * 1000.0f)),
                std::memory_order_relaxed);
        padLevelMilli_[pad].store(
                static_cast<std::int32_t>(
                    std::lround(projectState_.activeDrumProgram().pad(pad).level * 1000.0f)),
                std::memory_order_relaxed);
        padPanMilli_[pad].store(
                static_cast<std::int32_t>(
                    std::lround(projectState_.activeDrumProgram().pad(pad).pan * 1000.0f)),
                std::memory_order_relaxed);
        const auto& padState = projectState_.activeDrumProgram().pad(pad);
        const auto envelopeParameters =
                normalizeSampleEnvelopeParameters(
                        padState.envelopeAttackMs,
                        padState.envelopeDecayMs,
                        padState.envelopeSustain,
                        padState.envelopeReleaseMs);
        padEnvelopeAttackMilliMs_[pad].store(
                static_cast<std::int32_t>(
                        std::lround(envelopeParameters.attackMs * 1000.0f)),
                std::memory_order_relaxed);
        padEnvelopeDecayMilliMs_[pad].store(
                static_cast<std::int32_t>(
                        std::lround(envelopeParameters.decayMs * 1000.0f)),
                std::memory_order_relaxed);
        padEnvelopeSustainMilli_[pad].store(
                static_cast<std::int32_t>(
                        std::lround(envelopeParameters.sustain * 1000.0f)),
                std::memory_order_relaxed);
        padEnvelopeReleaseMilliMs_[pad].store(
                static_cast<std::int32_t>(
                        std::lround(envelopeParameters.releaseMs * 1000.0f)),
                std::memory_order_relaxed);
        padFilterCutoffMilliHz_[pad].store(
                normalizeSampleFilterCutoffMilliHz(padState.filterCutoffHz),
                std::memory_order_relaxed);
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            const auto& layerState = projectState_.activeDrumProgram().pad(pad).layer(layer);
            padLayerGainMilli_[pad][layer].store(
                    static_cast<std::int32_t>(
                        std::lround(layerState.gain * 1000.0f)),
                    std::memory_order_relaxed);
            padLayerTuningMilliSemitones_[pad][layer].store(
                    static_cast<std::int32_t>(
                        std::lround(layerState.tuningSemitones * 1000.0f)),
                    std::memory_order_relaxed);
            padLayerPanMilli_[pad][layer].store(
                    static_cast<std::int32_t>(
                        std::lround(layerState.pan * 1000.0f)),
                    std::memory_order_relaxed);
            padLayerVelocityMin_[pad][layer].store(
                    static_cast<std::int32_t>(layerState.velocityMinimum),
                    std::memory_order_relaxed);
            padLayerVelocityMax_[pad][layer].store(
                    static_cast<std::int32_t>(layerState.velocityMaximum),
                    std::memory_order_relaxed);
        }
    }
}

AudioEngine::~AudioEngine() {
    stop();
}

AudioEngine& AudioEngine::instance() {
    return mpc::MpcCore::instance().audio();
}

std::string AudioEngine::loadSample(
        std::span<const std::uint8_t> bytes) {
    if (stream_ != nullptr) {
        return "Stop audio before loading a sample";
    }

    const auto decoded = decodeWav(bytes);
    if (!decoded.has_value()) {
        return "Sample load failed: unsupported or invalid PCM WAV";
    }

    sample_ = std::make_shared<SampleBuffer>(*decoded);
    sampleDescription_ =
            std::to_string(sample_->sampleRate) + " Hz "
            + std::to_string(sample_->channelCount) + " ch "
            + std::to_string(sample_->frameCount()) + " frames";

    return "Fallback sample loaded | " + sampleDescription_;
}

std::string AudioEngine::chopPadSampleToPads(
        std::uint8_t sourcePadIndex,
        std::uint8_t sourceLayerIndex,
        std::uint8_t chopCount) {
    if (sourcePadIndex >= kPadCount
            || sourceLayerIndex >= kSampleLayerCount) {
        return "Chop failed: invalid source pad or layer";
    }

    if (stream_ != nullptr) {
        return "Stop audio before chopping a sample";
    }

    if (!isSupportedChopCount(chopCount)) {
        return "Chop failed: supported counts are 4, 8 or 16";
    }

    std::shared_ptr<const SampleBuffer> sourceSample =
            padSamples_[sourcePadIndex][sourceLayerIndex];
    SampleRegion sourceRegion{};
    std::string sourceDescription;

    if (sourceSample != nullptr) {
        sourceRegion = projectState_.activeDrumProgram().pad(sourcePadIndex).layer(sourceLayerIndex).region;
        sourceDescription = padSampleDescriptions_[sourcePadIndex][sourceLayerIndex];
    } else if (sourceLayerIndex == 0) {
        bool hasExplicitLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            hasExplicitLayer = hasExplicitLayer
                    || (padSamples_[sourcePadIndex][layer] != nullptr);
        }

        if (!hasExplicitLayer && sample_ != nullptr) {
            sourceSample = sample_;
            sourceRegion = fullSampleRegion(sample_->frameCount());
            sourceDescription = sampleDescription_;
        }
    }

    if (sourceSample == nullptr || sourceSample->frameCount() == 0) {
        return "Chop failed: no source sample assigned";
    }

    const auto sourceSampleId =
            projectState_.activeDrumProgram().pad(sourcePadIndex).layer(sourceLayerIndex).sample;

    if (!sourceRegion.isValidFor(sourceSample->frameCount())) {
        return "Chop failed: invalid source sample region";
    }

    const auto plan = makeEvenChopPlan(
            sourceRegion.frameCount(),
            static_cast<std::size_t>(chopCount));
    if (!plan.isValid()) {
        return "Chop failed: source region is too short";
    }

    mpc::domain::SampleId resolvedSourceSampleId = sourceSampleId;
    if (!resolvedSourceSampleId.isAssigned()) {
        resolvedSourceSampleId = projectState_.registerSample(
                "Chop source sample",
                "",
                static_cast<double>(sourceSample->sampleRate),
                static_cast<std::int64_t>(sourceSample->frameCount()));
    }

    for (std::size_t destinationPad = 0;
            destinationPad < plan.count;
            ++destinationPad) {
        for (std::size_t layer = 1;
                layer < kSampleLayerCount;
                ++layer) {
            const bool isSourceLayer =
                    destinationPad == sourcePadIndex
                    && layer == sourceLayerIndex;
            if (padSamples_[destinationPad][layer] != nullptr
                    && !isSourceLayer) {
                return "Chop failed: destination pad "
                        + std::to_string(destinationPad + 1)
                        + " has assigned extra layers";
            }
        }

        const auto& existing = padSamples_[destinationPad][0];
        if (existing != nullptr && existing != sourceSample) {
            return "Chop failed: destination pad "
                    + std::to_string(destinationPad + 1)
                    + " layer 1 is already assigned";
        }
    }

    for (std::size_t destinationPad = 0;
            destinationPad < plan.count;
            ++destinationPad) {
        const auto relativeRegion = plan.regions[destinationPad];
        padSamples_[destinationPad][0] = sourceSample;
        auto& destinationLayerState =
                projectState_.activeDrumProgram().pad(destinationPad).layer(0);
        destinationLayerState.sample = resolvedSourceSampleId;
        destinationLayerState.region = SampleRegion{
                sourceRegion.startFrame + relativeRegion.startFrame,
                sourceRegion.startFrame + relativeRegion.endFrame};
        padSampleDescriptions_[destinationPad][0] = sourceDescription;
    }

    return "Chop complete: Pad "
            + std::to_string(static_cast<unsigned>(sourcePadIndex + 1))
            + " layer "
            + std::to_string(static_cast<unsigned>(sourceLayerIndex + 1))
            + " -> pads 1-"
            + std::to_string(plan.count)
            + " (" + std::to_string(plan.count) + " slices)";
}

std::string AudioEngine::cropPadSampleRegion(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return "Crop failed: invalid pad or layer";
    }

    if (stream_ != nullptr) {
        return "Stop audio before cropping a sample";
    }

    std::shared_ptr<const SampleBuffer> sourceSample =
            padSamples_[padIndex][layerIndex];
    SampleRegion sourceRegion{};
    std::string sourceDescription;

    if (sourceSample != nullptr) {
        sourceRegion = projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).region;
        sourceDescription = padSampleDescriptions_[padIndex][layerIndex];
    } else if (layerIndex == 0) {
        bool hasExplicitLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            hasExplicitLayer = hasExplicitLayer
                    || (padSamples_[padIndex][layer] != nullptr);
        }

        if (!hasExplicitLayer && sample_ != nullptr) {
            sourceSample = sample_;
            sourceRegion = fullSampleRegion(sample_->frameCount());
            sourceDescription = sampleDescription_;
        }
    }

    if (sourceSample == nullptr || sourceSample->frameCount() == 0) {
        return "Crop failed: no source sample assigned";
    }

    if (!sourceRegion.isValidFor(sourceSample->frameCount())) {
        return "Crop failed: invalid source sample region";
    }

    const auto cropped = cropSampleToRegion(*sourceSample, sourceRegion);
    if (!cropped.has_value()) {
        return "Crop failed: invalid source region or sample format";
    }

    auto croppedSample = std::make_shared<SampleBuffer>(*cropped);
    const std::size_t croppedFrames = croppedSample->frameCount();
    padSamples_[padIndex][layerIndex] = croppedSample;
    auto& layerState = projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex);
    layerState.sample = projectState_.registerSample(
            "Cropped sample",
            "",
            static_cast<double>(croppedSample->sampleRate),
            static_cast<std::int64_t>(croppedSample->frameCount()));
    layerState.region = fullSampleRegion(croppedFrames);
    padSampleDescriptions_[padIndex][layerIndex] =
            std::to_string(croppedSample->sampleRate) + " Hz "
            + std::to_string(croppedSample->channelCount) + " ch "
            + std::to_string(croppedFrames) + " frames";

    return "Crop complete: Pad "
            + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer "
            + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " | source="
            + sourceDescription
            + " | frames=" + std::to_string(croppedFrames);
}

std::string AudioEngine::setPadLayerGain(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        float gain) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount
            || !std::isfinite(gain)) {
        return "Layer gain change failed: invalid value";
    }

    const auto milli = normalizeSampleLayerGainMilli(gain);
    const float applied = sampleLayerGainFromMilli(milli);
    projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).gain = applied;
    padLayerGainMilli_[padIndex][layerIndex].store(
            milli,
            std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " gain: " + std::to_string(milli / 10) + "%";
}

float AudioEngine::padLayerGain(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return 1.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).gain;
}

std::string AudioEngine::setPadLayerTuningSemitones(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        float semitones) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount
            || !std::isfinite(semitones)) {
        return "Layer tuning change failed: invalid value";
    }

    const auto milli =
            normalizeSampleLayerTuningMilli(semitones);
    const float applied = sampleLayerTuningFromMilli(milli);
    projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).tuningSemitones = applied;
    padLayerTuningMilliSemitones_[padIndex][layerIndex].store(
            milli,
            std::memory_order_relaxed);
    const char sign = applied >= 0.0f ? '+' : '-';
    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " tuning: " + sign
            + std::to_string(std::abs(applied)) + " st";
}

float AudioEngine::padLayerTuningSemitones(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return 0.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).tuningSemitones;
}

std::string AudioEngine::setPadLayerPan(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        float pan) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount
            || !std::isfinite(pan)) {
        return "Layer pan change failed: invalid value";
    }

    const auto milli = normalizeSampleLayerPanMilli(pan);
    const float applied = sampleLayerPanFromMilli(milli);
    projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).pan = applied;
    padLayerPanMilli_[padIndex][layerIndex].store(
            milli,
            std::memory_order_relaxed);
    if (applied < 0.0f) {
        return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
                + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
                + " pan: L" + std::to_string(
                        static_cast<int>(std::lround(-applied * 100.0f)));
    }
    if (applied > 0.0f) {
        return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
                + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
                + " pan: R" + std::to_string(
                        static_cast<int>(std::lround(applied * 100.0f)));
    }
    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " pan: C";
}

float AudioEngine::padLayerPan(
        std::uint8_t padIndex,        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return 0.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).pan;
}

std::string AudioEngine::setPadLayerVelocityRange(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        std::uint8_t minimum,
        std::uint8_t maximum) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return "Layer velocity range failed: invalid pad or layer";
    }

    if (minimum > maximum) {
        return "Layer velocity range failed: minimum exceeds maximum";
    }

    auto& layerState = projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex);
    layerState.velocityMinimum = minimum;
    layerState.velocityMaximum = maximum;
    padLayerVelocityMin_[padIndex][layerIndex].store(
            static_cast<std::int32_t>(minimum),
            std::memory_order_relaxed);
    padLayerVelocityMax_[padIndex][layerIndex].store(
            static_cast<std::int32_t>(maximum),
            std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " velocity: "
            + std::to_string(static_cast<unsigned>(minimum))
            + "-"
            + std::to_string(static_cast<unsigned>(maximum));
}

SampleLayerVelocityRange AudioEngine::padLayerVelocityRange(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return {};
    }

    return projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).velocityRange();
}

mpc::domain::DrumProgram AudioEngine::drumProgramSnapshot() const {
    return projectState_.activeDrumProgram();
}

std::string AudioEngine::setPadTuningSemitones(
        std::uint8_t padIndex,
        float semitones) {
    if (padIndex >= kPadCount || !std::isfinite(semitones)) {
        return "Tuning change failed: invalid value";
    }

    const float clamped = std::clamp(semitones, -24.0f, 24.0f);
    const auto milliSemitones = static_cast<std::int32_t>(
            std::lround(clamped * 1000.0f));
    projectState_.activeDrumProgram().pad(padIndex).tuningSemitones =
            static_cast<float>(milliSemitones) / 1000.0f;
    padTuningMilliSemitones_[padIndex].store(
            milliSemitones,
            std::memory_order_relaxed);

    const float applied = static_cast<float>(milliSemitones) / 1000.0f;
    const char sign = applied >= 0.0f ? '+' : '-';
    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " tuning: " + sign
            + std::to_string(std::abs(applied)) + " st";
}

float AudioEngine::padTuningSemitones(std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return 0.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).tuningSemitones;
}

std::string AudioEngine::setPadLevel(
        std::uint8_t padIndex,
        float level) {
    if (padIndex >= kPadCount || !std::isfinite(level)) {
        return "Level change failed: invalid value";
    }

    const float clamped = std::clamp(level, 0.0f, 1.0f);
    const auto milli = static_cast<std::int32_t>(std::lround(clamped * 1000.0f));
    projectState_.activeDrumProgram().pad(padIndex).level =
            static_cast<float>(milli) / 1000.0f;
    padLevelMilli_[padIndex].store(milli, std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " level: " + std::to_string(milli / 10) + "%";
}

float AudioEngine::padLevel(std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return 1.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).level;
}

std::string AudioEngine::setPadPan(
        std::uint8_t padIndex,
        float pan) {
    if (padIndex >= kPadCount || !std::isfinite(pan)) {
        return "Pan change failed: invalid value";
    }

    const float clamped = std::clamp(pan, -1.0f, 1.0f);
    const auto milli = static_cast<std::int32_t>(std::lround(clamped * 1000.0f));
    projectState_.activeDrumProgram().pad(padIndex).pan =
            static_cast<float>(milli) / 1000.0f;
    padPanMilli_[padIndex].store(milli, std::memory_order_relaxed);

    const float applied = static_cast<float>(milli) / 1000.0f;
    if (applied < 0.0f) {
        return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
                + " pan: L" + std::to_string(static_cast<int>(std::lround(-applied * 100.0f)));
    }
    if (applied > 0.0f) {
        return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
                + " pan: R" + std::to_string(static_cast<int>(std::lround(applied * 100.0f)));
    }
    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1)) + " pan: C";
}

float AudioEngine::padPan(std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return 0.0f;
    }

    return projectState_.activeDrumProgram().pad(padIndex).pan;
}

std::string AudioEngine::setPadEnvelopeParameters(
        std::uint8_t padIndex,
        float attackMs,
        float decayMs,
        float sustain,
        float releaseMs) {
    if (padIndex >= kPadCount) {
        return "Envelope change failed: invalid pad";
    }

    const auto parameters =
            normalizeSampleEnvelopeParameters(
                    attackMs, decayMs, sustain, releaseMs);
    auto& pad = projectState_.activeDrumProgram().pad(padIndex);
    pad.envelopeAttackMs = parameters.attackMs;
    pad.envelopeDecayMs = parameters.decayMs;
    pad.envelopeSustain = parameters.sustain;
    pad.envelopeReleaseMs = parameters.releaseMs;

    padEnvelopeAttackMilliMs_[padIndex].store(
            static_cast<std::int32_t>(std::lround(parameters.attackMs * 1000.0f)),
            std::memory_order_relaxed);
    padEnvelopeDecayMilliMs_[padIndex].store(
            static_cast<std::int32_t>(std::lround(parameters.decayMs * 1000.0f)),
            std::memory_order_relaxed);
    padEnvelopeSustainMilli_[padIndex].store(
            static_cast<std::int32_t>(std::lround(parameters.sustain * 1000.0f)),
            std::memory_order_relaxed);
    padEnvelopeReleaseMilliMs_[padIndex].store(
            static_cast<std::int32_t>(std::lround(parameters.releaseMs * 1000.0f)),
            std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " envelope: A=" + std::to_string(parameters.attackMs)
            + "ms D=" + std::to_string(parameters.decayMs)
            + "ms S=" + std::to_string(parameters.sustain)
            + " R=" + std::to_string(parameters.releaseMs) + "ms";
}

SampleEnvelopeParameters AudioEngine::padEnvelopeParameters(
        std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return {};
    }
    const auto& pad = projectState_.activeDrumProgram().pad(padIndex);
    return normalizeSampleEnvelopeParameters(
            pad.envelopeAttackMs,
            pad.envelopeDecayMs,
            pad.envelopeSustain,
            pad.envelopeReleaseMs);
}

std::string AudioEngine::setPadFilterCutoff(
        std::uint8_t padIndex,
        float cutoffHz) {
    if (padIndex >= kPadCount || !std::isfinite(cutoffHz)) {
        return "Filter cutoff change failed: invalid value";
    }

    const auto milliHz = normalizeSampleFilterCutoffMilliHz(cutoffHz);
    const float applied = sampleFilterCutoffFromMilliHz(milliHz);
    projectState_.activeDrumProgram().pad(padIndex).filterCutoffHz = applied;
    padFilterCutoffMilliHz_[padIndex].store(
            milliHz, std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " filter cutoff: " + std::to_string(applied) + " Hz";
}

float AudioEngine::padFilterCutoff(std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return 20000.0f;
    }
    return projectState_.activeDrumProgram().pad(padIndex).filterCutoffHz;
}

std::string AudioEngine::loadSampleForPad(
        std::span<const std::uint8_t> bytes,
        std::uint8_t padIndex) {
    return loadSampleForPadLayer(bytes, padIndex, 0);
}

std::string AudioEngine::loadSampleForPadLayer(
        std::span<const std::uint8_t> bytes,
        std::uint8_t padIndex,
        std::uint8_t layerIndex) {
    if (stream_ != nullptr) {
        return "Stop audio before loading a sample";
    }

    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return "Sample load failed: invalid pad or layer";
    }

    const auto decoded = decodeWav(bytes);
    if (!decoded.has_value()) {
        return "Sample load failed: unsupported or invalid PCM WAV";
    }

    padSamples_[padIndex][layerIndex] = std::make_shared<SampleBuffer>(*decoded);
    auto& layerState = projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex);
    layerState.sample = projectState_.registerSample(
            "Imported sample",
            "",
            static_cast<double>(padSamples_[padIndex][layerIndex]->sampleRate),
            static_cast<std::int64_t>(padSamples_[padIndex][layerIndex]->frameCount()));
    layerState.region =
            fullSampleRegion(padSamples_[padIndex][layerIndex]->frameCount());
    padSampleDescriptions_[padIndex][layerIndex] =
            std::to_string(padSamples_[padIndex][layerIndex]->sampleRate) + " Hz "
            + std::to_string(padSamples_[padIndex][layerIndex]->channelCount) + " ch "
            + std::to_string(padSamples_[padIndex][layerIndex]->frameCount()) + " frames";

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " sample loaded | "
            + padSampleDescriptions_[padIndex][layerIndex];
}

std::string AudioEngine::setPadSampleRegion(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        std::size_t startFrame,
        std::size_t endFrame) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return "Sample region change failed: invalid pad or layer";
    }

    if (stream_ != nullptr) {
        return "Stop audio before editing sample region";
    }

    const auto& sample = padSamples_[padIndex][layerIndex];
    if (sample == nullptr || sample->frameCount() == 0) {
        return "Sample region change failed: no sample assigned";
    }

    if (startFrame >= endFrame || endFrame > sample->frameCount()) {
        return "Sample region change failed: invalid frame range";
    }

    projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).region =
            SampleRegion{startFrame, endFrame};

    return "Pad "
            + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer "
            + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " region: "
            + std::to_string(startFrame)
            + "-"
            + std::to_string(endFrame);
}

SampleRegion AudioEngine::padSampleRegion(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return {};
    }

    return projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex).region;
}

std::size_t AudioEngine::padSampleFrameCount(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return 0;
    }

    const auto& sample = padSamples_[padIndex][layerIndex];
    if (sample != nullptr) {
        return sample->frameCount();
    }

    if (layerIndex == 0 && sample_ != nullptr) {
        bool hasExplicitLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            hasExplicitLayer =
                    hasExplicitLayer
                    || (padSamples_[padIndex][layer] != nullptr);
        }
        if (!hasExplicitLayer) {
            return sample_->frameCount();
        }
    }

    return 0;
}

std::uint32_t AudioEngine::padSampleRate(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return 0;
    }

    const auto& sample = padSamples_[padIndex][layerIndex];
    if (sample != nullptr) {
        return sample->sampleRate;
    }

    if (layerIndex == 0 && sample_ != nullptr) {
        bool hasExplicitLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            hasExplicitLayer =
                    hasExplicitLayer
                    || (padSamples_[padIndex][layer] != nullptr);
        }
        if (!hasExplicitLayer) {
            return sample_->sampleRate;
        }
    }

    return 0;
}

std::vector<WaveformPeak> AudioEngine::padWaveformPeaks(
        std::uint8_t padIndex,
        std::uint8_t layerIndex,
        std::size_t pointCount) const {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount
            || pointCount == 0) {
        return {};
    }

    std::shared_ptr<const SampleBuffer> sample =
            padSamples_[padIndex][layerIndex];

    if (sample == nullptr && layerIndex == 0 && sample_ != nullptr) {
        bool hasExplicitLayer = false;
        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            hasExplicitLayer =
                    hasExplicitLayer
                    || (padSamples_[padIndex][layer] != nullptr);
        }
        if (!hasExplicitLayer) {
            sample = sample_;
        }
    }

    if (sample == nullptr || sample->frameCount() == 0) {
        return {};
    }

    return buildWaveformPeaks(
            sample->interleaved,
            sample->channelCount,
            pointCount);
}

void AudioEngine::triggerPad(
        std::uint8_t padIndex,
        std::uint8_t velocity) {
    triggerPadAtOffset(padIndex, velocity, 0);
}

void AudioEngine::triggerPadAtOffset(
        std::uint8_t padIndex,
        std::uint8_t velocity,
        std::int32_t offsetFrames) {
    if (padIndex >= kPadCount || velocity == 0) {
        return;
    }

    const auto clampedOffset = std::max(offsetFrames, 0);
    static_cast<void>(triggerQueue_.tryEnqueue(
            AudioTriggerEvent{
                padIndex,
                velocity,
                clampedOffset}));
}

std::string AudioEngine::openInputStream() {
    if (inputStream_ != nullptr) {
        return "Input stream already active";
    }

    inputCallback_ = std::make_shared<InputCallback>(*this);

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Input)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::Float)
        ->setFormatConversionAllowed(true)
        ->setSampleRate(stream_ != nullptr ? stream_->getSampleRate() : 0)
        ->setChannelCount(1)
        ->setChannelConversionAllowed(true)
        ->setDataCallback(inputCallback_);

    const oboe::Result openResult = builder.openStream(inputStream_);
    if (openResult != oboe::Result::OK || inputStream_ == nullptr) {
        inputStream_.reset();
        inputCallback_.reset();
        return std::string("microphone open failed: ") + resultText(openResult);
    }

    const oboe::Result startResult = inputStream_->start();
    if (startResult != oboe::Result::OK) {
        const std::string message =
                std::string("microphone start failed: ")
                + resultText(startResult);
        inputStream_->close();
        inputStream_.reset();
        inputCallback_.reset();
        return message;
    }

    return "Input stream active";
}

std::string AudioEngine::stopInputStream() {
    if (inputStream_ == nullptr) {
        return "Input stream already stopped";
    }

    const oboe::Result stopResult = inputStream_->stop();
    const oboe::Result closeResult = inputStream_->close();

    inputStream_.reset();
    inputCallback_.reset();

    if (stopResult != oboe::Result::OK) {
        return std::string("Input stop failed: ") + resultText(stopResult);
    }

    if (closeResult != oboe::Result::OK) {
        return std::string("Input close failed: ") + resultText(closeResult);
    }

    return "Input stream stopped";
}

std::string AudioEngine::setRecordingThreshold(float threshold) {
    if (!std::isfinite(threshold)) {
        return "Recording threshold change failed: invalid value";
    }

    if (recordingEnabled_.load(std::memory_order_acquire)
            || recordingArmed_.load(std::memory_order_acquire)) {
        return "Recording threshold change failed: stop recording first";
    }

    const float clamped = std::clamp(threshold, 0.0f, 1.0f);
    const auto milli = static_cast<std::int32_t>(
            std::lround(clamped * 1000.0f));
    recordingThresholdMilli_.store(
            milli,
            std::memory_order_release);

    if (milli == 0) {
        return "Recording threshold: Off";
    }

    return "Recording threshold: "
            + std::to_string(milli / 10) + "%";
}

float AudioEngine::recordingThreshold() const {
    return static_cast<float>(
            recordingThresholdMilli_.load(
                    std::memory_order_acquire))
            / 1000.0f;
}

std::string AudioEngine::startRecording() {
    if (recordingEnabled_.load(std::memory_order_acquire)
            || recordingArmed_.load(std::memory_order_acquire)) {
        return recordingStatus();
    }

    if (recordedSamples_.size() != kMaxRecordingFrames) {
        recordedSamples_.resize(kMaxRecordingFrames);
    }

    recordedFrameCount_.store(0, std::memory_order_relaxed);
    recordingPeakMilli_.store(0, std::memory_order_relaxed);
    recordingSampleRate_.store(0, std::memory_order_relaxed);
    for (auto& peak : recordingWaveformPeakMilli_) {
        peak.store(0, std::memory_order_relaxed);
    }
    recordingOverflowed_.store(false, std::memory_order_relaxed);
    const bool thresholdEnabled =
            recordingThresholdMilli_.load(
                    std::memory_order_acquire) > 0;
    recordingArmed_.store(
            thresholdEnabled,
            std::memory_order_release);
    recordingEnabled_.store(
            !thresholdEnabled,
            std::memory_order_release);

    if (inputStream_ == nullptr) {
        const std::string inputResult = openInputStream();
        if (inputStream_ == nullptr) {
            return "Recording start failed: " + inputResult;
        }
    }

    recordingSampleRate_.store(
            inputStream_->getSampleRate(),
            std::memory_order_release);
    if (!thresholdEnabled) {
        recordingEnabled_.store(true, std::memory_order_release);
    }
    return recordingStatus();
}

std::string AudioEngine::stopRecording() {
    recordingEnabled_.store(false, std::memory_order_release);
    recordingArmed_.store(false, std::memory_order_release);

    if (!monitorEnabled_.load(std::memory_order_acquire)
            && inputStream_ != nullptr) {        const std::string inputResult = stopInputStream();
        if (inputStream_ != nullptr) {
            return "Recording stop failed: " + inputResult;
        }
    }

    return recordingStatus();
}

std::string AudioEngine::startMonitor() {
    if (monitorEnabled_.load(std::memory_order_acquire)) {
        return recordingStatus();
    }

    bool startedOutput = false;
    if (stream_ == nullptr) {
        const std::string outputResult = start();
        if (stream_ == nullptr) {
            return "Monitor start failed: sampler output unavailable | "
                    + outputResult;
        }
        startedOutput = true;
    }

    if (inputStream_ == nullptr) {
        const std::string inputResult = openInputStream();
        if (inputStream_ == nullptr) {
            if (startedOutput) {
                stopOutputStream();
            }
            return "Monitor start failed: " + inputResult;
        }
    }

    monitorEnabled_.store(true, std::memory_order_release);
    return recordingStatus();
}

std::string AudioEngine::stopMonitor() {
    monitorEnabled_.store(false, std::memory_order_release);

    if (!recordingEnabled_.load(std::memory_order_acquire)
            && inputStream_ != nullptr) {
        const std::string inputResult = stopInputStream();
        if (inputStream_ != nullptr) {
            return "Monitor stop failed: " + inputResult;
        }
    }

    return recordingStatus();
}

std::string AudioEngine::stopOutputStream() {
    if (stream_ == nullptr) {
        return "Audio output already stopped";
    }

    const oboe::Result stopResult = stream_->stop();
    const oboe::Result closeResult = stream_->close();

    stream_.reset();
    callback_.reset();

    if (stopResult != oboe::Result::OK) {
        return std::string("Audio stop failed: ") + resultText(stopResult);
    }

    if (closeResult != oboe::Result::OK) {
        return std::string("Audio close failed: ") + resultText(closeResult);
    }

    return "Audio output stopped";
}

std::string AudioEngine::assignRecordingToPadLayer(
        std::uint8_t padIndex,
        std::uint8_t layerIndex) {
    if (padIndex >= kPadCount || layerIndex >= kSampleLayerCount) {
        return "Recording assign failed: invalid pad or layer";
    }

    if (recordingEnabled_.load(std::memory_order_acquire)
            || recordingArmed_.load(std::memory_order_acquire)) {
        return "Recording assign failed: stop recording first";
    }

    const auto frameCount =
            recordedFrameCount_.load(std::memory_order_acquire);
    const auto sampleRate =
            recordingSampleRate_.load(std::memory_order_acquire);

    if (frameCount == 0u) {
        return "Recording assign failed: no recorded audio";
    }

    if (sampleRate <= 0) {
        return "Recording assign failed: invalid recording sample rate";
    }

    const bool restartOutput = stream_ != nullptr;
    if (restartOutput) {
        const std::string stopResult = stopOutputStream();
        if (stream_ != nullptr
                || stopResult.rfind("Audio stop failed:", 0) == 0
                || stopResult.rfind("Audio close failed:", 0) == 0) {
            return "Recording assign failed: could not stop sampler | "
                    + stopResult;
        }
    }

    SampleBuffer recordedSample;
    recordedSample.sampleRate = static_cast<std::uint32_t>(sampleRate);
    recordedSample.channelCount = 1;
    recordedSample.interleaved.assign(
            recordedSamples_.begin(),
            recordedSamples_.begin() + frameCount);

    padSamples_[padIndex][layerIndex] =
            std::make_shared<SampleBuffer>(std::move(recordedSample));
    auto& layerState = projectState_.activeDrumProgram().pad(padIndex).layer(layerIndex);
    layerState.sample = projectState_.registerSample(
            "Recorded sample",
            "",
            static_cast<double>(padSamples_[padIndex][layerIndex]->sampleRate),
            static_cast<std::int64_t>(padSamples_[padIndex][layerIndex]->frameCount()));
    layerState.region =
            fullSampleRegion(padSamples_[padIndex][layerIndex]->frameCount());
    padSampleDescriptions_[padIndex][layerIndex] =
            std::to_string(padSamples_[padIndex][layerIndex]->sampleRate) + " Hz "
            + std::to_string(padSamples_[padIndex][layerIndex]->channelCount) + " ch "
            + std::to_string(padSamples_[padIndex][layerIndex]->frameCount())
            + " frames (recording)";

    const std::string assigned =
            "Recording assigned | pad "
            + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer "
            + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " | "
            + padSampleDescriptions_[padIndex][layerIndex];

    if (!restartOutput) {
        return assigned;
    }

    const std::string startResult = start();
    if (stream_ == nullptr) {
        return assigned + " | sampler restart failed | " + startResult;
    }

    return assigned + " | sampler restarted";
}

std::uint32_t AudioEngine::recordingFrameCount() const {
    return recordedFrameCount_.load(std::memory_order_acquire);
}

std::uint32_t AudioEngine::recordingSampleRate() const {
    const auto value = recordingSampleRate_.load(std::memory_order_acquire);
    return value > 0 ? static_cast<std::uint32_t>(value) : 0u;
}

float AudioEngine::recordingPeak() const {
    return static_cast<float>(
            recordingPeakMilli_.load(std::memory_order_relaxed)) / 1000.0f;
}

std::vector<WaveformPeak> AudioEngine::recordingWaveformPeaks(
        std::size_t pointCount) const {
    if (pointCount == 0) {
        return {};
    }

    const auto frameCount =
            recordedFrameCount_.load(std::memory_order_acquire);
    if (frameCount == 0) {
        return std::vector<WaveformPeak>(pointCount);
    }

    const auto activeBins = std::min(
            kRecordingWaveformBinCount,
            std::max<std::size_t>(
                    1,
                    (static_cast<std::size_t>(frameCount)
                     * kRecordingWaveformBinCount
                     + kMaxRecordingFrames - 1)
                    / kMaxRecordingFrames));

    pointCount = std::min(pointCount, activeBins);
    std::vector<WaveformPeak> result(pointCount);

    for (std::size_t point = 0; point < pointCount; ++point) {
        const auto binBegin = point * activeBins / pointCount;
        const auto binEnd = std::max(
                binBegin + 1,
                (point + 1) * activeBins / pointCount);

        std::int32_t peakMilli = 0;
        for (std::size_t bin = binBegin;
                bin < std::min(binEnd, activeBins);
                ++bin) {
            peakMilli = std::max(
                    peakMilli,
                    recordingWaveformPeakMilli_[bin].load(
                            std::memory_order_relaxed));
        }

        const float peak = static_cast<float>(peakMilli) / 1000.0f;
        result[point] = WaveformPeak{-peak, peak};
    }

    return result;
}

std::string AudioEngine::recordingStatus() const {
    const auto frameCount =
            recordedFrameCount_.load(std::memory_order_acquire);
    const auto sampleRate =
            recordingSampleRate_.load(std::memory_order_acquire);
    const auto peakMilli =
            recordingPeakMilli_.load(std::memory_order_relaxed);
    const bool overflowed =
            recordingOverflowed_.load(std::memory_order_acquire);
    const bool recordingActive =
            recordingEnabled_.load(std::memory_order_acquire);
    const bool recordingArmed =
            recordingArmed_.load(std::memory_order_acquire);
    const bool monitorEnabled =
            monitorEnabled_.load(std::memory_order_acquire);
    const int thresholdPercent =
            static_cast<int>(
                std::lround(
                    static_cast<double>(
                        recordingThresholdMilli_.load(
                            std::memory_order_relaxed))
                    / 10.0));

    double milliseconds = 0.0;
    if (sampleRate > 0) {
        milliseconds =
                static_cast<double>(frameCount) * 1000.0
                / static_cast<double>(sampleRate);
    }

    const int peakPercent =
            static_cast<int>(std::lround(
                    static_cast<double>(peakMilli) / 10.0));

    std::string result =
            recordingActive
                ? "Recording active"
                : (recordingArmed
                    ? "Recording armed"
                    : (frameCount == 0
                        ? "Recording idle"
                        : "Recording stopped"));

    result += " | threshold="
            + (thresholdPercent == 0
                ? std::string("Off")
                : std::to_string(thresholdPercent) + "%")
            + " | rate=" + std::to_string(sampleRate) + " Hz"
            + " | frames=" + std::to_string(frameCount)
            + " | duration=" + std::to_string(
                    static_cast<std::int64_t>(std::lround(milliseconds)))
            + " ms"
            + " | peak=" + std::to_string(peakPercent) + "%"
            + (monitorEnabled ? " | monitor=on" : " | monitor=off");

    if (overflowed) {
        result += " | buffer full";
    }

    return result;
}

std::string AudioEngine::start() {
    if (stream_ != nullptr) {
        return status();
    }

    AudioEngine::SampleLayerGrid samples{};
    AudioEngine::SampleRegionGrid regions{};
    bool anySample = false;

    for (std::size_t pad = 0; pad < kPadCount; ++pad) {
        bool anyExplicitLayer = false;

        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            samples[pad][layer] = padSamples_[pad][layer];
            regions[pad][layer] = projectState_.activeDrumProgram().pad(pad).layer(layer).region;
            anyExplicitLayer = anyExplicitLayer
                    || (samples[pad][layer] != nullptr
                        && samples[pad][layer]->frameCount() > 0);
        }

        // Keep the existing bundled sample behavior for pads that have no
        // explicitly assigned layers. Once a pad has any explicit layer,
        // playback comes only from those assigned layers.
        if (!anyExplicitLayer && sample_ != nullptr) {
            samples[pad][0] = sample_;
            regions[pad][0] = fullSampleRegion(sample_->frameCount());
        }

        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            anySample = anySample
                    || (samples[pad][layer] != nullptr
                        && samples[pad][layer]->frameCount() > 0);
        }
    }

    if (!anySample) {
        return "Audio start failed: no sample loaded";
    }

    callback_ = std::make_shared<OutputCallback>(
            std::move(samples),
            std::move(regions),
            triggerQueue_,
            padTuningMilliSemitones_,
            padLevelMilli_,
            padPanMilli_,
            padLayerGainMilli_,
            padLayerTuningMilliSemitones_,
            padLayerPanMilli_,
            padLayerVelocityMin_,
            padLayerVelocityMax_,
            padEnvelopeAttackMilliMs_,
            padEnvelopeDecayMilliMs_,
            padEnvelopeSustainMilli_,
            padEnvelopeReleaseMilliMs_,
            padFilterCutoffMilliHz_,
            monitorSamples_,
            monitorWriteSequence_,
            monitorEnabled_);

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
        ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Shared)
        ->setFormat(oboe::AudioFormat::Float)
        ->setFormatConversionAllowed(true)
        ->setChannelCount(2)
        ->setChannelConversionAllowed(true)
        ->setUsage(oboe::Usage::Media)
        ->setContentType(oboe::ContentType::Music)
        ->setDataCallback(callback_);

    const oboe::Result openResult = builder.openStream(stream_);
    if (openResult != oboe::Result::OK || stream_ == nullptr) {
        stream_.reset();
        callback_.reset();
        return std::string("Audio open failed: ") + resultText(openResult);
    }

    const oboe::Result startResult = stream_->start();
    if (startResult != oboe::Result::OK) {
        const std::string message =
                std::string("Audio start failed: ") + resultText(startResult);
        stream_->close();
        stream_.reset();
        callback_.reset();
        return message;
    }

    return status();
}

std::string AudioEngine::stop() {
    const bool hadInput = inputStream_ != nullptr;
    monitorEnabled_.store(false, std::memory_order_release);
    recordingEnabled_.store(false, std::memory_order_release);
    recordingArmed_.store(false, std::memory_order_release);

    const std::string inputResult =
            hadInput ? stopInputStream() : std::string();
    const std::string outputResult = stopOutputStream();

    if (outputResult.rfind("Audio stop failed:", 0) == 0
            || outputResult.rfind("Audio close failed:", 0) == 0) {
        return outputResult;
    }

    if (inputResult.rfind("Input stop failed:", 0) == 0
            || inputResult.rfind("Input close failed:", 0) == 0) {
        return inputResult;
    }

    if (hadInput) {
        return std::string("Audio stopped | ") + recordingStatus();
    }

    return "Audio stopped";
}

std::string AudioEngine::status() const {
    if (stream_ == nullptr) {
        if (sample_ == nullptr) {
            return "Audio stopped | no sample loaded";
        }

        std::string result =
                "Audio stopped | fallback=" + sampleDescription_;

        for (std::size_t pad = 0; pad < kPadCount; ++pad) {
            for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
                if (!padSampleDescriptions_[pad][layer].empty()) {
                    result += " | pad" + std::to_string(pad + 1)
                            + ".layer" + std::to_string(layer + 1)
                            + "=" + padSampleDescriptions_[pad][layer];
                }
            }
        }

        return result;
    }

    return std::string("Audio output ")
        + streamStateText(stream_->getState())
        + " | API=" + oboe::convertToText(stream_->getAudioApi())
        + " | rate=" + std::to_string(stream_->getSampleRate())
        + " | channels=" + std::to_string(stream_->getChannelCount())
        + " | burst=" + std::to_string(stream_->getFramesPerBurst())
        + " | fallback=" + sampleDescription_
        + " | low-latency shared";
}

} // namespace mpc::audio
