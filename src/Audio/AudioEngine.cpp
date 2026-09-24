#include "AudioEngine.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>

namespace {

constexpr std::size_t kPadCount = 16;
constexpr float kToneFrequencyHz = 440.0f;
constexpr float kToneAmplitude = 0.04f;
constexpr float kPadBaseFrequencyHz = 150.0f;
constexpr float kPadFrequencyStepHz = 24.0f;
constexpr float kPadAmplitude = 0.11f;
constexpr float kPadDecaySeconds = 0.18f;
constexpr float kTwoPi = 6.2831853071795864769f;

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
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence,
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity)
            : triggerSequence_(triggerSequence),
              triggerVelocity_(triggerVelocity) {
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
        const float probePhaseStep =
                kTwoPi * kToneFrequencyHz / static_cast<float>(sampleRate);
        const float padDecayPerSample =
                1.0f / (static_cast<float>(sampleRate) * kPadDecaySeconds);

        for (std::size_t pad = 0; pad < kPadCount; ++pad) {
            const std::uint32_t sequence =
                    triggerSequence_[pad].load(std::memory_order_acquire);

            if (sequence != consumedSequence_[pad]) {
                consumedSequence_[pad] = sequence;

                const std::uint32_t velocity =
                        triggerVelocity_[pad].load(std::memory_order_relaxed);

                auto& voice = voices_[pad];
                voice.phase = 0.0f;
                voice.envelope = 1.0f;
                voice.phaseStep =
                        kTwoPi * (kPadBaseFrequencyHz
                                  + static_cast<float>(pad) * kPadFrequencyStepHz)
                        / static_cast<float>(sampleRate);
                voice.amplitude =
                        (static_cast<float>(std::min<std::uint32_t>(velocity, 127u))
                         / 127.0f) * kPadAmplitude;
                voice.active = velocity != 0;
            }
        }

        for (int32_t frame = 0; frame < numFrames; ++frame) {
            float mix = std::sin(probePhase_) * kToneAmplitude;
            probePhase_ += probePhaseStep;

            if (probePhase_ >= kTwoPi) {
                probePhase_ -= kTwoPi;
            }

            for (auto& voice : voices_) {
                if (!voice.active) {
                    continue;
                }

                mix += std::sin(voice.phase) * voice.amplitude * voice.envelope;
                voice.phase += voice.phaseStep;

                if (voice.phase >= kTwoPi) {
                    voice.phase -= kTwoPi;
                }

                voice.envelope -= padDecayPerSample;

                if (voice.envelope <= 0.0f) {
                    voice.envelope = 0.0f;
                    voice.active = false;
                }
            }

            mix = std::clamp(mix, -0.90f, 0.90f);

            for (int32_t channel = 0; channel < channelCount; ++channel) {
                output[frame * channelCount + channel] = mix;
            }
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    struct PadVoice {
        float phase = 0.0f;
        float phaseStep = 0.0f;
        float amplitude = 0.0f;
        float envelope = 0.0f;
        bool active = false;
    };

    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence_;
    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity_;
    std::array<std::uint32_t, kPadCount> consumedSequence_{};
    std::array<PadVoice, kPadCount> voices_{};
    float probePhase_ = 0.0f;
};

AudioEngine::AudioEngine() = default;

AudioEngine::~AudioEngine() {
    stop();
}

AudioEngine& AudioEngine::instance() {
    static AudioEngine engine;
    return engine;
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

    callback_ = std::make_shared<OutputCallback>(
            padTriggerSequence_,
            padTriggerVelocity_);

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
        return "Audio stopped";
    }

    return std::string("Audio output ")
        + streamStateText(stream_->getState())
        + " | API=" + oboe::convertToText(stream_->getAudioApi())
        + " | rate=" + std::to_string(stream_->getSampleRate())
        + " | channels=" + std::to_string(stream_->getChannelCount())
        + " | burst=" + std::to_string(stream_->getFramesPerBurst())
        + " | low-latency shared";
}

} // namespace mpc::audio
