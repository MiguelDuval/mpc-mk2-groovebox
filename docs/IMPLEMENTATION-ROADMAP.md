# Implementation Roadmap

## Stage 0 — Foundation
- Android shell.
- C++20 native core.
- Oboe linked.
- CI.
- Upstream dependency pins.
- Documentation.

## Stage 1 — Hardware bring-up
- USB MIDI discovery.
- MPC public port detection.
- MIDI IN/OUT.
- pads, velocity, aftertouch.
- buttons.
- jog.
- touch strip.

## Stage 2 — Feedback
- button LEDs.
- pad RGB SysEx.
- touch-strip LEDs.
- Note Repeat indicators.
- 160x80 LCD SysEx.

## Stage 3 — Audio
- JUCE/Tracktion Android integration.
- engine lifecycle.
- audio device management. **LANDSCAPE AUDIO SETTINGS SLICE IMPLEMENTED — explicit Android input/output device selection, AUTO or requested sample rate/buffer, shared/exclusive + low-latency/normal policy, runtime route diagnostics and a synthetic output test tone are now exposed through the Audio Settings page.**
- low-latency stream.
- one sample playback.

## Stage 4 — MPC domain
- Project.
- Sequence.
- Track.
- Drum Program.
- Pad.
- Sample.
- Layer.
- Automation.
- Q-Link.

## Stage 5 — Sampler
- record. **SOFTWARE + UI SLICE IMPLEMENTED — bounded microphone capture in RAM through a dedicated Oboe input stream; the Recorder now shows a live shared waveform, duration, peak, frame count and armed/active/stopped state. Physical recording/assignment workflow remains separately tracked.**
- monitor. **SOFTWARE SLICE IMPLEMENTED — independent bounded lock-free RAM monitor path feeding the low-latency output stream; Monitor On/Off is separate from Record. Independent monitor-only physical verification remains pending.**
- threshold. **SOFTWARE SLICE IMPLEMENTED — configurable 0–100% input threshold; with threshold Off recording starts immediately, otherwise recording arms and begins on the first input frame reaching the threshold. No pre-roll is captured. Physical verification remains pending.**
- trim. **SOFTWARE + UI SLICE IMPLEMENTED — each pad/layer has a non-destructive start/end playback region; the shared WaveformView exposes finger-draggable S/E handles, zoom and pan, with edits committed to the domain-backed region.**
- crop. **SOFTWARE SLICE IMPLEMENTED — destructive crop of the selected pad/layer region into a new PCM buffer on the control thread; the source region becomes the full region of the new buffer, while other pad/layer assignments remain untouched. A small diagnostic UI exposes Crop Region. Physical verification remains pending.**
- chop. **SOFTWARE SLICE IMPLEMENTED — deterministic 4/8/16-way equal chopping of the selected pad/layer region into pads 1-N; all chops share the original PCM buffer and receive independent non-destructive playback regions. A small diagnostic UI exposes Chop 4/8/16. Physical verification remains pending.**
- assign. **DONE for imported WAVs; physically verified on Build #135 with two different WAV samples on two different physical pads. RECORDED-AUDIO ASSIGNMENT SOFTWARE SLICE IMPLEMENTED — the last stopped microphone recording can be promoted into a selected pad/layer; physically verified on the real MPC Studio MkII.**
- multi-layer playback. **SOFTWARE SLICE IMPLEMENTED — 8 layers per pad; physical/audio verification pending.**
- MPC drum-program domain. **SOFTWARE FOUNDATION IMPLEMENTED — 16 pads × 8 layers now have a data-only domain model; sampler semantic state (sample IDs, regions, layer velocity/gain/tuning/pan, pad tuning/level/pan) is now domain-backed, with AudioEngine keeping only the realtime atomic projection and decoded buffers. A control-thread drum-program snapshot is available for the future sequencer/browser/project-state layers.**
- pitch/tuning. **SOFTWARE SLICE IMPLEMENTED — pad tuning remains the parent transpose; each sample layer now has independent ±24 st tuning, combined at trigger time in the realtime sampler path. Physical verification remains pending.**
- level. **SOFTWARE SLICE IMPLEMENTED — pad-level gain remains the parent level control; each of the eight sample layers now has an independent 0–100% gain applied at trigger time in the realtime sampler path. Physical verification remains pending.**
- pan. **SOFTWARE SLICE IMPLEMENTED — pad pan remains the parent stereo control; each sample layer now has independent L100–C–R100 pan, combined with pad pan in the realtime sampler path. Physical verification remains pending.**
- envelope/filter. **SOFTWARE SLICE IMPLEMENTED — each pad now has a deterministic one-shot ADSR amplitude envelope plus a realtime-safe one-pole low-pass cutoff. Parameters are domain-backed, atomically projected into the audio callback, covered by native contract tests, and exposed through the JNI boundary. Physical verification remains pending; dedicated editor controls remain a later UI slice.**

## Stage 6 — Sequencer
- record/overdub. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic record/overdub commit operations now accept captured MIDI note events, normalize ticks to the pattern loop, reject malformed duration/ratchet values, and provide Replace vs Overdub semantics.**
- quantize. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic nearest-grid quantization with loop wrapping and stable tie ordering.**
- swing. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic grid-aligned off-beat swing with 0–100% amount, bounded half-grid delay, stable ordering, and loop-wrap.**
- step sequencing. **SOFTWARE FOUNDATION IMPLEMENTED — grid-aligned set/replace/erase operations for MIDI notes with deterministic ordering and support for velocity, probability, ratchet and duration metadata.**
- grid editing.
- probability. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic per-note probability evaluation from a stable seed, with 0/127 fast paths.**
- ratchet. **SOFTWARE FOUNDATION IMPLEMENTED — ratchet metadata normalized to a bounded 1–8 playback count.**
- realtime event scheduler. **SOFTWARE FOUNDATION IMPLEMENTED — allocation-free pattern-window scheduling now bridges the sequencer cursor, deterministic probability and ratchet metadata into timestamped MIDI events, including loop-wrap windows and bounded output buffers. Ratchet expansion into timed retriggers remains a playback-layer concern.**
- automation.

## Stage 7 — MPC-like UI
- UI shell. **IMPLEMENTED FOUNDATION — landscape-only standalone-style shell with persistent transport/status bar, persistent mode rail, fixed main workspace, and no root diagnostic ScrollView.**
- Main. **UI FOUNDATION IMPLEMENTED — 4x4 software performance pads, selected-pad inspector, quick tone controls, layer selection and direct audition trigger.**
- Browser. **UI FOUNDATION IMPLEMENTED — dedicated Browser mode with explicit WAV load target; full indexed/searchable browser is a later slice.**
- Sampler. **UI FOUNDATION + WAVEFORM IMPLEMENTED — dedicated sample editor context with a shared editable waveform, region/edit, envelope, filter and layer tabs.**
- Sample Edit. **UI FOUNDATION + WAVEFORM IMPLEMENTED — direct S/E drag editing, zoom/pan, audition, crop and chop actions are isolated to the sample context.**
- Grid. **SHELL RESERVED — dedicated sequencer editor remains the next implementation slice.**
- Step. **SHELL RESERVED — dedicated step editor remains the next implementation slice.**
- Track Edit. **MODE SLOT RESERVED.**
- mixers. **FOUNDATION IMPLEMENTED — compact Pad Mix view with direct level control; full Track/Pad mixer remains later.**
- 16 Levels.
- Pad Perform.
- Q-Link.

## Stage 8 — Ableton Link
- tempo.
- beat phase.
- start/stop.
- quantized launch.

## Stage 9 — External I/O
- USB audio.
- generic MIDI controllers.
- routing.

## Stage 10 — MPC project interoperability
- research-backed import/export subset.
