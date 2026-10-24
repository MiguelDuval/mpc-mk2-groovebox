# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-24  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Latest verified full exact-HEAD acceptance

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`. Current verified HEAD: `985fdd3df616b0d0c0a284282527fbb15540e069`.
- **Android Build #3156 / run [40100660011](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40100660011) PASSED on exact SHA `985fdd3df616b0d0c0a284282527fbb15540e069`.** All workflow steps completed successfully, including APK assembly, full emulator smoke (step 52) and visual-evidence upload (step 53).
- APK artifact ID `12562642906`, size 10,009,933 bytes, SHA-256 `e787429c0338397ab9defa076acc5a8e9599a6dd5b4f199cc971601859a6224c`. Visual-evidence artifact ID `12563432229`, size 15,202,931 bytes, SHA-256 `34c9d6bb91561cd3a4b0c5f5da43898a8225048a41d638e82bcdc172fb8b0615`.
- Accepted smoke coverage includes Browser USER FOLDER/FAVORITE 1 and DocumentsUI restoration, Project save/open/overwrite/restart/recovery and malformed-load preservation, Arrangement placement operations, SAMP-008/009 and SAMP-010 waveform viewport/audition invariants.
- `.github/workflows/android-build.yml` retains `cancel-in-progress: false`. Physical Akai MPC Studio MkII validation remains a separate pending hardware gate.

## Current execution queue — 2026-10-25

1. PR #30 — Project SAF phases. Code head 2b9ee07a0ab45f2a526e24f7c5dfc4f9c7c2b862 passed Build #3166; docs head db0d1f64f9a7201d97423d9f67a6401c01f5aace passed Build #3174. This docs-refresh commit will need its own exact-SHA full workflow.
2. PR #57 — stacked SAF output-open classification. Head 40532b213e29dd57e235e9eeb3e8ff281d1c6fab on PR #30 docs head db0d1f64f9a7201d97423d9f67a6401c01f5aace passed Build #3175. Re-sync after the next PR #30 docs head; require full CI on the new stack SHA.
3. PR #49 / #51 — project persistence note values and timing. Exact heads 34f9e50016a720cae4f44933a0405609a4346be0 and 302f5e78e626b9e7006c26adf7ae5fba159d4848 passed Builds #3171 and #3172.
4. PR #54 — audio-trigger event boundary. Exact head 4b81293521965212a145e180237f01092ed6a7e6 passed Build #3173.
5. Input/recorder/event hardening. PR #53 77737cd95f27bbfbf41d32564cb34447470af1cd (#3177), #46 e011e0edfcf34d48eda6480157b36f4a074d79e1 (#3178), #43 a08f89b1e4061c5eba9847de3aa2ee1a33d6af0a (#3179), #50 8da23e1770b35c018e1f4617b3ab26155e45ad70 (#3180), #44 3834f697c793ab86199b9cc607ca6cd42af84013 (#3181), #42 4707d108f793e462a635f97379dcce6bfafacac9 (#3182), #41 cf74677e60144c5bc584aefd5a4f848e6d19f005 (#3183), #40 4f46254ab43d0df140e50578f658fd4565e8010b (#3184), #39 2695ce1daa6ccefb92bd5685293a68e5e2513503 (#3185), and #38 1a9ad9c6e71dc2066b342bd052ad9601e377a62d (#3186) passed exact-head workflows.
6. Note Repeat, DSP and sequence state. PR #36 268fd0f0cbc97f1178b98aea1e0f528d24aeee52 (#3187), PR #37 latest a6078eb8527c0504479385b11bbd017b38dab717 (#3193), PR #45 86795eb2d2472953fca7079a51d9b6d52c1dd915 (#3189), PR #52 682114d42e0463d86192c613be4ddaa229428623 (#3190), PR #55 0666bfa0ea721cef93ec8b54891eefeffa083087 (#3191), and PR #48 6737cea7f836650c4a7a447bc8f5d144dec49399 (#3192) passed on their exact heads. The earlier PR #37 Build #3188 failed at a late empty DocumentsUI hierarchy capture; latest head adds a bounded, fresh-tree retry and Build #3193 passed the full workflow.
7. Review/hardware gate. Do not merge open PRs without authorization. Do not touch main, rewrite history, or change cancel-in-progress: false. Physical MPC Studio MkII acceptance remains separate from emulator results.

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
