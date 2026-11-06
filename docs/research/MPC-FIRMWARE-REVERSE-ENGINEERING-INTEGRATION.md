# MPC Firmware Reverse-Engineering Research — Integration for MPC MkII Groovebox

**Research date:** 2026-11-06  
**Product:** MPC Studio MkII Groovebox (Android host + physical MPC Studio MkII)  
**Scope:** Transfer useful findings from two public reverse-engineering repositories into our product knowledge base. This is not a firmware modification project and not research for OpenPlugin on an MPC One Gen1.

## 1. Executive findings

The two repositories contribute useful information at different layers:

1. **TheKikGen/MPC-LiveXplore** documents standalone MPC/Force system access, firmware-image tooling, global MIDI mapping and experiments using generic USB-MIDI controllers as additional control surfaces. Its MPC.settings example exposes a useful distinction between MIDI devices assigned to musical tracks, global input, synchronization and controller duties.
2. **DeathCamel58/denon-reverse-engineering** contains a much deeper static analysis of the MPC 3.9.1 Gen1 application binary. It identifies the application as a 32-bit ARM ELF built around JUCE, with embedded Pure Data, SQLite references and Crashpad/Sentry components. It also documents how the application identifies a standalone unit at runtime and distinguishes the MPC 3.x image family from older MPC 2.x packaging.

**The most actionable product insight is MIDI-role separation.** A device that supplies musical notes is not necessarily the same device that supplies global controls or clock. We should preserve this distinction when the product later grows beyond the dedicated MkII control-surface connection. It does not justify changing the current transport or controller adapter now.

**The most important software insight is architectural, not a dependency recommendation.** Akai's MPC 3.9.1 application appears to use JUCE for its application/UI framework and embeds Pure Data for synthesis/DSP. Our project already uses JUCE but has selected Tracktion Engine as its audio/sequencing foundation. The firmware finding does not justify replacing Tracktion, adding Pure Data, changing audio callbacks, or copying any proprietary implementation.

**No runtime application code was changed by this research.** The findings below are secondary evidence and implementation leads. They do not supersede the official MPC Standalone OS 3.9 manual, our local Gen1 static-analysis record, the action catalog, the physical MkII traceability map, exact-SHA CI, or real hardware tests.

## 2. Source scope and confidence

### Source A — MPC-LiveXplore

Repository: https://github.com/TheKikGen/MPC-LiveXplore  
Relevant material reviewed:
- README and project links, including the firmware-image tools and MIDI/controller experiments.
- MPC/MPC.settings, a real settings example from an MPC X system.
- imgmaker/readme.md and the image tool source.
- MPC/autorun/readme.md.
- conf/kernel_config and the captured MPC X hardware report.

Useful anchors:
- [Project README](https://github.com/TheKikGen/MPC-LiveXplore/blob/master/README.md)
- [MIDI/controller experiments in the README](https://github.com/TheKikGen/MPC-LiveXplore#hacks--news-log)
- [MPC settings example](https://github.com/TheKikGen/MPC-LiveXplore/blob/master/MPC/MPC.settings)
- [Image-tool documentation](https://github.com/TheKikGen/MPC-LiveXplore/blob/master/imgmaker/readme.md)
- [Autorun technical notes](https://github.com/TheKikGen/MPC-LiveXplore/blob/master/MPC/autorun/readme.md)

### Source B — Denon Reverse Engineering

Repository: https://github.com/DeathCamel58/denon-reverse-engineering  
Relevant material reviewed at repository revision 0f7966516609e3de2b71e4aef21625bd8d94c57c:
- [MPC application analysis](https://github.com/DeathCamel58/denon-reverse-engineering/blob/0f7966516609e3de2b71e4aef21625bd8d94c57c/Writerside/topics/MPC-Application.md)
- [MPC firmware and device information](https://github.com/DeathCamel58/denon-reverse-engineering/blob/0f7966516609e3de2b71e4aef21625bd8d94c57c/Writerside/topics/MPC.md)
- [Firmware version and image-family notes](https://github.com/DeathCamel58/denon-reverse-engineering/blob/0f7966516609e3de2b71e4aef21625bd8d94c57c/Writerside/topics/Firmware.md)
- [Firmware container/signature research](https://github.com/DeathCamel58/denon-reverse-engineering/blob/0f7966516609e3de2b71e4aef21625bd8d94c57c/Writerside/topics/Signed-Firmware-Layout.md)

### Evidence labels used here

- **Documented static finding:** reported by a repository after static inspection or derived from a checked configuration/file. It is not a runtime test by our project.
- **Research lead:** a plausible design clue that needs an independent test before it becomes a product contract.
- **Not established:** the source itself does not prove the behavior, version coverage or applicability we would need.

The two repositories are independent third-party research, not official Akai specifications.

## 3. Findings from MPC-LiveXplore

### 3.1 Global control mapping exists separately from track MIDI

The project reports that standalone MPC software has a Global MIDI Learn/mapping facility. It also describes an experiment using a Linux LD_PRELOAD library to make a generic USB-MIDI controller act as a secondary control surface, extending the available physical controls without changing the main MPC hardware.

This matters to our product because the MPC Studio MkII is not merely a note keyboard. Its buttons, jog wheel, touch strip, pads and feedback channel are control-surface events that must reach the app's semantic command layer, while musical note input and external MIDI instruments have different destinations.

**Product interpretation:** preserve separate conceptual routes for:
- musical MIDI input routed to a track/program;
- global controller input routed to app-level commands;
- MIDI clock/transport synchronization;
- a dedicated control-surface adapter with its own feedback protocol.

This is a future routing model, not a request to put the MkII through Akai's Linux-specific LD_PRELOAD path. That technique is specific to modifying a running Linux application and has no role in our Android implementation.

### 3.2 The settings example exposes distinct MIDI device roles

The checked MPC.settings example contains a MidiDevices.Table with separate boolean fields named track, global, sync, control and ignoreAuto. It also contains MidiDevices.AutoEnableForTracks and MidiLearnEnabled.

The same file distinguishes audio-device setup from MIDI device entries and includes multiple MPC controller ports and external MIDI endpoints. It also stores per-surface Q-Link assignment/momentary state and page visibility settings.

**Confidence:** the fields and their role distinctions are present in the inspected settings example. Their exact semantics, defaults and behavior across current MPC versions were not independently exercised by our project. This is a machine-specific MPC X settings dump, not a clean factory schema.

**Product decision:** when generic external MIDI routing becomes an active roadmap item, model device roles explicitly instead of treating every opened input port as interchangeable. Do not copy example device names, IDs, saved paths, serial-like values, Q-Link numbers or other machine-specific settings. Do not change the current Android MIDI bridge or MkII semantic adapter on the basis of this file alone.


### 3.3 Global MIDI Learn and the XMM mapping model

The LiveXplore Wiki has a more detailed, version-specific description of standalone MPC Global MIDI Learn. In the documented workflow, a mapping is created/exported as an XMM file from desktop MPC software or MPC Beats, then selected by the standalone application. The Wiki says the controller must be enabled in MIDI settings with the **Control** role selected. It also describes the feature as undocumented and requiring SSH/configuration changes on the standalone device; this is not a supported public plugin API.

The XMM XML format is useful as a *conceptual mapping model*. The page documents per-control target IDs, MIDI message type, MIDI channel, data value/control number, a mapping behavior and a reverse flag. The author identifies behavior choices including Toggle button, Momentary button, Fixed button, Note, absolute CC, relative CC offset and relative CC two's-complement. The author explicitly says the Mapping_type field is not fully understood; the example distinguishes note, CC and no mapping, but this must not be treated as a fully verified formal specification.

**Future product design lead:** if/when we implement generic learnable MIDI control, the semantic mapping model should be expressive enough to represent source message (Note/CC), channel, control number/note number, target semantic action/parameter, toggle/momentary/fixed behavior, absolute vs relative encoder mode, direction/reverse and value scaling. Keep this as a future generic-controller feature, separate from the hard-coded physical MkII adapter. Do not parse or depend on Akai XMM as our internal project format without a deliberate, tested interoperability feature.

Source: [Global MIDI mapping in standalone mode](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-and-Force-global-midi-mapping-in-standalone-mode-how-to), edited 2022-09-03. This is older, third-party documentation of an undocumented Linux feature; current firmware applicability is not established.

### 3.4 Internal MPC controller MIDI ports are not the Studio MkII

A separate LiveXplore Wiki analysis captures traffic between the **internal control surface inside an MPC Live** and the standalone MPC application. It reports four ALSA ports in that environment: `MPC Public`, `MPC Private`, `MPC MIDI Port A` and `MPC MIDI Port B`. In the author's capture, Public carries specific SysEx traffic including pad LED/color updates, Private carries button/button-LED and pad-press traffic, and A/B are external MIDI jack ports connected by the MPC software.

The page also documents an Akai universal identity request and replies containing MPC-family product IDs, plus a separate SysEx family for internal-controller pad colors and a 128×32 OLED pixel command. These IDs/commands describe that internal MPC controller interface. They are **not** the USB-MIDI identity or SysEx map for the external 2021 MPC Studio MkII. Our MkII's separate public protocol research and 160×80 LCD implementation remain authoritative.

The Wiki's controller-MCU discussion names an STM32F401 but qualifies its clock estimate as probable. That observation is about the internal MPC Live controller hardware, not the Studio MkII. Do not infer the MkII MCU, port split, screen resolution or command bytes from it.

**Product decision:** this page is useful primarily as a warning against mixing hardware layers. The internal standalone MPC controller's Public/Private ALSA split is not a reason to rename or reinterpret the Android Studio MkII public MIDI port. Do not copy the MPC Live product IDs, 128×32 OLED messages, internal controller SysEx, or Linux ALSA port arrangement into the MkII adapter.

Source: [MPC internal controller MIDI aspects](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-internal-controller---Midi-aspects), last edited 2023-11-25. The author says the analysis concerns mainly MPC Live and may apply to other last-generation MPC/Force units; that scope limitation matters.

### 3.5 MPC.settings is persistent host configuration, not a project schema

The Wiki describes `MPC.settings` as a global XML configuration file read at application startup and saved when the configuration page is visited and at shutdown. It includes MIDI port definitions and other configuration. It also documents five folder shortcuts and an example `StandaloneWindowBounds` value of 1280×800.

This adds a useful distinction between persistent application/device preferences and the musical project/song state. It does not mean our project file should serialize device ports or global UI preferences. The five folder shortcuts are a useful small UX pattern for a local-first Browser, but the original paths are per-device and should not be copied. The 1280×800 bound is a value from a particular standalone configuration; it is not a measured Android pixel target, a promise of every device's display size, or a reason to change our landscape geometry.

Source: [MPC settings file properties](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-settings-file-properties), edited 2022-01-19. 

### 3.3 Q-Link and page-state configuration

The settings example contains Q-Link assignments with an associated Momentary flag, alongside visibility entries for Main Mode and Sample Edit areas. This supports the general idea that a performance control can have an assignment and a momentary/latching behavior independent of its physical input transport.

**Product interpretation:** this is relevant to future on-screen performance controls and any future generic assignable controller layer. It does not create a missing Q-Link knob bank on the MPC Studio MkII. The project master prompt remains correct: the 2021 Studio MkII does not have the dedicated physical Q-Link knob bank found on other MPC hardware.

### 3.4 Firmware and image-tool boundaries

MPC-LiveXplore documents two generations of its image utility:
- the older Python mpcimg workflow for older MPC/Force images;
- mpcimg2, a separate C utility for the changed image format beginning with MPC/Force 3.4.

The project README also lists SSH/bootstrap images for specific older MPC/Force firmware versions. Those are research artifacts for particular firmware generations, not a supported update path for our Android application.

The repository describes an autorun workaround for running custom scripts on supported standalone firmware. It is a system-level Linux execution mechanism, not an Akai plugin SDK and not an API for our groovebox. We will not depend on it, package it, or use it as a development requirement.

### 3.5 What we deliberately do not import

The settings file includes machine-specific paths, recent-project records, storage identifiers, device names and other local configuration. Those values are not product requirements and are not copied into our documentation or app.

A comment in the settings file suggests a possible debug-menu setting but explicitly leaves it untested. We do not treat that as a confirmed developer mode or design feature.


### 3.6 MPC 3 first-analysis notes and the Gen2 version trap

The LiveXplore Wiki's MPC 3 first-analysis page (last edited 2026-01-22) adds a useful version boundary. Its Gen1 observations include a move from Buildroot to Yocto, a dedicated image utility, the InMusic `edisksctl` mount utility, libc 2.39, a Linux 6.6.64 PREEMPT_RT kernel and external storage mounted with `noexec`. These are reported observations from the author's investigated Gen1 v3 environment, not requirements for our Android app.

More importantly, the same page describes a **MPC 3.7.0 Gen2** update as a different, uncompressed image containing three Linux partitions, with the main root filesystem in the third partition. It gives an example mount offset of 56,819,712 bytes for that exact 3.7.0 image.

This is a different-version observation. It does **not** prove that the Gen2 3.9.1 image has the same three-partition layout. But it means our old 3.9.1 Gen2 handoff's four-entry/XZ/FIT description cannot be trusted without validating the exact input image's bytes and hash; it might reflect an extraction/carving mistake, a different image/version, or a real format change. The conflicting hashes and extraction failures already recorded in section 6 must be resolved against one known-good 3.9.1 Gen2 artifact.

Source: [MPC 3 first analysis](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-3-first-analysis), edited 2026-01-22. Treat the Gen2 example as a 3.7.0-specific lead, not a 3.9.1 specification.

## 4. Findings from Denon Reverse Engineering

### 4.1 MPC 3.9.1 Gen1 application binary

The analysis identifies the main standalone MPC application at /usr/bin/MPC in the MPC 3.9.1 Gen1 image. The analyzed executable reports version 3.9.1.2 (f3af35cdc95) and is described as a stripped 32-bit little-endian ARM PIE for the RK3288 platform.

The analysis reports approximately 108 MB for the executable, about 73 MB in .text, and 91,459 function entries recovered using ARM exception-index metadata. Ordinary symbols are stripped; the report notes 9,266 surviving dynamic-symbol entries and a .gnu_debuglink section. It warns that default Ghidra auto-analysis misses many functions and that .ARM.exidx is important to reconstruct function boundaries.

**Product relevance:** this is a map of the original embedded application, not code we can link into Android. It provides future research anchors if we need to answer a specific unresolved MPC 3.9 behavior question. We should not start a broad binary-decompilation campaign unless a concrete UI/domain decision is blocked.

### 4.2 Internal framework evidence

The binary analysis reports:
- JUCE symbols/types such as juce::String and juce::File;
- Pure Data symbols and runtime option strings, interpreted by the researchers as an embedded Pd synthesis/DSP component;
- SQLite references associated with project/library storage;
- libcurl and c-ares networking components;
- Sentry-native/Crashpad crash-reporting components.

It explicitly distinguishes this MPC application from a separate Denon application in the same research repository: MPC uses JUCE, not Qt/QML.

**Product decision:** our selected stack remains Android + C++20 + JUCE 9.0.2 + Tracktion Engine + Oboe + Ableton Link. The fact that Akai embedded Pd does not prove Pd is required for MPC-like sound generation, and SQLite references do not prove that every project or sequence is stored in SQLite. No dependency change is justified by these observations.

### 4.3 Runtime device identity is separate from controller MIDI identity

The report describes the standalone MPC application reading its product code from /sys/firmware/devicetree/base/inmusic,product-code. The device tree provides the product code, while the serial-number node is populated at runtime by U-Boot. The app uses libaz0x-info.so for a larger set of system and hardware information.

The report distinguishes the system-unit serial, eMMC serial and SD-card serial. It also identifies a separate serial-number path associated with the control-surface MCU.

**Product decision:** do not confuse a standalone unit's device-tree identity, a control-surface MCU identity and the USB-MIDI identity/port of our external Studio MkII. Our Android app must continue identifying the physical MkII using observed USB-MIDI device/port information and our own protocol adapter, not Linux device-tree product codes.

### 4.4 Device-code table is not the same as the image compatibility list

The analyzed MPC application contains a hardware-type table with 26 entries. The report includes older controller products and entries that are not shipped as device trees in the inspected 3.9.1 image. Examples include historical MPC Studio identifiers and an entry labelled MPC Studio 2.

The same report warns that the app's recognized product-code table is broader than the device list shipped by that particular firmware image. A code being present in the app does not prove that a standalone firmware image supports that device.

**Product decision:** do not use a firmware hardware ID as a substitute for the Studio MkII's controller-protocol identity. The fact that the binary contains an entry labelled MPC Studio 2 does not prove what our 2021 MkII sends over USB, or that the device is an installable standalone MPC. Our controller-specific protocol authority remains the existing MkII traceability document and physical validation.

### 4.5 Gen1 firmware version boundaries

The reverse-engineering repository describes MPC 2.x as Buildroot-based and MPC 3.x as Yocto/OpenEmbedded-based. For the examined 3.9.1 Gen1 image it reports the az01 distribution at Yocto 5.0.17 (scarthgap).

It also describes the 3.9.1 Gen1 update as one image covering eight standalone hardware IDs, with board-specific device trees differentiating models. The MPC 3.x update container is described as AZ01 and contains a single rootfs partition. The older MPC 2.x update envelope is described separately as a flattened-device-tree format.

**Product relevance:** useful when interpreting research artifacts and deciding whether a firmware observation belongs to the Gen1 software generation. It has no direct bearing on Android package structure or our persistence format.

## 5. What this changes in the MPC MkII Groovebox knowledge base

| Area | Knowledge added | Engineering consequence |
|---|---|---|
| MIDI architecture | Standalone MPC distinguishes Track, Global, Sync and Control roles in a settings example; the Global Learn XMM format includes Note/CC, channel, target, toggle/momentary/fixed and absolute/relative control modes. | Preserve role-separated routing and a sufficiently expressive generic mapping model as future external-MIDI work. Do not expand current scope prematurely or use XMM as our native project format. |
| Internal MPC controller | LiveXplore documents separate Public/Private ports and internal-controller SysEx for MPC Live/related standalone devices. | Explicitly do not transfer these internal ports, product IDs or 128×32 OLED commands to external Studio MkII; use the separate MkII 160×80 protocol traceability. |
| Controller adapter | Generic controllers can be used as secondary control surfaces in Linux MPC experiments. | Reinforces our own semantic adapter boundary; do not import Linux preload hooks or MPC Desktop workflows. |
| Q-Link/performance state | Settings distinguish assignment and Momentary state; XMM mapping records Toggle/Momentary/Fixed and absolute/relative CC behavior. | Useful vocabulary for future on-screen/assignable controls; does not imply physical Q-Link hardware on Studio MkII. |
| Persistent settings | MPC.settings is a device/application-level XML preference store, with MIDI ports and five Browser folder shortcuts. | Keep app preferences separate from musical project serialization. Treat shortcut count as an optional UX reference, not copied per-device paths. |
| App framework | MPC 3.9.1 Gen1 application contains JUCE and embedded Pd; SQLite references are present. | Confirms JUCE is a compatible architectural reference, but leaves Tracktion/Oboe as our chosen engine stack. No Pd or SQLite adoption by inference. |
| Device identity | Runtime product code comes from the standalone device tree; other serials have separate origins. | Keep standalone hardware IDs out of the external MkII USB-MIDI identity path. |
| Binary research method | 32-bit ARM ELF is stripped; .ARM.exidx matters for full function recovery. | Use focused static analysis only when a specific firmware question blocks implementation. |
| Firmware versions | MPC 2.x and 3.x image/build systems differ; 3.9.1 Gen1 is not a universal Gen2 binary analysis. | Tag all firmware evidence by exact image/version/architecture and do not generalize from Gen1 to Gen2. |

### Preserved architecture boundaries

- Android platform and permissions remain in the Android layer.
- USB-MIDI discovery/ports remain in the transport layer.
- Raw MkII protocol values remain in the hardware adapter.
- Semantic controller actions remain separate from musical MIDI notes.
- Domain state remains independent of JUCE/Tracktion and Android classes.
- Realtime audio remains isolated from UI, file I/O and network work.
- The internal project format remains our own format; firmware SQLite strings are not evidence of an import/export format.

## 6. Gen2 research status — do not overstate the evidence

The public Denon Reverse Engineering material identifies a separate MPC-3.9.1-Gen2-update.img counterpart, but its detailed /usr/bin/MPC binary analysis reviewed above is explicitly from the **Gen1** image. It is not a Gen2 executable analysis.

The LiveXplore Wiki also describes a **3.7.0 Gen2** image as an uncompressed container with three Linux partitions and gives a main-rootfs offset of 56,819,712 bytes for that specific image. This is not the 3.9.1 Gen2 image and must not be applied to it blindly. It does show that Gen2 firmware layout can differ by version and strengthens the requirement to hash and parse the exact 3.9.1 Gen2 artifact before accepting our older handoff's four-partition/XZ/FIT interpretation.

Our repository also contains an older Gen2 research handoff on the historical main line (upload commits [9243f999](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/9243f999b2438984131747327a352e54ff947d37), [fcacdd1](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/fcacdd15a7a2d62d1ca88835551aa0f26cc9c4f0), and [764e80d](https://github.com/MiguelDuval/mpc-mk2-groovebox/commit/764e80d68567ef505c1ca72c2e424843ac2eeeb9)). That handoff must be treated as **incomplete and internally inconsistent**, not as a verified Gen2 platform specification:

- The initial handoff says the Gen2 image and several payloads were extracted, but it also records that the rootfs and boot/recovery filesystems were not mounted and that the MPC application binary was not located or analyzed.
- The initial technical report and a later audit record different whole-image SHA-256 values for the nominally same 366,327,168-byte input.
- A later repaired audit records a different, 89,128,960-byte input and extraction stopped at a total-output cap. Its result cannot silently be equated to the earlier full-size image.
- The audit log also records a failed Binwalk extraction attempt and a Python fallback that wrote zero files in the earlier run.

Consequently, the Gen2 handoff's claims about the app framework, audio/MIDI stack, project storage and UI are not independently verified. Do not infer that Gen2 shares the Gen1 32-bit executable, JUCE/Pd composition, partition format or detailed hardware behavior merely because the product version string is 3.9.1.

### Required follow-up before using Gen2 details

1. Obtain one known-good Gen2 image from a traceable source and record its exact byte size and SHA-256 before analysis.
2. Preserve the original read-only; keep all extracted files tied to that hash.
3. Identify and validate the container/partition table using an appropriate parser. Do not rely on entropy or generic Binwalk hits as proof of format.
4. Mount rootfs and boot/recovery filesystems read-only in Linux/WSL2; record filesystem checks and extraction logs.
5. Locate /usr/bin/MPC, inspect its ELF architecture/version/build ID, and inventory linked libraries before drawing framework conclusions.
6. Decode the Gen2 DTBs and compare board/product-code differences with Gen1.
7. Add only repeatable, hash-linked findings to this document and the reference index.

Until those steps are complete, Gen2 details remain an open research item rather than an input to implementation.

## 7. Implementation posture and next steps

**No changes to the current implementation roadmap are required.** This research supports later decisions and does not outrank current CI blockers or the active acceptance queue.

When external MIDI routing reaches its roadmap stage, design and test role-specific routing for Track Input, Global/Control and Sync separately from the dedicated MkII control-surface adapter. A role should be explicit and persisted only after the behavior and migration contract are designed. For user-mappable generic controllers, use the XMM research only as a capability checklist (Note/CC, channel, target, momentary/toggle, absolute/relative, reverse); implement our own typed schema and tests. Do not port the standalone MPC internal-controller Public/Private ports or their SysEx.

When a concrete UI/domain question requires more MPC internals, return to the Gen1 binary with a narrow question (for example, a specific Track/Program ownership path or a parameter serialization question), record source offsets and confidence, and confirm the behavior against the official v3.9 manual or device observation. Do not copy executable code, extracted assets, proprietary artwork or firmware binaries.

## 8. Sources and provenance

- TheKikGen Labs, **MPC-LiveXplore**: https://github.com/TheKikGen/MPC-LiveXplore
- TheKikGen Labs Wiki, [Global MIDI mapping / XMM format](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-and-Force-global-midi-mapping-in-standalone-mode-how-to) (2022-09-03).
- TheKikGen Labs Wiki, [MPC internal controller MIDI aspects](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-internal-controller---Midi-aspects) (2023-11-25).
- TheKikGen Labs Wiki, [MPC.settings properties](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-settings-file-properties) (2022-01-19).
- TheKikGen Labs Wiki, [MPC 3 first analysis](https://github.com/TheKikGen/MPC-LiveXplore/wiki/MPC-3-first-analysis) (2026-01-22).
- TheKikGen Labs, **MPC-LiveXplore image maker**: https://github.com/TheKikGen/MPC-LiveXplore/tree/master/imgmaker
- TheKikGen Labs, **MPC-LiveXplore bootstrap** (listed as a related project; not used as an app dependency): https://github.com/TheKikGen/MPC-LiveXplore-bootstrap
- DeathCamel58, **denon-reverse-engineering**, MPC 3.9.1 Gen1 analysis at revision 0f7966516609e3de2b71e4aef21625bd8d94c57c: https://github.com/DeathCamel58/denon-reverse-engineering/tree/0f7966516609e3de2b71e4aef21625bd8d94c57c
- Existing project-local Gen1 report: [MPC 3.9.1 Gen1 Firmware — Offline Static Analysis](MPC-3.9.1-GEN1-FIRMWARE-STATIC-ANALYSIS.md)
- Existing controller protocol authority: [MPC Studio MkII Reverse-Engineering Traceability](../MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md)
- Existing UI behavior and visuals authority: [MPC 3.9 Reference Index](../MPC3-REFERENCE-INDEX.md), [MPC 3.9 User Action Catalog](../MPC3-USER-ACTION-CATALOG.md), and the official MPC Standalone OS 3.9 manual.

---
**Status:** research integrated into project documentation; Gen1 static findings remain secondary evidence; Gen2 binary/app analysis is not yet complete; no runtime code or physical-hardware behavior was changed or reclassified.
