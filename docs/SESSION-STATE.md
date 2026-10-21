# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** operational handoff; verify mutable claims against live GitHub.

## Latest source and verification state

### Live checkpoint — Android Build #3031 failure, 2026-10-21

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; `main` remains untouched. Tested HEAD: `cdbf0bcfcf0eeb9369f351bb889c88770e68d7d2`.
- Android Build #3031 / run `39618045814` failed at step 52 (Android emulator smoke). Run: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39618045814. Native/unit checks, APK assembly and evidence preflight passed; the failure occurred in FAVORITE 1's DocumentsUI provider-root stabilization.
- **SAMP-008 passed on this exact SHA/run:** real-touch CROP changed the 3,840-frame fixture to 2,840 frames after Start=1,000 and reset the native region to `0..2,840`. **SAMP-009 passed on the same exact SHA/run:** real-touch CH4/CH8/CH16 each asserted exact native per-pad ranges with contiguous, non-overlapping coverage of all 2,840 frames.
- The Browser/SAF failure is a test-harness defect, not evidence that the Favorite folder or grant is missing. The evidence archive's `mpc-groovebox-browser-favorite-tree-picker-uiautomator.xml` shows `BBB-MPC-Browser-Favorite` at row bounds `[0,434][1080,626]`; however, Android DocumentsUI emitted duplicate RecyclerView children where the same title bounds were paired with an off-screen `item_root` bound `[0,1394][1080,1586]`. The old whole-hierarchy signature never stabilized within 30 seconds. Do not relax real folder, child WAV, grant, or persisted-restoration assertions.
- The next coherent fix narrows only root stabilization to the actual target row, root header, directory and footer geometry; it rejects recycled title/row pairs whose bounds are inconsistent. The final target row must still be fully visible and is still tapped using measured bounds.
- Run #3031 artifacts: APK `12353167922` (9,957,677 bytes; SHA-256 `96a82e00ef1b998d3ecebc8ed0f3fbda06ae0f46eab4a331dbc0b8bca9c7456b`); UI evidence `12354283054` (9,476,640 bytes; SHA-256 `eb6ba7c2a1e4e04e60d268282989b32a882362c9060bba4dbb8e639913deaba3`). The evidence archive has no root screenshot because stability failed before capture; the picker XML is present and was inspected.
- Browser USER FOLDER selection and its persisted restore passed before the Favorite root attempt. Project SAVE / SAVE AS / OPEN lifecycle was **not reached** in #3031 and remains unaccepted. The script contains direct SAVE-overwrite, force-stop/relaunch/Open, cancellation, malformed-load, and placement-free v1 assertions, but they still require one green exact-HEAD run.
- Live `.github/workflows/android-build.yml` uses `cancel-in-progress: false`; preserve it. Physical MPC Studio MkII verification remains **PENDING**. SAMP-010 remains isolated on its own feature branch; do not mix it into PR #23.

## Current execution queue — 2026-10-21

1. Verify the target-aware DocumentsUI stabilization fix on its exact-HEAD Android run. Inspect any failure logs/artifacts before the next commit.
2. On a green full run, record FAVORITE 1 folder selection, real child WAV, folder grant, Browser result and persisted restoration.
3. On that same green full run, verify Project Save As → mutate → Open; direct SAVE overwrite changes the saved domain SHA; force-stop/relaunch/Open restores exact MIDI-event and Pattern-placement state; Save As/Open cancellations preserve state; malformed input preserves the loaded project; placement-free v1 loads with two tracks and zero placements.
4. Only after those acceptance gates pass, resume the highest-priority roadmap gap. Keep SAMP-010 isolated until PR #23's Browser/project acceptance is green. Hardware tests remain separate from emulator evidence.

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
