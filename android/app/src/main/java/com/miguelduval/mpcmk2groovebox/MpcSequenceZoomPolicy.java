package com.miguelduval.mpcmk2groovebox;

final class MpcSequenceZoomPolicy {
    static final int MIN_GRID_VISIBLE_STEPS = 1;
    static final int MAX_GRID_VISIBLE_STEPS = 16;
    static final int MIN_GRID_VISIBLE_PADS = 1;
    static final int MAX_GRID_VISIBLE_PADS = 16;
    static final int MIN_TIMELINE_VISIBLE_BARS = 1;

    private MpcSequenceZoomPolicy() {}

    static int zoomGridStepsIn(int visibleSteps) {
        return zoomInCount(
                visibleSteps,
                MIN_GRID_VISIBLE_STEPS,
                MAX_GRID_VISIBLE_STEPS);
    }

    static int zoomGridStepsOut(int visibleSteps) {
        return zoomOutCount(
                visibleSteps,
                MIN_GRID_VISIBLE_STEPS,
                MAX_GRID_VISIBLE_STEPS);
    }

    static int zoomGridPadsIn(int visiblePads) {
        return zoomInCount(
                visiblePads,
                MIN_GRID_VISIBLE_PADS,
                MAX_GRID_VISIBLE_PADS);
    }

    static int zoomGridPadsOut(int visiblePads) {
        return zoomOutCount(
                visiblePads,
                MIN_GRID_VISIBLE_PADS,
                MAX_GRID_VISIBLE_PADS);
    }

    static int zoomTimelineBarsIn(int visibleBars, int totalBars) {
        final int max = Math.max(
                MIN_TIMELINE_VISIBLE_BARS,
                Math.max(1, totalBars));
        return zoomInCount(
                visibleBars,
                MIN_TIMELINE_VISIBLE_BARS,
                max);
    }

    static int zoomTimelineBarsOut(int visibleBars, int totalBars) {
        final int max = Math.max(
                MIN_TIMELINE_VISIBLE_BARS,
                Math.max(1, totalBars));
        return zoomOutCount(
                visibleBars,
                MIN_TIMELINE_VISIBLE_BARS,
                max);
    }

    static int stepIndexForTick(long tick, long ticksPerStep, int totalSteps) {
        final int boundedTotal = Math.max(1, totalSteps);
        final long stepTicks = Math.max(1L, ticksPerStep);
        final long nonNegativeTick = Math.max(0L, tick);
        return (int) Math.min(
                boundedTotal - 1L,
                nonNegativeTick / stepTicks);
    }

    static int gridStartForStep(int stepIndex, int visibleSteps, int totalSteps) {
        final int boundedTotal = Math.max(1, totalSteps);
        final int boundedVisible = clamp(visibleSteps, 1, boundedTotal);
        final int boundedStep = clamp(stepIndex, 0, boundedTotal - 1);
        final int desiredStart = boundedStep - boundedVisible / 2;
        return clamp(desiredStart, 0, boundedTotal - boundedVisible);
    }

    private static int zoomInCount(int current, int min, int max) {
        final int clamped = clamp(current, min, max);
        if (clamped <= min) return min;
        return Math.max(min, Math.min(
                max,
                (int) Math.floor(clamped / MpcZoomPolicy.STEP_FACTOR)));
    }

    private static int zoomOutCount(int current, int min, int max) {
        final int clamped = clamp(current, min, max);
        if (clamped >= max) return max;
        return Math.min(max, Math.max(
                min,
                (int) Math.ceil(clamped * MpcZoomPolicy.STEP_FACTOR)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
