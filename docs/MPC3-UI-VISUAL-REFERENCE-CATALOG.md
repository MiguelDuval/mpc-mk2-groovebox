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


# 8. Additional screen captures: sequence, edit, mixer, and performance contexts

The following references were found in this pass. Their provenance is explicit: an actual MPC One photograph is labelled as such; an older standalone/MPC2 look or a different MPC model is supplementary, not a pixel-accurate v3.9 target.

## 8.1 Step Sequencer — actual MPC One hardware photo

**Evidence B, photographed MPC One.** The touchscreen shows the 16-step/velocity editing context. The photo is from a public user video/thread and its visible UI is a legacy generation; use it for the screen's basic layout and step/velocity relationship only.

![MPC One Step Sequencer photographed on the hardware](https://img.youtube.com/vi/uuNyLLg-cXs/maxresdefault.jpg)

Source: [MPC One step sequencer video](https://www.youtube.com/watch?v=uuNyLLg-cXs).

**MPC 3.9 reference:** [official v3.9 User Guide](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf), Step Sequencer and Step Automation sections. The current guide takes precedence for labels, hierarchy, colors, function-bar controls, automation and current feature set.

## 8.2 Sample Edit — Chop and slice editor

**Evidence C, older UI appearance.** This is a real MPC waveform/chop page showing slice markers and the Chop/Program/Zoom action strip. It is useful for understanding the relationship between waveform, slice markers, threshold/manual modes, and lower actions; do not copy the legacy red skin.

![MPC waveform Chop mode and slice markers](https://dt7v1i9vyp3mf.cloudfront.net/styles/news_large/s3/imagelibrary/M/MPCBasics_07-NswMBI0X4nQgTah8CIBrwSGbQ5xLHYQO.jpg)

Source: [Sound On Sound — Akai MPC Basics, Sample Edit section](https://www.soundonsound.com/techniques/akai-mpc-basics?page=2).

## 8.3 Sampler — record / input monitor

**Evidence C, older UI appearance.** The screen shows an actual record workspace with a sample-length counter, RECORD action, “Waiting for signal” state and PAD TAP / PAD HOLD / CANCEL choices.

![Standalone MPC Sampler — record waiting for signal](https://dt7v1i9vyp3mf.cloudfront.net/styles/news_large/s3/imagelibrary/S/SampleSlicing_02-on3yRqdzNYbySPlbD3x43Dr_yKlI1RKX.jpg)

Source: [Sound On Sound — Sample Slicing: Beatmaking With Hardware](https://www.soundonsound.com/techniques/sample-slicing-beatmaking-hardware).

## 8.4 Next Sequence — actual MPC One

**Evidence B, actual MPC One photographed during a tutorial.** The screen shows the Next Sequence tile grid on hardware. Japanese instructional text overlays the photo and reduces clarity, so treat it as a workflow reference, not a clean design plate.

![MPC One Next Sequence grid photographed on hardware](https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221025/20221025034745.jpg)

Source: [Marronfieldsproduction — MPC One Next Seq & Song walkthrough](https://marronfieldsproduction.hatenablog.com/entry/2022/10/25/063000).

**Current v3.9 reference:** use the official guide's Next Sequence section to verify current controls, pad assignments, selected/queued sequence state, Next Bar, Sudden and Hold.

## 8.5 Song Mode — sequence playlist

**Evidence C, older MPC UI.** This direct image shows the Song playlist at left and sequence tiles at right. Song Mode is a separate workflow from Next Sequence: it arranges sequences into a fixed song structure rather than queueing a live next sequence.

![MPC Song Mode — sequence playlist and sequence tiles](https://www.mpc-tutor.com/assets/song-mode-playlist-1024x640.png)

Source: [MPC-Tutor — MPC X, MPC One & MPC Live: essential tips](https://www.mpc-tutor.com/mpc-x-mpc-live-6-tips-shortcuts/).

## 8.6 Channel Mixer — current MPC 3 I/O / monitoring panel

**Evidence A, official Akai support screenshot for the MPC 3 family.** This screenshot shows the channel-strip grid and I/O-related track state: Monitor, MIDI input, Send To and audio output, with Volume, Pan & Volume, Sends, Effects, I/O and Settings tabs. It is the current-generation mixing reference, although the screenshot is not explicitly captured on Gen 1 MPC One.

![MPC 3 Channel Mixer I/O, monitoring, sends and outputs](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69192726532/original/_w6tJ-s7R-DyxGc1avFs6s8caU_uAq9B7Q.png?1780034221=)

Source: [Akai support — Multi MIDI controller workflow](https://support.akaipro.com/en/support/solutions/articles/69000864879-akai-pro-mpc-series-setting-up-multiple-midi-controllers-in-a-project).

## 8.7 XL Channel Strip — I/O selector / output routing

**Evidence A, official Akai support MPC 3 screenshot.** This image shows the Main Track/Arrangement surface together with the left XL Channel Strip region and an output selector dialog.

![MPC 3 Main and XL Channel Strip output selector](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69154227724/original/W4p-2vkRxIavIh5ZOK9e8ZvP0fZ0iVPVxA.png?1745834845=)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 8.8 Pad Mixer — pad routing matrix

**Evidence C, older routing screen hosted by official Akai support.** This shows the pad grid with per-pad outputs and bottom tabs for Level, Pan, Mute, Send, Insert and Route. It is not the complete current MPC 3 Pad Mixer; use it only for the relationship between pad selection, route and per-pad mix parameters.

![MPC pad mixer routing matrix — older visual, not MPC 3.9 geometry](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69136157082/original/dgGihnRKJXr_dvqLa512k-gmoref-QAe_Q.JPG?1732159501=)

Source: [Akai support — Output Routing Basics](https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics).

## 8.9 MIDI / Sync Preferences

**Evidence B, photographed MPC One.** This real hardware image shows Preferences → MIDI / Sync, with input/output ports and per-port enable flags. Its exact firmware appearance is older, but the settings taxonomy is relevant.

![MPC One Preferences — MIDI / Sync ports](https://eu5.dh-cdn.net/uploads/db8181/original/3X/7/3/739f6d1d05b21c98e4cf4c2e8e8a9b23304aaad3.jpeg)

Source: [Elektronauts — MPC One MIDI/SYNC Preferences discussion](https://www.elektronauts.com/t/mpc-thread-mpc-live-mpc-x-mpc-one-part-2/173139?page=142).

A clean public screenshot of the same category is also available at [MPC-Tutor — MIDI/Sync preferences](https://www.mpc-tutor.com/mpc-midi-tutorial-connecting-midi-controllers/). The current v3.9 guide governs exact options and geometry.

## 8.10 Metronome / Click dialog

**Evidence A, official Akai MPC 3 screenshot.** The dialog shows Count-In, Enable, Rate, Sound, Level and Output. This is a dedicated modal dialog within the persistent shell and deserves an acceptance scenario independent of Main layout.

![MPC 3 Click/Metro dialog](https://s3.amazonaws.com/cdn.freshdesk.com/data/helpdesk/attachments/production/69183338703/original/C9jY-JC1NZ48_45iDBSAU8uYXQh9eRCcMA.png?1770305858=)

Source: [Akai support — Editing the Count-In and Metronome](https://support.akaipro.com/en/support/solutions/articles/69000857890-akai-pro-mpc-series-editing-the-count-in-and-metronome).

## 8.11 XYFX

**Evidence B, MPC Live photographed.** This shows the standalone XYFX touch grid with a movable XY position and effect-parameter context. It is another MPC model and an earlier OS appearance; use it for the interaction model (XY plane + destination + latch), not Gen 1 MPC One pixel geometry.

![MPC standalone XYFX touch grid](https://heikkisiltala.net/wiki/_media/images/akai_mpc_live/effectsxypad02.jpg?tok=792f2b&w=800)

Source: [Heikki Siltala — MPC Live effects and XY Pad](https://heikkisiltala.net/wiki/en/music/akai_mpc_live_effects).

## 8.12 Program Edit — Samples / layer parameters

**Evidence C, earlier MPC visual appearance.** These screenshots show the selected Pad and sample-layer editing area, including layer rows, semitone/fine tuning and the Samples tab. They are supplementary structure examples, not a complete current Track Edit contract.

![MPC Program Edit Samples page — layer list](https://www.mpc-tutor.com/assets/prog5.png)

![MPC Program Edit — per-pad sample-layer parameters](https://www.mpc-tutor.com/assets/offset2.png)

Source: [MPC-Tutor — Creating the Clap from Hell](https://www.mpc-tutor.com/mpc-x-mpc-live-tutorial-clap-from-hell/) and [Standalone MPC Cycle Kit tutorial](https://www.mpc-tutor.com/mpc-cycle-kit-chopped-break/).

## 8.13 Pad Color / pad assignment

**Evidence B, real MPC One screen photo.** This reference shows a 4×4 pad assignment grid, pad names and a palette for selecting colors. It belongs to Pad Color / pad configuration workflows, not to the Main workspace.

![MPC One pad color / sample assignment grid](https://cdn.mos.cms.futurecdn.net/oC9sLkM9L8r7nj9abQQTsj.jpg)

Source: [MusicRadar — 10 ways to get more out of your MPC hardware](https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC).

## 8.14 Q-Link / macro controls

**Evidence C, legacy UI.** Useful to understand the matrix of assignable controls, displayed parameter names and a route to Q-Link Edit. Do not use its color treatment or exact panel arrangement as a current MPC 3.9 target.

![MPC Q-Link parameter overview](https://www.mpc-tutor.com/assets/qlink-2.png)

Source: [MPC-Tutor — MPC X, MPC One & MPC Live essential tips](https://www.mpc-tutor.com/mpc-x-mpc-live-6-tips-shortcuts/).

## 8.15 Save dialog / project save options

**Evidence C, legacy standalone UI.** The Save window shows Project, Project As, Drum Program, Program FX Rack, Sequence, Audio Mixdown and Ableton Live Set. It is a separate modal workflow, not a full representation of every file-picker or confirmation state.

![MPC Save window — project, project as and other save formats](https://www.mpc-tutor.com/assets/save.png)

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

![MPC One Browser shown on the physical hardware](https://cdn.mos.cms.futurecdn.net/pFYHKeyGe33zd8ypBvFKoi.jpg)

Source: [MusicRadar — 10 ways to get more out of your MPC hardware](https://www.musicradar.com/how-to/10-ways-to-get-more-out-of-your-MPC).

## 8.18 Sample Edit — Trim on physical MPC One

**Evidence B, actual MPC One photographed during a tutorial.** This reference shows the waveform and Trim context on the real hardware. It is an older MPC OS appearance; current labels and available operations must be checked against v3.9.

![MPC One Sample Edit Trim screen photographed on the hardware](https://cdn-ak.f.st-hatena.com/images/fotolife/M/Marronfieldsproduction/20221028/20221028035306.jpg)

Source: [Marronfieldsproduction — MPC One Sample Edit / Trim walkthrough](https://marronfieldsproduction.hatenablog.com/entry/2022/10/28/063000).

## 8.19 Sample Assign — sample pool and pad grid on MPC One

**Evidence B, actual MPC One screen photo.** This reference captures a sample pool list to the left and pad assignment cells on the right. It distinguishes Sample Assign from ordinary Browser file selection.

![MPC One Sample Assign with sample pool and 4x4 pad assignment](https://assets.st-note.com/img/1714572254880-afLUx6An8g.jpg)

Source: [Note — MPC One sample/pad assignment workflow](https://note.com/suitebossa/n/nde0f912fe4ce).

## 8.20 Legacy sample waveform / automation detail

**Evidence C, legacy UI.** This image is a real standalone MPC waveform editor displaying an automation lane and bottom region-edit actions. It is relevant to waveform/region editing, but the old red-and-grey skin is not the target MPC 3.9 Main or Sample Edit design.

![Legacy MPC waveform editor with automation lane and bottom actions](https://ragtime.online/media/catalog/product/6/b/6bc500-crop-af798e-mpcone.jpg.auto.jpg)

Source: [Ragtime — Akai MPC One standalone sampler and sequencer](https://ragtime.online/akai-professional-mpc-one-standalone-sampler-and-sequencer.html).

This index covers 82 screen and sub-screen contexts from the MPC Standalone user-guide taxonomy and our repository contract. “Inline” means a remote screenshot image is embedded above; “official PDF figure link” means the guide's screenshot remains inside the linked PDF because no stable direct image URL was exposed; “guide link” is still a visual research task. The evidence column never says a screen is implemented merely because its original reference exists.

**Page-linked screenshot sources:** the v3.9 guide below is the target reference for MPC 3.9 labels and composition. The additional **MPC 2.11.7 User Guide** links are an extensive visual reference source with screenshots of many older standalone MPC contexts; they are explicitly labelled as legacy, not pixel-perfect MPC 3.9. These two guides make it possible to inspect every context in the table even where a direct standalone image URL was not found.
| ID | Screen / sub-screen | Available visual reference | Target/version and acceptance note | Manual screenshot references (page anchors) |
|---|---|---|---|
| VIS-001 | Main overview: Sequence + Track/Arrangement | Inline A/B images | Official v3.9 guide, Main Mode. Sequence and Track context must share the workspace.  | [MPC 3.9 guide p.15](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=15); [MPC 2.11.7 screenshots/section p.127](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=127) |
| VIS-002 | Toolbar / project / counter / transport | Inline Main images A/B | Official v3.9 guide, Toolbar; keep transport state persistent through navigation.  | [MPC 2.11.7 screenshots/section p.98](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=98) |
| VIS-003 | Pull-Down Menu / status controls | Official guide reference | Official v3.9 guide, Pull-Down Menu. Exact current panel image still not separately embedded.  | [MPC 2.11.7 screenshots/section p.86](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=86) |
| VIS-004 | Five shortcut rail | Inline Main/XL images A | Exactly five configurable destinations; no retired persistent seven-page rail.  | [MPC 2.11.7 screenshots/section p.86](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=86) |
| VIS-005 | XL Channel Strip, Main context | Inline A | Main and XL strip must remain one coherent shell.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262) |
| VIS-006 | XL Channel Strip — I/O / output chooser | Inline A | Do not imply the full mixer/routing implementation is complete solely from this visual.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262) |
| VIS-007 | Menu / mode launcher | Inline B; additional mode-tile overview | Use current v3.9 guide for exact inventory, not legacy tile appearance.  | [MPC 3.9 guide p.20](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=20); [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262) |
| VIS-008 | Browser — Places | Inline A | Current local storage and provider states must be truthful.  | [MPC 2.11.7 screenshots/section p.265](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=265) |
| VIS-009 | Browser — Content | Inline A | Current categories and empty/error states need separate acceptance.  | [MPC 2.11.7 screenshots/section p.262](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=262) |
| VIS-010 | Browser — Expansions | Inline A | Product decision excludes subscription upsells/cloud service integration.  | [MPC 2.11.7 screenshots/section p.169](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=169) |
| VIS-011 | Browser — Sample Assign / Sample Pool | Inline A | Assignment is distinct from browsing and must update real Pad/Layer state.  | [MPC 3.9 guide p.21](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=21); [MPC 2.11.7 screenshots/section p.127](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=127) |
| VIS-012 | Browser — Options / filters / favourites / audition | Inline Options + source links | Each preference/state needs its own test, not just dialog visibility.  | [MPC 2.11.7 screenshots/section p.155](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=155) |
| VIS-013 | Main Sequence Select / sequence fields | Inline Main A/B | Official v3.9 Main; Sequence changes must preserve selected Track and transport.  | [MPC 3.9 guide p.29](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=29); [MPC 2.11.7 screenshots/section p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173) |
| VIS-014 | Main Track fields / Program Select | Inline Main + official Program article | Selected Track is authoritative; Program must belong to that Track.  | [MPC 2.11.7 screenshots/section p.210](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=210) |
| VIS-015 | Main Track sample / waveform / layer context | Inline Main A | No permanent Android 4×4 performance pad grid in canonical Main.  | [MPC 2.11.7 screenshots/section p.254](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=254) |
| VIS-016 | Main Arrangement preview / switch | Inline Main A and MPC 3-family B | Sibling view of same Sequence/Track, not a separate state machine.  | [MPC 2.11.7 screenshots/section p.180](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=180) |
| VIS-017 | Track View overview / I/O / Monitor | Inline official A (older appearance) | Shift+Main; Monitor/REC/selection/Back need functional verification.  | [MPC 3.9 guide p.45](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=45); [MPC 2.11.7 screenshots/section p.266](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=266) |
| VIS-018 | Track View — group / track states | Official manual link | Exact current screenshot not separately hosted; use Track View chapter.  | [MPC 2.11.7 screenshots/section p.279](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=279) |
| VIS-019 | Arrangement — timeline / loop / playhead | Inline Main A and sibling MPC 3 B | Distinguish screen geometry for Gen 1 One from wider models.  | [MPC 2.11.7 screenshots/section p.284](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=284) |
| VIS-020 | Arrangement — editing placements / event menu | Official guide link + Main preview | Current project implements a subset; don't equate pattern-event editing with complete Linear Arranger coverage.  | [MPC 2.11.7 screenshots/section p.104](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=104) |
| VIS-021 | Grid View — Drum track / note editing | Legacy inline C | Official v3.9 Grid View is authority for full-color Drum Grid.  | [MPC 2.11.7 screenshots/section p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323) |
| VIS-022 | Grid View — Audio track | Official v3.9 guide link | Different visual/data contract from Drum grid.  | [MPC 2.11.7 screenshots/section p.301](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=301) |
| VIS-023 | Grid View — velocity / automation lanes | Legacy inline C / Chop visual as related context | Note editing and automation are separate lane states and require separate acceptance.  | [MPC 2.11.7 screenshots/section p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258) |
| VIS-024 | Step Sequencer — 16 steps / velocity | Inline B, actual One photo (legacy screen) | Official v3.9 guide supersedes old skin for current controls.  | [MPC 3.9 guide p.29](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=29); [MPC 2.11.7 screenshots/section p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173) |
| VIS-025 | Step Sequencer — step automation | Official v3.9 guide link | Must expose actual parameter selection and step values.  | [MPC 3.9 guide p.29](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=29); [MPC 2.11.7 screenshots/section p.173](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=173) |
| VIS-026 | List Edit — Events table | Official v3.7 PDF figure link | PDF p.184; screenshot is linked within the guide, not duplicated inline.  | [MPC 2.11.7 screenshots/section p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258) |
| VIS-027 | List Edit — Tempo events | Official manual link | Separate Tempo view and mutation semantics.  | [MPC 2.11.7 screenshots/section p.258](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=258) |
| VIS-028 | Track Edit — Drum / Global | Official guide + legacy Program Edit example | Verify current terminology and supported domain controls.  | [MPC 2.11.7 screenshots/section p.213](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=213) |
| VIS-029 | Track Edit — Drum / Samples / Layers | Inline official A + legacy inline C | Eight layer indicators, sample region and actual selected Layer state.  | [MPC 2.11.7 screenshots/section p.215](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=215) |
| VIS-030 | Track Edit — Drum / Envelopes | Official guide + related legacy Program Edit visual | ADSR changes must hit actual sampler state.  | [MPC 2.11.7 screenshots/section p.223](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=223) |
| VIS-031 | Track Edit — LFO / Modulations / Effects / Mod Matrix / Utilities | Official v3.9 guide link | Separate tabs; do not use a single Samples screenshot to claim these are complete.  | [MPC 2.11.7 screenshots/section p.226](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=226) |
| VIS-032 | Track Edit — Keygroup | Official v3.9 guide link | Keygroup has a different parameter layout from Drum Track.  | [MPC 2.11.7 screenshots/section p.230](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=230) |
| VIS-033 | Track Edit — Plugin | Official Program article / v3.9 guide | Only display real instrument parameters where supported.  | [MPC 2.11.7 screenshots/section p.254](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=254) |
| VIS-034 | Track Edit — MIDI | Official v3.9 guide link | Expose truthful port/channel data, not fake instrument controls.  | [MPC 2.11.7 screenshots/section p.255](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=255) |
| VIS-035 | Track Edit — CV | Official v3.9 guide link | Reference behavior; native implementation capability must be stated separately.  | [MPC 2.11.7 screenshots/section p.256](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=256) |
| VIS-036 | Sample Edit — Trim / region / loop | Inline B/C | Current v3.9 labels and boundaries override old visuals.  | [MPC 3.9 guide p.36](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=36); [MPC 2.11.7 screenshots/section p.183](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=183) |
| VIS-037 | Sample Edit — Chop / slice / threshold | Inline C | Markers, slice assignment, threshold/manual and conversion are separate operations.  | [MPC 2.11.7 screenshots/section p.194](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=194) |
| VIS-038 | Sample Edit — Pad mode / assignments | Official manual link + actual One trim photo | Assignment must preserve true sample and pad identity.  | [MPC 2.11.7 screenshots/section p.187](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=187) |
| VIS-039 | Sampler — record / waiting for signal | Inline C | Record, Stop, Monitor, Threshold and destination are actual state, not decorative.  | [MPC 2.11.7 screenshots/section p.266](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=266) |
| VIS-040 | Sampler — Slice | Official v3.9 guide link | Distinct sampler workflow; verify its own controls.  | [MPC 2.11.7 screenshots/section p.271](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=271) |
| VIS-041 | Sampler — Pad Tap | Official v3.9 guide link | Separate record-trigger mode.  | [MPC 2.11.7 screenshots/section p.272](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=272) |
| VIS-042 | Sampler — Pad Hold | Official v3.9 guide link | Separate record-trigger mode.  | [MPC 2.11.7 screenshots/section p.273](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=273) |
| VIS-043 | Sampler — Auto Sampler | Official v3.9 guide link | Dedicated workflow; no direct screenshot found in this pass.  | [MPC 2.11.7 screenshots/section p.273](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=273) |
| VIS-044 | Channel Mixer — Volume / Pan & Volume | Inline official MPC 3 A | Exact channel categories and current selected strip matter.  | [MPC 2.11.7 screenshots/section p.284](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=284) |
| VIS-045 | Channel Mixer — Sends | Inline official MPC 3 A | Routing/effects capability remains limited by the project's actual backend.  | [MPC 2.11.7 screenshots/section p.282](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=282) |
| VIS-046 | Channel Mixer — Effects | Inline official MPC 3 A | Effects insert/return semantics must be genuine or clearly unavailable.  | [MPC 2.11.7 screenshots/section p.283](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=283) |
| VIS-047 | Channel Mixer — I/O / Monitoring / Route | Inline official MPC 3 A | Official screenshot includes Monitor, MIDI In, Send To and Audio Out fields.  | [MPC 2.11.7 screenshots/section p.287](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=287) |
| VIS-048 | Pad Mixer — Volume / Pan & Volume | Inline C (older) + official routing image | No exact full-screen v3.9 Pad Mixer capture located.  | [MPC 2.11.7 screenshots/section p.279](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=279) |
| VIS-049 | Pad Mixer — Sends / Effects / I/O | Inline C (older routing) | Do not imply complete per-pad FX/routing support from a routing-only screenshot.  | [MPC 2.11.7 screenshots/section p.282](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=282) |
| VIS-050 | 16 Levels — Velocity / Tune / Filter / Layer / etc. | Official v3.7 PDF figure link | PDF p.75 shows Velocity/Tune variants; current 3.9 guide is final authority.  | [MPC 2.11.7 screenshots/section p.104](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=104) |
| VIS-051 | Pad Perform — Notes / Chords / Chromatic Chords / Progressions / Custom | Inline official A + inline legacy C | A separate performance context, not part of Main. Current mapping must show how physical pads are interpreted.  | [MPC 3.9 guide p.38](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=38); [MPC 2.11.7 screenshots/section p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323) |
| VIS-052 | Arpeggiator | Official v3.9 guide link | No clean direct screenshot found in this pass.  | [MPC 2.11.7 screenshots/section p.373](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=373) |
| VIS-053 | Erase context/dialog | Official v3.9 guide link | Destructive confirmation and target scope are separate modal states.  | [MPC 2.11.7 screenshots/section p.105](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=105) |
| VIS-054 | Next Sequence — sequence pad map / Next Bar / Sudden / Hold | Inline B actual One photo with overlay + official guide link | Screenshot quality is limited; current guide defines current state feedback.  | [MPC 2.11.7 screenshots/section p.301](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=301) |
| VIS-055 | Song Mode — playlist and sequence pads | Inline C | Separate from Next Sequence queueing.  | [MPC 2.11.7 screenshots/section p.303](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=303) |
| VIS-056 | Looper — waveform / Play / Overdub / Replace / Export | Official v3.7 PDF figure link | PDF p.332; not embedded independently because public host exposes the image only inside the PDF.  | [MPC 2.11.7 screenshots/section p.274](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=274) |
| VIS-057 | XYFX — XY plane / destination / latch | Inline B, MPC Live | Cross-device/older UI; current v3.9 controls remain authoritative.  | [MPC 2.11.7 screenshots/section p.177](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=177) |
| VIS-058 | Q-Link Edit — learn / edit assignment | Inline legacy Q-Link overview + official guide link | A macro overview does not replace the per-assignment editor reference.  | [MPC 2.11.7 screenshots/section p.306](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=306) |
| VIS-059 | XY Pad / Crossfader | Inline XYFX visual as related context; official guide link | Distinguish XY macro pad from standalone XYFX mode.  | [MPC 2.11.7 screenshots/section p.322](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=322) |
| VIS-060 | MIDI Control Mode — pads | Official v3.9 guide link | Controller-mapping context, not a DAW UI.  | [MPC 2.11.7 screenshots/section p.318](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=318) |
| VIS-061 | MIDI Control Mode — buttons | Official v3.9 guide link | Button mappings and feedback require separate validation.  | [MPC 2.11.7 screenshots/section p.320](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=320) |
| VIS-062 | MIDI Control Mode — Q-Link knobs / XY Pad | Official v3.9 guide link | Current assignments and feedback must reflect live state.  | [MPC 2.11.7 screenshots/section p.321](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=321) |
| VIS-063 | Pad Color / pad mapping | Inline B actual One photo + legacy pad-map table | Pad configuration view is distinct from Main's selected-pad waveform.  | [MPC 2.11.7 screenshots/section p.316](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=316) |
| VIS-064 | Sounds Mode — Favourites / Setlists | Official v3.9 guide link | Product scope excludes branded cloud/subscription upsell destinations.  | [MPC 2.11.7 screenshots/section p.474](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=474) |
| VIS-065 | Project — New Project / Open / Save / Save As | Inline legacy Save modal and Project Load/Save preferences | Current OS 3.9 file picker, cancellation and restore behavior must be independently accepted.  | [MPC 3.9 guide p.60](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=60); [MPC 2.11.7 screenshots/section p.308](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=308) |
| VIS-066 | Save Window — project / project as / export formats | Inline C | Dialog is separate from project lifecycle logic.  | [MPC 2.11.7 screenshots/section p.87](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=87) |
| VIS-067 | Preferences — Audio Device | Official v3.9 guide link | Current settings and audio routing must be truthful to Android backend.  | [MPC 3.9 guide p.62](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=62); [MPC 2.11.7 screenshots/section p.90](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=90) |
| VIS-068 | Preferences — Audio / Export | Official v3.9 guide link | Separate category; no direct image found.  | [MPC 3.9 guide p.63](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=63); [MPC 2.11.7 screenshots/section p.93](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=93) |
| VIS-069 | Preferences — MIDI / Sync | Inline B actual One photo + clean third-party capture | Old appearance; current v3.9 option names/semantics prevail.  | [MPC 3.9 guide p.63](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=63); [MPC 2.11.7 screenshots/section p.90](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=90) |
| VIS-070 | Preferences — Hardware | Official v3.9 guide link | No clean direct screenshot found.  | [MPC 3.9 guide p.64](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=64); [MPC 2.11.7 screenshots/section p.91](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=91) |
| VIS-071 | Preferences — Sequencer | Official v3.9 guide link | No clean direct screenshot found.  | [MPC 3.9 guide p.65](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=65); [MPC 2.11.7 screenshots/section p.92](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=92) |
| VIS-072 | Preferences — Project Defaults | Official v3.9 guide link | New-project defaults must remain consistent.  | [MPC 2.11.7 screenshots/section p.93](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=93) |
| VIS-073 | Preferences — Project Load/Save | Inline B older screenshot | Autosave, template, load and save settings are distinct.  | [MPC 3.9 guide p.67](https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf#page=67); [MPC 2.11.7 screenshots/section p.93](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=93) |
| VIS-074 | Preferences — General / network / account-related entries | Official v3.9 guide link | Exclude subscription upsells as specified by product policy.  | [MPC 2.11.7 screenshots/section p.94](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=94) |
| VIS-075 | Timing Correct dialog | Official v3.9 guide link | Quantization dialog and global enable state must be separately represented.  | [MPC 2.11.7 screenshots/section p.100](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=100) |
| VIS-076 | Metronome / Click dialog | Inline official MPC 3 A | Count-In, Enable, Rate, Sound, Level and Output.  | [MPC 2.11.7 screenshots/section p.102](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=102) |
| VIS-077 | Automation — Global / Track / Program | Official guide + inline legacy Q-Link/XY related visual | Read/Write/off state is not the same as the parameter editor.  | [MPC 2.11.7 screenshots/section p.103](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=103) |
| VIS-078 | Audio Mixdown / export | Official v3.9 guide link | Export dialogs and result paths are separate from project Save.  | [MPC 2.11.7 screenshots/section p.122](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=122) |
| VIS-079 | Keyboard Control / Touch Strip | Official v3.9 guide link | Hardware-specific contexts; don't imply controller equivalence without evidence.  | [MPC 2.11.7 screenshots/section p.323](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=323) |
| VIS-080 | Project import dialog / Keep-or-Discard / destructive confirmations | Official v3.9 guide and support links | All modal exits must preserve the expected project/selection state.  | [MPC 2.11.7 screenshots/section p.308](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=308) |
| VIS-081 | Track Type selector / Program-specific Main sections | Inline Main and Program references | Current target track types must match official MPC 3.9 vocabulary.  | [MPC 2.11.7 screenshots/section p.140](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=140) |
| VIS-082 | Dialogs: selectors, numeric entry, routing, effects, picker errors | Related screenshots above; no single image represents this set | Treat as context-specific dialog inventory, not one generic modal.  | [MPC 2.11.7 screenshots/section p.105](https://device.report/m/99853061e82566b0521a203feda6a5cd2e1fcceecd6882726f8c9f11f27ddde0.pdf#page=105) |

---

# 10. Explicit remaining visual-research gaps

This catalog is substantially expanded, but the following screenshot **source gaps remain open**. They are visible here rather than being hidden behind a vague “all screens documented” claim:

1. **No stable direct image URL found** for 16 Levels and List Edit; the official MPC v3.7 PDF does contain figures for both, linked at PDF pages 75 and 184 above. These should be converted into separately hosted/embedded reference screenshots if a reliable, lawful source is found.
2. **No stable direct image URL found** for the Looper screen; the official MPC v3.7 PDF figure is linked at page 332.
3. **Current standalone Gen 1 MPC One / OS 3.9 exact images are still missing** for the full Step Sequencer, full Grid View, several Track Edit tabs, full Pad Mixer, full Preferences categories, Next Sequence without tutorial text overlays, and several dialogs.
4. **No clean direct capture found** for Arpeggiator, Erase dialog, Keyboard Control/Touch Strip, some MIDI Control subpages and several per-track Track Edit variants.
5. The browser page's remote screenshots may move or disappear; each embedded image is a source-linked remote reference, not a repository-owned asset. The source and current version must be revalidated before pixel-level acceptance.

The next action for any still-unillustrated context is explicit: locate an official/public image whose URL is usable, capture it from actual MPC One/One+ hardware with permission, or retain a named gap with a link to the exact page in the official manual. **Do not replace a missing screenshot with an invented mock-up and present it as an original MPC capture.** A separate exact-Gen1 hardware screenshot set is the only way to claim complete visual coverage of every current 3.9 screen and modal.
 
## Source and rights note

Images are remotely embedded from their original public source URLs so the repository records readable references and attribution without bundling Akai screenshots or firmware-extracted artwork into the app. If a publisher changes/removes an image, use the source link and official v3.9 manual to refresh this catalog; do not silently substitute an unrelated MPC Desktop image.
