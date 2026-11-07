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

## Current development status

The repository contains an evolving Android/C++ groovebox implementation: MPC Studio MkII MIDI decoding/semantic routing, sample loading and editing foundations, sequencer and pattern operations, project persistence, local Browser flows and MPC-style UI workspaces. Individual capabilities have different evidence levels; consult the action catalog and live source rather than assuming every screen or backend is complete.

The immediate product objective is to connect the existing components into a complete instrument workflow: load a WAV, trigger it from the MPC Studio MkII, record/edit/play a pattern, and save/reopen the project.

## Development workflow

Read [`AGENTS.md`](AGENTS.md), then [`docs/FAST-DELIVERY-MODE.md`](docs/FAST-DELIVERY-MODE.md). The fast GitHub Actions lane builds and uploads an APK for ordinary iteration. The expensive native/JVM/emulator suite is reserved for deliberate milestone, high-risk and release validation. Do not create parallel stacked branches or stop product work for unrelated test-harness failures.

The active working branch is `checkpoint/mpc-ui-reference-and-product-20261030`; verify its live HEAD before every session. Keep `main` unchanged.

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
