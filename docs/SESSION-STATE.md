# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** current handoff checkpoint; reconcile status against exact-SHA GitHub evidence before each change.

## Latest source and verification state

### Live checkpoint — Project Menu View-tree freshness synchronization

- PR #23 is open on `feature/mpc-one-ui-fidelity`; reviewed pre-fix HEAD `cb7edd5763b9bf97960d4b9e4c5ba4c1b98bfc7b`. Android Build #3052 / run `39681503924` failed at step 52, “Run Android emulator smoke test”. The debug APK and visual evidence artifacts were uploaded as IDs `12385195107` and `12385202751`, tied to that exact SHA.
- Builds #3053 / run `39682859251` on PR #29 head `240cf5bf91a5a66b59c4577003c8b2d785641fbf` and #3054 / run `39683044677` on PR #28 head `85026b48eb70587d3d097762e5bb4b5ece590bc1` also failed at shared Android smoke step 52. The dedicated SAMP-010 zoom/audition component passed in #3053; native Project state and versioned persistence/Arrangement Pattern placement steps 38–39 passed in #3054. Neither result proves a green full workflow.
- **Confirmed synchronization defect in the pre-fix smoke:** `project_menu_hierarchy_marker_before` was sampled only after the actual Menu tap, after the render/click gate, and after `sleep 1`. The registered Android `OnPreDrawListener` can publish `MPC_MENU_VIEW_HIERARCHY_READY` during that interval. The following wait then incorrectly required the count to exceed a baseline that already included the fresh event. Build #3052’s failure message and its immediately following diagnostic logcat are consistent with this exact missed-event ordering: the timeout is printed, then the diagnostic output contains the READY marker.
- This checkpoint’s code change takes a single logcat baseline for render-complete, click-end and View-tree READY **before** the real Toolbar Menu touch. It requires render-complete and click-end counts to increase from that baseline, then waits for READY against the original pre-touch count. Screenshot capture follows successful READY. XML provenance/page and the positive generated-at-uptime timestamp are validated along with all ten visible, enabled, clickable tile targets, measured bounds and the real PROJECT tap. Native Project-state verification remains a separate required gate. No UI assertion or timeout was weakened or extended.
- Browser USER FOLDER / FAVORITE 1 restore and SAMP-008/009 remain confirmed component results on their previously stated exact SHAs; do not revisit them without fresh evidence of regression. PR #29 and #28 remain isolated drafts.
- Full Android Project SAVE / SAVE AS / OPEN remains **PENDING**. The existing workflow must still prove real SAF Save As, post-save musical mutation, Open restoration against canonical MIDI + Arrangement placements, direct SAVE overwrite, force-stop/relaunch/Open, cancellation invariance, malformed-load atomicity and placement-free v1 compatibility. Physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-22

1. Verify that the synchronization fix is on the live PR #23 HEAD and inspect the newly triggered Actions run on that exact SHA. Confirm the Menu READY wait, valid View-tree XML, ten target assertions, real measured PROJECT touch and authoritative native Project summary.
2. If the full smoke proceeds beyond Menu, continue the existing Project lifecycle on that same run. Diagnose any later failure from its exact step/log/evidence; do not reopen completed Browser or Sample Edit slices without proof of regression.

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
