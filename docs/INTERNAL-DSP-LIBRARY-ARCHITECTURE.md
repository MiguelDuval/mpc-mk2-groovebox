# Internal DSP Library Architecture

Status: CANONICAL DESIGN — pre-integration
Scope: internal instruments/effects only
External plugin hosts: out of scope for this phase
Commercial goal: proprietary/commercial Android distribution after formal license review

## 1. Goal

The application needs a native internal instrument/effect system that:
- runs inside the existing audio engine;
- is independent of VST/AAP/LV2/CLAP host APIs;
- keeps realtime processing deterministic;
- permits audited permissive DSP source;
- presents one coherent MPC-style UI;
- preserves project portability through an application-owned state format.

Third-party DSP is an implementation detail. The product-facing plugin contract belongs to this repository.

## 2. Layer boundary

UI -> semantic command -> MPC domain/audio control state -> internal plugin registry -> MpcAudioNode -> realtime audio graph -> Oboe/selected audio backend

Third-party code may exist below the MpcAudioNode adapter boundary.

Third-party code must not own MPC navigation, Android UI, hardware MIDI semantics, project schema, automation IDs, persistence schema, or realtime device lifecycle.

## 3. Node model

Minimum conceptual types:

MpcAudioNode: common realtime lifecycle: prepare, reset, process, latency, tail, parameter descriptors, state save/restore.

MpcInstrumentNode: MIDI event input, voice allocation, note lifecycle, pitch bend/modulation where supported, polyphony policy.

MpcEffectNode: audio input/output, wet/dry policy where meaningful, bypass, latency/tail reporting.

The exact C++ API is implementation work and must remain small until the first vertical slice proves it.

## 4. Parameter ownership

Every user-facing parameter has an application-owned stable identity: MpcParameterId, MpcParameterDescriptor, normalized value, display mapping, automation metadata.

Do not persist an upstream library parameter index or pointer as project state.

Adapters translate between application-owned parameters and any upstream parameter model.

## 5. State ownership

Project state belongs to the MPC domain model.

Recommended state contains: plugin type ID, schema version, engine revision metadata, application-owned parameter values, and serialized plugin state only where necessary.

Never silently bind a project to a floating upstream branch.

Every built-in plugin should have a stable application type ID, schema version, engine revision metadata, and migration hook when state changes.

## 6. Third-party adapter rules

Adapters may include audited upstream source, convert sample formats, map parameters, translate MIDI, maintain plugin-local state, precompute control data on the control thread, and expose latency/tail.

Adapters must not allocate, block, perform file/network I/O, call Android/UI APIs, mutate project/domain objects, or invoke unknown thread-unsafe upstream APIs from the realtime callback.

If an upstream implementation is not realtime-safe, it is not an acceptable realtime node without an explicit safe wrapper.

## 7. Vendoring policy

Prefer third_party/component with an exact tag or immutable commit and local license notice.

Do not use floating git branches, unpinned package versions, entire plugin repositories when only a DSP subset is needed, or foreign plugin binaries when source integration is the goal.

Prefer the smallest source subset that is required, license-audited, buildable on Android, and independently testable.

## 8. Product naming

Use application-owned names such as MPC Filter, MPC Saturator, MPC Tape, MPC Chorus, MPC Delay, MPC Reverb, MPC Poly Synth, MPC FM, MPC Physical and MPC SFZ.

Product naming does not grant rights to use upstream trademarks, presets, artwork, sounds or other content.

Credits and required license notices belong in the application attribution surface and release documentation.

## 9. Engine-selection policy

A source is selected only when it passes all three dimensions:

Legal: exact source license identified; relevant files reviewed; transitive dependencies reviewed; assets/presets reviewed; notices captured; no unapproved copyleft path compiled.

Technical: C/C++ integration practical; Android NDK build verified; realtime behavior understood; CPU/memory measured; state model understood.

Product: audible quality justifies inclusion; controls map naturally to MPC interaction; no foreign UI is required; no proprietary asset dependency exists.

## 10. Realtime acceptance

Every node must pass block-size tests at 16, 32, 64, 128, 256 and 512 frames, supported sample rates, silence and impulse tests, parameter boundaries, reset determinism, NaN/Inf checks, no allocation in process, no blocking, latency/tail reporting, and state round-trip.

Instrument nodes additionally require note on/off, velocity, pitch bend where supported, repeated-note stress, polyphony/voice stealing, and sustain where supported.

## 11. First-wave source roles

Airwindows: primary creative/character FX source.
DaisySP MIT core: primary modular synthesis and utility DSP source.
Signalsmith DSP / Basics: high-quality reusable DSP primitives and effects.
DSPFilters: filter/EQ implementation layer.
DSPark: modern C++20 DSP candidate; benchmark before adoption.
sndkit: small isolated DSP algorithms.
MSFA: FM/DX7-class instrument engine.
sfizz: SFZ engine candidate; project-level Yellow because upstream is archived.
CMSIS-DSP, KISS FFT and libsamplerate: performance/infrastructure components where measurement justifies inclusion.

## 12. Explicitly excluded from first-wave implementation

AAP host, VST host, CLAP host, LV2 host, embedded VCV Rack, embedded Cardinal, embedded Surge, embedded Vital, embedded Helm, embedded Odin2, embedded Dexed.

These remain research references, not dependencies.

## 13. Relationship to Tracktion/JUCE

The internal node layer must not force a proprietary/commercial decision about Tracktion Engine, JUCE or Ableton Link.

The current repository already gates Tracktion and Link through CMake options. Preserve that separation.

Long-term objective: keep the internal DSP layer independently testable even if the high-level sequencing/audio foundation changes.

## 14. Architecture exit criteria

Before expanding the library:
1. one effect processes a real Android audio block;
2. one poly synth renders a note;
3. both are controlled through application-owned parameters;
4. both save/restore state;
5. tests prove realtime safety;
6. source/notice inventory is complete;
7. Android performance is measured on a real target device.