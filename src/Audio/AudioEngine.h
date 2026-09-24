#pragma once

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
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

    std::string start();
    std::string stop();
    std::string status() const;

    // Queues a single-shot musical trigger for one physical MPC pad.
    // The request is consumed by the realtime audio callback without locks.
    void triggerPad(std::uint8_t padIndex, std::uint8_t velocity);

private:
    static constexpr std::size_t kPadCount = 16;

    class OutputCallback;

    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStream> stream_;
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerSequence_{};
    std::array<std::atomic<std::uint32_t>, kPadCount> padTriggerVelocity_{};
};

} // namespace mpc::audio
