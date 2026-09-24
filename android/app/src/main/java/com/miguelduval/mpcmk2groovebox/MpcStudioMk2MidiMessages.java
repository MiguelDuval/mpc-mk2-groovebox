package com.miguelduval.mpcmk2groovebox;

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
}
