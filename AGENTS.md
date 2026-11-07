# Agent Instructions — MPC Studio MkII Groovebox

This repository has one canonical project brief:

**[`docs/PROJECT_MASTER_PROMPT.md`](docs/PROJECT_MASTER_PROMPT.md)**

> **Active execution mode:** read [`docs/FAST-DELIVERY-MODE.md`](docs/FAST-DELIVERY-MODE.md) first. It overrides older instructions below wherever they require a full test/emulator run after every slice, require fixing unrelated test-harness failures before product work, or encourage stacked feature branches. Default to one canonical working line and the fast APK lane.

Before any implementation work, every coding agent must also read **[`docs/SESSION-STATE.md`](docs/SESSION-STATE.md)** and **[`docs/DEVELOPMENT-OPERATING-PROTOCOL.md`](docs/DEVELOPMENT-OPERATING-PROTOCOL.md)**, then verify their mutable status claims against live GitHub state. SESSION-STATE is a checkpoint, not a substitute for inspecting the active branch, PR, CI or source.

## Mandatory rules

1. **Preserve `main` as the stable reference.** Use the single live canonical checkpoint branch recorded in `docs/SESSION-STATE.md` for ordinary active implementation; isolate a second branch only for genuinely independent work or a separate writer.
2. **Read before editing.** Inspect current source, callers, relevant docs and recent commits before changing an existing subsystem.
3. **Build the APK through the fast GitHub Actions lane after a coherent implementation batch.** Reserve full native/JVM/emulator validation for milestone or high-risk changes.
4. **Fix only blockers to the current product path.** Classify CI failures; do not stop unrelated product work for a flaky test harness, emulator runner or non-critical defect.
5. **Keep architecture boundaries intact:** Android platform, MIDI transport, MPC hardware adapter, MPC domain, audio engine, UI and persistence must remain separable.
6. **The application display is mandatory landscape/horizontal.** Do not introduce portrait-first or portrait-only product layouts.
7. **Protect realtime audio.** No avoidable blocking I/O, allocations or UI work in the audio callback.
8. **Do not scatter raw MPC MIDI values through application code.** Keep them in the hardware adapter/map.
9. **Physical hardware is the authority.** Reverse-engineered mappings remain unconfirmed until tested on the actual MPC Studio MkII.
10. **Do not copy proprietary Akai code, firmware resources or artwork.** Use documented behavior and original implementation/graphics.
11. **Keep changes small and traceable.** Prefer one coherent objective per branch/commit.
12. **A red full-validation run is a diagnostic, not an automatic stop.** Fix product/build defects that block the current slice or risk audio/data integrity. Log and defer unrelated harness/environment failures; never falsify results or weaken meaningful assertions.
13. **Use test levels deliberately.** Fast APK build by default; focused tests only for directly affected logic; full native/JVM/emulator validation at deliberate milestones or high-risk changes.
14. **One writer per branch.** Do not let ChatGPT/GitHub tools, Cline, or another agent concurrently mutate the same branch or same files. Parallelize only across isolated branches/worktrees with explicit file ownership; integrate sequentially.
15. **Measure visual geometry when it materially affects the current slice.** Reuse available screenshots/bounds; do not delay functional work to obtain new reference captures for minor changes. Treat firmware layout units as evidence, not phone pixels.
16. **Use the action catalog as a reference, not a gate.** Reuse a relevant existing row when readily available. Do not delay implementation to research or create a catalog entry for a small change; update status at a meaningful milestone. Never claim a UI label is a working backend.
17. **Use the controller traceability map for hardware work.** Before changing MPC Studio MkII handling, read `docs/MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md` and `docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md`. Extract only physical-controller, protocol and hardware gesture knowledge; never import MPC Desktop or DAW navigation/workflows into our app. Keep source confidence, automated-test evidence and physical-device acceptance separate.

#

## Visual-reference guidance — milestone-based, non-blocking

For substantial visual redesigns and milestone acceptance, use the closest relevant `VIS-001`–`VIS-082` context from `docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md` and inspect the associated local capture through `docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md` when readily available.

- MPC Standalone OS 3.9 remains the preferred visual authority; older/third-party references are supplementary.
- Reuse existing assets. Do not spend a development session downloading, reconstructing or cataloguing missing screenshots.
- If the exact image is absent, inaccessible or only a manual page is available, use the closest evidence, note the gap briefly and continue. **Missing visual assets are not a blocker.**
- Routine commits do not require new screenshots/XML or a visual-reference report. Capture before/after evidence at a meaningful UI milestone or during deliberate full validation.
- Reference material is documentation-only and must not be packaged into the APK.
# Project source hierarchy
18. **Use the MPC One ↔ Studio cross-device matrix.** When mapping an MPC One action to the MPC Studio MkII, consult `docs/MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md` as well as the semantic map and protocol traceability document. It defines non-identical gestures, alternate routes, reserved actions and physical-validation gaps. Never silently equate different secondary functions or invent controls the Studio does not have.



Use these documents together:

- `docs/SESSION-STATE.md` — concise, update-at-milestone state checkpoint; verify against live GitHub before relying on it.
- `docs/DEVELOPMENT-OPERATING-PROTOCOL.md` — required session loop, product-scope exclusions, acceptance evidence and context-efficiency rules.
- `docs/PROJECT_MASTER_PROMPT.md` — master operating brief.
- `docs/ARCHITECTURE.md` — layer boundaries.
- `docs/IMPLEMENTATION-ROADMAP.md` — stage sequence.
- `docs/HARDWARE-MPC-STUDIO-MK2.md` — controller protocol overview.
- `docs/MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md` — source → protocol → implementation → automated test → physical validation map; excludes host-application workflows.
- `docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md` — physical control to app semantic action contract.
- `docs/MPC-STUDIO-MKII-MPC-ONE-INTEGRATION.md` — cross-device mapping from MPC One reference actions to Studio MkII physical gestures, product routes, non-identical modifiers and validation gaps.
- `docs/MIDI-TRANSPORT.md` — Android MIDI boundary.
- `docs/MPC-DOMAIN-MODEL.md` — musical domain model.
- `docs/MPC-UX-REFERENCE.md` — standalone-style UX reference.
- `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md` — permanent MPC 3.9 UI architecture and migration contract.
- `docs/MPC3-REFERENCE-INDEX.md` — searchable MPC 3.9 research/source index.
- `docs/MPC3-USER-ACTION-CATALOG.md` — canonical action-level link between Akai manual/support sources, firmware evidence, Android implementation/status and acceptance tests.
- `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md` — protected lower-layer and UI migration safety rules.
- `docs/PHYSICAL-TEST-CHECKLIST.md` — hardware acceptance tests.
- `THIRD_PARTY_NOTICES.md` — dependency/license record.

When these documents conflict with the current code, investigate and update the documentation rather than silently ignoring the discrepancy.

For the current delivery rules, read `docs/FAST-DELIVERY-MODE.md`. For product architecture and acceptance criteria, read `docs/PROJECT_MASTER_PROMPT.md`.

## Current execution queue

Follow the current, dated work queue at the top of `docs/IMPLEMENTATION-ROADMAP.md`. Older dated blocks in that roadmap and `docs/SESSION-STATE.md` are historical context only; live PR/HEAD/Actions evidence takes precedence. Read `docs/SESSION-HISTORY-ARCHIVE.md` only when a specific regression or historical question requires it.


## Current delivery override

All agents must follow `docs/FAST-DELIVERY-MODE.md`. Do not revive historical instructions that require green full emulator CI after every small change. A fast-lane APK build is the normal iteration check; full validation is deliberate and milestone-based. Keep the working line consolidated and prioritize complete musical workflows.
