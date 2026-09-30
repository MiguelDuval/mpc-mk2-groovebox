# MPC Studio MkII Control Contract

## Purpose

This document is the canonical control-surface contract for the Akai MPC Studio MkII in this project.

The product is built around the MPC Studio MkII as the primary physical performance surface. The Android display is the visual/compute surface. Hardware controls must therefore have stable semantic roles in the application even when the visible screen changes.

This document separates:

1. **Hardware identity** — what the physical control is.
2. **Transport identity** — how the control is represented on MIDI/SysEx.
3. **Semantic role** — what the control should do in this groovebox.
4. **Implementation state** — what exists in code now.
5. **Verification state** — what has been proven on the real controller.

Raw MIDI numbers belong in the hardware adapter/map only. UI, domain and audio code must consume semantic actions.

## Evidence and confidence

### Primary product behavior reference

The Akai MPC Studio II documentation describes the device as a controller for MPC software and documents its physical controls and secondary functions. The same documentation describes the Studio II touch strip, Q-Link controls, navigation controls, sampling controls and transport behavior.

### Protocol references

The repository hardware map is cross-checked against:

- bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts
- gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

These are reverse-engineered/practical mappings, not Akai firmware documentation.

**Physical hardware remains the final authority.** A protocol field can be documented and still remain unconfirmed for this application until tested on the real MPC Studio MkII.

## Hardware surface

### Navigation and data

| Physical control | Intended semantic role | Project status |
|---|---|---|
| Display / LCD | Hardware status mirror: page, selected pad/track, parameter, transport state, short messages | LCD transport is implemented/tested; application content is still a later stage |
| Data Dial | Contextual navigation and fine parameter editing; primary hardware value entry | MIDI rotation is decoded; semantic routing not implemented |
| Data Dial press | Enter/select/reset depending on active context | Hardware press is not yet represented as a dedicated semantic action |
| - / + | Increment/decrement current context value; coarse navigation | MIDI buttons decode; semantic routing not implemented |
| Undo / Redo | Project edit history | Undo input decodes; redo is Shift+Undo; semantic routing not implemented |
| Shift | Modifier for secondary printed functions | Input decodes; modifier routing not implemented |

### Pads and performance controls

| Physical control | Intended semantic role | Project status |
|---|---|---|
| 16 pads | Trigger/select notes and samples; velocity and pressure | **Implemented for pad triggering** |
| Pad Bank A-D | Select pad bank | Input decodes; bank state not implemented |
| Shift + Pad Bank = E-H | Select extended pad bank | Modifier path not implemented |
| Full Level | Force pad velocity to 127 | Feature not implemented |
| Shift + Full Level = Half Level | Force pad velocity to 64 | Feature not implemented |
| 16 Level | Map one sound across pads with a fixed ascending parameter | Feature/UI not implemented |
| Shift + 16 Level = Pad Perform | Scales/chords/progression performance | Feature/UI not implemented |
| Copy / Delete | Contextual copy/delete | Copy/delete semantics not implemented |
| Note Repeat | Hold to repeat pad at timing-correct rate | Sequencer foundation exists; hardware routing not implemented |
| Shift + Note Repeat = Latch | Latch Note Repeat | Not implemented |
| Q-Link 1-4 | Touch-sensitive contextual parameter control | Hardware protocol/routing not implemented |
| Q-Link button | Cycle Q-Link parameter column; Shift selects previous; hold exposes Q-Link context | Hardware routing not implemented |
| Touch Strip | Expressive continuous control | CC decoding exists; semantic modes not implemented |
| Touch Strip button | Select Touch Strip mode | Input decode exists |
| Pressing the strip | Dedicated touch/press action used by the practical controller mapping | **Missing from current control map; add explicit protocol support** |

### Mode and view controls

| Physical control | Primary action | Shift / alternate action |
|---|---|---|
| Mode | Hold, then press a pad for fast mode access | — |
| Main | Main mode | Track View |
| Track Select | Cycle/select track context | Sequence selection |
| Program Select | Select current program | Select track type |
| Browse | Browser | Save / parent-folder behavior depends on software context |
| Sample Select | Select sample on current pad | Cycle sample layers through the active context |
| Sample Start | Edit sample start | Loop start |
| Sample End | Edit sample end | — |
| Tune | Edit tuning | Fine tuning |
| Quantize | Quantize sequence events | Quantize selected events |
| TC On/Off | Timing Correct | Timing Correct configuration / Time Division / Swing |
| Zoom | Horizontal zoom | Vertical zoom |
| Pad Mute | Pad Mute mode | Track Mute mode |
| Step / Bar navigation | Move playhead by step or bar | Locate modifier changes the target to event/start/end navigation |
| Locate | Modifier for event/start/end navigation and location workflow | — |
| Erase | Quick note erase while playing; erase window when stopped | — |
| Automation Read/Write | Toggle automation mode | Global automation behavior where supported |

### Transport and recording

| Physical control | Intended semantic role | Project status |
|---|---|---|
| Record | Arm/enter record | Sequencer recording foundation exists; hardware routing not implemented |
| Overdub | Enable non-destructive recording | Sequencer overdub foundation exists; hardware routing not implemented |
| Stop | Stop transport; double-stop can silence lingering audio | Transport foundation is present in audio engine; physical routing not implemented |
| Play | Play from current playhead | Transport UI exists; physical routing not implemented |
| Play Start | Play from sequence start | Transport UI exists; physical routing not implemented |
| Tap Tempo | Set tempo by tapping | Tempo state exists in UI; physical routing not implemented |
| Shift + Tap Tempo | Local/master tempo context | Link/master-tempo model not yet complete |
| Seek Back / Seek Forward | Move playhead by one event/step context | Sequencer cursor/scheduler foundation exists |
| Nudge Left / Nudge Right | Fine event/playhead movement and future capture/automation-related actions | Reserved; semantic final role is context-dependent |

## Semantic architecture

The hardware path must be:

**MPC Studio MkII MIDI/SysEx**
→ **MPC Studio MkII hardware adapter**
→ **semantic controller event**
→ **application command / domain operation**
→ **UI/audio/sequencer state**
→ **hardware feedback**

Do not implement a raw MIDI note directly calling a Java button listener.

Instead:

**MIDI note 73**
→ **Record hardware control**
→ **Transport Record semantic action**
→ **sequencer/recording command**
→ **UI and hardware LED state**.

This preserves controller independence and keeps all raw numbers in one adapter.

## Recommended semantic action groups

### Group A — Live-critical / first binding

These are the first hardware actions to make genuinely usable because they are required continuously during playing:

- Play
- Play Start
- Stop
- Record
- Overdub
- Main
- Browse
- Data Dial + Enter
- +/- navigation
- Shift
- Pad Bank
- Note Repeat
- Pad Mute
- Tap Tempo
- Track Select
- Program Select

### Group B — Editing / production

- Undo / Redo
- Quantize
- Timing Correct
- Locate
- Step navigation
- Bar navigation
- Erase
- Sample Select
- Sample Start
- Sample End
- Tune / Fine
- Zoom
- Copy / Delete
- Automation Read/Write

### Group C — Performance and expressive control

- Full Level / Half Level
- 16 Level
- Pad Perform
- Next Sequence
- XYFX
- Q-Link
- Touch Strip modes
- Touch Strip configuration
- Bank E-H
- Track/Pad Mute

### Group D — Hardware feedback

- Pad RGB
- button LEDs
- Q-Link column indicators
- Touch Strip segment LEDs
- Note Repeat rate LEDs
- LCD pages and state

## Mapping principle for this groovebox

The controller should not force the application to copy every historical MPC screen literally.

Instead:

- physical controls keep their familiar musical meaning;
- the Android screen exposes the corresponding state clearly;
- secondary functions are context-sensitive but deterministic;
- the same semantic command must be usable from touch UI and hardware;
- live-critical operations must not require navigating the mode rail;
- the Data Dial is the principal hardware navigation/value control;
- Q-Link controls are reserved for direct musical parameters rather than navigation;
- touch strip is reserved for expressive continuous actions;
- pads remain available for performance even while editing.

## Current implementation gaps

The existing C++ decoder already understands:

- pad notes;
- pad pressure;
- generic buttons;
- jog rotation;
- jog press;
- touch strip CC;
- channel aftertouch.

The next hardware layer must turn generic button events into stable semantic identifiers. Current source does not yet do that. Q-Link controls are also absent from the current hardware map.

## Verification policy

A control is:

- **DOCUMENTED** when its behavior is supported by a reference.
- **DECODED** when the project parser recognizes the incoming MIDI/SysEx.
- **ROUTED** when it invokes a semantic application action.
- **FEEDBACK** when application state is sent back to the controller.
- **PHYSICALLY VERIFIED** only after the behavior is observed on the real MPC Studio MkII.

Do not collapse these states into one "implemented" flag.

## References

Official Akai manuals index:
https://www.akaipro.com/mpc-manuals

Model-specific Studio MkII manual (Manual Version 2.10.1 mirror):
https://www.manuals.co.uk/akai/mpc-studio-mk2/manual

Reverse-engineered MIDI/SysEx charts:
https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

Practical Ableton mapping:
https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script
