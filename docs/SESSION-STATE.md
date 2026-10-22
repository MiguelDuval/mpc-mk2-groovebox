# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** current handoff checkpoint; reconcile status against exact-SHA GitHub evidence before each change.

## Latest source and verification state

### Live checkpoint — Project persistence process-restart capture

- Primary PR #23 HEAD before this correction was `e9298e8d802d6d0e949415f52581855df9a92854`; Android Build #3057 / run `39781211397` completed **FAILURE** at the force-stop/relaunch section. Build URL: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39781211397.
- Build #3057 passed all preceding native/unit policy tests, APK assembly, upload, UI-smoke syntax/evidence preflight, and reached the real Project lifecycle. The Menu Android View tree contained 194 views and ten visible actionable tiles; Project captured 178 views; the post-Track-add SEQ page captured 250 views.
- Real DocumentsUI Save As → add Track in memory → Open saved file restored the original exact native Project digest `4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9`. Direct SAVE then overwrote the linked document with TRACKS=2 / PLACEMENTS=2 and new digest `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`.
- The remaining failure was harness wiring, not an observed native save/load mismatch: the cold relaunch omitted the smoke-mode extra, so the fresh process correctly reached `STARTUP_COMPLETE` but did not activate the opt-in Android View-tree capture marker. Commit `82a6ea1` adds a distinct `project-persistence-view-tree` instrumentation mode that enables real tree capture without re-seeding/resetting the native persistence fixture, and passes that mode on the cold relaunch.
- The follow-up smoke hardening checks that the restarted process emits no `PROJECT_TEST_FIXTURE_READY` marker before the actual saved file is opened. This guards against a future mode change silently turning the cold-restart test back into a seeded-fixture test.
- A final source review found that the initial Main pre-draw can occur before asynchronous native startup has finished. The View-tree serializer now refuses pre-startup snapshots, then schedules one fresh capture immediately after `STARTUP_COMPLETE`; the persisted XML that acceptance pulls therefore represents the ready-state page, not merely an early placeholder tree.
- Build #3058 / run `39790435311` on exact SHA `82a6ea1cf038a77cc1a45db9890fb6e8fe10416c` reached the DocumentsUI selection-return-target test after BROW-008 passed, but the smoke log recorded a failure before Project acceptance: the 8-second wait for the root drawer allowed only a 6-second and a 1-second UiAutomator dump, both timing out; the last successful XML still showed Recent, so it was not valid evidence about the post-tap drawer. The latest API snapshot still showed the job in progress during runner cleanup. This run therefore does **not** verify Project process-restart behavior.
- Commit `c56c16b` adds a ready-state gate for View-tree snapshots and schedules a fresh hierarchy after `STARTUP_COMPLETE`. Build #3060 / run `39792199195` for that exact SHA was queued at the last check. This next targeted fix gives the DocumentsUI drawer a 25-second bounded fresh-tree window and preserves a screenshot if the drawer still does not expose Downloads. The assertion remains that a newly captured hierarchy actually contains Downloads.
- Build #3056 / run `39780738111` on the earlier SHA failed at the old `SEQUENCE` page marker; commit `e9298e8` corrected it to the Activity's actual `SEQ` page ID and routed Save As cancellation evidence through the app-owned View-tree serializer.
- All capture evidence remains real Android Views with source/page/timestamp/bounds; actual Android DocumentsUI continues to be inspected via UiAutomator. No test assertion is being removed or weakened.
- Full Project SAVE / SAVE AS / OPEN remains **PENDING** until a green exact-HEAD run completes cold process restart/Open canonical equality, cancellation invariance, malformed-load atomicity and placement-free v1 compatibility. PRs #28/#29 remain isolated; physical MPC Studio MkII validation remains **PENDING**.

## Current execution queue — 2026-10-22

1. Validate the non-fixture View-tree mode at the force-stop/relaunch stage on the next exact-SHA Android run.
2. Continue the same full run through cancellation invariance, malformed-load preservation, and placement-free v1 compatibility; inspect artifact evidence and record only exact-SHA results.
3. Keep PRs #28/#29 isolated until the primary full emulator smoke passes; physical controller acceptance stays a separate gate.

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
