package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * MPC-style Channel Mixer workspace.
 *
 * This component owns only presentation and touch affordances. Musical state
 * remains owned by MainActivity/native audio, exposed through Listener.
 * Shows per-track level faders and pan controls mirroring the MPC One / OS 3.9
 * Channel Mixer.
 */
final class MpcChannelMixerView extends LinearLayout {
    enum ControlFocus {
        LEVEL,
        PAN
    }

    interface Listener {
        int selectedTrack();
        int trackCount();
        String trackLabel(int trackIndex);
        String trackType(int trackIndex);
        float trackLevel(int trackIndex);
        float trackPan(int trackIndex);

        void onTrackSelected(int track);
        void onControlFocus(int track, ControlFocus focus);
        void onTrackLevelSet(int track, float value);
        void onTrackPanDelta(int track, float delta);
    }

    /**
     * Maps {@link MpcUiState.DataDialFocus} to {@link ControlFocus} explicitly.
     * CHANNEL_MIXER_LEVEL -> LEVEL, CHANNEL_MIXER_PAN -> PAN.
     * No unsafe casts; only the two channel-mixer-relevant values are mapped.
     */
    static ControlFocus fromDataDialFocus(MpcUiState.DataDialFocus focus) {
        switch (focus) {
            case CHANNEL_MIXER_LEVEL:
                return ControlFocus.LEVEL;
            case CHANNEL_MIXER_PAN:
                return ControlFocus.PAN;
            default:
                return ControlFocus.LEVEL;
        }
    }

    private static final int BG = Color.rgb(14, 16, 18);
    private static final int PANEL = Color.rgb(28, 31, 34);
    private static final int PANEL_2 = Color.rgb(39, 43, 47);
    private static final int BORDER = Color.rgb(75, 82, 88);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int RED = Color.rgb(224, 30, 61);
    private static final int WHITE = Color.WHITE;

    private final Listener listener;
    private final LinearLayout stripRow;
    private ControlFocus controlFocus = ControlFocus.LEVEL;
    private final Strip[] strips = new Strip[128];
    private TextView headerTitle;
    private TextView focusHeader;

    MpcChannelMixerView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Channel Mixer");

        addView(buildHeader(context),
                new LayoutParams(LayoutParams.MATCH_PARENT, dp(context, 42)));

        HorizontalScrollView scroll = new HorizontalScrollView(context);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        scroll.setBackgroundColor(BG);

        stripRow = new LinearLayout(context);
        stripRow.setOrientation(HORIZONTAL);
        stripRow.setGravity(Gravity.TOP);
        stripRow.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        for (int i = 0; i < 128; i++) {
            strips[i] = new Strip(context, i);
            stripRow.addView(strips[i], new LayoutParams(
                    dp(context, 92), LayoutParams.MATCH_PARENT));
        }

        scroll.addView(stripRow, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        addView(scroll, new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void refresh(int selectedTrack) {
        final int count = listener.trackCount();
        headerTitle.setText(String.format(
                Locale.ROOT,
                "CHANNEL MIXER • %d TRACKS",
                count));
        focusHeader.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s • DATA DIAL",
                selectedTrack + 1,
                controlFocusLabel()));

        for (int i = 0; i < strips.length; i++) {
            strips[i].setVisible(i < count);
            strips[i].refresh(selectedTrack);
        }
    }

    void setControlFocus(ControlFocus focus) {
        controlFocus = focus == null ? ControlFocus.LEVEL : focus;
        focusHeader.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s • DATA DIAL",
                listener.selectedTrack() + 1,
                controlFocusLabel()));
        refreshAll();
    }

    ControlFocus controlFocus() {
        return controlFocus;
    }

    private String controlFocusLabel() {
        switch (controlFocus) {
            case PAN:
                return "PAN";
            case LEVEL:
            default:
                return "LEVEL";
        }
    }

    private LinearLayout buildHeader(Context context) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(context, 6), 0, dp(context, 6), 0);
        header.setBackgroundColor(PANEL);

        TextView title = text(context, "CHANNEL MIXER", 13, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LayoutParams(
                dp(context, 120), LayoutParams.MATCH_PARENT));

        headerTitle = text(context, "CHANNEL MIXER • 0 TRACKS", 9, TEXT);
        headerTitle.setTypeface(Typeface.DEFAULT_BOLD);
        headerTitle.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(headerTitle, new LayoutParams(
                0, LayoutParams.MATCH_PARENT, 1f));

        focusHeader = text(context, "TRACK 01 • DATA DIAL / SELECT", 8, MUTED);
        focusHeader.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(focusHeader, new LayoutParams(
                dp(context, 150), LayoutParams.MATCH_PARENT));

        return header;
    }

    private final class Strip extends LinearLayout {
        private final int track;
        private final TextView title;
        private final TextView levelValue;
        private final TextView panValue;
        private final Fader fader;
        private boolean visible = true;

        Strip(Context context, int track) {
            super(context);
            this.track = track;
            setOrientation(VERTICAL);
            setGravity(Gravity.TOP);
            setPadding(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3));
            setBackground(frame(context, PANEL_2, BORDER, 0));

            title = text(context, String.format(Locale.ROOT, "TR %02d", track + 1), 10, TEXT);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 0);
            title.setOnClickListener(v -> selectTrack());
            addView(title, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 28)));

            fader = new Fader(context);
            fader.setContentDescription("TRACK " + (track + 1) + " vertical fader");
            fader.setOnValueChanged(value -> {
                listener.onControlFocus(track, ControlFocus.LEVEL);
                listener.onTrackLevelSet(track, value);
                refreshAll();
            });
            addView(fader, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 142)));

            levelValue = text(context, "LVL —", 8, TEXT);
            levelValue.setGravity(Gravity.CENTER);
            addView(levelValue, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 24)));

            panValue = controlField(context, "PAN", "0.00");
            panValue.setOnClickListener(v ->
                    listener.onControlFocus(track, ControlFocus.PAN));
            addView(panValue, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 28)));

            LinearLayout panButtons = compactButtons(context, track, "PAN",
                    "−", "＋",
                    () -> {
                        listener.onControlFocus(track, ControlFocus.PAN);
                        listener.onTrackPanDelta(track, -0.05f);
                    },
                    () -> {
                        listener.onControlFocus(track, ControlFocus.PAN);
                        listener.onTrackPanDelta(track, 0.05f);
                    });
            addView(panButtons, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 27)));

            setOnClickListener(v -> selectTrack());
        }

        void setVisible(boolean visible) {
            this.visible = visible;
            setVisibility(visible ? VISIBLE : GONE);
        }

        void refresh(int selectedTrack) {
            final boolean selected = listener.selectedTrack() == track;
            setBackground(frame(getContext(),
                    selected ? RED : PANEL_2,
                    selected ? WHITE : BORDER,
                    0));
            title.setTextColor(selected ? WHITE : TEXT);

            final float level = listener.trackLevel(track);
            final float pan = listener.trackPan(track);
            fader.setValue(clamp(level, 0f, 1f), false);
            levelValue.setText(String.format(Locale.ROOT, "LVL %d%%",
                    Math.round(level * 100f)));
            panValue.setText(formatPan(pan));
        }

        private void selectTrack() {
            listener.onTrackSelected(track);
        }
    }

    private void refreshAll() {
        for (Strip strip : strips) {
            strip.refresh(listener.selectedTrack());
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static TextView text(Context context, String text, float size, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private static TextView controlField(Context context, String title, String value) {
        TextView view = new TextView(context);
        view.setText(title + " " + value);
        view.setTextSize(8);
        view.setTextColor(TEXT);
        view.setGravity(Gravity.CENTER);
        view.setSingleLine(true);
        return view;
    }

    private static LinearLayout compactButtons(Context context, int track, String label,
                                               String negText, String posText,
                                               Runnable negAction, Runnable posAction) {
        LinearLayout group = new LinearLayout(context);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER);
        group.setPadding(0, dp(context, 1), 0, dp(context, 1));

        Button neg = actionButton(context, negText, negAction);
        Button pos = actionButton(context, posText, posAction);

        group.addView(neg, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        group.addView(pos, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        return group;
    }

    private static Button actionButton(Context context, String text, Runnable action) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(8);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setTextColor(TEXT);
        button.setBackground(frame(context, MPC_PANEL_DARK, MPC_PANEL_BORDER, 0));
        button.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        if (action != null) button.setOnClickListener(v -> action.run());
        return button;
    }

    private static int MPC_PANEL_DARK = Color.rgb(28, 31, 34);
    private static int MPC_PANEL_BORDER = Color.rgb(75, 82, 88);

    private static GradientDrawable frame(Context context, int fill, int stroke,
                                          int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), stroke);
        return drawable;
    }

    private static String formatPan(float value) {
        if (Math.abs(value) < 0.01f) return "C";
        return (value < 0 ? "L" : "R") + Math.round(Math.abs(value) * 100f);
    }

    private final class Fader extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value = 0.8f;
        private FaderCallback callback;

        Fader(Context context) {
            super(context);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            setWillNotDraw(false);
        }

        void setOnValueChanged(FaderCallback callback) {
            this.callback = callback;
        }

        void setValue(float value, boolean notify) {
            this.value = clamp(value, 0f, 1f);
            invalidate();
            if (notify && callback != null) {
                callback.onValueChanged(this.value);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final float cx = getWidth() * 0.5f;
            final float top = 8f;
            final float bottom = Math.max(top + 1f, getHeight() - 8f);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(20, 22, 24));
            canvas.drawRect(cx - 7f, top, cx + 7f, bottom, paint);

            final float y = bottom - (bottom - top) * value;
            paint.setColor(RED);
            canvas.drawRect(cx - 7f, y, cx + 7f, bottom, paint);

            handle.setStyle(Paint.Style.FILL);
            handle.setColor(WHITE);
            canvas.drawRect(cx - 13f, y - 2f, cx + 13f, y + 2f, handle);

            paint.setTextSize(7f);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(MUTED);
            canvas.drawText("MAX", cx, top - 1f + paint.getTextSize(), paint);
            canvas.drawText("MIN", cx, bottom + paint.getTextSize() + 1f, paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    final float top = 8f;
                    final float bottom = Math.max(top + 1f, getHeight() - 8f);
                    final float next = 1f - ((event.getY() - top) / (bottom - top));
                    setValue(next, true);
                    return true;
                case MotionEvent.ACTION_UP:
                    performClick();
                    return true;
                default:
                    return true;
            }
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    private interface FaderCallback {
        void onValueChanged(float value);
    }
}