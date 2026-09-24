#include "MPC/MpcStudioMk2InputDecoder.h"

#include <cassert>
#include <initializer_list>
#include <cstdint>
#include <optional>
#include <span>

namespace {

using mpc::studio::InputEvent;
using mpc::studio::InputEventType;

InputEvent decode(std::initializer_list<std::uint8_t> bytes) {
    const auto event = mpc::studio::decodeInput(
        std::span<const std::uint8_t>(bytes.begin(), bytes.size()));

    assert(event.has_value());
    return *event;
}

void testPadVelocity() {
    const auto event = decode({0x99, 37, 100});

    assert(event.type == InputEventType::PadNote);
    assert(event.channel == 9);
    assert(event.number == 37);
    assert(event.value == 100);
    assert(event.padIndex == 0);
    assert(event.pressed);
}

void testPadRelease() {
    const auto event = decode({0x89, 37, 0});

    assert(event.type == InputEventType::PadNote);
    assert(event.padIndex == 0);
    assert(!event.pressed);
}

void testPadAftertouch() {
    const auto event = decode({0xA9, 37, 81});

    assert(event.type == InputEventType::PadAftertouch);
    assert(event.number == 37);
    assert(event.value == 81);
    assert(event.padIndex == 0);
}

void testButton() {
    const auto event = decode({0x90, 82, 127});

    assert(event.type == InputEventType::Button);
    assert(event.channel == 0);
    assert(event.number == 82);
    assert(event.value == 127);
    assert(event.pressed);
}

void testJogWheel() {
    const auto event = decode({0xB0, 100, 1});

    assert(event.type == InputEventType::JogWheel);
    assert(event.number == 100);
    assert(event.value == 1);
}

void testJogPress() {
    const auto event = decode({0x90, 111, 127});

    assert(event.type == InputEventType::JogPress);
    assert(event.number == 111);
    assert(event.pressed);
}

void testTouchStrip() {
    const auto event = decode({0xB0, 33, 64});

    assert(event.type == InputEventType::TouchStrip);
    assert(event.number == 33);
    assert(event.value == 64);
}

void testChannelAftertouch() {
    const auto event = decode({0xD0, 50});

    assert(event.type == InputEventType::ChannelAftertouch);
    assert(event.channel == 0);
    assert(event.value == 50);
}

void testUnknownMessageIsIgnored() {
    const std::uint8_t bytes[] = {0x99, 99, 100};

    const auto event = mpc::studio::decodeInput(
        std::span<const std::uint8_t>(bytes, sizeof(bytes)));

    assert(!event.has_value());
}

} // namespace

int main() {
    testPadVelocity();
    testPadRelease();
    testPadAftertouch();
    testButton();
    testJogWheel();
    testJogPress();
    testTouchStrip();
    testChannelAftertouch();
    testUnknownMessageIsIgnored();
    return 0;
}
