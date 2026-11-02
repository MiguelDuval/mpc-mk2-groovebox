package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;

/**
 * Centralised MPC One / OS 3.9 visual design tokens.
 *
 * All shell chrome colours, typography and geometry live here so that
 * {@link MpcShell}, {@code MainActivity} and the shortcut rail share one
 * source of truth. Values are derived from the MPC Standalone OS 3.9 visual
 * reference (graphite shell, dense fields, red selection accent) and are
 * presentation-only; they do not own musical or domain state.
 */
final class MpcStyle {

    // --- Shell chrome -------------------------------------------------------

    /** Outermost application background (graphite). */
    static final int BG = Color.rgb(17, 19, 22);

    /** Secondary panel surface, one step lighter than BG. */
    static final int SURFACE = Color.rgb(38, 42, 46);

    /** Tertiary surface, one step lighter than SURFACE. */
    static final int SURFACE_2 = Color.rgb(32, 37, 42);

    /** Hairline borders and separators. */
    static final int LINE = Color.rgb(64, 72, 80);

    /** Primary on-surface text. */
    static final int TEXT = Color.rgb(235, 239, 242);

    /** Secondary, de-emphasised text. */
    static final int MUTED = Color.rgb(156, 166, 174);

    /** Selection / active accent (MPC red). */
    static final int SELECTION_RED = Color.rgb(224, 30, 61);

    /** Selected fill variant of the selection red. */
    static final int SELECTED = Color.rgb(235, 42, 68);

    /** Cool cyan accent used by active editor controls and selected list strokes. */
    static final int ACCENT = Color.rgb(78, 180, 204);

    /** Brighter secondary cyan for selected/secondary control emphasis. */
    static final int ACCENT_2 = Color.rgb(112, 204, 223);

    /** Destructive/error emphasis; matches the MPC red selection token. */
    static final int DANGER = SELECTION_RED;

    /** Active / enabled affirmative (transport green). */
    static final int ACTIVE = Color.rgb(63, 207, 117);

    /** MPC panel surface used by context areas and strips. */
    static final int PANEL = Color.rgb(39, 43, 47);

    /** Secondary panel surface for alternating rows and nested panels. */
    static final int PANEL_2 = Color.rgb(39, 43, 47);

    /** Darker panel variant for nested surfaces. */
    static final int PANEL_DARK = Color.rgb(28, 31, 34);

    /** Panel border colour. */
    static final int PANEL_BORDER = Color.rgb(75, 82, 88);

    /** Time-signature highlight (MPC amber). */
    static final int TIME_SIGNATURE_HIGHLIGHT = Color.rgb(240, 194, 48);

    // --- Touch Strip modes ----------------------------------------------------

    /** Touch Strip controls track level. */
    static final int TOUCH_STRIP_MODE_LEVEL = 0;

    /** Touch Strip controls pan. */
    static final int TOUCH_STRIP_MODE_PAN = 1;

    /** Touch Strip controls tune. */
    static final int TOUCH_STRIP_MODE_TUNE = 2;

    /** Touch Strip controls sample start. */
    static final int TOUCH_STRIP_MODE_SAMPLE_START = 3;

    /** Touch Strip controls sample end. */
    static final int TOUCH_STRIP_MODE_SAMPLE_END = 4;

    /**
     * Returns a readable label for a Touch Strip mode.
     * Single canonical implementation; all callers should use this.
     */
    static String touchStripModeLabel(int mode) {
        switch (mode) {
            case TOUCH_STRIP_MODE_LEVEL:
                return "LEVEL";
            case TOUCH_STRIP_MODE_PAN:
                return "PAN";
            case TOUCH_STRIP_MODE_TUNE:
                return "TUNE";
            case TOUCH_STRIP_MODE_SAMPLE_START:
                return "SAMPLE START";
            case TOUCH_STRIP_MODE_SAMPLE_END:
                return "SAMPLE END";
            default:
                return "LEVEL";
        }
    }

    // --- Typography ----------------------------------------------------------

    static final float TEXT_SIZE_SMALL = 7f;
    static final float TEXT_SIZE_FIELD = 9f;
    static final float TEXT_SIZE_METRIC = 11f;
    static final float TEXT_SIZE_MAIN_FIELD = 13f;
    static final float TEXT_SIZE_MAIN_HEADER = 15f;

    static final Typeface TYPEFACE_NORMAL = Typeface.DEFAULT;
    static final Typeface TYPEFACE_BOLD = Typeface.DEFAULT_BOLD;

    // --- Geometry (dp, converted at call sites) ------------------------------

    /** Shell toolbar height. */
    static final int TOOLBAR_HEIGHT_DP = 44;

    /** Playhead / position strip height. */
    static final int POSITION_STRIP_HEIGHT_DP = 3;

    /** Shortcut rail width. */
    static final int SHORTCUT_RAIL_WIDTH_DP = 48;

    /** XL Channel Strip / context area width. */
    static final int CHANNEL_STRIP_WIDTH_DP = 210;

    /** Function Bar height. */
    static final int FUNCTION_BAR_HEIGHT_DP = 40;

    /** Rail content inset. */
    static final int RAIL_CONTENT_INSET_DP = 4;

    /** Context content inset. */
    static final int CONTEXT_CONTENT_INSET_DP = 4;

    /** Main content gutter. */
    static final int MAIN_CONTENT_GUTTER_DP = 4;

    /** Main section gap. */
    static final int MAIN_SECTION_GAP_DP = 2;

    /** Main field height. */
    static final int MAIN_FIELD_HEIGHT_DP = 40;

    /** Main metric height. */
    static final int MAIN_METRIC_HEIGHT_DP = 36;

    /** Main track header height. */
    static final int MAIN_TRACK_HEADER_HEIGHT_DP = 36;

    /** Main track type icon width. */
    static final int MAIN_TRACK_TYPE_ICON_WIDTH_DP = 38;

    /** Main program height. */
    static final int MAIN_PROGRAM_HEIGHT_DP = 32;

    /** Shortcut selection indicator width. */
    static final int SHORTCUT_SELECTION_WIDTH_DP = 3;

    /** Main section divider thickness. */
    static final int MAIN_SECTION_DIVIDER_DP = 2;

    /** Flat (non-rounded) corner radius — MPC uses rectangular surfaces. */
    static final int FLAT_RADIUS_DP = 0;

    /** Main panel corner radius. */
    static final int MAIN_RADIUS_DP = 0;

    // Toolbar geometry
    static final int TOOLBAR_INSET_DP = 6;
    static final int TOOLBAR_CONTROL_HEIGHT_DP = 34;
    static final int TOOLBAR_GAP_DP = 2;
    static final int TOOLBAR_MENU_WIDTH_DP = 38;
    static final int TOOLBAR_PROJECT_IDENTITY_WIDTH_DP = 106;
    static final int TOOLBAR_PROJECT_BROWSER_WIDTH_DP = 26;
    static final int TOOLBAR_TIMING_WIDTH_DP = 60;
    static final int TOOLBAR_METRO_WIDTH_DP = 58;
    static final int TOOLBAR_AUTO_WIDTH_DP = 48;
    static final int TOOLBAR_IO_WIDTH_DP = 40;

    private MpcStyle() {
    }

    static int dp(Context context, int valueDp) {
        return Math.round(
                valueDp * context.getResources().getDisplayMetrics().density);
    }
}
