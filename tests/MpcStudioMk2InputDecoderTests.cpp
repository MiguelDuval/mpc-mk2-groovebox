#include "MPC/MpcStudioMk2InputDecoder.h"
#include "MPC/MpcStudioMk2LedProtocol.h"

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

void testLcdPayloadEncoding() {
    const std::uint8_t source[] = {0x00, 0x7F, 0x80, 0xFF, 0x01, 0x02, 0x03, 0x04};

    const auto encoded = mpc::studio::encodeLcdPayload(
        std::span<const std::uint8_t>(source, sizeof(source)));

    assert(encoded.size() == 10);
    assert(encoded[0] == 0x0C);
    assert(encoded[1] == 0x00);
    assert(encoded[2] == 0x7F);
    assert(encoded[3] == 0x00);
    assert(encoded[4] == 0x7F);
    assert(encoded[7] == 0x03);
    assert(encoded[8] == 0x00);
    assert(encoded[9] == 0x04);
}

void testLcdChunkHeader() {
    const std::uint8_t png[] = {0, 1, 2, 3, 4, 5, 6, 7};
    const auto message = mpc::studio::makeLcdChunkSysEx(
        mpc::studio::lcdChunks[0],
        std::span<const std::uint8_t>(png, sizeof(png)));

    assert(message.size() == 26);
    assert(message[0] == 0xF0);
    assert(message[1] == 0x47);
    assert(message[2] == 0x7F);
    assert(message[3] == 0x4A);
    assert(message[4] == 0x04);
    assert(message[5] == 0x00);
    assert(message[6] == 0x12);
    assert(message[7] == 0x00);
    assert(message[8] == 0x20);
    assert(message[9] == 0x00);
    assert(message[10] == 0x00);
    assert(message[11] == 0x00);
    assert(message[12] == 0x00);
    assert(message[13] == 0x00);
    assert(message[14] == 0x08);
    assert(message[15] == 0x00);
    assert(message[16] == 0x00);
    assert(message[17] == 0x01);
    assert(message[21] == 0x05);
    assert(message[22] == 0x06);
    assert(message[23] == 0x00);
    assert(message[24] == 0x07);
    assert(message[25] == 0xF7);

    for (std::size_t i = 1; i + 1 < message.size(); ++i) {
        assert(message[i] < 0x80);
    }
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
    testLcdPayloadEncoding();
    testLcdChunkHeader();
    testUnknownMessageIsIgnored();
    return 0;
}
