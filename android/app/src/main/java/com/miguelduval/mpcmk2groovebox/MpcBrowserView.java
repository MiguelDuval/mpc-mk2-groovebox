package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Product-level Browser presentation.
 *
 * Storage access is deliberately callback-based. Android Document Provider is
 * not allowed to become the Browser information architecture.
 */
final class MpcBrowserView extends LinearLayout {
    interface Listener {
        void onSectionSelected(String section);
        void onFilterSelected(String filter);
        void onOpenStorage();
        void onPlayCurrent();
        void onSearchChanged(String query);
    }

    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);
    private static final int SURFACE_2 = Color.rgb(32, 37, 42);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int MPC_BROWSER_SELECTED = Color.rgb(224, 30, 61);
    private static final int MPC_FLAT_RADIUS_DP = 0;

    private final LinearLayout places;
    private final LinearLayout filters;
    private final LinearLayout results;
    private TextView location;
    private final TextView destination;
    private final TextView currentSample;
    private final TextView providerState;
    private final EditText search;

    private Listener listener;

    MpcBrowserView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Browser");

        LinearLayout sections = row(context);
        for (String section : new String[]{
                "PLACES", "CONTENT", "EXPANSIONS", "SAMPLE ASSIGN"}) {
            Button button = button(context, section);
            button.setOnClickListener(v -> {
                if (listener != null) listener.onSectionSelected(section);
                setActiveButton(sections, section);
            });
            sections.addView(button, weight());
        }
        addView(sections, paramsMatch(dp(context, 42)));

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(HORIZONTAL);

        places = new LinearLayout(context);
        places.setOrientation(VERTICAL);
        places.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        for (String place : new String[]{
                "INTERNAL", "DOCUMENTS", "MUSIC", "SAMPLES", "FAVORITES"}) {
            Button button = button(context, place);
            button.setTextSize(9);
            button.setOnClickListener(v -> {
                if (listener != null) listener.onSectionSelected(place);
                location.setText("PLACE • " + place);
            });
            places.addView(button, paramsMatch(context, 42));
        }
        body.addView(places, paramsWidth(context, 116));

        LinearLayout center = new LinearLayout(context);
        center.setOrientation(VERTICAL);
        center.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        LinearLayout searchRow = row(context);
        search = new EditText(context);
        search.setSingleLine(true);
        search.setHint("Search files");
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setTextSize(11);
        search.setPadding(dp(context, 8), 0, dp(context, 8), 0);
        search.setBackground(stroke(SURFACE_2, LINE));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                if (listener != null) listener.onSearchChanged(s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        searchRow.addView(search, new LayoutParams(0, dp(context, 42), 1));
        Button clear = button(context, "CLEAR");
        clear.setOnClickListener(v -> search.setText(""));
        searchRow.addView(clear, paramsWidth(context, 62));
        center.addView(searchRow);

        filters = row(context);
        for (String filter : new String[]{
                "PROJECT", "PATTERN", "KIT", "PRESET", "SAMPLE", "ALL"}) {
            Button button = button(context, filter);
            button.setTextSize(8);
            button.setOnClickListener(v -> {
                if (listener != null) listener.onFilterSelected(filter);
                setActiveButton(filters, filter);
            });
            filters.addView(button, weight());
        }
        center.addView(filters, paramsMatch(context, 38));

        location = info(context, "PLACE • INTERNAL");
        center.addView(location, paramsMatch(context, 34));

        ScrollView scroll = new ScrollView(context);
        results = new LinearLayout(context);
        results.setOrientation(VERTICAL);
        results.setPadding(0, dp(context, 3), 0, dp(context, 3));
        addResult(context,
                "OPEN STORAGE…",
                "Android Document Provider • storage backend",
                true);
        addResult(context,
                "CURRENT SAMPLE",
                "Loaded sample for selected pad/layer",
                false);
        scroll.addView(results);
        center.addView(scroll, new LayoutParams(0, 0, 1));
        body.addView(center, new LayoutParams(0, -1, 1));

        LinearLayout targetPanel = new LinearLayout(context);
        targetPanel.setOrientation(VERTICAL);
        targetPanel.setPadding(dp(context, 6), dp(context, 4), dp(context, 4), dp(context, 4));
        targetPanel.setBackgroundColor(SURFACE);

        destination = info(context, "LOAD TO • PAD 01 / LAYER 01");
        targetPanel.addView(sectionText(context, "DESTINATION"));
        targetPanel.addView(destination, paramsMatch(context, 46));

        currentSample = info(context, "SAMPLE • NONE");
        targetPanel.addView(sectionText(context, "CURRENT"));
        targetPanel.addView(currentSample, paramsMatch(context, 64));

        providerState = info(context,
                "PROVIDER • ANDROID DOCUMENTS");
        providerState.setTextSize(9);
        targetPanel.addView(providerState, paramsMatch(context, 58));

        Button load = button(context, "LOAD");
        load.setOnClickListener(v -> {
            if (listener != null) listener.onOpenStorage();
        });
        targetPanel.addView(load, paramsMatch(context, 46));

        Button audition = button(context, "PLAY CURRENT");
        audition.setOnClickListener(v -> {
            if (listener != null) listener.onPlayCurrent();
        });
        targetPanel.addView(audition, paramsMatch(context, 46));

        body.addView(targetPanel, paramsWidth(context, 190));
        addView(body, new LayoutParams(-1, 0, 1));

        setActiveButton(sections, "PLACES");
        setActiveButton(filters, "ALL");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setTarget(int pad, int layer) {
        destination.setText(String.format(
                Locale.ROOT,
                "LOAD TO • PAD %02d / LAYER %02d",
                pad + 1,
                layer + 1));
        currentSample.setText("SAMPLE • " + currentSampleName());
    }

    void setCurrentSampleName(String name) {
        currentSample.setText(
                "SAMPLE • " + (name == null || name.isEmpty() ? "NONE" : name));
    }

    void setLocation(String value) {
        location.setText("PLACE • " + (
                value == null || value.isEmpty() ? "INTERNAL" : value));
    }

    void setFilter(String value) {
        setActiveButton(filters,
                value == null || value.isEmpty() ? "ALL" : value);
    }

    void setSearch(String value) {
        search.setText(value == null ? "" : value);
    }

    String searchQuery() {
        return search.getText() == null
                ? ""
                : search.getText().toString();
    }

    void focusSearch() {
        search.requestFocus();
        search.setSelection(search.length());
    }

    private String currentSampleName() {
        CharSequence value = currentSample.getText();
        return value == null ? "NONE"
                : value.toString().replace("SAMPLE • ", "");
    }

    private void addResult(
            Context context,
            String title,
            String subtitle,
            boolean action) {
        LinearLayout row = row(context);
        row.setPadding(dp(context, 6), dp(context, 2), dp(context, 6), dp(context, 2));
        TextView main = info(context, title);
        main.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(main, new LayoutParams(0, dp(context, 48), 1));
        TextView sub = info(context, subtitle);
        sub.setTextSize(8);
        row.addView(sub, new LayoutParams(0, dp(context, 48), 1.7f));
        if (action) {
            Button open = button(context, "OPEN");
            open.setOnClickListener(v -> {
                if (listener != null) listener.onOpenStorage();
            });
            row.addView(open, paramsWidth(context, 64));
        } else {
            Button audition = button(context, "PLAY");
            audition.setOnClickListener(v -> {
                if (listener != null) listener.onPlayCurrent();
            });
            row.addView(audition, paramsWidth(context, 64));
        }
        results.addView(row, paramsMatch(context, 54));
    }

    private void setActiveButton(LinearLayout container, String label) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof Button)) continue;
            Button button = (Button) child;
            final boolean selected =
                    label.equals(button.getText().toString());
            button.setTextColor(selected ? Color.rgb(14, 16, 18) : TEXT);
            button.setBackground(stroke(
                    selected ? MPC_BROWSER_SELECTED : SURFACE_2,
                    selected ? MPC_BROWSER_SELECTED : LINE));
        }
    }

    private TextView info(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(TEXT);
        view.setTextSize(10);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 6), 0, dp(context, 6), 0);
        view.setBackground(stroke(SURFACE_2, LINE));
        return view;
    }

    private TextView sectionText(Context context, String value) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextColor(MUTED);
        view.setTextSize(8);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(context, 4), 0, dp(context, 4), 0);
        return view;
    }

    private Button button(Context context, String text) {
        Button view = new Button(context);
        view.setText(text);
        view.setTextColor(TEXT);
        view.setTextSize(9);
        view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        view.setMinHeight(0);
        view.setMinimumHeight(0);
        view.setPadding(dp(context, 3), 0, dp(context, 3), 0);
        view.setGravity(Gravity.CENTER);
        view.setBackground(stroke(SURFACE_2, LINE));
        return view;
    }

    private LinearLayout row(Context context) {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(HORIZONTAL);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private LayoutParams weight() {
        return new LayoutParams(0, -1, 1);
    }

    private LayoutParams paramsMatch(Context context, int height) {
        return new LayoutParams(-1, dp(context, height));
    }

    private LayoutParams paramsWidth(Context context, int width) {
        return new LayoutParams(dp(context, width), -1);
    }

    private LayoutParams paramsMatch(int height) {
        return new LayoutParams(-1, height);
    }

    private android.graphics.drawable.GradientDrawable stroke(int fill, int line) {
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, MPC_FLAT_RADIUS_DP));
        drawable.setStroke(1, line);
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
