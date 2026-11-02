#pragma once

#include <atomic>
#include <cstdint>
#include <string>

namespace mpc::audio {

/**
 * Platform-independent metronome implementation.
 * No Oboe/Android dependency; safe to compile and test on any host.
 * Realtime callback reads atomic snapshots only; control thread writes
 * configuration via public setters.
 */
class Metronome final {
public:
    Metronome() = default;

    // --- Control-thread API ---

    std::string setEnabled(bool enabled);
    [[nodiscard]] bool enabled() const noexcept;

    std::string setVolume(float volume);   // 0.0..1.0
    [[nodiscard]] float volume() const noexcept;

    std::string setSound(int soundIndex);  // 0=click,1=beep,2=wood,3=none
    [[nodiscard]] int sound() const noexcept;

    void setTempo(double bpm) noexcept;
    [[nodiscard]] double tempo() const noexcept;

    void setTimeSignature(int numerator, int denominator) noexcept;
    [[nodiscard]] int timeSigNumerator() const noexcept;
    [[nodiscard]] int timeSigDenominator() const noexcept;

    // --- Reference getters for realtime callback binding ---
    // Return references to atomic members so the output callback can bind
    // them in its constructor initializer list, avoiding method-call overhead
    // in the realtime path.
    std::atomic<bool>& enabledRef() noexcept { return enabled_; }
    std::atomic<std::int32_t>& volumeMilliRef() noexcept { return volumeMilli_; }
    std::atomic<std::int32_t>& soundRef() noexcept { return sound_; }
    std::atomic<double>& tempoBpmRef() noexcept { return tempoBpm_; }
    std::atomic<int>& timeSigNumeratorRef() noexcept { return timeSigNumerator_; }

    // --- Realtime callback state (owned by callback thread) ---
    // These are written only by the realtime output callback.
    std::atomic<bool> resetRequested{false};
    std::uint32_t framesRemaining = 0;
    double phase = 0.0;
    int beatInBar = 0;

private:
    std::atomic<bool> enabled_{false};
    std::atomic<std::int32_t> volumeMilli_{800};   // 0..1000 -> 0..1.0
    std::atomic<std::int32_t> sound_{0};           // 0=click,1=beep,2=wood,3=none
    std::atomic<double> tempoBpm_{120.0};
    std::atomic<int> timeSigNumerator_{4};
    std::atomic<int> timeSigDenominator_{4};
};

} // namespace mpc::audio
