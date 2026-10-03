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

    static float zoomVerticalIn(float value) {
        return clamp(value * STEP_FACTOR, MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM);
    }

    static float zoomVerticalOut(float value) {
        return clamp(value / STEP_FACTOR, MIN_VERTICAL_ZOOM, MAX_VERTICAL_ZOOM);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
