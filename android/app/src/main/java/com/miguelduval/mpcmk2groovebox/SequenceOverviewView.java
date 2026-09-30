package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

/**
 * Thin always-visible sequence context strip.
 *
 * This view is intentionally read-only: it consumes the existing sequence
 * transport state and never talks to the audio engine directly.
 */
public final class SequenceOverviewView extends View {
    private static final int BG = 0xff0d1012;
    private static final int TRACK = 0xff242a2f;
    private static final int GRID = 0xff465057;
    private static final int TEXT = 0xffd8dee2;
    private static final int MUTED = 0xff8e999f;
    private static final int RED = 0xffd84a55;
    private static final int RED_DIM = 0x7fc33f49;
    private static final int PLAYHEAD = 0xffffffff;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int sequenceIndex;
    private int sequenceCount;
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
        this.sequenceIndex = Math.max(0, sequenceIndex);
        this.sequenceCount = Math.max(0, sequenceCount);
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

        final float labelLeft = dp(8);
        final float timelineLeft = dp(58);
        final float labelRight = getWidth() - dp(70);
        final float timelineRight = getWidth() - dp(58);
        final float centerY = getHeight() * 0.5f;
        final float top = Math.max(dp(4), centerY - dp(5));
        final float bottom = Math.min(getHeight() - dp(4), centerY + dp(5));
        final float width = Math.max(1f, timelineRight - timelineLeft);

        canvas.drawColor(BG);

        drawText(
                canvas,
                String.format(
                        Locale.ROOT,
                        "S%02d/%02d",
                        sequenceIndex + 1,
                        Math.max(1, sequenceCount)),
                labelLeft,
                centerY + dp(3),
                TEXT,
                9,
                true);

        final String position = formatPosition(positionTicks);
        drawText(
                canvas,
                position,
                labelRight,
                centerY + dp(3),
                playing ? TEXT : MUTED,
                9,
                true);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TRACK);
        canvas.drawRoundRect(
                timelineLeft,
                top,
                timelineRight,
                bottom,
                dp(2),
                dp(2),
                paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(dp(1), 0.75f));
        paint.setColor(GRID);
        for (int bar = 0; bar <= barCount; bar++) {
            final float x = timelineLeft + width * bar / (float) barCount;
            canvas.drawLine(x, top - dp(2), x, bottom + dp(2), paint);
        }

        if (loopEnabled) {
            final float loopLeft = timelineLeft
                    + width * (loopStartBar - 1f) / barCount;
            final float loopRight = timelineLeft
                    + width * loopEndBar / (float) barCount;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(RED_DIM);
            canvas.drawRoundRect(
                    loopLeft,
                    top,
                    Math.max(loopLeft + dp(2), loopRight),
                    bottom,
                    dp(2),
                    dp(2),
                    paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(RED);
            canvas.drawLine(loopLeft, top - dp(2), loopLeft, bottom + dp(2), paint);
            canvas.drawLine(loopRight, top - dp(2), loopRight, bottom + dp(2), paint);
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
        final float playheadX = timelineLeft + width * playhead;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);
        canvas.drawRect(timelineLeft, top, playheadX, bottom, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(playing ? dp(2) : dp(1));
        paint.setColor(PLAYHEAD);
        canvas.drawLine(
                playheadX,
                top - dp(3),
                playheadX,
                bottom + dp(3),
                paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);
        canvas.drawCircle(playheadX, centerY, dp(2), paint);
    }

    private String formatPosition(long ticks) {
        final long ticksPerBeat = Math.max(
                1L,
                Math.round(4.0 * 960.0 / denominator));
        final long ticksPerBar = Math.max(
                ticksPerBeat,
                ticksPerBeat * numerator);
        long normalized = Math.max(0L, ticks);
        final int bar = (int) (normalized / ticksPerBar) + 1;
        normalized %= ticksPerBar;
        final int beat = (int) (normalized / ticksPerBeat) + 1;
        final int tick = (int) (normalized % ticksPerBeat);
        return String.format(Locale.ROOT, "%03d.%d.%03d", bar, beat, tick);
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
}
