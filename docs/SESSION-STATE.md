# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** current handoff checkpoint; reconcile status against exact-SHA GitHub evidence before each change.

## Latest source and verification state

### Live checkpoint — Project persistence E2E green; hardening picker evidence flake

- Primary PR #23 before this correction was `0d200c6e21e314ecdca810d727f2048d9eaef48b`. Android Build #3062 / run `39793668041` reached the extended real Project lifecycle after earlier Browser/Track Edit/Arrangement/Sample checks passed.
- The cold force-stop/relaunch itself passed: the new process emitted ready-state Main hierarchy evidence without reseeding the fixture. Reopening the directly overwritten SAF document restored `TRACKS=2`, `PLACEMENTS=2`, and exact v2 file/domain digest `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`. Save As cancellation and Open cancellation preserved that digest.
- Build #3062 then failed at the malformed-document leg due to a **test-helper outcome mismatch**. `open_project_document` unconditionally waited for `PROJECT_OPEN_COMPLETE`, but the deliberate malformed file correctly emits `PROJECT_OPEN_REJECTED` and must remain on `currentPage=PROJECT`. Runtime logs confirm `PROJECT_LOAD_FAILED • Invalid project header` and unchanged state SHA, but the wrong wait marker prevented the following atomicity assertion from running.
- The current correction makes the helper accept an explicit expected outcome marker. Normal opens require `PROJECT_OPEN_COMPLETE` and capture the fresh Main tree; the malformed-file call requires `PROJECT_OPEN_REJECTED` and leaves Project-route verification to the existing caller. No native behavior is changed and no acceptance assertion is weakened.
- The smoke-script preflight now checks for that explicit malformed-file marker route as well, so a future helper regression fails the inexpensive syntax/evidence-contract step before starting the emulator.
- Build #3063 / run `39801798022` passed all 52 workflow steps on exact SHA `bc61127f0e13eff18ee8460d3da95180878a07e5`. Its final runtime marker confirms: exact v2 state, direct SAVE overwrite, process restart, Save As/Open cancellation invariance, malformed-load preservation and placement-free v1 canonical-state equality all passed in that run.
- Build #3064 / run `39802124327` on descendant SHA `6adb04f28e62e23d6d8c27da66cc187e1c905997` failed before it could re-run the Project acceptance sequence: `wait_for_project_picker_item` called `capture_stable_picker_state(..., 8)`, whose protocol requires three matching fresh trees and a paired screenshot. The log shows only one attempt; a bounded dump completed too late to transfer within the remaining 2-second deadline. This is a flaky evidence-capture budget, not evidence of Project-state corruption.
- This change raises only the bounded stability window for Project picker captures from 8 to 30 seconds. Every accepted tree must still be fresh and the three signatures must still match; the assertion and real DocumentsUI path are unchanged. Re-run the full workflow on the resulting exact HEAD before treating that HEAD as green.
- While Build #3065 runs for the picker timeout fix, inspection of the successful Build #3063 Main screenshot/XML found two related visible state inconsistencies: the central Track row retained its pre-startup `Track 01` placeholder, while the XL strip correctly showed `DRUMS`; the central Track-owned Program field showed `NONE`, while the XL strip showed `Drum Program 1`. Source review confirmed startup completion refreshed other shell components but did not refresh Main's Sequence/Track fields. Commit `3c4da19` now calls `refreshMainModeFields()` after native startup on Main and asserts the Program label equals `nativeSequenceGetTrackProgram(selectedTrack)`. This follow-up adds the complementary runtime assertion that the central Track label matches `nativeSequenceTrackStatus(selectedTrack)`. Neither change mutates the model; both require exact-SHA CI verification. The smoke-script source-contract preflight now also requires both `MAIN_TRACK_CONTEXT_VERIFIED` and `MAIN_PROGRAM_CONTEXT_VERIFIED` marker strings, so later edits cannot silently remove either native-state equality assertion. This preflight guard needs the next current-head run.
- Build #3060 / run `39792199195` on `c56c16be9deb0f6d87664bd6c560237a7cfa6bee` failed earlier while trying to capture a stable DocumentsUI state; Build #3061 was cancelled after the branch advanced. The per-dump timeout change in `0d200c6` was included in Build #3062, which passed the force-stop/relaunch and canonical reopen portion before the malformed-file assertion routing defect.
- Full Project SAVE / SAVE AS / OPEN acceptance is **VERIFIED on exact SHA `bc61127f0e13eff18ee8460d3da95180878a07e5` (Build #3063)**, including malformed-load atomicity and placement-free v1 compatibility. The current descendant HEAD still needs a complete green rerun because Build #3064 hit the picker-evidence timeout described above. PRs #28/#29 remain isolated until the current primary HEAD passes; physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-22

1. Re-run full smoke with the 30-second bounded Project picker stability capture; confirm the three-tree + screenshot protocol survives Android 16 timing.
2. Verify both central Main Track identity and Program fields match the native selected Track immediately after startup; Build #3063 screenshot/XML exposed both stale placeholders.
3. If current HEAD is green, proceed with the next documented MPC One UI-fidelity slice without reopening passed persistence work absent regression evidence. Keep PRs #28/#29 isolated until current primary HEAD passes.

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

1. Re-run primary FAVORITE 1 with dual-format `ps` fallback and explicit runner PID diagnostics; inspect real picker root screenshot/XML and keep grant/restore assertions strict.
2. Check isolated exact-SHA Builds #3043 (SAMP-010) and #3044 (Project malformed-load unit regression) before moving either branch again.
3. After Favorite 1 passes, validate full Project SAVE/SAVE AS/OPEN lifecycle; integrate SAMP-010 only once its own acceptance and primary gates are green.
4. Physical MPC Studio MkII testing remains a separate pending gate.

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
