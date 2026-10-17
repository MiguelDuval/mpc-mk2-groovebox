# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`.
- Branch: `feature/mpc-one-ui-fidelity`.
- PR [#23 — MPC One Main UI visual fidelity pass](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains OPEN; base `fix/ui-shell-format-width-startup`.
- Last code-changing source commit verified by the full workflow: [`11bc2de28d1af93c0165375791b19400c794a937`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/11bc2de28d1af93c0165375791b19400c794a937).
- [Android Build #2761](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39043633365) completed **SUCCESS** on that exact SHA: compilation, JVM/native checks, debug APK assembly/upload, emulator UI smoke, and visual-evidence upload all passed.
- APK artifact: `app-debug.apk`, artifact ID `12105011775`, 8,228,253 bytes, digest `sha256:10cb2425522458c8c11ff4e040f593f2000e7ddf0dfdfe6f2f028849119dbc74`.
- UI-evidence artifact: `mpc-ui-visual-evidence`, artifact ID `12105227341`, 3,462,105 bytes, digest `sha256:f2c9f3ce8ea2843cdf31e947a553cec699924c3bd41877bc70b49e1b65dbc1f6`. The post-fix Main screenshot/hierarchy and all four Track Edit tuning snapshots were downloaded and inspected.
- Physical MPC Studio MkII verification remains **PENDING**. Emulator success is not physical-controller validation.

## Completed in this session

### 1. Track Edit sample-layer tuning acceptance — VERIFIED

The TUNE field was not missing or inaccessible. The Samples body is a real vertical `ScrollView` with bounds `[693,251][1778,849]`; the layer Gain/Tune/Pan controls follow the waveform, region controls and Gain row, so TUNE is below the initial viewport. The failure came from the smoke test expecting an off-screen node to be immediately discoverable. The helper now uses the live hierarchy to calculate a safe swipe inside the actual ScrollView and waits for the actionable control to appear. The test also preserves the original navigation entry route (Main or MPC Menu) when leaving and re-entering.

On Android Build #2761, the acceptance log and evidence confirmed baseline `+0.00 st`, one real TUNE+ tap to `+1.00 st`, persistence after leaving/re-entering Track Edit, and restoration to the original `+0.00 st`. The test retains the one-semitone, state round-trip and restore assertions.

### 2. Main left Channel Strip geometry — FIXED AND VERIFIED

The fresh pre-fix capture from successful Build #2760 showed the same shell bounds `MPC shell mixer strips [126,124][677,1080]`, but two competing weighted children: legacy `MPC condensed Mixer Strip [137,135][666,602]` and the intended `MPC Main XL Mixer Strips [137,602][666,1069]`. The oversized upper panel was the old condensed mixer view, not a system inset or firmware-unit scaling problem. `buildCompactContext()` initially hid it, but `applyCompactMixerVisibility()` subsequently re-exposed it whenever the saved Mixer Strip state was visible.

Commit `11bc2de28d1af93c0165375791b19400c794a937` now keeps that legacy view mounted but `GONE`, retaining existing refresh/state dependencies and preserving `mpcShell.setChannelStripVisible(visible)` plus the show/hide control. The smoke preflight protects against re-exposing the duplicate panel.

The post-fix Main hierarchy from Build #2761 confirms that `MPC condensed Mixer Strip` is absent and the intended `MPC Main XL Mixer Strips` expands to `[137,135][666,1069]`. The screenshot visibly shows the XL channel strips filling the left region while the canonical Toolbar → five Shortcuts → XL Channel Strip region → Workspace → Function Bar shell remains intact.

## Current execution order

1. Continue with the next high-value, complete musical workflow in the existing Track/Arrange/Grid/Step areas. Start from live source, the current roadmap, and targeted test gaps; do not repeat firmware research or cosmetic passes.
2. Keep physical MPC Studio MkII verification explicitly separate from Android emulator verification.
3. On the next session, re-check live PR #23 HEAD/base and Actions before editing; this file is a handoff checkpoint, not a replacement for live GitHub state.

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
