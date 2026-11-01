package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcHardwareFeedbackPolicyTest {
    @Test
    public void ledColorFamiliesMatchMkIiProtocol() {
        assertTrue(MpcHardwareFeedbackPolicy.isTwoColorButton(66));
        assertTrue(MpcHardwareFeedbackPolicy.isTwoColorButton(11));
        assertTrue(!MpcHardwareFeedbackPolicy.isTwoColorButton(15));
        assertEquals(2, MpcHardwareFeedbackPolicy.LED_SINGLE_FULL);
    }

    @Test
    public void singleColorButtonsUseSingleColorFullState() {
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_SINGLE_FULL,
                MpcHardwareFeedbackPolicy.buttonLedOnState(82));
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_SINGLE_FULL,
                MpcHardwareFeedbackPolicy.buttonLedOnState(70));
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL,
                MpcHardwareFeedbackPolicy.buttonLedOnState(11));
    }

    @Test
    public void noteRepeatRateLabelsFollowNativeRateIndexOrder() {
        assertTrue(MpcHardwareFeedbackPolicy.contextLabel(
                "MAIN", 0, false, false, false, 0,
                true, 1, 0).contains("1/8"));
        assertTrue(MpcHardwareFeedbackPolicy.contextLabel(
                "MAIN", 0, false, false, false, 0,
                true, 5, 0).contains("1/4T"));
    }

    @Test
    public void dualColorContextUsesPrimaryAndAlternateLedStates() {
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_OFF,
                MpcHardwareFeedbackPolicy.dualColor(false, false));
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL,
                MpcHardwareFeedbackPolicy.dualColor(true, false));
        assertEquals(
                MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL,
                MpcHardwareFeedbackPolicy.dualColor(true, true));
    }

    @Test
    public void semanticModesProduceDistinctControllerContexts() {
        assertEquals(
                "GRID • event editor",
                MpcHardwareFeedbackPolicy.contextLabel(
                        "GRID", 0, false, false, false, 0,
                        false, 2, 0));
        assertEquals(
                "STEP • 16-pad step edit",
                MpcHardwareFeedbackPolicy.contextLabel(
                        "STEP", 0, false, false, false, 0,
                        false, 2, 0));
        assertEquals(
                "TRACK VIEW • sequence tracks",
                MpcHardwareFeedbackPolicy.contextLabel(
                        "TRACK_VIEW", 0, false, false, false, 0,
                        false, 2, 0));
        assertEquals(
                "BROWSER • library / sample",
                MpcHardwareFeedbackPolicy.contextLabel(
                        "BROWSER", 0, false, false, false, 0,
                        false, 2, 0));
    }

    @Test
    public void zoomFocusMapsToAxisAndContextLabel() {
        assertEquals("H", MpcHardwareFeedbackPolicy.focusAxis(11));
        assertEquals("V", MpcHardwareFeedbackPolicy.focusAxis(14));
        assertEquals("", MpcHardwareFeedbackPolicy.focusAxis(0));

        final String grid =
                MpcHardwareFeedbackPolicy.contextLabel(
                        "SEQ", 13, false, false, false, 0,
                        false, 2, 0);
        final String copy =
                MpcHardwareFeedbackPolicy.contextLabel(
                        "SEQ", 0, false, false, true, 0,
                        false, 2, 0);

        assertTrue(grid.contains("ZOOM H"));
        assertTrue(copy.contains("COPY"));
    }
}
