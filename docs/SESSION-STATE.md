# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** handoff checkpoint; mutable status is reconciled against live GitHub before each change.

## Latest source and verification state

### Live checkpoint — Android Build #3047 and follow-up diagnosis

- PR #23 is **OPEN** on `feature/mpc-one-ui-fidelity`. HEAD `dcbb5810200a34e4143d37edb49e73e12b69c36f` was tested by Build #3047 / run `39666355459`, which failed at Android emulator smoke step 52. Build #3046 / `39664592692` also failed at step 52 on `a2705476d68793a99bb799515ab16ccacc2ed928`. Neither run completed FAVORITE 1. `main` remains unchanged, and `cancel-in-progress: false` is preserved.
- The latest artifacts show a real root XML containing `Files on Android SDK built for x86_64`, `BBB-MPC-Browser-Favorite`, and the `USE THIS FOLDER` action. The problem is after that successful fresh root capture: `tap_ui_picker_folder_until_child()` attempts a separate three-dump stable capture and fails before publishing `mpc-groovebox-browser-favorite-tree-root-uiautomator.xml`. The saved PNG with the picker name still shows the previous AAA folder; that is not root evidence.
- Build #3047's final timeout had a 1-second budget after a 30-second stabilization deadline. The raw `ps` output now contains ordinary Android processes in its first rows, but no matched UiAutomator runner there. This still does not establish the precise reason repeated accessibility calls stall; the actionable failure is the unnecessary triple-dump stage after a fresh root hierarchy was already captured.
- Current fix scope: keep the shared three-dump stable-capture helper as the default for all other picker flows. For FAVORITE 1 only, use the just-captured fresh hierarchy after the real breadcrumb tap, recheck exact provider-root title and the visible Favorite folder, capture a screenshot immediately, copy the same current XML into the dedicated root evidence path, and then tap the folder using that verified hierarchy. This is an evidence-driven, root-specific fallback—not cached UI, not a mocked state, and not a timeout increase. The real child file, persistable URI grant, Browser result, independent favorite preference, and force-stop/relaunch checks stay mandatory.
- SAMP-008 CROP and SAMP-009 CH4/CH8/CH16 passed on #3046/#3047: real touches and native PCM/destination getters verified 3840→2840 PCM and contiguous non-overlapping 4/8/16 ranges. USER FOLDER filter plus process-death restoration passed in the same runs. FAVORITE 1 remains unverified.
- PR #29 SAMP-010 fix commit `62f12da6516cb125f9905dab21d1a32b578882f5` fixes the tab-row removal bug: adding the viewport readout moved the Sample Edit tabs to child index 4, while four panel helpers continued removing from index 4. The new fixed-child count preserves ENV/FILTER/LAYER without disabling audit. This SHA has no exact-SHA workflow result; PR #29 is still draft and mergeable=false against its older base.
- PR #28 Build #3044 / run `39657357881` failed later in the shared Favorite 1 path, while its native Project state/persistence steps 38/39 passed. Full Project SAVE/SAVE AS/OPEN UI lifecycle remains **PENDING** because the primary acceptance doesn't get past FAVORITE 1. Physical MPC Studio MkII validation remains **PENDING** and is not implied by emulator tests.

## Current execution queue — 2026-10-21

1. Verify the new Favorite-root paired-evidence path on its exact-SHA Android Build. Require provider-root header, folder visibility, actual row tap, actual Favorite-only WAV, Android persisted grant, Browser indexing and force-stop/relaunch restoration.
2. Keep PR #29 isolated until the primary Browser gate passes; then reconcile its stale base using history-preserving changes and run its exact SHA. Continue the existing Project SAVE/SAVE AS/OPEN UI scenario only after FAVORITE 1 succeeds.

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
