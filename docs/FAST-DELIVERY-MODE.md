# Fast Product Delivery Mode

**Effective:** 2026-11-07  
**Status:** Active operating contract; overrides older process instructions wherever they conflict.  
**Applies to:** ChatGPT, Cline, GitHub coding agents, reviewers and future contributors.

This project is changing from evidence-heavy micro-iteration to **fast, product-first vertical delivery**. The objective is a usable MPC instrument, not a perfect CI dashboard.

## 1. The outcome we optimize

Optimize for the number of complete, usable musical workflows delivered per session.

A working end-to-end behavior is worth more than:
- another planning document;
- another branch or integration PR;
- another test harness;
- another cosmetic screenshot;
- another isolated helper with no user-visible route.

A slice should connect real UI/controller input → real domain command → real audio/state change → meaningful user feedback → persistence when appropriate.

## 2. One active development line

- Keep `main` untouched and stable.
- The active development line is `checkpoint/mpc-ui-reference-and-product-20261030` unless live GitHub state or the owner explicitly selects a replacement.
- Verify its live HEAD at session start. The branch name is stable; its SHA is not.
- Make ordinary product commits directly on this one branch. Create a separate branch only when work is genuinely independent or the canonical branch is blocked by another writer.
- One writer owns the active branch at a time. ChatGPT, Cline and other agents must not write to it concurrently.
- Stop creating a new PR for every small fix. Keep one meaningful integration PR for a milestone; use commits on the active line for the implementation batch.
- Do not continue stale stacked branches or PRs merely because they remain open. In particular, do not independently advance old integration stacks until their base/head relationship has been reconciled with the active branch.
- No force-push, reset, rebase, squash or history rewrite.

## 3. Default development loop — FAST

For each session:
1. Read this file and the live checkpoint; fetch the real active branch HEAD and latest workflow status.
2. Select the next highest-value item from the current queue in `docs/IMPLEMENTATION-ROADMAP.md`.
3. Inspect only the relevant source, callers and nearby tests. Do not audit the whole repository unless required by the task.
4. Implement a complete, coherent vertical slice. Do not stop after an audit or plan.
5. Build the Android debug APK through the GitHub Actions **fast lane**.
6. If the app builds, continue to the next independent item in the same product batch. Do not sit idle waiting for emulator acceptance unless the current work depends on it.
7. At the end of the batch, report concrete behavior changed, exact commit/HEAD, APK artifact/run, known defects and the next product slice.

The fast lane is the default for every push and ordinary pull request. It compiles/packages the app and uploads an APK. It does **not** run the full native/JVM suite or the long emulator smoke flow.

## 4. Test policy: keep tests, stop letting them run the project

Tests are tools for finding defects. They are not the product and they must not dictate every development step.

### Fast lane — default

- Compile/package the debug APK.
- Upload the resulting APK artifact.
- Do not run the full native test matrix, full JVM/JUnit test matrix, shell smoke preflight or Android emulator smoke test on every commit.
- Newer fast runs may cancel older fast runs for the same branch; stale fast runs have no value once a newer source revision exists.
- Documentation-only changes should not start Android builds unless the workflow itself changes.

### Targeted checks — selective

Run a focused test only when it directly covers code changed in the current slice and is cheap enough to give immediate feedback. Prefer one relevant test over a broad suite.

Do not add new tests merely to duplicate the same behavior through another harness. Reuse existing test coverage where possible.

### Full validation lane — deliberate milestone gate

Run the complete native/JVM/UI-emulator suite only when at least one of these is true:
- a major end-to-end workflow is ready for acceptance;
- a change touches realtime audio safety, MIDI transport/controller semantics, project serialization/migration, destructive editing or other high-risk state;
- a release candidate is being prepared;
- the owner explicitly asks for full verification.

Trigger it by adding the `full-validation` label to the milestone PR or appending `[full-validation]` to its title. The PR workflow listens for label/title edits. A commit marker is also supported on push-triggered branches. A `workflow_dispatch` full mode is available where GitHub exposes manual dispatch for the workflow.

Full validation is not run for every ordinary code change. Run it once at a meaningful milestone, not after every small commit.

### Failure triage

Classify a failing check before changing code:
- **Product/build defect:** the app does not compile/package, crashes, loses/corrupts user data, produces incorrect musical state, or destabilizes realtime audio. Fix the relevant defect.
- **Test harness defect:** flaky marker, stale hierarchy, timing race, brittle text assertion or broken capture helper. Do not pretend the product failed. Record the exact failing step and evidence, then continue independent product work.
- **Runner/environment defect:** emulator boot, SDK download, KVM, network or GitHub runner problem. Do not modify product code to compensate for an unrelated runner problem.
- **Non-critical product defect:** small visual mismatch, minor interaction rough edge or non-destructive limitation. Record it and continue unless it blocks the current workflow.

Do not run the same failed full workflow repeatedly without a relevant code/configuration change. One good diagnosis is more useful than five identical reruns.

Do not delete tests, falsify results, weaken meaningful assertions or hide known data/audio defects just to get a green badge. The change is **when** tests run and **what blocks progress**, not whether we care about correctness.

## 5. What is allowed to ship temporarily

The owner accepts small, non-critical defects being fixed in a later pass. Agents should not turn every minor imperfection into a project-wide blocker.

Acceptable temporary debt:
- imperfect spacing, colors or non-critical visual details;
- a secondary feature explicitly marked unavailable;
- a minor non-destructive interaction issue outside the current slice;
- a test-harness issue that does not prevent building/running the current product path.

Never knowingly accept:
- silent/corrupt project save or load;
- destructive loss of musical data;
- an audio-thread crash, deadlock or unbounded/blocking realtime operation;
- a broken core MIDI input path presented as working;
- a false success state for an operation that did not happen.

When a defect is deferred, write one concise entry with severity, affected workflow and reproduction clue. Do not create a new large tracking framework.

## 6. Scope and documentation discipline

- Implement product behavior before expanding research or documentation.
- Update only the canonical documents affected by a real change. Do not generate another status document for the same facts.
- Do not repeat research already present in the repository.
- Do not add a third-party engine or framework simply to make the APK larger. Integrate a dependency only when the next real user workflow needs it.
- The current build needs only the Oboe submodule. CI must not recursively clone JUCE, Tracktion Engine and Ableton Link while their build options remain OFF. If a future slice enables one of them, initialize only the newly required submodule(s) and update this rule.

## 7. Product completion metric

Count a feature only when a user can exercise it through its real route. A native helper, unit test, MIDI decoder, UI label or placeholder screen alone is not a completed feature.

Prioritize the first complete instrument workflow:
1. create/open a project;
2. assign a real WAV sample to a Pad/Layer;
3. trigger it from the physical MPC Studio MkII with velocity preserved;
4. record a MIDI pattern;
5. edit and play the pattern;
6. save and reopen the project without losing the musical state.

Then expand to the next high-value workflow. Prefer completing one end-to-end path over adding more isolated components.

## 8. Explicit agent behavior

- Do not respond with a plan and stop. Once the relevant source is understood, implement.
- Do not stop after one small fix if another independent, clearly scoped product task is available.
- Do not start another full-suite run just because the current CI is red. First classify the failure and decide whether it blocks the current code path.
- Do not spend a session repairing unrelated test infrastructure.
- Do not ask the owner to adjudicate routine technical decisions already covered by this contract. Make the smallest reversible engineering choice and document it.
- Give short milestone reports, not constant progress narration.
- Never claim that the full workflow or physical controller was verified unless the corresponding evidence exists.


## 9. Visual references and action catalog — useful, not blocking

- Use the action catalog as a quick lookup for substantial interactions; do not make a new catalog row a prerequisite for routine implementation.
- Use local MPC visual references for meaningful screen redesigns and milestone review when the relevant asset is readily available.
- If a screenshot is missing, inaccessible or not an exact match, record the gap briefly and continue with the closest reference and current implementation.
- Do not spend sessions downloading/reconstructing reference images, writing catalog metadata or capturing before/after evidence for every small commit.
- Screenshots/XML are milestone evidence, not a daily development toll.
