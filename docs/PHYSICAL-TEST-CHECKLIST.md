# MPC Studio MkII Physical Test Checklist

Run these tests with the public MIDI port.

## A. Software preflight

- [ ] Latest branch commit has a green Android Actions build.
- [ ] MPC Studio MkII native decoder tests pass.
- [ ] Debug APK artifact is produced.

## B. Discovery

- [ ] Controller appears in Refresh MIDI Devices.
- [ ] Manufacturer/product/name are visible.
- [ ] The application selects the public port when available.
- [ ] Connect reports an active MIDI connection.

## C. Incoming MIDI

- [ ] Pad 1 sends Note On.
- [ ] Pad velocity changes with strike strength.
- [ ] Pad aftertouch/pressure is observed when supported.
- [ ] Pad Note Off is observed.
- [ ] Main button sends its documented note.
- [ ] Play/Stop/Record are observed.
- [ ] Jog rotation is observed on CC 100.
- [ ] Jog press is observed as note 111.
- [ ] Touch strip sends CC 33.

## D. Outgoing feedback

- [ ] One pad can be set red.
- [ ] One pad can be set blue.
- [ ] Pad can be turned off.
- [ ] A button LED can be set dim/full.
- [ ] Touch-strip indicators respond.
- [ ] Note Repeat rate indicators respond.

## E. LCD

- [ ] A static 160x80 test image is rendered.
- [ ] Six image chunks are transmitted.
- [ ] The display reconstructs the expected frame.
- [ ] Text is readable.
- [ ] A second frame replaces the first.

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
**APK commit:** `005c4bc2a129b1ffb3659deafad21ead23754350`  
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
