# Unified Waveform System

## Purpose

Waveform is a first-class interaction surface of the groovebox, not a decorative image.

The same component is intended to serve:

- Sample Editor;
- Recorder;
- future Looper;
- audio-clip editing;
- future Grid/Sequence audio lanes;
- slice/chop workflows.

The design target is a compact, high-information waveform that remains usable on a phone in landscape orientation and can also be controlled from the MPC Studio MkII hardware.

## Research-derived interaction model

The current design follows documented interaction patterns used by modern standalone music instruments:

- MPC and Force keep the waveform visible while Start/End and processing controls are edited;
- Start and End can be adjusted directly by dragging waveform markers;
- zoom and horizontal navigation are essential for precise edits;
- Maschine separates recording, editing and slicing contexts but keeps the selected audio visible;
- Push uses waveform visibility as the primary context for audio clips and sample playback.

This project uses those functional principles while implementing original graphics and code.

## Shared data contract

The native audio layer exposes compact waveform peak envelopes rather than raw PCM to the Android renderer.

A waveform point contains:

- minimum amplitude;
- maximum amplitude.

Static samples are analyzed on the control thread.

Live recording uses a preallocated atomic peak buffer updated by the capture callback. The UI never reads the concurrently-written recording PCM buffer.

This keeps waveform updates out of the realtime output callback and avoids large JNI PCM transfers.

## UI component

WaveformView owns presentation and direct manipulation:

- min/max peak rendering;
- centered amplitude line;
- time ruler;
- selected play/edit range;
- Start (S) marker;
- End (E) marker;
- live recording state;
- recording endpoint marker;
- optional playhead;
- zoom in/out;
- pinch zoom;
- horizontal pan when zoomed;
- large touch hit zones around edit handles.

The component does not own audio state or Android lifecycle state.

## Current sampler behavior

The waveform represents the complete selected sample.

The S and E handles edit the pad/layer playback region.

A committed drag updates the domain-backed sample region.

FULL REGION restores the complete range.

CROP creates a new PCM buffer containing the selected range.

CHOP 4/8/16 divides the selected range into deterministic equal regions.

AUDITION triggers the selected pad without leaving the editor.

## Current recorder behavior

The recorder uses the same waveform component in read-only/live mode.

During recording it displays:

- WAITING FOR AUDIO when no signal has reached the capture threshold;
- a growing live waveform;
- the recording endpoint;
- duration;
- peak level;
- frame count;
- recording/armed/stopped state.

After stopping, the waveform remains visible until the next recording begins.

## Future extensions

The shared component is intentionally designed so later slices can add:

- independent selection range versus playback range;
- loop start/end markers;
- zero-crossing snap;
- slice markers;
- transient markers;
- beat/bar rulers;
- playback cursor;
- scrub/prelisten;
- non-destructive undo history;
- time-stretch/warp visualization;
- stereo dual-lane rendering where useful.

These belong to future feature slices; they should not be faked in the current implementation.

## UX rules

- The waveform must occupy meaningful screen area.
- Editing handles must be finger-addressable.
- Destructive operations must be visually and semantically distinct.
- The active range must be obvious without relying only on text.
- A recording must visibly prove whether audio is entering the application.
- Empty/error/waiting states must be distinguishable.
- The same waveform component should be reused rather than creating one-off renderers for each mode.
