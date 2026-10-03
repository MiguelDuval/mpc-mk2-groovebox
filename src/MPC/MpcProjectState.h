#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/Sequencer/MpcSequenceSettings.h"

#include <cstddef>
#include <cstdint>
#include <string>
#include <utility>

namespace mpc {

class MpcProjectState final {
public:
    MpcProjectState();

    MpcProjectState(const MpcProjectState&) = delete;
    MpcProjectState& operator=(const MpcProjectState&) = delete;

    domain::Project& project() noexcept {
        return project_;
    }

    const domain::Project& project() const noexcept {
        return project_;
    }

    domain::DrumProgram& activeDrumProgram() noexcept {
        return project_.drumPrograms[activeDrumProgramIndex_];
    }

    const domain::DrumProgram& activeDrumProgram() const noexcept {
        return project_.drumPrograms[activeDrumProgramIndex_];
    }

    domain::Sequence& activeSequence() noexcept {
        return project_.sequences[activeSequenceIndex_];
    }

    const domain::Sequence& activeSequence() const noexcept {
        return project_.sequences[activeSequenceIndex_];
    }

    std::size_t activeSequenceIndex() const noexcept {
        return activeSequenceIndex_;
    }

    std::size_t sequenceCount() const noexcept {
        return project_.sequences.size();
    }

    bool selectSequence(std::size_t sequenceIndex) noexcept;
    bool selectNextSequence() noexcept;
    bool selectPreviousSequence() noexcept;
    bool addSequence(
            std::string name = {}) ;

    std::size_t activeTrackIndex() const noexcept {
        return activeTrackIndex_;
    }

    bool selectTrack(std::size_t trackIndex) noexcept;
    bool setTrackProgram(
            std::size_t trackIndex,
            std::string programId);

    domain::DrumProgram* findDrumProgram(
            const std::string& programId) noexcept;
    const domain::DrumProgram* findDrumProgram(
            const std::string& programId) const noexcept;

    std::size_t activeProgramIndexForTrack(
            std::size_t trackIndex) const noexcept;

    bool setSequenceTempo(double tempoBpm) noexcept;
    bool setSequenceBars(std::int32_t bars) noexcept;
    bool setSequenceTimeSignature(
            std::int32_t numerator,
            std::int32_t denominator) noexcept;
    bool setSequenceLoop(
            bool enabled,
            std::int32_t startBar,
            std::int32_t endBar) noexcept;
    bool setSequenceQuantizeGrid(std::int32_t gridTicks) noexcept;
    bool setSequenceSwing(std::int32_t swingPercent) noexcept;

    bool addTrack(
            domain::TrackKind kind,
            std::string name = {}) ;
    
    std::string sequenceStatus() const;
    std::string trackStatus(std::size_t trackIndex) const;

    domain::SampleId registerSample(
            std::string name,
            std::string path,
            double sampleRate,
            std::int64_t lengthSamples);

    const domain::SampleRef* findSample(domain::SampleId id) const noexcept;

private:
    domain::Project project_{};
    std::size_t activeDrumProgramIndex_ = 0;
    std::size_t activeSequenceIndex_ = 0;
    std::size_t activeTrackIndex_ = 0;
    std::uint32_t nextSampleId_ = 1;
};

} // namespace mpc
