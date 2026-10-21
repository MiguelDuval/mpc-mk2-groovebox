# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable claims against live GitHub.

## Latest source and verification state

### Live checkpoint — 2026-10-21 18:02 UTC

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; HEAD at latest primary run: `09cbc15c702c2d1b1989da7d453e50820c71c993`. `main` is untouched. Workflow `cancel-in-progress: false`.
- Exact-HEAD Android Build #3039 / run `39643908281` completed **FAILURE** at smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39643908281. APK build, evidence preflight and evidence upload passed. APK artifact `12367216250` (9,957,677 bytes); evidence artifact `12367083607` (9,438,132 bytes).
- SAMP-008 CROP passed: real touch changed the fixture from 3,840 to 2,840 PCM frames after Start=1,000 and reset the region to `0..2,840`. SAMP-009 CH4/CH8/CH16 passed exact contiguous, non-overlapping native range checks across 2,840 frames. The Sample Edit→Browser return and query reset passed as well.
- USER FOLDER browsing/filters and process-death restoration passed, but FAVORITE 1 acceptance remains **RED**. DocumentsUI did become foreground after a real Choose Folder touch. The subsequent UI hierarchy snapshots timed out 14 times; the stale saved image/XML pair still described the previous `AAA-MPC-Browser-Tree` directory while an older hierarchy contained the `BBB-MPC-Browser-Favorite` row. No stable fresh provider-root screenshot was published, and Project SAVE/SAVE AS/OPEN was not reached.
- Root-cause correction being added to the existing smoke harness: on an Android-side UIAutomator dump/transfer timeout, clean up only lingering `com.android.commands.uiautomator.Launcher` runner processes before retrying. `adb_bounded` currently kills its host adb process group but cannot guarantee that the remote UIAutomator runner exits; one stuck remote UiAutomation session can poison subsequent dumps. Cleanup never kills DocumentsUI, never trusts stale XML as current, and does not relax row hit-bounds, actual touch, child WAV, folder grant or restart assertions. This hypothesis requires verification in the next exact-SHA run.
- Isolated SAMP-010 is being developed in draft PR #29, branch `feature/samp010-zoom-audition-acceptance`, base SHA `09cbc15c702c2d1b1989da7d453e50820c71c993`. Its first run #3040 / `39648068024` is in progress on SHA `47a35aba42a245afb80e70dcefb1c6f5bedadd15`. It remains separate from PR #23 until Browser SAF and Project persistence gates pass.
- Physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-21

1. Validate the selective remote UIAutomator-runner cleanup against FAVORITE 1 after a real picker transition; keep every folder/permission/persistence assertion strict.
2. While that primary fix runs, inspect the isolated PR #29 focused unit/preflight and full emulator smoke outcome. Do not merge SAMP-010 into #23 before primary Browser SAF and Project lifecycle are green.
3. Finish FAVORITE 1 child WAV + `USE THIS FOLDER` grant + Browser indexing + process-death restoration; then complete Project SAVE/SAVE AS/OPEN domain-equivalence and failure-safety tests.
4. Physical controller tests remain separate.

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

1. Fix the root cause of repeated remote UIAutomator dump timeouts after the FAVORITE 1 DocumentsUI transition; never reuse the stale hierarchy as proof of current state.
2. Verify on a new exact-HEAD primary workflow that FAVORITE 1 row interaction, child-file, grant, Browser results, and restart persistence all pass.
3. Complete Project SAVE / SAVE AS / OPEN invariants after Browser passes. Keep SAMP-010 isolated in PR #29 until both primary gates turn green.
4. Physical MPC Studio MkII remains pending.

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
