# Project Relevance — Android MPC-Style Groovebox

## How This Firmware Analysis Helps

### 1. Hardware Platform Confirmation
**Finding:** MPC Gen2 runs on Rockchip RK3588/RK3588S (AArch64, 8-core)
**Relevance:** Your Android groovebox can target the same SoC family. RK3588 has:
- 4x Cortex-A76 @ 2.4 GHz + 4x Cortex-A55 @ 1.8 GHz
- Mali-G610 MP4 GPU (OpenGL ES 3.2, Vulkan 1.2)
- NPU: 6 TOPS (useful for ML-assisted audio features)
- 4K/8K video decode/encode
- Rich I/O: PCIe 3.0, USB 3.0/2.0, GMAC, SDIO, I2C, SPI, UART, PWM, ADC

**Action:** Ensure your Android build targets RK3588 with appropriate kernel config.

---

### 2. Verified Boot Chain — Security Model
**Finding:** FIT image with RSA-2048 signatures (key: akai-1), signed kernel+DTB+ramdisk
**Relevance:** Android also uses verified boot (AVB). Understanding Akai's approach helps:
- They use U-Boot FIT + custom userspace tools (az01-image, az0x-migrate)
- Migration from legacy u-boot/fastboot to FIT is handled in initramfs
- Public key embedded in ramdisk for offline verification

**Action:** 
- Study libaz01-prog.so for signature verification logic
- Implement AVB 2.0 with similar key hierarchy
- Consider embedding recovery/verification tools in initramfs

---

### 3. Initramfs Architecture — Early Userspace
**Finding:** Minimal initramfs (15.5 MB CPIO) with custom Akai tools runs BEFORE pivot_root to main rootfs
**Relevance:** Your Android device can use similar pattern:
- Initramfs handles: firmware verification, migration, splash screen, early init
- Custom tools (az01-image, az0x-migrate, az0x-splash) are statically linked or minimal deps
- BusyBox provides shell utilities

**Action:**
- Design your initramfs with: verification -> migration -> splash -> pivot_root
- Consider porting az0x-splash concept for branded boot animation
- Use az0x-migrate pattern for OTA scheme transitions

---

### 4. Audio/MIDI Stack — IN ROOTFS (Not Yet Accessible)
**Finding:** Main MPC application, audio engine, MIDI stack, UI are in rootfs.ext4 (not mounted)
**Relevance:** This is the MOST VALUABLE missing piece. The rootfs likely contains:
- MPC application binary (likely Qt or custom UI framework)
- ALSA/PipeWire audio configuration
- MIDI stack (alsa-seq, libasound, custom MIDI routing)
- Pad/velocity processing, sample playback engine
- Plugin/VST hosting (if supported)
- Project/song file format parsers

**Action (PRIORITY):**
1. Mount rootfs.ext4 on Linux: sudo mount -o loop rootfs.ext4 /mnt
2. Extract and analyze:
   - Main executable(s) in /usr/bin/, /opt/, /app/
   - Libraries in /usr/lib/, /lib/
   - Audio config: /etc/alsa/, /etc/pipewire/, /usr/share/alsa/
   - MIDI config: /etc/asound.conf, udev rules for USB MIDI
   - UI assets: /usr/share/, /opt/akai/, /data/
   - Systemd services: /etc/systemd/system/*.service
