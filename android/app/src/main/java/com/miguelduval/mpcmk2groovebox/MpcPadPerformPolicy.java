package com.miguelduval.mpcmk2groovebox;

import java.util.Arrays;

/**
 * Pure musical mapping policy for MPC Pad Perform.
 *
 * Physical pad zero is the bottom-left pad. Notes are expressed as MIDI note
 * numbers; MPC-style octave labels use C0 for MIDI note 48 and C1 for note 60.
 * Pitch is always an ephemeral trigger offset from the source Drum Pad.
 */
final class MpcPadPerformPolicy {
    enum Mode {
        CHROMATIC("CHROMATIC"),
        NOTES("NOTES"),
        CHORDS("CHORDS"),
        CHROMATIC_CHORDS("CHROMATIC CHORDS"),
        PROGRESSIONS("PROGRESSIONS"),
        CUSTOM("CUSTOM");

        private final String label;
        Mode(String label) { this.label = label; }
        String label() { return label; }
    }

    enum Scale {
        MAJOR("MAJOR", new int[]{0, 2, 4, 5, 7, 9, 11}),
        MINOR("MINOR", new int[]{0, 2, 3, 5, 7, 8, 10}),
        DORIAN("DORIAN", new int[]{0, 2, 3, 5, 7, 9, 10}),
        PENTATONIC("PENTATONIC", new int[]{0, 2, 4, 7, 9});

        private final String label;
        private final int[] intervals;
        Scale(String label, int[] intervals) {
            this.label = label;
            this.intervals = intervals;
        }
        String label() { return label; }
        int[] intervals() { return Arrays.copyOf(intervals, intervals.length); }
    }

    enum ChordQuality {
        MAJOR("MAJ", new int[]{0, 4, 7}),
        MINOR("MIN", new int[]{0, 3, 7}),
        DIMINISHED("DIM", new int[]{0, 3, 6}),
        SUS4("SUS4", new int[]{0, 5, 7});

        private final String label;
        private final int[] intervals;
        ChordQuality(String label, int[] intervals) {
            this.label = label;
            this.intervals = intervals;
        }
        String label() { return label; }
        int[] intervals() { return Arrays.copyOf(intervals, intervals.length); }
    }

    enum Progression {
        I_V_VI_IV("I–V–vi–IV", new int[]{0, 4, 5, 3}),
        VI_IV_I_V("vi–IV–I–V", new int[]{5, 3, 0, 4}),
        I_VI_IV_V("I–vi–IV–V", new int[]{0, 5, 3, 4}),
        II_V_I_VI("ii–V–I–vi", new int[]{1, 4, 0, 5});

        private final String label;
        private final int[] degrees;
        Progression(String label, int[] degrees) {
            this.label = label;
            this.degrees = degrees;
        }
        String label() { return label; }
        int[] degrees() { return Arrays.copyOf(degrees, degrees.length); }
    }

    private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "D#", "E", "F",
            "F#", "G", "G#", "A", "A#", "B"
    };

    private MpcPadPerformPolicy() {}

    static int[] defaultCustomNoteMap(int baseNote) {
        final int[] result = new int[16];
        for (int pad = 0; pad < result.length; pad++) {
            result[pad] = clampMidi(baseNote + pad);
        }
        return result;
    }

    static int[] notesForPad(
            Mode mode,
            int physicalPad,
            int rootPitchClass,
            int octave,
            Scale scale,
            ChordQuality chordQuality,
            Progression progression,
            int[] customNoteMap) {
        final int pad = clamp(physicalPad, 0, 15);
        final int base = clampMidi(48 + (clamp(octave, -3, 5) * 12)
                + clamp(rootPitchClass, 0, 11));
        final Scale safeScale = scale == null ? Scale.MAJOR : scale;
        final ChordQuality safeQuality =
                chordQuality == null ? ChordQuality.MAJOR : chordQuality;
        final Progression safeProgression =
                progression == null ? Progression.I_V_VI_IV : progression;

        switch (mode == null ? Mode.CHROMATIC : mode) {
            case NOTES:
                return new int[]{clampMidi(base
                        + scaleNoteAtDegree(safeScale, pad))};
            case CHORDS: {
                final int scaleLength = safeScale.intervals.length;
                final int degree = pad % scaleLength;
                final int octaveShift = pad / scaleLength;
                return scaleTriad(base + (octaveShift * 12), safeScale, degree);
            }
            case CHROMATIC_CHORDS: {
                final int root = clampMidi(base + pad);
                return chord(root, safeQuality.intervals);
            }
            case PROGRESSIONS: {
                final int degree = safeProgression.degrees[pad % 4];
                final int root = clampMidi(base
                        + scaleNoteAtDegree(safeScale, degree));
                final int[] tones = scaleTriad(root, safeScale, degree);
                return invert(tones, pad / 4);
            }
            case CUSTOM:
                if (customNoteMap != null && customNoteMap.length == 16) {
                    return new int[]{clampMidi(customNoteMap[pad])};
                }
                return new int[]{clampMidi(base + pad)};
            case CHROMATIC:
            default:
                return new int[]{clampMidi(base + pad)};
        }
    }

    private static int[] scaleTriad(int base, Scale scale, int degree) {
        return new int[]{
                clampMidi(base + scaleNoteAtDegree(scale, degree)),
                clampMidi(base + scaleNoteAtDegree(scale, degree + 2)),
                clampMidi(base + scaleNoteAtDegree(scale, degree + 4))
        };
    }

    private static int scaleNoteAtDegree(Scale scale, int degree) {
        final int count = scale.intervals.length;
        final int octave = Math.floorDiv(degree, count);
        final int index = Math.floorMod(degree, count);
        return octave * 12 + scale.intervals[index];
    }

    private static int[] chord(int root, int[] intervals) {
        final int[] notes = new int[intervals.length];
        for (int i = 0; i < intervals.length; i++) {
            notes[i] = clampMidi(root + intervals[i]);
        }
        return notes;
    }

    private static int[] invert(int[] notes, int inversion) {
        final int[] result = Arrays.copyOf(notes, notes.length);
        final int count = Math.max(0, Math.min(notes.length, inversion % notes.length));
        for (int i = 0; i < count; i++) {
            result[i] += 12;
        }
        Arrays.sort(result);
        for (int i = 0; i < result.length; i++) {
            result[i] = clampMidi(result[i]);
        }
        return result;
    }

    static int tuningMilliSemitonesForNote(int midiNote) {
        return (clampMidi(midiNote) - 60) * 1000;
    }

    static String rootName(int pitchClass) {
        return NOTE_NAMES[Math.floorMod(pitchClass, NOTE_NAMES.length)];
    }

    static String noteName(int midiNote) {
        final int note = clampMidi(midiNote);
        return NOTE_NAMES[note % 12] + (Math.floorDiv(note, 12) - 4);
    }

    static String padLabel(
            Mode mode,
            int[] notes,
            Scale scale,
            ChordQuality quality) {
        if (notes == null || notes.length == 0) return "—";
        final String root = noteName(notes[0]);
        switch (mode == null ? Mode.CHROMATIC : mode) {
            case CHORDS:
                return root + "\n" + scaleTriadQuality(notes, scale);
            case CHROMATIC_CHORDS:
                return root + "\n" + (quality == null ? ChordQuality.MAJOR.label : quality.label);
            case PROGRESSIONS:
                return root + "\nCHORD";
            default:
                return root;
        }
    }

    private static String scaleTriadQuality(int[] notes, Scale scale) {
        if (notes.length < 3) return "NOTE";
        final int third = Math.floorMod(notes[1] - notes[0], 12);
        final int fifth = Math.floorMod(notes[2] - notes[0], 12);
        if (third == 3 && fifth == 6) return "DIM";
        if (third == 3 && fifth == 7) return "MIN";
        if (third == 4 && fifth == 7) return "MAJ";
        if (third == 4 && fifth == 8) return "AUG";
        return scale == null ? "TRIAD" : "SCALE";
    }

    private static int clampMidi(int note) {
        return clamp(note, 0, 127);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}