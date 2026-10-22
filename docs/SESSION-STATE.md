# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-22  
**Purpose:** live handoff; the live branch SHA and exact-SHA CI remain authoritative.

## Last verified green baseline

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23), branch `feature/mpc-one-ui-fidelity`, base `fix/ui-shell-format-width-startup`; the branch HEAD at this handoff is `8edcb9dd87d7d5505a34daf941057b6e0a2bf30e`.
- Latest green run: [Build #3078 / 39851914811](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39851914811), exact SHA `8c19821cc909ecd5e2e7b7de4a06f27d078f8185`; all 57 steps passed. This is a docs-only descendant of the product code accepted by Build #3077.
- [Build #3077 / 39846737408](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39846737408), SHA `e5aac93b5ce5b4544ccdde53acd14d421f7ad7e7`, passed the full New Project smoke: CANCEL preserved the exact canonical Project SHA, DISCARD & NEW reset the native model, Main displayed `Sequence 01`, Project mode displayed `NO FILE LINKED`, and evidence uploaded.
- [Build #3073 / 39823061552](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39823061552), SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`, remains the full Project-persistence baseline: real SAF Save As/direct SAVE overwrite, exact v2 MIDI/Pattern-placement state, force-stop/relaunch/reopen, cancellation invariance, malformed-load preservation and placement-free v1 compatibility.
- Main Program selector availability and real Sequence-name display were also accepted on Build #3073's exact SHA.

## Exact-SHA workflow investigation

- Build #3079 / run [39861808911](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39861808911) was triggered for current SHA `8edcb9dd87d7d5505a34daf941057b6e0a2bf30e`. Attempt 1 failed during Android toolchain setup because SDK installation reported **`NDK (Side by side) 28.2.13676358: Archive is not a ZIP archive`**; APK assembly, source validation and emulator smoke were skipped. This is a toolchain-download failure, not an observed product regression.
- Attempt 2 was requested against the same exact SHA. The last observed job state was still in progress at `Assemble debug APK`, 48 successful steps and no reported failed steps, but run metadata had not updated since 22:03 UTC. Do not claim the Shortcut UI acceptance green from this attempt until its final emulator smoke/evidence result exists.
- Follow-up commit `1cd6ca741e618229fbd8bd0a2ebde4d741af1fa0` combines Shortcut Rail restart acceptance with the async Save callback session-isolation fix. Source review then found one more race in the smoke itself: the GRID selection callback schedules the View-tree snapshot before the write-marker polling loop returns. The first script captured its tree baseline after that wait, which could miss the already-written tree. This follow-up captures the baseline after the actual GRID popup is visible but before selecting GRID. Build #3079 was still running the older script at the last check; it does not contain this correction. The next exact-SHA workflow must validate the corrected script and the full app behavior.

## Current implementation slice

### Shortcut Rail persistence acceptance
- Assignment, reorder and RESET DEFAULTS persist the five ordered mode names in versioned local preferences (`shortcut_modes_v1`) and restore before shell construction. JVM tests cover round-trip, constructor restoration, malformed-value fallback and rejection without partial mutation.
- The pending full emulator path touches the real EDIT SHORTCUTS Function Bar item, changes slot 1 to GRID using the Spinner, moves the slot using its actual ▲/▼ button, checks the committed ordered mode list, force-stops/relaunches, and verifies five enabled/clickable visible rail views in exact order from a real app-owned Main View tree. It resets factory order and verifies the default order after a second process death.
- New smoke-only logs and accessibility descriptions provide evidence; regular non-smoke product logging remains unchanged.

### Asynchronous Project-save/session isolation
- Source review found a real race: after Save As serializes Project A and begins asynchronous provider I/O, the user can complete New Project/Open before the Save callback returns. The old callback could then bind Project A's destination URI to Project B or overwrite the new session's status.
- The follow-up introduces a monotonically increasing Project-session generation that advances only after successful New/Open replacement. A save captures that generation, the serialized snapshot SHA and its Project name. Its completion can only change the active document URI/identity/UI if the captured generation remains current.
- If the old snapshot writes successfully after a session replacement, the write is logged against the captured snapshot but it does not link that URI to the active Project. If the active Project itself changed during I/O, the UI reports that the captured snapshot was saved while newer edits remain unsaved. Stale failures do not overwrite the new session's status.
- A focused pure-Java policy and tests cover generation matching and snapshot-digest comparison. The Android smoke source preflight checks the critical generation guards; real-provider write-failure and two-document isolation acceptance remain separate follow-up work.

## Next actions

1. Verify the latest branch HEAD and its own Build workflow. Require focused JVM/native tests, APK assembly, source/syntax preflight, full real Shortcut assignment/reorder/reset process-restart acceptance, and visual evidence upload before marking the Shortcut Rail slice accepted.
2. Keep Build #3079's failed initial toolchain attempt distinguished from product-test failures; rerun/repair only from exact live evidence.
3. After the current slice is green, continue **PROJ-002/003 provider write-failure recovery and two-document isolation**, retaining all exact Project canonical-state checks.
4. PRs #28 and #29 stay isolated with their existing bases. Physical Akai MPC Studio MkII acceptance remains pending; emulator CI does not prove hardware behavior.

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
