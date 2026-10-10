package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Deterministic, original vector glyphs for high-frequency Main affordances.
 * Geometry is intentionally simple and legible at small MPC-style touch sizes.
 */
final class MpcMainIconDrawable extends Drawable {
    enum Mode {
        PENCIL,
        LOOP,
        PLAY,
        MENU,
        CLOSE,
        PREVIOUS,
        NEXT
    }

    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(150, 158, 166);
    private static final int RED = Color.rgb(224, 30, 61);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Mode mode;
    private boolean selected;

    MpcMainIconDrawable(Mode mode, boolean selected) {
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
        final Rect bounds = getBounds();
        final float w = bounds.width();
        final float h = bounds.height();
        final float cx = bounds.exactCenterX();
        final float cy = bounds.exactCenterY();
        final float scale = Math.min(w, h);
        if (scale <= 0.0f) {
            return;
        }

        paint.setAntiAlias(true);
        paint.setColor(selected ? WHITE : MUTED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, scale * 0.08f));

        switch (mode) {
            case PENCIL:
                drawPencil(canvas, cx, cy, scale);
                break;
            case LOOP:
                drawLoop(canvas, cx, cy, scale);
                break;
            case PLAY:
                drawPlay(canvas, cx, cy, scale);
                break;
            case MENU:
                drawMenu(canvas, cx, cy, scale);
                break;
            case CLOSE:
                drawClose(canvas, cx, cy, scale);
                break;
            case PREVIOUS:
                drawChevron(canvas, cx, cy, scale, false);
                break;
            case NEXT:
                drawChevron(canvas, cx, cy, scale, true);
                break;
        }
    }

    private void drawPencil(Canvas canvas, float cx, float cy, float scale) {
        final float length = scale * 0.55f;
        final float x1 = cx - length * 0.5f;
        final float y1 = cy + length * 0.30f;
        final float x2 = cx + length * 0.5f;
        final float y2 = cy - length * 0.30f;

        canvas.save();
        canvas.rotate(-8.0f, cx, cy);
        canvas.drawLine(x1, y1, x2, y2, paint);
        path.reset();
        path.moveTo(x2, y2);
        path.lineTo(x2 + scale * 0.12f, y2 - scale * 0.12f);
        path.lineTo(x2 + scale * 0.16f, y2 - scale * 0.04f);
        path.lineTo(x2 + scale * 0.04f, y2 + scale * 0.08f);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? RED : MUTED);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(selected ? WHITE : MUTED);
        canvas.restore();
    }

    private void drawLoop(Canvas canvas, float cx, float cy, float scale) {
        final float radius = scale * 0.28f;
        final RectFCompat oval = new RectFCompat(
                cx - radius, cy - radius, cx + radius, cy + radius);
        canvas.drawArc(oval.left, oval.top, oval.right, oval.bottom, -55.0f, 275.0f, false, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? RED : MUTED);
        path.reset();
        path.moveTo(cx + radius * 0.88f, cy - radius * 0.85f);
        path.lineTo(cx + radius * 1.25f, cy - radius * 0.76f);
        path.lineTo(cx + radius * 1.00f, cy - radius * 0.46f);
        path.close();
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(selected ? WHITE : MUTED);
    }

    private void drawPlay(Canvas canvas, float cx, float cy, float scale) {
        final float halfH = scale * 0.30f;
        final float halfW = scale * 0.30f;
        path.reset();
        path.moveTo(cx - halfW * 0.65f, cy - halfH);
        path.lineTo(cx + halfW, cy);
        path.lineTo(cx - halfW * 0.65f, cy + halfH);
        path.close();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(selected ? WHITE : MUTED);
        canvas.drawPath(path, paint);
    }

    private void drawMenu(Canvas canvas, float cx, float cy, float scale) {
        final float unit = scale * 0.12f;
        final float gap = scale * 0.10f;
        final float start = -(unit + gap);
        paint.setStyle(Paint.Style.STROKE);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                final float x = cx + start + col * (unit + gap);
                final float y = cy + start + row * (unit + gap);
                canvas.drawRect(x, y, x + unit, y + unit, paint);
            }
        }
    }

    private void drawClose(Canvas canvas, float cx, float cy, float scale) {
        final float arm = scale * 0.27f;
        paint.setStrokeCap(Paint.Cap.SQUARE);
        canvas.drawLine(cx - arm, cy - arm, cx + arm, cy + arm, paint);
        canvas.drawLine(cx + arm, cy - arm, cx - arm, cy + arm, paint);
    }

    private void drawChevron(
            Canvas canvas,
            float cx,
            float cy,
            float scale,
            boolean next) {
        final float arm = scale * 0.23f;
        final float tipX = cx + (next ? arm * 0.80f : -arm * 0.80f);
        final float backX = cx - (next ? arm * 0.35f : -arm * 0.35f);
        path.reset();
        path.moveTo(backX, cy - arm);
        path.lineTo(tipX, cy);
        path.lineTo(backX, cy + arm);
        canvas.drawPath(path, paint);
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

    private static final class RectFCompat {
        final float left;
        final float top;
        final float right;
        final float bottom;

        RectFCompat(float left, float top, float right, float bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
