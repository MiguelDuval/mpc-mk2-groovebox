#include "Metronome.h"

#include <algorithm>
#include <cmath>
#include <string>

namespace mpc::audio {

std::string Metronome::setEnabled(bool enabled) {
    enabled_.store(enabled, std::memory_order_release);
    if (!enabled) {
        resetRequested.store(true, std::memory_order_release);
    }
    return std::string("METRONOME • ")
           + (enabled ? "ON" : "OFF")
           + " • " + std::to_string(
                   static_cast<int>(
                           volumeMilli_.load(std::memory_order_relaxed) / 10))
           + "%";
}

bool Metronome::enabled() const noexcept {
    return enabled_.load(std::memory_order_relaxed);
}

std::string Metronome::setVolume(float volume) {
    const float clamped = std::clamp(volume, 0.0f, 1.0f);
    volumeMilli_.store(
            static_cast<std::int32_t>(std::lround(clamped * 1000.0f)),
            std::memory_order_relaxed);
    return std::string("METRONOME VOLUME • ")
           + std::to_string(static_cast<int>(clamped * 100.0f)) + "%";
}

float Metronome::volume() const noexcept {
    return volumeMilli_.load(std::memory_order_relaxed) / 1000.0f;
}

std::string Metronome::setSound(int soundIndex) {
    if (soundIndex < 0 || soundIndex > 3) {
        return "Metronome sound failed: invalid mode";
    }
    sound_.store(static_cast<std::int32_t>(soundIndex), std::memory_order_relaxed);
    static const char* names[] = {"click", "beep", "wood", "none"};
    return std::string("METRONOME SOUND • ") + names[soundIndex];
}

int Metronome::sound() const noexcept {
    return static_cast<int>(sound_.load(std::memory_order_relaxed));
}

void Metronome::setTempo(double bpm) noexcept {
    const double clamped = std::clamp(bpm, 20.0, 300.0);
    tempoBpm_.store(clamped, std::memory_order_relaxed);
}

double Metronome::tempo() const noexcept {
    return tempoBpm_.load(std::memory_order_relaxed);
}

void Metronome::setTimeSignature(int numerator, int denominator) noexcept {
    if (numerator > 0) {
        timeSigNumerator_.store(numerator, std::memory_order_relaxed);
    }
    if (denominator > 0) {
        timeSigDenominator_.store(denominator, std::memory_order_relaxed);
    }
}

int Metronome::timeSigNumerator() const noexcept {
    return timeSigNumerator_.load(std::memory_order_relaxed);
}

int Metronome::timeSigDenominator() const noexcept {
    return timeSigDenominator_.load(std::memory_order_relaxed);
}

} // namespace mpc::audio
