package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Locale;

public final class WaveformView extends View {
    public interface OnSelectionCommitListener {
        void onSelectionCommitted(float startNormalized, float endNormalized);
    }

    private static final int BG = 0xff111518;
    private static final int GRID = 0xff344047;
    private static final int TEXT = 0xff98a6ae;
    private static final int WAVE = 0xff45d3ff;
    private static final int WAVE_DIM = 0xff39717d;
    private static final int RANGE = 0x3345d3ff;
    private static final int HANDLE = 0xffffb448;
    private static final int PLAYHEAD = 0xffffffff;
    private static final int OUTSIDE = 0x7a0b0f12;

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path upperPath = new Path();
    private final Path lowerPath = new Path();
    private final Path fillPath = new Path();
    private final RectF handleRect = new RectF();
    private final ScaleGestureDetector scaleDetector;

    private float[] peaks;
    private float startNormalized = 0f;
    private float endNormalized = 1f;
    private float progressNormalized = 0f;
    private float playheadNormalized = -1f;
    private float durationMs;
    private float viewportStart = 0f;
    private float viewportEnd = 1f;
    private float verticalZoom = 1f;
    private boolean editable;
    private boolean recording;
    private boolean showHandles;
    private int activeHandle = -1;
    private float downX;
    private float lastPanX;
    private boolean panning;
    private OnSelectionCommitListener selectionCommitListener;

    public WaveformView(Context context) {
        super(context);
        scaleDetector = createScaleDetector(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = createScaleDetector(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        scaleDetector = createScaleDetector(context);
        init();
    }

    private ScaleGestureDetector createScaleDetector(Context context) {
        return new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScaleBegin(
                            ScaleGestureDetector detector) {
                        activeHandle = -1;
                        panning = false;
                        return editable && !recording;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (!editable || recording) return false;

                        final float focusFraction = xToViewFraction(
                                detector.getFocusX());
                        final float currentSpan =
                                viewportEnd - viewportStart;
                        final float newSpan = clamp(
                                currentSpan / detector.getScaleFactor(),
                                0.0625f,
                                1.0f);
                        final float focusAbsolute =
                                viewportStart
                                + focusFraction * currentSpan;
                        final float newStart =
                                clamp(
                                        focusAbsolute
                                                - focusFraction * newSpan,
                                        0f,
                                        1f - newSpan);
                        setViewport(newStart, newStart + newSpan);
                        return true;
                    }
                });
    }

    private void init() {
        setFocusable(true);
        setClickable(true);
        fillPaint.setStyle(Paint.Style.FILL);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(1));
        textPaint.setColor(TEXT);
        textPaint.setTextSize(dp(10));
    }

    public void setPeaks(float[] minMaxPeaks) {
        peaks = minMaxPeaks;
        invalidate();
    }

    public void setSelection(float start, float end) {
        startNormalized = clamp(start, 0f, 1f);
        final float minimumEnd = Math.min(1f, startNormalized + 0.0001f);
        endNormalized = clamp(end, minimumEnd, 1f);
        if (endNormalized <= startNormalized) {
            startNormalized = Math.max(0f, endNormalized - 0.0001f);
        }
        invalidate();
    }

    public void setEditable(boolean enabled) {
        editable = enabled;
        showHandles = enabled;
        invalidate();
    }

    public void setRecording(boolean active) {
        recording = active;
        showHandles = !active && editable;
        if (active) {
            viewportStart = 0f;
            viewportEnd = 1f;
        }
        invalidate();
    }

    public void setProgress(float progress) {
        progressNormalized = clamp(progress, 0f, 1f);
        invalidate();
    }

    public void setPlayhead(float playhead) {
        playheadNormalized = playhead < 0f ? -1f : clamp(playhead, 0f, 1f);
        invalidate();
    }

    public void setDurationMs(float duration) {
        durationMs = Math.max(0f, duration);
        invalidate();
    }

    public void setOnSelectionCommitListener(OnSelectionCommitListener listener) {
        selectionCommitListener = listener;
    }

    public void resetZoom() {
        setViewport(0f, 1f);
        verticalZoom = 1f;
        invalidate();
    }

    public void zoomIn() {
        final float center = (startNormalized + endNormalized) * 0.5f;
        zoomAround(center, 0.5f);
    }

    public void zoomOut() {
        final float center = (viewportStart + viewportEnd) * 0.5f;
        zoomAround(center, 2.0f);
    }

    public void zoomVerticalIn() {
        verticalZoom = MpcZoomPolicy.zoomVerticalIn(verticalZoom);
        invalidate();
    }

    public void zoomVerticalOut() {
        verticalZoom = MpcZoomPolicy.zoomVerticalOut(verticalZoom);
        invalidate();
    }

    public float viewportSpanForTest() {
        return viewportEnd - viewportStart;
    }

    public float verticalZoomForTest() {
        return verticalZoom;
    }

    private void zoomAround(float center, float factor) {
        final float span = clamp(
                (viewportEnd - viewportStart) * factor,
                MpcZoomPolicy.MIN_HORIZONTAL_SPAN,
                MpcZoomPolicy.MAX_HORIZONTAL_SPAN);
        final float start = clamp(
                center - span * 0.5f, 0f, 1f - span);
        setViewport(start, start + span);
    }

    public boolean isZoomed() {
        return viewportStart > 0.0001f
                || viewportEnd < 0.9999f;
    }

    private void setViewport(float start, float end) {
        viewportStart = clamp(start, 0f, 0.999f);
        viewportEnd = clamp(end, viewportStart + 0.001f, 1f);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);

        final float left = dp(10);
        final float right = getWidth() - dp(10);
        final float top = dp(12);
        final float bottom = getHeight() - dp(24);
        final float centerY = (top + bottom) * 0.5f;

        drawGrid(canvas, left, right, top, bottom, centerY);
        drawSelectionRange(canvas, left, right, top, bottom);
        drawWave(canvas, left, right, top, bottom, centerY);

        if (durationMs > 0f) {
            drawTimeLabels(canvas, left, right);
        }

        if (recording) {
            final float px = absoluteToX(progressNormalized, left, right);
            linePaint.setColor(HANDLE);
            linePaint.setStrokeWidth(dp(2));
            canvas.drawLine(px, top, px, bottom, linePaint);
            drawMarkerLabel(canvas, px, top + dp(13), "REC");
        }

        if (playheadNormalized >= 0f) {
            final float px = absoluteToX(playheadNormalized, left, right);
            if (isInViewport(playheadNormalized)) {
                linePaint.setColor(PLAYHEAD);
                linePaint.setStrokeWidth(dp(1));
                canvas.drawLine(px, top, px, bottom, linePaint);
            }
        }

        if (editable && showHandles) {
            drawSelectionHandle(
                    canvas,
                    normalizedToViewport(startNormalized),
                    left, right, top, bottom, "S");
            drawSelectionHandle(
                    canvas,
                    normalizedToViewport(endNormalized),
                    left, right, top, bottom, "E");
        }

        linePaint.setColor(GRID);
        linePaint.setStrokeWidth(dp(1));
        canvas.drawLine(left, centerY, right, centerY, linePaint);
    }

    private void drawGrid(
            Canvas canvas,
            float left,
            float right,
            float top,
            float bottom,
            float centerY) {
        linePaint.setColor(GRID);
        linePaint.setStrokeWidth(dp(1));
        for (int i = 0; i <= 8; i++) {
            final float x = left + (right - left) * i / 8f;
            canvas.drawLine(x, top, x, bottom, linePaint);
        }
        canvas.drawLine(left, centerY, right, centerY, linePaint);
        canvas.drawLine(left, top, right, top, linePaint);
        canvas.drawLine(left, bottom, right, bottom, linePaint);
    }

    private void drawSelectionRange(
            Canvas canvas,
            float left,
            float right,
            float top,
            float bottom) {
        final float selectionStart =
                absoluteToX(Math.max(startNormalized, viewportStart), left, right);
        final float selectionEnd =
                absoluteToX(Math.min(endNormalized, viewportEnd), left, right);

        fillPaint.setColor(OUTSIDE);
        if (startNormalized > viewportStart) {
            canvas.drawRect(left, top, selectionStart, bottom, fillPaint);
        }
        if (endNormalized < viewportEnd) {
            canvas.drawRect(selectionEnd, top, right, bottom, fillPaint);
        }

        if (endNormalized > viewportStart
                && startNormalized < viewportEnd) {
            fillPaint.setColor(RANGE);
            canvas.drawRect(
                    Math.max(left, selectionStart),
                    top,
                    Math.min(right, selectionEnd),
                    bottom,
                    fillPaint);
        }
    }

    private void drawWave(
            Canvas canvas,
            float left,
            float right,
            float top,
            float bottom,
            float centerY) {
        if (peaks == null || peaks.length < 2) {
            textPaint.setTextSize(dp(12));
            final String message = recording
                    ? "WAITING FOR AUDIO…"
                    : "NO AUDIO";
            canvas.drawText(
                    message,
                    left,
                    centerY - dp(4),
                    textPaint);
            return;
        }

        final int pointCount = peaks.length / 2;
        final float height = Math.min(
                (bottom - top) * 0.42f * verticalZoom,
                (bottom - top) * 0.49f);
        final int firstPoint = Math.max(
                0,
                (int) Math.floor(viewportStart * (pointCount - 1)));
        final int lastPoint = Math.min(
                pointCount - 1,
                (int) Math.ceil(viewportEnd * (pointCount - 1)));

        upperPath.reset();
        lowerPath.reset();
        fillPath.reset();

        boolean hasVisibleData = false;
        for (int i = firstPoint; i <= lastPoint; i++) {
            final float minimum = clamp(peaks[i * 2], -1f, 1f);
            final float maximum = clamp(peaks[i * 2 + 1], -1f, 1f);
            if (Math.abs(minimum) > 0.0001f
                    || Math.abs(maximum) > 0.0001f) {
                hasVisibleData = true;
            }

            final float absolute =
                    i / Math.max(1f, pointCount - 1f);
            final float viewFraction = normalizedToViewport(absolute);
            final float x = left + (right - left) * viewFraction;
            final float upper = centerY - maximum * height;
            final float lower = centerY - minimum * height;

            if (i == firstPoint) {
                upperPath.moveTo(x, upper);
                lowerPath.moveTo(x, lower);
                fillPath.moveTo(x, upper);
            } else {
                upperPath.lineTo(x, upper);
                lowerPath.lineTo(x, lower);
                fillPath.lineTo(x, upper);
            }
        }

        if (!hasVisibleData) {
            textPaint.setTextSize(dp(12));
            canvas.drawText(
                    recording ? "WAITING FOR AUDIO…" : "NO AUDIO",
                    left,
                    centerY - dp(4),
                    textPaint);
            return;
        }

        for (int i = lastPoint; i >= firstPoint; i--) {
            final float minimum = clamp(peaks[i * 2], -1f, 1f);
            final float absolute =
                    i / Math.max(1f, pointCount - 1f);
            final float viewFraction = normalizedToViewport(absolute);
            final float x = left + (right - left) * viewFraction;
            fillPath.lineTo(x, centerY - minimum * height);
        }
        fillPath.close();

        fillPaint.setColor(recording ? 0x1439c7dc : 0x1f45d3ff);
        canvas.drawPath(fillPath, fillPaint);

        linePaint.setColor(recording ? WAVE_DIM : WAVE);
        linePaint.setStrokeWidth(dp(1.4f));
        canvas.drawPath(upperPath, linePaint);
        canvas.drawPath(lowerPath, linePaint);
    }

    private void drawSelectionHandle(
            Canvas canvas,
            float viewFraction,
            float left,
            float right,
            float top,
            float bottom,
            String label) {
        final float x = left + (right - left) * viewFraction;
        linePaint.setColor(HANDLE);
        linePaint.setStrokeWidth(dp(2));
        canvas.drawLine(x, top, x, bottom, linePaint);

        fillPaint.setColor(HANDLE);
        handleRect.set(
                x - dp(8), top,
                x + dp(8), top + dp(20));
        canvas.drawRoundRect(handleRect, dp(3), dp(3), fillPaint);
        canvas.drawCircle(x, top, dp(3), fillPaint);

        textPaint.setColor(BG);
        textPaint.setTextSize(dp(8));
        final float labelWidth = textPaint.measureText(label);
        canvas.drawText(label, x - labelWidth * 0.5f, top + dp(13), textPaint);
        textPaint.setColor(TEXT);
    }

    private void drawMarkerLabel(Canvas canvas, float x, float y, String text) {
        textPaint.setColor(HANDLE);
        textPaint.setTextSize(dp(8));
        canvas.drawText(text, x + dp(4), y, textPaint);
        textPaint.setColor(TEXT);
    }

    private void drawTimeLabels(Canvas canvas, float left, float right) {
        textPaint.setTextSize(dp(9));
        textPaint.setColor(TEXT);

        for (int i = 0; i <= 4; i++) {
            final float fraction = i / 4f;
            final float absolute =
                    viewportStart
                    + fraction * (viewportEnd - viewportStart);
            final String label = formatTime(durationMs * absolute);
            final float x = left + (right - left) * fraction;
            final float width = textPaint.measureText(label);

            if (i == 0) {
                canvas.drawText(label, x, getHeight() - dp(7), textPaint);
            } else if (i == 4) {
                canvas.drawText(label, x - width, getHeight() - dp(7), textPaint);
            } else {
                canvas.drawText(
                        label, x - width * 0.5f, getHeight() - dp(7), textPaint);
            }
        }
    }

    private String formatTime(float ms) {
        if (ms < 1000f) {
            return String.format(Locale.ROOT, "%.0fms", ms);
        }
        return String.format(Locale.ROOT, "%.1fs", ms / 1000f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        if (!editable || !showHandles || recording) {
            return true;
        }

        if (event.getPointerCount() > 1) {
            activeHandle = -1;
            panning = false;
            return true;
        }

        final float left = dp(10);
        final float right = getWidth() - dp(10);
        final float fractionInView =
                clamp((event.getX() - left) / Math.max(1f, right - left), 0f, 1f);
        final float fractionAbsolute =
                viewportStart
                + fractionInView * (viewportEnd - viewportStart);
        final float sx = left
                + (right - left)
                * normalizedToViewport(startNormalized);
        final float ex = left
                + (right - left)
                * normalizedToViewport(endNormalized);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                lastPanX = event.getX();
                panning = false;
                final float startDistance = Math.abs(event.getX() - sx);
                final float endDistance = Math.abs(event.getX() - ex);
                final float hitRadius = dp(32);
                if (startDistance <= hitRadius
                        && isInViewport(startNormalized)) {
                    activeHandle = 0;
                } else if (endDistance <= hitRadius
                        && isInViewport(endNormalized)) {
                    activeHandle = 1;
                } else {
                    activeHandle = -1;
                    panning = isZoomed();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (activeHandle >= 0) {
                    moveHandle(fractionAbsolute);
                } else if (panning) {
                    final float deltaFraction =
                            (event.getX() - lastPanX)
                            / Math.max(1f, right - left);
                    panBy(-deltaFraction);
                    lastPanX = event.getX();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeHandle >= 0) {
                    moveHandle(fractionAbsolute);
                    if (event.getActionMasked() == MotionEvent.ACTION_UP
                            && Math.abs(event.getX() - downX) >= dp(2)
                            && selectionCommitListener != null) {
                        selectionCommitListener.onSelectionCommitted(
                                startNormalized, endNormalized);
                    }
                }
                activeHandle = -1;
                panning = false;
                return true;

            default:
                return true;
        }
    }

    private void moveHandle(float fraction) {
        if (activeHandle == 0) {
            startNormalized = clamp(
                    fraction, 0f, Math.max(0f, endNormalized - 0.0001f));
        } else if (activeHandle == 1) {
            endNormalized = clamp(
                    fraction, Math.min(1f, startNormalized + 0.0001f), 1f);
        }
        invalidate();
    }

    private void panBy(float deltaFraction) {
        final float span = viewportEnd - viewportStart;
        final float newStart = clamp(
                viewportStart + deltaFraction, 0f, 1f - span);
        setViewport(newStart, newStart + span);
    }

    private boolean isInViewport(float normalized) {
        return normalized >= viewportStart - 0.0001f
                && normalized <= viewportEnd + 0.0001f;
    }

    private float normalizedToViewport(float normalized) {
        if (viewportEnd <= viewportStart) return 0f;
        return clamp(
                (normalized - viewportStart)
                        / (viewportEnd - viewportStart),
                0f,
                1f);
    }

    private float xToViewFraction(float x) {
        final float left = dp(10);
        final float right = getWidth() - dp(10);
        return clamp(
                (x - left) / Math.max(1f, right - left),
                0f,
                1f);
    }

    private float absoluteToX(float normalized, float left, float right) {
        return left + (right - left) * normalizedToViewport(normalized);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
