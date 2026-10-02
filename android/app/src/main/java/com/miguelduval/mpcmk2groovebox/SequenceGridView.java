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

    public interface ViewportListener {
        void onViewportChanged(int firstStep, int visibleSteps, int firstPad, int visiblePads);
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

    // Native grid pages are always fetched as 16×16. The viewport may expose
    // a smaller window for hardware Zoom without changing the underlying edit model.
    private final int[] velocities = new int[ROWS * COLUMNS];
    private Listener listener;
    private ViewportListener viewportListener;
    private boolean editable = true;
    private int playheadStep = -1;
    private int selectedPad = 0;
    private int firstStep = 0;
    private int visibleSteps = COLUMNS;
    private int totalSteps = COLUMNS;
    // firstPad is a visual row offset from the top (Pad 16 toward Pad 1).
    private int firstPad = 0;
    private int visiblePads = ROWS;
    private float downX;
    private float downY;
    private boolean draggingViewport;

    public SequenceGridView(Context context) {
        super(context);
        density = getResources().getDisplayMetrics().density;
        setFocusable(true);
        setContentDescription("Sequence 16 by 16 step grid");
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setViewportListener(ViewportListener listener) {
        this.viewportListener = listener;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        invalidate();
    }

    public void setViewport(
            int firstStep,
            int visibleSteps,
            int totalSteps,
            int firstPad,
            int visiblePads) {
        this.totalSteps = Math.max(1, totalSteps);
        this.visibleSteps = clamp(
                visibleSteps,
                1,
                Math.min(COLUMNS, this.totalSteps));
        this.firstStep = clamp(
                firstStep,
                0,
                Math.max(0, this.totalSteps - this.visibleSteps));
        this.visiblePads = clamp(visiblePads, 1, ROWS);
        this.firstPad = clamp(
                firstPad,
                0,
                ROWS - this.visiblePads);
        invalidate();
    }

    public int firstStepForTest() {
        return firstStep;
    }

    public int visibleStepsForTest() {
        return visibleSteps;
    }

    public int firstPadForTest() {
        return firstPad;
    }

    public int visiblePadsForTest() {
        return visiblePads;
    }

    public void setState(
            int[] sourceVelocities,
            int playheadStep,
            int selectedPad) {
        Arrays.fill(velocities, 0);
        this.selectedPad = Math.max(0, Math.min(ROWS - 1, selectedPad));
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

    public void setState(int[] sourceVelocities, int playheadStep) {
        setState(sourceVelocities, playheadStep, selectedPad);
    }

    public int selectedPadForTest() {
        return selectedPad;
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
        final float cellWidth = width / visibleSteps;
        final float cellHeight = height / visiblePads;

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(dp(9));
        paint.setColor(MUTED);

        for (int column = 0; column < visibleSteps; column++) {
            final float centerX = left + column * cellWidth + cellWidth * 0.5f;
            final int absoluteStep = firstStep + column;
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", absoluteStep + 1),
                    centerX,
                    dp(16),
                    paint);
        }

        paint.setStyle(Paint.Style.FILL);
        for (int row = 0; row < visiblePads; row++) {
            final float centerY = top + row * cellHeight + cellHeight * 0.5f;
            final int padIndex = (ROWS - 1) - (firstPad + row);
            paint.setColor(TEXT);
            canvas.drawText(
                    String.format(Locale.ROOT, "%02d", padIndex + 1),
                    dp(21),
                    centerY + dp(3),
                    paint);
        }

        for (int row = 0; row < visiblePads; row++) {
            final int padIndex = (ROWS - 1) - (firstPad + row);
            final boolean selectedRow = padIndex == selectedPad;
            for (int column = 0; column < visibleSteps; column++) {
                final float x0 = left + column * cellWidth + dp(1);
                final float y0 = top + row * cellHeight + dp(1);
                final float x1 = left + (column + 1) * cellWidth - dp(1);
                final float y1 = top + (row + 1) * cellHeight - dp(1);

                final int velocity =
                        velocities[padIndex * COLUMNS + column];
                paint.setStyle(Paint.Style.FILL);

                if (velocity > 0) {
                    paint.setColor(ACCENT);
                } else if (selectedRow) {
                    paint.setColor(Color.rgb(38, 50, 55));
                } else {
                    paint.setColor((column % 4 == 0) ? SURFACE_2 : SURFACE);
                }
                rect.set(x0, y0, x1, y1);
                canvas.drawRoundRect(rect, dp(3), dp(3), paint);

                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(LINE);
                rect.set(x0, y0, x1, y1);
                canvas.drawRoundRect(rect, dp(3), dp(3), paint);

                final int absoluteStep = firstStep + column;
                if (playheadStep == absoluteStep) {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(dp(2));
                    paint.setColor(DANGER);
                    rect.set(x0, y0, x1, y1);
                    canvas.drawRoundRect(rect, dp(3), dp(3), paint);
                }
            }
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LINE);
        for (int column = 4; column < visibleSteps; column += 4) {
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

        final float left = dp(42);
        final float top = dp(24);
        final float width = Math.max(1f, getWidth() - left - dp(6));
        final float height = Math.max(1f, getHeight() - top - dp(6));

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
            downY = event.getY();
            draggingViewport = false;
            return true;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            final float dx = event.getX() - downX;
            final float dy = event.getY() - downY;
            if (!draggingViewport
                    && Math.max(Math.abs(dx), Math.abs(dy)) <= dp(14)) {
                return true;
            }

            if (!draggingViewport) {
                draggingViewport = visibleSteps < COLUMNS
                        || visiblePads < ROWS;
            }
            if (!draggingViewport || viewportListener == null) {
                return true;
            }

            final float cellWidth = width / visibleSteps;
            final float cellHeight = height / visiblePads;
            final boolean horizontalGesture = Math.abs(dx) >= Math.abs(dy);
            final int stepShift = horizontalGesture && visibleSteps < COLUMNS
                    ? Math.round(-dx / Math.max(1f, cellWidth))
                    : 0;
            final int padShift = !horizontalGesture && visiblePads < ROWS
                    ? Math.round(-dy / Math.max(1f, cellHeight))
                    : 0;
            if (stepShift == 0 && padShift == 0) {
                return true;
            }
            final int nextFirstStep = clamp(
                    firstStep + stepShift,
                    0,
                    Math.max(0, totalSteps - visibleSteps));
            final int nextFirstPad = clamp(
                    firstPad + padShift,
                    0,
                    ROWS - visiblePads);
            if (nextFirstStep != firstStep || nextFirstPad != firstPad) {
                viewportListener.onViewportChanged(
                        nextFirstStep,
                        visibleSteps,
                        nextFirstPad,
                        visiblePads);
                downX = event.getX();
                downY = event.getY();
            }
            return true;
        }

        if (event.getActionMasked() != MotionEvent.ACTION_UP) {
            return true;
        }

        if (draggingViewport) {
            draggingViewport = false;
            return true;
        }

        if (event.getX() < left || event.getY() < top
                || event.getX() >= left + width
                || event.getY() >= top + height) {
            return true;
        }

        if (Math.abs(event.getX() - downX) > dp(14)
                || Math.abs(event.getY() - downY) > dp(14)) {
            return true;
        }

        final float cellWidth = width / visibleSteps;
        final float cellHeight = height / visiblePads;
        final int column = Math.min(
                visibleSteps - 1,
                Math.max(0, (int) ((event.getX() - left) / cellWidth)));
        final int row = Math.min(
                visiblePads - 1,
                Math.max(0, (int) ((event.getY() - top) / cellHeight)));

        final int padIndex = (ROWS - 1) - (firstPad + row);
        final int stepIndex = firstStep + column;
        listener.onCellTapped(padIndex, stepIndex);
        return true;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
