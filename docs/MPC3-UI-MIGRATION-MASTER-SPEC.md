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

### Persistent playhead strip

The shell places a thin, non-interactive sequence-position strip directly below the Toolbar.

It is presentation-only and receives normalized position from the existing semantic Sequence state. It remains visible while the workspace changes so transport position is never visually lost during live navigation. It does not own timing, scheduling, or touch handling.

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

Main Mode now renders a compact, read-only Pad Mixer Strip for the selected Drum pad with level, pan and tuning readouts. The strip is presentation-only and reads the existing pad state. Mutations remain in the dedicated Pad Mixer / sampler controls until a full MPC channel-strip backend contract is available.

The Main Track quick-sample surface also displays the selected project's sample name when available. Document-provider display names are propagated on the control thread into the existing SampleRef metadata; no realtime audio path depends on the filename.
The condensed Mixer Strip can be shown or hidden without changing selected Track, Pad, Sequence or transport state.

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

On the Main screen these six semantic actions occupy five visual MPC-style slots: New Track; SEQ + Rec Arm; - Track +; Mute; Solo. Do not hard-wire bottom buttons independently inside every page.

---

## 2026-10-02 Main shell context/channel refinement

The canonical shell now treats the left edge as two distinct persistent layers:

five configurable shortcuts → compact contextual channel/track strip → workspace

The compact strip is vertical and remains visible while the active workspace changes. It carries:

- Sequence + BPM;
- selected Track + Track Type and record/mute state;
- selected Track's Program;
- selected Pad + Bank;
- Data Dial focus + active Main subcontext;
- a thin sequence movement overview.

Sequence, Track and Program fields are direct entry points to the existing Main selection contexts. The strip is presentation-only and reads the existing semantic/domain state; it does not own transport or audio behavior.

### Main Program Select truthfulness rule

The persistent Program field and Main Program Select context are enabled only for a Drum Track. When another Track Type is selected, the UI must show an explicit unavailable state rather than exposing the Drum Program list as though it were owned by that Track.

### Track View shell authority

Track View uses the persistent shell Function Bar as the single mutation surface for the selected Track. Track rows expose status and selection, while REC ARM, Track −, Track +, Mute and Solo remain contextual shell actions. Changing Track from Main must not navigate away from Main merely to reveal the new selection.

## 5. Main Mode — P0

Main Mode becomes the center of the application.

Required conceptual regions:

1. Toolbar
2. Shortcuts
3. Mixer Strips
4. Sequence
5. Track / Arrangement Views
6. Function Buttons

### Main Track quick-sample surface

For a Drum Track, Main Track may provide a compact performance/sample surface: pads plus a selected Pad/Layer waveform with Start/End editing, audition, and entry into the dedicated sample workflow. Directly below the Track waveform/canvas, the UI uses the MPC Main track-state vocabulary **Monitor / Length / Velocity / Layer**; unsupported values are rendered as unavailable rather than fabricated. Double-tapping the waveform is the semantic Track Edit entry gesture; while Track Edit backend support is pending, the app opens a truthful reserved Track Edit context with the current Track/Pad/Layer instead of silently doing nothing. This surface reuses existing audio/sample-region semantics and must not introduce a second audio editing model.

In the sibling Arrangement view, double-tapping the overview is the semantic Grid entry gesture. The app opens Grid for supported Drum tracks and reports the unsupported Track Type explicitly otherwise.

### Sequence section

Show:

- sequence;
- BPM;
- time signature;
- length/bars;
- loop state;
- loop start/end;
- current sequence context;
- MPC-style compact fields: SEQ, BARS, START, END, TRANSPOSE;
- explicit unavailable indication for unsupported sequence semantics.

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

- GLOBAL;
- SAMPLES;
- AMP ENV;
- LFO;
- MODS;
- EFFECTS.

Track Edit geometry follows the MPC touch workflow: compact TRACK/PAD context at the top,
the scrollable parameter workspace in the center, and a persistent tab bar at the bottom.
Edit All Layers remains a visible but explicitly unavailable action until the domain exposes
an atomic multi-layer command.

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
- Main Sequence Select / Track Select subcontexts;
- Main Track + Program unified region;
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


## 2026-10-02 implementation checkpoint — Main/Menu vertical slice
The first functional Main/Menu vertical slice is now on `work/mpc3-ui-migration-master-spec`.

### Main Mode
The legacy Main composition has been replaced by a three-level MPC-style overview:
`Sequence → Track → Program/Performance/Arrangement`.
The existing pad engine, sample editor, sequencer and transport remain behind the workspace; no realtime/native foundation was rewritten.

### Menu
The launcher is now a canonical 4×4 vocabulary backed by `MpcModeRegistry`. Available contexts navigate into existing workspaces. Contexts that cannot yet be implemented truthfully are explicitly marked `RESERVED`, rather than exposing dead controls.

### Shell
The shell now keeps persistent timing context in the top toolbar. Timing Correct is functional; Metronome and Automation remain explicit future contexts.

### Semantic correction
The currently implemented four-strip mix workspace is classified as `PAD_MIXER`. `CHANNEL_MIXER` is reserved until the backend can represent real per-track strips without fabricating controls.

### Safety
No audio callback, sampler rendering path, sequencer clock/scheduler, raw MkII decoder, or SysEx transport was changed in this slice.

## 2026-10-02 Track View / Main fidelity checkpoint

The canonical MPC navigation layer now has a dedicated Track View workspace rather than treating the legacy Sequencer page as the Track View surface.

Track View currently provides:
- sequence summary;
- per-track horizontal strips;
- selection;
- record-arm state;
- mute state;
- truthful Track Type;
- truthful Track Program label from the selected Track's `programId`;
- event-count summary;
- explicit Grid, Step, New Track and deep Sequence Edit exits;
- Arrangement remains explicitly reserved until a true linear arranger exists.

Main Mode now uses the existing `SequenceTimelineView` as its selected-track arrangement preview, with time-signature-aware playhead positioning and loop start/end state.

The five promoted shortcuts are now user-reorderable from Menu. The 4×4 Menu remains the canonical mode vocabulary and unsupported contexts continue to be explicitly RESERVED/UNAVAILABLE.

A native track-context bridge exposes Track Type, Program and Mute state to presentation code. Track→Program ownership is now semantic: each Drum Track resolves playback from its own `programId`, and Main exposes Program Select for the selected Drum Track. Program Edit and Track Edit remain reserved because editing the contents of a Program is a separate backend capability.

No realtime audio callback, sampler rendering, sequencer scheduler/clock, raw MkII decoder or SysEx transport was changed in this checkpoint.

## 2026-10-02 Browser / Arrangement UX checkpoint

The Browser is now a dedicated product workspace rather than a direct Android picker wrapper. Its information architecture is explicitly separated into:
`Places → Content → Expansions → Sample Assign`, with file-type filters, search, audition and a destination panel for Pad/Layer loading. The Android Document Provider remains only the storage transport.

Arrangement is now a dedicated linear context with:
- track lanes;
- timeline/ruler;
- loop brace with commit back to the existing sequence loop state;
- playhead;
- horizontal zoom;
- track selection;
- double-tap event handoff to Grid;
- explicit Cut/Copy/Paste/Duplicate RESERVED states until truthful arrangement editing semantics exist.

Current arrangement domain limitation is intentional: the existing domain stores patterns and events but does not yet store explicit linear clip placements. The Arrangement view therefore renders the first pattern of each track from bar 1 as the truthful current projection. It must not be represented as a fully editable multi-clip arranger until a placement model is added.

Five shortcuts are configurable by both assignment and order. Only implemented contexts can be promoted; RESERVED contexts remain in Menu.

## 2026-10-02 Program Select / Track ownership checkpoint

The selected Drum Track now owns its Program through the domain `programId` relationship. Program Select is a Main subcontext, with Data Dial focus set to Program and a touch-selectable list of available Drum Programs.

The playback session resolves each Drum Track independently, so multiple Drum Tracks may reference different Drum Programs without changing the realtime scheduling architecture.

A newly created Drum Track inherits the currently active Drum Program rather than assuming `drum-program-1`. This keeps Track creation consistent with the selected program context.

Main and Track View explicitly establish their hardware/Data Dial focus on entry, preventing stale selection contexts from leaking across views.

## 2026-10-02 Track Edit semantic slice

- **Track Edit now has a real bounded workspace** via `MpcTrackEditView`, replacing the previous reserved-only gateway.
- **Global** remains truthful for every Track Type as Track context; Drum Tracks additionally expose the existing pad-global tune/level/pan semantics.
- **Samples** is functional for Drum Tracks using the existing eight-layer sample model, waveform region editing, per-layer gain/tuning/pan, velocity range and audition semantics.
- **Envelopes** is functional for Drum Tracks using the existing pad amp envelope and filter cutoff semantics.
- **LFO / Modulations / Effects** remain explicit RESERVED/UNAVAILABLE tabs because their complete backend/semantic contracts do not yet exist.
- **Edit All Layers** remains visible but disabled because the current backend has no atomic multi-layer command.
- Main pencil and Main Track waveform double-tap enter the same Track Edit workspace with the selected Track/Pad/Layer context preserved.
- Track Edit is now a truthful promotable mode in the Menu/shortcut registry; unsupported sub-tabs remain individually reserved.
- Studio MkII Data Dial layer focus is correctly established at hardware focus 10, with Data Dial/+/- changing the same selected-layer state used by the editor.


- **Pad-selection synchronization:** the shared pad-selection refresh path now also refreshes Track Edit, so physical pad selection, touch selection and the editor's Track/Pad context remain synchronized.


## 2026-10-02 Main canvas fidelity refinement

- Main's Sequence region now follows the MPC interaction model more closely: the sequence field is the direct selection target; the sequence edit pencil is present but explicitly reserved; BPM/BARS/START/END are fields controlled by Data Dial/+/- rather than duplicated local stepper buttons.
- Sequence fields are presented in one compact MPC-style row with a smaller auxiliary Time Signature/Loop row.
- Main's selected Data Dial target now receives a visible red selection outline, matching the documented MPC parameter-selection convention.
- Outer Main Track/Sequence containers were flattened so the workspace reads as one workstation canvas rather than a dashboard of independent Android cards.


## 2026-10-02 MPC shell / mixer-strip fidelity refinement

- The shell left edge now follows the MPC3 Main composition more closely: five shortcuts sit beside a dedicated condensed Mixer Strip area; central Sequence/Track/Program information is no longer duplicated as a dashboard column.
- The condensed mixer area exposes only backend-truthful channel information: selected Track status, selected Drum Pad level/pan/tune, and explicit RESERVED Track/Main Output level states where our backend does not yet expose those values.
- Toolbar was simplified toward the MPC Main information hierarchy: Project, playhead position, Timing Correct, Metronome, Automation and Menu; diagnostic AUDIO/MIDI chips remain internal rather than being part of the MPC-facing toolbar.


## 2026-10-02 Track Edit geometry fidelity checkpoint

The Track Edit workspace now follows the documented MPC composition rather than a generic Android editor layout:

- compact **TRACK** and **PAD** context fields occupy the top of the workspace;
- **EDIT ALL LAYERS** remains a visible, disabled/reserved action because no atomic multi-layer semantic command exists;
- the parameter workspace is vertically scrollable;
- **GLOBAL / SAMPLES / AMP ENV / LFO / MODS / EFFECTS** is a persistent bottom tab bar;
- selected-tab state uses the same red interaction accent used for MPC-style focused state;
- touch and hardware still converge on the existing semantic layer selection path.

This is a presentation-only refinement; realtime audio, MIDI transport, sequencer timing and sample decoding remain untouched.

## 2026-10-02 MkII-first shortcut presentation checkpoint

The persistent five-slot shortcut rail now presents compact original mode glyphs with short labels instead of numeric ordinals.
The slots remain navigation adapters backed by MpcNavigationController; selection state is still rendered from application state.

## 2026-10-02 MkII feedback fidelity checkpoint

Controller indication is now treated as persistent state projection: single-color/two-color LED encoding is centralized,
Full/Half/16 Levels and Pad/Track Mute modes are stateful, and Note Repeat rate labels share the native index order.

## 2026-10-02 MkII LCD focus checkpoint
The Studio MkII LCD companion mirror now projects the same Data Dial focus state used by the Android UI. The LCD deliberately remains a glanceable controller companion rather than a miniature copy of the main display: page/transport/sequence-track context remain visible, while a compact FOCUS line identifies the active Data Dial axis/selector. The projection is signature-gated and uses the existing six-chunk LCD SysEx transport.
