package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Thin always-visible sequence context strip.
 *
 * This view is intentionally read-only: it consumes the existing sequence
 * transport state and never talks to the audio engine directly.
 */
public final class SequenceOverviewView extends View {
    private static final int BG = 0xff0d1012;
    private static final int TRACK = 0xff242a2f;
    private static final int RED = 0xffd84a55;
    private static final int RED_DIM = 0x7fc33f49;
    private static final int PLAYHEAD = 0xffffffff;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int barCount = 1;
    private int loopStartBar = 1;
    private int loopEndBar = 1;
    private int numerator = 4;
    private int denominator = 4;
    private long positionTicks;
    private boolean loopEnabled = true;
    private boolean playing;

    public SequenceOverviewView(Context context) {
        super(context);
        init();
    }

    public SequenceOverviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SequenceOverviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setFocusable(false);
        setWillNotDraw(false);
        paint.setStrokeCap(Paint.Cap.BUTT);
    }

    public void setState(
            int sequenceIndex,
            int sequenceCount,
            int barCount,
            int loopStartBar,
            int loopEndBar,
            int numerator,
            int denominator,
            boolean loopEnabled,
            long positionTicks,
            boolean playing) {
        this.barCount = Math.max(1, barCount);
        this.loopStartBar = clampBar(loopStartBar);
        this.loopEndBar = clampBar(loopEndBar);
        if (this.loopStartBar > this.loopEndBar) {
            this.loopStartBar = this.loopEndBar;
        }
        this.numerator = Math.max(1, numerator);
        this.denominator = Math.max(1, denominator);
        this.loopEnabled = loopEnabled;
        this.positionTicks = Math.max(0L, positionTicks);
        this.playing = playing;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // This is intentionally a compact visual timeline, not a second
        // transport readout. Exact sequence/position text lives in the
        // global transport bar; this strip communicates movement at a glance.
        final float left = dp(8);
        final float right = getWidth() - dp(8);
        final float centerY = getHeight() * 0.5f;
        final float top = Math.max(dp(1), centerY - dp(2));
        final float bottom = Math.min(getHeight() - dp(1), centerY + dp(2));
        final float width = Math.max(1f, right - left);

        canvas.drawColor(BG);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TRACK);
        canvas.drawRect(left, top, right, bottom, paint);

        if (loopEnabled) {
            final float loopLeft = left + width * (loopStartBar - 1f) / barCount;
            final float loopRight = left + width * loopEndBar / (float) barCount;
            paint.setColor(RED_DIM);
            canvas.drawRect(loopLeft, top, Math.max(loopLeft + dp(1), loopRight), bottom, paint);
        }

        final double ticksPerBar =
                Math.max(1.0, numerator * 4.0 * 960.0 / denominator);
        final double sequenceTicks = Math.max(
                ticksPerBar,
                ticksPerBar * barCount);
        final float playhead = (float) clamp(
                positionTicks / sequenceTicks,
                0.0,
                1.0);
        final float playheadX = left + width * playhead;

        paint.setColor(RED);
        canvas.drawRect(left, top, playheadX, bottom, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(playing ? dp(1.5f) : dp(1));
        paint.setColor(playing ? PLAYHEAD : RED_DIM);
        canvas.drawLine(
                playheadX,
                Math.max(0f, top - dp(1)),
                playheadX,
                Math.min(getHeight(), bottom + dp(1)),
                paint);
    }

    private void drawText(
            Canvas canvas,
            String value,
            float x,
            float baseline,
            int color,
            float size,
            boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(dp(size));
        paint.setTypeface(bold
                ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        canvas.drawText(value, x, baseline, paint);
    }

    private int clampBar(int bar) {
        return Math.max(1, Math.min(barCount, bar));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(int value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
