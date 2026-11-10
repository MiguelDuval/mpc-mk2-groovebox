package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MpcTrackMixerPolicyTest {
    @Test
    public void trackFaderMapsTopToTwoHundredMiddleToUnityAndBottomToZero() {
        assertEquals(2.0f,
                MpcTrackMixerPolicy.levelForTouch(8f, 8f, 92f, 2f), 0.0001f);
        assertEquals(1.0f,
                MpcTrackMixerPolicy.levelForTouch(50f, 8f, 92f, 2f), 0.0001f);
        assertEquals(0.0f,
                MpcTrackMixerPolicy.levelForTouch(92f, 8f, 92f, 2f), 0.0001f);
    }

    @Test
    public void panSliderMapsLeftCentreAndRight() {
        assertEquals(-1.0f,
                MpcTrackMixerPolicy.panForTouch(10f, 10f, 90f), 0.0001f);
        assertEquals(0.0f,
                MpcTrackMixerPolicy.panForTouch(50f, 10f, 90f), 0.0001f);
        assertEquals(1.0f,
                MpcTrackMixerPolicy.panForTouch(90f, 10f, 90f), 0.0001f);
    }

    @Test
    public void malformedTrackMixValuesFallBackToNeutralAndClamp() {
        assertEquals(1.0f, MpcTrackMixerPolicy.clampLevel(Float.NaN), 0.0001f);
        assertEquals(2.0f, MpcTrackMixerPolicy.clampLevel(9f), 0.0001f);
        assertEquals(0.0f, MpcTrackMixerPolicy.clampLevel(-1f), 0.0001f);
        assertEquals(0.0f, MpcTrackMixerPolicy.clampPan(Float.NaN), 0.0001f);
        assertEquals(-1.0f, MpcTrackMixerPolicy.clampPan(-9f), 0.0001f);
        assertEquals("C", MpcTrackMixerPolicy.panLabel(0f));
        assertEquals("L 35", MpcTrackMixerPolicy.panLabel(-0.35f));
        assertEquals("R 80", MpcTrackMixerPolicy.panLabel(0.8f));
    }
}