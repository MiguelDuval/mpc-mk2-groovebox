package com.miguelduval.mpcmk2groovebox;

import java.util.Locale;

/** Pure accessibility-label policy for Pad Mixer touch controls. */
final class MpcPadMixerAccessibilityPolicy {
    private MpcPadMixerAccessibilityPolicy() {}

    static String controlButtonDescription(int pad, String control, boolean increase) {
        final int safePad = Math.max(0, Math.min(15, pad));
        final String normalizedControl = control == null
                ? "" : control.trim().toUpperCase(Locale.ROOT);
        if (!"PAN".equals(normalizedControl) && !"TUNE".equals(normalizedControl)) {
            throw new IllegalArgumentException(
                    "Pad Mixer touch control must be PAN or TUNE");
        }
        return String.format(
                Locale.ROOT,
                "PAD %02d %s %s",
                safePad + 1,
                normalizedControl,
                increase ? "increase" : "decrease");
    }
}
