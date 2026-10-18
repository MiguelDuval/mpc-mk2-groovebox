# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-18  
**Purpose:** operational handoff. Verify mutable status against live GitHub before implementation.

## Latest source and verification state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; working branch: `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN**, base `fix/ui-shell-format-width-startup`; not merged or retargeted.
- The implementation's user-facing Project SAVE / SAVE AS / OPEN route is present in `MainActivity.java`, using Android SAF and native serializer/loader calls. The native loader validates a candidate, stops transport/audio as needed, clears runtime sample buffers and incompatible edit history, then replaces model state. Sample PCM is external; assigned WAV data may need reloading/relinking.
- The first Project UI smoke scenario saves via SAF, adds a Track to the in-memory model, reopens the saved document and checks that Track/placement counts return to saved values. It does not yet compare every placement ID/timing/source offset and complete MIDI field in the Android flow or force-stop/relaunch between save and open. Native codec tests cover detailed field round-trip and the placement-free v1 fixture separately.
- Prior [Android Build #2808](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39121411497) passed on app SHA `63a5e51e7b66e0e879513acd9c9ff32d545f434e`, but that SHA predates the Android Project UI route and is **not** proof of the new UI integration.
- Immediately before this checkpoint update, PR #23 pointed to docs SHA `250a777fdec27bd4ecc3a23f756557eeb7ec8021`; Android Build #2834 (`39139616967`) had status PENDING with no conclusion. This update itself creates a newer documentation SHA, so #2834 cannot validate the resulting HEAD. No green result is claimed; check the latest run attached to the actual live PR head.
- A new controller research document now connects the public MIDI/SysEx source, official Akai support/quickstart/tutorial resources, code, tests and physical validation: [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). Source scope explicitly excludes MPC Desktop and DAW workflows. The official tutorial playlists are indexed, but full transcripts were not accessible in this pass; no claim is made that every video has been reviewed.
- Controller documentation and agent rules have been reconciled to reflect current non-pad semantic routing and partial feedback implementation. The upstream protocol repo has 72 binary LCD SysEx messages (60 `.syx` + 12 `.sysex`) and 72 paired PNGs; representative headers confirm six-chunk LCD traffic, but the payloads have not all been individually decoded. A newly catalogued conflict exists: the upstream README/current app map Mode to Note 114 while the Go POC `controls.yaml` has Mode=1. Do not resolve from assumption; capture physical bytes. The decoder also accepts Jog CC 100 and Touch Strip CC 33 without a channel constraint.
- Physical MPC Studio MkII verification remains **PENDING** for many current behaviors. Emulator/unit tests do not replace device checks, and the existing detailed physical record is tied to its historical APK/build.

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

## Current execution order

1. Check live PR #23 HEAD and its latest exact-SHA Android workflow; successive documentation commits may cancel earlier runs. Inspect logs, screenshots, hierarchy and APK before asserting the Project UI flow is verified.
2. Harden Project acceptance: true app-session reset/relaunch, exact placement IDs/references/start/length/source offsets and MIDI-note fields, placement-free v1 load, and bounded malformed/cancel/error paths. Keep sample PCM's external-reference limit explicit.
3. After the Project flow is accepted, select the next highest-value Arrange/Grid/Track/Step gap from current code/tests and the action catalog. Do not restart completed Pattern-placement work or repeat the full firmware crawl.
4. Controller hardware work must use the new traceability map and the semantic control map. Resolve the Jog/Touch Strip CC channel discrepancy only after capturing raw bytes on the real controller. Physical verification remains separate from emulator/CI.

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
