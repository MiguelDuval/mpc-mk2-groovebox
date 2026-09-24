# MPC Studio MkII Physical Test Checklist

Run these tests with the public MIDI port.

## A. Discovery

- [ ] Controller appears in Refresh MIDI Devices.
- [ ] Manufacturer/product/name are visible.
- [ ] The application selects the public port when available.
- [ ] Connect reports an active MIDI connection.

## B. Incoming MIDI

- [ ] Pad 1 sends Note On.
- [ ] Pad velocity changes with strike strength.
- [ ] Pad aftertouch/pressure is observed when supported.
- [ ] Pad Note Off is observed.
- [ ] Main button sends its documented note.
- [ ] Play/Stop/Record are observed.
- [ ] Jog rotation is observed on CC 100.
- [ ] Jog press is observed as note 111.
- [ ] Touch strip sends CC 33.

## C. Outgoing feedback

- [ ] One pad can be set red.
- [ ] One pad can be set blue.
- [ ] Pad can be turned off.
- [ ] A button LED can be set dim/full.
- [ ] Touch-strip indicators respond.
- [ ] Note Repeat rate indicators respond.

## D. LCD

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
