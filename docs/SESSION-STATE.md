# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

### Live checkpoint — 2026-10-21, update after Build #3009

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; `main` remains untouched. HEAD at this checkpoint: `73571a9c733fc21e2d01cd0cc288c7ca26218ba3`.
- Android Build #3009 / run `39585551501` failed on this exact HEAD: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39585551501. Source/unit checks, APK assembly, evidence preflight and SAMP-008/009 all progressed to the Browser/SAF acceptance.
- The explicit Sample Edit → Browser handoff added in `73571a9` passed: the log confirms Browser Search was reset after returning from Sample Edit. SAMP-008 CROP changed 3,840→2,840 frames and reset the range to `0..2,840`; SAMP-009 CH4/CH8/CH16 produced exact contiguous slices across all 2,840 frames.
- Current failure is later in real FAVORITE 1 Android DocumentsUI selection. The initial `wait_for_ui_text` had only a 4-second total budget; slow UIAutomator hierarchy transfer exhausted the window, leaving no usable XML, and the smoke reported that DocumentsUI had closed before the folder child was available. Do not loosen folder/permission checks. The fix caps each hierarchy dump attempt at 6 seconds, retries within an overall window, extends the initial tree-child waits to 15 seconds, and extends the exact “Use this folder” action budget.
- Run #3009 artifacts: APK `12339469482` (9,957,677 bytes); evidence `12340582239` (9,442,912 bytes, SHA-256 digest `146bfa9c62f81aea1dbf0ec17dc2a45631ffd4ee1b1e5f092053dc1bced5e2cb`).
- Project SAVE / SAVE AS / OPEN acceptance was not reached. It remains required on a green exact-HEAD smoke, including canonical state equality for MIDI events and Pattern placements, force-stop/relaunch/open, cancellation state invariance, malformed-input preservation, and placement-free v1 compatibility.
- `.github/workflows/android-build.yml` now uses `cancel-in-progress: false`; preserve this so new pushes cannot cancel the active critical run. Physical MPC Studio MkII verification remains **PENDING**.

## Current execution queue — 2026-10-21

1. Run the bounded DocumentsUI hierarchy retry fix through the complete exact-HEAD Android workflow. Keep all real picker, child-file, folder grant and persistence assertions strict.
2. While the full workflow runs, work on the separate SAMP-010 waveform zoom/audition slice on an isolated branch only. Do not merge it into PR #23 until the current acceptance run has a conclusion and its result is handled.
3. On a green exact-HEAD workflow, record Project SAVE / SAVE AS / OPEN acceptance from the same run. If any later assertion fails, diagnose that exact failure and repeat the smallest root-cause fix.
4. Physical MPC Studio MkII tests stay separate from emulator evidence.

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

1. Repair the explicit Sample Edit → Browser handoff used by the smoke after SAMP-008/009; keep the native frame/range assertions intact and inspect the fresh screenshot/XML.
2. Re-run the complete Android workflow on the resulting exact HEAD. Browser USER FOLDER/FAVORITE restoration and the full Project SAVE / SAVE AS / OPEN acceptance are still part of the current acceptance boundary.
3. After the current-SHA full workflow passes, choose the next product slice from the action catalog; SAMP-010 waveform zoom/audition remains next, not before full acceptance.
4. Controller traceability documentation is complete for the current research pass, but physical MPC Studio MkII verification remains pending.

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
