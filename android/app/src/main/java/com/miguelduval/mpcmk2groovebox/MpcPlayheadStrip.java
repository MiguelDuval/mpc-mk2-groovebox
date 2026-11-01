package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/**
 * Persistent transport-position indicator for the MPC shell.
 *
 * Presentation-only: it reads a normalized sequence position supplied by the
 * Activity and never receives touch input or participates in clock scheduling.
 */
final class MpcPlayheadStrip extends View {
    private static final int TRACK = 0xff262c31;
    private static final int PLAYHEAD = 0xffe04755;
    private static final int PLAYHEAD_EDGE = 0xffff7d86;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float progress;
    private boolean playing;

    MpcPlayheadStrip(Context context) {
        super(context);
        setContentDescription("MPC global sequence playhead");
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
    }

    void setState(long positionTicks, long sequenceTicks, boolean playing) {
        final long safeSequenceTicks = Math.max(1L, sequenceTicks);
        final long safePositionTicks = Math.max(0L, positionTicks);
        progress = Math.max(
                0f,
                Math.min(
                        1f,
                        safePositionTicks / (float) safeSequenceTicks));
        this.playing = playing;
        invalidate();
    }

    float progressForTest() {
        return progress;
    }

    boolean isPlayingForTest() {
        return playing;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TRACK);
        canvas.drawRect(0, 0, getWidth(), getHeight(), paint);

        final float markerX = progress * getWidth();
        paint.setColor(PLAYHEAD);
        canvas.drawRect(0, 0, markerX, getHeight(), paint);

        final float edgeWidth = Math.max(
                dp(1),
                playing ? dp(2) : dp(1));
        paint.setColor(PLAYHEAD_EDGE);
        canvas.drawRect(
                Math.max(0f, markerX - edgeWidth),
                0,
                Math.min(getWidth(), markerX + dp(1)),
                getHeight(),
                paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
