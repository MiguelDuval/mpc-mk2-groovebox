package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * MPC 3.9-style Pull-Down Menu shell.
 *
 * This view owns presentation and panel navigation only. Q-Link learning and
 * other unavailable controls are deliberately surfaced as RESERVED rather
 * than simulated until their semantic backends exist.
 */
final class MpcPullDownPanelView extends FrameLayout {
    interface Listener {
        void onClose();
        void onReservedAction(String label);
    }

    private static final int BG = Color.rgb(17, 19, 22);
    private static final int PANEL = Color.rgb(39, 43, 47);
    private static final int FIELD = Color.rgb(28, 31, 34);
    private static final int LINE = Color.rgb(75, 82, 88);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int GREEN = Color.rgb(63, 207, 117);

    private Listener listener;
    private int pageIndex;
    private float downY;
    private String projectName = "UNTITLED";
    private int sequenceNumber = 1;
    private double tempo = 120.0;
    private boolean midiReady;
    private boolean audioReady;

    MpcPullDownPanelView(Context context) {
        super(context);
        setBackgroundColor(BG);
        setElevation(dp(8));
        setContentDescription("MPC Pull-Down Menu");
        setClickable(true);
        setFocusable(true);
        renderPage();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setPage(int pageIndex) {
        this.pageIndex = Math.max(0, Math.min(1, pageIndex));
        renderPage();
    }

    int page() {
        return pageIndex;
    }

    void setContext(
            String projectName,
            int sequenceNumber,
            double tempo,
            boolean midiReady,
            boolean audioReady) {
        this.projectName = projectName == null || projectName.trim().isEmpty()
                ? "UNTITLED" : projectName.trim();
        this.sequenceNumber = Math.max(1, sequenceNumber);
        this.tempo = Double.isFinite(tempo) ? tempo : 120.0;
        this.midiReady = midiReady;
        this.audioReady = audioReady;
        renderPage();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                final float delta = event.getY() - downY;
                if (delta < -48.0f) {
                    if (listener != null) {
                        listener.onClose();
                    }
                }
                return true;
            default:
                return true;
        }
    }

    private void renderPage() {
        removeAllViews();

        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(6), dp(8), dp(6));
        panel.setBackground(background(PANEL, Color.TRANSPARENT, 0));

        LinearLayout header = row();
        TextView title = label(
                pageIndex == 0 ? "MPC CONTROL" : "Q-LINK",
                12,
                TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(
                0, dp(32), 1));

        TextView page = label(
                pageIndex == 0 ? "1 / 2" : "2 / 2",
                9,
                MUTED);
        page.setGravity(Gravity.CENTER);
        header.addView(page, new LinearLayout.LayoutParams(
                dp(44), dp(32)));

        Button close = button("×");
        close.setContentDescription("Close MPC Pull-Down Menu");
        close.setTextSize(18);
        close.setOnClickListener(v -> {
            if (listener != null) {
                listener.onClose();
            }
        });
        header.addView(close, new LinearLayout.LayoutParams(
                dp(38), dp(32)));

        panel.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        if (pageIndex == 0) {
            renderControlPage(panel);
        } else {
            renderQLinkPage(panel);
        }

        LinearLayout footer = row();
        Button previous = button("‹");
        previous.setEnabled(pageIndex > 0);
        previous.setAlpha(pageIndex > 0 ? 1.0f : 0.35f);
        previous.setContentDescription("MPC Pull-Down previous page");
        previous.setOnClickListener(v -> {
            if (pageIndex > 0) {
                setPage(pageIndex - 1);
            }
        });
        footer.addView(previous, new LinearLayout.LayoutParams(
                dp(42), dp(32)));

        TextView hint = label(
                pageIndex == 0
                        ? "Swipe up to close • use › for Q-LINK"
                        : "Swipe up to close • Q-Link controls are backend-reserved",
                8,
                MUTED);
        hint.setGravity(Gravity.CENTER);
        footer.addView(hint, new LinearLayout.LayoutParams(
                0, dp(32), 1));

        Button next = button("›");
        next.setEnabled(pageIndex < 1);
        next.setAlpha(pageIndex < 1 ? 1.0f : 0.35f);
        next.setContentDescription("MPC Pull-Down next page");
        next.setOnClickListener(v -> {
            if (pageIndex < 1) {
                setPage(pageIndex + 1);
            }
        });
        footer.addView(next, new LinearLayout.LayoutParams(
                dp(42), dp(32)));

        panel.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void renderControlPage(LinearLayout panel) {
        LinearLayout rowOne = row();
        rowOne.addView(field(
                "PROJECT",
                projectName,
                false,
                "MPC Pull-Down Project status"), weight());
        rowOne.addView(field(
                "SEQUENCE",
                String.format(java.util.Locale.ROOT, "%02d", sequenceNumber),
                false,
                "MPC Pull-Down Sequence status"), weight());
        rowOne.addView(field(
                "TEMPO",
                String.format(java.util.Locale.ROOT, "%.1f BPM", tempo),
                false,
                "MPC Pull-Down Tempo status"), weight());
        panel.addView(rowOne, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        LinearLayout rowTwo = row();
        rowTwo.addView(field(
                "MIDI",
                midiReady ? "IN / OUT" : "NO DEVICE",
                midiReady,
                "MPC Pull-Down MIDI status"), weight());
        rowTwo.addView(field(
                "AUDIO",
                audioReady ? "READY" : "NOT READY",
                audioReady,
                "MPC Pull-Down Audio status"), weight());

        rowTwo.addView(action(
                "METRO",
                false,
                "Metronome settings • RESERVED"), weight());
        panel.addView(rowTwo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
    }

    private void renderQLinkPage(LinearLayout panel) {
        LinearLayout rowOne = row();
        rowOne.addView(field(
                "CURRENT CONTROL",
                "Q-LINK 1",
                true,
                "MPC Pull-Down Q-Link current control"), weight());

        rowOne.addView(action(
                "LEARN",
                false,
                "Q-Link MIDI Learn • RESERVED"), weight());
        panel.addView(rowOne, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        LinearLayout rowTwo = row();
        rowTwo.addView(action(
                "MOMENTARY",
                false,
                "Q-Link Momentary • RESERVED"), weight());
        rowTwo.addView(action(
                "GO TO MIN",
                false,
                "Q-Link Go to Minimum • RESERVED"), weight());
        rowTwo.addView(action(
                "GO TO PREVIOUS",
                false,
                "Q-Link Go to Previous • RESERVED"), weight());
        panel.addView(rowTwo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));
    }

    private Button action(String text, boolean enabled, String description) {
        Button button = button(text);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1.0f : 0.48f);
        button.setTextSize(9);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setContentDescription(description);
        button.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReservedAction(description);
            }
        });
        return button;
    }

    private View field(
            String title,
            String value,
            boolean active,
            String description) {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(6), dp(3), dp(6), dp(3));
        box.setBackground(background(
                FIELD,
                active ? GREEN : LINE,
                0));
        box.setContentDescription(description);

        TextView caption = label(title, 7, MUTED);
        caption.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(caption, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));

        TextView state = label(value, 10, active ? TEXT : MUTED);
        state.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(state, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        return box;
    }

    private Button button(String text) {
        Button b = new Button(getContext());
        b.setText(text);
        b.setTextColor(TEXT);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(3), 0, dp(3), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(background(FIELD, LINE, 0));
        return b;
    }

    private TextView label(String text, int size, int color) {
        TextView v = new TextView(getContext());
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        return v;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(2), dp(2), dp(2), dp(2));
        return row;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, dp(54), 1);
    }

    private GradientDrawable background(int fill, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), stroke);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
