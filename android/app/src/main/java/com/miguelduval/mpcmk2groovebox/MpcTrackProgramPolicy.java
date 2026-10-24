package com.miguelduval.mpcmk2groovebox;

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
