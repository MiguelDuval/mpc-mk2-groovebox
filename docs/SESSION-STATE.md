# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-27  
**Purpose:** live handoff. Exact branch HEAD, exact-SHA CI and inspected artifacts govern; this document is not a substitute for live GitHub state.

## Latest verified primary acceptance — PR #23

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Live HEAD: `ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d`; base SHA: `5f562e54e2cf8235041590933232a3836d74a16a`.
- Android Build #3402 / [run 40528666946](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40528666946) completed **SUCCESS** on that exact HEAD: the `android-debug` job completed all 57 workflow steps, including the emulator smoke and evidence upload. APK artifact `12746190315` (10,009,933 bytes); visual-evidence artifact `12746712856` (15,218,255 bytes).
- Emulator acceptance is not physical-device certification. Physical Akai MPC Studio MkII validation remains PENDING. Keep workflow concurrency `cancel-in-progress: false`; do not modify `main`, merge/retarget automatically, rebase, force-push, or rewrite history.

## Current isolated execution queue — 2026-10-27

1. **PR #114 — Grid zoom viewport.** HEAD `1b177ad886f87ac18e09828daa8880825ee38a3e`; stored base SHA `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`. Build #3399 / [run 40527004496](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527004496) completed SUCCESS on the exact HEAD. Inspected evidence artifact `12745464018`: real ZOOM H narrows Steps 25–40 to 27–38; ZOOM V reduces the visible Pad rows from 16 to 12 (Pads 12–01); Pad 01 / Step 33 and its native event fields remain unchanged. This covers emulator zoom-in only; physical MkII zoom/Data Dial behavior remains pending.

2. **PR #112 — truthful MIDI IN/OUT availability.** HEAD `65c651388ab67195c09687e9e67952e6b91688f4`; stored base SHA `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`. Build #3400 / [run 40527456461](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527456461) completed SUCCESS on that exact HEAD. APK artifact `12744668844`; visual evidence `12746466950`. The inspected Main hierarchy reports both MIDI ports unavailable when no controller is connected. Physical connect/disconnect and real MIDI-port operation remain pending.

3. **PR #110 — Back/subcontext restoration and Project persistence.** HEAD `68a83fc7f9541efc14531854d1060e5969f5429f`; stored base SHA `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`. Build #3412 / [run 40547076817](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40547076817) completed SUCCESS on the exact HEAD, all 57 steps. APK artifact `12753252585`; visual evidence `12754733235`. The inspected smoke evidence verifies the real Save As/Open flow, matching fresh DocumentsUI PNG/XML viewport geometry, renamed B document, write/read-back, and A/B document isolation: A retained TRACKS=2 with SHA `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`; B retained TRACKS=3 with SHA `f7b2814435b816904dcce6e7c47e019aeb79620a46e4c9a5a4682d38ffd6c4af`. This is current-head emulator evidence for #110; hardware validation remains separate.

4. **PR #111 — Browser local categories + modified-date metadata.** HEAD `935e59374b5ac682ec6ccca16b66b43db28f16d7`; base SHA `5fe65c952c7a6771f49478f5a026be9c0ada0f38`. Builds #3394 / [run 40518877443](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518877443) and #3395 / [run 40518880896](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518880896) both completed SUCCESS on the exact HEAD.

5. **PR #115 — stacked Save As capture follow-up.** HEAD `cdce4f1776cce5da064bfbeebbca5f7d87d06af2`; Build #3403 / [run 40535592616](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40535592616) passed on that exact HEAD, but the PR’s stored base is older (`22d965cc4bebff6043b81c8cb5b45edba4510ee9)) than PR #110’s current head. Do not auto-rebase or retarget it. PR #110’s own newer exact-head Build #3412 now passes the full Save As/A/B flow independently.

6. **PR #119 — dedicated BROW-010 WAV-picker cancellation.** HEAD `6cc60f134b4849a498d63e36319bd216648455a5`, stacked on PR #111’s exact head `935e59374b5ac682ec6ccca16b66b43db28f16d7`. Build #3414 / [run 40555139093](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40555139093) is IN PROGRESS at emulator smoke step 52. The new scenario cancels the real Android Document Picker with Back and compares fresh Browser state/hierarchy plus the native sampler identity. Do not accept until this exact HEAD finishes and its PNG/XML evidence is inspected.

7. **PR #120 — Arrangement ARR-003 zoom viewport.** Current HEAD `6e2893cba4a66dae64dcca787df91bd2cef5dd32`, based on PR #23’s exact HEAD `ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d`. Build #3415 / run `40555726605` is still running on the older test-only SHA `f42ce88fc3f68046ff456fee5f9846e4193b2c12`; Build #3418 / [run 40555944967](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40555944967) is PENDING on the current HEAD. Only the exact-current-HEAD result can validate the accessibility viewport metadata and real-touch RESET → ZOOM + → ZOOM − test.

## Next acceptance order

1. Inspect the results and exact-SHA PNG/XML evidence for PR #119 Build #3414 and PR #120 Build #3418 when they finish; trace any failure to its actual step and state before changing code.
2. Keep PR #115’s older base untouched; use PR #110’s fresh success as separate evidence, not as proof that #115’s own base is current.
3. Update this checkpoint only from live Actions/PR state. Physical MPC Studio MkII checks remain separate from emulator acceptance.

This checkpoint is documentation-only and needs its own exact-HEAD Android workflow result before the new checkpoint itself is considered green.

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
