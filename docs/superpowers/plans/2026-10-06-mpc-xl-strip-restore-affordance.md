# MPC XL Channel Strip Restore Affordance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the collapsed XL Channel Strip's ambiguous Unicode restore glyph with the existing deterministic MPC mixer show/hide icon while preserving the current visibility state and shell geometry.

**Architecture:** `MpcShell` owns only shell geometry and presentation. The existing `MpcUiState.compactMixerVisible()` remains the source of truth; no navigation, mixer semantics, audio, MIDI, or realtime ownership changes. The existing `MpcMixerStripIconDrawable` supplies original deterministic vector geometry.

**Tech Stack:** Android Java 17, existing custom `Drawable` classes, source-level Android smoke contract, GitHub Actions Android build.

**Spec:** `docs/MPC3-UI-SCREEN-BLUEPRINT.md` and `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md`

## Global Constraints

- Work only on `feature/mpc-one-ui-fidelity`.
- Do not modify `main`; no reset, rebase, force-push, or history rewrite.
- Keep the XL Channel Strip width/visibility state owned by existing shell/UI state.
- Do not add raw MIDI, audio, sampler, sequencer, or hardware-protocol logic.
- Use original deterministic vector iconography; no proprietary Akai artwork.
- Keep the restore control a real touch target with truthful accessibility text.

## Review Focus

- Hidden Strip state: the restore affordance is visible only when the XL Channel Strip is hidden.
- Icon semantics: the visible control communicates show/hide/restore, not page navigation.
- Accessibility: the control has no visible Unicode glyph and its content description describes the actual action.
- Geometry: collapse remains 0dp channel-column width with the restore target on the workspace edge.
- Regression: the existing visible Strip path remains unchanged.

### Task 1: Deterministic XL Strip restore control

**Files:**
- Modify: `android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcShell.java`
- Modify: `tests/android_ui_smoke.sh`

**Interfaces:**
- Consumes: existing `setChannelStripVisible(boolean)` state and `MpcMixerStripIconDrawable.Mode.PERSONAL_CHANNEL_STRIP`.
- Produces: a hidden-state restore control with empty visible text, deterministic mixer icon, and truthful accessibility description.

- [ ] **Step 1: Add the source regression contract** requiring the restore button to use an empty visible text label, deterministic `PERSONAL_CHANNEL_STRIP` iconography, and no visible Unicode chevron glyph.
- [ ] **Step 2: Replace the restore button's `›` text in `MpcShell` with the existing `MpcMixerStripIconDrawable` show/hide icon and update its content description to describe showing the XL Channel Strip.
- [ ] **Step 3: Run the repository's source-level UI smoke/preflight checks and the focused Android unit test set covered by CI.
- [ ] **Step 4: Inspect the resulting GitHub Actions run; accept the code only when compilation/tests pass and any remaining failure is isolated to the already-known emulator infrastructure problem.
- [ ] **Step 5: Commit the implementation as one small presentation-only commit.**

