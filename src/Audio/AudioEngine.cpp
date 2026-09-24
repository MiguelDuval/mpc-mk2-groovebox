#include "AudioEngine.h"

#include <algorithm>
#include <cmath>
#include <cstdint>
#include <string>
#include <utility>

namespace {

constexpr std::size_t kPadCount = 16;
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
            std::array<std::shared_ptr<const SampleBuffer>, kPadCount> samples,
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence,
            std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity)
            : samples_(std::move(samples)),
              triggerSequence_(triggerSequence),
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

        for (std::size_t pad = 0; pad < kPadCount; ++pad) {
            const std::uint32_t sequence =
                    triggerSequence_[pad].load(std::memory_order_acquire);

            if (sequence != consumedSequence_[pad]) {
                consumedSequence_[pad] = sequence;

                const std::uint32_t velocity =
                        triggerVelocity_[pad].load(std::memory_order_relaxed);

                auto& voice = voices_[pad];
                voice.position = 0.0;
                voice.gain =
                        static_cast<float>(
                            std::min<std::uint32_t>(velocity, 127u))
                        / 127.0f
                        * kPadAmplitude;

                const auto& sample = samples_[pad];
                if (sample == nullptr
                        || sample->frameCount() == 0
                        || sample->channelCount == 0) {
                    voice.active = false;
                    continue;
                }

                // Each pad keeps the original chromatic behavior unless it is
                // later given an explicit musical tuning control. The important
                // change in this slice is that every pad now owns its sample slot.
                const float semitoneRatio =
                        std::pow(2.0f, static_cast<float>(pad) / 12.0f);
                const float sampleToOutputRate =
                        static_cast<float>(sample->sampleRate)
                        / static_cast<float>(sampleRate);
                voice.positionStep =
                        sampleToOutputRate * semitoneRatio;
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

                const auto& sample = samples_[pad];
                if (sample == nullptr || sample->frameCount() == 0) {
                    voice.active = false;
                    continue;
                }

                const std::size_t sourceFrame =
                        static_cast<std::size_t>(voice.position);

                if (sourceFrame >= sample->frameCount()) {
                    voice.active = false;
                    continue;
                }

                const std::size_t nextFrame =
                        std::min(sourceFrame + 1, sample->frameCount() - 1);
                const float fraction =
                        static_cast<float>(
                            voice.position
                            - static_cast<double>(sourceFrame));

                if (sample->channelCount == 1) {
                    const float sample0 =
                            sample->sampleAt(sourceFrame, 0);
                    const float sample1 =
                            sample->sampleAt(nextFrame, 0);
                    const float value =
                            sample0 + (sample1 - sample0) * fraction;
                    left += value * voice.gain;
                    right += value * voice.gain;
                } else {
                    const float left0 =
                            sample->sampleAt(sourceFrame, 0);
                    const float left1 =
                            sample->sampleAt(nextFrame, 0);
                    const float right0 =
                            sample->sampleAt(sourceFrame, 1);
                    const float right1 =
                            sample->sampleAt(nextFrame, 1);

                    left += (left0 + (left1 - left0) * fraction) * voice.gain;
                    right += (right0 + (right1 - right0) * fraction) * voice.gain;
                }

                voice.position += voice.positionStep;
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
        double position = 0.0;
        float positionStep = 0.0f;
        float gain = 0.0f;
        bool active = false;
    };

    std::array<std::shared_ptr<const SampleBuffer>, kPadCount> samples_;
    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerSequence_;
    std::array<std::atomic<std::uint32_t>, kPadCount>& triggerVelocity_;
    std::array<std::uint32_t, kPadCount> consumedSequence_{};
    std::array<PadVoice, kPadCount> voices_{};
};

AudioEngine::AudioEngine() = default;

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

std::string AudioEngine::loadSampleForPad(
        std::span<const std::uint8_t> bytes,
        std::uint8_t padIndex) {
    if (stream_ != nullptr) {
        return "Stop audio before loading a sample";
    }

    if (padIndex >= kPadCount) {
        return "Sample load failed: invalid pad";
    }

    const auto decoded = decodeWav(bytes);
    if (!decoded.has_value()) {
        return "Sample load failed: unsupported or invalid PCM WAV";
    }

    padSamples_[padIndex] = std::make_shared<SampleBuffer>(*decoded);
    padSampleDescriptions_[padIndex] =
            std::to_string(padSamples_[padIndex]->sampleRate) + " Hz "
            + std::to_string(padSamples_[padIndex]->channelCount) + " ch "
            + std::to_string(padSamples_[padIndex]->frameCount()) + " frames";

    return "Pad " + std::to_string(static_cast<unsigned>(padIndex + 1))
            + " sample loaded | "
            + padSampleDescriptions_[padIndex];
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

    std::array<std::shared_ptr<const SampleBuffer>, kPadCount> samples;
    bool anySample = false;

    for (std::size_t pad = 0; pad < kPadCount; ++pad) {
        samples[pad] = padSamples_[pad] != nullptr
                ? padSamples_[pad]
                : sample_;
        anySample = anySample
                || (samples[pad] != nullptr && samples[pad]->frameCount() > 0);
    }

    if (!anySample) {
        return "Audio start failed: no sample loaded";
    }

    callback_ = std::make_shared<OutputCallback>(
            std::move(samples),
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
        if (sample_ == nullptr) {
            return "Audio stopped | no sample loaded";
        }

        std::string result =
                "Audio stopped | fallback=" + sampleDescription_;

        for (std::size_t pad = 0; pad < kPadCount; ++pad) {
            if (!padSampleDescriptions_[pad].empty()) {
                result += " | pad" + std::to_string(pad + 1)
                        + "=" + padSampleDescriptions_[pad];
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
