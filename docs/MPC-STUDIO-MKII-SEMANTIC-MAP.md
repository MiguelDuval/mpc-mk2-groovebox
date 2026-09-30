# MPC Studio MkII Semantic Control Map

## Purpose

This document is the canonical product-level mapping contract for the **Akai MPC Studio MkII** in MPC MK2 Groovebox.

The controller is the primary physical performance surface. The Android device is the display, compute and audio host. The goal is not to clone Akai firmware or MPC Desktop; it is to reproduce the useful interaction model of a compact standalone groovebox with an original implementation.

This document bridges four things:

1. the physical control on the MkII;
2. its documented / reverse-engineered MIDI representation;
3. the **semantic action** that our application should receive;
4. the UI/context and implementation priority for that action.

Raw MIDI values belong only in the hardware adapter/control map. UI and domain code must consume semantic actions.

## Evidence and confidence

The hardware protocol is reconstructed from independent public observation and documentation. The primary protocol source is:

- https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

A second practical implementation reference is:

- https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

Akai's controller documentation establishes the intended control semantics, including transport, navigation, sample editing, pad-bank behavior, Mode shortcuts, Timing Correct, and the touch strip:

- https://www.akaipro.com/
- https://support.akaipro.com/
- MPC Studio MkII Quickstart Guide (public mirror used for verification):
  https://imagescdn.juno.co.uk/manual/846279-01U.pdf

Confidence labels in this repository mean:

- **CONFIRMED** — verified on the real MPC Studio MkII.
- **PROBABLE** — supported by multiple independent sources, but not yet physically verified.
- **UNCONFIRMED** — plausible but not safe to treat as production behavior.

For this project, the real controller remains the final authority.

## Current repository state

At the hardware-mapping research checkpoint:

- physical pads are the only controller inputs currently used as working musical controls;
- pad velocity and physical pad numbering are already part of the working path;
- the native decoder recognizes the documented button-note, jog, jog-press, touch-strip CC and pad-aftertouch message families;
- non-pad P0 controls now pass through the semantic adapter and the shared application command surface; remaining gaps are explicitly marked by control section below;
- button LED, touch-strip LED, Note Repeat LED and LCD message generators exist, but are not yet a complete state-driven feedback subsystem.

This distinction is important: **protocol support is not the same thing as functional mapping**.

## Hardware inventory

### A. Pads

**16 velocity/pressure-sensitive pads**

Physical numbering is canonical in our domain:

```
13 14 15 16
 9 10 11 12
 5  6  7  8
 1  2  3  4
```

Current hardware MIDI channel is 10 (zero-based channel 9). Pad note numbers are intentionally non-sequential and are already isolated in `MpcStudioMk2ControlMap.h`.

Semantic contract:

- normal mode: trigger the selected pad/sample;
- recording: the same trigger becomes a sequencer note event when record is armed;
- launcher mode: address the visible Sequence bank instead of triggering the drum pad;
- Mode-held: invoke the printed mode shortcut;
- Shift-held: perform the documented secondary pad behavior for the active context;
- Note Repeat-held: repeat the selected pad from the project clock;
- Locate-held/toggled: pads become locator set/jump targets;
- Pad Mute / Track Mute / 16 Levels / other contextual modes: pads become the active performance matrix for that mode.

Status: **WORKING for normal pad performance and the existing Sequence Launcher path**.

Priority: **P0**.

### B. Data Dial / Jog

Physical control:

- rotary jog/data dial;
- push action.

MIDI:

- rotation: CC 100;
- press: Note 111;
- button channel: 1 (zero-based 0).

Observed reverse-engineering behavior: clockwise produces value 1, counter-clockwise value 127.

Primary semantic contract:

- **Track navigation context**: select track;
- **Program navigation context**: select program / track type;
- **Sample navigation context**: change selected sample/parameter;
- **Browser context**: move through results/folders;
- **Parameter context**: edit the currently selected value;
- **SEQ/GRID/STEP context**: move selection/cursor or edit the currently focused value;
- press: **Enter / commit / open focused context**.

The exact context should be selected by the application, not by a raw MIDI note number.

Status: **SEMANTICALLY ROUTED — Data Dial and +/- share context-aware adjustment/selection behavior.**.

Priority: **P0**.

### C. +/- buttons

MIDI notes 54 and 55.

Akai semantics: increase/decrease the selected display field. They are the small-granularity companion to the data dial.

Semantic contract:

- adjust the current focused parameter;
- move selection when the current context defines a discrete selector;
- preserve the same context chosen by the most recent semantic navigation command.

Status: **SEMANTICALLY ROUTED — +/- emit the same focused adjustment command family as the Data Dial.**.

Priority: **P0**.

### D. Shift modifier

MIDI note 49.

Shift is not a page by itself. It is a modifier layer and must therefore be modeled as state.

Semantic contract:

- Shift + button invokes the button's secondary function;
- Shift + pad invokes the documented secondary pad behavior;
- Shift + Mode/other modifiers may be used for context-specific shortcuts where explicitly documented;
- release returns to the previous operating context unless the secondary action is a documented toggle.

Implementation rule: never implement Shift combinations by checking raw MIDI numbers inside unrelated screens. The hardware adapter emits a semantic chord/action.

Status: **SEMANTICALLY ROUTED as a stateful Shift modifier in the native hardware adapter.**.

Priority: **P0**.

### E. Mode

MIDI note 114.

Akai uses Mode as a momentary shortcut modifier: hold Mode and press a pad to jump directly to one of sixteen printed destinations.

For our product the canonical destinations are:

| Physical pad | Printed Akai destination | Groovebox semantic destination | Current UI status | Priority |
|---|---|---|---|---|
| 1 | Track View | MAIN / track context | foundation exists | P0 |
| 2 | Grid Editor | SEQ → GRID | implemented | P0 |
| 3 | Wave Editor | SAMPLE waveform | implemented | P0 |
| 4 | List Editor | MENU / future event-list context | reserved | P2 |
| 5 | Sample Edit | SAMPLE → EDIT | implemented foundation | P0 |
| 6 | Program Edit | MENU / future Program editor | reserved | P2 |
| 7 | Pad Mixer | MIX → PAD | foundation exists | P1 |
| 8 | Channel Mixer | MIX → TRACK | foundation reserved | P1 |
| 9 | Next Sequence | SEQ → LAUNCH | implemented | P0 |
| 10 | Song Mode | future arrangement/song view | reserved | P3 |
| 11 | MIDI Control Mode | future MIDI/control routing | reserved | P3 |
| 12 | Media/Browser | BROWSE | foundation exists | P0 |
| 13 | Sampler | SAMPLE / REC context | foundation exists | P0 |
| 14 | Looper | future LOOP mode | reserved | P3 |
| 15 | Step Sequence | SEQ → STEP | context exists | P0 |
| 16 | Save | PROJECT / Save | persistence later | P2 |

The Mode+pad gesture must be prioritized before implementing a large menu. It is exactly the kind of hardware-first navigation that lets the screen stay visually simple.

Status: **MODE+PAD chord routing implemented for all currently supported destinations; unsupported printed destinations remain explicitly reserved.**.

Priority: **P0** for implemented destinations.

### F. Main / Track View

MIDI note 52.

- press → MAIN;
- Shift + press → TRACK VIEW context.

Semantic action: `Navigate(Main)` or `Navigate(TrackView)`.

The action must not stop audio, clear the playhead or reset sequence state.

Status: **MAIN/TRACK VIEW semantic actions are routed without altering transport state.**.

Priority: **P0**.

### G. Track Select / Seq Select

MIDI note 13.

- press → enter track-selection context;
- data dial / +/- → select track;
- Shift + press → sequence-selection context;
- data dial / +/- → select sequence.

Our existing sequencer already has a bounded multi-Sequence model and explicit sequence selection/queueing. The hardware mapping should reuse that contract rather than create a second sequence selector.

Status: **TRACK and SEQUENCE selection contexts are routed through the existing domain selection/queue contract.**.

Priority: **P0**.

### H. Program Select / Track Type

MIDI note 14.

- press → select program;
- data dial / +/- → change program;
- Shift + press → select track type;
- data dial / +/- → change track type.

Our current domain already contains Drum, Keygroup, Plugin, MIDI and Audio-related track/program concepts. Unsupported types must remain visibly unavailable rather than silently creating broken objects.

Status: **DECODED, NOT ROUTED**.

Priority: **P1**.

### I. Browse / Up

MIDI note 50.

- press → BROWSE;
- Shift + press → parent folder / Up in Browser.

The Android document picker is an implementation detail. The product-level action remains Browser navigation.

Status: **BROWSE and Browser Up semantic actions are routed; Android document picker remains the current transport implementation.**.

Priority: **P0**.

### J. Sample Select

MIDI note 42.

- press → select the sample field for the selected pad;
- data dial / +/- → change selected sample;
- repeated press → cycle layer context.

Our domain supports eight layers, while the Akai manual's Studio MkII description exposes the first four layer-selection targets directly. The application should use the same one-control context idea while extending the underlying model to eight layers; do not hard-code a four-layer product limit.

Status: **PARTIAL — Sample Select enters a layer-selection focus using the existing eight-layer domain; full physical sample browsing remains pending.**.

Priority: **P1**.

### K. Sample Start / Loop Start

MIDI note 33.

Semantic action:

- enter Sample Start / Loop Start parameter context;
- data dial / +/- → adjust region start;
- repeated press → cycle selected layer;
- Shift variant → fine/secondary edit where supported by the current sample context.

The current shared waveform editor already owns non-destructive region editing. The hardware action should address that same domain-backed state.

Status: **ROUTED — Data Dial, +/- and Touch Strip address the shared sample-region domain.**.

Priority: **P1**.

### L. Sample End

MIDI note 34.

Semantic action:

- enter Sample End context;
- data dial / +/- → adjust end;
- repeated press → cycle layer.

Status: **ROUTED — Data Dial, +/- and Touch Strip address the shared sample-region domain.**.

Priority: **P1**.

### M. Tune / Fine

MIDI note 79.

- press → sample/pad tuning context;
- Shift + use → fine tuning;
- repeated press → layer selection context.

Our domain already supports pad tuning plus per-layer tuning. The physical action must operate on the selected layer where that context is active.

Status: **ROUTED — Data Dial, +/- and Touch Strip address per-layer tuning through the shared domain/audio command surface.**.

Priority: **P1**.

### N. Timing Correct / Quantize

Controls:

- Quantize = note 12;
- TC On/Off = note 15.

Semantic contract:

- Quantize → apply deterministic quantization using the active project grid;
- Shift + Quantize → selected-note/event quantization where a selection exists;
- TC On/Off → toggle record/edit Timing Correct;
- Shift + TC On/Off → open/select Timing Correct configuration (time division, swing and related settings).

The existing sequencer already has quantize and swing foundations. Hardware actions must dispatch to those same operations.

Status: **QUANTIZE routed to the selected Drum pattern; Timing Correct ON/OFF routed to the sequence recorder; Shift+Quantize remains reserved until an explicit event-selection contract exists.**.

Priority: **P1**.

### O. Zoom

MIDI note 66.

- press + data dial / +/- → horizontal zoom;
- Shift + press + data dial / +/- → vertical zoom.

Primary target contexts are SAMPLE waveform, GRID and other timeline views. The action must be context-sensitive and should never resize the global shell itself.

Status: **SEMANTIC CONTEXT ROUTED; actual horizontal/vertical zoom gestures remain pending.**.

Priority: **P1**.

### P. Pad Mute / Track Mute

MIDI note 4.

- press → Pad Mute context;
- Shift + press → Track Mute context.

The controller pads become the mute/assignment matrix while the mode is active.

This is a live-performance feature and must be implemented without requiring navigation through a menu.

Status: **TRACK MUTE and PAD MUTE contexts and pad targets are routed; mute is live-safe because playback sessions remain intact.**.

Priority: **P0**.

### Q. Note Repeat / Latch

MIDI note 11.

- hold → momentary Note Repeat;
- Shift + press → latch/unlatch Note Repeat;
- touch strip may set the repeat division.

The repeat scheduler must remain clock-synchronous and must not depend on UI frame rate.

The project already has Note Repeat LED protocol support; state feedback should be emitted only when the semantic state changes.

Status: **PERFORMANCE ROUTED to the native transport-clock scheduler; momentary and Shift-latched behavior are distinct, and the Note Repeat button LED mirrors semantic state. Generated repeat hits are currently performance-only and are not yet written to the sequence record queue**.

Priority: **P0**.

### R. Full Level / Half Level / 16 Level

MIDI note 39.

- press → Full Level on/off;
- Shift + press → Half Level on/off.

Pad velocity semantics:

- Full Level forces 127;
- Half Level forces 64;
- with both off, physical velocity is preserved.

These modifiers must operate before pad trigger semantics and therefore belong in the hardware/performance input layer, not inside individual screens.

Status: **FULL/HALF LEVEL + 16 LEVEL VELOCITY ROUTED**.

Priority: **P0**.

### S. Pad Banks A/E, B/F, C/G, D/H

MIDI notes 35–38.

Primary banks: A–D.

Shift-held banks: E–H.

The four buttons therefore form one 8-bank selection mechanism. They should update:

- logical pad-bank offset;
- software 4x4 pad labels/state;
- hardware pad RGB state;
- any sequence-launcher bank context where the launcher owns the pads.

The pad-bank operation must not create or destroy pads; it changes which logical range the sixteen physical pads address.

Status: **PAD BANK state is routed; launcher bank selection is operational, while normal performance remains constrained by the current 16-pad domain.**.

Priority: **P0**.

### T. Copy / Delete

MIDI note 122.

- press → Copy Pad/action context;
- Shift + press → Delete Pad/action context.

The existing sampler domain can copy/assign pad state, and destructive sample edits already exist. Future hardware routing must show the operation target before a destructive action is committed.

Status: **DECODED, SEMANTIC ROUTING NOT IMPLEMENTED**.

Priority: **P1**.

### U. Undo / Redo

MIDI note 67.

- press → Undo;
- Shift + press → Redo.

This is a global command and must be safe from every page.

The application should provide a compact state indication in the persistent status line and, later, on the MkII button LED.

Status: **DECODED, GLOBAL COMMAND NOT ROUTED**.

Priority: **P1**.

### V. Transport

Controls:

- Record = note 73;
- Overdub = note 80;
- Stop = note 81;
- Play = note 82;
- Play Start = note 83.

Canonical semantics:

- **Record** arms sequence recording; Play/Play Start begins recording;
- **Overdub** enables non-destructive recording;
- **Stop** stops playback;
- double Stop should be a second-stage behavior for silencing lingering voices if required by the audio contract;
- **Shift + Stop** returns the playhead to 1:1:0;
- **Play** starts from current playhead;
- **Play Start** starts from sequence start.

These commands must use the existing sequence transport clock. UI buttons and physical transport controls must dispatch the exact same semantic operations.

Status: **TRANSPORT routed to the existing sequence command surface; Play resumes the stopped playhead position.**.

Priority: **P0**.

### W. Step and Bar navigation

The current source names MIDI notes 68/69 as `NudgeLeft/NudgeRight` and 71/72 as `SeekBack/SeekForward`. Those names are misleading and must not become the product semantics.

Documented Akai meaning:

- 68 = Step <;
- 69 = Step >;
- 71 = Bar <<;
- 72 = Bar >>.

Semantic actions:

- Step < / > → move playhead one edit grid step;
- Locate + Step < / > → previous/next event;
- Bar << / >> → one bar left/right;
- Locate + Bar << / >> → sequence grid start/end.

The sequencer already uses **Nudge** as a separate note-editing concept (signed microtiming offset). Therefore the physical navigation buttons must be represented by semantic names such as `StepLeft`, `StepRight`, `BarLeft`, `BarRight`, not by `NudgeLeft` etc.

Status: **STEP/BAR semantic routing is implemented for stopped edit navigation; Locate variants remain reserved.**.

Priority: **P0 for transport/edit navigation**.

### X. Locate / markers

MIDI note 70.

Locate is a context modifier/toggle.

- momentary or latched;
- Pads 9–14 set/select up to six locators;
- Pads 1–6 jump to the corresponding locator;
- Locate + Step arrows = previous/next event;
- Locate + Bar arrows = sequence start/end.

First implementation should reuse the existing pad-selection event pipeline but temporarily change the semantic meaning of the pads.

Status: **DECODED, LOCATOR SYSTEM NOT IMPLEMENTED**.

Priority: **P1**.

### Y. Automation Read/Write

MIDI note 75.

- press → toggle Global Automation Read/Write;
- Shift + press → enable/disable Global Automation.

Automation is a later domain feature, but the hardware action should be reserved now so future routing does not conflict with another button.

Status: **DECODED, DOMAIN FEATURE NOT YET IMPLEMENTED**.

Priority: **P2**.

### Z. Tap Tempo / Master

MIDI note 53.

- press → tap tempo;
- Shift + press → sequence follows its own tempo vs master tempo.

Local tap-tempo calculation should feed the same sequence clock already used by the scheduler. Ableton Link integration is later and must not become a dependency of this local action.

Status: **TAP TEMPO is routed to the sequence tempo model. Master-tempo policy remains separate.**.

Priority: **P0** for tap tempo; **P2** for master-tempo policy.

### AA. Erase

MIDI note 9.

Canonical live behavior:

- while playing: hold Erase + pad → erase that pad's note event at the current playback position;
- while stopped: open an Erase context/window for notes, automation and other sequence data.

This is a strong example of why hardware semantics are contextual rather than one-button/one-screen mappings.

Status: **DECODED, RECORD/GRID DATA MODEL EXISTS, HARDWARE ROUTING NOT IMPLEMENTED**.

Priority: **P1**.

### AB. Touch Strip

The touch strip itself is distinct from the Touch Strip / Config button.

Reverse-engineered MIDI:

- touch/press note: 78;
- slide value: CC 33;
- button channel: 1 / zero-based 0.

Documented MPC software behavior supports multiple modes, including Note Repeat and expressive parameter control. The application should implement a smaller, coherent set first:

1. **Performance / Note Repeat division**;
2. **Pitch or modulation where the active Program supports it**;
3. **Track/Pad parameter control** (Level / Pan and later sends);
4. **Sample/Timeline parameter scrub** where a screen explicitly exposes a continuous range.

Touch Strip / Config button = note 0:

- press → cycle/select Touch Strip mode;
- Shift + press → open Touch Strip configuration;
- hold → enter temporary selection context where supported.

Touch-strip LED segments are CC 57–65 and should mirror the active value/mode.

Status: **CC DECODED; contextual Sample Start/End/Tune control routed. Note Repeat division/Touch NOTE 78 and Touch-strip LED feedback remain separate follow-up work.**.

Priority: **P0 basic performance value; P1 contextual modes**.

### AC. Button LED feedback

Each physical button's LED uses a CC matching the button's note number.

Observed LED value model:

- one-color LED: 0 off, 1 dim, 2 full;
- two-color LED: 0 off, 1/2 dim colors, 3/4 full colors.

Semantic rule:

- button LEDs mirror application state, not button presses alone;
- transport LEDs mirror transport state;
- mode LEDs mirror active mode/context;
- toggle buttons show actual state;
- feedback is cached and only written when state changes.

Status: **BASIC STATE ROUTER IMPLEMENTED for transport, Note Repeat, Level16 and mute-mode LEDs; writes are state-cached. Full LCD/state synchronization remains pending**.

Priority: **P0 for transport and mode state; P1 for secondary controls**.

### AD. Pad RGB feedback

Pad RGB SysEx is already implemented.

The controller accepts per-pad RGB values. The application should use the LEDs for:

- active/selected pad;
- pad-bank state;
- launcher active/queued sequence;
- mode modifiers;
- performance feedback;
- Note Repeat / special pad modes.

Do not drive RGB continuously from a UI timer. Generate feedback from semantic state changes.

Status: **WORKING for pad trigger/launcher feedback; broader semantic mode coloring and bank-state feedback remain pending.**.

Priority: **P0**.

### AE. Touch-strip LEDs / Note Repeat LEDs

Output protocol helpers exist.

- Touch strip: CC 57–65;
- Note Repeat divisions: CC 103–110.

These must become state-driven feedback owned by the hardware feedback layer.

Status: **NOTE REPEAT button LED routed; Touch-strip LED division feedback remains pending**.

Priority: **P1**.

### AF. 160x80 LCD

The MkII has a 160×80 color LCD controlled from software.

The project already has an LCD chunk/SysEx encoder and a test-frame generator.

The product-level LCD should eventually show a compact hardware context mirror:

- active mode;
- sequence / track;
- BPM;
- transport state;
- selected pad/sample;
- focused parameter;
- short transient status messages.

Do not attempt to mirror the whole Android UI. The LCD is a glanceable hardware companion.

Status: **PROTOCOL/TEST FRAME PARTIAL; PRODUCT DISPLAY NOT IMPLEMENTED**.

Priority: **P1 after core semantic routing; P0 for the first full hardware milestone's final status-feedback step**.

## Q-Link clarification

Do **not** add a fictional physical Q-Link knob bank to the MkII.

The 2021 MPC Studio MkII control surface is the pad/button/jog/touch-strip/LCD design documented here. Older MPC Studio hardware is a different product family and should not be used to invent missing controls.

Q-Link remains a valid **application feature** for contextual parameter editing, but on the MkII its physical implementation must use controls that actually exist on the device (for example, the data dial and touch strip) or future external hardware. The UI must never depend on a nonexistent dedicated bank of Q-Link knobs.

## Semantic architecture rule

The intended path is:

```
MPC Studio MkII MIDI
        ↓
MpcStudioMk2InputDecoder
        ↓
MpcStudioMk2 hardware semantic adapter
        ↓
semantic action
        ↓
MPC domain / sequence / audio command
        ↓
state change
        ↓
MpcStudioMk2 feedback layer
        ↓
button LEDs / pad RGB / touch LEDs / LCD
```

The Android MIDI bridge remains responsible for transport and device lifecycle. It must not acquire musical semantics.

The UI should dispatch the same semantic commands as the hardware adapter. Hardware and touch therefore become two input surfaces for one command model.

## Priority implementation order

### P0 — Make the MkII a real instrument

Implement first:

1. Shift state and Mode+Pad chord routing.
2. Transport: Record, Overdub, Stop, Play, Play Start.
3. Data Dial, Dial Press, +/-.
4. Main, Browse.
5. Track Select / Sequence Select.
6. Pad Banks A–H.
7. Note Repeat.
8. Full Level / Half Level.
9. Pad Mute / Track Mute.
10. Sequence Launcher pad routing and LED state.
11. Basic button LED synchronization.
12. Step/Bar playhead navigation.
13. Tap Tempo.
14. Touch Strip basic continuous-value path.

This gives the performer the core controls needed to operate the groovebox without touching the screen for every operation.

### P1 — Production workflow

Then implement:

- Sample Select;
- Sample Start/End;
- Tune/Fine;
- Quantize;
- Timing Correct;
- Zoom;
- Copy/Delete;
- Undo/Redo;
- Locate;
- Erase;
- touch-strip contextual modes;
- touch-strip and Note Repeat LED state;
- compact LCD status pages;
- Pad Mixer / Track Mixer navigation.

### P2 — Advanced editing/performance

Then:

- Automation Read/Write;
- richer Q-Link semantics using real MkII controls;
- advanced sample/process contexts;
- extended hardware feedback pages;
- context-specific shortcuts that are not required for basic live operation.

### P3 — Later product surface

Only after the core hardware workflow is stable:

- Song/arrangement view;
- Looper;
- MIDI Control routing;
- persistence/save shortcut;
- advanced interoperability;
- generic external controller mappings.

## Live-workflow invariants

Every hardware implementation must preserve these invariants:

1. Pressing a navigation button changes view/context, not musical time.
2. Transport controls remain globally available from every page.
3. The physical pads never silently change meaning; the active mode must explain the current pad role.
4. Destructive actions require an explicit semantic context and must remain undoable where practical.
5. LED state represents application state, not merely the last MIDI event.
6. LCD content is a concise context monitor, not a miniature copy of the Android screen.
7. Hardware and touchscreen controls invoke the same domain command whenever their semantics are equivalent.
8. No raw MIDI identifier is allowed to leak into the UI or domain layer.
9. No hardware mapping is declared physically confirmed until tested on the real MkII.
10. The audio callback never waits for UI or hardware feedback.

## Physical verification matrix

Before marking a control **CONFIRMED**, test on the actual MkII and record:

- device / Android version;
- APK build and commit;
- public MIDI port name;
- physical action;
- incoming MIDI bytes;
- expected semantic action;
- actual semantic action;
- feedback bytes if applicable;
- resulting UI/audio state.

The first physical batch should cover P0 only. This keeps failures attributable and protects the current audio/sampler baseline.


## 2026-09-30 implementation checkpoint

The MkII path now has a native semantic-adapter layer between MIDI decoding and Android. P0 controls are represented as stable semantic actions rather than raw MIDI identifiers.

Routed at this checkpoint: Mode+implemented-page shortcuts, Main/Browse, Track/Sequence selection, Data Dial direction/press, Plus/Minus adjustment, Pad Bank state, Full Level/Half Level, 16 Level Velocity, Pad/Track Mute contexts and targets, transport, stopped Step/Bar navigation, Tap Tempo, launcher-bank selection, Sample Start/End/Tune contextual Touch Strip control, Quantize/Timing Correct and native clock-synchronous Note Repeat performance.

Not declared complete: Note Repeat division selection from the Touch Strip, generated Note Repeat hit recording into the sequence record queue, Touch-strip LED division feedback, full LCD product pages, Locate marker/event navigation, program/track-type browsing, Undo/Redo and the remaining P1 editing commands. Physical MkII verification remains required before a control is marked CONFIRMED.
