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

## F. Pad trigger feedback

- [ ] With sampler audio running, strike a physical pad and confirm the matching hardware pad LED turns on with brightness reflecting strike velocity.
- [ ] Release the pad and confirm the matching hardware pad LED turns off.
- [ ] Strike pads 1 and 2 separately and confirm each LED responds only to its own pad.
- [ ] Confirm a zero-velocity Note On behaves as release and does not retrigger the sampler or leave the LED on.

These feedback checks are UNCONFIRMED until performed on the actual MPC Studio MkII.

## F. Sampler tuning

- [ ] With the bundled fallback sample loaded, select pad 1 and confirm the physical pad produces the sample at 0 st.
- [ ] Set pad 1 to +1 st, strike the physical pad, and confirm the pitch increases audibly.
- [ ] Set pad 1 to -1 st, strike the physical pad, and confirm the pitch decreases audibly.
- [ ] Reset pad 1 to 0 st and confirm the baseline pitch returns.
- [ ] Select pad 2, leave it at 0 st, and confirm changing pad 1 tuning does not change pad 2 pitch.
- [ ] Confirm the UI range limits remain −24 st to +24 st during physical use.

These tuning checks are intentionally UNCONFIRMED until performed on the actual MPC Studio MkII and the real Android audio output path.

## G. Sampler level and pan

- [ ] With pad 1 at 100% level and center pan, confirm its baseline playback level and stereo position.
- [ ] Set pad 1 level to 90%, strike it repeatedly, and confirm the playback level is reduced without changing pitch.
- [ ] Set pad 1 pan to L100, strike it with a stereo-capable sample, and confirm output is left-only.
- [ ] Set pad 1 pan to R100, strike it with a stereo-capable sample, and confirm output is right-only.
- [ ] Reset pad 1 to center pan and 100% level and confirm the baseline returns.
- [ ] Select pad 2 and confirm changing pad 1 level/pan does not change pad 2 settings.

These level/pan checks are intentionally UNCONFIRMED until performed on the actual MPC Studio MkII and the real Android audio output path.

## H. Sampler multi-layer playback

- [ ] Load a sample into pad 1 layer 1, start the sampler, and confirm it plays from the physical pad.
- [ ] Load a different sample into pad 1 layer 2 and confirm one pad strike audibly triggers both assigned layers together.
- [ ] Confirm layer 1 and layer 2 each restart from their own sample beginning on a new pad strike.
- [ ] Confirm changing the selected layer does not change the physical pad selection.
- [ ] Confirm the 8-layer selection clamps at layer 1 and layer 8.
- [ ] Confirm a pad with no explicit layer assignment still uses the bundled fallback sample.

These multi-layer checks are intentionally UNCONFIRMED until performed on the actual MPC Studio MkII and the real Android audio output path.

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
