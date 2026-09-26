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
- record. **SOFTWARE SLICE IMPLEMENTED — bounded microphone capture in RAM through a dedicated Oboe input stream; physical recording verification pending.**
- monitor. **SOFTWARE SLICE IMPLEMENTED — bounded lock-free RAM monitor path feeding the low-latency output stream; physical verification pending.**
- threshold.
- trim.
- chop.
- assign. **DONE for imported WAVs; physically verified on Build #135 with two different WAV samples on two different physical pads. RECORDED-AUDIO ASSIGNMENT SOFTWARE SLICE IMPLEMENTED — the last stopped microphone recording can be promoted into a selected pad/layer; physical verification pending.**
- multi-layer playback. **SOFTWARE SLICE IMPLEMENTED — 8 layers per pad; physical/audio verification pending.**
- pitch/tuning.
- level.
- pan.
- envelope/filter.

## Stage 6 — Sequencer
- record/overdub.
- quantize.
- swing.
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
