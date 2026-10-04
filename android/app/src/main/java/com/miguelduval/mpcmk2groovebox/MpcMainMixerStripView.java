package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Locale;

/**
 * MPC 3.9 Main Mode mixer-strip region.
 *
 * The official MPC Main layout places XL channel strips immediately beside
 * the five mode shortcuts. This view is intentionally presentation-first:
 * it exposes the track/pad/main-output mixer hierarchy while delegating every
 * mutation to MainActivity/native state.
 */
final class MpcMainMixerStripView extends LinearLayout {
    interface Listener {
        boolean isStartupReady();
        boolean mixerStripVisible();
        void toggleTrackMute();
        void onMixerStripVisibilityChanged(boolean visible);
    }

    private static final int BG = Color.rgb(23, 25, 28);
    private static final int PANEL = Color.rgb(31, 35, 39);
    private static final int PANEL_DARK = Color.rgb(25, 28, 31);
    private static final int LINE = Color.rgb(71, 78, 85);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int RED = Color.rgb(224, 30, 61);
    private static final int ACTIVE = Color.rgb(64, 201, 112);
    private static final int WHITE = Color.WHITE;
    private static final int FLAT_RADIUS_DP = 0;

    private final Listener listener;
    private final TextView modeLabel;
    private final TextView focusLabel;
    private final LinearLayout strips;

    MpcMainMixerStripView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Main XL Mixer Strips");

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));

        modeLabel = text(context, "MIXER STRIPS", 8, TEXT);
        modeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(modeLabel, new LayoutParams(0, dp(context, 22), 1));

        Button visibility = button(context, "◉", 10);
        visibility.setContentDescription("MPC Main mixer strips show or hide");
        visibility.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMixerStripVisibilityChanged(
                        !listener.mixerStripVisible());
            }
        });
        header.addView(visibility, new LayoutParams(dp(context, 28), dp(context, 22)));

        // Track/Pad selection is deliberately not duplicated here.
        // MPC places the Track/Pad mixer toggle in the lower-right corner
        // of the Main Track/Arrangement section. The top control is only
        // the strip visibility affordance.


        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 26)));

        focusLabel = text(context, "DIAL • NONE", 7, MUTED);
        focusLabel.setGravity(Gravity.CENTER_VERTICAL);
        focusLabel.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        focusLabel.setContentDescription("MPC Main mixer Data Dial focus");
        addView(focusLabel, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 20)));

        strips = new LinearLayout(context);
        strips.setOrientation(HORIZONTAL);
        strips.setGravity(Gravity.CENTER_VERTICAL);
        addView(strips, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
    }

    void setState(
            boolean visible,
            boolean padMode,
            String dialFocus,
            int selectedTrack,
            String trackType,
            String trackName,
            String programName,
            int selectedPad,
            float padLevel,
            float padPan,
            String padSampleName,
            boolean trackMuted) {
        // Keep the top control row mounted even when the XL strips are
        // collapsed, matching MPC's “tap the top icon to show/hide strips”
        // interaction. Only the expanded strip bodies are collapsed.
        setVisibility(View.VISIBLE);
        strips.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            modeLabel.setText("MIXER STRIPS • HIDDEN");
            focusLabel.setText("TAP ◉ TO SHOW");
            return;
        }

        final boolean ready = listener != null && listener.isStartupReady();
        modeLabel.setText(
                padMode ? "PAD STRIP / MAIN OUT" : "TRACK STRIP / MAIN OUT");
        focusLabel.setText(
                "DIAL • " + (dialFocus == null ? "NONE" : dialFocus));

        strips.removeAllViews();

        if (padMode) {
            // In Drum Pad view, the first XL strip becomes the selected Pad;
            // the second remains the project Main Output.
            strips.addView(buildPadStrip(
                    getContext(),
                    selectedPad,
                    padLevel,
                    padPan,
                    padSampleName),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            strips.addView(buildOutputStrip(
                    getContext()),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        } else {
            // In Track view, the first XL strip is the selected Track; the
            // second is the project Main Output.
            strips.addView(buildTrackStrip(
                    getContext(),
                    selectedTrack,
                    trackType,
                    trackName,
                    programName,
                    trackMuted,
                    ready),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            strips.addView(buildOutputStrip(
                    getContext()),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        }
    }

    private View buildTrackStrip(
            Context context,
            int track,
            String trackType,
            String trackName,
            String programName,
            boolean muted,
            boolean ready) {
        LinearLayout strip = baseStrip(context);
        addStripHeader(strip, "TRACK " + (track + 1), trackName);
        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});
        strip.addView(info(
                context,
                "TYPE\n" + safe(trackType),
                TEXT),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 38)));
        strip.addView(info(
                context,
                "PROGRAM\n" + safe(programName),
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 44)));

        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        meter.setValue(0.0f);
        meter.setContentDescription("MPC Main selected track level meter reserved");
        meter.setAlpha(0.55f);
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 54)));
        TextView level = info(context, "LEVEL\nRESERVED", MUTED);
        strip.addView(level, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 38)));

        TextView pan = info(context, "PAN\nRESERVED", MUTED);
        strip.addView(pan, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 46)));

        LinearLayout ms = row(context);
        Button mute = button(context, muted ? "M" : "M", 9);
        mute.setContentDescription("MPC Main selected track mute");
        mute.setTextColor(muted ? BG : MUTED);
        mute.setBackground(stroke(
                muted ? ACTIVE : PANEL_DARK,
                muted ? ACTIVE : LINE));
        mute.setEnabled(ready);
        mute.setAlpha(ready ? 1.0f : 0.5f);
        mute.setOnClickListener(v -> {
            if (listener != null) listener.toggleTrackMute();
        });
        ms.addView(mute, new LayoutParams(0, dp(context, 34), 1));

        Button solo = button(context, "S", 9);
        solo.setEnabled(false);
        solo.setAlpha(0.42f);
        solo.setContentDescription("MPC Main selected track solo reserved");
        ms.addView(solo, new LayoutParams(0, dp(context, 34), 1));
        strip.addView(ms, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 36)));

        TextView footer = info(context, "MIX\nTRACK", MUTED);
        strip.addView(footer, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private View buildPadStrip(
            Context context,
            int pad,
            float levelValue,
            float panValue,
            String sampleName) {
        LinearLayout strip = baseStrip(context);
        addStripHeader(
                strip,
                "PAD " + String.format(Locale.ROOT, "%02d", pad + 1),
                sampleName == null || sampleName.isEmpty() ? "NO SAMPLE" : sampleName);

        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});

        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        meter.setValue(levelValue);
        meter.setContentDescription("MPC Main selected pad level meter");
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 54)));

        TextView level = info(
                context,
                String.format(Locale.ROOT, "LEVEL\n%3d%%",
                        Math.round(levelValue * 100.0f)),
                TEXT);
        strip.addView(level, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 44)));

        TextView pan = info(
                context,
                "PAN\n" + panText(panValue),
                TEXT);
        strip.addView(pan, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 44)));

        LinearLayout ms = row(context);
        Button mute = button(context, "M", 9);
        mute.setEnabled(false);
        mute.setAlpha(0.42f);
        mute.setContentDescription("MPC Main pad mute reserved");
        ms.addView(mute, new LayoutParams(0, dp(context, 34), 1));

        Button solo = button(context, "S", 9);
        solo.setEnabled(false);
        solo.setAlpha(0.42f);
        solo.setContentDescription("MPC Main pad solo reserved");
        ms.addView(solo, new LayoutParams(0, dp(context, 34), 1));
        strip.addView(ms, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 36)));

        TextView footer = info(
                context,
                "PAD MIX\nA" + (pad + 1),
                MUTED);
        strip.addView(footer, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private View buildTrackSummaryStrip(
            Context context,
            int track,
            String trackType,
            String trackName,
            String programName,
            boolean muted,
            String title) {
        LinearLayout strip = baseStrip(context);
        addStripHeader(
                strip,
                title,
                trackName == null || trackName.isEmpty()
                        ? "TRACK " + (track + 1)
                        : trackName);
        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});
        strip.addView(info(
                context,
                "TYPE\n" + safe(trackType),
                TEXT),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));
        strip.addView(info(
                context,
                "PROGRAM\n" + safe(programName),
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 46)));
        strip.addView(info(
                context,
                "LEVEL\nRESERVED\nPAN RESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 78)));
        strip.addView(info(
                context,
                "MUTE " + (muted ? "ON" : "OFF") + "\nSOLO —",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 48)));
        strip.addView(info(
                context,
                "ROUTING\nRESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private View buildOutputStrip(Context context) {
        LinearLayout strip = baseStrip(context);
        addStripHeader(strip, "OUTPUT 1/2", "MAIN OUT");
        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});
        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        meter.setValue(0.0f);
        meter.setContentDescription("MPC Main output level meter reserved");
        meter.setAlpha(0.55f);
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 54)));
        strip.addView(info(
                context,
                "LEVEL\nRESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));
        strip.addView(info(
                context,
                "PAN\nRESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 56)));
        strip.addView(info(
                context,
                "OUTPUT\n1/2",
                TEXT),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 46)));
        strip.addView(info(
                context,
                "FX\nRESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 46)));
        strip.addView(info(
                context,
                "I/O\nRESERVED",
                MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private static final class MpcVerticalMeter extends View {
        private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;

        MpcVerticalMeter(Context context) {
            super(context);
            fillPaint.setColor(Color.rgb(71, 214, 195));
            trackPaint.setColor(Color.rgb(18, 21, 24));
            setWillNotDraw(false);
        }

        void setValue(float value) {
            this.value = Math.max(0.0f, Math.min(1.0f, value));
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final float w = getWidth();
            final float h = getHeight();
            final float left = w * 0.38f;
            final float right = w * 0.62f;
            canvas.drawRect(left, 2, right, h - 2, trackPaint);
            final float filled = (h - 4) * value;
            canvas.drawRect(left, h - 2 - filled, right, h - 2, fillPaint);
        }
    }

    private LinearLayout baseStrip(Context context) {
        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(VERTICAL);
        strip.setPadding(dp(context, 2), dp(context, 2), dp(context, 2), dp(context, 2));
        strip.setBackground(stroke(PANEL, LINE));
        return strip;
    }

    private void addStripHeader(LinearLayout strip, String title, String subtitle) {
        TextView titleView = text(getContext(), title, 8, TEXT);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.CENTER_VERTICAL);
        strip.addView(titleView, new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 26)));

        TextView subtitleView = text(getContext(), safe(subtitle), 7, MUTED);
        subtitleView.setGravity(Gravity.CENTER_VERTICAL);
        strip.addView(subtitleView, new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 34)));
    }

    private void addTabs(LinearLayout strip, String[] tabs) {
        LinearLayout tabsRow = new LinearLayout(getContext());
        tabsRow.setOrientation(HORIZONTAL);
        for (int i = 0; i < tabs.length; i++) {
            final boolean active = i == 0;
            TextView tab = text(
                    getContext(),
                    tabs[i],
                    7,
                    active ? BG : MUTED);
            tab.setGravity(Gravity.CENTER);
            tab.setTypeface(Typeface.DEFAULT_BOLD);
            tab.setBackground(stroke(
                    active ? WHITE : PANEL_DARK,
                    active ? WHITE : LINE));
            tab.setContentDescription(
                    "MPC Main mixer tab " + tabs[i]
                            + (active ? " active" : " unavailable"));
            if (!active) tab.setAlpha(0.5f);
            tabsRow.addView(tab, new LayoutParams(0, dp(getContext(), 22), 1));
        }
        strip.addView(tabsRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 24)));
    }

    private TextView info(Context context, String value, int color) {
        TextView view = text(context, value, 8, color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        view.setBackground(stroke(PANEL_DARK, LINE));
        return view;
    }

    private TextView text(Context context, String value, int size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        return view;
    }

    private Button button(Context context, String value, int size) {
        Button view = new Button(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(TEXT);
        view.setGravity(Gravity.CENTER);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(0, 0, 0, 0);
        view.setBackground(stroke(PANEL_DARK, LINE));
        return view;
    }

    private LinearLayout row(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }

    private String panText(float pan) {
        if (Math.abs(pan) < 0.01f) return "C";
        return String.format(
                Locale.ROOT,
                "%s%d",
                pan < 0 ? "L" : "R",
                Math.round(Math.abs(pan) * 100.0f));
    }

    private GradientDrawable stroke(int fill, int border) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(getContext(), FLAT_RADIUS_DP));
        drawable.setStroke(1, border);
        return drawable;
    }

    private int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
