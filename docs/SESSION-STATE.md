# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify mutable status against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; branch `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is **OPEN**, base `fix/ui-shell-format-width-startup`, not merged.
- Latest code-bearing commit verified by full CI: [`830e47f2eddcf46bccbb5333de5edc9c402c39ce`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/830e47f2eddcf46bccbb5333de5edc9c402c39ce). It adds native Arrangement CUT and routes CUT / COPY / PASTE / DUP into the canonical bottom Function Bar.
- [Android Build #2791](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39072384402) completed **SUCCESS** for that exact code SHA: all 57 workflow steps passed, including native PatternOps/edit-history tests, debug APK assembly, the full Android Emulator touch smoke, and UI evidence upload.
- APK artifact: `app-debug.apk`, ID `12116190078`, 8,746,381 bytes, SHA-256 `a302c6244c859eaa3acd10fb07e029fe2f781385c955bb903a3258d87ecd9076`. It was downloaded, recognized as an Android package, and its internal ZIP integrity passed.
- UI-evidence artifact: `mpc-ui-visual-evidence`, ID `12115971268`, 7,427,085 bytes, SHA-256 `a1737612ca1510ca73b35d10a2702344a8b51a2ae19b7e1214f8569561fa21fc`. The archive integrity passed and the Arrangement CUT/COPY/PASTE/DUP screenshots and XML were inspected.
- Arrangement evidence: MOVE ± GRID restores Track 01 / tick 7680 / note 036; DUP and COPY/PASTE each pass their Undo/Redo round trips; CUT removes only that source event, Undo restores it, Redo removes it, and final Undo returns the exact serialized Pattern and selection.
- Existing acceptance also stayed green: Main XL strips fill 0.977 vertical / 0.960 horizontal of the shell region, legacy panel absent; Grid DRAW/ERASE, Step probability 127→126, and Track Edit tuning +0.00 st→+1.00 st then restoration all passed.
- A Browser DocumentsUI lookup timed out once for `Browser-Favorite-Only.wav` in the remembered folder. Its root-navigation fallback succeeded; separate USER FOLDER / FAVORITE 1 tree grants, preference isolation, and cold process-restart restoration all passed.
- Physical MPC Studio MkII verification remains **PENDING**. These are emulator results, not physical-controller evidence.

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

The selected-event DUP, COPY and PASTE operations remain backed by native PatternOps and native edit history. CUT now copies the full unique event into the native clipboard and removes only that Pattern event after history registration succeeds. The four actions live in the bottom Function Bar and are enabled only when their required event/clipboard state exists; UNDO/REDO and EVENT ± GRID remain in the secondary action row.

Build #2791 performed real touches and checked authoritative native Pattern serialization:
- DUP → UNDO → REDO → UNDO;
- COPY → PASTE → UNDO → REDO → UNDO;
- CUT → UNDO → REDO → UNDO.

The last sequence returned Track 01 / tick 7680 / note 036 and exact original Pattern serialization. Build #2791 completed all 57 workflow steps successfully on code SHA 830e47f2eddcf46bccbb5333de5edc9c402c39ce. The APK and screenshot/accessibility artifact archives were downloaded, integrity-checked, and inspected. Full linear clip placement remains intentionally unimplemented.

## Current execution order

1. Add an explicit paste-target cursor for empty Arrangement timeline positions. Snap to the current quantize grid, show a marker/accessibility label, and keep event-only actions disabled while the cursor is the focus. This closes the current limitation where PASTE requires an existing selected event.
2. Extend real-emulator coverage for CUT → empty-timeline target → PASTE → UNDO/REDO and final CUT restoration. Cover Pattern boundaries/collisions and preserve every MIDI event field and exact native state.
3. Inspect the exact-HEAD full Android workflow/APK/evidence before considering the next slice complete. Keep physical MPC Studio MkII validation marked separately as pending.

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
