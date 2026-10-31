# Agent Instructions — MPC Studio MkII Groovebox

This repository has one canonical project brief:

**[`docs/PROJECT_MASTER_PROMPT.md`](docs/PROJECT_MASTER_PROMPT.md)**

Before any implementation work, every coding agent must also read **[`docs/SESSION-STATE.md`](docs/SESSION-STATE.md)** and **[`docs/DEVELOPMENT-OPERATING-PROTOCOL.md`](docs/DEVELOPMENT-OPERATING-PROTOCOL.md)**, then verify their mutable status claims against live GitHub state. SESSION-STATE is a checkpoint, not a substitute for inspecting the active branch, PR, CI or source.

## Mandatory rules

1. **Preserve `main` as the stable reference.** Experimental implementation belongs on feature branches.
2. **Read before editing.** Inspect current source, callers, relevant docs and recent commits before changing an existing subsystem.
3. **Build after meaningful changes.** Check the real GitHub Actions result; do not claim success from source inspection alone.
4. **Fix failures before unrelated features.**
5. **Keep architecture boundaries intact:** Android platform, MIDI transport, MPC hardware adapter, MPC domain, audio engine, UI and persistence must remain separable.
6. **The application display is mandatory landscape/horizontal.** Do not introduce portrait-first or portrait-only product layouts.
7. **Protect realtime audio.** No avoidable blocking I/O, allocations or UI work in the audio callback.
8. **Do not scatter raw MPC MIDI values through application code.** Keep them in the hardware adapter/map.
9. **Physical hardware is the authority.** Reverse-engineered mappings remain unconfirmed until tested on the actual MPC Studio MkII.
10. **Do not copy proprietary Akai code, firmware resources or artwork.** Use documented behavior and original implementation/graphics.
11. **Keep changes small and traceable.** Prefer one coherent objective per branch/commit.
12. **Resolve the current blocker first.** If the current feature branch has a failing latest relevant CI run, investigate and fix that failure before unrelated feature work. Never hide or weaken a meaningful acceptance assertion to obtain a green build.
13. **Use the cheapest sufficient test first.** Run focused tests during iteration; run full emulator/CI acceptance at a meaningful slice boundary and verify it against the exact resulting HEAD.
14. **One writer per branch.** Do not let ChatGPT/GitHub tools, Cline, or another agent concurrently mutate the same branch or same files. Parallelize only across isolated branches/worktrees with explicit file ownership; integrate sequentially.
15. **Measure visual geometry before changing it.** Use a fresh landscape screenshot and accessibility/UI bounds. Treat firmware layout units as evidence to interpret, not as phone pixel coordinates; do not guess the root cause.
16. **Use the action catalog.** Before changing a user-visible MPC interaction, locate its ID in `docs/MPC3-USER-ACTION-CATALOG.md`; verify the official source, backend route and exact-SHA acceptance evidence. Add/update the row when behavior or status changes. Do not implement from a label or stale matrix alone.
17. **Use the controller traceability map for hardware work.** Before changing MPC Studio MkII handling, read `docs/MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md` and `docs/MPC-STUDIO-MKII-SEMANTIC-MAP.md`. Extract only physical-controller, protocol and hardware gesture knowledge; never import MPC Desktop or DAW navigation/workflows into our app. Keep source confidence, automated-test evidence and physical-device acceptance separate.

#

## Mandatory visual-reference gate — applies to every agent and every UI change

Before implementing, changing, reviewing, or accepting any user-visible MPC-style screen, workspace, dialog, shortcut, control, navigation route, or layout, the agent **must inspect the actual local visual references** in both:

- `docs/MPC3-UI-VISUAL-REFERENCE-CATALOG.md` — select and name the relevant `VIS-001`–`VIS-082` context(s);
- `docs/MPC3-UI-REFERENCE-ASSETS/INDEX.md` and the linked local image/page capture(s) — open and visually inspect the image itself, not only its URL, filename, caption, or extracted text.

Use the official MPC Standalone OS 3.9 manual/reference as the target authority. Older v2.x or third-party images are supplementary only and must not override v3.9 geometry, labels, navigation, or behavior. Compare the current implementation and the proposed result against the same reference. In the PR/checkpoint, record the VIS ID(s), local asset path/source, and before/after screenshot or hierarchy evidence. If no exact screen screenshot exists, name the closest available manual page/reference and explicitly record the evidence gap; never call a text-only manual page a screenshot of the UI. If the asset cannot be opened, stop and report the blocker rather than silently skipping the reference check. These research assets stay in documentation and must not be packaged into the APK.

This gate is mandatory even for agents working from pre-existing plans, cached context, old screenshots, or a branch whose previous CI passed. Passing automated tests does not waive visual-reference review.

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

For the full rules, architecture, roadmap and acceptance criteria, read `docs/PROJECT_MASTER_PROMPT.md`.

## Current execution queue

Follow the current, dated work queue at the top of `docs/IMPLEMENTATION-ROADMAP.md`. Older dated blocks in that roadmap and `docs/SESSION-STATE.md` are historical context only; live PR/HEAD/Actions evidence takes precedence. Read `docs/SESSION-HISTORY-ARCHIVE.md` only when a specific regression or historical question requires it.
