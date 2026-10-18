# Development Operating Protocol — MPC MkII Groovebox

**Status:** Canonical working-process contract  
**Product branch:** `feature/mpc-one-ui-fidelity`  
**Primary PR:** #23 — MPC One Main UI visual fidelity pass  
**Purpose:** Reduce repeated research and test churn while increasing the number of complete, verified musical workflows delivered per session.

This document governs *how work is performed*. The project master prompt governs product/engineering principles. `docs/SESSION-STATE.md` is a short-lived checkpoint and must be verified against live GitHub state before it is trusted.

## 1. Source of truth and document hierarchy

At the start of each session, read these in order:

1. `docs/SESSION-STATE.md` — recent checkpoint, next task, known evidence gaps.
2. `docs/PROJECT_MASTER_PROMPT.md` and `AGENTS.md` — permanent project rules.
3. This file — execution loop, scope exclusions, completion criteria.
4. `docs/IMPLEMENTATION-ROADMAP.md` — priorities and known product gaps.
5. `docs/MPC3-USER-ACTION-CATALOG.md` — identify the action ID, expected state transition, source evidence, current implementation status and required acceptance path for the UI scenario being changed.
6. `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md`, `docs/MPC3-9-OS-KNOWLEDGE-BASE.md`, `docs/MPC3-REFERENCE-INDEX.md`, `docs/MPC3-UI-SCREEN-BLUEPRINT.md`, and `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md` — only the sections relevant to the current slice.
7. When working on the physical MPC Studio MkII, also read `docs/HARDWARE-MPC-STUDIO-MK2.md`, `docs/MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md`, `docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md`, `docs/MIDI-TRANSPORT.md`, and the applicable sections of `docs/PHYSICAL-TEST-CHECKLIST.md`.
8. Inspect current source, callers, relevant tests, latest commits, PR checks, exact-SHA CI runs and artifacts.

**Live-repository precedence:** Current branch/PR/Actions/source evidence outranks stale session notes. When evidence disagrees, verify it, correct the relevant document, and record the discrepancy. Do not re-read hundreds of commits by default. Inspect the latest 20–50 commits and broaden the history only when answering a specific historical or regression question.

## 2. Product scope: an autonomous local instrument

The product is a locally useful groovebox controlled primarily by the MPC Studio MkII. MPC Standalone OS 3.9 is the interaction and information-architecture reference, not a mandate to reproduce Akai's commercial ecosystem.

### Intentionally excluded

- Splice or another branded third-party content subscription.
- Online sound stores, paid expansion promotion, marketplace panels, partner advertisements, sales banners, upsell prompts, and nonessential cloud content integrations.
- Sign-in/account flows or vendor-specific cloud dependencies that are not required for local music production.
- Decorative entries that resemble working features but have no actual route/provider.

Do not add the items above unless the product owner explicitly changes this decision.

### Preserve the useful underlying capability

- Local internal/app-owned content and user files.
- Android Storage Access Framework / Document Provider as a platform mechanism.
- Available USB/external storage, subject to Android permissions and real device support.
- Local WAV import, semantic file browsing, sample audition where implemented, target Pad/Layer, sample assignment, and local expansion/user-content folders.
- Generic abstractions such as Places, Samples, Drums, Instruments, My Files, and user-defined favourite folders where they resolve to real data.

A category is not functional merely because it appears in the UI. Either connect it to a real provider/action and test it, mark it explicitly RESERVED/UNAVAILABLE, or omit it. An Android picker is a storage primitive underneath the product Browser; it must not become the product's whole browsing model.

### Canonical reference vs product-specific decision

Research documents must remain truthful about what Akai documents (including commercially branded services). The product scope above overrides copying such entries into our app. When documentation describes a factory item that we intentionally omit, label it **REFERENCE ONLY — intentionally excluded from this product**. Do not rewrite historical research to pretend the factory UI never had that item.

## 3. Priority order for the next UI milestones

Work in this order unless a newly discovered regression or an audio/MIDI stability issue warrants an urgent interruption:

1. **Protect the working foundation.** Preserve the current audio engine, realtime callback, sampler, sequencer clock/scheduler, MIDI transport, raw MkII decoder and hardware SysEx protocol. Do not rewrite them for presentation convenience.
2. **Remove excluded commercial surfaces from the actual product UI.** Start with Splice in Browser navigation and audit other top-level modes for stores, partner services, branded cloud dependencies and dead marketing surfaces. Keep the reference taxonomy documented but mark intentional product deviations.
3. **Finish the local Browser as a semantic workflow.** Implement truthful local results (not placeholder rows), useful place/category navigation, search, file-type filters, persistent favourites only when backed by storage state, and meaningful metadata. Keep Android Document Provider below this layer.
4. **Complete the sample workflow end to end.** Select a real file, preview it through a genuine audio action, preserve the selected target Pad/Layer, load it, assign it to the correct sample/layer state, return to the relevant context, and verify the result. Clearly identify any operation whose backend does not yet exist.
5. **Move to high-value musical contexts.** Deepen Track, Arrange, Grid and Step workflows in complete vertical slices; then improve mixer routing and project persistence according to the real domain/backend contracts.
6. **Verify hardware ergonomics.** Where behavior depends on MPC Studio MkII input or feedback, distinguish automated/emulator coverage from physical-device verification.

Do not chase isolated visual details while a high-frequency workflow is still a nonfunctional shell, unless the visual defect prevents that workflow from being tested.

## 4. The bounded implementation loop

For each coherent feature slice:

1. **Orient:** fetch the live branch HEAD, PR #23 state/base/head, recent commits, latest relevant Actions runs, and the known-good APK/artifact identity. Confirm all edits are on `feature/mpc-one-ui-fidelity`.
2. **Define:** state the user scenario, expected MPC-inspired behavior, current gap, relevant source files, real semantic/backend commands, acceptance checks, and what is explicitly out of scope.
   - For an MPC-style UI interaction, cite the catalog action ID; if no row exists, add one before implementation. Keep the official reference behavior, product-specific deviation, backend truth and test evidence distinct.
3. **Trace:** read the current view, its listeners/callers, the authoritative state model, relevant backend operation and existing tests. Do not recreate source from memory.
4. **Implement:** make the smallest coherent change that completes part of the user scenario. Keep one logical intent per commit and preserve existing domain/audio/MIDI paths.
5. **Test narrowly first:** run source/contract checks and the smallest relevant unit or native test. Fix failures before adding unrelated work.
6. **Verify the whole slice:** build a fresh APK; run Android UI smoke/emulator checks appropriate to the change; inspect real screenshot and accessibility/UI hierarchy; verify the action/state outcome, not only the visible control.
7. **Inspect CI evidence:** confirm Actions belongs to the exact current SHA; verify success, APK artifact existence, artifact integrity where available, and required screenshots/XML files inside the downloaded archive. A green run with missing requested evidence is a reporting/evidence defect and should be fixed.
8. **Close the loop:** update the roadmap/status matrix and `docs/SESSION-STATE.md` at the end of a meaningful milestone. Record commit, workflow URL/result, artifact name/checksum where available, tests, visual evidence, hardware status, and the next bounded task.

Keep the number of expensive full builds proportional to the amount of change. A small isolated view change can use a fast relevant test before the end-to-end build. Changes to Activity lifecycle, navigation, persistence, permissions, audio, MIDI, or device integration need broader tests sooner. Never skip the final required CI verification simply to save time.

### 4.1 Fast feedback, test economics, and failure triage

- Start with the smallest test that can answer the current question: a targeted unit/native test, source-contract test, or isolated UI scenario. Do not run unrelated Browser/Arrangement/Track workflows just because the only available command happens to run everything.
- Keep one reusable implementation of each UI acceptance scenario. Prefer selecting a scenario by an argument/environment variable or extracting a small helper over copying a second version of the flow.
- Full Android build + emulator smoke + artifact inspection remains mandatory at the acceptance boundary for a meaningful user-visible slice. A targeted green test is not a substitute for the final current-SHA integration run.
- When a run fails, classify it before changing code: (a) product defect, (b) test-fixture/setup defect, (c) brittle test assumption, (d) environment/tool failure. Use the failing step, UI hierarchy, screenshot and logs to determine which category applies. Fix the cause; never delete the check, lower its assertion, or call a build successful when it failed.
- UI tests must operate the real interface. If a target is inside a scrollable container, scroll to it using the current accessibility hierarchy before asserting visibility/actionability. Do not assume a control is visible merely because its view exists in source.
- Avoid repeated full builds for documentation-only and narrowly scoped changes when local/source validation is sufficient; however, respect repository branch/PR rules and never report a new SHA as integration-verified until its actual workflow result is known.
- Keep failure output bounded: report the failing step, root cause evidence, exact SHA/run URL and minimal relevant log excerpt, not the whole giant shell log.

## 5. Acceptance contract: evidence over appearance

For each feature, distinguish the following:

| Dimension | Evidence required |
|---|---|
| Visual | Actual runtime screenshot at the intended orientation/resolution, with no audit diagnostics covering the product UI |
| Interaction | Touch and/or semantic hardware action reaches the correct handler; hit targets, focus and Back behavior are usable |
| Musical state | Correct selected Sequence/Track/Program/Pad/Layer/file state, with the real domain/native command used where applicable |
| Persistence/navigation | State survives expected context changes and returns; persisted settings are only claimed if storage is implemented and tested |
| Hardware | Controller input and feedback tests; physical MkII result reported separately from emulator/software simulation |
| CI | Successful current-SHA workflow and a fresh APK artifact |
| Evidence package | Expected image/XML/log artifacts actually exist and match the run/commit being reported |

A label, button, accessibility description or fake meter does not prove functionality. Do not invent backend state, pretend an operation succeeded, or change unsupported actions into unrelated working screens.

## 6. Browser-specific acceptance target

A future Browser milestone is not complete until the supported local workflow has a real, tested path:

- Places/category changes affect actual results and do not leave stale navigation state.
- Search and type filters affect a semantic result model.
- Results represent accessible local items rather than placeholders.
- The selected item, destination Pad/Layer, and current sample are visibly and semantically distinct.
- Audition plays the selected item through a genuine supported path, or is explicitly unavailable until that path exists.
- Load/Assign changes the correct sample/layer state and is verified after navigation.
- Storage picker returns through the app flow without losing the user's intended target.
- Favourites persist only if a real URI/provider-backed implementation and tests exist.
- Browser Options must either implement a meaningful setting or remain unavailable/omitted; disabled decorative settings must not be described as complete.
- Splice and commercial content marketplace routes are absent from this product.

Use small independently verifiable slices if all of the above cannot be delivered in one commit.

## 7. Branch, code and repository safety

- Continue on `feature/mpc-one-ui-fidelity` and PR #23 unless the owner explicitly authorizes a different destination.
- Never modify `main` for this work; no reset, rebase, history rewrite or force-push.
- Do not merge or retarget PR #23 as part of routine UI development.
- One agent/writer owns a branch at a time. Do not have ChatGPT's GitHub mutations and Cline edit the same branch/files concurrently. Parallel tasks require isolated branches/worktrees, explicit file ownership, and serial integration.
- Do not replace an existing working backend with a mock or speculative architecture.
- Keep the realtime callback free of UI work, file I/O, allocations and blocking operations.
- Do not add paid tools, API dependencies, cloud services, or new required credentials. Prefer current GitHub/ChatGPT integration, repository tests and the existing Actions workflow.
- Make no claim that physical hardware has been verified without a physical test result.
- Fix any failure introduced by the slice before moving on.

## 8. Free-tier and context-efficiency rules

The process must work within a limited ChatGPT context and usage budget:

- Read `SESSION-STATE.md` and targeted ranges first; fetch complete large files only when an actual edit requires full content.
- Reuse the canonical docs rather than restating all project history in each prompt.
- Use live GitHub status checks instead of relying on remembered build numbers or prior-chat claims.
- Avoid repeatedly dumping huge diffs/logs into the chat. Query exact files, failures, workflow jobs and artifacts; summarize only the evidence needed for a decision.
- Do not run broad public research when the current repository already records an adequate, cited official reference. Research externally when behavior is uncertain, version-sensitive, or absent from the local reference index.
- Do not add paid services or require a paid ChatGPT plan. If a task exceeds the current session/tool capability, make bounded progress and record the next action in the state file instead of pretending it was completed.
- Prefer one complete vertical slice and its evidence over numerous tiny status-only commits.
- Follow the dated current execution queue at the top of `docs/IMPLEMENTATION-ROADMAP.md`; older task labels lower in the file can be historical. If CI is red, unblock the current failure before picking an unrelated feature.
- Use the smallest relevant test while iterating, then one full verification at the slice boundary. Avoid adding a second copy of a UI smoke scenario when a focused mode can reuse the existing implementation.

## 9. Required session checkpoint

At the start, read `docs/SESSION-STATE.md`, then independently verify its branch, HEAD, PR and CI claims. At the end of a meaningful implementation milestone, update it with:

- timestamp (UTC);
- active branch and actual HEAD at the time checked;
- PR number/state/base/head and ahead/behind status when available;
- latest relevant workflow/run URL, conclusion and tested SHA;
- fresh APK artifact identity and checksum when available;
- names of evidence files inspected, including missing files;
- completed change, tests, and unresolved failures;
- hardware verification status;
- exact next task and acceptance criteria.

Because a commit changes the branch HEAD, do not put an unverified future SHA into the file or claim its own resulting SHA is known before the commit exists. Always refetch live HEAD in the next session. `SESSION-STATE.md` is a checkpoint, never an authoritative substitute for GitHub.

## 10. Stop conditions for a slice

A slice is ready to hand off when:
- the defined scenario works or every remaining limitation is explicit;
- relevant tests pass;
- the current-SHA build/CI result is known;
- expected artifacts were inspected;
- documentation reflects actual rather than intended implementation;
- the next bounded task is recorded.

Do not continue polishing the same screen indefinitely once its core acceptance checks pass; move to the next highest-value user workflow.
