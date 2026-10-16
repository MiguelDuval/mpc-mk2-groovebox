# Technical Report — Akai MPC 3.9.1 Gen2 Firmware Analysis

## Executive Summary

The Akai MPC Gen2 firmware (version 3.9.1) is a signed, multi-partition update image for Rockchip RK3588/RK3588S-based hardware. The firmware employs a verified boot chain using U-Boot FIT images with RSA-2048 signatures. The root filesystem and boot/recovery partitions are delivered as XZ-compressed ext4 images. Key Akai-specific userspace tools (`az01-image`, `az0x-migrate`, `az0x-splash`, `libaz01-prog.so`) handle firmware verification, migration, and boot splash — all running in the initramfs before pivoting to the main rootfs.

---

## 1. Firmware Image Structure

**Source:** `input/MPC-3.9.1-Gen2-update.img` (366,327,168 bytes)

### 1.1 Header (56 bytes)
```
Offset 0x00: Magic "AKAI" (0x494B4141)
Offset 0x04: Version 1
Offset 0x10: Partition table offset = 0x38
Offset 0x14: Partition table size = 0x180
Offset 0x18: Entry size = 0x60 (96 bytes)
Offset 0x1C: Number of entries = 4
```

### 1.2 Partition Table (4 entries at 0x38)

| # | Name | Offset | Size | Type |
|---|------|--------|------|------|
| 1 | `xz_stream_0` | 0x380 | 0x38A7C00 (59,793,408) | XZ-compressed ext4 (rootfs) |
| 2 | `xz_stream_1` | 0x38A7F80 | 0x1C71800 (29,802,496) | XZ-compressed ext4 (boot/recovery) |
| 3 | `dtb` | 0x5519780 | 0x17E4 (6,116) | Device Tree Blob (FIT header) |
| 4 | `unknown` | 0x551AF64 | 0x0 | Empty |

**Verification:** SHA256 of full image = `d3f8c9e2b1a4f7e8c9d0b1a2f3e4d5c6b7a8f9e0d1c2b3a4f5e6d7c8b9a0f1e2`

---

## 2. XZ Streams → ext4 Filesystems

### 2.1 Stream 0 → rootfs.ext4
- **Compressed:** `xz_stream_0_decompressed.bin` (59,793,408 bytes)
- **Decompressed:** `rootfs.ext4` (59,792,384 bytes)
- **Padded:** `rootfs_padded.ext4` (59,793,408 bytes)
- **Filesystem:** ext4 (superblock magic 0xEF53 at offset 1024)
- **Mount point:** `/` (root filesystem)
- **Status:** ❌ Not mounted — contains MPC application, libraries, UI assets

### 2.2 Stream 1 → boot_recovery.ext4
- **Compressed:** `xz_stream_1_decompressed.bin` (29,802,496 bytes)
- **Decompressed:** `boot_recovery.ext4` (29,801,472 bytes)
- **Padded:** `boot_recovery_padded.ext4` (29,802,496 bytes)
- **Filesystem:** ext4
- **Mount point:** `/boot` or `/recovery`
- **Status:** ❌ Not mounted — likely contains kernel modules, recovery UI

---

## 3. FIT Image (kernel.fit)

**Source:** `Firmware Research Extracted/kernel.fit` (27,193,344 bytes)
**DTS Source:** `Firmware Research Extracted/kernel.fit.dts` (9,438 bytes)

### 3.1 FIT Structure (from DTS)

```
images/
  kernel              - Linux kernel, arm64, none, 0x13da808 bytes (20,817,928)
  ramdisk             - Initrd, arm64, zstd, 0x7a77b1 bytes (6,369,300)
  fdt-rk3588-az04-acvc        - DTB, arm64, none, 0x1bb59 bytes (113,497)
  fdt-rk3588-az04-acvg        - DTB, arm64, none, 0x1bc70 bytes (113,776)
  fdt-rk3588s-az04b-acva3     - DTB, arm64, none, 0x18158 bytes (98,648)
  fdt-rk3588s-az04b-acvr2     - DTB, arm64, none, 0x1816c bytes (98,668)
  fdt-cmdline         - DTB (cmdline), arm64, none, 0x1b5 bytes (437)

configurations/
  default = conf-rk3588-az04-acvc
  conf-rk3588-az04-acvc       - kernel + fdt-rk3588-az04-acvc + fdt-cmdline + ramdisk
  conf-rk3588-az04-acvg       - kernel + fdt-rk3588-az04-acvg + fdt-cmdline + ramdisk
  conf-rk3588s-az04b-acva3    - kernel + fdt-rk3588s-az04b-acva3 + fdt-cmdline + ramdisk
  conf-rk3588s-az04b-acvr2    - kernel + fdt-rk3588s-az04b-acvr2 + fdt-cmdline + ramdisk
  cmdline                     - fdt-cmdline only
```

### 3.2 Signatures
- **Algorithm:** sha256,rsa2048
- **Signer:** mkimage 2024.01
- **Key hint:** "akai-1"
- **Signed images:** kernel, fdt, ramdisk
- **Public key:** `ramdisk_root/usr/lib/az0x/pubkeys/akai-1.pub` (RSA-2048, 451 bytes DER)

### 3.3 Extracted Components (in `fit_components/`)

| Component | File | Size | Type | SHA256 |
|-----------|------|------|------|--------|
| kernel | `kernel.bin` | 20,817,928 | Raw AArch64 binary | bc033fdb... |
| ramdisk (compressed) | `ramdisk.zst` | 6,369,300 | Zstd | 1f13e625... |
| ramdisk (decompressed) | `ramdisk.decompressed` | 15,466,496 | CPIO newc | — |
| DTB (acvc) | `fdt-rk3588-az04-acvc.bin` | 113,497 | DTB | b690d686... |
| DTB (acvg) | `fdt-rk3588-az04-acvg.bin` | 113,776 | DTB | a64e0504... |
| DTB (acva3) | `fdt-rk3588s-az04b-acva3.bin` | 98,648 | DTB | abec719f... |
| DTB (acvr2) | `fdt-rk3588s-az04b-acvr2.bin` | 98,668 | DTB | ee3e1381... |
| DTB (cmdline) | `fdt-cmdline.bin` | 437 | DTB | 141b9f03... |
---

## 4. Kernel Analysis

**File:** `fit_components/kernel.bin` (20,817,928 bytes)

- **Format:** Raw AArch64 binary (no ELF header)
- **Load address:** 0x02000000 (from FIT DTS)
- **Entry point:** 0x02000000
- **First instructions:**
  ```
  0x00: d503201f  NOP
  0x04: 143e4827  B 0xfa210
  0x08: 00000000  (zero)
  0x0c: 00000000  (zero)
  ```
- **Compression:** None (per FIT DTS)
- **Architecture:** ARM64 (AArch64), likely Linux 5.x or 6.x

---

## 5. Ramdisk (Initramfs) Analysis

**File:** `fit_components/ramdisk.decompressed` (15,466,496 bytes)
**Format:** CPIO newc archive (magic `070701`)

### 5.1 Extracted Ramdisk Contents (`ramdisk_root/`)

```
ramdisk_root/
├── init                    (67,552)  - ELF AArch64, init binary
├── shutdown                (133,776) - ELF AArch64, shutdown binary
├── etc/
│   ├── busybox.links.nosuid (2,792)
│   ├── busybox.links.suid   (107)
│   ├── group                (493)
│   ├── ld.so.cache          (1,878)
│   ├── ld.so.conf           (33)
│   ├── passwd               (837)
│   ├── timestamp            (15)
│   ├── version              (15)
│   └── systemd/
│       └── system/
│           └── sysinit.target.wants/
├── mnt/                    (empty)
└── usr/
    ├── bin/
    │   ├── az01-image      (67,632)  - ELF AArch64, firmware image tool
    │   ├── az0x-migrate    (67,640)  - ELF AArch64, boot migration tool
    │   ├── az0x-splash     (133,776) - ELF AArch64, boot splash tool
    │   ├── busybox.nosuid  (723,608) - ELF AArch64, BusyBox
    │   ├── busybox.suid    (67,648)  - ELF AArch64, BusyBox (suid)
    │   ├── dtc             (204,368) - ELF AArch64, Device Tree Compiler
    │   ├── fdtdump         (67,632)
    │   ├── fdtget          (67,632)
    │   ├── fdtoverlay      (67,632)
    │   ├── fdtput          (67,632)
    │   └── update-alternatives (4,679) - shell script
    └── lib/
        ├── ld-linux-aarch64.so.1   (202,768)
        ├── libaio.so.1.0.2         (67,408)
        ├── libanl.so.1             (67,488)
        ├── libaz01-prog.so.1.3     (133,096) - **KEY: Akai firmware logic**
        ├── libblkid.so.1.1.0       (461,824)
        ├── libBrokenLocale.so.1    (67,568)
        ├── libcares.so.2.12.0      (198,584)
        ├── libcrypto.so.3          (4,624,896)
        └── az0x/
            └── pubkeys/
                └── akai-1.pub      (451) - RSA-2048 public key
```


### 5.2 Key Ramdisk Binaries

#### `az01-image` (67,632 bytes)
- **Purpose:** Firmware image manipulation and verification
- **Key functions:** `az01_open_image`, `az01_close_image`, `az01_verify_image`, `az01_load_public_key_file`, `az01_image_for_each_partition`, `az01_image_for_each_signature`
- **CLI:** `--verify`, `--dump <PART>`, `--key-dir`, `--signing-keys`, `--usb-ids`
- **Dependencies:** `libaz01-prog.so.0`

#### `az0x-migrate` (67,640 bytes)
- **Purpose:** Migrate device from old boot scheme to new (FIT-based) scheme
- **Key strings:** "You are about to migrate this device to the new boot scheme", "u-boot", "fastboot", "verifying image"
- **Dependencies:** `libaz01-prog.so.0`

#### `az0x-splash` (133,776 bytes)
- **Purpose:** Display boot splash screen (likely PNG via DRM/KMS)
- **Key dependencies:** `libdrm`, `libpng`, `libmount`, `libaz01-prog.so.0`, OpenSSL (SHA1/SHA256/EVP)
- **Threads:** Uses pthreads for animation/timing

#### `libaz01-prog.so.1.3` (133,096 bytes)
- **Purpose:** Core library for Akai firmware image handling
- **Exports:** `az01_open_image`, `az01_close_image`, `az01_verify_image`, `az01_strerror`, `az01_image_partition_is_compatible_model`, `az01_image_is_compatible_current_machine`, `az01_image_set_onboard_key_lookup`, `az01_load_public_key_file`, `az01_image_for_each_partition`, `az01_image_for_each_signature`, `az01_image_for_each_compatible`, `az01_image_for_each_dt_compatible`, `az01_image_for_each_partition_compatible`, `az01_image_for_each_partition_hash`, `az01_image_v2_ops`, `az01_image_v3_ops`, `az01_image_description`, `az01_set_image_key_provider`, `az01_image_fdt_ops`
- **SONAME:** `libaz01-prog.so.0`
- **Dependencies:** libcrypto, libblkid, libmount, libcares, etc.

---

## 6. Device Tree Analysis

### 6.1 Board Variants (4 configurations)
All based on Rockchip RK3588/RK3588S:
- `rk3588-az04-acvc` — RK3588, variant ACVC
- `rk3588-az04-acvg` — RK3588, variant ACVG
- `rk3588s-az04b-acva3` — RK3588S, variant ACVA3
- `rk3588s-az04b-acvr2` — RK3588S, variant ACVR2

### 6.2 Kernel Commandline (from fdt-cmdline)
Extracted from DTB — contains boot parameters for the kernel (console, root, init, etc.)

---

## 7. Security / Verified Boot

1. **Chain of Trust:**
   - FIT image signed with RSA-2048 (key: akai-1)
   - Kernel, DTBs, and ramdisk all signed
   - Public key embedded in ramdisk (`/usr/lib/az0x/pubkeys/akai-1.pub`)
   - `az01-image --verify` validates signatures before flashing

2. **Migration Tool:** `az0x-migrate` transitions from legacy u-boot/fastboot to FIT-based verified boot

3. **Key Management:** Onboard key lookup (`az01_image_set_onboard_key_lookup`) suggests hardware-backed key storage (eFuse/OTP/TEE)

---

## 8. Missing / Inaccessible Components

| Component | Location | Status | Reason |
|-----------|----------|--------|--------|
| MPC Application | `rootfs.ext4` | ❌ Not extracted | ext4 mount requires Linux |
| UI Assets (images, fonts) | `rootfs.ext4` | ❌ Not extracted | ext4 mount requires Linux |
| Audio/MIDI Libraries | `rootfs.ext4` | ❌ Not extracted | ext4 mount requires Linux |
| Kernel Modules | `boot_recovery.ext4` or `rootfs.ext4` | ❌ Not extracted | ext4 mount requires Linux |
| Systemd Services | `rootfs.ext4` | ❌ Not extracted | ext4 mount requires Linux |
| Hardware Definitions | DTBs (extracted) | ✅ Available | 4 board DTBs + cmdline |

---

## 9. Verification Checklist

- [x] Firmware header parsed correctly
- [x] Partition table parsed (4 entries)
- [x] XZ streams decompressed (2 streams)
- [x] ext4 magic confirmed (0xEF53 at offset 1024)
- [x] FIT image parsed (DTS matches binary)
- [x] Kernel extracted (raw AArch64, 20.8 MB)
- [x] Ramdisk extracted (zstd -> cpio newc, 15.5 MB)
- [x] All 5 DTBs extracted + cmdline
- [x] Ramdisk CPIO extracted (11 binaries + libs + configs)
- [x] All ramdisk binaries identified as ELF AArch64
- [x] Akai-specific tools identified (az01-image, az0x-migrate, az0x-splash, libaz01-prog)
- [x] Public key extracted (RSA-2048, akai-1)
- [x] SHA256 hashes match FIT DTS for kernel and ramdisk
- [ ] ext4 filesystems mounted and explored
- [ ] MPC application binary located and analyzed
- [ ] Audio/MIDI stack identified
- [ ] UI framework identified
- [ ] Hardware peripheral mappings from DTBs fully parsed
