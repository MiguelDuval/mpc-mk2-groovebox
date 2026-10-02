# MPC 3.9 UI Migration Master Specification

## 0. Status

**Status:** CANONICAL — global UI migration contract  
**Reference starting point:** a809196fab09de2be87c9019377f7756f3673d3d  
**Protected known-good baseline:** a1c73aa52d51689b117fa1b2b27a0a3f11cb869e  
**Target reference:** Akai MPC Standalone OS 3.9 on MPC One / One+ class hardware  
**Primary physical controller:** Akai MPC Studio MkII

This document permanently changes the UI direction.

The Android display is now treated as an original implementation of the documented MPC 3.9 standalone interaction architecture. This is not a color/shape reskin of the existing application.

The target is maximum behavioral and information-architecture fidelity to documented MPC 3.9 standalone behavior while retaining our own implementation, assets, domain model, audio engine and project format.

We may reproduce documented functional interaction patterns and information architecture. We do not copy Akai source code, firmware binaries, proprietary resources or artwork.

---

## 1. Product decision

### 1.1 What is being reproduced

The migration targets:

- navigation hierarchy;
- screen/context taxonomy;
- information hierarchy;
- relative placement of toolbar, shortcuts, workspace, mixer/channel strips and bottom function bar;
- Main Mode as the operational hub;
- Track / Program / Sequence workflows;
- Grid, Step, Track View, Track Edit, Sample Edit, Browser and Mixer concepts;
- contextual Data Dial and +/- behavior;
- contextual bottom functions;
- hardware-first navigation and state feedback;
- touch and hardware entry paths for the same semantic actions;
- live-performance behavior such as preserving transport while navigating.

The migration does not target:

- Akai source code;
- firmware extraction;
- proprietary graphic assets;
- undocumented behavior presented as fact;
- exact MPC project compatibility unless separately implemented and tested.

### 1.2 Canonical software source

Primary source:

MPC Standalone OS User Guide v3.9  
https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf

Supporting primary source:

MPC3 FAQ  
https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq

Supporting Akai workflow articles:

Browser:
https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser

Programs:
https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-%E3%82%B7%E3%83%AA%E3%83%BC%E3%82%BA%EF%BD%9C%E3%83%97%E3%83%AD%E3%82%B0%E3%83%A9%E3%83%A0%E3%81%AB%E3%81%A4%E3%81%84%E3%81%A6

MPC2 -> MPC3 architecture:
https://support.akaipro.com/en/support/solutions/articles/69000873048-mpc-series-loading-mpc2-projects-in-mpc3

Controller/protocol:

https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

---

## 2. Protected architecture boundary

The global UI rewrite is a presentation/navigation refactor. The following layers are protected:

- realtime audio callback;
- sample decoding and voice rendering;
- Oboe/Android audio stream lifecycle;
- Tracktion integration boundary;
- sequencer clock and realtime scheduling;
- Android MIDI transport;
- raw MkII MIDI decoding;
- hardware SysEx encoding;
- existing domain operations that already satisfy the new UI;
- persistence that is unrelated to presentation.

### Allowed lower-layer changes

A lower-layer change is allowed only when the current model cannot express a truthful MPC-like UI.

Examples:

- resolve Track -> Program from track ownership;
- add read-only queries needed to render a context;
- add non-realtime semantic commands;
- add domain state that is genuinely required by Main/Track View/Track Edit;
- separate navigation state from musical transport state.

These are semantic corrections, not permission to redesign the engine.

### Realtime safety

UI code must never introduce:

- allocation on the audio callback;
- blocking I/O on the audio callback;
- UI locks in realtime audio;
- Android view calls in the audio engine;
- file decoding in realtime;
- visual refresh work in the audio callback.

---

## 3. Target architecture

MpcUiState
  -> MpcNavigationController
  -> MpcShell
       -> Toolbar
       -> ShortcutRail
       -> MixerStripArea
       -> Workspace
       -> FunctionBar
  -> ContextView
  -> Semantic Actions
  -> Existing Native / Domain / Sequencer / Audio
  -> Controller Feedback + Phone UI state

The current monolithic Activity is a migration scaffold. It must not be replaced by another monolith.

### Required UI state

At minimum:

- current mode;
- current subcontext;
- selected sequence;
- selected track;
- selected program/track container;
- selected pad;
- selected sample layer;
- active pad bank;
- Data Dial focus;
- zoom axis/focus;
- transport state;
- loop state;
- Timing Correct state;
- metronome state;
- rec-arm state;
- mute/solo state;
- browser location/filter/search;
- editor tool;
- hardware modifier/latch states;
- availability of current action.

Transient status is not the source of truth.

---

## 4. Canonical MPC shell

### Toolbar

Persistent top-level information:

- project name;
- time counter / playhead;
- Timing Correct;
- metronome;
- global automation state;
- MIDI I/O state where relevant;
- system/resource state where appropriate;
- transport.

### Shortcuts

MPC 3.x documents five left-side shortcuts that are user-configurable.

The target shell must have:

- five shortcut slots;
- configurable destinations;
- persistent Main visibility;
- equivalent left-edge access from other contexts where practical.

The current seven-page permanent rail must therefore stop being the canonical navigation model.

### Mixer/channel strip area

The compact shell must reserve a region for contextual channel/pad information.

Full Channel Mixer and Pad Mixer remain separate modes.

### Workspace

One context at a time, using a common shell.

### Function bar

Bottom controls are first-class context functions.

Main Mode targets include:

- New Track;
- Rec Arm;
- Track -;
- Track +;
- Mute;
- Solo.

Do not hard-wire bottom buttons independently inside every page.

---

## 5. Main Mode — P0

Main Mode becomes the center of the application.

Required conceptual regions:

1. Toolbar
2. Shortcuts
3. Mixer Strips
4. Sequence
5. Track / Arrangement Views
6. Function Buttons

### Sequence section

Show:

- sequence;
- BPM;
- time signature;
- length/bars;
- loop state;
- loop start/end;
- current sequence context.

### Track section

Show:

- track number/name;
- track type;
- track/program relationship;
- program/instrument context;
- selected pad where relevant;
- track edit entry;
- track length;
- inserts/I/O where supported.

### Arrangement view

Main must be able to reveal the selected track on a horizontal arrangement timeline.

Required:

- events;
- playhead;
- loop selection;
- track focus;
- drill-down into Grid;
- no transport reset during navigation.

### Hardware

Main button -> Main.

Shift + Main -> Track View.

---

## 6. Track/program semantic prerequisite

MPC3 documents a major architectural change: tracks and programs are unified into one track container.

Our current domain still exposes Track.programId while historical playback can depend on a global active Drum Program.

Before full Program Select / Track Type / Track Edit UI:

1. selected Track is authoritative;
2. Track.programId resolves to its program/container;
3. playback uses the selected track's resolved program;
4. selecting a track selects its instrument context;
5. program editing is scoped to that track;
6. shared/legacy cases are represented explicitly.

Without this correction, the UI can display one instrument while audio renders another.

This is a domain semantic prerequisite, not a visual redesign.

---

## 7. Menu and navigation

Menu becomes the operating-system launcher.

Target:

- 4x4 mode grid;
- touch-friendly cells;
- configurable mode layout;
- five promoted shortcuts;
- New Project;
- Save;
- Project;
- Preferences/system functions;
- visible reserved destinations for unsupported modes.

Canonical mode surface:

- Main;
- Track View;
- Browser;
- Grid;
- Step Sequencer;
- Track Edit;
- Sample Edit;
- Sampler;
- Channel Mixer;
- Pad Mixer;
- 16 Levels;
- Pad Perform;
- Next Sequence;
- Arrange;
- List Edit;
- Project / Save;
- Preferences;
- MIDI / Control;
- Looper;
- XYFX / performance context.

Not all destinations have to be fully functional in phase one. The navigation vocabulary must be stable.

---

## 8. Grid View

Grid is a full event editor.

### Layout

- top context;
- horizontal time;
- vertical note/pad domain;
- playhead;
- loop range;
- event selection;
- tool state;
- contextual bottom functions;
- zoom and navigation.

Drum:

- pad rows;
- persistent row identity;
- event cells aligned to pad rows.

Melodic:

- piano-roll semantics.

### Tools

- draw/pencil;
- erase;
- selection;
- magnification/navigation.

### Touch

- horizontal navigation;
- vertical navigation;
- pinch/spread zoom;
- selection;
- event edit;
- playhead-aware navigation.

Existing sequence zoom code is a policy primitive to reuse, not the final Grid architecture.

### Hardware

- Shift + Main -> Grid;
- Data Dial -> Grid cursor/focus;
- +/- -> contextual adjustment;
- Zoom -> horizontal or vertical focus;
- pads -> contextual selection.

---

## 9. Step Sequencer and Step Edit

The Step Sequencer uses physical pads as step buttons.

Required:

- 16-step page;
- bar navigation;
- pad selection;
- visible note state;
- velocity indication;
- Timing Correct access;
- Note Repeat compatibility.

Step Edit fields already established in our product:

- velocity;
- probability;
- ratchet;
- nudge;
- duration.

Step paging is independent of Grid zoom.

---

## 10. Track View

Track View is a sequence-level overview.

Target:

- horizontal strip per track;
- vertical scroll;
- current track focus;
- time counter;
- record arm;
- mute;
- solo;
- New Track;
- Track -/+;
- Shift functions for duplicate/TC/click/settings.

Main Mode and Track View are related levels of one project overview.

---

## 11. Track Edit

Track Edit is attached to the selected track.

Target tabs, adapted to supported program types:

- Global;
- Samples;
- Envelopes;
- LFO;
- Modulations;
- Effects.

For Drum tracks:

- selected pad;
- sample layers;
- up to eight layers;
- per-layer editing;
- Edit All Layers where supported.

Reuse existing domain/audio functions rather than making a second parameter system.

---

## 12. Sample Edit and Sampler

One coherent sample workspace should replace scattered diagnostic pages.

Include:

- sample field;
- waveform;
- start;
- end;
- loop;
- slice/chop;
- zoom;
- pan;
- audition;
- trim/crop/chop/process;
- assign;
- contextual lower controls.

Reuse existing:

- waveform;
- sample regions;
- start/end;
- crop;
- chop;
- multi-layer;
- tuning;
- gain;
- pan;
- envelope;
- filter;
- record;
- monitor;
- threshold.

---

## 13. Browser

The current Android Document Picker is a storage primitive, not the product Browser.

Target concepts:

- Places;
- Content;
- Expansions/User content;
- Sample Assign;
- search;
- filters;
- favourites;
- audition;
- browser options;
- metadata;
- load destination.

Browser must consume a semantic storage provider, keeping Android storage mechanics underneath it.

Every load is:

source -> destination context -> domain command -> new state -> feedback.

---

## 14. Mixer

### Channel Mixer

Target:

- multiple visible strips;
- horizontal paging;
- volume;
- pan;
- sends;
- inserts/effects;
- I/O;
- returns/outputs;
- focused strip state.

### Pad Mixer

Target:

- Drum/Keygroup pad channels;
- multiple visible pads;
- level/pan/routing/effects as supported.

The current four-strip diagnostic layout is not the target.

---

## 15. Performance contexts

### 16 Levels

Preserve current semantic behavior:

- source pad;
- 16 fixed velocity levels;
- physical pad mapping;
- explicit active state.

Future variants can include Tune/Filter/Layer/Attack/Decay.

### Pad Perform

Target contexts can include Notes, Chords and Scales where supported.

The display must always show how pads are currently interpreted.

### Next Sequence

Treat as a dedicated live-performance context:

- current sequence;
- queued sequence;
- launch quantization;
- pad grid;
- state feedback.

---

## 16. Controller-first UX

The Studio MkII is the primary physical surface.

Every physical control needs:

- control identity;
- semantic action;
- modifier behavior;
- target context;
- phone indication;
- hardware feedback;
- test/verification state.

Canonical mapping remains:

docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md

Priority flows:

- Main / Track View;
- Browse / Save;
- Main / Grid;
- Mix / Sampler;
- Mute / Sample Edit;
- Track Select / Sequence Select;
- Program Select / Track Type;
- Zoom;
- Undo / Redo;
- Copy / Delete;
- Locate;
- Step / Bar;
- Note Repeat;
- Full Level / 16 Levels;
- Timing Correct;
- Pad Banks;
- Data Dial / +/-;
- Touch Strip;
- Mode + pad destinations;
- transport;
- Record / Overdub.

---

## 17. Indication parity

Every context must answer four questions immediately:

1. What mode/context is active?
2. What does Data Dial / +/- currently edit?
3. Which pad bank is active?
4. Is the current state primary, alternate/Shift, active or unavailable?

The phone uses persistent UI state.

The controller mirrors state with its available:

- LEDs;
- pad RGB;
- Touch Strip;
- LCD.

A transient toast/status message is never sufficient for a latched mode.

---

## 18. Component decomposition

Target reusable components:

Shell:
- MpcShellView
- MpcToolbarView
- MpcShortcutRailView
- MpcMixerStripView
- MpcFunctionBarView

Contexts:
- MpcMainView
- MpcTrackView
- MpcGridView
- MpcStepView
- MpcTrackEditView
- MpcSampleEditView
- MpcBrowserView
- MpcChannelMixerView
- MpcPadMixerView
- MpcMenuView
- MpcNextSequenceView
- MpcArrangeView

These names are targets for separation of responsibility, not a demand for one exact implementation technique.

---

## 19. MainActivity migration

MainActivity currently contains shell construction, page construction, refresh logic and hardware/UI synchronization.

Do not rewrite it as another monolith.

Migration:

1. extract UI state;
2. extract navigation;
3. extract shell;
4. extract contexts;
5. preserve semantic callbacks;
6. move repeated refresh logic into state-driven rendering;
7. delete old page duplication only after replacement is covered.

The final Activity should primarily own lifecycle/platform concerns and compose the UI.

---

## 20. Safe implementation phases

### Phase 0 — documentation lock

- master specification;
- reference index;
- safe-change contract;
- migration matrix;
- acceptance matrix.

### Phase 1 — shell

- Toolbar;
- five shortcuts;
- compact mixer strip;
- workspace host;
- function bar;
- persistent feedback.

### Phase 2 — navigation

- Menu;
- shortcuts;
- Main;
- Track View;
- Browser;
- Grid;
- Step;
- Sample Edit;
- Mix.

### Phase 3 — Main Mode

Recompose Main first.

### Phase 4 — track/program semantics

Correct Track -> Program resolution.

### Phase 5 — Grid + Step

Absorb existing Grid/zoom/Step work.

### Phase 6 — Sample workspace

Compose Sampler + Sample Edit.

### Phase 7 — Browser

Introduce semantic storage provider.

### Phase 8 — Mixer

Channel + Pad Mixer.

### Phase 9 — performance

16 Levels, Pad Perform, Next Sequence, mute contexts.

### Phase 10 — deep edit/system

Track Edit, Arrange, List Edit, Project, Preferences, automation, Q-Link.

---

## 21. Acceptance gates

A phase is complete only when:

### Architecture

- UI is state/action driven;
- audio/native boundary is preserved except documented semantic queries;
- raw MIDI stays out of views;
- audio callback has no UI dependency.

### UX

- shell placement follows MPC information hierarchy;
- hardware enters the same semantic context as touch;
- Data Dial focus is visible;
- bottom functions are contextual;
- controller and phone agree on latched states.

### Regression

- sampler playback still works;
- transport still works;
- record/overdub still works;
- Note Repeat still works;
- Step Edit still works;
- Locate still works;
- Copy/Delete still works;
- feedback still works.

### Evidence

- relevant GitHub Actions run is green;
- Android smoke covers shell/navigation;
- physical hardware verification is recorded before protocol behavior is considered confirmed.

---

## 22. Current -> target migration matrix

| Current | MPC 3.9 target | Decision |
|---|---|---|
| Seven-page permanent rail | Five configurable shortcuts + Menu | Replace |
| MAIN pad grid + inspector | Main Mode | Recompose |
| SEQ page | Main Sequence + Track/Arrangement | Recompose |
| Grid page | Grid View | Recompose |
| Step page | Step Sequencer / Step Edit | Recompose |
| Sample tabs | Sample Edit + Track Edit | Recompose |
| Android document picker | MPC Browser | Replace UX, preserve storage access |
| Four-strip MIX | Channel Mixer / Pad Mixer | Replace |
| MENU list | 4x4 Menu | Replace |
| persistent feedback strip | persistent MPC-context indication | Keep + integrate |
| waveform system | Sample Edit workspace | Reuse |
| sampler engine | presentation only | Protect |
| sequencer engine | presentation only | Protect |
| MIDI adapter | semantic hardware bridge | Protect |
| feedback encoder | state projection | Protect |

---

## 23. Research-derived rules

Canonical behavioral rules:

- Main Mode is the overview hub.
- Toolbar carries persistent project/timing information.
- Five shortcuts are configurable.
- Function buttons are contextual.
- Track View is a horizontal-strip track overview.
- Arrangement is a linear timeline.
- Grid is a full event editor.
- Main can drill into Track Edit and Grid.
- Browser separates Places, Content, Expansions and Sample Assign concepts.
- Browser supports audition and favourites.
- Channel Mixer and Pad Mixer are distinct contexts.
- Step Sequencer maps physical pads to step buttons.
- Timing Correct is available from multiple contexts.
- Six locators are part of the workflow.
- Menu layout can be rearranged.
- Shift provides documented alternate actions.
- Data Dial and +/- are context-sensitive.
- MPC3 uses a unified track/program container model.

---

## 24. Live-performance design principles

Evaluate every screen against:

- performer looking away from the screen;
- one-hand hardware operation;
- fast track/sequence switching;
- safe live mutation;
- immediate state recognition;
- predictable focus;
- transport continuity while navigating;
- limited modal dead ends;
- no accidental destructive edits;
- explicit armed/active/unavailable states.

A performer should recover the active control state at a glance.

---

## 25. Change-control rule

No new top-level UI page may be introduced merely because it is convenient.

Every new context must document:

- MPC reference context;
- owning shell region;
- hardware entry path;
- semantic state rendered;
- semantic command;
- controller feedback;
- phone indication;
- existing functionality reused;
- regression test.

This prevents the product from drifting back into a collection of unrelated diagnostic screens.

---

## 26. Completion definition

The migration is complete when:

- navigation is MPC 3.9-like;
- Main Mode is operational center;
- context transitions are coherent;
- track/program semantics are truthful;
- editors live inside the correct contexts;
- Browser is a real product workspace;
- Grid/Step/Sample Edit/Mix share one shell;
- Studio MkII drives the principal workflows;
- phone and controller state remain synchronized;
- audio/sequencer engine remains stable and independently testable.

This is the permanent UI direction for MPC MK2 Groovebox.
