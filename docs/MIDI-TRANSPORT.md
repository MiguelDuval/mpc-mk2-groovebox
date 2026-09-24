# MIDI Transport

Android provides the MIDI device service through MidiManager, MidiDevice,
MidiInputPort and MidiOutputPort.

The current bridge:
1. Enumerates available MIDI devices.
2. Prefers a device identified as MPC Studio MkII.
3. Prefers a port whose name contains "public".
4. Opens one input and one output port.
5. Receives MIDI through MidiReceiver.
6. Forwards messages into the native C++ MIDI boundary.
7. Provides an outgoing send path for hardware feedback.

The Android layer owns platform MIDI lifecycle. The native layer receives
semantic byte streams and will later decode them into MPC Studio actions.

References:
https://developer.android.com/reference/android/media/midi/package-summary
https://developer.android.com/ndk/guides/audio/midi
