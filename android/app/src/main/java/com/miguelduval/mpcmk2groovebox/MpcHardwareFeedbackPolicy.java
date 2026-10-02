package com.miguelduval.mpcmk2groovebox;

final class MpcHardwareFeedbackPolicy {
    static final int LED_OFF = 0;
    static final int LED_COLOR_1_DIM = 1;
    static final int LED_SINGLE_FULL = 2;
    static final int LED_COLOR_2_DIM = 2;
    static final int LED_COLOR_1_FULL = 3;
    static final int LED_COLOR_2_FULL = 4;

    private MpcHardwareFeedbackPolicy() {}

    static int dualColor(boolean active, boolean alternate) {
        if (!active) return LED_OFF;
        return alternate ? LED_COLOR_2_FULL : LED_COLOR_1_FULL;
    }

    static boolean isTwoColorButton(int cc) {
        switch (cc) {
            case 0:
            case 4:
            case 9:
            case 11:
            case 12:
            case 13:
            case 14:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 42:
            case 49:
            case 50:
            case 52:
            case 53:
            case 66:
            case 67:
            case 75:
            case 79:
            case 122:
                return true;
            default:
                return false;
        }
    }

    static String contextLabel(
            String page,
            int focus,
            boolean locate,
            boolean erase,
            boolean copyDelete,
            int copyDeleteMode,
            boolean noteRepeat,
            int noteRepeatRate,
            int touchStripMode) {
        if (locate) return "LOCATE • pad slots / transport";
        if (erase) return "ERASE • hold + pad";
        if (copyDelete) {
            return copyDeleteMode == 0
                    ? "COPY • source → destination(s)"
                    : "DELETE • select pad(s)";
        }
        if (noteRepeat) {
            return "NOTE REPEAT • " + noteRepeatRateLabel(noteRepeatRate);
        }
        if ("SAMPLE".equals(page) || "SAMPLE_EDIT".equals(page)) {
            switch (focus) {
                case 7: return "SAMPLE START • " + touchStripModeLabel(touchStripMode);
                case 8: return "SAMPLE END • " + touchStripModeLabel(touchStripMode);
                case 9: return "TUNE • " + touchStripModeLabel(touchStripMode);
                case 10: return "SAMPLE SELECT • layer";
                case 11: return "ZOOM H • sample waveform";
                case 12: return "ZOOM V • sample waveform";
                default: return "SAMPLE • touch strip";
            }
        }
        if ("GRID".equals(page)) {
            switch (focus) {
                case 13: return "GRID • zoom horizontal";
                case 14: return "GRID • zoom vertical";
                default: return "GRID • event editor";
            }
        }
        if ("STEP".equals(page)) {
            return "STEP • 16-pad step edit";
        }
        if ("TRACK_VIEW".equals(page)) {
            switch (focus) {
                case 2: return "TRACK SELECT • data dial";
                case 3: return "SEQUENCE SELECT • data dial";
                default: return "TRACK VIEW • sequence tracks";
            }
        }
        if ("BROWSER".equals(page)) {
            return "BROWSER • library / sample";
        }
        if ("SAMPLER".equals(page)) {
            return "SAMPLER • record / monitor";
        }
        if ("CHANNEL_MIXER".equals(page)) {
            return "CHANNEL MIXER • track strips";
        }
        if ("PAD_MIXER".equals(page)) {
            return "PAD MIXER • pad strips";
        }
        if ("NEXT_SEQUENCE".equals(page)) {
            return "NEXT SEQUENCE • queued launch";
        }
        if ("ARRANGE".equals(page)) {
            return "ARRANGE • linear timeline";
        }
        if ("SEQ".equals(page)) {
            switch (focus) {
                case 2: return "TRACK SELECT • data dial";
                case 3: return "SEQUENCE SELECT • data dial";
                case 4: return "PROGRAM SELECT";
                case 5: return "TRACK TYPE";
                case 13: return "ZOOM H • grid time";
                case 14: return "ZOOM V • grid pads";
                case 15: return "ZOOM H • timeline";
                case 16: return "ZOOM V • timeline unavailable";
                default: return "SEQ • playback / edit";
            }
        }
        return "MAIN • pad performance";
    }

    static String focusAxis(int focus) {
        switch (focus) {
            case 11:
            case 13:
            case 15:
                return "H";
            case 12:
            case 14:
                return "V";
            default:
                return "";
        }
    }

    private static String noteRepeatRateLabel(int index) {
        final String[] labels = {
                "1/4", "1/4T", "1/8", "1/8T",
                "1/16", "1/16T", "1/32", "1/32T"
        };
        return labels[Math.max(0, Math.min(labels.length - 1, index))];
    }

    private static String touchStripModeLabel(int mode) {
        final String[] labels = {
                "LEVEL", "PAN", "TUNE", "SAMPLE START", "SAMPLE END"
        };
        return labels[Math.max(0, Math.min(labels.length - 1, mode))];
    }
}
