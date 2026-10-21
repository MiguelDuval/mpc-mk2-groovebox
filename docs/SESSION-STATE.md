# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-21  
**Purpose:** handoff checkpoint; mutable status is reconciled against live GitHub before each change.

## Latest source and verification state

### Live checkpoint — Browser/Sample Edit acceptance advances; Project UI hierarchy blocks full smoke

- PR #23 remains **OPEN** on `feature/mpc-one-ui-fidelity`. HEAD `c721e93fde80e9bb0e45b94ce375ec17e20b2692` was tested by Build #3048 / run `39671703344`; the workflow failed at Android emulator smoke step 52. APK artifact `12379408203` (9,957,677 bytes; SHA-256 `a9f9fce7a4267b9e1f1457cc3be5df0c8fe8012f98c3a939df8392112b184caa`) and visual evidence ZIP `12380913218` (10,076,003 bytes; SHA-256 `50d8a83b6e0171d60cb199b00d5c4021088ab6ef361b089c9d6bd59beb7403cf`) were uploaded. `main` remains unchanged and `cancel-in-progress: false` is preserved.
- **FAVORITE 1 real acceptance passed inside #3048 on that exact SHA:** the fresh root PNG/XML pair shows `Files on Android SDK built for x86_64` and visible `BBB-MPC-Browser-Favorite`; the log confirms the real row tap, isolated USER FOLDER/FAVORITE 1 SAF preferences, Browser WAV result, and force-stop/relaunch restoration. The complete workflow is nevertheless red due to the later Project startup/menu stage; do not label the entire CI or Project lifecycle green.
- #3048 failure: after force-stopping/relaunching the app into `project-persistence`, native startup and `MPC_MENU_RENDER_COMPLETE` completed; a rendered Menu screenshot was captured. The immediately following standard `uiautomator dump` timed out after 8 seconds and no fresh Menu XML was produced. The prior tree XML remains from the earlier Main shell and cannot be relabelled as Menu evidence. The menu screen itself is visible in the screenshot, and subsequent actual tile tap plus native Project summary are the next acceptance steps. The current isolated change uses a bounded **compressed** hierarchy dump for this Project Menu checkpoint only, retaining mandatory fresh parseable XML rather than treating its absence as success.
- SAMP-008 CROP and SAMP-009 CH4/CH8/CH16 passed in #3048, with real touches and authoritative native getters: 3840→2840 PCM frames, and contiguous/non-overlapping CH4/CH8/CH16 destination ranges. USER FOLDER and FAVORITE 1 source/filter/preference/restart checks passed.
- PR #29 current head `816e11a70a1368d63d0137c72aa4154774c5de7b`, Build #3049 / run `39672208687`, also failed later at the same Project Menu hierarchy dump. Importantly, the log shows **SAMP-010 itself passed** on that SHA: zoom-in 10–90%, zoom-out and RESET 0–100%, unchanged sample region/frame data, plus a real native Pad audition trigger. The whole PR remains draft/not integrated because exact-SHA full smoke still fails later.
- PR #28 current head `d0eef888e33ef98d5a64ce05a2409672f6b6e763`, Build #3050 / run `39672408316`, failed at the same shared Project Menu hierarchy stage after Favorite 1 assignment/restoration; its native Project state/persistence tests passed in the workflow. That does not prove Android SAVE/SAVE AS/OPEN acceptance.
- Full Project SAVE / SAVE AS / OPEN remains **PENDING**. After the Menu hierarchy is recoverable, the existing flow must complete actual SAF Save As → mutate → Open, exact canonical state including MIDI and Arrangement placement IDs/parameters, direct SAVE overwrite, force-stop/relaunch/Open, cancellation invariance, malformed-load atomicity and placement-free v1 compatibility. Physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-21

1. Use one bounded compressed UiAutomator dump at the actual Project Menu checkpoint; require valid nonempty XML as before. Inspect the resulting exact-SHA run and continue the existing Project lifecycle until it either passes or exposes the next concrete failure.
2. Keep PR #29 isolated until primary full smoke is green. Its zoom/audition subacceptance is now confirmed on run #3049, but do not integrate a branch that still fails the later shared smoke.
3. Keep PR #28 isolated; native tests pass but end-to-end SAF acceptance remains pending.

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
