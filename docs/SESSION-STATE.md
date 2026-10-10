# Session State — MPC MkII Groovebox

**Last checkpoint written:** 2026-10-10  
**Purpose:** Fast handoff between ChatGPT sessions. Read first, then verify every mutable fact against live GitHub.

> **Important freshness rule:** This checkpoint is not live telemetry. At the start of a session, fetch the actual branch HEAD, PR #23 state/base/head, recent commits, relevant Actions runs/checks and artifacts. Never assume this file's referenced SHA is still HEAD. The last verified repository HEAD before this checkpoint write was `bf019ea84a17ebfcd3d3cbec871d2e28b6c2aa10`; writing this file will itself advance the branch.

## 1. Repository and branch safety

- Repository: `MiguelDuval/mpc-mk2-groovebox`
- Working branch: `feature/mpc-one-ui-fidelity`
- Primary PR: [#23 — feat: MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23)
- Last checked PR state: OPEN, not merged.
- Last checked PR base: `fix/ui-shell-format-width-startup`.
- Before the documentation refresh, branch comparison reported 448 commits ahead / 0 behind against the PR base. Recheck; the count changes whenever documentation is committed.
- Never reset/rebase/force-push, do not retarget or merge PR #23 during routine work, and do not edit `main`.
- Preserve the realtime audio callback, sampler playback/recording engine, sequencer scheduler/clock, MIDI transport, raw MPC Studio MkII decoder, SysEx protocol and working domain operations.

## 2. Product decision: local-first, no commercial ecosystem

This decision is approved by the product owner and is mandatory for implementation:

**Exclude from the actual product UI:** Splice, online stores/marketplaces, commercial expansion promotion, partner advertising, upsell panels, nonessential branded cloud integrations and vendor-specific sign-in/account dependencies.

**Keep:** actual local/app-owned files, generic local content categories backed by real results, Android Storage Access Framework / Document Provider as a lower-level storage primitive, available USB/external storage, user expansion folders, semantic sample browsing, preview where it is genuinely implemented, and loading/assignment to the selected Pad/Layer.

The official MPC reference documents may still mention factory commercial items as historical/reference research. That does not make those items product requirements. A visible UI item must be implemented, explicitly unavailable, or omitted; never fake a provider or action.

## 3. Last verified build and evidence — not current HEAD

- Last known successful Android Build: **#2608**.
- Tested implementation SHA: `77f4e18a8f40a2e3d58ec8dbdf873cb09070fe4a`.
- [Build #2608](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/38074926971).
- Live artifact listing/download for run `38074926971`: the `app-debug.apk` artifact (ID `11677509340`) has SHA-256 `43c036226d164d9c5fd911f9abd767f06b73e98049be00ac7a0547d91a730111`; the downloaded object is an APK ZIP container.
- Live artifact listing/download for the `mpc-ui-visual-evidence` archive (ID `11677764864`) has SHA-256 `4c9b46ec574c8b75573cc0d8ab83ed76fd347f37111582dd22b5036f357b6773`.
- **Evidence archive inspected directly:** it contains 10 files, including `mpc-groovebox-startup.png`, `mpc-groovebox-main-ready.png`, `mpc-groovebox-main-ready-uiautomator.xml`, Main UI hierarchy, Browser screenshot/hierarchy/XML, and Android storage-picker screenshot/XML/activity data. The ready-Main screenshot is 1920×1080, and all six required Main accessibility descriptions are present exactly once with positive bounds. A previous handoff reported those files missing from a different inspected archive; the direct inspection above resolves that discrepancy for artifact ID `11677764864`.
- After the documentation commits made during 2026-10-10, no successful workflow had yet been confirmed for the then-current documentation HEAD. A fresh Android Build was observed queued on commit `8d59f33fda3bf0f0dec66cc3455212369c1913c6`; this state-file correction will advance HEAD again, so verify Actions against the new live HEAD. The success above proves only the app SHA `77f4...`, not the documentation commits.
- Emulator evidence does not equal physical MPC Studio MkII verification.

## 4. Implementation snapshot

This is an observed high-level map, not a claim that each context is finished:

- **Main shell:** canonical toolbar, five shortcut rail, XL strip host, workspace and Function Bar exist; structural fidelity is improved but detailed MPC visual/functional parity remains incomplete.
- **Browser:** partial. The existing view has Places / Content / Expansions, search, six file-type filters, Options, target Pad/Layer, current-sample state and a native Android storage-picker action. The results area is still placeholder/informational rows rather than a proper semantic local-file result list. Splice has been present in Content navigation and must be removed from the actual app.
- **Browser gaps:** true local results/provider, search/filter behavior over those results, persisted favourites, genuine audition state, meaningful options, robust semantic Load/Assign flow.
- **Track View / Arrange / Grid / Step:** partial; preserve implemented semantic operations and explicitly reserved gaps. Do not claim full non-Drum Grid or edit-command parity.
- **Sampler / Sample Edit:** substantial software/UI foundation already exists (recording, waveform, region, crop/chop, monitor/threshold and sample-layer semantics); continue reusing it.
- **Pad Mixer:** supported Level/Pan/Tune path exists; additional routing/FX remains incomplete.
- **Channel Mixer, Project persistence, List Edit, Pad Perform, XYFX and other advanced contexts:** documented reserved/partial gaps. Do not fabricate them.
- **Physical MkII validation:** keep pending unless there is a specific current physical test report.
- Do not infer product completeness from the accumulated 400+ commits. Feature and acceptance matrices matter more than commit count.

Canonical detailed status lives in `docs/MPC3-9-OS-KNOWLEDGE-BASE.md` and `docs/IMPLEMENTATION-ROADMAP.md`.

## 5. Documentation refresh just made

New canonical process document:
- `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` — session loop, evidence contract, Browser acceptance target, local-first exclusions and free-tier/context-efficiency rules.

Documents updated to refer to the new rule and/or product decision:
- `AGENTS.md`
- `docs/PROJECT_MASTER_PROMPT.md`
- `docs/IMPLEMENTATION-ROADMAP.md`
- `docs/MPC3-9-OS-KNOWLEDGE-BASE.md`
- `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md`
- `docs/MPC3-REFERENCE-INDEX.md`
- `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md`

The documentation-only commits preceding this checkpoint include:
- `c53b3508237d521c64f7216d9cb8e7f249280260`
- `ff82e2d94fb9818c862d3b53ae01d45e265dd553`
- `fe5c08318fefd46bd3e49d7a0130bd77b288ba3f`
- `79ea7a921b5d2e9b6d65168bc550bad183558a76`
- `dc54fa50a6e5ec64a414a690cdf6e6f06932bb10`
- `d243473cc03aad6aac6f7e895ad2cc9c2c812fc3`
- `a56b0bf31d96c2efc808957b7be4b7413632b17a`
- `bf019ea84a17ebfcd3d3cbec871d2e28b6c2aa10`

These were documentation commits; their existence does not prove runtime behavior or a successful build at their HEAD.

## 6. Next implementation sequence

### Task 1 — remove excluded commercial Browser surface

1. Read the updated operating protocol, state, roadmap, relevant Browser code and tests.
2. Remove `SPLICE` from the actual Content navigation and add regression coverage that it is not presented as an actionable product destination.
3. Inspect the rest of the implemented UI for comparable commercial/vendor-cloud/marketing entries; remove only clearly excluded surfaces, preserving generic local folders and external storage access.
4. Avoid changing audio/MIDI/domain logic.
5. Run targeted source/unit/contract tests; then create a fresh APK and complete the relevant Android smoke/visual audit.
6. Inspect the actual evidence ZIP contents; explicitly verify screenshots/XML expected by the workflow. Fix any missing-artifact mismatch rather than merely changing the log.
7. Record the exact code commit, CI URL/result, artifact checksum and remaining evidence gaps in this file after verification.

### Task 2 — semantic local Browser provider and results

Design before coding: inspect Android document/storage APIs and current sample-import callbacks. Implement the smallest vertical slice that shows real accessible local audio files, selects a real item, preserves the chosen target Pad/Layer, loads/assigns it, and proves the resulting sample state. Add search/filter next, then favourites/options only when backed by real persisted semantics.

### Task 3 — finish high-value musical contexts

After Browser acceptance, prioritize Track/Arrange/Grid/Step real workflows; then mixer depth and project persistence according to existing domain contracts. Do not spend extended time on decorative polish while a high-frequency musical workflow remains fake/incomplete.

## 7. Session completion/update rule

At the end of every meaningful milestone, update this file once with verified facts:
- actual HEAD and date checked;
- PR state/base/head;
- tested SHA and Actions result;
- APK/artifact identity and checksum when available;
- evidence files actually inspected and any missing ones;
- change and tests;
- physical hardware status;
- exact next task.

Do not repeatedly commit status-only changes during a single feature slice. Do not predict the SHA of the commit that will contain this file. In the next session, fetch live state first.
