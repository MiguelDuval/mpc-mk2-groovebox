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
- UI shell. **MPC 3.9 MIGRATION FOUNDATION IMPLEMENTED — `MpcShell` owns the persistent Toolbar / five configurable shortcuts / XL Channel Strip region / Workspace / contextual Function Bar. Legacy seven-page rail is no longer the canonical shell; legacy workspace pages remain temporary adapters.**
- Main. **UI FOUNDATION + MPC MAIN CONTEXT REFINEMENT IMPLEMENTED — controller-first selected-pad context, quick sample waveform with Start/End editing, layer selection, direct audition trigger, persistent Main Pad Mixer Strip level/pan/tuning readout, real imported sample-name display, and MPC-style BAR/BEAT/TICK toolbar position display. The canonical Main composition no longer embeds an Android 4x4 pad grid, matching the hardware-first MPC workflow. Full Track Edit and full Channel Mixer remain separately gated by backend support.**
- Browser. **LOCAL WAV VERTICAL SLICE + SEARCH/FILTER INTERACTIONS IMPLEMENTED AND ANDROID E2E VERIFIED (Build #2637, code SHA `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`) — MediaStore-backed WAV rows, real selection, MediaPlayer audition, native Pad/Layer assignment, matching/non-matching Search, Clear, PROJECTS empty result and ALL/SAMPLES restoration all pass emulator smoke. Actual Android Document Picker selection/return target-preservation, user-selected document trees and persisted favourites remain the next slices. Unsupported categories stay honestly unavailable.**
- Sampler. **UI FOUNDATION + WAVEFORM IMPLEMENTED — dedicated sample editor context with a shared editable waveform, region/edit, envelope, filter and layer tabs.**
- Sample Edit. **UI FOUNDATION + WAVEFORM IMPLEMENTED — direct S/E drag editing, zoom/pan, audition, crop and chop actions are isolated to the sample context.**
- Grid. **SEQUENCE CONTEXT IMPLEMENTED — dedicated track/sequence context is now available from the Sequencer page; detailed note editing remains next.**
- Step. **SEQUENCE CONTEXT IMPLEMENTED — selected-track Step entry point is available; detailed step note editing remains next.**
- Track Edit. **BOUNDED SEMANTIC WORKSPACE IMPLEMENTED — GLOBAL / SAMPLES / AMP ENV are backed by existing Drum semantics; LFO / MODS / EFFECTS and Edit All Layers remain reserved. MPC-style top TRACK/PAD context and persistent bottom tabs now match the documented Track Edit geometry.**
- mixers. **FOUNDATION IMPLEMENTED — compact Pad Mix view with direct level control; full Track/Pad mixer remains later.**
- 16 Levels. **SEMANTIC VELOCITY SLICE IMPLEMENTED — Level16 captures the last played pad as the source sample, uses the 16 physical positions as fixed velocity steps from 1 to 127, and keeps note-on/note-off bound to the same source pad for recording. Tune/Filter/Layer/Attack/Decay parameter variants remain future slices.**
- Pad Perform.
- Q-Link.

## 2026-10-06 Main geometry fidelity increment

Branch: `feature/mpc-one-ui-fidelity`

- Main Sequence and Track/Arrangement zones now use dense rectangular surfaces with a thin MPC-red boundary between them rather than heavy outer card framing.
- Field-level borders remain available for active/editable controls.
- No semantic/backend ownership changed; this is presentation-only Main geometry fidelity.
- Source regression contract added to `tests/android_ui_smoke.sh`.
- Fresh Android Build + emulator smoke + UI visual evidence remain mandatory before acceptance.
- Physical MPC Studio MkII verification remains pending.

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

- Shell composition advanced — the persistent left edge is now: five shortcuts → compact contextual track/program channel strip → workspace.
- Context moved out of the workspace header — Sequence, Track, Program, Pad and Data Dial focus remain glanceable without consuming Main workspace height.
- Direct Main selection entry preserved — Sequence/Track/Program context fields target the existing Main subcontexts.
- UI audit hardened — the smoke audit now requires the persistent compact context strip and validates its minimum usable geometry.

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

- **Historical implementation checkpoint:** this early Main composition paired a software 4×4 performance grid with the selected Pad/Layer quick waveform context.
- **Quick sample editing reuses existing backend semantics:** waveform Start/End edits call the existing sample-region command; no new realtime/audio engine boundary was added.
- **Layer context is explicit:** Main exposes Layer as one Track-state field; tapping it enters the shared SAMPLE_LAYER Data Dial focus while the standard hardware +/- path changes the value without leaving the selected Track/Sequence context.
- **Superseded by the 2026-10-04 controller-first correction:** the canonical Main layout no longer renders a software 4×4 pad matrix, and the Main canvas no longer carries a permanent SAMPLE EDIT duplicate. Pads are selected from the MPC Studio MkII; waveform double-tap remains the Track Edit entry.

## 2026-10-02 Main Track state-row parity increment

- **MPC Main vocabulary added:** the Track workspace now exposes a compact Monitor / Length / Velocity / Layer row in the same visual neighborhood as the Track/Arrangement canvas.
- **Truthful availability enforced:** Monitor and track-level Velocity remain explicitly unavailable because the current semantic backend has no corresponding state; Length is shown as sequence-scoped; Layer reflects the selected Drum sample layer.
- **State synchronization:** changing the selected layer updates the Main Layer field without changing Track, Sequence or transport context.
- **QA contract extended:** the Android UI smoke preflight now requires all four Main Track-state content descriptions.

## 2026-10-02 Main semantic double-tap increment

- **Track Edit entry gesture wired:** double-tapping Main's Track sample/waveform enters the Track Edit semantic context; until the backend editor contract exists, the app shows a reserved gateway with current Track/Pad/Layer rather than a silent no-op.
- **Arrangement → Grid gesture wired:** double-tapping Main's Arrangement overview opens Grid for supported Drum tracks and explicitly refuses unsupported Track Types.
- **Gesture infrastructure isolated:** reusable double-tap hooks were added to the existing WaveformView and SequenceTimelineView without changing their sample-region, loop, zoom or transport semantics.

## 2026-10-02 Main Sequence field parity increment

- **MPC Sequence vocabulary aligned:** Main now presents SEQ / BARS / START / END / TRANSPOSE as compact state fields around the existing sequence name and BPM.
- **Existing semantics preserved:** time signature, loop state, loop markers and the working BPM/BARS/LOOP controls remain available; the parity work changes hierarchy and presentation rather than removing functional controls.
- **Truthful unavailable state:** TRANSPOSE is displayed as unavailable because the current domain has no transpose semantic.

## 2026-10-02 Main controller-first field focus increment

- **Direct field focus implemented:** Main SEQ/BPM/BARS/START/END fields now establish the shared Data Dial focus instead of being presentation-only.
- **Sequence loop editing implemented:** START and END fields use the existing loop-bar semantic command; BARS and BPM use the existing Sequence setters, keeping all mutations outside realtime audio.
- **MPC Function Bar parity refined:** Main and Track View now use five visual slots with grouped SEQ + REC ARM and − TRACK + controls while retaining all six semantic operations.
- **REC ARM indication parity:** the grouped REC ARM control now visibly latches ON/OFF from the real selected-track armed state; the UI audit exercises both transitions.
- **Track header parity refined:** the six-button Track Type row was reduced to a single MPC-style type field; unsupported Track Types remain visibly reserved.
- **Sample workflow parity refined:** an empty Drum pad exposes RECORD as the secondary Main action; a populated pad exposes BROWSE. Track Edit is represented by the compact pencil affordance and the documented double-tap gesture.

## 2026-10-02 Track Edit bounded semantic workspace checkpoint

- **Track Edit transitioned from reserved gateway to a real `MpcTrackEditView` workspace.**
- **Global/Samples/Envelopes** consume existing control-thread/native queries and existing setters; no new realtime path was added.
- **LFO/Modulations/Effects** remain visibly reserved, not simulated.
- **Edit All Layers** remains disabled because an atomic multi-layer backend operation does not exist.
- Main pencil and Main Track waveform double-tap now enter the same Track Edit workspace; selected Track/Pad/Layer context is preserved.
- **Studio MkII layer focus corrected:** Track Edit and Sample Select use hardware focus 10 for Data Dial/+/- layer selection.
- **Track Edit became a promotable Menu/shortcut mode** because the currently implemented subset is truthful; unsupported tabs remain individually reserved.


- **Track Edit controller synchronization refinement:** shared pad selection now refreshes the active Track Edit workspace, keeping MkII pad selection and the Track Edit context synchronized.


- **Main canvas fidelity refinement:** removed duplicate local sequence steppers, flattened Main workstation sections, added visible Data Dial selection outline, and kept unavailable Sequence Edit as a truthful reserved affordance.


- **Shell fidelity:** converted the persistent context column into a condensed Mixer Strip area and reduced the toolbar to the MPC Main information hierarchy.


### 2026-10-02 Track Edit geometry increment

- **IMPLEMENTED —** moved Track Edit tabs from the editor header into a persistent bottom bar.
- **IMPLEMENTED —** replaced the oversized page-title/context stack with the documented TRACK/PAD header and Edit All Layers affordance.
- **IMPLEMENTED —** aligned visible tab labels to MPC terminology: GLOBAL, SAMPLES, AMP ENV, LFO, MODS, EFFECTS.
- **PRESERVED —** existing semantic/backend coverage and explicit RESERVED states.


### Pad Mixer Data Dial checkpoint — 2026-10-04

**IMPLEMENTED on feature/mpc-one-ui-fidelity**

- Replaced the obsolete four-strip Pad Mixer diagnostic view with a dedicated 16-pad MPC-style workspace.
- Eight compact strips are sized for simultaneous visibility, with horizontal access to all 16 pads.
- Level uses a custom vertical fader; Pan and Tune remain directly editable through existing native pad operations.
- Data Dial semantics are explicit: selected Pad + focused Level/Pan/Tune field. Dial press cycles the focus; Dial delta edits only the focused parameter.
- Channel Mixer remains explicitly RESERVED because track-strip mixer backend semantics are not yet implemented.
- Browser chrome was brought onto the same zero-radius, red-selection shell language.


### 2026-10-04 UI fidelity checkpoint — Toolbar / Browser / Menu

- **Toolbar:** graphite persistent chrome; red reserved for selection/accent semantics.
- **Browser:** three MPC context tabs (`Places / Content / Expansions`), official file-type filter vocabulary, single-workspace composition, and Function Bar actions for Sample Assign / Audition / Load.
- **Menu:** 4×4 mode launcher remains intact; system actions moved to the single shell Function Bar to avoid nested command bars.
- **Pad Mixer:** dedicated 16-pad workspace, eight visible strips, real Level/Pan/Tune operations, and Data Dial focus cycle.


### 2026-10-04 Main fidelity checkpoint — XL Mixer Strips

The highest-priority Main-shell correction is complete at the presentation layer: the visible 210dp context column now hosts a dedicated `MpcMainMixerStripView` with Track/Pad/Output strip hierarchy. Legacy persistent context widgets remain mounted but hidden during migration.

Acceptance focus for the next run:
- Main launches with Mixer Strips visible.
- Track mode shows Track + Main Output.
- Drum Pad mode shows Pad + selected Track.
- Track/Pad switching remains only in the Main Track/Arrangement control.
- Toolbar uses the documented graphite Main surface; red is reserved for semantic selection/accent surfaces.
- Existing audio, sequencer, MIDI and controller semantics remain untouched.


### 2026-10-04 Main XL Strip visual confirmation

- Official MPC3 references were used to validate the left-side Main composition.
- Main XL strips stay compact: in Track context, selected Track + Main Output; in Drum Pad context, selected Pad + selected Track.
- The Track/Pad selector remains in the lower-right Track/Arrangement context and is not duplicated in the strip header.
- Vertical meter rendering is presentation-only; unsupported track/output gain semantics remain explicitly reserved.
- Shortcut icons are rendered by an original deterministic Drawable rather than platform-dependent Unicode glyphs.


## 2026-10-04 XL Channel Strip fidelity checkpoint

Branch: `feature/mpc-one-ui-fidelity`

The Main XL Channel Strip has been tightened against the documented MPC 3.9 Mixer Strips composition. The presentation now uses:

- two adjacent full-height channel strips after the five shortcut icons;
- Track mode: selected Track on the left, Main Output on the right;
- Drum Pad mode: selected Pad on the left, its selected Track on the right;
- flat LVL / FX / SEND / I/O header vocabulary with LVL as the currently implemented view;
- compact icon-only top visibility control rather than a redundant textual MIXER header;
- dense level meter + white-line fader geometry and dedicated pan slider;
- strip-local Track/Pad identity and program/sample context;
- Track Mute / Solo / Automation / Record surfaces with truthful availability;
- no standalone duplicate Data Dial row inside the XL strip.

The right-hand strip remains context-aware by model, but FX/SEND/I/O content is not fabricated until corresponding mixer backend semantics exist.

Primary source: MPC Standalone OS User Guide v3.9, Mixer Strips section (pp. 135-136 in the indexed manual). Akai's MPC3 FAQ also identifies the XL Channel Strip and One-to-One Track Workflow as MPC3 features.

No realtime audio callback, sampler scheduler, sequencer clock, raw MkII decoder, or hardware SysEx protocol was changed.

Verification target:
- Android compile/unit tests;
- Android emulator smoke;
- UI visual evidence artifact;
- physical MPC Studio MkII verification remains separate.


## 2026-10-04 Main Track Type UI correction

The earlier six-icon Track Type presentation was identified as an interpretation error during a direct re-check of the MPC 3.9 User Guide. The documented Main Track workflow uses a single Track Type icon beside the Track identity; tapping it opens Track Type selection. The six types (Drum, Keygroup, Plugin, MIDI, Clip, CV) are selector choices, not six persistent Main-header controls.

The implementation has therefore been corrected:
- one deterministic Track Type icon is visible in the Main Track header;
- its icon changes with the selected Track Type;
- tap and Data Dial focus enter the existing Track Type Select subcontext;
- unsupported Track Types remain truthful/unavailable in the current backend;
- the six-type vocabulary remains in the selector/domain mapping.

This correction takes precedence over the earlier 2026-10-04 Main Track type affordance fidelity checkpoint wording that described a persistent six-icon cluster.

Primary source: MPC Standalone OS User Guide v3.9, Main Mode Track section; the manual explicitly describes tapping the Track Type icon next to the track number to change type.


## 2026-10-04 Main Track identity geometry correction

The selected Track Type icon is positioned directly beside the Track identity and before the Track name, matching the documented Main Track workflow where the Track Type icon sits next to the track number. The icon remains a single selector affordance; six Track Types remain choices inside Track Type Select rather than persistent header controls.


## 2026-10-04 Main Track controller-first layout correction

The canonical Main Track/Arrangement workspace no longer embeds the Android 4x4 performance pad grid. MPC 3.9 selects Pads from the physical MPC surface; the Main display prioritizes the selected Pad/Layer waveform and its Track-state controls. The existing pad-grid renderer remains reusable only outside the canonical Main composition until a separate touch-first surface is intentionally designed.

## 2026-10-04 Main Track sample-area fidelity correction

- **IMPLEMENTED —** the Track sample canvas now uses the full available Main workspace width instead of the obsolete 64% split.
- **IMPLEMENTED —** an empty selected Drum pad presents large in-canvas BROWSE / RECORD actions, matching the documented MPC Main empty-sample workflow.
- **IMPLEMENTED —** a loaded sample keeps the waveform dominant and reduces audition to one compact play control; permanent SAMPLE EDIT duplication was removed from the Main canvas.
- **PRESERVED —** waveform selection editing and double-tap to Track Edit semantics; no audio, sampler, sequencer, MIDI or controller transport changes.


## 2026-10-04 Shortcut Rail selection indicator fidelity

- **IMPLEMENTED —** active Main shortcut selection now remains on the graphite shortcut surface and uses a thin red edge indicator instead of a full red tile.
- **PRESERVED —** five-shortcut navigation, deterministic icon rendering and controller semantics are unchanged.
- **RATIONALE —** the MPC Main shortcut rail communicates selection with a compact edge accent, keeping the rail visually subordinate to the central workspace.



### 2026-10-05 MPC Pull-Down Menu fidelity checkpoint

- **IMPLEMENTED —** added a persistent shell-owned Pull-Down surface with two visual pages: MPC Control and Q-Link.
- **IMPLEMENTED —** Main transport/position area accepts a downward swipe to open the panel; tapping outside or the panel close button dismisses it; upward swipe closes from the panel.
- **IMPLEMENTED —** current Sequence/BPM and MIDI/Audio readiness are projected into the Control page from existing UI/native state.
- **TRUTHFUL RESERVED —** Q-Link Learn / Momentary / Go To Min / Go To Previous remain explicitly unavailable until semantic Q-Link backend contracts exist.
- **PRESERVED —** transport, audio engine, sequencer, raw MkII decoder and hardware SysEx layers are untouched.
- **Verification target —** Android source/unit tests, emulator smoke, visual evidence, then physical MPC Studio MkII interaction review.


### 2026-10-05 MPC Pull-Down + Menu acceptance checkpoint

- Android Build #2299 (`83790e38d20d74687102bcd2f60e4d8b796ed912`) completed successfully.
- Full source/unit preflight, Debug APK assembly, emulator startup and `runUiAudit()` passed.
- Pull-Down now exposes the two-page Control/Q-Link shell; Q-Link operations remain explicitly RESERVED until backend semantics exist.
- Main 4×4 Menu tiles now use the deterministic MPC shortcut vector vocabulary instead of visible Unicode glyphs.
- KVM-enabled emulator smoke is now stable enough to exercise the real application runtime instead of failing during AVD boot.
- Physical MPC Studio MkII verification is still pending; this acceptance does not claim hardware validation.

## 2026-10-05 Pull-Down chrome fidelity correction

- **IMPLEMENTED —** Pull-Down close/previous/next controls now use deterministic original vector iconography rather than Unicode glyphs.
- **PRESERVED —** two-page Control/Q-Link overlay, truthful RESERVED actions, and existing shell/state ownership.
- **LOWER LAYERS —** none touched; audio callback, sampler/sequencer timing, MIDI transport, raw MkII decoder and hardware SysEx are unchanged.
- **Verification target —** source/unit preflight, Android emulator smoke, visual evidence, then physical MPC Studio MkII verification.


## 2026-10-05 Android runtime smoke synchronization checkpoint

Branch: `feature/mpc-one-ui-fidelity`

The Android emulator smoke gate now waits for the application-side `runUiAudit()` to report `UI_INTERACTION_COMPLETE` before requesting the external `uiautomator dump`. The previous fixed 5-second delay could race the long UI-thread audit: the app was alive and had produced a screenshot, but `uiautomator dump` could still report an idle-state failure while MainActivity was actively rebuilding contexts.

The gate now:
- fails immediately on an application-side `UI_HIERARCHY_FAILED` / `UI_INTERACTION_FAILED` / startup-finalization failure;
- bounds the application-audit wait to 120 seconds;
- bounds the accessibility dump itself to 30 seconds;
- preserves the real hierarchy/content assertion and therefore does not mask a runtime failure.

Lower layers touched: none.
Physical MPC Studio MkII verification: pending.


## 2026-10-05 UI-audit hierarchy correction

The runtime audit was aligned with the canonical MPC Main composition after Run #2323 exposed a stale test assumption. Main no longer exposes the legacy 4×4 pad grid or an always-visible Main shortcut, so the audit now validates the persistent Toolbar, exactly five shortcut destinations, Sequence/Track workspace, XL Channel Strip control and Main Function Bar directly.

Navigation checks now use the real shell hierarchy: Browser returns to Main through its Function Bar, and secondary modes are entered from the Toolbar Menu. The audit verifies controller-first Pad state through semantic selection/focus rather than fabricating an on-screen pad grid.

No product UI compatibility elements were reintroduced and no protected audio/MIDI/sequencer layers were changed.


## 2026-10-05 Factory Shortcut accessibility vocabulary correction

The factory fifth Shortcut Rail destination remains the `XY` user-facing destination while the internal navigation enum is `XYFX`. The shell now exposes the canonical `XY` label in the accessibility/semantic surface without changing the underlying mode identity or routing.

This is a presentation/accessibility vocabulary correction only; no audio, MIDI, decoder, SysEx or sequencer layer was touched.


## 2026-10-05 Compact context geometry correction

The runtime audit previously required the retired compact Track/Program context to exceed 300dp height. The implementation intentionally uses `WRAP_CONTENT` for this rail, with a compact stack of Sequence/Track/Program/Pad/Data Dial/overview fields, so the 300dp threshold was not a valid fidelity invariant.

The audit now requires a minimum 200dp content height plus the existing width and child-presence checks. This keeps the test anchored to a meaningful persistent context surface without forcing artificial empty space into the MPC shell.


## 2026-10-05 Main runtime audit lifecycle correction

Runtime evidence showed the Main UI was visually laid out correctly in the captured screenshot while the application audit could observe a still-unmeasured WaveformView when invoked directly from startup finalization. The audit is now scheduled from the decor root's first `OnPreDraw` callback.

This keeps geometry assertions tied to the actual rendered MPC shell lifecycle instead of adding sleeps or weakening the checks.


## 2026-10-05 Exact accessibility target correction

The Main LAYER audit now uses the field's complete accessibility description (`Main Track View selected layer • tap to focus Layer`). The view search helper intentionally uses exact semantic labels, so the previous shortened lookup could not find an otherwise functional controller-first Layer focus control.

## 2026-10-06 Main LAYER vocabulary fidelity

Branch: `feature/mpc-one-ui-fidelity`

`Main` now presents the selected Drum layer as `LAYER 1` / `LAYER 2` / etc., matching the documented MPC Main field vocabulary. The semantic layer range remains unchanged; the UI no longer adds a synthetic `/8` suffix that is not part of the MPC Main surface.

Presentation-only. No audio, sampler, sequencer, MIDI, MkII decoder or SysEx ownership changed.

## 2026-10-06 Startup visual-evidence ordering

Branch: `feature/mpc-one-ui-fidelity`

The Android smoke harness now captures the startup screenshot after launch/process/activity/fatal-crash checks and before the settling interval that allows the application-side UI audit to navigate across contexts. This keeps the visual artifact anchored to the canonical Main surface.

Acceptance-harness synchronization only. No audio, sampler, sequencer, MIDI, MPC Studio MkII decoder or SysEx behavior changed.

## 2026-10-06 MPC3 shell reconciliation

This roadmap is subordinate to `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md` and `docs/MPC3-9-OS-KNOWLEDGE-BASE.md`. The canonical visible shell is **Toolbar → five Shortcuts → XL Channel Strip region → Main workspace → Function Bar**. Any historical references to a persistent Android Sequence/Track/Program/Data Dial context rail are migration-only and must not be reintroduced into the visible Main UI.

Source: official MPC Standalone OS User Guide v3.9 Main Mode Overview (p15) and Akai Output Routing Basics.
Checked: 2026-10-06.
Confidence: HIGH.


## Current execution directive — local-first product / 2026-10-10

This directive updates the order of work; it does not authorize backend rewrites or replace the detailed stage history above. Consult `docs/SESSION-STATE.md` and `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` before choosing the next task.

### Product-scope decision

MPC Standalone OS 3.9 remains the UI/interaction reference, but reproducing Akai's commercial ecosystem is explicitly out of scope. Do not implement Splice, marketplaces, paid-expansion promotion, partner advertising, upsells or unnecessary vendor-specific cloud/account dependencies. Generic local file access, Android Document Provider, available USB storage, local user content, and real sample loading remain in scope. In reference documents, label factory commercial items as reference-only/intentionally excluded rather than erasing the historical research.

### Ordered implementation priorities

1. **Commercial exclusion DONE:** Splice is absent from product navigation and source smoke rejects its reintroduction. Stores, paid-content promotion, advertising, upsells and unnecessary vendor cloud/account dependencies remain excluded.
2. **Local Browser + Document Picker E2E — VERIFIED:** real local WAV discovery/filter/search, selection, audition, native loading/assignment, picker return to the correct Pad/Layer, and user document-tree results with persisted SAF grant survive process restart.
3. **Main Track Type correctness — VERIFIED:** native AUDIO/DRUM map to the correct icons; CLIP is not used as a Track Type; unsupported conversion truthfully reports RESERVED. Build #2704 onward passed.
4. **Arrangement event → Grid focus — VERIFIED on #2713:** seeded real note, production Arrangement hit-test, actual View/GestureDetector double-tap path, exact Track/tick delivery and focus target in Grid screenshot/XML; target step 33 is inside 25–40/64.
5. **Arrange loop handle — VERIFIED on #2713:** real handle drag changes native loop end; second handle drag restores the original loop; no pattern-event mutation is used for this check.
6. **Arrange lane selection — PARTIAL, honest fixture limitation:** on the clean one-Track fixture, the real lane tap preserves native and View selection state and proves Arrangement event data is unchanged. Build #2712 first exposed that the fixture has only one Track. The code now does not fail or fabricate an extra Track; the multi-Track change-and-restore branch is present but has not been exercised by this fixture because there is no native Track-removal contract.
7. **Arrange ↔ Grid Back/context round-trip — VERIFIED on #2741:** the real Grid Function Bar BACK returns to ARRANGE with the same Track/sequence/tick/event and unchanged pattern snapshots; a fresh same-event double-tap reopens the same absolute Grid step/window. Fresh screenshot/XML evidence was inspected.
8. **Favorite folders 1–5 — IMPLEMENTED; FAVORITE 1 E2E VERIFIED on #2741:** separate persisted SAF URI/name per slot and real WAV scanning. The real picker flow, independent USER/FAVORITE URI preferences and Favorite 1 restoration after process restart passed with screenshot/XML evidence. Slots 2–5 do not yet have separate real-picker/restart acceptance fixtures.
9. **Other local-storage gaps remain:** Content places without real providers stay truthfully unavailable/no-result, never fake populated.
10. Physical MPC Studio MkII behavior remains pending; emulator success is not hardware verification.

### Definition of done for a roadmap item

A UI item is not done because its button is drawn or an audit assertion passes. Record the real semantic action/state change, relevant tests, current-SHA Actions result, APK identity, inspected screenshots/UI hierarchy, and physical hardware status if relevant. Confirm that expected evidence files are actually present in the downloadable artifact.

Do not let repeated geometry-only smoke hardening displace feature delivery once a screen's key safety/interaction contracts pass. Fix real regressions promptly, then return to the highest-value incomplete workflow.


## 2026-10-11 — MPC 3.9.1 Gen1 firmware research checkpoint

A first-pass offline static audit is documented in `docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md` with structured facts in `docs/research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json`. The supplied image's XZ rootfs digest was verified, the ext4-format rootfs was traversed, and the stripped 32-bit ARM `MPC` ELF version note was identified as `3.9.1.2 (f3af35cdc95)`. Bundled resources include 759 PNGs and 13 ACVS JSON layout descriptors; static strings/type-info identify Main layout and Q-Link/Browser-related research anchors.

This is a research/documentation-only milestone; no Android code or realtime/audio/MIDI layer changed. No firmware, extracted proprietary images/content or full decompilation was added to the public repository. Bundled panel layout values are not yet confirmed as physical MPC One Main geometry. Follow-up native analysis (Ghidra plus nested SquashFS extraction) is optional research, not a prerequisite for the next product task. **The next implementation priority remains Android Document Picker selection → return → sample-target preservation**, then permissions/restoration tests for a persisted user-selected directory and favourites.


## 2026-10-11 — Firmware second-pass UI schema research

The second pass parsed Program Edit TUI JSON schema counts and Main/touch UI type-name anchors from the 3.9.1 Gen1 ELF. Main-specific names include `TouchMainPageNavigationPanel`, `TouchMainPageContentPanel`, track-specific panels, `TouchMainModeMixerProperties`, and dedicated Mixer strip containers. These are static identifiers, not decompiled behavior.

**New correctness lead:** both the firmware's six Track Type selector assets and the official v3.9 guide point to **Audio / Drum / Keygroup / Plugin / MIDI / CV**. The recent project selector vocabulary places CLIP where AUDIO should be audited. Treat this as a high-priority fidelity discrepancy: inspect the existing selector/enum/tests after completing the current Document Picker select-return-target-preservation workflow, and keep Clip/Arrangement and Mixer strip categories separate. No app code was changed by this research checkpoint.

Full data and limitations: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md`.


### 2026-10-14 — Arrangement event navigation slice

The current source review identified that `onEventDoubleTapped(trackIndex, tick)` discarded `tick` before entering Grid. Grid therefore opened at its stale viewport, a real workflow defect rather than a styling gap. The current changeset adds a pure tick→step/viewport policy, centers the target step when possible and clamps at the start/end of the sequence; boundary/overflow unit tests and source preflight cover the route. Exact-SHA Android CI is pending. The preceding Track Type correction is already verified on #2704.


### 2026-10-14 — Direct runtime evidence for Arrange → Grid event focus

Build #2705 passed the event-tick policy tests and full emulator workflow. The current follow-up upgrades evidence quality: the UI audit invokes the same production route used by Arrangement's double-tap listener at a non-zero synthetic tick, asserts the Activity and rendered Grid viewport agree, then pauses so the emulator harness can capture an actual screenshot and UI hierarchy. The harness checks the focus status and displayed `STEPS first–last / total` window before releasing the app-side audit. Exact-SHA CI is pending.


### 2026-10-14 — Build #2707 accepts Arrangement → Grid focus evidence

Android Build #2707 tested SHA `8c5bfcc114b5e2becfdf50a4cdf861bb0d57a623` and succeeded (57/57 steps). APK artifact `11867684347` (SHA-256 `38557fcdf0ac7b92d7a5b7ad0df9f8199dea67b59fbba44fd7955d04ab53e844`) and a 23-entry evidence archive `11867864670` (SHA-256 `7dae7d4c1b054cfa76ace4ce6744b5fbb82232b89e09aeb6f52bf01a4708f6cd`) were downloaded, integrity checked and inspected. The focused Grid screen shows step 33 inside 25–40/64; semantic accessibility matches. Browser E2E and SAF restoration also passed.

The remaining coverage gap is at the event boundary: runtime audit invokes the shared production route with a synthetic non-zero tick, but does not yet double-tap a populated Arrangement event through Android hit testing and listener delivery. Cover that real event-to-Grid transition next.


### 2026-10-14 — Build #2709 validates the real Arrange event gesture path

Android Build #2709 tested SHA `20ac2f54ca0075256a6b6534f5ffa7e06d358b77` and passed all 57 steps. The emulator audit creates a real note via the current sequencer API, derives the rendered Arrangement event center and first validates that the view hit-test resolves to that exact event. It then dispatches Android down/up events into the actual view and GestureDetector, verifies the listener returns the exact seeded track/tick, and checks that Grid's visible range contains step 33. The captured screenshot shows the seeded note in the `25–40/64` window; XML semantic range agrees. APK artifact `11871064482` SHA-256 `e653b9b42b4ec971fed383dbb9a1dd033ec0285f50cb94f7b8d6fc369624eb6a`; 23-file evidence archive `11871970352` SHA-256 `53f5e282f46e97e16ea399a1813bb8913192136385f06bb86f4541dd2fc0478e`. Next focus is real Arrange state/loop interactions, without inventing clip-placement semantics.


### 2026-10-15 — Build #2713 validates Arrange loop and single-lane contracts (historical checkpoint)

Android Build #2713 on exact SHA `cdcfdfb2500b4c6eaebeab5018b4cdce33213462` passed all 57 workflow steps. The preceding #2712 failure was an invalid test-fixture assumption (the clean emulator starts with one Track); the follow-up validates the actual single-lane tap without adding a disposable Track or claiming an alternate Track change. The multi-Track change/restore branch remains for fixtures that truly contain multiple Tracks. The loop-end drag/commit/restore, seeded event double-tap → Grid focus (step 33 within 25–40/64), local Browser assignment, Android Document Picker assignment and SAF folder restoration all passed.

APK artifact `11958217657` SHA-256 `4ca32dee06e76b69314446ab5a12b9ec8bf9a488d84c5d7487f1a4861783f960`; 23-entry visual archive `11958527786` SHA-256 `09fa5c463ee3f7449f4cf36907b89e38fae3ddafb057d4d1e3257bd99bbd10c5`. The next interaction slice is BACK from Grid to Arrange and re-entry through the same event, verifying Track/event context without mutating pattern data.

### 2026-10-15 — Browser Favorite folders 1–5 implementation (original acceptance pending; later verified by #2732)

Added five independently persisted SAF folder targets to the Browser Places list. Each Favorite stores its own URI/name, requires the persisted read grant, loads actual WAV entries through the existing document-tree catalog and can be reassigned through the Android tree picker. The local WAV filter policy accepts Favorite 1–5 only; an old folder's cached rows are cleared while switching locations. The later exact-HEAD acceptance result is recorded below.


### 2026-10-15 — Favorite picker runtime failure (#2721)

Android Build #2721 compiled and uploaded an APK for exact SHA `829b006a5f9e8547d82ed341c772155215c008b2`, but the full emulator smoke failed during FAVORITE 1 assignment. Inspected screenshot evidence shows DocumentsUI reopened inside `AAA-MPC-Browser-Tree` (USER FOLDER) instead of at the provider root; the previous test required `BBB-MPC-Browser-Favorite` to already be visible and exited before assignment. The revised smoke flow navigates the visible breadcrumb to the provider root, requires the Android `Use this folder` tree-grant action, and locates the Favorite-only fixture using bounded scrolling. **Favorites E2E remains unverified** until an exact-HEAD run passes assignment, isolated URI preferences, bidirectional switching and process-restart restoration. If the tree-grant action is still missing after navigation, fix the runtime route rather than weakening the assertion.


### 2026-10-15 — API 31 system overlay preflight (#2730)

Android Build #2730 on exact SHA `004ab89c3e4041f36f8ebfaeca63fe8283c3d3d7` failed before Browser or Favorite navigation. Its captured Main-ready screenshot and hierarchy show Android 12's first-run fullscreen confirmation over the real rendered Main workspace: `Viewing full screen` (`android:id/immersive_cling_title`) and enabled `GOT IT` (`android:id/ok`). This explains the missing `MPC One Main Toolbar` accessibility node; it is a system overlay, not evidence of a product shell regression. Run #2730: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38785925092

The smoke harness now inspects the initial hierarchy, taps the exact enabled OS `GOT IT` target through the API 31 system input injector, verifies the cling is gone, and only then captures startup/Main evidence. This is a bounded platform setup step and preserves strict Main assertions.

The previous #2729 evidence establishes that coordinates are stable and that title, icon, row centre, and right-side row gutter taps do not enter the Favorite directory on API 30. The API 31 `cmd input touchscreen tap` folder-row correction is still untested because #2730 stopped earlier. Do not call Favorites fixed until a new exact-head run completes separate-folder assignment, distinct URI persistence, bidirectional WAV switching and Favorite restoration after process restart.


### 2026-10-15 — Build #2732 verifies Browser Favorite 1 end to end

Android Build #2732 passed all 57 workflow steps on exact SHA `b7cebdc73943b55dd5440c549ec40e2ca7cde1c8` ([Actions run](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38798857884)). The emulator opened the real DocumentsUI provider root, entered `BBB-MPC-Browser-Favorite`, confirmed the Android tree grant and showed the real `Browser-Favorite-Only.wav`. Saved preferences contained independent `user_tree_uri` and `favorite_tree_uri_1` values for `AAA-MPC-Browser-Tree` and `BBB-MPC-Browser-Favorite`. Switching both directions restored the distinct real WAV rows/status, and force-stop/relaunch restored FAVORITE 1 and its file. Screenshot/UI hierarchy evidence was inspected. This remains emulator-only; physical MPC Studio MkII testing is pending.

### 2026-10-15 — Arrange → Grid → BACK → Arrange → same-event re-entry (runtime validation added; CI pending)

The audit uses the actual Grid Function Bar `BACK` click, verifies Arrangement returns with the same native/UI Track, sequence index and tick-addressable event, then compares all Tracks' Arrangement projections and every quantized Pad velocity on the selected Drum Track before and after navigation. It clears the old callback result and dispatches a fresh double-tap through the real `MpcArrangeView` gesture/listener path to the same event, verifying exact Track/tick, step and viewport before comparing the pattern again. The Android smoke harness captures and validates screenshots and accessibility hierarchies for both Arrange-after-BACK and the re-entered Grid. These assertions are not accepted until the commit's exact-HEAD Android Build passes and its evidence is inspected.


### 2026-10-15 — Build #2734: correct the Grid step-range test oracle

Build #2734 (`3e49c0d37f5115befd9af15b7a3f1f8ccb374216`) successfully compiled the app and reached emulator evidence capture after real Grid Function Bar BACK and re-opening the same Arrangement event. The Arrange screenshot/hierarchy verified the restored Arrangement workspace and enabled GRID action. The Grid re-entry screenshot/hierarchy showed the same Track 01 and step 25–40 of 64, but the test parser demanded a literal `STEPS` prefix not present in the actual visible label (`25–40/64`). That was a test-oracle mismatch, not evidence of a navigation or pattern-data failure. The parser is corrected to accept the real label format while continuing to check the accessibility semantic description and event step. The new exact-HEAD build must pass before this interaction slice can be marked VERIFIED.


### 2026-10-16 — Build #2741 validates Step-page focus, Arrange round-trip and Browser restoration

Android Build #2741 ([run 38941636973](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38941636973)) succeeded on exact implementation SHA `0ee28f46d1d2dfa04514a839b9c7a8744ae72909`. The build ran both `MpcSequenceZoomPolicyTest` and the newly wired `MpcSequenceStepPagePolicyTest`; all native/unit tests, APK assembly, emulator smoke and evidence upload completed successfully.

The new Step policy preserves the relative cursor column while changing the visible 16-step page and computes an absolute step within the sequence boundary. The app-side emulator audit then uses real Step controls: page 1 → page 2, toggles an empty matching column, adjusts the new event's probability through the existing selected-step operation, returns to page 1, re-enters page 2, removes the test event and checks that the existing Step 1 parameters remain unchanged. No new native or realtime audio/MIDI behaviour was added.

The same build verifies Arrange → Grid step 33 in the visible `25–40/64` window → actual Function Bar BACK → unchanged Arrangement event/pattern → same-event re-entry into the same Grid window. The downloaded evidence archive contains 36 files and passed ZIP integrity verification. Inspected screenshot/XML pairs confirm the Arrangement event remains at the focused step, the BACK action returns to Arrangement, and Grid re-entry restores Track 1 and the semantic range. Browser local WAV selection/audition/assignment, Android Document Picker return-target preservation, USER FOLDER permission/result restoration, and FAVORITE 1 independent URI + real WAV restoration after process restart also passed.

Artifact identity for this implementation SHA: APK artifact `12062804474`, 8,228,253 bytes, SHA-256 `e6f07253e8cb83240eba7ac924708363a672fb5b408773750f08069ab7da212b`; visual evidence artifact `12063274521`, 2,654,002 bytes, SHA-256 `938ebfd35935fe83378b233357466458207ddcbcfd5c11bd7f9601fe92449809`. The artifact has no dedicated Step-page screenshot/XML; its Step behavior is asserted by the app-side runtime path and the new unit tests. Physical MPC Studio MkII testing remains pending.

Builds #2738 and #2739 exposed Browser smoke harness issues after the successful APK compile: stale Search state after returning from DocumentsUI, then a false non-empty-query report because UIAutomator placed the empty EditText hint `Search files` in the `text` attribute. The test now explicitly clears Search after picker return and accepts only empty text or the exact hint. The resulting Build #2741 passed, including Browser's real tree and Favorite 1 restoration paths.

### Next implementation target — Track View musical edit round-trip

The current smoke audit checks the Track View workspace/Track strip before moving into Arrangement, but does not prove a user-driven Track View musical edit round-trip. Inspect the live handlers and reuse the existing sequencer operations to test one supported Track View action, confirm the native event and visible result, then return/re-enter Grid and verify the same musical location/data. Do not expand into a Track View rewrite or introduce unimplemented non-Drum operations.

### 2026-10-17 — Build #2747: Track View MUTE state round-trip with a populated Drum pattern

Android Build #2747 ([run 39026004772](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39026004772)) passed on exact implementation SHA `70800387a4d133405be09dcc2cc8a6f6f616463a`. The targeted change moves the existing seeded Arrangement/Drum event setup before the Track View action; the MUTE test's existing Grid velocity snapshot is therefore taken with a real Pad 1 note present rather than an empty pattern.

The emulator path taps the actual Track View MUTE button, checks the native `nativeSequenceIsTrackMuted` result flips and the displayed label reflects the new state, holds that state for an external screenshot/UIAutomator capture, validates the visible/accessibility button, then clicks again and verifies the original mute state, Track/sequence cursor and full per-step velocity snapshot are restored. The captured Track View image shows `EVENTS • 1` and a green `MUTE` control; accessibility XML reports the button as enabled/clickable with real hit bounds. The same audit continues through Arrangement event → Grid step 33 → BACK to Arrangement → same-event Grid re-entry, with the pattern remaining intact.

APK artifact: `12098428540`, 8,228,253 bytes, SHA-256 `41168cb1e1999fb0489fc5ee402c363a333ba32ddad5d63ea2c278c96b83f18a`. Visual evidence artifact: `12098488760`, 2,786,502 bytes, SHA-256 `c9075550b7c5b0b48bfcbdc3611b1aaf7fcbcd4201e2c39fea28bd0a3452fe84`; ZIP integrity passed, 38 files, and the Track View screenshot/XML plus Arrange/Grid screenshot/XML and Browser/SAF evidence were inspected. Physical MPC Studio MkII verification remains pending.

The new fixture is intentionally a test/interaction-acceptance improvement, not a claim that Track View itself edits notes: it verifies the existing MUTE operation does not alter musical event data. The next targeted product workflow is Track Edit sample-layer tuning/gain: change one real supported parameter, verify native/visible state, navigate away and return to the same Pad/Layer and confirm persistence through the existing sampler model. Avoid redesigning the sampler/backend.
