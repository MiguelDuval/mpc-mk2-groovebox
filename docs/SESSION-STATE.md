# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** handoff checkpoint; mutable status is reconciled against live GitHub before each change.

## Latest source and verification state

### Live checkpoint — 2026-10-21 after Android Build #3045

- PR #23 is **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; current and last tested head is `dabf66bc78c4312a033d72202d74d9ccf19d2fa0`. Build #3045 / run `39658221374` failed at Android emulator smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39658221374. `main` has not been modified. `.github/workflows/android-build.yml` keeps `cancel-in-progress: false`.
- Exact #3045 artifacts: APK `12372468977` (9,957,677 bytes; SHA-256 `797902f0614272c7196b8af0bf7316cb9f9c0d5ebc9db2ae7167d4f6fb2501a0`); visual evidence ZIP `12373218933` (9,452,124 bytes; SHA-256 `96367c91dd825c535804c1b4b259633b74c264169056cf940da761f543e36cfa`). Screenshot/XML show DocumentsUI inside `AAA-MPC-Browser-Tree`, with `Browser-Tree-Only.wav`; this is not provider-root or Favorite evidence.
- **Confirmed immediate #3045 defect:** after the first fresh picker hierarchy proved the Favorite-only WAV was absent from the restored AAA directory, the script unnecessarily requested four more UIAutomator dumps of that unchanged directory. The first repeated dump timed out and aborted before breadcrumb-root navigation. The process-list diagnostic filtered out ordinary process rows, so its “header only” output did not establish that Android `ps` had no process entries; the root cause was not proven by that diagnostic.
- Earlier Build #3042 / run `39654368042` did execute a real measured breadcrumb tap to provider root, but subsequent uncompressed UIAutomator dumps failed before a fresh root XML was produced. The current fix removes the redundant pre-root dump, enables opt-in compressed hierarchy captures for FAVORITE 1 root/child navigation, and prints representative process-list rows during recovery diagnosis. Compression remains a recovery strategy, not accepted evidence: Favorite root/child screenshots and parseable fresh XML, actual folder selection, persistable grant, Browser file result and force-stop/relaunch restoration must pass.
- SAMP-008 CROP, SAMP-009 CH4/CH8/CH16, Sample Edit→Browser/query reset, USER FOLDER filtering and process-death restoration passed on prior exact-SHA runs; do not rewrite them without new regression evidence.
- Isolated PR #29 SAMP-010 Build #3043 / run `39656991761` completed **FAILURE** at step 52 with `UI_INTERACTION_FAILED: sample waveform editor`; it is not accepted. Draft PR #28 malformed-project-load Build #3044 / run `39657357881` completed **FAILURE** at shared smoke step 52 later in FAVORITE 1. Its native Project Persistence test step passed; overall failure does not invalidate that unit result or prove the end-to-end project lifecycle.
- Full Project SAVE / SAVE AS / OPEN remains **PENDING** because the primary smoke stops at FAVORITE 1. Acceptance must compare canonical state including MIDI events and Arrangement Pattern-placement IDs/parameters, and cover overwrite, force-stop/relaunch/Open, cancellation invariance, malformed-load atomicity and placement-free v1 compatibility.
- Physical MPC Studio MkII validation remains **PENDING**; emulator results are not hardware proof.

## Current execution queue — 2026-10-21

1. Remove the same-directory re-dump before root navigation; opt into compressed fresh snapshots for FAVORITE 1 DocumentsUI root/child navigation and make recovery process-list diagnostics informative. Keep all real picker, folder-grant, WAV-isolation, Browser-result and restart assertions strict. Commit coherently and verify the new exact-SHA Actions run.
2. While the primary run executes, diagnose PR #29's `runUiAuditAfterSampleEdit()` failure from its log/source and confirm PR #28's native test results. Keep all changes isolated to their respective branches.
3. After FAVORITE 1 passes on primary, complete real Project SAVE/SAVE AS/OPEN acceptance. SAMP-010 remains unintegrated until its own exact-SHA checks and primary Browser/Project gates pass.

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

1. Re-run primary FAVORITE 1 with dual-format `ps` fallback and explicit runner PID diagnostics; inspect real picker root screenshot/XML and keep grant/restore assertions strict.
2. Check isolated exact-SHA Builds #3043 (SAMP-010) and #3044 (Project malformed-load unit regression) before moving either branch again.
3. After Favorite 1 passes, validate full Project SAVE/SAVE AS/OPEN lifecycle; integrate SAMP-010 only once its own acceptance and primary gates are green.
4. Physical MPC Studio MkII testing remains a separate pending gate.

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
