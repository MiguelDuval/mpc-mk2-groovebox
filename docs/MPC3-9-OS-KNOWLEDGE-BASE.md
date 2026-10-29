# MPC Standalone OS 3.9 — Reconstruction Knowledge Base

## 0. Status

Status: CANONICAL RESEARCH / IMPLEMENTATION MAP
Target: MPC Standalone OS 3.9, MPC One / One+ class workflow
Checked: 2026-10-06
Repository branch: feature/mpc-one-ui-fidelity
Known implementation HEAD at research start: 10d1f6b0ed302a930e3548da067c9a90caaae143
Canonical contract: docs/MPC3-UI-MIGRATION-MASTER-SPEC.md

This is the durable research map for future sessions. It records published MPC3 information architecture, routes, control semantics, current implementation status and known gaps. It does not copy Akai artwork, firmware, source code or proprietary resources.

**Visual screenshot archive:** [Open the local 82-context screenshot gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md), [image/PDF extraction manifest](MPC3-UI-REFERENCE-ASSETS/manifest.json), and [asset README](MPC3-UI-REFERENCE-ASSETS/README.md). These are repository-local research references only; they are not bundled into the Android application. The corresponding screen-by-screen source catalog is [MPC3-UI-VISUAL-REFERENCE-CATALOG.md](MPC3-UI-VISUAL-REFERENCE-CATALOG.md).

## Current implementation status override — 2026-10-18

This dated addendum supersedes older Project rows below that say the Project UI/route is absent or RESERVED. The current branch source contains a user-facing Project SAVE, SAVE AS and OPEN route in `MainActivity.java`, wired to the v2 native serializer/loader in `NativeEngine.cpp` and Android Storage Access Framework document intents.

The current emulator smoke scenario saves via SAF, mutates the in-memory Project by adding a Track, reopens the saved file, and checks that Track and Pattern-placement counts return to the saved state. This is a first integrated UI slice, not complete persistence acceptance: the Android smoke does not yet compare each placement identity/timing/source range and every MIDI field, does not force-stop/relaunch between save and open, and does not exercise all malformed/cancel/permission/provider-write failures. Native persistence tests separately cover detailed field round-trip and a placement-free v1 fixture. Sample PCM remains external and must be reloaded/relinked as needed.

The last observed exact-head Android workflow was still running its emulator smoke step at the time of this note. Verify live PR/Actions against the exact current HEAD before marking this flow verified. Use `docs/IMPLEMENTATION-ROADMAP.md`, `docs/SESSION-STATE.md` and `docs/MPC3-USER-ACTION-CATALOG.md` for the operational queue; the older table rows below are research/history and must not restart the now-wired Save/Open integration task.

## 1. Official sources

### S1 — MPC Standalone OS User Guide v3.9
URL: https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf
Checked: 2026-10-06
TOC anchors: Main Mode Overview p15; Browser p20; Linear Arranger p21; Step Sequencer p29; Sampling p34; Sample Editing p36; XY Pad automation p38; Tracks p45; Menu p56; Toolbar p57; Project p60; Preferences p61; Audio/MIDI/Sequencer settings p62–65; Project Load/Save p67–68.
Confidence: HIGH for taxonomy and section placement.

### S2 — MPC3 FAQ
URL: https://support.akaipro.com/en/support/solutions/articles/69000857771-mpc3-faq
Checked: 2026-10-06
Verified: MPC3 is a Standalone hardware OS. MPC One and One+ are supported. Core MPC3 additions include Main Mode, Linear Arranger, Track/Pad Mixer, XL Channel Strip, One-to-One Track Workflow, Disk Streaming, Advanced Automation, Q-Link/XY, Direct-to-Pad Sampling, Full-Color Drum Grid, 8 Sample Layers, plugin/effect compatibility and legacy import.
Critical architecture finding: MPC3 unifies tracks and programs into one track container.
Repository decision: selected Track/container is authoritative; never regress to an MPC2-style shared-program mental model for new UI.
Confidence: HIGH.

### S3 — MPC Browser
URL: https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser
Checked: 2026-10-06
Verified taxonomy: Sample Assign; Places; Content; Expansions; file/drive management; audition settings; favourite folders; filter buttons; Browser Options.
Places includes Internal, MPC Documents and connected storage. Content includes Drums, Instruments, Samples, Demos, My Files and Splice.
Browser has five favourite folders, six file-type filters, audition Auto/Sync/Warp plus audition level, and metadata/search/system-folder/sample-cleanup options.
Repository decision: Android Document Provider stays underneath the semantic Browser; it must not define product navigation.
Confidence: HIGH.

### S4 — Output Routing Basics
URL: https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics
Checked: 2026-10-06
Verified: default MPC3 track routing is Out 1/2; XL Channel Strips are on the left edge next to five mode icons; strips can be shown/hidden; Track/Arrangement has a contextual strip selector; Drum toggles track/single-pad/four-square strip context; Keygroup toggles track/keygroup/keyboard context.
Repository decision: visible left shell column is XL-strip space, not a Sequence/Track/Program dashboard.
Confidence: HIGH.

### S5 — Metronome
URL: https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome
Checked: 2026-10-06
Verified: Main Metro opens settings; holding Metronome in Pull-Down also opens them; Shift + Play Start toggles metronome; Count-In/Enable modes support Off/Record/Record+Play; rate, sound, volume and output routing are configurable.
Repository decision: current disabled METRO is a real backend gap, not an intended MPC3 behavior.
Confidence: HIGH.

### S6 — MkII protocol references
MPC Studio Mk2 SysEx charts: https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts
MPC Studio Mk2 Ableton remote script: https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script
Use only for controller/protocol cross-checking. Physical behavior is not considered confirmed without a real MkII test.

## 2. Version discipline

3.4 / 3.4.1: historical MPC3 lineage. Use for regression history only unless a specific release-note entry is cited. Confidence MEDIUM for individual deltas.
3.7: supporting evidence only; preserve explicit version labels when behavior is not independently confirmed in v3.9. Confidence MEDIUM.
3.9: canonical target. Confidence HIGH.
Desktop MPC3.x: compatibility/controller evidence only, never canonical Standalone visual evidence.

## 3. Canonical shell

Visible order:
Toolbar → five Shortcuts → XL Channel Strip region → active Workspace → Function Bar.

Current code:
Toolbar IMPLEMENTED.
Five shortcuts IMPLEMENTED.
XL Strip host IMPLEMENTED / PARTIAL.
Workspace host IMPLEMENTED.
Function Bar IMPLEMENTED / PARTIAL.
Legacy Sequence/Track/Program/Data Dial context dashboard = INTERNAL MIGRATION ONLY; hidden from canonical Main.
MpcShell.java still names the 210dp host contextArea; it is now interpreted as the XL Strip host.

## 4. Screen catalog

| Context | Entry | Exit | Focus model | Status | Main gap |
|---|---|---|---|---|---|
| Main | boot/shortcut/Menu | contextual/Main | Sequence/Track/Program/Layer | IMPLEMENTED | broader Track Types |
| Menu | Toolbar/Menu | Back | mode/shortcut | IMPLEMENTED / PARTIAL | project/system commands |
| Browser | shortcut/toolbar/Main | Back | Browser Item | PARTIAL | semantic file list/options |
| Track View | Menu/shortcut/Main | Main/Back | Track | PARTIAL | Solo/I/O/mixer depth |
| Arrange | Main/Menu | Grid/Track/Main | Timeline/Zoom | PARTIAL | edit commands |
| Grid | Arrange/Menu/shortcut | Main/Track/Step | cursor/zoom/tool | PARTIAL | melodic/deep editor |
| Step | Menu/Grid/shortcut | Grid/Main | Step/event | PARTIAL | broader track types |
| Track Edit | Main pencil/waveform/Menu | Main/Back | Layer/params | PARTIAL | LFO/Mods/FX/Edit All |
| Sample Edit | Main waveform/Browser | Main/Track | Layer/Start/End | PARTIAL | deeper process taxonomy |
| Sampler | Main Record/Menu | Sample/Main | contextual | PARTIAL | full sampler taxonomy |
| Channel Mixer | Menu/shortcut | previous | mixer control | RESERVED | track mixer backend |
| Pad Mixer | Menu/shortcut | Main/Back | Level/Pan/Tune | PARTIAL | FX/routing |
| 16 Levels | Menu | Main/Back | performance | RESERVED | semantic implementation |
| Pad Perform | Menu | Main/Back | performance | RESERVED | notes/chords/scales |
| Next Sequence | Menu/shortcut | Main/Back | sequence/launcher | PARTIAL | polish/depth |
| List Edit | Menu | Main/Back | event | RESERVED | editor backend |
| Project | Menu | Back | project item | PARTIAL (native codec only) | Android Save/Open route, SAF integration and sample-asset restoration |
| Preferences | Menu | Back | settings | PARTIAL | Audio implemented; MIDI/Sync + Sequencer taxonomy shell reserved |
| MIDI / Control | Toolbar/Menu | Back | contextual | PARTIAL | deeper management |
| Looper | taxonomy | — | performance | RESERVED | backend |
| XYFX | taxonomy | — | performance | RESERVED | backend |
| Sounds | taxonomy | Browser-like | Browser item | RESERVED | sound/expansion workflow |

## 5. Main routing matrix

Main → Sequence: Sequence field → SEQUENCE_SELECT → DataDialFocus.SEQUENCE → sequence list → nativeSequenceSelect → Main; selected sequence/focus retained. IMPLEMENTED.
Main → BPM/BARS: field focus → Data Dial/+/- → existing native sequence setter → Main refresh. IMPLEMENTED.
Main → Start/End: field focus → Data Dial/+/- → nativeSequenceSetLoopBars → Main refresh. IMPLEMENTED.
Main → Loop: tap → nativeSequenceSetLoopEnabled → Main refresh. IMPLEMENTED.
Main → Track: Track field or hardware TRACK_SELECTION_CONTEXT → Track Select → nativeSequenceSelectTrack → Main; Track focus retained. IMPLEMENTED.
Main → Program: Program field → Drum-only Program Select → nativeSequenceSetTrackProgram(selectedTrack, program) → Main. Non-Drum explicitly unavailable. PARTIAL / truthful.
Main → Track Type: focus-only semantic context; no mutation command exists. Hardware reports TRACK TYPE reserved. RESERVED, deliberately no unrelated page.
Main → Layer: selected sample layer context through existing Track Edit semantics. PARTIAL.
Main → waveform: waveform double-tap → Track Edit/Samples workflow with selected Track/Pad/Layer preserved. PARTIAL.
Main → Browse: BROWSE → Browser → file import primitive → sample state. PARTIAL.
Main → Record: RECORD → Sampler → capture/assign → Sample Edit/Main. PARTIAL.
Main → Arrangement: Track/Arrangement toggle → shared selected Track → arrangement preview; double-tap event/overview → Grid for supported Drum tracks. PARTIAL.
Main → TRANSPOSE: visible explicit unavailable value; no false setter.

## 6. Menu routing matrix

Available entries currently route to Main, Track View, Browser, Grid, Step, Track Edit, Sample Edit, Sampler, Pad Mixer, Next Sequence and Arrange.
Shell routes: Preferences → Audio Settings subset; MIDI / Control → MIDI page.
Reserved entries: Channel Mixer, 16 Levels, Pad Perform, List Edit, Project and taxonomy-only Sounds/XYFX/Looper.
Acceptance: a reserved Menu tile may report RESERVED/UNAVAILABLE, but must not open an unrelated working screen.

## 7. Browser routing matrix

Canonical route: Browser → Places / Content / Expansions → file list → audition/load.
Current route: Browser → section/place → search → six filters → target Pad/Layer → Function Bar LOAD → Android storage picker → existing sample import/assignment → state refresh.
Current gaps: real semantic file list; Sample Assign; persistent favourite locations 1–5; Browser Options; icon-based filters; Auto/Sync/Warp audition state.
Status: PARTIAL.

## 8. Mixer routing matrix

XL Strip: left edge next to five shortcuts; show/hide through shell/state. PARTIAL.
Pad Mixer: selected pad → Level/Pan/Tune focus → nativeAudioSetPadLevel/Pan/Tuning → refresh. FUNCTIONAL for implemented controls.
Channel Mixer: multi-channel/returns/outputs/sends/inserts not backed by current semantic API. RESERVED.
Never fabricate meters, routing, FX slots or levels.

## 9. Track View / Arrange / Grid / Step

Track View: selection, REC ARM, Track +/- and Mute are real; Solo and several mixer/I/O fields are reserved. PARTIAL.
Arrange: lanes/events, Track select, loop brace, zoom, MOVE ± GRID, DUP, selected-event COPY/PASTE, CUT and an explicit empty-timeline insertion cursor are native-model/edit-history-backed and Android Emulator-verified. CUT/COPY/PASTE/DUP use the canonical Function Bar. PASTE targets the snapped empty cursor when one is selected; the legacy selected-event path still targets one quantize-grid step later. Explicit linear clip placements remain absent, so Arrangement still projects the first Pattern of each Track from bar 1. Double-tap event/overview opens Grid. PARTIAL.
Grid: Drum renderer plus Draw/Erase/Select/Magnify and existing event operations; non-Drum explicitly unavailable. PARTIAL.
Step: physical-pad 16-step context, step selection and Velocity/Probability/Ratchet/Nudge/Duration state via native Step Edit bridge. PARTIAL.

## 10. Track Edit / Sample Edit / Sampler

Track Edit real subset: selected pad/layer, gain, tune, pan, velocity ranges, region Start/End, pad Level/Pan/Tune, amp envelope and filter cutoff.
Track Edit reserved: LFO, Modulation, Effects, atomic Edit All Layers.
Sample Edit real subset: waveform, Start/End, Full, Crop, Chop 4/8/16, Play/Audition, Zoom, layer state.
Sampler real subset: Record, Stop, Assign, Monitor On/Off, Threshold, waveform and telemetry.

## 11. Performance/system matrix

| Function | MPC3 | Our implementation |
|---|---|---|
| 16 Levels | dedicated performance context | RESERVED |
| Pad Perform | performance mapping | RESERVED |
| Track Mute | live mute | PARTIAL / real Main + Track View |
| Next Sequence | live launcher | PARTIAL |
| Q-Link | macro control | RESERVED |
| XY | XY performance | RESERVED |
| Looper | dedicated loop context | RESERVED |
| Project/New/Save | project lifecycle | RESERVED |
| Preferences | system/project settings | PARTIAL subset |
| MIDI/Control | controller settings | PARTIAL |
| Timing Correct | quantize/TC | IMPLEMENTED |
| Metronome | Metro/Count-In/Rate/Sound/Routing | RESERVED GAP |
| Automation | automation workflow | RESERVED GAP |

## 12. Function routing matrix

| Source | Expected | Current route | Backend/state | Status |
|---|---|---|---|---|
| Toolbar Menu | Menu | showMenuPage() | UI mode | AVAILABLE |
| Toolbar Browser | Browser | showBrowserPage() | Browser state | PARTIAL |
| Timing Correct | settings | dialog + native setters | sequence | AVAILABLE |
| Metro | settings/toggle | disabled | none | GAP |
| Automation | automation | disabled | none | GAP |
| MIDI IN/OUT | status/context | showMidiPage() | MIDI state | PARTIAL |
| Main Sequence | select sequence | showSequenceSelectPage() | nativeSequenceSelect | AVAILABLE |
| Main BPM | edit tempo | handleHardwareDialDelta/changeSequenceTempo | native sequence | AVAILABLE |
| Main BARS | edit bars | changeSequenceBars | native sequence | AVAILABLE |
| Main START/END | loop bounds | nativeSequenceSetLoopBars | sequence loop | AVAILABLE |
| Main LOOP | toggle | nativeSequenceSetLoopEnabled | sequence loop | AVAILABLE |
| Main Track | select | Track Select/nativeSequenceSelectTrack | selected track | AVAILABLE |
| Main Program | select Drum Program | Program Select/nativeSequenceSetTrackProgram | Track-owned program | AVAILABLE for Drum |
| Main Track Type | select type | focus/status only | no setter | RESERVED |
| Main Layer | layer focus | existing layer state | selected layer | PARTIAL |
| Main waveform | edit | Track Edit/Sample workflow | sample region | PARTIAL |
| Main BROWSE | sample browser | showBrowserPage() | import/storage | PARTIAL |
| Main RECORD | sampler | showRecordPage() | recording | PARTIAL |
| New Track | create track | addSequenceTrack() | sequence/domain | AVAILABLE |
| Rec Arm | toggle | native selected-track arm | sequence | AVAILABLE |
| Track +/- | adjacent track | nativeSequenceSelectTrack | selected track | AVAILABLE |
| Mute | toggle track | nativeSequenceToggleTrackMute | track state | AVAILABLE |
| Solo | solo | none | none | RESERVED |
| Browser Search | query | MpcUiState.browserSearch | browser state | PARTIAL |
| Browser Filters | filter | MpcUiState.browserFilter | browser state | PARTIAL |
| Browser Audition | preview | selectAndTriggerPad | pad trigger | PARTIAL |
| Browser Load | load file | openWavPicker() | import/assignment | PARTIAL |
| Sample Assign | pool/pad browser | disabled | none | RESERVED |
| XL Strip Show/Hide | strip region | shell + compactMixerVisible | UI state | AVAILABLE |
| Pad Level | edit gain | nativeAudioSetPadLevel | pad state | AVAILABLE |
| Pad Pan | edit pan | nativeAudioSetPadPan | pad state | AVAILABLE |
| Pad Tune | edit tune | nativeAudioSetPadTuning | pad state | AVAILABLE |
| Track View Mute | mute | nativeSequenceToggleTrackMute | track state | AVAILABLE |
| Arrange Loop | set loop | nativeSequenceSetLoopBars | sequence state | AVAILABLE |
| Arrange CUT | remove selected Pattern event and retain full event in clipboard | nativeSequenceCutArrangementEvent + cutPatternNoteTick + native edit history | Pattern event + native clipboard | IMPLEMENTED / EMULATOR-VERIFIED; empty-target cursor workflow remains intact |
| Arrange COPY / PASTE / DUP | selected Pattern event editing | native PatternOps + edit history + Android actions | Pattern event and value-owned clipboard | IMPLEMENTED / EMULATOR-VERIFIED for COPY/PASTE/DUP; canonical Function Bar promotion and CUT remain open |
| Grid Draw/Erase | event edit | existing sequence ops | event state | PARTIAL |
| Step Params | event fields | native Step Edit | event state | PARTIAL |
| Sampler Record | capture | nativeAudioStartRecording | audio | AVAILABLE |
| Sampler Monitor | monitor | native audio | monitor | AVAILABLE |
| Sampler Threshold | threshold | native audio | sampler state | AVAILABLE |
| Sampler Assign | assign recording | nativeAudioAssignRecordingToPadLayer | layer | AVAILABLE |

## 13. Canonical navigation paths

Path A: Main → Track Select → selected Track → Main → Program Select for Drum → selected Program → Main.
Path B: Main → empty pad → Browse → Places/Content/Expansions → search/filter → file → LOAD → sample state → Main.
Path C: Main → waveform double-tap → Track Edit → Samples/layer → edit → Main/back with Track/Pad/Layer preserved.
Path D: Main → Arrangement → selected lane → double-tap event/overview → Grid → edit → back/Main.
Path E: Track View → selected Track → Mute → native mute → Track View refresh → same state visible in Main.
Path F: Menu → reserved tile → explicit reserved state → no unrelated screen.

## 14. Data Dial / +/-

Major focus targets: Sequence, Sequence BPM, Sequence Bars, Sequence Start/End, Track, Program, Track Type, Layer, Pad Mixer Level/Pan/Tune, Step/step parameters, Browser Item, Zoom H/V and Timeline.
Hardware route: semantic action → handleHardwareDialDelta → focus-specific command → native/domain mutation → refresh → feedback.
Current truthful rule: Track/Program/Sequence mutate real state; Track Type has no setter and remains reserved.

## 15. Back navigation

MpcNavigationController currently stores Mode history only. Main subcontexts intentionally reuse Mode.MAIN.
Known limitation: deep Back does not snapshot full subcontext/Data Dial focus. This is a P1 routing refinement, not a reason to rewrite navigation now.

## 16. Code audit snapshot

MpcModeRegistry: canonical vocabulary and explicit availability. Good.
MpcNavigationController: UI-only, five shortcuts, subcontext/focus state. Gap = Mode-only history.
MpcUiState: immutable Track/Program/Pad/Layer/Browser/focus/transport state. Gap = limited availability reason metadata.
MpcShell: shell composition is correct; 210dp host is XL Strip region. contextArea naming is legacy and should be renamed only after migration dependencies are removed.
MainActivity: large migration scaffold but real semantic routes exist; do not replace with another monolith.
MpcMainMixerStripView: real Pad Level/Pan/Tune; unsupported Track/Output controls are reserved.
MpcBrowserView: taxonomy/search/filter/target state exists; semantic result/options/favourites incomplete.
MpcPadMixerView: real Level/Pan/Tune with Data Dial focus.

## 17. Documentation reconciliation

Canonical: docs/MPC3-UI-MIGRATION-MASTER-SPEC.md
Supporting: docs/MPC3-UI-SCREEN-BLUEPRINT.md, docs/MPC3-REFERENCE-INDEX.md, docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md, this file.
Legacy: docs/MPC-STYLE-UI-ROADMAP.md.
Retired: permanent 4x4 Android Main pad grid; seven-page permanent rail; diagnostic dashboard Main composition.
Current visible Main: Toolbar → five shortcuts → XL Strip region → central Sequence/Track workspace → Function Bar.

## 18. Visual references and legal boundary

Do not store Akai screenshots, firmware resources, proprietary artwork or extracted UI assets in the app.
Store source URL, manual section/page, observed geometry/relationship, and original annotated reconstructions.

**Visual screenshot index (expanded 2026-10-29):** use [MPC3-UI-VISUAL-REFERENCE-CATALOG.md](MPC3-UI-VISUAL-REFERENCE-CATALOG.md) as the picture-led companion to this knowledge base. The catalog currently contains 51 source-linked, captioned inline screenshot references plus an 82-context coverage table with manual/source links. It includes recovered official MPC 3.6/3.7 PDF figures for Grid View, 16 Levels, List Edit, Looper and LFO, plus official MPC 3 images for Sequence Edit/Erase/Clear All, Hardware Preferences, Project Load/Save/New Project selectors and Main Track/Program fields. Some legacy PDF page anchors still require page-by-page visual validation. It now includes recovered visual references for Grid View, 16 Levels, List Edit, Looper and Track Edit LFO from official MPC 3.6/3.7 PDFs, alongside current official support images. Every row identifies the visual source and distinguishes current MPC 3.x / official images from legacy MPC2 and sibling-device references. The catalog makes unresolved exact-version image gaps explicit; an older screenshot is a fallback visual reference, not proof of exact MPC 3.9 appearance. Screenshot coverage also does not prove our Android screen is implemented or accepted.
Do not use screenshot references as proof of implementation. The required acceptance chain remains: relevant reference → implementation status → real interaction/state verification → exact-SHA CI → inspected screenshot/evidence.

| Reference | Source | Anchor | Extract |
|---|---|---|---|
| Main | S1 | p15 | Toolbar/Sequence/Shortcuts/Track-Arrangement/Mixer/Function regions |
| Browser | S1/S3 | p20 | Places/Content/Expansions, filters, audition, load |
| Arranger | S1 | p21 | lanes/timeline/loop |
| Step | S1 | p29 | 16-step workflow |
| Sampling | S1 | p34 | record/assignment |
| Sample Edit | S1 | p36 | waveform/region |
| XY | S1 | p38 | performance/automation context |
| Menu | S1 | p56 | launcher |
| Toolbar | S1 | p57 | shell |
| Project | S1 | p60 | lifecycle |
| Preferences | S1 | p61 | settings |
| XL Strip | S4 | article | left strip/show-hide/selector |
| Metronome | S5 | article | Metro/Pull-Down/settings |

## 19. Known gaps / priority

P0: verify Track→Program playback authority; add semantic route/back history when covered by tests; build Browser semantic result provider.
P1: Metronome backend/state/hardware; full Channel Mixer; Track Type mutation; fuller Program workflows for Keygroup/Plugin/MIDI/Clip/CV; Track View Solo; explicit linear Pattern/clip placement model and persistence; contextual strip selector; Browser Sample Assign/favourites/options/audition state; 16 Levels; Pad Perform.
P2: List Edit; Project New/Save/Save As; Q-Link/XY; Automation; full Preferences; complete routing/FX/returns/submixes.

## 20. Test/acceptance

Every meaningful change: source-level preflight → relevant unit/native tests → Android Build → emulator startup → accessibility hierarchy → interaction → screenshot evidence → Actions inspection.
Never claim build success from source inspection. Never claim physical MkII behavior without a real hardware test.
Current baseline at research start: Android Build #2482 SUCCESS on 10d1f6b.
Physical MkII validation at current HEAD: NOT CLAIMED.

## 21. Research log — 2026-10-06

R-01 shell: S2/S4. Five shortcuts + XL strips at left. Decision: old visible context dashboard is retired. Confidence HIGH.
R-02 Browser: S3. Browser has Sample Assign, Places/Content/Expansions, six filters, five favourites, audition settings and Browser Options. Decision: current app PARTIAL. Confidence HIGH.
R-03 Metronome: S5. Metro is a real MPC3 feature. Decision: current disabled control is a backend gap. Confidence HIGH.
R-04 Track/Program: S2. One-to-One Track Workflow and unified Track container are architectural. Decision: selected Track is authoritative. Confidence HIGH.
R-05 CI: current HEAD 10d1f6b. Android Build #2482 success. Decision: use #2482 as current baseline, not historical runs.
## 22. Screen / context contract

The following contract is the implementation checklist for every significant MPC3 context. It separates documented MPC behavior from our current backend capability.

| Context | Canonical purpose | Visible regions / controls | Entry | Exit | Data Dial / +/- | Touch | Hardware-equivalent | State / editable data | Destructive actions | Backend dependency | Current status | Confidence | Test |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Main | operational hub | Toolbar; five Shortcuts; XL Strip; Sequence; Track/Arrangement; Function Bar | boot/Main/Menu/shortcut | contextual/back | Seq/Track/Program/BPM/Bars/loop/layer | field select, view toggle, waveform | Main/Track/Sequence selection, pads | sequence/track/program/pad/layer, tempo, bars, loop | none current subset | sequence + track/program + sample semantics | IMPLEMENTED/PARTIAL | HIGH | startup/focus/state/screenshot |
| Menu | OS launcher | 4x4 mode grid + Function Bar | Toolbar/Menu | Back/Main | mode selection | tap tile | Menu control | selected mode/reserved | none | navigation catalog | IMPLEMENTED/PARTIAL | HIGH | every tile |
| Browser | content discovery/load | Places, Content, Expansions, search, filters, results, target | shortcut/Toolbar/Main | Back/context return | Browser item | tap/select/search | Browser/MkII navigation | location/filter/search/target | load/assignment | semantic storage + import | PARTIAL | HIGH | search/filter/load/audition |
| Track View | sequence track overview | track header + track strips + Function Bar | Menu/shortcut/Main | Main/Back | Track | row selection | Track Select/Track +/- | selected Track, arm/mute | none current | sequence track state | PARTIAL | HIGH | select/arm/mute |
| Arrange | linear arrangement | lanes, timeline, playhead, loop brace, zoom, event editing actions | Main/Menu | Grid/Track/Main | Timeline/zoom | loop/track/event gestures | zoom/navigation | Pattern events, clipboard, loop, focus | CUT still pending; no explicit linear clip placements | Pattern event model + native edit history | PARTIAL | HIGH | MOVE/DUP/COPY/PASTE and loop/drill-down |
| Grid | detailed event editor | time grid, pad/piano-roll, playhead, tools, zoom | Arrange/Menu/shortcut | Main/Track/Step | cursor/zoom/tool | draw/erase/select/pinch | Grid navigation/pads | events, selected pad/time, zoom | erase/edit event | sequencer event model | PARTIAL | HIGH | Drum grid/state |
| Step | fast 16-step editor | 16 steps, event info, parameters | Menu/Grid/shortcut | Grid/Main/Back | Step/event params | step select/edit | physical pads | velocity/probability/ratchet/nudge/duration | erase step | step-edit API | PARTIAL | HIGH | 16-step/dial |
| Track Edit | selected track/pad/layer editor | TRACK/PAD context, workspace, tabs | Main waveform/pencil/Menu | Main/Back | layer/parameter | waveform/fields | pads/Data Dial/+/- | layers, gain/tune/pan, velocity, amp/filter | region/crop where invoked | audio/sample control-thread APIs | PARTIAL | HIGH | layer preservation |
| Sample Edit | waveform/process | sample identity, waveform, S/E, process controls, tabs | Main waveform/Browser/Sampler | Main/Track | layer/start/end | region/zoom | Data Dial/+/- where mapped | sample region/layer | crop/chop | audio/sample region semantics | PARTIAL | HIGH | region/crop/chop |
| Sampler | record/monitor/assign | waveform, Record/Stop, Monitor, Threshold, Assign | Main Record/Menu | Sample/Main | contextual | record controls | Record/transport | recording state, threshold, destination | recording/capture | recorder/native sampler | PARTIAL | HIGH | record→assign |
| Channel Mixer | full track/return/output mixing | strips, level/pan, sends, inserts, I/O, returns | Menu/shortcut/XL | previous/back | mixer field | direct mixer controls | mixer navigation | routing/mixer params | routing changes only when semantic | mixer backend | RESERVED | HIGH | reserved honesty |
| Pad Mixer | pad channel mixer | level/pan/tune, routing/FX if supported | Menu/shortcut | Main/Back | level/pan/tune | direct control | Data Dial | pad audio params | none current subset | pad audio APIs | PARTIAL | HIGH | native setter parity |
| 16 Levels | fixed velocity performance | 16 pad mapping + source pad | Menu/shortcut | Main/Back | performance/source | pad performance | 16 pads | velocity mapping | none | performance model | RESERVED | MEDIUM | reserved |
| Pad Perform | alternate pad interpretation | notes/chords/scales/other map | Menu/shortcut | Main/Back | note/scale/performance | pad mapping | pads | pad interpretation | none | performance model | RESERVED | HIGH | reserved |
| Next Sequence | live sequence launcher | slots, current/queued, pad bank | Menu/shortcut | Main/Back | sequence/launcher | pad launch | pads + Pad Bank | current/queued | launch state | launcher semantics | PARTIAL | HIGH | bank/launch |
| List Edit | event list editor | event rows/fields | Menu | Main/Back | event field | select/edit | Data Dial | event values | delete/edit | list-editor backend | RESERVED | MEDIUM | reserved |
| Project | project lifecycle | New/Open/Save/Save As | Menu/Toolbar | Main/back | project item | file/actions | project/menu | project identity/persistence | save/replace | native versioned project codec | PARTIAL (no UI route) | HIGH | no fake save; connect codec and verify actual reopen |
| Preferences | system/project settings | settings tabs | Menu | back | setting item | selectors/tabs | settings controls | device/audio/MIDI/sequencer defaults | settings apply | platform/device prefs | PARTIAL | HIGH | subset+reserved |
| MIDI / Control | MIDI/controller setup | routing/control configuration | Toolbar I/O/Menu | back | contextual | selectors | MIDI controls | ports/routes/learn | none current subset | Android MIDI + semantic controller | PARTIAL | HIGH | connection/route |
| Looper | loop performance | loop transport/content | Menu/taxonomy | Main/back | performance | pad/touch | performance controls | loop state | clear/replace later | looper backend | RESERVED | LOW | reserved |
| XYFX / XY | XY performance/control | XY surface/parameters | Menu/taxonomy | Main/back | XY macro | touch XY | touch strip/controller | effect macro state | effect params | FX/automation backend | RESERVED | MEDIUM | reserved |
| Sounds | sound/expansion audition | categories/previews | Menu/shortcut | Browser/Main | Browser item | browse/audition | pads/navigation | sound/preset identity | load/apply | content provider | RESERVED | MEDIUM | reserved |

### Per-context route invariants

1. Every entry path converges on one semantic mode/context. Touch and hardware never maintain parallel musical state.
2. Every mutable field has a real setter or is visibly reserved.
3. Back preserves transport, sequence, track, pad and layer state unless the documented workflow requires otherwise.
4. Data Dial focus is explicit state, not merely a toast/status message.
5. Hardware feedback is only marked verified after a physical MkII test.

## 23. Expanded function-routing matrix

Required route form: SOURCE CONTROL → EXPECTED MPC BEHAVIOR → CURRENT ROUTE → REQUIRED ROUTE → BACKEND COMMAND → STATE CHANGE → SCREEN CHANGE → HARDWARE FEEDBACK → STATUS.

| Source control | Expected MPC behavior | Current route | Required route | Backend command | State change | Screen change | Hardware feedback | Status |
|---|---|---|---|---|---|---|---|---|
| Toolbar Menu | open Menu | showMenuPage() | same | navigation MENU | mode | Menu | mode indication | AVAILABLE |
| Toolbar Browser | open Browser | showBrowserPage() | same | Browser state | browser context | Browser | browse context | PARTIAL |
| Timing Correct | edit quantize/TC | dialog + native setters | same | quantize/swing/TC | TC state | dialog/Main | TC indication | AVAILABLE |
| Metro | open metronome settings | disabled | Metro settings + Pull-Down | metronome backend | metro/count-in/rate/etc. | settings | Metro state | GAP |
| Automation | expose automation | disabled | contextual automation | automation backend | automation state | contextual view | automation indication | GAP |
| MIDI IN/OUT | MIDI context | showMidiPage() | same + semantic routing | Android MIDI/control | ports/routes | MIDI | MIDI state | PARTIAL |
| Shortcut slot | rapid context access | navigateToMode() | same; reserved explicit | mode state | mode | target | mode state | AVAILABLE/RESERVED |
| Main Sequence | choose sequence | showSequenceSelectPage() | same | nativeSequenceSelect | selected sequence | Main | selection | AVAILABLE |
| Main BPM | edit tempo | dial focus | same | native tempo setter | BPM | Main refresh | dial state | AVAILABLE |
| Main BARS | edit length | dial focus | same | native bars setter | bars | Main refresh | dial state | AVAILABLE |
| Main START/END | edit loop bounds | dial focus | same | native loop setter | loop | Main refresh | dial state | AVAILABLE |
| Main LOOP | toggle loop | tap | same | native loop enable | loop state | Main | loop LED | AVAILABLE |
| Main Track | select Track | Track Select | same | nativeSequenceSelectTrack | selected Track | Main | selection | AVAILABLE |
| Main Program | select Track-owned Drum Program | Program Select | same for supported Track Types | nativeSequenceSetTrackProgram | program/container | Main | selection | PARTIAL |
| Main Track Type | choose type | focus/status only | real type selector after semantic setter | track type command needed | type | Main | type state | RESERVED |
| Main Layer | select layer | focus/layer state | same | layer state APIs | selected layer | Main/Track Edit | layer indication | PARTIAL |
| Main waveform | open deeper editor | Track Edit | same | sample-region semantics | editor context | Track Edit | mode | PARTIAL |
| Main Browse | load sample | Browser → storage picker | Browser result selection → Load | sample import/assignment | sample/layer | Browser/Main | load state | PARTIAL |
| Main Record | record sample | Sampler | same | nativeAudioStartRecording | recording | Sampler | Record state | PARTIAL |
| Function New Track | create Track | addSequenceTrack() | same | sequence command | track list | Main/Track View | track state | AVAILABLE |
| Function Rec Arm | toggle arm | native selected arm | same | native arm | rec-arm | shell refresh | Rec LED | AVAILABLE |
| Function Track +/- | adjacent Track | native select | same | nativeSequenceSelectTrack | selected Track | Main/Track View | selection | AVAILABLE |
| Function Mute | toggle selected Track | native mute | same | nativeSequenceToggleTrackMute | mute | refresh | Mute LED | AVAILABLE |
| Function Solo | solo Track | disabled/reserved | semantic solo only when backend exists | none now | none | none | no false feedback | RESERVED |
| Browser Places | location navigation | setBrowser() | semantic Place provider | storage query | location | results | focus | PARTIAL |
| Browser Content | content category | setBrowser() | semantic content provider | browser query | category | results | focus | PARTIAL |
| Browser Expansions | expansion content | setBrowser() | semantic expansion provider | content query | location/category | results | focus | PARTIAL |
| Browser Search | filter results | browserSearch state | same + result provider | browser query | query | results | browser focus | PARTIAL |
| Browser Filters | filter file types | browserFilter state | same + typed result model | browser query | filter | results | browser focus | PARTIAL |
| Browser Audition | preview sample | selectAndTriggerPad | selected Browser item preview | audition backend later | audition | Browser | play state | PARTIAL |
| Browser LOAD | load selected file | openWavPicker() | semantic Browser selection → Load | sample import | sample/container | Main/Sample | load indication | PARTIAL |
| Sample Assign | project sample pool/pad assignment | disabled | semantic pool/pad assignment | sample-pool backend | pool/target | Browser/Sample Assign | pad assignment | RESERVED |
| XL Strip show/hide | show/hide strips | compactMixerVisible | same | UI state | visible | shell | strip indication | AVAILABLE |
| XL Strip selector | change track/pad context | existing selector | semantic channel context | selector + resolved channel | strip target | shell | strip indication | PARTIAL |
| Pad Mixer Level | edit gain | native setter | same | nativeAudioSetPadLevel | level | Pad Mixer/Main | dial/strip | AVAILABLE |
| Pad Mixer Pan | edit pan | native setter | same | nativeAudioSetPadPan | pan | Pad Mixer/Main | dial/strip | AVAILABLE |
| Pad Mixer Tune | edit tune | native setter | same | nativeAudioSetPadTuning | tune | Pad Mixer/Main | dial/strip | AVAILABLE |
| Track View select | select Track | row listener | same | nativeSequenceSelectTrack | selected Track | Track View | Track state | AVAILABLE |
| Track View Rec Arm | arm Track | Function Bar | same | native arm | rec-arm | Track View | Rec LED | AVAILABLE |
| Track View Mute | mute Track | native mute | same | native mute | mute | Track View | Mute LED | AVAILABLE |
| Track View Solo | solo Track | reserved | semantic solo only when backend exists | none now | none | none | none | RESERVED |
| Arrange loop brace | edit loop region | onLoopCommitted | same | nativeSequenceSetLoopBars | loop range | Arrange | loop state | AVAILABLE |
| Arrange CUT | remove selected Pattern event | nativeSequenceCutArrangementEvent + history-backed PatternOps | native Pattern + clipboard | selected event/clipboard state | Arrangement | canonical Function Bar | event removal + undo/redo | IMPLEMENTED / EMULATOR-VERIFIED |
| Arrange COPY/PASTE/DUP | selected Pattern-note operations | native PatternOps + native clipboard + edit history | canonical Function Bar actions | event/clipboard/cursor state | Pattern events | Arrange | event selection or empty-target cursor | IMPLEMENTED / EMULATOR-VERIFIED; explicit target cursor is grid-snapped and Pattern-bounded |
| Arrange double-tap event | open Grid | showSequenceGridPage() | same + preserve event focus | grid/event state | event selection | Grid | Grid state | PARTIAL |
| Grid Draw | add/modify event | grid listener | same | sequence event API | event | Grid | pad/event | PARTIAL |
| Grid Erase | remove event | grid listener | same | erase API | event removed | Grid | pad/event | PARTIAL |
| Grid Select | select pad/event/time | grid listener | same | semantic selection | focus | Grid | pad state | AVAILABLE/PARTIAL |
| Grid Magnify | zoom/navigation | view policy | same | view-only zoom | zoom | Grid | zoom state | PARTIAL |
| Step parameter | edit event | handleHardwareDialDelta | same | native Step Edit | event field | Step | dial state | PARTIAL |
| Track Edit layer gain | edit gain | listener/native audio | same | native layer gain | gain | Track Edit | dial | AVAILABLE |
| Track Edit layer tune | edit tune | listener/native audio | same | native layer tuning | tune | Track Edit | dial | AVAILABLE |
| Track Edit layer pan | edit pan | listener/native audio | same | native layer pan | pan | Track Edit | dial | AVAILABLE |
| Track Edit velocity range | edit velocity range | listener/native audio | same | native velocity min/max | layer range | Track Edit | dial | AVAILABLE |
| Track Edit amp envelope | edit envelope | listener/native audio | same | native envelope | envelope | Track Edit | dial | AVAILABLE |
| Track Edit filter | edit cutoff | listener/native audio | same | native filter cutoff | cutoff | Track Edit | dial | AVAILABLE |
| Sample FULL | restore full region | resetRegion() | same | sample-region command | region | Sample Edit | feedback | AVAILABLE |
| Sample CROP | crop region | cropRegion() | same | crop command | sample data | Sample Edit | operation | AVAILABLE |
| Sample CHOP | split region | chop() | same | chop command | pad/sample assignment | Sample Edit | pad state | AVAILABLE |
| Sampler RECORD | capture input | nativeAudioStartRecording | same | recorder | recording | Sampler | Record LED | AVAILABLE |
| Sampler STOP | stop capture | nativeAudioStopRecording | same | recorder | stopped | Sampler | transport | AVAILABLE |
| Sampler MONITOR | monitor input | native monitor | same | monitor backend | monitor | Sampler | monitor state | AVAILABLE |
| Sampler THRESHOLD | set threshold | native threshold | same | native threshold | threshold | Sampler | dial/state | AVAILABLE |
| Sampler ASSIGN | assign recording | nativeAudioAssignRecordingToPadLayer | same | native assign | layer sample | Sample/Main | pad/sample | AVAILABLE |
| Next Sequence pad | queue/launch sequence | launcher view | same + full quantization state later | launcher backend | current/queued | launcher | pad LEDs | PARTIAL |
| Menu reserved tile | report unsupported capability | explicit reserved | same | none | availability | reserved state | no false feedback | REQUIRED |
| Project New/Save | project lifecycle | no user route yet | native serializer first | project persistence | project files | Project/Main | project state | PARTIAL (codec only; UI route absent) |

## 24. Versioned behavior register

| Area | 3.4-era evidence | 3.7-era evidence | 3.9 canonical evidence | Repository rule |
|---|---|---|---|---|
| Main | older Main assumptions | supporting only | Main Mode overview with Toolbar/Sequence/Shortcuts/Track-Arrangement/Mixer/Function Buttons | 3.9 wins |
| Track/program | older shared-program model | transitional import | unified Track/Program container | selected Track/container authoritative |
| Browser | legacy browser | supporting | current Browser taxonomy incl. Sample Assign, favourites, filters, audition | picker underneath semantic Browser |
| Time Signature | legacy sequence assumptions | transitional | current 3.9 time-signature fields | never copy MPC2 behavior unverified |
| Track Edit | program-centric legacy | supporting layer evidence | current 3.9 Track Edit | label supporting version |
| Arrange | not equivalent | supporting | 3.9 Linear Arranger | Standalone evidence wins |
| Desktop | MPC2 desktop | Desktop beta may differ | not canonical | compatibility/controller evidence only |

## 25. Research provenance / confidence rule

For every new UI fact record: Source, Section/page, Checked, Confidence, Repository decision, Test.

The official v3.9 manual explicitly presents Main as Toolbar + Sequence + Shortcuts + Track/Arrangement Views + Mixer Strips + Function Buttons. It also describes double-tapping the Main waveform to open Track Edit and double-tapping Arrangement to open Grid View.

Checked: 2026-10-06.
Confidence: HIGH.

## 26. Research log — factory shortcut truthfulness

R-06 — 2026-10-06.
Finding: current MPC shell factory shortcut identity is Browser / Channel Mixer / Pad Mixer / Sounds / XY. Our backend does not yet implement Channel Mixer, Sounds or XYFX, so those three must remain visible as reserved destinations rather than being silently replaced by Track View/Grid/Step.
Implementation correction: aligned `MpcModeRegistryTest` with this contract and corrected the registry comment so availability is independent from factory identity.
Result: no audio/MIDI/domain rewrite; the semantic route for reserved shortcuts remains explicit RESERVED/UNAVAILABLE.
Confidence: HIGH for the repository contract; exact factory ordering is retained from the project brief and documented as an implementation target until independently captured from the v3.9 manual image.
Checked: 2026-10-06.


## 27. Functional routing log — Pull-Down time/tempo

R-07 — 2026-10-06.
Official v3.9 behavior: the Pull-Down is available from MPC3 modes; its time counter opens Locate and its BPM field opens Tempo. The panel has two pages. citeturn495906search2
Finding in project: Pull-Down originally rendered project/sequence/tempo as read-only fields and Metro as reserved.
Correction: Time and Tempo are now actionable through semantic listeners. Time uses existing `nativeSequenceLocateMoveTicks` so absolute BAR:BEAT:TICK input preserves playing state; Tempo uses existing `nativeSequenceSetTempo`. No realtime engine changes.
Test contract: smoke now asserts the Pull-Down listener/action methods and native commands exist.
Status: IMPLEMENTED for Time/Tempo; Metro remains a documented backend gap.
Confidence: HIGH.
Checked: 2026-10-06.


## 2026-10-06 Browser taxonomy and navigation checkpoint

Branch: `feature/mpc-one-ui-fidelity`

The Browser presentation was corrected to keep two different MPC concepts distinct:

- top-level Browser sections remain **Places / Content / Expansions**;
- file-type filters remain **Projects / Patterns / Kits / Plugin Presets / Samples / All**;
- selecting a Browser section now rebuilds its side navigation rather than leaving a stale Places list visible;
- Places exposes Internal, MPC Documents, Connected Storage and five Favorite slots;
- Content exposes Drums, Instruments, Samples, Demos, My Files and Splice;
- the selected Browser node is stored through the existing UI-state `browserLocation` string using a section/item key;
- Android Document Provider remains the storage primitive underneath the product Browser.

This is a bounded presentation/state-routing slice. It does not claim a full semantic file-result provider, Browser Options, audition Auto/Sync/Warp state, or real Sample Assign backend. Those remain explicit Browser gaps.


## 2026-10-06 Browser hierarchy navigation checkpoint

The Browser Function Bar `UP` action now follows the semantic Browser location hierarchy rather than rebuilding the Browser at its current root:

- `PLACES/MPC DOCUMENTS` → `PLACES`;
- `CONTENT/DRUMS` → `CONTENT`;
- `EXPANSIONS/USER CONTENT` → `EXPANSIONS`;
- a top-level Browser section returns to `PLACES`;
- `UP` is disabled at the Browser root.

This keeps navigation state in `MpcUiState.browserLocation` and leaves Android Document Provider as the underlying storage primitive.


## 2026-10-07 Preferences taxonomy checkpoint

Preferences now has an in-shell three-category taxonomy: **Audio / MIDI-Sync / Sequencer**.
Audio retains the real Android/Oboe settings implementation. MIDI-Sync and Sequencer are explicit RESERVED/UNAVAILABLE workspaces; no fabricated settings or setters were added. Runtime audit coverage now enters Preferences, switches into a reserved category, returns to Audio, and exits.


## 2026-10-08 Browser Options checkpoint

The Browser now exposes the MPC Browser Options gear and the documented six-option taxonomy. Because the semantic file-result provider is still partial, these options are intentionally visible but disabled rather than pretending to change provider behavior.

The icon-only Browser filters also now propagate selected state into their drawable, restoring truthful visual focus on the active filter.


## 2026-10-08 Clean visual-evidence checkpoint

UI-audit mode no longer renders hardware/status diagnostics over the application surface. Runtime audit assertions remain in logcat, while the captured startup image is now a clean Main surface suitable for visual comparison with MPC.


## 2026-10-09 Runtime smoke timeout hardening

The Android emulator was reaching the boot-completed state, after which an unbounded adb command in `tests/android_ui_smoke.sh` could hold the job indefinitely. The smoke script now bounds adb install/start/screenshot/logcat/process-check/UI-dump calls individually, uses a wall-clock deadline for UI-audit polling, and the emulator script plus whole job have hard time limits. This is test-infrastructure hardening only; it does not alter audio, MIDI, or product runtime code.


## 2026-10-09 Emulator timeout process-tree correction

The first hard-timeout pass still left the smoke step running beyond its 15-minute guard because GNU `timeout --foreground` does not manage the command's process group. The guard now uses normal process-group timeout behavior, including for ADB calls and the enclosing smoke command, so stuck children can be terminated instead of keeping the runner alive.


## 2026-10-09 Browser preflight and CI de-duplication

The Android smoke had stalled during one compound Browser grep preflight before reaching ADB. That check is now split into named, individually bounded source assertions, so the log identifies the exact contract on failure. Feature branches with an open PR now use the pull-request workflow rather than starting a duplicate push workflow; emulator boot has a ten-minute ceiling while the full job remains capped.


## Product-specific scope override — local-first build (2026-10-10)

The tables above describe published MPC Standalone OS 3.9 reference taxonomy. That taxonomy is not a mandate to reproduce the commercial content ecosystem in this project.

**Intentional product deviation:** Splice and other branded content subscriptions, online stores/marketplaces, paid expansion promotion, partner ads, upsell panels, and nonessential vendor-specific cloud/account integrations are excluded. In particular, do not implement a visible Splice Browser route just because the reference Browser taxonomy documents it.

Keep useful local workflows: Places, generic content categories backed by real local results, user files, local/user expansion folders, Android Document Provider as an underlying storage primitive, accessible external/USB storage, and real sample selection/loading. A generic category without a provider must not masquerade as a working list.

### Required Browser implementation status discipline

- Browser location navigation is presentation/state routing until actual results change with the location.
- Search and file-type filters are implemented only when they operate on a real semantic local result set.
- Favourites are implemented only when the selected URI/location is persisted and restored correctly.
- Audition is implemented only when selecting/previewing a real local result drives supported playback.
- Load/Assign is implemented only when the chosen content reaches the intended Pad/Layer and survives the expected context return.
- Android Document Picker integration alone is a storage-access primitive, not a complete MPC-style Browser.

Use `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` for the execution loop and `docs/SESSION-STATE.md` for the most recent verified checkpoint. Always verify those checkpoint values against live GitHub.

## 2026-10-11 Local WAV Browser + Search/Filter acceptance checkpoint

**Verified implementation SHA:** `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`  
**CI:** Android Build #2637 / run ID `38110061687`, full workflow SUCCESS (57/57 steps).  
**Run:** https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38110061687

Verified on the Android emulator:

- `MpcLocalAudioCatalog` queries actual MediaStore audio rows on explicit user request and only exposes WAV files supported by the existing native sampler loader. It does no work in the realtime audio callback.
- `MpcBrowserView` displays URI-backed file rows and size/duration metadata. Search and file-type filters operate on the actual local result set, with JVM policy tests and runtime smoke coverage.
- Runtime test covers matching Search → visible WAV, non-matching Search → `NO MATCHING WAV FILES`, Clear → row restored, PROJECTS → no WAV rows, ALL/SAMPLES → row restored.
- The selected local WAV is actually auditioned through Android `MediaPlayer`, then loaded/assigned through the existing native sampler operations to Pad 01 / Layer 01. UI accessibility state confirms `CURRENT SAMPLE • Browser-Smoke-Local.wav`, target `PAD 01 / LAYER 01`, and `ASSIGNED • Browser-Smoke-Local.wav → PAD 01 / LAYER 01`.
- No audio engine, realtime callback, MIDI transport, hardware decoder or SysEx subsystem was rewritten in this slice.
- Evidence artifact `mpc-ui-visual-evidence` ID `11690994455` was downloaded and inspected. It contains 12 PNG/XML evidence files, including `mpc-groovebox-browser-local-results.png` (1920×1080) and its `mpc-groovebox-browser-local-results-uiautomator.xml` hierarchy.

**APK:** artifact ID `11691089182`, 8,211,869 bytes; SHA-256 `4f94874ed5b82f739cc3826b1425f52b716281bdee216f517afe9525e3c852a9`.  
**Evidence ZIP:** artifact ID `11690994455`, 847,766 bytes; SHA-256 `efca6326eb57be868576c08ac7176f159fa413fbb173c12f593c0651acd54b07`.

Next acceptance slice:

1. Launch the Android Document Picker from Browser LOAD, select the actual WAV fixture, return to the app, and verify the target Pad/Layer and current sample state survive the round-trip.
2. Add `ACTION_OPEN_DOCUMENT_TREE` only as part of a real local folder-provider implementation. Test persistable URI access and restoration; implement favourites only when storage and restoration are genuine.
3. Continue to surface unsupported categories honestly. No Splice, online stores, upsell surfaces or unnecessary branded cloud/account dependencies.
4. Physical MPC Studio MkII verification remains pending. Emulator success is not evidence of a physical hardware test.



## 27. 2026-10-11 — MPC 3.9.1 Gen1 firmware static audit

Input identity: `MPC-3.9.1-Gen1-update.img`, SHA-256 `4c0f7797a006313fad206f9513eca8b3823e834c9e0941a6b5f088386cb835d6`, size 162,220,784 bytes. The AZ01 container declares an XZ `rootfs` payload at offset 340 bytes; its 162,220,428-byte payload SHA-1 matches the embedded digest. XZ validation passed and produced an ext4-format filesystem of 441,279,488 bytes.

Observed target app version is `3.9.1.2 (f3af35cdc95)`, ARM 32-bit EABI5 stripped ELF, Build ID `8371199f945ecc40f79cb19c17ff5de72810a91c`. A local read-only traversal found 11,529 entries with zero directory parsing errors. The bundled `ACVS` resource tree contains 759 PNGs and 13 JSON layout descriptors. Main layout-data type names, UI component names, Browser/Q-Link settings and track/channel vocabulary were identified in static strings/type-info. These are evidence anchors, not a recovered source tree.

**Interpretation limit:** `ACVS/layout` values are bundled panel geometry (e.g. toolbar height 55, status bar height 34, browser width 294, MainBody padding left 208/right 397). Help strings mention host/desktop-oriented concepts. These values are *not yet validated as pixel-perfect physical MPC One Main specifications* and must not be applied blindly. Use the official v3.9 guide and actual hardware screenshots as the behavioral/visual authority.

Full report: [`docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md`](research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md). Machine-readable summary: [`docs/research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json`](research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json).

**Public-repository boundary:** the firmware, raw filesystem, proprietary PNGs/content, complete private inventory and full decompilation are deliberately not checked in. The local audit did not execute the ARM binary, extract nested `/opt/ramfs-update.squashfs`, or perform Ghidra function-level decompilation. A promising next reverse-engineering anchor is `SerialisableTUIMainModeLayoutData` / `SerialisableTUIMainModeMixerLayoutData`, but this does not displace the next product task: Document Picker selection → return → sample target preservation.


## 28. 2026-10-11 — Second-pass firmware UI schema findings

### Main / Touch UI implementation anchors

The Gen1 3.9.1 image's stripped `MPC` ELF contains readable type-information/string anchors for `TouchMainPage`, `TouchMainPageNavigationPanel`, `TouchMainPageContentPanel`, track-specific Main panels (Audio, Bus, CV, Drum, Keygroup, MIDI, Plugin), `TouchMainModeMixerProperties` (`Display` / `PanelType`), `TouchMixerChannelStripMPC`, `TouchMultiMixerStrip`, `TouchDoubleChannelStripContainer`, `TouchModeShortcutPanel`, `QLinkMainMode` and `TransportBarCentreControls`. Property-name anchors include `tuiMainModeLayout` and `tuiMainModeMixerLayout`.

Confidence: HIGH that the names occur in the binary; MEDIUM that the names accurately summarize class responsibilities; not enough to establish exact geometry, routing, enum values or runtime transitions. This was strings/type-information inspection, not method-body decompilation.

### Program Edit page schema inventory

The extracted `Resources/Pages` tree contains 10 `TUI.json` files: four marked CompleteDescription (Drum, Keygroup, CV 8-channel, CV 16-channel) and six PartialDescription (MIDI, Legacy CV, four Plugin variants). These contain 66 tabs and 652 serialized component entries in total. Across local component definitions in page JSONs, 136 action descriptors were counted (Mouse Down 59, Enter Pressed 50, Double Click 27); handler labels include Show Overlay, Q-Link and Toggle Switch. These are schema counts, not observed user actions.

### High-priority Track Type vocabulary discrepancy

The firmware's `ACVS/graphics/track type selection` group contains six 125×125 selector assets: AUDIO, CV, DRUM, KEYGROUP, MIDI and PLUGIN. It has no `track-clip.png`. A separate `programtypeicons` group contains `strip-clip.png` as one of several Mixer/strip-category icons. The official v3.9 Standalone User Guide likewise identifies the six main Track Types as **Audio, Drum, Keygroup, Plugin, MIDI and CV**.

This conflicts with the current migration vocabulary that uses CLIP and omits AUDIO in its Track Type selector. **Action:** after the current Document Picker end-to-end workflow, audit the actual Track Type selector and tests and reconcile the visible type list against the official v3.9 guide and a real device reference. Do not silently equate clip/arrangement concepts with a selectable Track Type.

### Layout metadata boundary

`ACVS/layout/Help.json` has 51 help entries and includes mouse/hover/window vocabulary, while the same executable contains touch-specific Main classes. This supports a shared/multiple-interface resource interpretation and reinforces the boundary: panel JSON dimensions remain unverified as physical MPC One Main geometry. Do not apply them to Android without corroboration.

Full evidence: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md`; machine-readable summary: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json`.


## 29. 2026-10-13 — Local Browser end-to-end acceptance

Android Build #2701 completed SUCCESS on implementation SHA `f325676ecd147426952bfda4c1e160cc620489bd` (57/57 workflow steps). This closes the local Browser select/load path previously left open in Sections 27–28.

- MediaStore-backed WAV rows, positive/negative Search, Clear, real-result filtering, selected-file identity, genuine MediaPlayer audition and native sampler assignment passed runtime smoke.
- Android Document Picker selection → return → target Pad/Layer preservation and assigned-sample feedback passed.
- Android `ACTION_OPEN_DOCUMENT_TREE` persisted read access, real direct-child WAV rows, provider-specific filtering, and restoration of the saved USER FOLDER plus real file after app process stop/relaunch passed.
- The downloadable evidence archive for that SHA was inspected. It contains the Browser result screenshot/XML, Document Picker assignment screenshot/XML, and restored folder screenshot/XML; the restored hierarchy confirms `USER FOLDER • 1 WAV FILE INDEXED` and `TARGET • PAD 01 / LAYER 01`.
- No realtime audio callback, sampler engine, sequencer scheduler/clock, MIDI transport, raw MPC Studio MkII decoder or hardware SysEx layer was rewritten. Physical controller validation remains pending.

**Remaining Browser gaps:** favourite locations 1–5 are not yet stored/restored, and categories without genuine providers remain unavailable/no-result. Splice and commercial surfaces remain intentionally excluded.

**Next discrepancy to audit:** the Gen1 firmware Main Track Type selector assets and official v3.9 Guide indicate AUDIO / CV / DRUM / KEYGROUP / MIDI / PLUGIN. Native `TrackKind` reports AUDIO, but Android `MpcTrackTypeIconDrawable.Type` currently contains CLIP instead of AUDIO. Keep this distinct from Program Type/Clip-related strip vocabulary; make a small mapping/test correction without fabricating a Track Type conversion backend.


## 30. 2026-10-13 — Main Track Type vocabulary and truthful selection status

The Gen1 firmware evidence from Section 28 is now applied to the Main Track Type icon surface. The source update explicitly maps native `AUDIO` and `DRUM` labels, replaces the misleading CLIP icon with an original waveform-line AUDIO glyph, and keeps the reference vocabulary distinct from Program Type / Clip-Arrangement terminology.

- Main Track Type label set remains the six reference types Audio / CV / Drum / Keygroup / MIDI / Plugin. Native `TrackKind` currently contains Audio / Drum / Keygroup / Plugin / MIDI and has no CV kind; CV remains reference-only/unavailable for native track creation in this build.
- The Main header continues to show one icon for the currently selected type, not a six-icon cluster. Drum editing remains supported. Other non-Drum editing workflows and conversion of an existing Track's type remain explicitly RESERVED until semantic backend operations exist.
- The touch affordance and hardware `TRACK_TYPE_SELECTION_CONTEXT` now report `TRACK TYPE CHANGE • RESERVED • no existing-track conversion operation`; neither enters a fake Track Type Select state. No type conversion is emulated using unrelated `addTrack` operations.
- Smoke assertions verify the native AUDIO and DRUM mapping branches, keep CV visibly in the documented icon taxonomy, reject CLIP in Track Type mapping/icon vocabulary and inspect the actual Main accessibility description for an unavailable-conversion state.
- No realtime audio callback, sampler engine, sequencer scheduler/clock, MIDI transport, raw MkII decoder or SysEx protocol was changed. Build #2704 accepted the icon correction; Build #2705 passed the additional Grid-focus policy tests. Physical controller testing remains pending.


### Build #2703 compile-time regression and follow-up

The first implementation commit's Android Build #2703 failed during Java compilation because `MpcMainMixerStripView.trackTypeIcon()` still referenced `MpcTrackTypeIconDrawable.Type.CLIP` after the shared drawable enum was changed. No APK was produced by that run. This identifies a second production consumer, not a reason to restore CLIP to the Main Track Type vocabulary.

The follow-up correction maps native `AUDIO` and `DRUM` explicitly in the XL Mixer Strip icon helper and extends `tests/android_ui_smoke.sh` to require the correct Audio/Drum/CV mapping and reject the stale CLIP reference in that helper. The corrected slice was accepted by Android Build #2704 on SHA `8a9f5ecd2e7326d7b1f703c6aafc76a5b800bfcb` (57/57 steps); Build #2705 independently passed the additional Grid-focus math tests. No audio/MIDI/domain operations were touched.


## 31. 2026-10-14 — Track Type fix accepted; Arrangement-to-Grid navigation defect found

Android Build #2704 on SHA `8a9f5ecd2e7326d7b1f703c6aafc76a5b800bfcb` completed successfully (57/57 steps). Its APK and visual evidence were downloaded, ZIP-integrity checked and inspected. The Main hierarchy reports the active DRUM workflow and explicitly reports type conversion unavailable. Picker assignment and process-restarted USER FOLDER restoration evidence remain valid at that exact SHA.

A new code audit found that Arrangement event double-tap receives a real event tick but throws it away before opening Grid. The current implementation slice maps the supplied tick into the active quantize grid, selects a bounded absolute step and chooses a visible viewport that contains/centers that step where the sequence length permits. New unit tests cover negative/oversized ticks, sequence boundaries and narrow windows; source preflight verifies the live route. This source slice remains pending exact-SHA Android CI. No realtime audio, MIDI transport or sampler path is changed.


## 32. 2026-10-14 — Build #2705 passes; Grid event-focus evidence upgraded

Android Build #2705 on `be431ccef023466216ada643208b18dec98a6bc0` succeeded (57/57 steps). The APK and 21-file visual-evidence archive were downloaded, ZIP-integrity checked and inspected. Artifact IDs: APK `11867100675` (SHA-256 `5529f668063f05f78e8c40b78673d657d036f3eaf9da033589023b4e259abc82`), visual archive `11867460643` (SHA-256 `c12adc1dde7b43e76b01f6171d876b606011e64688f91ec0df00b8566362454c`). Browser assignment and SAF folder restoration remain verified; the Main hierarchy reports unavailable track conversion truthfully.

The event-tick/viewport math is now passed by the targeted JVM tests, and Main's Arrange listener routes into it. To prove the interaction visually rather than rely on source assertions alone, the next validation adds a runtime audit at a non-zero tick, asserts `SequenceGridView.firstStepForTest()`, pauses for a Grid screenshot/accessibility dump and checks the displayed step-range includes the focus target. This evidence-harness change is not accepted until exact-SHA CI and artifact inspection pass.


## 33. 2026-10-14 — Build #2706 visual proof and accessibility-label correction

Android Build #2706 on `b505b58a3e0dda61c3eb4dd41e49d682c4478ee1` built the APK and reached the new Arrangement→Grid screenshot point. The captured 1920×1080 image correctly displayed step range `25–40/64`, with target step 33 visible in the viewport. The run then failed because the XML assertion expected the app's status/LCD feedback to be its own accessibility node and assumed the visible label had a literal `STEPS` prefix; the actual UI hierarchy had neither.

The fix does not weaken the functional check. It assigns the actual range TextView a semantic content description `Grid visible step range: steps NN through NN of NN`, and the smoke validator requires the runtime target to lie inside both the visible text range and that accessibility range. It also checks that the two representations agree. The new commit requires a full Android Build and fresh screenshot/XML inspection before this evidence slice is accepted.


## 34. 2026-10-14 — Build #2707 accepts Grid focus runtime evidence

Android Build #2707 verified SHA `8c5bfcc114b5e2becfdf50a4cdf861bb0d57a623` and passed 57/57 steps. APK artifact `11867684347` has SHA-256 `38557fcdf0ac7b92d7a5b7ad0df9f8199dea67b59fbba44fd7955d04ab53e844`. Visual evidence artifact `11867864670` has SHA-256 `7dae7d4c1b054cfa76ace4ce6744b5fbb82232b89e09aeb6f52bf01a4708f6cd`, with 23 files including the Arrangement→Grid screenshot and XML. The inspected screen and hierarchy agree: target step 33, visible range 25–40/64, semantic content description `Grid visible step range: steps 25 through 40 of 64`.

The failed #2706 validator was a false negative: it assumed the status message was a standalone XML node and that the range label included a literal `STEPS` prefix. #2707 corrected the accessibility contract and passed the full emulator workflow. Direct double-tap on a populated Arrangement event remains separate untested interaction coverage; the UI audit calls the shared production route using a synthetic tick.


## 35. 2026-10-14 — Build #2709 validates Arrangement double-tap → Grid

Android Build #2709 verified SHA `20ac2f54ca0075256a6b6534f5ffa7e06d358b77` and passed 57/57 steps. APK artifact `11871064482` has SHA-256 `e653b9b42b4ec971fed383dbb9a1dd033ec0285f50cb94f7b8d6fc369624eb6a`. Visual archive `11871970352` has SHA-256 `53f5e282f46e97e16ea399a1813bb8913192136385f06bb86f4541dd2fc0478e` and contains 23 entries including the focused Grid screenshot/XML. The UI seeded a note with the existing sequencer mutation, checked the exact event center through the production hit test, dispatched the double-tap down/up events through `MpcArrangeView` and its actual `GestureDetector`, and asserted the listener delivered the exact expected Track/tick. The screenshot shows step 33 as an active note within range 25–40/64; semantic accessibility agrees.

This proves the app-side Android gesture/listener path under emulator MotionEvents. It does not claim finger testing on the MPC Studio MkII hardware. The Browser local WAV/SAF assignment and persisted folder restoration paths also passed unchanged.


## 37. 2026-10-15 — Build #2713 validates Arrange loop and single-lane contracts

Android Build #2713 verified exact SHA `cdcfdfb2500b4c6eaebeab5018b4cdce33213462` and passed 57/57 steps. APK artifact `11958217657` has SHA-256 `4ca32dee06e76b69314446ab5a12b9ec8bf9a488d84c5d7487f1a4861783f960`; evidence archive `11958527786` has SHA-256 `09fa5c463ee3f7449f4cf36907b89e38fae3ddafb057d4d1e3257bd99bbd10c5` and contains 23 files, including the Arrangement→Grid focus screenshot/XML and Browser, picker and restored USER FOLDER evidence.

Build #2712 failed because the new lane-selection audit assumed the clean fixture contained two Tracks. The fix validates the real single-lane tap, confirms native/View selection remain aligned and Arrangement data remains unchanged, and explicitly marks alternate-Track change unavailable in that fixture. No disposable Track was added and no Track-removal backend was invented. The multi-Track change/restore branch is present but is not claimed as covered by the one-Track fixture. The loop-end gesture changes the native loop then restores the original via the same handle drag.

**Next:** test actual Grid BACK → Arrange return preservation (selected Track, original event tick and pattern data), then re-enter Grid via the same event gesture and verify the same absolute viewport. The focused note must not be modified to make navigation pass.
