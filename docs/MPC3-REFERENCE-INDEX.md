# MPC 3.9 Reference Index and Research Notebook

## Purpose

Search this file before making any substantial UI decision.

Format:
Question -> Source -> Finding -> Confidence -> Repository decision -> Test.

Primary software reference is the official MPC Standalone OS v3.9 guide. Current MPC3 support material is preferred over older MPC2 material when both describe the same feature.

---

## 1. Official MPC 3.9 manual

MPC Standalone OS User Guide v3.9

https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf

### Important sections

The v3.9 guide includes:

- MPC2 vs MPC3;
- Updates in v3.9;
- Main Mode;
- Toolbar;
- Shortcuts;
- Function Buttons;
- Sequence;
- Track;
- Track Edit;
- Arrangement;
- Mixer Strips;
- Track View;
- Browser;
- Grid;
- Step Sequencer;
- Sample Edit and Sampler workflows.

### Main Mode findings

- Main Mode is an overview of high-frequency functions.
- Toolbar shows project/timing information.
- Time Counter displays the playhead position and appears across many contexts.
- Five left-side shortcut icons can be customized.
- Main has context-sensitive function buttons along the bottom.
- Sequence information and Track information share the same workspace.
- Main can expose Arrangement for the selected track.
- Double-tapping relevant areas drills down to deeper editors.

Decision:
Main Mode becomes the center of our product, replacing the current pad-grid-and-inspector MAIN page.

---

## 2. MPC3 architecture

Akai MPC3 FAQ

https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq

### Findings

MPC3 added, among other things:

- new Main Mode;
- Linear Arranger;
- full-color Track and Pad Mixer;
- XL Channel Strip;
- One-to-One Track Workflow;
- Disk Streaming;
- Advanced Automation;
- Q-Link and X/Y macro controls;
- Direct to Pad Sampling;
- Full-Color Drum Grid;
- 8 Sample Layers;
- legacy project importing.

The FAQ explicitly describes a major architectural change: tracks and programs are unified into one track container.

Decision:
Our Track/Program semantics must support the selected track as authoritative before full Program Select/Track Edit work is considered faithful.

---

## 3. MPC2 -> MPC3 project architecture

Loading MPC2 Projects in MPC3

https://support.akaipro.com/en/support/solutions/articles/69000873048-mpc-series-loading-mpc2-projects-in-mpc3

### Findings

- MPC3 unifies tracks and programs.
- Import behavior depends on whether multiple MPC2 tracks referenced the same program.
- Older sequence/project structures can be transformed during import.

Decision:
Do not design our new UI around assumptions copied from an older MPC2 mental model.

---

## 4. Browser

Akai — Understanding the MPC Browser

https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser

### Findings

Browser concepts:

- Places;
- Content;
- Expansions;
- Sample Assign;
- internal/external drives;
- search;
- filter buttons;
- favourite locations 1-5;
- sample audition;
- browser options;
- metadata visibility;
- search through subfolders;
- system-folder visibility;
- sample-pool assignment.

Decision:
Android Document Picker is only a storage access implementation. It must not define product navigation.

---

## 5. Programs and track workflow

Akai — Understanding and Loading Programs

https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-%E3%82%B7%E3%83%AA%E3%83%BC%E3%82%BA%EF%BD%9C%E3%83%97%E3%83%AD%E3%82%B0%E3%83%A9%E3%83%A0%E3%81%AB%E3%81%A4%E3%81%いて

### Findings

- Program is the instrument building block.
- Drum Programs organize samples around 16 pads.
- Program selection is part of Main Mode.
- Browser participates in program/sample loading.
- Keygroup, Plugin, MIDI, CV and Clip program types have different pad behavior.
- Program edits are contextual to the selected track/program workflow.

Decision:
The Main Track region must show truthful track type and instrument/program context.

---

## 6. Arrangement and Track View

Source:
MPC Standalone OS User Guide v3.9

Supporting searchable manual mirror used only for text discovery:
https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b

### Findings

Main/Arrangement:

- horizontal timeline;
- selected track context;
- playhead;
- loop brace;
- track headers;
- record-arm, mute, solo and automation indicators;
- horizontal/vertical zoom;
- fit-to-view behavior;
- Cut, Copy, Paste and Duplicate using loop selection.

Track View:

- one horizontal strip per track;
- vertical navigation;
- track field;
- Time Counter;
- New Track;
- Rec Arm;
- Track -/+;
- Mute;
- Solo;
- Shift functions such as Duplicate Track, Timing Correct, Click and Track Settings.

Decision:
Main and Track View become two related project overview levels, not unrelated pages.

---

## 7. Grid

Primary:
MPC Standalone OS User Guide v3.9

Supporting manual discovery:
https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b

### Findings

Grid is a full event-editing workspace.

Documented interaction concepts include:

- time horizontally;
- drum rows/pads;
- piano-roll editing for melodic tracks;
- draw/pencil;
- erase;
- selection;
- magnification/navigation;
- horizontal/vertical navigation;
- pinch/spread zoom.

Decision:
Our existing SequenceGridView and zoom policy are foundations, not the final product Grid.

---

## 8. Step Sequencer

Primary:
MPC Standalone OS User Guide v3.9

Additional current Akai evidence:
MPC Live III FAQ documents Step Edit parameters such as Velocity, Ratchet, Probability and Nudge Ticks:
https://support.akaipro.com/en/support/solutions/articles/69000868537-akai-pro-mpc-live-iii-frequently-asked-questions

### Findings

- physical pads function as step buttons;
- 16-step interaction is central to the hardware workflow;
- Step Edit exposes event-level editing;
- current Akai material documents Velocity, Ratchet, Probability and Nudge Ticks and other per-step parameters.

Decision:
Keep our existing Step Edit semantic model, but present it inside a native MPC-like Step workspace.

---

## 9. Mixer

Primary:
MPC Standalone OS User Guide v3.9

### Findings

Main Mode contains compact mixer/channel-strip information.

Full contexts include:

- Channel Mixer;
- Pad Mixer.

Modern MPC3 material also documents a larger channel-strip architecture and full-color mixer workflow.

Decision:
Our current four-strip diagnostic MIX page is temporary. The target is contextual Channel/Pad Mixer architecture.

---

## 10. Menu

Primary:
MPC Standalone OS User Guide v3.9

### Findings

- Menu is the operating-system style mode launcher.
- Mode icons can be rearranged.
- Five most-used modes can be promoted into the left-side shortcuts.
- System-level actions such as project/save/preferences live around Menu-level navigation.

Decision:
The current seven-page permanent rail is replaced as the canonical mode launcher by Menu + five shortcuts.

---

## 11. Hardware controller protocol

MPC Studio MkII MIDI/SysEx Charts

https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

### Findings

Public reverse engineering reports:

- button notes on MIDI channel 1;
- button LED feedback on matching CC numbers;
- one-color LED state values Off/Dim/Full;
- two-color LED state values Off/Color 1 Dim/Color 2 Dim/Color 1 Full/Color 2 Full;
- jog rotation CC 100;
- jog press Note 111;
- Touch Strip CC 33;
- Touch Strip indicator CC bank;
- Note Repeat rate indicator CC bank;
- pad RGB SysEx;
- 160x80 LCD sent as six SysEx PNG chunks.

Important engineering interpretation from this public research:
controller feedback is host-driven; therefore our app must actively maintain state and send feedback.

Confidence:
PROBABLE until verified against the real Studio MkII. Existing physical-test records upgrade individual behaviors when tested.

---

## 12. Practical MkII mapping

https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

Use for cross-checking practical controller workflows and button combinations.

Do not treat it as authoritative over current Akai documentation or physical hardware.

---

## 13. Existing repository architecture

Read together:

- docs/PROJECT_MASTER_PROMPT.md
- docs/ARCHITECTURE.md
- docs/MPC-DOMAIN-MODEL.md
- docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md
- docs/HARDWARE-MPC-STUDIO-MK2.md
- docs/IMPLEMENTATION-ROADMAP.md
- docs/MPC-UX-REFERENCE.md
- docs/PHYSICAL-TEST-CHECKLIST.md

### Current architecture decision

Keep:

Android platform
-> native C++
-> MkII adapter
-> domain
-> Tracktion adapter
-> audio graph/device
-> JUCE/Oboe
-> Android audio.

UI migration sits above these layers.

---

## 14. Terminology lock

Use these canonical names:

- Main Mode
- Track View
- Arrangement / Arrange
- Grid View
- Step Sequencer
- Step Edit
- Track Edit
- Sample Edit
- Sampler
- Browser
- Channel Mixer
- Pad Mixer
- 16 Levels
- Pad Perform
- Next Sequence
- Menu
- Toolbar
- Shortcuts
- Function Buttons
- Sequence
- Track
- Program
- Pad
- Sample Layer
- Locator
- Timing Correct
- Data Dial
- Pad Bank

Do not introduce alternate top-level names for these contexts.

---

## 15. Open research questions

Record rather than guess:

- exact final LCD composition for our original application;
- exact LED color policy for all semantic states;
- persistence format for shortcut layout;
- best Q-Link substitute because Studio MkII has no dedicated Q-Link knob bank;
- detailed Arrangement gestures on phone;
- Android scoped-storage implementation behind Browser;
- practical subset of MPC3 modes reachable from Studio MkII;
- undocumented gestures that prove useful in live performance.

Each answer must record source and confidence.

---

## 16. Research maintenance rule

When a source changes, update:

- source URL;
- date checked;
- affected section;
- new finding;
- repository decision;
- regression/physical test.

The reference index exists so future sessions do not rebuild the research from memory.


---

## 16.1 Track Edit current implementation evidence

Current Akai support material confirms that Track Edit exposes pad parameters for Drum tracks, including tuning, filter and amp envelopes, and that the Samples tab presents the assigned layers with tuning/level controls. The current product implementation mirrors only the subset already backed by our domain/audio semantics.

Source checked:
https://support.akaipro.com/en/support/solutions/articles/69000874731-akai-mpc-series-tuning-basics

Checked:
2026-10-02

Repository decision:
Implement Track Edit Global/Samples/Envelopes against existing control-thread state; keep LFO/Modulations/Effects reserved until their semantic/backend contracts exist.


---

## 16.2 Main parameter-selection fidelity

Reference evidence:
The MPC 3.x manual documents that selected parameters are highlighted and adjusted using the data dial or +/- buttons; Main Mode presents Sequence, Track/Arrangement and Function Button regions as one operational workspace. citeturn429791search5turn268950search3

Repository decision:
Main fields should behave as selection targets rather than decorative cards or duplicated micro-controls. The current UI therefore renders the focused Data Dial target with a red outline and removes local BPM/BARS steppers from Main.


---

## 16.3 Shell composition fidelity

The current MPC3 reference describes Main as Toolbar + Shortcuts + Mixer Strips + Sequence + Track/Arrangement + Function Buttons. The Mixer Strips sit on the left edge beside the five mode shortcuts and can be shown/hidden. citeturn329559search12turn329559search17

Repository decision:
Our shell now reserves that left edge for five shortcuts plus condensed mixer strips; Sequence/Track/Program remain in the central Main workspace. Values without a current backend semantic remain explicitly reserved.


## 16.4 Track Edit geometry reference lock

Source checked: official MPC Standalone OS user guides, including current MPC 3.x documentation and the v3.7 Track Edit description. The documented Drum Track Edit screen uses a top TRACK/PAD context, a visible Edit All Layers control, and a bottom parameter-tab surface. citeturn152242search1turn152242search2

Repository decision:
the Android Track Edit workspace must keep TRACK/PAD context at the top, parameter content in the middle, and GLOBAL/SAMPLES/AMP ENV/LFO/MODS/EFFECTS navigation at the bottom. Display labels are canonical; internal enum names may remain implementation-oriented.

Confidence: HIGH for the overall geometry and bottom-tab behavior; individual unsupported parameters remain governed by our truthfulness rule.

## 16.5 MkII LED state policy correction

The Studio MkII protocol differentiates single-color LEDs (OFF/DIM/FULL = 0/1/2) from two-color LEDs.
The Android feedback layer now uses a shared `buttonLedOnState()` policy and retains mutually-exclusive Level/Mute states.
