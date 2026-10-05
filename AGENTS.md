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
6. **The application display is mandatory landscape/horizontal.** Do not introduce portrait-first or portrait-only product layouts.
7. **Protect realtime audio.** No avoidable blocking I/O, allocations or UI work in the audio callback.
8. **Do not scatter raw MPC MIDI values through application code.** Keep them in the hardware adapter/map.
9. **Physical hardware is the authority.** Reverse-engineered mappings remain unconfirmed until tested on the actual MPC Studio MkII.
10. **Do not copy proprietary Akai code, firmware resources or artwork.** Use documented behavior and original implementation/graphics.
11. **Keep changes small and traceable.** Prefer one coherent objective per branch/commit.

## Project source hierarchy

Use these documents together:

- `docs/PROJECT_MASTER_PROMPT.md` — master operating brief.
- `docs/ARCHITECTURE.md` — layer boundaries.
- `docs/IMPLEMENTATION-ROADMAP.md` — stage sequence.
- `docs/HARDWARE-MPC-STUDIO-MK2.md` — controller protocol research.
- `docs/MIDI-TRANSPORT.md` — Android MIDI boundary.
- `docs/MPC-DOMAIN-MODEL.md` — musical domain model.
- `docs/MPC-UX-REFERENCE.md` — standalone-style UX reference.
- `docs/MPC3-UI-MIGRATION-MASTER-SPEC.md` — permanent MPC 3.9 UI architecture and migration contract.
- `docs/MPC3-REFERENCE-INDEX.md` — searchable MPC 3.9 research/source index.
- `docs/UI-MIGRATION-SAFE-CHANGE-CONTRACT.md` — protected lower-layer and UI migration safety rules.
- `docs/PHYSICAL-TEST-CHECKLIST.md` — hardware acceptance tests.
- `docs/INTERNAL-DSP-LIBRARY-RESEARCH.md` — internal synth/FX engine and commercial-license triage.
- `docs/INTERNAL-DSP-LIBRARY-ARCHITECTURE.md` — internal audio-node and third-party adapter boundary.
- `docs/INTERNAL-PLUGIN-UX-CONTRACT.md` — product/UI/hardware contract for built-in instruments and effects.
- `docs/ADR-0010-INTERNAL-DSP-AND-COMMERCIAL-LICENSING.md` — accepted internal-DSP licensing architecture decision.
- `docs/DSP-INTEGRATION-GATE.md` — mandatory gate for integrating any DSP engine.
- `docs/THIRD-PARTY-SBOM-POLICY.md` — release/provenance policy.
- `docs/THIRD-PARTY-SBOM.md` — current dependency and candidate inventory.
- `THIRD_PARTY_NOTICES.md` — dependency/license record.

When these documents conflict with the current code, investigate and update the documentation rather than silently ignoring the discrepancy.

For the full rules, architecture, roadmap and acceptance criteria, read `docs/PROJECT_MASTER_PROMPT.md`.
