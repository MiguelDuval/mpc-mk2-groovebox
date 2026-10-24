# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-24  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified full exact-HEAD acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`. Current verified HEAD: `985fdd3df616b0d0c0a284282527fbb15540e069`.
- **Android Build #3156 / run [40100660011](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40100660011) PASSED on exact SHA `985fdd3df616b0d0c0a284282527fbb15540e069`.** All workflow steps completed successfully, including APK assembly, full emulator smoke (step 52) and visual-evidence upload (step 53).
- APK artifact ID `12562642906`, size 10,009,933 bytes, SHA-256 `e787429c0338397ab9defa076acc5a8e9599a6dd5b4f199cc971601859a6224c`. Visual-evidence artifact ID `12563432229`, size 15,202,931 bytes, SHA-256 `34c9d6bb91561cd3a4b0c5f5da43898a8225048a41d638e82bcdc172fb8b0615`.
- Accepted smoke coverage includes Browser USER FOLDER/FAVORITE 1 and DocumentsUI restoration, Project save/open/overwrite/restart/recovery and malformed-load preservation, Arrangement placement operations, SAMP-008/009 and SAMP-010 waveform viewport/audition invariants.
- `.github/workflows/android-build.yml` retains `cancel-in-progress: false`. Physical Akai MPC Studio MkII validation remains a separate pending hardware gate.

## Current execution queue — 2026-10-24

1. **PR #30 — Project SAF failure phases.** Original code head `eeef2a864126bca34e08410b063a9f1f3ec36287` passed Build #3129. The branch has documentation-reconciliation commits and must incorporate primary HEAD `985fdd3df616b0d0c0a284282527fbb15540e069` through a normal two-parent merge commit. Verify the resulting SHA and mergeability; the merged head requires its own full exact-SHA CI. Do not retarget or rewrite history.
2. **PR #57 — provider output-open classification.** Head `d1b7b8b49da2fd7899b9457a82e37c6a80faafe6` passed Android Build #3165 / run [40109297128](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40109297128): focused Android policy tests, APK, full emulator smoke and visual-evidence upload all succeeded. APK artifact `12565778824`; visual evidence artifact `12566209009`. This PR is stacked on #30; recheck its dependency after #30 advances.
3. **PR #56 — Browser Show file size.** Head `8a672ba9fc0ec44bd6b71f088506a35229e958b0` passed Android Build #3162 / run [40108676401](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40108676401), including complete emulator smoke and visual-evidence upload. The storage-picker action is measured after a real pre-draw/layout pass; strict hit-target assertions remain enabled.
4. **PR #55 — low-pass filter state.** Head `d4c9324270548ef47b132fa9b655e9a0aadc2717` passed Android Build #3158 / run [40108527287](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40108527287), including native/JVM checks, full emulator smoke and evidence upload. The transition test resets state before the separate impulse/decay assertions.

Physical-controller acceptance remains separate. Never modify `main`, rewrite history, or merge PRs without authorization.

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
