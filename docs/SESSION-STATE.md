# Live checkpoint — 2026-10-30

**Canonical working branch:** `checkpoint/mpc-ui-reference-and-product-20261030`  
**Latest confirmed parent before this state update:** `a94ab477acf6476ab8f27f3ded4c0417bd9b7f0d` (the subsequent state-file commit becomes the branch HEAD; refetch the branch live in the next session).  
**Product baseline:** `ac59c5e23f87680e69e311e25d2e45d7ab1b8f8d` (`feature/mpc-one-ui-fidelity`).  
**Main:** not modified. **Physical Akai MPC Studio MkII validation:** PENDING.

## Consolidation result so far

A dedicated checkpoint branch now carries the local MPC 3.x screenshot catalog, mandatory visual-reference rules and the verified feature slices listed below. This is the single branch to continue from while the consolidated build is being accepted. Old PRs remain as historical/source records; do not develop new slices on their stacks.

| Integrated slice | Source exact SHA / evidence | Status on checkpoint |
|---|---|---|
| MPC visual screenshot catalog | PR #129; 82 VIS contexts, 402 source-image records (370 downloaded / 32 failed), 248 PDF page captures, 281 embedded crops | Integrated; 32 retrieval gaps remain explicit |
| P0 Pad/Track Mute + Reserved routes | Product SHA `f734930cae403d0794e3d4b504e4bf8f5f7c6e84`; Build #3465 run `40591850947`, PR-head Build #3466 run `40660926245` | Integrated; source emulator acceptance green |
| Native Track Solo | SHA `06d08f38d2789a0c62cf64b23cdd8e8dd2b420cd`; Build #3468 run `40663060996` | Integrated; source emulator/native acceptance green |
| Grid inverse zoom + touch proof | PR #123 SHA `b11c2a4ce55a21528f056e740df108fb81d30b9b` Build #3457 run `40578192003`; PR #117 Build #3413 run `40547525120`; PR #114 Build #3399 run `40527004496` | Integrated; combined exact-head verification pending |
| Arrangement pan/zoom proof | SHA `25f62d07d8e06e45847cca02f002066d10a4ac3d`; Build #3458 run `40578350114` | Integrated; combined exact-head verification pending |
| Back navigation context + truthful MIDI state | PR #110 Build #3412 run `40547076817`; PR #112 Build #3400 run `40527456461` | Integrated; source runs green, hardware remains pending |
| Project Save As evidence capture | PR #115 Build #3403 run `40535592616` | Integrated; source run green |
| Browser local folders / file size / modified date | PR #80 Build #3374 run `40461592533`; PR #111 Builds #3394/#3395 runs `40518877443`, `40518880896` | Core code/test blobs and acceptance blocks already present; combined exact-head verification pending |
| Browser picker cancellation | SHA `e707ffd026210cef3a022d743741f5f1f631b749`; Build #3424 run `40562838515` | Integrated; positive selection assignment and hardware SAF remain separate |
| Sample Edit CH4/8/16 evidence preservation | SHA `ade77f513404112a251ad65cbd445e33003af599`; Build #3514 run `40895016008` | Integrated; combined exact-head verification pending |

## Held changes

- PR #126 WAV export codec is NOT integrated. Its latest source run #3480 / `40710623733` failed at `MpcProjectState.cpp:209` on `-Werror=unused-variable` (`oldKind`); the decoder step itself passed. Its source also expects an `AudioEngine::exportSampleToWav` route absent from the checkpoint's current AudioEngine API. A temporary untested `encodeWav` addition to `WavSample.cpp/.h` was reverted. Re-evaluate as a fresh isolated feature only after defining the current product export API.
- PR #127 Browser XML assertion repair remains held; Build #3516 failed. Inspect the exact failing step and current checkpoint assertions before importing.
- PR #116 picker-cancel follow-up remains held after its exact source run #3411 failed; PR #119 is the accepted cancel-path source instead.
- Do not merge or copy an entire old sample-lifecycle branch just to bring over individual fixes.

## Mandatory visual-reference rule

Every agent must open the relevant `VIS-001`–`VIS-082` entry in `docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md` and visually inspect its local image/page capture from `docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md` before changing/reviewing/accepting a screen. Record VIS IDs, asset paths, and before/after evidence. MPC Standalone OS 3.9 is primary; older images are supplementary. Missing direct screenshots must be called out; no visual-reference gate is waived by a green test.

## Next actions

1. Open a Draft consolidation PR from `checkpoint/mpc-ui-reference-and-product-20261030` to `feature/mpc-one-ui-fidelity` to trigger the Android workflow on the integrated exact HEAD.
2. Inspect exact-head workflow result, job steps, APK artifact and visual-evidence archive. Validate required markers for Mute, Solo, Grid, Arrangement, Browser, Sample Edit and Project Save As; do not claim acceptance before inspecting evidence.
3. Fix only concrete failures on this checkpoint branch, rerun exact-head CI, then update this file with the resulting SHA/run/artifact identities.
4. After green consolidated CI, decide whether to merge checkpoint PR into the product feature branch. Never change `main` as part of this consolidation.

---

## Consolidated engineering checkpoint — 2026-10-30 (current handoff)

**Candidate branch:** `checkpoint/mpc-ui-reference-and-product-20261030`  
**Immutable product starting point:** PR #23 product-code SHA `ac59c5e23f87680e69e311e25d2e45d7ab1b8f8d`. The consolidated candidate is a descendant of this SHA; the product branch and `main` were not changed.  
**Validation state:** the integrated changes below were individually accepted on their own exact source heads. The combined branch has **not yet passed its own exact-HEAD Android Build** and must not be called a green/release checkpoint until that workflow and artifact inspection complete. A draft validation PR against `feature/mpc-one-ui-fidelity` is the next action.

### Integrated source slices and their independent evidence

| Slice integrated into this candidate | Accepted source evidence |
|---|---|
| Reference screenshot archive: 82 VIS contexts; local gallery, manifest and extracted assets | PR #129 source head `9c4bdbc7c673d68136ff9fff9184549aac56fff6`; local assets/INDEX and catalog vendored. Inventory: 402 source-image records, 370 downloaded, 32 failed, 248 PDF page captures, 281 figure crops, 59 direct-image sources. |
| Main Mute workspace + truthful Reserved routes | PR #124 product SHA `f734930cae403d0794e3d4b504e4bf8f5f7c6e84`, Build #3465 / [run 40591850947](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40591850947); branch/doc HEAD `e7de5881c936a8b1167ce6835ee7424944407749`, Build #3466 / [run 40660926245](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40660926245). Pad/Track 4×4 evidence and matching hierarchy inspected on source run. |
| Per-Track recorder mapping + malformed queue-field rejection | PR #86 source head `dda28c999993205d3950872f872b091ad5d59a31`, Build #3314 / [run 40343495124](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40343495124); PR #98 source head `201c2b308bd7e279aac810acf76ce76a44123f8d`, Build #3335 / [run 40379300776](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40379300776). Each armed Track uses its own assigned Drum Program; note mapping is captured at note-on; malformed velocity/pressed bytes are ignored before model mutation. |
| Native Track Solo playback and real UI state | PR #125 head `06d08f38d2789a0c62cf64b23cdd8e8dd2b420cd`, Build #3468 / [run 40663060996](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40663060996). |
| Reversible discrete Grid zoom ladder and exact inverse viewport history | PR #117 head `62c13b07aa17eaf5d2e7fec6e24b75d66e7b37e8`, Build #3413 / [run 40547525120](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40547525120); PR #123 head `b11c2a4ce55a21528f056e740df108fb81d30b9b`, Build #3457 / [run 40578192003](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40578192003). |
| Real-touch Grid zoom H/V viewport isolation and visual evidence | PR #114 head `1b177ad886f87ac18e09828daa8880825ee38a3e`, Build #3399 / [run 40527004496](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527004496). |
| Arrangement ruler pan + zoom round-trip, native event-data invariance | PR #120 head `25f62d07d8e06e45847cca02f002066d10a4ac3d`, Build #3458 / [run 40578350114](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40578350114). |
| Shell Back restores prior subcontext/focus; Project Save As fresh compressed hierarchy is retained | PR #110 head `68a83fc7f9541efc14531854d1060e5969f5429f`, Build #3412 / [run 40547076817](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40547076817); follow-up PR #115 head `cdce4f1776cce5da064bfbeebbca5f7d87d06af2`, Build #3403 / [run 40535592616](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40535592616). |
| Toolbar MIDI readiness reflects actual connected ports and reports disconnect | PR #112 head `65c651388ab67195c09687e9e67952e6b91688f4`, Build #3400 / [run 40527456461](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40527456461). Physical Studio connection remains unverified. |
| Browser local Content categories, file-size/date options and picker-cancel state preservation | PR #56 head `20e33dd7b9d2462dcd816da1281b2206948d243b`, Build #3239 / [run 40144540628](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40144540628); PR #80 head `5fe65c952c7a6771f49478f5a026be9c0ada0f38`, Build #3374 / [run 40461592533](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40461592533); PR #111 head `935e59374b5ac682ec6ccca16b66b43db28f16d7`, Builds #3394/#3395 / runs [40518877443](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518877443) and [40518880896](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40518880896); PR #119 head `e707ffd026210cef3a022d743741f5f1f631b749`, Build #3424 / [run 40562838515](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40562838515). |
| Sample Edit CH4/CH8/CH16 evidence snapshot immutability | PR #128 head `ade77f513404112a251ad65cbd445e33003af599`, Build #3514 / [run 40895016008](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40895016008). |

### Intentionally not integrated

- PR #126 WAV export codec/host-test adjustment: its exact current head has no successful exact-SHA workflow result in the available record. Keep separate until fully verified.
- PR #127 Browser XML assertion follow-up: latest exact-head Build #3516 failed. Do not integrate a failing assertion branch; diagnose separately.
- PR #122 empty-search follow-up and PR #116 duplicate picker-cancel path: recorded exact-head runs failed; not integrated. PR #119 is the accepted picker-cancel source slice.
- PR #121 is a stale/failed documentation checkpoint, superseded by this live state document.
- Source PRs remain unchanged/open as source records unless explicitly closed; do not merge them into `main`. The consolidation candidate is the single working target pending its own validation.

### Mandatory acceptance gates / next step

1. Open a Draft validation PR from this branch to `feature/mpc-one-ui-fidelity` and run Android Build on the exact returned head SHA.
2. Resolve any compile, native/JUnit, shell preflight, emulator-smoke or artifact failures without weakening acceptance assertions. Keep `cancel-in-progress: false`.
3. Inspect APK and visual-evidence artifact from that exact run; verify Grid/Arrangement, Solo/Mute, Browser, Project picker and Sample Chop evidence files, including their matching XML. Record artifact IDs/checksums only after inspection.
4. UI work must inspect the actual local MPC OS 3.9 reference image(s) first, record VIS ID(s)/asset paths and before/after proof. The rule is mandatory in `AGENTS.md`, master prompt, development protocol, blueprint, action catalog, reference index and roadmap.
5. Physical Akai MPC Studio MkII validation is still PENDING. Emulator success is not hardware certification.

Do not modify `main`, force-push, rebase, reset, rewrite history, or merge this checkpoint into the product branch automatically. Re-fetch live refs and Actions before every acceptance claim.

**Checkpoint date:** 2026-10-27  
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


---

## Latest combined candidate acceptance — 2026-10-31 re-check

**Canonical branch:** `checkpoint/mpc-ui-reference-and-product-20261030`  
**PR:** [#133 — checkpoint: consolidate accepted MPC groovebox work](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/133), still Draft and still targets `feature/mpc-one-ui-fidelity`.  
**Exact candidate SHA:** `76733e7b8b9a30e05a48e202f2db49ab18d27b62`.  
**Exact workflow:** Android Build #3593, run [41105497950](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/41105497950), completed `success` for that exact SHA. The `android-debug` job passed all 53 substantive steps, including native policy/playback/scheduler/recorder/project tests, JVM tests, debug APK assembly/upload, smoke syntax/preflight, emulator smoke and visual evidence upload. No `main` change.

### Downloaded and inspected exact-run artifacts

| Artifact | ID | Size | SHA-256 |
|---|---:|---:|---|
| `app-debug.apk` | `13001052554` | 10,433,069 bytes | `293bd5263421da1eb56b877ee4db8a391741c354462331c25c5aa7afa38de8e4` |
| `mpc-ui-visual-evidence.zip` | `13003441439` | 17,852,842 bytes | `062a834ac7b506705d033f9eb077e3472a0d754fa4cd92c804382705e54a47e6` |

The APK was verified as an Android package. The evidence ZIP contains 223 entries: 104 PNGs, 86 UiAutomator XML files, plus logs/state snapshots. 81 PNG/XML basename pairs match. The required key pairs are present for Pad Mute, Track Mute, Grid zoom baseline/H/V, Arrangement zoom, Browser local results and picker assignment/cancel, Project Save As picker, and Sample Edit CH4/CH8/CH16. Their screenshot/hierarchy pairs were visually or structurally inspected from this exact-run archive.

### Solo acceptance boundary

Native Track Solo playback policy/tests are included in the successful exact-head workflow; the workflow's source preflight also verifies the real routing functions and requires the Solo UI round-trip assertion contract. The uploaded evidence does **not** contain a dedicated Solo ON/restored PNG/XML pair. The available Track View capture shows MUTE ON and SOLO OFF, with a live accessible Solo control. The downloaded Actions log did not expose the `UI_TRACK_SOLO_ROUNDTRIP_COMPLETE` marker as a standalone line, so retain the narrower statement: **native Solo implementation/test accepted; dedicated UI Solo ON/restoration screenshot proof remains an explicit evidence gap**. Do not invent a screenshot or claim physical Studio validation.

### Reference screenshot archive inventory (checkpoint files)

- `docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md` and `docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md` contain 82 named `VIS-001`–`VIS-082` contexts; the gallery has 1,294 embedded local image references and no remote Markdown image URLs.
- `manifest.json`: 402 source-image records (370 downloaded, 32 failed); 248 rendered PDF page captures, 281 rendered figure crops, one explicit out-of-range page record; 42 total failure records comprising 32 extracted-image retrieval failures, 9 source-page-scan failures, and 1 legacy PDF retrieval failure. These failure categories are explicit, not silently counted as recovered.
- Current MPC Standalone OS 3.9 page captures are primary; older standalone manuals remain supplementary. Archive is documentation/reference material and is not packaged in the APK.
- PR #129 is still an open source-of-record PR. The archive content is vendored into this checkpoint; do not close or merge source PRs automatically.

### Remaining gates

1. Keep PR #133 Draft until reviewer/product decision; do not merge or retarget automatically.
2. Add a dedicated, exact-run Solo ON → OFF UI evidence pair (or clearly keep it excluded from UI screenshot acceptance) without weakening the native playback assertions.
3. Keep physical Akai MPC Studio MkII validation pending; emulator success is not hardware certification.
4. Preserve explicit reference-archive retrieval failures; only retry where a stable authoritative source exists.
5. PR #126 WAV export, #127 Browser assertion follow-up, #122 empty-search retry and #116 duplicate picker path remain excluded pending their own clean exact-head evidence.



---

## Live continuation state — 2026-10-31, after reference supplement publication

**Canonical branch:** `checkpoint/mpc-ui-reference-and-product-20261030`  
**Current PR-head at this state update:** `584319747ad57268b66ff9731d5a8318b0dce1df` (documentation/screenshot supplement commit).  
**PR:** [#133](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/133), OPEN / DRAFT, base `feature/mpc-one-ui-fidelity`.  
**Main:** unchanged.

### Exact combined Android verification attempts

| Build | Exact SHA | Run | Result | Interpretation |
|---|---|---:|---|---|
| #3593 | `76733e7b8b9a30e05a48e202f2db49ab18d27b62` | `41105497950` | SUCCESS | Prior consolidated code head passed all 53 substantive steps; exact-run APK and evidence were downloaded/inspected. |
| #3595 | `20d722c6fb76a9c2eab1804e600022a1ba06fbdb` | `41210241963` | SUCCESS | Follow-up checkpoint before the Solo evidence patch; APK and visual evidence archive were downloaded and inspected. |
| #3602 | `95e9968611c3679dfc8cff515f2086afe767daa4` | `41217444703` | FAILURE at smoke preflight | Static source check expected literal release names, while MainActivity constructs names dynamically from the capture phase. Native/JUnit checks and APK assembly/upload passed. |
| #3603 | `ca4b1e9083089c688b0add75689fee03ed61dc15` | `41218075435` | FAILURE at smoke preflight | Static UI description check expected whole ON/OFF literals while Java builds the descriptions from a prefix and state ternary. Native/JUnit checks and APK assembly/upload passed. |
| #3604 | `364c0c3145462af4f54217daa016e256ba59a565` | `41218372258` | FAILURE at smoke preflight | Same static content-description expectation mismatch. This run did not reach the emulator Solo capture. APK artifact `13049067180` was uploaded. |
| #3605 | `44bbe2d4591dd550b6065c32e3f688dc6c79f88a` | `41218623141` | IN PROGRESS at authoring | The current static checks use the dynamic Java description expressions. Steps 1–48 (setup, native/JUnit, MPC Studio MkII policies, sequencer, sample, project and edit-history checks) have all completed SUCCESS. Step 49, Assemble debug APK, is currently IN PROGRESS; steps 50–53 (APK upload, smoke preflight, emulator smoke and UI evidence upload) are pending. No acceptance claim until the run completes. |
| #3606 | `584319747ad57268b66ff9731d5a8318b0dce1df` | `41218984748` | ACTION_REQUIRED / no job | The workflow was triggered by the GitHub Actions bot while publishing reference supplements. GitHub required workflow approval and created no job; this is not a compile/test failure and is not a successful validation. A later user-authored checkpoint commit must start the exact-head workflow for the final branch contents. |

### Track Solo ON/OFF UI evidence change

The native Solo source/tests and routing contract were already part of the combined candidate. The missing piece was a dedicated UI ON/OFF screenshot/XML pair. MainActivity and the shell evidence flow were updated to:

1. Read the current native Solo state and verify the real Function Bar control; do not toggle before capturing the baseline.
2. Pause the application at `UI_TRACK_SOLO_CAPTURE_READY: phase=before`.
3. Capture the current native state to a PNG and a fresh, parseable UiAutomator XML hierarchy; check that the Solo node's content description, selected state, enabled/clickable flag and bounds agree with the native state.
4. Release the app through the phase-specific cache signal; toggle the real native/UI Solo state; verify sequence/track/pad selection and the complete note snapshot remain unchanged.
5. Pause at `phase=toggled`, capture the opposite Solo state as the second PNG/XML pair, validate it against native state, then release the app.
6. Restore the original Solo value and verify state/context/note data. Emit `UI_TRACK_SOLO_ROUNDTRIP_COMPLETE` only after both screenshot states and restoration are complete.

Expected uploaded evidence files:

- `mpc-groovebox-track-solo-on.png`
- `mpc-groovebox-track-solo-on-uiautomator.xml`
- `mpc-groovebox-track-solo-off.png`
- `mpc-groovebox-track-solo-off-uiautomator.xml`

The shell preflight now checks the actual dynamic description expressions, capture-phase/release-signal contracts, screenshot whitelist and final ON/OFF artifact gate. **The Solo UI evidence pair remains UNVERIFIED until Build #3605 finishes its emulator smoke and the exact-run PNG/XML files are inspected.** Do not infer UI acceptance from native tests or preflight alone.

### Reference image archive — updated after recovery

- `docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md` and `docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md` retain exactly 82 named VIS contexts.
- Gallery: 1,296 local image embeds (the original 1,294 plus two new official support-reference crops under VIS-081); no new remote Markdown image embeds.
- Original `inline_and_extracted_images` accounting is deliberately unchanged: 402 records, 370 downloaded and 32 failed.
- New, separately typed `supplemental_images` records: 2 official Akai support article crops for MIDI Program and CV Program track controls. They have source URL, page URL, local path, byte count, dimensions and SHA-256; they are not presented as MPC OS 3.9 full-page captures.
- Publisher workflow [run 41218963156](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/41218963156) succeeded and pushed commit `584319747ad57268b66ff9731d5a8318b0dce1df`. Exact source assets were verified by byte count, PNG signature, IHDR dimensions and SHA-256 before publication. Publisher artifact ID `13049530089`, SHA-256 `88740698574d6f1e4aef81838d12fa6088b44aebb6b5460e883a78cbb5c473d6`.
- `docs/MPC3-UI-REFERENCE-ASSETS/RECOVERY-NOTES.md` records the outcome. The 30 old Akai 2020 MPC-101 image URLs remain unrecovered (404; no usable Wayback CDX/Availability capture); the old banner and generic Freshdesk image are not UI references. Nine MPC-Tutor page scans remain blocked (403) and did not yield retrievable Wayback page captures. One older v2.11.7 guide PDF source remains unavailable (403). These original failures stay explicit and are not overwritten by the two new supplemental crops.

### Physical controller validation

`docs/MPC-STUDIO-MKII-CURRENT-HEAD-ACCEPTANCE.md` contains the current-head physical test protocol across USB/MIDI lifecycle, 16 pad-note map, pad velocity/aftertouch, button/transport/modifiers, Jog/Touch Strip, LEDs/RGB SysEx, LCD chunks and end-to-end recording/playback/save. All HW test IDs remain **NOT RUN** because no physical MPC Studio MkII was available to perform this test. `docs/PHYSICAL-TEST-CHECKLIST.md` links the current-head protocol and explicitly limits its older 2026-09-24 historical record.

### Immediate acceptance order

1. Let Android Build #3605 finish. Inspect its exact run outcome; if it succeeds, download the exact-run visual-evidence ZIP and verify the two Solo PNG/XML pairs plus all previously required Mute/Grid/Arrangement/Browser/Project/Sample evidence.
2. If #3605 reaches a real emulator Solo capture failure, inspect its fresh log/PNG/XML and fix the actual state-machine issue without weakening native-state, selected-state, bounds or note-invariance assertions.
3. Commit an up-to-date session-state/PR description handoff and let the final PR-head Android Build run on the newer exact SHA `584319747ad57268b66ff9731d5a8318b0dce1df` plus that handoff commit. The #3605 code run predates the two supplemental docs images and is not proof for the final PR head.
4. Keep PR #133 Draft; do not merge or retarget automatically. Keep the physical hardware gate NOT RUN and do not modify `main`.

