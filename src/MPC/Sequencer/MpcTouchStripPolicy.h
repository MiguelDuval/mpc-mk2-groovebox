#pragma once
#include <array>
#include <cstdint>
namespace mpc::sequencer::touch_strip {
enum class Mode : std::uint8_t { Level=0, Pan=1, Tune=2, SampleStart=3, SampleEnd=4 };
inline constexpr std::array<const char*,5> kModeLabels{{"LEVEL","PAN","TUNE","SAMPLE START","SAMPLE END"}};
[[nodiscard]] constexpr Mode nextMode(Mode mode) noexcept {
 switch(mode){case Mode::Level:return Mode::Pan;case Mode::Pan:return Mode::Tune;case Mode::Tune:return Mode::SampleStart;case Mode::SampleStart:return Mode::SampleEnd;case Mode::SampleEnd:return Mode::Level;}
 return Mode::Level;
}
[[nodiscard]] constexpr std::int32_t modeIndex(Mode mode) noexcept{return static_cast<std::int32_t>(mode);}
[[nodiscard]] constexpr std::int32_t modeIndicatorSegment(Mode mode) noexcept{return modeIndex(mode)*2;}
[[nodiscard]] constexpr std::int32_t valueIndicatorSegment(std::uint8_t value) noexcept{return (static_cast<std::int32_t>(value)*9)/128;}
} // namespace mpc::sequencer::touch_strip
