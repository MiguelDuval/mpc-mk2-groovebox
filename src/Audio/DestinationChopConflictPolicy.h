#pragma once

#include "MPC/Domain/MpcDomain.h"
#include "MPC/Domain/SampleRegion.h"

#include <array>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <vector>

namespace mpc::audio {

class SampleBuffer;

// Grid type mirroring AudioEngine::padSamples_ so this policy can be
// unit-tested without AudioEngine, Oboe or any host API.
using ChopSampleGrid =
        std::array<std::array<std::shared_ptr<const SampleBuffer>,
                mpc::domain::kMaxSampleLayers>,
                mpc::domain::kMaxProgramPads>;

struct ChopDestinationConflict final {
    std::uint16_t padIndex = 0;          // 0-based destination pad
    std::size_t layerIndex = 0;          // 0-based layer inside the pad
    bool domainSampleAssigned = false;   // DrumProgram layer SampleId retained
    bool runtimeBufferOccupied = false;  // padSamples_ PCM pointer occupied
};

// Read-only destination-conflict preflight for chopPadSampleToPads.
// Every destination is checked against BOTH identities before any mutation:
//   * the persisted DrumProgram layer SampleId (domain assignment identity),
//   * the padSamples_ runtime PCM buffer (runtime pointer identity).
//
// Project Save/Open can retain a domain SampleId while the PCM runtime
// pointer is null; that retained assignment must conflict just like an
// occupied runtime buffer. The exact same source SampleId or source buffer
// is allowed on every destination so a source can be re-chopped onto itself
// (first CH4, then CH8/CH16 onto pads that already reference the source).
// The exact source pad/layer is excluded because chopping overwrites itself
// in place. Extra layers (1..7) must be free on every destination.
//
// Returns all conflicts; the check is pure and mutates nothing.
inline std::vector<ChopDestinationConflict> findChopDestinationConflicts(
        std::uint8_t sourcePadIndex,
        std::uint8_t sourceLayerIndex,
        const std::shared_ptr<const SampleBuffer>& sourceSample,
        mpc::domain::SampleId sourceSampleId,
        std::size_t destinationCount,
        const ChopSampleGrid& padSamples,
        const mpc::domain::DrumProgram& program,
        mpc::domain::SampleRegion sourceRegion = {}) {
    std::vector<ChopDestinationConflict> conflicts;
    const std::size_t sourcePad =
            static_cast<std::size_t>(sourcePadIndex);
    const std::size_t sourceLayer =
            static_cast<std::size_t>(sourceLayerIndex);

    for (std::size_t destinationPad = 0;
            destinationPad < destinationCount;
            ++destinationPad) {
        // Extra layers must be free on every destination. Chopping onto the
        // exact source pad/layer overwrites itself in place and is excluded.
        for (std::size_t layer = 1;
                layer < mpc::domain::kMaxSampleLayers;
                ++layer) {
            if (destinationPad == sourcePad && layer == sourceLayer) {
                continue;
            }
            const auto& layerState =
                    program.pad(destinationPad).layer(layer);
            const bool domainAssigned = layerState.sample.isAssigned();
            const bool runtimeOccupied =
                    padSamples[destinationPad][layer] != nullptr;
            if (domainAssigned || runtimeOccupied) {
                conflicts.push_back(ChopDestinationConflict{
                        static_cast<std::uint16_t>(destinationPad), layer,
                        domainAssigned, runtimeOccupied});
            }
        }

        // Layer 1: SampleId is authoritative when present. Same SampleId +
        // same region is allowed (rehydration-safe); same SampleId +
        // different region is rejected; different SampleId is rejected
        // regardless of region/pointer aliasing. Only when no domain
        // identity exists do we fall back to runtime pointer equality.
        const auto& destinationLayer =
                program.pad(destinationPad).layer(0);
        const bool domainAssigned = destinationLayer.sample.isAssigned();
        const bool runtimeOccupied =
                padSamples[destinationPad][0] != nullptr;
        if (!domainAssigned && !runtimeOccupied) {
            continue;
        }
        const bool domainConflict =
                domainAssigned
                && (!sourceSampleId.isAssigned()
                    || destinationLayer.sample.value != sourceSampleId.value
                    || destinationLayer.region.startFrame
                            != sourceRegion.startFrame
                    || destinationLayer.region.endFrame
                            != sourceRegion.endFrame);
        const bool runtimeConflict =
                !domainAssigned && runtimeOccupied
                && (sourceSample == nullptr
                    || padSamples[destinationPad][0] != sourceSample);
        if (domainConflict || runtimeConflict) {
            conflicts.push_back(ChopDestinationConflict{
                    static_cast<std::uint16_t>(destinationPad), 0,
                    domainAssigned, runtimeOccupied});
        }
    }
    return conflicts;
}

} // namespace mpc::audio
