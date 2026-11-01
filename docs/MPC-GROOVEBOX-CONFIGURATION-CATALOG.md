# MPC Groovebox Configuration Catalog — UI Completeness Matrix

Status: living document. Updated at each Mission B milestone.
Source of truth for UI implementation status: current source code + exact-HEAD test evidence.
Authority order per docs/AGENTS.md: source code and exact-head test evidence > physical MPC observations > official MPC 3.9 manual > firmware research > screenshots > inference.

Status vocabulary:
- VERIFIED — named exact-HEAD CI/emulator evidence exercised the behavior
- IMPLEMENTED — REVERIFY — source indicates implementation, evidence older than current HEAD
- PARTIAL — surface exists, semantic gaps remain
- GAP — no real implementation
- RESERVED / UNAVAILABLE — intentionally truthful unavailable state
- REFERENCE ONLY — EXCLUDED — commercial/cloud feature, not in product scope
- UNKNOWN / NEEDS INVESTIGATION — not yet inspected

## UI Completeness Matrix

| Area | VIS ID(s) | Action ID(s) | Official source | Current implementation | Semantic/domain dependency | Status | Missing work | Test coverage | Visual evidence | Priority |
|------|-----------|--------------|-----------------|----------------------|---------------------------|--------|--------------|---------------|-----------------|----------|
| Shell / Toolbar | VIS-001 | Main toolbar fields | MPC 3.9 guide p.57 | Toolbar with project, time, Timing Correct, metro, automation, MIDI I/O, transport | Native transport state | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Shell / Shortcut rail | VIS-002 | Five shortcuts | MPC 3.9 guide | 5 configurable slots: Browser, Channel Mixer, Pad Mixer, Sounds, XY | MpcNavigationController shortcuts | IMPLEMENTED — REVERIFY | Channel Mixer/Sounds/XY reserved truthfully | Emulator smoke #3465 | Run #40591850947 | P0 |
| Shell / Contextual channel strip | VIS-005/VIS-006 | XL Channel Strip | MPC 3.9 guide p.133 | Context strip for track/pad/main output | Track/Program resolution | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Shell / Function Bar | VIS-001 | Contextual bottom functions | MPC 3.9 guide | Contextual buttons per mode; MUTE enabled, SOLO enabled | Track/Program/Pad state | PARTIAL | SOLO just wired; other modes need per-context audit | Emulator smoke #3465 | Run #40591850947 | P0 |
| Shell / Back navigation | VIS-001 | Back/Return | MPC 3.9 guide | History stack, navigateBackFromShell | Navigation state | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Sequence selection | VIS-013 | Sequence select | MPC 3.9 guide p.112 | Sequence dropdown/field in Main | nativeSequenceSelect | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / BPM | VIS-001 | BPM field | MPC 3.9 guide | BPM field, Data Dial focus SEQUENCE_BPM | nativeSequenceSetTempo | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Time signature | VIS-013 | Time Signature | MPC 3.9 guide p.112 | TimeSig field (n/d) | nativeSequenceSetTimeSignature | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Bar count & length | VIS-013 | Bars field | MPC 3.9 guide | BARS field | nativeSequenceSetBars | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Loop state & boundaries | VIS-001 | Loop ON/OFF, Start/End | MPC 3.9 guide | Loop toggle, START/END BAR fields | nativeSequenceSetLoop/SetLoopBars | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Selected track | VIS-014/VIS-081 | Track field | MPC 3.9 guide p.117 | Track field, track header | nativeSequenceGetSelectedTrack | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Track type | VIS-014 | Track Type icon | MPC 3.9 guide p.117 | Single Track Type icon (Audio/Drum/Keygroup/MIDI/Plugin/CV) | Track.type/kind | IMPLEMENTED — REVERIFY | CLIP→AUDIO discrepancy resolved in code | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Selected program | — | Program field | MPC 3.9 guide | Program name via nativeSequenceGetTrackProgram | Track.programId → program resolution | PARTIAL | Program select UI not yet exposed | None known | — | P0 |
| Main / Selected pad | — | Pad field | MPC 3.9 guide | Pad context in Main | Pad selection state | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Track/Arrangement overview | VIS-016/VIS-019/VIS-020 | Arrangement/Track view | MPC 3.9 guide p.129 | Arrangement preview + track workspace | Arrangement state | PARTIAL | Preview exists; full arrangement view needs audit | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Playhead | VIS-001 | Playhead strip | MPC 3.9 guide | MpcPlayheadStrip (3dp position strip) | Transport clock position | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Main / Transport & recording | VIS-001 | Transport controls | MPC 3.9 guide | Transport state in toolbar | nativeSequenceStart/Stop/Record | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | Run #40591850947 | P0 |
| Track Mute | VIS-001 | Mute grid | MPC 3.9 guide | 16-track bank, empty slots disabled | Track.muted | VERIFIED | None known | Emulator smoke #3465 | Run #40591850947 artifacts | P0 |
| Pad Mute | VIS-001 | Pad Mute grid | MPC 3.9 guide | 16-pad grid, Pad ON/OFF state | Pad.muted | VERIFIED | None known | Emulator smoke #3465 | Run #40591850947 artifacts | P0 |
| Track Solo | — | Solo toggle | MPC 3.9 guide | Function Bar SOLO button, native solo gating | Track.soloed | IMPLEMENTED — REVERIFY | Just wired; needs emulator re-verification | Unit tests MpcSequencerPlaybackSoloTests | — | P0 |
| Grid | — | Grid view | MPC 3.9 guide p.29 | SequenceGridView 16×16 with viewport | Pattern events | PARTIAL | View exists; native data binding needs audit | None known | — | P0 |
| Step Sequencer | — | Step view | MPC 3.9 guide p.29 | MpcSequenceStepPagePolicy | Step edit state | PARTIAL | Policy exists; UI integration needs audit | None known | — | P0 |
| Track Edit | — | Track Edit | MPC 3.9 guide | MpcTrackEditView | ADSR, envelope, filter | PARTIAL | Exists; needs full workflow audit | Native tests #3465 | — | P0 |
| Sample Edit | — | Sample Edit | MPC 3.9 guide p.36 | Sample waveform/region editor | Sample regions/layers | PARTIAL | Exists; needs full workflow audit | None known | — | P1 |
| Browser | VIS-008/VIS-009 | Browser | MPC 3.9 guide p.136 | Places/Content/Expansions, file filters | Local file system (SAF) | PARTIAL | Exists; real file ops need audit | None known | — | P1 |
| Arrangement | VIS-016/VIS-019/VIS-020 | Linear Arranger | MPC 3.9 guide p.21 | MpcArrangeView (lanes, placements, events) | Arrangement placements | PARTIAL | View exists; move/copy/paste needs audit | Native tests #2781/#2791 | — | P0 |
| Pad Mixer | — | Pad Mixer | MPC 3.9 guide | MpcPadMixerView | Pad level/pan/tune | IMPLEMENTED — REVERIFY | None known | None known | — | P1 |
| Channel Mixer | — | Channel Mixer | MPC 3.9 guide | RESERVED (unavailable screen) | — | RESERVED / UNAVAILABLE | Backend not implemented | None known | — | P1 |
| Menu | VIS-001 | Project launcher | MPC 3.9 guide p.56 | Pull-Down Menu + mode grid | Navigation state | IMPLEMENTED — REVERIFY | None known | Emulator smoke #3465 | — | P0 |
| Project | VIS-001 | Save/Load/Open | MPC 3.9 guide p.60-68 | SAVE/SAVE AS/OPEN via SAF + native serializer | Project persistence | PARTIAL | Save/Open works; deep field round-trip not fully tested | Emulator smoke #3465 | Run #40591850947 | P0 |
| Preferences | — | Preferences | MPC 3.9 guide p.61 | Preferences categories (AUDIO, MIDI/SYNC, SEQUENCER) | Audio/MIDI settings | PARTIAL | Categories exist; full wiring needs audit | None known | — | P2 |
| Recording | — | Recording options | MPC 3.9 guide | Recording state in toolbar | nativeAudioStart/StopRecording | PARTIAL | Basic recording; options need audit | None known | — | P2 |
| Q-Link / Data Dial | — | Contextual parameter | MPC 3.9 guide | Data Dial focus model (MpcUiState.DataDialFocus) | Focused semantic parameter | PARTIAL | Focus model defined; per-screen wiring incomplete | None known | — | P1 |

## Status legend and decision rules

- **VERIFIED**: exact-HEAD CI/emulator evidence exists (Build #3465 / run 40591850947).
- **IMPLEMENTED — REVERIFY**: code present but evidence predates current HEAD; re-verify on next CI run.
- **PARTIAL**: UI surface exists but semantic wiring, tests, or evidence are incomplete.
- **RESERVED / UNAVAILABLE**: intentionally truthful; backend not implemented, user sees explicit unavailable state.
- **REFERENCE ONLY — EXCLUDED**: Splice, subscriptions, cloud, desktop workflows — out of product scope.

## Known implementation notes

- PR #133 (checkpoint/mpc-ui-reference-and-product-20261030 @ 9f2868c) is Draft, not merged. Build #3632 against that SHA failed at emulator smoke step 55 (SAMP-007 UI hierarchy dump timeout).
- Current working line: fix/p0-mute-grid-ui-recovery-20261027 @ 7ecb624b. P0 Mute verified by Build #3465 / run 40591850947.
- Track Solo native gating + UI + unit tests committed as dabe2076 / f768c3cf / 7ecb624b.
- Physical MPC Studio MkII acceptance: NOT RUN (no hardware).

## Next actions

1. Re-verify Track Solo on emulator (current HEAD CI).
2. Audit Grid/Step Sequencer native data binding (P0).
3. Audit Arrangement move/copy/paste (P0).
4. Implement Browser real file operations (P1).
5. Implement Sample Edit region operations (P1).