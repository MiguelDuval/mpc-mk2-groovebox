# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`.
- Branch: `feature/mpc-one-ui-fidelity`; PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is **OPEN**, base `fix/ui-shell-format-width-startup`, HEAD `57744f9eed12a34076f2b5911c2ad7e9c7d1c4a6`.
- [Android Build #2766](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39048036076) completed **SUCCESS** on that exact HEAD: source checks, JVM/native checks, debug APK assembly/upload, emulator UI smoke and visual-evidence upload all passed.
- APK artifact: `app-debug.apk`, artifact ID `12106627441`, 8,228,253 bytes, digest `sha256:4c1ee5d06ba16ef67db2ee1694f53d1ec6aee26e149b41c30c1bf6f66e56ccb5`.
- UI-evidence artifact: `mpc-ui-visual-evidence`, artifact ID `12106253584`, 4,085,397 bytes, digest `sha256:38a349063dee27a194f5ca8742724b690c5d0a55967f82b58ec57b7bbc5a6d25`. Downloaded and inspected the real Main screenshot/hierarchy, Track Edit before/changed/reopened/restored screenshots and hierarchies, Arrangement→Grid focused-step capture, plus Grid DRAW and ERASE screenshots/hierarchies.
- Physical MPC Studio MkII verification remains **PENDING**. These results are from Android Emulator only.

## Completed in this session

### 1. Track Edit sample-layer tuning acceptance — VERIFIED

The TUNE row is below the initial viewport in the real Track Edit Samples `ScrollView`; it was not a missing or inaccessible backend parameter. The smoke test scrolls inside the live container using accessibility-derived bounds and waits for the actionable TUNE+ control. It retains the real +1-semitone check, comparison to native sampler state, leave/re-entry persistence and safe restoration of the original value. The actual entry route (Main or MPC Menu TRACK EDIT) is preserved during re-entry.

Build #2766 repeated the acceptance: baseline `+0.00 st`, one real TUNE+ tap to `+1.00 st`, persistence after leaving/reopening Track Edit, then restoration to `+0.00 st`.

### 2. Main left Channel Strip geometry — FIXED AND VERIFIED

The old condensed Mixer view was a duplicate weighted child: `applyCompactMixerVisibility()` re-exposed it from the saved Mixer state, splitting the shell's left region in half. It was not an Android-insets or firmware-coordinate problem. The legacy view remains mounted but `GONE`, preserving state/refresh dependencies and the shell show/hide control. The smoke test validates live relative geometry.

Build #2766 hierarchy reports `MPC Main XL Mixer Strips [137,135][666,1069]` and no legacy condensed-mixer content panel. The XL mixer passes the height/width fill thresholds and its toggle hit target remains contained.

### 3. Grid DRAW/ERASE native-state round-trip — VERIFIED

The Grid cells are custom-drawn on a Canvas and do not expose per-cell Android views. `MainActivity` now sets the selected-cell accessibility description from `nativeSequenceGetGridVelocities()`, reporting real note presence and velocity rather than inventing UI state.

The smoke test derives the touch coordinate from the live Grid canvas bounds and Android `wm density`. Starting from the actual Arrangement→Grid route, it touched Pad 01 / Step 34; the native projection reported velocity 100. It then selected the enabled ERASE control from the real accessibility hierarchy, touched the same cell, and confirmed “no note.” The seeded Step 33 note was preserved and DRAW mode restored. No assertion was weakened.

In Build #2766, the captured DRAW screenshot visibly shows the seeded Step 33 note plus Step 34; the ERASE screenshot shows Step 34 removed and Step 33 retained. The related accessibility XML agrees with the actual selected-cell state.

## Current execution order

1. Add an external-emulator touchscreen acceptance for Step Sequencer: use the live hierarchy to tap a visible step, verify the real native note parameters, alter a parameter through an actual visible control, then remove the temporary note without changing the seeded fixture note. The existing in-app audit already covers paging and state-preservation; retain it rather than duplicating another synthetic-only flow.
2. Run the full Android workflow at the exact resulting SHA, inspect its conclusion and downloadable APK/evidence. Keep physical MPC Studio MkII testing separately marked pending.
3. Re-check live PR #23 HEAD/base and Actions again before the next implementation slice.

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
