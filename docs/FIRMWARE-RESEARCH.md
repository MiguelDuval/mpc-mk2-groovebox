# MPC Firmware Research — Evidence, Relevance, and Next Steps

**Status date:** 2026-10-16  
**Firmware label reported by the handoff:** `MPC-3.9.1-Gen2-update.img`  
**Research package:** [`CHATGPT_HANDOFF/`](../CHATGPT_HANDOFF/)  
**Purpose:** Preserve useful firmware findings for engineering decisions without treating an incomplete or internally inconsistent report as ground truth.

## Executive conclusion

The Cline handoff is useful as a **preliminary boot-chain and firmware-container map**. It is **not yet sufficient to explain the actual MPC application, UI, audio engine, MIDI behavior, or project format**. The main root filesystem and boot/recovery filesystem were reportedly identified but not mounted and explored.

There are also material inconsistencies in the handoff's image identity, offsets, and file-type inventory. Treat all firmware-specific claims below as **reported by the handoff, pending validation against the exact original image and extracted artifacts**. Do not use the handoff's claimed full-image hash as a trusted checksum.

## 1. Findings reported by Cline

The following are findings recorded in `CHATGPT_HANDOFF/README.md`, `technical_report.md`, `filesystem_map.txt`, and `important_strings.txt`. The original firmware binaries are not included in the repository handoff, so these findings have not been independently revalidated here.

### 1.1 Container and filesystem structure

The handoff reports:

- An update-image header with the ASCII magic `AKAI`.
- A four-entry partition table beginning at offset `0x38`.
- Two entries named `xz_stream_0` and `xz_stream_1`, described as ext4 filesystem payloads.
- A FIT/U-Boot image named `kernel.fit`.
- Two filesystem images named `rootfs.ext4` and `boot_recovery.ext4`, both reported as ext4 based on superblock magic.
- A decompressed initramfs reported as a CPIO `newc` archive compressed with Zstandard inside the FIT.

**Confidence:** preliminary report only. Image-size and offset inconsistencies in section 2 must be resolved before relying on the layout.

### 1.2 Reported platform and boot chain

The handoff reports four device-tree configurations for Rockchip RK3588/RK3588S board identifiers:

- `rk3588-az04-acvc`
- `rk3588-az04-acvg`
- `rk3588s-az04b-acva3`
- `rk3588s-az04b-acvr2`

It also reports an AArch64 Linux kernel, a U-Boot FIT container, RSA-2048 signature metadata with key hint `akai-1`, and a public key in the initramfs.

**Important limitation:** presence of signature metadata is not the same as successful cryptographic verification. The handoff explicitly says signatures were not verified. Do not describe the verified-boot chain as independently confirmed until signature verification succeeds against the exact extracted components.

### 1.3 Reported early-boot tools

The handoff lists these initramfs components:

- `az01-image` — reportedly exposes image verification, partition inspection/dumping, key lookup and compatibility-related symbols/options.
- `az0x-migrate` — reportedly handles migration between boot schemes.
- `az0x-splash` — reportedly uses DRM/KMS-related functionality for a boot splash.
- `libaz01-prog.so.1.3` — reportedly exports image parsing, partition, compatibility, key-provider and verification-related functions.
- BusyBox, the dynamic loader, OpenSSL, and filesystem/device-tree utilities.

The exported names and strings are potentially useful clues about **firmware update and boot management**, not evidence about the MPC music application's internal implementation.

## 2. Data-integrity and consistency warnings

These issues must be resolved before the handoff is treated as a reliable map.

### 2.1 Conflicting image sizes for the same filename

The current handoff says the original image is **366,327,168 bytes**.

A separate earlier Colab audit in the project history recorded a file with the same filename as **89,128,960 bytes** and a different SHA-256 (`2ef02e332e96c1b711e9eeff3b3e16bfb3049ae6ac1262d1019320e435717a48`). These may be different files, but their identity has not been reconciled. Do not merge their findings or assume they are the same firmware input.

### 2.2 Partition offsets do not account for the claimed full image

The reported final partition ends at offset `0x0551AF64` (**89,239,396 bytes**). That is far below the handoff's claimed image size of 366,327,168 bytes, leaving most of the claimed file outside the described partition map. It is also 110,436 bytes beyond the 89,128,960-byte size recorded by the earlier Colab audit.

This may mean the input files differ, offsets were interpreted incorrectly, or the reported table is incomplete. Re-read the header and table from the exact original image before trusting the layout.

### 2.3 Claimed original-image SHA-256 is not trustworthy yet

`technical_report.md` gives this hash:

`d3f8c9e2b1a4f7e8c9d0b1a2f3e4d5c6b7a8f9e0d1c2b3a4f5e6d7c8b9a0f1e2`

Its highly patterned form is suspicious and it has not been independently recomputed. Treat it as **unverified and potentially a placeholder**, not as a checksum for provenance or integrity.

### 2.4 Inventory classifications are unreliable

The inventory labels some Python scripts as DTS, a CPIO archive as a text file, and a `.fit` file as a generic DTB. Large-file hashes were skipped. This means the inventory is useful as a file list but not as a validated file-type report.

Some component sizes are also unusually similar. Verify types from file signatures and parser output instead of trusting extensions or inventory labels.

### 2.5 Reported XZ sizes need validation

The report describes the XZ stream and the resulting ext4 image as almost the same size. That is possible in some cases but unusual enough to warrant checking the actual XZ stream boundaries, decompressor exit status, output length, and ext4 superblock independently.

## 3. Relevance to this Android groovebox

### Directly useful now

- The handoff identifies concrete firmware artifacts and likely locations to inspect next.
- The device-tree files may reveal the hardware peripherals of the MPC standalone hardware, once correctly decoded.
- The initramfs strings may help understand update and boot mechanics if that becomes relevant to device-level research.
- The report identifies the two filesystem images that must be explored before the application itself can be understood.

### Not a basis for Android application implementation yet

The reported RK3588/RK3588S SoC and boot chain describe the **MPC's embedded hardware**, not the Android phone/tablet that hosts this project. They do not imply that the app should target RK3588 or reproduce Akai's boot architecture.

The handoff currently provides no verified findings about:

- The main MPC application executable or UI framework.
- Audio engine, DSP, plugin hosting, or audio routing.
- MIDI stack, controller mappings, or USB behavior.
- Sampler, sequencer, project/song file formats, or user-facing workflow implementation.
- The actual application resources, fonts, images, or screen layouts.

Continue implementing from the project's own architecture and independently sourced behavior references. Do not copy Akai firmware code, proprietary resources, or artwork.

## 4. Recommended next steps — in order

### P0 — Reconcile the source image

1. Record the exact original file path, byte size, and freshly computed SHA-256.
2. Compare those values with the earlier Colab input and the Cline input. If they differ, label them as separate artifacts and keep their reports separate.
3. Parse the header and all table entries directly from that exact file.
4. Check every offset and length against the file size; explain any unaccounted region.
5. Verify each decompression by exit code, output size, magic bytes and hashes. Do not proceed on filenames alone.

### P1 — Inspect the root filesystem read-only

Once image identity and offsets are resolved, use a Linux environment or suitable ext4 inspection tool to inspect `rootfs.ext4` **read-only**. Start with directory listings and metadata, then locate the main application executable and its libraries.

Prioritize identifying:

- Application executable(s) and architecture.
- UI framework and resources.
- Audio libraries/configuration/services.
- MIDI/USB configuration and device rules.
- Sample, preset, project and sequence formats.
- System services and launch configuration.

Do not upload proprietary firmware contents to external services or execute extracted binaries.

### P2 — Inspect boot/recovery and device trees

- Inspect `boot_recovery.ext4` read-only for modules and recovery components.
- Convert each DTB to DTS using `dtc`, validate the output, and compare board variants.
- Verify FIT signatures with the correct public key and the exact extracted components.
- Record kernel version/configuration only when supported by actual metadata or tool output.

### P3 — Produce evidence that can guide implementation

For every useful finding, record source path, tool/command, output, hash when relevant, and confidence. Prefer small text reports and metadata over committing binary firmware content. Keep confirmed findings separate from hypotheses.

## 5. Completion criteria for the firmware research

The research becomes sufficient to guide meaningful application-level work when the following are available and internally consistent:

- [ ] Original image size and SHA-256 independently verified.
- [ ] Partition offsets and lengths account for the actual image, or unexplained regions are documented.
- [ ] XZ/FIT/CPIO/ext4 extraction verified by parser results and hashes.
- [ ] Main application executable and UI framework identified.
- [ ] Audio and MIDI/USB components/configuration located.
- [ ] Relevant sample/project/sequence formats identified, if present.
- [ ] Device-tree findings validated from parsed DTS, not strings alone.
- [ ] Any signature claims backed by successful cryptographic verification.

## 6. Source records

- `CHATGPT_HANDOFF/README.md`
- `CHATGPT_HANDOFF/technical_report.md`
- `CHATGPT_HANDOFF/filesystem_map.txt`
- `CHATGPT_HANDOFF/important_strings.txt`
- `CHATGPT_HANDOFF/inventory.csv`
- `CHATGPT_HANDOFF/binary_inventory.csv`
- `CHATGPT_HANDOFF/UNRESOLVED.md`
- `CHATGPT_HANDOFF/project_relevance.md`

This document records the handoff's current claims and limitations. It does not certify that the underlying binary artifacts have been independently verified.
