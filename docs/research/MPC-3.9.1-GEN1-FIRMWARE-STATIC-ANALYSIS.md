# MPC 3.9.1 Gen1 Firmware — Offline Static Analysis

**Status:** first-pass static analysis completed; function-level decompilation remains open  
**Date:** 2026-10-11  
**Target:** Akai MPC One / Gen1 family, firmware image named `MPC-3.9.1-Gen1-update.img`  
**Analysis policy:** offline inspection only. The supplied image and extracted proprietary resources are not stored in this public repository.

## Executive result

The supplied image is **not opaque or encrypted at the root-filesystem layer**. Its envelope declares an XZ-compressed `rootfs` payload. I extracted the XZ stream, validated its embedded SHA-1 digest, decompressed it successfully, identified a modern ext4-format root filesystem, and traversed/extracted its reachable entries for local inspection. The embedded MPC application is a stripped 32-bit ARM ELF carrying a product-version note `3.9.1.2 (f3af35cdc95)`.

The highest-value discovery for UI research is a large bundled resource tree under `/usr/share/Akai/SME0/Resources`, including **772 ACVS resource entries: 759 PNG files and 13 JSON layout descriptors**. The executable also contains readable UI/component RTTI and configuration keys. These facts give us an evidence-based vocabulary for future comparisons with official MPC 3.9 documentation and captured device screens.

This is not a claim that the original source code has been recovered or that every bundled resource maps to the physical MPC One Main screen. The resources include panel/help descriptions that appear host/desktop-oriented; exact applicability must be checked before using them to drive a standalone layout decision.

## 1. Input identity and integrity

| Field | Observed result |
|---|---|
| Supplied filename | `MPC-3.9.1-Gen1-update.img` |
| File size | 162,220,784 bytes |
| Whole-image SHA-256 | `4c0f7797a006313fad206f9513eca8b3823e834c9e0941a6b5f088386cb835d6` |
| Envelope marker | ASCII `AZ01` at offset `0x00` |
| Snapshot string | `SNAPSHOT-20260709104848` at offset `0x10` |
| Human-readable image label | `MPC upgrade image` |
| Partition/section marker | `PARTL` |
| Payload name | `rootfs` |
| Compression declaration | `xz` |
| Declared digest algorithm | `sha1` |
| XZ payload offset | 340 bytes (`0x154`) |
| XZ payload length | 162,220,428 bytes |
| Embedded and recomputed payload SHA-1 | `835e1bc192961bf4906c998e1bcf269d2055f6c7` — match |
| XZ validation | `xz -t` passed |

The XZ stream ends with its standard `YZ` footer. The image has a trailing `EOF` marker and padding beyond the compressed stream. The matching SHA-1 confirms payload integrity relative to the digest in the supplied container; **this is not a signature or an independent proof of publisher authenticity**.

### Declared Gen1 compatibility identifiers

The image contains eight device compatible strings:

- `inmusic,acv5`
- `inmusic,acv8`
- `inmusic,acva`
- `inmusic,acva2`
- `inmusic,acvb`
- `inmusic,acvm`
- `inmusic,acvr`
- `inmusic,acv5s`

The associated eight numeric target IDs are `0x09e8403a`, `0x09e8403b`, `0x09e84046`, `0x09e84047`, `0x09e8404b`, `0x09e84052`, `0x09e84056`, and `0x09e84057`. This matches a Gen1-targeted image; do not treat it as a Gen2 firmware package.

## 2. Root filesystem

The XZ payload decompresses to a **441,279,488-byte ext4-format filesystem image**.

| Filesystem fact | Value |
|---|---|
| Filesystem UUID | `b869c04d-3d59-485d-8bf1-b844c378176e` |
| Block size | 1,024 bytes |
| Declared blocks | 430,936 |
| Inodes | 107,696 |
| Inode size | 256 bytes |
| Group count | 53 |
| Incompat features | filetype, extents, 64bit, flex_bg, csum_seed |
| Read-only-compatible features | sparse_super, large_file, huge_file, dir_nlink, extra_isize, project |
| Decompressed rootfs SHA-256 | `6aafd4384469a76df4c4270aa4f07b3f7e411828a691c75d5471c5fdfe2d69f3` |
| Decompressed rootfs SHA-1 | `93c353b051ab9bf789c9cb2f5d7a7798e0c7470f` |

A local read-only metadata walker traversed **11,529 directory/file entries with zero directory-parse errors**. Local extraction produced regular-file payloads totalling 384,690,598 bytes. The walker is a custom analysis helper, not a substitute for a filesystem consistency check by `e2fsck`; that validation was not run in this environment. Extracted firmware contents remain under a temporary local path and are not included in this repository.

## 3. Version, platform and primary executable

Observed version markers:

- `/etc/az01-release`: `AZ01_VERSION=9ad1b9c0`; build tag `jenkins-ACVS-MPC3-SME0-MPC3-Yocto-563`.
- `/usr/share/Akai/SME0/VERSION`: `3.9.1.2`.
- `/usr/bin/MPC`, ELF note `.note.mpc.version`: `3.9.1.2 (f3af35cdc95)`.
- `/usr/bin/MPC` Build ID: `8371199f945ecc40f79cb19c17ff5de72810a91c`.

`/usr/bin/MPC` is an ELF 32-bit little-endian ARM EABI5 PIE, dynamically linked through `/usr/lib/ld-linux-armhf.so.3`, stripped of ordinary symbol names. It contains a large code segment, an extensive read-only data section and readable strings/type-information names. We did not execute the target ARM binary.

Other notable components found by path/name:

- `/usr/bin/DeviceControlServer.bin` — approximately 5.9 MB.
- `/usr/bin/test-app-launcher` — approximately 6.0 MB.
- `/boot/zImage-6.18.26-az01-2026-04-30-rt4` — 6,314,224 bytes.
- `/opt/ramfs-update.squashfs` — 3,710,976 bytes, identified as SquashFS v4, zlib-compressed, reported block size 131,072 and 273 inodes. This nested image was identified but not extracted in the available environment.

## 4. Resource-tree inventory

Counts below are metadata about the local image, not a publication of the assets themselves.

| Path | Observed inventory | Research value |
|---|---|---|
| `/usr/share/Akai/SME0/Resources/ACVS` | 772 entries: 759 PNG + 13 JSON; 2,846,324 bytes | UI pictogram groups and panel/layout descriptors |
| `.../ACVS/graphics/toolbar` | 41 PNG files | Mode/toolbar icon vocabulary |
| `.../ACVS/graphics/statusbar` | 46 PNG files | Panel visibility/status icon vocabulary |
| `.../ACVS/graphics/filebrowser` | 52 PNG files | File/sample filter and Browser-related vocabulary |
| `.../ACVS/graphics/mixers/channelstrip` | 26 PNG files | Channel-strip states and program-type icon categories |
| `.../ACVS/graphics/sequenceeditor` | 38 PNG files | Sequence-editor pictogram vocabulary |
| `.../ACVS/graphics/trackview` | 18 PNG files | Track state and track-view pictograms |
| `.../ACVS/graphics/programedit` | 29 PNG files | Program Edit resource category |
| `.../ACVS/graphics/soundsbrowser` | 39 PNG files | Sounds Browser resource category |
| `/usr/share/Akai/SME0/Resources/Pages` | 232 directory/file entries, including structured Program Edit JSON | Component definitions, tabs and parameter-page schemas |
| `/usr/share/Akai/SME0` | 1,884 entries; 31,450,023 bytes of regular-file payloads | Broader Akai/SME0 implementation resources |
| `/usr/share/Akai/Content` | 3,674 entries; 89,490,644 bytes of regular-file payloads | Presets, samples and other content, not needed for UI reproduction |

The structured Program Edit material includes TUI/component JSON for Drum, Keygroup, MIDI, CV (8- and 16-channel), and Plugin contexts. These files may give future analysis a way to inventory parameter names, tabs, component types and Q-Link assignments, but they are not reproduced here.

### 4.1 Layout JSON findings

Thirteen JSON layout descriptors are packaged in `ACVS/layout`. Selected values observed directly in these files:

- `Toolbar.json`: height 55.
- `StatusBar.json`: bottom aligned, height 34.
- `MainBody.json`: full-width/full-height base with padding left 208, right 397, top 58, bottom 35.
- `Inspector.json`: width 206; top padding 58 and bottom 36.
- `Browser.json`: right aligned, width 294; top padding 56 and bottom 27.
- `FileBrowser.json`: right aligned, width 294; top padding 58 and bottom 27.
- `GuiSequenceEditor.json`: full-width/full-height base with bottom padding 338.
- `PadPanel.json`: width 264, height 310.
- `ProjectInfo.json`: height 336.

**Critical interpretation caveat:** these numbers are geometry from the bundled panel descriptor files; they are not established pixel measurements of the physical MPC One Main screen. The companion help strings refer to concepts such as hover, a View menu, a window Status Bar, and account UI. Treat these descriptors as evidence about the shared/panel UI framework and possible relationships only. Do not copy these widths into Android Main without checking their target context against the official standalone manual and actual device screenshots.

## 5. Executable string/type-information observations

Readable symbols and string literals in the stripped `MPC` ELF identify useful investigation targets. Examples observed include:

- Main layout data types: `SerialisableTUIMainModeLayoutData` and `SerialisableTUIMainModeMixerLayoutData`.
- Q-Link/Main: `QLinkMainMode` and `ShowQLinksStatusOverlay`.
- Media Browser parameter model: `ACVFMediaBrowserParameter`.
- UI behavior/control: `MpcInterfaceBehaviour`, `ModeShortcutPanel.Enabled`, `TransportBarCentreControls`.
- Sound selection settings: `SoundSelect.DefaultView`, `SoundSelect.OpenProgramEdit`, `SoundSelect.ShowLockedPlugins`.
- Step Sequencer settings: `stepSequencerButtonModes.DefaultStepSequencerMode`.
- Main/clip behavior: `TUIMainModeLaunchSelectedClipDuringPlayback`.
- Automation/Q-Link setting: `AutomationLaneFollowsQLinks`.
- Touch-focused component names: `TouchProgramEditPageToolbar`, `TouchStepSequenceStepEditPageEventsTab`.

Strings/type-info also contain the vocabulary of Drum, Keygroup, Clip, Plugin, MIDI, CV, Audio, Send, Submix, Output and Input track/channel categories, and terms related to XYFX availability/insertion. These are evidence that the names/features occur in the binary, **not proof by themselves that each type or operation is enabled in every hardware mode**.

Because ordinary symbols are stripped, these initial findings are not a decompilation of specific method bodies. The next higher-value static-analysis step would be to import the ELF into a matching ARM 32-bit profile in Ghidra and trace how Main layout data and default values are serialized/deserialized. The Ghidra project/database and decompiled output should remain local unless there is a separate approved sharing decision.

## 6. Application to MPC MkII Groovebox

1. **Preserve the official v3.9 manual as the public behavioral authority.** Use this firmware audit as secondary implementation evidence and use real MPC One screenshots/device observation as the visual authority.
2. **Prioritize Main layout-data analysis.** The two `SerialisableTUIMainMode*LayoutData` types are promising anchors for local follow-up work. Establish their actual fields/defaults before drawing conclusions from the names.
3. **Build a vocabulary crosswalk.** Compare confirmed resource groups, component names and available labels with the project's existing screen map and current code. Record confidence and any version/host caveat.
4. **Do not blindly apply panel geometry.** The shared layout JSON may reflect a different display/context than the MPC One standalone Main composition.
5. **Do not change audio/MIDI/backend code based only on strings.** No audio callback, sampler, sequencer scheduler, USB MIDI transport, MkII decoder or SysEx implementation was modified by this research.
6. **Keep the next product slice unchanged.** The current next implementation task remains the real Android Document Picker selection → return → sample-target preservation test, followed by a persisted user-selected document tree/favourites only with runtime permission/restoration tests. Firmware research is supporting evidence, not a reason to delay that slice for cosmetic-only work.

## 7. Reproducibility and tooling boundary

Analysis performed locally: whole-file SHA-256, envelope field inspection, XZ payload extraction, embedded SHA-1 validation, XZ test/decompression, ext4 metadata/tree traversal, local extraction, `file`, `readelf -n`, file-size/path inventory, JSON parsing, and string/type-info inspection.

The analysis environment did not have REA, Binwalk, Unblob, `unsquashfs` or Ghidra available. The main filesystem was therefore handled with the available XZ tool plus a custom read-only ext4 metadata walker. This is enough to establish the findings above, but not a full automated firmware audit or native-code decompilation.

Unfinished investigations:
- extract and inventory `/opt/ramfs-update.squashfs` using a compatible SquashFS tool;
- run a proper filesystem consistency check in a Linux environment with `e2fsprogs`;
- perform targeted Ghidra analysis of Main-layout serialization and default state;
- compare each promising finding with v3.9 manual sections and captured standalone screens.

## 8. Public repository boundary

Do **not** commit or attach to public issues/PRs:

- the firmware image or its compressed/raw filesystem;
- extracted proprietary PNGs, fonts, presets or audio content;
- the complete private file inventory;
- full decompiled or disassembled implementation code.

This report retains only hashes, structural metadata, file-path-level observations, concise technical identifiers and carefully qualified deductions. Continue to use original artwork and the project's own implementation.
