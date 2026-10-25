## Latest verified product-code acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Live HEAD: `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`; base SHA: `5f562e54e2cf8235041590933232a3836d74a16a`.
- **Android Build #3301 / run [40207854314](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207854314) passed on exact SHA `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`.** All 57 steps passed, including native/JVM tests, APK assembly/upload, emulator UI smoke and visual-evidence upload. Artifacts: APK `12604227275`; visual evidence `12605069409`.
- Physical Akai MPC Studio MkII validation remains a separate **PENDING hardware gate**. Keep `.github/workflows/android-build.yml` at `cancel-in-progress: false`; do not modify `main`, rewrite history, or merge/retarget PRs automatically.

## Current execution queue — 2026-10-25 (verify live refs before acting)

1. **Save As visual evidence — PRs #58–59: exact-head CI PASSED.** PR #58 head `63ea6805e819537a96eed446bdde1b3aa518531d`, Build #3254 / [run 40177584543](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177584543); PR #59 head `5a12f545c6b798a6a8dce2e3f53448fb2ba06f41`, Build #3280 / [run 40192720803](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40192720803). Downloaded and inspected PR #59 evidence artifact `12599204885`: three distinct screenshot/XML pairs cover the initial Save As picker, second document picker showing the first `Untitled.mpcproj`, and recovery export destination. All showed package `com.android.documentsui`, expected destination/filename, and the relevant project rows. Both PRs remain OPEN drafts; no merge or retarget was performed.
2. **Persistence validation — PRs #60–67: the recorded latest heads all have successful full Android workflow runs.** #60 `596a8110565a99316ccb0d5aa2a04d1e0bba5c56` / [40177392571](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177392571); #61 `0fdba37e521e1b39a96e3c95ee9d0078a43e9883` / [40177684480](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177684480); #62 `26579254409aa1a5a5d0f0f8cac9bda5cd368832` / [40178612521](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178612521); #63 `04b4ab82635d49ca104a10a036702cf99c496c5c` / [40178770260](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178770260); #64 `85dd29b49b8a91fd9c033c4b839857778ad4674b` / [40185698676](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40185698676); #65 `396a7b46d0c2995aae3b1b240df01136fdd05ae8` / [40186113233](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40186113233); #66 `0ca8355ce704e35a57f90f6936053e54f5577296` / [40186451595](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40186451595); #67 `232923a956367c1a984c1878d014bffbec3fc6df` / [40191346634](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40191346634). These are separate OPEN drafts, not integrated into PR #23. PR #67 correctly reports the full-source loop bound before the secondary playback-region bound; do not treat the earlier failing runs as acceptance.
3. **Browser local Drums/Demos — PR #80.** The exact run for previous head `9e62c91c5d0437649150bbcce73b3eaefce3cbf8`, Build #3298 / [run 40206997235](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40206997235), FAILED at emulator smoke step 52. The captured hierarchy showed `LOCAL AUDIO • 3 WAV FILES INDEXED` but only the Demo row visible inside the clipped results ScrollView; the test then timed out looking for `Browser-Smoke-Local.wav`. This points to a smoke visibility/scroll issue rather than proof the provider failed. A strict test-only scroll-and-recapture fix is now at head `60f7b73e9a5888e9dbbdd20b7ffbb6d0e5bb4056`; Build #3302 / [run 40210755051](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40210755051) was **IN PROGRESS at last observation**. Re-check the exact head and inspect its final evidence; do not relax row assertions.
4. **Browser modified date — PR #81.** Head `f88b5050c93455c9e60a26d697cc0dd705448fb5`; Build #3297 / [run 40205769766](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40205769766) passed on that exact SHA.
5. **Pad Mixer accessibility labels — PR #82.** Head `7ae58ea9744c8b5b28608807d105398b5212d22e`; Build #3300 / [run 40207666754](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207666754) passed on that exact SHA.
6. **Sequencer position overflow — PR #83.** Two confirmed signed-overflow sites in loop normalization are fixed: `MpcSequenceTransportClock::normalize` (code commit `60e5575a460fb5de66ee9ed30346132ad2fb85a0`) and `MpcSequencerCursor::setPositionTicks` (code commit `674d388e6f91690a286141b0c2b86883bb68d960`). Both paths reduce operands modulo the loop length before subtracting. Regression tests exercise `INT64_MIN` and `INT64_MAX` with loop range `[960, 2880)`. The docs checkpoint itself changes the PR head; only a full workflow on the current live head can accept the complete PR. Do not treat earlier SHA runs as final acceptance.
7. Keep older follow-ups #30/#56/#57 isolated until their merge base and live diff are explicitly reconciled. Physical Studio MkII validation is still separate from emulator acceptance.

## Next acceptance order

1. Finish reviewing the latest PR #80 exact-SHA Android run and its evidence; repair the actual scroll/state issue if still failing.
2. Verify the current full workflow and focused native regression for PR #83 on its latest head.
3. Continue reviewing green but unmerged persistence PRs #60–67 for branch dependencies and live diffs; no automatic merge/retarget.
4. Keep the physical MPC Studio MkII validation gate distinct from emulator acceptance.

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
