# MPC 3.x Standalone — UI Visual Reference Catalog

**Research checkpoint:** 2026-10-29  
**Product target:** Akai MPC One (Gen 1) running MPC Standalone OS 3.9; this repository's Android landscape interface.  
**Purpose:** a visual companion to `MPC3-9-OS-KNOWLEDGE-BASE.md`, `MPC3-UI-SCREEN-BLUEPRINT.md`, and `MPC3-UI-MIGRATION-MASTER-SPEC.md`. The catalog is meant to prevent implementation from being driven only by memory, source-code convenience, or isolated test cases.

## How to use this catalog

1. **Use the official MPC Standalone OS v3.9 User Guide first** for screen purpose, controls, navigation and version-specific behavior: [official v3.9 guide PDF](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf).
2. **Use official Akai support screenshots as the primary visual references** where present below. These are externally hosted images embedded from their source pages, not copied into the application.
3. **Treat third-party screenshots as supplementary evidence.** They may show MPC 3.4/3.5 or an older OS, or a different MPC model. A different-version screenshot is useful for understanding the workflow, but it must not override v3.9 documentation for geometry, control placement, labels or behavior.
4. **Do not silently mark an unillustrated screen as implemented.** The cross-reference below records every context listed in the repository's screen contract and indicates whether this catalog has a directly embedded screenshot or a manual/source reference only.
5. These are references to the **original MPC**, not screenshots of our Android app. Use them to build our own implementation; do not include these proprietary screenshots or extracted firmware artwork in the app binary.

### Evidence labels

- **A — Official:** Akai/inMusic documentation or official support image.
- **B — Specialist reference:** public MPC specialist/editorial screenshot. Firmware and device differences are called out.
- **C — Legacy only:** pre-MPC 3 / MPC 2.x-style image; use for broad functional structure only, never as the MPC 3.9 layout authority.
- **Manual-only:** the v3.9 guide is linked at the relevant section by name, but a stand-alone public image URL suitable for inline embedding was not found in this research pass.

---

# 1. Core shell and Main

## 1.1 Main — Sequence + Track/Arrangement + Function Bar

**Evidence A.** Official MPC-series support image showing the MPC 3 Main composition, sequence fields, selected Track, Track/Arrangement area, selected waveform surface and bottom Function Bar. This is the clearest currently found direct image of the MPC 3 Main workspace. It is an MPC 3-series reference, not a guarantee that every pixel corresponds to the Gen 1 MPC One on 3.9.

![MPC 3 Main — sequence, track, arrangement preview and function bar](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903713/original/tG8Q7SJWSKww5E-BtgLHjff7Z1Y0EL7Jkw.png?1764065208=)

Source: [Akai support — Tuning Basics](https://support.akaipro.com/en/support/solutions/articles/69000874731/).

**Implementation observations:** preserve the hierarchy of the sequence section, selected Track identity/type, Track/Arrangement view, selected Pad/Layer waveform surface, compact track-state fields and bottom actions. This image supports the controller-first Main decision: physical pads select the pad; the canonical Main surface is not a permanent Android 4×4 pad matrix.

## 1.2 Main — sibling hardware with a wider MPC 3 display

**Evidence B.** This is an MPC Live III screen and therefore not an exact MPC One hardware capture. Use it only as a cross-device MPC 3-family example of the Main arrangement composition and the common design language.

![MPC 3-family Main arrangement example on MPC Live III](https://dt7v1i9vyp3mf.cloudfront.net/styles/news_large/s3/imagelibrary/A/AkaiMPCLiveIII_04-ajv0NghfxDb0Qn7EjFNP6tIDITCeULIC.jpg)

Source: [Sound On Sound — MPC Live III review](https://www.soundonsound.com/reviews/akai-mpc-live-iii).

## 1.3 XL Channel Strips / I-O routing

**Evidence A.** Official MPC-series support image showing the XL Channel Strip opened on I/O with the output-selection panel. It is a reference for the strip region and track/pad routing context, not a claim that the full Channel Mixer feature set is implemented in our project.

![MPC 3 XL Channel Strip and I-O routing](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154227724/original/W4p-2vkRxIavIh5ZOK9e8ZvP0fZ0iVPVxA.png?1745834845=)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 1.4 Menu / mode launcher

**Evidence B.** MPC 3-family mode launcher. Use it for the basic tile-based launcher concept and mode vocabulary; verify the exact current tile inventory and dimensions against the v3.9 guide.

![MPC 3 mode menu](https://www.mpc-tutor.com/assets/mpc-menu-1024x639.png)

Source: [MPC-Tutor — MPC buyer's guide](https://www.mpc-tutor.com/mpc-buyers-guide-mpc-one-vs-mpc-live-ii-vs-mpc-x/).

## 1.5 Sequence/Track/Program Main from an older interface — historical comparison only

**Evidence C.** This older visual may help explain the familiar Sequence / Track / Program information blocks, but it is not the target MPC 3.9 composition. Do **not** copy its red header, left navigation rail, or older Program-centric architecture into the canonical UI.

![Legacy MPC Main screen — comparison only, not MPC 3.9 target](https://dt7v1i9vyp3mf.cloudfront.net/styles/news_large/s3/imagelibrary/M/MPCBasics_02-1eS.kAqGJkEsbEBcA_yfgeJUvXCMWXIi.jpg)

Source: [Sound On Sound — Akai MPC Basics](https://www.soundonsound.com/techniques/akai-mpc-basics).

---

## 1.6 Track View — full track overview

**Evidence A.** Official Akai support screenshot of the standalone Track View, showing separate track rows, track type/program fields, MIDI routing, per-track Monitor state, level metering, Mute/Solo and the bottom contextual Function Bar. The screenshot uses a legacy MPC 3-family visual skin, so it is valid for information hierarchy and interaction semantics, not for copying its older colors or pixel geometry into our MPC One / OS 3.9 target.

![Official MPC Standalone Track View — track rows, monitor, level, mute/solo and Function Bar](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69128760784/original/ZxmnIeI0mSiXFQy513lN30sfQ0ai0ObVWg.png?1726736736=)

Source: [Akai support — Input Monitoring Explained](https://support.akaipro.com/en/support/solutions/articles/69000860101-mpc-standalone-input-monitoring-explained).

**Implementation observations:** Track View is a real separate context (Shift+Main), not the Main Track region. Validate track selection, record-arm, Monitor IN/AUTO/MERGE, mute/solo availability, selected row, truthful Audio/Drum/MIDI/instrument semantics, and Back/Main return. Do not confuse controls in this capture with a claim that our current code supports every Track View capability.

# 2. Browser — direct official screenshots

The following images are from Akai support documentation. They show distinct Browser contexts; they must not be conflated into one screen or replaced with fake populated content.

## 2.1 Places / storage locations

![MPC Browser — Places tab and local storage locations](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69177984964/original/iLqdSWSEhgtkBJkc5rRpi5_3L-xx41uWXg.png?1764935456=)

## 2.2 Content categories

![MPC Browser — Content categories](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165719905/original/-BBHK1y9CkqUgmPuiShYZjuXKQpaFT1gFw.png?1754995552=)

## 2.3 Expansions

![MPC Browser — Expansions tab](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165719916/original/LpsBnLwwLU5Antxb5wdSSQK_9pafduji-Q.png?1754995559=)

## 2.4 Sample Assign / project sample pool

![MPC Browser — Sample Assign and sample pool](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165722181/original/w67qMx9X5NaS3yf3_Efh5FUtSujKqosltQ.png?1754997241=)

## 2.5 Browser Options dialog

![MPC Browser Options dialog](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165716730/original/_c1kISWJxQzR99bZd8JmlBS0S0gVwNyBOg.png?1754993450=)

Source for all five Browser images: [Akai support — Understanding the MPC's Browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser).

**Implementation observations:** Places, Content and Expansions are separate navigation contexts; Sample Assign is a distinct sample-pool workflow. Search, file-type filters, favourites, audition and Options are separate controls/states. Where our provider does not support a location, show a truthful unavailable/empty state rather than invented content.

---

# 3. Pad Perform

**Evidence A.** Official Akai support screenshot of Pad Perform in Custom mode with a 4×4 note map, octave control and mode tabs. This is a real separate performance context, not the canonical Main workspace.

![Pad Perform — custom note map and 4×4 pad interpretation](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166324277/original/6NjC6-cRea38vgWsZYANVNmmRnv9QbtgLw.png?1755523845=)

Source: [Akai support — How to Remap the MPC Pads](https://support.akaipro.com/en/support/solutions/articles/69000867733-akai-pro-mpc-series-how-to-remap-the-mpc-pads).

Supplementary Notes-mode reference (older MPC OS visual, use for musical mapping only): [Sound On Sound — Akai MPC Basics, Pad Perform](https://www.soundonsound.com/techniques/akai-mpc-basics?page=2).

**Acceptance rule:** Pad Perform must not be marked complete just because pad labels appear somewhere else in Main. The dedicated Notes/Chords/Chromatic Chords/Progressions/Custom behavior needs its own implementation and acceptance status.

---

# 4. Track Edit and Sample Edit

## 4.1 Track Edit — Samples / Layer waveform

**Evidence A.** Official support image of the dedicated Track Edit Samples tab showing Track/Pad context, waveform, Layers 1–8, sample region and layer parameters.

![Track Edit — sample layers and waveform](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903727/original/-0xrv1nhFLQ1XfkWCRFkoCgZQe0zfjr4LA.png?1764065223=)

Source: [Akai support — Tuning Basics](https://support.akaipro.com/en/support/solutions/articles/69000874731/).

## 4.2 Sample Edit — current MPC 3-family waveform editor

**Evidence B.** MPC 3.4+ specialist reference showing the Sample Edit waveform, start marker, slice/loop controls and bottom Trim/Zoom/Assign/Process actions. This is a close workflow reference but was not captured from a Gen 1 MPC One running v3.9.

![MPC 3 Sample Edit — waveform, slice and process controls](https://www.mpc-tutor.com/assets/slicemotion/slice05.png)

Source: [MPC-Tutor — MPC3 Slice Motion tutorial](https://www.mpc-tutor.com/mpc3-tutorial-slice-motion-vocals/).

## 4.3 Track Edit plugin / keygroup context

Official context reference: [Akai support — Understanding and Loading Programs](https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs). The article illustrates the Track/Program selection workflow and explains entry to Program Edit. Use the official v3.9 guide for the full Program Edit parameter-tab layout.

## 4.4 Automation / envelope controls

**Evidence B.** MPC 3.4+ example showing the pull-down performance status and an envelope parameter being edited. This is supporting evidence for the automation gesture and visual feedback, not a substitute for a dedicated automation/XYFX reference.

![MPC 3 envelope automation and performance status](https://www.mpc-tutor.com/assets/slicemotion/slice24.png)

Source: [MPC-Tutor — MPC3 Slice Motion tutorial](https://www.mpc-tutor.com/mpc3-tutorial-slice-motion-vocals/).

---

# 5. Grid View and Step Sequencer

## 5.1 Grid View — note events and velocity lane

**Evidence C.** This screenshot comes from an older UI style. Use only to understand the relationship between note events and the velocity lane; do not copy its red toolbar or navigation treatment as the MPC 3.9 visual target.

![Legacy Grid View — note grid with velocity lane; visual structure only](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69137425072/original/UbA7URmBzaEGtijRhATbUeKqQJQobByonw.png?1733155392=)

Source: [Akai support — Humanizing Your Tracks](https://support.akaipro.com/en/support/solutions/articles/69000863026-akai-pro-mpc-series-humanizing-your-tracks).

**Current authoritative reference:** official [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), section **Grid View**. Search the guide for “Grid View” and review the current drum-grid, note editing, velocity lane and event-selection screenshots. The official guide is authoritative where the embedded legacy image differs.

## 5.2 Step Sequencer

**Manual-only in this catalog.** The official v3.9 guide is the authority: [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), section **Step Sequencer**. It contains the 16-step workspace, timing/track fields, parameter lane and step controls. An independently hosted direct screenshot URL for the v3.9 Step Sequencer was not recovered in this research pass.

Do not treat a Step Sequencer screen as complete merely because 16 buttons are drawn: track length, bar/pad selection, time division and supported step parameters belong to the screen contract.

---

# 6. Sampler / recording

**Evidence C.** Public standalone-MPC screenshot of the record/monitoring surface. This older visual is useful only for the central recording affordance and state hierarchy; the v3.9 guide remains the visual authority.

![Legacy standalone MPC Sampler recording screen — interaction reference only](https://dt7v1i9vyp3mf.cloudfront.net/styles/news_large/s3/imagelibrary/S/SampleSlicing_02-on3yRqdzNYbySPlbD3x43Dr_yKlI1RKX.jpg)

Source: [Sound On Sound — Sample Slicing: Beatmaking With Hardware](https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware).

**Current authoritative reference:** official [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), sections **Sampler** and **Sampling**. The screen must represent real record/stop/monitor/threshold and assignment state; a cosmetic recording panel is not functional acceptance.

---

# 7. Preferences / Project lifecycle

## 7.1 Project Load/Save preferences

**Evidence B.** Public standalone-MPC preferences screenshot showing the Project Load/Save settings category and Auto Save options. Treat it as a general structure reference; verify exact labels, ordering and current options against OS 3.9.

![Preferences — project load/save and autosave](https://www.mpc-tutor.com/assets/autosave.jpg)

Source: [MPC-Tutor — Backup Strategies for MPC & Force Users](https://www.mpc-tutor.com/backup-strategies-mpc-force/).

## 7.2 Project / Menu Save / New Project

The [MPC 3 menu screenshot above](https://www.mpc-tutor.com/assets/mpc-menu-1024x639.png) provides a visual reference for menu-level project commands, not a complete Project dialog reference.

**Current authoritative reference:** official [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), sections **Project**, **New Project**, **Save**, and **Save As**. The exact file-picker/confirmation dialogs depend on the workflow; the UI must preserve cancellation and loaded-state invariants.

---

# 8. Full screen/context coverage index

This index mirrors the significant contexts in `MPC3-9-OS-KNOWLEDGE-BASE.md`. “Direct image” means an inline image above shows that context. “Manual/reference only” means the context remains indexed to the official guide but this research did not find a suitable independently hosted direct screenshot for embedding. This is intentional: a missing image is recorded as a research gap rather than hidden.

| ID | Screen/context | Visual evidence in this catalog | Authoritative next reference / screen contract |
|---|---|---|---|
| VIS-001 | Main (Sequence + Track/Arrangement) | **Direct image — A** | Official v3.9 guide, Main Mode |
| VIS-002 | Toolbar / transport status | **Visible in Main images — A/B** | Official v3.9 guide, Toolbar |
| VIS-003 | XL Channel Strips / I-O | **Direct image — A** | Official support Output Routing Basics; v3.9 guide, Mixer Strips |
| VIS-004 | Menu / mode launcher | **Direct image — B** | Official v3.9 guide, Menu |
| VIS-005 | Browser — Places | **Direct image — A** | Official support Browser article; v3.9 guide, Browser |
| VIS-006 | Browser — Content | **Direct image — A** | Official support Browser article; v3.9 guide, Browser |
| VIS-007 | Browser — Expansions | **Direct image — A** | Official support Browser article; obey this project's local-first/no upsell decision |
| VIS-008 | Browser — Sample Assign / sample pool | **Direct image — A** | Official support Browser article; native sample assignment is a separate workflow |
| VIS-009 | Browser — Options / filters / audition / favourites | **Direct image for Options; article links for the rest — A** | Official support Browser article; separate each preference and state |
| VIS-010 | Track View | **Direct official image — A (legacy visual skin; use semantics/layout only)** | Official Akai Input Monitoring Explained; current v3.9 guide governs target geometry and labels |
| VIS-011 | Arrangement / linear timeline | **Sibling-device MPC 3 image — B; Main preview image — A** | Official v3.9 guide, Arrangement; use Gen 1 One layout over wider-device geometry |
| VIS-012 | Grid View | **Legacy direct image — C** | Official v3.9 guide, Grid View takes precedence |
| VIS-013 | Step Sequencer | **Manual-only** | Official v3.9 guide, Step Sequencer |
| VIS-014 | Track Edit — Samples/Layers | **Direct image — A** | Official support Tuning Basics; official v3.9 guide, Track Edit |
| VIS-015 | Track Edit — Plugin/Keygroup/instrument | **Program/track entry image plus article — A** | Official support Understanding and Loading Programs; v3.9 guide, Program Edit/Track Edit |
| VIS-016 | Sample Edit — Trim/Chop/Process | **Direct image — B** | MPC-Tutor MPC3+ sample-edit example; official v3.9 guide is authoritative |
| VIS-017 | Sampler — record/monitor/assign | **Legacy direct image — C** | Official v3.9 guide, Sampler/Sampling |
| VIS-018 | Pad Mixer | **XL strip routing reference — A; not a full Pad Mixer screen** | Official v3.9 guide, Pad Mixer; confirm all controls have truthful native support |
| VIS-019 | Channel Mixer | **XL strip/I-O direct image — A; not a full mixer screen** | Official v3.9 guide, Channel Mixer; don't imply full routing/FX capability where backend is reserved |
| VIS-020 | 16 Levels | **Manual-only** | Official v3.9 guide, 16 Levels; requires its own source-pad/parameter/active-state behavior |
| VIS-021 | Pad Perform | **Direct image — A** | Official support Remap the MPC Pads; dedicated context, not Main |
| VIS-022 | Next Sequence | **Manual-only** | Official v3.9 guide, Next Sequence; verify current/queued sequence and launch-state behavior |
| VIS-023 | List Edit | **Manual-only** | Official v3.9 guide, List Edit |
| VIS-024 | Project / New / Open / Save / Save As | **Preferences image plus Menu reference — B** | Official v3.9 guide, Project; exact picker round-trip and cancellation states matter |
| VIS-025 | Preferences | **Direct image — B (older layout)** | Official v3.9 guide, Preferences; use only as a category-map clue |
| VIS-026 | MIDI / Control settings | **Manual-only** | Official v3.9 guide, Audio/MIDI/Sync and controller configuration sections |
| VIS-027 | Looper | **Manual-only** | Official v3.9 guide, Looper |
| VIS-028 | XY / XYFX / performance automation | **Automation visual — B; no direct current XYFX screen** | Official v3.9 guide, XY Pad/XYFX and automation; distinguish controller/macro controls from standalone desktop plugin UI |
| VIS-029 | Sounds / preset browsing | **Track/Program entry image plus official loading guide — A** | Official support Understanding and Loading Programs; product decision still excludes branded cloud/subscription upsell destinations |
| VIS-030 | Dialogs: track type, timing correct, project/file pickers, destructive confirmation | **Not one single screen; see official guide** | Capture/validate each dialog as a state of its owning context; never infer all dialogs from the parent screen |

---

# 9. Explicit remaining visual-research gaps

These items are **not silently considered complete**. The research found official guide coverage, but did not find a directly embeddable, independently hosted screenshot for every one:

- A standalone MPC One Gen 1 / OS 3.9 screenshot of the full Step Sequencer.
- Exact OS 3.9 full-screen Grid View and Track View captures, separate from Main's preview.
- Full-screen Pad Mixer and Channel Mixer captures with the complete parameter/tab surface.
- Dedicated 16 Levels, Next Sequence, Looper, XYFX, List Edit and MIDI/Control screens.
- Every modal/dialog state, including current Project picker and destructive-confirmation variants.

The official v3.9 PDF is linked at the start of this document for each manual-only row. These gaps should be closed by locating a direct official image, recording a screenshot from an actual MPC One/One+ with permission, or explicitly retaining a labelled reference gap. **Do not replace a missing screenshot with an invented mock-up and present it as an original MPC capture.**

## Source and rights note

Images are remotely embedded from their original public source URLs so the repository records readable references and attribution without bundling Akai screenshots or firmware-extracted artwork into the app. If a publisher changes/removes an image, use the source link and official v3.9 manual to refresh this catalog; do not silently substitute an unrelated MPC Desktop image.
