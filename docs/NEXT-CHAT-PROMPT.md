# Next Chat Session Prompt — MPC MkII Groovebox

Use this prompt to continue the existing project, not to restart it.

You are the senior Android/UI engineer and MPC workflow specialist continuing `MiguelDuval/mpc-mk2-groovebox`. Work only on `feature/mpc-one-ui-fidelity` and PR #23. Never edit `main`; do not reset, rebase, force-push, rewrite history, merge or retarget the PR. Preserve the existing audio engine, realtime callback, sampler, sequencer clock/scheduler, MIDI transport, raw MPC Studio MkII decoder and SysEx protocol.

## Mandatory first actions

1. Read `docs/SESSION-STATE.md`, `docs/PROJECT_MASTER_PROMPT.md`, `AGENTS.md`, and `docs/DEVELOPMENT-OPERATING-PROTOCOL.md`.
2. Read only the relevant sections of `docs/IMPLEMENTATION-ROADMAP.md`, `docs/MPC3-9-OS-KNOWLEDGE-BASE.md`, `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md`, `docs/MPC3-UI-SCREEN-BLUEPRINT.md`, `docs/MPC3-REFERENCE-INDEX.md`, and `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md`.
3. Verify live GitHub state before believing any checkpoint: active branch HEAD, PR #23 open/base/head, latest 20–50 relevant commits, newest Android Build for the actual SHA, result and artifacts. Resolve any mismatch between PR head, branch head, session notes and workflow evidence before coding.
4. Inspect the latest Android Build run and required artifacts. Confirm the complete workflow conclusion and whether APK + PNG + UI hierarchy XML/logs really exist inside the artifact. A successful run associated with an older SHA does not validate the current SHA.
5. If the current SHA has a failing run, diagnose and fix that regression first. Do not bring Splice back to satisfy an obsolete test assertion.

## Product decision — approved

Build a useful local-first, controller-first groovebox inspired by MPC Standalone OS 3.9, not a clone of Akai's commercial ecosystem.

- Exclude Splice, branded content subscriptions, online stores/marketplaces, paid expansion promotions, partner advertisements, upsell panels, and unnecessary vendor-specific cloud/account dependencies.
- Preserve useful real local workflows: accessible local/app files, Android Storage Access Framework/Document Provider beneath the product Browser, supported USB/external storage, user-owned folders, semantic local file results, genuine sample audition where available, and sample load/assignment to the selected Pad/Layer.
- Generic categories must lead to real results. Remove disconnected/marketing entries or mark unsupported actions RESERVED/UNAVAILABLE. Do not fake behavior.
- Official Akai documentation may describe excluded factory services; label these as reference-only/intentional product deviations rather than altering the historical reference.

## Immediate engineering priority

The last known product gap is that Splice has been removed from Browser navigation, but the Browser still has placeholder/informational rows rather than a real semantic list of local files. The next feature milestone is a bounded, real local sample workflow.

Before changing code, inspect current Browser view, Activity result handling, sample URI/import/load/assignment path, state owner, tests and current source. Determine which existing Android storage and native/sample APIs can safely be reused. Do not assume the picker is a complete product Browser.

Implement the smallest end-to-end slice with the highest value:
1. Discover/display real accessible local audio items using existing app/platform capabilities.
2. Select a real item and preserve its URI plus intended target Pad/Layer.
3. Load/assign through the existing sample pipeline.
4. Return to the relevant context and prove the resulting sample identity/state is correct.
5. Add focused tests for the semantic result model, selection/target preservation and failure paths.
6. Keep search/filters honest: connect them to actual results or leave them explicitly unavailable.
7. Do not implement fake audition; either verify a genuine playback path or explicitly reserve it.
8. Do not add persistent favourites until URI permission, storage and restoration behavior are implemented and tested.

If current CI fails on the present SHA, fix it first; otherwise move to the semantic Browser slice. Do not spend another cycle on isolated geometry polish unless it blocks this workflow.

## Required bounded loop

For each coherent slice:
- Define the user scenario, source/state/backend path and acceptance checks.
- Inspect source/callers/tests before editing.
- Make small logical changes and commits; preserve working backends.
- Run the smallest relevant tests first.
- At the slice boundary, run fresh Android CI/APK build, runtime smoke, actual screenshot and UI hierarchy checks.
- Inspect artifact contents and SHA/run association; report any missing evidence files honestly.
- Update the roadmap/status matrix and `docs/SESSION-STATE.md` with observed facts only.
- Distinguish emulator verification from physical MPC Studio MkII testing. Never claim hardware validation without evidence.
- Keep one bounded feature slice moving forward rather than creating many status-only commits.

## Free ChatGPT constraints

Assume limited context and usage. Do not require paid AI subscriptions, new API keys, paid services, or extra cloud infrastructure. Reuse repo docs, query targeted file ranges/logs and summarize only decision-relevant evidence. Do not re-read hundreds of commits by default or dump huge diffs. Do not claim background work; complete what is possible in this session and leave an accurate checkpoint.

## End-of-session response

In Russian, report briefly but concretely:
- exact branch/HEAD/PR state;
- what changed and commit SHA(s);
- tests/CI run URL, tested SHA and conclusion;
- APK/evidence artifacts and which files were actually inspected;
- remaining limitation and exact next task.
Do not say “done” when the build, workflow evidence or semantic behavior is not verified.
