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

## 3. Live CI situation at the last check

- Last successful full Android workflow before the current local-only Browser contract: [Android Build #2617](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38082008806), tested SHA `ca0dd4c5f209c0a34ad4c6ae1182ed13429036bb`. This does not validate later Browser changes.
- [Android Build #2618](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38085798429), SHA `7754a5b22a0331e94638fece768364952603028f`, **FAILED** in the emulator smoke step. The new product-scope preflight correctly rejected the string `splice` in Browser source, but an older information-architecture assertion still incorrectly required `SPLICE`. Native tests and APK assembly had passed before smoke failed. This is a contradictory test expectation, not evidence that Splice should be retained.
- Fix committed at `a8c13c6cb7907c9d868fc928bd71d03672ddd0b8` (`test: align Browser IA contract with local-only scope`): removed the obsolete `SPLICE` requirement from the Browser IA source preflight, retaining the separate assertion that Splice must not appear in the product Browser and the checks for generic local categories.
- At the last live check, Android Build #2619 for SHA `a8c13c6cb7907c9d868fc928bd71d03672ddd0b8` was **IN PROGRESS**: [Build #2619](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38086888043). This session-state refresh advances HEAD, so #2619 may be cancelled/superseded. **The first task next session is to inspect the latest Android Build for the actual current PR SHA and fix any failure before adding unrelated work.**
- Do not treat an APK produced by a workflow whose emulator smoke failed as a validated release artifact. Check the exact tested SHA and the conclusion of the entire workflow.
- Earlier Build #2608 and its visual evidence describe the older SHA `77f4e18a8f40a2e3d58ec8dbdf873cb09070fe4a`, not current source. Its screenshots/hierarchy should not be used to assert that the Browser now excludes Splice or that later interactions work.

## 4. Current implementation snapshot

- **Main shell:** toolbar, five-slot Shortcut Rail, XL Channel Strip host, workspace and Function Bar exist. Fidelity is improved but exact visual and behavioral parity is incomplete.
- **Browser:** Splice has been removed from the actual Content navigation. Regression preflight requires its absence. Generic local categories retained include Drums, Instruments, Samples, Demos and My Files.
- **Browser is still PARTIAL:** its result area remains placeholder/informational rows (Open Storage plus Current Sample), not a real semantic list of indexed local files. Search/filter controls and navigation state exist, but do not imply a fully working local result provider. Persisted favourites, complete options and a genuine selectable local result workflow remain gaps.
- **Sample workflow:** existing WAV import route uses Android Document Picker and native sample load/assignment for the current selected Pad/Layer. Preserve this path while introducing a product-level result list; do not regress it.
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

### Task 0 — verify the latest CI for the actual head

1. Read PR #23's live head SHA and branch state.
2. Inspect the newest Android Build run associated with that SHA. The previously running #2619 may have been superseded by this checkpoint commit.
3. Confirm the result includes native tests, APK assembly, Android emulator smoke, and visual-evidence upload; identify the exact failed step if not green.
4. Correct any test/evidence mismatch with a small change. Do not bring back Splice to satisfy an outdated assertion.
5. Inspect the artifact names/files from the latest successful run. For visual evidence, verify expected PNG and XML hierarchy files exist in the downloaded archive, not only that the workflow log said they were captured.
6. Update this state file at the end of the next meaningful implementation slice; don't create multiple status-only commits while the slice is unfinished.

### Task 1 — semantic local Browser results (next implementation milestone)

Design a small real vertical slice after inspecting the current code and Android storage capabilities:

- Keep Android Document Provider underneath the product Browser.
- Provide a real way to discover accessible local audio files (for example a persisted user-selected document tree and/or indexed accessible local media, selected based on existing app/dependency support).
- Show real result rows with file identity/metadata, not placeholders.
- Selecting a result must preserve its URI and the current target Pad/Layer.
- Load must use the existing sample decoder/native load/assignment path and update the correct sample name/state after returning to Main.
- Search/type filters must be connected to the semantic local results or explicitly remain unavailable; do not claim functionality from a UI control alone.
- Treat audition as a separate verified behavior. Do not fake preview by triggering an unrelated already-loaded sample.
- Persist favourite locations only after URI permissions/storage and restoration are designed and tested.
- Add tests for provider/result filtering and an Android smoke path appropriate to the new workflow.
- Keep the first slice bounded: a real local WAV result → select target → load/assign → visible state result is more valuable than a broad unfinished browser framework.

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

