# MPC Firmware Research — ChatGPT Handoff

## Source Directory
**Research Root:** `D:\MPC-Firmware-Research\`
**Extracted Artifacts:** `D:\MPC-Firmware-Research\Firmware Research Extracted\`
**Handoff Location:** `D:\MPC-Firmware-Research\CHATGPT_HANDOFF\`

## Contents of This Handoff

| File | Description |
|------|-------------|
| `README.md` | This file |
| `inventory.csv` | Complete file inventory (57 files, 679.6 MB) with paths, sizes, extensions, identified types, and SHA-256 hashes |
| `technical_report.md` | Summary of key technical findings with source artifact references |
| `filesystem_map.txt` | Documented filesystem structures, partitions, and directory layouts |
| `important_strings.txt` | Relevant strings from binaries related to MPC interface, audio, MIDI, USB, hardware, settings |
| `binary_inventory.csv` | Important binaries with paths, sizes, SHA-256, types, and utility rationale |
| `project_relevance.md` | How findings apply to Android MPC-style groovebox development |
| `UNRESOLVED.md` | Unknown formats, missing tools, incomplete extractions, evidence still needed |

## What Was Examined

1. **Original firmware image:** `MPC-3.9.1-Gen2-update.img` (366 MB) — Akai MPC Gen2 firmware update
2. **Partition structure:** 4 partitions identified in firmware header (xz_stream_0, xz_stream_1, dtb, unknown)
3. **XZ-compressed streams:** Two XZ streams decompressed to ext4 filesystems (rootfs ~59 MB, boot_recovery ~29 MB)
4. **FIT image:** `kernel.fit` (25.9 MB) — U-Boot Flattened Image Tree containing:
   - Linux kernel (AArch64, raw binary, 20.8 MB)
   - Initrd ramdisk (Zstandard compressed, 6.4 MB → 15.5 MB decompressed)
   - 5 Device Tree Blobs for RK3588/RK3588S variants (az04-acvc, az04-acvg, az04b-acva3, az04b-acvr2)
   - Kernel commandline DTB
   - All components RSA-2048 signed with key "akai-1"
5. **Ramdisk contents:** Minimal initramfs with:
   - `init` and `shutdown` binaries
   - BusyBox (suid/nosuid variants)
   - Akai-specific tools: `az01-image`, `az0x-migrate`, `az0x-splash`
   - Library: `libaz01-prog.so.1.3` (core image verification/migration logic)
   - Standard libraries (libcrypto, libblkid, libmount, etc.)
   - Public key: `akai-1.pub` (RSA-2048)

## Key Hardware Platform

- **SoC:** Rockchip RK3588 / RK3588S (AArch64, 8-core Cortex-A76/A55)
- **Board variants:** az04-acvc, az04-acvg (RK3588); az04b-acva3, az04b-acvr2 (RK3588S)
- **Boot scheme:** U-Boot FIT image with signed components, verified boot chain
- **Filesystems:** ext4 for rootfs and boot/recovery partitions

## Verification Status

| Component | Status |
|-----------|--------|
| Firmware header parsing | ✅ Verified (4 partitions, correct offsets) |
| XZ stream extraction | ✅ Verified (SHA256 matches FIT DTS hashes) |
| ext4 filesystem identification | ✅ Confirmed by superblock magic (0xEF53) |
| FIT image parsing | ✅ Verified (DTS matches extracted components) |
| Kernel extraction | ✅ Raw AArch64 binary, 20.8 MB |
| Ramdisk extraction | ✅ Zstd → CPIO newc archive, 15.5 MB |
| DTB extraction | ✅ 5 DTBs + cmdline, all valid |
| Signature verification | ⚠️ RSA-2048 signatures present, not verified (public key available) |
| Rootfs ext4 mount | ❌ Not mounted (requires Linux/loop mount) |
| Boot/recovery ext4 mount | ❌ Not mounted |
| MPC application binaries | ⚠️ Likely in rootfs.ext4 (not accessible on Windows) |

## Tools Used

- Custom Python scripts for firmware parsing, FIT extraction, XZ/Zstd decompression
- Standard libraries: `zstandard`, `lzma`, `struct`, `hashlib`
- No external reverse engineering tools (Ghidra, IDA, etc.) were used

## Disclaimer

This handoff contains extracted firmware artifacts for research purposes. The original firmware image is copyrighted by Akai Professional / inMusic Brands. Do not redistribute the original firmware or large binary blobs. The analysis artifacts (scripts, reports, hashes, strings) are research outputs.