package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcPadPerformPolicyTest {
    @Test
    public void chromaticMapStartsAtBottomLeftAndRisesAcrossPhysicalRows() {
        assertArrayEquals(new int[]{48}, map(MpcPadPerformPolicy.Mode.CHROMATIC, 0));
        assertArrayEquals(new int[]{51}, map(MpcPadPerformPolicy.Mode.CHROMATIC, 3));
        assertArrayEquals(new int[]{60}, map(MpcPadPerformPolicy.Mode.CHROMATIC, 12));
        assertArrayEquals(new int[]{63}, map(MpcPadPerformPolicy.Mode.CHROMATIC, 15));
    }

    @Test
    public void scaleNotesUseSelectedScaleAndWrapAcrossOctaves() {
        assertArrayEquals(new int[]{48}, map(MpcPadPerformPolicy.Mode.NOTES, 0));
        assertArrayEquals(new int[]{50}, map(MpcPadPerformPolicy.Mode.NOTES, 1));
        assertArrayEquals(new int[]{59}, map(MpcPadPerformPolicy.Mode.NOTES, 6));
        assertArrayEquals(new int[]{60}, map(MpcPadPerformPolicy.Mode.NOTES, 7));
        assertArrayEquals(new int[]{51}, map(
                MpcPadPerformPolicy.Mode.NOTES, 2,
                MpcPadPerformPolicy.Scale.MINOR));
    }

    @Test
    public void diatonicChordPadsGenerateRealMajorScaleTriads() {
        assertArrayEquals(new int[]{48, 52, 55}, mapChord(0));
        assertArrayEquals(new int[]{55, 59, 62}, mapChord(4));
        assertArrayEquals(new int[]{57, 60, 64}, mapChord(5));
    }

    @Test
    public void chromaticChordPadsRespectSelectedChordQuality() {
        assertArrayEquals(new int[]{48, 52, 55}, mapChromaticChord(0,
                MpcPadPerformPolicy.ChordQuality.MAJOR));
        assertArrayEquals(new int[]{49, 52, 56}, mapChromaticChord(1,
                MpcPadPerformPolicy.ChordQuality.MINOR));
        assertArrayEquals(new int[]{48, 51, 54}, mapChromaticChord(0,
                MpcPadPerformPolicy.ChordQuality.DIMINISHED));
        assertArrayEquals(new int[]{48, 53, 55}, mapChromaticChord(0,
                MpcPadPerformPolicy.ChordQuality.SUS4));
    }

    @Test
    public void progressionPadsMapFourChordSlotsAndExposeInversionsByRow() {
        assertArrayEquals(new int[]{48, 52, 55}, mapProgression(0));
        assertArrayEquals(new int[]{55, 59, 62}, mapProgression(1));
        assertArrayEquals(new int[]{57, 60, 64}, mapProgression(2));
        assertArrayEquals(new int[]{53, 57, 60}, mapProgression(3));
        assertArrayEquals(new int[]{52, 55, 60}, mapProgression(4));
    }

    @Test
    public void rootKeyAndOctaveTransposeTheEntirePadMap() {
        assertArrayEquals(new int[]{67}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC, 0, 7, 1,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{70}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC, 3, 7, 1,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
    }

    @Test
    public void progressionVariantUsesDiatonicChordFromSelectedScale() {
        assertArrayEquals(new int[]{50, 53, 57}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, 0, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.II_V_I_VI, null));
    }

    @Test
    public void customMapUsesPerPadNotesWithoutChangingOtherSlots() {
        int[] custom = MpcPadPerformPolicy.defaultCustomNoteMap(60);
        custom[5] = 73;
        assertArrayEquals(new int[]{73}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 5, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                custom));
        assertArrayEquals(new int[]{66}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 6, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                custom));
    }

    @Test
    public void aftertouchTargetsSourcePadOnlyWhenEnabledInPadPerform() {
        assertEquals(5, MpcPadPerformPolicy.aftertouchTargetPad(
                true, true, 5, 12));
        assertEquals(-1, MpcPadPerformPolicy.aftertouchTargetPad(
                true, false, 5, 12));
        assertEquals(12, MpcPadPerformPolicy.aftertouchTargetPad(
                false, true, 5, 12));
    }

    @Test
    public void transientTuningAndMpcOctaveLabelsAreCorrect() {
        assertEquals(-12000,
                MpcPadPerformPolicy.tuningMilliSemitonesForNote(48, 60, 0.0f));
        assertEquals(0,
                MpcPadPerformPolicy.tuningMilliSemitonesForNote(60, 60, 0.0f));
        assertEquals(12000,
                MpcPadPerformPolicy.tuningMilliSemitonesForNote(48, 36, 0.0f));
        assertEquals(14500,
                MpcPadPerformPolicy.tuningMilliSemitonesForNote(48, 36, 2.5f));
        assertEquals("C0", MpcPadPerformPolicy.noteName(48));
        assertEquals("C1", MpcPadPerformPolicy.noteName(60));
        assertEquals("C#", MpcPadPerformPolicy.rootName(13));
    }

    @Test
    public void chromaticMapClampsToMidiBoundariesAtZeroAnd127() {
        assertArrayEquals(new int[]{127}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC, 15, 11, 5,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{127}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC, 8, 11, 5,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{12}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC, 0, 0, -3,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
    }

    private static int[] map(MpcPadPerformPolicy.Mode mode, int pad) {
        return MpcPadPerformPolicy.notesForPad(
                mode, pad, 0, 0, MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                null);
    }

    private static int[] map(
            MpcPadPerformPolicy.Mode mode,
            int pad,
            MpcPadPerformPolicy.Scale scale) {
        return MpcPadPerformPolicy.notesForPad(
                mode, pad, 0, 0, scale,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                null);
    }

    private static int[] mapChord(int pad) {
        return MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHORDS, pad, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                null);
    }

    private static int[] mapChromaticChord(
            int pad, MpcPadPerformPolicy.ChordQuality quality) {
        return MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC_CHORDS, pad, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR, quality,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null);
    }

    private static int[] mapProgression(int pad) {
        return MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, pad, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV,
                null);
    }

    @Test
    public void customNoteMapClampsEachSlotToMidiRangeAndFallsBackWhenMalformed() {
        int[] custom = MpcPadPerformPolicy.defaultCustomNoteMap(48);
        custom[0] = 0;
        custom[1] = 127;
        custom[2] = 200;
        custom[15] = -5;
        assertArrayEquals(new int[]{0}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 0, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, custom));
        assertArrayEquals(new int[]{127}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 1, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, custom));
        assertArrayEquals(new int[]{127}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 2, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, custom));
        assertArrayEquals(new int[]{0}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 15, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, custom));
        // A null or wrong-length map falls back to chromatic-from-base.
        assertArrayEquals(new int[]{48}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 0, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        int[] shortMap = new int[]{60, 62};
        assertArrayEquals(new int[]{50}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CUSTOM, 2, 0, 0,
                MpcPadPerformPolicy.Scale.MINOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, shortMap));
    }

    @Test
    public void chordAndProgressionVoicingsStayInsideMidiRange() {
        // Chromatic Chords at the top of the range: root clamps to 127 and so
        // does every tone of the voicing.
        assertArrayEquals(new int[]{127, 127, 127}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC_CHORDS, 15, 11, 5,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{119, 123, 126}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.CHROMATIC_CHORDS, 0, 11, 5,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        // Progressions cycle four chord slots; high pads apply inversions.
        assertArrayEquals(new int[]{48, 52, 55}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, 12, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{53, 57, 60}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, 15, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
        assertArrayEquals(new int[]{60, 65, 69}, MpcPadPerformPolicy.notesForPad(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, 11, 0, 0,
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR,
                MpcPadPerformPolicy.Progression.I_V_VI_IV, null));
    }

    @Test
    public void everyModeKeepsAllSixteenPadsInsideMidiRange() {
        MpcPadPerformPolicy.Mode[] modes = MpcPadPerformPolicy.Mode.values();
        assertEquals(6, modes.length);
        int[][] extremes = new int[][]{
                {0, 0, 0},
                {11, 5, 15},
                {0, -3, 0}
        };
        for (MpcPadPerformPolicy.Mode mode : modes) {
            for (int[] extreme : extremes) {
                int[] notes = MpcPadPerformPolicy.notesForPad(
                        mode, extreme[2], extreme[0], extreme[1],
                        MpcPadPerformPolicy.Scale.MAJOR,
                        MpcPadPerformPolicy.ChordQuality.MAJOR,
                        MpcPadPerformPolicy.Progression.I_V_VI_IV, null);
                assertTrue("mode=" + mode + " pad=" + extreme[2],
                        notes.length > 0);
                for (int note : notes) {
                    assertTrue("mode=" + mode + " pad=" + extreme[2] + " note=" + note,
                            note >= 0 && note <= 127);
                }
            }
        }
    }

    @Test
    public void aftertouchTargetPadFollowsPadPerformAndAftertouchToggle() {
        // Pressure released or aftertouch disabled: no pad owns aftertouch.
        assertEquals(-1, MpcPadPerformPolicy.aftertouchTargetPad(true, false, 5, 12));
        // Outside Pad Perform the physical pad is addressed regardless of the
        // Pad Perform aftertouch toggle.
        assertEquals(12, MpcPadPerformPolicy.aftertouchTargetPad(false, false, 5, 12));
        // Enabled in Pad Perform: aftertouch targets the source Drum Pad, not
        // the physical pad that moved.
        assertEquals(5, MpcPadPerformPolicy.aftertouchTargetPad(true, true, 5, 12));
        // Enabled outside Pad Perform: aftertouch targets the physical pad.
        assertEquals(12, MpcPadPerformPolicy.aftertouchTargetPad(false, true, 5, 12));
        // Pad indexes are clamped to the 16-pad map.
        assertEquals(15, MpcPadPerformPolicy.aftertouchTargetPad(true, true, 20, 5));
        assertEquals(0, MpcPadPerformPolicy.aftertouchTargetPad(true, true, -5, 5));
    }

    @Test
    public void padLabelsShowModeRootAndAccessibilityFallbacks() {
        assertEquals("C0", MpcPadPerformPolicy.padLabel(
                MpcPadPerformPolicy.Mode.CHROMATIC, new int[]{48},
                MpcPadPerformPolicy.Scale.MAJOR, null));
        assertEquals("C0\nMAJ", MpcPadPerformPolicy.padLabel(
                MpcPadPerformPolicy.Mode.CHORDS, new int[]{48, 52, 55},
                MpcPadPerformPolicy.Scale.MAJOR, null));
        assertEquals("C0\nMAJ", MpcPadPerformPolicy.padLabel(
                MpcPadPerformPolicy.Mode.CHROMATIC_CHORDS, new int[]{48, 52, 55},
                MpcPadPerformPolicy.Scale.MAJOR,
                MpcPadPerformPolicy.ChordQuality.MAJOR));
        assertEquals("C0\nCHORD", MpcPadPerformPolicy.padLabel(
                MpcPadPerformPolicy.Mode.PROGRESSIONS, new int[]{48, 52, 55},
                MpcPadPerformPolicy.Scale.MAJOR, null));
        assertEquals("C1", MpcPadPerformPolicy.padLabel(
                MpcPadPerformPolicy.Mode.NOTES, new int[]{60},
                MpcPadPerformPolicy.Scale.MINOR, null));
        assertEquals("—", MpcPadPerformPolicy.padLabel(null, new int[0], null, null));
    }
}