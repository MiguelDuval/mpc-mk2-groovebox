package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Compact original Browser chrome glyphs. */
final class MpcBrowserChromeIconDrawable extends Drawable {
    enum Mode { OPTIONS }

    private static final int COLOR = Color.rgb(156, 166, 174);
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Mode mode;

    MpcBrowserChromeIconDrawable(Mode mode, int sizePx) {
        this.mode = mode == null ? Mode.OPTIONS : mode;
        paint.setColor(COLOR);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(Math.max(1.5f, sizePx * 0.08f));
        setBounds(0, 0, sizePx, sizePx);
    }

    @Override public void draw(Canvas canvas) {
        if (mode != Mode.OPTIONS) return;
        final RectF b = new RectF(getBounds());
        final float cx = b.centerX();
        final float cy = b.centerY();
        final float r = Math.min(b.width(), b.height()) * 0.28f;
        canvas.drawCircle(cx, cy, r, paint);
        for (int i = 0; i < 8; i++) {
            final double angle = (Math.PI * 2.0 * i) / 8.0;
            final float x1 = cx + (float)Math.cos(angle) * r * 1.18f;
            final float y1 = cy + (float)Math.sin(angle) * r * 1.18f;
            final float x2 = cx + (float)Math.cos(angle) * r * 1.55f;
            final float y2 = cy + (float)Math.sin(angle) * r * 1.55f;
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
    @Override public void setColorFilter(android.graphics.ColorFilter filter) {
        paint.setColorFilter(filter);
    }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
