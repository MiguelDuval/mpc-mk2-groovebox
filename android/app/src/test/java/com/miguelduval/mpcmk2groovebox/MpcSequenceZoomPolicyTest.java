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
    public void everyGridZoomInRungIsExactlyReversibleForStepsAndPads() {
        int[] levels = {16, 12, 10, 8, 6, 5, 4, 3, 2, 1};
        for (int i = 0; i < levels.length; i++) {
            int current = levels[i];
            if (i == levels.length - 1) {
                assertEquals(1, MpcSequenceZoomPolicy.zoomGridStepsIn(current));
                assertEquals(1, MpcSequenceZoomPolicy.zoomGridPadsIn(current));
                continue;
            }

            int next = levels[i + 1];
            assertEquals(next, MpcSequenceZoomPolicy.zoomGridStepsIn(current));
            assertEquals(current, MpcSequenceZoomPolicy.zoomGridStepsOut(next));
            assertEquals(next, MpcSequenceZoomPolicy.zoomGridPadsIn(current));
            assertEquals(current, MpcSequenceZoomPolicy.zoomGridPadsOut(next));
        }
    }

    @Test
    public void gridZoomRemainsBoundedForNonLadderViewportSizes() {
        assertEquals(12, MpcSequenceZoomPolicy.zoomGridStepsIn(15));
        assertEquals(16, MpcSequenceZoomPolicy.zoomGridStepsOut(15));
        assertEquals(1, MpcSequenceZoomPolicy.zoomGridStepsIn(0));
        assertEquals(16, MpcSequenceZoomPolicy.zoomGridStepsOut(99));
        assertEquals(1, MpcSequenceZoomPolicy.zoomGridPadsIn(-3));
        assertEquals(16, MpcSequenceZoomPolicy.zoomGridPadsOut(32));
    }

    @Test
    public void arrangementTickMapsToTheCorrectBoundedGridStep() {
        assertEquals(0, MpcSequenceZoomPolicy.stepIndexForTick(0, 120, 64));
        assertEquals(40, MpcSequenceZoomPolicy.stepIndexForTick(4_800, 120, 64));
        assertEquals(0, MpcSequenceZoomPolicy.stepIndexForTick(-120, 120, 64));
        assertEquals(0, MpcSequenceZoomPolicy.stepIndexForTick(480, 120, 0));
        assertEquals(
                63,
                MpcSequenceZoomPolicy.stepIndexForTick(
                        Long.MAX_VALUE, 1, 64));
    }

    @Test
    public void focusedGridWindowContainsTargetAndClampsAtSequenceEnds() {
        assertEquals(0, MpcSequenceZoomPolicy.gridStartForStep(0, 16, 64));
        assertEquals(32, MpcSequenceZoomPolicy.gridStartForStep(40, 16, 64));
        assertEquals(48, MpcSequenceZoomPolicy.gridStartForStep(63, 16, 64));
        assertEquals(0, MpcSequenceZoomPolicy.gridStartForStep(6, 16, 8));
        assertEquals(40, MpcSequenceZoomPolicy.gridStartForStep(40, 1, 64));
        assertEquals(0, MpcSequenceZoomPolicy.gridStartForStep(-3, 8, 64));
        assertEquals(0, MpcSequenceZoomPolicy.gridStartForStep(12, 0, 0));
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
