# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-28  
**Purpose:** live handoff. Exact branch HEAD, exact-SHA CI and inspected artifacts govern; this document is not a substitute for live GitHub state.

## Latest verified primary acceptance — PR #23

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: feature/mpc-one-ui-fidelity → fix/ui-shell-format-width-startup. Current HEAD: d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991. Base SHA: 5f562e54e2cf8235041590933232a3836d74a16a.
- Product-code acceptance: SHA 3de910cc0e28b5abe82e588766b77c49262fe650 passed Android Build #3296 / [run 40205072938](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40205072938), all 57 steps including emulator smoke and visual-evidence upload.
- The current documentation-only HEAD d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991 passed Android Build #3301 / [run 40207854314](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207854314), all 57 steps. APK artifact 12604227275, 10,009,933 bytes, SHA-256 f6786f1ede09351fe529964011f56d6fb02eef7fa6852bfa61cd21c98fe134cd. Visual-evidence artifact 12605069409, 15,211,863 bytes, SHA-256 e9590c0998885a6c746c70a754303a0172f6eea1f909478505ee1d2ede4042e9.
- The prior code SHA 3de910cc0e28b5abe82e588766b77c49262fe650 artifacts were APK 12602514729 and evidence 12604850216. Both the code and docs-only exact-SHA workflows passed. Verified scenarios include SAMP-008 crop, SAMP-009 CH4/CH8/CH16, SAMP-010 viewport-only zoom/reset/audition, real Browser local WAV/FAVORITE 1, and Project persistence/malformed-load preservation.
- Physical Akai MPC Studio MkII validation remains a separate PENDING hardware gate. Workflow concurrency remains cancel-in-progress: false. Do not modify main, merge/retarget automatically, or rewrite history.

## Current isolated execution queue — 2026-10-27

1. **PR #114 — Grid zoom viewport acceptance.** Branch test/grid-zoom-viewport-acceptance-20261027; HEAD 1b177ad886f87ac18e09828daa8880825ee38a3e; base SHA d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991. Build #3399 / [run 40527004496](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527004496) is IN PROGRESS at emulator smoke step 52. APK artifact 12743758269, 10,009,933 bytes, SHA-256 1999ad7db0d38fd24a0fa3a528c247eac0f13edbe0d5ae3b73914a17c8902f65. Previous #3397 / run 40519736989 failed because the smoke expected a transient vertical-zoom status node absent from the accessibility tree. Its inspected screenshot proves the real viewport changed from 16 visible Pads to 12 (Pads 12–01). The new commit exposes the Canvas Pad range through accessibility and strictly asserts reduced row count, Pad 01 retention, unchanged horizontal range, event identity/timing/fields and event count. Evidence artifact 12742367746 was inspected; the new run still needs final evidence and full success.
2. **PR #112 — truthful MIDI IN/OUT unavailable state.** Branch fix/honest-toolbar-midi-port-status-20261027; HEAD 65c651388ab67195c09687e9e67952e6b91688f4; base SHA d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991. Build #3400 / [run 40527456461](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527456461) is IN PROGRESS at emulator smoke step 52. Previous #3386 / run 40504866939 failed because a newly added Python bounds regex was over-escaped; the captured IN/OUT indicators were visible, correctly unavailable and had positive bounds. Commit 65c6513 corrects the regex only; inspected failure evidence artifact 12735532458. Do not accept until the exact-head workflow and artifacts finish.
3. **PR #110 — shell Back/subcontext restoration.** Branch fix/restore-shell-navigation-context-20261027; HEAD 22d965cc4bebff6043b81c8cb5b45edba4510ee9; base SHA d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991. Build #3401 / [run 40527990598](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527990598) is IN PROGRESS at native Project state test step 38. Previous #3398 / run 40520813386 failed at emulator smoke step 52 because standard UiAutomator capture timed out while DocumentsUI was the resumed activity; the picker failure itself was not established. Inspected evidence artifact 12742799195 and APK artifact 12743361682. Commit 22d965c uses the existing bounded scratch-preserving compressed-tree path for the initial large DocumentsUI snapshot and validates the root package/root-navigation action; later Downloads and real file-row assertions remain strict.

## Next acceptance order

1. Wait for exact-SHA full workflows #3399, #3400 and #3401; inspect their precise failing step and uploaded XML/PNG/diagnostics before any further fix.
2. If Grid zoom fails, distinguish accessibility contract failure from actual viewport state using the new Pad-range description and the real screenshot; never weaken native event assertions.
3. If MIDI status fails, inspect the exact visible node and bounds; do not infer hardware availability from an allocated bridge.
4. If DocumentsUI capture fails again, inspect fresh-dump logs and the last validated hierarchy; do not reuse stale XML as current evidence or merely raise timeouts.
5. Review ancestry/live diffs before considering integration of stacked PRs. Do not merge or retarget automatically.
6. Keep physical MPC Studio MkII validation separate from emulator acceptance.

This checkpoint commit is documentation-only. Its own exact-SHA workflow must be checked before declaring the new HEAD green.
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

### 10. Track Solo (MAIN-019) — IMPLEMENTED, pending CI verification

Implemented real Track Solo on branch `feature/mpc-overnight-20261028`, based on verified P0 recovery SHA `f734930c`.

Changes:
- `src/MPC/Sequencer/MpcSequencerPlayback.h` — added `trackSoloed_` and `anyTrackSoloed_` pointers; `advance()` suppresses event routing when `anyTrackSoloed_` is true and this track is not soloed; mute takes precedence over solo.
- `src/MPC/Sequencer/MpcSequencePlaybackSession.h` — computes `anyTrackSoloed` across all tracks and passes `&track.soloed` and `&anyTrackSoloed` to each `MpcSequencerPlayback`; added `updateSoloState()` to recompute when solo changes during playback.
- `src/NativeEngine.cpp` — added `nativeSequenceIsTrackSolo`, `nativeSequenceToggleTrackSolo`, `nativeSequenceIsPadSolo`, `nativeSequenceTogglePadSolo` JNI methods; calls `updateSoloState()` after toggling track solo.
- `android/app/.../MainActivity.java` — enabled SOLO button in Function Bar (MAIN and TRACK_VIEW modes) with visual active-state indication (shows "SOLO" green when active, "S" muted when off); added `toggleSelectedTrackSolo()` with Function Bar refresh; wired mixer strip SOLO button via `toggleTrackSolo()` listener; added native method declarations; wired Track View per-track SOLO button.
- `android/app/.../MpcMainMixerStripView.java` — SOLO button enabled, shows active state when soloed, disabled when not ready.
- `tests/MpcSequencerPlaybackTests.cpp` — added 4 solo behaviour test cases (soloed track routes, non-soloed suppressed when any soloed, mute takes precedence).
- `tests/android_ui_smoke.sh` — updated Track View strip control contract: replaced `'Track View track solo unavailable'` with `'Track View track solo'` and added `'nativeSequenceToggleTrackSolo(trackIndex)'`.
- `docs/MPC3-USER-ACTION-CATALOG.md` — MAIN-019 changed from RESERVED to IMPLEMENTED — reverify at acceptance boundary.

### 11. Browser favorite location gating fix — FIXED

Fixed `MpcBrowserView.isCatalogLocationSupported()` which incorrectly gated FAVORITE 1-5 locations behind `userFolderConfigured && userFolderAccessReady`. Favorites now check `isDocumentLocation()` only, independent of user folder state. Also simplified `unavailableLocationTitle()` and `unavailableLocationDetail()` for favorites to not reference user folder state.

Not yet verified: CI build, Android emulator acceptance, exact-SHA workflow result.

### 12. Sample WAV persistence across project save/reopen — IMPLEMENTED

Fixed the critical gap where imported sample WAV data was lost on project save/reopen. `nativeProjectLoad` acknowledged "sample PCM is not embedded; assigned WAVs may need reload" — samples would not play after project reopen.

Changes:
- `src/Audio/AudioEngine.h` — `loadSampleForPadLayer` accepts optional `sourcePath` parameter; `loadSampleForPad` delegates with default empty path.
- `src/Audio/AudioEngine.cpp` — passes `sourcePath` to `projectState_.registerSample()` instead of hardcoded `""`.
- `src/NativeEngine.cpp` — JNI `nativeAudioLoadSampleForPadLayer` accepts `jstring sourcePath` and converts to UTF-8; `nativeProjectLoad` now iterates all Drum Program pad layers, reads WAV files from stored `SampleRef.path` paths, and reloads them into the AudioEngine after project replacement. Removed the "sample PCM is not embedded" disclaimer from the load result.
- `android/app/.../MainActivity.java` — JNI declaration updated with `String sourcePath` parameter; `assignSelectedBrowserFile()` saves imported WAV bytes to `getFilesDir()/samples/` and passes the internal path; WAV picker result handler (`REQUEST_OPEN_WAV`) does the same; `loadBundledSample()` passes `null` for path (bundled asset has no file path).

Sample lifecycle end-to-end is now complete:
- Browser → real WAV file selection (already existed)
- Import/load → WAV bytes saved to app-private storage, path stored in SampleRef (NEW)
- Assignment → Pad/Layer via `nativeAudioLoadSampleForPadLayer` (already existed)
- Audition → `selectAndTriggerPad()` / MediaPlayer preview (already existed)
- Real playback → sequencer → triggerQueue → AudioEngine plays `padSamples_` (already existed, now survives reopen)
- Project save → SampleRef metadata + path serialized (already existed)
- Project reopen → WAV data reloaded from stored paths into AudioEngine (NEW)

Not yet verified: CI build, Android emulator acceptance, exact-SHA workflow result.

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
