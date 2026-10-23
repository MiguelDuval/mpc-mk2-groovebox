# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-23  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified full exact-HEAD acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`. Verified pre-change HEAD: `549c78a709097a261277b090aa27672dab590c54`.
- **Android Build #3097 / run [39931641764](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39931641764) passed on that exact SHA.** All 57 steps succeeded, including focused JVM/native checks, APK assembly, emulator smoke step 52 and visual-evidence upload step 53.
- APK artifact ID `12492765076`, SHA-256 `5cc0a7e4b06356a6f9a03457c4937c645a57f9719cee6f642572305710b7eaa8`. Visual evidence artifact ID `12492653431`, SHA-256 `7c8ea85eac7d884a252b54baf9ced830e69eaf15d46a715f24ed96764cc18a30`.
- SAMP-008 passed real-touch CROP: 3,840 → 2,840 PCM frames after Start=1,000; native region reset to `0..2840`. SAMP-009 passed CH4/CH8/CH16 exact contiguous, non-overlapping native ranges across all 2,840 frames.
- Browser USER FOLDER and FAVORITE 1 real DocumentsUI assignment, result/filter checks, separate URI/preference state and force-stop/relaunch restoration passed. Project acceptance passed Save As → mutate → Open, direct SAVE overwrite with changed serialized SHA, process restart/reopen, two-document A/B isolation, cancelled Save As/Open state invariance, malformed-load preservation and placement-free v1 canonical-state equality.
- Project fixture SHA before mutation: `4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9`; direct overwrite SHA after adding Track/Pattern/placement: `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`.
- `.github/workflows/android-build.yml` retains `cancel-in-progress: false`. Physical Akai MPC Studio MkII acceptance remains a separate PENDING gate.

## Current candidate — SAMP-010 waveform zoom/audition

- The primary Browser/Project gate is green on SHA `549c78a709097a261277b090aa27672dab590c54`; only after verifying that result, the isolated SAMP-010 slice is being ported into the existing primary smoke rather than added as a duplicate workflow.
- Candidate changes add accessible `VIEW start–end%` state independent of sample Start/End; viewport callbacks for button/pinch/reset; shared finite/bounded zoom math with focused JVM tests; stable accessible controls; real-touch ZOOM+/ZOOM−/RESET plus native Pad-trigger marker and native frame/region invariance checks. Five unique PNG milestones and matching XML evidence are declared for upload.
- The exact-SHA workflow for this candidate has not run yet. SAMP-010 is not accepted until the full Android smoke and evidence upload pass on the resulting SHA.
- Isolated PRs #25/#26/#29 remain unmerged. Only the SAMP-010-specific code/test slice is being ported; unrelated changes from those branches are excluded.

## Current execution queue

1. Verify the new commit is the exact PR #23 HEAD and inspect its Actions run. Require the waveform policy suite, all native/JVM checks, full emulator smoke (including SAMP-008/009, Browser and Project lifecycle) and visual evidence upload.
2. If the run fails, inspect the exact failed step and fresh PNG/XML/log evidence; make only the smallest root-cause fix and do not add commits while the full run is active unless a confirmed critical defect requires it.
3. After SAMP-010 is accepted, inspect the existing provider-failure work on `fix/project-provider-failure-phases` (Build #3109 / run `39965373487` was active at the last live check on SHA `cee0c8b5eef8e09c7197b22274f3c2dcd779e833`); do not duplicate that branch/work.
4. Keep physical MPC Studio MkII validation separate from emulator acceptance.

## Completed in this session

### 1. Track Edit sample-layer tuning — VERIFIED

The TUNE row is below the initial viewport inside the real Samples `ScrollView`. Live-bound scrolling exposes TUNE+, the test performs one real touch, verifies `+0.00 st → +1.00 st` against authoritative sampler state, exits/re-enters the actual route, confirms persistence, then restores the original value.

### 2. Main left Channel Strip geometry — FIXED / VERIFIED

The old condensed mixer was a duplicate weighted child re-exposed by saved Mixer visibility. It remains mounted but GONE to preserve refresh/state dependencies and shell visibility wiring. Live hierarchy checks confirm the XL strips fill the Main left region with the legacy panel absent.

### 3. Grid DRAW/ERASE — VERIFIED

A real emulator touch adds Pad 01 / Step 34 at velocity 100, ERASE removes the temporary note, and the seeded Step 33 note remains intact. The selected-cell accessibility projection is read from native sequencer state.

### 4. Step Sequencer touch/edit/remove — VERIFIED

Real emulator touches add Pad 01 / Step 02, adjust probability through the actual PARAM and decrement controls from 127 to 126, then erase the temporary event. Step 01 is unchanged; native cleanup and the parameter-selector restore pass.

### 5. Arrangement loop-brace touch/restore — VERIFIED

The real external drag changed the loop end from bar 4 to bar 3, and a second real drag restored bar 4. Native loop state and the application-side release handshake passed.

### 6. Arrangement event MOVE ± GRID — IMPLEMENTED / VERIFIED

The Canvas event projection now carries each MIDI note number from the native model. A single tap identifies the event; EVENT − / EVENT + move it one current quantize-grid interval earlier/later via a native Pattern mutation. The native operation rejects invalid/out-of-range/colliding destinations, preserves duration/velocity/probability/ratchet/nudge, and records undo/redo in the existing edit history. The UI reloads and verifies the native event projection after mutation.

The full emulator smoke used a real canvas touch to select Track 01 / Note 036 at tick 7680, tapped EVENT + to reach tick 7920, then EVENT − to restore tick 7680. Before/moved/restored screenshots and matching accessibility hierarchies were inspected. The native Pattern operation tests and Arrangement projection JUnit tests passed as part of Build #2781.

### 7. Arrangement DUP / COPY / PASTE / CUT + canonical Function Bar — VERIFIED

DUP/COPY/PASTE/CUT are backed by native Pattern operations and native edit history, surfaced from the canonical bottom Function Bar. Event-only actions require a real event selection; PASTE additionally requires valid native clipboard state. UNDO/REDO and EVENT ± GRID remain in the secondary action row.

Build #2791 performed real touches and verified DUP → UNDO → REDO → UNDO, COPY → PASTE → UNDO → REDO → UNDO, and CUT → UNDO → REDO → UNDO against exact native Pattern state. Its full workflow passed all 57 steps, and its APK/evidence artifacts were inspected.

### 8. Arrangement empty-timeline paste cursor — IMPLEMENTED / VERIFIED

Build #2793 verified an actual empty-timeline touch at tick 240. The snapped cursor has its own state (not a fabricated event selection), shows a cyan marker and accessible Track/tick label, leaves the native Pattern unchanged, and enables PASTE only while a valid cursor and native clipboard coexist. After PASTE, note duration, pitch, velocity, probability, ratchet and nudge are preserved; UNDO restores the cursor and prior CUT Pattern, REDO recreates the event, and final UNDO CUT restores the exact fixture.

Verified by [Android Build #2793](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/39075574304) on code SHA [`f49748de9db450ffc3b3b781befac99916f2a134`](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/f49748de9db450ffc3b3b781befac99916f2a134). All 57 workflow steps passed. The exact APK and visual-evidence archives were downloaded and integrity-checked; cursor/pasted/final-restored screenshots and accessibility XML were inspected.

Historical note superseded on 2026-10-18: explicit Pattern placements and their place/move/split/delete/duplicate operations are now implemented and emulator-verified. The open gap is project file save/reload through the Android user workflow.

### 9. MPC Studio MkII protocol/support/tutorial traceability — DOCUMENTATION COMPLETE / VIDEO TRANSCRIPTS NOT FULLY REVIEWED

Created [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). It documents the full pad-note map, 39 button/input mappings, Jog Wheel/Touch Strip input, button LED CC semantics, RGB pad SysEx, the six-chunk 160×80 LCD transport, corresponding source files/tests, and a physical validation queue. It links the primary community reverse-engineering repository, its Go/JavaScript LCD proof-of-concepts, official Akai FAQ and troubleshooting guide, the manufacturer-authored Quickstart mirror, and the official Akai-linked Academy/Masterclass playlists. Only hardware-level facts are retained; MPC Desktop, plug-in, licensing and DAW workflows are explicitly excluded.

The official playlists could not be fully transcript-audited through the available source view, so the document labels them indexed but not fully reviewed. It does not invent video findings. Hardware doc/semantic-map status summaries were reconciled; agent instructions and the operating protocol now require traceability before controller work. No app/runtime code was changed in this documentation slice. The current exact-HEAD workflow result still requires a live check.

## Superseded execution note — 2026-10-21

The previous FAVORITE 1 / Project persistence queue is historical. Build #3073 superseded that pending state on exact SHA `e8ba129b7773decfd26d9c0d6f16521fca7d4e9d`: the full Android workflow passed, including Project persistence acceptance. Use the current queue at the top of this file for next steps.

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
