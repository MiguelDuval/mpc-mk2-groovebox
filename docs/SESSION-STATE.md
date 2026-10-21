# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

### Live checkpoint — 2026-10-21, after Build #3013

- Repository `MiguelDuval/mpc-mk2-groovebox`; PR #23 remains **OPEN**, branch `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`. `main` remains untouched. HEAD tested by Build #3013: `d87b0e70ea9a924b26a7e45527737918411fe485`.
- Android Build #3013 / run `39597577142` failed during FAVORITE 1 acceptance: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39597577142. Unit/native tests, APK build, screenshot-contract preflight, Browser-return/Search reset, SAMP-008/009, and Browser USER FOLDER selection/restoration all passed before the failure.
- SAMP-008 native acceptance: 3,840-frame source, Start=1,000, CROP result=2,840 frames, native region reset to `0..2,840`. SAMP-009 CH4/CH8/CH16 all assigned exact contiguous source ranges, no gaps or overlaps.
- FAVORITE 1 was tapped through the real Browser action and Android DocumentsUI became foreground. A single initial `dump_device_ui` then timed out while transferring the hierarchy; because no fresh XML was available, `wait_for_ui_text` could not see the real child and the test exited before folder selection. This is a hierarchy-capture failure, not proof DocumentsUI was closed or that the folder grant was rejected.
- Fix now applies the existing `dump_device_ui_retry` to the first post-picker hierarchy and the post-folder-selection hierarchy, with up to four separate bounded captures. On failure, it retains a current screenshot and logs the foreground Activity; the actual file, picker, Use This Folder, persisted grant and restoration assertions remain unchanged.
- Build #3013 artifacts: APK `12344324949` (9,957,677 bytes); visual evidence `12344838626` (9,428,123 bytes; SHA-256 `91d01fc74f609745e172b36a3bbc21e57a9a19e266f7e15be4b4fcd144971c91`). The archive was inspected; the missing picker XML was caused by the failed fresh hierarchy transfer.
- Project SAVE / SAVE AS / OPEN acceptance was not reached by #3013; it remains in the smoke queue. Required acceptance still includes exact persisted domain digest, force-stop/relaunch/open, SAVE AS and OPEN cancellation invariance, malformed-load preservation and placement-free v1 canonical-state equality.
- `.github/workflows/android-build.yml` must remain `cancel-in-progress: false`. SAMP-010 work is isolated in draft PRs #25/#26; no pass is claimed yet. Physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-21

1. Run the first-picker hierarchy retry fix through the full exact-SHA Android workflow. Confirm real FAVORITE 1 folder child selection, Use This Folder grant, returned WAV, process restart and persisted result.
2. Continue Project SAVE / SAVE AS / OPEN acceptance only after Browser USER FOLDER/FAVORITE restoration passes on the tested SHA.
3. Keep SAMP-010 zoom/audition validation isolated in draft PRs #25/#26 while critical PR #23 checks are in flight; integrate only after the base acceptance reaches a known result.
4. Physical controller verification stays separate from emulator proof. No changes to `main`.

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
