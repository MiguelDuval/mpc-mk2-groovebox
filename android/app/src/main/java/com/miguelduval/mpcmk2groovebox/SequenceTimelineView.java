package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public final class SequenceTimelineView extends View {
    public interface OnLoopCommitListener {
        void onLoopCommitted(int startBar, int endBar);
    }

    private static final int BG = 0xff111518;
    private static final int GRID = 0xff344047;
    private static final int TEXT = 0xffb2bdc4;
    private static final int RANGE = 0x668f2630;
    private static final int RANGE_EDGE = 0xffe04755;
    private static final int PLAYHEAD = 0xffffffff;
    private static final int HANDLE = 0xffffb448;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF handleRect = new RectF();

    private int barCount = 4;
    private int loopStartBar = 1;
    private int loopEndBar = 4;
    private float playheadBar = 1f;
    private int viewportStartBar = 1;
    private int viewportVisibleBars = 4;
    private int activeHandle = -1;
    private OnLoopCommitListener loopCommitListener;
    private GestureDetector gestureDetector;
    private OnDoubleTapListener doubleTapListener;

    public SequenceTimelineView(Context context) {
        super(context);
        init();
    }

    public SequenceTimelineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SequenceTimelineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        gestureDetector = new GestureDetector(
                getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        if (doubleTapListener != null) {
                            doubleTapListener.onDoubleTapped();
                        }
                        return true;
                    }
                });
        fillPaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.STROKE);
        textPaint.setTextSize(dp(10));
        setFocusable(true);
        setClickable(true);
    }

    public void setBarCount(int count) {
        barCount = Math.max(1, count);
        loopStartBar = clampBar(loopStartBar);
        loopEndBar = clampBar(loopEndBar);
        if (loopStartBar > loopEndBar) loopStartBar = loopEndBar;
        viewportVisibleBars = Math.min(viewportVisibleBars, barCount);
        viewportVisibleBars = Math.max(1, viewportVisibleBars);
        viewportStartBar = clampViewportStart(viewportStartBar);
        ensurePlayheadVisible();
        invalidate();
    }

    public void setLoop(int startBar, int endBar) {
        loopStartBar = clampBar(startBar);
        loopEndBar = clampBar(endBar);
        if (loopStartBar > loopEndBar) {
            loopStartBar = loopEndBar;
        }
        invalidate();
    }

    public void setPlayheadBar(float bar) {
        playheadBar = clamp(
                bar <= 0f ? 1f : bar,
                1f,
                Math.max(1f, barCount));
        ensurePlayheadVisible();
        invalidate();
    }

    public void zoomIn() {
        final int nextVisible = MpcSequenceZoomPolicy.zoomTimelineBarsIn(
                viewportVisibleBars,
                barCount);
        if (nextVisible == viewportVisibleBars) return;
        zoomAroundPlayhead(nextVisible);
    }

    public void zoomOut() {
        final int nextVisible = MpcSequenceZoomPolicy.zoomTimelineBarsOut(
                viewportVisibleBars,
                barCount);
        if (nextVisible == viewportVisibleBars) return;
        zoomAroundPlayhead(nextVisible);
    }

    public int visibleBarsForTest() {
        return viewportVisibleBars;
    }

    public int viewportStartBarForTest() {
        return viewportStartBar;
    }

    public void resetZoom() {
        viewportVisibleBars = barCount;
        viewportStartBar = 1;
        ensurePlayheadVisible();
        invalidate();
    }

    public void setOnLoopCommitListener(OnLoopCommitListener listener) {
        loopCommitListener = listener;
    }

    public void setOnDoubleTapListener(OnDoubleTapListener listener) {
        doubleTapListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);

        final float left = dp(12);
        final float right = getWidth() - dp(12);
        final float top = dp(18);
        final float bottom = getHeight() - dp(8);
        final float width = Math.max(1f, right - left);

        final float rangeLeft = xForBar(loopStartBar, left, width);
        final float rangeRight = xForBar(loopEndBar + 0.999f, left, width);
        fillPaint.setColor(RANGE);
        if (rangeRight >= left && rangeLeft <= right) {
            canvas.drawRect(
                    Math.max(left, rangeLeft),
                    top,
                    Math.min(right, rangeRight),
                    bottom,
                    fillPaint);
        }

        linePaint.setStrokeWidth(dp(1));
        linePaint.setColor(GRID);
        for (int visible = 0; visible <= viewportVisibleBars; visible++) {
            final float x = left + width * visible / viewportVisibleBars;
            canvas.drawLine(x, top, x, bottom, linePaint);
        }

        linePaint.setColor(RANGE_EDGE);
        linePaint.setStrokeWidth(dp(2));
        if (rangeLeft >= left && rangeLeft <= right) {
            canvas.drawLine(rangeLeft, top, rangeLeft, bottom, linePaint);
        }
        if (rangeRight >= left && rangeRight <= right) {
            canvas.drawLine(rangeRight, top, rangeRight, bottom, linePaint);
        }

        textPaint.setColor(TEXT);
        textPaint.setTextSize(dp(10));
        for (int visible = 0; visible < viewportVisibleBars; visible++) {
            final int bar = viewportStartBar + visible;
            final float x = left + width * visible / viewportVisibleBars;
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", bar),
                    x + dp(4),
                    dp(13),
                    textPaint);
        }

        if (playheadBar >= viewportStartBar
                && playheadBar <= viewportStartBar + viewportVisibleBars) {
            final float playheadX = xForBar(playheadBar, left, width);
            linePaint.setColor(PLAYHEAD);
            linePaint.setStrokeWidth(dp(1));
            canvas.drawLine(playheadX, top, playheadX, bottom, linePaint);
        }

        final float startX = xForBar(loopStartBar, left, width);
        final float endX = xForBar(loopEndBar + 0.999f, left, width);
        if (startX >= left && startX <= right) {
            drawHandle(canvas, startX, top, "IN");
        }
        if (endX >= left && endX <= right) {
            drawHandle(canvas, endX, top, "OUT");
        }
    }

    private void drawHandle(Canvas canvas, float x, float top, String label) {
        fillPaint.setColor(HANDLE);
        handleRect.set(x - dp(10), top - dp(1), x + dp(10), top + dp(13));
        canvas.drawRoundRect(handleRect, dp(3), dp(3), fillPaint);
        textPaint.setColor(BG);
        textPaint.setTextSize(dp(7));
        final float labelWidth = textPaint.measureText(label);
        canvas.drawText(label, x - labelWidth * 0.5f, top + dp(9), textPaint);
        textPaint.setColor(TEXT);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        final float left = dp(12);
        final float right = getWidth() - dp(12);
        final float width = Math.max(1f, right - left);
        final float startX = xForBar(loopStartBar, left, width);
        final float endX = xForBar(loopEndBar + 0.999f, left, width);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                final float x = event.getX();
                if (Math.abs(x - startX) <= dp(34)) {
                    activeHandle = 0;
                } else if (Math.abs(x - endX) <= dp(34)) {
                    activeHandle = 1;
                } else {
                    activeHandle = -1;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle >= 0) {
                    updateHandle(event.getX(), left, width);
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (activeHandle >= 0) {
                    updateHandle(event.getX(), left, width);
                    if (loopCommitListener != null) {
                        loopCommitListener.onLoopCommitted(loopStartBar, loopEndBar);
                    }
                }
                activeHandle = -1;
                return true;

            case MotionEvent.ACTION_CANCEL:
                activeHandle = -1;
                return true;

            default:
                return true;
        }
    }

    private void updateHandle(float x, float left, float width) {
        final int bar = clampBar(
                viewportStartBar
                        + (int) Math.floor(
                                clamp((x - left) / width, 0f, 0.99999f)
                                        * viewportVisibleBars));
        if (activeHandle == 0) {
            loopStartBar = Math.min(bar, loopEndBar);
        } else if (activeHandle == 1) {
            loopEndBar = Math.max(bar, loopStartBar);
        }
        invalidate();
    }

    private void zoomAroundPlayhead(int visibleBars) {
        viewportVisibleBars = Math.max(
                1,
                Math.min(barCount, visibleBars));
        final int centerBar = clampBar(Math.round(playheadBar));
        final int desiredStart =
                centerBar - (viewportVisibleBars - 1) / 2;
        viewportStartBar = clampViewportStart(desiredStart);
        invalidate();
    }

    private void ensurePlayheadVisible() {
        if (playheadBar < viewportStartBar) {
            viewportStartBar = clampViewportStart(
                    (int) Math.floor(playheadBar));
        } else if (playheadBar > viewportStartBar + viewportVisibleBars) {
            viewportStartBar = clampViewportStart(
                    (int) Math.ceil(playheadBar) - viewportVisibleBars);
        }
    }

    private float xForBar(float bar, float left, float width) {
        final float relative = (bar - viewportStartBar)
                / Math.max(1f, viewportVisibleBars);
        return left + width * relative;
    }

    private int clampViewportStart(int start) {
        return Math.max(
                1,
                Math.min(
                        Math.max(1, barCount - viewportVisibleBars + 1),
                        start));
    }

    private int clampBar(int bar) {
        return Math.max(1, Math.min(barCount, bar));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
