# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify mutable status against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; branch `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN**, base `fix/ui-shell-format-width-startup`, not merged.
- Latest code-bearing commit verified by the full workflow: [`f49748de9db450ffc3b3b781befac99916f2a134`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/f49748de9db450ffc3b3b781befac99916f2a134), implementing the explicit Arrangement insertion cursor for empty-timeline PASTE.
- [Android Build #2793](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39075574304) completed **SUCCESS** on that exact SHA. All 57 workflow steps passed, including native/domain/JVM tests, APK assembly, full Android Emulator touch smoke and evidence upload.
- APK artifact: `app-debug.apk`, ID `12117086509`, 8,746,381 bytes, SHA-256 `a47c767a9c476fe696c75b6dbba0922c1b9a8d8830d704b4555abc29249ee8fc`. Downloaded, recognized as an Android package and ZIP-integrity checked.
- UI-evidence artifact: `mpc-ui-visual-evidence`, ID `12116759419`, 8,382,189 bytes, SHA-256 `7074520600906be7a5cf3c348da795f7426c94e4f60613fbffcb57f076cdf5df`. Archive integrity passed; cursor, pasted, restored screenshots and accessibility hierarchies were inspected.
- Arrangement acceptance: MOVE ± GRID; DUP; COPY/PASTE; CUT/UNDO/REDO; and CUT → empty cursor tick 240 → PASTE → UNDO → REDO → UNDO → undo CUT all passed against authoritative native Pattern serialization. The inserted event retained duration, pitch, velocity, probability, ratchet and nudge; final Undo restored exact starting Pattern and source selection.
- Other acceptance stayed green: Main XL strips fill 0.977 vertical / 0.960 horizontal of their shell region, legacy panel absent; Grid DRAW/ERASE, Step probability 127→126, Track Edit tuning +0.00 st→+1.00 st and restoration, and loop-brace touch/restore passed.
- One Browser DocumentsUI lookup timed out for `Browser-Favorite-Only.wav` in the remembered folder, then its root-navigation fallback succeeded. USER FOLDER / FAVORITE 1 permissions, preference isolation, and cold process restart restoration all passed; workflow remained green.
- Physical MPC Studio MkII verification remains **PENDING**. All results in this checkpoint are Android Emulator evidence.

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

The Arrangement remains a first-Pattern-per-Track projection until a real linear clip-placement model is implemented.

## Current execution order

1. Inspect the existing Sequence/Track/Pattern data model, project save/load path, native edit history and Arrangement projection before introducing explicit linear Pattern placements.
2. Add the smallest backward-compatible Pattern-placement domain object and unit tests, then implement one real user path: create/place a Pattern on a Track timeline, move it by one quantize-grid interval, and undo/redo with exact persistence restoration.
3. Add real-emulator touch evidence for the new placement path and run the full Android workflow on the exact final SHA. Keep physical MPC Studio MkII validation separately marked pending.

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
