package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Locale;

public final class SequenceLauncherView extends View {
    public interface Listener {
        void onSequenceTapped(int sequenceIndex);
    }

    private static final int CELLS = 16;
    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int ACCENT = Color.rgb(69, 211, 255);
    private static final int QUEUED = Color.rgb(255, 180, 72);
    private static final int ACTIVE = Color.rgb(63, 207, 117);
    private static final int DANGER = Color.rgb(236, 83, 83);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private Listener listener;
    private int sequenceCount;
    private int activeSequence = -1;
    private int queuedSequence = -1;
    private int bank;

    public SequenceLauncherView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setFocusable(true);
        setContentDescription("Sequence live launcher");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setState(
            int sequenceCount,
            int activeSequence,
            int queuedSequence,
            int bank) {
        this.sequenceCount = Math.max(0, sequenceCount);
        this.activeSequence = activeSequence;
        this.queuedSequence = queuedSequence;
        final int maxBank = Math.max(0, (this.sequenceCount - 1) / CELLS);
        this.bank = Math.max(0, Math.min(maxBank, bank));
        invalidate();
    }

    public int getBank() {
        return bank;
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG);

        final float left = dp(6);
        final float top = dp(8);
        final float width = Math.max(1f, getWidth() - dp(12));
        final float height = Math.max(1f, getHeight() - dp(16));
        final float gap = dp(6);
        final float cellWidth = (width - gap * 3f) / 4f;
        final float cellHeight = (height - gap * 3f) / 4f;

        for (int index = 0; index < CELLS; index++) {
            final int sequenceIndex = bank * CELLS + index;
            final int row = index / 4;
            final int column = index % 4;

            final float x0 = left + column * (cellWidth + gap);
            final float y0 = top + row * (cellHeight + gap);
            final float x1 = x0 + cellWidth;
            final float y1 = y0 + cellHeight;

            final boolean exists = sequenceIndex < sequenceCount;
            final boolean active = sequenceIndex == activeSequence;
            final boolean queued = sequenceIndex == queuedSequence;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(
                    !exists
                            ? BG
                            : active
                                ? ACTIVE
                                : queued
                                    ? QUEUED
                                    : SURFACE);
            canvas.drawRoundRect(
                    rect.set(x0, y0, x1, y1),
                    dp(8),
                    dp(8),
                    paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(
                    active
                            ? ACCENT
                            : queued
                                ? DANGER
                                : LINE);
            canvas.drawRoundRect(
                    rect.set(x0, y0, x1, y1),
                    dp(8),
                    dp(8),
                    paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(14));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(exists ? TEXT : MUTED);
            canvas.drawText(
                    exists
                            ? String.format(Locale.ROOT, "S%02d", sequenceIndex + 1)
                            : "—",
                    x0 + cellWidth * 0.5f,
                    y0 + cellHeight * 0.5f + dp(5),
                    paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP
                || listener == null) {
            return true;
        }

        final float left = dp(6);
        final float top = dp(8);
        final float width = Math.max(1f, getWidth() - dp(12));
        final float height = Math.max(1f, getHeight() - dp(16));
        final float gap = dp(6);
        final float cellWidth = (width - gap * 3f) / 4f;
        final float cellHeight = (height - gap * 3f) / 4f;

        if (event.getX() < left || event.getY() < top) return true;

        final int column = Math.min(
                3,
                Math.max(0, (int) ((event.getX() - left)
                        / (cellWidth + gap))));
        final int row = Math.min(
                3,
                Math.max(0, (int) ((event.getY() - top)
                        / (cellHeight + gap))));
        final float cellX = event.getX()
                - (left + column * (cellWidth + gap));
        final float cellY = event.getY()
                - (top + row * (cellHeight + gap));

        if (cellX > cellWidth || cellY > cellHeight) {
            return true;
        }

        final int sequenceIndex = bank * CELLS + row * 4 + column;
        if (sequenceIndex < 0 || sequenceIndex >= sequenceCount) {
            return true;
        }

        listener.onSequenceTapped(sequenceIndex);
        return true;
    }
}
