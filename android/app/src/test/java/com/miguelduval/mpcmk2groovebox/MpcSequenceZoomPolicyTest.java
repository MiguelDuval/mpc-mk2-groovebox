package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcSequenceZoomPolicyTest {
    @Test
    public void gridZoomInDecreasesVisibleWindowAndZoomOutRestoresBounds() {
        int steps = MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS;
        int pads = MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS;

        steps = MpcSequenceZoomPolicy.zoomGridStepsIn(steps);
        pads = MpcSequenceZoomPolicy.zoomGridPadsIn(pads);

        assertTrue(steps < MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS);
        assertTrue(pads < MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS);

        for (int i = 0; i < 32; i++) {
            steps = MpcSequenceZoomPolicy.zoomGridStepsIn(steps);
            pads = MpcSequenceZoomPolicy.zoomGridPadsIn(pads);
        }
        assertEquals(MpcSequenceZoomPolicy.MIN_GRID_VISIBLE_STEPS, steps);
        assertEquals(MpcSequenceZoomPolicy.MIN_GRID_VISIBLE_PADS, pads);

        for (int i = 0; i < 32; i++) {
            steps = MpcSequenceZoomPolicy.zoomGridStepsOut(steps);
            pads = MpcSequenceZoomPolicy.zoomGridPadsOut(pads);
        }
        assertEquals(MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS, steps);
        assertEquals(MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS, pads);
    }

    @Test
    public void timelineZoomRespectsSequenceLength() {
        int visible = 16;
        visible = MpcSequenceZoomPolicy.zoomTimelineBarsIn(visible, 16);
        assertTrue(visible < 16);
        assertTrue(
                MpcSequenceZoomPolicy.zoomTimelineBarsOut(visible, 16)
                        <= 16);

        assertEquals(
                1,
                MpcSequenceZoomPolicy.zoomTimelineBarsIn(1, 1));
        assertEquals(
                1,
                MpcSequenceZoomPolicy.zoomTimelineBarsIn(1, 64));
        assertEquals(
                1,
                MpcSequenceZoomPolicy.zoomTimelineBarsOut(1, 1));
    }
}
