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
- 16 Level / Pad Perform dual-function button (normal / Shift-modified performance context).
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



## Current semantic-routing gap

The public controller documentation distinguishes the Touch Strip itself from the Touch Strip / Config button. The strip has a press/touch event on MIDI Note 78 and continuous position on CC 33. The current decoder already recognizes CC 33, but the Note 78 press is not yet exposed as a dedicated input event. This is a known P0/P1 hardware-adapter gap and must be implemented before the Touch Strip / Config workflow is considered complete. citeturn397999search0turn397999search1

The controller also has a dual-purpose 16 Level / Pad Perform control. Normal press enters 16 Level, while Shift + press enters Pad Perform. 16 Level uses the data dial or +/- to select the parameter whose value is distributed across the sixteen pads; Pad Perform changes the pad matrix into a musical performance surface. citeturn548440view0

The complete semantic contract for both controls is maintained in docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md.
