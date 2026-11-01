# Nemotron Autonomous Handoff — MPC Studio MkII Groovebox

**Product:** MPC Studio MkII Groovebox (Android), reference target Akai MPC One Gen 1 / MPC Standalone OS 3.9.
**Repository:** MiguelDuval/mpc-mk2-groovebox
**Canonical docs:** docs/PROJECT_MASTER_PROMPT.md, docs/SESSION-STATE.md, docs/DEVELOPMENT-OPERATING-PROTOCOL.md, docs/MPC3-UI-MIGRATION-MASTER-SPEC.md, docs/MPC3-UI-SCREEN-BLUEPRINT.md, docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md, docs/MPC3-USER-ACTION-CATALOG.md.

---

## 1. Live state (verified against GitHub, 2026-11-01)

| Item | Value |
|---|---|
| Active working branch | `feature/mpc-one-ui-fidelity-continue` |
| Branch created from | `checkpoint/mpc-ui-reference-and-product-20261030` (PR #133) |
| Exact HEAD | `0d53d709edaaaff67de2ac49526c76487e56e0ac` |
| Product baseline | `ac59c5e23f87680e69e311e25d2e45d7ab1b8f8d` (`feature/mpc-one-ui-fidelity`) |
| main | NOT modified (and must not be) |
| Local clone | blobless partial clone at `.../chat/mpc-pr133`; files materialized on demand via single-blob `git fetch origin <sha>` + `git checkout HEAD -- <path>` |

## 2. Open PRs (reconciled)

| PR | Branch | HEAD | State | Notes |
|---|---|---|---|---|
| #133 | `checkpoint/mpc-ui-reference-and-product-20261030` | `0d53d709...` | OPEN / DRAFT | Canonical consolidated base; contains visual-reference archive (82 VIS contexts, 402 source records, 370 downloaded / 32 failed gaps explicit), integrated Track Solo, Mute, Grid, Arrangement, Back nav, Project Save As, Browser pieces. |
| #124 | `fix/p0-mute-grid-ui-recovery-20261027` | `8635b927...` | OPEN / DRAFT | Mission handoff described Track Solo here, but SESSION-STATE.md (authoritative, Oct 30) attributes integrated Track Solo to **PR #125, SHA `06d08f38d2789a0c62cf64b23cdd8e8dd2b420cd`, Build #3468**. PR #133 already contains the complete Track Solo implementation (see section 4). Do NOT blindly merge #124 into #133; diff only unique changes if any. |

## 3. CI / acceptance evidence (last known)

| Build | Exact SHA | Run | Result |
|---|---|---:|---|
| #3608 | `dea4297a54caf60daae874adcab9b5c09480f1bc` | 41226252891 | SUCCESS — 53 steps incl. emulator smoke + evidence upload |
| #3609 | `727f8c3cd931171161e3fb5bc6a99c711cce9949` | 41226976655 | SUCCESS — 53 steps incl. emulator smoke + evidence upload |

Per the earlier mission handoff: Build #3632 (PR #133 head `9f2868c...`) FAILURE at emulator smoke step 55; Build #3639 (PR #124 head `8635b927...`, run 41318551480) PENDING. These may be stale relative to SESSION-STATE.md; refetch live before acceptance claims.

APK artifact SHA-256 examples (#3608): `5e67867de8a00523a1610de99abff32a406f2e497d39752c81174e2a5ab831a5`; (#3609): `10e8d34f73a2232b2360587d959927d304b12514a4be741b158abce47d019544`.
Visual evidence archives (#3608/#3609): 227 entries each (106 PNG + 116 XML). Covers Mute, Solo OFF/ON, Grid H/V zoom, Arrangement pan/zoom, Browser, Sample Chop CH4/8/16, Track Edit gain/pan/tune/velocity.

## 4. Track Solo — already integrated on PR #133 (verified by reading source)

Files read at `0d53d709`:
- `src/MPC/Sequencer/MpcSequenceSoloPolicy.h` — `shouldRouteTrackForSolo(muted, soloed, anySoloed) = !muted && (!anySoloed || soloed)` (Mute wins on same track; multiple Solo tracks work).
- `src/MPC/Sequencer/MpcSequencerPlayback.h` — takes `const bool* trackSoloed / anyTrackSoloed`; gates routing in `advance()`.
- `src/MPC/Sequencer/MpcSequencePlaybackSession.h` — `advance()` recomputes `anySoloedPlayableTrack_ = hasSoloedPlaybackTrack(...)` **every audio tick** from live `track->soloed` pointers; `stop()` resets but domain `track.soloed` persists across transport stop/restart.
- `src/NativeEngine.cpp` — `nativeSequenceToggleTrackSolo` (~line 4188): `track.soloed = !track.soloed` guarded by `canSoloTrackPlayback` (Drum only, active program); `nativeSequenceIsTrackSoloed` reads `tracks[trackIndex].soloed` live; `nativeSequenceCanSoloTrackPlayback` (~line 3217).

Conclusion: Solo is dynamic during playback, no stale cache, Mute takes precedence, works across sequence/transport restart. Tests: `tests/MpcSequencerPlaybackSoloTests.cpp` (5 scenarios) per mission handoff. **No solo integration work is needed on #133.**

## 5. Environment limitation (honest)

- Local build is NOT possible in this environment: no `gradlew` in repo, four git submodules (JUCE, tracktion_engine, oboe, ableton_link) not checked out, `ANDROID_HOME` unset, NDK unconfirmed, no Gradle in PATH.
- GitHub Composio toolkit is blocked for this session; plain `git`/`fetch_web_content`/`raw.githubusercontent.com` work.
- Physical MPC Studio MkII validation: **NOT RUN** (per SESSION-STATE.md).
- Code changes below were NOT built or run locally; they require CI/exact-SHA build verification before acceptance claims.

## 6. Changes made in this session

None yet (this handoff is the first committed slice on the new branch).

## 7. Known defects / gaps

- 32 VIS image retrieval gaps remain explicit in the catalog (do not treat as implemented).
- UI completeness matrix labels are "REVERIFY" — must be re-evaluated against current source (see docs/MPC-GROOVEBOX-CONFIGURATION-CATALOG.md).
- Physical MkII hardware acceptance NOT RUN.
- Local build/test unavailable (see section 5).

## 8. Next concrete task

Inspect the current shell implementation (`MainActivity`, `MpcShell`, Toolbar, ShortcutRail, XL Channel Strip, Workspace, Function Bar) against `docs/MPC3-UI-SCREEN-BLUEPRINT.md` and the relevant VIS contexts; implement the highest-impact P0 visual-fidelity slice (canonical shell geometry / Main Mode / Function Bar contextual behavior), record VIS IDs + before/after evidence, then commit and continue.

---

**Guidance for next session:** read `docs/SESSION-STATE.md` and `docs/DEVELOPMENT-OPERATING-PROTOCOL.md`; verify live GitHub refs/Actions before relying on any SHA; keep one writer on this branch; do not merge PR #124/#133 automatically or touch `main`.
