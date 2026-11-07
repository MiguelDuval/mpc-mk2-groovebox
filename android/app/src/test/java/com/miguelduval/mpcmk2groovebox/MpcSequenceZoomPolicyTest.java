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
    public void gridZoomInOutRestoresEverySequenceLengthClampedWindow() {
        for (int totalSteps = 1; totalSteps <= 64; totalSteps++) {
            int originalVisible = Math.min(
                    MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS,
                    totalSteps);
            int zoomedIn = Math.min(
                    MpcSequenceZoomPolicy.zoomGridStepsIn(originalVisible),
                    totalSteps);
            int zoomedOut = Math.min(
                    MpcSequenceZoomPolicy.zoomGridStepsOut(zoomedIn),
                    totalSteps);
            assertEquals(
                    "round-trip must restore sequence length " + totalSteps,
                    originalVisible,
                    zoomedOut);
        }
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
    public void arrangementZoomInThenOutRestoresResetWindowForLongSequences() {
        // Arrangement RESET shows at most eight bars. A single in/out step
        // must return to that exact visible-bar count for every longer Sequence.
        for (int totalBars = 8; totalBars <= 64; totalBars++) {
            final int resetVisibleBars = Math.min(8, totalBars);
            final int zoomedInBars = MpcSequenceZoomPolicy.zoomTimelineBarsIn(
                    resetVisibleBars, totalBars);
            assertTrue(
                    "zoom-in must narrow a " + totalBars + "-bar sequence",
                    zoomedInBars < resetVisibleBars);
            final int zoomedOutBars = MpcSequenceZoomPolicy.zoomTimelineBarsOut(
                    zoomedInBars, totalBars);
            assertEquals(
                    "reset-window round trip for totalBars=" + totalBars,
                    resetVisibleBars,
                    zoomedOutBars);
        }
    }

    @Test
    public void gridZoomHistoryRestoresExactNonReciprocalHorizontalWindow() {
        MpcGridZoomHistory history = new MpcGridZoomHistory();
        MpcGridZoomHistory.Window before = new MpcGridZoomHistory.Window(16, 32, 64);
        MpcGridZoomHistory.Window after = new MpcGridZoomHistory.Window(12, 34, 64);
        history.record(true, before, after);

        MpcGridZoomHistory.Window restored = history.restoreOpposite(false, after);
        assertTrue("opposite zoom must restore the exact prior window", restored != null);
        assertEquals(before.visibleCount, restored.visibleCount);
        assertEquals(before.start, restored.start);
        assertEquals(before.totalCount, restored.totalCount);
        assertEquals(0, history.depthForTest());
    }

    @Test
    public void consecutiveGridZoomsUnwindOneExactWindowAtATime() {
        MpcGridZoomHistory history = new MpcGridZoomHistory();
        MpcGridZoomHistory.Window first = new MpcGridZoomHistory.Window(16, 24, 64);
        MpcGridZoomHistory.Window second = new MpcGridZoomHistory.Window(12, 26, 64);
        MpcGridZoomHistory.Window third = new MpcGridZoomHistory.Window(9, 27, 64);
        history.record(true, first, second);
        history.record(true, second, third);

        MpcGridZoomHistory.Window restoredSecond = history.restoreOpposite(false, third);
        assertTrue(restoredSecond != null);
        assertEquals(second.visibleCount, restoredSecond.visibleCount);
        assertEquals(second.start, restoredSecond.start);

        MpcGridZoomHistory.Window restoredFirst = history.restoreOpposite(false, restoredSecond);
        assertTrue(restoredFirst != null);
        assertEquals(first.visibleCount, restoredFirst.visibleCount);
        assertEquals(first.start, restoredFirst.start);
        assertEquals(first.totalCount, restoredFirst.totalCount);
        assertEquals(0, history.depthForTest());
    }

    @Test
    public void externalGridViewportChangeInvalidatesInverseZoomHistory() {
        MpcGridZoomHistory history = new MpcGridZoomHistory();
        MpcGridZoomHistory.Window before = new MpcGridZoomHistory.Window(16, 24, 64);
        MpcGridZoomHistory.Window after = new MpcGridZoomHistory.Window(12, 26, 64);
        history.record(true, before, after);

        MpcGridZoomHistory.Window changedExternally =
                new MpcGridZoomHistory.Window(12, 25, 64);
        assertTrue(history.restoreOpposite(false, changedExternally) == null);
        assertEquals(0, history.depthForTest());
    }

    @Test
    public void sameDirectionGridZoomKeepsHistoryForTheNextInverseAction() {
        MpcGridZoomHistory history = new MpcGridZoomHistory();
        MpcGridZoomHistory.Window before = new MpcGridZoomHistory.Window(16, 24, 64);
        MpcGridZoomHistory.Window after = new MpcGridZoomHistory.Window(12, 26, 64);
        history.record(true, before, after);

        assertTrue(history.restoreOpposite(true, after) == null);
        assertEquals(1, history.depthForTest());
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
