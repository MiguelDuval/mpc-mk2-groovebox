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

## Controller-first control contract

The MPC Studio MkII is the primary physical performance surface for this project. The canonical semantic mapping is documented in [MPC-STUDIO-MK2-CONTROL-CONTRACT.md](MPC-STUDIO-MK2-CONTROL-CONTRACT.md).

The low-level hardware map covers 16 pads, 39 documented panel-button definitions, a separate Touch Strip Press control, jog wheel rotation/press and the touch strip CC. The practical controller mapping also distinguishes a **Touch Strip Press** action on MIDI note 78 from the separate Touch Strip mode-selection button. The adapter must keep these as separate controls.

The controller contract has four distinct implementation states:
- **decoded** — incoming MIDI/SysEx is recognized;
- **routed** — it invokes a semantic application action;
- **feedback** — application state is sent back to the controller;
- **physically verified** — the behavior is observed on the real MkII.

Q-Link knobs are first-class hardware controls and must be represented in the adapter before they are exposed to UI/domain code. Their exact transport messages remain a protocol-mapping task and must be tested on the physical controller before being marked confirmed.

The intended semantic flow is:

MPC Studio MkII → hardware adapter → semantic controller event → application command/domain → UI/audio/sequencer → hardware feedback.

Protocol rule:
Never scatter raw MIDI numbers through business logic.

Testing rule:
Treat reverse-engineered fields as CONFIRMED, PROBABLE or UNCONFIRMED and maintain a physical-device test log.
