#include "AudioEngine.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <utility>

namespace {

constexpr std::size_t kPadCount = 16;
constexpr std::size_t kSampleLayerCount = 8;
constexpr float kPadAmplitude = 0.85f;

const char* resultText(oboe::Result result) {
    return oboe::convertToText(result);
}

const char* streamStateText(oboe::StreamState state) {
    return oboe::convertToText(state);
}

} // namespace

namespace mpc::audio {

class AudioEngine::OutputCallback final : public oboe::AudioStreamDataCallback {
public:
    OutputCallback(
            AudioEngine::SampleLayerGrid samples,
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence,
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity,
            std::array<std::atomic<std::int32_t>, kPadCount>& tuningMilliSemitones,
            std::array<std::atomic<std::int32_t>, kPadCount>& levelMilli,
            std::array<std::atomic<std::int32_t>, kPadCount>& panMilli)
            : samples_(std::move(samples)),
              triggerSequence_(triggerSequence),
              triggerVelocity_(triggerVelocity),
              tuningMilliSemitones_(tuningMilliSemitones),
              levelMilli_(levelMilli),
              panMilli_(panMilli) {
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

        for (std::size_t pad = 0; pad < kPadCount; ++pad) {
            const std::uint32_t sequence =
                    triggerSequence_[pad].load(std::memory_order_acquire);

            if (sequence != consumedSequence_[pad]) {
                consumedSequence_[pad] = sequence;

                const std::uint32_t velocity =
                        triggerVelocity_[pad].load(std::memory_order_relaxed);

                auto& voice = voices_[pad];
                const float velocityGain =
                        static_cast<float>(
                            std::min<std::uint32_t>(velocity, 127u))
                        / 127.0f;
                const float level =
                        static_cast<float>(
                            levelMilli_[pad].load(std::memory_order_relaxed))
                        / 1000.0f;
                const float pan =
                        static_cast<float>(
                            panMilli_[pad].load(std::memory_order_relaxed))
                        / 1000.0f;

                voice.gain = velocityGain * level * kPadAmplitude;
                voice.leftGain = pan > 0.0f ? 1.0f - pan : 1.0f;
                voice.rightGain = pan < 0.0f ? 1.0f + pan : 1.0f;

                bool anyLayer = false;
                for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
                    auto& layerVoice = voice.layers[layer];
                    const auto& sample = samples_[pad][layer];

                    layerVoice.position = 0.0;
                    layerVoice.active = sample != nullptr
                            && sample->frameCount() > 0
                            && sample->channelCount > 0;
                    anyLayer = anyLayer || layerVoice.active;
                }

                if (!anyLayer) {
                    voice.active = false;
                    continue;
                }

                const float tuningSemitones =
                        static_cast<float>(
                            tuningMilliSemitones_[pad].load(
                                std::memory_order_relaxed))
                        / 1000.0f;
                const float semitoneRatio =
                        std::pow(2.0f, tuningSemitones / 12.0f);
                for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
                    const auto& sample = samples_[pad][layer];
                    auto& layerVoice = voice.layers[layer];
                    if (!layerVoice.active || sample == nullptr) {
                        continue;
                    }

                    const float sampleToOutputRate =
                            static_cast<float>(sample->sampleRate)
                            / static_cast<float>(sampleRate);
                    layerVoice.positionStep =
                            sampleToOutputRate * semitoneRatio;
                }
                voice.active = velocity != 0;
            }
        }

        for (int32_t frame = 0; frame < numFrames; ++frame) {
            float left = 0.0f;
            float right = 0.0f;

            for (std::size_t pad = 0; pad < kPadCount; ++pad) {
                auto& voice = voices_[pad];
                if (!voice.active) {
                    continue;
                }

                bool anyActiveLayer = false;

                for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
                    auto& layerVoice = voice.layers[layer];
                    if (!layerVoice.active) {
                        continue;
                    }

                    const auto& sample = samples_[pad][layer];
                    if (sample == nullptr || sample->frameCount() == 0
                            || sample->channelCount == 0) {
                        layerVoice.active = false;
                        continue;
                    }

                    const std::size_t sourceFrame =
                            static_cast<std::size_t>(layerVoice.position);

                    if (sourceFrame >= sample->frameCount()) {
                        layerVoice.active = false;
                        continue;
                    }

                    const std::size_t nextFrame =
                            std::min(sourceFrame + 1, sample->frameCount() - 1);
                    const float fraction =
                            static_cast<float>(
                                layerVoice.position
                                - static_cast<double>(sourceFrame));

                    if (sample->channelCount == 1) {
                        const float sample0 =
                                sample->sampleAt(sourceFrame, 0);
                        const float sample1 =
                                sample->sampleAt(nextFrame, 0);
                        const float value =
                                sample0 + (sample1 - sample0) * fraction;
                        left += value * voice.gain * voice.leftGain;
                        right += value * voice.gain * voice.rightGain;
                    } else {
                        const float left0 =
                                sample->sampleAt(sourceFrame, 0);
                        const float left1 =
                                sample->sampleAt(nextFrame, 0);
                        const float right0 =
                                sample->sampleAt(sourceFrame, 1);
                        const float right1 =
                                sample->sampleAt(nextFrame, 1);

                        left += (left0 + (left1 - left0) * fraction)
                                * voice.gain * voice.leftGain;
                        right += (right0 + (right1 - right0) * fraction)
                                * voice.gain * voice.rightGain;
                    }

                    layerVoice.position += layerVoice.positionStep;
                    anyActiveLayer = true;
                }

                voice.active = anyActiveLayer;
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

        return oboe::DataCallbackResult::Continue;
    }

private:
    struct PadVoice {
        struct LayerVoice {
            double position = 0.0;
            float positionStep = 0.0f;
            bool active = false;
        };

        std::array<LayerVoice, kSampleLayerCount> layers{};
        float gain = 0.0f;
        float leftGain = 1.0f;
        float rightGain = 1.0f;
        bool active = false;
    };

    AudioEngine::SampleLayerGrid samples_;
    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence_;
    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity_;
    std::array<std::atomic<std::int32_t>, kPadCount>& tuningMilliSemitones_;
    std::array<std::atomic<std::int32_t>, kPadCount>& levelMilli_;
    std::array<std::atomic<std::int32_t>, kPadCount>& panMilli_;
    std::array<std::uint32_t, kPadCount> consumedSequence_{};
    std::array<PadVoice, kPadCount> voices_{};
};

AudioEngine::AudioEngine() {
    for (std::size_t pad = 0; pad < kPadCount; ++pad) {
        padTriggerSequence_[pad].store(0, std::memory_order_relaxed);
        padTriggerVelocity_[pad].store(0, std::memory_order_relaxed);
        padTuningMilliSemitones_[pad].store(0, std::memory_order_relaxed);
        padLevelMilli_[pad].store(1000, std::memory_order_relaxed);
        padPanMilli_[pad].store(0, std::memory_order_relaxed);
    }
}

AudioEngine::~AudioEngine() {
    stop();
}

AudioEngine& AudioEngine::instance() {
    static AudioEngine engine;
    return engine;
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

std::string AudioEngine::setPadTuningSemitones(
        std::uint8_t padIndex,
        float semitones) {
    if (padIndex >= kPadCount || !std::isfinite(semitones)) {
        return "Tuning change failed: invalid value";
    }

    const float clamped = std::clamp(semitones, -24.0f, 24.0f);
    const auto milliSemitones = static_cast<std::int32_t>(
            std::lround(clamped * 1000.0f));
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

    return static_cast<float>(
            padTuningMilliSemitones_[padIndex].load(
                    std::memory_order_relaxed))
            / 1000.0f;
}

std::string AudioEngine::setPadLevel(
        std::uint8_t padIndex,
        float level) {
    if (padIndex >= kPadCount || !std::isfinite(level)) {
        return "Level change failed: invalid value";
    }

    const float clamped = std::clamp(level, 0.0f, 1.0f);
    const auto milli = static_cast<std::int32_t>(std::lround(clamped * 1000.0f));
    padLevelMilli_[padIndex].store(milli, std::memory_order_relaxed);

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " level: " + std::to_string(milli / 10) + "%";
}

float AudioEngine::padLevel(std::uint8_t padIndex) const {
    if (padIndex >= kPadCount) {
        return 1.0f;
    }

    return static_cast<float>(
            padLevelMilli_[padIndex].load(std::memory_order_relaxed))
            / 1000.0f;
}

std::string AudioEngine::setPadPan(
        std::uint8_t padIndex,
        float pan) {
    if (padIndex >= kPadCount || !std::isfinite(pan)) {
        return "Pan change failed: invalid value";
    }

    const float clamped = std::clamp(pan, -1.0f, 1.0f);
    const auto milli = static_cast<std::int32_t>(std::lround(clamped * 1000.0f));
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

    return static_cast<float>(
            padPanMilli_[padIndex].load(std::memory_order_relaxed))
            / 1000.0f;
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
    padSampleDescriptions_[padIndex][layerIndex] =
            std::to_string(padSamples_[padIndex][layerIndex]->sampleRate) + " Hz "
            + std::to_string(padSamples_[padIndex][layerIndex]->channelCount) + " ch "
            + std::to_string(padSamples_[padIndex][layerIndex]->frameCount()) + " frames";

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " layer " + std::to_string(static_cast<unsigned>(layerIndex + 1))
            + " sample loaded | "
            + padSampleDescriptions_[padIndex][layerIndex];
}

void AudioEngine::triggerPad(
        std::uint8_t padIndex,
        std::uint8_t velocity) {
    if (padIndex >= kPadCount || velocity == 0) {
        return;
    }

    padTriggerVelocity_[padIndex].store(
            velocity,
            std::memory_order_relaxed);
    padTriggerSequence_[padIndex].fetch_add(
            1,
            std::memory_order_release);
}

std::string AudioEngine::start() {
    if (stream_ != nullptr) {
        return status();
    }

    AudioEngine::SampleLayerGrid samples{};
    bool anySample = false;

    for (std::size_t pad = 0; pad < kPadCount; ++pad) {
        bool anyExplicitLayer = false;

        for (std::size_t layer = 0; layer < kSampleLayerCount; ++layer) {
            samples[pad][layer] = padSamples_[pad][layer];
            anyExplicitLayer = anyExplicitLayer
                    || (samples[pad][layer] != nullptr
                        && samples[pad][layer]->frameCount() > 0);
        }

        // Keep the existing bundled sample behavior for pads that have no
        // explicitly assigned layers. Once a pad has any explicit layer,
        // playback comes only from those assigned layers.
        if (!anyExplicitLayer && sample_ != nullptr) {
            samples[pad][0] = sample_;
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
            padTriggerSequence_,
            padTriggerVelocity_,
            padTuningMilliSemitones_,
            padLevelMilli_,
            padPanMilli_);

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
    if (stream_ == nullptr) {
        return "Audio stopped";
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
