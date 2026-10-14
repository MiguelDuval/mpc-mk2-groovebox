# Session State — MPC MkII Groovebox

**Checkpoint refreshed:** 2026-10-14 (last independently verified Android Build #2705 is SHA `be431ccef023466216ada643208b18dec98a6bc0`; current evidence-harness follow-up requires its own exact-SHA check)  
**Purpose:** Compact handoff for the next ChatGPT session. Read this first, then independently verify every mutable fact against GitHub.

> **Freshness rule:** This checkpoint will itself be committed after the pre-checkpoint SHA above. Therefore, it does **not** claim CI success for the commit containing this state refresh. On every session start, verify the live PR head, branch HEAD, latest Android Build and artifact files. Do not rely on remembered commit numbers or on this checkpoint instead of GitHub.

## Last independently verified checkpoint — 2026-10-14 07:07 CEST

**Live GitHub source of truth:** branch `feature/mpc-one-ui-fidelity`; PR #23 OPEN / not merged; base `fix/ui-shell-format-width-startup`; fully tested code SHA `20ac2f54ca0075256a6b6534f5ffa7e06d358b77`. This checkpoint will be committed as docs-only and must receive its own exact-SHA Android Build.

### Android Build #2709 — SUCCESS, exact SHA verified

- [Android Build #2709](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38514032693) tested `20ac2f54ca0075256a6b6534f5ffa7e06d358b77`; **57/57 workflow steps passed**. APK assembly/upload, Android emulator smoke and visual-evidence upload all succeeded.
- The Arrangement audit now seeds one currently unused Pad 1 note through the existing native sequencer operation, locates the corresponding rendered Arrangement event, verifies a touch point with the production hit-test, and dispatches down/up MotionEvents through the actual `MpcArrangeView` and `GestureDetector` path.
- The route then asserts that the real listener delivered the expected Track and exact event tick to `openGridFromArrangementEvent`, and that the Grid viewport points at the right step. This is an automated Android gesture-path test, not a claim of finger testing on hardware.
- Actual screenshot/XML inspected: the seeded note appears at step **33** and the visible range is **25–40/64**; XML includes `Grid visible step range: steps 25 through 40 of 64`. The test verifies target step is inside that range.
- Local Browser regressions stayed green: MediaStore WAV scan/search/filter/selection/audition/assignment; Document Picker select → return → PAD 01 / LAYER 01; document-tree files, filtering, and persisted SAF access restored after app stop/relaunch.

### Verified APK and visual evidence for SHA `20ac2f54ca0075256a6b6534f5ffa7e06d358b77`

- APK artifact ID `11871064482`, `app-debug.apk`, **8,211,869 bytes**; SHA-256 `e653b9b42b4ec971fed383dbb9a1dd033ec0285f50cb94f7b8d6fc369624eb6a`. Downloaded and ZIP-integrity checked.
- Visual evidence artifact ID `11871970352`, **1,671,154 bytes**; SHA-256 `53f5e282f46e97e16ea399a1813bb8913192136385f06bb86f4541dd2fc0478e`. Downloaded and ZIP-integrity checked; **23 files**, including Main screenshot/XML, Arrangement→Grid focus screenshot/XML, Browser results, picker assignment and restored USER FOLDER evidence.
- Inspected hierarchy confirms Main Track Type DRUM truthfully reports conversion unavailable; Picker assignment confirms `Browser-Picker-Return.wav → PAD 01 / LAYER 01`; restored folder confirms `USER FOLDER • 1 WAV FILE INDEXED` and `Browser-Tree-Only.wav`.
- Physical MPC Studio MkII verification remains **PENDING**; runtime checks were performed on Android emulator.

### Completed implementation slices

**Local Browser E2E:** genuine MediaStore and SAF/document-tree local files, filters/search, selection, audition, loading and sampler assignment to the correct Pad/Layer; Android picker return; persisted document-tree folder access after app process restart. Splice, paid stores, upsells and unnecessary account/cloud flows remain excluded.

**Main Track Type correctness:** native AUDIO/DRUM labels map to the shared icon drawable; misleading CLIP call sites are gone; Audio uses a waveform glyph; unsupported type changes honestly report RESERVED. Verified on #2704–#2709.

**Arrangement event → Grid focus:** policy maps an event tick to its quantized absolute step and centers/clamps the Grid viewport. #2705 passed pure boundary/overflow tests; #2707 checked runtime viewport + screenshot/XML; #2709 drives a seeded populated event through the actual Android View/GestureDetector double-tap path and verifies exact listener track/tick plus focused Grid state.

### Current next task — continue functional musical workflows

The actual Arrangement event gesture route is now covered. Audit the remaining Arrange contract next: selected-track changes, visible clips/events, loop-handle drag/commit and preservation of Grid/Track context on return. Keep changes tied to real pattern/domain semantics; event placement remains read-only while the backend lacks a placement model. Add assertions for actual state change and no unintended note mutation, then run full exact-SHA Android CI and inspect fresh evidence.

### Known remaining gaps

- Favourite folders 1–5 are not genuinely persisted/restored.
- Locations/categories without real providers remain unavailable/no-result.
- Only Drum editing is supported in Grid/Step; other types must not silently route to Drum editing. Native `TrackKind` has no CV kind.
- Existing-track type conversion remains RESERVED until a real semantic/backend operation exists.
- Physical MPC Studio MkII behavior remains unverified.

## 1. Repository and safety

- Repository: `MiguelDuval/mpc-mk2-groovebox`
- Active branch: `feature/mpc-one-ui-fidelity`
- Primary PR: [#23 — MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23)
- Immediately before this documentation update, PR #23 was **OPEN, not merged**, base `fix/ui-shell-format-width-startup`, head branch `feature/mpc-one-ui-fidelity`, head SHA `dbfdac56802e1d7435ba1b4b7647e3a848917703`.
- Live GitHub reported 472 commits and 40 changed files in PR #23 before this documentation-only commit. Verify fresh branch/PR refs on the next session; do not use these values as post-commit state.
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

### Task 3 — Arrangement event → Grid focus — VERIFIED on Build #2709

Build #2709 (SHA `20ac2f54ca0075256a6b6534f5ffa7e06d358b77`) passed 57/57 steps. The emulator audit seeded a genuine Grid note, found its Arrangement event via the view hit-test, dispatched a double-tap gesture through the actual `GestureDetector`, and confirmed that listener delivery preserved the expected track and exact tick. The screenshot/XML shows the note at step 33 within the 25–40/64 viewport; APK and 23-entry evidence archive were downloaded, integrity-checked and inspected. This is emulator MotionEvent coverage, not physical controller testing.





After Browser's core local workflow is verified, improve real Track, Arrange, Grid and Step scenarios; then increase mixer depth and project persistence according to existing semantic/domain APIs. Do not prioritize another long decorative-only pass over a broken high-frequency workflow.

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
