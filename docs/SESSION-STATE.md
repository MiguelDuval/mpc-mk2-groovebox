# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-25  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified product-code acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Live HEAD: `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`; base SHA: `5f562e54e2cf8235041590933232a3836d74a16a`.
- **Android Build #3301 / [run 40207854314](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207854314) passed on that exact SHA, all 57 steps.** APK artifact `12604227275`; visual-evidence artifact `12605069409`. Both APK assembly/upload and the emulator smoke/evidence upload completed successfully.
- Physical Akai MPC Studio MkII validation remains a separate **PENDING hardware gate**. `.github/workflows/android-build.yml` must retain `cancel-in-progress: false`. Do not modify `main`, rewrite history, merge or retarget PRs automatically.

## Current execution queue — 2026-10-26 (live HEAD and exact-SHA checks govern)

1. **Save As evidence — PRs #58–59.** PR #58 HEAD `63ea6805e819537a96eed446bdde1b3aa518531d`, [Build #3254](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177584543) passed; PR #59 HEAD `5a12f545c6b798a6a8dce2e3f53448fb2ba06f41`, [Build #3280](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40192720803) passed. Downloaded artifact `12599204885` was inspected: three separate screenshot/XML pairs show initial Save As, the second picker with the previously created `Untitled.mpcproj`, and recovery export destination. Both PRs remain open; no merge/retarget was performed.
2. **Persistence validation — PRs #60–67.** All recorded latest HEADs are unchanged and have successful full Android workflows: #60 `596a8110565a99316ccb0d5aa2a04d1e0bba5c56` / [40177392571](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177392571); #61 `0fdba37e521e1b39a96e3c95ee9d0078a43e9883` / [40177684480](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177684480); #62 `26579254409aa1a5a5d0f0f8cac9bda5cd368832` / [40178612521](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178612521); #63 `04b4ab82635d49ca104a10a036702cf99c496c5c` / [40178770260](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178770260); #64 `85dd29b49b8a91fd9c033c4b839857778ad4674b` / [40185698676](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40185698676); #65 `396a7b46d0c2995aae3b1b240df01136fdd05ae8` / [40186113233](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40186113233); #66 `0ca8355ce704e35a57f90f6936053e54f5577296` / [40186451595](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40186451595); #67 `232923a956367c1a984c1878d014bffbec3fc6df` / [40191346634](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40191346634). The PRs are still separate open drafts with dependencies; green CI is not permission to merge or retarget.
3. **Browser local Drums/Demos — PR #80.** The old HEAD `9e62c91c5d0437649150bbcce73b3eaefce3cbf8`, [Build #3298](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40206997235), failed at smoke step 52. On `60f7b73e9a5888e9dbbdd20b7ffbb6d0e5bb4056`, [Build #3302](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40210755051) also failed at step 52: the final hierarchy showed PLACES/INTERNAL, while the script was reusing pre-navigation Browser coordinates after BROW-001 and a single swipe did not expose the root WAV row. The current PR HEAD is `67270c0a48d86ff2dcbb924edab5c6b0da01dab6`; [Build #3308](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40260800325) is **PENDING** at last check. A prior-head run #3307 (SHA `60ff6bd3b9e5a89bd96d0a9c101a8c855bde13a4`) was still running; neither prior result accepts the latest SHA. The current patch explicitly recaptures and re-enters CONTENT, then uses bounded live-scroll/capture iterations while keeping the exact-file assertion strict.
4. **Browser modified date — PR #81.** HEAD `f88b5050c93455c9e60a26d697cc0dd705448fb5`; [Build #3297](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40205769766) passed on that exact SHA.
5. **Pad Mixer accessibility labels — PR #82.** HEAD `7ae58ea9744c8b5b28608807d105398b5212d22e`; [Build #3300](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40207666754) passed on that exact SHA.
6. **Sequencer normalization overflow — PR #83.** Current HEAD `02510fe925f828b8e179b8364de0c549586fa95e`; [Build #3306](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40211396059) passed all 57 steps, including native/JVM tests, APK assembly/upload, emulator smoke and evidence upload. The fix covers both `MpcSequenceTransportClock::normalize` and `MpcSequencerCursor::setPositionTicks`, with `INT64_MIN` / `INT64_MAX` regressions.
7. **Scheduler bounded-work/ratchet overflow — PR #84.** Based on product SHA `d827a40e4bb5cafdcda5dc5bb37f2b581b6e7991`. Current code HEAD `455f4e9c0ca56f1826715a2d0b1d95fe7b49d589`; [Build #3310](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40261070841) is **PENDING** at last check. Prior-head Build #3309 on `40b1193b96fec66e021a920ac2371493efb562f3` was in progress before the follow-up refined work-budget behavior. Neither result yet accepts the current HEAD. Regressions cover whole-pattern/range scheduling with million-cycle windows, `INT64_MAX` ratchet spans, and overflowing base offsets.
8. Keep older green follow-ups (#30, #56, #57, and the other stacked PR chains) isolated until live diffs and base ancestry are reconciled. Physical controller validation remains separate from emulator acceptance.

## Next acceptance order

1. Resolve the latest exact-SHA full workflow for PR #80 and inspect its final hierarchy/evidence.
2. Resolve exact-SHA full Android CI for PR #84, including the new scheduler regressions.
3. Recheck PR #60–67 live heads/dependencies before any integration; never infer mergeability from green checks alone.
4. Keep physical MPC Studio MkII validation pending until the real controller has been exercised.

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
