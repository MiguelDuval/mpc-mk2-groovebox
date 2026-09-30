package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
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
    private float playheadNormalized;
    private int activeHandle = -1;
    private OnLoopCommitListener loopCommitListener;

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
        if (bar <= 0f) {
            playheadNormalized = 0f;
        } else {
            playheadNormalized = clamp((bar - 1f) / Math.max(1f, barCount), 0f, 1f);
        }
        invalidate();
    }

    public void setOnLoopCommitListener(OnLoopCommitListener listener) {
        loopCommitListener = listener;
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

        fillPaint.setColor(RANGE);
        final float rangeLeft = left + width * (loopStartBar - 1f) / barCount;
        final float rangeRight = left + width * loopEndBar / barCount;
        canvas.drawRect(rangeLeft, top, rangeRight, bottom, fillPaint);

        linePaint.setStrokeWidth(dp(1));
        linePaint.setColor(GRID);
        for (int bar = 0; bar <= barCount; bar++) {
            final float x = left + width * bar / barCount;
            canvas.drawLine(x, top, x, bottom, linePaint);
        }

        linePaint.setColor(RANGE_EDGE);
        linePaint.setStrokeWidth(dp(2));
        canvas.drawLine(rangeLeft, top, rangeLeft, bottom, linePaint);
        canvas.drawLine(rangeRight, top, rangeRight, bottom, linePaint);

        textPaint.setColor(TEXT);
        textPaint.setTextSize(dp(10));
        for (int bar = 1; bar <= barCount; bar++) {
            final float x = left + width * (bar - 1f) / barCount;
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", bar),
                    x + dp(4),
                    dp(13),
                    textPaint);
        }

        final float playheadX = left + width * playheadNormalized;
        linePaint.setColor(PLAYHEAD);
        linePaint.setStrokeWidth(dp(1));
        canvas.drawLine(playheadX, top, playheadX, bottom, linePaint);

        drawHandle(canvas, rangeLeft, top, "IN");
        drawHandle(canvas, rangeRight, top, "OUT");
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
        final float left = dp(12);
        final float right = getWidth() - dp(12);
        final float width = Math.max(1f, right - left);
        final float startX = left + width * (loopStartBar - 1f) / barCount;
        final float endX = left + width * loopEndBar / barCount;

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
        final int bar = clampBar(1 + Math.round(
                clamp((x - left) / width, 0f, 0.9999f) * barCount));
        if (activeHandle == 0) {
            loopStartBar = Math.min(bar, loopEndBar);
        } else if (activeHandle == 1) {
            loopEndBar = Math.max(bar, loopStartBar);
        }
        invalidate();
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
