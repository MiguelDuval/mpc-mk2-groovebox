# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-20  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

### Live checkpoint — 2026-10-20

- Source commit at checkpoint entry: `e64caae301edc9b978d9c13423eff99da8110c83` on `feature/mpc-one-ui-fidelity`. PR #23 remains open against `fix/ui-shell-format-width-startup`; `main` remains untouched at `9243f999b2438984131747327a352e54ff947d37`. The checkpoint-document commit itself will advance the feature HEAD; always refresh the live HEAD/Actions before relying on this SHA.
- Active Android Build #2972 / run `39495867515` is still in the emulator smoke step on older SHA `be2f9e2b1e00fe4795afd37780383bb261cbc678`. The 54 preceding workflow checks, including native tests, APK assembly/upload and `bash -n tests/android_ui_smoke.sh`, passed; APK artifact `12298925525` exists (9,956,909 bytes). The smoke step has no conclusion yet. It predates the new Sample Edit crop/chop acceptance and the final Crop/Chop stream-stop error guards; it cannot verify those changes.
- Run #2992 / `39499332491` was queued on source SHA `e64caae301edc9b978d9c13423eff99da8110c83`. That source adds real-touch SAMP-008 CROP acceptance (Start +1000, native PCM frame count reduced by exactly 1000, region reset), a control-thread-safe audio-stream stop/mutate/restart for Chop, and real-touch SAMP-009 CH4/CH8/CH16 acceptance with post-mutation native getter evidence for all destination regions. Native chop-plan tests now verify exact contiguous uneven-frame partitions for all three supported counts. All of these remain **IMPLEMENTED / EXACT-SHA CI PENDING**, not verified.
- Previous Android Build #2970 / run `39468622843` failed because its 42-minute smoke timed out after opening DocumentsUI for FAVORITE 1; USER FOLDER restoration passed, but Favorite assignment and Project persistence acceptance were not reached. This is a smoke-harness stall, not proof of a Project lifecycle defect. The new tap cleanup runs the ADB client in an isolated host process group, confirms DocumentsUI foreground, then performs bounded process cleanup. Its full latest-SHA result is still pending.
- The next acceptance priority remains the complete current-HEAD Project SAVE / SAVE AS / OPEN lifecycle: real SAF provider; full canonical state including MIDI events and Pattern placement identity/parameters; force-stop/relaunch; both cancellations; malformed-input preservation; and placement-free v1 compatibility. Do not label this latest source verified until one run for the resulting exact HEAD succeeds.
- Physical MPC Studio MkII tests remain **PENDING**; emulator evidence is not physical-device evidence.


- Repository: `MiguelDuval/mpc-mk2-groovebox`; working branch: `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN**, base `fix/ui-shell-format-width-startup`; not merged or retargeted.
- Latest completed exact-SHA Android Build #2902 / run 39244891264 passed on `b2aa9cacf21a99b1ad51ef4046e55e9537a8754a`: full emulator smoke, Step Sequencer Pad selection and Track Edit GAIN/PAN acceptance passed. [Actions #2902](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39244891264). APK artifact 12183172318 (SHA-256 `50b118babbd6f89b305d8addeffdcbc40f72e37e07fbc09ea197b5e9d77d60c8`); UI evidence artifact 12183699319 (downloaded archive SHA-256 `434e9a1e1962ce6b4025463e06538c2bc5e5c649cc805ea89130a6c2c5105468`). ZIP integrity passed, and changed/restored gain/pan accessibility hierarchies and screenshots were inspected.
- Newer Android Build #2903 / run 39251501163 on exact PR SHA `534fdf35d9fe3f67c0ab62e93392489a0d208fe3` failed before the EDIT-007 velocity-bound mutation assertions. APK assembly/upload and shell syntax checks passed; real Pad selection, Step Edit and Track Edit GAIN/PAN assertions had passed earlier in the same smoke. The failure is isolated to the scrolling test harness: after Track Edit had scrolled below the waveform, UiAutomator no longer exposed `Track Edit Samples waveform`, so the helper could not derive a swipe. Candidate fix on `feature/scroll-helper-fix` now resolves the visible scrollable container from semantic GAIN/TUNE/PAN/REGION descendants; exact-PR-SHA CI is still required.
- EDIT-004/005 are verified by actual touch: GAIN 100% → 90% → 100%, preserving TUNE/PAN; PAN C → L100 → C, preserving GAIN/TUNE. These were already part of PR #23 at exact SHA `b2aa9cacf21a99b1ad51ef4046e55e9537a8754a`; the independent velocity-range candidate remains unverified until its changes are tested on the PR's exact SHA.
- The implementation's user-facing Project SAVE / SAVE AS / OPEN route is present in `MainActivity.java`, using Android SAF and native serializer/loader calls. The native loader validates a candidate, stops transport/audio as needed, clears runtime sample buffers and incompatible edit history, then replaces model state. Sample PCM is external; assigned WAV data may need reloading/relinking.
- Project SAVE / SAVE AS / OPEN lifecycle is now VERIFIED by Android Build #2856 / run 39162451542 on exact HEAD 617511efb820e96c6d9576051fa30012a3368db1 (run completed successfully; [Actions](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39162451542)). The real DocumentsUI/SAF flow restored the exact canonical domain SHA 4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9, including two non-default MIDI events and placement identity/reference/start 480/length 720/source offset 120. The same extended smoke passed force-stop/relaunch → Open, SAVE AS/Open cancellation with unchanged state SHA, malformed-load rejection without active-state mutation, and placement-free runtime-valid v1 Open with zero placements and canonical-v2 state-oracle equality.
- Historical note: Build #2808 predates the Android Project UI route and is not proof of Project UI integration. The current acceptance is proven by Build #2856, not by the older codec-only result. APK artifact 12149263768 (9,956,909 bytes; SHA-256 ecca25dd5a02c0d8eb02fd0d19a13a11e68cb0e38558c11ec1a5ddceddd376f0) and visual-evidence artifact 12150136466 (12,168,362 bytes; SHA-256 662d8ff8a923c8f186109b90845fb02d676216d662e1017da7f221b9fe51cdf5) are unexpired.
- Android Build #2856 / run 39162451542 passed on exact SHA 617511efb820e96c6d9576051fa30012a3368db1 with full Project lifecycle emulator acceptance. The prior Build #2835 / run 39139658033 was cancelled on older SHA 450bca9aac99adc47c61d14d3abc76a77c3cf439 and is superseded, not counted as either pass or failure.
- A new controller research document now connects the public MIDI/SysEx source, official Akai support/quickstart/tutorial resources, code, tests and physical validation: [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). Source scope explicitly excludes MPC Desktop and DAW workflows. The official tutorial playlists are indexed, but full transcripts were not accessible in this pass; no claim is made that every video has been reviewed.
- Controller documentation and agent rules have been reconciled to reflect current non-pad semantic routing and partial feedback implementation. The upstream protocol repo has 72 binary LCD SysEx messages (60 `.syx` + 12 `.sysex`) and 72 paired PNGs; representative headers confirm six-chunk LCD traffic, but the payloads have not all been individually decoded. A newly catalogued conflict exists: the upstream README/current app map Mode to Note 114 while the Go POC `controls.yaml` has Mode=1. Do not resolve from assumption; capture physical bytes. The decoder also accepts Jog CC 100 and Touch Strip CC 33 without a channel constraint.
- Physical MPC Studio MkII verification remains **PENDING** for many current behaviors. Emulator/unit tests do not replace device checks, and the existing detailed physical record is tied to its historical APK/build.
- Added [MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md](MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md) as the cross-device contract between MPC One / MPC 3.9 target actions and Studio MkII physical gestures. It includes a full top-panel control matrix, Mode+Pad 1–16 routes, explicit mismatched modifier gestures, no-equivalent controls (notably Q-Link bank / XYFX), semantic routing contracts, source register and a bounded physical-test queue. It preserves unresolved protocol conflicts rather than guessing. This is documentation, not proof of physical acceptance.


## Current execution queue — 2026-10-19

1. Project SAVE / SAVE AS / OPEN remains VERIFIED by Android Build #2902 / run 39244891264 on exact SHA `b2aa9cacf21a99b1ad51ef4046e55e9537a8754a`. Real SAF v2 save/mutate/open restored canonical state SHA `4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9`; force-stop/relaunch, cancellation, malformed-file preservation and placement-free v1 compatibility passed.
2. Step Edit VEL/PROB, page navigation and Pad selection are verified by Builds #2900/#2901. Track Edit GAIN/PAN is verified by #2902 with exact inverse restoration and unrelated fields preserved.
3. Build #2903 / run 39251501163 failed in the EDIT-007 scroll precondition, before any velocity bound mutation was attempted. The root cause is a stale scroll-helper assumption, not evidence of a native velocity-range defect. A corrected helper is staged on `feature/scroll-helper-fix`.
4. NEXT: safely integrate that helper fix against the current PR HEAD and require a new full Android Build. If EDIT-007 passes, reconcile the separately prepared EDIT-008 region Start/End test with this helper fix and take it as the next bounded action.

PR #23 remains open against `fix/ui-shell-format-width-startup`; `main` is untouched. Physical MPC Studio MkII validation remains **PENDING**.

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

1. Inspect the Android Build attached to the new implementation SHA, including emulator-smoke conclusion, logs, screenshots/accessibility hierarchy, APK and evidence bundle. Unit tests and APK upload do not close persistence.
2. If an acceptance assertion fails, fix the actual cause without weakening it and rerun at the resulting exact SHA. Preserve the external sample-PCM contract.
3. Only after persistence is green, select the next high-value Arrange/Grid/Track/Step gap from live source/tests/action catalog; do not restart placement work or repeat controller research.
4. Controller hardware work must use the new traceability map and the semantic control map. Resolve the Jog/Touch Strip CC channel discrepancy only after capturing raw bytes on the real controller. Physical verification remains separate from emulator/CI.

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
