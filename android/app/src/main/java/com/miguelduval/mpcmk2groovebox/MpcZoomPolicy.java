package com.miguelduval.mpcmk2groovebox;

final class MpcZoomPolicy {
    static final float MIN_HORIZONTAL_SPAN = 0.0625f;
    static final float MAX_HORIZONTAL_SPAN = 1.0f;
    static final float MIN_VERTICAL_ZOOM = 1.0f;
    static final float MAX_VERTICAL_ZOOM = 4.0f;
    static final float STEP_FACTOR = 1.25f;

    private MpcZoomPolicy() {}

    static float zoomHorizontalIn(float span) {
        return clamp(span / STEP_FACTOR, MIN_HORIZONTAL_SPAN, MAX_HORIZONTAL_SPAN);
    }

    static float zoomHorizontalOut(float span) {
        return clamp(span * STEP_FACTOR, MIN_HORIZONTAL_SPAN, MAX_HORIZONTAL_SPAN);
    }

    /**
     * Keeps the viewport width independent from the sample's editable Start/End
     * region. Button zoom and pinch zoom share this normalized bound.
     */
    static float clampHorizontalSpan(float span) {
        return clamp(span, MIN_HORIZONTAL_SPAN, MAX_HORIZONTAL_SPAN);
    }

    /**
     * Returns a centered viewport start while keeping the full range in [0, 1].
     */
    static float viewportStartForCenter(float center, float span) {
        final float boundedSpan = clampHorizontalSpan(span);
        final float boundedCenter = clamp(center, 0.0f, 1.0f);
        return clamp(boundedCenter - boundedSpan * 0.5f, 0.0f, 1.0f - boundedSpan);
    }

    static float zoomVerticalIn(float value) {
        return clamp(value * STEP_FACTOR, MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM);
    }

    static float zoomVerticalOut(float value) {
        return clamp(value / STEP_FACTOR, MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM);
    }

    private static float clamp(float value, float min, float max) {
        if (Float.isNaN(value) || value == Float.NEGATIVE_INFINITY) return min;
        if (value == Float.POSITIVE_INFINITY) return max;
        return Math.max(min, Math.min(max, value));
    }
}
