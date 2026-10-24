# Plugins and Effects — MPC MK2 Groovebox

**Status:** Architecture proposal; no third-party instrument/effect is approved for production yet.  
**Research snapshot:** 2026-10-23  
**Applies to:** Android, C++20, current Oboe audio path, MPC project/track/pad model.

## 1. Product intent and decision summary

The project is intended to be distributed free of charge as an open-source application. The owner does not currently plan to sell the app. This removes the requirement to preserve a closed-source commercial product strategy, so GPL-family components can be considered alongside permissive MIT/BSD components.

This is **not** a license exemption: distributing an APK for free still counts as distribution, and every included component's conditions must be met. Do not add a "non-commercial only" restriction merely because the author does not plan to charge for the app; that would constrain downstream users and is not the normal meaning of an open-source software license.

The preferred product direction is to reuse existing DSP engines and code rather than implement each synth/effect algorithm from scratch. Our code should concentrate on adapters, MIDI/state mapping, host integration, MPC-style controls, persistence, routing, and tests.

### Current decisions

1. **One real-time output path.** Oboe remains the current Android audio endpoint. Do not start one independent OS audio stream per instrument or effect.
2. **Keep real-time DSP in-process for the first releases.** Prefer native C/C++ processors that render into host-supplied buffers. A separate Android plugin process is an optional later experiment, not a v1 dependency.
3. **Do not mistake the planned Tracktion Engine integration for an active subsystem.** At the inspected repository snapshot, root `CMakeLists.txt` sets `MPC_BUILD_TRACKTION` to `OFF` by default and only adds JUCE/Tracktion Engine inside that option. The current native library links Oboe and compiles the project's own `src/Audio/AudioEngine.cpp`. Tracktion's advertised built-in processors are therefore candidates to verify, not features we may claim are already available in the APK.
4. **First integration spike: 0x808 DSP core.** It offers a high feature-to-code ratio (drum-machine synthesis, subtractive/FM/wavetable modes, sample/SF2 support and effects) under MIT. Do not import its GUI, standalone audio backend, sequencer or project format. Prove that useful instrument processors can be separated from its own workstation architecture and driven by MPC note/transport events.
5. **Effects: use selected Airwindows processors** for additional colour/lo-fi/saturation, subject to per-file dependency and license review. Do not vendor the entire collection blindly.
6. **SoundFont: avoid duplicate engines.** The 0x808 repository already vendors TinySoundFont. Reuse that copy only if its integration can be cleanly isolated; otherwise integrate upstream TinySoundFont directly. Do not carry both copies.
7. **Defer full external plugin hosting.** AAP is a promising Android plugin ecosystem, but its own README says its API is not yet stable and its host/plugin model uses separate Android processes communicating through Binder/shared memory. That adds lifecycle, parameter, failure and latency complexity. Revisit only after the in-process graph is stable.
8. **No performance promises before device measurements.** The Redmi Note 13 Pro 5G is our first real target. Track count and polyphony targets below are acceptance-test goals, not existing benchmark results.

## 2. Current repository baseline and constraints

The live branch inspected for this plan was `feature/mpc-one-ui-fidelity` at commit `2c00c1603ea7228e4eacd384a4e73b6ac0fedb69`. The associated Android Build #3113 (run `39984123027`) was reported successful for that exact SHA. The work here is documentation-only and must not be treated as a code integration or as evidence that audio-plugin acceptance has passed.

Relevant code observations:

- Root `CMakeLists.txt`: the current shared native library includes `src/Audio/AudioEngine.cpp` and links `third_party/oboe`.
- `MPC_BUILD_TRACKTION` exists but defaults to `OFF`; JUCE and Tracktion Engine are added only when enabled.
- `README.md` describes Tracktion Engine/JUCE as the planned foundation. The build graph makes clear that the planned integration is not currently enabled by default.
- The current audio implementation contains project-owned Oboe/sample/recording code. Any change to the rendering path must preserve existing sample, recording, MIDI, sequencer, persistence and controller behavior.
- No repository-root `LICENSE` file was found at the inspected ref. The app's own top-level license is therefore still an open governance item. Do not infer it from the license of one dependency.

The current UI and project-persistence acceptance queue has priority over new audio-plugin implementation. This document records a future plan; it does not authorize interrupting the active UI/Browser/Project acceptance slice. Before implementation, re-read `docs/SESSION-STATE.md`, `docs/IMPLEMENTATION-ROADMAP.md`, current source, live PR head and exact-SHA Actions evidence.

## 3. Target audio graph

The intended user experience is a groovebox-style project, not a collection of separate audio apps:

```text
MPC MIDI / sequencer / pads
        |
        +--> Drum instrument / sampler
        +--> Synth instrument(s)
        +--> SoundFont/SFZ instrument (optional)
        |
   Per-track insert chain
        |
    Track mixer (gain / pan / mute / solo)
        |
        +------ Send A ------> Shared reverb ------+
        +------ Send B ------> Shared delay -------+
        |                                          |
        +------------------------------------------+
                         |
              Master insert chain
                         |
                    Oboe output
                 (phone or USB device)
```

This is a target architecture, not a statement that all blocks exist today.

The audio engine must remain the only owner of real-time mixing and output. Instruments receive prepared MIDI/note events and a render request, and write audio to supplied buffers. File loading, preset parsing, plugin discovery, UI, persistence, Android IPC and expensive resource preparation happen off the real-time callback.

### Integration contract

For any processor we adopt, the host needs a narrow, testable adapter contract covering:

- stable processor type ID and version;
- input/output channel layout and supported sample rates;
- prepare/release lifecycle, sample rate and maximum block size;
- process-audio entry point with no UI dependencies;
- note-on/note-off and parameter-event delivery where applicable;
- parameter metadata, ranges, defaults and automation mapping;
- state capture/restore without doing file I/O inside the audio callback;
- deterministic bypass and silence behavior on failure;
- no allocation, blocking lock, file/network I/O or logging on the real-time path;
- explicit source/license/version metadata for the processor.

Do not define a generic VST-like hosting API with dozens of unneeded concepts before the first working instrument. Implement the smallest internal adapter that is sufficient for 0x808 and one effect, then generalize only when a second implementation demonstrates the need.

## 4. Candidate ranking — instruments and synth engines

The ranking is about fit for *this project*, not a claim about sound quality or measured CPU speed. Each candidate still requires an Android NDK build, integration spike and on-device benchmark.

### A. 0x808 DSP engine — **P0 / first feasibility spike**

Repository: https://github.com/averagenative/0x808  
License stated by upstream: MIT. Its README describes a pure C99 DSP engine separated from the GUI/audio-driver layer; available features include drum-machine synthesis, subtractive, four-operator FM and wavetable synthesis, sample playback, SoundFont support, and per-track/master filter, delay, reverb, overdrive, fuzz and chorus.

**Why it is first:** one upstream codebase may cover the drum-synth and several melodic-synth roles, reducing the number of independent integrations.

**Important caveat:** 0x808 is a whole workstation as well as a DSP engine. Its internal engine has its own tracks/mixer/transport and is not already a drop-in Android plugin. The spike must determine whether we can use per-instrument processors with externally supplied MIDI and sample-accurate rendering, without importing the GUI, another OS audio stream, duplicate sequencer, internal project save format or unwanted track mixer. If only a whole-engine integration is practical, do not let its internal sequencer replace the MPC domain/transport.

**Bundled assets:** upstream describes bundled drum samples as CC0, but verify the license manifest for every asset actually copied. Do not copy assets merely because the engine code is MIT.

### B. TinySoundFont — **P1 / compact SF2 option or 0x808 fallback**

Repository: https://github.com/schellingb/TinySoundFont  
License: MIT.

This is a small SoundFont 2 synthesizer implemented in C/C++, useful for an additional MIDI instrument with SF2 presets. The upstream source documents unsupported SoundFont modulators and limitations around the built-in chorus/reverb sends.

Use it if SF2 support is needed and the 0x808 SF2 path cannot be cleanly extracted. Since 0x808 already vendors TinySoundFont, reuse exactly one code copy. Do not add a second independent SF2 engine before proving the feature gap.

SoundFont files are separate works: only bundle presets/libraries with explicit redistribution rights.

### C. Dexed — **P2 / dedicated FM alternative**

Upstream: https://github.com/asb2m10/dexed  
Android port: https://github.com/atsushieno/aap-juce-dexed

Dexed is an existing FM synthesizer; the Android port is built through AAP/JUCE. Upstream/port distribution is GPL-family, so it can be considered for an open-source release, provided the full app and notices comply with applicable terms.

**Integration choices to compare:** use the existing Android AAP port as a separate plugin app (only if we implement/test an AAP host), versus port/host the synth processor in-process in the common audio graph. Neither is assumed to be ready in our APK today. The first choice crosses Android process boundaries; the second may require more adaptation and a review of the JUCE/GPL build/licensing arrangement.

Because 0x808 already includes FM synthesis, Dexed is only justified if it materially improves FM patch compatibility, expressive control or sound quality beyond the simpler FM mode.

### D. Odin 2 — **P3 / optional broad synth**

Upstream: https://github.com/TheWaveWarden/odin2  
Android port: https://github.com/atsushieno/aap-juce-odin2

Odin 2 is a richer synthesizer option under GPLv3. The Android port README says it builds and shows the UI but still has UI elements that may not work properly. Treat it as an experimental/future candidate, not a dependency for the first audio slice. It overlaps with the synth modes planned for 0x808 and must justify the extra binary, integration and runtime cost.

### E. Mutable Instruments Plaits — **P4 / selective algorithm port**

Upstream: https://github.com/pichenettes/eurorack  
License scope: upstream distinguishes MIT for STM32 projects from GPL-3.0 for AVR projects; the exact files used must be checked.

Plaits is a macro-oscillator algorithm collection rather than a ready Android plugin. It would require an instrument wrapper, voice allocation, MIDI integration, parameter/state mapping and UI. Consider it only if the first synth set leaves a specific sound-design gap. Use a new product name and original UI; do not imply endorsement or use trademarks improperly.

### Deferred candidates

- Surge XT: potentially valuable, but intentionally not an initial dependency because it brings a larger synthesis engine and a more complex port/build. Reconsider after basic multi-track performance is measured.
- sfizz: BSD-2-Clause SFZ engine candidate if advanced SFZ libraries become a real requirement. It is an alternative to the SF2 path, not an immediate addition alongside TinySoundFont.
- AAP: future optional plugin ecosystem, not the core routing/processing architecture for v1.

## 5. Candidate ranking — effects and MPC-like minimum set

### First reuse, then add

Tracktion Engine advertises existing processor classes (including basic 4OSC/sampler synths, chorus, compressor, delay, four-band EQ, low-pass, phaser, pitch shifter, tone generator, volume/pan, insert/send/return/rack facilities) and an API for registering processors. See https://github.com/Tracktion/tracktion_engine/blob/develop/FEATURES.md.

However, those processors cannot be treated as currently available in the APK until the `MPC_BUILD_TRACKTION=ON` route has been built and the processor graph is demonstrated to work with our required single Oboe output path. The first architecture spike must establish whether Tracktion becomes the canonical graph host or whether the current Oboe engine remains the graph owner. Do not run two independent mixers or two output streams.

Airwindows repository: https://github.com/airwindows/airwindows  
License: MIT. An experimental Android AAP port exists at https://github.com/atsushieno/aap-airwindows, but the recommended first route is to evaluate selected DSP source/processors for direct in-process integration rather than depending on the AAP port.

### Mandatory effect capabilities

The MPC One / MPC 3.9 user guide is a product reference, not a source of proprietary code or assets. Use it to determine expected user-facing categories; reimplement or reuse legally distributable open code only.

| Capability | First release expectation | Preferred source/path |
|---|---|---|
| Track gain, pan, mute, solo | Essential | Host mixer / current domain model |
| Low-pass and high-pass filtering | Essential | Verified host processor or selected in-process DSP |
| Parametric EQ | Essential on track and master | Tracktion processor only after graph gate; otherwise selected open DSP |
| Compressor | Essential on track; master dynamics later | Verified host processor or compatible open source |
| Peak limiter | Essential safety/master tool | Existing compatible processor; test for true peak/oversampling claims before labelling |
| Saturation / overdrive / distortion | Essential creative effect | 0x808 if independently reusable; selected Airwindows processor |
| Tempo delay | Essential, sync to MPC transport | Shared return plus optional insert |
| Algorithmic reverb | Essential, shared return | 0x808 or verified host processor; one shared return first |
| Chorus | Essential modulation effect | 0x808 or verified host processor |
| Phaser | Standard modulation effect | Host/selected source after verification |
| Flanger | Standard modulation effect | Add after baseline if not covered by chosen processors |
| Tremolo / auto-pan | Standard utility/modulation | Add only after core set |
| Bitcrusher / sample-rate reduction | Strongly desired for electronic music | Selected Airwindows processor / verified processor |
| Gate / transient shaping | Useful second wave | Only after CPU/persistence/automation foundations |
| Pitch shift / time stretch | Not a first-wave requirement | Evaluate as a separate, potentially expensive processor |
| Convolution reverb | Deferred | Larger CPU/memory/asset cost; measure before acceptance |

This is a capability checklist; exact processor names and routing semantics must be checked against the actual implementation and licensing before being shown as available.

## 6. Licensing policy for an open-source release

### Intent

The project owner intends to distribute the application for free as open-source software and does not presently plan to sell it. Open-source reuse is the priority; a restrictive non-commercial clause is not requested.

### Practical rules

1. Keep a `THIRD_PARTY_NOTICES.md` entry for every imported dependency: upstream URL, pinned commit/tag, license text/path, files/components used, local modifications and asset provenance.
2. MIT/BSD/ISC/CC0 are usually straightforward if notices and attribution are retained.
3. GPLv3 and AGPLv3 are acceptable candidates for evaluation because the application is intended to be open source, but their reciprocal-source and redistribution requirements must be followed.
4. Do not assume an upstream project's root license covers every subdirectory, font, sample, preset, impulse response, generated asset or vendored dependency.
5. Do not copy VST binaries, proprietary AIR plugins, Akai resources, commercial preset banks, or copyrighted audio content merely because a source-code engine is open source.
6. Do not use a non-commercial license to simplify the situation; it could contradict the intended open-source reuse model.
7. Before public APK redistribution, resolve the project's own top-level license. The inspected branch had no root `LICENSE` file. JUCE uses AGPLv3/commercial dual licensing; Tracktion Engine uses GPLv3-or-later/commercial licensing. Since Tracktion is currently optional/off, this must be a deliberate architecture and license decision, not an inherited assumption. Legal compatibility should be checked at the exact versions/commits we ship.

This plan is engineering documentation, not legal advice.

## 7. Performance targets for the Redmi Note 13 Pro 5G

The following are **initial test profiles, not measured capacity claims**:

- Baseline: 8 active instrument tracks, 64 total simple-synth voices, 1–2 lightweight inserts per track, one shared reverb, one shared tempo delay, master EQ and limiter.
- Stress profile: 12–16 active tracks and up to 128 aggregate voices for lightweight instruments; separate, lower polyphony limits may apply to complex engines.
- Initial test formats: 44.1 kHz and 48 kHz where the Android route/device accepts them; probe buffer sizes such as 128, 192 and 256 frames instead of hard-coding a universal promise.
- Sustained test: at least 30 minutes under repeatable load, including thermal steady state; run with screen interaction and representative MIDI automation.
- Record audio callback duration/deadline misses, xrun counters, CPU estimate, active voices, memory high-water mark and thermal behavior. Do not claim a configuration is accepted if audible dropouts or xrun counters appear.

Use one shared effect send for reverb/delay before adding per-track duplicates. Do not run oversampling on every processor by default.

## 8. Acceptance gates

A candidate becomes approved only after:

- exact upstream commit/tag pinned;
- license files and transitive dependencies/assets reviewed;
- Android ABI/NDK build succeeds;
- smoke test produces deterministic non-silent audio from MIDI events;
- parameters and note-off/panic behavior are verified;
- preset/state save-load round-trip passes;
- effect bypass and graph reorder do not corrupt audio;
- no audio callback allocation, lock, I/O or UI interaction is introduced;
- multi-track and sustained tests pass on the target phone;
- an honest fallback state is shown if a candidate is unavailable or fails to instantiate.

No candidate is approved by this document alone.

## 9. Upstream references

- Tracktion Engine features and license: https://github.com/Tracktion/tracktion_engine/blob/develop/FEATURES.md and https://github.com/Tracktion/tracktion_engine/blob/develop/LICENSE.md
- JUCE licensing: https://github.com/juce-framework/JUCE/blob/master/LICENSE.md
- Oboe guide: https://github.com/google/oboe/blob/main/docs/FullGuide.md
- 0x808 features/license: https://github.com/averagenative/0x808 and https://github.com/averagenative/0x808/blob/main/LICENSE
- Airwindows license: https://github.com/airwindows/airwindows/blob/master/LICENSE
- TinySoundFont: https://github.com/schellingb/TinySoundFont
- AAP architecture/status: https://github.com/atsushieno/aap-core
- Dexed Android port: https://github.com/atsushieno/aap-juce-dexed
- Odin 2 Android port: https://github.com/atsushieno/aap-juce-odin2
- Mutable Instruments license scope: https://github.com/pichenettes/eurorack
- MPC standalone v3.9 user guide: https://cdn.inmusicbrands.com/Software/15JM26PSBC/MPC%20Standalone%20OS%20-%20User%20Guide%20-%20v3.9.pdf
