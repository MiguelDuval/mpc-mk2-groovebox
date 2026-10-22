# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** live handoff, governed by current branch HEAD and exact-SHA Actions evidence.

## Last fully green baseline

- Branch: `feature/mpc-one-ui-fidelity`; PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23), base `fix/ui-shell-format-width-startup`.
- Latest green Android workflow before the current Shortcut Rail UI-acceptance slice: [Build #3078 / run 39851914811](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39851914811), exact SHA `8c19821cc909ecd5e2e7b7de4a06f27d078f8185`; all 57 workflow steps passed. This is a docs-only descendant of the product code accepted by Build #3077.
- Product-code baseline for New Project acceptance: [Build #3077 / run 39846737408](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39846737408), exact SHA `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`. It passed full emulator smoke, including New Project Cancel state-SHA equality, DISCARD & NEW native reset, default Sequence identity, unlinked Project document and visual evidence upload.
- Earlier full persistence baseline: [Build #3073 / run 39823061552](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39823061552), exact SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`. Real SAF Save As, direct SAVE overwrite, v2 canonical state with MIDI events and Pattern placements, process restart/reopen, cancellation invariance, malformed-load preservation and compatible placement-free v1 equality all passed.
- Main fixes verified in that product-code history: unsupported Track Types cannot use Drum Program Select; Main and Sequence Select display the actual model Sequence name, with stable ID only as empty-name fallback.
- Build #3074 / run [39829301517](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39829301517) failed on `dc4ab44dce08d0e1caf056f7a7f829a72f46cda5` because the shortcut validator used Menu entries and rejected promoted factory SOUNDS/XY; fixed by using `shortcutEntries()`.
- Build #3075 / run [39830122978](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39830122978) failed smoke preflight because it expected a line-wrapped encoder invocation on one line; that matcher was corrected.
- Build #3076 / run [39832727885](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39832727885) failed before New Project acceptance due to a one-shot UiAutomator dump timeout. Build #3077 passed after in-app taps were derived from measured Android View bounds and retry was reserved for the native dialog.

## Current engineering slice — real Shortcut Rail UI persistence acceptance

- Shortcut assignment, reorder and RESET DEFAULTS write the versioned `shortcut_modes_v1` value synchronously and restore before shell construction. JVM tests already cover exact-order serialization, constructor restoration, malformed/stale fallback, and atomic invalid updates.
- The current slice adds smoke-only preference read/write markers, accessibility descriptions for Edit Shortcuts, reorder controls and Reset Defaults, and a real in-app View-tree capture of the Shortcut editor.
- The extended emulator path assigns GRID using the actual spinner, moves a slot through the real ▲/▼ button, verifies the exact persisted ordered mode list, force-stops/relaunches and verifies all five visible clickable rail roots in the real Main View tree, then resets factory order and repeats after a second process death.
- Acceptance for this new UI path is **pending exact-SHA CI**. Do not infer it from controller JVM tests or existing Browser/Project restart tests.

## Next actions

1. Verify the exact current PR head and its Android Build. Require focused JVM/native checks, APK assembly, smoke syntax/evidence preflight, both Shortcut Rail process-restart acceptance paths, and visual evidence upload on that same SHA.
2. Fix any failure at its actual root cause; retain ordered live View-tree assertions and process death/relaunch. Do not substitute static-source matching for UI acceptance.
3. Once green, take the next independent lifecycle gap: **PROJ-002/003 provider write-failure recovery and two-document isolation**, preserving all existing Project canonical-state invariants.
4. PRs #28 and #29 remain isolated. Physical Akai MPC Studio MkII acceptance is still pending; emulator CI does not prove hardware input.

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
