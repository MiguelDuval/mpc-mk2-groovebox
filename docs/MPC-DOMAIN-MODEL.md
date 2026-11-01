# MPC Domain Model

The app uses a domain model deliberately closer to a standalone MPC than
to a generic desktop DAW.

Core hierarchy:

Project
→ Sequence
→ Track
→ Program
→ Pad
→ Sample Layer
→ Sample

A future Drum Program layer may expose 128 logical pad slots, while the
current implementation deliberately models the 16 physical MPC Studio MkII
pads as the first vertical slice. Physical bank/mode selection can later map
the controller surface onto a larger logical program without changing the
physical pad contract.

A Pad is capable of up to eight sample layers. Layers carry sample regions,
tuning, gain/pan and velocity ranges. Pad-level state carries trigger mode,
mute group, polyphony and velocity behavior.

Patterns own time-positioned MIDI note events. The event model already has
fields for velocity, probability and ratchet so the sequencer can grow toward
MPC-style performance without changing the core data shape.

This is an application-domain model. It is intentionally independent of
Tracktion Engine and Android. A later adapter will translate domain objects
to/from the audio engine.

The model is not an MPC file-format implementation and does not claim complete
XPJ/XPM compatibility.

Timing contract:

- Sequencer tick resolution is 960 ticks per quarter note in the current runtime foundation.
- A default 4/4 pattern of 3840 ticks therefore represents one bar at the domain level.
- Tick-to-frame conversion is kept in the sequencer layer and requires explicit tempo and output sample rate.
- The audio layer consumes frame offsets and does not own BPM or PPQN policy.
## 2026-10-02 MPC3 track/program semantic prerequisite

The target UI follows MPC3's documented unified Track/Program workflow.

The current domain still exposes Program references and a Track.programId. This is acceptable as an internal representation, but the UI must treat the selected Track/container as the authoritative instrument context.

Before exposing full Program Select, Track Type and Track Edit behavior:

- resolve the selected Track to its actual program/container;
- make playback use that resolved association rather than a global UI selection;
- make Track View, Main and Track Edit read the same selected-track state;
- represent shared/legacy program cases explicitly.

This is a semantic prerequisite for UI fidelity. It does not justify moving Tracktion or realtime audio objects into the domain model.

The permanent UI migration documents the required transition in:
docs/MPC3-UI-MIGRATION-MASTER-SPEC.md

