# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** handoff checkpoint; mutable status is reconciled against live GitHub before each change.

## Latest source and verification state

### Live checkpoint — 2026-10-21, after Build #3042

- Primary PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; last tested primary SHA `b28b8723f31082982762e849026bddb6bb469dcf`. `main` is untouched. `.github/workflows/android-build.yml` remains `cancel-in-progress: false`.
- Android Build #3042 / run `39654368042` failed at emulator smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39654368042. APK artifact `12372125372` (9,957,677 bytes, SHA-256 `c6b84f731dad3f76d4607bf9452cd4fd29a9cc18fcd6bd716216ae874a740675`); evidence archive `12371044140` (9,437,444 bytes, SHA-256 `356829ea83795e4d464ab7eb67cf1d34236b8d08a9b3fef9b0902fd69b487582`). The archive and FAVORITE 1 picker screenshot were inspected.
- On #3042, SAMP-008 CROP and SAMP-009 CH4/CH8/CH16 passed again: 3,840→2,840 PCM frames, region reset to `0..2,840`, and exact contiguous native ranges for every chop count. Sample Edit→Browser/query reset and USER FOLDER WAV scan/filter/process-death restore also passed.
- FAVORITE 1 remains **RED**. The first picker screenshot/XML showed `AAA-MPC-Browser-Tree` with only `Browser-Tree-Only.wav`. The test activated the measured breadcrumb `Android SDK built for x86_64` to return to provider root, then all 16 bounded UiAutomator dumps failed; no current root XML was produced. Stale-runner cleanup was invoked but produced no PID match. The root cause is narrowed to Android `ps` output shape/runner detection or a runner hidden by that process-list variant—not permission acceptance. This change queries both `ps -A -o PID,NAME,ARGS` and default `ps -A`, discovers the PID column by header and adds a bounded diagnostic when no UiAutomator runner is found. No Favorite folder/grant/child/index/restart assertion is weakened.
- Isolated PR #29 SAMP-010 Build #3043 / run `39656991761` is active on merge SHA `c5d1bdac79147a6628b9116b56aa21abd0b11c2a`. Isolated PR #28 Project malformed-load atomicity Build #3044 / run `39657357881` is active on merge SHA `d66d817af19764491c3bf3c3718f3952eba6b818`. Both are history-preserving merges with the tested primary parent `b28b...`; neither changes PR #23’s ref. Their exact run conclusions/artifacts remain pending.
- Project SAVE / SAVE AS / OPEN lifecycle is **PENDING**: run #3042 stopped at Favorite 1 before reaching it. Required acceptance includes real SAF Save As/Open, canonical state equality including MIDI events and Pattern placement IDs/parameters, direct SAVE overwrite, force-stop/relaunch/Open, Save As/Open cancellation invariance, malformed file rejection preserving active state, and placement-free v1 compatibility.
- Physical MPC Studio MkII verification remains **PENDING**; emulator results are not hardware proof.

## Current execution queue — 2026-10-21

1. Commit the process-list fallback/column-aware UiAutomator cleanup fix with this checkpoint; run cheap source-contract validation and inspect the next exact-HEAD primary Actions result plus PNG/XML archive.
2. Inspect Builds #3043 and #3044 when complete; fix only evidence-backed failures on their own isolated branches, then sync them to the newest primary SHA without rewriting history.
3. Once FAVORITE 1 passes on primary, complete the entire Project SAVE/SAVE AS/OPEN acceptance. SAMP-010 remains isolated until both Browser and Project gates are green.

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
