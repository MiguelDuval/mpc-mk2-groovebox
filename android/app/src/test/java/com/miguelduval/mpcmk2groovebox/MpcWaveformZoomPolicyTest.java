package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcWaveformZoomPolicyTest {
    private static final float EPSILON = 0.00001f;

    @Test
    public void zoomInCentersOnSelectionAndZoomOutCanRestoreWholeSample() {
        float zoomedSpan = MpcZoomPolicy.zoomHorizontalIn(1.0f);
        float zoomedStart = MpcZoomPolicy.viewportStartForCenter(0.5f, zoomedSpan);

        assertEquals(0.8f, zoomedSpan, EPSILON);
        assertEquals(0.1f, zoomedStart, EPSILON);
        assertEquals(0.9f, zoomedStart + zoomedSpan, EPSILON);

        float restoredSpan = MpcZoomPolicy.zoomHorizontalOut(zoomedSpan);
        float restoredStart = MpcZoomPolicy.viewportStartForCenter(0.5f, restoredSpan);
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN, restoredSpan, EPSILON);
        assertEquals(0.0f, restoredStart, EPSILON);
        assertEquals(1.0f, restoredStart + restoredSpan, EPSILON);
    }

    @Test
    public void repeatedZoomInAndOutNeverCrossMinimumOrMaximumSpan() {
        float span = MpcZoomPolicy.MAX_HORIZONTAL_SPAN;
        for (int i = 0; i < 40; i++) {
            span = MpcZoomPolicy.zoomHorizontalIn(span);
        }
        assertEquals(MpcZoomPolicy.MIN_HORIZONTAL_SPAN, span, EPSILON);
        assertTrue(span > 0.0f);

        for (int i = 0; i < 40; i++) {
            span = MpcZoomPolicy.zoomHorizontalOut(span);
        }
        assertEquals(MpcZoomPolicy.MAX_HORIZONTAL_SPAN, span, EPSILON);
    }

    @Test
    public void zoomFocusClampsAtBothSampleEdgesAndKeepsRangeInsideBounds() {
        float span = 0.25f;

        float leftStart = MpcZoomPolicy.viewportStartForCenter(0.05f, span);
        float middleStart = MpcZoomPolicy.viewportStartForCenter(0.5f, span);
        float rightStart = MpcZoomPolicy.viewportStartForCenter(0.95f, span);

        assertEquals(0.0f, leftStart, EPSILON);
        assertEquals(0.375f, middleStart, EPSILON);
        assertEquals(0.75f, rightStart, EPSILON);
        for (float start : new float[]{leftStart, middleStart, rightStart}) {
            assertTrue(start >= 0.0f);
            assertTrue(start + span <= 1.0f);
        }
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

    @Test
    public void viewportFocusGridAlwaysRemainsInsideNormalizedSampleBounds() {
        float[] centers = new float[] {
                0.0f, 0.001f, 0.05f, 0.125f, 0.25f, 0.5f,
                0.75f, 0.875f, 0.95f, 0.999f, 1.0f
        };
        float[] spans = new float[] {
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                0.125f, 0.25f, 0.5f, 0.8f,
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN
        };

        for (float center : centers) {
            for (float span : spans) {
                float boundedSpan = MpcZoomPolicy.clampHorizontalSpan(span);
                float start = MpcZoomPolicy.viewportStartForCenter(center, span);
                float end = start + boundedSpan;

                assertTrue("start must be normalized: " + start,
                        start >= 0.0f && start <= 1.0f);
                assertTrue("end must be normalized: " + end,
                        end >= 0.0f && end <= 1.0f);
                assertEquals("span must be preserved", boundedSpan, end - start, EPSILON);

                if (center >= boundedSpan * 0.5f
                        && center <= 1.0f - boundedSpan * 0.5f) {
                    assertEquals("interior focus must remain centered",
                            center - boundedSpan * 0.5f, start, EPSILON);
                }
            }
        }
    }

    @Test
    public void malformedZoomValuesFailSafelyInsideNormalizedBounds() {
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NaN),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.POSITIVE_INFINITY),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(Float.NEGATIVE_INFINITY),
                EPSILON);
        assertEquals(0.0f,
                MpcZoomPolicy.viewportStartForCenter(Float.NaN, 0.25f),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(-2.0f),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.clampHorizontalSpan(2.0f),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalIn(Float.NaN),
                EPSILON);
        assertEquals(
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN,
                MpcZoomPolicy.zoomHorizontalOut(Float.POSITIVE_INFINITY),
                EPSILON);

    }
}
