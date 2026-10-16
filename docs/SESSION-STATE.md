# Session State — MPC Studio MkII Groovebox

**Updated:** 2026-10-16  
**Purpose:** Short continuation note. Verify GitHub state again before any implementation work.

## Current repository state

- Repository: `MiguelDuval/mpc-mk2-groovebox`
- Stable reference branch: `main`
- Documentation-only work is in PR [#24](https://github.com/MiguelDuval/mpc-mk2-groovebox/pull/24), branch `docs/firmware-research-handoff`.
- PR #24 adds `docs/FIRMWARE-RESEARCH.md` and links it from `AGENTS.md`.
- The PR is open and unmerged. `main` was not directly modified.
- No application code was changed and no Android build was run for this documentation-only task.
- Last observed `main` SHA before the documentation branch was created: `9243f999b2438984131747327a352e54ff947d37`. Re-check before relying on it.

## Firmware handoff status

The uploaded reports are under `CHATGPT_HANDOFF/`. Start with:

1. `CHATGPT_HANDOFF/README.md`
2. `CHATGPT_HANDOFF/technical_report.md`
3. `CHATGPT_HANDOFF/filesystem_map.txt`
4. `CHATGPT_HANDOFF/UNRESOLVED.md`
5. `docs/FIRMWARE-RESEARCH.md` — interpreted findings, relevance, integrity warnings, and prioritized next steps.

The report claims an AKAI update header, two ext4 filesystem images, a U-Boot FIT/kernel/initramfs, RK3588/RK3588S device-tree variants, and early-boot verification/migration tools. These remain **handoff-reported, not independently verified against the raw binary artifacts**.

### Critical unresolved integrity issue

- Cline reports the source image as 366,327,168 bytes.
- An earlier Colab audit in conversation history recorded the same filename at 89,128,960 bytes with a different SHA-256.
- The Cline report's partition map ends at offset `0x0551AF64` (89,239,396 bytes), leaving most of the claimed 366 MB image unaccounted for.
- The source-image SHA-256 printed in `technical_report.md` looks patterned and has not been independently recomputed.
- The inventory misclassifies some file types and skips hashes for large files.

Do not combine the two image audits or use the reported full-image checksum as trusted evidence until the exact input file is identified and hashed locally.

## Is the firmware research complete?

**No.** The handoff is useful for preliminary boot/container orientation, but the main root filesystem and boot/recovery filesystem were reportedly not mounted and explored. It does not yet identify the actual MPC application, UI framework, audio/MIDI stack, sample/project formats, or application resources.

## Next concrete steps

1. Reconcile the exact firmware image path, byte size and freshly computed SHA-256 against the earlier Colab input.
2. Parse the header and partition table from that exact file and explain all offsets/regions.
3. Validate extraction and hashes; then inspect `rootfs.ext4` and `boot_recovery.ext4` read-only in Linux or with suitable ext4 tools.
4. Identify the MPC application executable, UI framework, audio/MIDI/USB components and project/sample formats.
5. Parse and compare DTBs and verify FIT signatures only after input identity and extracted components are validated.
6. Turn confirmed findings into small, source-referenced implementation decisions. Do not copy proprietary Akai code, firmware resources, or artwork.

## Operating constraints

- Keep `main` as the stable reference; use feature branches and small commits.
- Before further changes, inspect the live branch/PR/CI state and read `AGENTS.md`, `docs/PROJECT_MASTER_PROMPT.md`, and relevant subsystem docs.
- Do not claim firmware findings, builds, or physical hardware behavior are verified without evidence.
- Keep the user's goal in focus: advance useful musical workflows (Main, Browser, Sampler, Grid/Step Sequencer, Mixer, MIDI controller path) rather than extending audits indefinitely.
