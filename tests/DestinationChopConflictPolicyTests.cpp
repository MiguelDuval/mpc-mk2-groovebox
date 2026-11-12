#include "Audio/DestinationChopConflictPolicy.h"

#include <cassert>
#include <cstdint>
#include <memory>

using namespace mpc::audio;

// Complete the forward-declared SampleBuffer so this host test can build and
// compare real runtime buffers without pulling in AudioEngine or Oboe.
namespace mpc::audio {
struct SampleBuffer final {
    std::uint32_t sampleRate = 44100;
    std::uint16_t channelCount = 1;
};
} // namespace mpc::audio

namespace {

mpc::domain::SampleId sampleId(std::uint32_t value) {
    mpc::domain::SampleId id{};
    id.value = value;
    return id;
}

ChopSampleGrid emptyGrid() {
    ChopSampleGrid grid{};
    return grid;
}

// Destination pad 2 layer 0 retains an assigned domain SampleId after
// Project Save/Open while its PCM runtime pointer is null: exactly the
// SAMP-009 collision that must be rejected without mutation.
void testAssignedDomainSampleIdWithNullRuntimeIsRejected() {
    mpc::domain::DrumProgram program{};
    program.pad(2).layer(0).sample = sampleId(42);

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 4, emptyGrid(), program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 2);
    assert(conflicts[0].layerIndex == 0);
    assert(conflicts[0].domainSampleAssigned);
    assert(!conflicts[0].runtimeBufferOccupied);
}

// Same source SampleId with no runtime buffer on the destination is the
// same-source case: allowed (post-reload re-chop onto the source identity).
void testSameSourceSampleIdNullRuntimeIsAllowed() {
    mpc::domain::DrumProgram program{};
    program.pad(1).layer(0).sample = sampleId(7);

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 4, emptyGrid(), program);
    assert(conflicts.empty());
}

// Same source runtime buffer on the destination is allowed: first CH4 then
// CH8/CH16 keep chopping onto pads that already reference the source buffer.
void testSameSourceRuntimeBufferIsAllowed() {
    mpc::domain::DrumProgram program{};
    const auto source = std::make_shared<const mpc::audio::SampleBuffer>();

    ChopSampleGrid grid = emptyGrid();
    grid[3][0] = source;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, source, sampleId(7), 8, grid, program);
    assert(conflicts.empty());
}


// Different assigned domain SampleId with null runtime buffer is rejected:
void testDifferentAssignedSampleIdIsRejected() {
    mpc::domain::DrumProgram program{};
    program.pad(5).layer(0).sample = sampleId(9);

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 8, emptyGrid(), program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 5);
    assert(conflicts[0].layerIndex == 0);
    assert(conflicts[0].domainSampleAssigned);
    assert(!conflicts[0].runtimeBufferOccupied);
}

// Conflicting runtime buffer on layer 1 is rejected even when the domain
// layer is unassigned.
void testConflictingRuntimeBufferIsRejected() {
    mpc::domain::DrumProgram program{};
    const auto other = std::make_shared<const mpc::audio::SampleBuffer>();

    ChopSampleGrid grid = emptyGrid();
    grid[6][0] = other;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 8, grid, program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 6);
    assert(conflicts[0].layerIndex == 0);
    assert(!conflicts[0].domainSampleAssigned);
    assert(conflicts[0].runtimeBufferOccupied);
}

// Exact source pad/layer is excluded: chopping onto itself never conflicts.
void testExactSourceLayerIsExcluded() {
    mpc::domain::DrumProgram program{};
    const auto source = std::make_shared<const mpc::audio::SampleBuffer>();
    program.pad(0).layer(1).sample = sampleId(7);

    ChopSampleGrid grid = emptyGrid();
    grid[0][1] = source;

    const auto conflicts = findChopDestinationConflicts(
            0, 1, source, sampleId(7), 4, grid, program);
    assert(conflicts.empty());
}

// Assigned extra layers (1..7) are rejected on destinations.
void testAssignedExtraLayersAreRejected() {
    mpc::domain::DrumProgram program{};
    program.pad(9).layer(3).sample = sampleId(11);

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 16, emptyGrid(), program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 9);
    assert(conflicts[0].layerIndex == 3);
    assert(conflicts[0].domainSampleAssigned);
}

// Occupied extra runtime buffers are rejected too.
void testOccupiedExtraLayerIsRejected() {
    mpc::domain::DrumProgram program{};
    const auto buffer = std::make_shared<const mpc::audio::SampleBuffer>();

    ChopSampleGrid grid = emptyGrid();
    grid[11][7] = buffer;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 16, grid, program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 11);
    assert(conflicts[0].layerIndex == 7);
    assert(conflicts[0].runtimeBufferOccupied);
}

// All destinations are validated: multiple conflicts are collected, none
// silently skipped, and nothing is mutated (the policy is pure).
void testAllConflictsAreCollectedBeforeMutation() {
    mpc::domain::DrumProgram program{};
    program.pad(1).layer(0).sample = sampleId(41);
    program.pad(3).layer(0).sample = sampleId(42);
    program.pad(4).layer(2).sample = sampleId(43);
    const auto other = std::make_shared<const mpc::audio::SampleBuffer>();
    ChopSampleGrid grid = emptyGrid();
    grid[7][0] = other;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, nullptr, sampleId(7), 16, grid, program);
    assert(conflicts.size() == 4);
    assert(conflicts[0].padIndex == 1 && conflicts[0].layerIndex == 0);
    assert(conflicts[1].padIndex == 3 && conflicts[1].layerIndex == 0);
    assert(conflicts[2].padIndex == 4 && conflicts[2].layerIndex == 2);
    assert(conflicts[3].padIndex == 7 && conflicts[3].layerIndex == 0);
}

// Same domain SampleId with a different runtime pointer is allowed:
// SampleId identity survives PCM rehydration even when buffers differ.
void testSameSampleIdDifferentRuntimeIsAllowed() {
    mpc::domain::DrumProgram program{};
    program.pad(2).layer(0).sample = sampleId(42);

    const auto source = std::make_shared<const mpc::audio::SampleBuffer>();
    const auto dest = std::make_shared<const mpc::audio::SampleBuffer>();

    ChopSampleGrid grid = emptyGrid();
    grid[2][0] = dest;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, source, sampleId(42), 4, grid, program);
    assert(conflicts.empty());
}

// Different domain SampleId with the same runtime pointer is rejected:
// domain SampleId is authoritative; pointer aliasing never hides a mismatch.
void testDifferentSampleIdSameRuntimeIsRejected() {
    mpc::domain::DrumProgram program{};
    program.pad(2).layer(0).sample = sampleId(43);

    const auto shared = std::make_shared<const mpc::audio::SampleBuffer>();

    ChopSampleGrid grid = emptyGrid();
    grid[2][0] = shared;

    const auto conflicts = findChopDestinationConflicts(
            0, 0, shared, sampleId(42), 4, grid, program);
    assert(conflicts.size() == 1);
    assert(conflicts[0].padIndex == 2);
    assert(conflicts[0].layerIndex == 0);
    assert(conflicts[0].domainSampleAssigned);
    assert(conflicts[0].runtimeBufferOccupied);
}

// Fully free destination grid yields no conflicts.
void testFreeDestinationsYieldNoConflicts() {
    mpc::domain::DrumProgram program{};
    assert(findChopDestinationConflicts(
                   0, 0, nullptr, sampleId(7), 16, emptyGrid(), program)
                   .empty());
}

} // namespace

int main() {
    testAssignedDomainSampleIdWithNullRuntimeIsRejected();
    testSameSourceSampleIdNullRuntimeIsAllowed();
    testSameSourceRuntimeBufferIsAllowed();
    testSameSampleIdDifferentRuntimeIsAllowed();
    testDifferentSampleIdSameRuntimeIsRejected();
    testDifferentAssignedSampleIdIsRejected();
    testConflictingRuntimeBufferIsRejected();
    testExactSourceLayerIsExcluded();
    testAssignedExtraLayersAreRejected();
    testOccupiedExtraLayerIsRejected();
    testAllConflictsAreCollectedBeforeMutation();
    testFreeDestinationsYieldNoConflicts();
    return 0;
}
