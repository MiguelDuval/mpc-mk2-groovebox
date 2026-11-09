package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        assertEquals(-12000, MpcPadPerformPolicy.tuningMilliSemitonesForNote(48));
        assertEquals(0, MpcPadPerformPolicy.tuningMilliSemitonesForNote(60));
        assertEquals("C0", MpcPadPerformPolicy.noteName(48));
        assertEquals("C1", MpcPadPerformPolicy.noteName(60));
        assertEquals("C#", MpcPadPerformPolicy.rootName(13));
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
}