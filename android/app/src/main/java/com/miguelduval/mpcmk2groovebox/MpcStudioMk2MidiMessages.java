package com.miguelduval.mpcmk2groovebox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public final class MpcStudioMk2MidiMessages {
    private static final int TOUCH_STRIP_LED_BASE_CC = 57;
    private static final int TOUCH_STRIP_LED_COUNT = 9;
    private static final int NOTE_REPEAT_LED_BASE_CC = 103;
    private static final int NOTE_REPEAT_LED_COUNT = 8;

    private MpcStudioMk2MidiMessages() {
    }

    public static byte[] padRgb(int pad, int red, int green, int blue) {
        return new byte[] {
                (byte) 0xF0,
                0x47, 0x47, 0x4A, 0x65,
                0x00, 0x04,
                (byte) (pad & 0x7F),
                (byte) (red & 0x7F),
                (byte) (green & 0x7F),
                (byte) (blue & 0x7F),
                (byte) 0xF7
        };
    }

    public static byte[] buttonLed(int cc, int value) {
        return new byte[] {
                (byte) 0xB0,
                (byte) (cc & 0x7F),
                (byte) (value & 0x7F)
        };
    }

    public static byte[] touchStripLed(int cc, int brightness) {
        return buttonLed(cc, brightness);
    }

    public static byte[] touchStripLedSegment(int segment, int brightness) {
        if (segment < 0 || segment >= TOUCH_STRIP_LED_COUNT) {
            throw new IllegalArgumentException("Touch-strip LED segment out of range");
        }

        return buttonLed(
                TOUCH_STRIP_LED_BASE_CC + segment,
                brightness);
    }

    public static byte[] noteRepeatLed(int index, int brightness) {
        if (index < 0 || index >= NOTE_REPEAT_LED_COUNT) {
            throw new IllegalArgumentException("Note Repeat LED index out of range");
        }

        return buttonLed(
                NOTE_REPEAT_LED_BASE_CC + index,
                brightness);
    }

    public static List<byte[]> lcdTestFrame() {
        Bitmap frame = Bitmap.createBitmap(160, 80, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(frame);
        canvas.drawColor(0xFF000000);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(13.0f);
        canvas.drawText("MPC STUDIO MkII", 5.0f, 17.0f, paint);

        paint.setColor(0xFF00FF00);
        canvas.drawRect(5.0f, 27.0f, 155.0f, 42.0f, paint);

        paint.setColor(0xFF000000);
        paint.setTextSize(10.0f);
        canvas.drawText("LCD SYSEX TEST", 15.0f, 38.0f, paint);

        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(10.0f);
        canvas.drawText("160 x 80 / 6 CHUNKS", 5.0f, 60.0f, paint);

        paint.setTextSize(8.0f);
        canvas.drawText("MIDI TRANSPORT OK", 5.0f, 73.0f, paint);

        List<byte[]> messages = new ArrayList<>(6);
        int[][] chunks = {
                {0, 0, 60, 60},
                {0, 60, 60, 20},
                {60, 0, 60, 60},
                {60, 60, 60, 20},
                {120, 0, 40, 60},
                {120, 60, 40, 20}
        };

        for (int[] chunk : chunks) {
            Bitmap tile = Bitmap.createBitmap(
                    frame, chunk[0], chunk[1], chunk[2], chunk[3]);

            ByteArrayOutputStream png = new ByteArrayOutputStream();
            tile.compress(Bitmap.CompressFormat.PNG, 100, png);

            messages.add(lcdChunk(
                    chunk[0],
                    chunk[1],
                    png.toByteArray()));

            tile.recycle();
        }

        frame.recycle();
        return messages;
    }

    private static byte[] lcdChunk(int x, int y, byte[] pngBytes) {
        return nativeLcdChunk(x, y, pngBytes);
    }

    private static native byte[] nativeLcdChunk(
            int x,
            int y,
            byte[] pngBytes);
}
