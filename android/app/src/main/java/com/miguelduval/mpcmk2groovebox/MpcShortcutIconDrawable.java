package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Small original line-icon set for the five configurable MPC shortcut slots.
 *
 * These are intentionally not copied Akai artwork; they reproduce the same
 * pictographic vocabulary so the Android font/rendering stack cannot change
 * the visual identity of the rail.
 */
final class MpcShortcutIconDrawable extends Drawable {
    private static final int INACTIVE = Color.rgb(156, 166, 174);
    private static final int ACTIVE = Color.WHITE;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final MpcUiState.Mode mode;
    private boolean selected;

    MpcShortcutIconDrawable(MpcUiState.Mode mode) {
        this.mode = mode;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStrokeWidth(2.2f);
    }

    void setSelected(boolean value) {
        if (selected == value) return;
        selected = value;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        final RectF b = new RectF(getBounds());
        final float cx = b.centerX();
        final float cy = b.centerY();
        final float w = b.width();
        final float h = b.height();
        final float s = Math.min(w, h) * 0.32f;
        paint.setColor(selected ? ACTIVE : INACTIVE);

        switch (mode == null ? MpcUiState.Mode.RESERVED : mode) {
            case MAIN:
                home(canvas, cx, cy, s);
                break;
            case BROWSER:
                folder(canvas, cx, cy, s);
                break;
            case GRID:
                grid(canvas, cx, cy, s);
                break;
            case STEP:
                steps(canvas, cx, cy, s);
                break;
            case TRACK_VIEW:
                tracks(canvas, cx, cy, s);
                break;
            case TRACK_EDIT:
                pencil(canvas, cx, cy, s);
                break;
            case SAMPLE_EDIT:
                waveform(canvas, cx, cy, s);
                break;
            case SAMPLER:
                sampler(canvas, cx, cy, s);
                break;
            case CHANNEL_MIXER:
                mixer(canvas, cx, cy, s);
                break;
            case PAD_MIXER:
                padGrid(canvas, cx, cy, s);
                break;
            case LEVELS_16:
                levelGrid(canvas, cx, cy, s);
                break;
            case PAD_PERFORM:
                cross(canvas, cx, cy, s);
                break;
            case NEXT_SEQUENCE:
                play(canvas, cx, cy, s);
                break;
            case ARRANGE:
                arrange(canvas, cx, cy, s);
                break;
            case LIST_EDIT:
                list(canvas, cx, cy, s);
                break;
            case PROJECT:
                document(canvas, cx, cy, s);
                break;
            case MENU:
                grid(canvas, cx, cy, s);
                break;
            case SOUNDS:
                note(canvas, cx, cy, s);
                break;
            case XYFX:
                xy(canvas, cx, cy, s);
                break;
            default:
                square(canvas, cx, cy, s);
                break;
        }
    }

    private void home(Canvas c, float x, float y, float s) {
        final PathProxy p = new PathProxy();
        p.moveTo(x - s, y);
        p.lineTo(x, y - s);
        p.lineTo(x + s, y);
        p.moveTo(x - s * 0.72f, y - 0.05f * s);
        p.lineTo(x - s * 0.72f, y + s);
        p.lineTo(x + s * 0.72f, y + s);
        p.lineTo(x + s * 0.72f, y - 0.05f * s);
        p.stroke(c, paint);
    }

    private void folder(Canvas c, float x, float y, float s) {
        c.drawRect(x - s, y - s * .55f, x + s, y + s * .65f, paint);
        c.drawLine(x - s, y - s * .55f, x - s * .35f, y - s * .55f, paint);
        c.drawLine(x - s * .35f, y - s * .55f, x - s * .1f, y - s * .2f, paint);
        c.drawLine(x - s * .1f, y - s * .2f, x + s, y - s * .2f, paint);
    }

    private void grid(Canvas c, float x, float y, float s) {
        for (int r = -1; r <= 1; r++) {
            for (int col = -1; col <= 1; col++) {
                c.drawRect(
                        x + col * s * .55f - s * .16f,
                        y + r * s * .55f - s * .16f,
                        x + col * s * .55f + s * .16f,
                        y + r * s * .55f + s * .16f,
                        paint);
            }
        }
    }

    private void steps(Canvas c, float x, float y, float s) {
        for (int i = 0; i < 4; i++) {
            final float xx = x - s * .8f + i * s * .55f;
            final float hh = s * (.45f + i * .38f);
            c.drawLine(xx, y + s * .8f, xx, y + s * .8f - hh, paint);
        }
    }

    private void tracks(Canvas c, float x, float y, float s) {
        for (int i = 0; i < 3; i++) {
            final float yy = y - s * .65f + i * s * .65f;
            c.drawLine(x - s, yy, x + s, yy, paint);
            c.drawCircle(x - s * .35f + i * s * .1f, yy, s * .12f, paint);
        }
    }

    private void pencil(Canvas c, float x, float y, float s) {
        c.save();
        c.rotate(-38, x, y);
        c.drawRect(x - s * .15f, y - s, x + s * .15f, y + s * .55f, paint);
        c.drawLine(x - s * .15f, y + s * .55f, x, y + s, paint);
        c.restore();
    }

    private void waveform(Canvas c, float x, float y, float s) {
        final float[] v = {.2f, .55f, .95f, .35f, .75f, .45f, .85f, .3f};
        float lastX = x - s;
        float lastY = y - v[0] * s;
        for (int i = 1; i < v.length; i++) {
            final float xx = x - s + (2 * s * i / (v.length - 1));
            final float yy = y - v[i] * s;
            c.drawLine(lastX, lastY, xx, yy, paint);
            lastX = xx;
            lastY = yy;
        }
    }

    private void sampler(Canvas c, float x, float y, float s) {
        c.drawCircle(x, y, s * .72f, paint);
        c.drawCircle(x, y, s * .18f, paint);
    }

    private void mixer(Canvas c, float x, float y, float s) {
        for (int i = 0; i < 3; i++) {
            final float xx = x - s * .72f + i * s * .72f;
            c.drawLine(xx, y - s, xx, y + s, paint);
            final float yy = y + (i == 1 ? -.3f : i == 2 ? .35f : .1f) * s;
            c.drawCircle(xx, yy, s * .18f, paint);
        }
    }

    private void padGrid(Canvas c, float x, float y, float s) {
        for (int r = -1; r <= 1; r++) {
            for (int col = -1; col <= 1; col++) {
                c.drawRoundRect(
                        new RectF(
                                x + col * s * .63f - s * .2f,
                                y + r * s * .63f - s * .2f,
                                x + col * s * .63f + s * .2f,
                                y + r * s * .63f + s * .2f),
                        1, 1, paint);
            }
        }
    }

    private void levelGrid(Canvas c, float x, float y, float s) {
        for (int r = -1; r <= 1; r++) {
            for (int col = -1; col <= 1; col++) {
                c.drawCircle(
                        x + col * s * .6f,
                        y + r * s * .6f,
                        s * .11f,
                        paint);
            }
        }
    }

    private void cross(Canvas c, float x, float y, float s) {
        c.drawCircle(x, y, s * .75f, paint);
        c.drawLine(x - s, y, x + s, y, paint);
        c.drawLine(x, y - s, x, y + s, paint);
    }

    private void play(Canvas c, float x, float y, float s) {
        final PathProxy p = new PathProxy();
        p.moveTo(x - s * .55f, y - s);
        p.lineTo(x + s * .9f, y);
        p.lineTo(x - s * .55f, y + s);
        p.close();
        p.stroke(c, paint);
    }

    private void arrange(Canvas c, float x, float y, float s) {
        c.drawLine(x - s, y - s * .7f, x + s, y - s * .7f, paint);
        c.drawLine(x - s, y, x + s * .45f, y, paint);
        c.drawLine(x - s, y + s * .7f, x + s, y + s * .7f, paint);
        c.drawRect(x + s * .45f, y - s * .15f, x + s * .9f, y + s * .15f, paint);
    }

    private void list(Canvas c, float x, float y, float s) {
        for (int i = 0; i < 3; i++) {
            final float yy = y - s * .65f + i * s * .65f;
            c.drawLine(x - s * .35f, yy, x + s, yy, paint);
            c.drawCircle(x - s * .7f, yy, s * .1f, paint);
        }
    }

    private void document(Canvas c, float x, float y, float s) {
        c.drawRect(x - s * .78f, y - s, x + s * .78f, y + s, paint);
        c.drawLine(x - s * .45f, y - s * .35f, x + s * .4f, y - s * .35f, paint);
        c.drawLine(x - s * .45f, y + s * .1f, x + s * .4f, y + s * .1f, paint);
    }

    private void note(Canvas c, float x, float y, float s) {
        c.drawLine(x + s * .35f, y - s, x + s * .35f, y + s * .35f, paint);
        c.drawLine(x + s * .35f, y - s, x + s, y - s * .65f, paint);
        c.drawCircle(x - s * .05f, y + s * .5f, s * .35f, paint);
        c.drawCircle(x + s * .35f, y + s * .35f, s * .35f, paint);
        c.drawLine(x - s * .05f, y + s * .5f, x + s * .35f, y + s * .35f, paint);
    }

    private void xy(Canvas c, float x, float y, float s) {
        c.drawLine(x - s, y + s, x + s, y - s, paint);
        c.drawLine(x - s, y - s, x + s, y + s, paint);
        c.drawCircle(x, y, s * .25f, paint);
    }

    private void square(Canvas c, float x, float y, float s) {
        c.drawRect(x - s, y - s, x + s, y + s, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) {
        paint.setColorFilter(filter);
    }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 24; }
    @Override public int getIntrinsicHeight() { return 24; }

    private static final class PathProxy {
        private final android.graphics.Path path = new android.graphics.Path();
        void moveTo(float x, float y) { path.moveTo(x, y); }
        void lineTo(float x, float y) { path.lineTo(x, y); }
        void close() { path.close(); }
        void stroke(Canvas canvas, Paint paint) { canvas.drawPath(path, paint); }
    }
}
