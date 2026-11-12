package com.miguelduval.mpcmk2groovebox;

import java.util.Locale;

/**
 * Truth policy for Main's Track-owned Program context.
 *
 * The current backend supports selecting a Drum Program from Main. Other Track
 * Types must not imply that a Drum Program list can be assigned to them.
 */
final class MpcTrackProgramPolicy {
    private static final float AVAILABLE_ALPHA = 1.0f;
    private static final float UNAVAILABLE_ALPHA = 0.45f;

    private MpcTrackProgramPolicy() {}

    static boolean supportsProgramSelect(String trackType) {
        return trackType != null && "DRUM".equalsIgnoreCase(trackType.trim());
    }

    // MPC 3.9 Pad Perform works on Keygroup, MIDI, Plugin and CV tracks — it
    // does NOT work on Drum Track. Drum Pad sample triggering is a different
    // contract and must never be used as a Pad Perform substitute.
    static boolean supportsPadPerform(String trackType) {
        if (trackType == null) return false;
        final String t = trackType.trim().toUpperCase(Locale.ROOT);
        return "KEYGROUP".equals(t)
                || "MIDI".equals(t)
                || "PLUGIN".equals(t);
    }

    static boolean selectorEnabled(boolean startupComplete, String trackType) {
        return startupComplete && supportsProgramSelect(trackType);
    }

    static String caption(String trackType) {
        return supportsProgramSelect(trackType) ? "DRUM PROGRAM" : "PROGRAM";
    }

    static float selectorAlpha(boolean startupComplete, String trackType) {
        return selectorEnabled(startupComplete, trackType)
                ? AVAILABLE_ALPHA : UNAVAILABLE_ALPHA;
    }
}
