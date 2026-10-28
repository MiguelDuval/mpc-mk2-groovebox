# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-28  
**Purpose:** live handoff. Always re-fetch GitHub PR/branch/Actions state; this checkpoint is not authoritative for mutable refs.

## Live GitHub state checked — 2026-10-28

- Local checkout was not accessible in this session, so local `git status`, worktrees, untracked files and unpushed changes could not be inspected. No claim is made that local overnight work was checked. Remote state below comes from live GitHub APIs and the exact-run Actions record.
- Product PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`; code HEAD observed at checkpoint start `ac59c5e23f87680e69e311e25d2e45d7ab1b8f8d`; base SHA `5f562e54e2cf8235041590933232a3836d74a16a`.
- Recovery PR [#124](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/124) is OPEN as Draft: `fix/p0-mute-grid-ui-recovery-20261027`, originally based on the exact PR #23 code SHA above. P0 product-code SHA `f734930cae403d0794e3d4b504e4bf8f5f7c6e84` passed Android Build #3465 / [run 40591850947](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40591850947), all 57 steps.
- APK artifact `app-debug.apk` ID `12775051524`, 10,010,205 bytes, SHA-256 `3b1ea246a0a5e1f069d6fc99eea82952b7ea5624a77712b20c08a063be909e71`. Visual evidence artifact `mpc-ui-visual-evidence` ID `12775747260`, 15,558,199 bytes, SHA-256 `96bf4e0e085ffb7ee86fe0a7600358473f6c4a6df71148e46dbe2b21d1dd345b`. The visual artifact belongs to run #3465 / head SHA `f734930cae403d0794e3d4b504e4bf8f5f7c6e84`; the downloaded archive hash matched GitHub's digest and the Pad/Track PNGs and hierarchy XML were inspected.
- Verified Pad evidence files: `mpc-groovebox-mute-pad.png`, `mpc-groovebox-mute-pad-uiautomator.xml`; grid exposes 16 pads with visible native ON/OFF state. Verified Track evidence: `mpc-groovebox-mute-track.png`, `mpc-groovebox-mute-track-uiautomator.xml`; 16 Track slots, Track 01 live ON/OFF state, empty slots honestly disabled. Smoke toggled/restored real native mute state and checked Sequence/Track/Pad selection and event/note snapshot invariance.
- Main MUTE → workspace → Back is runtime-asserted. Reserved Channel Mixer/Sounds/XY/Menu routes replace stale content with an explicit unavailable workspace and Back preserves previous musical context. The evidence archive includes dedicated Mute PNG/XML; the Reserved-route behavior is confirmed by runtime assertions, not a separately claimed screenshot.
- Physical Akai MPC Studio MkII validation remains **PENDING**. Emulator success is not physical-controller acceptance. PR #124 remains a Draft and has not been merged.
- The docs-only checkpoint is being advanced as a descendant of tested product-code SHA `f734930...`; after the ref update, re-fetch PR #124's live HEAD and verify the workflow for that exact docs descendant before calling the updated branch green.

## Current execution queue — 2026-10-28

1. **Track Solo — real playback semantics.** The function-bar Solo is disabled/reserved. `Track::soloed` exists in `src/MPC/Domain/MpcDomain.h` and is serialized in `src/MPC/MpcProjectPersistence.cpp`, but `MpcSequencePlaybackSession.h` currently supplies only `&track.muted` to playback. `MpcSequencerPlayback.h` applies Mute before routing scheduled events to the audio-trigger queue and does not apply Solo. Trace and test the whole native path before enabling UI: normal playback with no solos; single/multiple Solo tracks; mute precedence; live Solo changes during playback; Sequence changes; state save/load; and truthful unavailability for unsupported Track Types. No Solo simulation through Mute.
2. Create an isolated child branch from the actual live P0 PR branch after refreshing its HEAD; target `fix/p0-mute-grid-ui-recovery-20261027` as the Draft PR base so dependency on PR #124 stays explicit. Do not add Solo to PR #124 or retarget either existing PR.
3. Use focused native/domain tests first, then full exact-SHA Android workflow and inspect any relevant artifacts. Keep the audio callback free of Solo calculations, UI calls, allocation and blocking work.
4. If Solo cannot be completed safely with the current Drum playback model, keep it RESERVED and choose the next small functional gap from the live roadmap.

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
