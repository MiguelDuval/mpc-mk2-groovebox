#include "AudioEngine.h"

#include <cmath>
#include <cstdint>
#include <string>

namespace {

constexpr float kToneFrequencyHz = 440.0f;
constexpr float kToneAmplitude = 0.04f;
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
        const float phaseStep = kTwoPi * kToneFrequencyHz
                / static_cast<float>(sampleRate);

        for (int32_t frame = 0; frame < numFrames; ++frame) {
            const float sample = std::sin(phase_) * kToneAmplitude;
            phase_ += phaseStep;

            if (phase_ >= kTwoPi) {
                phase_ -= kTwoPi;
            }

            for (int32_t channel = 0; channel < channelCount; ++channel) {
                output[frame * channelCount + channel] = sample;
            }
        }

        return oboe::DataCallbackResult::Continue;
    }

private:
    float phase_ = 0.0f;
};

AudioEngine::AudioEngine() = default;

AudioEngine::~AudioEngine() {
    stop();
}

std::string AudioEngine::start() {
    if (stream_ != nullptr) {
        return status();
    }

    callback_ = std::make_shared<OutputCallback>();

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
