# Agent Instructions — MPC Studio MkII Groovebox

This repository has one canonical project brief:

**[`docs/PROJECT_MASTER_PROMPT.md`](docs/PROJECT_MASTER_PROMPT.md)**

Every coding agent working on this repository must read that document before making changes.

## Mandatory rules

1. **Preserve `main` as the stable reference.** Experimental implementation belongs on feature branches.
2. **Read before editing.** Inspect current source, callers, relevant docs and recent commits before changing an existing subsystem.
3. **Build after meaningful changes.** Check the real GitHub Actions result; do not claim success from source inspection alone.
4. **Fix failures before unrelated features.**
5. **Keep architecture boundaries intact:** Android platform, MIDI transport, MPC hardware adapter, MPC domain, audio engine, UI and persistence must remain separable.
6. **Protect realtime audio.** No avoidable blocking I/O, allocations or UI work in the audio callback.
7. **Do not scatter raw MPC MIDI values through application code.** Keep them in the hardware adapter/map.
8. **Physical hardware is the authority.** Reverse-engineered mappings remain unconfirmed until tested on the actual MPC Studio MkII.
9. **Do not copy proprietary Akai code, firmware resources or artwork.** Use documented behavior and original implementation/graphics.
10. **Keep changes small and traceable.** Prefer one coherent objective per branch/commit.
11. **Treat firmware reverse-engineering reports as evidence with a confidence level, not as unquestioned truth.** Resolve the input image identity and validate binary claims before using them to drive application changes.

## Current documentation entry points

- `docs/PROJECT_MASTER_PROMPT.md` — canonical product and engineering brief.
- `docs/SESSION-STATE.md` — concise continuation state; verify live GitHub state before acting.
- `docs/FIRMWARE-RESEARCH.md` — firmware handoff findings, relevance, inconsistencies and next validation steps.
- `CHATGPT_HANDOFF/` — original Cline-generated reports; treat them as preliminary evidence, not independently verified facts.
- `THIRD_PARTY_NOTICES.md` — dependency/license record, if present in the active branch.

## Documentation integrity warning

The previous version of this file referenced subsystem documents such as `docs/ARCHITECTURE.md`, `docs/IMPLEMENTATION-ROADMAP.md`, `docs/HARDWARE-MPC-STUDIO-MK2.md`, `docs/MIDI-TRANSPORT.md`, `docs/MPC-DOMAIN-MODEL.md`, `docs/MPC-UX-REFERENCE.md` and `docs/PHYSICAL-TEST-CHECKLIST.md`. Those files were **not present in the `main` branch's `docs/` directory when this handoff was reviewed**. Do not assume those paths exist: check the current branch/tree first. Restore or recreate missing subsystem documentation as a separate, evidence-based task rather than silently treating unavailable files as read.

When documentation conflicts with current source code, tests, successful CI or physical hardware evidence, investigate and record the discrepancy instead of guessing.

For the full product vision, architecture constraints, roadmap principles and acceptance criteria, read `docs/PROJECT_MASTER_PROMPT.md`.
