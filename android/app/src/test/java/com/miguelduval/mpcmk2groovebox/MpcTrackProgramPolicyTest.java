package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcTrackProgramPolicyTest {
    private static final float EPSILON = 0.00001f;

    @Test
    public void onlyDrumTrackOwnsTheSupportedProgramSelector() {
        assertTrue(MpcTrackProgramPolicy.supportsProgramSelect("DRUM"));
        assertTrue(MpcTrackProgramPolicy.supportsProgramSelect("drum"));
        assertTrue(MpcTrackProgramPolicy.supportsProgramSelect("  Drum  "));
        for (String type : new String[] {
                "AUDIO", "KEYGROUP", "PLUGIN", "MIDI", "CV", "CLIP", "", " "
        }) {
            assertFalse("Unexpected Program Select support for " + type,
                    MpcTrackProgramPolicy.supportsProgramSelect(type));
        }
        assertFalse(MpcTrackProgramPolicy.supportsProgramSelect(null));
    }

    @Test
    public void selectorIsUnavailableUntilStartupIsComplete() {
        assertFalse(MpcTrackProgramPolicy.selectorEnabled(false, "DRUM"));
        assertTrue(MpcTrackProgramPolicy.selectorEnabled(true, "DRUM"));
        assertFalse(MpcTrackProgramPolicy.selectorEnabled(true, "AUDIO"));
        assertFalse(MpcTrackProgramPolicy.selectorEnabled(false, "AUDIO"));
    }

    @Test
    public void unsupportedTrackTypesNeverAdvertiseDrumProgramCaption() {
        assertEquals("DRUM PROGRAM", MpcTrackProgramPolicy.caption("DRUM"));
        for (String type : new String[] {
                "AUDIO", "KEYGROUP", "PLUGIN", "MIDI", "CV", null
        }) {
            assertEquals("PROGRAM", MpcTrackProgramPolicy.caption(type));
        }
    }

    @Test
    public void availableAndUnavailableSelectorsUseConsistentVisualEmphasis() {
        assertEquals(1.0f, MpcTrackProgramPolicy.selectorAlpha(true, "DRUM"), EPSILON);
        assertEquals(0.45f, MpcTrackProgramPolicy.selectorAlpha(false, "DRUM"), EPSILON);
        assertEquals(0.45f, MpcTrackProgramPolicy.selectorAlpha(true, "AUDIO"), EPSILON);
        assertEquals(0.45f, MpcTrackProgramPolicy.selectorAlpha(true, null), EPSILON);
    }
}
