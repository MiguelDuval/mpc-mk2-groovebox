#pragma once

#include "MPC/Domain/MpcDomain.h"

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

    domain::SampleId registerSample(
            std::string name,
            std::string path,
            double sampleRate,
            std::int64_t lengthSamples);

    const domain::SampleRef* findSample(domain::SampleId id) const noexcept;

private:
    domain::Project project_{};
    std::size_t activeDrumProgramIndex_ = 0;
    std::uint32_t nextSampleId_ = 1;
};

} // namespace mpc
