
# Internal DSP Library Research — MPC Studio MkII Groovebox

**Status:** RESEARCH / DECISION SUPPORT  
**Branch:** research/internal-dsp-library  
**Scope:** internal/offline audio engines only; external AAP/VST/CLAP/LV2 hosting is explicitly out of scope for this phase.  
**Commercial target:** future proprietary/commercial Android distribution, subject to a formal legal review before release.

> This is an engineering license-triage and architecture document, not legal advice. A commercial release requires a final dependency/SBOM/license audit against the exact commits, source files, generated code, presets, samples, artwork and build artifacts actually shipped.

## 1. Executive decision

The project should build its first internal instrument/effect library from permissive-licensed DSP components, wrapped behind our own native C++ audio-node contract.

Preferred first-wave sources:

1. Airwindows — large MIT DSP effect catalogue.
2. DaisySP — MIT core synthesis/DSP modules; explicitly exclude its LGPL module set.
3. Signalsmith DSP — MIT header-only DSP primitives.
4. Signalsmith Basics — MIT reusable effects.
5. DSPFilters — MIT IIR/filter design and processing.
6. DSPark — MIT, modern C++20, header-only, zero-dependency DSP collection.
7. sndkit — MIT/Unlicense dual-licensed tangled output; compact portable algorithms.
8. Gamma — permissive MIT-style license; broad synthesis/filtering toolkit.
9. MSFA / music-synthesizer-for-Android — Apache-2.0 FM/DX7-oriented engine.
10. STK — MIT C++ synthesis/physical-model toolkit.
11. sfizz — BSD-2-Clause SFZ engine; technically attractive, but upstream is archived and therefore lifecycle-risk.
12. CMSIS-DSP — Apache-2.0 ARM DSP primitives where they materially improve Android ARM performance.
13. KISS FFT — BSD-3-Clause FFT building block.
14. SpeexDSP — BSD-3-Clause processing primitives.
15. libsamplerate — BSD-2-Clause sample-rate conversion.

Architectural rule:

borrow DSP implementation, own the plugin contract, own the UI, own presets/state schema, own parameter IDs, own automation mapping.

Do not embed foreign plugin GUIs into the MPC UI.

## 2. Green / Yellow / Red definitions

### GREEN

Suitable for the planned proprietary/commercial architecture from a licensing standpoint, assuming the exact component used is covered by the stated permissive license and all required copyright notices are preserved.

Typical licenses:

- MIT
- BSD-2-Clause / BSD-3-Clause
- Apache-2.0
- Public domain / CC0 for code where clearly established

Green is not a substitute for file-level provenance. Dependencies and bundled assets still require an audit.

### YELLOW

Potentially usable, but requires additional engineering or licensing review.

Typical reasons:

- LGPL obligations;
- special license exceptions;
- archived/unmaintained upstream;
- mixed-license tree;
- large framework footprint;
- platform mismatch;
- generated-code licensing complexity;
- non-code assets under separate licenses.

Yellow means evaluate deliberately, not forbidden.

### RED

Do not use in the proprietary/commercial internal core under the present strategy.

Typical reasons:

- GPL/AGPL copyleft on the engine/library as a whole;
- commercial-license requirement for proprietary use;
- explicit non-commercial restrictions;
- source/binary licensing that does not grant the rights needed by our architecture;
- proprietary assets inseparable from the proposed integration.

A technically excellent Red project may still be valuable as a research/reference source. It is not an approved dependency.

## 3. Candidate matrix

| # | Candidate | Primary role | License / status | Tier | Android/C++ fit | Decision |
|---:|---|---|---|---|---|---|
| 1 | Airwindows | FX / saturation / delay / reverb / colour | MIT | GREEN | Excellent C/C++ DSP fit | Adopt first |
| 2 | DaisySP core | synth / FX / physical modelling | MIT core | GREEN* | Excellent; Android explicitly supported | Adopt first |
| 3 | Signalsmith DSP | DSP primitives | MIT | GREEN | Excellent; header-only C++ | Adopt |
| 4 | Signalsmith Basics | chorus / limiter / reverb / analyser / frequency FX | MIT | GREEN | Excellent; simple C++ API | Adopt |
| 5 | DSPFilters | IIR filters / EQ | MIT | GREEN | Excellent | Adopt |
| 6 | DSPark | broad modern DSP / synth / FX | MIT | GREEN | Excellent; C++20/CMake | Evaluate first-wave |
| 7 | sndkit | compact C DSP algorithms | MIT/Unlicense output | GREEN | Excellent | Adopt selectively |
| 8 | Gamma | synthesis / filtering / FFT / signal processing | permissive MIT-style | GREEN | Good C++ source integration | Evaluate |
| 9 | STK | physical-model / algorithmic instruments | MIT | GREEN* | Good portable C++; source-selective use | Evaluate |
| 10 | MSFA | FM / DX7-style synthesis engine | Apache-2.0 | GREEN | Excellent; Android-native origin | Adopt for FM |
| 11 | sfizz | SFZ sample instrument engine | BSD-2-Clause; archived | YELLOW | Strong C++ fit | Evaluate, pin exact commit |
| 12 | ymfm | Yamaha FM/chip emulation | BSD-3-Clause | GREEN | Good native C++ | Evaluate for FM variants |
| 13 | AudioKitEX | C/C++ DSP modules | MIT | GREEN | C/C++ useful; surrounding ecosystem Apple-oriented | Use source selectively |
| 14 | SoundpipeAudioKit | filters / FX / oscillators | MIT | GREEN | C components are portable | Source mine selectively |
| 15 | AudioKit Synth One | complete synth reference/source | MIT | GREEN* | DSP useful; whole app iOS-centric | Mine DSP, do not port app UI |
| 16 | DunneAudioKit | synth / chorus / delay / sampler-oriented DSP | MIT | GREEN | Useful C/C++ modules | Evaluate selectively |
| 17 | STKAudioKit | STK instruments wrapped for AudioKit | MIT | GREEN* | Source useful; wrapper is not needed | Prefer upstream STK directly |
| 18 | DevoloopAudioKit | guitar/effect processors | MIT | GREEN | Selective source use | Low priority |
| 19 | SporthAudioKit | graph-oriented DSP | MIT | GREEN | Technically usable; architecture mismatch | Reference only |
| 20 | Freeverb | algorithmic reverb | Public domain | GREEN | Trivial integration | Reference/backup |
| 21 | Soundpipe | broad C DSP algorithms | MIT; archived | YELLOW | Portable | Use only when a unique algorithm is needed |
| 22 | libsamplerate | sample-rate conversion | BSD-2-Clause | GREEN | Excellent | Infrastructure candidate |
| 23 | CMSIS-DSP | ARM FFT/filter/vector kernels | Apache-2.0 | GREEN | Excellent on Android ARM | Performance layer |
| 24 | KISS FFT | FFT | BSD-3-Clause | GREEN | Excellent and tiny | Use where needed |
| 25 | SpeexDSP | resampling / preprocessing / filters | BSD-3-Clause | GREEN | Excellent C/Android history | Selective utility use |
| 26 | FluidSynth | SoundFont 2 synth | LGPL-2.1 | YELLOW | Very good integration fit | Evaluate after license plan |
| 27 | SoundTouch | time-stretch / pitch / rate | LGPL-2.1 | YELLOW | Android + C++ | Evaluate if needed |
| 28 | Csound | synthesis/processing runtime | LGPL-2.1 | YELLOW | Heavy architecture footprint | Reference / later evaluation |
| 29 | Faust libraries | generated DSP / many algorithms | LGPL + exceptions vary | YELLOW | Very powerful | Per-library, per-generated-code audit |
| 30 | Calf Studio Gear | large FX/instrument pack | LGPL-2.1-only + deps | YELLOW | Primarily Linux/LV2/JACK | Not a priority |
| 31 | LSP Plugins | large professional FX collection | LGPLv3 | YELLOW | Primarily desktop/plugin-oriented | Not a priority |
| 32 | KFR | modern C++ DSP / FFT / filters | mixed/commercial licensing to verify | YELLOW | Technically excellent | Do not add before license confirmation |
| 33 | Surge XT | large hybrid synth | GPL-3.0-or-later | RED | Technically strong | Research only |
| 34 | Vital source | wavetable / spectral synth | GPLv3 + separate proprietary path | RED | Technically strong | No use without explicit separate license |
| 35 | Helm | polyphonic synth | GPL-3.0 | RED | C++/JUCE, desktop-oriented | Research only |
| 36 | Odin 2 | hybrid synth | GPL-3.0 | RED | C++/JUCE | Research only |
| 37 | Dexed | DX7 plugin | GPLv3 | RED | Technically strong | Use MSFA instead |
| 38 | ZynAddSubFX | additive/subtractive/PAD synth | GPL-2.0-or-later | RED | Heavy but capable | Research only |
| 39 | SuperCollider | synthesis runtime | GPLv3 | RED | Powerful but architectural mismatch | Research only |
| 40 | VCV Rack | modular environment | GPLv3 + commercial exception | RED | Large host ecosystem | Do not embed |
| 41 | DISTRHO Cardinal | Rack-based modular plugin | GPLv3+ | RED | Large static aggregate | Do not embed |
| 42 | Bespoke Synth | modular synth | GPLv3 | RED | Heavy app architecture | Research only |
| 43 | MVerb | reverb | GPL-3.0 | RED | Tiny/self-contained DSP | Do not ship under current strategy |
| 44 | Tunefish 4 | synthesizer | GPL-3.0 | RED | C++ synth | Research only |
| 45 | OB-Xd source <=2.11 | virtual analogue synth | GPLv3 source; later versions are binary-oriented | RED | Technically useful | Do not integrate source |

* GREEN refers to the specifically approved permissive component subset, not every file or optional module in a repository.

## 4. First-wave approved source families

### 4.1 Airwindows — primary creative FX source

Repository:
https://github.com/airwindows/airwindows

Current repository LICENSE is MIT.

Use for:

- saturation;
- analogue-style colour;
- tape/console-style processing;
- distortion;
- creative delay/reverb/echo;
- stereo/utility processing;
- dynamics/level processing.

Integration rule:

Do not port VST wrappers or foreign UI. Port or vendor selected DSP implementation behind our own MpcEffectNode.

Commercial distribution requirement:

- preserve MIT copyright/license text;
- pin an exact upstream revision;
- record which source files are actually included;
- do not assume plugin trademarks or names are licensed merely because the code is MIT.

### 4.2 DaisySP — primary synthesis/DSP foundation

Repository:
https://github.com/daisyaudio/DaisySP

DaisySP explicitly supports commercial and closed-source usage under MIT. Current upstream also separates LGPL modules; our policy is MIT-core modules only.

Use for:

- oscillators;
- envelopes;
- LFOs;
- subtractive synthesis;
- FM building blocks;
- physical modelling;
- granular;
- drum synthesis;
- filters;
- phaser;
- wavefolder;
- overdrive;
- decimation;
- limiter;
- signal conditioning;
- DC blocking.

Important source provenance:

Current DaisySP licensing documentation identifies Plaits-derived code under MIT within the project. Use only components whose file-level provenance remains clear.

### 4.3 Signalsmith DSP + Signalsmith Basics

Repositories:
https://github.com/Signalsmith-Audio/dsp
https://github.com/Signalsmith-Audio/basics

Both are MIT.

Signalsmith DSP is header-only C++ DSP. Basics provides reusable effect classes including analyser, chorus, crunch, frequency shifter, limiter and reverb.

Why it fits our project:

- no plugin host required;
- directly callable C++ classes;
- explicit configuration;
- useful latency/tail information;
- small adapter boundary.

### 4.4 DSPFilters — filter/EQ foundation

Repository:
https://github.com/vinniefalco/DSPFilters

MIT license.

Use it for:

- Butterworth;
- Chebyshev;
- elliptic;
- high/low/band-pass;
- arbitrary-order IIR;
- filter/EQ building blocks.

It should sit beneath our own user-facing MpcFilter/MpcEQ implementations.

### 4.5 DSPark — modern all-round candidate

Repository:
https://github.com/CristianMoresi/DSPark

MIT license.

Current project description: C++20, header-only, zero dependencies, 100+ real-time processors, cross-platform/mobile targets.

This is unusually close to our baseline:

- C++20;
- CMake;
- mobile;
- direct vendor/include integration;
- no mandatory heavyweight framework.

Before adoption, pin a specific release/commit and benchmark on target Android devices.

### 4.6 sndkit — small algorithm source

Repository:
https://github.com/PaulBatchelor/sndkit

Tangled output is dual-licensed MIT/Unlicense; the literate text is CC0.

Use for small isolated algorithms:

- oscillators;
- filters;
- delays;
- envelopes;
- utility DSP;
- synthesis primitives.

Preferred use:

copy only the small algorithm required, retain license/provenance, and avoid pulling the interpreter/runtime into the application.

### 4.7 Gamma — broad synthesis toolkit

Repository:
https://github.com/LancePutnam/Gamma

Gamma uses a permissive MIT-style license.

It contains:

- oscillators;
- phase/frequency tools;
- filtering;
- FFT/STFT utilities;
- synthesis helpers;
- signal generators;
- generic DSP types.

It is broader than Signalsmith or DaisySP, so treat it as an evaluation/reference source rather than an automatic dependency.

### 4.8 MSFA — FM instrument

Repository:
https://github.com/google/music-synthesizer-for-android

Relevant source carries Apache-2.0 licensing.

Dexed documents that its synth engine is based on MSFA while Dexed itself is GPLv3.

This is the model we want:

MSFA engine → our InstrumentNode → our MPC UI

not:

Dexed plugin → embedded plugin wrapper/UI.

Primary use:

- DX7-compatible operator architecture;
- FM synthesis;
- future patch/import compatibility work.

Preset compatibility remains a separate provenance question.

### 4.9 sfizz — SFZ instrument engine

Repository:
https://github.com/sfztools/sfizz

BSD-2-Clause, but repository was archived on 2026-06-21.

It is an excellent conceptual fit for a sample-based instrument because the engine understands SFZ regions, envelopes, MIDI state and sample mappings.

However:

- upstream is archived;
- dependency tree needs review;
- build must be pinned;
- long-term maintenance becomes ours.

Therefore treat sfizz as Yellow for project adoption even though the core license is permissive.

## 5. Proposed internal architecture

The project should introduce an application-owned abstraction approximately like:

    MpcAudioNode
      ├── MpcInstrumentNode
      └── MpcEffectNode

An instrument/effect adapter should expose only our concepts:

    prepare(sampleRate, maxBlockSize)
    reset()
    process(...)
    setParameter(parameterId, normalizedValue)
    handleMidi(MidiEvent)
    getLatencySamples()
    getTailSamples()
    serializeState()
    restoreState()

Underlying engines must not leak:

- JUCE AudioProcessor objects;
- VST types;
- Tracktion types;
- Android types;
- foreign GUI classes;
- foreign parameter IDs.

Example:

    MpcEffectNode
        -> AirwindowsAdapter
            -> selected Airwindows DSP

and:

    MpcInstrumentNode
        -> MpcFmInstrument
            -> MSFA engine

and:

    MpcInstrumentNode
        -> MpcSfzInstrument
            -> sfizz

## 6. User-facing design

The user should see one coherent MPC-style plugin/instrument system.

Example:

    INSERT FX 1
    ┌─────────────────────┐
    │ MPC SATURATOR       │
    │                     │
    │ DRIVE       43      │
    │ TONE        57      │
    │ MIX         34      │
    │ OUTPUT       0      │
    └─────────────────────┘

The implementation can use Airwindows, Signalsmith, DSPark or another approved source.

Likewise:

    PROGRAM
    ┌─────────────────────┐
    │ MPC FM              │
    │                     │
    │ ALGORITHM       5   │
    │ FEEDBACK        2   │
    │ OP1 LEVEL       98  │
    │ ...                 │
    └─────────────────────┘

Foreign engine names should not become the application UI by accident.

## 7. First internal factory

Do not ship 45 tiny foreign plugins.

Build approximately 20 coherent internal processors from approved source families.

### Effects

1. MPC Filter
2. MPC Parametric EQ
3. MPC Compressor
4. MPC Limiter
5. MPC Gate
6. MPC Saturator
7. MPC Tape
8. MPC Distortion
9. MPC Chorus
10. MPC Phaser
11. MPC Flanger
12. MPC Delay
13. MPC Ping-Pong Delay
14. MPC Reverb
15. MPC Bit Crusher / Decimator
16. MPC Stereo
17. MPC Beat Repeat
18. MPC Stutter
19. MPC Multi-FX
20. MPC Master Bus

Some processors should initially use a single proven algorithm rather than multiple competing implementations.

### Instruments

1. MPC Drum Sampler — existing project sampler
2. MPC Poly Synth — DaisySP-based
3. MPC Mono/Bass Synth — DaisySP-based
4. MPC FM Synth — MSFA-based
5. MPC Physical Synth — DaisySP/STK-based
6. MPC SFZ Instrument — sfizz, only after Yellow review
7. MPC Macro Synth — selected permissive algorithms

The application names above are product design names and do not grant rights to use third-party trademarks.

## 8. Performance policy

Every integrated DSP engine must pass a common native test contract.

Minimum tests:

- sample-rate initialization;
- block sizes 16 / 32 / 64 / 128 / 256 / 512;
- mono and stereo where supported;
- silence-input stability;
- impulse-input stability;
- parameter edge values;
- reset determinism;
- no NaN/Inf under valid input;
- bounded CPU usage;
- no allocations in process();
- no blocking I/O;
- no Android/UI interaction;
- latency/tail reporting;
- state save/restore determinism.

For instruments:

- note-on/note-off;
- velocity 1/64/127;
- pitch bend;
- modulation where supported;
- polyphony limit;
- voice stealing;
- sustain;
- reset;
- deterministic rendering for fixed seed where applicable.

## 9. Dependency policy

Do not add a source repository to third_party/ merely because it appears in this document.

Before integration:

1. choose exact upstream commit/tag;
2. inspect root license;
3. inspect submodules;
4. inspect each file actually compiled;
5. inspect examples/assets/presets;
6. inspect transitive dependencies;
7. record SPDX identifiers;
8. record copyright holders;
9. record required notices;
10. record source redistribution obligation;
11. record build flags that could pull optional copyleft code;
12. run Android build;
13. run realtime audio tests.

Prefer vendoring a small, audited source subset over linking an entire desktop plugin repository.

## 10. Commercial compliance structure

Create a dedicated inventory eventually:

    third_party/
      airwindows/
      daisysp/
      signalsmith/
      dspfilters/
      dspark/
      sndkit/
      msfa/
      ymfm/
      ...

And maintain:

    THIRD_PARTY_NOTICES.md
    docs/THIRD_PARTY-SBOM.md

For every shipped component record:

- Component
- Upstream URL
- Exact commit/tag
- SPDX license
- Copyright
- Source files included
- Transitive dependencies
- Required notices
- Changes made
- Source redistribution obligation
- Assets included
- Presets included
- Trademark restrictions
- Commercial review status

Do not describe a repository as MIT in the inventory when only a subset of files has actually been reviewed.

## 11. Explicit exclusions

The following are deliberately excluded from the internal proprietary core:

- AAP host
- VST host
- CLAP host
- LV2 host
- embedded VCV Rack
- embedded Cardinal
- embedded Surge
- embedded Vital
- embedded Helm
- embedded Odin2
- embedded Dexed

These projects can remain research references, but they are not approved internal dependencies.

## 12. Existing project dependency warning

The repository currently records:

- JUCE 9.0.2
- Tracktion Engine
- Oboe 1.10.0
- Ableton Link 4.0

in THIRD_PARTY_NOTICES.md.

Their licensing remains a separate commercial-release workstream.

Do not claim the complete application is commercially license-safe merely because the internal DSP library uses permissive licenses.

## 13. Recommended implementation sequence

### Phase A — architecture

Define:

- MpcAudioNode;
- MpcEffectNode;
- MpcInstrumentNode;
- parameter descriptors;
- state serialization;
- realtime test harness.

No external DSP code yet.

### Phase B — first Green effect

Recommended candidates:

Signalsmith Basics Limiter, or one carefully selected Airwindows DSP.

Prove:

    UI parameter
      -> semantic command
      -> audio-node parameter
      -> DSP
      -> realtime output
      -> preset/state

### Phase C — first Green synth

Build the first poly synth from DaisySP.

Prove:

    MPC pad/key event
      -> InstrumentNode
      -> voice allocation
      -> oscillator
      -> envelope
      -> filter
      -> output

### Phase D — FM

Integrate MSFA.

### Phase E — creative FX expansion

Add selected Airwindows processors.

### Phase F — advanced sample instruments

Evaluate sfizz after a dedicated dependency/license review.

## 14. Final recommendation

The project should adopt a small, audited internal DSP ecosystem, not a collection of copied desktop plugins.

Strongest initial stack:

    Core DSP:
      DaisySP
      Signalsmith DSP
      Signalsmith Basics
      DSPark
      DSPFilters
      sndkit
      Gamma

    Character FX:
      Airwindows

    Instruments:
      DaisySP
      MSFA
      STK (selective)
      sfizz (later / Yellow)

    Performance primitives:
      CMSIS-DSP
      KISS FFT
      libsamplerate
      SpeexDSP (selective)

The first implementation target is therefore:

MpcAudioNode contract → one Green effect → one Green synth → common preset/state format → realtime tests → measured Android performance.

Only after that should the internal catalogue expand.
