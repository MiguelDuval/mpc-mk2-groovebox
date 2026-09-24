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

A Drum Program reserves 128 logical pad slots while MPC Studio MkII exposes
16 physical pads at a time. Physical bank/mode selection maps the controller
surface onto the logical program.

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
