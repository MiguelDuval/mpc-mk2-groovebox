package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcZoomPolicyTest {
    private static final float EPSILON = 0.00001f;

    @Test
    public void horizontalZoomInAndOutAreInverseWithinTheAllowedRange() {
        float span = MpcZoomPolicy.MAX_HORIZONTAL_SPAN;
        float zoomed = MpcZoomPolicy.zoomHorizontalIn(span);

        assertTrue(zoomed < span);
        assertEquals(span, MpcZoomPolicy.zoomHorizontalOut(zoomed), EPSILON);

        for (int i = 0; i < 40; i++) {
            span = MpcZoomPolicy.zoomHorizontalIn(span);
        }
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN, span, EPSILON);

        for (int i = 0; i < 40; i++) {
            span = MpcZoomPolicy.zoomHorizontalOut(span);
        }
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN, span, EPSILON);
    }

    @Test
    public void centeredViewportStaysInsideTheFullSampleAtBothEdges() {
        assertEquals(
                0.375f,
                MpcZoomPolicy.viewportStartForCenter(0.5f, 0.25f),
                EPSILON);
        assertEquals(
                0.0f,
                MpcZoomPolicy.viewportStartForCenter(0.0f, 0.25f),
                EPSILON);
        assertEquals(
                0.75f,
                MpcZoomPolicy.viewportStartForCenter(1.0f, 0.25f),
                EPSILON);
        assertEquals(
                0.0f,
                MpcZoomPolicy.viewportStartForCenter(0.5f, 1.0f),
                EPSILON);
    }

    @Test
    public void horizontalSpanRejectsNanAndBoundsInfiniteOrOutOfRangeValues() {
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NaN),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NEGATIVE_INFINITY),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.POSITIVE_INFINITY),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(-2.0f),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(2.0f),
                EPSILON);
    }

    @Test
    public void zoomOperationsRemainDeterministicForNonFiniteInputs() {
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalIn(Float.NaN),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalOut(Float.POSITIVE_INFINITY),
                EPSILON);
        assertEquals(
                0.0f,
                MpcZoomPolicy.viewportStartForCenter(Float.NaN, 0.25f),
                EPSILON);
    }

    @Test
    public void verticalZoomStopsAtItsDeclaredMinimumAndMaximum() {
        float zoom = MpcZoomPolicy.MIN_VERTICAL_ZOOM;
        assertEquals(1.25f, MpcZoomPolicy.zoomVerticalIn(zoom), EPSILON);

        for (int i = 0; i < 40; i++) {
            zoom = MpcZoomPolicy.zoomVerticalIn(zoom);
        }
        assertEquals(MpcZoomPolicy.MAX_VERTICAL_ZOOM, zoom, EPSILON);

        for (int i = 0; i < 40; i++) {
            zoom = MpcZoomPolicy.zoomVerticalOut(zoom);
        }
        assertEquals(MpcZoomPolicy.MIN_VERTICAL_ZOOM, zoom, EPSILON);
    }
}
