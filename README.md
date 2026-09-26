# MPC Studio MkII Groovebox

Android standalone-style groovebox centered on the Akai MPC Studio MkII.

## Product direction

The target is a compact, hardware-oriented workflow inspired by the standalone MPC One/One+/Live II family, not a shrunken MPC Desktop workstation.

The Android device is the visual/compute surface. The MPC Studio MkII is the primary physical performance surface.

## Planned foundation

- Tracktion Engine — audio/sequencing foundation.
- JUCE 9.0.2 — native application/UI framework.
- Google Oboe 1.10.0 — Android low-latency audio layer.
- Ableton Link 4.0 — network tempo/transport synchronization.
- Android MIDI service — USB/BLE/virtual MIDI transport.

## Current development branches

- main — stable reference.
- foundation/mpc-studio-mk2 — repository architecture/bootstrap.
- feature/midi-transport — stable hardware/MIDI foundation reference.
- feature/sampler — current sampler/audio implementation work.

## Current stage

The current sampler branch builds on the MIDI foundation and adds:

- PCM WAV decoding and bounded sample playback.
- Per-pad / per-layer sample assignment with up to 8 layers.
- Per-pad tuning, level and pan controls.
- Bounded microphone recording into RAM.
- Independent microphone monitoring with separate Monitor On/Off controls.
- Assignment of the last recording to a selected pad/layer.
- Non-destructive per-pad/layer sample regions with a small diagnostic trim editor.
- Automated native decoder/region tests plus Android UI hierarchy/interaction smoke coverage.

The full MPC-style sampler UI, waveform editing and chop workflow are still later stages.

## First hardware milestone

Connect the MPC Studio MkII by USB OTG.

The first diagnostic APK is intended to:

1. list MIDI devices;
2. select the MPC Studio MkII public port;
3. show incoming MIDI messages;
4. forward those messages to the native C++ boundary.

Next hardware tests are pad velocity/aftertouch, RGB pad SysEx,
button LEDs, touch-strip feedback, jog wheel and the 160x80 LCD.

## Research references

Tracktion Engine:
https://github.com/Tracktion/tracktion_engine

JUCE:
https://github.com/juce-framework/JUCE

Oboe:
https://github.com/google/oboe

Ableton Link:
https://github.com/Ableton/link

MPC Studio MkII protocol:
https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

MPC Studio MkII practical mapping:
https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

MPC project file research:
https://github.com/kurtjcu/MPC-project-file-definitions

Official MPC standalone UX reference:
https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf

## Agent instructions

The canonical project operating brief is [docs/PROJECT_MASTER_PROMPT.md](docs/PROJECT_MASTER_PROMPT.md).

Coding agents should first read [AGENTS.md](AGENTS.md) and then the master prompt plus the relevant documents under `docs/`.
