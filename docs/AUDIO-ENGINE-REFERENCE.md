# Audio Engine and Routing Reference

## Purpose

The audio engine is a first-class subsystem, separate from the UI and MPC hardware adapter. The current implementation uses Google Oboe for the Android low-latency stream.

The intended path is:

UI / hardware command
→ MPC domain
→ sampler / sequencer
→ audio graph and device
→ Oboe
→ Android audio subsystem

The application must remain useful without Ableton Link or an external USB interface.

## Current routing/settings slice

The Audio Settings page exposes:

- Output device: Android default route or a specific connected output device ID.
- Input device: Android default input or a specific connected input device ID.
- Sample rate: AUTO, 44.1, 48, 88.2 or 96 kHz.
- Buffer size: AUTO, 64, 96, 128, 192, 256, 384, 512 or 1024 frames.
- Sharing: SHARED or EXCLUSIVE.
- Performance: LOW LATENCY or NORMAL.
- TEST OUTPUT: a short 440 Hz stereo diagnostic tone.
- REFRESH DEVICES and APPLY & RESTART.
- Runtime diagnostics including actual Oboe API, device ID, sample rate, channel count, sharing mode, performance mode, buffer size/capacity, burst size, callback count and output peak.

AUTO leaves sample rate and buffer selection to Android/Oboe. The actual values are queried from the opened stream.

Android's AudioManager exposes connected input/output AudioDeviceInfo objects. Their IDs can be passed to Oboe, and the opened Oboe stream reports the actual device ID; a requested device is not guaranteed to be the device finally selected.

## Why SHARED is the default

The project previously requested Oboe EXCLUSIVE output unconditionally. That is useful for some low-latency cases but makes device negotiation more fragile.

The default is now SHARED + LOW LATENCY. EXCLUSIVE remains available as an explicit diagnostic/advanced option.

The goal of the first routing milestone is not to force one Android audio mode. It is to make the actual negotiated device/path visible and testable.

## Current limitations

This slice does not yet implement:

- per-track or per-pad output buses;
- stereo pair / mono output assignment;
- independent cue/headphone bus;
- master, cue and metronome buses;
- input channel routing beyond selecting an Android input endpoint;
- input gain / phantom power / hardware mixer controls;
- automatic hot-switching while preserving uninterrupted playback;
- persistent audio preferences;
- latency calibration;
- xrun-based automatic buffer tuning;
- Tracktion Engine device abstraction.

A selected Android audio endpoint is a device route, not a full DAW-style multichannel routing matrix.

## Future audio architecture

### 1. Device layer

Maintain an explicit device registry for current Android input/output endpoints. Track:

- Android device ID;
- device type;
- human-readable product name;
- channel counts;
- advertised sample rates where available;
- connection/disconnection state.

Device changes should be observable through Android's AudioDeviceCallback and should not silently replace a user-selected endpoint.

### 2. Engine layer

Keep the logical engine settings independent of Android UI:

- requested output device;
- requested input device;
- sample-rate policy;
- buffer policy;
- sharing policy;
- performance policy;
- channel layout.

The audio callback must continue to avoid blocking I/O, allocation and locks.

### 3. Routing layer

Later add semantic buses:

- Master L/R;
- Cue L/R;
- Metronome;
- optional submixes;
- optional external outputs.

Sampler pads and tracks should route to buses in the MPC domain / audio graph rather than directly to Android device IDs.

### 4. USB audio

For a USB interface, the application should first select the Android USB audio endpoint by device ID, then inspect the device's actual capabilities before requesting a rate/channel configuration.

Android API 34+ exposes mixer-attribute information for supported devices; USB devices are specifically guaranteed to expose configurable mixer attributes, but other endpoints may expose no dynamic mixer attributes.

Future USB handling should therefore be capability-driven, not based on assumptions about a particular interface.

### 5. Latency

Buffer size affects latency and glitch tolerance. Oboe allows the runtime buffer size to be adjusted and XRuns can be used as evidence when tuning the buffer.

The future UI should show:

- requested buffer;
- actual buffer;
- burst size;
- estimated output latency;
- xrun count.

The app should start conservatively, then offer explicit latency tuning rather than silently changing settings during performance.

## Diagnostic workflow

When audio is reported as silent:

1. Open Audio Settings.
2. Confirm the output device shown by Android.
3. Press TEST OUTPUT.
4. Inspect the returned route diagnostics.
5. If TEST OUTPUT is audible, the Android output path is working and the next suspect is sample loading/triggering.
6. If TEST OUTPUT is silent, investigate device selection, negotiated API/mode, Android route state or external hardware before modifying the sampler.
7. Only after the output path is proven should pad/multi-layer playback be treated as the isolated test.

## References

- Android AudioManager / AudioDeviceInfo: https://developer.android.com/reference/android/media/AudioManager
- Android AudioDeviceInfo: https://developer.android.com/reference/android/media/AudioDeviceInfo
- Google Oboe AudioStreamBuilder: https://github.com/google/oboe/blob/main/include/oboe/AudioStreamBuilder.h
- Google Oboe AudioStream: https://github.com/google/oboe/blob/main/include/oboe/AudioStream.h
- Google Oboe realtime callback guidance: https://github.com/google/oboe/blob/main/include/oboe/AudioStreamCallback.h
- Akai MPC output routing basics: https://support.akaipro.com/en/support/solutions/articles/69000868280-akai-pro-mpc-series-output-routing-basics
- Akai MPC standalone silent-output troubleshooting: https://support.inmusicstore.com/en/support/solutions/articles/69000879031-mpc-standalone-my-mpc-shows-input-signal-but-i-can-t-hear-anything
- Akai MPC software audio-device/routing settings: https://support.inmusicstore.com/en/support/solutions/articles/69000826685-akai-pro-mpc-2-0-how-do-i-manually-change-audio-routing-
