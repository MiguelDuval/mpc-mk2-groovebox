package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MpcSequenceStepPagePolicyTest {
    @Test
    public void movingForwardKeepsTheSelectedColumnOnTheNextPage() {
        assertEquals(
                20,
                MpcSequenceStepPagePolicy.selectedStepForPageChange(
                        4, 0, 1, 64, 16));
    }

    @Test
    public void movingBackwardKeepsTheSelectedColumnOnThePreviousPage() {
        assertEquals(
                4,
                MpcSequenceStepPagePolicy.selectedStepForPageChange(
                        20, 1, 0, 64, 16));
    }

    @Test
    public void movingPagesWithoutASelectionFocusesTheFirstVisibleStep() {
        assertEquals(
                32,
                MpcSequenceStepPagePolicy.selectedStepForPageChange(
                        -1, 0, 2, 64, 16));
    }

    @Test
    public void relativeColumnIsClampedOnThePartialFinalPage() {
        assertEquals(
                49,
                MpcSequenceStepPagePolicy.selectedStepForPageChange(
                        47, 2, 3, 50, 16));
    }

    @Test
    public void unchangedPageDoesNotMoveTheCurrentSelection() {
        assertEquals(
                21,
                MpcSequenceStepPagePolicy.selectedStepForPageChange(
                        21, 1, 1, 64, 16));
    }
}
