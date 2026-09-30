#include "MpcProjectState.h"

namespace mpc {

MpcProjectState::MpcProjectState() {
    project_.id = "project-1";
    project_.name = "Untitled";

    domain::DrumProgram program;
    program.id = "drum-program-1";
    program.name = "Drum Program 1";
    program.type = domain::ProgramType::Drum;

    for (std::size_t pad = 0; pad < domain::kMaxProgramPads; ++pad) {
        program.pads[pad].index = static_cast<std::uint16_t>(pad);
        program.pads[pad].midiNote = static_cast<std::uint8_t>(36 + pad);
        program.pads[pad].name = "Pad " + std::to_string(pad + 1);
    }

    project_.drumPrograms.push_back(std::move(program));
}

domain::SampleId MpcProjectState::registerSample(
        std::string name,
        std::string path,
        double sampleRate,
        std::int64_t lengthSamples) {
    if (nextSampleId_ == 0) {
        nextSampleId_ = 1;
    }

    const auto id = domain::SampleId{nextSampleId_++};

    domain::SampleRef ref;
    ref.id = "sample-" + std::to_string(id.value);
    ref.name = name.empty()
            ? ("Sample " + std::to_string(id.value))
            : std::move(name);
    ref.path = std::move(path);
    ref.sampleRate = sampleRate;
    ref.lengthSamples = lengthSamples;
    ref.assetId = id;
    project_.samples.push_back(std::move(ref));

    return id;
}

const domain::SampleRef* MpcProjectState::findSample(
        domain::SampleId id) const noexcept {
    if (!id.isAssigned()) {
        return nullptr;
    }

    for (const auto& sample : project_.samples) {
        if (sample.assetId.value == id.value) {
            return &sample;
        }
    }

    return nullptr;
}

} // namespace mpc
