#pragma once

#include "Audio/AudioEngine.h"
#include "MPC/MpcProjectState.h"
#include "MPC/Sequencer/MpcSequenceRecordQueue.h"
#include "MPC/Sequencer/MpcSequenceRecorder.h"
#include "MPC/Sequencer/MpcSequenceTransport.h"

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

    sequencer::SequenceRecordQueue& sequenceRecordQueue() noexcept {
        return sequenceRecordQueue_;
    }

    sequencer::MpcSequenceRecorder& sequenceRecorder() noexcept {
        return sequenceRecorder_;
    }

    sequencer::MpcSequenceTransportClock& sequenceTransportClock() noexcept {
        return sequenceTransportClock_;
    }

private:
    MpcCore();

    MpcProjectState projectState_;
    audio::AudioEngine audio_;
    sequencer::SequenceRecordQueue sequenceRecordQueue_;
    sequencer::MpcSequenceRecorder sequenceRecorder_;
    sequencer::MpcSequenceTransportClock sequenceTransportClock_;
};

} // namespace mpc
