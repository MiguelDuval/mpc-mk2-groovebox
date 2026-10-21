# Implementation Roadmap

## CURRENT EXECUTION QUEUE — 2026-10-21 (live branch and exact-SHA Actions govern)

### 1. FAVORITE 1 folder picker — RETRY / LAST-GOOD-HIERARCHY FIX

- Latest completed PR #23 run at triage: Build #3035 / `39623619559`, exact SHA `fe0742cc2ceaf21b6b5589c1a07553839f7431f2`, failed at emulator smoke step 52. See https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39623619559.
- SAMP-008 and SAMP-009 native runtime assertions passed before the later Browser/SAF failure. CROP produced 2,840 frames from the 3,840-frame fixture after Start=1,000; CH4/CH8/CH16 covered all 2,840 frames with adjacent exact ranges. Browser return and Search reset also passed.
- Root failure evidence: the FAVORITE 1 folder-scroll loop made one direct bounded hierarchy dump after a real scroll; UIAutomator timed out and smoke stopped despite the DocumentsUI picker remaining active. The evidence package from artifact `12357687535` was downloaded; `mpc-groovebox-browser-favorite-tree-picker.png` was inspected.
- Focused fix: make `dump_device_ui_retry` publish via a scratch XML only after a valid complete dump, preserving the prior good hierarchy across failed attempts. Route the FAVORITE 1 scroll-loop refresh through the existing retry helper. Keep folder-row consistency/full visibility, actual touch, exact child WAV, URI grant and restart restoration checks unchanged.

### 2. Required full acceptance — NOT YET PASSED

- Pass USER FOLDER/FAVORITE selection, folder grant and persistence on one green exact-HEAD full workflow.
- Then pass real Project Save As → mutate → Open with canonical equality, including MIDI events and Pattern placement IDs/parameters; direct SAVE overwrite must change persisted domain SHA; force-stop/relaunch/Open must restore it; cancellation must preserve state; malformed input must not damage the active project; placement-free v1 must load with two tracks and zero placements.
- Keep SAMP-010 isolated until Project/Browser acceptance is green. Physical MPC Studio MkII validation remains separately pending.

## HISTORICAL EXECUTION QUEUE — 2026-10-17 (superseded by the current 2026-10-21 status above)

Older dated “next task” sections below retain history only. Start each slice from live HEAD, current source and real test gaps.

### 1. Track Edit sample-layer tuning — COMPLETE / VERIFIED
- The real Samples ScrollView places TUNE below the initial viewport. The smoke scrolls using live accessibility bounds, checks a real one-semitone TUNE+ touch against sampler state, verifies persistence after re-entry, and restores the original value.
- Reconfirmed in successful exact-SHA [Android Build #2781](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39062512475), commit [`af266a38bb850a20313bccfef9a7b953c4191151`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/af266a38bb850a20313bccfef9a7b953c4191151).

### 2. Main XL Channel Strip geometry — COMPLETE / VERIFIED
- The duplicate legacy mixer remains GONE while preserving shell visibility wiring and state dependencies. The runtime hierarchy verifies the XL strips fill the persistent context region and that the legacy panel is absent.
- Full workflow Build #2781 passed on the commit above.

### 3. Grid DRAW/ERASE — COMPLETE / VERIFIED
- Grid's custom Canvas now exposes selected-cell note state from the native sequencer projection, not duplicate UI state.
- The emulator added Pad 01 / Step 34 at velocity 100, erased that note via a real touch, and preserved the seeded Step 33 event. Build #2781 passed with both screenshots and hierarchies uploaded.
