package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcHardwareFeedbackPolicyTest {
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
