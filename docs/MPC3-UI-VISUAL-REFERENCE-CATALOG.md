# MPC 3.x Standalone — UI Visual Reference Catalog

**Research checkpoint:** 2026-10-29  
**Product target:** Akai MPC One (Gen 1) running MPC Standalone OS 3.9; this repository's Android landscape interface.  
**Purpose:** a visual companion to `MPC3-9-OS-KNOWLEDGE-BASE.md`, `MPC3-UI-SCREEN-BLUEPRINT.md`, and `MPC3-UI-MIGRATION-MASTER-SPEC.md`. The catalog is meant to prevent implementation from being driven only by memory, source-code convenience, or isolated test cases.

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

# 9. Full screen/context coverage index

This index covers 82 screen and sub-screen contexts from the MPC Standalone user-guide taxonomy and our repository contract. “Inline” means a remote screenshot image is embedded above; “official PDF figure link” means the guide's screenshot remains inside the linked PDF because no stable direct image URL was exposed; “guide link” is still a visual research task. The evidence column never says a screen is implemented merely because its original reference exists.

**Page-linked screenshot sources:** the [MPC hardware user guide v2.11.7](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf) contains screenshots for many older standalone MPC One/Live/X contexts; these are explicitly **legacy references**, not pixel-perfect MPC 3.9. The table links each of the 82 contexts to a relevant manual/source as a navigational starting point. **Important QA limitation:** not every legacy page anchor has yet been visually inspected one-by-one; treat those links as candidate page navigation until verified. The official [MPC Standalone OS v3.9 guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf) remains the authority for current terminology and composition. Where a screen is new to MPC 3.x, use the current guide and official Akai support images above.

| ID | Screen / sub-screen | Available visual reference | Target/version and acceptance note | Manual/source reference (page anchors; legacy anchors need spot-check)  | Stored screenshot asset(s) |
|---|---|---|---|--- |---|
| VIS-001 | Main overview: Sequence + Track/Arrangement | Inline A/B images | Official v3.9 guide, Main Mode. Sequence and Track context must share the workspace.  | [MPC 3.9 guide p.15](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=15); [MPC 2.11.7 screenshots/section p.127](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=127)  [mpc-v39 p.15](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-015.jpg) |
| VIS-002 | Toolbar / project / counter / transport | Inline Main images A/B | Official v3.9 guide, Toolbar; keep transport state persistent through navigation.  | [MPC 2.11.7 screenshots/section p.98](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=98)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-002) |
| VIS-003 | Pull-Down Menu / status controls | Official guide reference | Official v3.9 guide, Pull-Down Menu. Exact current panel image still not separately embedded.  | [MPC 2.11.7 screenshots/section p.86](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=86)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-003) |
| VIS-004 | Five shortcut rail | Inline Main/XL images A | Exactly five configurable destinations; no retired persistent seven-page rail.  | [MPC 2.11.7 screenshots/section p.86](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=86)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-004) |
| VIS-005 | XL Channel Strip, Main context | Inline A | Main and XL strip must remain one coherent shell.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262)  [mpc-v39 p.20](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-020.jpg) |
| VIS-006 | XL Channel Strip — I/O / output chooser | Inline A | Do not imply the full mixer/routing implementation is complete solely from this visual.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262)  [mpc-v39 p.20](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-020.jpg) |
| VIS-007 | Menu / mode launcher | Inline B; additional mode-tile overview | Use current v3.9 guide for exact inventory, not legacy tile appearance.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262)  [mpc-v39 p.20](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-020.jpg) |
| VIS-008 | Browser — Places | Inline A | Current local storage and provider states must be truthful.  | [MPC 2.11.7 screenshots/section p.265](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=265)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-008) |
| VIS-009 | Browser — Content | Inline A | Current categories and empty/error states need separate acceptance.  | [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-009) |
| VIS-010 | Browser — Expansions | Inline A | Product decision excludes subscription upsells/cloud service integration.  | [MPC 2.11.7 screenshots/section p.169](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=169)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-010) |
| VIS-011 | Browser — Sample Assign / Sample Pool | Inline A | Assignment is distinct from browsing and must update real Pad/Layer state.  | [MPC 3.9 guide p.21](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=21); [MPC 2.11.7 screenshots/section p.127](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=127)  [mpc-v39 p.21](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-021.jpg) |
| VIS-012 | Browser — Options / filters / favourites / audition | Inline Options; current audition/filter/favourites details in §8.26 | Dialog, popover, six filter icons and five favourite shortcuts are referenced; some state variants remain absent.  | [Current MPC 3.9 guide, Browser p.136](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=136); [MPC 2.11.7 reference p.155](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=155)  [mpc-v39 p.136](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-136.jpg) |
| VIS-013 | Main Sequence Select / sequence fields | Inline Main A/B | Official v3.9 Main; Sequence changes must preserve selected Track and transport.  | [MPC 3.9 guide p.29](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=29); [MPC 2.11.7 screenshots/section p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173)  [mpc-v39 p.29](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-029.jpg) |
| VIS-014 | Main Track fields / Program Select | Inline Main + official Program article | Selected Track is authoritative; Program must belong to that Track.  | [MPC 2.11.7 screenshots/section p.210](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=210)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-014) |
| VIS-015 | Main Track sample / waveform / layer context | Inline Main A | No permanent Android 4×4 performance pad grid in canonical Main.  | [MPC 2.11.7 screenshots/section p.254](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=254)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-015) |
| VIS-016 | Main Arrangement preview / switch | Inline Main A and MPC 3-family B | Sibling view of same Sequence/Track, not a separate state machine.  | [MPC 2.11.7 screenshots/section p.180](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=180)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-016) |
| VIS-017 | Track View overview / I/O / Monitor | Inline official A (older appearance) | Shift+Main; Monitor/REC/selection/Back need functional verification.  | [MPC 3.9 guide p.45](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=45); [MPC 2.11.7 screenshots/section p.266](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=266)  [mpc-v39 p.45](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-045.jpg) |
| VIS-018 | Track View — group / track states | Official manual link | Exact current screenshot not separately hosted; use Track View chapter.  | [MPC 2.11.7 screenshots/section p.279](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=279)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-018) |
| VIS-019 | Arrangement — timeline / loop / playhead | Inline Main A and sibling MPC 3 B | Distinguish screen geometry for Gen 1 One from wider models.  | [MPC 2.11.7 screenshots/section p.284](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=284)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-019) |
| VIS-020 | Arrangement — editing placements / event menu | Official guide link + Main preview | Current project implements a subset; don't equate pattern-event editing with complete Linear Arranger coverage.  | [MPC 2.11.7 screenshots/section p.104](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=104)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-020) |
| VIS-021 | Grid View — Drum track / note editing | Legacy inline C; current v3.9 figures in §8.25 | The v3.9 guide p.167 includes Drum and other Grid layouts.  | [Current MPC 3.9 guide p.167](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=167); [MPC 2.11.7 legacy p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323)  [mpc-v39 p.167](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-167.jpg) |
| VIS-022 | Grid View — Audio track | Official v3.9 guide link | Different visual/data contract from Drum grid.  | [MPC 2.11.7 screenshots/section p.301](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=301)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-022) |
| VIS-023 | Grid View — velocity / automation lanes | Legacy inline C / Chop visual as related context | Note editing and automation are separate lane states and require separate acceptance.  | [MPC 2.11.7 screenshots/section p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-023) |
| VIS-024 | Step Sequencer — 16 steps / velocity | Inline B, actual One photo (legacy); current v3.9 screenshot figure in §8.25 | Use the direct page reference for the current 16-step workspace and velocity states.  | [Current MPC 3.9 guide p.179](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=179); [MPC 2.11.7 legacy p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173)  [mpc-v39 p.179](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-179.jpg) |
| VIS-025 | Step Sequencer — step automation | Official v3.9 guide link | Must expose actual parameter selection and step values.  | [MPC 3.9 guide p.29](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=29); [MPC 2.11.7 screenshots/section p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173)  [mpc-v39 p.29](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-029.jpg) |
| VIS-026 | List Edit — Events table | Current v3.9 figure in §8.25; v3.7 is historical fallback | The current guide's event table is visible on p.184.  | [Current MPC 3.9 guide p.184](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=184); [MPC 2.11.7 legacy p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258)  [mpc-v39 p.184](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-184.jpg) |
| VIS-027 | List Edit — Tempo events | Official manual link | Separate Tempo view and mutation semantics.  | [MPC 2.11.7 screenshots/section p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-027) |
| VIS-028 | Track Edit — Drum / Global | Official guide + legacy Program Edit example | Verify current terminology and supported domain controls.  | [MPC 2.11.7 screenshots/section p.213](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=213)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-028) |
| VIS-029 | Track Edit — Drum / Samples / Layers | Inline official A + legacy inline C | Eight layer indicators, sample region and actual selected Layer state.  | [MPC 2.11.7 screenshots/section p.215](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=215)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-029) |
| VIS-030 | Track Edit — Drum / Envelopes | Official guide + related legacy Program Edit visual | ADSR changes must hit actual sampler state.  | [MPC 2.11.7 screenshots/section p.223](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=223)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-030) |
| VIS-031 | Track Edit — LFO / Modulations / Effects / Mod Matrix / Utilities | Current v3.9 LFO figure on p.212; other tabs remain separate contexts | The LFO image does not imply that all tabs or modulation backends are implemented.  | [Current MPC 3.9 guide p.212](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=212); [MPC 2.11.7 legacy p.226](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=226)  [mpc-v39 p.212](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-212.jpg) |
| VIS-032 | Track Edit — Keygroup | Official v3.9 guide link | Keygroup has a different parameter layout from Drum Track.  | [MPC 2.11.7 screenshots/section p.230](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=230)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-032) |
| VIS-033 | Track Edit — Plugin | Official Program article / v3.9 guide | Only display real instrument parameters where supported.  | [MPC 2.11.7 screenshots/section p.254](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=254)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-033) |
| VIS-034 | Track Edit — MIDI | Official v3.9 guide link | Expose truthful port/channel data, not fake instrument controls.  | [MPC 2.11.7 screenshots/section p.255](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=255)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-034) |
| VIS-035 | Track Edit — CV | Official v3.9 guide link | Reference behavior; native implementation capability must be stated separately.  | [MPC 2.11.7 screenshots/section p.256](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=256)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-035) |
| VIS-036 | Sample Edit — Trim / region / loop | Inline B/C | Current v3.9 labels and boundaries override old visuals.  | [MPC 3.9 guide p.36](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=36); [MPC 2.11.7 screenshots/section p.183](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=183)  [mpc-v39 p.36](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-036.jpg) |
| VIS-037 | Sample Edit — Chop / slice / threshold | Inline C | Markers, slice assignment, threshold/manual and conversion are separate operations.  | [MPC 2.11.7 screenshots/section p.194](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=194)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-037) |
| VIS-038 | Sample Edit — Pad mode / assignments | Official manual link + actual One trim photo | Assignment must preserve true sample and pad identity.  | [MPC 2.11.7 screenshots/section p.187](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=187)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-038) |
| VIS-039 | Sampler — record / waiting for signal | Inline C | Record, Stop, Monitor, Threshold and destination are actual state, not decorative.  | [MPC 2.11.7 screenshots/section p.266](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=266)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-039) |
| VIS-040 | Sampler — Slice | Official v3.9 guide link | Distinct sampler workflow; verify its own controls.  | [MPC 2.11.7 screenshots/section p.271](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=271)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-040) |
| VIS-041 | Sampler — Pad Tap | Official v3.9 guide link | Separate record-trigger mode.  | [MPC 2.11.7 screenshots/section p.272](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=272)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-041) |
| VIS-042 | Sampler — Pad Hold | Official v3.9 guide link | Separate record-trigger mode.  | [MPC 2.11.7 screenshots/section p.273](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=273)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-042) |
| VIS-043 | Sampler — Auto Sampler | Current v3.9 control-detail figures p.304–305 in §8.25 | Fields are shown for source, note range, velocity layers, sample duration/tail, loop settings and completion; a full-screen capture remains unavailable.  | [Current MPC 3.x guide p.304](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=304); [MPC 2.11.7 legacy p.273](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=273)  [mpc-v39 p.304](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-304.jpg) |
| VIS-044 | Channel Mixer — Volume / Pan & Volume | Inline official MPC 3 A | Exact channel categories and current selected strip matter.  | [MPC 2.11.7 screenshots/section p.284](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=284)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-044) |
| VIS-045 | Channel Mixer — Sends | Inline official MPC 3 A | Routing/effects capability remains limited by the project's actual backend.  | [MPC 2.11.7 screenshots/section p.282](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=282)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-045) |
| VIS-046 | Channel Mixer — Effects | Inline official MPC 3 A | Effects insert/return semantics must be genuine or clearly unavailable.  | [MPC 2.11.7 screenshots/section p.283](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=283)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-046) |
| VIS-047 | Channel Mixer — I/O / Monitoring / Route | Inline official MPC 3 A | Official screenshot includes Monitor, MIDI In, Send To and Audio Out fields.  | [MPC 2.11.7 screenshots/section p.287](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=287)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-047) |
| VIS-048 | Pad Mixer — Volume / Pan & Volume | Inline legacy routing image; current v3.9 full-screen and tabs in §8.25 | Current guide p.145–146 includes the Pad Mixer and Volume/Pan views.  | [Current MPC 3.9 guide p.145](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=145); [current Volume/Pan p.146](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=146)  [mpc-v39 p.145](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-145.jpg); [mpc-v39 p.146](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-146.jpg) |
| VIS-049 | Pad Mixer — Sends / Effects / I/O | Inline C (older routing) | Do not imply complete per-pad FX/routing support from a routing-only screenshot.  | [MPC 2.11.7 screenshots/section p.282](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=282)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-049) |
| VIS-050 | 16 Levels — Velocity / Tune / Filter / Layer / etc. | Current v3.9 dialog figures in §8.25 | The current guide shows Velocity/Tune variants on p.80; additional parameter mappings are described separately.  | [Current MPC 3.9 guide p.80](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=80); [MPC 2.11.7 legacy p.104](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=104)  [mpc-v39 p.80](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-080.jpg) |
| VIS-051 | Pad Perform — Notes / Chords / Chromatic Chords / Progressions / Custom | Inline official A + inline legacy C | A separate performance context, not part of Main. Current mapping must show how physical pads are interpreted.  | [MPC 3.9 guide p.38](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=38); [MPC 2.11.7 screenshots/section p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323)  [mpc-v39 p.38](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-038.jpg) |
| VIS-052 | Arpeggiator | Current v3.9 dialog figure in §8.25; supplemental official support image above | The current guide shows the dialog on p.84.  | [Current MPC 3.9 guide p.84](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=84); [MPC 2.11.7 legacy p.373](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=373)  [mpc-v39 p.84](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-084.jpg) |
| VIS-053 | Erase context/dialog | Current v3.9 Erase dialog figure in §8.25 plus official Sequence Edit screenshots in §8.22 | Preserve target scope, time range, event mask and confirmation as separate states.  | [Current MPC 3.9 guide p.87](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=87); [MPC 2.11.7 legacy p.105](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=105)  [mpc-v39 p.87](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-087.jpg) |
| VIS-054 | Next Sequence — sequence pad map / Next Bar / Sudden / Hold | Inline B actual One photo with overlay + official guide link | Screenshot quality is limited; current guide defines current state feedback.  | [MPC 2.11.7 screenshots/section p.301](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=301)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-054) |
| VIS-055 | Song Mode — playlist and sequence pads | Inline C | Separate from Next Sequence queueing.  | [MPC 2.11.7 screenshots/section p.303](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=303)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-055) |
| VIS-056 | Looper — waveform / Play / Overdub / Replace / Export | Current v3.9 Looper figure in §8.25; v3.7 reference historical fallback | The current guide screen is on p.340.  | [Current MPC 3.9 guide p.340](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=340); [MPC 2.11.7 legacy p.274](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=274)  [mpc-v39 p.340](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-340.jpg) |
| VIS-057 | XYFX — XY plane / destination / latch | Inline B, MPC Live | Cross-device/older UI; current v3.9 controls remain authoritative.  | [MPC 2.11.7 screenshots/section p.177](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=177)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-057) |
| VIS-058 | Q-Link Edit — learn / edit assignment | Inline legacy Q-Link overview + official guide link | A macro overview does not replace the per-assignment editor reference.  | [MPC 2.11.7 screenshots/section p.306](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=306)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-058) |
| VIS-059 | XY Pad / Crossfader | Inline XYFX visual as related context; official guide link | Distinguish XY macro pad from standalone XYFX mode.  | [MPC 2.11.7 screenshots/section p.322](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=322)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-059) |
| VIS-060 | MIDI Control Mode — pads | Official v3.9 guide link | Controller-mapping context, not a DAW UI.  | [MPC 2.11.7 screenshots/section p.318](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=318)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-060) |
| VIS-061 | MIDI Control Mode — buttons | Official v3.9 guide link | Button mappings and feedback require separate validation.  | [MPC 2.11.7 screenshots/section p.320](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=320)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-061) |
| VIS-062 | MIDI Control Mode — Q-Link knobs / XY Pad | Official v3.9 guide link | Current assignments and feedback must reflect live state.  | [MPC 2.11.7 screenshots/section p.321](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=321)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-062) |
| VIS-063 | Pad Color / pad mapping | Inline B actual One photo + legacy pad-map table | Pad configuration view is distinct from Main's selected-pad waveform.  | [MPC 2.11.7 screenshots/section p.316](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=316)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-063) |
| VIS-064 | Sounds Mode — Favourites / Setlists | Official v3.9 guide link | Product scope excludes branded cloud/subscription upsell destinations.  | [MPC 2.11.7 screenshots/section p.474](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=474)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-064) |
| VIS-065 | Project — New Project / Open / Save / Save As | Inline legacy Save modal and Project Load/Save preferences | Current OS 3.9 file picker, cancellation and restore behavior must be independently accepted.  | [MPC 3.9 guide p.60](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=60); [MPC 2.11.7 screenshots/section p.308](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=308)  [mpc-v39 p.60](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-060.jpg) |
| VIS-066 | Save Window — project / project as / export formats | Inline C legacy image; current guide Save Window figure on p.69 in §8.25 | The current Save-window tile layout is visible; downstream file picker and overwrite states remain distinct.  | [Current MPC 3.x guide p.69](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=69); [MPC 2.11.7 legacy p.87](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=87)  [mpc-v39 p.69](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-069.jpg) |
| VIS-067 | Preferences — Audio Device | Official v3.9 guide link | Current settings and audio routing must be truthful to Android backend.  | [MPC 3.9 guide p.62](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=62); [MPC 2.11.7 screenshots/section p.90](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=90)  [mpc-v39 p.62](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-062.jpg) |
| VIS-068 | Preferences — Audio / Export | Official v3.9 guide link | Separate category; no direct image found.  | [MPC 3.9 guide p.63](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=63); [MPC 2.11.7 screenshots/section p.93](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=93)  [mpc-v39 p.63](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-063.jpg) |
| VIS-069 | Preferences — MIDI / Sync | Actual One photo plus current-generation official Akai image in §8.26 | The guide p.63 defines the category; use the newer official image for visual details but do not claim Gen 1 provenance for that image.  | [Official MPC 3 support image/source in §8.26](https://support.akaipro.com/en/support/solutions/articles/69000867660-akai-pro-mpc-series-wireless-remote-midi-control-in-a-daw); [MPC 3.9 guide p.63](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=63); [MPC 2.11.7 legacy p.90](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=90)  [mpc-v39 p.63](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-063.jpg) |
| VIS-070 | Preferences — Hardware | Official Akai Hardware Preferences image in §8.23; current guide p.64 text | Current support image shows MPC 3 Hardware preferences; per-control OS/version provenance remains limited.  | [Official Hardware Preferences image/source in §8.23](https://support.akaipro.com/en/support/solutions/articles/69000870813-mpc-3-standalone-how-do-i-turn-off-the-tap-tempo-light-); [MPC 3.9 guide p.64](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=64); [MPC 2.11.7 legacy p.91](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=91)  [mpc-v39 p.64](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-064.jpg) |
| VIS-071 | Preferences — Sequencer | Official v3.9 guide link | No clean direct screenshot found.  | [MPC 3.9 guide p.65](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=65); [MPC 2.11.7 screenshots/section p.92](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=92)  [mpc-v39 p.65](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-065.jpg) |
| VIS-072 | Preferences — Project Defaults | Supplementary screenshot in §8.26; current guide p.66 text | Screenshot helps inventory fields, but exact hardware/firmware appearance is not verified.  | [Akai Project Defaults article](https://support.akaipro.com/en/support/solutions/articles/69000822965-akai-pro-mpc-series-adjusting-project-defaults); [MPC 3.9 guide p.66](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=66); [MPC 2.11.7 legacy p.93](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=93)  [mpc-v39 p.66](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-066.jpg) |
| VIS-073 | Preferences — Project Load/Save | Official current-generation support images in §8.23 and §8.26; v3.9 guide p.67 text | Autosave settings, timeout selector and Temporary File Location are visible; some sub-states remain unpictured.  | [MPC 3.9 guide p.67](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=67); [official Auto Save article](https://support.akaipro.com/en/support/solutions/articles/69000864617-akai-pro-mpc-series-saving-projects-with-auto-save)  [mpc-v39 p.67](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-067.jpg) |
| VIS-074 | Preferences — General / network / account-related entries | Official v3.9 guide link | Exclude subscription upsells as specified by product policy.  | [MPC 2.11.7 screenshots/section p.94](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=94)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-074) |
| VIS-075 | Timing Correct dialog | Official v3.9 guide link | Quantization dialog and global enable state must be separately represented.  | [MPC 2.11.7 screenshots/section p.100](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=100)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-075) |
| VIS-076 | Metronome / Click dialog | Inline official MPC 3 A | Count-In, Enable, Rate, Sound, Level and Output.  | [MPC 2.11.7 screenshots/section p.102](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=102)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-076) |
| VIS-077 | Automation — Global / Track / Program | Official guide + inline legacy Q-Link/XY related visual | Read/Write/off state is not the same as the parameter editor.  | [MPC 2.11.7 screenshots/section p.103](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=103)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-077) |
| VIS-078 | Audio Mixdown / export | Current guide figure on p.100 in §8.25 | The Audio Mixdown screen is shown; destination chooser, export progress and error/cancel paths remain separate states.  | [Current MPC 3.x guide p.100](https://device.report/m/b0630c574556ed080cb5fb3133352cca4e1dcc92282f78317c0c8f88f9f0090b_optim.pdf#page=100); [MPC 2.11.7 legacy p.122](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=122)  [mpc-v39 p.100](MPC3-UI-REFERENCE-ASSETS/pdf/mpc-v39/page-100.jpg) |
| VIS-079 | Keyboard Control / Touch Strip | Official v3.9 guide link | Hardware-specific contexts; don't imply controller equivalence without evidence.  | [MPC 2.11.7 screenshots/section p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-079) |
| VIS-080 | Project import dialog / Keep-or-Discard / destructive confirmations | Official v3.9 guide and support links | All modal exits must preserve the expected project/selection state.  | [MPC 2.11.7 screenshots/section p.308](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=308)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-080) |
| VIS-081 | Track Type selector / Program-specific Main sections | Inline Main and Program references | Current target track types must match official MPC 3.9 vocabulary.  | [MPC 2.11.7 screenshots/section p.140](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=140)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-081) |
| VIS-082 | Dialogs: selectors, numeric entry, routing, effects, picker errors | Related screenshots above; no single image represents this set | Treat as context-specific dialog inventory, not one generic modal.  | [MPC 2.11.7 screenshots/section p.105](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=105)  [local gallery](MPC3-UI-REFERENCE-ASSETS/INDEX.md#vis-082) |

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
