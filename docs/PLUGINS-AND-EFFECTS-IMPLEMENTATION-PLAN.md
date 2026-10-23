# Plugins and Effects — Implementation Plan

**Status:** Proposed execution plan; design-only until the active acceptance queue is clear.  
**Date:** 2026-10-23  
**Companion decision record:** [PLUGINS-AND-EFFECTS.md](PLUGINS-AND-EFFECTS.md)  
**Scope:** Add reusable instruments and effects to MPC MK2 Groovebox without replacing the MPC domain, duplicating the sequencer, or destabilizing the Oboe audio path.

## 0. Execution rule: do not interrupt the active acceptance work

The latest live snapshot inspected for this plan has PR #23 open on `feature/mpc-one-ui-fidelity`, with the observed branch SHA `2c00c1603ea7228e4eacd384a4e73b6ac0fedb69` and successful Android Build #3113 / run `39984123027` for that SHA. The current session/roadmap notes still identify the SAMP-010 waveform-zoom/audition slice and provider-failure work as the next queue; the docs and PR metadata are not fully synchronized.

Before any plugin code work:
1. Refresh live PR head, Actions, current `docs/SESSION-STATE.md`, `docs/IMPLEMENTATION-ROADMAP.md`, and relevant source.
2. Reconcile stale SHA/checkpoint claims without discarding still-valid evidence.
3. Finish the currently owned UI/Browser/Project acceptance slice and the provider-failure queue according to live status. Do not duplicate work already active on another branch.
4. Keep physical MPC Studio MkII acceptance as a separate hardware gate; emulator checks do not replace it.
5. Begin the audio architecture spike on an isolated branch/worktree. One writer per branch. Do not add plugin code to the UI acceptance branch while its critical workflow is active.

This plan is queued engineering work, not an instruction to merge or enable a new engine today.

## 1. Current baseline: facts that determine the plan

At the inspected repository snapshot:

- Root `CMakeLists.txt` builds the native shared library with the project's own `src/Audio/AudioEngine.cpp` and links `third_party/oboe`.
- `MPC_BUILD_TRACKTION` is an option whose default is `OFF`. JUCE and Tracktion Engine are added only inside `if(MPC_BUILD_TRACKTION)`.
- Thus Oboe is part of the current native audio path, while Tracktion Engine is a planned/optional subsystem, not a processor graph we can assume is available in the running APK.
- The existing `AudioEngine.cpp` already carries sampler/input/recording-related responsibilities. Adding processors must preserve sample playback/region/chop behavior, recording/monitoring, MIDI scheduling, sequencer timing, project persistence and hardware events.
- `THIRD_PARTY_NOTICES.md` already records JUCE 9.0.2, Tracktion Engine, Oboe 1.10.0 and Ableton Link license/pin information, but explicitly calls for a complete audit before binary distribution.
- The inspected ref had no repository-root `LICENSE` file. The product's top-level license remains to be selected/documented.

No document or upstream README is proof of runtime integration. All adoption decisions need to be confirmed in build files, source and exact-SHA tests.

## 2. Integration decision

### Preferred target

Use **one in-process native audio graph** for the project's tracks/instruments/effects, with **one Oboe output stream** on Android. Keep the MPC project, MIDI/sequencer and sample-domain models authoritative. Reused synth/effect engines are processors inside that graph, not standalone DAWs and not separate operating-system audio sessions.

The planned Tracktion Engine may be the graph/processor host if a small proof-of-concept can demonstrate a correct low-latency render path through the required single Oboe endpoint. Tracktion advertises insert/send/return/rack routing, basic synths/effects and a processor registration API. Its current build option being OFF means this must be proved, not assumed.

### Mandatory architecture spike before selecting graph ownership

Create an isolated spike that answers these questions with compiled code and a rendered test signal:

1. Can the repository's pinned Tracktion Engine/JUCE versions build for the Android ABI used by the app when `MPC_BUILD_TRACKTION=ON`?
2. Can the Tracktion render graph be driven from the current Oboe output path without creating a second audio output stream, second master mixer or hidden buffering layer that breaks the latency target?
3. Can one built-in processor (preferably 4OSC or sampler) receive deterministic note events and render non-silent audio through Oboe?
4. Can an insert/send/return path render correctly and save/restore its processor state?
5. What changes are required to the current project model and thread ownership, and can those changes be isolated behind an engine adapter?
6. Does the selected open-source distribution model satisfy the JUCE, Tracktion Engine and Ableton Link license conditions?

**Decision gate:**
- **Path A — preferred if the spike passes:** Tracktion Engine owns the canonical audio graph/processor state; Oboe remains the sole Android audio endpoint. Add external/in-house processor adapters to that graph. Do not let Tracktion open its own unrelated stream in parallel with the current Oboe owner.
- **Path B — fallback if the spike fails or causes unacceptable complexity:** retain the existing Oboe callback as audio owner and create a minimal project-owned in-process processor graph. Integrate only DSP cores that can render into supplied buffers. Do not embed all of Tracktion Engine just to borrow one effect.
- Do not maintain both graphs long-term. Record the decision and reasons in a short architecture ADR before general plugin integration.

AAP is not the v1 host. AAP uses a host/plugin process boundary with Binder IPC/shared memory and its own README says the API is still not stable. An external AAP route may be prototyped later for optional third-party plugin apps, but it must not be placed in the critical real-time path before latency and lifecycle testing.

## 3. Proposed milestones

### Milestone 0 — requirements and dependency/license inventory

**Deliverables**
- Reconcile current branch/CI status and confirm the work queue is clear.
- Write the architecture ADR from the Tracktion/Oboe spike.
- Extend `THIRD_PARTY_NOTICES.md` with exact upstream tag/commit, component/subdirectory, license file, modified status, selected source files, vendored dependency list, bundled assets and preset provenance for every accepted component.
- Decide the project's own license before publishing distributable APKs.

**License guidance**
The owner intends free-of-charge open-source distribution and does not presently plan to sell the app. This makes reciprocal licenses acceptable in principle, but does not remove redistribution obligations. The project should not impose a non-commercial-only clause unless that product policy is explicitly changed.

JUCE's community option is AGPLv3 and Tracktion Engine is GPL-3.0-or-later or commercial. Ableton Link's recorded license is GPLv2+ or proprietary. If these modules remain in the shipped build, evaluate a compatible top-level copyleft license (AGPLv3 is the first candidate to review because of JUCE) and confirm compatibility at the pinned versions. Do not treat that as an automatic final legal ruling. If we choose not to ship a dependency, document that it is not linked/compiled into the APK; a checked-out submodule alone is not evidence of inclusion.

**Gate:** license matrix is complete enough to decide which code and assets may be included; no ambiguous preset/sample/font is bundled.

### Milestone 1 — canonical audio graph and real-time contract

**Goal:** establish the host/processor boundary before importing a large engine.

The graph should support, at minimum:
- instrument node per instrument track;
- track gain/pan/mute/solo;
- ordered per-track inserts;
- a mixer sum;
- shared send/return buses for reverb and delay;
- ordered master inserts;
- one Oboe output;
- prepared parameter and MIDI events delivered at deterministic render positions.

**Realtime rules**
- Audio processing uses preallocated host-owned buffers with bounded block size.
- No file/preset I/O, network/IPC, UI calls, logging, unbounded loops, or allocation in the render callback.
- No blocking mutex on the render path. Use preallocated event queues/atomic snapshots where cross-thread exchange is required.
- Plugin creation, state restore, preset loading, sample decode, asset scans and destruction occur off the real-time thread; swap prepared instances at a safe boundary.
- Note-off, all-notes-off/panic, transport stop, suspend/resume and device route changes must not strand voices.
- Define fixed maximum input/output channels and a safe silence/bypass behavior for processors that fail or return an unsupported layout.

**Tests**
- Deterministic test tone and a one-note MIDI render.
- Silence, clipping, NaN/Inf and buffer-boundary checks.
- Processor bypass identity (within expected float tolerance).
- Audio callback allocation guard where practical, plus code review/static checks.
- Repeated start/stop and route-change stress without leaks or stuck notes.

**Gate:** one instrument renders through a single Oboe stream; existing sampler/recording tests remain green.

### Milestone 2 — 0x808 DSP-core integration spike

**Why first:** 0x808 is MIT-licensed and upstream separates a pure C99 DSP engine from GUI/audio-driver layers; it advertises drum synthesis, subtractive/FM/wavetable synthesis, sampler/SF2 support and common effects. It is the highest-density candidate, but not a ready Android plugin.

**Do not port**
- GUI/frontend code;
- SDL/miniaudio or other standalone audio I/O path;
- 0x808's own step sequencer, arrangement and playback clock;
- its own project save/load schema;
- desktop plugin wrappers (VST3/CLAP);
- optional MP3-export path or LGPL encoder unless deliberately reviewed and enabled.

**Investigate and port only if technically separable**
- engine DSP types and voice rendering;
- MIDI/note event entry points;
- per-instrument parameter access;
- sample/SF2 loader on the control thread;
- selected effect algorithms;
- required static data/presets after asset license review.

**Spike implementation**
1. Pin the exact 0x808 upstream commit.
2. Build only the minimal engine sources for Android arm64-v8a (and the emulator ABI used by CI), with warnings enabled.
3. Create a small adapter capable of preparing one 0x808 instrument, triggering notes from the MPC event layer, rendering float PCM into the host's buffer, releasing notes and resetting voices.
4. Prove that one drum voice and one pitched voice can render without the 0x808 sequencer/mixer or audio backend.
5. Test note timing, sample-rate changes, max-block-size changes, panic/stop and state/preset round-trip.
6. Measure callback budget with one instance, then several instances. Record results; do not claim a maximum track count until tested.
7. If the engine can only render one internally mixed workstation output, decide explicitly whether that is useful as one multi-part instrument. Do not pretend that its tracks are independent MPC mixer channels unless the API proves it.

**Decision:** adopt the separable C core if it passes and yields simpler integration than a comparable Tracktion-only alternative. Otherwise stop the port after the spike and select an alternative; no sunk-cost rewrite.

### Milestone 3 — baseline MPC-style effects

First inspect what is actually provided by the selected graph host and what current code already supports. Do not duplicate an effect just because it appears in the reference list.

**Minimum effects set**
1. track gain/pan and metering;
2. low-pass/high-pass filter;
3. parametric EQ;
4. compressor;
5. master limiter / peak protection;
6. saturation or overdrive;
7. tempo-synchronised delay;
8. algorithmic reverb;
9. chorus;
10. phaser;
11. flanger or another second modulation effect;
12. bitcrusher/sample-rate reduction.

Tremolo, auto-pan, gate, transient processing, pitch shift and convolution reverb are later additions unless a working processor is already available with negligible integration cost.

**Routing**
- Allow insert effects on each instrument track.
- Add exactly one shared reverb return and one shared delay return for the first version.
- Add master EQ and limiter.
- Use dry/wet, send level, bypass and reset behavior consistently.
- Ensure the shared buses remain single instances while the project plays; do not construct a new effect per buffer or note.
- Expose host-level effect parameters to MPC automation/persistence; do not require plugin UIs to be present for playback.

### Milestone 4 — Airwindows selection

**Goal:** add a small curated set of effects from the MIT-licensed Airwindows source, not the entire repository.

**Initial shortlist**
- one saturation/console/tape-colour candidate;
- one straightforward distortion/overdrive candidate;
- one Lo-Fi/bit-depth/sample-rate reduction candidate;
- one stereo utility candidate only if stereo semantics are well-defined and it passes mono compatibility checks.

Do not lock exact plugin names before source review and build testing. Airwindows contains many independent processors; each selected processor must have its own pinned source and dependency/license check.

**Integration**
- Prefer direct in-process DSP source adapted behind the chosen internal/Tracktion processor interface.
- Do not make the experimental AAP Airwindows port a mandatory dependency.
- Keep the upstream source recognizable, preserve required copyright/license notices and local-change attribution.
- Do not alter DSP behavior just to fit a UI label; map parameter ranges carefully and add a reference test preset.
- Avoid processor variants that allocate, do file I/O or depend on desktop-only APIs in the render call.

**Gate:** selected effect builds for Android, shows audible and deterministic change in an offline/native render test, has parameter/state tests and passes multi-instance load tests.

### Milestone 5 — SoundFont instrument

**Preferred ownership:** if Milestone 2 can isolate the TinySoundFont version already vendored by 0x808, use that single source copy. If not, remove/avoid the 0x808 SF2 component and integrate upstream TinySoundFont directly under its MIT license. No duplicate copies.

**Required behavior**
- load SF2 and enumerate presets on the control thread;
- show bank/program and patch name;
- receive MIDI notes/velocity/program change as supported;
- handle note-off, sustain where implemented, all-notes-off and panic;
- prepare voice buffers off the callback;
- persist SF2 reference/URI and preset selection;
- report missing/revoked content access without corrupting the project;
- do not bundle large or ambiguously licensed sound banks.

**Limits:** upstream TinySoundFont documents incomplete support for modulators and some built-in effects. Use the MPC effects graph for shared reverb/chorus rather than assuming full SF2 embedded effects support.

**Gate:** load, select preset, render multiple notes, stop/panic, save/reopen and missing-file behavior all pass. Benchmark 16/32/64 active voices as diagnostic points; acceptance is based on stable audio, not on a guaranteed number.

### Milestone 6 — advanced synth decision: Dexed vs Odin 2 vs no additional engine

Do not integrate both by default.

**Dexed** is chosen only if a dedicated FM workflow/patch ecosystem is a real product need beyond the 0x808 FM mode. Compare:
- direct in-process processor port using upstream DSP code;
- existing AAP port as a separate app, behind a future optional host.

The AAP port is an experiment, not an assumption of drop-in integration. If using direct code, pin source version, preserve GPL-3.0 obligations and avoid importing a second GUI/framework unless necessary.

**Odin 2** is a later broad-synth option. Its upstream is GPLv3 and its current AAP port reports remaining UI limitations. Do not add it until the basic graph is stable, the license model is settled, and a target-device benchmark demonstrates that it offers enough musical value to justify its binary size and CPU cost.

**Plaits** is only a targeted algorithm-port candidate if a concrete sound-design gap remains. It is not a complete plugin, so expect wrapper, voice management, parameter mapping and new UI/state work.

**Gate:** one advanced synth is selected based on musical need, license compliance, integration cost and target-phone test results. "More features" alone is not a sufficient reason.

### Milestone 7 — processor state, automation and project persistence

Plugin state must become part of the MPC project lifecycle, not an independent cache.

**Persist at least**
- stable plugin identifier and compatible version/schema;
- processor parameters (canonical values, not localized display text);
- preset/bank selection and safe reference to external content;
- insert order, bypass state, dry/wet and routing/send levels;
- track and master placement;
- compatibility metadata for unknown processors.

Use the existing versioned project persistence approach. Add fields through explicit schema version/migration tests; preserve the existing v1 reader guarantees and malformed-load state preservation.

**Restore behavior**
- Load and validate state off the audio callback.
- Apply parameters only after the processor is prepared.
- If a processor is missing or incompatible, preserve its serialized state and show a truthful UNAVAILABLE placeholder/bypass. Do not silently substitute a different synth or erase saved parameters.
- Reopen of the project must reproduce track order, effect order, parameter values, routes and preset selections.
- Canceled load/save actions must not mutate current audio/project state.
- Never serialize raw pointers, implementation-owned heap layouts or device-specific addresses.

**Gate:** save → close/force-stop → reopen restores instruments/effects, with exact canonical-state equality; unknown processor and bad state cases preserve the project.

### Milestone 8 — performance and stability on Redmi Note 13 Pro 5G

The phone's CPU/GPU/RAM specs do not establish audio headroom. The app must measure actual render cost.

**Diagnostic baseline**
- 8 active instrument tracks;
- 64 aggregate voices using lightweight instruments;
- 1–2 relatively inexpensive inserts per track;
- one shared reverb, one shared delay;
- master EQ + limiter;
- sample and synth MIDI patterns with representative automation.

**Stress profile**
- 12–16 tracks and up to 128 aggregate voices for lightweight instruments;
- complex instruments receive individual lower limits based on measurement;
- test different buffer sizes, including 128/192/256-frame requests when allowed by the actual stream.

These are target profiles, not guaranteed phone capacity.

**Measurements**
- actual sample rate, actual callback buffer frames, stream sharing/performance modes;
- maximum and high-percentile render duration relative to buffer deadline;
- xrun/dropout counts before and after each scenario;
- CPU estimate, native heap high-water mark, active voice count;
- thermal behavior after at least 30 minutes of repeatable load;
- repeated background/foreground, screen interaction, USB device connect/disconnect and audio stream reopen;
- audible recordings for null/clipping/NaN/Inf and discontinuity checks where feasible.

**Pass criterion:** no audible dropout or xrun in the defined baseline test, no runaway memory growth, no stuck voices, no callback blocking, no state corruption after stop/restart. Stress-profile result may be lower than target; document the measured ceiling honestly and add per-instrument polyphony guidance.

## 4. Suggested code boundaries

Names below are proposals; align to the real repository layout before implementation.

- `src/Audio/AudioGraph`: graph ownership, track routing, buses, output stages.
- `src/Audio/Processor`: minimal processor contract, parameter descriptors and state interface.
- `src/Audio/Instrument`: adapter for drum/synth/SF2 instrument engines.
- `src/Audio/Effects`: selected processor adapters.
- `src/Audio/PluginCatalog`: stable IDs, availability, metadata/license provenance; not a general dynamic loader in v1.
- `tests/audio`: deterministic buffer tests and small native offline renders.
- `docs/THIRD_PARTY_NOTICES.md` or existing root `THIRD_PARTY_NOTICES.md`: extend current file; do not duplicate notices in a second document.
- `docs/adr/ADR-XXXX-audio-graph-ownership.md`: capture the Tracktion/Oboe decision after the spike.

Avoid changing UI, MIDI transport, project persistence and engine code in one broad patch. Create a thin vertical slice that proves one instrument from MIDI input through audio output, then a second slice proving effect insertion and project save/restore.

## 5. Test matrix

| Area | Test | Required evidence |
|---|---|---|
| Build | Android native build for device + emulator ABIs | Exact commit CI job |
| DSP determinism | Offline render of fixed MIDI/events twice | Repeatable output or documented tolerance |
| Instrument semantics | Note-on velocity, note-off, program/preset, panic | Native unit tests |
| Effect behavior | bypass, wet/dry, parameter extremes, mono/stereo | Unit/offline audio tests |
| Routing | two tracks, one shared send return, master chain | Channel/path assertion + render |
| Timing | scheduled notes remain aligned with transport | Event frame/tick tests |
| Callback safety | no allocation/I/O/waiting lock in render path | Code guard/review + stress test |
| State | Save/restore parameters and effect order | Versioned persistence tests |
| Recovery | missing plugin, invalid plugin state, denied sample URI | Safe UNAVAILABLE fallback; project remains unchanged |
| Runtime | device, sustained load, background/resume, USB route change | Target phone report/recording |
| Regression | sampler crop/chop, monitor, recording, current MIDI and project acceptance | Existing native/JVM/UI suite and exact-SHA CI |

## 6. Release and acceptance sequence

Do not call the plugin subsystem ready merely because the APK builds or a sound is heard once.

1. Finish the project’s active UI/Browser/Project queue and reconcile current checkpoint documents.
2. Complete the Tracktion/Oboe graph ownership spike and write the ADR.
3. Integrate one 0x808-derived instrument through a single Oboe path.
4. Add one selected Airwindows processor and prove insert/bypass/parameter persistence.
5. Add the first shared reverb or delay return and route two tracks to it.
6. Add SoundFont support only after source ownership, performance and content-license rules are clear.
7. Select one advanced synth only if the first engine does not cover the defined musical requirement.
8. Complete state/restore/recovery and device-load tests.
9. Update `THIRD_PARTY_NOTICES.md` and the top-level license before publishing any APK that contains the new components.
10. Merge only after exact-SHA CI is green, source state is current, and evidence covers the implemented scope.

Each milestone should be one coherent branch/commit package with focused tests first, followed by the full Android workflow at the meaningful acceptance boundary. Never relax assertions or label a processor AVAILABLE when the backend cannot instantiate it.

## 7. Non-goals for the first integration wave

- writing a full synthesizer from scratch;
- supporting arbitrary VST3/CLAP binaries on Android;
- treating AAP as a stable required framework;
- migrating the whole MPC project/sequence/persistence model into 0x808;
- running two competing audio engines or mixing graphs;
- loading assets, scanning plugins or changing the UI from an audio callback;
- promising a maximum plugin count without target-device measurements;
- bundling commercial presets/sample libraries with unclear rights;
- undertaking real-time ML audio synthesis before deterministic DSP is stable.

## 8. Definition of done for the first usable plugin chain

The first milestone is complete only when the following real workflow works:

1. Open an MPC project.
2. Add/select one drum or synth instrument backed by reused open-source DSP.
3. Trigger it through the MPC sequencer and the existing MIDI event path.
4. Route it to a track mixer.
5. Add one effect, adjust a parameter, bypass and re-enable it.
6. Send a second track to one shared reverb or delay return.
7. Apply master EQ and limiter.
8. Save, close/force-stop, reopen and verify the same plugin IDs, parameters, effect order and routing.
9. Play the project on the Redmi Note 13 Pro 5G under the agreed baseline profile without audio dropouts.
10. Verify exact-SHA Android CI and retain the relevant native test/device evidence.

Only then should we add more plugin candidates.
