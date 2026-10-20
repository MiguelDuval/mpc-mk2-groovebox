# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-20  
**Purpose:** operational handoff. Verify live GitHub state before trusting this snapshot.

## Live source and validation status

- Repository: `MiguelDuval/mpc-mk2-groovebox`; branch `feature/mpc-one-ui-fidelity`; PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN** against `fix/ui-shell-format-width-startup`. `main` has not been modified or merged.
- Live HEAD when this candidate was prepared: `4ef2992332cee03981cc63f0e4c77ef9eecc1e71`. Build #2964 / run [39425077675](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39425077675) is the exact-SHA Android validation target on that SHA. It had assembled/uploaded APK artifact 12266295661 (9,956,909 bytes), passed shell syntax validation and was still executing the full emulator smoke at checkpoint. A success must not be inferred from the APK artifact alone.
- Build #2963 / run 39400944235 on SHA `0916a0f9f543471f69c3bb889448ac8d95141acd` ended with exit 124 from the smoke's 30-minute internal timeout. Before timeout, logs confirmed Browser Places/Content/Expansions isolation, truthful CONTENT/DRUMS/INSTRUMENTS/DEMOS unavailable-provider states, real WAV visibility in SAMPLES/MY FILES, select-vs-assignment, audition-vs-assignment, Android document-tree WAV results and user-folder restoration. As the full workflow ended unsuccessfully, these are partial observations, not full-run VERIFIED status.
- The current workflow budget is 42 minutes for the smoke script, 45 minutes for the emulator step and 50 minutes for the job. Reconcile Build #2964's final logs/artifacts before deciding whether this budget is enough.
- A follow-on code candidate `8cdcbd5a79ad91a97af46b0e8d03fb130b153ddd` is prepared but not yet live at checkpoint. It adds a functional Browser “Show file size” option backed by local SharedPreferences, dynamic WAV metadata, a unit test and real-touch toggle/reopen/restore acceptance. It also archives category and select/audition states to the UI artifact. This candidate needs its own exact-SHA Actions run.
- Track Edit EDIT-009 remains pending exact-SHA acceptance. The sustain selector was corrected to match the actual `Track Edit envelope sustain decrease/increase` content descriptions. The full acceptance must prove each Attack/Decay/Sustain/Release field change and inverse restoration, cutoff ±500 Hz change/restore, unchanged unrelated values and changed/restored hierarchy/screenshots.
- Historical verified foundation belongs to recorded SHAs: Project SAVE/SAVE AS/OPEN at Build #2900 / run 39236521987; Pad selection at #2901 / run 39242015786; Track Edit GAIN/PAN at #2902 / run 39244891264; velocity bounds and sample region Start/End at #2907 / run 39286224809. Do not automatically transfer those results to newer HEADs.
- Physical MPC Studio MkII verification remains **PENDING**. Emulator and unit acceptance do not prove all hardware mappings or audible output.

## Current execution queue — 2026-10-20

1. Reconcile Build #2964's final state and inspect exact-SHA logs/artifacts. Preserve any failing assertion as a concrete blocker rather than hiding it behind an unrelated feature.
2. If #2964 passes, integrate the Browser file-size candidate on the PR branch and run full exact-SHA CI. Confirm the checkbox genuinely changes metadata, persists when Options is reopened, restores correctly, and all evidence files exist.
3. Re-run EDIT-009 under a green full workflow and inspect Attack/Decay/Sustain/Release/filter changed/restored evidence before marking it verified.
4. Keep the user-action catalog honest: BROW-003/007/008 have partial observed acceptance while BROW-012/013 remain pending until the file-size candidate's full run passes.

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

1. Inspect the Android Build attached to the new implementation SHA, including emulator-smoke conclusion, logs, screenshots/accessibility hierarchy, APK and evidence bundle. Unit tests and APK upload do not close persistence.
2. If an acceptance assertion fails, fix the actual cause without weakening it and rerun at the resulting exact SHA. Preserve the external sample-PCM contract.
3. Only after persistence is green, select the next high-value Arrange/Grid/Track/Step gap from live source/tests/action catalog; do not restart placement work or repeat controller research.
4. Controller hardware work must use the new traceability map and the semantic control map. Resolve the Jog/Touch Strip CC channel discrepancy only after capturing raw bytes on the real controller. Physical verification remains separate from emulator/CI.

### 10. MPC One ↔ Studio MkII cross-device integration — DOCUMENTATION COMPLETE / PHYSICAL VALIDATION PENDING

Created [MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md](MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md) and linked it from AGENTS.md, the controller semantic map, protocol traceability, MPC3 reference index and operating protocol. The crosswalk distinguishes the MPC 3.9 target action, Studio's printed hardware gesture, semantic app route, backend status and physical acceptance. High-risk differences are explicitly documented: Shift+Main (Track View vs Grid), Shift+Browse (Browser Up vs Save), Pad Mute / Track Mute ordering, Studio Mode+Pad shortcuts, and the absence of physical eight-knob Q-Link and two-axis XYFX controls.

The source register includes official MPC 3.9 documentation, Studio MkII support/Quickstart, public MIDI/SysEx reverse engineering and Akai workflow articles. Studio Academy/Masterclass playlists remain indexed but not fully transcript-audited because usable transcripts were not available. Mode Note 114 vs Note 1 and CC100/CC33 channel handling remain unresolved pending real hardware captures. No app/runtime code was changed. Physical controller verification remains PENDING; the implementation roadmap's Project Save/Open exact-SHA acceptance queue is unchanged.

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
