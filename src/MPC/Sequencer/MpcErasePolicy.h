#pragma once
#include "MPC/Domain/MpcDomain.h"
#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <optional>
#include <span>
namespace mpc::sequencer::erase {
[[nodiscard]] inline std::int64_t normalizeTick(std::int64_t tick,std::int64_t lengthTicks) noexcept {
 if(lengthTicks<=0) return 0;
 tick%=lengthTicks; return tick<0?tick+lengthTicks:tick;
}
[[nodiscard]] inline std::int64_t circularDistance(std::int64_t a,std::int64_t b,std::int64_t lengthTicks) noexcept {
 if(lengthTicks<=0) return 0;
 const auto lhs=normalizeTick(a,lengthTicks), rhs=normalizeTick(b,lengthTicks);
 const auto direct=std::llabs(lhs-rhs); return std::min(direct,lengthTicks-direct);
}
[[nodiscard]] inline std::optional<std::size_t> nearestEventIndex(
 std::span<const domain::MidiNoteEvent> notes,std::uint8_t noteNumber,
 std::int64_t playheadTicks,std::int64_t lengthTicks,std::int64_t maxDistanceTicks) noexcept {
 if(notes.empty()||lengthTicks<=0||maxDistanceTicks<0) return std::nullopt;
 std::optional<std::size_t> bestIndex; std::int64_t bestDistance=0;
 for(std::size_t index=0;index<notes.size();++index){
  const auto& note=notes[index];
  if(note.note!=noteNumber||note.velocity==0) continue;
  const auto effectiveTick=normalizeTick(static_cast<std::int64_t>(note.tick)+static_cast<std::int64_t>(note.nudgeTicks),lengthTicks);
  const auto distance=circularDistance(effectiveTick,playheadTicks,lengthTicks);
  if(distance>maxDistanceTicks||(bestIndex.has_value()&&distance>=bestDistance)) continue;
  bestDistance=distance; bestIndex=index;
 }
 return bestIndex;
}
inline bool eraseNearestEvent(domain::Pattern& pattern,std::uint8_t noteNumber,std::int64_t playheadTicks,std::int64_t maxDistanceTicks){
 const auto index=nearestEventIndex(std::span<const domain::MidiNoteEvent>(pattern.notes.data(),pattern.notes.size()),noteNumber,playheadTicks,pattern.lengthTicks,maxDistanceTicks);
 if(!index.has_value()) return false; pattern.notes.erase(pattern.notes.begin()+*index); return true;
}
} // namespace mpc::sequencer::erase
