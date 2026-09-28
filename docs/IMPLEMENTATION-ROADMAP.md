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
- audio device management.
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
- record. **SOFTWARE SLICE IMPLEMENTED — bounded microphone capture in RAM through a dedicated Oboe input stream; now independent from monitor. Physical recording/assignment workflow verified on the real MPC Studio MkII using headphones.**
- monitor. **SOFTWARE SLICE IMPLEMENTED — independent bounded lock-free RAM monitor path feeding the low-latency output stream; Monitor On/Off is separate from Record. Independent monitor-only physical verification remains pending.**
- threshold. **SOFTWARE SLICE IMPLEMENTED — configurable 0–100% input threshold; with threshold Off recording starts immediately, otherwise recording arms and begins on the first input frame reaching the threshold. No pre-roll is captured. Physical verification remains pending.**
- trim. **SOFTWARE SLICE IMPLEMENTED — each pad/layer has a non-destructive start/end playback region; sample data is unchanged, realtime playback respects the region, and a bounded diagnostic UI exposes start/end nudging plus Full Region. Full waveform editing/chop remain later slices.**
- crop. **SOFTWARE SLICE IMPLEMENTED — destructive crop of the selected pad/layer region into a new PCM buffer on the control thread; the source region becomes the full region of the new buffer, while other pad/layer assignments remain untouched. A small diagnostic UI exposes Crop Region. Physical verification remains pending.**
- chop. **SOFTWARE SLICE IMPLEMENTED — deterministic 4/8/16-way equal chopping of the selected pad/layer region into pads 1-N; all chops share the original PCM buffer and receive independent non-destructive playback regions. A small diagnostic UI exposes Chop 4/8/16. Physical verification remains pending.**
- assign. **DONE for imported WAVs; physically verified on Build #135 with two different WAV samples on two different physical pads. RECORDED-AUDIO ASSIGNMENT SOFTWARE SLICE IMPLEMENTED — the last stopped microphone recording can be promoted into a selected pad/layer; physically verified on the real MPC Studio MkII.**
- multi-layer playback. **SOFTWARE SLICE IMPLEMENTED — 8 layers per pad; physical/audio verification pending.**
- MPC drum-program domain. **SOFTWARE FOUNDATION IMPLEMENTED — 16 pads × 8 layers now have a data-only domain model; sampler semantic state (sample IDs, regions, layer velocity/gain/tuning/pan, pad tuning/level/pan) is now domain-backed, with AudioEngine keeping only the realtime atomic projection and decoded buffers. A control-thread drum-program snapshot is available for the future sequencer/browser/project-state layers.**
- pitch/tuning. **SOFTWARE SLICE IMPLEMENTED — pad tuning remains the parent transpose; each sample layer now has independent ±24 st tuning, combined at trigger time in the realtime sampler path. Physical verification remains pending.**
- level. **SOFTWARE SLICE IMPLEMENTED — pad-level gain remains the parent level control; each of the eight sample layers now has an independent 0–100% gain applied at trigger time in the realtime sampler path. Physical verification remains pending.**
- pan. **SOFTWARE SLICE IMPLEMENTED — pad pan remains the parent stereo control; each sample layer now has independent L100–C–R100 pan, combined with pad pan in the realtime sampler path. Physical verification remains pending.**
- envelope/filter.

## Stage 6 — Sequencer
- record/overdub. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic record/overdub commit operations now accept captured MIDI note events, normalize ticks to the pattern loop, reject malformed duration/ratchet values, and provide Replace vs Overdub semantics.**
- quantize.
- swing. **SOFTWARE FOUNDATION IMPLEMENTED — deterministic grid-aligned off-beat swing with 0–100% amount, bounded half-grid delay, stable ordering, and loop-wrap.**
- step sequencing.
- grid editing.
- probability.
- ratchet.
- automation.

## Stage 7 — MPC-like UI
- Main.
- Browser.
- Sampler.
- Sample Edit.
- Grid.
- Step.
- Track Edit.
- mixers.
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
