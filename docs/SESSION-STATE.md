# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable claims against live GitHub.

## Latest source and verification state

### Live checkpoint — corrective harness recovery, 2026-10-21 16:34 UTC

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`. Before corrective recovery the remote HEAD was `04378e76ebe3c485f69b77b526a5dccec85f50b8`; `main` remains untouched.
- Latest completed full workflow at root-cause triage: Android Build #3035 / run `39623619559`, exact SHA `fe0742cc2ceaf21b6b5589c1a07553839f7431f2`, failed at emulator smoke step 52: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39623619559. Native/unit tests, APK build and evidence preflight had passed on that run.
- On #3035, SAMP-008 CROP changed 3,840 → 2,840 frames after Start=1,000 and reset the range to `0..2,840`; SAMP-009 CH4/CH8/CH16 verified contiguous native frame coverage; Sample Edit → Browser and Browser Search reset also passed.
- Root failure was a one-shot UIAutomator hierarchy dump after a real DocumentsUI scroll. Run #3035 evidence artifact `12357687535` (9,423,739 bytes, SHA-256 `425da1d23a3c200648b76f1716264c4bd5e5abb37099d0c4fa459dd0b7d5f098`) was downloaded; `mpc-groovebox-browser-favorite-tree-picker.png` and the logged picker XML were inspected. They show the system folder picker, not an app/provider rejection.
- **Critical corrective note:** the intervening commit `04378e76ebe3c485f69b77b526a5dccec85f50b8` unintentionally published only the retry-helper fragment instead of the complete 583 KB smoke script, and shortened the roadmap. Its exact-SHA Build #3036 (`39635700353`) is active and must not be counted as acceptance evidence. This corrective commit restores the full parent script and roadmap, reapplies the scratch-preserving retry fix and retains the full historical records. Do not reset/rewrite history; use this normal forward correction.
- The intended fix makes `dump_device_ui_retry` publish a new hierarchy from a scratch path only after a valid parseable dump, and routes the FAVORITE 1 folder-scroll refresh through the existing retry helper. Real visibility/touch/child-WAV/tree grant/persistence assertions are unchanged.
- Build #3035 APK artifact `12355519965` (9,957,677 bytes; SHA-256 `bce4114b3ed65171ffa76699d80f95bfe8d5db88251b0077d07df35911e6e30c`). Project SAVE / SAVE AS / OPEN acceptance has not been reached and is still mandatory on a green exact-HEAD workflow. `cancel-in-progress: false` remains in effect; physical MPC Studio MkII acceptance remains **PENDING**.

## Current execution queue — 2026-10-21

1. Verify the corrective commit preserves the complete smoke script (greater than 10,000 lines), the full roadmap/history and all original acceptance assertions; let Build #3036 finish naturally, then inspect the new exact-SHA workflow attached to the corrective commit.
2. Complete FAVORITE 1 folder-row selection, exact child WAV, URI grant, Browser results and persisted restoration without lowering assertion strength.
3. On the same green full run, complete Project SAVE / SAVE AS / OPEN acceptance: canonical MIDI-event and Pattern placement equality, direct SAVE overwrite, force-stop/relaunch/open, cancellation invariance, malformed-load preservation and placement-free v1 compatibility.
4. Keep SAMP-010 isolated until the full Browser/project acceptance is green; physical controller checks remain separate.

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
