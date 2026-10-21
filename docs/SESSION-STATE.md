# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable claims against live GitHub.

## Latest source and verification state

### Live checkpoint — 2026-10-21 17:10 UTC

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; current HEAD at triage was `e2a3b5da9722130885fb5ffdfff0c088922d7260`. `main` remains untouched.
- Latest exact-HEAD PR run #3037 / `39636137531` failed at Android emulator smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39636137531. Native/unit tests, APK build, evidence preflight and upload succeeded.
- SAMP-008 passed: real-touch CROP reduced the fixture 3,840→2,840 frames after Start=1,000 and reset the active range to `0..2,840`. SAMP-009 passed exact native contiguous coverage for CH4, CH8 and CH16. Sample Edit → Browser and Browser Search reset also passed. The overall workflow is still **RED** because later SAF acceptance failed; do not mark the sample slice fully accepted yet.
- Run #3037 failure: after USER FOLDER persistence passed, the FAVORITE 1 Document Provider picker opened in the prior `AAA-MPC-Browser-Tree` directory. The smoke navigated back to the provider root, but `capture_stable_picker_state` used one un-retried `dump_device_ui` call per snapshot and broke immediately on a transient timeout. The log contains `UI hierarchy deadline expired before transfer`; the uploaded artifact `12364796422` (9,435,921 bytes, SHA-256 `7ffa3413febf94285411873d357a0a5bd31d4ac90bc1549b0f32a8b5df77f853`) confirms the system picker stayed foreground and exposed `BBB-MPC-Browser-Favorite` in the root hierarchy. This is a flaky stable-capture harness path, not evidence that the URI grant or folder fixture is invalid.
- Fix in progress: cap each hierarchy dump at 8 seconds within the existing overall stabilization deadline; retry transient dump/signature failures; retain the last valid hierarchy; publish a PNG only after the post-screenshot hierarchy matches it. Keep target-row bounds, actual folder taps, exact child WAV, `USE THIS FOLDER`, persisted-grant and restart assertions strict.
- Run #3037 APK artifact `12361224147` (9,957,677 bytes); visual evidence artifact `12364796422`. Project SAVE / SAVE AS / OPEN acceptance was not reached. It remains required on a green exact-HEAD workflow, including MIDI events and Pattern placement identity/parameters, overwrite, force-stop/relaunch, cancellation invariance, malformed-load preservation and placement-free v1 compatibility.
- `.github/workflows/android-build.yml` remains `cancel-in-progress: false`. Physical MPC Studio MkII verification remains **PENDING**.

## Current execution queue — 2026-10-21

1. Validate and commit the bounded retry in `capture_stable_picker_state` plus the live Browser/SAMP status rows. Trigger one new full workflow, then inspect the failure/success and its PNG/XML artifacts for that exact SHA.
2. Complete FAVORITE 1 child-WAV navigation, folder grant, Browser indexing and process-death restoration on the same exact-HEAD green run.
3. Only after Browser acceptance, finish Project SAVE / SAVE AS / OPEN lifecycle coverage; do not integrate SAMP-010 while the required gates remain red.
4. Physical controller checks remain separate from emulator results.

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

## Current execution order — 2026-10-21

1. Retry transient DocumentsUI hierarchy failures while preserving the last valid snapshot and matching visual evidence; do not weaken folder-row, touch, file, grant or restart assertions.
2. Obtain a green current-HEAD full Android smoke through FAVORITE 1/USER FOLDER persistence, then Project SAVE / SAVE AS / OPEN lifecycle acceptance.
3. Keep SAMP-010 waveform zoom/audition isolated until these acceptance gates pass; physical MPC Studio MkII validation is pending.

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
