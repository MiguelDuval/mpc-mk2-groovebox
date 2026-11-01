# Architecture

## Product boundary

MPC Studio MkII is the primary hardware surface. Android supplies the visual/compute surface and the audio engine.

The visual surface is **landscape/horizontal by project requirement**. Screen architecture must assume a horizontal canvas; portrait orientation is not a supported product layout.

## Layering

Android shell
→ native C++ application
→ MPC Studio MkII hardware adapter
→ MPC domain model
→ Tracktion Engine adapter
→ audio graph/device layer
→ JUCE/Oboe
→ Android audio subsystem

Hardware code must never depend directly on Tracktion objects.

The domain model must use semantic controls, not raw MIDI numbers.

The UI must dispatch semantic actions rather than MIDI bytes.

## First vertical slice

MPC Studio pad
→ MIDI input
→ hardware adapter
→ sampler
→ audio output
→ RGB pad feedback
→ LCD status.

## Dependency posture

JUCE, Tracktion Engine, Oboe and Ableton Link are pinned as upstream submodules. Tracktion/JUCE integration is intentionally gated while the Android CMake boundary is being proven.
## 2026-10-02 MPC 3.9 UI migration boundary

The Android presentation layer is now a permanent MPC 3.9 interaction-architecture migration.

Target dependency direction:

UI state/navigation
→ semantic application commands
→ existing MPC domain
→ Tracktion/audio adapters
→ realtime audio

The UI may request domain state and semantic commands, but it must not reach directly into realtime engine objects.

The migration is intentionally allowed to reorganize Android presentation classes extensively while preserving the lower layers.

Protected by default:

- realtime audio callback;
- sample playback/rendering;
- sequencer scheduler/clock;
- MIDI transport;
- raw MkII decoder;
- hardware feedback protocol;
- Tracktion/JUCE/Oboe realtime boundaries.

A lower-layer change is justified only when the existing semantic model cannot represent truthful MPC 3.9 state, such as per-track program/container ownership.

Canonical UI documents:

- docs/MPC3-UI-MIGRATION-MASTER-SPEC.md
- docs/MPC3-UI-SCREEN-BLUEPRINT.md
- docs/MPC3-REFERENCE-INDEX.md
- docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md

