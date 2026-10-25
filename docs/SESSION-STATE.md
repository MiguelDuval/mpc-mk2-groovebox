# Session State — MPC MkII Groovebox

**Checkpoint date:** 2026-10-25  
**Purpose:** live handoff; exact branch HEAD and exact-SHA Actions evidence govern.

## Current live checkpoint — 2026-10-25

### Primary product branch

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) remains OPEN: `feature/mpc-one-ui-fidelity` → `fix/ui-shell-format-width-startup`. Live head is `30e270c51038afdd037e9d226f20eb5cc310bc82`; base SHA is `5f562e54e2cf8235041590933232a3836d74a16a`.
- **Android Build #3215 / run [40141743745](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40141743745) passed on that exact SHA.** The job succeeded, including emulator smoke and visual-evidence upload. APK artifact `12579022465`; evidence artifact `12579887000`. Keep physical MPC Studio MkII acceptance separate.
- `.github/workflows/android-build.yml` still has `cancel-in-progress: false`.

### PR #43 — second-document Save As picker failure

- PR [#43](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/43), head `67583ed41f9d86db6a8244a666b3a8322742de31`, failed Android Build #3242 / [run 40145378936](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40145378936), job `121031806244`, step 52. The log shows DocumentsUI in the foreground and a `package="com.android.documentsui"` tree, but UIAutomator consumed the stable-capture helper's entire 8-second budget and the hierarchy transfer could not start. The helper reported only one attempt; no product Save As assertion had failed.
- The comparison is exact: PR [#50](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/50), head `2390f23670c3679a6a33c348aeb589440ff6765b`, passed Build #3244 / [run 40145568578](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40145568578), including all 57 workflow steps and evidence upload. The smoke script blob on both heads was identical: `f2406e89993944696b86a5533b3470725d3e6ecb`. PR #50 changed only recorder code/tests, not the picker harness. This points to a transient UIAutomator/emulator capture stall combined with a retry-budget flaw, not a demonstrated application defect.
- PR [#58](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/58), HEAD `63ea6805e819537a96eed446bdde1b3aa518531d`, is an open draft based on PR #43. Build #3254 / [run 40177584543](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177584543) passed all 57 steps, including emulator smoke and evidence upload.
- PR [#59](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/59), HEAD `819128356f7e097907c5cc4cbf881f040b093f24`, preserves three immutable matched DocumentsUI screenshot/XML pairs for initial Save As, second-document Save As and recovery export. Build #3264 / [run 40178922087](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178922087) passed all 57 steps. I inspected the artifact itself: each of the three preserved XML files identifies `com.android.documentsui` and contains the expected filename, and each corresponding screenshot shows the Android document picker at the expected save point. The earlier #3255/#3258 failures were preflight guard issues; the corrected exact-head run passes.
- PR [#60](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/60), HEAD `596a8110565a99316ccb0d5aa2a04d1e0bba5c56`, rejects empty/duplicate Pattern IDs when a Track has Arrangement placements. Build #3253 / [run 40177392571](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177392571) passed all 57 steps.
- PR [#61](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/61), HEAD `0fdba37e521e1b39a96e3c95ee9d0078a43e9883`, validates persisted BPM, time signature, sequence length/loop, quantize grid, swing and assigned locator positions. Build #3256 / [run 40177684480](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40177684480) passed all 57 steps.
- PR [#62](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/62), HEAD `26579254409aa1a5a5d0f0f8cac9bda5cd368832`, validates assigned SampleRegions, known sample-frame bounds, and ordered 7-bit SampleLayer velocity ranges plus gain/pan/tuning bounds. Build #3262 / [run 40178612521](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178612521) passed all 57 steps.
- PR [#63](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/63), HEAD `04b4ab82635d49ca104a10a036702cf99c496c5c`, adds shared Pad tuning/level/pan/envelope/filter bounds to persistence. Build #3263 / [run 40178770260](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40178770260) passed all 57 steps.
- PR [#64](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/64), HEAD `85dd29b49b8a91fd9c033c4b839857778ad4674b`, rejects negative/non-finite sample rates and negative sample lengths on serialization and decode. Build #3265 / [run 40185698676](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40185698676) has passed focused Android test step 14 and was at APK assembly step 49; exact-head smoke and evidence are still pending.
- PR [#65](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/65), HEAD `2f05794ad449339cff8c1077b5aea9658a861db9`, validates assigned SampleLayer loop metadata: non-negative ordered ranges, known sample-length bounds, and compatibility for disabled `[0,0)` loops. Build #3266 / [run 40185895495](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40185895495) has started toolchain setup; tests and all later acceptance steps remain pending.
- All PRs remain drafts/open; none has been merged or retargeted. Acceptance still requires tests, APK assembly/upload, emulator smoke step 52 and visual-evidence upload step 53 on each relevant exact HEAD. `.github/workflows/android-build.yml` retains `cancel-in-progress: false`; physical MPC Studio MkII validation remains a separate hardware gate.

### Immediate queue

1. Inspect current run #3265 for full completion of the new SampleRef metadata validator and then verify #3266's sample-loop tests.
2. Keep monitoring #3253/#3254/#3256/#3257/#3259/#3262/#3263 exact-head outcomes; these are now green at the latest polled SHA.
3. Keep PR #58–#65 open as drafts until exact-head CI and review justify acceptance; do not merge or retarget the stack automatically.

## Previous verified full exact-HEAD checkpoint — PR #23 (historical)

- PR [#23](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/23) is OPEN on `feature/mpc-one-ui-fidelity`, targeting `fix/ui-shell-format-width-startup`. Verified product-code HEAD: `f067a252904a242b284cf8b077570c2e04225fc8`; target base SHA at verification: `5f562e54e2cf8235041590933232a3836d74a16a`.
- **Android Build #3150 / run [40098511757](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40098511757) PASSED on exact SHA `f067a252904a242b284cf8b077570c2e04225fc8`.** All 57 steps succeeded, including APK assembly, full emulator smoke (step 52) and visual-evidence upload (step 53).
- APK artifact ID `12561652251`, size 10,009,933 bytes, SHA-256 `7ad1a55431daeb926f14dd92cdbeb8e972d2a013efab98d9a24efb2fa49fab44`. Visual-evidence artifact ID `12561778921`, size 15,208,813 bytes, SHA-256 `bb678d8cbe0fc3e07f75ac904f1dbcc3bdb551397d170d2163fd3cac1493ad45`. The evidence archive was inspected: 87 PNGs, 97 XML hierarchies and 3 text/log files; Browser, Project, crop/chop and zoom/audition milestones are present.
- SAMP-008 passed real-touch CROP: 3,840 → 2,840 PCM frames after Start=1,000; native region reset to `0..2840`. SAMP-009 passed CH4/CH8/CH16 with exact contiguous, non-overlapping native slices covering all 2,840 frames. SAMP-010 passed real-touch ZOOM+/ZOOM−/RESET (viewport 10–90% then 0–100%), native Pad 1 audition at velocity 112, and sample-frame/region invariance.
- Browser USER FOLDER and FAVORITE 1 passed real DocumentsUI folder selection, exact `Browser-Favorite-Only.wav` result/indexing, isolated URI/preference state and process-restart restoration.
- Project acceptance passed Save As → mutate → Open with exact native state, including non-default MIDI events and Pattern placement identity/timing/source offsets; direct SAVE overwrite changed the serialized SHA; process restart/Open restored the saved state; cancelled Save As/Open preserved state; malformed load preserved the active Project; placement-free v1 loaded to the canonical domain; two-document A/B isolation, New Project and recovery-export round trips passed.
- Project fixture SHA before mutation: `4976f1c933b9cc933aa5c62d37c9cebb194970ba9451577456e44c17e539e5b9`; direct overwrite SHA after mutation: `0c774ef1fa5e1566fe67f45bba58502dba3e1472a71555a6b10d1696b8185c89`.
- `.github/workflows/android-build.yml` retains `cancel-in-progress: false`. Physical Akai MPC Studio MkII validation remains a separate PENDING hardware gate. This acceptance evidence is for product-code SHA `f067a252904a242b284cf8b077570c2e04225fc8`; a later docs-only HEAD still needs its own exact-SHA workflow result.

## Previous execution queue — 2026-10-24 (superseded by the current checkpoint above)

1. **Project provider failure phases — PR #30.** Open, non-draft, head `eeef2a864126bca34e08410b063a9f1f3ec36287`; Android Build #3129 / run [40083641707](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40083641707) passed on that exact SHA. APK artifact `12556590889`; visual-evidence artifact `12555998800`. The PR metadata still reports base SHA `2c00c1603ea7228e4eacd384a4e73b6ac0fedb69`, while the live primary branch is now `f067a252904a242b284cf8b077570c2e04225fc8`; refresh the live diff/base before any integration. Do not merge or retarget automatically.
2. **Output-open versus stream-write classification — PR #57**, stacked on `fix/project-provider-failure-phases`, head `2dd936c3d9d26ace0d198f6cefabe4387d712318`. Android Build #3155 / run [40100283734](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40100283734) was IN PROGRESS at focused Android unit policy tests (step 14) at the last check. It is not accepted until the exact-SHA workflow, full emulator smoke and evidence upload finish green.
3. **Browser Show file size — PR #56.** Build #3154 / run [40099462547](https://github.com/MiguelDuval/mpc-mk2-groovebox/actions/runs/40099462547) failed at emulator smoke step 52 on SHA `d6a06ba8186d78eeaa03761e429c6505bc40fb85`: the option audit passed, then the storage-picker action had zero measured bounds. A review comment records the likely post-toggle layout race and asks for a next-pre-draw/measurement continuation, without weakening the bounds assertion or increasing timeouts. Do not create duplicate smoke tests or write on that branch concurrently.
4. Keep physical-controller acceptance separate. Never modify `main`, rewrite history, or merge PRs without explicit authorization.
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
