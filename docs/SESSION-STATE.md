# Session State — MPC MkII Groovebox

**Checkpoint prepared:** 2026-10-10 (live GitHub verification at pre-checkpoint HEAD `a8c13c6cb7907c9d868fc928bd71d03672ddd0b8`)  
**Purpose:** Compact handoff for the next ChatGPT session. Read this first, then independently verify every mutable fact against GitHub.

> **Freshness rule:** This checkpoint will itself be committed after the pre-checkpoint SHA above. Therefore, it does **not** claim CI success for the commit containing this state refresh. On every session start, verify the live PR head, branch HEAD, latest Android Build and artifact files. Do not rely on remembered commit numbers or on this checkpoint instead of GitHub.

## 1. Repository and safety

- Repository: `MiguelDuval/mpc-mk2-groovebox`
- Active branch: `feature/mpc-one-ui-fidelity`
- Primary PR: [#23 — MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23)
- At the last live check before this state refresh, PR #23 was **OPEN, not merged**, base branch `fix/ui-shell-format-width-startup`, head SHA `a8c13c6cb7907c9d868fc928bd71d03672ddd0b8`.
- PR #23 showed 453 commits and 35 changed files. A previous compare against the base reported 452 commits ahead / 0 behind; recheck this against live refs after the new state commit.
- Never work on `main` for this migration. Do not reset, rebase, rewrite history, force-push, merge or retarget PR #23 during routine UI development.
- Protect: realtime audio callback; sampler engine; sequencer clock/scheduler; MIDI transport; raw MkII decoder; SysEx protocol; working domain operations. Only introduce lower-layer changes when needed for a real semantic prerequisite.
- Physical MPC Studio MkII behavior stays **unverified/pending** unless a real device test report is available.

## 2. Approved product scope: local-first, no commercial ecosystem

The product owner explicitly approved this scope.

**Exclude from the actual product UI:** Splice and other branded content subscriptions; online stores/marketplaces; paid expansion promotion; partner advertisements; upsell/sales panels; unnecessary vendor-specific cloud or account/sign-in dependencies.

**Keep:** real local/app-owned files; generic categories backed by actual results; Android Storage Access Framework / Document Provider as a lower-level storage mechanism; accessible USB/external storage supported by the device; user-owned expansion folders; local sample browsing; genuine audition where implemented; and loading/assignment to the selected Pad/Layer.

The official Akai reference can document its own commercial taxonomy. That is historical/reference evidence, not a requirement for this product. Mark intentional deviations as reference-only and do not add disconnected placeholders.

## 3. Live CI situation — verified 2026-10-11 00:35 UTC

- PR #23 remains **OPEN / not merged**, base `fix/ui-shell-format-width-startup`, branch `feature/mpc-one-ui-fidelity`. The validated implementation SHA at this checkpoint is `bc71f1dfb69d8a94b7de266300a704c27b2a1338`.
- **Android Build #2632 — SUCCESS** for that exact SHA, run ID `38098504950`: all 57 workflow steps succeeded, including unit/native checks, the dedicated `MpcBrowserFileTest`, APK assembly, emulator startup/UI smoke, the Browser local MediaStore → result selection → audition → Pad/Layer assignment path, and UI evidence upload. Run: https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38098504950
- Test-harness failures immediately before this success are understood and corrected. #2630 showed zero indexed WAVs because its deprecated media-scanner broadcast did not seed MediaStore. #2631 used `content query --where "_display_name='Browser-Smoke-Local.wav'"`, which failed on the emulator with `IllegalArgumentException: Invalid token Browser`; the preceding `scan_file` call had actually returned a valid MediaStore URI. Commit `bc71f1d` queries the returned item's numeric `_id` and checks the expected filename before entering the UI flow.
- **APK artifact:** ID `11686723680`, 8,211,869 bytes; downloaded as `MPC-MkII-Groovebox-2632.apk`. SHA-256: `17c146db6c9c34f650e0f3742bdb382eda1f437133945371fa232169905f07a0`. The file is identified as an Android package and its ZIP structure passes integrity checking.
- **Visual evidence artifact:** ID `11686754109`, 847,926 bytes; SHA-256 of the downloaded evidence ZIP: `ba972a004365a983c57be4af030a254cb43e0d6a7c91bc287c587dab37b08d42`. Actual archive contents were inspected. It contains `mpc-groovebox-browser-local-results.png` (1920×1080, 217,491 bytes) and `mpc-groovebox-browser-local-results-uiautomator.xml` (54,465 bytes), plus the startup/Main/Browser/Document Picker evidence. The Browser hierarchy records `CURRENT SAMPLE • Browser-Smoke-Local.wav`, its target `PAD 01 / LAYER 01`, and `ASSIGNED • Browser-Smoke-Local.wav → PAD 01 / LAYER 01`.
- No physical MPC Studio MkII verification is claimed. Emulator runtime verification passed.
- This section records the last fully validated **implementation** SHA. Updating this document advances the PR head; verify the fresh workflow for the documentation commit independently before treating that newer SHA as CI-green.

## 4. Current implementation snapshot

- **Main shell:** toolbar, five-slot Shortcut Rail, XL Channel Strip host, workspace and Function Bar exist. Fidelity is improved but exact visual and behavioral parity is incomplete.
- **Browser:** Splice has been removed from the actual Content navigation. Regression preflight requires its absence. Generic local categories retained include Drums, Instruments, Samples, Demos and My Files.
- **Browser core local WAV slice — IMPLEMENTED and emulator-verified:** the result panel now contains actual MediaStore-indexed WAV rows rather than placeholder sample/storage rows. Explicit Scan permission, selected-file identity, target Pad/Layer context, real MediaPlayer audition, and assignment through the existing native sampler operation were exercised by Android smoke. Pure JVM tests cover WAV eligibility and semantic search/filter decisions.
- **Browser remains PARTIAL:** indexed results currently cover WAVs in the supported `PLACES/INTERNAL`, `CONTENT/SAMPLES`, and `CONTENT/MY FILES` locations. Other categories show honest no-provider states. Runtime smoke has not yet exercised search/filter taps end-to-end; folders selected through `ACTION_OPEN_DOCUMENT_TREE`, persisted/restored favourite locations, and the actual SAF file-pick → return → target assignment path remain follow-up work.
- **Sample workflow:** MediaStore result assignment and actual audition are verified. The separate Android Document Picker entry still uses the existing native WAV loader and now snapshots the intended Pad/Layer across picker navigation; UI smoke currently verifies picker launch, but a real pick-and-return target-preservation run is still pending.
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

1. PR #23 head `bc71f1dfb69d8a94b7de266300a704c27b2a1338` matched the tested run #2632.
2. Full Android Build #2632 succeeded (57/57 steps), including the local Browser WAV filtering JVM test, fresh APK, emulator smoke, and evidence upload.
3. The emulator smoke proved this real sequence: MediaStore WAV indexed → Browser result visible and selectable → actual selected-file audition state → native assignment to Pad 01 / Layer 01 → updated current sample label and assignment status.
4. APK and evidence archives were downloaded and inspected. The expected local Browser PNG and XML hierarchy are physically present in the evidence archive.
5. The current commit is verified; this state/roadmap/knowledge-base documentation update creates a newer PR head which requires its own workflow check.

### Task 1 — extend acceptance of the semantic local Browser (next)

Keep the implementation narrow; do not rewrite the Browser or native audio layer.

1. Add runtime interaction assertions that search and the real `SAMPLES / ALL` filters change the indexed WAV result set. Keep JVM policy tests, but do not treat those alone as UI acceptance.
2. Exercise selecting a real WAV through the Android Document Picker and returning to the app. Assert the preserved Pad/Layer target and visible sample name after assignment; do not only assert that the picker launches.
3. Then design a separate slice for a user-selected document tree, persistable URI access and restored favourite folders. Add it only with explicit permission/restoration tests. Until then favourites and unimplemented locations remain unavailable, not decorative successes.
4. Continue auditing for excluded Splice/marketplace/paid-content routes when new Browser paths are added.

### Task 2 — high-value musical contexts

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

## Session refresh — 2026-10-11 UTC (post local WAV Browser slice)

This is the latest checkpoint for the implementation state. Always compare it with live GitHub; this documentation commit is newer than the implementation SHA proven by Android Build #2632.

- At checkpoint time, PR #23 was OPEN, base `fix/ui-shell-format-width-startup`, implementation branch `feature/mpc-one-ui-fidelity`, validated code SHA `bc71f1dfb69d8a94b7de266300a704c27b2a1338`.
- Android Build #2632 / run ID `38098504950` completed **SUCCESS** on that SHA. All 57 workflow steps passed. Exact results and archive identities are in section 3 above.
- Local Browser runtime E2E verified a WAV row discovered from MediaStore, selected-file audition, actual sampler assignment to Pad 01 / Layer 01, and current-sample/assignment feedback. No audio engine/realtime callback, MIDI transport, raw MkII decoder or SysEx subsystem was rewritten for this slice.
- The next high-value task is **runtime search/filter coverage plus an actual SAF file-pick → return → assignment target-preservation test**. Only after these should the work move to a separate, tested document-tree/favourite persistence slice. Avoid more geometry-only cleanup while these functional acceptance gaps remain.
- Known gaps: runtime search/filter taps and actual SAF selected-file assignment need end-to-end verification; document-tree browsing and persisted favorites are not implemented; unsupported category/file types do not produce real lists; physical MPC Studio MkII verification remains pending.
- Keep the local-first product scope: no Splice, stores, paid expansion promotion, partner ads, upsells or unnecessary vendor account/cloud dependencies.

