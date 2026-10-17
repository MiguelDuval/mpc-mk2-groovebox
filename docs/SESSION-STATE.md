# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; branch `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is **OPEN**; base `fix/ui-shell-format-width-startup`; HEAD [`af266a38bb850a20313bccfef9a7b953c4191151`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/af266a38bb850a20313bccfef9a7b953c4191151).
- [Android Build #2781](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39062512475) completed **SUCCESS** on that exact SHA. Android native/JVM checks, PatternOps tests, debug APK assembly, emulator UI smoke, and visual-evidence upload passed.
- APK artifact: `app-debug.apk`, artifact ID `12112066456`, 8,379,757 bytes, digest `sha256:15e422455360d96ada944a0d1c4c42ffbd9cb5acc6c951d2c53de1a6fa4dc6f9`.
- UI-evidence artifact: `mpc-ui-visual-evidence`, artifact ID `12111553593`, 5,588,247 bytes, digest `sha256:b87731e92e4da3fb2e1a9f6bd250c4ffb3d28d0c77afcff24389260e3f416e8f`. It was downloaded and inspected. The Arrangement selected/moved/restored screenshots and XML show `TRACK 01 • TICK 7680 • NOTE 036` → `TICK 7920` → `TICK 7680`.
- The log reports a short initial timeout looking for `Browser-Favorite-Only.wav` inside the remembered DocumentsUI folder; the scripted fallback navigates to provider root, and the subsequent favorite assignment, isolated preference and process-restart restoration test passed. The overall workflow is green.
- Physical MPC Studio MkII verification remains **PENDING**. All results above are Android Emulator evidence, not physical-controller evidence.

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

1. Extend Arrangement editing beyond one-grid movement with the smallest safe, model-backed duplicate or copy/paste operation for a selected MIDI event. Reuse the same native Pattern and edit-history path; define collision and sequence-boundary behavior before wiring controls.
2. Add a real-emulator touch round-trip with authoritative native-state checks and safe fixture restoration; retain the passing MOVE ± GRID and loop-brace acceptance.
3. Recheck live PR #23 HEAD/base and run the full Android workflow at the exact final SHA. Keep physical-controller validation separately marked pending.

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
