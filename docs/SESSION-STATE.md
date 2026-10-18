# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-18  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; working branch: `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is **OPEN**, base `fix/ui-shell-format-width-startup`; it remains unmerged and has not been retargeted.
- At the start of this checkpoint, branch HEAD was `1dd5742331642e34713636a1ac811752e5fb1bb9`. The current branch includes application changes beyond the old native-codec SHA `63a5e51e7b66e0e879513acd9c9ff32d545f434e`; the last commit being documentation-only does **not** mean the entire current branch is documentation-only.
- The versioned native Project codec v2 has a real filesystem save/load API and native round-trip tests for placements, MIDI event values, sample/program metadata, invalid input and the documented placement-free v1 fixture.
- **Project UI integration now exists in source:** `MainActivity.java` exposes Project SAVE, SAVE AS (Android `ACTION_CREATE_DOCUMENT`) and OPEN (`ACTION_OPEN_DOCUMENT`). Save serializes the current native Project; Open reads a bounded document and calls `nativeProjectLoad`. The native bridge validates a candidate Project before replacement, stops transport/audio as needed, clears runtime sample buffers, replaces the Project, clears incompatible edit history and the native Arrangement event clipboard, and then refreshes/restarts the application context. Sample PCM is not embedded; WAV data may need reloading/relinking.
- The first UI smoke scenario uses the real SAF picker: save a one-Track Project, add a Track in memory, open the saved file, then require the summary to return to the persisted one-Track/one-placement state. This proves more than native serialization alone, but it currently checks counts rather than every placement ID/timing field and complete MIDI state, and it does not force-stop/relaunch between save and open. Native codec tests cover detailed fields separately.
- Prior [Android Build #2808](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39121411497) succeeded on the earlier code SHA `63a5e51e7b66e0e879513acd9c9ff32d545f434e`; that run predates the newly wired Android Project route and must not be cited as UI-route acceptance.
- Android Build #2819 (`39136433515`) was **IN_PROGRESS** on the preceding HEAD `1dd5742331642e34713636a1ac811752e5fb1bb9` when this state was audited: native/JVM tests, APK assembly/upload and smoke-shell syntax had passed; the emulator smoke step was still running. That is not a green final result. Because this audit advances the PR branch, inspect the newly triggered exact-HEAD workflow and replace this note only with observed final status.
- The APK and visual evidence attached to #2808 are verified for that older code SHA, not proof of the current Project UI route. Check current run artifacts and SHA before claiming new acceptance.
- Physical MPC Studio MkII verification remains **PENDING**; emulator evidence does not stand in for a physical-controller test.

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

## Current execution order

1. Verify the newest exact-HEAD Android workflow and inspect its Project Save/Open logs, screenshots, accessibility hierarchy and APK; do not infer a final result from an in-progress run.
2. Harden the Project acceptance path: process stop/relaunch, exact placement tuples and MIDI-note fields, placement-free v1 load, and bounded malformed/cancel/error behavior. Keep sample PCM's external-reference limitation explicit.
3. After the persistence flow is accepted, select the next highest-value Arrange/Grid/Track/Step gap using the action catalog and current source/tests; do not reopen finished Arrangement placement work or repeat full firmware research.
4. Physical MPC Studio MkII input/feedback verification remains a separate hardware task; emulator checks do not close it.

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
