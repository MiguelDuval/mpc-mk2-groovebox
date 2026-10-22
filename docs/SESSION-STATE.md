# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** live handoff, governed by the current branch HEAD and exact-SHA Actions result.

## Last fully verified baseline

- Branch: `feature/mpc-one-ui-fidelity`; PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23), base `fix/ui-shell-format-width-startup`.
- Baseline HEAD before the current shortcut-persistence slice: `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`.
- Full Android Build [#3073 / run 39823061552](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39823061552) completed **success** on that exact SHA. The run included every workflow job step, Android emulator smoke and visual-evidence upload.
- Build #3073 confirms the real SAF Project flow: v2 canonical state including MIDI events and Pattern placements, direct SAVE overwrite, force-stop/relaunch/open, SAVE AS and OPEN cancellation invariance, malformed-load preservation and compatible placement-free v1 equality. Main hierarchy acceptance also passed for MAIN, MENU, PROJECT and SEQ.
- The two latest Main fixes are verified on that baseline: Program Select is disabled/truthfully captioned for unsupported Track Types, and Main/Sequence Select use the selected Sequence's stored name with stable-ID fallback only for empty names.
- Recent run context: #3072 (SHA `9b930da...`), #3071 (`53ef953...`) and #3070 (`d84e8fc...`) were cancelled; #3069 succeeded on older SHA `2d992807ebc3d545598cc35c9ea15991e09738dd`; #3068 failed on its earlier static preflight. None supersedes exact-SHA Build #3073 as the latest green baseline.

## Current implementation slice — five-slot shortcut persistence

- The five-slot Shortcut Rail already supported assignment, reordering and factory reset, but `MpcNavigationController` initialized every process with factory defaults and `MainActivity` never saved user edits. A valid customization therefore disappeared on the next process start.
- This slice adds a strict, versioned preference payload (`shortcut_modes_v1`), saves assignment/reordering/reset through the actual Shortcut configuration UI, and restores all five modes before shell construction. Malformed/stale values safely fall back to factory order.
- JVM coverage round-trips a custom five-slot ordering, rejects malformed/unsupported modes without partial state mutation, and verifies safe fallback. The focused Gradle unit gate and Android-smoke source-contract preflight include the new wiring.
- **Acceptance boundary:** the baseline Build #3073 does not verify this new change. Use the live PR head SHA and the Actions run triggered by the commit containing this checkpoint. Report a green result only after the full Android emulator smoke and artifact-upload steps pass. A dedicated UI-driven customization → force-stop → relaunch assertion is still a useful additional acceptance refinement; do not confuse existing Browser URI persistence coverage with shortcut preference coverage.

## Next actions

1. Refresh PR #23 HEAD and its exact-SHA workflow. Confirm the current commit SHA in the run metadata before interpreting the result; do not reuse Build #3073 as evidence for this new slice.
2. If the workflow is green, the next functional gap from the action catalog is **PROJ-001 — a real New Project workflow**. The Menu's NEW PROJECT function is currently truthfully disabled; implement safe model replacement and state/history reconciliation rather than wiring it to a decorative reset. Confirm unsaved-work handling before enabling it.
3. Keep PRs #28 and #29 isolated; do not change their bases or merge them as part of this slice. Physical Akai MPC Studio MkII acceptance remains pending and is not proved by emulator CI.

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

## Superseded execution note — 2026-10-21

The previous FAVORITE 1 / Project persistence queue is historical. Build #3073 superseded that pending state on exact SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`: the full Android workflow passed, including Project persistence acceptance. Use the current queue at the top of this file for next steps.

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
