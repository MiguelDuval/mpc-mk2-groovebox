# MPC Studio MkII — Current-HEAD Physical Acceptance Protocol

**Purpose:** verify the Android MPC Groovebox against a real Akai MPC Studio MkII. This protocol is scoped to the current product build and the controller's physical USB-MIDI / LED / LCD behavior. It does not copy MPC Desktop workflows.

**Status:** NOT RUN on the current candidate. A historical owner-reported hardware check exists in [PHYSICAL-TEST-CHECKLIST.md](PHYSICAL-TEST-CHECKLIST.md), but it was performed on APK commit `005c4bc2a129b1ffb3659deafad21ead23754350` on 2026-09-24. It does not certify later code.

**Candidate build at protocol authoring:** `240577fe4dc17d28ab3dcce3a2a57de728c9e389`, Android Build #3600 / run `41217247316` (pending when this protocol was written). Perform physical testing only after the exact SHA's Android Build, native/JUnit tests and UI smoke are green. Record the final tested APK SHA separately.

## 1. Test fixture and evidence record

Required equipment:

- Akai MPC Studio MkII controller.
- Android phone running the tested debug APK.
- USB-C OTG adapter/cable; connect the controller directly for the first pass, without an unpowered hub.
- Headphones connected to the phone or the app's configured audio output. The MkII is a USB-MIDI controller, not a general audio interface.
- A way to preserve Android app logs and, where available, raw MIDI input/output events. Do not route test notes to a loudspeaker where microphone monitoring can create feedback.

Before testing, record:

| Field | Value |
|---|---|
| Test date / tester | NOT RUN |
| Repository branch / exact HEAD SHA | `checkpoint/mpc-ui-reference-and-product-20261030` / `240577fe4dc17d28ab3dcce3a2a57de728c9e389` (candidate; re-check live) |
| Android Build run / result | #3600 / pending at authoring |
| Installed APK SHA-256 | NOT RECORDED |
| Phone model / Android version | NOT RECORDED |
| OTG adapter / cable | NOT RECORDED |
| Detected USB MIDI device identity | NOT RECORDED |
| Input/output port names | NOT RECORDED |

For every failing or ambiguous item capture the physical gesture, the raw MIDI bytes/status/channel/value, the app's displayed state, the expected result, and a photo/video when it materially helps. A unit test or online protocol chart is not a physical pass.

## 2. Fresh connection and lifecycle

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-001 | Connect MkII before launching the app; open MIDI device selection / refresh. | Android enumerates the controller and both usable MIDI directions. | NOT RUN |
| HW-002 | Inspect device name, manufacturer and product identity. | Controller is identified as Akai Professional MPC Studio / MkII where Android exposes the MkII product string. Record actual values; do not hard-code a guessed USB ID. | NOT RUN |
| HW-003 | Select the controller's Public MIDI port when present. | App reports a real active connection only after input and output ports open successfully. | NOT RUN |
| HW-004 | Disconnect USB while the app is open. | MIDI readiness drops; the UI does not continue reporting a live connection. | NOT RUN |
| HW-005 | Reconnect the controller and refresh/reopen ports. | Connection recovers without app restart, duplicate readers or stuck button/modifier states. | NOT RUN |
| HW-006 | Force-stop and relaunch the app with controller attached. | Port discovery/connection recovers; no stale latched Shift/Mode/Note Repeat state survives incorrectly. | NOT RUN |

## 3. Incoming pad and button events

The table below reflects the current project mapping documented in [MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md](MPC-STUDIO-MKII-REVERSE-ENGINEERING-TRACEABILITY.md). Values are candidates until observed on this physical unit.

### 3.1 Physical pads

Pad channel is documented as MIDI Channel 10 (zero-based channel nibble 9). Pads use a non-sequential Note map:

| Physical pad | Expected MIDI Note |
|---:|---:|
| 1 | 37 |
| 2 | 36 |
| 3 | 42 |
| 4 | 82 |
| 5 | 40 |
| 6 | 38 |
| 7 | 46 |
| 8 | 44 |
| 9 | 48 |
| 10 | 47 |
| 11 | 45 |
| 12 | 43 |
| 13 | 49 |
| 14 | 55 |
| 15 | 51 |
| 16 | 53 |

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-010 | Tap each of the 16 pads once, recording raw Note On and release messages. | Each pad maps to the corresponding row above; no adjacent/other pad is selected. | NOT RUN |
| HW-011 | Strike Pad 1 softly, medium, then hard. | Note On velocity changes monotonically with strike strength and remains in 1–127. | NOT RUN |
| HW-012 | Hold Pad 1, then release it. Also test Note On velocity 0 if the controller emits it. | One note-on and one release are observed; no stuck note or retrigger on release. | NOT RUN |
| HW-013 | Apply pad pressure after the initial strike where supported. | Polyphonic aftertouch is associated with the same physical pad; record actual status byte, channel, note and pressure values. | NOT RUN |
| HW-014 | Play a simple known kit from the tested app. | Only the struck pad's assigned sample triggers; velocity affects the expected response; audio comes from the phone/app audio path. | NOT RUN |

### 3.2 Buttons, modifiers and transport

Button inputs are documented on MIDI Channel 1 (zero-based channel nibble 0). The project control map is the source of truth for the expected note number for each named button.

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-020 | Press and release Main, Browse, Menu, Step Sequencer and Track View buttons. | Each raw input maps to its own supported in-app semantic action; no desktop-only page is opened or implied. | NOT RUN |
| HW-021 | Press Play, Stop and Record separately, then test Play Start. | App transport/recorder state changes once per gesture and remains synchronized with the visible transport. | NOT RUN |
| HW-022 | Hold Shift and press/release one documented secondary-function button. | Modifier is applied only for the intended chord and clears on release. | NOT RUN |
| HW-023 | Test Mode+Pad on at least two pads. | Only app-supported shortcuts open; unsupported destinations remain explicitly unavailable. | NOT RUN |
| HW-024 | Test Pad Bank A–D and the documented alternate bank gesture. | Bank selection changes the pad identity/context without producing phantom triggers. | NOT RUN |
| HW-025 | Test Note Repeat as a hold gesture, then its Shift/latch variant if implemented. | Repeats use the app's native sequencer/timing policy; release or unlatch returns to the expected state. | NOT RUN |
| HW-026 | Hold Erase during playback and touch a pad/event target. | Only the supported target is erased; releasing Erase restores normal pad behavior. | NOT RUN |

**Unresolved mapping discrepancy:** the traceability document records a source conflict for the Mode button: the main protocol chart says Note 114 while a Go POC annotation says Note 1. Capture the real Mode press/release bytes and resolve this explicitly. Do not mark either value physically confirmed in advance.

## 4. Jog Wheel and Touch Strip

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-030 | Rotate Jog Wheel clockwise and counter-clockwise slowly and quickly. | Raw CC 100 direction/value changes are captured. Data Dial moves the focused field in the correct direction without changing unrelated state. | NOT RUN |
| HW-031 | Press and release the Jog Wheel. | Documented Note 111 press/release is captured; the app commits/enters only in the supported focus context. | NOT RUN |
| HW-032 | Touch and release the Touch Strip without sliding. | Documented Note 78 touch/release is captured where emitted. | NOT RUN |
| HW-033 | Slide Touch Strip from minimum to maximum and back. | CC 33 range and direction are captured; values map smoothly to the active contextual control. | NOT RUN |
| HW-034 | Repeat Jog and Touch Strip while a different field/context is focused. | The same physical input acts on the newly focused context; it does not change the wrong field. | NOT RUN |

Record the raw status byte for CC 100 and CC 33 to resolve the documented uncertainty about whether these CCs should be strictly channel-filtered.

## 5. Outgoing button LEDs and pad RGB

### 5.1 Button LEDs

The public protocol chart says button LED CC equals the corresponding button Note number (Jog Wheel is an exception). Test representative single-colour and two-colour LEDs:

| ID | Output sent by the app | Expected physical response | Status |
|---|---|---|---|
| HW-040 | Set a single-colour button LED to 0, 1 and 2. | Off, dim and full states are visually distinct. | NOT RUN |
| HW-041 | Set a two-colour button LED to 0, 1, 2, 3 and 4. | Off, colour 1 dim, colour 2 dim, colour 1 full, colour 2 full. | NOT RUN |
| HW-042 | Navigate, stop playback, cancel an action, then reconnect. | LEDs reflect current semantic app state rather than echoing the last physical press. | NOT RUN |

### 5.2 RGB pads

Current implementation packet documented by the native protocol builder:

`F0 47 47 4A 65 00 04 <pad-index> <R> <G> <B> F7`

Pad index is zero-based; Pad 1 = 0 and Pad 16 = 15. RGB data bytes must remain seven-bit safe.

| ID | Physical check | Expected result | Status |
|---|---|---|---|
| HW-043 | Send red, blue and off to Pad 1. | Only physical Pad 1 changes to each requested state and then turns off. | NOT RUN |
| HW-044 | Send a different colour to Pad 16. | Only Pad 16 changes; no wrap/indexing error. | NOT RUN |
| HW-045 | Trigger pads while the app is in Grid and Mute contexts. | Pad-trigger indication and explicit RGB state do not fight or leave stale colours after note-off. | NOT RUN |

## 6. Touch Strip and Note Repeat indicator LEDs

| ID | Output range | Physical check | Status |
|---|---|---|---|
| HW-050 | Touch Strip LED CC 57–65 | Walk all nine segments from off (0) to maximum brightness (127); confirm each physical segment responds in the expected order. | NOT RUN |
| HW-051 | Note Repeat indicator CC 103–110 | Walk all eight divisions from off to maximum brightness; confirm selected rate is represented correctly. | NOT RUN |
| HW-052 | Repeated identical frame/state | Confirm redundant updates do not make LEDs flicker or desynchronize. | NOT RUN |

## 7. LCD display transport

The LCD is documented as 160 × 80 pixels and six image regions:

| Chunk | X | Y | Width | Height |
|---:|---:|---:|---:|---:|
| 1 | 0 | 0 | 60 | 60 |
| 2 | 0 | 60 | 60 | 20 |
| 3 | 60 | 0 | 60 | 60 |
| 4 | 60 | 60 | 60 | 20 |
| 5 | 120 | 0 | 40 | 60 |
| 6 | 120 | 60 | 40 | 20 |

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-060 | Send a test frame with large labels in all four corners and contrasting vertical/horizontal boundaries. | Entire 160×80 frame appears; no swapped, missing or misaligned chunks. | NOT RUN |
| HW-061 | Replace it with a visibly different frame, not just a changed MIDI state. | All six chunks update; old pixels are not left behind. | NOT RUN |
| HW-062 | Re-send an identical frame. | No corruption/flicker; redundant-frame suppression is safe. | NOT RUN |
| HW-063 | Disconnect/reconnect while the LCD is active. | A full current-state frame is restored after reconnect. | NOT RUN |
| HW-064 | Run for ten minutes while changing sequence/track/tempo/pad and transport. | Display remains legible and tracks the current app state without partial updates or corrupted frames. | NOT RUN |

## 8. End-to-end musical workflow

| ID | Physical action | Expected result | Status |
|---|---|---|---|
| HW-070 | Record a four-bar pattern with physical pads while using Play/Stop/Record. | Notes land on the intended pad/track, correct velocity, and remain quantized according to the chosen policy. | NOT RUN |
| HW-071 | Hold Note Repeat on a pad, then release. | Ratchets/repeats use native timing and do not continue after release unless intentionally latched. | NOT RUN |
| HW-072 | Toggle Track Mute and Solo from the Android UI, then play using physical pads. | Playback semantics change for the selected track while physical pad input still routes to the correct track. | NOT RUN |
| HW-073 | Use Jog/Touch Strip to edit tempo or another focused parameter. | UI, native state and MIDI hardware response remain consistent; Undo/Redo preserves the musical state where supported. | NOT RUN |
| HW-074 | Save, stop, close and reload the project. | Notes, track/program mapping and sample assignments persist; no duplicate or phantom tracks. | NOT RUN |

## 9. Pass criteria and sign-off

A test passes only when the exact physical action was performed on the real MkII and the observed response matches the expectation. Automated tests and screenshots can establish software contracts, but cannot mark any physical item above PASS.

Do not convert NOT RUN to PASS from a protocol chart, a successful CI build, a desktop tutorial, a stale physical report or an emulator result.

At sign-off attach:

- exact candidate branch/head SHA and Android Actions run ID;
- APK SHA-256 installed on the phone;
- phone model, Android version, OTG adapter/cable and actual MIDI port names;
- raw MIDI byte captures for the disputed or unexpected events;
- screenshots/photos/video for LEDs and LCD where useful;
- a short result for each `HW-xxx` item, including failures and deviations.

**Physical acceptance status:** NOT RUN. This document is a ready-to-use test protocol, not a claim that the hardware has been tested.
