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
