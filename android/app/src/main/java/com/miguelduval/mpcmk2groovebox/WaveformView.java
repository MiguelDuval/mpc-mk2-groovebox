package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
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

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path upperPath = new Path();
    private final Path lowerPath = new Path();
    private final RectF handleRect = new RectF();

    private float[] peaks;
    private float startNormalized = 0f;
    private float endNormalized = 1f;
    private float progressNormalized = 0f;
    private float playheadNormalized = -1f;
    private float durationMs;
    private boolean editable;
    private boolean recording;
    private boolean showHandles;
    private int activeHandle = -1;
    private float downX;
    private OnSelectionCommitListener selectionCommitListener;

    public WaveformView(Context context) {
        super(context);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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
        endNormalized = clamp(end, startNormalized + 0.0001f, 1f);
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

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);

        final float left = dp(10);
        final float right = getWidth() - dp(10);
        final float top = dp(12);
        final float bottom = getHeight() - dp(24);
        final float centerY = (top + bottom) * 0.5f;

        drawGrid(canvas, left, right, top, bottom);
        drawWave(canvas, left, right, top, bottom, centerY);

        if (durationMs > 0f) {
            drawTimeLabels(canvas, left, right, bottom);
        }

        final float sx = left + (right - left) * startNormalized;
        final float ex = left + (right - left) * endNormalized;
        fillPaint.setColor(RANGE);
        canvas.drawRect(sx, top, ex, bottom, fillPaint);

        if (peaks != null && peaks.length >= 2) {
            drawWave(canvas, left, right, top, bottom, centerY);
        }

        if (recording) {
            final float px = left + (right - left) * progressNormalized;
            linePaint.setColor(HANDLE);
            linePaint.setStrokeWidth(dp(2));
            canvas.drawLine(px, top, px, bottom, linePaint);
        }

        if (playheadNormalized >= 0f) {
            final float px = left + (right - left) * playheadNormalized;
            linePaint.setColor(PLAYHEAD);
            linePaint.setStrokeWidth(dp(1));
            canvas.drawLine(px, top, px, bottom, linePaint);
        }

        if (editable && showHandles) {
            drawHandle(canvas, sx, top, bottom, true);
            drawHandle(canvas, ex, top, bottom, false);
        }

        linePaint.setColor(GRID);
        linePaint.setStrokeWidth(dp(1));
        canvas.drawLine(left, centerY, right, centerY, linePaint);
    }

    private void drawGrid(Canvas canvas, float left, float right, float top, float bottom) {
        linePaint.setColor(GRID);
        linePaint.setStrokeWidth(dp(1));
        for (int i = 0; i <= 8; i++) {
            float x = left + (right - left) * i / 8f;
            canvas.drawLine(x, top, x, bottom, linePaint);
        }
        canvas.drawLine(left, top, right, top, linePaint);
        canvas.drawLine(left, bottom, right, bottom, linePaint);
    }

    private void drawWave(Canvas canvas, float left, float right, float top, float bottom, float centerY) {
        if (peaks == null || peaks.length < 2) {
            textPaint.setTextSize(dp(12));
            canvas.drawText(recording ? "WAITING FOR AUDIO…" : "NO AUDIO", left, centerY - dp(4), textPaint);
            return;
        }

        final int pointCount = peaks.length / 2;
        final float height = (bottom - top) * 0.42f;
        upperPath.reset();
        lowerPath.reset();

        boolean hasVisibleData = false;
        for (int i = 0; i < pointCount; i++) {
            final float min = clamp(peaks[i * 2], -1f, 1f);
            final float max = clamp(peaks[i * 2 + 1], -1f, 1f);
            if (Math.abs(min) > 0.0001f || Math.abs(max) > 0.0001f) {
                hasVisibleData = true;
            }
            final float x = left + (right - left) * i / Math.max(1f, pointCount - 1f);
            final float upper = centerY - max * height;
            final float lower = centerY - min * height;
            if (i == 0) {
                upperPath.moveTo(x, upper);
                lowerPath.moveTo(x, lower);
            } else {
                upperPath.lineTo(x, upper);
                lowerPath.lineTo(x, lower);
            }
        }

        if (!hasVisibleData) {
            textPaint.setTextSize(dp(12));
            canvas.drawText(recording ? "WAITING FOR AUDIO…" : "NO AUDIO", left, centerY - dp(4), textPaint);
            return;
        }

        linePaint.setColor(recording ? WAVE_DIM : WAVE);
        linePaint.setStrokeWidth(dp(1.4f));
        canvas.drawPath(upperPath, linePaint);
        canvas.drawPath(lowerPath, linePaint);

        fillPaint.setColor(recording ? 0x1439c7dc : 0x1f45d3ff);
        upperPath.close();
        lowerPath.close();
        canvas.drawPath(upperPath, fillPaint);
        canvas.drawPath(lowerPath, fillPaint);
    }

    private void drawHandle(Canvas canvas, float x, float top, float bottom, boolean start) {
        linePaint.setColor(HANDLE);
        linePaint.setStrokeWidth(dp(2));
        canvas.drawLine(x, top, x, bottom, linePaint);
        fillPaint.setColor(HANDLE);
        final float w = dp(8);
        final float h = dp(20);
        final float cy = start ? top + h : bottom - h;
        handleRect.set(x - w, start ? top : bottom - h,
                x + w, start ? top + h : bottom);
        canvas.drawRoundRect(handleRect, dp(3), dp(3), fillPaint);
        canvas.drawCircle(x, start ? top : bottom, dp(3), fillPaint);
    }

    private void drawTimeLabels(Canvas canvas, float left, float right, float bottom) {
        textPaint.setTextSize(dp(9));
        textPaint.setColor(TEXT);
        for (int i = 0; i <= 4; i++) {
            float fraction = i / 4f;
            float x = left + (right - left) * fraction;
            String label = formatTime(durationMs * fraction);
            if (i == 0) {
                canvas.drawText(label, x, getHeight() - dp(7), textPaint);
            } else if (i == 4) {
                float width = textPaint.measureText(label);
                canvas.drawText(label, x - width, getHeight() - dp(7), textPaint);
            } else {
                float width = textPaint.measureText(label);
                canvas.drawText(label, x - width * 0.5f, getHeight() - dp(7), textPaint);
            }
        }
    }

    private String formatTime(float ms) {
        if (ms < 1000f) return String.format(Locale.ROOT, "%.0fms", ms);
        return String.format(Locale.ROOT, "%.1fs", ms / 1000f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editable || !showHandles || recording) {
            return true;
        }

        final float left = dp(10);
        final float right = getWidth() - dp(10);
        final float x = clamp(event.getX(), left, right);
        final float fraction = clamp((x - left) / Math.max(1f, right - left), 0f, 1f);
        final float sx = left + (right - left) * startNormalized;
        final float ex = left + (right - left) * endNormalized;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                final float startDistance = Math.abs(x - sx);
                final float endDistance = Math.abs(x - ex);
                final float hitRadius = dp(32);
                if (startDistance <= hitRadius) {
                    activeHandle = 0;
                } else if (endDistance <= hitRadius) {
                    activeHandle = 1;
                } else {
                    activeHandle = x <= (sx + ex) * 0.5f ? 0 : 1;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                moveHandle(fraction);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                moveHandle(fraction);
                if (event.getActionMasked() == MotionEvent.ACTION_UP
                        && Math.abs(x - downX) >= dp(2)
                        && selectionCommitListener != null) {
                    selectionCommitListener.onSelectionCommitted(
                            startNormalized, endNormalized);
                }
                activeHandle = -1;
                invalidate();
                return true;

            default:
                return true;
        }
    }

    private void moveHandle(float fraction) {
        if (activeHandle == 0) {
            startNormalized = clamp(fraction, 0f, Math.max(0f, endNormalized - 0.0001f));
        } else if (activeHandle == 1) {
            endNormalized = clamp(fraction,
                    Math.min(1f, startNormalized + 0.0001f), 1f);
        }
        invalidate();
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
