# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Latest verified implementation state

- Repository: MiguelDuval/mpc-mk2-groovebox; branch feature/mpc-one-ui-fidelity.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN, base fix/ui-shell-format-width-startup, not merged.
- Verified application commit: [ec41174ae729d6c4e7a13de8fd8171e80b904134](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/ec41174ae729d6c4e7a13de8fd8171e80b904134), adding native Pattern-backed Arrangement COPY/PASTE and fixing paste selection restoration across Undo/Redo.
- [Android Build #2789](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39068765934) completed **SUCCESS** on that exact SHA. Native/JVM/domain tests, PatternOps and edit-history tests, debug APK assembly, full Android Emulator smoke, and visual-evidence upload all passed.
- APK artifact: app-debug.apk, ID 12114811559, 8,656,477 bytes, SHA-256 32a474f2c4e5871c234fb4d0d1adab567b3d4e49ec6f816a86664dfe300cfe98. Downloaded and verified to be an Android package.
- UI-evidence artifact: mpc-ui-visual-evidence, ID 12114423332, 6,997,526 bytes, SHA-256 4cff56a245109f14c05a887769d4fc1d23744366a3abae6073f8d9e7c3e71d6c. Downloaded, archive integrity checked, and inspected. Arrangement COPY/PASTE/UNDO/REDO screenshots and accessibility hierarchies show source tick 7680, pasted/redo tick 7920, and final undo tick 7680. Smoke assertions confirm COPY is non-mutating, PASTE adds exactly one event, and Undo restores the exact native Pattern serialization.
- The same successful run retained earlier acceptances: Main XL geometry measured from live Android bounds at vertical 0.977 / horizontal 0.960 fill, legacy panel absent; Grid DRAW/ERASE and Step probability change passed; Track Edit TUNE changed +0.00 st → +1.00 st, persisted after re-entry, then was restored to +0.00 st.
- One existing Browser DocumentsUI test printed a timeout for Browser-Favorite-Only.wav in the remembered folder; the test's root-navigation fallback ran and the USER FOLDER / FAVORITE 1 assignment, preference isolation, and process-restart restoration subsequently passed. Workflow stayed green.
- Physical MPC Studio MkII verification is **PENDING**. These are Android Emulator screenshots/hierarchies, not physical-controller evidence.

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

## Current execution order

1. Promote the working selected-event COPY / PASTE / DUP operations into the canonical bottom Function Bar; the current screenshot shows these function slots still disabled even though real handlers exist in the secondary Arrangement action row. Tie enabled state and accessibility names to actual selection and native clipboard state.
2. Implement Arrangement CUT as a native Pattern mutation that captures the full selected event in the clipboard and removes only that event. Define collision/boundary behavior and rollback; register Undo/Redo. Add native tests and a real-emulator touch → CUT → UNDO → REDO → UNDO restoration that leaves the fixture exactly unchanged.
3. Re-run the full Android workflow for the exact resulting SHA. Keep MOVE ± GRID, DUP/COPY/PASTE, loop-brace, Grid and Step acceptance intact. Physical MPC Studio MkII verification remains a separate pending item.

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
