# MPC-Style UI Architecture & Roadmap

## Purpose

The Android screen is the visual/compute surface for the MPC Studio MkII Groovebox. The UI must behave like a dedicated standalone instrument, not a diagnostic form and not a desktop DAW compressed onto a phone.

This document records the research-derived information architecture and the implementation order for the UI.

## Research synthesis

### Akai MPC standalone

Modern MPC standalone documentation separates the product into explicit modes such as Main, Browser, Grid View, Step Sequencer, Track Edit, Track View, Sample Edit, Sampler, mixers, Pad Mute/Track Mute, Q-Link, Next Sequence and Project. The Mode Menu can be customized, while a small left-side shortcut strip provides rapid access to frequently used modes. Main Mode combines project/transport context with track/arrangement information and fast pad/sample access.

Source:
https://cdn.inmusicbrands.com/Software/37/MPC%20Standalone%20OS%20-%20v3.7.pdf

Sampler and Sample Edit are deliberately task-focused: waveform/sample information stays visible while editing parameters such as start/end, trim/process and recording controls. The Sampler exposes monitor and threshold together with an input-level context.

### Akai Force

Force separates high-level workflows into Matrix/clip launching, Clip editing, Mixer and Browser, while retaining a linear arranger. Its Clip Matrix gives a large project overview and direct performance access. The hardware and display share the same state, which makes the screen a context monitor rather than a form full of unrelated controls.

Source:
https://cdn.inmusicbrands.com/akai/Force/330_dsghdfgt/Force%20-%20User%20Guide%20-%20v3.3.pdf

### Native Instruments Maschine+

Maschine+ uses a small set of strong mode entry points: Browser, Ideas/Song, Mixer, Channel/Plug-in control and Sampler. Ideas view separates experimentation from timeline arrangement. The Browser uses filtering and preview, while eight contextual knobs/parameter slots expose only the controls relevant to the selected context.

Source:
https://docs.native-instruments.com/ni-tech-manuals/maschine-plus-manual/en/maschine--overview
https://docs.native-instruments.com/ni-tech-manuals/maschine-plus-manual/en/using-the-browser

### Ableton Push

Push reinforces a similar principle: a single focused screen/view is selected, and the encoders, display buttons and pad grid change meaning according to that view. Session Screen/Pad modes separate overview from performance, while Clip/Device/Browser views expose detailed editing only when requested.

Source:
https://www.ableton.com/en/push/manual/

## Design decisions for this project

### 1. Persistent global shell

Every screen shares:

- Project name;
- current sequence/pattern;
- tempo and meter;
- transport;
- audio state;
- MIDI/Link state;
- one persistent status/message line.

The global shell never becomes a vertically scrolling document.

### 2. Primary modes

The first-level workflow is:

1. MAIN — perform, audition, select pad, quick pad controls.
2. BROWSE — find/import audio and project content.
3. SAMPLE — sample region and per-pad sample editing.
4. SEQ — pattern/grid/step workflow.
5. MIX — track/pad mixing.
6. REC — sampling and monitoring.
7. MENU — secondary modes and project/settings.

Secondary modes such as Grid, Step, Track Edit, Pad Mixer, Q-Link, Project and hardware diagnostics belong behind the Menu or contextual actions.

### 3. Main screen composition

MAIN is the default performance workspace:

- large 4x4 software pad surface;
- selected-pad inspector;
- sample identity/region summary;
- quick tuning, level and pan;
- layer selection;
- direct entry into SAMPLE, SEQ and MIX;
- physical MPC pads remain first-class and trigger the same semantic action.

A tap on a software pad is an audition/play action. A long press is reserved for entering the selected-pad editing context.

### 4. Sample screen composition

SAMPLE is task-focused rather than a list of controls:

- sample header;
- waveform area;
- region/status line;
- context tabs;
- EDIT: start/end, full region, crop, chop;
- ENV: attack/decay/sustain/release;
- FILTER: cutoff;
- LAYER: gain/tune/pan/velocity range.

Only the active context is expanded. This is the main mechanism that replaces the old "million buttons" diagnostic page.

### 5. Browser model

The long-term browser follows:

Places → Content type → Search/Tags → Results → Preview → Load.

Android's document picker remains a transport implementation detail; it must not dictate the product information architecture.

### 6. Sequencer model

The sequencer will use two related views:

- Grid: detailed timeline/event editing;
- Step: fast rhythmic programming.

Pattern/sequence context stays visible. The user should not leave the performance environment to adjust a single timing parameter.

### 7. Mixer model

MIX will eventually provide both:

- Track Mix;
- Pad Mix.

The currently selected channel/pad is visually obvious, and all visible controls are direct-manipulation controls rather than nested forms.

### 8. Hardware parity

Important hardware actions must have visible counterparts:

- pad selection/trigger;
- transport;
- mode/context;
- sample editing;
- mix;
- step/grid workflow;
- MIDI connection state.

The software screen is a companion surface to the MPC Studio MkII, not its replacement.

## Visual language

The implementation uses an original dark instrument UI:

- dark neutral background;
- slightly lighter functional surfaces;
- thin separators;
- restrained cyan/amber accents;
- red reserved for destructive/record states;
- compact typography;
- strong state indication;
- large touch targets for performance actions.

Do not copy Akai, Native Instruments or Ableton artwork, textures, logos or proprietary graphics.

## Implementation roadmap

### UI-0 — Shell
- landscape-only shell;
- persistent top transport/status bar;
- persistent mode rail;
- persistent bottom status line;
- no root ScrollView.

### UI-1 — Main
- 4x4 pad surface;
- selected pad inspector;
- direct audition;
- quick tone controls;
- layer selector.

### UI-2 — Sample
- waveform surface;
- region editing;
- envelope;
- filter;
- layer controls.

### UI-3 — Browser
- searchable content model;
- preview/audition;
- load target selection;
- recent/project content.

### UI-4 — Sequencer
- Grid;
- Step;
- pattern navigation;
- note/event editing.

### UI-5 — Mixer
- Track Mix;
- Pad Mix;
- meters;
- sends.

### UI-6 — Performance
- Note Repeat;
- 16 Levels;
- Pad Perform;
- Q-Link;
- Next Sequence;
- Matrix/performance overview inspired by Force/Maschine concepts.

### UI-7 — Hardware display
- LCD status pages;
- pad state feedback;
- hardware-context indicators.

### UI-8 — Polish and workflow hardening
- animation only where it improves state recognition;
- accessibility;
- touch ergonomics;
- fast mode switching;
- state persistence;
- physical test passes.

## Current implementation status

UI-0 and the first UI-1/UI-2 foundation are being introduced on the sampler branch so the physical sampler tests can be performed without the previous diagnostic scroll wall.

The current UI deliberately leaves the future waveform renderer and full sequencer editor as separate slices rather than faking their behavior.
