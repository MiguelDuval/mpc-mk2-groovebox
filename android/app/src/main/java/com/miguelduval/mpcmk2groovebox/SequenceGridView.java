package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;
import java.util.Locale;

public final class SequenceGridView extends View {
    public interface Listener {
        void onCellTapped(int padIndex, int stepIndex);
    }

    private static final int ROWS = 16;
    private static final int COLUMNS = 16;
    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);
    private static final int SURFACE_2 = Color.rgb(32, 37, 42);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int ACCENT = Color.rgb(69, 211, 255);
    private static final int DANGER = Color.rgb(236, 83, 83);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;

    private final int[] velocities = new int[ROWS * COLUMNS];
    private Listener listener;
    private boolean editable = true;
    private int playheadStep = -1;
    private float downX;
    private float downY;

    public SequenceGridView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setFocusable(true);
        setContentDescription("Sequence 16 by 16 step grid");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        invalidate();
    }

    public void setState(int[] sourceVelocities, int playheadStep) {
        Arrays.fill(velocities, 0);
        if (sourceVelocities != null) {
            System.arraycopy(
                    sourceVelocities,
                    0,
                    velocities,
                    0,
                    Math.min(sourceVelocities.length, velocities.length));
        }
        this.playheadStep = playheadStep;
        invalidate();
    }

    private float dp(float value) {
        return value * density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawColor(BG);

        final float left = dp(42);
        final float top = dp(24);
        final float width = Math.max(1f, getWidth() - left - dp(6));
        final float height = Math.max(1f, getHeight() - top - dp(6));
        final float cellWidth = width / COLUMNS;
        final float cellHeight = height / ROWS;

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(9));
        paint.setColor(MUTED);

        for (int column = 0; column < COLUMNS; column++) {
            final float centerX = left + column * cellWidth + cellWidth * 0.5f;
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", column + 1),
                    centerX,
                    dp(16),
                    paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < ROWS; row++) {
            final float centerY = top + row * cellHeight + cellHeight * 0.5f;
            paint.setColor(TEXT);
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", ROWS - row),
                    dp(21),
                    centerY + dp(3),
                    paint);
        }

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                final float x0 = left + column * cellWidth + dp(1);
                final float y0 = top + row * cellHeight + dp(1);
                final float x1 = left + (column + 1) * cellWidth - dp(1);
                final float y1 = top + (row + 1) * cellHeight - dp(1);

                final int velocity = velocities[row * COLUMNS + column];
                paint.setStyle(Paint.Style.FILL);

                if (velocity > 0) {
                    paint.setColor(ACCENT);
                } else {
                    paint.setColor((column % 4 == 0) ? SURFACE_2 : SURFACE);
                }
                canvas.drawRoundRect(
                        rect.set(x0, y0, x1, y1),
                        dp(3),
                        dp(3),
                        paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(LINE);
                canvas.drawRoundRect(
                        rect.set(x0, y0, x1, y1),
                        dp(3),
                        dp(3),
                        paint);

                if (playheadStep == column) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(DANGER);
                    canvas.drawRoundRect(
                            rect.set(x0, y0, x1, y1),
                            dp(3),
                            dp(3),
                            paint);
                }
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LINE);
        for (int column = 4; column < COLUMNS; column += 4) {
            final float x = left + column * cellWidth;
            canvas.drawRect(x - dp(1), top, x + dp(1), top + height, paint);
        }

        if (!editable) {
            paint.setColor(0xB8141012);
            canvas.drawRect(left, top, left + width, top + height, paint);
            paint.setColor(MUTED);
            paint.setTextSize(dp(12));
            canvas.drawText(
                    "STOP PLAYBACK TO EDIT",
                    left + width * 0.5f,
                    top + height * 0.5f + dp(4),
                    paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!editable || listener == null) {
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            return true;
        }

        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return true;
        }

        final float left = dp(42);
        final float top = dp(24);
        final float width = Math.max(1f, getWidth() - left - dp(6));
        final float height = Math.max(1f, getHeight() - top - dp(6));

        if (event.getX() < left || event.getY() < top
                || event.getX() >= left + width
                || event.getY() >= top + height) {
            return true;
        }

        if (Math.abs(event.getX() - downX) > dp(14)
                || Math.abs(event.getY() - downY) > dp(14)) {
            return true;
        }

        final int column = Math.min(
                COLUMNS - 1,
                Math.max(0, (int) ((event.getX() - left)
                        / (width / COLUMNS))));
        final int row = Math.min(
                ROWS - 1,
                Math.max(0, (int) ((event.getY() - top)
                        / (height / ROWS))));

        listener.onCellTapped(ROWS - 1 - row, column);
        return true;
    }
}
