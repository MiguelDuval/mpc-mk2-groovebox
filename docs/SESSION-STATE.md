# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-23  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Last accepted full-workflow baselines

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains open on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`.
- **Latest full exact-HEAD acceptance before this real-provider slice:** [Android Build #3100 / run 39939008154](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39939008154), exact SHA `3eafdf79d38a09100e444e5afa8516a51a02f30d`. The typed transaction failure-phase implementation, focused JVM cases, native/JVM checks, full Android emulator smoke (step 52) and visual-evidence upload (step 53) passed.
- Build #3100 artifacts: APK ID `12495955478` (10,009,613 bytes); visual evidence ID `12496337751` (14,075,908 bytes). Both artifacts are available from the linked run.
- Build #3097 / run 39931641764, exact SHA `549c78a709097a261277b090aa27672dab590c54`, remains the predecessor full-green baseline for the preimage-read latch fix.
- Build #3096 / run 39925775167 on SHA `12f988395c7d6ad50a40c84406770411fa1e72e1` remains the immediately preceding green baseline; Build #3097 verifies the follow-up that guards write-denial latching with `writeAttempted=false`.
- Build #3082 accepted the native Spinner popup scroll using measured live ListView bounds. The earlier GRID wait was a test-driver gap; it is closed on this exact SHA and should not be re-investigated without a fresh regression.
- [Build #3077 / run 39846737408](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39846737408), SHA `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`, accepted real New Project cancellation/reset/unlinked-file behavior.
- [Build #3073 / run 39823061552](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39823061552), SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`, accepted the real-SAF Project persistence baseline: Save As, direct SAVE overwrite, exact v2 MIDI/Pattern-placement state, force-stop/relaunch/reopen, cancellation, malformed-load preservation and v1 compatibility.
- Physical Akai MPC Studio MkII acceptance remains a separate pending hardware gate; emulator evidence does not prove physical-controller operation.

## Current Project workflow slice — SAF write/overwrite protection

- At the previous accepted HEAD, Project Save used `openOutputStream(uri, "wt")` and wrote the serialized Project directly. Several document providers truncate as soon as the stream opens. A mid-write or close failure could therefore damage the only saved document even though the native Project stayed in memory.
- The current implementation adds `MpcProjectDocumentWriteTransaction`: read and SHA-256-verify a bounded private preimage before touching the provider; write the captured Project snapshot; read the provider document back and require exact byte-count/SHA-256 equality. If writing or verification fails, attempt rollback from the verified preimage and verify that restoration. If rollback cannot be verified, retain the previous document bytes in app-private recovery storage and report that condition explicitly.
- A failed Save does not relink Save As to the active Project and does not replace the active in-memory model. Errors before the target write report `destination unchanged`; verified rollback reports `previous document restored`; uncertain rollback reports `recovery copy retained` and tells the user to use Save As.
- Added JVM fault-injection tests for exact write/readback, partial-write exceptions, silent truncation, preimage-read failure, readback provider failure, failed rollback with byte-exact recovery retention, and over-limit snapshots. Source-contract preflight keeps critical integration and failure tests present.
- **Transaction acceptance is confirmed on Build #3088 and revalidated through Build #3097** for the supported SAVE / SAVE AS / OPEN and recovery journeys. Build #3097 accepted the fix that a preimage-read EACCES/SecurityException with `writeAttempted=false` must not disable direct SAVE. The transaction uses verified best-effort rollback, not a guarantee of atomic replacement by arbitrary SAF providers.
- **Build #3100 accepted the typed transaction failure-phase correction** on exact SHA `3eafdf79d38a09100e444e5afa8516a51a02f30d`: provider output, post-write readback/mismatch, private rollback-backup read, rollback output and rollback verification are distinct. A readback-only `SecurityException` no longer counts as evidence that the provider denied a write; actual forward/rollback output permission denial still latches direct SAVE unavailable. JVM fault-injection tests and the complete emulator workflow passed.
- The remaining Project reliability gap is actual URI-grant revocation. A new debug-only Android smoke path releases the exact persisted modes for a real DocumentsUI URI, force-stops to remove temporary grants, relaunches and probes the URI through the real `ContentResolver`, then verifies that direct SAVE is disabled while SAVE AS can create and verify a replacement document. This test path is a candidate and requires its own exact-SHA full workflow.

### Live CI — exact-SHA results and provider-failure state

- **Build #3097 / [run 39931641764](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39931641764) SUCCEEDED** on exact HEAD `549c78a709097a261277b090aa27672dab590c54`. The Android build’s focused policy/unit tests, native tests, full emulator smoke (step 52), and visual-evidence upload (step 53) all passed. The uploaded APK is artifact `12492765076`; visual evidence is artifact `12492653431`.
- Build #3097 validates the already-landed transaction-phase guard for preimage-read failures: an EACCES/SecurityException before opening the provider output does not disable direct SAVE. It does **not** prove that arbitrary provider failures or revoked grants have been exercised on a real DocumentsUI provider.
- Candidate under test: separate `PROVIDER_WRITE` from `PROVIDER_READBACK` / `PROVIDER_READBACK_MISMATCH`, and record rollback output failure separately as `ROLLBACK_WRITE`. Direct SAVE should be latched unavailable only when the actual forward or rollback output operation reports a write-access denial. A readback SecurityException by itself is not write-denial evidence.
- New JVM cases inject readback SecurityException, genuine write SecurityException, and denied rollback output. They complement existing exact byte/SHA rollback, partial write, silent truncation, preimage read and recovery-copy tests. JVM fault injection is not labeled as real Android-provider or physical-controller acceptance.
- Keep the full exact-SHA gate: fresh candidate run must pass focused JVM and native checks, source-contract preflight, the entire Android emulator smoke, Project SAVE/SAVE AS/OPEN lifecycle, and visual-evidence upload before the phase-policy slice can be marked accepted.
- Earlier relevant baselines remain: Build #3088 / [run 39878406244](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39878406244) verified real DocumentsUI recovery export and reopen; Build #3089 / [run 39885681706](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39885681706) verified persistence of only provider-returned URI permission modes; Build #3096 / [run 39925775167](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39925775167) and Build #3097 passed full emulator smoke and evidence upload.

## Current queue

1. Complete the new real DocumentsUI URI-revocation smoke on an exact SHA: prove the URI was writable before release, prove there is no persisted/live read or write grant after process restart, and confirm the real ContentResolver read is denied while the active musical-state SHA remains unchanged.
2. Verify the real Project View has direct SAVE disabled and SAVE AS enabled/clickable with measured bounds. Then create a distinct replacement document via DocumentsUI, confirm exact read-back verification, direct-SAVE it and reopen it.
3. Require full exact-SHA Android CI including focused JVM suites, native tests, Bash/source-contract checks, the whole emulator smoke and visual-evidence upload. This covers grant revocation, not every provider-specific write-denial mode.
4. Leave PR #28/#29 bases unchanged, keep `cancel-in-progress: false`, and keep physical MPC Studio MkII verification as a separate hardware gate.

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
