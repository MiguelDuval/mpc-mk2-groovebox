package com.miguelduval.mpcmk2groovebox;

/** Pure bounds/mapping policy for the Main XL Track channel strip. */
final class MpcTrackMixerPolicy {
    private static final float MIN_LEVEL = 0.0f;
    private static final float MAX_LEVEL = 2.0f;
    private static final float MIN_PAN = -1.0f;
    private static final float MAX_PAN = 1.0f;

    private MpcTrackMixerPolicy() {}

    static float clampLevel(float value) {
        if (!Float.isFinite(value)) return 1.0f;
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, value));
    }

    static float clampPan(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(MIN_PAN, Math.min(MAX_PAN, value));
    }

    static float levelForTouch(
            float y, float top, float bottom, float maximum) {
        final float span = Math.max(1.0f, bottom - top);
        final float normalized = clampUnit((bottom - y) / span);
        return normalized * Math.max(0.001f, Math.min(MAX_LEVEL, maximum));
    }

    static float panForTouch(float x, float left, float right) {
        final float span = Math.max(1.0f, right - left);
        final float normalized = clampUnit((x - left) / span);
        return clampPan((normalized * 2.0f) - 1.0f);
    }

    static String panLabel(float value) {
        final float pan = clampPan(value);
        if (Math.abs(pan) < 0.005f) return "C";
        return (pan < 0.0f ? "L " : "R ")
                + Math.round(Math.abs(pan) * 100.0f);
    }

    private static float clampUnit(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}