# MPC Studio MkII Physical Test Checklist

Run these tests with the public MIDI port.

## A. Software preflight

- [x] Latest branch commit has a green Android Actions build.
- [x] MPC Studio MkII native decoder tests pass.
- [x] Debug APK artifact is produced.

## B. Discovery

- [x] Controller appears in Refresh MIDI Devices.
- [x] Manufacturer/product/name are visible.
- [x] The application selects the public port when available.
- [x] Connect reports an active MIDI connection.

## C. Incoming MIDI

- [x] Pad 1 sends Note On.
- [x] Pad velocity changes with strike strength.
- [x] Pad aftertouch/pressure is observed when supported.
- [x] Pad Note Off is observed.
- [x] Main button sends its documented note.
- [x] Play/Stop/Record are observed.
- [x] Jog rotation is observed on CC 100.
- [x] Jog press is observed as note 111.
- [x] Touch strip sends CC 33.

## D. Outgoing feedback

- [x] One pad can be set red.
- [x] One pad can be set blue.
- [x] Pad can be turned off.
- [x] A button LED can be set dim/full.
- [x] Touch-strip indicators respond.
- [x] Note Repeat rate indicators respond.

## E. LCD

- [x] A static 160x80 test image is rendered.
- [x] Six image chunks are transmitted.
- [x] The display reconstructs the expected frame.
- [x] Text is readable.
- [x] A second frame replaces the first.

## F. Sampler tuning

- [ ] With the bundled fallback sample loaded, select pad 1 and confirm the physical pad produces the sample at 0 st.
- [ ] Set pad 1 to +1 st, strike the physical pad, and confirm the pitch increases audibly.
- [ ] Set pad 1 to -1 st, strike the physical pad, and confirm the pitch decreases audibly.
- [ ] Reset pad 1 to 0 st and confirm the baseline pitch returns.
- [ ] Select pad 2, leave it at 0 st, and confirm changing pad 1 tuning does not change pad 2 pitch.
- [ ] Confirm the UI range limits remain −24 st to +24 st during physical use.

These tuning checks are intentionally UNCONFIRMED until performed on the actual MPC Studio MkII and the real Android audio output path.

## Evidence

For each failed test, capture:

- phone model/Android version;
- APK build/commit;
- connected port name;
- exact physical action;
- observed MIDI bytes;
- expected result;
- photo/video when useful.

Never call a protocol behavior confirmed solely from a reverse-engineering document. Confirm on the actual MkII hardware before locking it into the production adapter.


## Physical verification record

**Date:** 2026-09-24  
**Verified by:** project owner on physical MPC Studio MkII  
**APK commit:** `005c4bc2a129b1ffb3659deafad21ead23754350` (verified by owner)  
**CI:** Android Build #72 — GREEN  
**Detected MIDI identity:** `id=33 name=Akai Professional MPC Studio manufacturer=Akai Professional product=MPC Studio`

Project owner reported all verification steps in the current hardware smoke test as passing, including:

- MIDI device discovery.
- Connection to the MPC Studio MkII.
- Physical pad MIDI input.
- Button/transport MIDI input.
- Pad RGB feedback.
- Play LED feedback.
- Touch-strip LED feedback.
- Note Repeat LED feedback.
- LCD test frame rendering.

This record confirms the tested behavior on the physical controller for this build. It does not imply that every hardware protocol field in the broader reverse-engineered documentation has been physically verified.
