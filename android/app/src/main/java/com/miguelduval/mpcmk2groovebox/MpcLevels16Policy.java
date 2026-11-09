package com.miguelduval.mpcmk2groovebox;

/**
 * Supported 16 Levels parameter mapping. Unsupported MPC 3.9 types remain
 * visible in the selector but cannot be committed until a transient native
 * parameter path exists.
 */
final class MpcLevels16Policy {
    enum Type {
        VELOCITY("VELOCITY"),
        TUNE("TUNE"),
        FILTER("FILTER"),
        LAYER("LAYER"),
        SLICE("SLICE"),
        ARTICULATION("ARTICULATION"),
        ATTACK("ATTACK"),
        DECAY("DECAY"),
        PROBABILITY("PROBABILITY"),
        RATCHET("RATCHET");

        private final String label;
        Type(String label) { this.label = label; }
        String label() { return label; }
    }

    private MpcLevels16Policy() {}

    static boolean isSupported(Type type) {
        return type == Type.VELOCITY
                || type == Type.TUNE
                || type == Type.FILTER;
    }

    static int velocityForPadIndex(int physicalPadIndex) {
        return 7 + clamp(physicalPadIndex, 0, 15) * 8;
    }

    // The center pad (Pad 09) is untransposed. The range is a deliberate
    // +/- semitone performance span and remains transient, not project data.
    static int tuneSemitonesForPadIndex(int physicalPadIndex) {
        return clamp(physicalPadIndex, 0, 15) - 8;
    }

    static int aftertouchTargetPad(
            boolean filterModeActive,
            int sourcePad,
            int physicalPad) {
        return clamp(filterModeActive ? sourcePad : physicalPad, 0, 15);
    }

    static int padIndexForVelocity(int velocity) {
        return clamp(Math.round((velocity - 7) / 8.0f), 0, 15);
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}