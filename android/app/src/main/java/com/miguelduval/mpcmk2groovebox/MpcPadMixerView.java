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
 * MPC-style Pad Mixer workspace.
 *
 * This component owns only presentation and touch affordances. Musical state
 * remains owned by MainActivity/native audio, exposed through Listener.
 */
final class MpcPadMixerView extends LinearLayout {
    enum ControlFocus {
        LEVEL,
        PAN,
        TUNE
    }

    interface Listener {
        int selectedPad();
        float padLevel(int pad);
        float padPan(int pad);
        float padTuning(int pad);
        String padSampleName(int pad);

        void onPadSelected(int pad);
        void onControlFocus(int pad, ControlFocus focus);
        void onPadLevelSet(int pad, float value);
        void onPadPanDelta(int pad, float delta);
        void onPadTuningDelta(int pad, float delta);
    }

    private static final int VISIBLE_STRIPS = 8;

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
    private final Strip[] strips = new Strip[16];
    private TextView trackHeader;
    private TextView focusHeader;

    MpcPadMixerView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Pad Mixer");

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

        for (int i = 0; i < 16; i++) {
            strips[i] = new Strip(context, i);
            stripRow.addView(strips[i], new LayoutParams(
                    dp(context, 92), LayoutParams.MATCH_PARENT));
        }

        scroll.addView(stripRow, new HorizontalScrollView.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        addView(scroll, new LayoutParams(
                LayoutParams.MATCH_PARENT, 0, 1f));
    }

    void refresh(String trackLabel, String programLabel, int selectedPad) {
        trackHeader.setText(String.format(
                Locale.ROOT,
                "TRACK %s • DRUM • %s",
                trackLabel == null ? "01" : trackLabel,
                programLabel == null || programLabel.isEmpty() ? "PROGRAM —" : programLabel));
        focusHeader.setText(String.format(
                Locale.ROOT,
                "PAD %02d • %s • DATA DIAL",
                selectedPad + 1,
                controlFocusLabel()));
        for (Strip strip : strips) {
            strip.refresh();
        }
    }

    void setControlFocus(ControlFocus focus) {
        controlFocus = focus == null ? ControlFocus.LEVEL : focus;
        focusHeader.setText(String.format(
                Locale.ROOT,
                "PAD %02d • %s • DATA DIAL",
                listener.selectedPad() + 1,
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
            case TUNE:
                return "TUNE";
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

        TextView title = text(context, "PAD MIXER", 13, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(title, new LayoutParams(
                dp(context, 88), LayoutParams.MATCH_PARENT));

        trackHeader = text(context, "TRACK 01 • DRUM • PROGRAM", 9, TEXT);
        trackHeader.setTypeface(Typeface.DEFAULT_BOLD);
        trackHeader.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(trackHeader, new LayoutParams(
                0, LayoutParams.MATCH_PARENT, 1f));

        focusHeader = text(context, "PAD 01 • DATA DIAL / SELECT", 8, MUTED);
        focusHeader.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        header.addView(focusHeader, new LayoutParams(
                dp(context, 150), LayoutParams.MATCH_PARENT));

        return header;
    }

    private final class Strip extends LinearLayout {
        private final int pad;
        private final TextView title;
        private final TextView sample;
        private final Fader fader;
        private final TextView levelValue;
        private final TextView panValue;
        private final TextView tuneValue;

        Strip(Context context, int pad) {
            super(context);
            this.pad = pad;
            setOrientation(VERTICAL);
            setGravity(Gravity.TOP);
            setPadding(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3));
            setBackground(frame(PANEL_2, BORDER, 0));

            title = text(context, String.format(Locale.ROOT, "PAD %02d", pad + 1), 10, TEXT);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            title.setGravity(Gravity.CENTER);
            title.setPadding(0, 0, 0, 0);
            title.setOnClickListener(v -> selectPad());
            addView(title, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 30)));

            sample = text(context, "NO SAMPLE", 7, MUTED);
            sample.setGravity(Gravity.CENTER);
            sample.setSingleLine(true);
            addView(sample, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 28)));

            fader = new Fader(context);
            fader.setContentDescription("PAD " + (pad + 1) + " vertical fader");
            fader.setOnValueChanged(value -> {
                listener.onControlFocus(pad, ControlFocus.LEVEL);
                listener.onPadLevelSet(pad, value);
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
                    listener.onControlFocus(pad, ControlFocus.PAN));
            addView(panValue, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 28)));

            LinearLayout panButtons = compactButtons(context,
                    "−", "＋",
                    () -> {
                        listener.onControlFocus(pad, ControlFocus.PAN);
                        listener.onPadPanDelta(pad, -0.05f);
                    },
                    () -> {
                        listener.onControlFocus(pad, ControlFocus.PAN);
                        listener.onPadPanDelta(pad, 0.05f);
                    });
            addView(panButtons, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 27)));

            tuneValue = controlField(context, "TUNE", "0.0");
            tuneValue.setOnClickListener(v ->
                    listener.onControlFocus(pad, ControlFocus.TUNE));
            addView(tuneValue, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 28)));

            LinearLayout tuneButtons = compactButtons(context,
                    "−", "＋",
                    () -> {
                        listener.onControlFocus(pad, ControlFocus.TUNE);
                        listener.onPadTuningDelta(pad, -1.0f);
                    },
                    () -> {
                        listener.onControlFocus(pad, ControlFocus.TUNE);
                        listener.onPadTuningDelta(pad, 1.0f);
                    });
            addView(tuneButtons, new LayoutParams(
                    LayoutParams.MATCH_PARENT, dp(context, 27)));

            setOnClickListener(v -> selectPad());
        }

        void refresh() {
            final boolean selected = listener.selectedPad() == pad;
            setBackground(frame(
                    selected ? RED : PANEL_2,
                    selected ? WHITE : BORDER,
                    0));
            title.setTextColor(selected ? WHITE : TEXT);
            sample.setText(sampleName());
            fader.setValue(clamp(listener.padLevel(pad), 0f, 1f), false);
            levelValue.setText(String.format(
                    Locale.ROOT, "LVL %3d%%",
                    Math.round(clamp(listener.padLevel(pad), 0f, 1f) * 100f)));
            panValue.setBackground(frame(
                    PANEL,
                    selected && controlFocus == ControlFocus.PAN ? WHITE : BORDER,
                    0));
            tuneValue.setBackground(frame(
                    PANEL,
                    selected && controlFocus == ControlFocus.TUNE ? WHITE : BORDER,
                    0));
            levelValue.setBackground(frame(
                    selected && controlFocus == ControlFocus.LEVEL ? RED : PANEL,
                    selected && controlFocus == ControlFocus.LEVEL ? WHITE : BORDER,
                    0));
            panValue.setText(String.format(
                    Locale.ROOT, "PAN %+.2f",
                    clamp(listener.padPan(pad), -1f, 1f)));
            tuneValue.setText(String.format(
                    Locale.ROOT, "TUNE %+.1f",
                    listener.padTuning(pad)));
        }

        private String sampleName() {
            final String name = listener.padSampleName(pad);
            return name == null || name.trim().isEmpty()
                    ? "NO SAMPLE"
                    : name.trim();
        }

        private void selectPad() {
            listener.onPadSelected(pad);
        }
    }

    private TextView controlField(
            Context context,
            String name,
            String value) {
        TextView field = text(context, name + " " + value, 8, TEXT);
        field.setGravity(Gravity.CENTER);
        field.setBackground(frame(PANEL, BORDER, 0));
        return field;
    }

    private LinearLayout compactButtons(
            Context context,
            String left,
            String right,
            Runnable leftAction,
            Runnable rightAction) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        Button l = button(context, left);
        l.setOnClickListener(v -> {
            leftAction.run();
            refreshAll();
        });
        row.addView(l, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));

        Button r = button(context, right);
        r.setOnClickListener(v -> {
            rightAction.run();
            refreshAll();
        });
        row.addView(r, new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f));
        return row;
    }

    private Button button(Context context, String value) {
        Button b = new Button(context);
        b.setText(value);
        b.setTextColor(TEXT);
        b.setTextSize(9);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(0, 0, 0, 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(frame(PANEL, BORDER, 0));
        return b;
    }

    private TextView text(Context context, String value, float size, int color) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.DEFAULT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private GradientDrawable frame(int fill, int stroke, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(getContext(), radiusDp));
        drawable.setStroke(dp(getContext(), 1), stroke);
        return drawable;
    }

    private void refreshAll() {
        for (Strip strip : strips) {
            strip.refresh();
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private final class Fader extends View {
        interface Callback {
            void onValueChanged(float value);
        }

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint handle = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value = 0.8f;
        private Callback callback;

        Fader(Context context) {
            super(context);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            setWillNotDraw(false);
        }

        void setOnValueChanged(Callback callback) {
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
}
