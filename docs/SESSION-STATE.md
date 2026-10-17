# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Live state checked before this documentation commit

- Repository: `MiguelDuval/mpc-mk2-groovebox`.
- Branch: `feature/mpc-one-ui-fidelity`.
- PR: [#23 — MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23), OPEN; base `fix/ui-shell-format-width-startup`.
- Implementation HEAD checked: `35a608af7e13867257cf4ae3ed2e6ec3d6bf034b`.
- Latest inspected workflow for that SHA: [Android Build #2754](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39031420706), **FAILURE** in `Run Android emulator smoke test`. C++/native/JVM checks and APK assembly/upload passed before smoke failed.
- Failing assertion: `Track Edit did not expose the layer tuning value`. The failure is not yet conclusively classified as a product layout defect or a brittle test/scroll assumption. Inspect fresh UI hierarchy and logs; preserve the actual tuning workflow acceptance contract.
- APK artifact for #2754: `app-debug.apk`, artifact ID `12100765905`, 8,228,253 bytes, published digest `sha256:9c0729ea00d10fbb76c24bc23f5fe5652dfd2b40c8b82d1022f8fd31d6247757`.
- Visual evidence artifact for #2754: `mpc-ui-visual-evidence`, artifact ID `12100158502`, 1,541,992 bytes, published digest `sha256:7b81bd186c2667463215e245a6d8850b2ae174b80b062659db76b0574631657c`. Artifacts were listed from the workflow but not downloaded/inspected during this documentation pass.
- Physical MPC Studio MkII verification remains **PENDING**.

## Current execution order

1. Fix and verify the Track Edit tuning acceptance failure. Use the real UI, correctly handle scrollable content, verify the tuning value changes by one semitone, verify state/display agreement and exit/re-entry persistence, and restore the original test value safely. Do not relax assertions.
2. After CI is unblocked, investigate the owner-reported Main left Channel Strip geometry issue described in the current queue at the top of `docs/IMPLEMENTATION-ROADMAP.md`. A large unexplained panel above the strips appears to push the strips out of alignment. Capture a fresh landscape screenshot and UI hierarchy; determine the real panel/constraint/inset/scaling cause before changing code.
3. Continue the next high-value working UI slice only after the above blockers are resolved.

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
