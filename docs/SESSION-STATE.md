# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-23  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Last accepted full-workflow baselines

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains open on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`.
- **Latest full exact-HEAD acceptance:** [Android Build #3082 / run 39868644535](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39868644535), exact SHA `0d953da7eb9370e5180424d9d6de1a9b87c25442`. All 57 workflow steps passed, including the full emulator smoke and evidence upload.
- Build #3082 produced visual evidence artifact ID `12466226732`, SHA-256 `fc6947c70610d587c874edbb648a051286778d2422d551dcf73a195ccbb9acb4`, plus APK artifact ID `12464309278`.
- Build #3082 accepted the native Spinner popup scroll using measured live ListView bounds. The earlier GRID wait was a test-driver gap; it is closed on this exact SHA and should not be re-investigated without a fresh regression.
- [Build #3077 / run 39846737408](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39846737408), SHA `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`, accepted real New Project cancellation/reset/unlinked-file behavior.
- [Build #3073 / run 39823061552](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39823061552), SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`, accepted the real-SAF Project persistence baseline: Save As, direct SAVE overwrite, exact v2 MIDI/Pattern-placement state, force-stop/relaunch/reopen, cancellation, malformed-load preservation and v1 compatibility.
- Physical Akai MPC Studio MkII acceptance remains a separate pending hardware gate; emulator evidence does not prove physical-controller operation.

## Current Project workflow slice — SAF write/overwrite protection

- At the previous accepted HEAD, Project Save used `openOutputStream(uri, "wt")` and wrote the serialized Project directly. Several document providers truncate as soon as the stream opens. A mid-write or close failure could therefore damage the only saved document even though the native Project stayed in memory.
- The current implementation adds `MpcProjectDocumentWriteTransaction`: read and SHA-256-verify a bounded private preimage before touching the provider; write the captured Project snapshot; read the provider document back and require exact byte-count/SHA-256 equality. If writing or verification fails, attempt rollback from the verified preimage and verify that restoration. If rollback cannot be verified, retain the previous document bytes in app-private recovery storage and report that condition explicitly.
- A failed Save does not relink Save As to the active Project and does not replace the active in-memory model. Errors before the target write report `destination unchanged`; verified rollback reports `previous document restored`; uncertain rollback reports `recovery copy retained` and tells the user to use Save As.
- Added JVM fault-injection tests for exact write/readback, partial-write exceptions, silent truncation, preimage-read failure, readback provider failure, failed rollback with byte-exact recovery retention, and over-limit snapshots. Source-contract preflight keeps critical integration and failure tests present.
- **This transaction slice is implemented but awaits exact-SHA full Android CI.** It is best-effort rollback, not an atomic rename guarantee; arbitrary SAF providers do not promise atomic writes.

### Live CI while the next independent slice proceeds

- Async Open isolation commit `2bd45d8ec9d4ff33e596d74a50cd9d84b630501d` is still under Android Build #3083 / [run 39872152402](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39872152402) on its exact SHA. At the last live check, the focused Android unit-policy step was running; no result is claimed.
- The write-protection patch is based on `2bd45d8` and needs its own full workflow. The workflow's `cancel-in-progress: false` remains unchanged so overlapping acceptance runs do not cancel one another.

## Current queue

1. Follow Build #3083 to final conclusion on exact SHA `2bd45d8ec9d4ff33e596d74a50cd9d84b630501d`; if it fails, fix only evidence-backed causes without rewriting history.
2. Validate the SAF write transaction on the new exact SHA with JVM fault-injection tests, native tests, Bash/source-contract checks, the full real UI smoke, and evidence upload.
3. Extend Android emulator smoke with real two-document Save As/Open isolation and a deterministic provider write-failure path where practical; verify active canonical-state SHA and file bytes after each failure.
4. Keep PR #28/#29 bases unchanged. Physical MPC Studio MkII remains a separate hardware acceptance gate.

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
