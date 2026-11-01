# MPC Studio MkII pad numbering

The groovebox uses the physical MPC Studio MkII pad number as its canonical
zero-based internal pad index (`0 = Pad 1`, …, `15 = Pad 16`).

Physical layout when the controller is oriented normally:

```text
13  14  15  16
 9  10  11  12
 5   6   7   8
 1   2   3   4
```

Therefore the Android virtual pad grid is rendered with Pads 1–4 on the
bottom row and Pads 13–16 on the top row.

The hardware MIDI note numbers are a separate mapping and remain unchanged.
The MIDI decoder converts each controller note to this canonical physical
pad index before triggering the sampler.
