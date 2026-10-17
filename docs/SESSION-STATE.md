# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-17  
**Purpose:** fast operational handoff. Verify all mutable details against live GitHub before implementation.

## Latest verified implementation state

- Repository: `MiguelDuval/mpc-mk2-groovebox`; branch `feature/mpc-one-ui-fidelity`.
- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains **OPEN**, base `fix/ui-shell-format-width-startup`, HEAD `f9d27bd418d279d5cb76f00e1f1ead7e13fb0a41`.
- [Android Build #2779](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39059031605) completed **SUCCESS** on that exact SHA: compilation/native/JVM checks, debug APK assembly/upload, full emulator smoke and visual-evidence upload passed.
- APK artifact: `app-debug.apk`, artifact ID `12109844469`, 8,244,637 bytes, digest `sha256:cf04913a32641ba22c7b91cd0e1c6898ca70028cfdb7af2326665f294504552b`.
- UI-evidence artifact: `mpc-ui-visual-evidence`, artifact ID `12110627619`, 5,222,001 bytes, digest `sha256:55def79d6f8ad0fed90a60e753a8e5d9e8a70ec31e97cbd07b7fe616dc0e659a`. The evidence ZIP was downloaded, unpacked and visually inspected: Main landscape, Arrangement loop before/changed/restored, Grid DRAW/ERASE, Step Sequencer before/add/parameter/erase, Track Edit tuning before/changed/reopened/restored, and their accessibility hierarchies.
- Physical MPC Studio MkII verification remains **PENDING**. All results cited here are Android Emulator evidence.

## Completed in this session

### 1. Track Edit sample-layer tuning — VERIFIED

The TUNE row is below the initial viewport in the real Samples `ScrollView`; it is not a missing backend parameter. The test uses live accessibility bounds to scroll to the action, performs one real TUNE+ tap, verifies `+0.00 st → +1.00 st` against sampler-backed state, leaves/reopens the original navigation route, checks persistence, and safely restores the initial value. Build #2779 logged a successful full round-trip.

### 2. Main left Channel Strip geometry — FIXED / VERIFIED

The legacy condensed mixer panel was a duplicate weighted child re-exposed by saved Mixer visibility. It remains mounted but GONE to preserve state/refresh dependencies; the intended XL strips now fill the region. Build #2779's live hierarchy test measured 0.977 vertical and 0.960 horizontal fill, confirmed the legacy panel absent, and confirmed the visibility hit target inside the mixer.

### 3. Grid DRAW/ERASE — VERIFIED

The custom-canvas Grid exposes selected-cell accessibility from the native sequencer model. In the emulator, a real touch added Pad 01 / Step 34 at velocity 100; a real ERASE-mode touch removed it; the seeded Step 33 note stayed unchanged. Captured DRAW/ERASE screenshots and hierarchies agree with the native note projection.

### 4. Step Sequencer touch/edit/remove and header — VERIFIED

Real emulator touches added Pad 01 / Step 02, showed native-backed `VEL=100` and `PROB=127`, used the actual Function Bar PARAM and minus buttons to change probability to 126, and removed the temporary event. Step 01 remained unchanged. The app-side audit verified native cleanup and restored the parameter selector. The header screenshot/hierarchy confirms a single-line `TRACK 01 • DRUM` context and distinct PAD status, PAD−, and PAD+ controls.

### 5. Arrangement loop-brace native commit/restore — VERIFIED

Build #2776 exposed an exclusive-right-boundary issue when the calculated touch started at x=1778, exactly on the view's right edge. The gesture derivation now clamps touch coordinates inside the live canvas and uses Android density for geometry. Build #2778 then proved the native loop changed but exposed a separate test-harness bug: unquoted `adb shell run-as ... sh -c "echo ... > cache/file"` let redirection execute outside the app UID and failed with “Permission denied.” Commit [`f9d27bd418d279d5cb76f00e1f1ead7e13fb0a41`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/f9d27bd418d279d5cb76f00e1f1ead7e13fb0a41) fixed the quoting without weakening loop assertions.

On Build #2779, the actual touchscreen drag changed the native loop end from bar 4 to bar 3; the test then restored bar 4 and verified the native model, application-side acknowledgement, before/changed/restored screenshots and accessibility captures. The success marker was `Arrange loop-brace real-touch → native commit → real-touch restore round-trip passed.`

## Current execution order

1. Implement the first real Arrangement event-editing operation. Start from the current `MpcArrangeView`, native sequence/track model, and existing CUT/COPY/PASTE/DUP actions. Confirm the model can represent the operation; add domain/native tests before UI wiring. Preserve current event-to-Grid navigation and the newly verified loop workflow. Avoid adding mock event state.
2. Validate one real operation through the emulator, including native-state round-trip and safe fixture restoration; inspect screenshots/hierarchies.
3. Recheck live PR #23 HEAD/base and run the full Android workflow on the exact final SHA. Keep physical-controller validation separately marked pending.

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
