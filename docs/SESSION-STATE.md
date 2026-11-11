# Live checkpoint — 2026-11-11, Sample Chop smoke helper-order repair

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**Integration PR:** #139 — Draft, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `1fa503cedafa1ee5ab7635c74cccc93cc4346e43`.  
**Base:** `checkpoint/mpc-ui-reference-and-product-20261030` at `a029af66d423778d21607d6470b46bb8e4ca0b88`.

## Latest exact-head Android Build #3971

- Run [43027176657](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/43027176657) tested `1fa503cedafa1ee5ab7635c74cccc93cc4346e43` and failed in emulator smoke step 56; steps 1–55, including native/JVM, APK assembly/upload and smoke source preflight, passed.
- The emulator log proves SAMP-008 CROP passed: 3,840 source frames, crop start 1,000, output PCM 2,840 frames, native region reset to 0..2,840.
- The next CH4/CH8/CH16 setup failed before the native Chop action: `project_marker_timestamp: command not found` at the Sample Edit PLAY marker baseline. The corresponding wait helper was also declared below its first use.
- Root cause was Bash function declaration order in `tests/android_ui_smoke.sh`, not AudioEngine, CROP or Chop behavior. No CH4/8/16 acceptance claim is made from #3971.

## Forward correction in this checkpoint

- Move `project_marker_timestamp_from_logs()`, `project_marker_timestamp()` and `wait_for_new_project_marker()` above the first Sample Chop function/call site. Their bodies, monotonic timestamp semantics, final diagnostic recheck and bounded ADB behavior stay unchanged.
- Add a Python source-order preflight that requires exactly one definition of each helper and requires all three definitions to appear before `run_sample_chop_acceptance()`.
- Keep the real Sample Edit PLAY marker, running-output-stream assertion, non-destructive overwrite protection, exact CH4/CH8/CH16 frame partition checks and existing timeout budgets unchanged.

## Acceptance still open

The next exact-HEAD Android Build must pass the full workflow. Verify Browser USER FOLDER/FAVORITE 1 SAF flows, full Project Save/Open/restart/cancel/malformed/v1 lifecycle, SAMP-007 fresh idle hierarchy and SAMP-008/009/010 evidence on that same SHA. Inspect the APK and visual-evidence archive from the exact successful run; APK creation alone is diagnostic.

PR #139 remains Draft and non-mergeable. Do not merge, retarget, rebase, force-push, reset or rewrite history. PR #23 and `main` remain untouched. Physical MPC Studio MkII validation is **NOT RUN**.

---

# Live checkpoint — 2026-11-10, restore wait-helper boundary

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `fc142c92afa23faea706ebaa561e456230ce3383`.

## Exact-head Android Build #42889181952

- Tested SHA: `fc142c92afa23faea706ebaa561e456230ce3383`; run [#42889181952](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42889181952) failed in step 55, before emulator execution.
- Native/JVM and policy steps 1–52 succeeded. Debug APK assembly and upload (steps 53–54) succeeded. Step 55 failed with `tests/android_ui_smoke.sh: line 16122: syntax error: unexpected end of file`; step 56 was skipped; evidence upload step 57 ran.
- The first forward correction at `14fc395f...` removed the unexpected extra brace after `project_marker_timestamp()`. The subsequent correction inadvertently removed both adjacent braces after `wait_for_new_project_marker()`; one of those was the required function terminator.

## Forward correction

Restored exactly one closing brace after `wait_for_new_project_marker()`. The second adjacent brace remains removed. No assertions, timeout values, marker comparisons, or function bodies changed.

Next exact-head workflow must pass both `bash -n` and the screenshot capture/upload contract before emulator smoke can start. Run #3965 APK is diagnostic only.

PR #139 remains Draft. No merge, rebase, reset, force-push or history rewrite. `main` and PR #23 remain untouched. Physical Studio MkII acceptance remains NOT RUN.

---

# Live checkpoint — 2026-11-10, second smoke preflight brace correction

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `14fc395f11099bc84cb48191603028afd16854d1`.

## Exact-head Android Build #42879310935

- Tested SHA: `14fc395f11099bc84cb48191603028afd16854d1`; run [#42879310935](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42879310935) failed in step 55, before emulator execution.
- Native/JVM tests, debug APK assembly and APK upload succeeded. Step 55 passed the initial `bash -n` phase but failed on a second unmatched closing brace at line 13907. Step 56 emulator smoke was skipped; step 57 visual-evidence upload ran.
- The extra brace was immediately after `wait_for_new_project_marker()` and before `reuse_current_project_view_tree()`. A full-file structural scan found this as the only duplicate standalone closing-brace sequence after the prior correction.

## Forward correction

Removed the second stray closing brace. Both affected helpers retain their original bodies and control flow; all acceptance assertions and timeouts are unchanged. New exact-head Build is required to verify shell syntax plus screenshot capture/upload allowlists before emulator smoke.

The preceding Build #3964 also confirms all preceding native/JVM and policy test steps passed and APK assembly/upload succeeded. Its uploaded APK is diagnostic only.

PR #139 remains Draft. No merge, rebase, reset, force-push or history rewrite. `main` and PR #23 remain untouched. Physical Studio MkII acceptance is NOT RUN.

---

# Live checkpoint — 2026-11-10, smoke preflight syntax repair

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `6d30739cbf9390b33922cffeba07de7de57559c9`.

## Exact-head Android Build #42875525757

- Tested SHA: `6d30739cbf9390b33922cffeba07de7de57559c9`; run [#42875525757](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42875525757) failed before emulator execution.
- Steps 1–54 succeeded, including native/JVM test suites, debug APK assembly and upload. Step 55, `Validate Android UI smoke syntax and evidence contract`, failed; step 56 emulator smoke was skipped; step 57 visual-evidence upload ran.
- Exact shell error: `tests/android_ui_smoke.sh: line 13598: syntax error near unexpected token '}'`.
- The monotonic marker parser itself was corrected on this SHA. The failure was a separate unmatched closing brace immediately after `project_marker_timestamp()`, before `project_view_tree_uptime_from_xml()`.

## Forward correction

Removed only the stray standalone closing brace. The marker parser, monotonic freshness comparison, app-owned View-tree assertions, smoke acceptance checks and timeout budgets remain unchanged. New exact-head Actions run is required; no Android runtime acceptance is inferred from run #42875525757.

PR #139 stays Draft. No merge, rebase, force-push or history rewrite. `main` and primary product PR #23 remain untouched. Physical MPC Studio MkII acceptance remains NOT RUN.

---

# Live checkpoint — 2026-11-10, timestamp parser correction

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**Integration PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `8452e1102d51f4477c78d90e24bc186dd55a856a`.

The monotonic Logcat freshness implementation introduced in the parent commit had an over-escaped raw Python regex. Corrected the parser to match the leading device-monotonic timestamp (with or without surrounding brackets/spaces) and convert it to integer microseconds. The freshness contract remains strict: newest timestamp must be greater than the baseline timestamp.

Run #42875383559 was already triggered on parent SHA `8452e1102d51f4477c78d90e24bc186dd55a856a` before this parser correction and is not authoritative for the corrected source. Do not claim its runtime result validates this parser. The next workflow triggered by the corrected SHA is authoritative.

---

# Live checkpoint — 2026-11-10, Logcat rollover correction

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**Integration PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Parent HEAD:** `803541960c1fe838a3bed907f95029b00ef77f4a`.

## Exact-head Android Build #42862450499

- Tested exact SHA `803541960c1fe838a3bed907f95029b00ef77f4a`; run [#42862450499](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42862450499) failed at emulator smoke step 56.
- Steps 1–55 passed, including native/JVM tests, APK assembly/upload and smoke syntax/evidence preflight. Step 57 uploaded visual evidence.
- The Menu action itself succeeded: the log contains `MPC_MENU_RENDER_COMPLETE` and `MPC_MENU_CLICK_END currentPage=MENU`, followed by a fresh Menu app-owned hierarchy.
- Root cause: `project_marker_count` counted matches only in the last 4,096 Logcat entries. This is a circular buffer; after rollover, a new event can replace an old match while the total count remains unchanged. The previous final-read/diagnostic-read patch could not solve that invariant.

## Forward correction

`tests/android_ui_smoke.sh` now records the latest matching marker's device-monotonic timestamp (`logcat -v brief,monotonic`) as integer microseconds. Every wait compares the newest timestamp against its original baseline. Freshness therefore remains ordered through ring-buffer rollover and is independent of wall-clock corrections. Diagnostic fallback and the final post-deadline recheck use the same timestamp parser; no event assertion or timeout budget is weakened.

The separate app-owned View-tree acceptance continues to use the independent `generated-at-uptime-ms` contract.

## Verification boundary

The next exact-HEAD Android Build is authoritative. Do not call P0 green until step 56 emulator smoke and step 57 visual evidence upload both succeed on that exact SHA. PR #139 remains Draft; no merge/rebase/force-push or `main` changes. VIS-054 Next Sequence work remains behind this P0 Project/Browser acceptance gate. Physical MPC Studio MkII acceptance remains separate and NOT RUN.

---

# Live checkpoint — 2026-11-10, post-run #42840353384

**Repository:** MiguelDuval/mpc-mk2-groovebox  
**Integration PR:** #139, branch `feature/firmware-informed-midi-list-edit-20261106`.  
**Source SHA before this forward-only correction:** `9d29d2987ba1732522f1926f2979507ab505941f`.

## Exact-head Android Build #42840353384

- Exact HEAD: `9d29d2987ba1732522f1926f2979507ab505941f`.
- Native/JVM tests and APK assembly/upload succeeded.
- Android UI smoke reached late Project → Menu acceptance; the full workflow failed at step 56.
- The monotonic Android View-tree capture worked: Project Open emitted a fresh app-owned MAIN snapshot at uptime 1,069,978 ms and the smoke validated 232 nodes.
- Browser BROW-010 cancellation had one bounded UiAutomator dump timeout (8s); the next fresh attempt succeeded and BROW-010 passed with unchanged target Pad/Layer/sample/Browser state.
- The terminating failure was a Logcat marker race. `wait_for_new_project_marker` timed out waiting for `MPC_MENU_RENDER_COMPLETE`; the diagnostic log immediately afterward contained `MPC_MENU_RENDER_COMPLETE`, `MPC_MENU_CLICK_END currentPage=MENU` and a fresh Menu hierarchy marker. This was not a missing Menu render or failed navigation.

## Forward correction

The marker waiter now checks three bounded evidence points after its deadline: the existing final poll, the post-timeout diagnostic snapshot itself, and a short post-diagnostic recheck. Every branch preserves the strict `count > original baseline` requirement; no stale marker or cached UI tree can satisfy the wait. The Browser picker retry remains unchanged because its retry already recovered and passed.

## Verification boundary

The next full Android Build on the exact new HEAD is authoritative. Do not mark UI acceptance complete until step 56 (emulator smoke) and step 57 (visual evidence upload) both succeed on that same SHA. Keep PR #139 Draft and do not merge/rebase/force-push or modify `main`. Physical MPC Studio MkII acceptance remains separate.

---

# Live checkpoint — 2026-11-10 integration pass

**Repository:** MiguelDuval/mpc-mk2-groovebox.  
**Working candidate:** PR #139, branch `feature/firmware-informed-midi-list-edit-20261106`. The authoritative branch HEAD is always the current GitHub ref; do not rely on a copied SHA from this note. PR #139 remains a draft integration candidate and is not mergeable into its checkpoint base at this stage. Do not merge, rebase, force-push, or rewrite history. PR #23 and `main` remain outside this work.

## UI smoke synchronization correction

This pass updates the Android UI smoke harness and Project persistence View-tree capture on PR #139:

- The Activity observes the window decor's live `ViewTreeObserver`, requests a fresh root traversal and publishes the app-owned View tree from the next pre-draw. This covers state-only SAMPLE playhead/accessibility updates that do not reliably schedule a workspace-child traversal.
- View-tree freshness is compared using app-owned XML's monotonic `generated-at-uptime-ms`, with strict page/provenance/non-empty/control validation retained. Baseline collection is batched to two ADB reads; bounded Logcat counts remain for diagnostic/application events.
- SAMP-007 stops after preserving fresh in-region and advancing native cursor snapshots. It then waits for a newer SAMPLE XML whose native playhead is idle; it no longer depends on a particular Logcat message or a fixed 11-second delay. The edited region start/end/total and idle cursor remain asserted exactly.
- Reuse of an already-validated View tree updates the current evidence pointer; the v1 Project-open acceptance reuses the fresh Main snapshot rather than racing a duplicate redraw.
- Shortcut editor freshness checks use monotonic XML timestamps. Existing 16 Levels, Pad Perform, arrangement, project, browser and other acceptance coverage is preserved.

## Verification status

The pre-existing PR #142 run `42627399409` failed in the SAMP-007 *Logcat stop-marker wait* after in-region cursor movement had already passed. That run was on a stale, divergent base and is diagnostic evidence, not the current PR #139 head. The changes above are a forward-only commit on PR #139. The new exact-head GitHub Actions run is the acceptance authority; do not call this pass green until the full workflow, including Android emulator smoke and visual-evidence upload, succeeds on the exact live branch SHA.

Physical Akai MPC Studio MkII acceptance remains separate and has not been established by emulator CI.

---

# Live checkpoint — 2026-11-07 16:28 CET

**Repository:** MiguelDuval/mpc-mk2-groovebox.  
**Primary product PR #23:** OPEN, head branch feature/mpc-one-ui-fidelity at ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d, base fix/ui-shell-format-width-startup at 5f562e54e2cf8235041590933232a3836d74a16. PR #23 is unchanged. Its last verified exact-head Android Build is #3402 / run 40528666946: 53/53 substantive steps successful, APK artifact 12746190315 (10,009,933 bytes; APK SHA-256 1bfb1f64254e6c2efda3b1f1118b169e4b648c35994b25c44524b77569a229aa), visual-evidence artifact 12746712856 (15,218,255 bytes). No newer run for this PR head was present in the live run listing.
**Current integration candidate:** PR #139, OPEN / DRAFT, head branch feature/firmware-informed-midi-list-edit-20261106. Exact head before this forward fix: b693b02655306d2497f697d10090ac71a487bde6. The current checkpoint base branch ref is ec6a0d351c35adca46ac9a7efc30c8e4d89cd7c3; PR #139's merge base remains a029af66d423778d21607d6470b46bb8e4ca0b88, so the candidate is 45 commits ahead / 10 behind and GitHub reports it not mergeable. Do not rebase, merge, force-push or rewrite history. Keep it a separate validation candidate.
**Current checkpoint workflow:** Build #3800 / run 42387293216 is in progress on checkpoint branch HEAD ec6a0d351c35adca46ac9a7efc30c8e4d89cd7c3; last observed at step 6, Setup Gradle 9.6. Do not write to the checkpoint branch while that run is active.
**Separate repair PR #137:** OPEN / DRAFT, head 554193b835d3e43bc778375beea60fc9e7b79661. Its older Build #3724 failure was on 5bc8b07f... at C++ compile (missing close of the List Edit extern-C block); commit a818c838... added the close, and the current PR #137 head contains that correction. No exact-head workflow was found for 554193b. This repair stack is not being merged into PR #139 wholesale.
**Main:** unchanged. **Physical Akai MPC Studio MkII acceptance:** NOT RUN.

## GitHub-only platform contract

GitHub Actions is the only authoritative CI/CD system and the only accepted APK delivery path. A local build or an APK uploaded by a failed workflow is diagnostic only. Final acceptance requires one successful full workflow on the exact reported HEAD, including native/JVM tests, APK assembly/upload, emulator smoke and visual-evidence upload. Inspect both artifacts from that same successful run and compute the APK file's own SHA-256 separately from any artifact-archive digest.

## Latest exact-head Android Build — PR #139

- Build #3791 / run 42382469230, exact SHA b693b02655306d2497f697d10090ac71a487bde6.
- Job android-debug: steps 1–55 SUCCESS; step 56, Run Android emulator smoke test, FAILURE; step 57, Upload MPC UI visual evidence, SUCCESS. This is not a green build.
- APK artifact 13578416781, 11,291,229 bytes. Downloaded file identified as an Android APK; SHA-256 4655f3d69e59bd9859231f9833e64670d4d6433b11d0fa1301497cb228991a54. Diagnostic only.
- Evidence artifact 13578569037, 15,496,201 bytes; downloaded ZIP SHA-256 eea739e5c4550eddc0151d7396cb7d27b2095a7c84220415fdf4a4d340a0aa8a. PNG/XML were inspected.
- SAMP-008 CROP passed: 3,840 source frames, crop start 1,000, resulting PCM 2,840 frames, native region reset 0..2,840.
- SAMP-009 CH4/CH8/CH16 passed exact contiguous, non-overlapping native slices covering all 2,840 frames; evidence boundaries were 0/710/2,840, 0/355/2,840 and 0/177/2,840.
- SAMP-010 zoom-in/out/reset and native Pad audition passed without sample-data mutation. Do not expand this work before primary Browser/Project acceptance is green.
- Browser Favorite 1 assignment, independent SAF URI preferences and process-restart restoration passed. BROW-013 modified-date option, real metadata rendering, persistence and inverse restore passed. Browser WAV selection/audition/Pad assignment also passed. The run remains failed overall.
- Project Save As → mutate → Open restored the initial two-Track/two-Placement model and exact domain SHA; direct SAVE overwrite changed the persisted domain SHA. The later Open of mpc-isolation-5273.mpcproj logged PROJECT_OPEN_COMPLETE with persistedDomainExact=true and domain SHA f7b2814435b816904dcce6e7c47e019aeb79620a46e4c9a5a4682d38ffd6c4af (3 Tracks / 3 Patterns / 3 Placements / 2 MIDI notes).
- Failure: the smoke's 20-second wait expired at 15:03:00.440; the app emitted PROJECT_SMOKE_VIEW_HIERARCHY_READY page=MAIN at 15:03:00.516, 76 ms later. The app had loaded the project; the test exited before pulling the fresh Main XML. The archived project-current-view-hierarchy.xml was still page PROJECT (uptime 1,128,902 ms), and project-extended-open.png showed the DocumentsUI folder before the selected file returned. No fresh Main XML was archived for that failing Open.
- Prior e277 fix already changed project_marker_count to a fixed 5-second bounded logcat read with -t 4096. Run #3791 shows a second, distinct boundary race: the final logcat -d snapshot can start before the marker and return after the deadline, with no subsequent read.
- This commit adds one final bounded marker-count read after the wait loop. Acceptance still requires count > the original baseline; no cached-tree fallback, fabricated marker or relaxed XML provenance is introduced. Exact resulting SHA and workflow run will be recorded after GitHub confirms the forward commit.

## VIS-050 reference review — 16 Levels

Inspected the actual local MPC 3.9 reference image, not only its catalog entry: docs/MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-080.jpg and figures/page-080-figure-01.jpg / figure-02.jpg. VIS-050 shows the 16 Levels modal with Active, Type, Pad and (for Tune) Original Pad controls. The current Android touch page is a product-specific 4×4 velocity-grid adaptation with source-Pad selection; it is not a pixel-for-pixel replica of that modal. Only Velocity is implemented; Tune remains RESERVED for recording because SequenceRecordEvent has no note-variation field; Filter/Layer/Attack/Decay remain RESERVED. Physical Studio MkII mapping is not accepted until hardware validation.

## Next acceptance order

1. Verify the forward-fix commit and run on its exact SHA. Inspect the fresh app-owned MAIN XML after Project Open: page=MAIN, source=android-view-tree, positive generated-at-uptime-ms newer than the previous snapshot, non-empty nodes and the unique Main toolbar target.
2. Continue the full Project path: Save As → mutate → Open, direct Save overwrite with changed serialized SHA, force-stop/relaunch/Open, Cancel Save As/Open state invariance, malformed-load preservation and placement-free v1 via the real picker.
3. Keep Browser USER FOLDER and FAVORITE 1 real DocumentsUI touches, exact WAV, SAF permission and process-restart restoration strict.
4. Reconfirm SAMP-008 and SAMP-009 on the same exact SHA. SAMP-007 has no pass record in #3791 and remains unaccepted. SAMP-010 was exercised in #3791 but stays isolated from further expansion.
5. Only after a complete exact-head green workflow, move to the next roadmap gap. Physical MPC Studio MkII validation remains a separate NOT RUN gate.

Workflow guardrails: .github/workflows/android-build.yml retains cancel-in-progress: false. PR #23 and main are untouched. The checkpoint branch is not being modified while Build #3800 is active. No merge, reset, rebase, force-push or history rewrite.

## Historical notes

Everything below this live handoff is retained for engineering lineage. Older PR #133 / PR #23 status blocks, old branch names, CI run numbers and “next action” lists are historical unless independently reconfirmed against live GitHub.

---


## Historical consolidated engineering checkpoint — 2026-10-30 (superseded by the live 2026-11-07 state above) (current handoff)

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



---

## Superseding exact-head acceptance update — 2026-10-31

This section supersedes the older pending/unverified Solo wording earlier in this file.

### Confirmed Track Solo OFF/ON evidence

**Android Build #3605** — run `41218623141`, exact SHA `44bbe2d4591dd550b6065c32e3f688dc6c79f88a`: **SUCCESS**. Native/JUnit, APK, smoke preflight, emulator smoke and visual-evidence upload all passed.

**Android Build #3606** — run `41218984748`, exact SHA `584319747ad57268b66ff9731d5a8318b0dce1df`: **SUCCESS**. This exact head includes both supplemental Akai Program reference screenshots. Full Android Build and emulator UI smoke passed; artifact upload completed.

Downloaded and inspected exact-run evidence from #3606, archive ID `13050917539`:

| File | Bytes | SHA-256 | Verified UI state |
|---|---:|---|---|
| `mpc-groovebox-track-solo-off.png` | 230150 | `f46a1b4f93967ee47506eb7416fcbf32e6f537e6c6986a5bf3f1a30157a6d69d` | OFF visually dark |
| `mpc-groovebox-track-solo-off-uiautomator.xml` | 56487 | `5bf1cf4631a03efe0948be14d8d37364ddd7c193658690cb71fc2fa324bdeeed` | exactly one Solo node; `selected=false`, clickable/enabled; bounds `[1570,975][1794,1080]` |
| `mpc-groovebox-track-solo-on.png` | 230361 | `ecc8d511198b8aadf96705002ad54c975dee72803b1edc815d8832b352ed39a8` | ON visually green |
| `mpc-groovebox-track-solo-on-uiautomator.xml` | 56493 | `fa7c9c0afc5f40c9c438f481f64391ecb2f970eb09ae0e7dd6f10ec197cc634b` | exactly one Solo node; `selected=true`, clickable/enabled; same bounds |

Both XML hierarchies contain exactly one `MPC One Main Toolbar` node. The screenshots were opened and visually checked; the ON/OFF pair is not inferred from native tests alone. Build #3606's log independently reports:
- `Track Solo OFF verified: clickable selected=false bounds=(1570, 975, 1794, 1080)`
- `Track Solo ON verified: clickable selected=true bounds=(1570, 975, 1794, 1080)`

This closes the dedicated Track Solo visual-evidence gap for the successful #3606 code+reference checkpoint.

### Subsequent flaky Track Edit UI smoke and correction

**Android Build #3607** — run `41219642887`, exact SHA `ae7f52a954eac1dc37d46b41385a5ec273ecec82`: FAILURE in emulator smoke, after Solo OFF/ON had passed and earlier Track Edit GAIN/PAN, region and velocity field-isolation tests had passed.

The failing assertion was the final re-entry into upper Track Edit parameters after velocity MIN/MAX acceptance. The last UiAutomator XML showed TUNE/PAN rows in the current ScrollView while the GAIN row remained just above the viewport. The old helper switched back toward the lower controls as soon as the velocity row ceased to be visible, causing alternating forward/reverse gestures. This was a smoke-helper scroll-state defect; it did not indicate a failed native GAIN edit or failed Solo behavior.

Fix commit: `dea4297a54caf60daae874adcab9b5c09480f1bc`, `tests/android_ui_smoke.sh`. The helper now locks the first evidence-derived direction (reverse when returning from velocity; forward when approaching from waveform/region) until the complete GAIN/TUNE/PAN visibility contract succeeds. It does not weaken the target visibility or field-isolation assertions.

### Current verification after the scroll-helper fix

**Android Build #3608** — run `41226252891`, exact SHA `dea4297a54caf60daae874adcab9b5c09480f1bc`:
- setup, all native/JUnit checks and APK assembly: SUCCESS;
- APK artifact ID `13051293765`, 10,433,069 bytes;
- smoke syntax/evidence preflight: SUCCESS;
- emulator smoke: in_progress;
- evidence upload: pending.

Live step summary at the time of this document update: 49 Assemble debug APK: success; 50 Upload debug APK: success; 51 Validate Android UI smoke syntax and evidence contract: success; 52 Run Android emulator smoke test: in_progress; 53 Upload MPC UI visual evidence: pending. Do not call #3608 green until its emulator stage and exact-run evidence upload complete.

### Latest repository / PR / external gates

- PR #133 remains OPEN / DRAFT, base `feature/mpc-one-ui-fidelity`, current branch `checkpoint/mpc-ui-reference-and-product-20261030`, current code SHA `dea4297a54caf60daae874adcab9b5c09480f1bc` at the start of this state update.
- Main remains unchanged. No merge, rebase, force-push or branch retarget was performed.
- Reference archive remains at 82 named VIS contexts / 1,296 local image embeds; original manifest accounting is 402 source-image records, 370 downloaded / 32 original URL records marked failed, plus two separate downloaded official Akai Program supplements.
- Physical MPC Studio MkII acceptance remains **NOT RUN**; no emulator build or native test is a substitute for the physical USB-MIDI / LED / LCD checks in `MPC-STUDIO-MKII-CURRENT-HEAD-ACCEPTANCE.md`.


---

## Superseding consolidated configuration handoff — 2026-10-31

This section supersedes the earlier pending/in-progress wording for Android Builds #3608 and #3609 above. For the cross-branch feature matrix, source PRs, acceptance references and explicit exclusions, see [MPC-GROOVEBOX-CONFIGURATION-CATALOG.md](MPC-GROOVEBOX-CONFIGURATION-CATALOG.md).

**Canonical integration branch:** checkpoint/mpc-ui-reference-and-product-20261030  
**PR:** [#133 — checkpoint: consolidate accepted MPC groovebox work](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/133), OPEN / DRAFT, target feature/mpc-one-ui-fidelity.  
**Verified candidate immediately before this documentation handoff:** 727f8c3cd931171161e3fb5bc6a99c711cce9949.  
**Product baseline:** ac59c5e23f87680e69e311e25e2d45d7ab1b8f8d.  
**Main:** unchanged. No merge, rebase, force-push or retarget was performed.

### Exact combined Android acceptance — both final gates successful

| Build | Exact SHA | Run | Result | Scope |
|---|---|---:|---|---|
| #3608 | dea4297a54caf60daae874adcab9b5c09480f1bc | 41226252891 | **SUCCESS** | Includes the Track Edit scroll-helper correction; all 53 substantive Android workflow steps passed, including native/JUnit, APK, smoke preflight, emulator smoke and evidence upload. |
| #3609 | 727f8c3cd931171161e3fb5bc6a99c711cce9949 | 41226976655 | **SUCCESS** | Exact checkpoint HEAD; all 53 substantive Android workflow steps passed, including native/JUnit, APK assembly/upload, emulator smoke and evidence upload. |

### Exact-run artifact records

| Build | Artifact | ID | Size | GitHub artifact SHA-256 |
|---|---|---:|---:|---|
| #3608 | app-debug.apk | 13051293765 | 10,433,069 B | 5e67867de8a00523a1610de99abff32a406f2e497d39752c81174e2a5ab831a5 |
| #3608 | mpc-ui-visual-evidence | 13053086005 | 18,234,393 B | 078113a3706aab9243e1b93fea5704051e84a274491c4365aecf18dd29b80137 |
| #3609 | app-debug.apk | 13054637251 | 10,433,069 B | 10e8d34f73a2232b2360587d959927d304b12514a4be741b158abce47d019544 |
| #3609 | mpc-ui-visual-evidence | 13055390134 | 18,169,816 B | 0a071ecf77e98e1364294cc2d214dd2987e0d7db8ef2f1292ae6007fb68287c1 |

The GitHub artifact digests were checked against the downloaded artifact ZIPs. Each visual-evidence archive contains 227 entries, including 106 PNGs and 116 XML hierarchies. Every PNG decoded successfully and every XML parsed successfully in both archives. Required image/hierarchy sets are present for Mute, Solo ON/OFF, Grid H/V zoom, Arrangement pan/zoom, Browser local result and picker assignment, Sample Chop CH4/CH8/CH16, and Track Edit gain/pan/tuning/velocity checks.

### Inspected exact-run behavior

- **Mute:** separate Pad Mute and Track Mute workspaces. The Pad view shows a 4×4 grid with Pad 01 MUTE ON and the remaining Pads OFF. The Track view exposes actual Track slots and disables empty slots rather than fabricating Tracks. Native values are toggled and restored by the smoke.
- **Solo:** OFF and ON each have a dedicated PNG and fresh XML. OFF is selected=false; ON is selected=true; both are enabled/clickable and use identical bounds [1570,975][1794,1080]. The ON screenshot visibly highlights Solo. Native playback policy tests cover Solo routing, Mute precedence, multiple Solo Tracks and active-Sequence scope.
- **Grid / Step:** real H/V viewport changes are evidenced while the selected Pad/Step and native note state remain coherent. Existing exact-source tests cover Draw/Erase and Step parameter edit/remove.
- **Arrangement:** ruler pan and zoom-in/zoom-out/reset evidence is present. Acceptance checks compare native event data before/after viewport-only changes. Existing source acceptance covers loop-brace restoration, MOVE ± GRID, DUP/COPY/CUT/PASTE, Undo/Redo, empty-timeline paste cursor and Pattern placements.
- **Browser:** local WAV result and picker assignment captures identify the selected file and target PAD 01 / LAYER 01; cancellation evidence is separate. File-size and modified-date options, local Content categories and picker state-preservation remain part of the integrated source set.
- **Track Edit / Sample:** the corrected helper reaches GAIN/TUNE/PAN after velocity-range testing without reversing scroll direction. Exact archives contain independent Gain, Pan, Tune, Velocity MIN/MAX and sample-region evidence. Sample Chop CH4/CH8/CH16 has dedicated PNG/XML snapshots.
- **Project / engine:** the workflow's native and Android checks for project state/versioned save-load, Pattern operations, sequencer runtime, audio trigger queue, recording, MIDI decoder and MPC Studio MkII policy suites all passed.

### Physical hardware boundary

Physical Akai MPC Studio MkII acceptance remains **NOT RUN**. These successful Android/emulator and native test runs do not certify physical USB-MIDI, pad velocity/aftertouch, LEDs/RGB SysEx, LCD transport, Jog Wheel or Touch Strip behavior. The physical protocol remains in MPC-STUDIO-MKII-CURRENT-HEAD-ACCEPTANCE.md.

### Still excluded / not accepted

- PR #126 WAV export: not integrated; its source acceptance failed and its expected export API does not match the current checkpoint API.
- PR #127 Browser XML assertion repair: held after failed exact-head run.
- PR #122 Browser empty-search retry: held after failed exact-head run.
- PR #116 duplicate picker-cancel path: held after failed exact-head run; PR #119 is the accepted cancel-state source.
- Reserved/unsupported areas remain unavailable unless their real domain/backend is implemented. This catalog does not claim completion of every MPC 3.x feature.

PR #133 remains Draft for product review. Do not merge automatically or change main.

## SAMPLE playback cursor lifecycle — in progress (2026-11-05/06)

**Root cause identified:** `startSamplePlaybackCursorUpdates()` scheduled `waveformUiHandler.postDelayed(this, 33L)` unconditionally at the end of every callback — even when no playback cursor exists. This continuous ~30fps main-thread activity prevents Android UiAutomator's global idle detection from completing, causing `dump_device_ui`/`wait_for_ui_description` calls after tapping SAMPLE to open Sample Edit to time out.

### Fixes committed on checkpoint/mpc-ui-reference-and-product-20261030

- **Commit `c2d02463`**: Moved `SAMPLE_CURSOR_UPDATER_STARTED` log out of `Runnable.run()` into `startSamplePlaybackCursorUpdates()` so it records one event per start, not ~30fps.
- **Commit `cc97dfb0`**: Added `scheduleProjectPersistenceViewHierarchyCapture("SAMPLE")` in `showSamplePage()` so the app publishes its own SAMPLE view-tree snapshot instead of relying on external UiAutomator dumps.
- **Commit `00814f9b`**: Added `scheduleProjectPersistenceViewHierarchyCapture("SAMPLE")` after region edits (`nudgeRegionStart`, `nudgeRegionEnd`, `resetRegion`, `cropRegion`), after PLAY button press, on meaningful cursor bucket changes, and on playback stop (grace exhausted). Added bounded startup grace (10 retries at 33ms) and cancellation on page exit/destruction.
- **Commit `3dca8cc3`**: Fixed SAMPLE view-tree freshness protocol — removed duplicate `SAMPLE)` case in `capture_project_persistence_view_tree()`, and update `PROJECT_SAMPLE_TREE_MARKER_BEFORE` baseline after each successful capture so subsequent captures require fresh snapshots.
- **Commit `fd723a93`**: Removed redundant second `capture_project_persistence_view_tree "SAMPLE"` call after entering Sample Edit that would time out because no new marker is generated without a state change.

### CI status (PR #133)

| Build | Run | Head SHA | Step 55 result | Notes |
|---|---|---|---|---|
| #3707 | 42041254623 | `cc97dfb0` | FAILURE | Steps 1-54 SUCCESS; emulator smoke test failed |
| #3709 | 42043947543 | `00814f9b` | FAILURE | Steps 1-54 SUCCESS; emulator smoke test failed |
| #3710 | 42063374222 | `3dca8cc3` | FAILURE | Steps 1-54 SUCCESS; emulator smoke test failed |
| #3711 | 42196891082 | `fd723a93` | FAILURE | Steps 1-54 SUCCESS; emulator smoke test failed |

**Current branch HEAD:** `fd723a93ec06f8d8c2f0882ba557b6365f032103` (pushed to origin).

All four builds compiled and assembled the APK successfully (steps 1-54 pass), but step 55 (emulator smoke test) consistently fails after ~75 seconds. Detailed step-55 logs are not accessible via the available API. The failure may be in the SAMPLE view-tree capture protocol, the `tap_visible_text_early "SAMPLE"` action, or an earlier test section affected by the `project-persistence` smoke-mode restart.

**Not yet verified:** complete idle → playback → idle → playback lifecycle; physical MPC Studio MkII validation remains PENDING.


---

## Project / Browser Android smoke — logcat timestamp format correction (2026-11-10)

**Canonical branch:** `feature/firmware-informed-midi-list-edit-20261106`  
**Parent tested:** `7ce7de8356408c20a5bc646a438248cc919983b4`  
**Build #3966:** [run 42890159999](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42890159999) — **FAILURE in emulator smoke**, after steps 1–55 succeeded.

### Confirmed cause

- The syntax/evidence preflight passed; native and JVM test suites passed; the debug APK assembled and uploaded.
- The emulator cold-started, loaded the Project persistence fixture, and published a fresh app-owned MAIN hierarchy.
- A real touch opened Menu. App logs contain `MPC_MENU_RENDER_COMPLETE`, `MPC_MENU_CLICK_END currentPage=MENU`, and `MPC_MENU_VIEW_HIERARCHY_READY`.
- The smoke helper nevertheless timed out with `baseline=0 current=0`. Its timestamp reader used `logcat -v brief,monotonic`; Android's `brief` format omits timestamps, so the numeric monotonic parser had no timestamp to read. This is a smoke-observability defect, not evidence that the Menu failed to open.
- Forward fix: use `threadtime,monotonic` for both event timestamp reads and timeout diagnostics, and update the static synchronization contract. Keep strict monotonic timestamp comparison; do not fall back to match counts or weaken UI assertions.
- Build #3966's APK and partial visual-evidence archive are diagnostic only. They do not establish Project/Browser acceptance because the emulator run stopped before the full scenario.

### Remaining gates

Run the complete exact-HEAD Android workflow with the timestamp format correction. Require emulator smoke SUCCESS and inspect exact-run APK plus complete visual-evidence artifact. Continue to keep PR #139 Draft; do not merge/rebase/force-push. Physical Akai MPC Studio MkII acceptance remains NOT RUN.


---

## Browser SAF file-picker acceptance — list-mode retry correction (2026-11-11)

**Branch:** `feature/firmware-informed-midi-list-edit-20261106`  
**Parent:** `4d1a752c962472d4ffd0a7d39a72505e4c0756bf`  
**Build #3967:** [run 42905384984](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/42905384984) — **FAILURE in emulator smoke, step 56**.

### Confirmed behavior

- Steps 1–55 passed, including native/JVM suites, APK assembly/upload and Android smoke syntax/evidence preflight. The monotonic logcat timestamp fix is verified by the emulator advancing beyond the earlier Menu marker timeout.
- Browser local WAV scan, selection, audition, assignment and BROW-013 modified-date acceptance passed before the SAF scenario.
- The SAF test copied `Browser-Picker-Return.wav` into `/sdcard/Download`; the DocumentsUI hierarchy exposed its preview description, proving the file was present in Downloads.
- The test did not accept that preview as a real row. The captured tree still advertised `List view` (meaning grid mode remained active), and did not expose filename text. UiAutomator also had a bounded dump timeout during the wait.
- Forward fix: retry the measured `List view` touch at most twice, inspect the resulting fresh DocumentsUI hierarchy after each touch, and continue only after the provider advertises `Grid view` (the list-mode toggle action). Increase the file-row text wait to 30s with per-dump cap 8s. Keep the requirement for real filename text and selectable row; do not select the preview icon or bypass SAF.
- This improves bounded recovery without weakening the acceptance condition.

### Remaining gates

Re-run the complete exact-head workflow. Require actual selectable filename row, successful Activity result and correct Pad/Layer assignment; then complete the remainder of Project save/load/relaunch/cancel/malformed/v1 tests. Require full workflow SUCCESS and inspect artifacts from that exact run. Keep PR #139 Draft; physical MPC Studio MkII acceptance remains NOT RUN.

## Build #3968 audit — APK provenance, binary delta and SAMP-009 correction (2026-11-11)

- Build #3968 (43008353222) was triggered by a push to feature/firmware-informed-midi-list-edit-20261106; run head_sha, PR #139 head_sha, and branch ref all matched 560dad0affaa3e9e025e3690a1e38d52f584e354. The workflow checks out the triggering ref and assembles :app:assembleDebug from android/app/build/outputs/apk/debug/app-debug.apk; artifact metadata records the same branch and SHA. This is not a wrong-branch artifact.
- APK size was exactly 11,365,677 bytes for #3954, #3967 and #3968. In all inspected APKs the four ABI libraries, classes.dex, classes2.dex, manifest and resource table were byte-identical. Since #3954, 13 commits changed only MainActivity.java (33 additions / 11 deletions, largely UI acceptance instrumentation), tests/android_ui_smoke.sh, and this state log; no src/Audio, src/MPC, or src/NativeEngine.cpp source changed. This explains the stable package weight: the iterations were predominantly smoke-test repair, not large engine feature additions.
- #3968 passed native/JVM tests, APK assembly/upload and smoke preflight. Project Save/Open and the preceding Browser acceptance sections completed; step 56 failed at SAMP-009 because chop() refreshed the Sample inspector but did not schedule a new app-owned SAMPLE View-tree capture. The smoke therefore saw the pre-CH4 generated-at-uptime-ms and timed out.
- Corrective source change: after successful CH4 state mutation and inspector refresh, chop() now calls scheduleProjectPersistenceViewHierarchyCapture(SAMPLE), matching Crop and region-edit flows. The next exact-HEAD Android run must confirm the full smoke and visual evidence; no P0-green or release claim is made yet.

## Build #3969 failure analysis — SAMP-009 fixture collision (2026-11-11)

- Exact SHA: 4304290e2051d8b6d751c7230638b0decefaf706. Branch, PR #139 head and APK artifact provenance matched; PR #139 remained Draft.
- Native/JVM, APK assembly/upload, smoke preflight, Browser acceptance, extended Project Save/Open (including process restart/cancel/malformed/v1), SAMP-007 cursor motion/idle and SAMP-008 Crop all passed on this exact SHA.
- The first SAMP-009 CH4/CH8/CH16 suite passed earlier in the same run. It used one source PCM buffer and verified exact contiguous native regions.
- The later repeated CH4 attempt used a different long cursor WAV assigned to Pad 1 while Pads 2–4 still contained the earlier fixture's different PCM buffer. AudioEngine::chopPadSampleToPads correctly rejects replacing an occupied destination Pad with another buffer before mutation. The later smoke block incorrectly expected this destructive overwrite to succeed. Its repeated View-tree polling obscured the native precondition failure.
- Forward correction: the first clean-pad CH4/CH8/CH16 pass now starts the output stream before CH4 and requires native result markers to prove streamWasRunning=true and streamRunningAfter=true. The later cursor-fixture CH4 interaction now asserts the non-destructive conflict rejection and unchanged source region. Every Chop attempt emits a structured result marker. App-owned pre-draw capture requests are coalesced so repeated inspector refreshes replace one pending listener instead of queuing stale callbacks.
- The native refusal to overwrite an unrelated assigned sample is preserved. The next exact-HEAD run must pass the positive stream-restart assertion and the negative overwrite-protection assertion, then the remaining P0 gates.

## Build #3970 pre-emulator smoke correction — Sample PLAY marker (2026-11-11)

- Exact source SHA before this fix: 66b0bb62ef2fd4c0f2fdc66bb91b1cd274928723. The action was still building at step 53; its checked-out SHA remains immutable.
- Static inspection found the new clean-pad audition gate was waiting for UI_SAMPLE_AUDITION_TRIGGERED. That marker is emitted only in uiAuditSmokeMode, whereas this workflow launches project-persistence-view-tree. The real Sample Edit PLAY button emits SAMPLE_EDIT_PLAY_TRIGGERED unconditionally.
- Corrected both the marker baseline and wait to SAMPLE_EDIT_PLAY_TRIGGERED. This prevents a deterministic false timeout before the first CH4. No audio or acceptance assertion was relaxed.
