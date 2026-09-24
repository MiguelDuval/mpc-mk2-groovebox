#pragma once

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

    std::string start();
    std::string stop();
    std::string status() const;

private:
    class OutputCallback;

    std::shared_ptr<OutputCallback> callback_;
    std::shared_ptr<oboe::AudioStream> stream_;
};

} // namespace mpc::audio
