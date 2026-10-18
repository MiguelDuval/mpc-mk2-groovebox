# MPC Studio MkII — Reverse-Engineering Traceability

**Purpose:** A traceable, controller-first knowledge base for using the physical Akai MPC Studio MkII with our own Android groovebox.  
**Reviewed:** 2026-10-18  
**Target:** The 2021 MPC Studio MkII controller and its USB-MIDI / feedback protocol.  
**Not a target:** MPC Desktop application, its menus, windows, plugins, licensing, accounts, sound stores, DAW integrations or UI navigation.

Read this with [HARDWARE-MPC-STUDIO-MK2.md](HARDWARE-MPC-STUDIO-MK2.md), [MPC-STUDIO-MKII-SEMANTIC-MAP.md](MPC-STUDIO-MKII-SEMANTIC-MAP.md), [MIDI-TRANSPORT.md](MIDI-TRANSPORT.md) and [PHYSICAL-TEST-CHECKLIST.md](PHYSICAL-TEST-CHECKLIST.md).

## 1. Scope firewall: what we extract

The controller is an input/output device, not the application we are building. From hardware documents, support pages, tutorial videos and public research, retain only facts useful to our own host implementation:

- Physical surface inventory and button labels.
- Incoming USB-MIDI messages, channels, values, press/release behavior and pad numbering.
- Incoming velocity/aftertouch/jog/Touch Strip data.
- Outgoing button LED, pad RGB, Touch Strip LED and LCD/SysEx behavior.
- Hardware connection, power, MIDI-port and generic troubleshooting facts that transfer to a custom Android host.
- Physical button gestures printed/documented for the controller, captured as candidate intent to map into our own semantics.

Do **not** import or document as product requirements: MPC Desktop screen flows, desktop menus/preferences, software activation/licensing, account registration, AU/VST/AAX/plugin hosting, desktop window behavior, desktop drivers as a prerequisite for Android, desktop browser/content navigation, MPC desktop sound packs or expansion redemption, or DAW-specific button mappings. In mixed Akai support articles, retain only a separable hardware fact and label the excluded material as out of scope.

MPC Desktop terminology may appear in an upstream source because that source was tested with it. It is not evidence that our app should copy that workflow. Every supported physical action must route into our own semantic command/state model.

## 2. Evidence levels

| Level | Meaning | Permitted use |
|---|---|---|
| **A — physical observation** | Captured on a real MPC Studio MkII with build, port, raw bytes and visible/audible outcome logged. | Final authority for the specific behavior tested. |
| **B — manufacturer-authored hardware source** | Akai quickstart/support content describing this controller or a physical control. | Strong for inventory and intended control concepts; not raw protocol proof. |
| **C — independent protocol research** | Public reverse engineering, MIDI/SysEx tables and proof-of-concept senders. | Main source for candidate MIDI bytes/protocol formats; preserve physical-verification boundary. |
| **D — implementation/test evidence** | Our adapter source, unit/native tests, renderer tests and CI runs. | Proves what our code does for tested inputs/outputs, not automatically what hardware does. |
| **E — tutorial/demo inference** | Video close-up or narration where host software may also be visible. | Use only clear controller evidence; never infer our app's routes from desktop UI. |

Confidence belongs to a behavior, not an entire document. CONFIRMED means that exact behavior has a physical test record. PROBABLE means sources agree but physical proof is missing. UNCONFIRMED means a live behavior or edge case remains unknown.

## 3. Source register

### R-01 — Public MIDI / SysEx chart repository

**Repository:** [bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts)  
**Reviewed files:** [README.md](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts/blob/main/README.md), [Go LCD proof of concept](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts/blob/main/go-lcd-poc/main.go), [Go POC control annotations](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts/blob/main/go-lcd-poc/controls.yaml), [Python LCD proof of concept](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts/blob/main/python-lcd-poc/main.py), and [JavaScript LCD proof of concept](https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts/blob/main/lcd-poc/index.js). The upstream default branch is `main`; use current branch URLs, not the old `master` path.  
**Evidence:** Independent, non-manufacturer protocol research based on observation and sending MIDI.  
**Retained:** button-note/LED-CC table, pad-note mapping, Jog Wheel, Touch Strip, RGB pad SysEx and LCD chunking/encoding. The Go, Python and JavaScript LCD proof-of-concepts provide independent encoders and six-region transmission examples; `python-lcd-poc/png.py` is a generic third-party PNG helper, not controller protocol evidence.  
**Excluded:** the author's host-specific MPC Software / Ableton workflow. We use protocol facts, not copied source code. Code examples are corroborating experiments, not a license to copy implementation.

### R-01a — Binary LCD capture and image-fixture inventory

The upstream repository default branch is `main` (not `master`); the source links above use the current branch. Its `sysex/` directory contains **72 binary messages**: 60 files named as six-chunk groups `a1.syx` through `j6.syx`, plus 12 numeric `.sysex` files. Its `png/` directory contains 72 same-stem PNG fixtures. Representative byte inspections show the LCD message prefix `F0 47 7F 4A 04`, followed by size/flag fields, chunk coordinates and a seven-bit-safe payload beginning with the encoded PNG signature. Numeric captures `1.sysex`–`6.sysex` contain the six distinct chunk coordinates (0,0), (0,60), (60,0), (60,60), (120,0), (120,60); captures `7.sysex`–`12.sysex` repeat the same coordinates for another frame. Representative `a1.syx`, `a2.syx`, `j1.syx` and `j6.syx` show the same LCD transport family.

**Audit boundary:** inventory and representative headers/payload prefixes were inspected; all 72 payloads were **not** individually decoded or reassembled into image fixtures. These are useful test references, not exhaustive binary reverse-engineering. They remain external-source evidence and were not copied into our product repository.

**Important mapping conflict:** the public README's Button Note table identifies Mode as Note **114**, matching our current control map and semantic adapter. But the upstream `go-lcd-poc/controls.yaml` labels Mode with `data1: 1`. This is an unresolved cross-source discrepancy, plausibly a stale/incorrect POC annotation, not a fact to silently normalize. Keep the current 114 mapping as the best-supported candidate for now, log actual incoming status/note from the real controller, and resolve the discrepancy in a physical test before declaring it CONFIRMED.

### R-02 — Akai Professional MPC Studio MkII FAQ

[Akai Pro MPC Studio MKII FAQ](https://support.akaipro.com/en/support/solutions/articles/69000799777-akai-pro-mpc-studio-mkii-frequently-asked-questions)

Hardware facts retained:
- The unit is a USB MIDI controller and is bus-powered over USB.
- It does not operate as a general audio interface; our app must use the phone's supported audio path or a separate audio interface.
- It has MIDI In and MIDI Out through two 1/8-inch TRS-to-5-pin breakout cables. These physical sockets are different from USB-MIDI.
- The Touch Strip is an assignable real-time control surface; Akai gives effects, pitch bend, modulation, sustain/pitch and key project/mixing parameters as examples. Data Dial changes the selected Touch Strip setting.

Desktop installer, authorization, operating-system, plug-in, included sound pack and account/license instructions in the FAQ are intentionally excluded. The FAQ's intended Touch Strip targets do not require us to reproduce an MPC Desktop page or effect.

### R-03 — Akai Professional MPC Studio MkII Troubleshooting Guide

[Official controller troubleshooting article](https://support.akaipro.com/en/support/solutions/articles/69000800207-akai-pro-mpc-studio-mkii-troubleshooting-guide)

Retained: Akai explicitly identifies the unit as a MIDI controller and links to generic USB-MIDI troubleshooting and MIDI-monitor diagnostics. These suggest an evidence-driven connection test: device discovery, port enumeration, known physical input and inspection of the outgoing response.

Excluded: MPC Desktop version matching, its installer bundle, account/activation and desktop firmware-updater walkthrough. Do not infer that our Android host requires a desktop driver; test Android USB-MIDI enumeration directly.

### R-04 — Akai-authored MPC Studio MkII Quickstart Guide (mirrored copy)

[Quickstart PDF mirror](https://imagescdn.juno.co.uk/manual/846279-01U.pdf)

The document is manufacturer-authored and specific to the MkII, but this available URL is a third-party mirror rather than an Akai-hosted download. Use it for physical control names, hardware inventory and printed modifier/gesture hints with confidence B, then use the actual device to settle ambiguous behavior.

Useful hardware-level cues include Data Dial as navigation/field adjustment/commit, +/- as fine field adjustment, Shift as a secondary-function modifier, Mode+Pad shortcuts, bank selection, Full/Half Level, Note Repeat hold/latch, Touch Strip modes, Sample Select/layer selection, Sample Start/End/Tune, Quantize, Timing Correct, Zoom, Mute, transport, Step/Bar navigation, Locate/markers and Erase. These are intent hints, **not** prescribed destinations or screen flows for our app. Map each one to our own semantic action only when supported by our source and tests.

### R-05 — Official Akai controller tutorial resources

Akai's [MPC Studio MkII FAQ](https://support.akaipro.com/en/support/solutions/articles/69000799777-akai-pro-mpc-studio-mkii-frequently-asked-questions) links these video resources:
- [MPC Studio Academy playlist](https://www.youtube.com/playlist?list=PLyawkFIWnO1PkLpXAN1cciuvoXbrt7M9U)
- [MPC Studio Masterclass playlist](https://www.youtube.com/playlist?list=PLyawkFIWnO1O6ipwAPUWnZgA5wxp2g0fL)
- [MPC Studio Masterclass Full — Akai Professional](https://www.youtube.com/watch?v=TTx7yxS_le4) (published 2021-09-21). Its public description characterizes it as an end-to-end music-making walkthrough (sound selection, drums/melodies, export/share), so those host-dependent procedures are **not** requirements for our app. Only an isolated physical control gesture or controller response could be used as hardware evidence.

**Review status:** the playlist links were verified on Akai's official FAQ, and the individual Masterclass video metadata/description was checked. The hosted playlist/video pages did not expose usable full transcripts in the current research pass; these resources are catalogued but are **not claimed as fully transcript-audited**. No button gesture or controller behavior is attributed to a video without a reviewed timestamp. Do not invent tutorial findings.

For future manual review, retain only physical actions that are visibly or verbally clear: named control, press/hold/release/modifier gesture, physical pad/bank selection and observable controller LED/LCD response. Ignore software windows, desktop menu sequences, software-specific routing and host-dependent navigation. Record video title, timestamp, URL, exact physical observation, confidence and whether it agrees with protocol data. If a tutorial only demonstrates an effect inside MPC Desktop and the physical controller event cannot be isolated, mark it REFERENCE ONLY — host-dependent and do not use it to define our app.

### R-06 — Secondary practical mapping source

[gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script](https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script)

This is an Ableton-specific integration. Keep it only as an optional secondary cross-check when an actual physical note/CC or modifier is unclear. **Do not import its Ableton commands, DAW arrangement, page navigation or application workflow** into this project's documentation or code. Never let it overrule the manufacturer hardware reference, primary protocol charts or physical capture.

## 4. Controller transport and device model

The MkII is host-driven: external software interprets incoming messages and sends back LED/LCD feedback. Our product supplies that host behavior itself.

| Property | Current understanding | Confidence / caveat |
|---|---|---|
| USB connection | USB 2.0 data connection; bus-powered according to Akai FAQ. | Manufacturer source. Verify Android OTG/USB-C adapter and permission path on the target phone. |
| USB MIDI device | Current app enumerates Android MIDI devices, prefers a device identified as MPC Studio MkII, prefers a port containing “public”, and opens one input and one output port. | Source implementation; physical host enumeration may vary. |
| Public port | Primary reverse-engineering repository says to use the Public port for custom controls. | Independent source; check actual Android port names. Desktop private-port details are excluded. |
| Button channel | MIDI Channel 1 = zero-based channel nibble 0. | Public chart; physical spot checks are recorded for an older build. |
| Pad channel | MIDI Channel 10 = zero-based channel nibble 9. | Public chart; current decoder test verifies this mapping. |
| Button press/release | Note On velocity 127 indicates pressed; Note Off or Note On velocity 0 indicates released. | Public chart and decoder tests; physical edge cases still need current-build spot checks. |
| Pad input | Non-sequential Note numbers; velocity matters; polyphonic aftertouch is decoded. | Mapping tests exist. Physical pressure range, zero-velocity behavior and feedback interactions are separate acceptance items. |
| Output feedback | Host sends CC and SysEx for button LEDs, pad RGB, Touch Strip LEDs and LCD. | Encoding tests exist. Physical feedback needs per-function tests. |

### Channel-validation discrepancy to resolve

The mapping discrepancy in R-01a is a separate open item: the README/current app say Mode Note 114, while the Go POC YAML says Note 1. Do not silently merge these values; obtain a raw physical capture of Mode press/release and preserve the evidence in the hardware test log.



The source charts specify Channel 1 for buttons and several controls. The current decoder enforces channel nibble 0 for button notes, Jog press and Touch Strip touch Note 78, and channel nibble 9 for pads/aftertouch. However, its CC branches currently accept CC 100 (Jog Wheel) and CC 33 (Touch Strip slide) **without checking the MIDI channel**. This may be intentional tolerance, but differs from a strictly channel-specific source interpretation. Capture these controls on the actual unit, log the raw status byte, then document the intended tolerance or add a focused channel-policy test. Do not silently treat this as settled.

## 5. Complete input control map

This table mirrors the current project control map. Button identifiers and LED CC numbers belong in the controller map, not scattered through app/domain code. The Note column is a MIDI note number, not a pitch name.

| Physical control | MIDI Note / input | Feedback LED CC | Current app-level meaning / scope |
|---|---:|---:|---|
| Touch Strip mode button | 0 | 0 | Cycle Touch Strip semantic mode. |
| Pad Mute | 4 | 4 | Enter Pad Mute context; pad presses become targets. |
| Erase | 9 | 9 | Stateful Erase context; pad presses select targets in the supported workflow. |
| Note Repeat | 11 | 11 | Momentary Note Repeat; Shift+press toggles latch. |
| Quantize | 12 | 12 | Quantize in supported selection/context. |
| Track Select | 13 | 13 | Track selection; Shift variant selects Sequence. |
| Program Select | 14 | 14 | Program selection; Shift variant is Track Type selection, a known routing gap. |
| Timing Correct On/Off | 15 | 15 | Timing Correct recorder policy; Shift config only where supported. |
| Sample Start / Loop Start | 33 | 33 | Focus sample-region start for current layer. |
| Sample End | 34 | 34 | Focus sample-region end. |
| Pad Bank A/E | 35 | 35 | Select bank A or Shift bank E. |
| Pad Bank B/F | 36 | 36 | Select bank B or Shift bank F. |
| Pad Bank C/G | 37 | 37 | Select bank C or Shift bank G. |
| Pad Bank D/H | 38 | 38 | Select bank D or Shift bank H. |
| Full Level / Half Level | 39 | 39 | Full Level sets velocity 127; Shift toggles Half Level (64). |
| 16 Level | 40 | 40 | Reinterpret pads as velocity values for the selected source pad where supported. |
| Sample Select | 42 | 42 | Focus sample/layer selection; eight-layer model exists, while physical guide mentions first four layer targets. |
| Shift | 49 | 49 | Stateful modifier, not an independent page. |
| Browse | 50 | 50 | Navigate to our Browser; Shift+Browse means Browser Up where supported. |
| Main | 52 | 52 | Navigate Main; Shift+Main enters Track View context. |
| Tap Tempo / Master | 53 | 53 | Tap Tempo; shifted Master-Tempo action only if supported. |
| Plus | 54 | 54 | Increment focused value/selector. |
| Minus | 55 | 55 | Decrement focused value/selector. |
| Zoom | 66 | 66 | Contextual zoom; Shift selects a second axis where implemented. |
| Undo / Shift+Undo | 67 | 67 | Undo / Redo, within implemented edit-history coverage. |
| Step left | 68 | 68 | Move stopped edit/playhead position one step in supported contexts. Not the sequencer event's signed Nudge parameter. |
| Step right | 69 | 69 | Move one edit/playhead step. |
| Locate | 70 | 70 | Locator context; implementation uses a local hold threshold. |
| Bar left | 71 | 71 | Move by bar in supported stopped navigation context. |
| Bar right | 72 | 72 | Move by bar in supported stopped navigation context. |
| Record | 73 | 73 | Arm recording using native recorder/transport. |
| Automation Read/Write | 75 | 75 | Decoded/reserved; full automation domain not implemented. |
| Tune / Fine | 79 | 79 | Focus Pad/Layer tuning; exact modifier/layer cycling needs physical test. |
| Overdub | 80 | 80 | Supported overdub behavior through the existing recorder. |
| Stop | 81 | 81 | Stop; double-stop/Shift-stop only where implemented and tested. |
| Play | 82 | 82 | Start/resume from current playhead. |
| Play Start | 83 | 83 | Start from sequence beginning. |
| Mode | 114 | 114 | Stateful shortcut modifier: Mode+Pad launches a supported app context, not an MPC Desktop page. |
| Copy / Shift+Copy | 122 | 122 | Copy/Delete pad gesture with explicit targets and safe commit. |

The Jog Wheel's rotation is CC 100, and its press is Note 111; it does not have a button LED CC row in the public chart.

### Physical pad note map

The bottom-left physical pad is Pad 1. Physical pad index is zero-based in code, but user-facing labels are 1–16. MIDI Note values are not sequential with visual pad number.

| User-visible pad | Zero-based physical index | MIDI Note | Channel |
|---:|---:|---:|---:|
| 1 | 0 | 37 | 10 (nibble 9) |
| 2 | 1 | 36 | 10 (nibble 9) |
| 3 | 2 | 42 | 10 (nibble 9) |
| 4 | 3 | 82 | 10 (nibble 9) |
| 5 | 4 | 40 | 10 (nibble 9) |
| 6 | 5 | 38 | 10 (nibble 9) |
| 7 | 6 | 46 | 10 (nibble 9) |
| 8 | 7 | 44 | 10 (nibble 9) |
| 9 | 8 | 48 | 10 (nibble 9) |
| 10 | 9 | 47 | 10 (nibble 9) |
| 11 | 10 | 45 | 10 (nibble 9) |
| 12 | 11 | 43 | 10 (nibble 9) |
| 13 | 12 | 49 | 10 (nibble 9) |
| 14 | 13 | 55 | 10 (nibble 9) |
| 15 | 14 | 51 | 10 (nibble 9) |
| 16 | 15 | 53 | 10 (nibble 9) |

## 6. Incoming message families

| Message family | Public research / code map | Code path | Test and open boundary |
|---|---|---|---|
| Buttons | Note numbers above, channel nibble 0; press/release by non-zero/zero velocity. | Input decoder → semantic adapter. | Native decoder and adapter tests exist. Re-test high-frequency controls on current APK. |
| Pads | Channel nibble 9; explicitly mapped note; Note On velocity 0 means release. | Input decoder + control map. | Native tests cover map, velocity and release. Physical note/velocity tests recorded on older APK; repeat where behavior changed. |
| Pad pressure | Polyphonic pressure, status 0xA0, channel nibble 9, maps note to pad index. | Input decoder. | Native test verifies decode. Pressure response/curve in the audio path needs a separate test. |
| Jog rotation | CC 100; public chart reports values 1 and 127 as two direction values. | Decoder emits JogWheel; adapter converts it into context-aware Data Dial delta. | Unit tests cover one representative value. Verify actual direction/value stream and channel physically. |
| Jog press | Note 111 on channel nibble 0. | Decoder emits JogPress; adapter emits a commit/Enter-like semantic action. | Unit test covers press; physical click/context test remains needed. |
| Touch Strip touch | Note 78 press/release on channel nibble 0 per source chart; source says touch Note behavior may depend on host setting. | Decoder emits TouchStripTouch; adapter tracks touched state. | Decoder/adapter tests exist. Check actual touch/release events on device. |
| Touch Strip position | CC 33 treated as a seven-bit value 0–127. | Decoder emits TouchStrip; adapter maps to current context or Note Repeat division. | Native tests cover representative values. Confirm channel and endpoint values physically. |
| Channel aftertouch | Generic MIDI channel pressure 0xD0 accepted by decoder. | InputEventType::ChannelAftertouch. | Generic MIDI handling is not evidence that this controller uses channel pressure for a specific surface. Do not conflate with pad poly pressure. |

## 7. Outgoing feedback protocol

### 7.1 Button LEDs

The public chart reports button LED CC equal to its Note number (except Jog). The host must keep LEDs in sync with semantic app state, not merely echo physical presses.

- **Single-colour LED:** 0 off, 1 dim, 2 full.
- **Two-colour LED:** 0 off, 1 colour 1 dim, 2 colour 2 dim, 3 colour 1 full, 4 colour 2 full.

Current policy lives in [MpcHardwareFeedbackPolicy.java](../android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcHardwareFeedbackPolicy.java); outgoing CC/state caching are in MainActivity and protocol helpers. [MpcHardwareFeedbackPolicyTest.java](../android/app/src/test/java/com/miguelduval/mpcmk2groovebox/MpcHardwareFeedbackPolicyTest.java) tests representative single-/two-colour cases. It does not prove a full physical LED matrix.

### 7.2 RGB pad SysEx

The current native builder emits this 12-byte packet:

**F0 47 47 4A 65 00 04 <pad-index> <R> <G> <B> F7**

Pad index is zero-based (Pad 1 = 0); RGB values are MIDI-safe data bytes. The builder is in [MpcStudioMk2LedProtocol.cpp](../src/MPC/MpcStudioMk2LedProtocol.cpp); exact packet assertion is in [MpcStudioMk2InputDecoderTests.cpp](../tests/MpcStudioMk2InputDecoderTests.cpp). The UI uses RGB feedback for selected/step/launcher contexts. The physical checklist confirms a subset of RGB behavior for an earlier build; broader state-to-colour policy remains to be verified.

### 7.3 Touch Strip and Note Repeat indicator LEDs

- Touch Strip indicator CCs: **57–65** (nine segments).
- Note Repeat division indicator CCs: **103–110** (eight divisions).
- Brightness is seven-bit: **0–127**.

The upstream table has malformed Note Repeat rows displaying maximum values 128–134. Those are invalid MIDI CC data values; do not reproduce the upstream typo. Our outgoing builders constrain values to seven-bit values; native tests cover representative messages and index boundaries. Physical segment/brightness response still requires testing.

### 7.4 160 × 80 LCD image transport

The controller LCD is 160 × 80. The independent Go and JavaScript proof-of-concepts and current native implementation agree on six image regions:

| Chunk | X | Y | Width | Height |
|---:|---:|---:|---:|---:|
| 1 | 0 | 0 | 60 | 60 |
| 2 | 0 | 60 | 60 | 20 |
| 3 | 60 | 0 | 60 | 60 |
| 4 | 60 | 60 | 60 | 20 |
| 5 | 120 | 0 | 40 | 60 |
| 6 | 120 | 60 | 40 | 20 |

Each chunk is a PNG. The encoded stream packs each group of up to seven original bytes behind a control byte whose bits record source high bits. The SysEx packet begins **F0 47 7F 4A 04**, then size/flag metadata, x/y origin and PNG-size metadata, payload and final **F7**. Use our own makeLcdChunkSysEx implementation rather than copying an upstream proof of concept.

**Current app mapping:** [MpcStudioMk2LcdRenderer.java](../android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcStudioMk2LcdRenderer.java) produces an original compact status/context display (page, transport, sequence/track, tempo/time signature, bar/beat, selected pad/layer, queued sequence and status). [MpcStudioMk2MidiMessages.java](../android/app/src/main/java/com/miguelduval/mpcmk2groovebox/MpcStudioMk2MidiMessages.java) renders the 160×80 frame and encodes six chunks; MainActivity synchronizes state and avoids redundant sends using a signature cache.

**Test boundary:** native tests validate data packing, packet header, 7-bit safety and PNG-size boundaries. The Java renderer test checks state signature, not full end-to-end wire transmission. The physical checklist records a static test frame on an older build; the current product renderer's whole state/refresh behavior has not been comprehensively physically accepted.

## 8. Implementation and test traceability

| Concern | Current implementation | Automated evidence | What it does not prove |
|---|---|---|---|
| Pad/button note map | src/MPC/MpcStudioMk2ControlMap.h | tests/MpcStudioMk2InputDecoderTests.cpp | Live behavior for every control on current APK. |
| MIDI decode, press/release, velocity, pressure, Jog, Touch Strip | src/MPC/MpcStudioMk2InputDecoder.cpp | Native input decoder tests | Exact live channel/value stream beyond tested examples. |
| Stateful Shift/Mode/Locate/Note Repeat/Full/Half Level/Mute/Pad Bank | src/MPC/MpcStudioMk2SemanticAdapter.cpp | tests/MpcStudioMk2SemanticAdapterTests.cpp | All gesture timing/combinations physically. |
| Button LED policy | MpcHardwareFeedbackPolicy.java, MainActivity.java | MpcHardwareFeedbackPolicyTest.java | All actual button LED types, colours and brightness response. |
| RGB pad SysEx | src/MPC/MpcStudioMk2LedProtocol.cpp | Exact packet assertion in input decoder tests | Correct state-to-colour semantics in every mode. |
| Touch Strip / Note Repeat indicator CC | Native LED protocol + MpcStudioMk2MidiMessages.java | Native CC builders + feedback policy tests | All segments and brightness on physical device. |
| LCD renderer | MpcStudioMk2LcdRenderer.java, MpcStudioMk2MidiMessages.java, MainActivity | Native packet/encoding tests; Java state-signature test | End-to-end pixels/content refresh on the current hardware build. |
| Android MIDI lifecycle | docs/MIDI-TRANSPORT.md, current MainActivity | App tests/runtime diagnostics where available | Permission/OTG/device enumeration on every Android phone. |
| Physical acceptance | docs/PHYSICAL-TEST-CHECKLIST.md | Owner record on a named older APK build | Behavior not listed in that record or all behavior on newer APKs. |

### Existing documentation mismatch

Older paragraphs in HARDWARE-MPC-STUDIO-MK2.md and the semantic map claim that only pads are working inputs and other controls are future bring-up work. This is stale relative to current source: the decoder and semantic adapter route many non-pad commands, and MainActivity has feedback hooks and an LCD context renderer. This does **not** mean every semantic command or feedback mode is complete; verify each control individually.

## 9. Hardware gesture hints — not desktop UI requirements

Akai's Quickstart supports these controller-level cues. They help design our own interaction model, but each route must terminate in our app's commands/state.

- Shift is a stateful secondary-function modifier.
- Mode+Pad is a direct context shortcut; map only contexts the app actually supports.
- +/- and Data Dial act on the focused selection/value.
- Pad Bank buttons select A–D and alternate E–H through the documented modifier gesture; follow our adapter's tested convention and don't invent an extra gesture without a test.
- Full/Half Level are performance velocity modifiers, not UI-only toggles.
- Note Repeat may be momentary while held or latched with Shift; rate selection belongs to our native timing policy, not Android frame timing.
- Touch Strip is an assignable continuous performance input; our app chooses local contextual mappings only where backed by state and setters.
- Locate uses pads for marker set/jump; our sequence domain owns marker state.
- Step/Bar navigation is distinct from the event's signed Nudge/microtiming parameter.
- Erase, Copy and Undo/Redo need explicit target/commit rules and safe native edit history.

Never infer that every MPC Desktop operation must exist in our app. A documented hardware gesture may be implemented differently, marked unavailable, or reserved if our backend contract does not yet exist.

## 10. Physical validation queue

The physical checklist contains an owner report dated **2026-09-24** for APK commit 005c4bc2a129b1ffb3659deafad21ead23754350. It reports device discovery, incoming pad/button/transport, Jog/Touch Strip, basic pad RGB, simple button/indicator feedback and a static LCD frame. This historical record remains tied to that build; it does not imply that later code at current HEAD was physically retested.

Keep these gaps visible:
1. Capture raw channel/value pairs for Jog CC 100 and Touch Strip CC 33; verify Note 78 touch/release.
2. Test Mode+Pad / Shift / Locate / Note Repeat gesture timing, including release, latching and interrupted chords.
3. Ensure transport, modifier and contextual LEDs mirror semantic state after navigation, cancellation, stop, error and reconnect—not merely after immediate presses.
4. Verify Pad RGB follows the correct physical pad and mode, including zero-velocity release; do not assume pad trigger feedback and explicit RGB are the same mechanism.
5. Send a content-changing LCD frame through the current renderer. Verify all six chunks, legibility, colour, partial-frame replacement, repeated-frame suppression, reconnect/resume and absence of corruption.
6. Test USB disconnect/reconnect and Android app restart; stale latched modes must be cleared or intentionally restored.
7. Test all nine Touch Strip segments and eight Note Repeat LEDs individually at brightness 0 and 127.
8. Test MIDI In/Out TRS only if our app supports external MIDI devices; this is separate from USB MIDI controller communication.

For each physical test record date, controller identity, Android/OTG path, build SHA, port name, exact gesture, raw bytes, expected/observed semantic action, outgoing bytes and photo/video where useful. Do not promote PROBABLE to CONFIRMED because an online table or unit test passed.

## 11. Research maintenance

- When a protocol fact changes, update this traceability map, control map/semantic map and corresponding test.
- Add per-action physical results rather than a vague overall “hardware working” checkbox.
- If a video is used, record its title, URL and timestamp plus the exact hardware observation; do not import host-software workflow.
- Avoid copying upstream source code, assets, screenshots or proprietary firmware resources; record facts and implement original code.
- The official tutorial playlists are indexed for future selective review, but transcripts were not fully audited in the 2026-10-18 pass. Do not claim tutorial support without a specific clip/timestamp actually reviewed.
