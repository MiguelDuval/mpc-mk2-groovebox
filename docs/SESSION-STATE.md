# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-19  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; working branch: `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN**, base `fix/ui-shell-format-width-startup`; not merged or retargeted.
- Latest completed exact-SHA Android Build #2900 / run 39236521987 passed on `a285ed9bcdb3426e71f85a600838f6589bc121d9`: real Android SAF Project save/open lifecycle, Grid VEL/PROB/RAT/NUDGE/DUR field-isolation round trips, Step Edit VEL and PROB mutation/restoration, RAT/NUDGE/DURATION field-isolation, and real-touch Step page 1 → 2 → 1 navigation all passed. APK artifact 12179887151 (SHA-256 `1a5892b5477471196f0561e957379ebf24d6253db70123612718723157659b76`); UI evidence artifact 12180247158 (SHA-256 `f6629398bdf5d0f26d148a6e94bdcc87cbfbc73525debff9a58860a9870f7fab`). The next candidate adds real-touch Pad selection round-trip; its own exact-SHA CI is pending.
- The implementation's user-facing Project SAVE / SAVE AS / OPEN route is present in `MainActivity.java`, using Android SAF and native serializer/loader calls. The native loader validates a candidate, stops transport/audio as needed, clears runtime sample buffers and incompatible edit history, then replaces model state. Sample PCM is external; assigned WAV data may need reloading/relinking.
- Project SAVE / SAVE AS / OPEN lifecycle is now VERIFIED by Android Build #2856 / run 39162451542 on exact HEAD 617511efb820e96c6d9576051fa30012a3368db1 (run completed successfully; [Actions](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39162451542)). The real DocumentsUI/SAF flow restored the exact canonical domain SHA 4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9, including two non-default MIDI events and placement identity/reference/start 480/length 720/source offset 120. The same extended smoke passed force-stop/relaunch → Open, SAVE AS/Open cancellation with unchanged state SHA, malformed-load rejection without active-state mutation, and placement-free runtime-valid v1 Open with zero placements and canonical-v2 state-oracle equality.
- Historical note: Build #2808 predates the Android Project UI route and is not proof of Project UI integration. The current acceptance is proven by Build #2856, not by the older codec-only result. APK artifact 12149263768 (9,956,909 bytes; SHA-256 ecca25dd5a02c0d8eb02fd0d19a13a11e68cb0e38558c11ec1a5ddceddd376f0) and visual-evidence artifact 12150136466 (12,168,362 bytes; SHA-256 662d8ff8a923c8f186109b90845fb02d676216d662e1017da7f221b9fe51cdf5) are unexpired.
- Android Build #2856 / run 39162451542 passed on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 with full Project lifecycle emulator acceptance. The prior Build #2835 / run 39139658033 was cancelled on older SHA 450bca9aac99adc47c61d14d3abc76a77c3cf439 and is superseded, not counted as either pass or failure.
- A new controller research document now connects the public MIDI/SysEx source, official Akai support/quickstart/tutorial resources, code, tests and physical validation: [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). Source scope explicitly excludes MPC Desktop and DAW workflows. The official tutorial playlists are indexed, but full transcripts were not accessible in this pass; no claim is made that every video has been reviewed.
- Controller documentation and agent rules have been reconciled to reflect current non-pad semantic routing and partial feedback implementation. The upstream protocol repo has 72 binary LCD SysEx messages (60 `.syx` + 12 `.sysex`) and 72 paired PNGs; representative headers confirm six-chunk LCD traffic, but the payloads have not all been individually decoded. A newly catalogued conflict exists: the upstream README/current app map Mode to Note 114 while the Go POC `controls.yaml` has Mode=1. Do not resolve from assumption; capture physical bytes. The decoder also accepts Jog CC 100 and Touch Strip CC 33 without a channel constraint.
- Physical MPC Studio MkII verification remains **PENDING** for many current behaviors. Emulator/unit tests do not replace device checks, and the existing detailed physical record is tied to its historical APK/build.
- Added [MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md](MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md) as the cross-device contract between MPC One / MPC 3.9 target actions and Studio MkII physical gestures. It includes a full top-panel control matrix, Mode+Pad 1–16 routes, explicit mismatched modifier gestures, no-equivalent controls (notably Q-Link bank / XYFX), semantic routing contracts, source register and a bounded physical-test queue. It preserves unresolved protocol conflicts rather than guessing. This is documentation, not proof of physical acceptance.


## Current execution queue — 2026-10-19

1. **Project SAVE / SAVE AS / OPEN — VERIFIED.** Android Build #2900 / run 39236521987 passed on exact SHA `a285ed9bcdb3426e71f85a600838f6589bc121d9` and re-ran the real DocumentsUI/SAF lifecycle. V2 save → mutate live Project → Open restored canonical state SHA `4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9`, including MIDI events and Pattern-placement identity/parameters; force-stop/relaunch, SAVE AS/Open cancellation, malformed-file preservation and placement-free v1 compatibility passed. [Actions #2900](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39236521987).
2. **Grid event parameters and Step Edit STEP-005/006/007/008/009 — VERIFIED on #2900.** Grid VEL/PROB/RAT/NUDGE/DUR real-touch changes/restores passed. Step Edit VEL changed 100 → 101 → 100; PROB 127 → 126 → 127; RAT 1 → 2 → 1; NUDGE/DURATION field-isolation round trips passed, preserving unrelated fields and Step 01.
3. **STEP-002 — VERIFIED on #2900.** Real touchscreen navigation changed page 1 → page 2 and returned to page 1. Relative column 2 stayed mapped to absolute Step 18 then Step 2; the empty fixture and existing Pattern state were preserved.
4. **NEXT: STEP-001 Pad selection.** The isolated follow-up adds PAD + / PAD − real-touch checks: switch Pad 1 → Pad 2 while preserving the absolute Step cursor, verify the UI projection agrees with the selected Pad's native event state, then restore Pad 1 and assert the temporary event and Step 01 fixtures remain unchanged. Require CI on the resulting PR SHA before marking the action verified.
5. Physical MPC Studio MkII validation remains **PENDING**; emulator evidence is not hardware proof. Uncovered Project boundaries remain provider overwrite/write-failure recovery, opening two saved Projects independently, and relinking external sample PCM.

PR #23 remains open against `fix/ui-shell-format-width-startup`; `main` remains untouched.

## Completed in this session

### 1. Track Edit sample-layer tuning — VERIFIED

The TUNE row is below the initial viewport inside the real Samples `ScrollView`. Live-bound scrolling exposes TUNE+, the test performs one real touch, verifies `+0.00 st → +1.00 st` against authoritative sampler state, exits/re-enters the actual route, confirms persistence, then restores the original value.

### 2. Main left Channel Strip geometry — FIXED / VERIFIED

The old condensed mixer was a duplicate weighted child re-exposed by saved Mixer visibility. It remains mounted but GONE to preserve refresh/state dependencies and shell visibility wiring. Live hierarchy checks confirm the XL strips fill the Main left region with the legacy panel absent.

### 3. Grid DRAW/ERASE — VERIFIED

A real emulator touch adds Pad 01 / Step 34 at velocity 100, ERASE removes the temporary note, and the seeded Step 33 note remains intact. The selected-cell accessibility projection is read from native sequencer state.

### 4. Step Sequencer touch/edit/remove — VERIFIED

Real emulator touches add Pad 01 / Step 02, adjust probability through the actual PARAM and decrement controls from 127 to 126, then erase the temporary event. Step 01 is unchanged; native cleanup and the parameter-selector restore pass.

### 5. Arrangement loop-brace touch/restore — VERIFIED

The real external drag changed the loop end from bar 4 to bar 3, and a second real drag restored bar 4. Native loop state and the application-side release handshake passed.

### 6. Arrangement event MOVE ± GRID — IMPLEMENTED / VERIFIED

The Canvas event projection now carries each MIDI note number from the native model. A single tap identifies the event; EVENT − / EVENT + move it one current quantize-grid interval earlier/later via a native Pattern mutation. The native operation rejects invalid/out-of-range/colliding destinations, preserves duration/velocity/probability/ratchet/nudge, and records undo/redo in the existing edit history. The UI reloads and verifies the native event projection after mutation.

The full emulator smoke used a real canvas touch to select Track 01 / Note 036 at tick 7680, tapped EVENT + to reach tick 7920, then EVENT − to restore tick 7680. Before/moved/restored screenshots and matching accessibility hierarchies were inspected. The native Pattern operation tests and Arrangement projection JUnit tests passed as part of Build #2781.

### 7. Arrangement DUP / COPY / PASTE / CUT + canonical Function Bar — VERIFIED

DUP/COPY/PASTE/CUT are backed by native Pattern operations and native edit history, surfaced from the canonical bottom Function Bar. Event-only actions require a real event selection; PASTE additionally requires valid native clipboard state. UNDO/REDO and EVENT ± GRID remain in the secondary action row.

Build #2791 performed real touches and verified DUP → UNDO → REDO → UNDO, COPY → PASTE → UNDO → REDO → UNDO, and CUT → UNDO → REDO → UNDO against exact native Pattern state. Its full workflow passed all 57 steps, and its APK/evidence artifacts were inspected.

### 8. Arrangement empty-timeline paste cursor — IMPLEMENTED / VERIFIED

Build #2793 verified an actual empty-timeline touch at tick 240. The snapped cursor has its own state (not a fabricated event selection), shows a cyan marker and accessible Track/tick label, leaves the native Pattern unchanged, and enables PASTE only while a valid cursor and native clipboard coexist. After PASTE, note duration, pitch, velocity, probability, ratchet and nudge are preserved; UNDO restores the cursor and prior CUT Pattern, REDO recreates the event, and final UNDO CUT restores the exact fixture.

Verified by [Android Build #2793](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39075574304) on code SHA [`f49748de9db450ffc3b3b781befac99916f2a134`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/f49748de9db450ffc3b3b781befac99916f2a134). All 57 workflow steps passed. The exact APK and visual-evidence archives were downloaded and integrity-checked; cursor/pasted/final-restored screenshots and accessibility XML were inspected.

Historical note superseded on 2026-10-18: explicit Pattern placements and their place/move/split/delete/duplicate operations are now implemented and emulator-verified. The open gap is project file save/reload through the Android user workflow.

### 9. MPC Studio MkII protocol/support/tutorial traceability — DOCUMENTATION COMPLETE / VIDEO TRANSCRIPTS NOT FULLY REVIEWED

Created [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). It documents the full pad-note map, 39 button/input mappings, Jog Wheel/Touch Strip input, button LED CC semantics, RGB pad SysEx, the six-chunk 160×80 LCD transport, corresponding source files/tests, and a physical validation queue. It links the primary community reverse-engineering repository, its Go/JavaScript LCD proof-of-concepts, official Akai FAQ and troubleshooting guide, the manufacturer-authored Quickstart mirror, and the official Akai-linked Academy/Masterclass playlists. Only hardware-level facts are retained; MPC Desktop, plug-in, licensing and DAW workflows are explicitly excluded.

The official playlists could not be fully transcript-audited through the available source view, so the document labels them indexed but not fully reviewed. It does not invent video findings. Hardware doc/semantic-map status summaries were reconciled; agent instructions and the operating protocol now require traceability before controller work. No app/runtime code was changed in this documentation slice. The current exact-HEAD workflow result still requires a live check.

## Current execution order

1. Verify STEP-001 Pad selection with real touchscreen PAD + / PAD −, native state projection, preserved Step cursor and exact fixture restoration on the candidate's own exact-SHA Android Build.
2. Select the next bounded incomplete action from `docs/MPC3-USER-ACTION-CATALOG.md`; do not reopen Project persistence or completed Grid/Step parameter work without a regression.
3. Keep physical MPC Studio MkII acceptance separate from emulator CI and continue preserving the app's native-authoritative music model.

### 10. MPC One ↔ Studio MkII cross-device integration — DOCUMENTATION COMPLETE / PHYSICAL VALIDATION PENDING

Created [MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md](MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md) and linked it from AGENTS.md, the controller semantic map, protocol traceability, MPC3 reference index and operating protocol. The crosswalk distinguishes the MPC 3.9 target action, Studio's printed hardware gesture, semantic app route, backend status and physical acceptance. High-risk differences are explicitly documented: Shift+Main (Track View vs Grid), Shift+Browse (Browser Up vs Save), Pad Mute / Track Mute ordering, Studio Mode+Pad shortcuts, and the absence of physical eight-knob Q-Link and two-axis XYFX controls.

The source register includes official MPC 3.9 documentation, Studio MkII support/Quickstart, public MIDI/SysEx reverse engineering and Akai workflow articles. Studio Academy/Masterclass playlists remain indexed but not fully transcript-audited because usable transcripts were not available. Mode Note 114 vs Note 1 and CC100/CC33 channel handling remain unresolved pending real hardware captures. No app/runtime code was changed. Physical controller verification remains PENDING; the implementation roadmap's Project Save/Open exact-SHA acceptance queue is unchanged.

## Mandatory operating rules

- `AGENTS.md`, `docs/PROJECT_MASTER_PROMPT.md`, and `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` govern all agents.
- The dated queue at the top of `docs/IMPLEMENTATION-ROADMAP.md` is the immediate task order. Older dated notes are historical, not a current claim.
- One writer per branch: do not have Cline and ChatGPT mutate the same branch or files simultaneously. Parallel work is allowed only on isolated branches/worktrees with explicit file ownership and sequential integration.
- Start with targeted checks; perform full exact-HEAD CI and inspect real APK/evidence artifacts at the acceptance boundary.
- A build is not accepted on source inspection, a previous SHA, a screenshot alone, or a green targeted test.
- Never modify `main`, reset/rebase/rewrite history, force-push or merge PR #23 as part of routine work.
- Preserve existing audio/MIDI/sampler/sequencer boundaries. Never trade genuine acceptance coverage for a green workflow.

## Historical notes

The prior long-form checkpoint has been preserved in `docs/SESSION-HISTORY-ARCHIVE.md`. Read it only for a specific regression/history investigation, and verify any claim against live evidence.
