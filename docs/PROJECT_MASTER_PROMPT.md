# PROJECT MASTER PROMPT — MPC Studio MkII Groovebox

## 0. Purpose

You are an engineering agent working on **MPC Studio MkII Groovebox**, an Android standalone-style music production application centered on the **Akai MPC Studio MkII** controller.

This document is the project's canonical operating brief.

Before making code changes, read this document and then consult the more specific repository documents it points to. Treat repository evidence, build results, physical hardware tests, and current source code as higher-confidence evidence than assumptions.

The project must be developed incrementally, with every important layer independently testable.

---

## 1. Product vision

Build an Android groovebox that makes an MPC Studio MkII behave like the physical control surface of a compact standalone music workstation.

The phone/tablet is the **screen, compute platform and audio engine host**.

The MPC Studio MkII is the **primary physical performance surface**.

The target interaction model is the compact, hardware-oriented workflow documented for modern standalone MPC systems such as MPC One/One+/Live II-class products. This is a **functional and interaction reference**, not a request to clone proprietary MPC Desktop software.

The app should feel like a dedicated instrument:

- fast;
- immediate;
- touch-friendly;
- hardware-first;
- performance-oriented;
- musically predictable;
- stable during live use.

Do not turn the project into a generic desktop DAW squeezed onto Android.

---

## 2. Core principles

### 2.1 Hardware-first

The physical MPC Studio MkII must remain a first-class input/output device, not an optional keyboard.

Every hardware control should eventually map to a semantic application action.

Do not scatter raw MIDI numbers throughout the application.

### 2.2 Audio-first

Realtime audio correctness has priority over visual polish.

Never sacrifice audio stability for a visual effect.

Avoid allocations, blocking file I/O, locks, network operations and other unpredictable work in the realtime audio callback wherever reasonably possible.

### 2.3 Separation of concerns

Keep these layers independent:

1. Android platform layer.
2. MIDI/USB transport.
3. MPC Studio MkII hardware adapter.
4. MPC domain model.
5. Audio/sequencing engine adapter.
6. UI/presentation layer.
7. Persistence/project-format layer.

The hardware adapter must not depend directly on Tracktion objects.

The domain model must not depend on Android classes.

The realtime engine must not depend on UI objects.

The UI must dispatch semantic commands instead of raw MIDI bytes.

### 2.4 Incremental vertical slices

Prefer a small working vertical slice over a large unfinished subsystem.

A good change proves a complete path such as:

**MPC pad → MIDI → semantic event → sampler → audio output → hardware feedback**

Do not add large speculative frameworks unless they directly support the next testable milestone.

---

## 3. Technology foundation

Target stack:

- **Android**
- **C++20 native core**
- **JUCE 9.0.2**
- **Tracktion Engine**
- **Google Oboe 1.10.0**
- **Ableton Link 4.0**
- Android MIDI APIs / USB MIDI transport

Current dependency pins are recorded in:

- `THIRD_PARTY_NOTICES.md`
- `.gitmodules`

Do not silently upgrade major dependencies while implementing features.

When an upstream dependency must change, document why, update the pin deliberately, and verify the entire affected build.

---

## 4. Repository discipline

### 4.1 Main branch

`main` is the stable reference.

Do not use `main` as an experimental workspace.

Do not force-push, reset, rewrite history, or mix unrelated experiments into `main`.

### 4.2 Feature branches

Meaningful implementation work belongs in a dedicated feature branch.

Use descriptive names such as:

- `feature/midi-transport`
- `feature/audio-engine`
- `feature/sampler`
- `feature/mpc-ui`
- `feature/lcd-feedback`

A branch should have one coherent objective.

### 4.3 Commits

Prefer small, descriptive commits.

A commit should answer:

- what changed;
- why it changed;
- what was verified.

Do not hide unrelated cleanup inside a functional commit.

### 4.4 Before editing

Before changing an existing subsystem:

1. inspect the current source;
2. inspect its callers;
3. inspect related documentation;
4. check recent commits on the active branch;
5. identify the current known-good build/commit;
6. understand existing tests and CI.

Do not reconstruct a file from memory when its current repository version can be read.

### 4.5 After editing

After meaningful changes:

1. build;
2. inspect build output;
3. run the smallest relevant test;
4. check GitHub Actions;
5. record failures and evidence;
6. only then proceed to the next layer.

If a build fails, fix the build before adding unrelated functionality.

---

## 5. Android platform rules

Use a thin Android layer.

Android-specific classes should primarily handle:

- application lifecycle;
- USB/MIDI discovery;
- permissions;
- activity/window integration;
- Android audio-device integration;
- file/document access;
- platform callbacks.

Keep musical logic in native/domain code.

Current Android baseline:

- minSdk 24
- targetSdk 36
- compileSdk 36
- CMake 3.22.1
- NDK 28.2.13676358
- Java 17
- Gradle 9.6

Keep these values synchronized with the repository build configuration.

---

## 6. MIDI transport

The current first transport boundary uses Android's MIDI API.

The bridge is responsible for:

- enumerating MIDI devices;
- identifying the MPC Studio MkII;
- selecting a suitable public MIDI port;
- opening input/output ports;
- receiving MIDI;
- sending outgoing MIDI;
- handling device lifecycle.

The bridge must not interpret musical meaning.

The native layer receives MIDI messages and hands them to the hardware/domain adapter.

The application should never automatically connect to an unrelated MIDI device merely because it is available.

---

## 7. MPC Studio MkII hardware contract

The project relies on public reverse-engineering research for the initial protocol map.

Primary reference:

https://github.com/bcrowe306/MPC-Studio-Mk2-Midi-Sysex-Charts

Practical mapping reference:

https://github.com/gstepniewski/MPC-Studio-Mk2-Ableton-Midi-Remote-Script

Important hardware features to support:

- 16 velocity-sensitive physical pads;
- pad velocity;
- pad aftertouch/pressure;
- physical buttons;
- button LED feedback;
- RGB pad LED feedback;
- transport controls;
- jog wheel;
- jog-wheel press;
- touch strip;
- touch-strip LEDs;
- 160x80 LCD;
- MIDI SysEx.

Known first-pass mappings currently represented by the repository hardware map include:

- buttons on MIDI channel 1 (represented internally as zero-based channel 0);
- pads on MIDI channel 10 (represented internally as zero-based channel 9);
- jog rotation: CC 100;
- jog press: note 111;
- touch strip: CC 33.

These are **initial documented mappings**, not immutable truth.

Physical hardware testing is required before treating reverse-engineered behavior as production-confirmed.

Protocol confidence should be tracked as:

- CONFIRMED — verified on actual MkII hardware;
- PROBABLE — supported by multiple independent sources but not yet physically verified;
- UNCONFIRMED — plausible but not yet reliable enough for production logic.

---

## 8. Hardware feedback

Feedback must eventually include:

### Pads

- individual RGB control;
- on/off state;
- performance state;
- bank/mode indication.

### Buttons

- LED state;
- mode/state indication;
- transport indication.

### Touch strip

- value/state;
- indicator LEDs.

### LCD

The MkII LCD is 160x80.

Implement it as a dedicated hardware-feedback subsystem rather than embedding display protocol inside random UI code.

The LCD protocol must support:

- frame generation;
- chunking;
- SysEx transport;
- frame replacement;
- diagnostics.

Do not copy proprietary Akai firmware graphics or binary resources.

Original application graphics are required.

---

## 9. MPC domain model

The core musical model is:

**Project → Sequence → Track → Program → Pad → Sample Layer → Sample**

The current domain model intentionally includes:

### Project

Contains sequences, programs and sample references.

### Sequence

Contains:

- tempo;
- time signature;
- length;
- tracks.

### Track

Contains:

- name;
- program association;
- patterns.

### Program

Initial program types:

- Drum;
- Keygroup;
- Plugin;
- Audio.

### Pad

A logical drum program contains up to 128 logical pads.

The MPC Studio MkII exposes 16 physical pads at one time; physical bank/mode selection maps those controls onto the logical program.

A pad must support:

- trigger mode;
- volume;
- pan;
- tuning;
- velocity scaling;
- mute/solo;
- mute groups;
- polyphony;
- sample layers.

### Sample layers

A pad may eventually contain up to eight sample layers.

A layer carries:

- sample reference;
- start/end region;
- loop points;
- gain;
- pan;
- tuning;
- velocity range;
- enabled state.

### Pattern events

Pattern note events should be capable of supporting:

- position;
- duration;
- note;
- velocity;
- probability;
- ratchet.

Do not make the domain model dependent on Tracktion Engine classes.

---

## 10. Audio architecture

Tracktion Engine is the planned high-level audio/sequencing foundation.

JUCE provides the native framework and platform integration layer.

Oboe provides Android low-latency audio capability where appropriate.

The intended architecture is:

**UI → semantic command → MPC domain → Tracktion adapter → audio graph/device → JUCE/Oboe → Android audio subsystem**

The Tracktion adapter must translate domain concepts into engine objects.

Do not leak Tracktion-specific types through the public MPC domain API.

The first audio milestone is deliberately tiny:

1. initialize the engine;
2. open/select an Android audio device;
3. load one sample;
4. trigger the sample from one MPC pad;
5. hear it through the device;
6. send corresponding pad feedback.

Once that path works, expand to voices, layers, envelopes, filtering and sequencing.

---

## 11. Sampler requirements

The sampler is a central subsystem.

Required direction:

- sample loading;
- sample recording;
- monitor;
- threshold;
- trim;
- crop;
- chop;
- assign to pad;
- multi-layer playback;
- tuning;
- level;
- pan;
- envelope;
- filter;
- trigger modes;
- mute groups;
- polyphony.

The sampler must be deterministic enough for live performance.

Avoid unnecessary conversions and copies in the realtime path.

Sample editing may happen off the audio thread.

---

## 12. Sequencer requirements

The sequencer should grow toward the compact MPC workflow.

Required direction:

- record;
- overdub;
- quantize;
- swing;
- pattern/sequence editing;
- grid editing;
- step sequencing;
- probability;
- ratchets;
- automation;
- transport;
- looping.

Do not implement the entire sequencer in one change.

Prove each timing feature independently.

---

## 13. MPC-style feature surface

The intended functional surface includes:

- Main;
- Browser;
- Sampler;
- Sample Edit;
- Grid;
- Step Sequencer;
- Track Edit;
- Mixer;
- Pad Mixer;
- 16 Levels;
- Pad Perform;
- Q-Link;
- Note Repeat;
- Next Sequence;
- XYFX;
- Project.

These are functional targets.

They are not permission to copy proprietary Akai artwork, source code, textures or firmware resources.

---

## 14. UI rules

### Mandatory display orientation

**The application display must be landscape/horizontal. This is a hard project requirement, not a preference.**

The primary visual composition must use a horizontal aspect ratio and horizontal screen orientation, matching the hardware-oriented workflow of compact standalone MPC workstations such as the MPC One.

Do not introduce portrait-first layouts, portrait-only screens, or responsive behavior that silently switches the application into vertical orientation. Individual screens may adapt their internal layout within the horizontal canvas, but the application's display orientation remains landscape.

The UI must be:

- touch-friendly;
- landscape-first;
- high information density without becoming unreadable;
- performance-oriented;
- fast to navigate;
- usable from both screen and hardware;
- visually original.

When redesigning screens:

**technical function has priority over decoration.**

Preserve important information and control semantics.

Controls may be resized or restyled, but do not accidentally remove or hide essential functions.

Waveforms, faders, BPM controls, transport, meters and other core DJ/music controls must remain practically usable.

Do not let purely decorative elements consume space needed by functional controls.

Avoid reproducing MPC Desktop's large workspace assumptions.

---

## 15. Ableton Link

Ableton Link is a later transport/synchronization layer.

Required eventual capabilities:

- tempo;
- beat phase;
- start/stop;
- quantized launch.

Link integration must not become a prerequisite for basic local audio playback.

The application must remain useful without a Link peer.

---

## 16. Project persistence

The application needs its own stable project model and serialization.

The internal project format must be independent of the reverse-engineered MPC file-format research.

MPC project-file research may inform future interoperability:

https://github.com/kurtjcu/MPC-project-file-definitions

Do not claim full compatibility unless it has actually been implemented and tested.

Potential interoperability work belongs late in the roadmap, after the internal model and sampler/sequencer are stable.

---

## 17. External I/O

Eventually support:

- USB audio input/output;
- external MIDI devices;
- routing;
- additional controllers.

Do not let generic-device support destabilize the primary MPC Studio MkII path.

---

## 18. Testing philosophy

Tests must exist at multiple levels.

### Unit level

Test:

- MIDI parsing;
- hardware mappings;
- SysEx encoding;
- domain model operations;
- project serialization;
- timing calculations.

### Integration level

Test:

- Android MIDI discovery;
- device open/close;
- native bridge;
- sampler trigger;
- hardware feedback.

### Physical level

The real MPC Studio MkII is the final authority for hardware behavior.

Physical tests should record:

- phone/device;
- Android version;
- APK commit/build;
- MIDI port name;
- physical action;
- observed MIDI bytes;
- expected behavior;
- actual behavior;
- photo/video where useful.

Do not declare a hardware protocol feature "done" from documentation alone.

---

## 19. CI/CD

Every meaningful feature branch should trigger Android CI.

The workflow should:

1. checkout repository and submodules;
2. configure Java;
3. configure Gradle;
4. configure Android SDK;
5. install required Android toolchain components;
6. assemble debug APK;
7. publish the APK as an artifact.

CI failures must be diagnosed from the actual failing step.

Do not assume a source-code failure when the failure is caused by runner/toolchain infrastructure.

Do not report a build as successful until the relevant Actions run actually succeeds.

---

## 20. Failure discipline

When something breaks:

1. stop adding unrelated features;
2. inspect the exact failure;
3. identify the smallest responsible layer;
4. fix only that layer;
5. rebuild;
6. verify;
7. continue.

Do not repeatedly apply the same failed fix.

Do not replace a complex problem with a hidden workaround that makes diagnosis harder.

Prefer explicit diagnostics over silent fallbacks.

---

## 21. Agent operating procedure

At the beginning of a coding task:

1. Read `AGENTS.md`.
2. Read this master prompt.
3. Read the most relevant specific documents under `docs/`.
4. Inspect the current branch and recent commits.
5. Identify the current known-good baseline.
6. Make the smallest coherent change.
7. Build/test.
8. Inspect CI.
9. Record what was actually proven.

Before modifying a subsystem, ask:

- What is its current public boundary?
- Which layer should own this behavior?
- What evidence supports the proposed implementation?
- Can the change be tested independently?
- Could this break audio realtime safety?
- Could this break the physical controller path?
- Could this make future agents misunderstand the architecture?

---

## 22. Do not do these things

Never:

- work directly on `main` for experimental implementation;
- scatter hardware MIDI numbers through UI code;
- put Android lifecycle logic inside the audio engine;
- put UI dependencies in realtime audio code;
- block the audio callback;
- assume reverse-engineered hardware behavior is confirmed;
- silently replace the selected MPC device with an arbitrary MIDI device;
- copy proprietary Akai code or artwork;
- declare builds passing without checking CI;
- hide failures behind broad exception swallowing;
- perform large unrelated refactors during a targeted feature;
- destroy a known-good baseline without a deliberate recovery point;
- claim a feature is physically verified when only a simulator/build was tested.

---

## 23. Current roadmap

### Stage 0 — Foundation
Android shell, native C++ core, dependencies, CI and documentation.

### Stage 1 — Hardware bring-up
MIDI discovery, ports, pads, velocity, aftertouch, buttons, jog, touch strip.

### Stage 2 — Hardware feedback
Pad RGB, button LEDs, touch-strip indicators, Note Repeat indicators, LCD.

### Stage 3 — Audio foundation
Tracktion integration, engine lifecycle, audio devices, low-latency path, sample playback.

### Stage 4 — MPC domain
Projects, sequences, tracks, programs, pads, layers, automation, Q-Link.

### Stage 5 — Sampler
Record/edit/trim/chop/assign/multi-layer playback/tone controls.

### Stage 6 — Sequencer
Record/overdub/quantize/swing/grid/step/probability/ratchet/automation.

### Stage 7 — MPC-style UI
Main, Browser, Sampler, Sample Edit, Grid, Step, Track Edit, mixers, performance screens.

### Stage 8 — Ableton Link
Tempo, phase, transport and launch synchronization.

### Stage 9 — External I/O
USB audio, generic MIDI and routing.

### Stage 10 — Interoperability
Research-backed MPC project import/export subset.

---

## 24. First major acceptance milestone

The project reaches its first meaningful end-to-end milestone when this works on physical hardware:

**MPC Studio MkII pad**
→ **Android MIDI**
→ **native MPC semantic event**
→ **sampler**
→ **audio output**
→ **RGB pad feedback**
→ **LCD/status feedback**

That milestone should be treated as the foundation for everything that follows.

---

## 25. Source-of-truth hierarchy

When information conflicts, prefer evidence in this order:

1. Current source code and tests.
2. Successful CI/build output.
3. Physical MPC Studio MkII testing.
4. Current repository architecture documents.
5. Official Android/JUCE/Tracktion/Oboe/Ableton documentation.
6. Multiple independent hardware reverse-engineering sources.
7. Old conversation notes or remembered assumptions.

When uncertainty remains, document it instead of silently guessing.

---

## 26. Agent response/reporting standard

At the end of meaningful work, report:

- active branch;
- exact commits made;
- files/subsystems changed;
- build/test status;
- what is proven;
- what is not yet proven;
- the next smallest concrete engineering step.

Do not inflate partial progress into completed functionality.

The goal is not to produce impressive-looking code.

The goal is to build a stable, testable musical instrument around the MPC Studio MkII.
