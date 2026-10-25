# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-25  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified product-code acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Verified product-code HEAD: `3de910cc0e28b5abe82e588766b77c49262fe650`; base SHA: `5f562e54e2cf8235041590933232a3836d74a16a`.
- **Android Build #3296 / run [40205072938](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40205072938) PASSED on that exact SHA.** All 57 workflow steps passed, including focused native/JVM tests, APK assembly, UI smoke step 52, and visual-evidence upload step 53.
- APK artifact `12602514729`, 10,009,933 bytes, SHA-256 `6361f319c682457e34a183b089388bdbd621a7989054cee046fc9359af8bc8c9`. Visual-evidence artifact `12604850216`, 15,206,981 bytes, SHA-256 `665358fae296e0ceba8593d34abc728560da1193e4fbf0a404f3b6045614198e`. The evidence archive passed ZIP integrity and was inspected: 87 PNG, 97 XML, 3 text files and 2 logs.
- Inspected evidence confirms the local Browser WAV is visible/selected in CONTENT/MY FILES, FAVORITE 1 exposes the exact `Browser-Favorite-Only.wav`, malformed Project load preserves the existing two-Track/two-Pattern/two-placement/two-note state, and SAMP-008/009/010 screenshots/hierarchies are present. SAMP-008 CROP, SAMP-009 CH4/CH8/CH16, and SAMP-010 viewport-only zoom/reset plus native audition passed without sample-data mutation.
- Physical Akai MPC Studio MkII validation remains a separate **PENDING hardware gate**. `.github/workflows/android-build.yml` keeps `cancel-in-progress: false`; do not modify `main`, rewrite history, or merge/retarget PRs automatically.

## Current execution queue — 2026-10-25

1. **Browser Drums/Demos — PR #80.** Current head `9e62c91c5d0437649150bbcce73b3eaefce3cbf8`; Build #3298 / run [40206997235](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40206997235) is **IN PROGRESS** at emulator smoke step 52. Prior Build #3294 failed waiting for `Browser-Smoke-Local.wav` after BROW-001; the final UIAutomator dump timed out and the wait helper erased its last host hierarchy. Commit `9e62c91` changes the existing wait helper to dump to scratch and preserve the last validated hierarchy; the new full run must establish whether the missing result is a product/state issue. Do not weaken the row assertion or call BROW-003 accepted before exact-SHA green CI and evidence inspection.
2. **Browser modified-date option — PR #81.** Current head `f88b5050c93455c9e60a26d697cc0dd705448fb5`; Build #3297 / run [40205769766](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40205769766) is **IN PROGRESS** at emulator smoke step 52. Earlier Build #3295 passed on older SHA `72a2ea59d06115871ecc9d388062e76bcb46c721`; that does not accept the current head.
3. **Pad Mixer accessibility labels — PR #82.** Current head `7ae58ea9744c8b5b28608807d105398b5212d22e`; Build #3300 / run [40207666754](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207666754) is **IN PROGRESS**, last observed at Android toolchain setup step 7. Initial Build #3299 failed focused unit tests because the test invoked a static helper on the Android View class, triggering `ExceptionInInitializerError` in the local JVM. Commit `7ae58ea` extracts a pure accessibility-label policy and tests that policy without initializing Android UI classes. Keep PR #82 draft until its exact-SHA full workflow passes.
4. **Project provider failure classification — PRs #30 and #57.** Exact heads `918b195768a2e2326adb4edb73f046e0a34a1da1` / Build #3240 run [40144775117](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40144775117) and `345872ba400cac7bd9df665e619609f1ffaa25d6` / Build #3241 run [40144832991](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40144832991) both passed on their exact heads. Their PR base metadata still records the older primary SHA `30e270c51038afdd037e9d226f20eb5cc310bc82`; review live diffs and branch ancestry before any integration. No merge or retarget is authorized.
5. **Browser Show file size — PR #56.** Exact head `20e33dd7b9d2462dcd816da1281b2206948d243b`; Build #3239 / run [40144540628](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40144540628) passed. PR #56 is the base for #80/#81; keep those stacked changes isolated until their own exact-SHA results and artifacts are reviewed.
6. Physical MPC Studio MkII validation remains separate from emulator acceptance. Preserve one writer per branch; do not commit to a branch while its relevant full workflow is active except for an evidence-confirmed critical fix.

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
