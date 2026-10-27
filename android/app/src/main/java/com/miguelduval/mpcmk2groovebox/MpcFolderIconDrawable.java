package com.miguelduval.mpcmk2groovebox;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/**
 * Small original folder glyph used by the MPC-style Toolbar.
 *
 * It is intentionally drawn in code rather than relying on a device font,
 * keeping the instrument UI visually stable across Android renderers.
 */
final class MpcFolderIconDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    MpcFolderIconDrawable() {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
    }

    @Override
    public void draw(Canvas canvas) {
        final float w = getBounds().width();
        final float h = getBounds().height();
        if (w <= 0.0f || h <= 0.0f) {
            return;
        }

        final float left = getBounds().left + w * 0.17f;
        final float right = getBounds().right - w * 0.17f;
        final float top = getBounds().top + h * 0.29f;
        final float bottom = getBounds().bottom - h * 0.25f;
        final float tabTop = getBounds().top + h * 0.20f;
        final float radius = Math.max(1.5f, w * 0.045f);

        path.reset();
        path.moveTo(left, top);
        path.lineTo(left + w * 0.24f, top);
        path.lineTo(left + w * 0.33f, tabTop);
        path.lineTo(right - w * 0.10f, tabTop);
        path.quadTo(right, tabTop, right, tabTop + radius);
        path.lineTo(right, bottom - radius);
        path.quadTo(right, bottom, right - radius, bottom);
        path.lineTo(left + radius, bottom);
        path.quadTo(left, bottom, left, bottom - radius);
        path.close();

        canvas.drawPath(path, paint);

        // Cut a thin red slot into the lower edge so the glyph reads as a
        // folder instead of a generic rectangle while preserving the Toolbar
        // color language underneath.
        paint.setColor(Color.rgb(224, 30, 61));
        final RectF slot = new RectF(
                left + w * 0.12f,
                bottom - h * 0.18f,
                right - w * 0.12f,
                bottom - h * 0.10f);
        canvas.drawRoundRect(slot, radius, radius, paint);
        paint.setColor(Color.WHITE);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return 20;
    }

    @Override
    public int getIntrinsicHeight() {
        return 20;
    }
}
