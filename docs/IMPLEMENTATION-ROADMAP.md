# Implementation Roadmap

## Stage 0 — Foundation
- Android shell.
- C++20 native core.
- Oboe linked.
- CI.
- Upstream dependency pins.
- Documentation.

## Stage 1 — Hardware bring-up
- USB MIDI discovery.
- MPC public port detection.
- MIDI IN/OUT.
- pads, velocity, aftertouch.
- buttons.
- jog.
- touch strip.

## Stage 2 — Feedback
- button LEDs. **BASIC STATE-DRIVEN ROUTER IMPLEMENTED — transport, Note Repeat, Level16 and mute-mode states are cached so repeated UI refreshes do not resend identical MIDI feedback. Full LCD/Touch-strip state synchronization remains.**
- pad RGB SysEx.
- touch-strip LEDs.
- Note Repeat indicators. **STATE-DRIVEN SLICE IMPLEMENTED — ON/OFF is reflected on the physical Note Repeat button, and the dedicated division LED bank now mirrors the independently selected repeat rate.**
- 160x80 LCD SysEx.

## Stage 3 — Audio
- JUCE/Tracktion Android integration.
- engine lifecycle.
- audio device management. **LANDSCAPE AUDIO SETTINGS SLICE IMPLEMENTED — explicit Android input/output device selection, AUTO or requested sample rate/buffer, shared/exclusive + low-latency/normal policy, runtime route diagnostics and a synthetic output test tone are now exposed through the Audio Settings page.**
- low-latency stream.
- one sample playback.

## Stage 4 — MPC domain
- Project.
- Sequence.
- Track.
- Drum Program.
- Pad.
- Sample.
- Layer.
- Automation.
- Q-Link.

## Stage 5 — Sampler
- record. **SOFTWARE + UI SLICE IMPLEMENTED — bounded microphone capture in RAM through a dedicated Oboe input stream; the Recorder now shows a live shared waveform, duration, peak, frame count and armed/active/stopped state. Physical recording/assignment workflow remains separately tracked.**
- monitor. **SOFTWARE SLICE IMPLEMENTED — independent bounded lock-free RAM monitor path feeding the low-latency output stream; Monitor On/Off is separate from Record. Independent monitor-only physical verification remains pending.**
- threshold. **SOFTWARE SLICE IMPLEMENTED — configurable 0–100% input threshold; with threshold Off recording starts immediately, otherwise recording arms and begins on the first input frame reaching the threshold. No pre-roll is captured. Physical verification remains pending.**
- trim. **SOFTWARE + UI SLICE IMPLEMENTED — each pad/layer has a non-destructive start/end playback region; the shared WaveformView exposes finger-draggable S/E handles, zoom and pan, with edits committed to the domain-backed region.**
- crop. **SOFTWARE SLICE IMPLEMENTED — destructive crop of the selected pad/layer region into a new PCM buffer on the control thread; the source region becomes the full region of the new buffer, while other pad/layer assignments remain untouched. A small diagnostic UI exposes Crop Region. Physical verification remains pending.**
- chop. **SOFTWARE SLICE IMPLEMENTED — deterministic 4/8/16-way equal chopping of the selected pad/layer region into pads 1-N; all chops share the original PCM buffer and receive independent non-destructive playback regions. A small diagnostic UI exposes Chop 4/8/16. Physical verification remains pending.**
- assign. **DONE for imported WAVs; physically verified on Build #135 with two different WAV samples on two different physical pads. RECORDED-AUDIO ASSIGNMENT SOFTWARE SLICE IMPLEMENTED — the last stopped microphone recording can be promoted into a selected pad/layer; physically verified on the real MPC Studio MkII.**
- multi-layer playback. **SOFTWARE SLICE IMPLEMENTED — 8 layers per pad; physical/audio verification pending.**
- MPC drum-program domain. **SOFTWARE FOUNDATION IMPLEMENTED — 16 pads × 8 layers now have a data-only domain model; sampler semantic state (sample IDs, regions, layer velocity/gain/tuning/pan, pad tuning/level/pan) is now domain-backed, with AudioEngine keeping only the realtime atomic projection and decoded buffers. A control-thread drum-program snapshot is available for the future sequencer/browser/project-state layers.**
- pitch/tuning. **SOFTWARE SLICE IMPLEMENTED — pad tuning remains the parent transpose; each sample layer now has independent ±24 st tuning, combined at trigger time in the realtime sampler path. Physical verification remains pending.**
- level. **SOFTWARE SLICE IMPLEMENTED — pad-level gain remains the parent level control; each of the eight sample layers now has an independent 0–100% gain applied at trigger time in the realtime sampler path. Physical verification remains pending.**
- pan. **SOFTWARE SLICE IMPLEMENTED — pad pan remains the parent stereo control; each sample layer now has independent L100–C–R100 pan, combined with pad pan in the realtime sampler path. Physical verification remains pending.**
- envelope/filter. **SOFTWARE SLICE IMPLEMENTED — each pad now has a deterministic one-shot ADSR amplitude envelope plus a realtime-safe one-pole low-pass cutoff. Parameters are domain-backed, atomically projected into the audio callback, covered by native contract tests, and exposed through the JNI boundary. Physical verification remains pending; dedicated editor controls remain a later UI slice.**

## Stage 6 — Sequencer
- sequence transport/settings. **SOFTWARE + UI SLICE IMPLEMENTED — active Sequence now owns BPM, 1–128 bar length, time signature, loop enabled/range, quantize grid and swing; the landscape Sequencer page exposes these controls plus a bar timeline with draggable red IN/OUT loop range and live playhead.**
- sequence/track model. **SOFTWARE + UI SLICE IMPLEMENTED — Sequence is the time container; Tracks are independent content lanes with Drum, Keygroup, Plugin, MIDI and Audio kinds, selection and record-arm state.**
- record/overdub. **SOFTWARE + TIMING-CORRECT STATE IMPLEMENTED — deterministic record/overdub commit operations normalize/retain timing according to the sequence Timing Correct state, while preserving Replace vs Overdub semantics.**
- quantize. **SOFTWARE + HARDWARE COMMAND SLICE IMPLEMENTED — deterministic nearest-grid quantization with loop wrapping and stable tie ordering; the MkII Quantize control now applies it to the selected Drum pattern, while Shift+Quantize remains reserved for selection-aware editing.**
- swing. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic grid-aligned off-beat swing with 0–100% amount, bounded half-grid delay, stable ordering, and loop-wrap.**
- step sequencing. **SOFTWARE FOUNDATION IMPLEMENTED — grid-aligned set/replace/erase operations for MIDI notes with deterministic ordering and support for velocity, probability, ratchet, nudge and duration metadata.**
- grid editing.
- probability. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic per-note probability evaluation from a stable seed, with 0/127 fast paths.**
- ratchet. **SOFTWARE FOUNDATION IMPLEMENTED — ratchet metadata normalized to a bounded 1–8 playback count.**
- realtime event scheduler. **SOFTWARE FOUNDATION IMPLEMENTED — allocation-free pattern-window scheduling now bridges the sequencer cursor, deterministic probability and ratchet metadata into timestamped MIDI events, including loop-wrap windows and bounded output buffers. Ratchet expansion into timed retriggers remains a playback-layer concern.**
- automation.
- event duration editing. **SOFTWARE + UI SLICE IMPLEMENTED — selected Drum step events expose deterministic duration editing from ¼ to 4× the active step grid, with playback-time mutation blocked and an effective grid duration shown for legacy zero-duration events. Hardware binding remains a follow-up physical ergonomics slice.**

## Permanent MPC 3.9 UI migration program — 2026-10-02

The former generic "MPC-like UI" Stage 7 is now superseded by a permanent MPC 3.9 standalone UI migration.

Canonical documents:

- docs/MPC3-UI-MIGRATION-MASTER-SPEC.md
- docs/MPC3-REFERENCE-INDEX.md
- docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md

Primary software reference:
https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf

Primary current MPC3 architecture reference:
https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq

### Permanent UI target

The product UI is to be rebuilt around the documented MPC 3.9 information architecture:

Toolbar → Shortcuts → Mixer/Channel context → Sequence → Track/Arrangement → Function Buttons

with Menu as the operating-system launcher and Grid, Step, Track Edit, Sample Edit, Sampler, Browser, Channel Mixer, Pad Mixer and performance modes as coherent contexts.

This is an architectural migration, not a palette/shape reskin.

### Protected lower layer

Do not rewrite or destabilize:

- realtime audio callback;
- sampler engine;
- sequencer scheduler/clock;
- MIDI transport;
- raw MkII decoder;
- hardware SysEx protocol;
- existing domain operations.

Lower-layer edits are allowed only for truthful semantic prerequisites such as Track → Program resolution or read-only state queries.

### Migration order

1. Documentation lock. **DONE — canonical MPC3 migration docs are locked.**
2. UI state/navigation extraction. **FOUNDATION IMPLEMENTED — `MpcUiState` and `MpcNavigationController` are now separate presentation-layer boundaries.**
3. MPC shell extraction. **FOUNDATION IMPLEMENTED — `MpcShell` now owns Toolbar / five shortcuts / compact context / Workspace / Function Bar composition.**
4. Menu + five shortcuts.
5. Main Mode.
6. Track View + Arrangement.
7. Track → Program semantic correction.
8. Grid + existing zoom/Step.
9. Sample Edit + Sampler composition.
10. Browser provider + MPC Browser UI.
11. Channel Mixer + Pad Mixer.
12. 16 Levels / Pad Perform / Next Sequence.
13. Track Edit / Arrange / List Edit / Project / Preferences.
14. Feedback parity and final physical workflow verification.

### Rule

No new top-level UI page is accepted unless its MPC 3.9 context, hardware entry path, semantic state, backend command and controller feedback are documented first.

## Stage 7 — MPC 3.9 standalone UI migration
- UI shell. **MPC 3.9 MIGRATION FOUNDATION IMPLEMENTED — `MpcShell` owns the persistent Toolbar / five configurable shortcuts / compact channel context / Workspace / contextual Function Bar. Legacy seven-page rail is no longer the canonical shell; legacy workspace pages remain temporary adapters.**
- Main. **UI FOUNDATION + MPC MAIN CONTEXT REFINEMENT IMPLEMENTED — 4x4 software performance pads, selected-pad context, quick sample waveform with Start/End editing, layer selection, direct audition trigger, persistent Main Pad Mixer Strip level/pan/tuning readout, real imported sample-name display, MPC-style BAR/BEAT/TICK toolbar position display, direct TRACK/ARRANGEMENT sibling header tabs over one shared workspace, and state-correct BROWSE/RECORD handling for empty pads. Full Track Edit and full Channel Mixer remain separately gated by backend support.**
- Browser. **UI FOUNDATION IMPLEMENTED — dedicated Browser mode with explicit WAV load target; full indexed/searchable browser is a later slice.**
- Sampler. **UI FOUNDATION + WAVEFORM IMPLEMENTED — dedicated sample editor context with a shared editable waveform, region/edit, envelope, filter and layer tabs.**
- Sample Edit. **UI FOUNDATION + WAVEFORM IMPLEMENTED — direct S/E drag editing, zoom/pan, audition, crop and chop actions are isolated to the sample context.**
- Grid. **SEQUENCE CONTEXT IMPLEMENTED — dedicated track/sequence context is now available from the Sequencer page; detailed note editing remains next.**
- Step. **SEQUENCE CONTEXT IMPLEMENTED — selected-track Step entry point is available; detailed step note editing remains next.**
- Track Edit. **BOUNDED SEMANTIC WORKSPACE IMPLEMENTED — GLOBAL / SAMPLES / AMP ENV are backed by existing Drum semantics; LFO / MODS / EFFECTS and Edit All Layers remain reserved. MPC-style top TRACK/PAD context and persistent bottom tabs now match the documented Track Edit geometry.**
- mixers. **FOUNDATION IMPLEMENTED — compact contextual channel-strip view now supports persistent show/hide plus Drum Track Track/Pad focus switching; direct pad level/pan/tuning readouts remain read-only, while full Track/Pad mixer mutation remains later.**
- 16 Levels. **SEMANTIC VELOCITY SLICE IMPLEMENTED — Level16 captures the last played pad as the source sample, uses the 16 physical positions as fixed velocity steps from 1 to 127, and keeps note-on/note-off bound to the same source pad for recording. Tune/Filter/Layer/Attack/Decay parameter variants remain future slices.**
- Pad Perform.
- Q-Link.


### 2026-10-03 MPC channel-strip presentation increment

- The compact shell mixer now follows the MPC 3.x channel-context model more closely: the condensed strip can be shown/hidden independently and, for Drum Tracks, can cycle between the selected Track strip and selected Pad strip.
- The Track/Pad focus is presentation-only and never mutates musical selection or transport state.
- Non-Drum Track Types disable the Pad strip focus so the UI cannot imply a Drum-pad context that the backend does not own.

### 2026-10-03 Main compact-context direct-entry increment — superseded

- This intermediate slice temporarily exposed Sequence/Track/Program navigation through the persistent left rail.
- It was superseded after comparison with the official MPC 3.x XL Channel Strip model.
- The canonical implementation keeps Sequence/Program selection in the central Main workspace and reserves the left rail for mixer/channel presentation.

### 2026-10-03 MPC compact-context state ownership increment

- Compact Mixer visibility and Track/Pad presentation focus are now part of immutable MpcUiState and are mutated through MpcNavigationController.
- Non-Drum Tracks disable the Track/Pad affordance and render the effective context as Track without destroying the stored presentation preference.
- MainActivity now derives the visible compact strip from navigation state instead of owning duplicate presentation booleans.
- Smoke preflight covers the state ownership and effective Drum-only presentation gate.

## Stage 8 — Ableton Link
- tempo.
- beat phase.
- start/stop.
- quantized launch.

## Stage 9 — External I/O
- USB audio.
- generic MIDI controllers.
- routing.

## Stage 10 — MPC project interoperability
- research-backed import/export subset.


## MPC Studio MkII hardware-control slice

This supplements Stages 1, 2 and 7; it does **not** replace the roadmap sequence.

### Hardware semantic routing — P0

After the existing transport/device foundation is protected, the first controller implementation slice is:

- Shift state and documented Shift chords.
- Mode + pad shortcuts.
- Record, Overdub, Stop, Play, Play Start.
- Data Dial rotation/press and +/-.
- Main and Browse.
- Track Select / Sequence Select.
- Pad Banks A–H.
- Note Repeat and Full/Half Level.
- Pad Mute / Track Mute.
- Step/Bar navigation.
- Tap Tempo.
- Sequence Launcher physical-pad routing.

All of these must dispatch semantic actions through the MPC Studio MkII adapter. Do not put controller MIDI numbers in UI code.

### 2026-10-01 Step Edit hardware context

- **PHYSICAL STEP SELECTION SLICE IMPLEMENTED —** while `SEQ • STEP` is active, the 16 physical pads select steps 1–16 of the current page through a semantic action; pad presses no longer trigger audio or Note Repeat in this editor context. Software step editing remains available in parallel. Physical MkII verification remains required.

### Step Edit pad feedback

- **STATE-DRIVEN FEEDBACK SLICE IMPLEMENTED —** in `SEQ • STEP`, the 16 physical pads mirror step state: dim = empty, green = active, amber = selected, red/cyan-ish = playhead, white = selected + playhead; LEDs are updated only when the projected state changes. Leaving Step Edit clears the pad bank. Physical MkII verification remains required.

### Hardware feedback — P0/P1

Then make hardware state visible:

- transport LED state;
- mode/context LED state;
- pad RGB state;
- launcher active/queued state;
- Note Repeat indicators;
- touch-strip indicators.

The existing LED/SysEx generators are protocol foundations, not a completed feedback layer.

### Hardware-context production controls — P1

Add:

- Sample Select;
- Sample Start/End;
- Tune/Fine;
- Quantize;
- Timing Correct;
- Zoom;
- Copy/Delete. **MPC Studio MkII PHYSICAL GESTURE + DOMAIN SLICE IMPLEMENTED — Copy is a stateful hold gesture (source pad → destination pad(s) → release), Shift+Copy selects pad sample-assignment deletion targets; native edit history supports undo/redo for these mutations. Physical verification remains required.**
- Undo/Redo. **GLOBAL COMMAND SLICE IMPLEMENTED — Undo is a normal MkII Undo press and Shift+Undo is Redo; current native history covers pad Copy/Delete edits. Physical verification remains required.**
- Locate;
- Erase;
- touch-strip contextual control. **INITIAL CONTEXTUAL SLICE IMPLEMENTED — Sample Start/End focus scrubs the corresponding region boundary; Tune focus spans −24..+24 st; while Note Repeat is active, the strip selects its independent musical repeat division.**
- compact LCD status.

### Advanced hardware surface — P2/P3

Later:

- Automation Read/Write;
- richer contextual parameter control;
- Song/arrangement shortcut;
- Looper shortcut;
- MIDI Control routing;
- Save/persistence shortcut.

The complete control-by-control contract is documented in `docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md`.


## MPC Studio MkII implementation checkpoint — 2026-09-30

The controller slice has moved from MIDI protocol bring-up to semantic application routing. The canonical path is now:

MPC Studio MkII → InputDecoder → MpcStudioMk2SemanticAdapter → semantic action → domain/sequence/audio command → state → hardware feedback.

P0 code coverage now includes transport, Main/Browse, Track/Sequence selection, Data Dial, pad-bank state, Full/Half Level, mute contexts, stopped Step/Bar navigation, Tap Tempo and Mode+Pad navigation. Sequence playback also preserves a stopped playhead position when Play resumes.

Note Repeat scheduling, Touch Strip contextual control, complete button/pad/LCD feedback, and the remaining P1 production controls stay separate and explicitly incomplete.

This controller work supplements Stages 1, 2 and 7; it does not replace the overall roadmap.


## 2026-10-02 Main Program context truthfulness increment

The Program truthfulness rules remain valid; the older note that described a **persistent compact Program context** refers to the superseded intermediate shell and is no longer a current UI requirement.

- Program Select now resolves availability from the selected Track Type.
- Drum Tracks expose the Drum Program list; non-Drum Tracks show an explicit unavailable state.
- Persistent compact Program context mirrors the same availability instead of offering a misleading Drum assignment action.
- Screen Blueprint now documents the compact context semantics and non-Drum behavior.

## 2026-10-02 Track View workflow increment

- Track View is now a selectable status-strip workspace; row controls no longer duplicate the shell Function Bar.
- Track View exposes type, Program/instrument, event summary, REC-arm, mute and unavailable Solo state at a glance.
- TRACK − / TRACK + now preserve the current Main/Track View context instead of forcing navigation into Track View.
- Track View has an explicit UI-audit workspace/row contract.

## 2026-10-02 MPC 3.9 shell context/channel increment

Historical intermediate shell architecture; superseded by the 2026-10-03 XL Channel Strip alignment. The current left edge is five shortcuts followed by contextual mixer/channel presentation, while Sequence/Program selection and Data Dial focus remain single-owner controls in the central Main workspace.

- UI audit hardened — the smoke audit requires the persistent XL Channel Strip presentation and its Drum-only Track/Pad affordance.

## 2026-10-02 Main Mode UI composition refinement

- **MPC One-style Main region refinement —** Sequence remains the upper context while Track + Program are composed as one continuous Track region; the Program field is directly owned by the selected Track.
- **Selection contexts are now visually explicit —** Sequence Select, Track Select and Program Select remain Main subcontexts rather than creating unrelated top-level pages.
- **Main function bar carries high-frequency Track operations —** New Track, Rec Arm, Track −, Track + and Mute are kept in the contextual function bar; reserved Solo remains visible but unavailable.
- **Workspace duplication reduced —** Track View/Program Select/Browser transitions are kept contextual instead of duplicating the same controls across separate cards.
- **UI audit coverage added —** the in-process smoke audit now verifies Main Track/Program composition and the three Main selection subcontexts.

## 2026-10-01 Note Repeat production increment

- **Independent repeat-rate control implemented:** Note Repeat no longer derives its playback interval from the Sequence recording grid. It owns a separate eight-position rate map with straight and triplet divisions.
- **Touch Strip context implemented:** when Note Repeat is active, CC 33 is interpreted as a discrete rate selector; outside that mode the existing sample/tuning contextual strip behavior is unchanged.
- **Hardware feedback implemented:** the eight Note Repeat division indicators are updated from semantic rate state, including a full clear when Note Repeat is disabled.
- **Timing architecture preserved:** rate changes wake the native scheduler and re-align the next repeat against the same transport clock; Android UI refresh is not part of repeat timing.
- **Recording scope unchanged:** generated Note Repeat hits remain performance-only until a safe recording contract is added.

## 2026-10-01 Physical Step Edit parameter control

 - **Step Edit hardware selection is now actionable:** once a physical pad selects a step in `SEQ • STEP`, the MkII Data Dial and `+/-` edit the selected event without leaving the editor.
 - **Context cycling:** Data Dial press cycles the physical edit target through Velocity, Probability, Ratchet, Nudge and Duration. This is an application-level ergonomic layer, not a claim about the factory Studio MkII mapping.
 - **Precision:** Shift/fine input is meaningful for the continuous Nudge and Duration fields; discrete event fields remain one-unit controls because their domains are integer/discrete.
 - **Safety:** empty steps are never created by encoder editing; playback/audio callbacks are not touched by the parameter-edit path.
 - **Feedback:** status refresh, Step Edit pad LED projection and event information remain synchronized after hardware edits.
 - Physical MkII verification remains required.

## 2026-10-01 Locate hardware production slice

- **LOCATE semantic mode implemented —** the MPC Studio MkII Locate button now supports a short-press toggle and a duration-based momentary hold; the application uses a 350 ms threshold as its local gesture policy.
- **Six timeline locators implemented —** Pads 9–14 set locator slots 1–6 and Pads 1–6 jump to the corresponding slots. Locator positions belong to the active Sequence rather than the Android UI state.
- **Live jump path implemented —** locator jumps update both the transport clock and the active playback session, so a jump can occur without tearing down the sequence playback object.
- **Locate navigation implemented —** Locate + Step moves to the previous/next event of the selected Drum track; Locate + Bar moves to sequence start/end.
- **Locate dial path implemented —** Data Dial and +/- move the playhead by one musical beat; Shift/fine moves by one project tick.
- **Safety and feedback —** empty locator slots report a no-op status; leaving Locate through a display/context command clears the application Locate state; the Locate button LED mirrors active state.
- Physical MkII verification remains required.

## 2026-10-01 Touch Strip controller production increment

- **Touch Strip button routed:** real MkII note 0 cycles five coherent application modes; Shift+press opens a compact non-destructive configuration/status context.
- **Touch event routed:** reverse-engineered note 78 is decoded separately and exposed as semantic touch state.
- **Live parameter modes implemented:** Level, Pan and Tune use existing audio controls; Sample Start/End use the existing region editor.
- **Feedback implemented:** CC 57–65 show selected mode/value; CC 103–110 mirror Note Repeat's selected musical division.
- **Safety preserved:** no UI timers or audio-callback work are introduced; CC 33 remains scheduler-owned while Note Repeat is active.
- Physical MkII verification remains required.


## 2026-10-01 Erase hardware production increment

- **Erase routed:** real MkII Erase button enters a stateful hold context and consumes pad presses.
- **Live erase implemented:** one nearest matching selected-pad event is removed around the current playhead within half-grid tolerance, with circular sequence-boundary handling.
- **Playback preserved:** erase mutates the pattern without stopping/recreating the active playback session.
- **Feedback:** Erase button LED mirrors the active context and status reports armed/no-event/playback-required states.
- **Stopped context remains explicit:** the broader event/automation Erase window is not faked; it stays a future editing slice.
- Physical MkII verification remains required.


- **2026-10-02 MkII Zoom hardware increment**
- **SEQUENCE CONTEXT SLICE IMPLEMENTED —** Zoom note 66 now selects a horizontal or Shift+Zoom vertical context; Sample Editor uses both axes, SEQ Grid uses horizontal time-window zoom plus vertical pad-row zoom, and the SEQ timeline uses horizontal bar-window zoom. Grid vertical zoom keeps all underlying pads available through a swipeable focused row window; timeline zoom follows the playhead so live navigation stays coherent. Physical MkII verification remains required.

## 2026-10-01 LCD product feedback and Touch Strip event-path hardening

- **Touch Strip touch event is now end-to-end:** reverse-engineered Touch Strip touch Note 78 is dispatched through the native semantic adapter instead of being dropped at the MIDI boundary.
- **LCD product layer implemented:** Android renders a compact 160×80 context mirror from application state and reuses the existing six-chunk PNG/SysEx transport.
- **Live-display discipline:** the projected LCD signature uses coarse bar/beat position while playing, so the hardware display is not regenerated for every 80 ms UI tick.
- **State separation preserved:** LCD rendering consumes semantic/application state and does not introduce raw controller MIDI identifiers into UI code.
- Physical MkII verification remains required.


## 2026-10-02 Controller feedback contract

Hardware indication is now treated as a first-class part of every MkII controller feature, not as a final polish pass.

- SEMANTIC → FEEDBACK → PHONE parity: each controller context must project its active state to the MkII LEDs and to a persistent phone-side controller-status strip.
- Two-color MkII buttons: primary context uses protocol color 1/full, Shift/alternate context uses protocol color 2/full; inactive state is explicitly cleared.
- Context focus: Zoom, Track/Sequence Select, Program/Track Type, Sample Select/Start/End and Tune have explicit feedback focus on the controller where the corresponding function is implemented.
- Persistent phone indication: the shell displays MKII, active context, DIAL axis and BANK so the physical controller never becomes an invisible mode switch.
- Stateful performance contexts: Locate, Erase, Copy/Delete, Note Repeat, Full/Half Level, 16 Level and transport keep their state indication synchronized.
- Timing Correct: its enabled state is mirrored to the controller button LED.
- Feedback is state-driven: repeated identical LED frames are suppressed; LCD rendering remains signature-gated.
- Unknown/unimplemented contexts must be visibly reported instead of appearing active.
- Physical MkII verification remains required for LED colors, brightness semantics and all controller-specific feedback paths.


### 2026-10-02 MPC 3.9 UI vocabulary + Main/Menu checkpoint
- **Mode registry implemented.** Menu and five promoted shortcuts now consume one canonical MPC mode vocabulary; unsupported contexts are explicitly marked RESERVED/UNAVAILABLE.
- **Menu implemented.** Replaced the legacy list-style launcher with a 4×4 MPC mode grid plus explicit system actions.
- **Main Mode re-composed.** Main now presents Sequence → Track → Program/Performance/Arrangement information instead of the former pad-grid + inspector composition.
- **Pad Mixer semantics corrected.** The existing four-pad level workspace is exposed as Pad Mixer; Channel Mixer remains reserved until truthful track-strip level/routing backend support exists.
- **Toolbar timing context implemented.** Timing Correct is now a persistent functional control; Metronome and Automation are visibly reserved rather than dead/fake controls.
- **Navigation smoke audit migrated.** UI interaction paths now use canonical Main / Track View / Grid / Step / Menu / Next Sequence / Sample Edit / Sampler contexts.

## 2026-10-02 Browser / Arrangement / Shortcut UX checkpoint
- Browser: **workspace foundation implemented** with MPC information architecture; Android Document Provider remains storage backend.
- Arrangement: **read/write loop-brace + read-only linear event projection implemented**; full clip-placement editing remains blocked by the current pattern-only domain.
- Five shortcuts: **assignment + ordering implemented**; reserved modes cannot be promoted.

## 2026-10-02 Track → Program semantic ownership checkpoint

- **Track → Program is now authoritative for Drum Tracks.** Presentation queries resolve the selected Track's `programId`; playback resolves each Drum Track against its own Program instead of a global active Program.
- **Program Select is implemented as a Main subcontext.** The selected Drum Track can choose among available Drum Programs by touch or MkII Data Dial; non-Drum Tracks remain explicitly gated.
- **New Drum Track default is semantic.** A newly created Drum Track inherits the current active Drum Program rather than a hard-coded program identifier.
- **Main/Track View hardware focus is synchronized on entry.** Main clears selection subcontext and returns Data Dial focus to NONE; Track View enters TRACK_SELECT/Data Dial TRACK; returning from Program Select therefore cannot leave a stale Program focus behind.
- Physical MkII verification remains required.

## 2026-10-02 Main/Step presentation truthfulness increment

- **Track Type display now resolves from semantic backend state:** Main no longer infers a Track Type from human-readable status text, avoiding incorrect MIDI/DRUM fallbacks.
- **Program label normalization hardened:** the persistent compact Program context and Main Program field strip the backend's existing `PROGRAM •` prefix before composing their own field label.
- **Step mutation controls are backend-gated:** unsupported Track Types now leave Step navigation visible while disabling parameter mutation actions, matching the same truthfulness rule already used by Grid.


## 2026-10-02 Main Track/Arrangement view increment

- **Main lower workspace now follows the MPC Track/Arrangement hierarchy:** Track is the default local view; Arrangement is a sibling presentation of the same Sequence/Track state.
- **No duplicate top-level mode introduced:** the view switch is local to Main and preserves transport, selected Track, selected Sequence and Data Dial state.
- **Track workspace keeps truthful affordances:** performance pads and Grid remain available; Track Edit is visible as reserved until its semantic/backend contract is implemented.



## 2026-10-02 Global transport-position strip increment

- **Persistent shell playhead implemented:** a thin non-interactive position strip now sits directly beneath Toolbar and above every workspace.
- **Single state source:** the strip reads Sequence position from the existing refreshSequenceOverview() path; no new timer, clock or audio-callback work was introduced.
- **Navigation continuity:** Main, Track View, Grid, Step, Browser and future shell contexts retain the same transport-position indicator without changing Navigation Mode.


## 2026-10-02 Main Track quick-sample increment

- **MPC-style Main Track surface implemented:** the default Main Track view now pairs 4×4 performance pads with a selected Pad/Layer quick waveform context.