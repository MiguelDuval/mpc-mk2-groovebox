# Session State — MPC MkII Groovebox

**Checkpoint prepared:** 2026-10-11 (implementation evidence verified on `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`; documentation refresh itself will create a newer SHA that requires its own CI check)  
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

## 3. Live CI situation — verified 2026-10-11 04:06 UTC

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
- **Browser remains PARTIAL:** indexed WAV results currently work in `PLACES/INTERNAL`, `CONTENT/SAMPLES`, and `CONTENT/MY FILES`. Other categories show honest no-provider states. User-selected document-tree roots via `ACTION_OPEN_DOCUMENT_TREE`, persisted/restored favourite locations, and actual SAF file-pick → return → Pad/Layer target preservation remain unimplemented/follow-up work.
- **Sample workflow:** MediaStore result assignment and actual audition are verified. The Document Picker route still uses the existing native WAV loader and snapshots the intended Pad/Layer; only picker launch is currently verified on Android smoke. A real picker selection/return/assignment test is next.
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

### Task 1 — validate the Android Document Picker assignment path (next)

Keep the work bounded; preserve the existing picker and native loader rather than rewriting storage or audio.

1. Extend emulator smoke to launch Browser LOAD / Android Document Picker, select the known local WAV fixture, return to the app, and assert the intended Pad/Layer target survived the activity round-trip.
2. Assert the actual selected filename and native sampler state after return/assignment—not just that the picker opened. Keep the MediaStore workflow green.
3. Then design a separate document-tree slice using `ACTION_OPEN_DOCUMENT_TREE`, persistable URI permissions, restoration after app/activity recreation, and actual folder-backed Browser results. Only add favourite folders with storage-and-restoration tests.
4. Continue excluding Splice/marketplaces/paid-content promotion and leave unsupported categories truthfully unavailable.

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

## Session refresh — 2026-10-11 UTC (Search/filter acceptance passed)

This checkpoint records the implementation SHA proven by Android Build #2637. The documentation commit following it requires its own CI check.

- At checkpoint time, PR #23 was OPEN, base `fix/ui-shell-format-width-startup`, implementation branch `feature/mpc-one-ui-fidelity`, tested SHA `94ee3bfa9a8407d20ab6f0c69bde47519fe8fb07`.
- Android Build #2637 / run ID `38110061687` completed **SUCCESS** for that exact SHA; all 57 workflow steps passed. APK and visual-evidence artifact IDs, sizes, SHA-256 sums, and inspected files are listed in section 3.
- The local Browser runtime path now verifies real MediaStore WAV discovery → Search positive/negative → Clear → PROJECTS/ALL/SAMPLES filter changes → result selection → genuine audition → native Pad/Layer assignment. The evidence hierarchy explicitly shows `CURRENT SAMPLE • Browser-Smoke-Local.wav` and `ASSIGNED • Browser-Smoke-Local.wav → PAD 01 / LAYER 01`.
- The next concrete task is **a real Android Document Picker selection → return → target-preservation/sample-state test**. After that, add a separately tested user-selected document tree and persisted favorites. Avoid further cosmetic-only Browser work until these workflows are complete.
- Known gaps: real picker selection/return assignment still needs runtime coverage; user-selected tree/favourites are not implemented; non-WAV/project/plugin categories have no real provider and must stay unavailable; physical MPC Studio MkII verification remains pending.
- The local-first scope stays fixed: no Splice, stores, paid expansion promotion, partner ads, upsells, or unnecessary vendor accounts/cloud.

