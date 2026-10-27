# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-27  
**Purpose:** live handoff. Exact branch HEAD, exact-SHA CI and inspected artifacts govern; this document is not a substitute for live GitHub state.

## Latest verified primary acceptance — PR #23

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Live HEAD: `ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d`; base SHA: `5f562e54e2cf8235041590933232a3836d74a16a`.
- Android Build #3402 / [run 40528666946](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40528666946) completed **SUCCESS** on that exact HEAD: the `android-debug` job completed all 57 workflow steps, including the emulator smoke and evidence upload. APK artifact `12746190315` (10,009,933 bytes); visual-evidence artifact `12746712856` (15,218,255 bytes).
- Emulator acceptance is not physical-device certification. Physical Akai MPC Studio MkII validation remains PENDING. Keep workflow concurrency `cancel-in-progress: false`; do not modify `main`, merge/retarget automatically, rebase, force-push, or rewrite history.

## Current isolated execution queue — 2026-10-27

1. **PR #114 — Grid zoom viewport.** HEAD `1b177ad886f87ac18e09828daa8880825ee38a3e`; Build #3399 / [run 40527004496](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527004496) SUCCESS on exact HEAD. Inspected artifact `12745464018`: H zoom narrows Steps 25–40 to 27–38; V zoom reduces visible Pad rows from 16 to 12 while Pad 01 / Step 33 and native event data stay unchanged. Hardware/Data Dial acceptance remains pending.

2. **PR #112 — truthful MIDI IN/OUT availability.** HEAD `65c651388ab67195c09687e9e67952e6b91688f4`; Build #3400 / [run 40527456461](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527456461) SUCCESS on exact HEAD. Emulator truthfully shows IN/OUT unavailable without connected hardware; physical connect/disconnect and actual MIDI I/O remain pending.

3. **PR #110 — shell Back/subcontext and Project Save As/Open.** HEAD `68a83fc7f9541efc14531854d1060e5969f5429f`; Build #3412 / [run 40547076817](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40547076817) SUCCESS, all 57 steps. Inspected evidence artifact `12754733235`: fresh DocumentsUI PNG/XML geometry matched; Save As B and real save/read-back passed; A stayed TRACKS=2 (SHA `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`) and B stayed TRACKS=3 (SHA `f7b2814435b816904dcce6e7c47e019aeb79620a46e4c9a5a4682d38ffd6c4af`).

4. **PR #111 — Browser local categories and modified-date metadata.** HEAD `935e59374b5ac682ec6ccca16b66b43db28f16d7`; Builds #3394 / [run 40518877443](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518877443) and #3395 / [run 40518880896](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518880896) SUCCESS on exact HEAD.

5. **PR #115 — stacked Save As capture.** HEAD `cdce4f1776cce5da064bfbeebbca5f7d87d06af2`; Build #3403 / [run 40535592616](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40535592616) SUCCESS on that exact HEAD, but its base SHA remains older (`22d965cc4bebff6043b81c8cb5b45edba4510ee9`). Do not auto-rebase/retarget; PR #110's current-head Build #3412 is separate newer evidence.

6. **PR #119 — BROW-010 real WAV-picker cancellation.** Code/test HEAD `6cc60f134b4849a498d63e36319bd216648455a5`; Build #3414 / [run 40555139093](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40555139093) SUCCESS, all 57 steps. Inspected evidence artifact `12758636875`: real Android DocumentsUI Back returned to Browser, retained target Pad 01 / Layer 01 and native `Browser-Picker-Return.wav`, and preserved location/search. PNG/XML for picker-open and Browser-return were reviewed. The follow-up documentation commit `e707ffd026210cef3a022d743741f5f1f631b749` is undergoing its own exact-head Build #3424 / [run 40562838515](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40562838515).

7. **PR #120 — ARR-003 Arrangement zoom viewport.** Current HEAD `32e2e4930959dfd850ea760deeafbff274da229a`; Build #3425 / [run 40563050574](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40563050574) is IN PROGRESS. Earlier failures were traced to test-oracle integration: one preflight checked the owning source file incorrectly; another changed the custom Canvas root contentDescription, breaking MainActivity's exact view lookup. Current code restores the stable root identity, publishes viewport metadata on the ZOOM − control, adds native viewport/event-state logs, and adds a unit regression for RESET's 8-bar window. Exact-head runtime result is pending.

8. **PR #122 — BROW-016 empty Search → retry.** Current HEAD `9a26724314d8fe9e09faee00e4abd0af1d8324b4`; Build #3426 / [run 40563628868](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40563628868) is IN PROGRESS. It starts from PR #119's cancellation acceptance, types a deterministic no-match query into real Search, verifies explicit NO MATCHING WAV FILES and zero file rows while preserving native sample/Pad/Layer/location, then taps CLEAR and compares the complete previous visible file list/state. Permission denial and removed-volume paths remain separate gaps.

## Next acceptance order

1. Check exact-head builds #3424, #3425 and #3426; inspect the real failing step and artifact evidence before any follow-up edit.
2. For PR #120, require stable Arrangement route lookup, narrower visible bars after ZOOM +, restored viewport after ZOOM −, and unchanged native event data.
3. For PR #122, verify the real no-match and clear/retry screenshots/XML plus unchanged native sample/Pad/Layer and exact file-list restoration.
4. Keep hardware validation separate from emulator acceptance. Do not merge, retarget, rebase, force-push or rewrite history automatically.

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
