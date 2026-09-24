package com.miguelduval.mpcmk2groovebox;

public final class MpcStudioMk2MidiMessages {
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
}
