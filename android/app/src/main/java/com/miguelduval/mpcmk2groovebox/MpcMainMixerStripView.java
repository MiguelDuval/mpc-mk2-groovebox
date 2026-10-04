package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * MPC 3 Main Mode XL mixer-strip region.
 *
 * The physical MPC presentation is intentionally dense: two adjacent channel
 * strips, a compact identity header, LVL/FX/SEND/I/O tabs, level/pan controls,
 * and only the controls that are truthful for the selected strip type.
 *
 * This view remains presentation-first. Realtime mixer mutations still belong
 * to MainActivity/native state.
 */
final class MpcMainMixerStripView extends LinearLayout {
    interface Listener {
        boolean isStartupReady();
        boolean mixerStripVisible();
        void toggleTrackMute();
        void onMixerStripVisibilityChanged(boolean visible);
    }

    private static final int BG = Color.rgb(20, 22, 25);
    private static final int PANEL = Color.rgb(28, 31, 35);
    private static final int PANEL_DARK = Color.rgb(20, 23, 26);
    private static final int LINE = Color.rgb(69, 76, 83);
    private static final int TEXT = Color.rgb(238, 241, 244);
    private static final int MUTED = Color.rgb(145, 155, 164);
    private static final int RED = Color.rgb(224, 30, 61);
    private static final int METER = Color.rgb(69, 205, 191);
    private static final int DISABLED = Color.rgb(84, 92, 99);
    private static final int WHITE = Color.WHITE;
    private static final int ACTIVE = Color.rgb(63, 207, 117);
    private static final int FLAT_RADIUS_DP = 0;

    private final Listener listener;
    private final Button visibilityButton;
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
        header.setPadding(dp(context, 3), 0, dp(context, 2), 0);

        /*
         * MPC3 uses a compact top-of-strip icon for showing/hiding the XL
         * Channel Strip region. Do not consume strip height with a textual
         * "MIXER" title; the surrounding shell already establishes the mixer context.
         */
        visibilityButton = button(context, "◉", 10);
        visibilityButton.setContentDescription("MPC Main mixer strips show or hide");
        visibilityButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMixerStripVisibilityChanged(!listener.mixerStripVisible());
            }
        });
        header.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(
                visibilityButton,
                new LayoutParams(dp(context, 26), dp(context, 20)));
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 20)));

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
        setVisibility(View.VISIBLE);
        strips.setVisibility(visible ? View.VISIBLE : View.GONE);

        visibilityButton.setContentDescription(
                "MPC Main mixer strips " + (visible ? "shown" : "hidden"));
        visibilityButton.setAlpha(visible ? 1.0f : 0.65f);
        if (!visible) {
            return;
        }

        strips.removeAllViews();

        if (padMode) {
            // MPC 3: Drum Pad view pairs the selected Pad with its selected
            // Track; Main Output belongs to Track view.
            strips.addView(
                    buildPadStrip(
                            getContext(),
                            selectedPad,
                            padLevel,
                            padPan,
                            padTuning,
                            padSampleName,
                            levelFocus,
                            panFocus,
                            tuneFocus),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            strips.addView(
                    buildTrackStrip(
                            getContext(),
                            selectedTrack,
                            trackType,
                            trackName,
                            programName,
                            trackMuted,
                            listener != null && listener.isStartupReady()),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
        } else {
            // MPC 3: Track view pairs the selected Track with Main Output.
            strips.addView(
                    buildTrackStrip(
                            getContext(),
                            selectedTrack,
                            trackType,
                            trackName,
                            programName,
                            trackMuted,
                            listener != null && listener.isStartupReady()),
                    new LayoutParams(0, LayoutParams.MATCH_PARENT, 1));
            strips.addView(
                    buildOutputStrip(getContext()),
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
        addIdentityHeader(
                strip,
                context,
                String.format(Locale.ROOT, "%02d", track + 1),
                clean(trackName, "Track " + (track + 1)),
                trackType);

        addProgramBand(
                strip,
                context,
                clean(programName, "PROGRAM RESERVED"),
                "PROGRAM");

        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});

        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        // Track level is not yet backed by a writable mixer value. Do not
        // fabricate a half-scale meter; reserved state must remain visually empty.
        meter.setValue(0.0f);
        meter.setEnabledState(false);
        meter.setContentDescription("MPC XL selected track level meter and fader reserved");
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 104)));

        TextView level = valueLabel(
                context,
                "LEVEL\nRESERVED",
                MUTED);
        strip.addView(level, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 30)));

        MpcPanSlider pan = new MpcPanSlider(context);
        pan.setValue(0.0f);
        pan.setEnabledState(false);
        pan.setContentDescription("MPC XL selected track pan slider reserved");
        strip.addView(pan, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));

        TextView panValue = valueLabel(context, "PAN\nC • RESERVED", MUTED);
        strip.addView(panValue, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 28)));

        LinearLayout controls = row(context);
        Button muteButton = controlButton(context, "MUTE", !muted);
        muteButton.setContentDescription("MPC Main selected track mute");
        muteButton.setEnabled(ready);
        muteButton.setAlpha(ready ? 1.0f : 0.55f);
        muteButton.setBackground(stroke(
                muted ? ACTIVE : PANEL_DARK,
                muted ? ACTIVE : LINE));
        muteButton.setTextColor(muted ? BG : TEXT);
        muteButton.setOnClickListener(v -> {
            if (listener != null) listener.toggleTrackMute();
        });
        controls.addView(muteButton, new LayoutParams(0, dp(context, 30), 1));

        Button solo = controlButton(context, "SOLO", false);
        solo.setContentDescription("MPC Main selected track solo reserved");
        controls.addView(solo, new LayoutParams(0, dp(context, 30), 1));

        /*
         * Mixer-strip controls stay limited to channel-local functions.
         * REC ARM and high-frequency Track operations belong to the shell
         * Function Bar; duplicating them here breaks MPC hierarchy and touch
         * efficiency.
         */
        strip.addView(controls, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 32)));

        TextView output = flatLabel(
                context,
                "OUT 1/2",
                TEXT);
        output.setContentDescription("MPC XL selected track output 1/2");
        strip.addView(output, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 24)));

        TextView footer = flatLabel(context, "TRACK • SELECTED", MUTED);
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
        addIdentityHeader(
                strip,
                context,
                "A" + (pad + 1),
                "PAD " + String.format(Locale.ROOT, "%02d", pad + 1),
                "DRUM");

        addProgramBand(
                strip,
                context,
                clean(sampleName, "NO SAMPLE"),
                "SAMPLE");

        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});

        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        meter.setValue(levelValue);
        meter.setEnabledState(true);
        meter.setDialFocus(levelFocus);
        meter.setContentDescription("MPC Main selected pad level meter and fader");
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 104)));

        TextView level = valueLabel(
                context,
                String.format(
                        Locale.ROOT,
                        "LEVEL\n%3d%%",
                        Math.round(clamp01(levelValue) * 100.0f)),
                TEXT);
        strip.addView(level, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 30)));

        MpcPanSlider pan = new MpcPanSlider(context);
        pan.setValue(panValue);
        pan.setEnabledState(true);
        pan.setDialFocus(panFocus);
        pan.setContentDescription("MPC Main selected pad pan slider");
        strip.addView(pan, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));

        TextView panValueLabel = valueLabel(
                context,
                "PAN\n" + panText(panValue),
                TEXT);
        strip.addView(panValueLabel, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 28)));

        TextView tuneValue = valueLabel(
                context,
                String.format(Locale.ROOT, "TUNE\n%+.1f", padTuning),
                tuneFocus ? TEXT : MUTED);
        tuneValue.setContentDescription("MPC Main selected pad tuning");
        tuneValue.setBackground(stroke(
                tuneFocus ? RED : PANEL_DARK,
                tuneFocus ? WHITE : LINE));
        strip.addView(
                tuneValue,
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 30)));

        LinearLayout controls = row(context);
        Button mute = controlButton(context, "MUTE", false);
        mute.setContentDescription("MPC Main pad mute reserved");
        controls.addView(mute, new LayoutParams(0, dp(context, 30), 1));

        Button solo = controlButton(context, "SOLO", false);
        solo.setContentDescription("MPC Main pad solo reserved");
        controls.addView(solo, new LayoutParams(0, dp(context, 30), 1));

        strip.addView(controls, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 32)));

        TextView route = flatLabel(context, "PROGRAM", TEXT);
        route.setContentDescription("MPC XL selected pad routing to Program");
        strip.addView(route, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 24)));

        TextView footer = flatLabel(
                context,
                "PAD • SELECTED",
                MUTED);
        strip.addView(footer, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private View buildOutputStrip(Context context) {
        LinearLayout strip = baseStrip(context);
        addIdentityHeader(
                strip,
                context,
                "1/2",
                "OUTPUT 1/2",
                "MAIN");

        addProgramBand(strip, context, "MAIN OUTPUT", "OUTPUT");
        addTabs(strip, new String[]{"LVL", "FX", "SEND", "I/O"});

        MpcVerticalMeter meter = new MpcVerticalMeter(context);
        meter.setValue(0.0f);
        meter.setEnabledState(false);
        meter.setContentDescription("MPC Main output level meter and fader reserved");
        strip.addView(meter, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 104)));

        strip.addView(
                valueLabel(context, "LEVEL\nRESERVED", MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 30)));

        MpcPanSlider pan = new MpcPanSlider(context);
        pan.setValue(0.0f);
        pan.setEnabledState(false);
        pan.setContentDescription("MPC Main output pan slider reserved");
        strip.addView(pan, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));

        strip.addView(
                valueLabel(context, "PAN\nC • RESERVED", MUTED),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 28)));

        Button mute = controlButton(context, "MUTE", false);
        mute.setContentDescription("MPC Main output mute reserved");
        LinearLayout muteRow = row(context);
        muteRow.addView(mute, new LayoutParams(0, dp(context, 30), 1));
        strip.addView(muteRow, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 32)));

        TextView fx = flatLabel(context, "FX INSERTS • RESERVED", MUTED);
        strip.addView(fx, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 24)));

        TextView footer = flatLabel(context, "MAIN OUTPUT", MUTED);
        strip.addView(footer, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1));
        return strip;
    }

    private void addIdentityHeader(
            LinearLayout strip,
            Context context,
            String number,
            String title,
            String trackType) {
        LinearLayout identity = row(context);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.setPadding(dp(context, 2), 0, dp(context, 2), 0);

        if ("MAIN".equalsIgnoreCase(trackType)) {
            TextView mainGlyph = text(context, "M", 9, MUTED);
            mainGlyph.setTypeface(Typeface.DEFAULT_BOLD);
            mainGlyph.setGravity(Gravity.CENTER);
            mainGlyph.setContentDescription("MPC XL main output strip");
            identity.addView(mainGlyph, new LayoutParams(dp(context, 24), dp(context, 26)));
        } else {
            ImageView icon = new ImageView(context);
            MpcTrackTypeIconDrawable drawable = trackTypeIcon(trackType);
            drawable.setSelected("DRUM".equalsIgnoreCase(trackType));
            drawable.setEnabledState(!"".equals(trackType));
            icon.setImageDrawable(drawable);
            icon.setContentDescription("MPC XL track type " + clean(trackType, "reserved"));
            identity.addView(icon, new LayoutParams(dp(context, 24), dp(context, 26)));
        }

        LinearLayout labels = new LinearLayout(context);
        labels.setOrientation(VERTICAL);
        labels.setGravity(Gravity.CENTER_VERTICAL);

        TextView name = text(
                context,
                number + "  " + clean(title, "—"),
                9,
                TEXT);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        labels.addView(name, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 22)));

        TextView type = text(
                context,
                clean(trackType, "TYPE RESERVED"),
                7,
                MUTED);
        labels.addView(type, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 14)));

        identity.addView(labels, new LayoutParams(0, dp(context, 38), 1));
        strip.addView(identity, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 40)));
    }

    private void addProgramBand(
            LinearLayout strip,
            Context context,
            String value,
            String caption) {
        LinearLayout band = row(context);
        band.setPadding(dp(context, 4), 0, dp(context, 3), 0);
        TextView left = text(context, caption, 7, MUTED);
        left.setTypeface(Typeface.DEFAULT_BOLD);
        band.addView(left, new LayoutParams(dp(context, 46), dp(context, 28)));

        TextView right = text(context, value, 8, TEXT);
        right.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        band.addView(right, new LayoutParams(0, dp(context, 28), 1));
        band.setBackgroundColor(PANEL_DARK);
        strip.addView(band, new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 28)));
    }

    private void addTabs(LinearLayout strip, String[] tabs) {
        LinearLayout tabsRow = new LinearLayout(getContext());
        tabsRow.setOrientation(HORIZONTAL);
        tabsRow.setGravity(Gravity.CENTER_VERTICAL);
        tabsRow.setBackgroundColor(PANEL);

        for (String tabName : tabs) {
            LinearLayout tab = new LinearLayout(getContext());
            tab.setOrientation(VERTICAL);
            tab.setGravity(Gravity.CENTER);
            tab.setContentDescription(
                    "MPC XL " + tabName + " mixer tab"
                            + ("LVL".equals(tabName) ? " active" : " unavailable"));
            tab.setEnabled("LVL".equals(tabName));

            TextView label = text(
                    getContext(),
                    tabName,
                    7,
                    "LVL".equals(tabName) ? TEXT : MUTED);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            if (!"LVL".equals(tabName)) label.setAlpha(0.60f);
            tab.addView(label, new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 18)));

            View indicator = new View(getContext());
            indicator.setBackgroundColor("LVL".equals(tabName) ? RED : Color.TRANSPARENT);
            tab.addView(indicator, new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 2)));

            tabsRow.addView(
                    tab,
                    new LayoutParams(0, dp(getContext(), 20), 1));
        }

        strip.addView(
                tabsRow,
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(getContext(), 22)));
    }

    private LinearLayout baseStrip(Context context) {
        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(VERTICAL);
        strip.setPadding(dp(context, 1), dp(context, 1), dp(context, 1), dp(context, 1));
        strip.setBackground(stroke(PANEL, LINE));
        return strip;
    }

    private TextView text(Context context, String value, int size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        return view;
    }

    private TextView valueLabel(Context context, String value, int color) {
        TextView view = text(context, value, 8, color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private TextView flatLabel(Context context, String value, int color) {
        TextView view = text(context, value, 7, color);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        return view;
    }

    private Button controlButton(Context context, String value, boolean active) {
        Button view = button(context, value, 7);
        view.setTextColor(active ? TEXT : MUTED);
        view.setBackground(stroke(active ? PANEL_DARK : PANEL_DARK, LINE));
        view.setAlpha(active ? 1.0f : 0.58f);
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

    private MpcTrackTypeIconDrawable trackTypeIcon(String trackType) {
        String value = clean(trackType, "DRUM").toUpperCase(Locale.ROOT);
        MpcTrackTypeIconDrawable.Type type;
        switch (value) {
            case "KEYGROUP":
                type = MpcTrackTypeIconDrawable.Type.KEYGROUP;
                break;
            case "PLUGIN":
                type = MpcTrackTypeIconDrawable.Type.PLUGIN;
                break;
            case "MIDI":
                type = MpcTrackTypeIconDrawable.Type.MIDI;
                break;
            case "CLIP":
                type = MpcTrackTypeIconDrawable.Type.CLIP;
                break;
            case "CV":
                type = MpcTrackTypeIconDrawable.Type.CV;
                break;
            default:
                type = MpcTrackTypeIconDrawable.Type.DRUM;
                break;
        }
        return new MpcTrackTypeIconDrawable(type);
    }

    private String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "NONE" : value.trim();
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
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

    private static final class MpcVerticalMeter extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;
        private boolean enabledState = true;
        private boolean dialFocus;

        MpcVerticalMeter(Context context) {
            super(context);
            setWillNotDraw(false);
            setContentDescription("MPC XL level meter and white-line fader");
        }

        void setValue(float value) {
            this.value = Math.max(0.0f, Math.min(1.0f, value));
            invalidate();
        }

        void setEnabledState(boolean enabled) {
            enabledState = enabled;
            invalidate();
        }

        void setDialFocus(boolean focused) {
            dialFocus = focused;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            final float w = getWidth();
            final float h = getHeight();
            final float meterLeft = w * 0.43f;
            final float meterRight = w * 0.53f;
            final float top = 8;
            final float bottom = h - 8;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(PANEL_DARK);
            canvas.drawRect(meterLeft, top, meterRight, bottom, paint);

            paint.setColor(enabledState ? METER : DISABLED);
            final float filled = (bottom - top) * value;
            canvas.drawRect(
                    meterLeft,
                    bottom - filled,
                    meterRight,
                    bottom,
                    paint);

            paint.setColor(LINE);
            paint.setStrokeWidth(1);
            for (int i = 0; i <= 8; i++) {
                final float y = top + (bottom - top) * (i / 8.0f);
                canvas.drawLine(w * 0.34f, y, w * 0.41f, y, paint);
                canvas.drawLine(w * 0.55f, y, w * 0.62f, y, paint);
            }

            // MPC-style white level line. It is intentionally dimmed while
            // the track/output backend is not yet writable.
            final float faderX = w * 0.72f;
            paint.setColor(enabledState ? WHITE : DISABLED);
            paint.setStrokeWidth(enabledState ? 3 : 2);
            final float faderY = bottom - (bottom - top) * value;
            canvas.drawLine(faderX - 15, faderY, faderX + 9, faderY, paint);
            canvas.drawLine(faderX, top, faderX, bottom, paint);

            paint.setStrokeWidth(1);
            paint.setColor(MUTED);
            paint.setTextSize(Math.max(7, getResources().getDisplayMetrics().scaledDensity * 7));
            canvas.drawText("0", w * 0.13f, bottom + 1, paint);
            canvas.drawText("-∞", w * 0.13f, top + 8, paint);

            if (dialFocus) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(RED);
                paint.setStrokeWidth(2);
                canvas.drawRect(1, 1, w - 1, h - 1, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    private static final class MpcPanSlider extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value;
        private boolean enabledState = true;
        private boolean dialFocus;

        MpcPanSlider(Context context) {
            super(context);
            setWillNotDraw(false);
        }

        void setValue(float value) {
            this.value = Math.max(-1.0f, Math.min(1.0f, value));
            invalidate();
        }

        void setEnabledState(boolean enabled) {
            enabledState = enabled;
            invalidate();
        }

        void setDialFocus(boolean focused) {
            dialFocus = focused;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final float w = getWidth();
            final float y = getHeight() * 0.52f;
            final float left = w * 0.16f;
            final float right = w * 0.84f;
            final float center = (left + right) * 0.5f;
            final float knobX = center + (right - left) * 0.5f * value;

            paint.setColor(enabledState ? MUTED : DISABLED);
            paint.setStrokeWidth(2);
            canvas.drawLine(left, y, right, y, paint);
            paint.setStrokeWidth(1);
            canvas.drawLine(center, y - 6, center, y + 6, paint);

            paint.setColor(enabledState ? WHITE : DISABLED);
            canvas.drawCircle(knobX, y, 4.0f, paint);

            paint.setTextSize(Math.max(7, getResources().getDisplayMetrics().scaledDensity * 7));
            paint.setColor(MUTED);
            canvas.drawText("L", left, y - 7, paint);
            canvas.drawText("C", center - 3, y - 7, paint);
            canvas.drawText("R", right - 6, y - 7, paint);

            if (dialFocus) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(RED);
                paint.setStrokeWidth(2);
                canvas.drawRect(1, 1, w - 1, getHeight() - 1, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }
}
