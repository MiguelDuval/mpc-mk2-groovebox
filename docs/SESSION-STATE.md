# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable claims against live GitHub.

## Latest source and verification state

### Live checkpoint — Android Build #3035 failed; next bounded fix prepared, 2026-10-21 16:27 UTC

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; live head before this fix is `fe0742cc2ceaf21b6b5589c1a07553839f7431f2`. `main` remains untouched.
- Latest exact-HEAD run at triage: Android Build #3035 / `39623619559`, failed at step 52 (emulator smoke): https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39623619559. Native/unit steps, APK build, evidence preflight and artifact upload completed successfully.
- SAMP-008 and SAMP-009 targeted runtime checks passed in this run: CROP changed 3,840 → 2,840 frames after Start=1,000 and reset the region to `0..2,840`; CH4/CH8/CH16 native pad regions formed exact contiguous coverage without gaps/overlap. Sample Edit → Browser and Browser search reset passed too. Full acceptance is still red because Browser/SAF has not completed.
- Run #3035 failed during FAVORITE 1 provider-root fixture scrolling. After a real DocumentsUI state transition and a swipe, one direct 8-second `dump_device_ui` failed; the test aborted rather than retrying the transient hierarchy dump. The uploaded screenshot `mpc-groovebox-browser-favorite-tree-picker.png` and logged XML show DocumentsUI remained visible with its folder-picker controls, not an app/provider rejection. The failure is a harness robustness issue.
- Fix prepared in this checkpoint: `dump_device_ui_retry` captures to scratch and only replaces the last valid XML after a complete parseable snapshot; the FAVORITE 1 folder-scroll loop uses that bounded 4-attempt helper instead of a one-shot dump. This preserves real folder visibility/touch/child-WAV/grant/persistence checks.
- Artifacts from #3035: APK `12355519965` (9,957,677 bytes; SHA-256 `bce4114b3ed65171ffa76699d80f95bfe8d5db88251b0077d07df35911e6e30c`); visual evidence `12357687535` (9,423,739 bytes; SHA-256 `425da1d23a3c200648b76f1716264c4bd5e5abb37099d0c4fa459dd0b7d5f098`). Evidence ZIP was downloaded; the FAVORITE 1 picker PNG was inspected.
- Current Project SAVE / SAVE AS / OPEN acceptance was not reached; it remains required on a single green exact-HEAD full smoke with MIDI event/Pattern placement equality, force-stop/relaunch/open, cancellation invariance, malformed-load preservation and placement-free v1 compatibility.
- `.github/workflows/android-build.yml` remains `cancel-in-progress: false`; preserve it. The isolated SAMP-010 draft remains separate; physical MPC Studio MkII acceptance remains **PENDING**.

## Current execution queue — 2026-10-21

1. Validate the scratch-preserving retry helper and use it after the FAVORITE 1 folder-list swipe, with a small source/syntax check and a new full Android workflow on the exact resulting SHA.
2. Require FAVORITE 1 to expose and tap the real folder row, select the exact child WAV, grant the tree, show the Browser result, and restore it after process death. Do not relax any acceptance assertion.
3. On the same green full run, complete Project SAVE / SAVE AS / OPEN acceptance and update verified state. Do not integrate SAMP-010 before that gate.

## Completed in this session

### 1. Track Edit sample-layer tuning — VERIFIED

The TUNE row is below the initial viewport inside the real Samples `ScrollView`. Live-bound scrolling exposes TUNE+, the test performs one real touch, verifies `+0.00 st → +1.00 st` against authoritative sampler state, exits/re-enters the actual route, confirms persistence, then restores the original value.

### 2. Main left Channel Strip geometry — FIXED / VERIFIED

The old condensed mixer was a duplicate weighted child re-exposed by saved Mixer visibility. It remains mounted but GONE to preserve refresh/state dependencies and shell visibility wiring. Live hierarchy checks confirm the XL strips fill the Main left region with the legacy panel absent.

### 3. Grid DRAW/ERASE — VERIFIED

A real emulator touch adds Pad 01 / Step 34 at velocity 100, ERASE removes the temporary note, and the seeded Step 33 event remains intact. The selected-cell accessibility projection is read from native sequencer state.

### 4. Step Sequencer touch/edit/remove — VERIFIED

Real emulator touches add Pad 01 / Step 02, adjust probability through the actual PARAM and decrement controls from 127 to 126, then erase the temporary event. Step 01 is unchanged; native cleanup and the parameter-selector restore pass.

### 5. Arrangement loop-brace touch/restore — VERIFIED

The real external drag changed the loop end from bar 4 to bar 3, and a second real drag restored bar 4. Native loop state and the application-side release handshake passed.

### 6. Arrangement event MOVE ± GRID — IMPLEMENTED / VERIFIED

The Arrangement canvas exposes native note identity in its accessibility projection. The real-touch smoke moved Track 01 / Note 036 from tick 7680 to tick 7920 using EVENT +, restored tick 7680 using EVENT −, verified native Pattern state, then used UNDO/REDO/UNDO to prove reversible edits and exact canonical restoration.

### 7. Arrangement event DUP/COPY/PASTE/CUT — VERIFIED

Real touch and native Pattern snapshots verified event duplication and clipboard operations, retaining event fields and preventing invalid destinations; the acceptance assertions remain part of the current full smoke.

### 8. Arrangement empty-timeline paste cursor — IMPLEMENTED / VERIFIED

The empty-canvas paste cursor is a real Arrangement command, not a fake success. The smoke validates insertion at a grid-snapped empty target and full native-state equivalence after undo/redo/restore.

### 9. Controller traceability — DOCUMENTATION COMPLETE / PHYSICAL VALIDATION PENDING

MPC Studio MkII protocol/support/tutorial traceability and cross-device mapping are documented. Video transcripts have not been fully reviewed; hardware-specific claims still require physical validation.

## Mandatory operating rules

- Never touch `main`; no reset/rebase/force-push/history rewrite/merge in this workflow.
- One writer per branch. Do not push while an exact-HEAD full workflow is active unless a confirmed critical defect requires a fix.
- Keep SAMP-010 on its isolated branch until Browser and Project persistence acceptance on PR #23 is green.
- Keep screenshot evidence curated to unique milestones; preserve real touches and native state assertions.
- Physical MPC Studio MkII validation remains **PENDING**.
