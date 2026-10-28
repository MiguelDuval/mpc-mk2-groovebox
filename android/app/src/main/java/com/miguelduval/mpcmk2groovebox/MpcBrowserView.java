package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        void onChooseFolder();
        void onLocalScanRequested();
        void onFileSelected(MpcBrowserFile file);
        void onAuditionSelectedFile();
        void onSearchChanged(String query);
        void onOptionsRequested();
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
    private String activeLocationItem = "INTERNAL";
    private List<MpcBrowserFile> localFiles = Collections.emptyList();
    private MpcBrowserFile selectedFile;
    private boolean localScanComplete;
    private String localScanMessage = "LOCAL AUDIO • NOT SCANNED";
    private List<MpcBrowserFile> userFolderFiles = Collections.emptyList();
    private String userFolderName = "";
    private String userFolderStatus = "USER FOLDER • NOT CONFIGURED";
    private boolean userFolderConfigured;
    private boolean userFolderAccessReady;
    private boolean userFolderScanComplete;
    private boolean userFolderScanFailed;

    // Browser Option: whether file size is shown in result metadata rows.
    // Toggled from the Browser Options dialog (BROW-012). Defaults to on
    // because size is already part of metadataLine(); the option makes it
    // user-controllable rather than always-on decoration.
    boolean showFileSize = true;

    MpcBrowserView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(BG);
        setContentDescription("MPC Browser");

        sections = row(context);
        for (String section : new String[]{
                "PLACES", "CONTENT", "EXPANSIONS"}) {
            Button button = button(context, section);
            button.setContentDescription("Browser " + section + " tab");
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
        search.setContentDescription("MPC Browser search files");
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
                renderResults();
            }
            @Override public void afterTextChanged(Editable e) {}
        });
        searchRow.addView(search, new LayoutParams(0, dp(context, 42), 1));
        Button clear = button(context, "CLEAR");
        clear.setContentDescription("MPC Browser clear search");
        clear.setOnClickListener(v -> search.setText(""));
        searchRow.addView(clear, paramsWidth(context, 62));
        center.addView(searchRow);

        // Keep the fixed-width Options target outside the weighted filter group.
        // This prevents the six filters from squeezing Options to zero hit bounds.
        LinearLayout filterBar = row(context);
        filters = row(context);
        filters.setContentDescription("MPC Browser FILTER Buttons");
        for (String filter : new String[]{
                "PROJECTS", "PATTERNS", "KITS", "PLUGIN PRESETS", "SAMPLES", "ALL"}) {
            Button button = button(context, "");
            button.setTextSize(8);
            button.setGravity(Gravity.CENTER);
            button.setContentDescription("MPC Browser filter " + filter);
            final MpcBrowserFilterIconDrawable icon =
                    new MpcBrowserFilterIconDrawable(
                            MpcBrowserFilterIconDrawable.Filter.fromLabel(filter),
                            dp(context, 20));
            icon.setBounds(0, 0, dp(context, 20), dp(context, 20));
            button.setTag(icon);
            button.setCompoundDrawables(icon, null, null, null);
            button.setCompoundDrawablePadding(0);
            button.setOnClickListener(v -> {
                if (listener != null) listener.onFilterSelected(filter);
                setActiveButton(filters, filter);
                renderResults();
            });
            filters.addView(button, weight());
        }
        Button options = button(context, "");
        options.setGravity(Gravity.CENTER);
        options.setMinWidth(0);
        options.setMinimumWidth(0);
        options.setContentDescription("MPC Browser Options");
        final MpcBrowserChromeIconDrawable optionsIcon =
                new MpcBrowserChromeIconDrawable(
                        MpcBrowserChromeIconDrawable.Mode.OPTIONS,
                        dp(context, 20));
        optionsIcon.setBounds(0, 0, dp(context, 20), dp(context, 20));
        options.setCompoundDrawables(optionsIcon, null, null, null);
        options.setCompoundDrawablePadding(0);
        options.setOnClickListener(v -> {
            if (listener != null) listener.onOptionsRequested();
        });
        filterBar.addView(filters, new LayoutParams(0, -1, 1));
        filterBar.addView(options, paramsWidth(context, 38));
        center.addView(filterBar, paramsMatch(context, 38));

        location = info(context, "PLACE • INTERNAL");
        location.setContentDescription(
                "MPC Browser current location: PLACE • INTERNAL");
        center.addView(location, paramsMatch(context, 34));

        LinearLayout targetContext = row(context);
        targetContext.setContentDescription(
                "Browser target context • state only");
        destination = info(context, "TARGET • PAD 01 / LAYER 01");
        destination.setTextSize(9);
        destination.setSingleLine(true);
        destination.setEllipsize(TextUtils.TruncateAt.END);
        destination.setContentDescription("MPC Browser target Pad and Layer");
        currentSample = info(context, "SAMPLE • NONE");
        currentSample.setTextSize(9);
        currentSample.setSingleLine(true);
        currentSample.setEllipsize(TextUtils.TruncateAt.END);
        currentSample.setContentDescription("MPC Browser current sample");
        targetContext.addView(destination,
                new LayoutParams(0, dp(context, 34), 1.0f));
        targetContext.addView(currentSample,
                new LayoutParams(0, dp(context, 34), 1.25f));
        center.addView(targetContext,
                paramsMatch(context, 34));

        providerState = info(context,
                "LOCAL AUDIO • NOT SCANNED");
        providerState.setContentDescription("MPC Browser provider status");
        providerState.setTextSize(8);
        center.addView(providerState,
                paramsMatch(context, 28));

        ScrollView scroll = new ScrollView(context);
        scroll.setContentDescription("MPC Browser results list");
        results = new LinearLayout(context);
        results.setOrientation(VERTICAL);
        results.setPadding(0, dp(context, 3), 0, dp(context, 3));
        scroll.addView(results);
        center.addView(scroll, new LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        body.addView(center, new LayoutParams(0, -1, 1));
        addView(body, new LayoutParams(-1, 0, 1));

        setSection("PLACES");
        setActiveButton(filters, "ALL");
        renderResults();
    }

    void setListener(Listener listener) {
        this.listener = listener;
    }

    void auditionSelectedFile() {
        if (listener != null) listener.onAuditionSelectedFile();
    }

    MpcBrowserFile selectedFile() {
        return selectedFile;
    }

    void clearSelectedFile() {
        selectedFile = null;
        renderResults();
        if (listener != null) listener.onFileSelected(null);
    }

    void setLocalFiles(List<MpcBrowserFile> files, String status) {
        localFiles = files == null
                ? Collections.emptyList()
                : new ArrayList<>(files);
        localScanComplete = true;
        setProviderStatus(status == null || status.trim().isEmpty()
                ? "LOCAL AUDIO • " + localFiles.size() + " WAV FILES INDEXED"
                : status);
        renderResults();
    }

    void setScanState(String status) {
        setProviderStatus(status);
        renderResults();
    }

    void setProviderStatus(String status) {
        final String value = status == null || status.trim().isEmpty()
                ? "LOCAL AUDIO • NOT SCANNED" : status;
        if (isDocumentTreeLocation()) userFolderStatus = value;
        else localScanMessage = value;
        providerState.setText(value);
    }

    void setUserFolderState(String name, boolean accessReady, String status) {
        final String nextName = name == null ? "" : name.trim();
        userFolderName = nextName;
        userFolderConfigured = !nextName.isEmpty();
        userFolderAccessReady = userFolderConfigured && accessReady;
        userFolderScanComplete = false;
        userFolderScanFailed = false;
        // Favorites can point to different URIs with the same display name.
        // Never show a previous folder's cached WAVs while a new location scans.
        userFolderFiles = Collections.emptyList();
        userFolderStatus = status == null || status.trim().isEmpty()
                ? (userFolderConfigured ? "USER FOLDER • NOT SCANNED" : "USER FOLDER • NOT CONFIGURED")
                : status;
        rebuildSideNavigation();
        setActiveNavigationItem(activeLocationItem);
        renderResults();
    }

    void setUserFolderFiles(String name, List<MpcBrowserFile> files, String status) {
        final String nextName = name == null ? "" : name.trim();
        if (!nextName.isEmpty()) userFolderName = nextName;
        userFolderConfigured = !userFolderName.isEmpty();
        userFolderAccessReady = userFolderConfigured;
        userFolderFiles = files == null ? Collections.emptyList() : new ArrayList<>(files);
        userFolderScanComplete = true;
        userFolderScanFailed = false;
        userFolderStatus = status == null || status.trim().isEmpty()
                ? "USER FOLDER • " + userFolderFiles.size() + " WAV FILES INDEXED" : status;
        rebuildSideNavigation();
        setActiveNavigationItem(activeLocationItem);
        renderResults();
    }

    void setUserFolderFailure(String name, String status) {
        final String nextName = name == null ? "" : name.trim();
        if (!nextName.isEmpty()) userFolderName = nextName;
        userFolderConfigured = !userFolderName.isEmpty();
        userFolderAccessReady = userFolderConfigured;
        userFolderFiles = Collections.emptyList();
        userFolderScanComplete = true;
        userFolderScanFailed = true;
        userFolderStatus = status == null || status.trim().isEmpty()
                ? "USER FOLDER • READ FAILED • CHOOSE FOLDER AGAIN" : status;
        rebuildSideNavigation();
        setActiveNavigationItem(activeLocationItem);
        renderResults();
    }

    void setTarget(int pad, int layer) {
        destination.setText(String.format(
                Locale.ROOT,
                "TARGET • PAD %02d / LAYER %02d",
                pad + 1,
                layer + 1));
        currentSample.setText(
                "CURRENT SAMPLE • " + currentSampleName());
    }

    void setCurrentSampleName(String name) {
        currentSample.setText(
                "CURRENT SAMPLE • " + (name == null || name.isEmpty() ? "NONE" : name));
    }

    void setSection(String section) {
        activeSection = isKnownSection(section) ? section : "PLACES";
        activeLocationItem = defaultItemForSection(activeSection);
        setActiveButton(sections, activeSection);
        rebuildSideNavigation();
        setActiveNavigationItem(activeLocationItem);
        updateLocationLabel();
        renderResults();
    }

    void setLocation(String value) {
        final String raw = value == null ? "" : value;
        final int separator = raw.indexOf('/');
        if (separator > 0) {
            final String section = raw.substring(0, separator);
            final String item = raw.substring(separator + 1);
            if (isKnownSection(section)) {
                activeSection = section;
                activeLocationItem = item;
                setActiveButton(sections, activeSection);
                rebuildSideNavigation();
                updateLocationLabel();
                setActiveNavigationItem(item);
                renderResults();
                return;
            }
        }
        if (isKnownSection(raw)) {
            setSection(raw);
            return;
        }
        activeSection = "PLACES";
        activeLocationItem = raw.isEmpty() ? "INTERNAL" : raw;
        setActiveButton(sections, activeSection);
        rebuildSideNavigation();
        updateLocationLabel();
        setActiveNavigationItem(activeLocationItem);
        renderResults();
    }

    private String defaultItemForSection(String section) {
        if ("CONTENT".equals(section)) return "SAMPLES";
        if ("EXPANSIONS".equals(section)) return "EXPANSIONS";
        return "INTERNAL";
    }

    private void updateLocationLabel() {
        final String label = locationLabel(activeSection, activeLocationItem);
        location.setText(label);
        location.setContentDescription("MPC Browser current location: " + label);
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
                    "DEMOS", "MY FILES"};
        } else if ("EXPANSIONS".equals(activeSection)) {
            items = new String[]{
                    "EXPANSIONS", "USER CONTENT"};
        } else {
            final List<String> placesItems = new ArrayList<>();
            Collections.addAll(placesItems, "INTERNAL", "MPC DOCUMENTS", "CONNECTED STORAGE");
            placesItems.add("USER FOLDER");
            Collections.addAll(placesItems,
                    "FAVORITE 1", "FAVORITE 2", "FAVORITE 3", "FAVORITE 4", "FAVORITE 5");
            items = placesItems.toArray(new String[0]);
        }

        for (String item : items) {
            final String section = activeSection;
            Button button = button(getContext(), item);
            button.setContentDescription(
                    "MPC Browser navigation " + section + " " + item);
            button.setTextSize(9);
            button.setOnClickListener(v -> {
                activeLocationItem = item;
                if (listener != null) {
                    listener.onNavigationItemSelected(section, item);
                }
                updateLocationLabel();
                setActiveNavigationItem(item);
                renderResults();
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
            button.setSelected(selected);
            button.setTextColor(selected ? Color.rgb(14, 16, 18) : TEXT);
            button.setBackground(stroke(
                    selected ? MPC_BROWSER_SELECTED : SURFACE_2,
                    selected ? MPC_BROWSER_SELECTED : LINE));
            final Object tag = button.getTag();
            if (tag instanceof MpcBrowserFilterIconDrawable) {
                ((MpcBrowserFilterIconDrawable) tag).setSelected(selected);
            }
        }
    }

    void setFilter(String value) {
        setActiveButton(filters,
                value == null || value.isEmpty() ? "ALL" : value);
        renderResults();
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
                : value.toString().replace("CURRENT SAMPLE • ", "");
    }

    private boolean isUserFolderLocation() {
        return "PLACES".equals(activeSection)
                && "USER FOLDER".equalsIgnoreCase(activeLocationItem);
    }

    private boolean isFavoriteLocation() {
        if (!"PLACES".equals(activeSection)) return false;
        final String item = activeLocationItem.toUpperCase(Locale.ROOT);
        return "FAVORITE 1".equals(item)
                || "FAVORITE 2".equals(item)
                || "FAVORITE 3".equals(item)
                || "FAVORITE 4".equals(item)
                || "FAVORITE 5".equals(item);
    }

    private boolean isDocumentTreeLocation() {
        return isUserFolderLocation() || isFavoriteLocation();
    }

    private boolean isCatalogLocationSupported() {
        if ("PLACES".equals(activeSection)) {
            if ("INTERNAL".equalsIgnoreCase(activeLocationItem)) return true;
            if (isFavoriteLocation()) return isDocumentTreeLocation();
            return isDocumentTreeLocation() && userFolderConfigured && userFolderAccessReady;
        }
        if ("CONTENT".equals(activeSection)) {
            return "SAMPLES".equalsIgnoreCase(activeLocationItem)
                    || "MY FILES".equalsIgnoreCase(activeLocationItem);
        }
        return false;
    }

    private String unavailableLocationTitle() {
        if ("EXPANSIONS".equals(activeSection)) return "LOCAL EXPANSIONS NOT INDEXED";
        if ("PLACES".equals(activeSection)) {
            if (isUserFolderLocation()) return userFolderConfigured
                    ? "USER FOLDER ACCESS NEEDS RENEWAL" : "USER FOLDER NOT CONFIGURED";
            if (isFavoriteLocation()) {
                return activeLocationItem + " NOT CONFIGURED";
            }
            return "PLACE PROVIDER UNAVAILABLE";
        }
        return "CATEGORY PROVIDER NOT IMPLEMENTED";
    }

    private String unavailableLocationDetail() {
        if ("EXPANSIONS".equals(activeSection)) return "No local expansion catalog is connected yet.";
        if ("PLACES".equals(activeSection)) {
            if (isUserFolderLocation()) return "Choose CHOOSE FOLDER to grant this app read access to a local folder.";
            if ("MPC DOCUMENTS".equalsIgnoreCase(activeLocationItem)
                    || "CONNECTED STORAGE".equalsIgnoreCase(activeLocationItem)) {
                return "Choose a local folder to browse real documents. Android file import remains available from LOAD.";
            }
            if (isFavoriteLocation()) {
                return "Use CHOOSE FOLDER to assign a persistent local folder to "
                        + activeLocationItem + ".";
            }
            return "This place is not backed by the local WAV catalog.";
        }
        return "No real local provider is available for " + activeLocationItem + " yet.";
    }

    void renderResults() {
        results.removeAllViews();
        providerState.setText(isDocumentTreeLocation() ? userFolderStatus : localScanMessage);
        final List<MpcBrowserFile> sourceFiles =
                isDocumentTreeLocation() ? userFolderFiles : localFiles;
        final List<MpcBrowserFile> matchingFiles = new ArrayList<>();
        for (MpcBrowserFile file : sourceFiles) {
            if (file.matchesBrowserQuery(
                    activeSection, activeLocationItem, activeFilter(), searchQuery())) {
                matchingFiles.add(file);
            }
        }
        final List<MpcBrowserFile> visibleFiles =
                MpcBrowserFile.sortedByDisplayName(matchingFiles);

        if (selectedFile != null) {
            MpcBrowserFile stillVisible = null;
            for (MpcBrowserFile file : visibleFiles) {
                if (selectedFile.uri().equals(file.uri())) {
                    stillVisible = file;
                    break;
                }
            }
            if (stillVisible == null) {
                selectedFile = null;
                if (listener != null) listener.onFileSelected(null);
            } else {
                selectedFile = stillVisible;
            }
        }

        addActionRow(
                "SCAN LOCAL AUDIO…",
                "INDEX ACCESSIBLE DEVICE WAV FILES",
                "MPC Browser scan local audio files",
                true);
        if (isDocumentTreeLocation()) {
            addActionRow(
                    isUserFolderLocation() || !userFolderConfigured
                            ? "CHOOSE FOLDER…" : "CHANGE FOLDER…",
                    isUserFolderLocation()
                            ? "PERSIST A LOCAL FOLDER FOR BROWSING"
                            : "PERSIST A LOCAL FOLDER FOR THIS FAVORITE",
                    "MPC Browser choose a local folder for " + activeLocationItem,
                    false);
        } else {
            addActionRow(
                    "OPEN STORAGE…",
                    "ANDROID DOCUMENT PICKER • DIRECT WAV LOAD",
                    "MPC Browser open Android storage picker",
                    false);
        }
        if (!isCatalogLocationSupported()) {
            addInfoRow(unavailableLocationTitle(), unavailableLocationDetail());
            return;
        }
        if (isDocumentTreeLocation()) {
            if (userFolderScanFailed) {
                addInfoRow(activeLocationItem + " READ FAILED",
                        "Read access failed. Choose another local folder to retry.");
                return;
            }
            if (!userFolderScanComplete) {
                addInfoRow(activeLocationItem + " SCANNING",
                        "Indexing WAV files directly inside " + userFolderName + ".");
                return;
            }
            if (visibleFiles.isEmpty()) {
                if (userFolderFiles.isEmpty()) {
                    addInfoRow("NO WAV FILES IN " + activeLocationItem,
                            "No WAV files were found directly inside " + userFolderName + ".");
                } else {
                    addInfoRow("NO MATCHING WAV FILES", "Change Search or select SAMPLES / ALL to show folder WAV items.");
                }
                return;
            }
        } else {
            if (!localScanComplete) {
                addInfoRow("LOCAL WAV CATALOG NOT SCANNED", "Select SCAN LOCAL AUDIO to list real device files.");
                return;
            }
            if (visibleFiles.isEmpty()) {
                if (localFiles.isEmpty()) {
                    addInfoRow("NO LOCAL WAV FILES", "No accessible WAV files were found in the device media catalog.");
                } else {
                    addInfoRow("NO MATCHING WAV FILES", "Change Search or select SAMPLES / ALL to show indexed WAV items.");
                }
                return;
            }
        }

        for (MpcBrowserFile file : visibleFiles) {
            addFileResult(file, selectedFile != null
                    && selectedFile.uri().equals(file.uri()));
        }
    }

    private String activeFilter() {
        for (int i = 0; i < filters.getChildCount(); i++) {
            final View child = filters.getChildAt(i);
            if (!(child instanceof Button)) continue;
            final Button button = (Button) child;
            if (!button.isSelected()) continue;
            final CharSequence description = button.getContentDescription();
            if (description != null) {
                final String value = description.toString();
                final String prefix = "MPC Browser filter ";
                if (value.startsWith(prefix)) return value.substring(prefix.length());
            }
            return button.getText().toString();
        }
        return "ALL";
    }

    private void addActionRow(
            String title,
            String subtitle,
            String contentDescription,
            boolean scanAction) {
        final LinearLayout row = row(getContext());
        row.setPadding(dp(getContext(), 6), dp(getContext(), 2),
                dp(getContext(), 6), dp(getContext(), 2));
        row.setContentDescription(contentDescription);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(stroke(SURFACE_2, LINE));
        row.setOnClickListener(v -> {
            if (listener == null) return;
            if (scanAction) listener.onLocalScanRequested();
            else if (isDocumentTreeLocation()) listener.onChooseFolder();
            else listener.onOpenStorage();
        });

        final TextView main = info(getContext(), title);
        main.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        main.setSingleLine(true);
        main.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(main, new LayoutParams(0, dp(getContext(), 34), 1));

        final TextView sub = info(getContext(), subtitle);
        sub.setTextSize(8);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(sub, new LayoutParams(0, dp(getContext(), 34), 1.7f));
        results.addView(row, paramsMatch(getContext(), 40));
    }

    private void addInfoRow(String title, String subtitle) {
        final LinearLayout row = row(getContext());
        row.setPadding(dp(getContext(), 6), dp(getContext(), 2),
                dp(getContext(), 6), dp(getContext(), 2));
        row.setContentDescription("MPC Browser status " + title);
        row.setClickable(false);
        row.setFocusable(false);
        row.setBackground(stroke(BG, LINE));

        final TextView main = info(getContext(), title);
        main.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        main.setSingleLine(true);
        main.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(main, new LayoutParams(0, dp(getContext(), 34), 1));

        final TextView sub = info(getContext(), subtitle);
        sub.setTextSize(8);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(sub, new LayoutParams(0, dp(getContext(), 34), 1.7f));
        results.addView(row, paramsMatch(getContext(), 40));
    }

    private void addFileResult(MpcBrowserFile file, boolean selected) {
        final LinearLayout row = row(getContext());
        row.setPadding(dp(getContext(), 6), dp(getContext(), 2),
                dp(getContext(), 6), dp(getContext(), 2));
        row.setContentDescription("MPC Browser file " + file.displayName()
                + (selected ? " selected" : ""));
        row.setClickable(true);
        row.setFocusable(true);
        row.setSelected(selected);
        row.setBackground(stroke(
                selected ? MPC_BROWSER_SELECTED : SURFACE_2,
                selected ? MPC_BROWSER_SELECTED : LINE));
        row.setOnClickListener(v -> {
            selectedFile = file;
            renderResults();
            if (listener != null) listener.onFileSelected(file);
        });

        final TextView main = info(getContext(), file.displayName());
        main.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        main.setSingleLine(true);
        main.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(main, new LayoutParams(0, dp(getContext(), 34), 1));

        final String subtitle = showFileSize
                ? file.metadataLine()
                : (file.durationMs() <= 0
                        ? "DURATION UNKNOWN • WAV"
                        : file.metadataLine().replaceFirst(
                                "^[^•]*•", "").trim());
        final TextView sub = info(getContext(), subtitle);
        sub.setTextSize(8);
        sub.setSingleLine(true);
        sub.setEllipsize(TextUtils.TruncateAt.END);
        row.addView(sub, new LayoutParams(0, dp(getContext(), 34), 1.7f));
        results.addView(row, paramsMatch(getContext(), 40));
    }

    private void setActiveButton(LinearLayout container, String label) {
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (!(child instanceof Button)) continue;
            Button button = (Button) child;
            final CharSequence description = button.getContentDescription();
            final String accessibleLabel = description == null
                    ? "" : description.toString();
            final boolean selected = label.equals(button.getText().toString())
                    || accessibleLabel.equals("MPC Browser filter " + label)
                    || accessibleLabel.equals("Browser " + label + " tab");
            button.setSelected(selected);
            button.setTextColor(selected ? Color.rgb(14, 16, 18) : TEXT);
            button.setBackground(stroke(
                    selected ? MPC_BROWSER_SELECTED : SURFACE_2,
                    selected ? MPC_BROWSER_SELECTED : LINE));
            final Object tag = button.getTag();
            if (tag instanceof MpcBrowserFilterIconDrawable) {
                ((MpcBrowserFilterIconDrawable) tag).setSelected(selected);
            }
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
