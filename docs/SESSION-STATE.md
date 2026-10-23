# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-23  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified full exact-HEAD acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains OPEN on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`.
- **Latest full exact-HEAD acceptance:** [Android Build #3113 / run 39984123027](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39984123027), exact SHA `2c00c1603ea7228e4eacd384a4e73b6ac0fedb69`. All native/JVM checks, APK assembly, full emulator smoke and evidence upload passed after PR #31/#32 integration.
- APK artifact ID `12515141867`; visual evidence artifact ID `12516242478`. Smoke confirmed Browser USER FOLDER/FAVORITE 1 restoration, Project SAVE/SAVE AS/OPEN and recovery, Shortcut reset/restart, SAMP-008/009 and SAMP-010 native audition/viewport invariants.
- PR #30 was at `352e1319f1377c646f268dad07f54e7336214d6f` before this sync. No exact-SHA full run exists for that head. Build #3109 failed on prior SHA `cee0c8b5eef8e09c7197b22274f3c2dcd779e833` while navigating DocumentsUI FAVORITE 1; it did not reach the controlled output-denial test.
- SAF work already accepted on earlier exact SHAs: transaction failure-phase classification Build #3100; real DocumentsUI URI revocation and SAVE AS recovery Build #3107; revocation probe safety guards Build #3108. The controlled provider-output-denial test remains pending.
- Physical Akai MPC Studio MkII acceptance remains a separate pending hardware gate.

## Current candidate — SAMP-010 waveform zoom/audition

- `MpcProjectDocumentWriteTransaction` captures and SHA-256-verifies a bounded private preimage before output, writes the serialized snapshot, verifies exact read-back bytes, and attempts rollback on ambiguous write/readback failures. If rollback cannot be verified, it retains the byte-exact recovery copy; this is best-effort rollback, not an arbitrary-provider atomicity guarantee.
- The transaction exposes distinct failure phases. Build #3100 accepted that readback-only `SecurityException` is not evidence of write denial, while a denied forward or rollback output operation can disable direct SAVE. Build #3097 accepted the earlier guard that preimage-read EACCES/`SecurityException` with `writeAttempted=false` does not latch read-only state.
- Build #3107 accepted real DocumentsUI URI revocation and recovery using a disposable document; Build #3108 accepted the guard that no resolver stream is opened after restart unless read/write access is actually denied and no persisted grant remains.
- Added a debug-only provider fixture that returns the exact active Project bytes on read, denies the first output open, then permits rollback. Its full acceptance must verify `PROVIDER_WRITE`, `rollbackFailurePhase=NONE`, unchanged active canonical SHA, exact provider restoration, disabled direct SAVE, and working SAVE AS/direct SAVE/reopen.
- Separately, PR #23 Build #3113 accepted SAMP-010 real Pad audition and viewport-only zoom with unchanged sample PCM/region state. Those results do not substitute for PR #30's output-denial acceptance.

## Current execution queue

### Live CI — exact-SHA results and provider-failure state
- **PR #23 baseline:** Build #3113 / run [39984123027](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39984123027) passed the complete workflow on exact SHA `2c00c1603ea7228e4eacd384a4e73b6ac0fedb69`; APK artifact `12515141867`, visual evidence `12516242478`.
- **PR #30 current pre-sync result:** head `352e1319f1377c646f268dad07f54e7336214d6f` has no corresponding complete run. Build #3109 / run [39965373487](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39965373487) failed on the earlier SHA `cee0c8b5eef8e09c7197b22274f3c2dcd779e833` at Browser FAVORITE 1 DocumentsUI navigation. Fresh PNG/XML showed the smoke remained in an empty `Audiobooks` folder after a tap aimed at the Favorite folder; this is a UI-driver failure, not a Project-provider result.
- **Previously accepted SAF tests:** Build #3100 accepted typed transaction failure phases; Build #3107 accepted real URI revocation/recovery; Build #3108 accepted revocation safety guards. These do not cover a provider that retains URI access but denies the output stream.
- **Remaining gap:** run the controlled provider-output-denial scenario through the current production SAVE path, verify exact rollback and active-state invariance, then achieve a new full exact-SHA workflow and evidence upload.

## Current queue
1. Check the merged PR #30 branch HEAD and wait for its own exact-SHA Android workflow; require APK, focused JVM/native tests, full Browser/Project/SAMP smoke, real grant-revocation and controlled output-denial assertions, plus visual evidence.
2. If a step fails, inspect that exact step and its fresh logs/PNG/XML. Keep grant-revocation preconditions and assertions strict; do not infer Project failure from a Browser picker failure.
3. Fix only demonstrated root causes. Then use the catalog to choose the next semantic feature slice; Browser Options must remain unavailable until real state/behavior is implemented.
4. Preserve `cancel-in-progress: false`, do not modify `main` or rewrite history, and keep physical MPC Studio MkII validation separate.
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
