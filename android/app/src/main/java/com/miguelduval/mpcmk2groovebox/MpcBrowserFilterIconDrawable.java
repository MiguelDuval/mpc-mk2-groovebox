package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Original compact pictograms for the six MPC Browser file-type filters.
 *
 * The Browser uses pictographic filters on the standalone surface. These
 * glyphs reproduce the documented vocabulary without copying proprietary
 * artwork.
 */
final class MpcBrowserFilterIconDrawable extends Drawable {
    enum Filter {
        PROJECTS,
        PATTERNS,
        KITS,
        PLUGIN_PRESETS,
        SAMPLES,
        ALL;

        static Filter fromLabel(String label) {
            for (Filter value : values()) {
                if (value.name().equals(label)) return value;
            }
            return ALL;
        }
    }

    private static final int ACTIVE = Color.WHITE;
    private static final int INACTIVE = Color.rgb(156, 166, 174);

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Filter filter;
    private boolean selected;

    MpcBrowserFilterIconDrawable(Filter filter, int sizePx) {
        this.filter = filter == null ? Filter.ALL : filter;
        paint.setStrokeCap(Paint.Cap.SQUARE);
        paint.setStrokeJoin(Paint.Join.MITER);
        setBounds(0, 0, sizePx, sizePx);
    }

    void setSelected(boolean selected) {
        if (this.selected == selected) return;
        this.selected = selected;
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        final float w = getBounds().width();
        final float h = getBounds().height();
        final float cx = getBounds().exactCenterX();
        final float cy = getBounds().exactCenterY();
        final float s = Math.min(w, h);
        if (s <= 0.0f) return;

        paint.setColor(selected ? ACTIVE : INACTIVE);
        paint.setStrokeWidth(Math.max(1.5f, s * 0.085f));
        paint.setStyle(Paint.Style.STROKE);

        switch (filter) {
            case PROJECTS:
                drawDocument(canvas, cx, cy, s);
                break;
            case PATTERNS:
                drawPattern(canvas, cx, cy, s);
                break;
            case KITS:
                drawKit(canvas, cx, cy, s);
                break;
            case PLUGIN_PRESETS:
                drawPlug(canvas, cx, cy, s);
                break;
            case SAMPLES:
                drawWaveform(canvas, cx, cy, s);
                break;
            case ALL:
            default:
                drawAll(canvas, cx, cy, s);
                break;
        }
    }

    private void drawDocument(Canvas canvas, float cx, float cy, float s) {
        final float half = s * 0.31f;
        final float left = cx - half;
        final float right = cx + half;
        final float top = cy - half;
        final float bottom = cy + half;
        path.reset();
        path.moveTo(left, top);
        path.lineTo(cx + s * 0.08f, top);
        path.lineTo(right, cy - s * 0.08f);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.close();
        canvas.drawPath(path, paint);
        canvas.drawLine(cx + s * 0.08f, top, cx + s * 0.08f, cy - s * 0.08f, paint);
        canvas.drawLine(cx + s * 0.08f, cy - s * 0.08f, right, cy - s * 0.08f, paint);
    }

    private void drawPattern(Canvas canvas, float cx, float cy, float s) {
        final float left = cx - s * 0.34f;
        for (int i = -1; i <= 1; i++) {
            final float y = cy + i * s * 0.22f;
            canvas.drawLine(left, y, cx + s * 0.31f, y, paint);
            canvas.drawCircle(cx - s * 0.16f, y, s * 0.055f, paint);
        }
    }

    private void drawKit(Canvas canvas, float cx, float cy, float s) {
        final float d = s * 0.23f;
        final float gap = s * 0.08f;
        for (int row = -1; row <= 1; row += 2) {
            for (int col = -1; col <= 1; col += 2) {
                final float x = cx + col * (d + gap) * 0.62f;
                final float y = cy + row * (d + gap) * 0.62f;
                canvas.drawRect(x - d * 0.5f, y - d * 0.5f,
                        x + d * 0.5f, y + d * 0.5f, paint);
            }
        }
    }

    private void drawPlug(Canvas canvas, float cx, float cy, float s) {
        final float arm = s * 0.18f;
        canvas.drawLine(cx - arm, cy - s * 0.28f,
                cx - arm, cy - s * 0.05f, paint);
        canvas.drawLine(cx + arm, cy - s * 0.28f,
                cx + arm, cy - s * 0.05f, paint);
        canvas.drawArc(new RectF(
                cx - s * 0.26f, cy - s * 0.12f,
                cx + s * 0.26f, cy + s * 0.36f),
                0.0f, 180.0f, false, paint);
        canvas.drawLine(cx, cy + s * 0.12f, cx, cy + s * 0.32f, paint);
        path.reset();
        path.moveTo(cx, cy + s * 0.32f);
        path.lineTo(cx - s * 0.10f, cy + s * 0.22f);
        path.moveTo(cx, cy + s * 0.32f);
        path.lineTo(cx + s * 0.10f, cy + s * 0.22f);
        canvas.drawPath(path, paint);
    }

    private void drawWaveform(Canvas canvas, float cx, float cy, float s) {
        final float[] values = {0.0f, 0.48f, -0.25f, 0.62f, -0.45f, 0.26f, -0.10f, 0.0f};
        float lastX = cx - s * 0.34f;
        float lastY = cy + values[0] * s * 0.52f;
        for (int i = 1; i < values.length; i++) {
            final float x = cx - s * 0.34f
                    + (s * 0.68f * i / (values.length - 1));
            final float y = cy + values[i] * s * 0.52f;
            canvas.drawLine(lastX, lastY, x, y, paint);
            lastX = x;
            lastY = y;
        }
    }

    private void drawAll(Canvas canvas, float cx, float cy, float s) {
        final float d = s * 0.16f;
        for (int row = -1; row <= 1; row++) {
            for (int col = -1; col <= 1; col++) {
                final float x = cx + col * s * 0.28f;
                final float y = cy + row * s * 0.28f;
                canvas.drawRect(x - d, y - d, x + d, y + d, paint);
            }
        }
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }

    @Override public void setColorFilter(android.graphics.ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}
