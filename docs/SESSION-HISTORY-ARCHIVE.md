# Session History Archive — MPC MkII Groovebox

**Archived:** 2026-10-17  
**Purpose:** Preserve the previous long-form `docs/SESSION-STATE.md` before it was reduced to an operational checkpoint.

This file is historical context, not current state. It intentionally preserves prior checkpoint and task notes as they were at archive time. They may contain stale branch heads, build numbers, priorities, or assumptions. Before using any historical detail, verify it against live GitHub, current source and current Actions evidence. Do not read this archive as part of the normal session-start loop.

---

# Session State — MPC MkII Groovebox

**Checkpoint refreshed:** 2026-10-17 (Android Build #2747 passed on exact implementation SHA `70800387a4d133405be09dcc2cc8a6f6f616463a`; Track View mute round-trip now starts with one real Pad 1 note and verifies that toggling MUTE preserves the populated pattern. Actual screenshot/accessibility evidence, APK and 38-file visual-evidence archive were downloaded and inspected. The forthcoming documentation commit has its own new SHA and requires its own Actions result.)  
**Purpose:** Compact handoff for the next ChatGPT session. Read this first, then independently verify every mutable fact against GitHub.

> **Freshness rule:** This checkpoint will itself be committed after the pre-checkpoint SHA above. Therefore, it does **not** claim CI success for the commit containing this state refresh. On every session start, verify the live PR head, branch HEAD, latest Android Build and artifact files. Do not rely on remembered commit numbers or on this checkpoint instead of GitHub.

## Current verified state — Android Build #2747 / Track View mute + populated pattern preservation

- **Live verified HEAD before this documentation update:** `70800387a4d133405be09dcc2cc8a6f6f616463a` on `feature/mpc-one-ui-fidelity`; PR #23 remains OPEN against `fix/ui-shell-format-width-startup`. [Android Build #2747](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39026004772) passed on that exact SHA: focused Android/JVM policy tests, native tests, debug APK assembly, emulator smoke and evidence upload all succeeded.
- **New Track View round-trip:** the audit now creates a real Pad 1 Drum note *before* the Track View MUTE action, then snapshots all per-step Pad velocities. It taps the actual Track View MUTE button, checks the native mute state flips and the visible/accessibility label becomes `MUTE`, captures evidence while that state is visible, taps MUTE again to restore the original state, then checks the native flag, visible label, selected Track/sequence and complete Grid note snapshot all match their pre-test values. This avoids the previous empty-pattern false assurance.
- The inspected screenshot `mpc-groovebox-track-view-muted.png` visibly shows Track 01 • DRUMS, `EVENTS • 1` and the active green MUTE control. Its accessibility hierarchy identifies one enabled/clickable `Track View track 1 mute` button with text `MUTE` and real hit bounds. The test then continues through the existing seeded Arrangement event → Grid step 33, actual Function Bar BACK → Arrangement and same-event re-entry to Grid step 33 in visible range `25–40/64`.
- **Previously verified Step Sequencer behavior remains covered:** five `MpcSequenceStepPagePolicyTest` cases run in CI; the runtime path switches pages, toggles a test note on page 2, edits its probability, re-enters and removes the test note while verifying Step 1 is unchanged. The current evidence archive still does not include a dedicated Step-page screenshot/XML pair.
- **Browser/SAF scenarios remain covered:** local WAV scan, selection, audition and Pad/Layer assignment; Android Document Picker select → return → assignment; USER FOLDER result/filter and read-grant restoration after process restart; FAVORITE 1 independent URI and WAV restoration after process restart. FAVORITE 2–5 have separate implementation state but do not yet have separate full real-picker/restart acceptance fixtures.
- **APK artifact for tested SHA `70800387a4d133405be09dcc2cc8a6f6f616463a`:** `app-debug.apk`, artifact ID `12098428540`, 8,228,253 bytes, SHA-256 `41168cb1e1999fb0489fc5ee402c363a333ba32ddad5d63ea2c278c96b83f18a`.
- **Evidence artifact for tested SHA:** `mpc-ui-visual-evidence`, artifact ID `12098488760`, 2,786,502 bytes, SHA-256 `c9075550b7c5b0b48bfcbdc3611b1aaf7fcbcd4201e2c39fea28bd0a3452fe84`; ZIP integrity passed and the downloaded archive contains 38 files. Inspected files include Track View muted screenshot/XML, Arrange-after-BACK screenshot/XML, Grid re-entry screenshot/XML, Main and Browser evidence, Android Document Picker, USER FOLDER and FAVORITE 1 restoration evidence.
- Build #2746 passed on the previous SHA after adding Track View MUTE audit evidence, but its screenshot showed `EVENTS • 0`. Build #2747 strengthens the fixture by seeding the real note before the toggle; its screenshot confirms `EVENTS • 1`. An intermediate smoke preflight failure on #2745 was fixed by matching the new asynchronous callback signature; the next run exposed only a misplaced artifact-path preflight check, fixed before #2746/#2747 passed.
- **Physical MPC Studio MkII verification remains PENDING.** The Track View action was exercised on an Android emulator via the actual UI button; this does not establish physical USB/HID behavior, pad LEDs or controller-specific mute semantics. The docs commit below creates a new HEAD and must have its own exact-SHA Actions result.

### Completed — Step-page focus + Arrange/Grid context continuity

The implementation SHA from #2741 verified the 16-step-page focus policy through actual page/cell controls and tested the Arrange → Grid step 33 → real BACK → same-event Grid re-entry path without changing pattern data. The relevant Android policy tests and exact-SHA smoke remain green.

### Completed — Track View mute with populated pattern, state restoration verified on #2747

The current emulator fixture seeds one real Drum note before Track View is tested. A user action toggles Track MUTE, the native model and visible MUTE label agree, screenshot/XML evidence is captured while muted, then a second tap restores the original mute state. The full Grid velocity snapshot and selected Track/sequence are unchanged. The subsequent Arrangement/Grid round-trip proves the seeded event is still addressable at step 33. This proves the Track View mute workflow and note preservation; it does **not** claim that notes are edited directly within Track View.

### Next high-value scenario — Track Edit sample-layer parameter round-trip

Inspect `MpcTrackEditView`, its MainActivity listeners and the existing sampler/layer edit operations. Select a supported Drum Pad/Layer with a real sample, change one supported layer parameter (prefer tuning or gain), verify the authoritative sampler state and visible value change, navigate out and back/reselect the same Pad/Layer, and verify the edited value persists. Restore test state when safe. Do not add a new parameter model, sample backend or fake values; keep sampler audio/realtime boundaries untouched.

## Last verified full Android build before the current feature — 2026-10-15

**Verified build boundary:** Build #2714 succeeded on `13254a9e5839d3405f2a3a7ea46c377da95a83ac` (run [38707304218](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38707304218)). The subsequent Browser Favorite folders code landed in implementation commit `e65bfa4efa3bae0c5936c3791a3d9f87c2274994`; the picker-initial-URI correction below is newer still. Build #2714 does **not** validate this feature. Always fetch the current branch/PR head and its exact-SHA Actions result before making claims.

### Android Build #2713 — SUCCESS (previous verified feature checkpoint)

- Run [Android Build #2713](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38705685719) tested `cdcfdfb2500b4c6eaebeab5018b4cdce33213462`; **57/57 workflow steps passed**. APK assembly/upload, Android emulator runtime smoke and visual-evidence upload completed successfully.
- #2712 (parent SHA `ae3261f299078f157747f9b3a55760ccf67c8725`) failed in the emulator audit because the fresh test fixture has one Track and the new selection check required two. This was a fixture assumption, not a product/compiler regression.
- The fix adds a truthful single-Track branch: taps the real lane using the production View/touch path, verifies native and rendered selection remain aligned, and confirms the Arrangement data string is unchanged. It explicitly records that changing to an alternate Track is **unavailable in this fixture**; it does not add a temporary Track just for testing. The multi-Track change-and-restore branch remains present for future fixtures with two or more Tracks.
- Full smoke passed again, including the actual seeded Arrangement event double-tap → Grid focus (step 33 inside visible range 25–40/64), loop-handle drag → native loop change → handle-driven restore, Browser local WAV audition/assignment, Android Document Picker assignment to PAD 01 / LAYER 01, and persisted USER FOLDER restoration after process stop/relaunch.

### Verified APK and visual evidence for SHA `cdcfdfb2500b4c6eaebeab5018b4cdce33213462`

- APK artifact ID `11958217657`, `app-debug.apk`, **8,211,869 bytes**; SHA-256 `4ca32dee06e76b69314446ab5a12b9ec8bf9a488d84c5d7487f1a4861783f960`. Downloaded and ZIP-integrity checked.
- Visual evidence artifact ID `11958527786`, **1,673,291 bytes**; SHA-256 `09fa5c463ee3f7449f4cf36907b89e38fae3ddafb057d4d1e3257bd99bbd10c5`. Downloaded and ZIP-integrity checked; **23 files** including Main screenshot/XML, Arrangement→Grid focus screenshot/XML, Browser results, Picker assignment and restored USER FOLDER screenshot/XML.
- Inspected Arrangement→Grid screenshot shows range `25–40/64`, with the seeded note at step 33. The corresponding hierarchy reports `Grid visible step range: steps 25 through 40 of 64`. Browser and storage XML are present in the same evidence archive.
- Physical MPC Studio MkII verification remains **PENDING**; these are emulator results.

### Completed implementation slices

**Local Browser E2E:** real MediaStore and SAF/document-tree local files, search/filter, selection, audition, load and assignment; Android Document Picker return preserves the correct Pad/Layer; USER FOLDER URI/read grant and WAV results restore after process stop/relaunch. FAVORITE 1 has independent persisted SAF URI/name state and full assignment/restoration E2E coverage; slots 2–5 share the implementation but do not yet have separate full picker/restart fixtures. Commercial Splice/stores/upsells/cloud-account dependencies stay excluded.

**Main Track Type correctness:** native AUDIO/DRUM labels map to the shared type icon; misleading CLIP enum/call sites are gone; Audio has its waveform glyph; unsupported non-Drum conversion truthfully reports RESERVED. Accepted by Builds #2704 onward.

**Arrangement event → Grid focus:** the emulator seeds a real Pad 1 note through the existing sequencer command, resolves the actual rendered event through production hit-testing, dispatches Android down/up events through `MpcArrangeView` / `GestureDetector`, verifies exact Track/tick listener delivery, and confirms the Grid viewport includes the event's step. Screenshot/XML show step 33 in range 25–40/64.

**Arrangement loop and lane interaction contracts:** the real loop-end handle drag changes native loop state and restores the original range through the same handle gesture. The single-Track clean fixture now verifies an actual lane tap preserves native/View selection coherence and event data. Changing between two different Tracks is not covered in this one-Track fixture; no track-removal backend exists to safely create/clean up an alternate test Track.

### Browser Favorite slots 1–5 — implementation; FAVORITE 1 E2E verified on #2741

- FAVORITE 1 was assigned through the real Android DocumentsUI tree-picker flow. The audit confirmed an actual `Browser-Favorite-Only.wav` row, an independent `favorite_tree_uri_1` separate from `user_tree_uri`, and restoration of FAVORITE 1 plus its indexed WAV after process restart.
- The Browser smoke now clears Search again after returning from the Android Document Picker, because that picker return can restore a previous UI query. UIAutomator reports the empty EditText hint `Search files` in its `text` attribute; the assertion accepts only that exact hint or empty text, not arbitrary retained queries.
- **Scope boundary:** Favorite slots 2–5 have independent implementation state, but the automated real-picker/process-restart acceptance currently exercises FAVORITE 1. Do not claim separate end-to-end verification for every slot until the runtime fixtures exist.

### Completed — Arrange ↔ Grid BACK/context round-trip (Android Build #2741)

The exact-HEAD emulator audit uses the actual Grid Function Bar BACK action and a fresh same-event double-tap through `MpcArrangeView`/GestureDetector. It confirms the Arrangement workspace and its selected native Track/sequence/tick are restored; the same event remains hit-testable; pattern snapshots are unchanged; and re-entry restores the absolute step and viewport. The Arrange-after-BACK and Grid-re-entry screenshot/accessibility pairs were downloaded and inspected. Physical controller behavior remains unverified.

### Completed — Track View mute with populated pattern, state restoration verified on #2747

The emulator fixture seeds one real Drum note before Track View is tested. A user action toggles Track MUTE, the native model and visible MUTE label agree, screenshot/XML evidence is captured while muted, then a second tap restores the original mute state. The full Grid velocity snapshot and selected Track/sequence are unchanged. The subsequent Arrangement/Grid round-trip proves the seeded event is still addressable at step 33. This proves the Track View mute workflow and note preservation; it does **not** claim notes are edited directly within Track View.

### Next high-value scenario — Track Edit sample-layer parameter round-trip

Inspect `MpcTrackEditView`, its MainActivity listeners and the existing sampler/layer edit operations. Select a supported Drum Pad/Layer with a real sample, change one supported layer parameter (prefer tuning or gain), verify the authoritative sampler state and visible value change, navigate out and back/reselect the same Pad/Layer, and verify the edited value persists. Restore test state when safe. Do not add a new parameter model, sample backend or fake values; keep sampler audio/realtime boundaries untouched.

### Known remaining gaps

- FAVORITE 1's independent SAF target, real WAV listing and process-restart restoration passed in Build #2741. FAVORITE 2–5 still need separate real-picker/runtime fixtures; UIAutomator evidence confirms the tested FAVORITE 1 path only.
- Locations/categories without real providers remain unavailable/no-result.
- Only Drum editing is supported in Grid/Step; other Track Types must not silently route to Drum editing. Native `TrackKind` has no CV kind.
- Existing-Track type conversion remains RESERVED until a true semantic/backend operation exists.
- Multi-Track Arrange selection change/restore is not exercised by the current single-Track fixture.
- Physical MPC Studio MkII behavior remains unverified.
- Track View MUTE was verified end-to-end on emulator with one real note present; Track Edit layer tuning/gain persistence is the next bounded scenario.

## 1. Repository and safety

- Repository: `MiguelDuval/mpc-mk2-groovebox`
- Active branch: `feature/mpc-one-ui-fidelity`
- Primary PR: [#23 — MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23)
- At the beginning of this implementation cycle, PR #23 was **OPEN, not merged**, base `fix/ui-shell-format-width-startup`, branch `feature/mpc-one-ui-fidelity`, head SHA `13254a9e5839d3405f2a3a7ea46c377da95a83ac`. Feature commits have advanced since then; query live GitHub before the next change.
- PR commit and file counts change as the migration advances; treat live GitHub metadata as authoritative rather than relying on older recorded counts.
- Never work on `main` for this migration. Do not reset, rebase, rewrite history, force-push, merge or retarget PR #23 during routine UI development.
- Protect: realtime audio callback; sampler engine; sequencer clock/scheduler; MIDI transport; raw MkII decoder; SysEx protocol; working domain operations. Only introduce lower-layer changes when needed for a real semantic prerequisite.
- Physical MPC Studio MkII behavior stays **unverified/pending** unless a real device test report is available.

## 2. Approved product scope: local-first, no commercial ecosystem

The product owner explicitly approved this scope.

**Exclude from the actual product UI:** Splice and other branded content subscriptions; online stores/marketplaces; paid expansion promotion; partner advertisements; upsell/sales panels; unnecessary vendor-specific cloud or account/sign-in dependencies.

**Keep:** real local/app-owned files; generic categories backed by actual results; Android Storage Access Framework / Document Provider as a lower-level storage mechanism; accessible USB/external storage supported by the device; user-owned expansion folders; local sample browsing; genuine audition where implemented; and loading/assignment to the selected Pad/Layer.

The official Akai reference can document its own commercial taxonomy. That is historical/reference evidence, not a requirement for this product. Mark intentional deviations as reference-only and do not add disconnected placeholders.

## 3. CI record — historical snapshot verified 2026-10-11 04:06 UTC (latest pre-documentation check is recorded at the end)

- PR #23 is **OPEN / not merged**, base `fix/ui-shell-format-width-startup`, head branch `feature/mpc-one-ui-fidelity`. The implementation SHA verified by the latest full workflow is `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`.
- **Android Build #2637 — SUCCESS** on that exact SHA, run ID `38110061687`: 57/57 steps passed, including native/JVM tests, `MpcBrowserFileTest`, fresh APK assembly, Android emulator smoke, local Browser Search/filter interactions, sample audition/assignment, and visual evidence upload. Run: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38110061687
- Browser smoke runtime now resets inherited Search/Filter state, dismisses the landscape input method before tapping Clear, and verifies the end-to-end interaction set: matching Search shows the WAV result; non-matching Search removes it; Clear restores it; PROJECTS yields no WAV rows; ALL and SAMPLES restore the indexed WAV. Then real selection, MediaPlayer audition and native assignment complete on Pad 01 / Layer 01.
- Earlier failing test attempts were harness defects, not product regressions: #2634 referenced a nonexistent wait helper; #2635 exposed an inherited Search/filter state after the app-side audit; #2636 exposed that the landscape keyboard could intercept the Clear-button tap. These were corrected in the smoke script; the full #2637 workflow passed without weakening the semantic assertions.
- **APK artifact:** ID `11691089182`, 8,211,869 bytes; downloaded and validated as an Android package. SHA-256: `4f94874ed5b82f739cc3826b1425f52b716281bdee216f517afe9525e3c852a9`. Local copy: `MPC-MkII-Groovebox-2637.apk`.
- **Visual evidence artifact:** ID `11690994455`, 847,766 bytes; SHA-256: `efca6326eb57be868576c08ac7176f159fa413fbb173c12f593c0651acd54b07`. The downloaded ZIP was inspected: it contains 12 screenshot/XML evidence files. The Browser local-result screenshot is 1920×1080 (217,491 bytes), with a matching `mpc-groovebox-browser-local-results-uiautomator.xml` (54,464 bytes). The hierarchy confirms `CURRENT SAMPLE • Browser-Smoke-Local.wav`, target `PAD 01 / LAYER 01`, and `ASSIGNED • Browser-Smoke-Local.wav → PAD 01 / LAYER 01`.
- Physical MPC Studio MkII verification is still **PENDING**. Emulator verification is not hardware verification.
- This section is valid for implementation SHA `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`. This documentation refresh creates a newer commit; check its own workflow result before describing that newer SHA as validated.

## 4. Current implementation snapshot

- **Main shell:** toolbar, five-slot Shortcut Rail, XL Channel Strip host, workspace and Function Bar exist. Fidelity is improved but exact visual and behavioral parity is incomplete.
- **Browser:** Splice has been removed from the actual Content navigation. Regression preflight requires its absence. Generic local categories retained include Drums, Instruments, Samples, Demos and My Files.
- **Browser core local WAV slice — IMPLEMENTED and emulator-verified:** MediaStore-backed result rows, selected-file identity, real MediaPlayer audition, and assignment through the existing native sampler operation are complete. Full runtime smoke now additionally verifies positive/negative Search, Clear, PROJECTS filtering and ALL/SAMPLES restoration on real result rows.
- **Browser core local workflow — IMPLEMENTED and emulator-verified:** MediaStore-backed real WAV rows; positive/negative Search, Clear and file-type filters; actual file selection and MediaPlayer audition; native sampler assignment; and visible target/current-sample feedback.
- **Browser SAF folder workflow — IMPLEMENTED and emulator-verified:** `ACTION_OPEN_DOCUMENT_TREE`, persistable read permission, real direct-child WAV results, Search/filter behavior over that provider, SharedPreferences URI/name storage, and restoration of the USER FOLDER destination/result after a full app process stop/relaunch all passed Android Build #2701.
- **Android Document Picker load — E2E-verified:** selecting a real WAV in the picker returns to the app and assigns it through the existing sampler path to the snapshotted target Pad/Layer. Build #2701 evidence confirms `CURRENT SAMPLE` and `ASSIGNED` for PAD 01 / LAYER 01.
- **Browser remains PARTIAL only in storage conveniences/providers:** favourite folders 1–5 are not actually persisted/implemented; MPC DOCUMENTS, CONNECTED STORAGE, non-Samples content categories and expansion providers stay unavailable/no-result unless connected to real sources. Do not describe decorative navigation as implemented storage access.
- **Main Track Type:** current source correction maps native `AUDIO` to the Audio icon and keeps `CLIP` out of the Track Type vocabulary. Existing-track type conversion remains RESERVED; verify the exact new HEAD's full Android workflow before accepting the correction.
- **Track View / Arrange / Grid / Step:** partial; reuse existing event/domain operations and preserve explicit unavailable states for unsupported edits/non-Drum tracks.
- **Sampler / Sample Edit:** substantial existing software/UI foundation (recording, monitor/threshold, waveform, region, crop/chop and layer semantics). Reuse, do not rewrite.
- **Pad Mixer:** real Level/Pan/Tune subset exists. Full Channel Mixer/routing/FX remain incomplete.
- **Project persistence, List Edit, Pad Perform, XYFX and other advanced contexts:** reserved/partial. No fake operations.

Canonical implementation status and product decisions also live in `docs/MPC3-9-OS-KNOWLEDGE-BASE.md` and `docs/IMPLEMENTATION-ROADMAP.md`.

## 5. Operating documents now established

- `docs/SESSION-STATE.md` — this concise checkpoint. Update at meaningful milestones; live verification overrides it.
- `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` — mandatory workflow, local-first product scope, slice acceptance, evidence contract and free-tier/context-efficiency rules.
- `AGENTS.md` — instructs agents to read both files before implementation.
- `docs/PROJECT_MASTER_PROMPT.md` — incorporates the new execution rules and commercial-scope decision.
- `docs/IMPLEMENTATION-ROADMAP.md` — priority order: remove excluded surfaces, complete real local Browser and sample workflow, then Track/Arrange/Grid/Step and project/mixer work.
- `docs/MPC3-9-OS-KNOWLEDGE-BASE.md`, `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md`, `docs/MPC3-REFERENCE-INDEX.md`, `docs/MPC3-UI-SCREEN-BLUEPRINT.md`, `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md` — reference taxonomy, intentional product deviation, screen design and safety contract.

Do not dump/re-read all large docs every session. Read this state, the protocol and master brief, then only the relevant sections of the roadmap/spec/source/tests.

## 6. Exact next-work order

### Task 0 — verify CI for the local WAV Browser slice — PASSED 2026-10-11

1. PR #23 implementation head `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07` matched Android Build #2637.
2. Full Android Build #2637 succeeded (57/57 steps), including local Browser JVM policy tests, fresh APK, emulator smoke, Search/filter runtime interactions and visual evidence.
3. Search with a matching name showed the indexed WAV; a non-match produced the real no-results state; Clear restored the item. PROJECTS removed WAV rows; ALL and SAMPLES restored them. MediaStore discovery, file selection, audition, native assignment and state feedback also passed.
4. The APK and evidence archives were downloaded and inspected. Exact identifiers, SHA-256 sums and screenshot/XML evidence are recorded in section 3 above.
5. The next documentation-only commit will have a new SHA and must be checked against its own workflow.

### Task 1 — Local Browser E2E acceptance — PASSED 2026-10-13

1. Android Document Picker select → return → native assignment to the preserved Pad/Layer passed on Build #2701.
2. Android user document-tree selection, real WAV rows, filters, persisted URI/read-grant restoration after process stop/relaunch passed on Build #2701.
3. Actual APK and visual-evidence artifacts for exact SHA `f325676ecd147426952bfda4c1e160cc620489bd` were downloaded and inspected. Run, IDs, hashes and evidence files are recorded in the current checkpoint above.
4. Keep the storage workflow intact. Add favourite locations only when actual storage, persistence/restoration and tests are implemented.

### Task 2 — Main Track Type vocabulary and honest reserved state — ACCEPTED on Build #2704

1. Native AUDIO/DRUM mapping, waveform glyph, obsolete CLIP icon removal and truthful reserved selection feedback are compiled and verified on exact SHA `8a9f5ecd2e7326d7b1f703c6aafc76a5b800bfcb`.
2. Build #2703's missed `MpcMainMixerStripView` caller was fixed; Build #2704 passed 57/57 steps and produced inspected APK/evidence artifacts. The failed intermediate attempt remains recorded below as history, not as current state.
3. Track conversion is still RESERVED. No sampler/audio/MIDI/domain operation was invented.

### Task 3 — Arrangement gestures — VERIFIED on Build #2713

Android Build #2713 tested `cdcfdfb2500b4c6eaebeab5018b4cdce33213462` and passed all 57 steps. It retains the actual populated-event double-tap → Grid route and exact Track/tick assertion, the loop-end handle change/restore check, and Browser E2E. It also validates the real lane-tap path without creating a test Track in the one-Track fresh fixture; multi-Track selection remains untested because the current fixture exposes one Track and there is no native Track-removal operation. APK/evidence artifacts for #2713 are recorded in the checkpoint above.

After Browser's core local workflow is verified, improve real Track, Arrange, Grid and Step scenarios; then increase mixer depth and project persistence according to existing semantic/domain APIs. Do not prioritize another long decorative-only pass over a broken high-frequency workflow.

### Task 4 — Arrange ↔ Grid Back/context preservation — NEXT

After the populated Arrangement event opens Grid, invoke the actual Function Bar BACK action. Verify return to ARRANGE preserves the native selected Track, selected lane, original event tick and event data; confirm that the same event remains hittable; re-dispatch its real event gesture; and verify the same absolute Grid step/window. No pattern mutation may be used to fake a navigation result. Full Android Build + fresh screenshot/XML required.

## 7. Required test/evidence ladder

For each meaningful slice:

1. Inspect current source, callers, state owner/backend command, docs, tests and relevant commits.
2. Run the smallest relevant unit/source/native tests first.
3. Build a fresh APK on the active branch.
4. Run emulator UI smoke and capture real screenshot + accessibility/UI hierarchy relevant to the screen.
5. Verify the semantic result/state change as well as geometry/hit bounds/focus/Back behavior.
6. Check GitHub Actions for the exact SHA and inspect the APK/evidence artifacts, including archive contents and checksums where available.
7. Update roadmap/status and this file with the commit, tested SHA, CI URL/conclusion, artifact identity, evidence files inspected, known gaps, hardware status and exact next task.

The final check must not be skipped to save time. Use fast targeted checks during iteration and the full cycle at the acceptance boundary. Do not create a separate full CI run for every tiny intermediate edit where a local or targeted test is sufficient.

## 8. Branch and commit discipline

- Stay on `feature/mpc-one-ui-fidelity`, PR #23.
- Never reset/rebase/force-push or edit `main`.
- One coherent intent per commit; for a multi-document documentation update, prefer one atomic commit rather than a succession of per-file commits where tooling permits.
- Do not add paid services, new credentials, cloud dependencies or required paid AI tools. Assume ChatGPT free-tier context/usage limits and optimize by reusing docs, targeted ranges, concise evidence inspection and bounded implementation slices.
- No asynchronous promises: complete the checks possible in the current session and record unresolved work accurately.



## Documentation update — 2026-10-11

- Added `docs/DEVELOPMENT-OPERATING-PROTOCOL.md`: required start-of-session checks, local-first product scope, bounded implementation loop, Browser acceptance criteria, evidence contract and free-tier/context-efficiency rules.
- Added `docs/NEXT-CHAT-PROMPT.md`: reusable engineering prompt for the next ChatGPT session; read it when handing off this project.
- Updated the master prompt, `AGENTS.md`, implementation roadmap, MPC 3.9 knowledge base, UI migration master spec, reference index and safe-change contract to record the approved commercial-scope exclusions and the new operating process.
- Live status tool output returned PR #23 as OPEN, base `fix/ui-shell-format-width-startup`, head SHA `889f237b3f3f5727466b38653ba49cfe8411727c` (453 commits in the PR result). Android Build #2620 for that SHA reported **SUCCESS**. Run: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38087051940
- **Important freshness warning:** documentation commits were made after the last checkpoint; the PR-search and workflow tools may not expose a perfectly synchronized branch/PR snapshot. On the next session, fetch the actual branch head and PR head independently, then check CI for that exact SHA before relying on #2620. Do not infer current-HEAD validation from the run above.
- Browser implementation remains the next functional priority: Splice removal is recorded, but the real semantic local result list and verified end-to-end local sample selection/load/assignment remain incomplete. Physical MPC Studio MkII verification is still pending.


---

## Session refresh — 2026-10-11 UTC (Search/filter acceptance passed)

This checkpoint records the implementation SHA proven by Android Build #2637. The documentation commit following it requires its own CI check.

- At checkpoint time, PR #23 was OPEN, base `fix/ui-shell-format-width-startup`, implementation branch `feature/mpc-one-ui-fidelity`, tested SHA `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`.
- Android Build #2637 / run ID `38110061687` completed **SUCCESS** for that exact SHA; all 57 workflow steps passed. APK and visual-evidence artifact IDs, sizes, SHA-256 sums, and inspected files are listed in section 3.
- The local Browser runtime path now verifies real MediaStore WAV discovery → Search positive/negative → Clear → PROJECTS/ALL/SAMPLES filter changes → result selection → genuine audition → native Pad/Layer assignment. The evidence hierarchy explicitly shows `CURRENT SAMPLE • Browser-Smoke-Local.wav` and `ASSIGNED • Browser-Smoke-Local.wav → PAD 01 / LAYER 01`.
- The next concrete task is **a real Android Document Picker selection → return → target-preservation/sample-state test**. After that, add a separately tested user-selected document tree and persisted favorites. Avoid further cosmetic-only Browser work until these workflows are complete.
- Known gaps: real picker selection/return assignment still needs runtime coverage; user-selected tree/favourites are not implemented; non-WAV/project/plugin categories have no real provider and must stay unavailable; physical MPC Studio MkII verification remains pending.
- The local-first scope stays fixed: no Splice, stores, paid expansion promotion, partner ads, upsells, or unnecessary vendor accounts/cloud.



---

## Firmware research refresh — 2026-10-11

- Inspected owner-supplied `MPC-3.9.1-Gen1-update.img` locally; SHA-256: `4c0f7797a006313fad206f9513eca8b3823e834c9e0941a6b5f088386cb835d6`, 162,220,784 bytes.
- Verified the envelope declares an XZ `rootfs` payload at offset 340, length 162,220,428; the embedded SHA-1 matches and `xz -t` succeeds. Decompressed filesystem SHA-256: `6aafd4384469a76df4c4270aa4f07b3f7e411828a691c75d5471c5fdfe2d69f3`; local read-only traversal found 11,529 entries with no directory parse errors.
- Confirmed application version `3.9.1.2 (f3af35cdc95)`, stripped ARM EABI5 ELF Build ID `8371199f945ecc40f79cb19c17ff5de72810a91c`. Found ACVS resources with 759 PNG and 13 JSON layout files, panel dimensions to investigate, and static RTTI/string anchors for Main layout data and Q-Link/Browser settings.
- Detailed report: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md`; structured summary: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json`.
- Limits: no Ghidra function-level decompilation, no target execution or physical device capture, no nested SquashFS extraction, and no e2fsck consistency validation. Layout JSON is not treated as verified pixel geometry for the standalone Main screen. No firmware/image assets/private inventory were added to this public repository.
- Live pre-documentation Android Build run [38110623016](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38110623016) succeeded for parent SHA `dbfdac56802e1d7435ba1b4b7647e3a848917703`. The new documentation commit's Actions outcome must be checked separately; do not carry forward the parent's green status.
- **Next implementation task unchanged:** real Android Document Picker file selection → return → target preservation/sample-state assertion. Then user-selected tree/persisted favourites with grant restoration tests. Do not reprioritize to UI cosmetics because of the firmware findings.


---

## Firmware research refresh — 2026-10-11 (second pass)

- Re-inspected extracted UI metadata, not only the filesystem container: 10 Program Edit `TUI.json` files, 66 tabs, 652 component entries; 199 local component definitions and 136 serialized action descriptors across page JSONs. `Help.json` has 51 entries and host/shared mouse/hover/window vocabulary. The same stripped ELF contains touch-specific Main and Mixer class-name anchors.
- Main/touch strings include `TouchMainPageNavigationPanel`, `TouchMainPageContentPanel`, separate Audio/Bus/CV/Drum/Keygroup/MIDI/Plugin track panels, `TouchMainModeMixerProperties`, `TouchMixerChannelStripMPC`, and `TouchDoubleChannelStripContainer`. These are metadata/type-name observations only; no function-body decompilation or runtime validation.
- **Important Track Type discrepancy:** firmware selector assets are exactly Audio/CV/Drum/Keygroup/MIDI/Plugin, and the official v3.9 guide lists the same six types. The project's recent UI vocabulary has CLIP but not AUDIO. Audit visible selector/tests after the current SAF Document Picker selection-return-target-preservation task. Do not confuse `strip-clip.png` or a `TouchMainPageTrackClipPanel` class name with Track Type selector proof.
- The report and JSON summary were updated: `docs/research/MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md` and `docs/research/MPC-3.9.1-GEN1-FIRMWARE-ANALYSIS.json`. Knowledge base, reference index, UI blueprint, roadmap and master spec point future agents to the discrepancy and confidence boundary.
- Parent implementation/documentation commit `371e1f415900cd2ea13fbf5efedb3ac9deba0d72` passed Android Build #38111336958: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38111336958 . All follow-on documentation commits require verification at the final current branch SHA.
- **No application source/audio/MIDI/backend changes in this research pass.** Next functional task remains real Android Document Picker file selection → return → Pad/Layer target preservation. Follow that with the Track Type selector source/test audit.


### 2026-10-15 — Browser Favorite picker E2E failure diagnosis and harness correction

- Android Build #2721 for exact SHA `829b006a5f9e8547d82ed341c772155215c008b2` successfully assembled and uploaded the APK, and the earlier smoke stages passed through the USER FOLDER document-tree selection, real WAV scan, filter checks and process-restart restoration. The full workflow **failed** in the new FAVORITE 1 assignment stage.
- Inspected the uploaded emulator evidence artifact (run #2721). The real DocumentsUI screenshot shows the picker reopened inside `AAA-MPC-Browser-Tree` (the already configured USER FOLDER), with `Browser-Tree-Only.wav`; `Browser-Favorite-Only.wav` and `BBB-MPC-Browser-Favorite` were not in that directory. The test had assumed the Favorite-only folder was immediately visible and aborted before assignment. The first capture also lacked the visible “Use this folder” control, so the next run must confirm the picker has reached the tree-grant UI rather than presuming the route is correct.
- Corrected the smoke harness to read the actual DocumentsUI breadcrumb, navigate back to its provider root when Android restores the previous folder, confirm the “Use this folder” tree-grant action, then search the root viewport with bounded real swipes before selecting the separate Favorite-only fixture. Corrected the stale comment that described the Favorite sample as reused.
- This is **not a passing Favorites milestone**. The next exact-HEAD Actions run must verify the app route, separate SAF preference keys/URIs, USER FOLDER ↔ FAVORITE 1 switching and process-restart restoration. Follow with a source fix if the tree-grant control still fails to appear.


### 2026-10-15 — API 31 system overlay preflight (#2730)

- Android Build #2730 on exact SHA `004ab89c3e4041f36f8ebfaeca63fe8283c3d3d7` failed before the Browser scenario: the Android 12 first-run `Viewing full screen` confirmation (`android:id/immersive_cling_title`, clickable `android:id/ok` = `GOT IT`) covered the rendered Main workspace. The matched Main-ready screenshot/XML prove this was an OS overlay, not a missing MPC toolbar; the harness then correctly failed the canonical accessibility assertion. Run: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38785925092
- Added a bounded, accessibility-targeted dismissal of the one-time OS confirmation before the startup screenshot and Main audit capture. It uses the API 31 system input command and verifies the overlay is gone; no app UI assertion is relaxed.
- The API 31 folder-row input fix (`cmd input touchscreen tap`) has not yet reached its acceptance scenario because #2730 stopped at the system overlay. The next exact-HEAD Actions run must first prove the overlay is dismissed, then test Favorite row activation; Favorites remain unverified until the distinct folder/WAV, independent saved URIs, bidirectional switching and process-restart restoration pass.
