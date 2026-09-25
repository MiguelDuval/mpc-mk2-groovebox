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
- record.
- monitor.
- threshold.
- trim.
- chop.
- assign. **DONE — physically verified on Build #135 with two different WAV samples on two different physical pads.**
- multi-layer playback.
- pitch/envelope/filter.

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
