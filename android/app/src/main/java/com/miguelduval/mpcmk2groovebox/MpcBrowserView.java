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
        void onNavigationItemSelected(String section, String item);
        void onFilterSelected(String filter);
        void onNavigateUp();
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

    private final LinearLayout sections;
    private final LinearLayout places;
    private final LinearLayout filters;
    private final LinearLayout results;
    private TextView location;
    private final TextView destination;
    private final TextView currentSample;
    private final TextView providerState;
    private final EditText search;

    private Listener listener;
    private String activeSection = "PLACES";

    MpcBrowserView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Browser");

        sections = row(context);
        for (String section : new String[]{
                "PLACES", "CONTENT", "EXPANSIONS"}) {
            Button button = button(context, section);
            button.setOnClickListener(v -> {
                setSection(section);
                if (listener != null) listener.onSectionSelected(section);
            });
            sections.addView(button, weight());
        }
        addView(sections, paramsMatch(dp(context, 42)));

        LinearLayout body = new LinearLayout(context);
        body.setOrientation(HORIZONTAL);

        places = new LinearLayout(context);
        places.setOrientation(VERTICAL);
        places.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));

        ScrollView sideScroll = new ScrollView(context);
        sideScroll.addView(places);
        body.addView(sideScroll, paramsWidth(context, 116));

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
        filters.setContentDescription("MPC Browser FILTER Buttons");
        for (String filter : new String[]{
                "PROJECTS", "PATTERNS", "KITS", "PLUGIN PRESETS", "SAMPLES", "ALL"}) {
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

        LinearLayout targetContext = row(context);
        targetContext.setContentDescription(
                "Browser target context • state only");
        destination = info(context, "TARGET • PAD 01 / LAYER 01");
        currentSample = info(context, "SAMPLE • NONE");
        targetContext.addView(destination,
                new LayoutParams(0, dp(context, 34), 1.0f));
        targetContext.addView(currentSample,
                new LayoutParams(0, dp(context, 34), 1.25f));
        center.addView(targetContext,
                paramsMatch(context, 34));

        providerState = info(context,
                "PROVIDER • ANDROID DOCUMENTS • LOAD BELOW");
        providerState.setTextSize(8);
        center.addView(providerState,
                paramsMatch(context, 28));

        ScrollView scroll = new ScrollView(context);
        results = new LinearLayout(context);
        results.setOrientation(VERTICAL);
        results.setPadding(0, dp(context, 3), 0, dp(context, 3));
        addResult(context,
                "OPEN STORAGE…",
                "ANDROID DOCUMENTS • USE LOAD BELOW",
                true);
        addResult(context,
                "CURRENT SAMPLE",
                "SELECTED PAD/LAYER • USE AUDITION BELOW",
                false);
        scroll.addView(results);
        center.addView(scroll, new LayoutParams(0, 0, 1));
        body.addView(center, new LayoutParams(0, -1, 1));
        addView(body, new LayoutParams(-1, 0, 1));

        setSection("PLACES");
        setActiveButton(filters, "ALL");
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void setTarget(int pad, int layer) {
        destination.setText(String.format(
                Locale.ROOT,
                "TARGET • PAD %02d / LAYER %02d",
                pad + 1,
                layer + 1));
        currentSample.setText(
                "SAMPLE • " + currentSampleName());
    }

    void setCurrentSampleName(String name) {
        currentSample.setText(
                "SAMPLE • " + (name == null || name.isEmpty() ? "NONE" : name));
    }

    void setSection(String section) {
        activeSection = isKnownSection(section) ? section : "PLACES";
        setActiveButton(sections, activeSection);
        rebuildSideNavigation();
    }

    void setLocation(String value) {
        final String raw = value == null ? "" : value;
        final int separator = raw.indexOf('/');
        if (separator > 0) {
            final String section = raw.substring(0, separator);
            final String item = raw.substring(separator + 1);
            if (isKnownSection(section)) {
                activeSection = section;
                setActiveButton(sections, activeSection);
                rebuildSideNavigation();
                location.setText(locationLabel(section, item));
                setActiveNavigationItem(item);
                return;
            }
        }
        if (isKnownSection(raw)) {
            setSection(raw);
            location.setText(
                    "PLACES".equals(raw)
                            ? "PLACE • INTERNAL"
                            : raw);
            return;
        }
        setSection("PLACES");
        location.setText("PLACE • " + (raw.isEmpty() ? "INTERNAL" : raw));
        setActiveNavigationItem(raw.isEmpty() ? "INTERNAL" : raw);
    }

    private boolean isKnownSection(String section) {
        return "PLACES".equals(section)
                || "CONTENT".equals(section)
                || "EXPANSIONS".equals(section);
    }

    private String locationLabel(String section, String item) {
        if ("CONTENT".equals(section)) {
            return "CONTENT • " + item;
        }
        if ("EXPANSIONS".equals(section)) {
            return "EXPANSIONS • " + item;
        }
        return "PLACE • " + item;
    }

    private void rebuildSideNavigation() {
        places.removeAllViews();
        final String[] items;
        if ("CONTENT".equals(activeSection)) {
            items = new String[]{
                    "DRUMS", "INSTRUMENTS", "SAMPLES",
                    "DEMOS", "MY FILES", "SPLICE"};
        } else if ("EXPANSIONS".equals(activeSection)) {
            items = new String[]{
                    "EXPANSIONS", "USER CONTENT"};
        } else {
            items = new String[]{
                    "INTERNAL", "MPC DOCUMENTS", "CONNECTED STORAGE",
                    "FAVORITE 1", "FAVORITE 2", "FAVORITE 3",
                    "FAVORITE 4", "FAVORITE 5"};
        }

        for (String item : items) {
            final String section = activeSection;
            Button button = button(getContext(), item);
            button.setTextSize(9);
            button.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNavigationItemSelected(section, item);
                }
                location.setText(locationLabel(section, item));
                setActiveNavigationItem(item);
            });
            places.addView(button, paramsMatch(getContext(), 42));
        }
    }

    private void setActiveNavigationItem(String item) {
        for (int i = 0; i < places.getChildCount(); i++) {
            final View child = places.getChildAt(i);
            if (!(child instanceof Button)) continue;
            final Button button = (Button) child;
            final boolean selected = item.equals(button.getText().toString());
            button.setTextColor(selected ? Color.rgb(14, 16, 18) : TEXT);
            button.setBackground(stroke(
                    selected ? MPC_BROWSER_SELECTED : SURFACE_2,
                    selected ? MPC_BROWSER_SELECTED : LINE));
        }
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
        row.addView(main, new LayoutParams(0, dp(context, 44), 1));
        TextView sub = info(context, subtitle);
        sub.setTextSize(8);
        row.addView(sub, new LayoutParams(0, dp(context, 44), 1.7f));
        results.addView(row, paramsMatch(context, 50));
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
        drawable.setCornerRadius(dp(getContext(), MPC_FLAT_RADIUS_DP));
        drawable.setStroke(1, line);
        return drawable;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
