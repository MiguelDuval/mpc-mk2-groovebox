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
