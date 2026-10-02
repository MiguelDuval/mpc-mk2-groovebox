package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Controller-first Track Edit workspace.
 *
 * This view owns presentation and tab-local interaction only. It consumes a
 * control-thread snapshot supplied by MainActivity and returns semantic edit
 * requests through Listener. It never talks to native/realtime code directly.
 */
final class MpcTrackEditView extends LinearLayout {
    interface Listener {
        void onBack();
        void onAudition();
        void onLayerDelta(int delta);
        void onLayerGainDelta(float delta);
        void onLayerTuningDelta(float delta);
        void onLayerPan(float pan);
        void onLayerVelocityMinDelta(int delta);
        void onLayerVelocityMaxDelta(int delta);
        void onRegionStartDelta(long delta);
        void onRegionEndDelta(long delta);
        void onRegionSelection(float startNormalized, float endNormalized);
        void onPadTuningDelta(float delta);
        void onPadLevelDelta(float delta);
        void onPadPan(float pan);
        void onEnvelopeDelta(float attack, float decay, float sustain, float release);
        void onEnvelopeReset();
        void onFilterDelta(float deltaHz);
        void onFilterSet(float cutoffHz);
    }

    static final class Snapshot {
        final boolean ready;
        final boolean drumTrack;
        final int trackIndex;
        final String trackType;
        final String trackStatus;
        final String programName;
        final int selectedPad;
        final int selectedLayer;
        final String sampleName;
        final long sampleFrames;
        final int sampleRate;
        final long sampleStart;
        final long sampleEnd;
        final float layerGain;
        final float layerTuning;
        final float layerPan;
        final int velocityMin;
        final int velocityMax;
        final float padTuning;
        final float padLevel;
        final float padPan;
        final float envelopeAttack;
        final float envelopeDecay;
        final float envelopeSustain;
        final float envelopeRelease;
        final float filterCutoff;
        final float[] waveformPeaks;

        Snapshot(
                boolean ready,
                boolean drumTrack,
                int trackIndex,
                String trackType,
                String trackStatus,
                String programName,
                int selectedPad,
                int selectedLayer,
                String sampleName,
                long sampleFrames,
                int sampleRate,
                long sampleStart,
                long sampleEnd,
                float layerGain,
                float layerTuning,
                float layerPan,
                int velocityMin,
                int velocityMax,
                float padTuning,
                float padLevel,
                float padPan,
                float envelopeAttack,
                float envelopeDecay,
                float envelopeSustain,
                float envelopeRelease,
                float filterCutoff,
                float[] waveformPeaks) {
            this.ready = ready;
            this.drumTrack = drumTrack;
            this.trackIndex = Math.max(0, trackIndex);
            this.trackType = safe(trackType, "UNKNOWN");
            this.trackStatus = safe(trackStatus, "Track unavailable");
            this.programName = safe(programName, "—");
            this.selectedPad = Math.max(0, Math.min(15, selectedPad));
            this.selectedLayer = Math.max(0, Math.min(7, selectedLayer));
            this.sampleName = safe(sampleName, "NO SAMPLE");
            this.sampleFrames = Math.max(0L, sampleFrames);
            this.sampleRate = Math.max(0, sampleRate);
            this.sampleStart = Math.max(0L, sampleStart);
            this.sampleEnd = Math.max(0L, sampleEnd);
            this.layerGain = layerGain;
            this.layerTuning = layerTuning;
            this.layerPan = layerPan;
            this.velocityMin = Math.max(0, Math.min(127, velocityMin));
            this.velocityMax = Math.max(this.velocityMin,
                    Math.min(127, velocityMax));
            this.padTuning = padTuning;
            this.padLevel = padLevel;
            this.padPan = padPan;
            this.envelopeAttack = envelopeAttack;
            this.envelopeDecay = envelopeDecay;
            this.envelopeSustain = envelopeSustain;
            this.envelopeRelease = envelopeRelease;
            this.filterCutoff = filterCutoff;
            this.waveformPeaks = waveformPeaks;
        }

        private static String safe(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }
    }

    private enum Tab {
        GLOBAL, SAMPLES, ENVELOPES, LFO, MODULATIONS, EFFECTS
    }

    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);
    private static final int SURFACE_2 = Color.rgb(32, 37, 42);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    // MPC-style selected/latched state uses a red accent.\n    private static final int ACCENT = Color.rgb(221, 52, 52);

    private final Listener listener;
    private final Button[] tabButtons = new Button[Tab.values().length];
    private final TextView titleView;
    private final TextView contextView;
    private final LinearLayout body;
    pri    MpcTrackEditView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setPadding(dp(6), dp(4), dp(6), dp(2));

        // MPC Track Edit uses a compact context header, not a second page title.
        LinearLayout header = row();

        contextView = field("TRACK --");
        contextView.setContentDescription("Track Edit Track context");
        contextView.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(contextView, new LinearLayout.LayoutParams(0, dp(42), 1.0f));

        padContextView = field("PAD --");
        padContextView.setContentDescription("Track Edit Pad context");
        padContextView.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams padLp = new LinearLayout.LayoutParams(dp(82), dp(42));
        padLp.leftMargin = dp(4);
        header.addView(padContextView, padLp);

        editAllLayersButton = actionButton("EDIT ALL LAYERS", null);
        editAllLayersButton.setEnabled(false);
        editAllLayersButton.setAlpha(0.42f);
        editAllLayersButton.setContentDescription("Track Edit Edit All Layers RESERVED");
        LinearLayout.LayoutParams allLayersLp = new LinearLayout.LayoutParams(dp(124), dp(42));
        allLayersLp.leftMargin = dp(4);
        header.addView(editAllLayersButton, allLayersLp);

        Button back = actionButton("BACK", v -> this.listener.onBack());
        back.setContentDescription("Track Edit back");
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(60), dp(42));
        backLp.leftMargin = dp(4);
        header.addView(back, backLp);

        addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        body = column();
        body.setPadding(0, dp(4), 0, dp(4));
        scroll.addView(body, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // Keep the parameter context switch persistent at the bottom, outside
        // the scrollable editor body, matching the MPC workflow.
        LinearLayout tabs = row();
        tabs.setPadding(0, dp(2), 0, 0);
        String[] labels = {"GLOBAL", "SAMPLES", "AMP ENV", "LFO", "MODS", "EFFECTS"};
        String[] descriptions = {
                "Track Edit Global tab",
                "Track Edit Samples tab",
                "Track Edit AMP ENV tab",
                "Track Edit LFO tab",
                "Track Edit Modulations tab",
                "Track Edit Effects tab"
        };
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button button = actionButton(labels[i], v -> {
                tab = Tab.values()[index];
                render();
            });
            button.setContentDescription(descriptions[i]);
            tabButtons[i] = button;
            tabs.addView(button, new LinearLayout.LayoutParams(
                    0, dp(42), 1f));
        }
        tabs.setContentDescription("Track Edit bottom tab bar");
        addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));
    }

    void bind(Snapshot snapshot) {
        this.snapshot = snapshot;
        render();
    }

    private void render() {
        if (snapshot == null) return;

        contextView.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s",
                snapshot.trackIndex + 1,
                snapshot.trackType));
        padContextView.setText(String.format(
                Locale.ROOT,
                "PAD %02d",
                snapshot.selectedPad + 1));
        editAllLayersButton.setContentDescription(
                "Track Edit Edit All Layers RESERVED");

        for (int i = 0; i < tabButtons.length; i++) {
            final boolean active = Tab.values()[i] == tab;
            tabButtons[i].setText(new String[]{
                    "GLOBAL", "SAMPLES", "AMP ENV", "LFO", "MODS", "EFFECTS"
            }[i]);
            tabButtons[i].setTextColor(active ? BG : TEXT);
            tabButtons[i].setBackground(strokeBackground(
                    active ? ACCENT : SURFACE_2,
                    active ? ACCENT : LINE,
                    5));
        }

        body.removeAllViews();
        switch (tab) {
            case GLOBAL: renderGlobal(); break;
            case SAMPLES: renderSamples(); break;
            case ENVELOPES: renderEnvelopes(); break;
            case LFO:
                renderReserved("TRACK EDIT LFO",
                        "LFO parameters are reserved until a truthful LFO "
                                + "semantic/backend contract exists.");
                break;
            case MODULATIONS:
                renderReserved("TRACK EDIT MODULATIONS",
                        "Modulation routing is reserved until explicit "
                                + "source/destination semantics exist.");
                break;
            case EFFECTS:
                renderReserved("TRACK EDIT EFFECTS",
                        "Insert/track effects are reserved until the effect "
                                + "routing and parameter backend is implemented.");
                break;
        }
    }

    private void renderGlobal() {
        section("GLOBAL");
        body.addView(infoField("TRACK", String.format(
                Locale.ROOT, "Track %02d • %s",
                snapshot.trackIndex + 1, snapshot.trackType)));
        body.addView(infoField("STATUS", snapshot.trackStatus));

        if (!snapshot.drumTrack) {
            body.addView(reservedMessage(
                    "DRUM TRACK FEATURES RESERVED",
                    "Samples, amp envelope and pad-layer editing are unavailable "
                            + "for this Track Type in the current backend."));
            return;
        }

        body.addView(sectionLabel("PAD GLOBAL"));
        body.addView(infoField("PAD", String.format(
                Locale.ROOT, "PAD %02d • %s",
                snapshot.selectedPad + 1, snapshot.programName)));
        body.addView(parameterRow(
                "TUNE", formatSigned(snapshot.padTuning) + " st", "−", "+",
                v -> listener.onPadTuningDelta(-1f),
                v -> listener.onPadTuningDelta(1f)));
        body.addView(parameterRow(
                "LEVEL", String.format(Locale.ROOT, "%.0f%%", snapshot.padLevel * 100f),
                "−", "+",
                v -> listener.onPadLevelDelta(-0.10f),
                v -> listener.onPadLevelDelta(0.10f)));
        body.addView(parameterRow(
                "PAN", formatPan(snapshot.padPan), "L", "R",
                v -> listener.onPadPan(-1f),
                v -> listener.onPadPan(1f)));
        body.addView(actionButton("CENTER PAN", v -> listener.onPadPan(0f)), height(40));
    }

    private void renderSamples() {
        section("SAMPLES");
        if (!snapshot.drumTrack) {
            body.addView(reservedMessage(
                    "SAMPLES • RESERVED / UNAVAILABLE",
                    "Selected Track is " + snapshot.trackType
                            + ". The current sample-layer backend is Drum-only."));
            return;
        }

        LinearLayout layerRow = row();
        layerRow.addView(actionButton("LAYER −", v -> listener.onLayerDelta(-1)),
                new LinearLayout.LayoutParams(dp(76), dp(42)));
        TextView layer = field(String.format(Locale.ROOT,
                "LAYER %d/8", snapshot.selectedLayer + 1));
        layer.setContentDescription("Track Edit selected layer");
        layerRow.addView(layer, new LinearLayout.LayoutParams(0, dp(42), 1f));
        layerRow.addView(actionButton("LAYER +", v -> listener.onLayerDelta(1)),
                new LinearLayout.LayoutParams(dp(76), dp(42)));
        Button allLayers = actionButton("EDIT ALL LAYERS", null);
        allLayers.setEnabled(false);
        allLayers.setAlpha(0.42f);
        allLayers.setContentDescription("Track Edit Edit All Layers RESERVED");
        layerRow.addView(allLayers, new LinearLayout.LayoutParams(dp(116), dp(42)));
        body.addView(layerRow);

        body.addView(infoField("SAMPLE",
                snapshot.sampleFrames > 0
                        ? snapshot.sampleName
                        : "NO SAMPLE • ASSIGN FROM BROWSER / SAMPLER"));

        WaveformView waveform = new WaveformView(getContext());
        waveform.setContentDescription("Track Edit Samples waveform");
        waveform.setEditable(snapshot.sampleFrames > 0);
        waveform.setMinimumHeight(dp(126));
        if (snapshot.sampleFrames > 0 && snapshot.waveformPeaks != null) {
            waveform.setPeaks(snapshot.waveformPeaks);
            waveform.setSelection(
                    clamp01((float) snapshot.sampleStart / snapshot.sampleFrames),
                    clamp01((float) snapshot.sampleEnd / snapshot.sampleFrames));
            waveform.setDurationMs(
                    snapshot.sampleRate > 0
                            ? snapshot.sampleFrames * 1000.0f / snapshot.sampleRate
                            : 0.0f);
            waveform.setOnSelectionCommitListener(
                    (startNormalized, endNormalized) ->
                            listener.onRegionSelection(startNormalized, endNormalized));
        } else {
            waveform.setPeaks(null);
            waveform.setSelection(0f, 1f);
            waveform.setDurationMs(0f);
        }
        body.addView(waveform, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(134)));

        body.addView(infoField("REGION",
                snapshot.sampleFrames > 0
                        ? String.format(Locale.ROOT,
                                "S %d • E %d • %d frames",
                                snapshot.sampleStart, snapshot.sampleEnd, snapshot.sampleFrames)
                        : "NO SAMPLE"));

        LinearLayout region = row();
        region.addView(actionButton("S −1K", v -> listener.onRegionStartDelta(-1000)), weight());
        region.addView(actionButton("S +1K", v -> listener.onRegionStartDelta(1000)), weight());
        region.addView(actionButton("E −1K", v -> listener.onRegionEndDelta(-1000)), weight());
        region.addView(actionButton("E +1K", v -> listener.onRegionEndDelta(1000)), weight());
        body.addView(region, height(42));

        body.addView(parameterRow(
                "GAIN", String.format(Locale.ROOT, "%.0f%%", snapshot.layerGain * 100f), "−", "+",
                v -> listener.onLayerGainDelta(-0.10f),
                v -> listener.onLayerGainDelta(0.10f)));
        body.addView(parameterRow(
                "TUNE", formatSigned(snapshot.layerTuning) + " st", "−", "+",
                v -> listener.onLayerTuningDelta(-1f),
                v -> listener.onLayerTuningDelta(1f)));
        body.addView(parameterRow(
                "PAN", formatPan(snapshot.layerPan), "L", "R",
                v -> listener.onLayerPan(-1f),
                v -> listener.onLayerPan(1f)));
        body.addView(actionButton("CENTER LAYER PAN", v -> listener.onLayerPan(0f)), height(40));

        body.addView(infoField("VELOCITY", String.format(
                Locale.ROOT, "%d — %d", snapshot.velocityMin, snapshot.velocityMax)));
        LinearLayout velocity = row();
        velocity.addView(parameterButton("MIN −", v -> listener.onLayerVelocityMinDelta(-1)));
        velocity.addView(parameterButton("MIN +", v -> listener.onLayerVelocityMinDelta(1)));
        velocity.addView(parameterButton("MAX −", v -> listener.onLayerVelocityMaxDelta(-1)));
        velocity.addView(parameterButton("MAX +", v -> listener.onLayerVelocityMaxDelta(1)));
        body.addView(velocity, height(42));

        body.addView(actionButton("AUDITION", v -> listener.onAudition()), height(42));
    }

    private void renderEnvelopes() {
        section("ENVELOPES");
        if (!snapshot.drumTrack) {
            body.addView(reservedMessage(
                    "ENVELOPES • RESERVED / UNAVAILABLE",
                    "Amp envelope and filter are currently exposed only for "
                            + "the implemented Drum pad backend."));
            return;
        }

        body.addView(infoField("PAD", String.format(
                Locale.ROOT, "PAD %02d • AMP ENVELOPE", snapshot.selectedPad + 1)));
        body.addView(parameterRow(
                "ATTACK", String.format(Locale.ROOT, "%.0f ms", snapshot.envelopeAttack),
                "−", "+",
                v -> listener.onEnvelopeDelta(-100, 0, 0, 0),
                v -> listener.onEnvelopeDelta(100, 0, 0, 0)));
        body.addView(parameterRow(
                "DECAY", String.format(Locale.ROOT, "%.0f ms", snapshot.envelopeDecay),
                "−", "+",
                v -> listener.onEnvelopeDelta(0, -100, 0, 0),
                v -> listener.onEnvelopeDelta(0, 100, 0, 0)));
        body.addView(parameterRow(
                "SUSTAIN", String.format(Locale.ROOT, "%.0f%%", snapshot.envelopeSustain * 100f),
                "−", "+",
                v -> listener.onEnvelopeDelta(0, 0, -0.10f, 0),
                v -> listener.onEnvelopeDelta(0, 0, 0.10f, 0)));
        body.addView(parameterRow(
                "RELEASE", String.format(Locale.ROOT, "%.0f ms", snapshot.envelopeRelease),
                "−", "+",
                v -> listener.onEnvelopeDelta(0, 0, 0, -100),
                v -> listener.onEnvelopeDelta(0, 0, 0, 100)));
        body.addView(actionButton("ENVELOPE RESET", v -> listener.onEnvelopeReset()), height(42));

        body.addView(sectionLabel("FILTER"));
        body.addView(infoField("CUTOFF", formatCutoff(snapshot.filterCutoff)));
        LinearLayout filter = row();
        filter.addView(parameterButton("−500", v -> listener.onFilterDelta(-500)));
        filter.addView(parameterButton("+500", v -> listener.onFilterDelta(500)));
        filter.addView(parameterButton("3K", v -> listener.onFilterSet(3000)));
        filter.addView(parameterButton("6K", v -> listener.onFilterSet(6000)));
        filter.addView(parameterButton("12K", v -> listener.onFilterSet(12000)));
        filter.addView(parameterButton("20K", v -> listener.onFilterSet(20000)));
        body.addView(filter, height(42));
    }

    private void renderReserved(String heading, String message) {
        section(heading);
        body.addView(reservedMessage("RESERVED / UNAVAILABLE", message));
        body.addView(infoField("CURRENT CONTEXT", String.format(
                Locale.ROOT, "TRACK %02d • %s • PAD %02d",
                snapshot.trackIndex + 1, snapshot.trackType, snapshot.selectedPad + 1)));
    }

    private void section(String title) {
        TextView heading = label(title, 9, MUTED);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setPadding(dp(8), dp(5), dp(8), dp(3));
        body.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));
    }

    private TextView sectionLabel(String title) {
        TextView view = label(title, 9, MUTED);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView infoField(String title, String value) {
        TextView view = label("", 11, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setText(title + "\n" + value);
        view.setContentDescription("Track Edit " + title.toLowerCase(Locale.ROOT));
        view.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        lp.bottomMargin = dp(4);
        return view;
    }

    private TextView field(String value) {
        TextView view = label(value, 11, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        return view;
    }

    private LinearLayout parameterRow(
            String title,
            String value,
            String left,
            String right,
            View.OnClickListener leftAction,
            View.OnClickListener rightAction) {
        LinearLayout row = row();
        TextView valueField = field(title + "\n" + value);
        row.addView(valueField, new LinearLayout.LayoutParams(0, dp(42), 1.5f));
        row.addView(parameterButton(left, leftAction),
                new LinearLayout.LayoutParams(0, dp(42), 0.65f));
        row.addView(parameterButton(right, rightAction),
                new LinearLayout.LayoutParams(0, dp(42), 0.65f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42));
        lp.bottomMargin = dp(4);
        row.setLayoutParams(lp);
        return row;
    }

    private Button parameterButton(String text, View.OnClickListener listener) {
        return actionButton(text, listener);
    }

    private Button actionButton(String text, View.OnClickListener listener) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(10);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(TEXT);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), 0, dp(4), 0);
        button.setGravity(Gravity.CENTER);
        button.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        if (listener != null) button.setOnClickListener(listener);
        return button;
    }

    private TextView label(String text, float size, int color) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout row() {
        LinearLayout view = new LinearLayout(getContext());
        view.setOrientation(HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LinearLayout column() {
        LinearLayout view = new LinearLayout(getContext());
        view.setOrientation(VERTICAL);
        return view;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1f);
        lp.leftMargin = dp(2);
        lp.rightMargin = dp(2);
        return lp;
    }

    private LinearLayout.LayoutParams height(int value) {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(value));
    }

    private TextView reservedMessage(String title, String message) {
        TextView view = label(title + "\n" + message, 11, MUTED);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(10), dp(7), dp(10), dp(7));
        view.setContentDescription(title);
        view.setBackground(strokeBackground(SURFACE, LINE, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(5);
        return view;
    }

    private android.graphics.drawable.GradientDrawable strokeBackground(
            int fill, int stroke, int radiusDp) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.max(1, Math.round(value * getResources()
                .getDisplayMetrics().density));
    }

    private static String tabContentDescription(Tab value) {
        switch (value) {
            case GLOBAL: return "Global tab";
            case SAMPLES: return "Samples tab";
            case ENVELOPES: return "Envelopes tab";
            case LFO: return "LFO tab";
            case MODULATIONS: return "Modulations tab";
            case EFFECTS: return "Effects tab";
            default: return "tab";
        }
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static String formatSigned(float value) {
        return String.format(Locale.ROOT, "%+.1f", value);
    }

    private static String formatPan(float value) {
        if (Math.abs(value) < 0.01f) return "C";
        return (value < 0 ? "L" : "R") + Math.round(Math.abs(value) * 100f);
    }

    private static String formatCutoff(float value) {
        if (value >= 1000f) {
            return String.format(Locale.ROOT, "%.1f kHz", value / 1000f);
        }
        return String.format(Locale.ROOT, "%.0f Hz", value);
    }
}
