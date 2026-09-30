# UI/UX DESIGN CONTRACT — MPC Studio MkII Groovebox

This document is the guardrail for the current horizontal touch UI.

## Core composition

Every page uses the same shell:

1. compact global top bar;
2. thin sequence-length/playhead strip immediately below it;
3. page-specific workspace.

The sequence strip is persistent across pages. It represents the active Sequence length, marks bar divisions, and moves the playhead during playback/recording. It must not become a tall timeline editor.

## Main

Main is a performance screen.

It contains:

- 16 playable pads;
- selected-pad/sample context;
- a compact selected-track context;
- direct navigation to Sample, Sequencer, Tracks and Recorder.

Main must not contain a large track matrix, pad mute/solo modes, or a collection of per-track cards above the pads.

Pads are performance controls. Track mute/solo/arm are track controls and must not be coupled to a special pad mode.

## Tracks

Tracks are independent of the drum-pad concept.

The primary user-facing track families are:

- SAMPLE — pad/sample playback;
- SYNTH — keygroup/plugin instruments;
- MIDI — external MIDI destinations;
- AUDIO — recorded/clip audio.

A Sequence can contain mixed track families. The interface must communicate the track type without implying that every track is a drum track.

Track management belongs on the dedicated Tracks/Sequencer surfaces. Each track row should make selection, mute, solo and arm easy to hit without turning the screen into a dense grid of tiny cards.

## Global transport

Use hierarchy, not a row of identical buttons.

- PLAY is a visually prominent transport control.
- REC is prominent and red.
- STOP is neutral and smaller.
- REPLACE/OVERDUB is a compact mode selector, not a large standalone DUB button.
- Track ARM belongs with the selected track, not in the global transport cluster.

Avoid adding secondary diagnostics as large top-bar blocks. Audio/MIDI state may remain compact.

## Reference principles

The interaction study draws from:

- Akai MPC workflows: compact Sequence/Track/Program hierarchy and dedicated transport/navigation;
- Native Instruments MASCHINE+: separate Sound/Group/Pattern concepts, dedicated sampling and transport controls;
- Ableton Push: overview-oriented track/clip views with dedicated Session/Mix/Clip modes.

Use these products as interaction references only. Do not copy proprietary graphics, artwork, firmware resources or source code.

## Change discipline

A future UI change must answer:

- What user task becomes faster?
- Where is the control physically placed?
- Does it preserve the Main performance surface?
- Does it add information that belongs on another page?
- Does it keep the red sequence strip persistent?

When a new feature can live in an existing page without adding a new permanent control, prefer the existing page.

## Acceptance for this redesign

The redesign is not accepted until:

- Main is visually simpler than the transport/track-system prototype;
- no "Drum Drums", "Drum Percs", "Hats", "Kick", etc. are used as separate default tracks;
- default tracks demonstrate mixed track families;
- the sequence strip is immediately under the top bar on every page;
- transport controls have clear size/color hierarchy;
- DUB is not a large global button;
- track mute/solo/arm are not implemented as pad modes on Main;
- the physical sampler path remains intact.
