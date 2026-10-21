# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; mutable status must be rechecked against live GitHub.

## Latest source and verification state

### Live checkpoint — 2026-10-21 17:55 UTC

- Primary PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; PR HEAD `09cbc15c702c2d1b1989da7d453e50820c71c993`. `main` is untouched; workflow concurrency remains `cancel-in-progress: false`.
- Exact-HEAD Android Build #3039 / run `39643908281` completed **FAILURE** at smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39643908281. APK build and smoke evidence preflight passed; APK artifact `12367216250` (9,957,677 bytes), evidence artifact `12367083607` (9,438,132 bytes).
- SAMP-008 passed on #3039: real-touch CROP changed 3,840→2,840 PCM frames after Start=1,000 and reset the region to `0..2,840`. SAMP-009 passed exact contiguous native CH4/CH8/CH16 coverage of all 2,840 frames. The Sample Edit→Browser return and query reset also passed.
- #3039 reached actual FAVORITE 1 DocumentUI after a real Choose Folder touch, but subsequent fresh hierarchy calls timed out repeatedly. The log shows 14 bounded stability snapshots, `UI hierarchy deadline expired before transfer`, then failure to stabilize the provider-root hierarchy. The existing XML confirmed DocumentsUI remained foreground with the Favorite-only folder listed; there is still no evidence of a product permission/grant rejection. Browser/FAVORITE acceptance remains **RED**, and Project SAVE/SAVE AS/OPEN was not reached.
- Next primary acceptance remains: prove FAVORITE 1’s actual folder-row touch → child WAV visibility → Android `USE THIS FOLDER` grant → Browser indexing → process-death URI/grant restoration, followed by the full Project lifecycle invariants (MIDI event data and Pattern placement ID/parameters, direct SAVE overwrite, force-stop/relaunch/open, cancellation invariance, malformed-load preservation, placement-free v1 compatibility).
- While the primary acceptance run is now complete, SAMP-010 is being developed in separate branch `feature/samp010-zoom-audition-acceptance` from this same base SHA. Do not integrate it into PR #23 until the primary Browser SAF and Project persistence gates pass on a green exact-SHA run.
- Physical MPC Studio MkII validation remains **PENDING**; emulator evidence is not physical-device evidence.

## Current execution queue — 2026-10-21

1. Complete and commit SAMP-010 in the isolated branch: normalized viewport readout, accessible real-touch zoom/reset, native-trigger marker for PLAY, and unit + existing-smoke checks proving viewport operations do not mutate sample data. Run its exact-SHA workflow and inspect the evidence before proposing integration.
2. In parallel with that isolated work, prepare the smallest evidence-based fix for the primary FAVORITE 1 picker capture: retain actual folder target/child/grant/restart assertions; diagnose why UIAutomator dumps stall after the picker transition instead of masking the failure.
3. Once the isolated SAMP-010 result is known and primary run can resume, make the primary SAF fix on PR #23 as one coherent batch, then finish Project SAVE / SAVE AS / OPEN acceptance.
4. Physical controller checks remain separate.

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

1. The latest primary run #3039 is RED on FAVORITE 1 DocumentsUI capture; do not record full Browser acceptance or begin Project lifecycle acceptance yet.
2. Build SAMP-010 on the isolated branch and require exact-SHA test results plus screenshot/XML evidence before considering integration.
3. Fix the primary Browser picker dump stall without removing current/target folder row, URI grant, child WAV, restart and persistence assertions.
4. Physical Akai MPC Studio MkII checks remain pending.

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
