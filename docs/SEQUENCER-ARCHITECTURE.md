# Sequencer Architecture

This implementation follows the standalone MPC mental model without copying Akai code or artwork.

## Musical hierarchy

Project
→ Sequence
→ Track
→ Program
→ Pad / sample layer
→ events

The Sequence is the time container. It owns tempo, time signature, total length and loop range.
A Track is a content lane inside the active Sequence. The current domain supports Drum, Keygroup, Plugin, MIDI and Audio track kinds.

## Sequence settings

- BPM: 20–300.
- Length: 1–128 bars.
- Time signature: numerator 1–16; denominator 4, 8, 16 or 32.
- Loop: enabled/disabled, with an independent start/end bar.
- Quantize grid: 1/64, 1/32, 1/16, 1/8 or 1/4.
- Swing: 0–100%.

Default Sequence 01 is 4 bars at 120 BPM in 4/4, looped across the full sequence, with a 1/16 quantize grid.

## Timeline

The Android Sequence page exposes a dedicated timeline showing every bar and a red loop range.
IN/OUT handles select the loop start/end bars. The transport playhead is drawn independently of the edit range.

Changing length or time signature keeps the loop expressed in musical bars rather than blindly preserving raw tick offsets.

## Track model

Track rows are selectable independently of the Sequence container.
Drum tracks are currently connected to the existing sampler scheduler. Keygroup, Plugin, MIDI and Audio tracks are represented in the domain/UI but their dedicated playback engines and clip models are later slices.

## Realtime boundary

The UI drives semantic Sequence commands. The sequence runtime converts musical ticks to scheduled events and the existing audio trigger queue carries pad triggers into the realtime audio callback.

The realtime callback remains free of UI, file I/O and container allocation.

## Next sequencer slice

The next implementation target is real track recording/overdub: armed track → captured MIDI pad events → Pattern notes → quantize/swing → playback. After that, add dedicated audio/looper clip objects so Audio tracks can hold recorded audio on the same Sequence timeline.