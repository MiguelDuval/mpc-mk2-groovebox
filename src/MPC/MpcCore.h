#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/MpcProjectState.h"

namespace mpc {

class MpcCore final {
public:
    static MpcCore& instance();

    MpcCore(const MpcCore&) = delete;
    MpcCore& operator=(const MpcCore&) = delete;

    MpcProjectState& projectState() noexcept {
        return projectState_;
    }

    const MpcProjectState& projectState() const noexcept {
        return projectState_;
    }

    audio::AudioEngine& audio() noexcept {
        return audio_;
    }

    const audio::AudioEngine& audio() const noexcept {
        return audio_;
    }

private:
    MpcCore();

    MpcProjectState projectState_;
    audio::AudioEngine audio_;
};

} // namespace mpc
