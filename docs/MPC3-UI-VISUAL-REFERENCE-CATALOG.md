# MPC 3.x Standalone — UI Visual Reference Catalog

**Research checkpoint:** 2026-10-29  
**Product target:** Akai MPC One (Gen 1) running MPC Standalone OS 3.9; this repository's Android landscape interface.  
**Purpose:** a visual companion to `MPC3-9-OS-KNOWLEDGE-BASE.md`, `MPC3-UI-SCREEN-BLUEPRINT.md`, and `MPC3-UI-MIGRATION-MASTER-SPEC.md`. The catalog is meant to prevent implementation from being driven only by memory, source-code convenience, or isolated test cases.

**Local screenshot archive:** [Open the 82-context screenshot gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md) · [Inspect image/PDF extraction manifest](MPC3-UI-REFERENCE-ASSETS/manifest.json) · [See archived image files](MPC3-UI-REFERENCE-ASSETS/README.md).

## How to use this catalog

1. **Use the official MPC Standalone OS v3.9 User Guide first** for screen purpose, controls, navigation and version-specific behavior: [official v3.9 guide PDF](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf).
2. **Use official Akai support screenshots as the primary visual references** where present below. Where retrieval succeeded, reference images are vendored locally under MPC3-UI-REFERENCE-ASSETS; source URLs and provenance are recorded in the local gallery manifest. These are documentation-only and are not packaged into the Android app.
3. **Treat third-party screenshots as supplementary evidence.** They may show MPC 3.4/3.5 or an older OS, or a different MPC model. A different-version screenshot is useful for understanding the workflow, but it must not override v3.9 documentation for geometry, control placement, labels or behavior.
4. **Do not silently mark an unillustrated screen as implemented.** The cross-reference below records every context listed in the repository's screen contract and indicates whether this catalog has a directly embedded screenshot or a manual/source reference only.
5. These are references to the **original MPC**, not screenshots of our Android app. Use them to build our own implementation; do not include these proprietary screenshots or extracted firmware artwork in the app binary.

**Additional source with page anchors:** the [MPC hardware user guide v2.11.7 (legacy UI, with individual screen captures)](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf) is linked per context in Section 9. It includes screenshots for older standalone MPC X/Live/One screens such as Main, Grid, Track View, Step Sequencer, Sample Edit, Program Edit, Browser, Sampler, Looper, Pad/Channel Mixer, 16 Levels, Pad Perform, Next Sequence, Song Mode, Q-Link, Project, Pad Color and MIDI Control. Treat these as supplementary visual evidence only, not as MPC 3.9 layout authority.

### Evidence labels

- **A — Official:** Akai/inMusic documentation or official support image.
- **B — Specialist reference:** public MPC specialist/editorial screenshot. Firmware and device differences are called out.
- **C — Legacy only:** pre-MPC 3 / MPC 2.x-style image; use for broad functional structure only, never as the MPC 3.9 layout authority.
- **Manual-only:** the v3.9 guide is linked at the relevant section by name, but a stand-alone public image URL suitable for inline embedding was not found in this research pass.

---

# 1. Core shell and Main

## 1.1 Main — Sequence + Track/Arrangement + Function Bar

**Evidence A.** Official MPC-series support image showing the MPC 3 Main composition, sequence fields, selected Track, Track/Arrangement area, selected waveform surface and bottom Function Bar. This is the clearest currently found direct image of the MPC 3 Main workspace. It is an MPC 3-series reference, not a guarantee that every pixel corresponds to the Gen 1 MPC One on 3.9.

![MPC 3 Main — sequence, track, arrangement preview and function bar](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-main-sequence-track-arrangement-preview-and-function-b-f147ffbaae13.png)

Source: [Akai support — Tuning Basics](https://support.akaipro.com/en/support/solutions/articles/69000874731/).

**Implementation observations:** preserve the hierarchy of the sequence section, selected Track identity/type, Track/Arrangement view, selected Pad/Layer waveform surface, compact track-state fields and bottom actions. This image supports the controller-first Main decision: physical pads select the pad; the canonical Main surface is not a permanent Android 4×4 pad matrix.

## 1.2 Main — sibling hardware with a wider MPC 3 display

**Evidence B.** This is an MPC Live III screen and therefore not an exact MPC One hardware capture. Use it only as a cross-device MPC 3-family example of the Main arrangement composition and the common design language.

![MPC 3-family Main arrangement example on MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-family-main-arrangement-example-on-mpc-live-iii-08350244657b.jpg)

Source: [Sound On Sound — MPC Live III review](https://www.soundonsound.com/reviews/akai-mpc-live-iii).

## 1.3 XL Channel Strips / I-O routing

**Evidence A.** Official MPC-series support image showing the XL Channel Strip opened on I/O with the output-selection panel. It is a reference for the strip region and track/pad routing context, not a claim that the full Channel Mixer feature set is implemented in our project.

![MPC 3 XL Channel Strip and I-O routing](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-xl-channel-strip-and-i-o-routing-e97042beaa09.png)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 1.4 Menu / mode launcher

**Evidence B.** MPC 3-family mode launcher. Use it for the basic tile-based launcher concept and mode vocabulary; verify the exact current tile inventory and dimensions against the v3.9 guide.

![MPC 3 mode menu](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-mode-menu-c6cc42242737.png)

Source: [MPC-Tutor — MPC buyer's guide](https://www.mpc-tutor.com/mpc-buyers-guide-mpc-one-vs-mpc-live-ii-vs-mpc-x/).

## 1.5 Sequence/Track/Program Main from an older interface — historical comparison only

**Evidence C.** This older visual may help explain the familiar Sequence / Track / Program information blocks, but it is not the target MPC 3.9 composition. Do **not** copy its red header, left navigation rail, or older Program-centric architecture into the canonical UI.

![Legacy MPC Main screen — comparison only, not MPC 3.9 target](MPC3-UI-REFERENCE-ASSETS/images/legacy-mpc-main-screen-comparison-only-not-mpc-3-9-target-8ff5381cc7f4.jpg)

Source: [Sound On Sound — Akai MPC Basics](https://www.soundonsound.com/techniques/akai-mpc-basics).

---

## 1.6 Track View — full track overview

**Evidence A.** Official Akai support screenshot of the standalone Track View, showing separate track rows, track type/program fields, MIDI routing, per-track Monitor state, level metering, Mute/Solo and the bottom contextual Function Bar. The screenshot uses a legacy MPC 3-family visual skin, so it is valid for information hierarchy and interaction semantics, not for copying its older colors or pixel geometry into our MPC One / OS 3.9 target.

![Official MPC Standalone Track View — track rows, monitor, level, mute/solo and Function Bar](MPC3-UI-REFERENCE-ASSETS/images/official-mpc-standalone-track-view-track-rows-monitor-level-44e1b1d212c5.png)

Source: [Akai support — Input Monitoring Explained](https://support.akaipro.com/en/support/solutions/articles/69000860101-mpc-standalone-input-monitoring-explained).

**Implementation observations:** Track View is a real separate context (Shift+Main), not the Main Track region. Validate track selection, record-arm, Monitor IN/AUTO/MERGE, mute/solo availability, selected row, truthful Audio/Drum/MIDI/instrument semantics, and Back/Main return. Do not confuse controls in this capture with a claim that our current code supports every Track View capability.

# 2. Browser — direct official screenshots

The following images are from Akai support documentation. They show distinct Browser contexts; they must not be conflated into one screen or replaced with fake populated content.

## 2.1 Places / storage locations

![MPC Browser — Places tab and local storage locations](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-places-tab-and-local-storage-locations-9a871e38dc98.png)

## 2.2 Content categories

![MPC Browser — Content categories](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-content-categories-58ff6709c4c7.png)

## 2.3 Expansions

![MPC Browser — Expansions tab](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-expansions-tab-2c49d5c06e47.png)

## 2.4 Sample Assign / project sample pool

![MPC Browser — Sample Assign and sample pool](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-sample-assign-and-sample-pool-cdd94eae8477.png)

## 2.5 Browser Options dialog

![MPC Browser Options dialog](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-options-dialog-df0c8fcf44f3.png)

Source for all five Browser images: [Akai support — Understanding the MPC's Browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser).

**Implementation observations:** Places, Content and Expansions are separate navigation contexts; Sample Assign is a distinct sample-pool workflow. Search, file-type filters, favourites, audition and Options are separate controls/states. Where our provider does not support a location, show a truthful unavailable/empty state rather than invented content.

---

# 3. Pad Perform

**Evidence A.** Official Akai support screenshot of Pad Perform in Custom mode with a 4×4 note map, octave control and mode tabs. This is a real separate performance context, not the canonical Main workspace.

![Pad Perform — custom note map and 4×4 pad interpretation](MPC3-UI-REFERENCE-ASSETS/images/pad-perform-custom-note-map-and-4-4-pad-interpretation-c523ff4e337b.png)

Source: [Akai support — How to Remap the MPC Pads](https://support.akaipro.com/en/support/solutions/articles/69000867733-akai-pro-mpc-series-how-to-remap-the-mpc-pads).

Supplementary Notes-mode reference (older MPC OS visual, use for musical mapping only): [Sound On Sound — Akai MPC Basics, Pad Perform](https://www.soundonsound.com/techniques/akai-mpc-basics?page=2).

**Acceptance rule:** Pad Perform must not be marked complete just because pad labels appear somewhere else in Main. The dedicated Notes/Chords/Chromatic Chords/Progressions/Custom behavior needs its own implementation and acceptance status.

---

# 4. Track Edit and Sample Edit

## 4.1 Track Edit — Samples / Layer waveform

**Evidence A.** Official support image of the dedicated Track Edit Samples tab showing Track/Pad context, waveform, Layers 1–8, sample region and layer parameters.

![Track Edit — sample layers and waveform](MPC3-UI-REFERENCE-ASSETS/images/track-edit-sample-layers-and-waveform-259bac67e5a1.png)

Source: [Akai support — Tuning Basics](https://support.akaipro.com/en/support/solutions/articles/69000874731/).

## 4.2 Sample Edit — current MPC 3-family waveform editor

**Evidence B.** MPC 3.4+ specialist reference showing the Sample Edit waveform, start marker, slice/loop controls and bottom Trim/Zoom/Assign/Process actions. This is a close workflow reference but was not captured from a Gen 1 MPC One running v3.9.

![MPC 3 Sample Edit — waveform, slice and process controls](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-sample-edit-waveform-slice-and-process-controls-10ef314bb22a.png)

Source: [MPC-Tutor — MPC3 Slice Motion tutorial](https://www.mpc-tutor.com/mpc3-tutorial-slice-motion-vocals/).

## 4.3 Track Edit plugin / keygroup context

Official context reference: [Akai support — Understanding and Loading Programs](https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs). The article illustrates the Track/Program selection workflow and explains entry to Program Edit. Use the official v3.9 guide for the full Program Edit parameter-tab layout.

## 4.4 Automation / envelope controls

**Evidence B.** MPC 3.4+ example showing the pull-down performance status and an envelope parameter being edited. This is supporting evidence for the automation gesture and visual feedback, not a substitute for a dedicated automation/XYFX reference.

![MPC 3 envelope automation and performance status](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-envelope-automation-and-performance-status-dec6f11c0f82.png)

Source: [MPC-Tutor — MPC3 Slice Motion tutorial](https://www.mpc-tutor.com/mpc3-tutorial-slice-motion-vocals/).

---

# 5. Grid View and Step Sequencer

## 5.1 Grid View — note events and velocity lane

**Evidence C.** This screenshot comes from an older UI style. Use only to understand the relationship between note events and the velocity lane; do not copy its red toolbar or navigation treatment as the MPC 3.9 visual target.

![Legacy Grid View — note grid with velocity lane; visual structure only](MPC3-UI-REFERENCE-ASSETS/images/legacy-grid-view-note-grid-with-velocity-lane-visual-structu-d909300d6d41.png)

Source: [Akai support — Humanizing Your Tracks](https://support.akaipro.com/en/support/solutions/articles/69000863026-akai-pro-mpc-series-humanizing-your-tracks).

**Current authoritative reference:** the official [MPC Standalone OS v3.9 User Guide, p.167](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=167) shows three current Grid layouts: Audio, Drum and piano-roll/event lane. See the full page-verified atlas in §8.25. The legacy inline image above remains only a visual history reference.

## 5.2 Step Sequencer

**Current v3.9 figure found:** the [official guide, p.179](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=179) shows the current 16-step workspace and velocity steps. A standalone PNG URL is not exposed, but the actual screenshot is visible in the linked PDF. See §8.25 for the indexed page atlas.

Do not treat a Step Sequencer screen as complete merely because 16 buttons are drawn: track length, bar/pad selection, time division and supported step parameters belong to the screen contract.

---

# 6. Sampler / recording

**Evidence C.** Public standalone-MPC screenshot of the record/monitoring surface. This older visual is useful only for the central recording affordance and state hierarchy; the v3.9 guide remains the visual authority.

![Legacy standalone MPC Sampler recording screen — interaction reference only](MPC3-UI-REFERENCE-ASSETS/images/legacy-standalone-mpc-sampler-recording-screen-interaction-r-09b6ee494081.jpg)

Source: [Sound On Sound — Sample Slicing: Beatmaking With Hardware](https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware).

**Current authoritative reference:** the official [MPC Standalone OS v3.9 User Guide, pp.296–298](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=296) has current Sampler recording/input figures. These pages do not provide a complete independent screenshot for every Slice/Pad Tap/Pad Hold/Auto Sampler state; those remain explicitly tracked in §8.25. The screen must represent real record/stop/monitor/threshold and assignment state.

---

# 7. Preferences / Project lifecycle

## 7.1 Project Load/Save preferences

**Evidence B.** Public standalone-MPC preferences screenshot showing the Project Load/Save settings category and Auto Save options. Treat it as a general structure reference; verify exact labels, ordering and current options against OS 3.9.

![Preferences — project load/save and autosave](MPC3-UI-REFERENCE-ASSETS/images/preferences-project-load-save-and-autosave-61d56b95926b.jpg)

Source: [MPC-Tutor — Backup Strategies for MPC & Force Users](https://www.mpc-tutor.com/backup-strategies-mpc-force/).

## 7.2 Project / Menu Save / New Project

The [MPC 3 menu screenshot above](https://www.mpc-tutor.com/assets/mpc-menu-1024x639.png) provides a visual reference for menu-level project commands, not a complete Project dialog reference.

**Current authoritative reference:** official [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), sections **Project**, **New Project**, **Save**, and **Save As**. The exact file-picker/confirmation dialogs depend on the workflow; the UI must preserve cancellation and loaded-state invariants.

---


# 8. Additional screen captures: sequence, edit, mixer, and performance contexts

The following references were found in this pass. Their provenance is explicit: an actual MPC One photograph is labelled as such; an older standalone/MPC2 look or a different MPC model is supplementary, not a pixel-accurate v3.9 target.

## 8.1 Step Sequencer — actual MPC One hardware photo

**Evidence B, photographed MPC One.** The touchscreen shows the 16-step/velocity editing context. The photo is from a public user video/thread and its visible UI is a legacy generation; use it for the screen's basic layout and step/velocity relationship only.

![MPC One Step Sequencer photographed on the hardware](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-step-sequencer-photographed-on-the-hardware-c2831dc25a58.jpg)

Source: [MPC One step sequencer video](https://www.youtube.com/watch?v=uuNyLLg-cXs).

**MPC 3.9 reference:** [official v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), Step Sequencer and Step Automation sections. The current guide takes precedence for labels, hierarchy, colors, function-bar controls, automation and current feature set.

## 8.2 Sample Edit — Chop and slice editor

**Evidence C, older UI appearance.** This is a real MPC waveform/chop page showing slice markers and the Chop/Program/Zoom action strip. It is useful for understanding the relationship between waveform, slice markers, threshold/manual modes, and lower actions; do not copy the legacy red skin.

![MPC waveform Chop mode and slice markers](MPC3-UI-REFERENCE-ASSETS/images/mpc-waveform-chop-mode-and-slice-markers-4f464ebf5b5c.jpg)

Source: [Sound On Sound — Akai MPC Basics, Sample Edit section](https://www.soundonsound.com/techniques/akai-mpc-basics?page=2).

## 8.3 Sampler — record / input monitor

**Evidence C, older UI appearance.** The screen shows an actual record workspace with a sample-length counter, RECORD action, “Waiting for signal” state and PAD TAP / PAD HOLD / CANCEL choices.

![Standalone MPC Sampler — record waiting for signal](MPC3-UI-REFERENCE-ASSETS/images/legacy-standalone-mpc-sampler-recording-screen-interaction-r-09b6ee494081.jpg)

Source: [Sound On Sound — Sample Slicing: Beatmaking With Hardware](https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware).

## 8.4 Next Sequence — actual MPC One

**Evidence B, actual MPC One photographed during a tutorial.** The screen shows the Next Sequence tile grid on hardware. Japanese instructional text overlays the photo and reduces clarity, so treat it as a workflow reference, not a clean design plate.

![MPC One Next Sequence grid photographed on hardware](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-next-sequence-grid-photographed-on-hardware-a484f614d5e5.jpg)

Source: [Marronfieldsproduction — MPC One Next Seq & Song walkthrough](https://marronfieldsproduction.hatenablog.com/entry/2022/10/25/063000).

**Current v3.9 reference:** use the official guide's Next Sequence section to verify current controls, pad assignments, selected/queued sequence state, Next Bar, Sudden and Hold.

## 8.5 Song Mode — sequence playlist

**Evidence C, older MPC UI.** This direct image shows the Song playlist at left and sequence tiles at right. Song Mode is a separate workflow from Next Sequence: it arranges sequences into a fixed song structure rather than queueing a live next sequence.

![MPC Song Mode — sequence playlist and sequence tiles](MPC3-UI-REFERENCE-ASSETS/images/mpc-song-mode-sequence-playlist-and-sequence-tiles-55a35ec6a52c.png)

Source: [MPC-Tutor — MPC X, MPC One & MPC Live: essential tips](https://www.mpc-tutor.com/mpc-x-mpc-live-6-tips-shortcuts/).

## 8.6 Channel Mixer — current MPC 3 I/O / monitoring panel

**Evidence A, official Akai support screenshot for the MPC 3 family.** This screenshot shows the channel-strip grid and I/O-related track state: Monitor, MIDI input, Send To and audio output, with Volume, Pan & Volume, Sends, Effects, I/O and Settings tabs. It is the current-generation mixing reference, although the screenshot is not explicitly captured on Gen 1 MPC One.

![MPC 3 Channel Mixer I/O, monitoring, sends and outputs](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-channel-mixer-i-o-monitoring-sends-and-outputs-0d37b279b37e.png)

Source: [Akai support — Multi MIDI controller workflow](https://support.akaipro.com/en/support/solutions/articles/69000864879-akai-pro-mpc-series-setting-up-multiple-midi-controllers-in-a-project).

## 8.7 XL Channel Strip — I/O selector / output routing

**Evidence A, official Akai support MPC 3 screenshot.** This image shows the Main Track/Arrangement surface together with the left XL Channel Strip region and an output selector dialog.

![MPC 3 Main and XL Channel Strip output selector](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-xl-channel-strip-and-i-o-routing-e97042beaa09.png)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 8.8 Pad Mixer — pad routing matrix

**Evidence C, older routing screen hosted by official Akai support.** This shows the pad grid with per-pad outputs and bottom tabs for Level, Pan, Mute, Send, Insert and Route. It is not the complete current MPC 3 Pad Mixer; use it only for the relationship between pad selection, route and per-pad mix parameters.

![MPC pad mixer routing matrix — older visual, not MPC 3.9 geometry](MPC3-UI-REFERENCE-ASSETS/images/mpc-pad-mixer-routing-matrix-older-visual-not-mpc-3-9-geomet-d095d6c025c0.jpg)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 8.9 MIDI / Sync Preferences

**Evidence B, photographed MPC One.** This real hardware image shows Preferences → MIDI / Sync, with input/output ports and per-port enable flags. Its exact firmware appearance is older, but the settings taxonomy is relevant.

![MPC One Preferences — MIDI / Sync ports](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-preferences-midi-sync-ports-539cacc26b3a.jpg)

Source: [Elektronauts — MPC One MIDI/SYNC Preferences discussion](https://www.elektronauts.com/t/mpc-thread-mpc-live-mpc-x-mpc-one-part-2/173139?page=142).

A clean public screenshot of the same category is also available at [MPC-Tutor — MIDI/Sync preferences](https://www.mpc-tutor.com/mpc-midi-tutorial-connecting-midi-controllers/). The current v3.9 guide governs exact options and geometry.

## 8.10 Metronome / Click dialog

**Evidence A, official Akai MPC 3 screenshot.** The dialog shows Count-In, Enable, Rate, Sound, Level and Output. This is a dedicated modal dialog within the persistent shell and deserves an acceptance scenario independent of Main layout.

![MPC 3 Click/Metro dialog](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-click-metro-dialog-b9112d55a543.png)

Source: [Akai support — Editing the Count-In and Metronome](https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome).

## 8.11 XYFX

**Evidence B, MPC Live photographed.** This shows the standalone XYFX touch grid with a movable XY position and effect-parameter context. It is another MPC model and an earlier OS appearance; use it for the interaction model (XY plane + destination + latch), not Gen 1 MPC One pixel geometry.

![MPC standalone XYFX touch grid](MPC3-UI-REFERENCE-ASSETS/images/mpc-standalone-xyfx-touch-grid-d943bc106424.jpg)

Source: [Heikki Siltala — MPC Live effects and XY Pad](https://heikkisiltala.net/wiki/en/music/akai_mpc_live_effects).

## 8.12 Program Edit — Samples / layer parameters

**Evidence C, earlier MPC visual appearance.** These screenshots show the selected Pad and sample-layer editing area, including layer rows, semitone/fine tuning and the Samples tab. They are supplementary structure examples, not a complete current Track Edit contract.

![MPC Program Edit Samples page — layer list](MPC3-UI-REFERENCE-ASSETS/images/mpc-program-edit-samples-page-layer-list-f88c7df9c650.png)

![MPC Program Edit — per-pad sample-layer parameters](MPC3-UI-REFERENCE-ASSETS/images/mpc-program-edit-per-pad-sample-layer-parameters-0570d70052d8.png)

Source: [MPC-Tutor — Creating the Clap from Hell](https://www.mpc-tutor.com/mpc-x-mpc-live-tutorial-clap-from-hell/) and [Standalone MPC Cycle Kit tutorial](https://www.mpc-tutor.com/mpc-cycle-kit-chopped-break/).

## 8.13 Pad Color / pad assignment

**Evidence B, real MPC One screen photo.** This reference shows a 4×4 pad assignment grid, pad names and a palette for selecting colors. It belongs to Pad Color / pad configuration workflows, not to the Main workspace.

![MPC One pad color / sample assignment grid](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-pad-color-sample-assignment-grid-07ac001a859b.jpg)

Source: [MusicRadar — 10 ways to get more out of your MPC hardware](https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC).

## 8.14 Q-Link / macro controls

**Evidence C, legacy UI.** Useful to understand the matrix of assignable controls, displayed parameter names and a route to Q-Link Edit. Do not use its color treatment or exact panel arrangement as a current MPC 3.9 target.

![MPC Q-Link parameter overview](MPC3-UI-REFERENCE-ASSETS/images/mpc-q-link-parameter-overview-ba262bc8ab91.png)

Source: [MPC-Tutor — MPC X, MPC One & MPC Live essential tips](https://www.mpc-tutor.com/mpc-x-mpc-live-6-tips-shortcuts/).

## 8.15 Save dialog / project save options

**Evidence C, legacy standalone UI.** The Save window shows Project, Project As, Drum Program, Program FX Rack, Sequence, Audio Mixdown and Ableton Live Set. It is a separate modal workflow, not a full representation of every file-picker or confirmation state.

![MPC Save window — project, project as and other save formats](MPC3-UI-REFERENCE-ASSETS/images/mpc-save-window-project-project-as-and-other-save-formats-44af233c0765.png)

Source: [MPC-Tutor — MPC Projects](https://www.mpc-tutor.com/mpc-x-mpc-live-projects-lowdown/).

**Official support workflow:** [Saving projects with Auto Save](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save) and [saving/backing up to external drive](https://support.akaipro.com/en/support/solutions/articles/69000860953-mpc-standalone-the-best-way-to-save-and-back-up-projects-to-an-external-drive).

## 8.16 Official-manual figures for screens without an independent image URL

For several contexts, the official MPC Standalone User Guide includes a screenshot figure, but the figure is bundled inside a PDF and no stable direct image URL was exposed by the public host. These are **linked visual references**, not embedded image files:

- [16 Levels — official MPC Standalone OS v3.7 User Guide, PDF page 75](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf#page=75). The PDF shows the 16 Levels dialog in Velocity and Tune variants; the guide describes current parameter options.
- [List Edit — official MPC Standalone OS v3.7 User Guide, PDF page 184](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf#page=184). The PDF figure shows Arrange/Grid/List navigation, event table, View filter, Timing Correct, Events/Tempo, Insert/Delete, Play and Nudge.
- [Looper — official MPC Standalone OS v3.7 User Guide, PDF page 332](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf#page=332). The PDF figure shows input/source, sync, Record To Play/Overdub, output gain, waveform, Export to Track, Clear, Reverse, Replace, Play and Overdub.
- [Step Sequencer and Step Automation — official MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf). Search within the official guide for “Step Sequencer” and “Step Automation”; the guide is the authority for the current version.
- [Track Edit — official MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf). See Drum Track, Keygroup, Plugin, MIDI, CV and Audio Track subsections; a single screenshot cannot represent all track-specific editor tabs.

These PDF links are included because the screenshots are inside the manual; they are not a claim that those page images are duplicated inline in this Markdown document.


## 8.17 Browser photographed directly on MPC One

**Evidence B, actual MPC One hardware photo.** This is useful as a full-hardware context shot for the Browser screen dimensions and the Places/Content/Expansions search surface. It is an older firmware visual and must not override the MPC 3.9 layout or product decisions.

![MPC One Browser shown on the physical hardware](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-browser-shown-on-the-physical-hardware-9d3f4d676642.jpg)

Source: [MusicRadar — 10 ways to get more out of your MPC hardware](https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC).

## 8.18 Sample Edit — Trim on physical MPC One

**Evidence B, actual MPC One photographed during a tutorial.** This reference shows the waveform and Trim context on the real hardware. It is an older MPC OS appearance; current labels and available operations must be checked against v3.9.

![MPC One Sample Edit Trim screen photographed on the hardware](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-sample-edit-trim-screen-photographed-on-the-hardware-4f5d9b339bab.jpg)

Source: [Marronfieldsproduction — MPC One Sample Edit / Trim walkthrough](https://marronfieldsproduction.hatenablog.com/entry/2022/10/28/063000).

## 8.19 Sample Assign — sample pool and pad grid on MPC One

**Evidence B, actual MPC One screen photo.** This reference captures a sample pool list to the left and pad assignment cells on the right. It distinguishes Sample Assign from ordinary Browser file selection.

![MPC One Sample Assign with sample pool and 4x4 pad assignment](MPC3-UI-REFERENCE-ASSETS/images/mpc-one-sample-assign-with-sample-pool-and-4x4-pad-assignmen-dd8a4b575ad3.jpg)

Source: [Note — MPC One sample/pad assignment workflow](https://note.com/suitebossa/n/nde0f912fe4ce).

## 8.20 Legacy sample waveform / automation detail

**Evidence C, legacy UI.** This image is a real standalone MPC waveform editor displaying an automation lane and bottom region-edit actions. It is relevant to waveform/region editing, but the old red-and-grey skin is not the target MPC 3.9 Main or Sample Edit design.

![Legacy MPC waveform editor with automation lane and bottom actions](MPC3-UI-REFERENCE-ASSETS/images/legacy-mpc-waveform-editor-with-automation-lane-and-bottom-a-4874dacff8fb.jpg)

Source: [Ragtime — Akai MPC One standalone sampler and sequencer](https://ragtime.online/akai-professional-mpc-one-standalone-sampler-and-sequencer.html).

This index covers 82 screen and sub-screen contexts from the MPC Standalone user-guide taxonomy and our repository contract. “Inline” means a remote screenshot image is embedded above; “official PDF figure link” means the guide's screenshot remains inside the linked PDF because no stable direct image URL was exposed; “guide link” is still a visual research task. The evidence column never says a screen is implemented merely because its original reference exists.

**Page-linked screenshot sources:** the v3.9 guide below is the target reference for MPC 3.9 labels and composition. The additional **MPC 2.11.7 User Guide** links are an extensive visual reference source with screenshots of many older standalone MPC contexts; they are explicitly labelled as legacy, not pixel-perfect MPC 3.9. These two guides make it possible to inspect every context in the table even where a direct standalone image URL was not found.

## 8.21 Additional recovered official screenshot references

These screen references were found by searching the **contents of official MPC standalone user-guide PDFs**, not by looking only for standalone PNG files. The PDF itself contains the relevant screen figures, even when Akai does not expose those figures as separately addressable image URLs. This is why the earlier catalogue's "no image found" wording was too strong.

### Grid View — note events / velocity lane

**Evidence A, official Akai standalone guide.** Akai's MPC Standalone OS v3.7 User Guide includes distinct screenshot figures for Audio, Drum, and Keygroup/MIDI/CV Grid View layouts. The PDF's figures show the waveform-based audio grid, the drum pad lane plus note events, and the keyboard piano-roll plus velocity lane.

[Open the official MPC Standalone OS v3.7 User Guide — Grid View figure (PDF page 161)](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf#page=161).

**Supplementary current MPC 3 screenshot:** a screenshot of Grid's piano-roll/events surface appears in Akai's official custom-arpeggiator article. It shows Grid, a piano keyboard, colored events, a velocity lane, automation and bottom edit controls. This article discusses both standalone and desktop workflows, so use it as a screen-layout reference and confirm version/hardware-specific details in the standalone manual.

![MPC Grid with piano-roll events, velocity lane and bottom edit functions](MPC3-UI-REFERENCE-ASSETS/images/mpc-grid-with-piano-roll-events-velocity-lane-and-bottom-edi-cc323c849b57.png)

Source: [Akai support — Creating and Loading Custom Arp Patterns](https://support.akaipro.com/en/support/solutions/articles/69000856004-mpc-series-creating-custom-arp-patterns).

### 16 Levels — Velocity and Tune dialogs

**Evidence A, official standalone guide.** The MPC Standalone OS v3.7 User Guide contains two screenshot figures side by side: 16 Levels in Velocity mode and 16 Levels in Tune mode. The same dialog supports other mapped parameters such as Filter, Layer, Slice, Articulation, Attack, Decay, Probability and Ratchet.

[Open the official MPC Standalone OS v3.7 User Guide — 16 Level figures (PDF page 75)](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf#page=75).

### List Edit — event table

**Evidence A, official standalone guide.** MPC Standalone OS v3.7 documents a full List Edit screenshot with ARRANGE / GRID / LIST navigation, time counter, timing-correct selector, event-type filter, event rows and bottom EVENTS / TEMPO / INSERT / DELETE / PLAY / NUDGE controls. The guide is the source; the screenshot is embedded inside the PDF rather than exposed as a standalone PNG.

[Open the official MPC Standalone OS v3.7 User Guide and search for “List Edit Mode”](https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.7.pdf).

### Looper — record, overdub, waveform and export

**Evidence A, official standalone guide.** MPC Standalone OS v3.6 has a full Looper screenshot showing input/source, sync, bar count, Record To Play/Overdub, output gain, waveform, Export to Track, Clear, Reverse, Replace, Play and Overdub.

[Open the official MPC Standalone OS v3.6 User Guide — Looper figure (PDF page 329)](https://cdn.inmusicbrands.com/Software/36/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.6.pdf#page=329).

### Track Edit — LFO tab

**Evidence A, official standalone guide.** MPC Standalone OS v3.6 includes a detailed LFO editor figure with LFO 1/LFO 2, waveform, destinations, reset, fade-in, delay, rate, level and the bottom MODS / EFFECTS / LFO / UTILITIES / MOD MATRIX navigation.

[Open the official MPC Standalone OS v3.6 User Guide — LFO figure (PDF page 203)](https://cdn.inmusicbrands.com/Software/36/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.6.pdf#page=203).

### Arpeggiator — standalone dialog and settings

**Evidence A, official Akai support image.** The screenshot shows the Arpeggiator dialog with Enabled, Latch Notes, Time Division, Action, Note Length, Velocity, Swing, Pattern, Sustain Pedal Latches and Latch Mode.

![MPC Arpeggiator dialog with timing, action, pattern and latch settings](MPC3-UI-REFERENCE-ASSETS/images/mpc-arpeggiator-dialog-with-timing-action-pattern-and-latch-ffbb8f3d1d49.png)

Source: [Akai support — Creating and Loading Custom Arp Patterns](https://support.akaipro.com/en/support/solutions/articles/69000856004-mpc-series-creating-custom-arp-patterns). The exact standalone/desktop context of this particular dialog must be verified against the current guide before using its geometry as an MPC One 3.9 target.

### MIDI / Sync Preferences

**Evidence B, specialist MPC screenshot.** The MIDI tutorial page has a direct image of the MIDI/Sync Preferences screen with input/output port lists and port enable controls.

![MPC standalone MIDI Sync preferences with input and output ports](MPC3-UI-REFERENCE-ASSETS/images/mpc-standalone-midi-sync-preferences-with-input-and-output-p-86295292ded6.png)

Source: [MPC-Tutor — MIDI/Sync Preferences](https://www.mpc-tutor.com/mpc-midi-tutorial-connecting-midi-controllers/). This screenshot is from the older MPC 2.11-era visual skin and is for settings taxonomy only.



## 8.22 Main / Sequence Edit and destructive-operation dialogs

**Evidence A, official Akai MPC 3 standalone screenshots.** The Sequence Edit menu is not merely a generic confirmation dialog. It contains Erase, Clear All, Clear Events, Trim, Transpose, Delete Bars, Insert Bars, Half Length, Double Length, Copy Sequence, Copy Bars and Copy Events.

![MPC 3 Sequence Edit menu with Erase, Clear All, Clear Events, Trim and other sequence operations](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-sequence-edit-menu-with-erase-clear-all-clear-events-t-b8feae20d5c0.jpg)

**Erase — range, track, and event type.** The dedicated Erase modal exposes the Sequence, Track, start/end Bar/Beat/Tick range, and All / Automation / Note / Except Note modes.

![MPC 3 Erase dialog with sequence, track, time range and event-type options](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-erase-dialog-with-sequence-track-time-range-and-event-2c9917019641.png)

**Erase — pad/bank mask.** In Note mode, the dialog exposes an eight-bank pad mask; this is a separate state and must not be collapsed into the parent Erase screenshot.

![MPC 3 Erase Note mode with the eight-bank pad selection mask](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-erase-note-mode-with-the-eight-bank-pad-selection-mask-519b44b92828.png)

**Clear All confirmation.** The UI asks whether to clear everything in the Sequence and set it to unused, with separate Clear All and Cancel actions.

![MPC 3 Clear Sequence destructive confirmation dialog](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-clear-sequence-destructive-confirmation-dialog-c38dc8416680.png)

Source for all four images: [Akai support — How to Delete a Sequence, Track or Program](https://support.akaipro.com/en/support/solutions/articles/69000864104-akai-pro-mpc-series-how-to-delete-a-sequence-track-or-program).

## 8.23 Preferences — Hardware and Project Load/Save (MPC 3)

**Evidence A, official MPC 3 standalone screenshot.** The Hardware page contains visible preferences such as Screen Brightness, Power On Screen, Tap Tempo behavior, Flash Tap Tempo Light, Bank Button Press and Shift + Q-Link Press. This screenshot demonstrates why Preferences needs multiple indexed states, not a single generic settings screenshot.

![MPC 3 Preferences — Hardware tab with Tap Tempo, Bank Button and Q-Link settings](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-preferences-hardware-tab-with-tap-tempo-bank-button-an-5c75684d6f80.png)

**Project Load/Save.** The official MPC 3 screen shows New Project Behaviour, User Auto Load File, New Project Dialog, Template File and Temporary File Location. These are not the same thing as the project-file picker or save-confirmation modal.

![MPC 3 Preferences — Project Load/Save tab](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-preferences-project-load-save-tab-d4eb28c2459b.png)

**New Project Behaviour selector.** The selector offers Empty Project, Factory Project and User Auto Load Project.

![MPC 3 New Project Behaviour selector](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-new-project-behaviour-selector-2c470b0aab21.png)

**New Project Dialog selector.** The preference supports Off, Demo, Recent and Demo/Template/Recent.

![MPC 3 New Project Dialog selector](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-new-project-dialog-selector-e4d1be9a8cc1.png)

Sources: [Akai support — Tap Tempo Light preference](https://support.akaipro.com/en/support/solutions/articles/69000870813-mpc-3-standalone-how-do-i-turn-off-the-tap-tempo-light-) and [Akai support — New Project Dialog](https://support.akaipro.com/en/support/solutions/articles/69000857945-akai-pro-mpc-series-new-project-dialog).

## 8.24 Track Type / Program fields on Main

**Evidence A, official MPC 3 Main context.** These images document the selector affordance on the Track identity and the separate Program identity, and help prevent the Main screen from being treated as a generic pad grid.

![MPC 3 Main Track Type selector affordance](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-main-track-type-selector-affordance-cd86da325d66.png)

![MPC 3 Main Program context and Pad Perform entry](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-main-program-context-and-pad-perform-entry-15b124a1caee.png)

Source: [Akai support — Understanding and Loading Programs](https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs).



## 8.25. Current MPC Standalone OS v3.9 screenshot atlas — page-verified

This is the **current-version visual atlas** recovered from the official MPC Standalone OS v3.9 User Guide. The screenshots below are printed figures embedded in the guide, not independent PNG/JPEG files. Each link goes directly to the relevant printed page; the readable mirror is included as a second way to inspect the page when the official CDN viewer ignores the page fragment.

- **Primary source:** [Official Akai/inMusic MPC Standalone OS v3.9 User Guide (PDF)](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf).
- **Page-readable mirror:** [MPC Standalone OS v3.9 User Guide — 530-page PDF mirror](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf). This is a mirror of the guide, not a separate Akai source.
- Page numbers below are the printed page numbers. The mirror page anchors were individually opened/rendered during this research pass; where the guide contains descriptive text but no screenshot, that is explicitly noted.
- **Version / hardware boundary:** these are current MPC OS 3.9 standalone-guide references. They are much stronger than pre-MPC-3 screenshots for labels and layout. They do not, by themselves, prove that every screenshot was captured on a Gen 1 MPC One rather than another supported standalone MPC; hardware-specific differences remain tagged below.

### A. Shell, Main, global dialogs and preferences

| Visual ID / context | v3.9 guide page | What the page actually shows | Confidence / limit |
|---|---:|---|---|
| VIS-001 Main workspace / overall shell | [p.15](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=15) | Main Mode map: Toolbar, Shortcuts, Mixer Strips, Sequence, Track/Arrangement and Function Buttons | Current OS 3.9 overview; canonical shell reference |
| Main Mode expanded composition | [p.109](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=109) | Detailed Main Mode screen and labeled regions | Use alongside p.15; not a separate device-specific screenshot set |
| VIS-002 Toolbar / project / transport | [p.57](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=57) | Toolbar and Sync/Undo History examples | Current guide screenshot |
| VIS-003 Menu / mode launcher | [p.56](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=56) | Menu tile launcher plus lower Project / New Project / Save / Preferences actions | Current guide screenshot |
| System Resources popup | [p.59](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=59) | System resource overlay / status detail | Useful modal reference |
| VIS-065 Project overview | [p.60](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=60) | Project window, project contents and lower action row | Current Project context; does not replace every load/save picker state |
| VIS-066 Save Window / save destination chooser | [p.69](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=69) | Current Save window with tiles for Project, Project As, Track/Program/Sample and export actions | Current OS 3.x family figure; not a screenshot of every file picker/overwrite confirmation |
| VIS-078 Audio Mixdown / export | [p.100](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=100) | Audio Mixdown screen with audio range, render source/options and file format/sample rate controls | Current OS 3.x family figure; export destination picker and every result/error state remain separate |
| Preferences landing page | [p.61](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=61) | Preferences category sidebar and information/activation view | Screenshot exists for the landing page, not each preferences subpage |
| VIS-075 Timing Correct | [p.75](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=75) | Timing Correct window including normal and pad-bank-oriented examples | Current OS 3.9 modal reference |
| VIS-076 Metronome / Click | [p.77](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=77) | Click/Metro controls and options | Current OS 3.9 dialog |
| VIS-077 Automation states | [p.78](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=78) | Automation-related control states and operating description | State/semantics reference; not a complete automation editor screenshot |
| VIS-050 16 Levels | [p.80](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=80) | 16 Levels dialogs including Velocity and Tune variants | Current OS 3.9; supersedes the v3.7-only visual claim |
| VIS-051 Pad Perform | [p.81](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=81) | Pad Perform Custom mapping example | Current OS 3.9; other modes are separately described |
| VIS-052 Arpeggiator | [p.84](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=84) | Arpeggiator dialog / timing and latch options | Current guide page; dialog geometry reference |
| VIS-053 Erase dialog | [p.87](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=87) | Erase dialog and pad-bank mask variants | Current OS 3.9; distinguish scope and confirmation state |

### B. Sequence, Track, Browser, Arrangement and mixing

| Visual ID / context | v3.9 guide page | What the page actually shows | Confidence / limit |
|---|---:|---|---|
| VIS-013 Sequence details / Time Signature | [p.112](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=112) | Sequence fields and Time Signature dialog | Current Main/sequence reference |
| VIS-014 / VIS-081 Track fields and type | [p.117](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=117) | Track section examples, including differing track types | Use with official Main Track/Program images in §8.24; type-specific screens differ |
| Track edit / duplicate / delete actions | [p.126](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=126) | Track editing entry and action popups | Current context for track menus/dialogs |
| VIS-016 / VIS-019 / VIS-020 Arrangement | [p.129](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=129) | Arrangement view and its relationship to Main | Current guide; some editing states are on following pages |
| VIS-005 / VIS-006 XL Channel Strips | [p.133](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=133) | Mixer-strip context on Main | Current shell/strip reference |
| VIS-008 / VIS-009 Browser | [p.136](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=136) | Current Browser Places/Content workflow examples | Current guide; Browser Options subdialog has a separate direct official support image in §2.5 |
| VIS-011 Sample Assign | [p.139](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=139) | Sample Assign view and pool/assignment context | Current guide screenshot |
| VIS-044–VIS-047 Channel Mixer | [pp.140–144](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=140) | Mixer overview and Volume, Pan & Volume, Sends, Effects and I/O contexts across the page range | Current OS 3.9. Use individual pages 140, 141, 142, 143 and 144 when comparing each tab |
| VIS-048 / VIS-049 Pad Mixer overview | [p.145](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=145) | Full Pad Mixer context and its tab organization | **Closes the earlier claim that no current full-screen Pad Mixer reference was found** |
| Pad Mixer — Volume / Pan & Volume | [p.146](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=146) | Per-pad level and pan views | Current OS 3.9 |
| Pad Mixer — Sends / Effects | [p.147](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=147) | Per-pad Sends and Effects contexts | Current OS 3.9 |
| Pad Mixer — I/O | [p.148](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=148) | Per-pad I/O context | Current OS 3.9 |
| VIS-064 Sounds Mode | [pp.149–150](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=149) | Sounds Mode and its favorites/setlist examples | Current guide; cloud/paid options remain intentionally out of product scope |
| VIS-057 XYFX | [pp.151–153](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=151) | XYFX screen and different controlled-effect modes | Current OS 3.9 guide reference |
| VIS-019 / VIS-020 Arrange tab and views | [p.154](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=154) | Arrange/Grid/List navigation and timeline context | Current guide; detailed Grid and List references follow |

### C. Grid, Step, List Edit and all major Track Edit types

| Visual ID / context | v3.9 guide page | What the page actually shows | Confidence / limit |
|---|---:|---|---|
| VIS-021 / VIS-022 / VIS-023 Grid View | [p.167](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=167) | Three Grid layouts: Audio waveform, Drum pad/event lane and piano-roll/event lane with velocity | **Closes the earlier current-version Grid screenshot gap** |
| VIS-024 / VIS-025 Step Sequencer | [p.179](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=179) | 16-step sequencer and velocity step presentation | Current OS 3.9 |
| VIS-026 / VIS-027 List Edit | [p.184](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=184) | Event table, event filtering and event/tempo workflow | **Closes the earlier current-version List Edit screenshot gap** |
| VIS-028 Track Edit — Drum / Global | [p.190](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=190) | Current Drum Track Edit Global layout | Current OS 3.9 |
| Track Edit — zones / stem dialogs | [p.191](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=191) | Edit Zones dialog and Create Stems window | Current dialogs; Create Stems applicability depends on device/software scope |
| VIS-029 Track Edit — Samples / layer start-end | [pp.197, 203](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=197) | Sample-region editing, Samples/Oscs and velocity/layer context | Current OS 3.9; inspect both pages |
| VIS-031 Track Edit — LFO | [p.212](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=212) | LFO editor with waveform/rate/destination controls | **Closes the earlier current-version LFO screenshot gap** |
| VIS-032 Track Edit — Keygroup | [p.215](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=215) | Keygroup Track Edit context | Current OS 3.9; not interchangeable with Drum layout |
| Keygroup sample/loop details | [p.221](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=221) | Keygroup sample region and loop context | Current OS 3.9 |
| Filter detail | [p.230](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=230) | Filter parameters / velocity sensitivity context | One parameter view, not every filter variant |
| VIS-033 Track Edit — Plugin | [p.254](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=254) | Plugin track instrument screen example | Current OS 3.9; specific plugin UI varies |
| VIS-034 Track Edit — MIDI | [p.255](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=255) | MIDI track control / CC context | Current OS 3.9 |
| VIS-035 Track Edit — CV | [p.256](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=256) | CV track screen context | Reference only where that hardware/backend context applies |
| VIS-030 Track Edit — envelopes | [p.257](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=257) | Envelope editor variants | Current OS 3.9; parameter variants are separate states |

### D. Track View, Mute, Sample Edit and Sampler

| Visual ID / context | v3.9 guide page | What the page actually shows | Confidence / limit |
|---|---:|---|---|
| Track Mute | [p.260](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=260) | Track Mute context | Current OS 3.9 |
| Track Groups | [p.261](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=261) | Track Group context/state | Current OS 3.9 |
| Pad Mute | [p.264](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=264) | Pad Mute with the 4×4 pad matrix | Current OS 3.9; a dedicated workspace separate from Main |
| VIS-017 / VIS-018 Track View | [p.266](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=266) | Track View overview | Current OS 3.9 |
| VIS-036 / VIS-037 / VIS-038 Sample Edit | [p.269](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=269) | Sample Edit waveform / Trim context | Current OS 3.9; use the dedicated section for Chop/Slice-related references too |
| VIS-039–VIS-043 Sampler family | [pp.296–298](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=296), [Auto Sampler p.304–305](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=304) | Sampler input/recording states, plus Auto Sampler control-detail figures for source, note range, velocity layers, sampling, looping and completion | Current guide; Auto Sampler figures are embedded control-detail images, not a complete full-screen capture of every Sampler mode |

### E. Performance, control, MIDI and Looper

| Visual ID / context | v3.9 guide page | What the page actually shows | Confidence / limit |
|---|---:|---|---|
| VIS-058 Q-Link Edit | [p.306](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=306) | Q-Link Edit workflow | Current OS 3.9 |
| Q-Link assignment categories | [p.314](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=314) | Several Q-Link assignment variants | Current guide; do not infer an available physical knob bank on MPC Studio MkII |
| VIS-059 XY Pad / Crossfader | [p.316](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=316) | XY Pad macro editor and fullscreen XY canvas | Current OS 3.9 |
| VIS-054 Next Sequence | [p.318](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=318) | Next Sequence performance map | Current OS 3.9 |
| MIDI Learn | [p.320](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=320) | MIDI Learn target/type list | Current OS 3.9 |
| VIS-055 Song Mode | [p.328](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=328) | Song Mode playlist and sequence arrangement | Current OS 3.9; separate from Next Sequence |
| VIS-060–VIS-062 MIDI Control Mode | [pp.333–337](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=333) | MIDI Control overview and Pad/Button/Q-Link/XY pages | Current OS 3.9; check p.333, 334, 335, 336 and 337 individually |
| VIS-063 Pad Color | [p.338](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=338) | Pad Color Mode | Current OS 3.9 |
| VIS-056 Looper | [p.340](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=340) | Looper screen and operations | **Closes the earlier reliance on only a v3.6/v3.7 Looper figure** |

### F. Preference and dialog coverage: what the v3.9 PDF does *not* visually show

The guide's Preferences chapter spans **pp.61–68**. It provides the Preferences landing screenshot on p.61 and detailed descriptions of Audio Device, Audio/Export, MIDI/Sync, Hardware, Sequencer, Project Defaults, Project Load/Save and General settings across subsequent pages. However, most of those later pages are text-led; **they are not individual full-screen screenshots of every Preferences subpage**. Use the page links as a reliable label/control taxonomy, but do not use them as a screenshot-based geometry claim:

- [Preferences overview, p.61](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=61)
- [Audio Device settings, p.62](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=62)
- [Audio / Export and MIDI / Sync settings, pp.63–64](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=63)
- [Hardware and Sequencer settings, pp.64–65](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=64)
- [Project Defaults, p.66](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=66)
- [Project Load/Save and General settings, pp.67–68](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=67)

**Remaining screenshot gaps to pursue from public material:** (1) dedicated full-screen screenshots of Audio Device, Audio/Export, Hardware, Sequencer and General Preferences subpages—MIDI/Sync and Project Load/Save have direct official support images in §8.26, Project Defaults has a supplementary MPC-series screenshot there, and the guide's p.66 text remains the label/behavior authority; (2) further Browser Options states beyond the captured Options modal, audition popover, filter strip and favourites row; (3) full-screen Sampler Slice, Pad Tap and Pad Hold states. The guide's Auto Sampler settings have current control-detail figures on pp.304–305, but no full-screen Auto Sampler capture was found; (4) current file-picker, overwrite/cancel/permission/error dialogs across project load/save and sample import. The guide describes the Project Import flow on p.7 but does not show the dialog there; (5) exact Gen 1 MPC One OS 3.9 captures for screens whose public examples are MPC 3.x-family screenshots without explicit hardware provenance. For these, an official guide page with prose is **not** marked as a screenshot. Continue targeted searches for trustworthy standalone images/tutorials and add links only when the image can be identified and version-labeled.

---


## 8.26. Additional official support screenshots — Browser options, MIDI/Sync and Auto Save

These official Akai support images add a few direct visual states beyond the PDF page atlas above. The Browser images are from Akai's MPC 3 Browser article; the Preferences images are from current-generation MPC 3 support workflows, but individual screenshots do not always disclose the exact hardware/OS build. Use the official v3.9 guide for labels and semantics where they differ.

### Browser — live audition popover and filters

**Evidence A, official Akai MPC Browser screenshot.** The first image shows the Audition popover open above the file list, including Auto, Sync, Warp and the audition level. This is a direct image—not merely a manual-page link—and is useful for matching the popover footprint and its relationship to the Browser Function Bar.

![MPC Browser with the Audition Auto Sync Warp level popover open](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-with-the-audition-auto-sync-warp-level-popover-o-20bbc5cef83e.png)

Source: [Akai support — How to Audition Expansions and Samples](https://support.akaipro.com/en/support/solutions/articles/69000871929-akai-pro-mpc-series-how-to-audition-expansions-and-samples). Firmware/device is not stated for this figure; use alongside the current v3.9 PDF atlas.

**Browser filter strip.** This official crop shows the six file-type filters plus Options gear in a single horizontal strip. It does not show the full Browser workspace and is therefore a control-detail reference only.

![MPC Browser search and six file-type filter icons](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-search-and-six-file-type-filter-icons-7bf79c7b2e9e.png)

Source: [Akai support — Understanding the MPC Browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser).

**Favourite folder quick-access buttons.** This crop isolates the five numbered Browser favourite-folder buttons. It is an interaction-detail reference, not a whole-screen target.

![MPC Browser favourite folder buttons 1 to 5](MPC3-UI-REFERENCE-ASSETS/images/mpc-browser-favourite-folder-buttons-1-to-5-76fac09c8950.png)

Source: [Akai support — Understanding the MPC Browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser).

### Preferences — MIDI / Sync

**Evidence A, official Akai MPC 3 screenshot.** This is a current-generation Preferences → MIDI / Sync screen with input and output ports, Global/Control/Track and Sync/Track routing toggles, automatic port discovery and MIDI Control Mode Output. It materially improves the reference for VIS-069 and the MIDI-port part of VIS-060–VIS-062.

![MPC 3 Preferences MIDI Sync port routing controls](MPC3-UI-REFERENCE-ASSETS/images/mpc-3-preferences-midi-sync-port-routing-controls-39f024016216.png)

Source: [Akai support — Wireless Remote MIDI Control in a DAW](https://support.akaipro.com/en/support/solutions/articles/69000867660-akai-pro-mpc-series-wireless-remote-midi-control-in-a-daw). The article explicitly includes MPC One/One+ among supported standalone hardware, but the embedded image is not identified as Gen 1 One specifically.

### Sampler — direct screenshot showing the four recording modes

**Evidence B, official Akai support image; historical MPC standalone skin, not a current OS 3.9 geometry target.** This direct full-screen capture shows input source and channel configuration, the input meter/fader region, Monitor state, Arm control, sample-length counter and the four recording-mode tabs: Sample, Slice, Pad Tap and Pad Hold. This is a stronger reference for the distinct Sampler workspace and mode separation than an unlabeled generic panel. It does not depict every mode after selection, nor prove the exact Gen 1 MPC One OS 3.9 appearance.

![MPC Standalone Sampler screen with input source, Arm, sample counter and Sample Slice Pad Tap Pad Hold tabs](MPC3-UI-REFERENCE-ASSETS/images/mpc-standalone-sampler-screen-with-input-source-arm-sample-c-f05873886bbf.png)

Source: [Akai support — Standalone Mode Sampler Overview](https://support.akaipro.com/en/support/solutions/articles/69000838263-akai-pro-mpc-series-standalone-mode-sampler-overview). The linked article describes Sample, Slice, Pad Hold and Pad Tap, including record-arm and signal-threshold behavior.

**Sampler input / Monitor detail crop.** This official support figure isolates the Input Source selector, level meter/fader and Monitor On/Off control. It is an annotated explanatory crop—not a full-screen target—and its older visual styling is not the OS 3.9 pixel reference.

![Akai MPC Sampler input-source and Monitor On Off controls](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-sampler-input-source-and-monitor-on-off-controls-7f4281c61ce5.png)

### Project Defaults — supplementary settings screenshot

**Evidence B, specialist MPC tutorial; exact hardware/firmware generation is not established.** This image exposes the New Project defaults taxonomy (tempo, sequence bars, time signature, pad slice, drum/keygroup filter and default plugin). Use it to inventory controls, not as a pixel-level MPC One OS 3.9 target. The current v3.9 manual's text at p.66 remains authoritative for behavior and labels.

![MPC Project Defaults settings including tempo, sequence bars, time signature and new-project dialog](MPC3-UI-REFERENCE-ASSETS/images/mpc-project-defaults-settings-including-tempo-sequence-bars-f1aad95c6ba7.png)

Source: [MPC-Tutor — How to Set Custom Time Signatures in MPC Sequences](https://www.mpc-tutor.com/how-to-set-custom-time-signatures-in-mpc-sequences/).

### Project Load/Save — current settings, timeout menu and save progress

**Evidence A, official Akai MPC 3 support screenshot.** The first image shows the Project Load/Save Preferences page with new-project behaviour, Auto Save and its timeout selector. It is a direct view into this settings family and complements the text-led pages in the v3.9 guide. It does not provide a separate image for every preferences category or every selector state.

![MPC Preferences Project Load Save Auto Save and timeout selector](MPC3-UI-REFERENCE-ASSETS/images/mpc-preferences-project-load-save-auto-save-and-timeout-sele-48ed794ec671.png)

Source: [Akai support — Saving Projects with Auto Save](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save).

The following crop makes the Temporary File Location row legible; use it only as a detail reference for this setting.

![MPC Project Load Save Temporary File Location row](MPC3-UI-REFERENCE-ASSETS/images/mpc-project-load-save-temporary-file-location-row-92d4b4620bb0.png)

**Auto Save progress modal.** This older-looking Akai support image documents the blocking save-progress state while Auto Save writes data. It is valid for the modal's purpose and progress feedback, **not** for copying its older red-header visual skin into the MPC 3.9 target.

![MPC project auto-save progress modal](MPC3-UI-REFERENCE-ASSETS/images/mpc-project-auto-save-progress-modal-1b777b813aac.png)

Source: [Akai support — Saving Projects with Auto Save](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save).

## 8.27. Supplementary MPC One hardware guide — version 2.13

The primary layout authority remains the current [MPC Standalone OS v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf). For contexts whose v3.9 chapter is text-led, a further **standalone hardware guide, Manual Version 2.13**, covers the MPC One / One+ and sibling hardware and contains many additional screen figures. We have added it as a supplementary image source, and the local screenshot archive renders mapped pages from it wherever downloads succeed.

- [MPC standalone hardware User Guide v2.13 — full PDF (524 pages)](https://manuals.plus/m/98138a9e32b84f6146aee38f6d2efd1ad8996d06f7ec4c5bf91ecb369b27a3c4.pdf).
- This is an older MPC 2.x-era interface, not an MPC OS 3.9 target. Its role is to supply extra screen imagery and explain control/workspace structure where the current manual only describes behavior in prose.
- Its contents include Main, Track, Program, Grid, Track View, Step Sequencer, List Edit, Browser, Sample Assign, Sampler modes (Sample/Slice/Pad Tap/Pad Hold/Auto Sampler), Looper, Pad/Channel Mixer, Pad/Track Mute, Next Sequence, Song Mode, Q-Link Edit, Project, Pad Color, MIDI Control, Pad Perform, Preferences and key dialogs.
- The generated gallery labels these full-page images as **legacy manual-page captures**. Where a page actually includes a UI figure, the image/crop is a visual reference; a text-only page capture is not represented as a standalone UI screenshot and cannot establish exact OS 3.9 geometry.

**Archived images:** use [the local VIS-001–VIS-082 gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md) to open the stored page captures/crops. The source PDF is not bundled into the repo or APK; the render outputs are stored locally in the documentation assets folder.

## 8.28. Locally archived screenshots/images from cited source pages

This section contains local image files discovered by scanning the support/tutorial/manual webpages already cited in the catalog, MPC3-9-OS-KNOWLEDGE-BASE.md and MPC3-REFERENCE-INDEX.md. The screenshots remain in docs/MPC3-UI-REFERENCE-ASSETS/images/ and are indexed individually. Source article links are retained so the context/version can be checked.

**Extraction result:** 239 additional images saved from 30 cited source pages; 11 image-download failures. The same URL is stored only once even when it is used in multiple documents.

### Source page: [https://heikkisiltala.net/wiki/en/music/akai_mpc_live_effects](https://heikkisiltala.net/wiki/en/music/akai_mpc_live_effects)

**heikkisiltala.net**

![heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/heikkisiltala-net-168660b8e2b7.png)

Original image: https://heikkisiltala.net/wiki/_media/wiki/logo.png

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-bb966d2ed93e.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effectsselect.jpg?w=800&tok=c985dd

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-a1ab2ec53260.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects01.jpg?w=800&tok=009d1c

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-f380299abd7d.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects02.jpg?w=800&tok=f40e5a

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-52c891ac77cb.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects03.jpg?w=800&tok=2b56dc

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-54b9c225a86b.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects04.jpg?w=800&tok=fa04ce

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-1314c673d5df.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects05.jpg?w=800&tok=4664ee

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-81ff05d07ce9.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects06.jpg?w=800&tok=c1e5c9

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-514046d401a7.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effects07.jpg?w=800&tok=3ae8a7

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-b355df976080.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effectsparams.jpg?w=800&tok=7fe0be

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-4e833ed8e67d.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effectsxypad01.jpg?w=800&tok=95fec2

**Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net**

![Reference image from A quick dive into the effects of Akai MPC Live - heikkisiltala.net](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-a-quick-dive-into-the-effects-of-akai-m-2facec04a44d.jpg)

Original image: https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effectscrash.jpg?w=800&tok=9988ba

### Source page: [https://marronfieldsproduction.hatenablog.com/entry/2022/10/25/063000](https://marronfieldsproduction.hatenablog.com/entry/2022/10/25/063000)

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-0bbb6b326080.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20230124/20230124054947.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-dde60b40d89b.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20220602/20220602050150.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-3ac8cee3b293.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20220602/20220602050322.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-8e8102113acc.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025032119.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-9185d311db1e.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025032807.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-478ba8c9e4a6.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025032955.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-62e6c5a35d5f.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025033532.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-fa791df768cd.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025033849.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-ad0985996f8d.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025034216.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-cc9f90e0c890.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025034608.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-a484f614d5e5.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025034745.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その8 〜NEXT SEQ&SONG編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-8-next-seq-so-32cbdc14572b.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025035612.jpg

### Source page: [https://marronfieldsproduction.hatenablog.com/entry/2022/10/28/063000](https://marronfieldsproduction.hatenablog.com/entry/2022/10/28/063000)

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-b8d10edbae2f.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20230124/20230124054722.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-ae48ba0e37a2.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028032058.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-441156675357.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028032159.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-20bdfcb36ee7.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028032407.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-19c0f7885194.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028032531.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-35cfd4f56d71.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028032927.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-3118092be7f8.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028033844.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-743b4fa68c49.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028034040.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-e8ce9b41ed68.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028034205.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-138b9ccfa760.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221027/20221027050438.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-52c54710b3d5.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028034450.jpg

**Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ**

![Reference image from AKAI Professional MPC ONE アカイ 操作方法 その9 ～SAMPLE EDIT / TRIM編～｜初心者でもわかる 解説 - くりっぱーとろにくすのブログ](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-professional-mpc-one-9-sample-edit-c2aeb8a04a2c.jpg)

Original image: https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028034610.jpg

### Source page: [https://note.com/suitebossa/n/nde0f912fe4ce](https://note.com/suitebossa/n/nde0f912fe4ce)

**見出し画像**

![見出し画像](MPC3-UI-REFERENCE-ASSETS/images/reference-8bc8c5d52509.jpg)

Original image: https://assets.st-note.com/production/uploads/images/139231419/rectangle_large_type_2_f2f3b59b86d795e0ba226c81891ee30c.jpeg?width=1200

**くり**

![くり](MPC3-UI-REFERENCE-ASSETS/images/reference-2f8dbe11dec1.jpg)

Original image: https://assets.st-note.com/production/uploads/images/168448251/profile_92cb290dc9b192092447651b988f62ae.jpeg?width=60

**画像**

![画像](MPC3-UI-REFERENCE-ASSETS/images/reference-89df087d5279.jpg)

Original image: https://assets.st-note.com/img/1714572254880-afLUx6An8g.jpg?width=1200

**画像**

![画像](MPC3-UI-REFERENCE-ASSETS/images/reference-53c000dc6cbc.jpg)

Original image: https://assets.st-note.com/img/1714612206661-ebdQsLmxIU.jpg?width=1200

**Reference image from 【自作曲】古典的なワンループではじめて作ったインスト・ヒップホップ(長文記事)｜くり**

![Reference image from 【自作曲】古典的なワンループではじめて作ったインスト・ヒップホップ(長文記事)｜くり](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-24f549d57dda.jpg)

Original image: https://assets.st-note.com/production/uploads/images/134564786/d2ceb5de7770f65b2224581a2a9ee8b4.jpeg?width=80&dpr=2

**Article image: 【自作曲】古典的なワンループではじめて作ったインスト・ヒップホップ(長文記事)｜くり**

![Article image: 【自作曲】古典的なワンループではじめて作ったインスト・ヒップホップ(長文記事)｜くり](MPC3-UI-REFERENCE-ASSETS/images/article-image-1f654c04c1e8.jpg)

Original image: https://assets.st-note.com/production/uploads/images/139231419/rectangle_large_type_2_f2f3b59b86d795e0ba226c81891ee30c.jpeg?fit=bounds&quality=85&width=1280

### Source page: [https://ragtime.online/akai-professional-mpc-one-standalone-sampler-and-sequencer.html](https://ragtime.online/akai-professional-mpc-one-standalone-sampler-and-sequencer.html)

**Ragtime logo**

![Ragtime logo](MPC3-UI-REFERENCE-ASSETS/images/ragtime-logo-9173441997f4.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/ASSETS/ragtime_bqZjUoX50.png

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-c74e8a62f6d0.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/audiocard_2_1zNrtkynM.png?updatedAt=1628533398181

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-40b2ddd3e9c9.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/mgame_RSjRqrJaR.webp

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-4966618f5760.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/mic_3_m_mEWLmsm.png?updatedAt=1628533398410

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-8c1b55ee2f2a.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES_BLOCKS/EWI_KfU67EOiX.webp?ik-sdk-version=javascript-1.4.3&updatedAt=1642689250529

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-8c8941c897ed.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/controller_2_fBSHIYGr8M.png?updatedAt=1628533399732

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-d9061da7bef2.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/pads1_1_ZQTc8tJ5Z.png?updatedAt=1628533398547

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-6f0d1fce2f82.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/controller_mixer_1_1_dRQ9BN84l.png?updatedAt=1628533396991

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-1e292e5a21f8.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/vocalizer_c0qbqy-ny.webp

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-56c2cac2cea6.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/monitors_1_jYB4fWq0l.png?updatedAt=1628533399874

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-f8608a850a72.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/headphones-compressor_3laPkdWjqf.png?updatedAt=1628533397610

**Thumbnail Image**

![Thumbnail Image](MPC3-UI-REFERENCE-ASSETS/images/thumbnail-image-5ce02f8967ff.png)

Original image: https://ik.imagekit.io/n5sapb3cshf/CATEGORIES/FORCE_1_jmMwH-2OQ.png?updatedAt=1628533400140

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs](https://support.akaipro.com/en/support/solutions/articles/69000804211-akai-pro-mpc-series-understanding-and-loading-programs)

**Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-and-l-6cf7de173186.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135880685/original/--VWD-WNhZ0ECwUKriyzLtcT7WdH1HeLfQ.png?1732016335

**Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-and-l-5136d8e4ef8d.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69135881223/original/OBU4OIPLsVppo0XE-rgOgxdwLUu5JvMD3Q.png?1732016692

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000822965-akai-pro-mpc-series-adjusting-project-defaults](https://support.akaipro.com/en/support/solutions/articles/69000822965-akai-pro-mpc-series-adjusting-project-defaults)

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-dde85bc91c52.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000822965-akai-pro-mpc-series-adjusting-project-defaults/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000838263-akai-pro-mpc-series-standalone-mode-sampler-overview](https://support.akaipro.com/en/support/solutions/articles/69000838263-akai-pro-mpc-series-standalone-mode-sampler-overview)

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-2374295f235e.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69100429123/original/4rZhrvv0dtkrTWSZicSEtAx1hSO3XjKEpQ.png?1704386540

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-561060eb9518.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69075163375/original/tP8Gznhoc4AujoPV18pa0M4lGw_Fi4F1pQ.png?1683839460

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-5198f69fea78.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69100430913/original/5R6DYHPdhe1aEthhqWTvonA0dFNHobHu2w.jpeg?1704387141

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-e835795d63cb.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69075165589/original/YFSMdgWyQKiv9I98J_Jl4nbGEHYgQBj7LA.png?1683841018

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-f9bf513c4eed.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69097883202/original/YXtMpzQlXVKqibkoEwM5xONCahU9xVTGtw.png?1702073919

**Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional**

![Reference image from Akai Pro | MPC Series | Sampler Functionality Overview  : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-sampler-functionali-922b68100480.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69078266836/original/U4Wbu-DH6ji3PLWqOJzhpc4j5eYzUS63tg.png?1686345856

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-b6cfba7414fe.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000838263-akai-pro-mpc-series-standalone-mode-sampler-overview/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000856004-mpc-series-creating-custom-arp-patterns](https://support.akaipro.com/en/support/solutions/articles/69000856004-mpc-series-creating-custom-arp-patterns)

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-93a907c0a506.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69117560329/original/SD0nOB-VmjoCDMeZc3sbzMnscoqnYhGUiw.png?1718117081

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-6a56d2eb27e2.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69117558761/original/X6YzVYIHcCvnono-XLBBuTKCLaMlEfU8gw.png?1718116604

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-c66a596de4ab.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69117558608/original/9s1MRfk1vI0K_6ubpYauxjR2bCN_UBPCSQ.png?1718116566

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-2646d14d78bc.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194801033/original/NcJsWEGW4E8GKzQShSO1Ex2Ek7wkMdv6Cg.png?1782213213

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-d8c1b41c823c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183845836/original/TzsAGGXtkTO9JX28cqZwoErGeA_jO-gqDQ.png?1770829495

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-699f7e26cb2f.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183845883/original/1CBDKgfMCNUAdBNHZpqgpzTbHev99vPi9g.png?1770829512

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-374074717b12.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183845903/original/U4EH5E56I1vo9QRhVnF9RmjmpevkdBHgpQ.png?1770829523

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-eace82204ad5.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183844308/original/pzqsDDE_MtUwNphVo0AXb2yNqvYhpCi5Aw.png?1770828894

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-da936d49a65e.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183843427/original/3nHMpT9SDYE3Kr8Dn568FZHOPxjHegChew.png?1770828553

**Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional**

![Reference image from Akai Pro MPC Series | Creating and Loading Custom Arp Patterns : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-creating-and-loadin-8d53b514772d.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183843429/original/zlVCUYNNPAr2oFzTgPxlHYGFsl-FshPQYA.png?1770828554

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-b8ada76d27c3.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000856004-akai-pro-mpc-series-creating-and-loading-custom-arp-patterns/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome](https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome)

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-c08a6b15ab8a.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183336529/original/9OALv9ECy6OaavnivWSF3p10-q-w6U3-1w.png?1770305029

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-edef23cd1de4.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183340020/original/pQn0HBwlzebiIWTGhqC88eTAfEhpgB9Xow.png?1770306467

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-4af145ca241f.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183339772/original/GCZ65DucfgWfrdcfOzVVl5fZhMNCe2RSLQ.png?1770306350

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-76aad4363b86.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183338703/original/C9jY-JC1NZ48_45iDBSAU8uYXQh9eRCcMA.png?1770305858

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-ec253ddcacbe.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183340900/original/Wv7NG_ATIctjoSG_uwVxwf23lmjY22OTCg.png?1770306769

**Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional**

![Reference image from Akai Pro MPC Series | Editing The Count-In And Metronome : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-editing-the-count-i-c8cca84fd4e2.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183338467/original/lC-ZlDHgHPzelAnfopCNFuSFcNBUCA7YPQ.png?1770305766

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-38df0dccf7ba.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000857945-akai-pro-mpc-series-new-project-dialog](https://support.akaipro.com/en/support/solutions/articles/69000857945-akai-pro-mpc-series-new-project-dialog)

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-8d23e9e8525c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69168497713/original/s9gP8pA76ajOWSNlcUpsc-Cy6I7xnj3S_w.png?1757329977

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-3252e2ea5f30.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69138813912/original/TFeEVQpnJVND2eHfJqO0YbHWSgiJ9KFWvw.png?1734019440

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-bdd543fbf0bc.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69138813984/original/gX_OQmumVTxn8tS9Db0i6LbmpCAwXHoOgg.png?1734019463

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-3361903de08a.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69168497633/original/losLbj-hGvLzhujOiTJoGEADHmkCLyMwlw.png?1757329912

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-cac16c42a331.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69168497555/original/1B5Ebe8l-fxKf8RUdYXAAru39U1WG5118Q.png?1757329862

**Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional**

![Reference image from Akai Pro MPC Series | New Project Dialog : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-new-project-dialog-f8f08e7f59ee.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69168497526/original/Go4z7ExFaEGKbgu1F2FPTXPQrara8DcZuQ.png?1757329832

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-6d9f690444c5.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000857945-akai-pro-mpc-series-new-project-dialog/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000860101-mpc-standalone-input-monitoring-explained](https://support.akaipro.com/en/support/solutions/articles/69000860101-mpc-standalone-input-monitoring-explained)

**Reference image from MPC Standalone | Input Monitoring Explained : Akai Professional**

![Reference image from MPC Standalone | Input Monitoring Explained : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-mpc-standalone-input-monitoring-explain-c83279d6a1fd.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69128760784/original/ZxmnIeI0mSiXFQy513lN30sfQ0ai0ObVWg.png?1726736736

**Reference image from MPC Standalone | Input Monitoring Explained : Akai Professional**

![Reference image from MPC Standalone | Input Monitoring Explained : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-mpc-standalone-input-monitoring-explain-1c15fd63b61c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69128761005/original/dJGjDMzu9eRB4EcBIUH1weSfWGrPgM9FrQ.png?1726736969

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-f6c07e83932c.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000860101-mpc-standalone-input-monitoring-explained/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000860953-mpc-standalone-the-best-way-to-save-and-back-up-projects-to-an-external-drive](https://support.akaipro.com/en/support/solutions/articles/69000860953-mpc-standalone-the-best-way-to-save-and-back-up-projects-to-an-external-drive)

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-bcebd6802e55.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000860953-mpc-standalone-the-best-way-to-save-and-back-up-projects-to-an-external-drive/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000863026-akai-pro-mpc-series-humanizing-your-tracks](https://support.akaipro.com/en/support/solutions/articles/69000863026-akai-pro-mpc-series-humanizing-your-tracks)

**Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional**

![Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-humanizing-your-tra-eeba2084eb80.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69137425072/original/UbA7URmBzaEGtijRhATbUeKqQJQobByonw.png?1733155392

**Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional**

![Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-humanizing-your-tra-c5f6f2d426e8.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69137426686/original/humvbeR61kDk-G8I68zs0hLdjdGCcg7ImQ.png?1733155939

**Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional**

![Reference image from Akai Pro MPC Series | 'Humanizing' Your Tracks : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-humanizing-your-tra-822b92eb37cf.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69137426726/original/1y3YOgrXGdUJP1cg_QpsiaDb8R7TyBn95Q.png?1733155950

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-e3b7e9c1868a.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000863026-akai-pro-mpc-series-humanizing-your-tracks/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000864104-akai-pro-mpc-series-how-to-delete-a-sequence-track-or-program](https://support.akaipro.com/en/support/solutions/articles/69000864104-akai-pro-mpc-series-how-to-delete-a-sequence-track-or-program)

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-1d26ac45afab.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69140019339/original/j0bMsqqvr-TJu9C0QKZHohzDiUWD9yXRTg.png?1734950875

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-e784f7b51b09.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171567388/original/UsXKoEIoE5RZ1myEv2B_n5tTWxMGcLIJtQ.png?1759737743

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-68d739ab1fed.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171567821/original/Gn_oosMLkpAb2-Jd4S9CQs8fcCCtzKUJ0g.jpeg?1759738112

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-5d5155f92c0e.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568345/original/y9ooBl_93rautQ01WZCrR0X81Z86U1iGwg.png?1759738577

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-f03a826284ab.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568606/original/TKMwaGq0Sqgh2kqcKpJd-Nzu3qgFJw4qaA.png?1759738674

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-2fc90603d5dc.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568665/original/qU8ICjwRpGJMfeQyYczMd8Um6tUps0nI_g.jpeg?1759738732

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-ca34324c9562.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568002/original/p4XyPOCyXsuXuyqyDqTpVf9cmkKEgnK2jg.png?1759738294

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-bf1fd98526b9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568221/original/092V3ySe4dRRHrDCC6t0B9llhaitkVEB9A.png?1759738445

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-b0483dbbc6ce.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171568182/original/t-_VFLG4EIhuPp8h_fPRI7ihBixo1sSY4w.png?1759738407

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-447435f3fdd4.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69140020909/original/Jxr3VhacqMWPVR3RUqJyp8VUb9XnrSbRmQ.png?1734952287

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-98966220bda1.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69140021061/original/fEYSHit4HJ-LKyQAkb7GurvY5qXAYLmfDA.png?1734952367

**Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Delete a Sequence, Track or Program : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-delete-a-seq-10ef956a04b0.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69140020963/original/xc34RBN24CyRNFXSxKUQuc2pGoUDlPom5w.png?1734952299

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save)

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-8fbf1577b725.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69141760223/original/5DzwF5Vl2plrpntAD1xcK-rgv-pIWaoCsQ.png?1736424524

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-6decf40d0689.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69155310481/original/UHQkJ3Q7dOCU3OYKqq7-M1tT736gXs-XMg.png?1746635795

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-8fa71f515adc.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69155310100/original/Eje2-0LPl4-jsEQVNg9NQ3UiBxhAMjIW3A.png?1746635653

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-71a5589363b9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69141762232/original/FtmdsIpb1y5Th0qupBLf7AvPPW5lb_td2A.png?1736425797

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-969e411634cf.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171579905/original/R2eyYY8WETnzKOJdNxR8jTD5h_4JbjeaPQ.png?1759748116

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-d44f92192dc6.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69171579830/original/_1hlmRTPC8LbNGK4jUaBX5-5MfWFy0lwlQ.jpeg?1759748044

**Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional**

![Reference image from Akai Pro MPC Series | Saving Projects with Auto Save : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-saving-projects-wit-bad7c83ccfb4.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69141762124/original/2iTHEiuc9sSIugbtfuTebJWIKStJ-QeOAQ.png?1736425733

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-5ef5e3f1e7ea.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000864879-akai-pro-mpc-series-setting-up-multiple-midi-controllers-in-a-project](https://support.akaipro.com/en/support/solutions/articles/69000864879-akai-pro-mpc-series-setting-up-multiple-midi-controllers-in-a-project)

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-c76d6059ca34.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142812363/original/1jNDcp3DWndmGYbzqXUcgziZiKkbzl8Irg.png?1737115796

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-8e89b6be9915.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142814500/original/NeT4Qp9Bv9gqN4GOjnx72Vt5jkDTrcNSug.png?1737117257

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-09a02872c68c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142815523/original/SjTiVXvRKaaQJA95oESfkEuVopgwGMAOvw.png?1737117788

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-e1f64443ae0a.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142815419/original/hf7V-9PefY1a8-QKlsZ4ztiG-cyEYMnHLg.png?1737117724

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-6e371104af1d.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142815265/original/9_C128NPeuPkqr5Vim5fH9hCBzKVlJV3Vg.png?1737117618

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-e0ea4a916cc8.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142815693/original/7h6P3bbdcjqCAnqH-2XCrMNySXM8X25BCA.png?1737117888

**Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional**

![Reference image from Akai Pro MPC Series | Setting Up Multiple MIDI Controllers in a Project : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-setting-up-multiple-38e95205964c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69142816270/original/wxwPe9HvTpk__66jFjy3-2aeJDnWaaRv9Q.png?1737118278

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-fee1b6b0275e.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000864879-akai-pro-mpc-series-setting-up-multiple-midi-controllers-in-a-project/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000867660-akai-pro-mpc-series-wireless-remote-midi-control-in-a-daw](https://support.akaipro.com/en/support/solutions/articles/69000867660-akai-pro-mpc-series-wireless-remote-midi-control-in-a-daw)

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-eef1fffeb6c7.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151186760/original/AkpAn0Ntc3SG8uSy6fNzfzT_aE9c4raaYw.png?1743504069

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-5c3e048199c2.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151187334/original/K3Rj4HLJKoojJOw3JZOQb0U20nqFNTexuQ.png?1743504538

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-66c74bd91257.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151187211/original/BngsM20Ey5AoiSOQ6Ng9QMlIXTz7nOgLmA.png?1743504469

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-42ba200e2db6.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151187913/original/qUWciG6cOb30KJXSUof5XWV_BAkPclk0yA.png?1743505244

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-3da1c47bb0d9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151186799/original/eYgyAJMX8iAgGaJ_jiNnYv3r5_LeefVWwA.png?1743504115

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-ee9a89373da6.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151187775/original/bWMxpfT1B9GBMXzBNgwtNR6gdX8VxPHFnA.png?1743505090

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-fd9131568acc.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151186800/original/_WnV-1MPjO3HVPbQ6y90cq9dXL3wMOZuiQ.png?1743504115

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-6718b985729e.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151186798/original/PoH5YUudKqUSP1le_4fhx7BCP0esrOK-tg.png?1743504114

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-e4cd4ba21baa.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69190673208/original/RLe-F3O144aQzcB5Gn-mszkX0C2alWpF2A.png?1777983107

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-f3a555bdf3c9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69190775939/original/FQy_CVtxNQ7Vt-vgCZV0ky8m0mD9Cdh7sw.png?1778054429

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-f8713f22bc94.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69190775933/original/6NMzzp_oiTF9qkf4r71f2Y1Dx34ZU0HNow.png?1778054422

**Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional**

![Reference image from Akai Pro MPC Series | Wireless Remote MIDI Control in a DAW : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-wireless-remote-mid-8bca09dbd6ba.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69190669001/original/aa02Ne5CQgtH4b1wy9fI9bEHzVzysuT15A.png?1777979753

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000867733-akai-pro-mpc-series-how-to-remap-the-mpc-pads](https://support.akaipro.com/en/support/solutions/articles/69000867733-akai-pro-mpc-series-how-to-remap-the-mpc-pads)

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-29beaf29335c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69151535413/original/UTJhTkQJ-wEES7c6GIt_KOLNol5DrxgoaA.png?1743704482

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-d7c8a43bcc0a.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69163831414/original/4EhW8OAPsp_w5xfyACluZzGEQa7xS8vgbg.png?1753374720

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-e12e79065324.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154038300/original/Ki9hF9rbz6WETan4C1Ss1XgFdQhMnHRQrw.png?1745586869

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-8cdfffb08c49.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154038298/original/mky7nQluOANBF2EUet92nu5QLCTeHRRkCg.png?1745586868

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-11c157601099.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154040375/original/2J9YPl-yv2ejMhc4DL_oPpUvv-Y0eZIbmw.png?1745587740

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-ab6a04b74bc1.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166325243/original/QL8lez1_hVtdJgH4rAwovenOZqPDXSdo8g.png?1755524199

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-6ad6077ebb14.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166324270/original/kNUu1xaqdEDVXANfWhUskMdG8i6-GErlOQ.png?1755523845

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-017f70ca5ae0.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69190453365/original/E6HpoK2SGvl0X05G_-AfNzwCo7XQHCAR6A.png?1777651523

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-9532706e26a8.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166437339/original/1EemteCTrdXhHFXXbCah8Yh3jJlA4lVXvA.jpeg?1755595085

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-5bbf51a89fba.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166324277/original/6NjC6-cRea38vgWsZYANVNmmRnv9QbtgLw.png?1755523845

**Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional**

![Reference image from Akai Pro MPC Series | How To Remap the MPC Pads : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-remap-the-mp-bfb5cdbabfe1.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69166324278/original/6gcBmaxHOXh5-Q_eHBtDyVi4RcP7ALasuQ.jpeg?1755523845

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-542b69c2a17d.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000867733-akai-pro-mpc-series-how-to-remap-the-mpc-pads/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics)

**Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-output-routing-basi-200055e7dfb7.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154225445/original/3mk7s_BsMWUg1W1gHm6LeHzsuXIP_rAf2Q.png?1745833538

**Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-output-routing-basi-f50f87ff302d.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154227724/original/W4p-2vkRxIavIh5ZOK9e8ZvP0fZ0iVPVxA.png?1745834845

**Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-output-routing-basi-d43a961939a8.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154228216/original/_Q0Nkv5wT-Q07GwDf-Y_gJ485Ppj4izTLQ.png?1745835220

**Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-output-routing-basi-a0f26bc85d5f.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154227632/original/sTGW3i1QnQ9AZCuZGYo7gTgxjB0WsDz--A.png?1745834797

**Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Output Routing Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-output-routing-basi-ffe303c8aaee.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154227792/original/VL1SCTrvG9pDWp63LaUnyBLJutW8FVQ0Jw.png?1745834889

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-5429298f8f2a.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000870813-mpc-3-standalone-how-do-i-turn-off-the-tap-tempo-light-](https://support.akaipro.com/en/support/solutions/articles/69000870813-mpc-3-standalone-how-do-i-turn-off-the-tap-tempo-light-)

**Reference image from MPC 3 Standalone | How Do I Turn Off The Tap Tempo Light? : Akai Professional**

![Reference image from MPC 3 Standalone | How Do I Turn Off The Tap Tempo Light? : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-mpc-3-standalone-how-do-i-turn-off-the-f2f308c1e656.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69161763950/original/pWP6-5PdcH9pY2cA4pGmjd4H5xZz47NkEw.png?1751856629

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-a775c2e937fa.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000870813-mpc-3-standalone-how-do-i-turn-off-the-tap-tempo-light-/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000871929-akai-pro-mpc-series-how-to-audition-expansions-and-samples](https://support.akaipro.com/en/support/solutions/articles/69000871929-akai-pro-mpc-series-how-to-audition-expansions-and-samples)

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-802e4b6a0564.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165714058/original/q1WC0gJ9WdE08IGvYEwz2lfWFKfVy9YHLg.png?1754991265

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-74b89924e3be.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194479504/original/d9jfTSWS1ofKZnm40uQ7LJWS6scMDM9R2Q.png?1781791465

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-b33286661cff.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194477736/original/LhkqSuZshb5JiW4LoonLsHkfvVUjzKEvuA.png?1781790791

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-609474f6e2f0.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194479105/original/h1GgGHoMqcQ6kApurqbDjByl2zVMnkN74g.png?1781791308

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-c325650cd188.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194477737/original/kqmDN2C26Se9bZdMOi8RLKb_BGQccB_Z9Q.png?1781790791

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-5973dab2840f.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69194478713/original/mHGM3MMMNLY9QGuJBkeXKdGGDmKptUGKrw.png?1781791146

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-e244b340d177.jpg)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165714543/original/KK0OeRfzTCKtlw6cbQNyowAY7Mp0cyD0hA.jpeg?1754991706

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-618e9dc1d009.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165714830/original/HRPM15H3QGx5rCFgejfZXmyueyaB6wzaJA.png?1754991934

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-9e8527a158a7.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165715252/original/PctcLmtjGUy7sr9Hxe2QcJbLCDmXjJu5sQ.png?1754992291

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-6e343bbf7bde.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165715352/original/vnmgZPomzoQKAEZ16fpH_0Y8N0A6AE-DDA.png?1754992391

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-35db5d3928fa.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165715554/original/NWURoLiusABAG6rrubXbuxf-pmUkMGliKw.png?1754992557

**Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional**

![Reference image from Akai Pro MPC Series | How to Audition Expansions : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-how-to-audition-exp-bd5406baeb29.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165715626/original/ZgepHM1fsT3jM9N1rop6oSpx8F81swSJgQ.png?1754992624

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser](https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser)

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-ff0b672171f4.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165717071/original/L9CzCR5Bf55d7HLvFy7-HGCFQN7Iz7-kHg.png?1754993728

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-d3fb3f5bb924.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165721645/original/yUiQI0jMqr8m4np4n8MpRcu_NPS3FDsx0Q.png?1754996767

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-17ece7a890f6.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165722181/original/w67qMx9X5NaS3yf3_Efh5FUtSujKqosltQ.png?1754997241

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-85e027b47bc9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69177984964/original/iLqdSWSEhgtkBJkc5rRpi5_3L-xx41uWXg.png?1764935456

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-ae0958273dd8.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165719905/original/-BBHK1y9CkqUgmPuiShYZjuXKQpaFT1gFw.png?1754995552

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-5a83303ae776.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165719916/original/LpsBnLwwLU5Antxb5wdSSQK_9pafduji-Q.png?1754995559

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-e76c345d3dc5.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165720834/original/bfmemXwyNk0Svwn6FERr4Hipe0u6iqoj7w.png?1754996187

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-578a3aa922b7.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165717538/original/03TYqionEXfsV6ARKxTlp1yoNQCUSEZXXQ.png?1754993997

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-4a048dffc741.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165717412/original/nPUeZTX2NKRVboS-wO9S1keK9pXbIUuZIQ.png?1754993912

**Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional**

![Reference image from Akai Pro MPC Series | Understanding the MPC's Browser : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-understanding-the-m-91bff1fc31c2.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69165716730/original/_c1kISWJxQzR99bZd8JmlBS0S0gVwNyBOg.png?1754993450

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-45896ffec2ff.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000871930-akai-pro-mpc-series-understanding-the-mpc-s-browser/hit

### Source page: [https://support.akaipro.com/en/support/solutions/articles/69000874731/](https://support.akaipro.com/en/support/solutions/articles/69000874731/)

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-31e45226f112.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69015392873/logo/QNLPi0l-L5pEK6YaEKpzsqVT5o9VQHMIHw.png

**no results**

![no results](MPC3-UI-REFERENCE-ASSETS/images/no-results-1763a160535a.png)

Original image: https://support.akaipro.com/assets/cdn/portal/images/no-results.png

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-a346f1ec422c.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903282/original/w7dKTM6i7KpUkxo3R_EIrJBN6GxEMDasLQ.png?1764064899

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-3a0cddccb586.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903693/original/fDAI9ec7YKkbDo2LIIqgA0BrL4iyQxr8mA.png?1764065184

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-94052c0b9ed8.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903702/original/ny3KqM8sX1ElzZxk1Tucq1PbNPCVj0fOng.png?1764065192

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-49e412221904.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903713/original/tG8Q7SJWSKww5E-BtgLHjff7Z1Y0EL7Jkw.png?1764065208

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-c1597e02213d.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903727/original/-0xrv1nhFLQ1XfkWCRFkoCgZQe0zfjr4LA.png?1764065223

**Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional**

![Reference image from Akai Pro MPC Series | Tuning Basics : Akai Professional](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-pro-mpc-series-tuning-basics-akai-2333e0c442a9.png)

Original image: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69176903838/original/1D9nGWA-0xbd6jQDjGrq-Dx-7mDgGbPoew.png?1764065305

**Article views count**

![Article views count](MPC3-UI-REFERENCE-ASSETS/images/article-views-count-aa06be8cb24f.gif)

Original image: https://support.akaipro.com/en/support/solutions/articles/69000874731-akai-pro-mpc-series-tuning-basics/hit

### Source page: [https://www.elektronauts.com/t/mpc-thread-mpc-live-mpc-x-mpc-one-part-2/173139?page=142](https://www.elektronauts.com/t/mpc-thread-mpc-live-mpc-x-mpc-one-part-2/173139?page=142)

**IMG_6703.HEIC**

![IMG_6703.HEIC](MPC3-UI-REFERENCE-ASSETS/images/img-6703-heic-12a6eb6688a4.jpg)

Original image: https://eu5.dh-cdn.net/uploads/db8181/optimized/3X/1/e/1e164e0795f01647e995704c8e3fcd165621e935_2_666x500.jpeg

**IMG_6702.HEIC**

![IMG_6702.HEIC](MPC3-UI-REFERENCE-ASSETS/images/img-6702-heic-1d8e6b146a47.jpg)

Original image: https://eu5.dh-cdn.net/uploads/db8181/optimized/3X/7/3/739f6d1d05b21c98e4cf4c2e8e8a9b23304aaad3_2_666x500.jpeg

**IMG_6701.HEIC**

![IMG_6701.HEIC](MPC3-UI-REFERENCE-ASSETS/images/img-6701-heic-40d6a6adf49d.jpg)

Original image: https://eu5.dh-cdn.net/uploads/db8181/optimized/3X/1/7/179319931c38338bf40e517082720e5d2c5e6628_2_666x500.jpeg

**IMG_6700.HEIC**

![IMG_6700.HEIC](MPC3-UI-REFERENCE-ASSETS/images/img-6700-heic-eda488bc2c87.jpg)

Original image: https://eu5.dh-cdn.net/uploads/db8181/optimized/3X/a/d/ad74631173018693dbe056953e021487a1a52823_2_666x500.jpeg

**image**

![image](MPC3-UI-REFERENCE-ASSETS/images/image-5ebf2253150e.jpg)

Original image: https://eu5.dh-cdn.net/uploads/db8181/optimized/3X/4/9/495de4c5006d77ef619d643c4c76fb1e531e3a31_2_690x455.jpeg

**AraabMUZIK MPC Instrumental Beats are DISRESPECTFUL**

![AraabMUZIK MPC Instrumental Beats are DISRESPECTFUL](MPC3-UI-REFERENCE-ASSETS/images/araabmuzik-mpc-instrumental-beats-are-disrespectful-493404f5c0c5.jpg)

Original image: https://img.youtube.com/vi/7r1I3GwKl-M/maxresdefault.jpg

**:slight_smile:**

![:slight_smile:](MPC3-UI-REFERENCE-ASSETS/images/slight-smile-258b6480d79d.png)

Original image: https://www.elektronauts.com/images/emoji/twitter/slight_smile.png?v=9

**MPC 2.11.8 Update**

![MPC 2.11.8 Update](MPC3-UI-REFERENCE-ASSETS/images/mpc-2-11-8-update-cfd5b74af4e2.jpg)

Original image: https://img.youtube.com/vi/sFC48c8S1cg/maxresdefault.jpg

**:expressionless:**

![:expressionless:](MPC3-UI-REFERENCE-ASSETS/images/expressionless-ac43e2a42e92.png)

Original image: https://www.elektronauts.com/images/emoji/twitter/expressionless.png?v=9

**Article image: MPC Thread : MPC Live - MPC X - MPC One (Part 2) - Page 142 - Other Gear - Elektronauts**

![Article image: MPC Thread : MPC Live - MPC X - MPC One (Part 2) - Page 142 - Other Gear - Elektronauts](MPC3-UI-REFERENCE-ASSETS/images/article-image-mpc-thread-mpc-live-mpc-x-mpc-one-part-2-page-bf6a1083858b.png)

Original image: https://eu5.dh-cdn.net/uploads/db8181/original/3X/6/5/653ee57cf417d28eee2d69cd6f2aca0dc55d5fd1.png

### Source page: [https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC](https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC)

**Reference image from 10 ways to get more out of your MPC hardware | MusicRadar**

![Reference image from 10 ways to get more out of your MPC hardware | MusicRadar](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-10-ways-to-get-more-out-of-your-mpc-har-7709011117e5.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/flexiimages/zdve3chhhn1770884186.jpg

**Reference image from 10 ways to get more out of your MPC hardware | MusicRadar**

![Reference image from 10 ways to get more out of your MPC hardware | MusicRadar](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-10-ways-to-get-more-out-of-your-mpc-har-1aed1c730c34.png)

Original image: https://cdn.mos.cms.futurecdn.net/flexiimages/duor0csveo1781690031-400-80.png

**Akai MPC**

![Akai MPC](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-147c4566f031.png)

Original image: https://cdn.mos.cms.futurecdn.net/UnUzoRX4AyS7vHY69ZhkbG.png

**Akai MPC**

![Akai MPC](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-9d3f4d676642.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/pFYHKeyGe33zd8ypBvFKoi.jpg

**Akai MPC Sample**

![Akai MPC Sample](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-sample-f353d98b76dd.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/m2BWDNwisxvW63imPTCJq4.jpg

**Drum recording**

![Drum recording](MPC3-UI-REFERENCE-ASSETS/images/drum-recording-2ed3aa8aeaa0.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/hyxVDB8REfFckjAqXRz8Wk.jpg

**Richard Barbieri studio**

![Richard Barbieri studio](MPC3-UI-REFERENCE-ASSETS/images/richard-barbieri-studio-41620a08cedd.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/YncCt5wL7gRcqzetHBq5rf.jpg

**Akai MPC**

![Akai MPC](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-07ac001a859b.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/oC9sLkM9L8r7nj9abQQTsj.jpg

**DJ Shadow**

![DJ Shadow](MPC3-UI-REFERENCE-ASSETS/images/dj-shadow-e4aa4d90d0c6.png)

Original image: https://cdn.mos.cms.futurecdn.net/wYy6FxsUND2ZNVtLnDsEWn.png

**An Apple Mac Mini M4 on a desk with an audio interface, studio monitors, and studio headphones**

![An Apple Mac Mini M4 on a desk with an audio interface, studio monitors, and studio headphones](MPC3-UI-REFERENCE-ASSETS/images/an-apple-mac-mini-m4-on-a-desk-with-an-audio-interface-studi-09894a04865f.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/p7RPnbUDWdjSP5pfmcgoYA.jpg

**Recording**

![Recording](MPC3-UI-REFERENCE-ASSETS/images/recording-1afae341a1ef.png)

Original image: https://cdn.mos.cms.futurecdn.net/cS2pj8wYRqbosx3wzFocwn.png

**Akai MPC**

![Akai MPC](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-33b335b8efbc.jpg)

Original image: https://cdn.mos.cms.futurecdn.net/SYiUWYzjb9HKYvNkhUZuNi.jpg

### Source page: [https://www.soundonsound.com/reviews/akai-mpc-live-iii](https://www.soundonsound.com/reviews/akai-mpc-live-iii)

**Home**

![Home](MPC3-UI-REFERENCE-ASSETS/images/home-903e0bab8794.png)

Original image: https://www.soundonsound.com/sites/all/themes/sound_on_sound/images/logo-mobile.png

**Akai MPC Live III**

![Akai MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-live-iii-78d0649df73d.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/A/AkaiMPCLiveIII_01-cMMaFawLqw6rI9UVzWpokR0xgsdRda0R.jpg

**The Live III measures 435 x 256 x 67mm and weighs in at 3.9kg.**

![The Live III measures 435 x 256 x 67mm and weighs in at 3.9kg.](MPC3-UI-REFERENCE-ASSETS/images/the-live-iii-measures-435-x-256-x-67mm-and-weighs-in-at-3-9k-2674e778d6d5.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/A/AkaiMPCLiveIII_05-HqfqYfz6xzqdKI8KCB9mFo3mEi_5e0kD.jpg

**The Matrix page is central to Live III’s new clip‑based workflow.**

![The Matrix page is central to Live III’s new clip‑based workflow.](MPC3-UI-REFERENCE-ASSETS/images/the-matrix-page-is-central-to-live-iii-s-new-clip-based-work-3066e875d303.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/A/AkaiMPCLiveIII_03-WCBLXae7hYcy_skvOVMkvfjC.coIR_uV.jpg

**The redesigned Main view now gives you a focused view of the clip or track you’re working on.**

![The redesigned Main view now gives you a focused view of the clip or track you’re working on.](MPC3-UI-REFERENCE-ASSETS/images/the-redesigned-main-view-now-gives-you-a-focused-view-of-the-f6503b70230f.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/A/AkaiMPCLiveIII_04-Rt2NBnwa2Jzh5sLrPOKFoYzOtG3pP2Z8.jpg

**Akai MPC Live III**

![Akai MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-live-iii-11db9808b25d.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/A/AkaiMPCLiveIII_02-LLciSxodnQdNIk0WDTn8hH6JY0drfUAw.jpg

**Sweetwater Affiliate logo 14px**

![Sweetwater Affiliate logo 14px](MPC3-UI-REFERENCE-ASSETS/images/sweetwater-affiliate-logo-14px-c324200b5b65.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/sos_original/s3/imagelibrary/s/sweetwater-affiliate-logo-bin14px-2G2FWZFhOPBFCjMP26oh4F8zSehELano.jpg

**G4M logo**

![G4M logo](MPC3-UI-REFERENCE-ASSETS/images/g4m-logo-3415eb2a398c.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/sos_original/s3/imagelibrary/g/g4m_logo_110x54-fqTOe1qRN47cL5wmxxowzfD.J8.T7M0T.jpg

**Reference image from Akai MPC Live III**

![Reference image from Akai MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-mpc-live-iii-b35383e5357c.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/300px_wide_plain/s3/media-youtube/kRe9hGWqdMM-CzKvp4TcN4ecZvsF4xm0biSQynDWXaDg.jpg

**Reference image from Akai MPC Live III**

![Reference image from Akai MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-mpc-live-iii-5d0bd4f1a8e0.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/300px_wide_plain/s3/media-youtube/7LP3RtDPLlg-tmrkwO7RwaiKfBOt8wMVfdiVQymrvXNn.jpg

**Reference image from Akai MPC Live III**

![Reference image from Akai MPC Live III](MPC3-UI-REFERENCE-ASSETS/images/reference-image-from-akai-mpc-live-iii-8eaa972e4bae.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/300px_wide_plain/s3/media-youtube/2VNvwEolM3s-ESBHqW4XnM_BxcGdVfFQuCIY_tQukIoD.jpg

### Source page: [https://www.soundonsound.com/techniques/akai-mpc-basics](https://www.soundonsound.com/techniques/akai-mpc-basics)

**Akai MPC Basics**

![Akai MPC Basics](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-basics-ec6617241ec6.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_01-vc.CXVyiCNisVk_X1pA.IxZ82IbPhTFw.jpg

**Screen 1. The Main screen focuses on one Track at a time, and is divided into the three building blocks of an MPC Project: Sequence, Track, Program.**

![Screen 1. The Main screen focuses on one Track at a time, and is divided into the three building blocks of an MPC Project: Sequence, Track, Program.](MPC3-UI-REFERENCE-ASSETS/images/screen-1-the-main-screen-focuses-on-one-track-at-a-time-and-63d1529caf67.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_02-zszvCi1FIBiKUvzFXqSEDhNQuvwiJ6Zh.jpg

**Screen 2. The MPC file browser, focused on installed Expansion packs.**

![Screen 2. The MPC file browser, focused on installed Expansion packs.](MPC3-UI-REFERENCE-ASSETS/images/screen-2-the-mpc-file-browser-focused-on-installed-expansion-10c34eeb90c1.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_03-PlrR_G.oM0DG7uu0lzpwVvzkpLLaecNh.jpg

**Screen 3. The contents of MIDI Tracks are shown in Grid view.**

![Screen 3. The contents of MIDI Tracks are shown in Grid view.](MPC3-UI-REFERENCE-ASSETS/images/screen-3-the-contents-of-midi-tracks-are-shown-in-grid-view-dce294c98ae2.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_04-zqOhq73ZaySAycsxC5bYs2I.mJC_gnZU.jpg

### Source page: [https://www.soundonsound.com/techniques/akai-mpc-basics?page=2](https://www.soundonsound.com/techniques/akai-mpc-basics?page=2)

**Screen 4. The Pad Perform setup page. This is the standard Pad mode for Keygroup and Plugin programs.**

![Screen 4. The Pad Perform setup page. This is the standard Pad mode for Keygroup and Plugin programs.](MPC3-UI-REFERENCE-ASSETS/images/screen-4-the-pad-perform-setup-page-this-is-the-standard-pad-156d490e4576.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_05-iedvyGj_OheR0WYbhZp0mJ0g6yPgjlGs.jpg

**Screen 5. If you want to track long audio parts, structure your song inside one long Sequence.**

![Screen 5. If you want to track long audio parts, structure your song inside one long Sequence.](MPC3-UI-REFERENCE-ASSETS/images/screen-5-if-you-want-to-track-long-audio-parts-structure-you-48bbb931034b.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_06-abyUL6tYYlwigclGsubuL6iBSQ_roRsv.jpg

**Screen 6. Chopping samples and loops is one of the most popular MPC workflows.**

![Screen 6. Chopping samples and loops is one of the most popular MPC workflows.](MPC3-UI-REFERENCE-ASSETS/images/screen-6-chopping-samples-and-loops-is-one-of-the-most-popul-ee382fd77cd9.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_07-oVuE5ctVUHkPMmuRP4zs9OaKGT0Jbx.6.jpg

**Screen 7. You can open your MPC project directly in your DAW via the VST plug‑in. Here I've split out the Programs and Audio tracks to different buses in Live so I can record an arrangement in real time.**

![Screen 7. You can open your MPC project directly in your DAW via the VST plug‑in. Here I've split out the Programs and Audio tracks to different buses in Live so I can record an arrangement in real time.](MPC3-UI-REFERENCE-ASSETS/images/screen-7-you-can-open-your-mpc-project-directly-in-your-daw-50154d5ae023.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/M/MPCBasics_08-zKBuNKAsEjohBMJEBKITg3ZuJtBRDBCm.jpg

### Source page: [https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware](https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware)

**Akai MPC left, Native Instruments Maschine middle and Ableton Push right.**

![Akai MPC left, Native Instruments Maschine middle and Ableton Push right.](MPC3-UI-REFERENCE-ASSETS/images/akai-mpc-left-native-instruments-maschine-middle-and-ableton-f30a9b63e330.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_01-FCtRDq0nyCXoGs1glq4H1OE4PLfH7jsU.jpg

**The Slice mode in MPC’s Sampler view lets you grab slices to pads live as you record.**

![The Slice mode in MPC’s Sampler view lets you grab slices to pads live as you record.](MPC3-UI-REFERENCE-ASSETS/images/the-slice-mode-in-mpc-s-sampler-view-lets-you-grab-slices-to-71c8e0fa29d4.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_02-MNUFMgafTGGhB469Bw1VoYNOU.7.Mqkl.jpg

**If you import a loop in Maschine, make sure you switch the pad from the Audio to the Sampler plug‑in to enable Slicing.**

![If you import a loop in Maschine, make sure you switch the pad from the Audio to the Sampler plug‑in to enable Slicing.](MPC3-UI-REFERENCE-ASSETS/images/if-you-import-a-loop-in-maschine-make-sure-you-switch-the-pa-aa53a43941a3.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_03-pZCydq2FxGNFRIB5Zkppe_IVbQNL9HPu.jpg

**Unless cutting to individual hits, you’ll need to tempo‑match samples. On Maschine, process before chopping; MPC and Push can warp.**

![Unless cutting to individual hits, you’ll need to tempo‑match samples. On Maschine, process before chopping; MPC and Push can warp.](MPC3-UI-REFERENCE-ASSETS/images/unless-cutting-to-individual-hits-you-ll-need-to-tempo-match-4448eb123f1c.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_04-joUkKG5jbUyy9PcZz_zWwUKrdXem_5e5.jpg

**Transient Detection is useful for cutting a sample into individual hits.**

![Transient Detection is useful for cutting a sample into individual hits.](MPC3-UI-REFERENCE-ASSETS/images/transient-detection-is-useful-for-cutting-a-sample-into-indi-45f1bba9f333.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_06-NnPZZWZ6I0q37o9S6VSgZoFkr_5f7KV2.jpg

**Manually chopping a break into phrases on Push. Real‑time warp tempo matches to the Session.**

![Manually chopping a break into phrases on Push. Real‑time warp tempo matches to the Session.](MPC3-UI-REFERENCE-ASSETS/images/manually-chopping-a-break-into-phrases-on-push-real-time-war-9786f08912f9.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_05-q0F8WEUQ6xLE6E16HGohPsp151rcFGEb.jpg

**From the Chop screen on MPC, hold Shift to access the Convert button to create a program from your slices.**

![From the Chop screen on MPC, hold Shift to access the Convert button to create a program from your slices.](MPC3-UI-REFERENCE-ASSETS/images/from-the-chop-screen-on-mpc-hold-shift-to-access-the-convert-102f24c723f8.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_07-5M6gGNDYZb9YB3wUCyrWCKs2FdBXHonx.jpg

**When it comes to crate digging, DJ Shadow may be the all‑time master. Seen here in his natural habitat, the basement of Sacramento’s Rare Records.**

![When it comes to crate digging, DJ Shadow may be the all‑time master. Seen here in his natural habitat, the basement of Sacramento’s Rare Records.](MPC3-UI-REFERENCE-ASSETS/images/when-it-comes-to-crate-digging-dj-shadow-may-be-the-all-time-e357aefa95c7.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/news_preview/s3/imagelibrary/S/SampleSlicing_09-ejLKBPQzjtGQE2ubfuK13goBISZga1Zl.jpg

**Even just three slices can be enough to create a completely new beat from a sample.**

![Even just three slices can be enough to create a completely new beat from a sample.](MPC3-UI-REFERENCE-ASSETS/images/even-just-three-slices-can-be-enough-to-create-a-completely-e64007febea9.jpg)

Original image: https://dt7v1i9vyp3mf.cloudfront.net/styles/header/s3/imagelibrary/S/SampleSlicing_08-Lsrww1fudjo0ZkNOieiukVWDKhYv_goA.jpg

### Images discovered but not downloaded

- Article image: Akai Pro MPC Series | Tuning Basics : Akai Professional: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69015392873/logo/QNLPi0l-L5pEK6YaEKpzsqVT5o9VQHMIHw.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&amp;X-Amz-Credential=AKIAS6FNSMY2XLZULJPI%2F20261029%2Fus-east-1%2Fs3%2Faws4_request&amp;X-Amz-Date=20261029T045114Z&amp;X-Amz-Expires=604800&amp;X-Amz-SignedHeaders=host&amp;X-Amz-Signature=9d095d7bcfc51afb0bf33dcce37135183550fcf90ce141c8e514ac8fbdaa543f — failed after retries: 400 Client Error: Bad Request for url: https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69015392873/logo/QNLPi0l-L5pEK6YaEKpzsqVT5o9VQHMIHw.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&amp;X-Amz-Credential=AKIAS6FNSMY2XLZULJPI%2F20261029%2Fus-east-1%2Fs3%2Faws4_request&amp;X-Amz-Date=20261029T045114Z&amp;X-Amz-Expires=604800&amp;X-Amz-SignedHeaders=host&amp;X-Amz-Signature=9d095d7bcfc51afb0bf33dcce37135183550fcf90ce141c8e514ac8fbdaa543f

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/03/Screen-Shot-03-05-20-at-04.11-PM.png — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/03/Screen-Shot-03-05-20-at-04.11-PM.png

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0030_med--1024x795.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0030_med--1024x795.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0030_screenclose-.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0030_screenclose-.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0035_CU.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0035_CU.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0037-1024x796.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0037-1024x796.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0040_CU.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0040_CU.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0044-1024x796.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0044-1024x796.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0046_CU.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0046_CU.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0048_CU.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0048_CU.jpg

- Reference image from Akai Pro MPC Series | Understanding and Loading Programs : Akai Professional: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0051_CU.jpg — failed after retries: 404 Client Error: Not Found for url: https://www.akaipro.com/wp/wp-content/uploads/sites/2/2020/05/MPC-101_0051_CU.jpg

# 9. Full screen/context coverage index

This index covers 82 screen and sub-screen contexts from the MPC Standalone user-guide taxonomy and our repository contract. “Inline” means a remote screenshot image is embedded above; “official PDF figure link” means the guide's screenshot remains inside the linked PDF because no stable direct image URL was exposed; “guide link” is still a visual research task. The evidence column never says a screen is implemented merely because its original reference exists.

**Page-linked screenshot sources:** the [MPC hardware user guide v2.11.7](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf) contains screenshots for many older standalone MPC One/Live/X contexts; these are explicitly **legacy references**, not pixel-perfect MPC 3.9. The table links each of the 82 contexts to a relevant manual/source as a navigational starting point. **Important QA limitation:** not every legacy page anchor has yet been visually inspected one-by-one; treat those links as candidate page navigation until verified. The official [MPC Standalone OS v3.9 guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf) remains the authority for current terminology and composition. Where a screen is new to MPC 3.x, use the current guide and official Akai support images above.

| ID | Screen / sub-screen | Available visual reference | Target/version and acceptance note | Manual/source reference (page anchors; legacy anchors need spot-check)  | Stored screenshot asset(s) |
|---|---|---|---|--- |---|
| VIS-001 | Main overview: Sequence + Track/Arrangement | Inline A/B images | Official v3.9 guide, Main Mode. Sequence and Track context must share the workspace.   [mpc-v39 p.15](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-015.jpg); [mpc-v39 p.109](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-109.jpg) |
| VIS-002 | Toolbar / project / counter / transport | Inline Main images A/B | Official v3.9 guide, Toolbar; keep transport state persistent through navigation.   [mpc-v39 p.57](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-057.jpg) |
| VIS-003 | Pull-Down Menu / status controls | Official guide reference | Official v3.9 guide, Pull-Down Menu. Exact current panel image still not separately embedded.   [mpc-v39 p.56](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-056.jpg); [mpc-v39 p.59](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-059.jpg) |
| VIS-004 | Five shortcut rail | Inline Main/XL images A | Exactly five configurable destinations; no retired persistent seven-page rail.   [mpc-v39 p.15](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-015.jpg) |
| VIS-005 | XL Channel Strip, Main context | Inline A | Main and XL strip must remain one coherent shell.   [mpc-v39 p.133](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-133.jpg) |
| VIS-006 | XL Channel Strip — I/O / output chooser | Inline A | Do not imply the full mixer/routing implementation is complete solely from this visual.   [mpc-v39 p.133](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-133.jpg) |
| VIS-007 | Menu / mode launcher | Inline B; additional mode-tile overview | Use current v3.9 guide for exact inventory, not legacy tile appearance.   [mpc-v39 p.56](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-056.jpg) |
| VIS-008 | Browser — Places | Inline A | Current local storage and provider states must be truthful.   [mpc-v39 p.136](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-136.jpg) |
| VIS-009 | Browser — Content | Inline A | Current categories and empty/error states need separate acceptance.   [mpc-v39 p.136](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-136.jpg) |
| VIS-010 | Browser — Expansions | Inline A | Product decision excludes subscription upsells/cloud service integration.   [mpc-v39 p.136](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-136.jpg) |
| VIS-011 | Browser — Sample Assign / Sample Pool | Inline A | Assignment is distinct from browsing and must update real Pad/Layer state.   [mpc-v39 p.139](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-139.jpg) |
| VIS-012 | Browser — Options / filters / favourites / audition | Inline Options; current audition/filter/favourites details in §8.26 | Dialog, popover, six filter icons and five favourite shortcuts are referenced; some state variants remain absent.   [mpc-v39 p.136](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-136.jpg) |
| VIS-013 | Main Sequence Select / sequence fields | Inline Main A/B | Official v3.9 Main; Sequence changes must preserve selected Track and transport.   [mpc-v39 p.112](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-112.jpg) |
| VIS-014 | Main Track fields / Program Select | Inline Main + official Program article | Selected Track is authoritative; Program must belong to that Track.   [mpc-v39 p.117](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-117.jpg) |
| VIS-015 | Main Track sample / waveform / layer context | Inline Main A | No permanent Android 4×4 performance pad grid in canonical Main.   [mpc-v39 p.109](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-109.jpg); [mpc-v39 p.197](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-197.jpg); [mpc-v39 p.203](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-203.jpg) |
| VIS-016 | Main Arrangement preview / switch | Inline Main A and MPC 3-family B | Sibling view of same Sequence/Track, not a separate state machine.   [mpc-v39 p.129](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-129.jpg); [mpc-v39 p.154](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-154.jpg) |
| VIS-017 | Track View overview / I/O / Monitor | Inline official A (older appearance) | Shift+Main; Monitor/REC/selection/Back need functional verification.   [mpc-v39 p.266](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-266.jpg) |
| VIS-018 | Track View — group / track states | Official manual link | Exact current screenshot not separately hosted; use Track View chapter.   [mpc-v39 p.261](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-261.jpg); [mpc-v39 p.266](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-266.jpg) |
| VIS-019 | Arrangement — timeline / loop / playhead | Inline Main A and sibling MPC 3 B | Distinguish screen geometry for Gen 1 One from wider models.   [mpc-v39 p.129](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-129.jpg); [mpc-v39 p.154](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-154.jpg) |
| VIS-020 | Arrangement — editing placements / event menu | Official guide link + Main preview | Current project implements a subset; don't equate pattern-event editing with complete Linear Arranger coverage.   [mpc-v39 p.129](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-129.jpg); [mpc-v39 p.154](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-154.jpg) |
| VIS-021 | Grid View — Drum track / note editing | Legacy inline C; current v3.9 figures in §8.25 | The v3.9 guide p.167 includes Drum and other Grid layouts.   [mpc-v39 p.167](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-167.jpg) |
| VIS-022 | Grid View — Audio track | Official v3.9 guide link | Different visual/data contract from Drum grid.   [mpc-v39 p.167](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-167.jpg) |
| VIS-023 | Grid View — velocity / automation lanes | Legacy inline C / Chop visual as related context | Note editing and automation are separate lane states and require separate acceptance.   [mpc-v39 p.167](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-167.jpg) |
| VIS-024 | Step Sequencer — 16 steps / velocity | Inline B, actual One photo (legacy); current v3.9 screenshot figure in §8.25 | Use the direct page reference for the current 16-step workspace and velocity states.   [mpc-v39 p.179](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-179.jpg) |
| VIS-025 | Step Sequencer — step automation | Official v3.9 guide link | Must expose actual parameter selection and step values.   [mpc-v39 p.179](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-179.jpg) |
| VIS-026 | List Edit — Events table | Current v3.9 figure in §8.25; v3.7 is historical fallback | The current guide's event table is visible on p.184.   [mpc-v39 p.184](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-184.jpg) |
| VIS-027 | List Edit — Tempo events | Official manual link | Separate Tempo view and mutation semantics.   [mpc-v39 p.184](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-184.jpg) |
| VIS-028 | Track Edit — Drum / Global | Official guide + legacy Program Edit example | Verify current terminology and supported domain controls.   [mpc-v39 p.190](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-190.jpg) |
| VIS-029 | Track Edit — Drum / Samples / Layers | Inline official A + legacy inline C | Eight layer indicators, sample region and actual selected Layer state.   [mpc-v39 p.197](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-197.jpg); [mpc-v39 p.203](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-203.jpg) |
| VIS-030 | Track Edit — Drum / Envelopes | Official guide + related legacy Program Edit visual | ADSR changes must hit actual sampler state.   [mpc-v39 p.257](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-257.jpg) |
| VIS-031 | Track Edit — LFO / Modulations / Effects / Mod Matrix / Utilities | Current v3.9 LFO figure on p.212; other tabs remain separate contexts | The LFO image does not imply that all tabs or modulation backends are implemented.   [mpc-v39 p.212](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-212.jpg) |
| VIS-032 | Track Edit — Keygroup | Official v3.9 guide link | Keygroup has a different parameter layout from Drum Track.   [mpc-v39 p.215](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-215.jpg) |
| VIS-033 | Track Edit — Plugin | Official Program article / v3.9 guide | Only display real instrument parameters where supported.   [mpc-v39 p.254](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-254.jpg) |
| VIS-034 | Track Edit — MIDI | Official v3.9 guide link | Expose truthful port/channel data, not fake instrument controls.   [mpc-v39 p.255](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-255.jpg) |
| VIS-035 | Track Edit — CV | Official v3.9 guide link | Reference behavior; native implementation capability must be stated separately.   [mpc-v39 p.256](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-256.jpg) |
| VIS-036 | Sample Edit — Trim / region / loop | Inline B/C | Current v3.9 labels and boundaries override old visuals.   [mpc-v39 p.269](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-269.jpg) |
| VIS-037 | Sample Edit — Chop / slice / threshold | Inline C | Markers, slice assignment, threshold/manual and conversion are separate operations.   [mpc-v39 p.269](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-269.jpg) |
| VIS-038 | Sample Edit — Pad mode / assignments | Official manual link + actual One trim photo | Assignment must preserve true sample and pad identity.   [mpc-v39 p.269](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-269.jpg) |
| VIS-039 | Sampler — record / waiting for signal | Inline C | Record, Stop, Monitor, Threshold and destination are actual state, not decorative.   [mpc-v39 p.296](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-296.jpg); [mpc-v39 p.297](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-297.jpg); [mpc-v39 p.298](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-298.jpg) |
| VIS-040 | Sampler — Slice | Official v3.9 guide link | Distinct sampler workflow; verify its own controls.   [mpc-v39 p.296](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-296.jpg); [mpc-v39 p.297](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-297.jpg); [mpc-v39 p.298](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-298.jpg) |
| VIS-041 | Sampler — Pad Tap | Official v3.9 guide link | Separate record-trigger mode.   [mpc-v39 p.296](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-296.jpg); [mpc-v39 p.297](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-297.jpg); [mpc-v39 p.298](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-298.jpg) |
| VIS-042 | Sampler — Pad Hold | Official v3.9 guide link | Separate record-trigger mode.   [mpc-v39 p.296](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-296.jpg); [mpc-v39 p.297](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-297.jpg); [mpc-v39 p.298](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-298.jpg) |
| VIS-043 | Sampler — Auto Sampler | Current v3.9 control-detail figures p.304–305 in §8.25 | Fields are shown for source, note range, velocity layers, sample duration/tail, loop settings and completion; a full-screen capture remains unavailable.   [mpc-v39 p.304](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-304.jpg); [mpc-v39 p.305](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-305.jpg) |
| VIS-044 | Channel Mixer — Volume / Pan & Volume | Inline official MPC 3 A | Exact channel categories and current selected strip matter.   [mpc-v39 p.140](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-140.jpg); [mpc-v39 p.141](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-141.jpg); [mpc-v39 p.142](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-142.jpg); [mpc-v39 p.143](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-143.jpg); [mpc-v39 p.144](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-144.jpg) |
| VIS-045 | Channel Mixer — Sends | Inline official MPC 3 A | Routing/effects capability remains limited by the project's actual backend.   [mpc-v39 p.140](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-140.jpg); [mpc-v39 p.141](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-141.jpg); [mpc-v39 p.142](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-142.jpg); [mpc-v39 p.143](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-143.jpg); [mpc-v39 p.144](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-144.jpg) |
| VIS-046 | Channel Mixer — Effects | Inline official MPC 3 A | Effects insert/return semantics must be genuine or clearly unavailable.   [mpc-v39 p.140](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-140.jpg); [mpc-v39 p.141](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-141.jpg); [mpc-v39 p.142](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-142.jpg); [mpc-v39 p.143](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-143.jpg); [mpc-v39 p.144](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-144.jpg) |
| VIS-047 | Channel Mixer — I/O / Monitoring / Route | Inline official MPC 3 A | Official screenshot includes Monitor, MIDI In, Send To and Audio Out fields.   [mpc-v39 p.140](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-140.jpg); [mpc-v39 p.141](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-141.jpg); [mpc-v39 p.142](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-142.jpg); [mpc-v39 p.143](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-143.jpg); [mpc-v39 p.144](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-144.jpg) |
| VIS-048 | Pad Mixer — Volume / Pan & Volume | Inline legacy routing image; current v3.9 full-screen and tabs in §8.25 | Current guide p.145–146 includes the Pad Mixer and Volume/Pan views.   [mpc-v39 p.145](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-145.jpg); [mpc-v39 p.146](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-146.jpg); [mpc-v39 p.147](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-147.jpg); [mpc-v39 p.148](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-148.jpg) |
| VIS-049 | Pad Mixer — Sends / Effects / I/O | Inline C (older routing) | Do not imply complete per-pad FX/routing support from a routing-only screenshot.   [mpc-v39 p.145](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-145.jpg); [mpc-v39 p.146](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-146.jpg); [mpc-v39 p.147](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-147.jpg); [mpc-v39 p.148](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-148.jpg) |
| VIS-050 | 16 Levels — Velocity / Tune / Filter / Layer / etc. | Current v3.9 dialog figures in §8.25 | The current guide shows Velocity/Tune variants on p.80; additional parameter mappings are described separately.   [mpc-v39 p.80](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-080.jpg) |
| VIS-051 | Pad Perform — Notes / Chords / Chromatic Chords / Progressions / Custom | Inline official A + inline legacy C | A separate performance context, not part of Main. Current mapping must show how physical pads are interpreted.   [mpc-v39 p.81](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-081.jpg) |
| VIS-052 | Arpeggiator | Current v3.9 dialog figure in §8.25; supplemental official support image above | The current guide shows the dialog on p.84.   [mpc-v39 p.84](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-084.jpg) |
| VIS-053 | Erase context/dialog | Current v3.9 Erase dialog figure in §8.25 plus official Sequence Edit screenshots in §8.22 | Preserve target scope, time range, event mask and confirmation as separate states.   [mpc-v39 p.87](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-087.jpg) |
| VIS-054 | Next Sequence — sequence pad map / Next Bar / Sudden / Hold | Inline B actual One photo with overlay + official guide link | Screenshot quality is limited; current guide defines current state feedback.   [mpc-v39 p.318](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-318.jpg) |
| VIS-055 | Song Mode — playlist and sequence pads | Inline C | Separate from Next Sequence queueing.   [mpc-v39 p.328](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-328.jpg) |
| VIS-056 | Looper — waveform / Play / Overdub / Replace / Export | Current v3.9 Looper figure in §8.25; v3.7 reference historical fallback | The current guide screen is on p.340.   [mpc-v39 p.340](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-340.jpg) |
| VIS-057 | XYFX — XY plane / destination / latch | Inline B, MPC Live | Cross-device/older UI; current v3.9 controls remain authoritative.   [mpc-v39 p.151](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-151.jpg); [mpc-v39 p.152](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-152.jpg); [mpc-v39 p.153](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-153.jpg) |
| VIS-058 | Q-Link Edit — learn / edit assignment | Inline legacy Q-Link overview + official guide link | A macro overview does not replace the per-assignment editor reference.   [mpc-v39 p.306](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-306.jpg); [mpc-v39 p.314](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-314.jpg) |
| VIS-059 | XY Pad / Crossfader | Inline XYFX visual as related context; official guide link | Distinguish XY macro pad from standalone XYFX mode.   [mpc-v39 p.316](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-316.jpg) |
| VIS-060 | MIDI Control Mode — pads | Official v3.9 guide link | Controller-mapping context, not a DAW UI.   [mpc-v39 p.333](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-333.jpg); [mpc-v39 p.334](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-334.jpg); [mpc-v39 p.335](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-335.jpg); [mpc-v39 p.336](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-336.jpg); [mpc-v39 p.337](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-337.jpg) |
| VIS-061 | MIDI Control Mode — buttons | Official v3.9 guide link | Button mappings and feedback require separate validation.   [mpc-v39 p.333](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-333.jpg); [mpc-v39 p.334](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-334.jpg); [mpc-v39 p.335](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-335.jpg); [mpc-v39 p.336](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-336.jpg); [mpc-v39 p.337](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-337.jpg) |
| VIS-062 | MIDI Control Mode — Q-Link knobs / XY Pad | Official v3.9 guide link | Current assignments and feedback must reflect live state.   [mpc-v39 p.333](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-333.jpg); [mpc-v39 p.334](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-334.jpg); [mpc-v39 p.335](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-335.jpg); [mpc-v39 p.336](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-336.jpg); [mpc-v39 p.337](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-337.jpg) |
| VIS-063 | Pad Color / pad mapping | Inline B actual One photo + legacy pad-map table | Pad configuration view is distinct from Main's selected-pad waveform.   [mpc-v39 p.338](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-338.jpg) |
| VIS-064 | Sounds Mode — Favourites / Setlists | Official v3.9 guide link | Product scope excludes branded cloud/subscription upsell destinations.   [mpc-v39 p.149](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-149.jpg); [mpc-v39 p.150](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-150.jpg) |
| VIS-065 | Project — New Project / Open / Save / Save As | Inline legacy Save modal and Project Load/Save preferences | Current OS 3.9 file picker, cancellation and restore behavior must be independently accepted.   [mpc-v39 p.60](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-060.jpg) |
| VIS-066 | Save Window — project / project as / export formats | Inline C legacy image; current guide Save Window figure on p.69 in §8.25 | The current Save-window tile layout is visible; downstream file picker and overwrite states remain distinct.   [mpc-v39 p.69](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-069.jpg) |
| VIS-067 | Preferences — Audio Device | Official v3.9 guide link | Current settings and audio routing must be truthful to Android backend.   [mpc-v39 p.62](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-062.jpg) |
| VIS-068 | Preferences — Audio / Export | Official v3.9 guide link | Separate category; no direct image found.   [mpc-v39 p.63](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-063.jpg) |
| VIS-069 | Preferences — MIDI / Sync | Actual One photo plus current-generation official Akai image in §8.26 | The guide p.63 defines the category; use the newer official image for visual details but do not claim Gen 1 provenance for that image.   [mpc-v39 p.63](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-063.jpg); [mpc-v39 p.64](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-064.jpg) |
| VIS-070 | Preferences — Hardware | Official Akai Hardware Preferences image in §8.23; current guide p.64 text | Current support image shows MPC 3 Hardware preferences; per-control OS/version provenance remains limited.   [mpc-v39 p.64](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-064.jpg) |
| VIS-071 | Preferences — Sequencer | Official v3.9 guide link | No clean direct screenshot found.   [mpc-v39 p.65](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-065.jpg) |
| VIS-072 | Preferences — Project Defaults | Supplementary screenshot in §8.26; current guide p.66 text | Screenshot helps inventory fields, but exact hardware/firmware appearance is not verified.   [mpc-v39 p.66](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-066.jpg) |
| VIS-073 | Preferences — Project Load/Save | Official current-generation support images in §8.23 and §8.26; v3.9 guide p.67 text | Autosave settings, timeout selector and Temporary File Location are visible; some sub-states remain unpictured.   [mpc-v39 p.67](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-067.jpg) |
| VIS-074 | Preferences — General / network / account-related entries | Official v3.9 guide link | Exclude subscription upsells as specified by product policy.   [mpc-v39 p.68](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-068.jpg) |
| VIS-075 | Timing Correct dialog | Official v3.9 guide link | Quantization dialog and global enable state must be separately represented.   [mpc-v39 p.75](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-075.jpg) |
| VIS-076 | Metronome / Click dialog | Inline official MPC 3 A | Count-In, Enable, Rate, Sound, Level and Output.   [mpc-v39 p.77](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-077.jpg) |
| VIS-077 | Automation — Global / Track / Program | Official guide + inline legacy Q-Link/XY related visual | Read/Write/off state is not the same as the parameter editor.   [mpc-v39 p.78](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-078.jpg) |
| VIS-078 | Audio Mixdown / export | Current guide figure on p.100 in §8.25 | The Audio Mixdown screen is shown; destination chooser, export progress and error/cancel paths remain separate states.   [mpc-v39 p.100](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-100.jpg) |
| VIS-079 | Keyboard Control / Touch Strip | Official v3.9 guide link | Hardware-specific contexts; don't imply controller equivalence without evidence.   [mpc-v39 p.339](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-339.jpg) |
| VIS-080 | Project import dialog / Keep-or-Discard / destructive confirmations | Official v3.9 guide and support links | All modal exits must preserve the expected project/selection state.   [mpc-v39 p.60](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-060.jpg); [mpc-v39 p.69](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-069.jpg); [mpc-v39 p.87](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-087.jpg) |
| VIS-081 | Track Type selector / Program-specific Main sections | Inline Main and Program references | Current target track types must match official MPC 3.9 vocabulary.   [mpc-v39 p.117](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-117.jpg) |
| VIS-082 | Dialogs: selectors, numeric entry, routing, effects, picker errors | Related screenshots above; no single image represents this set | Treat as context-specific dialog inventory, not one generic modal.   [mpc-v39 p.56](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-056.jpg); [mpc-v39 p.69](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-069.jpg); [mpc-v39 p.87](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-087.jpg); [mpc-v39 p.100](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-100.jpg) |

---

# 10. Exact-version screenshot gaps and fallback references

The v3.9 PDF atlas in §8.25 corrects several former gaps. The catalogue now has **61 inline screenshot references** from public image hosts and an **82-context coverage index**; current-version PDF-page screenshot references are linked for most major MPC 3.x screens. “Linked in the guide” does not mean a PNG has been extracted or embedded in this Markdown.

## Coverage now backed by current OS v3.9 page figures

Current guide figures have been located and page-rendered for Main / Menu / toolbar; Timing Correct, Metronome, 16 Levels, Pad Perform, Arpeggiator and Erase dialogs; Browser and Sample Assign; Channel Mixer and all major Pad Mixer tabs; Arrangement; Audio, Drum and piano-roll Grid layouts; Step Sequencer; List Edit; Drum / Keygroup / Plugin / MIDI / CV Track Edit contexts and LFO/envelope views; Track Mute, Pad Mute, Track Groups and Track View; Sample Edit / Sampler recording; Q-Link, XY Pad, Next Sequence, Song Mode, MIDI Control, Pad Color and Looper.

Additional direct official Akai support images now cover the Browser audition popover, filter strip, favourite-folder controls, MIDI/Sync port-routing preferences, Project Load/Save Auto Save timeout settings and the blocking Auto Save progress modal. The separate screenshot list in §8.26 labels cropped control-detail images and old-skin images explicitly.

This resolves the earlier statements that the current-version Grid, List Edit, Step Sequencer, Pad Mixer, 16 Levels, Arpeggiator, Erase, LFO and Looper figures were unavailable. Some earlier sections still link older manuals for historical comparison; §8.25 is the preferred current OS 3.9 reference where it supplies a page figure.

## Genuine remaining visual gaps

1. **Exact Gen 1 hardware provenance:** the v3.9 guide is authoritative for OS/labels and contains many current screenshots, but it does not state that every pictured screen was photographed on an MPC One Gen 1. Do not claim pixel-level exactness for a page unless hardware provenance is independently confirmed.
2. **Preferences subpage images:** the guide shows Preferences landing on p.61 and textual descriptions for the settings taxonomy. Direct images now exist for MIDI/Sync and Project Load/Save, and Akai support has a Hardware Preferences image earlier in the catalog. The complete set of current full-screen Audio Device, Audio/Export, Hardware, Sequencer and General pages is still not covered by dedicated current screenshots. Project Defaults has a supplementary MPC-series screenshot in §8.26, but its exact OS/hardware provenance is not confirmed.
3. **Browser variants and modal states:** current references show the Options modal, audition popover, filter strip and five favourite shortcuts, but not every selected/unselected/filter/empty/error state or all Browser drive format/delete confirmation variants.
4. **Sampler variants:** the current guide shows Sampler recording states on pp.296–298 and Auto Sampler control-detail figures on pp.304–305. Dedicated full-screen current captures are still needed for Slice, Pad Tap and Pad Hold, plus a full-screen (rather than partial control images) Auto Sampler view.
5. **Project/file failure and confirmation paths:** direct references exist for New Project selectors, the current Save-window tile chooser (p.69), Audio Mixdown (p.100), Auto Save and several destructive-operation dialogs, but not for every current OS 3.9 open/save overwrite, cancel, permission, media-missing and error variant. The current guide describes Project Import at p.7 but does not show that dialog there.
6. **Exact Gen 1 screenshots for every remaining mode:** an MPC 3-family shot without stated hardware provenance is useful for information hierarchy, but must not be silently labeled as an MPC One Gen 1 capture. Legacy 2.x/3.6/3.7 images remain historical supplementary references, not exact OS 3.9 layout authority.

## Completion rule for future UI reconstruction

Use §8.25 as the primary, version-matched visual map. Compare the Android implementation against each relevant manual figure and the direct Akai-support images already embedded above; log screenshot, route, geometry and state discrepancies against the corresponding VIS ID. For a missing screenshot, retain a specific gap label and source-search task rather than substituting a vaguely similar image or declaring the context complete. Do not mark visual fidelity complete just because a navigation route exists.

## Source and rights note

Images are remotely embedded from their original public source URLs so the repository records readable references and attribution without bundling Akai screenshots or firmware-extracted artwork into the app. If a publisher changes/removes an image, use the source link and official v3.9 manual to refresh this catalog; do not silently substitute an unrelated MPC Desktop image.
