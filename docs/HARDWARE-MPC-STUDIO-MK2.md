# Akai MPC Studio MkII Hardware Reference

Primary reverse-engineering source:
https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

Secondary practical mapping/reference:
https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

Relevant documented controls:

- 16 velocity-sensitive pads.
- Pad aftertouch/pressure.
- Physical buttons expressed as MIDI notes.
- Button LED feedback through MIDI CC.
- RGB pad feedback through SysEx.
- Jog wheel rotation via CC 100.
- Jog wheel press as note 111.
- Touch strip position via CC 33.
- Touch-strip LED feedback through CCs.
- 160x80 LCD feedback through chunked SysEx PNG transport.

Important mappings confirmed in the public charts:

- Button channel: MIDI channel 1 (zero-based channel 0 in code).
- Pad channel: MIDI channel 10 (zero-based channel 9 in code).
- Pad notes are not visually sequential; use the explicit hardware map.

Protocol rule:
Never scatter raw MIDI numbers through business logic.

Testing rule:
Treat reverse-engineered fields as CONFIRMED, PROBABLE or UNCONFIRMED and maintain a physical-device test log.


## Canonical semantic mapping

The complete product mapping contract is maintained in:

**[MPC Studio MkII Semantic Control Map](MPC-STUDIO-MKII-SEMANTIC-MAP.md)**

That document is the source of truth for the distinction between:

- physical controller control;
- MIDI protocol;
- semantic application action;
- UI/context;
- current implementation status;
- implementation priority;
- physical-test confidence.

Important current interpretation:

- the physical pads are the only controller inputs currently used as working musical controls;
- the other physical controls are not “unimportant”: they are already decoded or protocol-mapped in the repository, but their semantic routing is still a planned hardware-bring-up slice;
- the hardware adapter must model Shift, Mode, Locate and other contextual combinations as stateful semantic chords rather than leaking raw MIDI values into the UI;
- the source-level names `NudgeLeft/NudgeRight` and `SeekBack/SeekForward` are legacy names for the physical Step and Bar navigation buttons. They must not be treated as the sequencer's separate Nudge parameter;
- the 2021 MkII has no dedicated Q-Link knob bank. Q-Link remains an application feature, but the MkII mapping must use controls that physically exist on this controller.



## Semantic hardware checkpoint — 2026-09-30

The repository now contains an explicit `MpcStudioMk2SemanticAdapter` between physical MIDI decoding and application behavior. It owns stateful Shift, Mode, Locate, Note Repeat, Full/Half Level, mute contexts and pad-bank semantics, while Android receives stable semantic actions.

The design deliberately preserves the actual MkII surface: Data Dial and Touch Strip are the real continuous controls; no fictional dedicated Q-Link knob bank is introduced.

The sequence transport path now preserves a stopped playhead position when Play is pressed, and mute state is applied without tearing down the active playback session. These are live-workflow behaviors, not cosmetic mappings.

Physical-device verification is still required before repository-level probable mappings are promoted to CONFIRMED.
