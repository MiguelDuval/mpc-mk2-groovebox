# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** handoff checkpoint; live PRs, exact-SHA workflow runs and artifacts supersede historical notes below.

## Latest source and verification state

### Live checkpoint — 2026-10-21

- Primary PR #23 is **OPEN** on `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; current HEAD `b28b8723f31082982762e849026bddb6bb469dcf`. `main` is untouched. Workflow concurrency is `cancel-in-progress: false`.
- Android Build #3042 / run `39654368042` tests exact SHA `b28b8723f31082982762e849026bddb6bb469dcf`: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39654368042. On the latest live poll, APK artifact `12372125372` (9,957,677 bytes, SHA-256 `c6b84f731dad3f76d4607bf9452cd4fd29a9cc18fcd6bd716216ae874a740675`) and evidence-contract preflight passed; emulator smoke step 52 remained **IN PROGRESS**, and the evidence ZIP had not yet been published. No final run conclusion is claimed.
- Prior exact-HEAD Build #3041 / run `39648841982` on SHA `085b84877074b74d51ee73b8547947b409500025` failed later in FAVORITE 1 DocumentsUI selection. SAMP-008 CROP passed (3,840→2,840 frames, native region 0..2,840), SAMP-009 CH4/CH8/CH16 passed exact contiguous source-frame coverage, Sample Edit→Browser passed, and USER FOLDER filtering + process-death restoration passed.
- Build #3041 failure evidence showed the FAVORITE 1 picker initially in `AAA-MPC-Browser-Tree` with `Browser-Tree-Only.wav`, while the target fixture `Browser-Favorite-Only.wav` lives in a different folder. The smoke repeatedly queried that absent child via new UIAutomator dumps; later root-transition dumps timed out. The current primary smoke fix checks the first fresh hierarchy locally, navigates to the provider root and captures a new hierarchy before any root/folder tap; it also cleans up only stale UiAutomator runners and keeps `cancel-in-progress:false`. Build #3042 is the exact-SHA test of that correction; FAVORITE is not yet accepted.
- Project SAVE / SAVE AS / OPEN acceptance remains **PENDING**: real SAF save/open, canonical state round-trip including MIDI notes and Pattern placement IDs/parameters, overwrite SAVE, process restart + OPEN, cancellation invariance, malformed-file rejection preserving active state, and placement-free v1 compatibility still need a green current-HEAD emulator run.
- Draft PR #29 SAMP-010 is **OPEN** on `feature/samp010-zoom-audition-acceptance`, head `417e3fbbbd279184ad74734bbb9c38bfad8cd6d7`, based on old primary SHA `09cbc15c702c2d1b1989da7d453e50820c71c993`; GitHub reports it non-mergeable/dirty after PR #23 advanced. Its first run #3040 / `39648068024` failed before the external smoke at `UI_INTERACTION_FAILED: sample waveform editor`, because the app-side UI audit looked up the waveform immediately after navigation. The branch now has a bounded re-resolving view lookup and diagnostics; the SAMP-010 acceptance path is being synchronized to current primary HEAD with a history-preserving merge, and the exact merged-head workflow has not yet been observed.
- Keep SAMP-010 isolated from primary PR #23 until it passes viewport-only zoom in/out/reset, unchanged native frame/region/PCM state, actual native Pad 01 velocity 112 audition, and the full Browser + Project lifecycle gates. Physical MPC Studio MkII verification remains **PENDING**.

## Current execution queue — 2026-10-21

1. Wait for primary exact-HEAD Build #3042 to finish; inspect its terminal failure or successful result plus actual visual-evidence artifact before any further primary-branch edit.
2. Independently resolve PR #29 dirty base by a two-parent merge with current PR #23 HEAD `b28b8723f31082982762e849026bddb6bb469dcf`, preserving history and keeping the primary branch untouched. Carry forward the SAMP-010 code, test and four unique evidence checkpoints while retaining the latest primary Browser FAVORITE fixes and concurrency setting.
3. Inspect the exact merged-head CI; fix only the next evidenced failure. Once Browser FAVORITE 1 passes, complete Project SAVE / SAVE AS / OPEN acceptance. Only integrate SAMP-010 into PR #23 after these gates are green.

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

1. Inspect primary Build #3042 exact-SHA evidence; its outcome supersedes this checkpoint when known.
2. Maintain isolated, history-preserving PR #29 sync with primary HEAD b28; prove viewport-only SAMP-010 and native Pad audition on the merged exact HEAD.
3. After Browser FAVORITE 1 passes, verify real Project SAVE / SAVE AS / OPEN lifecycle and failure-safety; only then consider integrating SAMP-010.
4. Physical MPC Studio MkII tests remain a separate pending acceptance gate.

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
