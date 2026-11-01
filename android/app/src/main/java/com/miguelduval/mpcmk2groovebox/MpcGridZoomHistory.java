package com.miguelduval.mpcmk2groovebox;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Tracks reversible discrete zoom steps for one independent grid axis.
 *
 * A policy's numeric scale factor is not necessarily reciprocal after rounding
 * (for example, 16 visible steps can zoom to 12, then back out to 15). Remember
 * the actual before/after windows so the opposite action restores the exact
 * viewport rather than approximating it.
 */
final class MpcGridZoomHistory {
    static final class Window {
        final int visibleCount;
        final int start;
        final int totalCount;

        Window(int visibleCount, int start, int totalCount) {
            this.visibleCount = Math.max(1, Math.min(Math.max(1, totalCount), visibleCount));
            this.totalCount = Math.max(1, totalCount);
            this.start = Math.max(0, Math.min(
                    Math.max(0, this.totalCount - this.visibleCount), start));
        }

        boolean sameAs(Window other) {
            return other != null
                    && visibleCount == other.visibleCount
                    && start == other.start
                    && totalCount == other.totalCount;
        }
    }

    private static final class Transition {
        final boolean zoomIn;
        final Window before;
        final Window after;

        Transition(boolean zoomIn, Window before, Window after) {
            this.zoomIn = zoomIn;
            this.before = before;
            this.after = after;
        }
    }

    private final Deque<Transition> transitions = new ArrayDeque<>();

    /**
     * Restores the previous window only for the opposite direction and only
     * while the live viewport still matches the state produced by that zoom.
     * External viewport changes invalidate the history rather than resurrecting
     * stale positions.
     */
    Window restoreOpposite(boolean requestedZoomIn, Window current) {
        final Transition last = transitions.peek();
        if (last == null) {
            return null;
        }
        if (!last.after.sameAs(current)) {
            transitions.clear();
            return null;
        }
        if (last.zoomIn == requestedZoomIn) {
            return null;
        }
        transitions.pop();
        return last.before;
    }

    void record(boolean zoomIn, Window before, Window after) {
        if (before == null || after == null || before.sameAs(after)) {
            return;
        }
        transitions.push(new Transition(zoomIn, before, after));
    }

    void clear() {
        transitions.clear();
    }

    int depthForTest() {
        return transitions.size();
    }
}
