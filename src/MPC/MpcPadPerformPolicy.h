#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>

namespace mpc::studio::pad_perform {

inline constexpr std::int32_t kMinimumOctaveShift = -2;
inline constexpr std::int32_t kMaximumOctaveShift = 2;
inline constexpr std::int32_t kMaximumPitchOffsetSemitones = 48;
inline constexpr std::int32_t kRootNoteCount = 12;

enum class Scale : std::uint8_t {
    Chromatic = 0,
    Major = 1,
    NaturalMinor = 2,
    MajorPentatonic = 3,
    MinorPentatonic = 4
};

[[nodiscard]] inline constexpr bool isValidScaleIndex(
        std::int32_t index) noexcept {
    return index >= static_cast<std::int32_t>(Scale::Chromatic)
            && index <= static_cast<std::int32_t>(Scale::MinorPentatonic);
}


[[nodiscard]] inline constexpr bool isValidRootNoteIndex(
        std::int32_t index) noexcept {
    return index >= 0 && index < kRootNoteCount;
}

// Pad order follows the 4x4 physical/touchscreen grid from Pad 1 to Pad 16.
// Diatonic and pentatonic modes continue upward through successive octaves.
[[nodiscard]] inline constexpr std::int32_t semitoneOffset(
        std::uint8_t padIndex,
        std::int32_t scaleIndex,
        std::int32_t octaveShift,
        std::int32_t rootNoteIndex = 0) noexcept {
    constexpr std::int32_t chromatic[] = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
    constexpr std::int32_t major[] = {
            0, 2, 4, 5, 7, 9, 11};
    constexpr std::int32_t naturalMinor[] = {
            0, 2, 3, 5, 7, 8, 10};
    constexpr std::int32_t majorPentatonic[] = {
            0, 2, 4, 7, 9};
    constexpr std::int32_t minorPentatonic[] = {
            0, 3, 5, 7, 10};

    const std::int32_t* degrees = chromatic;
    std::size_t degreeCount = sizeof(chromatic) / sizeof(chromatic[0]);
    switch (isValidScaleIndex(scaleIndex)
            ? static_cast<Scale>(scaleIndex)
            : Scale::Chromatic) {
        case Scale::Major:
            degrees = major;
            degreeCount = sizeof(major) / sizeof(major[0]);
            break;
        case Scale::NaturalMinor:
            degrees = naturalMinor;
            degreeCount = sizeof(naturalMinor) / sizeof(naturalMinor[0]);
            break;
        case Scale::MajorPentatonic:
            degrees = majorPentatonic;
            degreeCount = sizeof(majorPentatonic) / sizeof(majorPentatonic[0]);
            break;
        case Scale::MinorPentatonic:
            degrees = minorPentatonic;
            degreeCount = sizeof(minorPentatonic) / sizeof(minorPentatonic[0]);
            break;
        case Scale::Chromatic:
        default:
            break;
    }

    const auto index = static_cast<std::size_t>(padIndex % 16u);
    const auto octave = std::clamp(
            octaveShift, kMinimumOctaveShift, kMaximumOctaveShift);
    const auto root = isValidRootNoteIndex(rootNoteIndex) ? rootNoteIndex : 0;
    const auto relative = degrees[index % degreeCount]
            + static_cast<std::int32_t>(index / degreeCount) * 12
            + octave * 12 + root;
    return std::clamp(
            relative,
            -kMaximumPitchOffsetSemitones,
            kMaximumPitchOffsetSemitones);
}

} // namespace mpc::studio::pad_perform
