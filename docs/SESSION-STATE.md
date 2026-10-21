# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable status claims against live GitHub.

## Latest source and verification state

### Live checkpoint — 2026-10-21 18:28 UTC

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; source head at the latest completed run was `085b84877074b74d51ee73b8547947b409500025`. `main` remains untouched. `.github/workflows/android-build.yml` uses `cancel-in-progress: false`.
- Exact-HEAD Android Build #3041 / run `39648841982` failed at emulator smoke step 52 on that head: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39648841982. Unit/build/evidence preflight and evidence upload passed. APK artifact `12369396516` (9,957,677 bytes); evidence artifact `12369491694` (9,425,136 bytes; SHA-256 `956814fa1b0636f85d23055ef845e80f5eb61e7bf6b2e2ed7e408971412f2613`).
- SAMP-008 exact native CROP assertions and SAMP-009 CH4/CH8/CH16 exact range coverage passed again on #3041: source 3,840 frames → CROP result 2,840 frames, region `0..2,840`; each Chop count yielded contiguous non-overlapping coverage. Sample Edit → Browser and query reset passed.
- Browser USER FOLDER scanning, filtering and process-death restoration passed. FAVORITE 1 remains **RED**: DocumentsUI became foreground, but the fresh picker was still at `AAA-MPC-Browser-Tree` with `Browser-Tree-Only.wav`; the isolated `Browser-Favorite-Only.wav` could not exist there. The test spent 15 seconds repeatedly re-dumping that same directory while looking for a file from another folder, then attempted root navigation and UiAutomator dumps timed out. The last accepted screenshot/XML pair still described AAA and no stable provider-root evidence was published. This is a test orchestration/UIAutomator stability failure, not evidence that Favorite URI persistence works.
- Fix being committed from the prior head: use the just-captured picker hierarchy for a local child-file check instead of repeatedly dumping the wrong current directory; then navigate to the root, pause for the DocumentsUI transition and demand a fresh hierarchy before any folder tap. Broaden remote runner cleanup to handle Android `ps` column variation and invoke it on the deadline-before-transfer path. No folder-row, hit-bound, touch, file, grant or restart assertion is removed or weakened.
- Project SAVE / SAVE AS / OPEN lifecycle was not reached on #3041; it remains an acceptance blocker after Browser FAVORITE 1 passes. Required checks include exact native MIDI/Pattern placement identity and parameter round-trip, normal SAVE overwrite, process restart + OPEN, cancellations preserving state, malformed-load preservation and placement-free v1 compatibility.
- Separate draft PR #29 / SAMP-010 is **OPEN** on `feature/samp010-zoom-audition-acceptance`, head `47a35aba42a245afb80e70dcefb1c6f5bedadd15`, base `feature/mpc-one-ui-fidelity`. Android Build #3040 / run `39648068024` has completed **FAILURE**; its focused feature and exact smoke failure need independent triage. Do not integrate into PR #23 until Browser and Project gates pass.
- Physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-21

1. Verify the bounded FAVORITE 1 picker change on the next exact-HEAD Android Build, including a fresh root screenshot/XML, actual folder row touch, child WAV, `USE THIS FOLDER` grant, Browser indexing and process-death restoration.
2. Once that primary run is active, continue working only on isolated PR #29; diagnose and fix its exact-SHA SAMP-010 failure without touching PR #23 while its run is active.
3. After a green primary Browser workflow, complete Project SAVE/SAVE AS/OPEN round-trip and failure-safety acceptance, then consider integrating SAMP-010.

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
