# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** live handoff, governed by current branch HEAD and exact-SHA Actions evidence.

## Last fully green baseline

- Branch: `feature/mpc-one-ui-fidelity`; PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23), base `fix/ui-shell-format-width-startup`.
- **Latest full green workflow: [Build #3077 / run 39846737408](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39846737408), exact SHA `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`.** It passed all 57 workflow steps, including complete emulator smoke and visual evidence upload. The prior fully green persistence baseline is Build #3073 / run 39823061552 at `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`.
- Build #3073 passed the complete Android workflow, including debug APK, emulator smoke and visual evidence. Project persistence accepted exact v2 state, direct SAVE overwrite, process restart/reopen, Save As/Open cancellation invariance, malformed-load preservation and compatible placement-free v1 state. Main/Menu/Project/SEQ hierarchy checks also passed.
- Main fixes verified at that SHA: Program Select is disabled/truthfully captioned for unsupported Track Types; Main and Sequence Select display the selected Sequence's actual stored name, with stable ID only as empty-name fallback.
- Build #3074 / run [39829301517](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39829301517) failed on `dc4ab44dce08d0e1caf056f7a7f829a72f46cda5` because the shortcut-mode validator checked the 4×4 Menu tile catalog and rejected factory-promoted SOUNDS/XY shortcuts. The fix uses `MpcModeRegistry.shortcutEntries()`; subsequent focused JVM tests passed.
- Build #3075 / run [39830122978](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39830122978) failed on exact SHA `e7ab67ded8926113ebc62d0d4aba0030b4b3560e` at the first Android smoke source preflight because the shortcut encoder call is line-wrapped; the checker was corrected in the next slice.
- Build #3076 / run [39832727885](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39832727885) failed on exact SHA `a5b81320fa5cb43fb0d667a7811a2b80fc635ae1` late in Android smoke. Focused JVM/native checks, APK assembly/upload and smoke syntax/evidence-contract validation passed, but the emulator stopped when a one-shot UiAutomator dump timed out while opening Menu for the new acceptance block (bounded `adb shell uiautomator dump`). This occurred before New Project's Cancel/reset/unlinked assertions; it did not establish a product defect. The follow-up uses already-captured measured Android View bounds for in-app taps and reserves retried UiAutomator dumps for the native AlertDialog.

## Current engineering slice — persistent Shortcut Rail + PROJ-001 New Project

- Five-slot shortcut assignment, reordering, and factory reset are stored synchronously in versioned local preferences (`shortcut_modes_v1`) and restored before shell construction. Invalid/stale values safely revert to factory order.
- Shortcut controller JVM tests cover exact-order round trip, constructor restore, malformed/stale fallback, and atomic rejection of an invalid bulk update. A **real UI customize → force-stop → relaunch** check remains an explicit acceptance gap; Browser URI restart coverage is unrelated.
- This change adds a real **NEW PROJECT** Function Bar operation, replacing the disabled placeholder. The dialog offers CANCEL (must preserve exact canonical state SHA), SAVE FIRST (save or open SAVE AS, without resetting), and DISCARD & NEW. The native reset stops transport/recording and audio, clears runtime sample projection, stale record-queue events, edit history, arrangement clipboard and playback session, restores the canonical default model and transport position, then the Activity resets selections and clears the old current-document URI. The previous document is never deleted or silently overwritten.
- Native model tests assert the restored default Program/Sequence/Track/Pattern/placement topology, empty MIDI notes/samples, selection indices and sample-ID allocator. The extended Android smoke drives Cancel and compares the exact state SHA, confirms reset/summary, verifies Main renders `Sequence 01`, then confirms Project mode says `NO FILE LINKED`.
- Build #3075's preflight matcher defect is fixed in this same coherent slice. The new code and end-to-end UI acceptance remain **pending verification on the exact commit SHA produced by this checkpoint**; do not treat any previous build as evidence for them.

## Next actions

1. Build #3077 / run 39846737408 passed the full workflow on `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`, including the New Project Cancel/reset/unlinked-document checks and evidence upload.
2. Next implementation slice: add real UI-level Shortcut Rail customization → force-stop → relaunch acceptance (assignment/order persistence, five visible destinations, factory reset). Existing JVM preference round-trip tests alone do not prove the Activity wiring.
3. After Shortcut Rail restart acceptance, implement and test **PROJ-002/003 provider write-failure recovery and two-document isolation**, keeping the already-green canonical persistence invariants.
4. PRs #28 and #29 remain isolated. Physical Akai MPC Studio MkII acceptance is still pending; emulator results do not prove hardware input behavior.

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
