package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcWaveformZoomPolicyTest {
    private static final float EPSILON = 0.00001f;

    @Test
    public void zoomInCentersOnSelectionAndZoomOutRestoresWholeSample() {
        float zoomedSpan = MpcZoomPolicy.zoomHorizontalIn(1.0f);
        float zoomedStart = MpcZoomPolicy.viewportStartForCenter(0.5f, zoomedSpan);

        assertEquals(0.8f, zoomedSpan, EPSILON);
        assertEquals(0.1f, zoomedStart, EPSILON);
        assertEquals(0.9f, zoomedStart + zoomedSpan, EPSILON);

        float restoredSpan = MpcZoomPolicy.zoomHorizontalOut(zoomedSpan);
        float restoredStart = MpcZoomPolicy.viewportStartForCenter(0.5f, restoredSpan);
        assertEquals(1.0f, restoredSpan, EPSILON);
        assertEquals(0.0f, restoredStart, EPSILON);
        assertEquals(1.0f, restoredStart + restoredSpan, EPSILON);
    }

    @Test
    public void repeatedHorizontalZoomNeverCrossesNormalizedBounds() {
        float span = MpcZoomPolicy.MAX_HORIZONTAL_SPAN;
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
    public void viewportFocusClampsAtBothEdgesAndPreservesSpan() {
        float span = 0.25f;
        assertEquals(0.0f, MpcZoomPolicy.viewportStartForCenter(0.05f, span), EPSILON);
        assertEquals(0.375f, MpcZoomPolicy.viewportStartForCenter(0.5f, span), EPSILON);
        assertEquals(0.75f, MpcZoomPolicy.viewportStartForCenter(0.95f, span), EPSILON);

        for (float center : new float[] {0.0f, 0.001f, 0.05f, 0.5f, 0.95f, 0.999f, 1.0f}) {
            float start = MpcZoomPolicy.viewportStartForCenter(center, span);
            float end = start + span;
            assertTrue("start must be normalized: " + start, start >= 0.0f && start <= 1.0f);
            assertTrue("end must be normalized: " + end, end >= 0.0f && end <= 1.0f);
            assertEquals("span must be preserved", span, end - start, EPSILON);
        }
    }

    @Test
    public void invalidAndInfiniteSpanValuesAreBounded() {
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NaN), EPSILON);
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.POSITIVE_INFINITY), EPSILON);
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NEGATIVE_INFINITY), EPSILON);
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(-2.0f), EPSILON);
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(2.0f), EPSILON);
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalIn(Float.NaN), EPSILON);
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalOut(Float.POSITIVE_INFINITY), EPSILON);
    }

    @Test
    public void verticalZoomRemainsBoundedAtBothExtremes() {
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
