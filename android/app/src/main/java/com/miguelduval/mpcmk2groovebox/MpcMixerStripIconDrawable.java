package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Deterministic MPC-style mixer context iconography.
 *
 * This is original vector geometry: no platform glyphs or copied artwork.
 * It only communicates shell state; it owns no musical state.
 */
final class MpcMixerStripIconDrawable extends Drawable {
    enum Mode {
        PERSONAL_CHANNEL_STRIP,
        TRACK_PAD_SELECTOR
    }

    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(150, 158, 166);
    private static final int RED = Color.rgb(224, 30, 61);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Mode mode;
    private boolean selected;

    MpcMixerStripIconDrawable(Mode mode, boolean selected) {
        this.mode = mode;
        this.selected = selected;
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
    }

    void setSelected(boolean selected) {
        if (this.selected == selected) {
            return;
        }
        this.selected = selected;
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        float w = bounds.width();
        float h = bounds.height();
        float cx = bounds.exactCenterX();
        float cy = bounds.exactCenterY();
        float scale = Math.min(w, h);
        if (scale <= 0.0f) {
            return;
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, scale * 0.085f));
        paint.setColor(selected ? WHITE : MUTED);

        if (mode == Mode.PERSONAL_CHANNEL_STRIP) {
            drawEye(canvas, cx, cy, scale);
        } else {
            drawTrackPadSelector(canvas, cx, cy, scale);
        }
    }

    private void drawEye(Canvas canvas, float cx, float cy, float scale) {
        float halfW = scale * 0.36f;
        float halfH = scale * 0.21f;
        path.reset();
        path.moveTo(cx - halfW, cy);
        path.quadTo(cx, cy - halfH, cx + halfW, cy);
        path.quadTo(cx, cy + halfH, cx - halfW, cy);
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? RED : MUTED);
        canvas.drawCircle(cx, cy, scale * 0.075f, paint);

        if (!selected) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.25f, scale * 0.065f));
            paint.setColor(MUTED);
            canvas.drawLine(
                    cx - scale * 0.31f,
                    cy - scale * 0.31f,
                    cx + scale * 0.31f,
                    cy + scale * 0.31f,
                    paint);
        }
    }

    private void drawTrackPadSelector(Canvas canvas, float cx, float cy, float scale) {
        float unit = scale * 0.16f;
        float gap = scale * 0.07f;

        if (selected) {
            drawSquare(canvas, cx - unit * 0.5f, cy, unit);
        } else {
            float group = unit * 2.0f + gap;
            float left = cx - group * 0.5f + unit * 0.5f;
            float top = cy - group * 0.5f + unit * 0.5f;
            drawSquare(canvas, left, top, unit);
            drawSquare(canvas, left + unit + gap, top, unit);
            drawSquare(canvas, left, top + unit + gap, unit);
            drawSquare(canvas, left + unit + gap, top + unit + gap, unit);
        }
    }

    private void drawSquare(Canvas canvas, float cx, float cy, float size) {
        float half = size * 0.5f;
        canvas.drawRect(cx - half, cy - half, cx + half, cy + half, paint);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        invalidateSelf();
    }
}
