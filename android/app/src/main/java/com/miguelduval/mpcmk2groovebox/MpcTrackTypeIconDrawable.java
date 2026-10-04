package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Small original pictographic vocabulary for the six MPC Main Track Types.
 *
 * The drawable deliberately uses original line geometry rather than copied
 * Akai artwork. It keeps the Main Track Type cluster visually stable across
 * Android font/rendering differences.
 */
final class MpcTrackTypeIconDrawable extends Drawable {
    enum Type {
        DRUM,
        KEYGROUP,
        PLUGIN,
        MIDI,
        CLIP,
        CV
    }

    private static final int INACTIVE = Color.rgb(156, 166, 174);
    private static final int ACTIVE = Color.WHITE;
    private static final int DISABLED = Color.rgb(91, 99, 106);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Type type;
    private boolean selected;
    private boolean enabled = true;

    MpcTrackTypeIconDrawable(Type type) {
        this.type = type;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(1.8f);
    }

    void setSelected(boolean value) {
        if (selected == value) return;
        selected = value;
        invalidateSelf();
    }

    void setEnabledState(boolean value) {
        if (enabled == value) return;
        enabled = value;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        final RectF b = new RectF(getBounds());
        final float cx = b.centerX();
        final float cy = b.centerY();
        final float s = Math.min(b.width(), b.height()) * 0.30f;
        paint.setColor(enabled ? (selected ? ACTIVE : INACTIVE) : DISABLED);

        switch (type) {
            case DRUM:
                drum(canvas, cx, cy, s);
                break;
            case KEYGROUP:
                keygroup(canvas, cx, cy, s);
                break;
            case PLUGIN:
                plugin(canvas, cx, cy, s);
                break;
            case MIDI:
                midi(canvas, cx, cy, s);
                break;
            case CLIP:
                clip(canvas, cx, cy, s);
                break;
            case CV:
                cv(canvas, cx, cy, s);
                break;
        }
    }

    private void drum(Canvas c, float x, float y, float s) {
        final float cell = s * 0.55f;
        for (int row = -1; row <= 0; row++) {
            for (int col = -1; col <= 0; col++) {
                final float left = x + col * cell * 1.35f - cell * 0.5f;
                final float top = y + row * cell * 1.35f - cell * 0.5f;
                c.drawRect(left, top, left + cell, top + cell, paint);
            }
        }
    }

    private void keygroup(Canvas c, float x, float y, float s) {
        final float left = x - s;
        final float top = y - s;
        final float right = x + s;
        final float bottom = y + s;
        c.drawRect(left, top, right, bottom, paint);
        final float whiteKey = (right - left) / 4.0f;
        for (int i = 1; i < 4; i++) {
            final float xx = left + whiteKey * i;
            c.drawLine(xx, top, xx, bottom, paint);
        }
        c.drawLine(
                left + whiteKey * 0.62f, top,
                left + whiteKey * 0.62f, top + s * 0.58f,
                paint);
        c.drawLine(
                left + whiteKey * 1.62f, top,
                left + whiteKey * 1.62f, top + s * 0.58f,
                paint);
    }

    private void plugin(Canvas c, float x, float y, float s) {
        c.drawRoundRect(
                new RectF(x - s * 0.65f, y - s * 0.7f,
                        x + s * 0.15f, y + s * 0.7f),
                s * 0.12f, s * 0.12f, paint);
        c.drawLine(x + s * 0.15f, y - s * 0.3f, x + s * 0.95f, y - s * 0.3f, paint);
        c.drawLine(x + s * 0.15f, y + s * 0.3f, x + s * 0.95f, y + s * 0.3f, paint);
        c.drawLine(x + s * 0.95f, y - s * 0.3f, x + s * 0.95f, y + s * 0.3f, paint);
        c.drawLine(x - s * 0.95f, y, x - s * 0.65f, y, paint);
    }

    private void midi(Canvas c, float x, float y, float s) {
        c.drawCircle(x, y, s, paint);
        final float r = s * 0.18f;
        final float[] dx = {-0.48f, -0.24f, 0.0f, 0.24f, 0.48f};
        for (float offset : dx) {
            c.drawCircle(x + offset * s, y + s * 0.08f, r, paint);
        }
    }

    private void clip(Canvas c, float x, float y, float s) {
        final Path p = new Path();
        p.moveTo(x - s, y - s);
        p.lineTo(x + s * 0.35f, y - s);
        p.lineTo(x + s, y - s * 0.35f);
        p.lineTo(x + s, y + s);
        p.lineTo(x - s, y + s);
        p.close();
        c.drawPath(p, paint);
        c.drawLine(x + s * 0.35f, y - s, x + s * 0.35f, y - s * 0.35f, paint);
        c.drawLine(x + s * 0.35f, y - s * 0.35f, x + s, y - s * 0.35f, paint);
    }

    private void cv(Canvas c, float x, float y, float s) {
        final Path p = new Path();
        p.moveTo(x - s, y - s * 0.35f);
        p.lineTo(x - s * 0.35f, y + s * 0.35f);
        p.lineTo(x + s * 0.35f, y - s * 0.35f);
        p.lineTo(x + s, y + s * 0.35f);
        c.drawPath(p, paint);
        c.drawLine(x - s, y + s * 0.7f, x + s, y + s * 0.7f, paint);
    }

    @Override public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override public void setColorFilter(android.graphics.ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override public int getIntrinsicWidth() {
        return 20;
    }

    @Override public int getIntrinsicHeight() {
        return 20;
    }
}
