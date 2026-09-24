#pragma once

#include <array>
#include <cstdint>

namespace mpc::studio {

struct ButtonDefinition {
    const char* name;
    std::uint8_t midiNote;
    std::uint8_t ledCc;
};

struct PadDefinition {
    std::uint8_t physicalIndex;
    std::uint8_t midiNote;
};

inline constexpr std::array<PadDefinition, 16> pads{{
    {0, 37}, {1, 36}, {2, 42}, {3, 82},
    {4, 40}, {5, 38}, {6, 46}, {7, 44},
    {8, 48}, {9, 47}, {10, 45}, {11, 43},
    {12, 49}, {13, 55}, {14, 51}, {15, 53}
}};

inline constexpr std::array<ButtonDefinition, 39> buttons{{
    {"TouchStripButton", 0, 0},
    {"PadMute", 4, 4},
    {"Erase", 9, 9},
    {"NoteRepeat", 11, 11},
    {"Quantize", 12, 12},
    {"TrackSelect", 13, 13},
    {"ProgramSelect", 14, 14},
    {"TCOnOff", 15, 15},
    {"SampleStart", 33, 33},
    {"SampleEnd", 34, 34},
    {"PadBankAE", 35, 35},
    {"PadBankBF", 36, 36},
    {"PadBankCG", 37, 37},
    {"PadBankDH", 38, 38},
    {"FullLevel", 39, 39},
    {"Level16", 40, 40},
    {"SampleSelect", 42, 42},
    {"Shift", 49, 49},
    {"Browse", 50, 50},
    {"Main", 52, 52},
    {"TapTempo", 53, 53},
    {"Plus", 54, 54},
    {"Minus", 55, 55},
    {"Zoom", 66, 66},
    {"Undo", 67, 67},
    {"NudgeLeft", 68, 68},
    {"NudgeRight", 69, 69},
    {"Locate", 70, 70},
    {"SeekBack", 71, 71},
    {"SeekForward", 72, 72},
    {"Record", 73, 73},
    {"AutomationReadWrite", 75, 75},
    {"Tune", 79, 79},
    {"Overdub", 80, 80},
    {"Stop", 81, 81},
    {"Play", 82, 82},
    {"PlayStart", 83, 83},
    {"Mode", 114, 114},
    {"Copy", 122, 122}
}};

inline constexpr std::uint8_t buttonChannel = 0;
inline constexpr std::uint8_t padChannel = 9;
inline constexpr std::uint8_t jogWheelCc = 100;
inline constexpr std::uint8_t jogWheelPressNote = 111;
inline constexpr std::uint8_t touchStripCc = 33;

inline constexpr std::array<std::uint8_t, 9> touchStripLedCcs{
    57, 58, 59, 60, 61, 62, 63, 64, 65
};

inline constexpr std::array<std::uint8_t, 8> noteRepeatLedCcs{
    103, 104, 105, 106, 107, 108, 109, 110
};

} // namespace mpc::studio
