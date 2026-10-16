# Unresolved Issues — MPC Firmware Research

## Critical Blockers

### 1. Rootfs.ext4 Not Mounted
- **Problem:** The main root filesystem (59.8 MB ext4) contains the MPC application, audio stack, MIDI stack, UI assets, and system configuration. It cannot be mounted on Windows.
- **Impact:** Cannot analyze the actual MPC application logic, audio engine, UI framework, or file formats.
- **Resolution:** Mount on Linux: sudo mount -o loop,ro rootfs.ext4 /mnt/mpc-rootfs
- **Alternative:** Use WSL2 with loop mount, or a Linux VM.

### 2. Boot/Recovery ext4 Not Mounted
- **Problem:** The boot_recovery partition (29.8 MB ext4) likely contains kernel modules, recovery UI, and possibly early-boot audio drivers.
- **Impact:** Missing kernel modules (especially audio codecs, USB audio, MIDI), recovery flow.
- **Resolution:** Mount on Linux: sudo mount -o loop,ro boot_recovery.ext4 /mnt/mpc-boot

### 3. Kernel Not Fully Analyzed
- **Problem:** Raw AArch64 kernel binary (20.8 MB) extracted but not disassembled.
- **Impact:** Don't know kernel version, config, built-in drivers vs modules, custom Akai patches.
- **Resolution:** 
  - Run strings kernel.bin | grep -i "linux version"
  - Use vmlinux-to-elf or similar to analyze
  - Check for Akai-specific drivers (search for "az01", "akai", "mpc")

### 4. DTBs Not Fully Parsed
- **Problem:** 4 board DTBs + cmdline extracted but not converted to DTS for full analysis.
- **Impact:** Hardware peripheral mappings (I2S, SPDIF, USB, GPIO, PWM, ADC, display) unknown.
- **Resolution:** 
  dtc -I dtb -O dts fdt-rk3588-az04-acvc.bin > acvc.dts
  # Repeat for all 4 variants
  # Compare to find board-specific differences

---

## Unknown Formats / Structures

### 5. Firmware Image Header — Partially Understood
- **Known:** Magic "AKAI", version, partition table offset/size/entry count
- **Unknown:** Bytes 0x08-0x1F (24 bytes), flags at 0x20-0x37
- **Need:** More firmware samples to decode unknown fields

### 6. Partition Entry Fields — Partially Understood
- **Known:** Name (16B), Offset (8B), Size (8B), Flags (8B), Unk1 (8B)
- **Unknown:** Flags bitfield meaning, Unk1 purpose
- **Need:** Compare across firmware versions

### 7. FIT Image Signatures — Not Verified
- **Problem:** RSA-2048 signatures present in FIT but not cryptographically verified.
- **Public key:** Available (akai-1.pub, 451 bytes DER)
- **Need:** Verify signatures using OpenSSL to confirm chain of trust.

### 8. libaz01-prog.so Internal Logic — Not Reversed
- **Problem:** Core Akai firmware library (133 KB) not decompiled.
- **Key unknowns:** 
  - Image format parsing (v2 vs v3 ops)
  - Partition compatibility checking logic
  - Onboard key lookup mechanism (hardware-backed?)
  - Signature verification implementation
- **Resolution:** Load in Ghidra/IDA for static analysis (AArch64 ELF)

### 9. az0x-splash DRM/KMS Details — Unknown
- **Problem:** Splash tool uses DRM/KMS but exact connector/mode/framebuffer logic unknown.
- **Need:** Decompile or trace to understand PNG loading, display initialization.

### 10. Boot Migration Logic — Partially Understood
- **Known:** az0x-migrate transitions from legacy u-boot/fastboot to FIT
- **Unknown:** Exact partition rewrite steps, rollback handling, failure recovery
- **Need:** Decompile az0x-migrate and libaz01-prog migration functions

---

## Missing Tools / Environment

### 11. Linux Environment Required
- **Problem:** ext4 mount, DTB parsing, ELF analysis, kernel inspection all require Linux.
- **Current:** Analysis done on Windows with Python only.
- **Resolution:** Provision Linux VM or WSL2 for remaining work.

### 12. Reverse Engineering Tools Not Used
- **Missing:** Ghidra, IDA Pro, Binary Ninja for libaz01-prog.so, az01-image, az0x-migrate, kernel
- **Missing:** vmlinux-to-elf / extract-vmlinux for kernel analysis
- **Missing:** fwupd / fwupdate tools for firmware update testing

### 13. Firmware Version Comparison — Not Done
- **Problem:** Only one firmware version (3.9.1) analyzed.
- **Need:** Compare with older/newer versions to understand evolution, find debug symbols, identify changes.
---

## Evidence Still Needed

### 14. MPC Application Binary — Location Unknown
- **Expected:** In rootfs.ext4 at /usr/bin/, /opt/akai/, /app/, or /data/
- **Need:** Mount rootfs to locate main executable(s)

### 15. Audio Configuration — Unknown
- **Expected:** ALSA config (/etc/asound.conf, /usr/share/alsa/), PipeWire config, UCM profiles
- **Need:** Mount rootfs and boot_recovery

### 16. MIDI/USB Configuration — Unknown
- **Expected:** udev rules for USB MIDI, ALSA sequencer config, gadget config for USB device mode
- **Need:** Mount rootfs

### 17. UI Assets — Unknown
- **Expected:** Images, fonts, themes, QML/Qt resources if Qt-based
- **Need:** Mount rootfs

### 18. Systemd Services — Unknown
- **Expected:** MPC service units, audio service, MIDI service, watchdog
- **Need:** Mount rootfs

### 19. Kernel Modules — Unknown
- **Expected:** In boot_recovery.ext4 or rootfs.ext4 under /lib/modules/
- **Critical:** snd-soc-* (audio codecs), snd-usb-audio, midi drivers, gpio-keys, leds-gpio
- **Need:** Mount boot_recovery and rootfs

### 20. Hardware Bring-up Documentation — Missing
- **Need:** Schematics, pinmux, codec datasheets for RK3588 audio paths
- **Source:** DTBs provide partial info; need vendor docs for complete picture.

---

## Recommended Next Steps (Priority Order)

1. **HIGHEST: Mount rootfs.ext4 on Linux** — unlocks 90% of missing evidence
2. **Mount boot_recovery.ext4** — gets kernel modules, recovery
3. **Convert all 4 DTBs to DTS** — maps hardware peripherals
4. **Decompile libaz01-prog.so** — understands verification/migration logic
5. **Verify FIT signatures** — confirms trust chain
6. **Analyze kernel version/config** — from kernel.bin strings
7. **Compare with other MPC firmware versions** — if available
