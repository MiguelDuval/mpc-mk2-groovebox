# MPC Studio MkII Groovebox — Master Specification

## Product

A personal/free Android groovebox designed around Akai MPC Studio MkII hardware and modeled on the compact standalone workflow of MPC One/One+/Live II-class systems.

## Foundation

- C++20.
- JUCE 9.x.
- Tracktion Engine.
- Google Oboe.
- Ableton Link.
- Android MIDI/USB support.

## Core philosophy

The phone is the visual and compute surface.
The MPC Studio MkII is the physical performance surface.

The app must feel like a dedicated musical instrument, not a shrunken desktop DAW.

## Core domain

Project
→ Sequences
→ Tracks
→ Programs
→ Pads
→ Layers
→ Samples
→ Automation
→ Mixer
→ Q-Link mappings

## Required hardware integration

- 16 pads.
- velocity.
- aftertouch.
- RGB pad LEDs.
- button LEDs.
- touch strip.
- jog wheel.
- transport.
- LCD.
- SysEx.

## Required musical functions

- sampler;
- sample editor;
- drum programs;
- multi-layer samples;
- sequences/patterns;
- step sequencer;
- grid editor;
- mixer;
- pad mixer;
- Q-Link;
- Note Repeat;
- 16 Levels;
- Pad Perform;
- automation;
- project save/load;
- Ableton Link;
- USB audio.

## First acceptance criterion

MPC Studio MkII pad
→ MIDI
→ Tracktion-backed sampler
→ audio output
→ RGB pad feedback
→ LCD status.

## Legal/design rule

Implement independent code and artwork. Do not copy proprietary Akai firmware code, binary resources or graphics.

