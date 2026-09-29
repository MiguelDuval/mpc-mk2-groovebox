package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements AndroidMidiBridge.Listener {
    private static final String TAG = "MpcGroovebox";
    private static final int REQUEST_OPEN_WAV = 1001;
    private static final int REQUEST_RECORD_AUDIO = 1002;
    private static final int REQUEST_MONITOR_AUDIO = 1003;
    private static final int MAX_SAMPLE_BYTES = 32 * 1024 * 1024;
    private static final String SMOKE_MODE_EXTRA = "mpc.groovebox.smoke.mode";

    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);
    private static final int SURFACE_2 = Color.rgb(32, 37, 42);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int ACCENT = Color.rgb(69, 211, 255);
    private static final int ACCENT_2 = Color.rgb(255, 180, 72);
    private static final int DANGER = Color.rgb(236, 83, 83);
    private static final int ACTIVE = Color.rgb(63, 207, 117);

    static {
        System.loadLibrary("mpcgroovebox");
    }

    private AndroidMidiBridge midiBridge;
    private FrameLayout content;
    private TextView pageTitle;
    private TextView audioState;
    private TextView midiState;
    private TextView projectState;
    private TextView bottomStatus;
    private TextView selectedPadInfo;
    private TextView sampleInfo;
    private TextView regionInfo;
    private TextView envelopeInfo;
    private TextView filterInfo;
    private TextView recordingInfo;
    private final Button[] padButtons = new Button[16];
    private final Button[] modeButtons = new Button[7];
    private final Handler waveformUiHandler = new Handler(Looper.getMainLooper());
    private Runnable recordingWaveformUpdater;
    private WaveformView sampleWaveform;
    private WaveformView recordingWaveform;
    private TextView recordingTelemetry;
    private int selectedPad = 0;
    private int selectedLayer = 0;
    private String currentPage = "MAIN";
    private volatile boolean destroyed;
    private volatile boolean startupComplete;
    private boolean uiOnlySmokeMode;
    private boolean uiAuditSmokeMode;
    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor();

    private static native String nativeEngineInfo();
    private static native String nativeAudioLoadSample(byte[] data);
    private static native String nativeAudioLoadSampleForPadLayer(byte[] data, int pad, int layer);
    private static native void nativeAudioTriggerPad(int pad, int velocity);
    private static native String nativeAudioSetPadTuning(int pad, float semitones);
    private static native float nativeAudioGetPadTuning(int pad);
    private static native String nativeAudioSetPadLevel(int pad, float level);
    private static native float nativeAudioGetPadLevel(int pad);
    private static native String nativeAudioSetPadPan(int pad, float pan);
    private static native float nativeAudioGetPadPan(int pad);
    private static native String nativeAudioSetPadLayerGain(int pad, int layer, float gain);
    private static native float nativeAudioGetPadLayerGain(int pad, int layer);
    private static native String nativeAudioSetPadLayerTuning(int pad, int layer, float semitones);
    private static native float nativeAudioGetPadLayerTuning(int pad, int layer);
    private static native String nativeAudioSetPadLayerPan(int pad, int layer, float pan);
    private static native float nativeAudioGetPadLayerPan(int pad, int layer);
    private static native String nativeAudioSetPadLayerVelocityRange(
            int pad, int layer, int minimum, int maximum);
    private static native int nativeAudioGetPadLayerVelocityMin(int pad, int layer);
    private static native int nativeAudioGetPadLayerVelocityMax(int pad, int layer);
    private static native String nativeAudioSetPadSampleRegion(
            int pad, int layer, long startFrame, long endFrame);
    private static native long nativeAudioGetPadSampleRegionStart(int pad, int layer);
    private static native long nativeAudioGetPadSampleRegionEnd(int pad, int layer);
    private static native long nativeAudioGetPadSampleFrameCount(int pad, int layer);
    private static native int nativeAudioGetPadSampleRate(int pad, int layer);
    private static native float[] nativeAudioGetPadWaveformPeaks(
            int pad, int layer, int points);
    private static native String nativeAudioChopPadSampleToPads(
            int pad, int layer, int chopCount);
    private static native String nativeAudioCropPadSampleRegion(int pad, int layer);
    private static native String nativeAudioSetPadEnvelope(
            int pad, float attackMs, float decayMs, float sustain, float releaseMs);
    private static native float nativeAudioGetPadEnvelopeAttack(int pad);
    private static native float nativeAudioGetPadEnvelopeDecay(int pad);
    private static native float nativeAudioGetPadEnvelopeSustain(int pad);
    private static native float nativeAudioGetPadEnvelopeRelease(int pad);
    private static native String nativeAudioSetPadFilterCutoff(int pad, float cutoffHz);
    private static native float nativeAudioGetPadFilterCutoff(int pad);
    private static native String nativeAudioStart();
    private static native String nativeAudioStop();
    private static native String nativeAudioStatus();
    private static native String nativeAudioSetRecordingThreshold(float threshold);
    private static native float nativeAudioGetRecordingThreshold();
    private static native String nativeAudioStartRecording();
    private static native String nativeAudioStopRecording();
    private static native String nativeAudioStartMonitor();
    private static native String nativeAudioStopMonitor();
    private static native String nativeAudioAssignRecordingToPadLayer(int pad, int layer);
    private static native String nativeAudioRecordingStatus();
    private static native float[] nativeAudioGetRecordingWaveformPeaks(int points);
    private static native int nativeAudioGetRecordingFrameCount();
    private static native int nativeAudioGetRecordingSampleRate();
    private static native float nativeAudioGetRecordingPeak();
    private static native int nativeAudioGetRecordingFrameCapacity();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String smokeMode = getIntent().getStringExtra(SMOKE_MODE_EXTRA);
        uiOnlySmokeMode = "ui-only".equals(smokeMode);
        uiAuditSmokeMode = "ui-audit".equals(smokeMode);

        applyFullscreenWindowPolicy();

        setContentView(buildApplicationShell());
        applyFullscreenWindowPolicy();
        Log.i(TAG, "UI_READY");

        if (uiOnlySmokeMode) {
            bottomStatus.setText("UI-only startup diagnostic");
            Log.i(TAG, "UI_ONLY_COMPLETE");
            return;
        }

        content.postOnAnimation(() -> {
            Log.i(TAG, "STARTUP_BEGIN");
            startupExecutor.execute(() -> {
                Log.i(TAG, "NATIVE_INFO_BEGIN");
                final String engineInfo = nativeEngineInfo();
                Log.i(TAG, "NATIVE_INFO_END");

                Log.i(TAG, "BUNDLED_SAMPLE_BEGIN");
                final String sampleResult = loadBundledSample();
                Log.i(TAG, "BUNDLED_SAMPLE_END");

                final String audioResult = nativeAudioStart();
                Log.i(TAG, "AUDIO_START_RESULT=" + audioResult);

                runOnUiThread(() -> {
                    if (destroyed) return;

                    startupComplete = true;
                    bottomStatus.setText(
                            engineInfo + " | " + sampleResult + " | " + audioResult);
                    setAudioStateFromResult(audioResult);
                    refreshAllInspectorState();

                    Log.i(TAG, "MIDI_BRIDGE_BEGIN");
                    midiBridge = new AndroidMidiBridge(this, this);
                    Log.i(TAG, "MIDI_BRIDGE_END");
                    Log.i(TAG, "STARTUP_COMPLETE");

                    if (uiAuditSmokeMode) {
                        runUiAudit();
                    }
                });
            });
        });
    }

    private View buildApplicationShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        root.addView(buildTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.HORIZONTAL);

        body.addView(buildModeRail(), new LinearLayout.LayoutParams(dp(112),
                ViewGroup.LayoutParams.MATCH_PARENT));

        content = new FrameLayout(this);
        content.setBackgroundColor(BG);
        body.addView(content, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        root.addView(body, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        bottomStatus = label("Initializing…", 11, MUTED);
        bottomStatus.setPadding(dp(12), 0, dp(12), 0);
        bottomStatus.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(bottomStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        showMainPage();
        updateModeRailSelection();
        return root;
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(6), dp(10), dp(6));
        bar.setBackgroundColor(SURFACE);

        projectState = label("UNTITLED", 12, TEXT);
        projectState.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(projectState, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1.2f));

        pageTitle = label("MAIN", 12, ACCENT);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(pageTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.55f));

        TextView tempo = label("120.0 BPM", 12, TEXT);
        tempo.setGravity(Gravity.CENTER);
        bar.addView(tempo, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f));

        audioState = statusChip("AUDIO OFF", MUTED);
        bar.addView(audioState, new LinearLayout.LayoutParams(dp(96), dp(38)));

        midiState = statusChip("MIDI —", MUTED);
        bar.addView(midiState, new LinearLayout.LayoutParams(dp(96), dp(38)));

        Button play = topButton("PLAY");
        play.setOnClickListener(v -> {
            final String result = nativeAudioStart();
            setAudioStateFromResult(result);
            setBottomStatus(result);
        });
        bar.addView(play, new LinearLayout.LayoutParams(dp(72), dp(38)));

        Button stop = topButton("STOP");
        stop.setOnClickListener(v -> {
            final String result = nativeAudioStop();
            setAudioStateFromResult(result);
            setBottomStatus(result);
        });
        bar.addView(stop, new LinearLayout.LayoutParams(dp(72), dp(38)));

        Button midi = topButton("MIDI");
        midi.setOnClickListener(v -> showMidiPage());
        bar.addView(midi, new LinearLayout.LayoutParams(dp(72), dp(38)));

        Button menu = topButton("MENU");
        menu.setOnClickListener(v -> showMenuPage());
        bar.addView(menu, new LinearLayout.LayoutParams(dp(72), dp(38)));

        return bar;
    }

    private View buildModeRail() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(dp(6), dp(6), dp(6), dp(6));
        rail.setBackgroundColor(Color.rgb(18, 21, 24));

        String[] pages = {"MAIN", "BROWSE", "SAMPLE", "SEQ", "MIX", "REC", "MENU"};
        for (int i = 0; i < pages.length; i++) {
            Button button = modeButton(pages[i], pages[i]);
            modeButtons[i] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            params.topMargin = i == 0 ? 0 : dp(4);
            params.bottomMargin = i == pages.length - 1 ? 0 : 0;
            rail.addView(button, params);
        }
        return rail;
    }

    private Button modeButton(String text, String page) {
        Button b = button(text);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinHeight(dp(56));
        b.setMinimumHeight(dp(56));
        b.setPadding(dp(3), 0, dp(3), 0);
        b.setTag(page);
        b.setContentDescription(page + " mode");
        b.setOnClickListener(v -> {
            switch (page) {
                case "MAIN": showMainPage(); break;
                case "BROWSE": showBrowserPage(); break;
                case "SAMPLE": showSamplePage(); break;
                case "SEQ": showSequencePage(); break;
                case "MIX": showMixPage(); break;
                case "REC": showRecordPage(); break;
                default: showMenuPage(); break;
            }
            updateModeRailSelection();
        });
        return b;
    }

    private void updateModeRailSelection() {
        for (Button button : modeButtons) {
            if (button == null) continue;
            boolean selected = String.valueOf(button.getTag()).equals(currentPage);
            button.setTextColor(selected ? BG : TEXT);
            button.setBackground(strokeBackground(
                    selected ? ACCENT : SURFACE_2,
                    selected ? ACCENT : LINE,
                    8));
        }
    }

    private void applyFullscreenWindowPolicy() {
        Window window = getWindow();
        window.setStatusBarColor(BG);
        window.setNavigationBarColor(BG);

        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
            return;
        }

        int flags = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decor.setSystemUiVisibility(flags);
    }

    private void showMainPage() {
        currentPage = "MAIN";
        pageTitle.setText("MAIN");
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout workspace = row();

        LinearLayout padSurface = column();
        padSurface.addView(sectionLabel("PERFORM / 16 PADS"));
        padSurface.addView(buildPadGrid(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout quick = row();
        quick.addView(actionButton("LOAD", v -> openWavPicker()), weight());
        quick.addView(actionButton("SAMPLE", v -> showSamplePage()), weight());
        quick.addView(actionButton("REC", v -> showRecordPage()), weight());
        quick.addView(actionButton("MIX", v -> showMixPage()), weight());
        padSurface.addView(quick, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        workspace.addView(padSurface, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.60f));
        workspace.addView(buildInspector(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.40f));

        page.addView(workspace, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(page);
        refreshPadSelectionVisuals();
        updateModeRailSelection();
    }

    private View buildPadGrid() {
        LinearLayout grid = column();
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            LinearLayout row = row();
            for (int col = 0; col < 4; col++) {
                final int pad = rowIndex * 4 + col;
                Button b = button(String.format(Locale.ROOT, "%02d", pad + 1));
                b.setTextSize(15);
                b.setTypeface(Typeface.DEFAULT_BOLD);
                b.setOnClickListener(v -> selectAndTriggerPad(pad, 112));
                b.setOnLongClickListener(v -> {
                    selectedPad = pad;
                    refreshPadSelectionVisuals();
                    showSamplePage();
                    return true;
                });
                padButtons[pad] = b;
                row.addView(b, weight());
            }
            grid.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }
        return grid;
    }

    private View buildInspector() {
        LinearLayout inspector = panel();
        inspector.addView(sectionLabel("SELECTED PAD"));

        selectedPadInfo = label("", 15, TEXT);
        selectedPadInfo.setTypeface(Typeface.DEFAULT_BOLD);
        inspector.addView(selectedPadInfo, marginParams());

        sampleInfo = label("", 12, MUTED);
        inspector.addView(sampleInfo, marginParams());

        LinearLayout layerRow = row();
        Button layerDown = actionButton("LAYER −", v -> {
            selectedLayer = Math.max(0, selectedLayer - 1);
            refreshAllInspectorState();
        });
        Button layerUp = actionButton("LAYER +", v -> {
            selectedLayer = Math.min(7, selectedLayer + 1);
            refreshAllInspectorState();
        });
        layerRow.addView(layerDown, weight());
        layerRow.addView(layerUp, weight());
        inspector.addView(layerRow);

        inspector.addView(sectionLabel("QUICK TONE"));

        LinearLayout tone1 = row();
        tone1.addView(actionButton("TUNE −1", v -> changePadTuning(-1)), weight());
        tone1.addView(actionButton("TUNE +1", v -> changePadTuning(1)), weight());
        tone1.addView(actionButton("LEVEL −10", v -> changePadLevel(-0.10f)), weight());
        tone1.addView(actionButton("LEVEL +10", v -> changePadLevel(0.10f)), weight());
        inspector.addView(tone1);

        LinearLayout tone2 = row();
        tone2.addView(actionButton("PAN L", v -> setPadPan(-1)), weight());
        tone2.addView(actionButton("PAN C", v -> setPadPan(0)), weight());
        tone2.addView(actionButton("PAN R", v -> setPadPan(1)), weight());
        tone2.addView(actionButton("EDIT", v -> showSamplePage()), weight());
        inspector.addView(tone2);

        regionInfo = label("", 11, MUTED);
        inspector.addView(regionInfo, marginParams());

        LinearLayout detail = row();
        detail.addView(actionButton("SAMPLE", v -> showSamplePage()), weight());
        detail.addView(actionButton("SEQ", v -> showSequencePage()), weight());
        detail.addView(actionButton("MIX", v -> showMixPage()), weight());
        inspector.addView(detail);

        return inspector;
    }

    private void showSamplePage() {
        currentPage = "SAMPLE";
        pageTitle.setText("SAMPLE");
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout header = row();
        header.addView(sectionLabelView("PAD " + (selectedPad + 1)
                + "  •  LAYER " + (selectedLayer + 1) + "/8",
                new LinearLayout.LayoutParams(0, dp(34), 1)));
        header.addView(actionButton("AUDITION", v -> selectAndTriggerPad(selectedPad, 112)),
                new LinearLayout.LayoutParams(dp(96), dp(38)));
        header.addView(actionButton("LOAD WAV", v -> openWavPicker()),
                new LinearLayout.LayoutParams(dp(110), dp(38)));
        page.addView(header);

        sampleWaveform = new WaveformView(this);
        sampleWaveform.setContentDescription("Sample waveform editor");
        sampleWaveform.setEditable(true);
        page.addView(sampleWaveform, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(184)));

        regionInfo = label("", 12, MUTED);
        regionInfo.setPadding(dp(10), 0, dp(10), 0);
        page.addView(regionInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        LinearLayout tabs = row();
        tabs.addView(actionButton("EDIT", v -> showSampleEditPanel(page)), weight());
        tabs.addView(actionButton("ENV", v -> showSampleEnvelopePanel(page)), weight());
        tabs.addView(actionButton("FILTER", v -> showSampleFilterPanel(page)), weight());
        tabs.addView(actionButton("LAYER", v -> showSampleLayerPanel(page)), weight());
        page.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        sampleWaveform.setOnSelectionCommitListener(
                (startNormalized, endNormalized) -> {
                    final long total = nativeAudioGetPadSampleFrameCount(
                            selectedPad, selectedLayer);
                    if (total <= 0) {
                        setBottomStatus("Waveform edit failed: no sample assigned");
                        return;
                    }
                    final long start = Math.max(
                            0, Math.min(total - 1,
                                    Math.round(startNormalized * total)));
                    final long end = Math.max(
                            start + 1, Math.min(total,
                                    Math.round(endNormalized * total)));
                    final String result = nativeAudioSetPadSampleRegion(
                            selectedPad, selectedLayer, start, end);
                    setBottomStatus(result);
                    refreshRegionInfo();
                });

        showSampleEditPanel(page);
        content.addView(page);
        refreshSampleInfo();
        refreshRegionInfo();
        refreshSampleWaveform();
    }

    private void showSampleEditPanel(LinearLayout page) {
        removeBelow(page, 4);
        if (sampleWaveform != null) {
            sampleWaveform.setEditable(true);
            sampleWaveform.setRecording(false);
        }

        LinearLayout row1 = row();
        row1.addView(actionButton("START −1K", v -> nudgeRegionStart(-1000)), weight());
        row1.addView(actionButton("START +1K", v -> nudgeRegionStart(1000)), weight());
        row1.addView(actionButton("END −1K", v -> nudgeRegionEnd(-1000)), weight());
        row1.addView(actionButton("END +1K", v -> nudgeRegionEnd(1000)), weight());
        page.addView(row1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout row2 = row();
        row2.addView(actionButton("FULL REGION", v -> resetRegion()), weight());
        row2.addView(actionButton("CROP", v -> cropRegion()), weight());
        row2.addView(actionButton("CHOP 4", v -> chop(4)), weight());
        row2.addView(actionButton("CHOP 8", v -> chop(8)), weight());
        row2.addView(actionButton("CHOP 16", v -> chop(16)), weight());
        page.addView(row2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        LinearLayout row3 = row();
        row3.addView(actionButton("AUDITION", v -> selectAndTriggerPad(selectedPad, 112)), weight());
        row3.addView(actionButton("ZOOM +", v -> {
            if (sampleWaveform != null) sampleWaveform.zoomIn();
        }), weight());
        row3.addView(actionButton("ZOOM −", v -> {
            if (sampleWaveform != null) sampleWaveform.zoomOut();
        }), weight());
        row3.addView(actionButton("RESET VIEW", v -> {
            if (sampleWaveform != null) sampleWaveform.resetZoom();
        }), weight());
        page.addView(row3, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        page.addView(label("Drag S / E to set the play range. Pinch or use ZOOM to inspect detail; swipe when zoomed to navigate. "
                        + "CROP is destructive and requires stopped audio.",
                11, MUTED), marginParams());
    }

    private void showSampleEnvelopePanel(LinearLayout page) {
        removeBelow(page, 4);
        if (sampleWaveform != null) sampleWaveform.setEditable(false);

        envelopeInfo = label("", 12, TEXT);
        envelopeInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        envelopeInfo.setGravity(Gravity.CENTER_VERTICAL);
        envelopeInfo.setPadding(dp(12), 0, dp(12), 0);
        page.addView(envelopeInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        page.addView(parameterRow("ATTACK", "−100", "+100",
                v -> changeEnvelope(-100, 0, 0, 0),
                v -> changeEnvelope(100, 0, 0, 0)), compactHeight());
        page.addView(parameterRow("DECAY", "−100", "+100",
                v -> changeEnvelope(0, -100, 0, 0),
                v -> changeEnvelope(0, 100, 0, 0)), compactHeight());
        page.addView(parameterRow("SUSTAIN", "−10%", "+10%",
                v -> changeEnvelope(0, 0, -0.10f, 0),
                v -> changeEnvelope(0, 0, 0.10f, 0)), compactHeight());
        page.addView(parameterRow("RELEASE", "−100", "+100",
                v -> changeEnvelope(0, 0, 0, -100),
                v -> changeEnvelope(0, 0, 0, 100)), compactHeight());
        page.addView(actionButton("ENVELOPE RESET", v -> setEnvelope(0, 0, 1, 0)),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        refreshEnvelopeInfo();
    }

    private void showSampleFilterPanel(LinearLayout page) {
        removeBelow(page, 4);
        if (sampleWaveform != null) sampleWaveform.setEditable(false);

        filterInfo = label("", 13, TEXT);
        filterInfo.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(filterInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout presets = row();
        float[] values = {300, 800, 1500, 3000, 6000, 12000, 20000};
        for (float value : values) {
            presets.addView(actionButton(formatCutoff(value),
                    v -> setFilter(((Button) v).getText().toString())),
                    weight());
        }
        page.addView(presets, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        SeekBar seek = new SeekBar(this);
        seek.setMax(20000);
        seek.setProgress(Math.round(nativeAudioGetPadFilterCutoff(selectedPad)));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    final String result = nativeAudioSetPadFilterCutoff(selectedPad, progress);
                    setBottomStatus(result);
                    refreshFilterInfo();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        page.addView(seek, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        page.addView(actionButton("FILTER RESET / 20 kHz",
                v -> {
                    final String result = nativeAudioSetPadFilterCutoff(selectedPad, 20000);
                    setBottomStatus(result);
                    refreshFilterInfo();
                }),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        refreshFilterInfo();
    }

    private void showSampleLayerPanel(LinearLayout page) {
        removeBelow(page, 4);
        if (sampleWaveform != null) sampleWaveform.setEditable(false);

        TextView layerInfo = label("", 12, TEXT);
        layerInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        layerInfo.setPadding(dp(10), 0, dp(10), 0);
        page.addView(layerInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        page.addView(parameterRow("GAIN", "−10", "+10",
                v -> changeLayerGain(-0.10f),
                v -> changeLayerGain(0.10f)), compactHeight());
        page.addView(parameterRow("TUNE", "−1", "+1",
                v -> changeLayerTuning(-1),
                v -> changeLayerTuning(1)), compactHeight());
        page.addView(parameterRow("PAN", "L", "R",
                v -> setLayerPan(-1),
                v -> setLayerPan(1)), compactHeight());
        page.addView(actionButton("CENTER PAN", v -> setLayerPan(0)),
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        final int min = nativeAudioGetPadLayerVelocityMin(selectedPad, selectedLayer);
        final int max = nativeAudioGetPadLayerVelocityMax(selectedPad, selectedLayer);
        layerInfo.setText("Layer " + (selectedLayer + 1)
                + "/8  •  Gain " + Math.round(nativeAudioGetPadLayerGain(selectedPad, selectedLayer) * 100)
                + "%  •  Tune " + formatSigned(nativeAudioGetPadLayerTuning(selectedPad, selectedLayer))
                + " st  •  Pan " + formatPan(nativeAudioGetPadLayerPan(selectedPad, selectedLayer))
                + "  •  Vel " + min + "-" + max);
    }

    private void showRecordPage() {
        currentPage = "REC";
        pageTitle.setText("RECORDER");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("INPUT / SAMPLER"));

        recordingInfo = label("", 13, TEXT);
        recordingInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        recordingInfo.setPadding(dp(12), 0, dp(12), 0);
        page.addView(recordingInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        recordingWaveform = new WaveformView(this);
        recordingWaveform.setContentDescription("Recording waveform monitor");
        recordingWaveform.setEditable(false);
        recordingWaveform.setRecording(false);
        page.addView(recordingWaveform, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(184)));

        recordingTelemetry = label("No recorded audio", 11, MUTED);
        recordingTelemetry.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(recordingTelemetry, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        LinearLayout controls1 = row();
        Button record = actionButton("RECORD", v -> {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_AUDIO);
                setBottomStatus("Microphone permission requested");
                return;
            }
            final String result = nativeAudioStartRecording();
            setBottomStatus(result);
            refreshRecordingInfo();
        });
        record.setTextColor(DANGER);
        controls1.addView(record, weight());
        controls1.addView(actionButton("STOP", v -> {
            final String result = nativeAudioStopRecording();
            setBottomStatus(result);
            refreshRecordingInfo();
        }), weight());
        controls1.addView(actionButton("ASSIGN", v -> {
            final String result = nativeAudioAssignRecordingToPadLayer(
                    selectedPad, selectedLayer);
            setBottomStatus(result);
            refreshRecordingInfo();
        }), weight());
        page.addView(controls1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout controls2 = row();
        controls2.addView(actionButton("MONITOR ON", v -> startMonitor()), weight());
        controls2.addView(actionButton("MONITOR OFF", v -> {
            final String result = nativeAudioStopMonitor();
            setBottomStatus(result);
            refreshRecordingInfo();
        }), weight());

        Button threshold = actionButton("THRESHOLD " +
                Math.round(nativeAudioGetRecordingThreshold() * 100) + "%",
                v -> {
                    float next = nativeAudioGetRecordingThreshold() + 0.25f;
                    if (next > 1.0f) next = 0.0f;
                    final String result = nativeAudioSetRecordingThreshold(next);
                    setBottomStatus(result);
                    refreshRecordingInfo();
                    showRecordPage();
                });
        controls2.addView(threshold, weight());
        page.addView(controls2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        page.addView(label("Record and Monitor remain independent. Use headphones for live monitoring to avoid acoustic feedback.",
                11, MUTED), marginParams());

        content.addView(page);
        refreshRecordingInfo();
        startRecordingWaveformUpdates();
    }

    private void showBrowserPage() {
        currentPage = "BROWSE";
        pageTitle.setText("BROWSER");
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout top = row();
        top.addView(sectionLabelView("PROJECT / USER AUDIO",
                new LinearLayout.LayoutParams(0, dp(38), 1)));
        top.addView(actionButton("LOAD WAV", v -> openWavPicker()),
                new LinearLayout.LayoutParams(dp(120), dp(40)));
        page.addView(top);

        TextView target = label("Target: Pad " + (selectedPad + 1)
                + " / Layer " + (selectedLayer + 1), 13, TEXT);
        target.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        target.setGravity(Gravity.CENTER_VERTICAL);
        target.setPadding(dp(12), 0, 0, 0);
        page.addView(target, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        TextView browserNote = label(
                "Browser architecture: Places → Content → Search → Results → Preview → Load. "
                        + "The current slice uses Android's document picker as the transport.",
                13, MUTED);
        browserNote.setBackground(strokeBackground(SURFACE, LINE, 8));
        browserNote.setPadding(dp(12), dp(10), dp(12), dp(10));
        page.addView(browserNote, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(92)));

        sampleInfo = label("", 12, TEXT);
        page.addView(sampleInfo, marginParams());
        refreshSampleInfo();

        content.addView(page);
    }

    private void showSequencePage() {
        currentPage = "SEQ";
        pageTitle.setText("SEQUENCER");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("PATTERN / GRID / STEP"));

        LinearLayout transport = row();
        transport.addView(actionButton("PLAY", v -> setBottomStatus(nativeAudioStart())), weight());
        transport.addView(actionButton("STOP", v -> setBottomStatus(nativeAudioStop())), weight());
        transport.addView(actionButton("GRID", v -> setBottomStatus("Grid editor shell ready")), weight());
        transport.addView(actionButton("STEP", v -> setBottomStatus("Step editor shell ready")), weight());
        page.addView(transport, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        TextView roadmap = label(
                "16-step performance canvas will live here: pattern length, quantize, swing, "
                        + "probability, ratchet and event editing without leaving the performance context.",
                14, TEXT);
        roadmap.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        roadmap.setPadding(dp(16), dp(16), dp(16), dp(16));
        page.addView(roadmap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        content.addView(page);
    }

    private void showMixPage() {
        currentPage = "MIX";
        pageTitle.setText("MIX");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("PAD / LAYER MIX"));

        LinearLayout strips = row();
        for (int pad = 0; pad < 4; pad++) {
            final int p = pad;
            LinearLayout strip = panel();
            TextView title = label("PAD " + (p + 1), 12, TEXT);
            title.setTypeface(Typeface.DEFAULT_BOLD);
            strip.addView(title);

            SeekBar level = new SeekBar(this);
            level.setMax(100);
            level.setProgress(Math.round(nativeAudioGetPadLevel(p) * 100));
            level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    if (fromUser) nativeAudioSetPadLevel(p, progress / 100.0f);
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
            strip.addView(level, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

            strip.addView(actionButton("SELECT", v -> {
                selectedPad = p;
                refreshPadSelectionVisuals();
                showMainPage();
            }), new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
            strips.addView(strip, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        page.addView(strips, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(page);
    }

    private void showMidiPage() {
        currentPage = "MIDI";
        pageTitle.setText("MIDI");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("MPC STUDIO MKII"));

        LinearLayout controls = row();
        controls.addView(actionButton("REFRESH", v -> {
            if (midiBridge == null) {
                setBottomStatus("MIDI bridge is still starting");
                return;
            }
            bottomStatus.setText(midiBridge.describeDevices());
        }), weight());
        controls.addView(actionButton("CONNECT", v -> {
            if (midiBridge == null) {
                setBottomStatus("MIDI bridge is still starting");
                return;
            }
            midiBridge.connectPreferred();
        }), weight());
        controls.addView(actionButton("PAD LED", v -> {
            if (midiBridge != null) midiBridge.testPadBlue();
        }), weight());
        controls.addView(actionButton("LCD", v -> {
            if (midiBridge != null) midiBridge.testLcd();
        }), weight());
        page.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        TextView devices = label("Tap REFRESH to inspect Android MIDI devices.",
                13, MUTED);
        devices.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        devices.setPadding(dp(12), dp(10), dp(12), dp(10));
        page.addView(devices, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(page);
    }

    private void showMenuPage() {
        currentPage = "MENU";
        pageTitle.setText("MENU");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("WORK MODES"));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        String[][] items = {
                {"MAIN", "MAIN"}, {"BROWSER", "BROWSE"}, {"SAMPLE", "SAMPLE"}, {"RECORDER", "REC"},
                {"SEQUENCER", "SEQ"}, {"MIXER", "MIX"}, {"GRID", "GRID"}, {"STEP", "STEP"},
                {"TRACK EDIT", "TRACK"}, {"PAD MIX", "PAD"}, {"Q-LINK", "QLINK"}, {"PROJECT", "PROJECT"}
        };

        for (String[] item : items) {
            final String menuLabel = item[0];
            final String menuTarget = item[1];
            Button b = actionButton(menuLabel, v -> {
                switch (menuTarget) {
                    case "MAIN": showMainPage(); break;
                    case "BROWSE": showBrowserPage(); break;
                    case "SAMPLE": showSamplePage(); break;
                    case "REC": showRecordPage(); break;
                    case "SEQ": showSequencePage(); break;
                    case "MIX": showMixPage(); break;
                    default: setBottomStatus(menuLabel + " shell reserved for the next UI slice");
                }
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(62);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            grid.addView(b, lp);
        }

        page.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(page);
    }

    private View parameterRow(String name, String minus, String plus,
                              View.OnClickListener minusAction,
                              View.OnClickListener plusAction) {
        LinearLayout row = row();
        TextView title = label(name, 11, MUTED);
        title.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1));
        row.addView(actionButton(minus, minusAction), new LinearLayout.LayoutParams(dp(82), dp(44)));
        row.addView(actionButton(plus, plusAction), new LinearLayout.LayoutParams(dp(82), dp(44)));
        return row;
    }

    private void selectAndTriggerPad(int pad, int velocity) {
        selectedPad = pad;
        refreshPadSelectionVisuals();
        if (startupComplete) {
            nativeAudioTriggerPad(pad, velocity);
            setBottomStatus("AUDITION • Pad " + (pad + 1)
                    + " • velocity " + velocity
                    + " | " + nativeAudioStatus());
        } else {
            setBottomStatus("Audio engine still starting");
        }
    }

    private void refreshPadSelectionVisuals() {
        for (int i = 0; i < padButtons.length; i++) {
            if (padButtons[i] == null) continue;
            int fill = i == selectedPad ? Color.rgb(32, 52, 60) : SURFACE_2;
            int stroke = i == selectedPad ? ACCENT : LINE;
            padButtons[i].setBackground(strokeBackground(fill, stroke, 8));
        }
        refreshAllInspectorState();
    }

    private void refreshAllInspectorState() {
        if (selectedPadInfo != null) {
            selectedPadInfo.setText("Pad " + (selectedPad + 1)
                    + "  •  Layer " + (selectedLayer + 1) + "/8");
        }
        refreshSampleInfo();
        refreshRegionInfo();
    }

    private void refreshSampleInfo() {
        if (sampleInfo == null) return;
        long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            sampleInfo.setText("No explicit sample • bundled fallback is available on unassigned pads");
            return;
        }
        sampleInfo.setText("Sample assigned • " + total + " frames");
    }

    private void refreshRegionInfo() {
        if (regionInfo == null) return;
        long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            regionInfo.setText("Region: no sample");
            return;
        }
        long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        regionInfo.setText("Region " + start + " → " + end + "  /  " + total + " frames");
    }

    private void refreshEnvelopeInfo() {
        if (envelopeInfo == null) return;
        envelopeInfo.setText(String.format(Locale.ROOT,
                "A %.0f ms  •  D %.0f ms  •  S %.0f%%  •  R %.0f ms",
                nativeAudioGetPadEnvelopeAttack(selectedPad),
                nativeAudioGetPadEnvelopeDecay(selectedPad),
                nativeAudioGetPadEnvelopeSustain(selectedPad) * 100,
                nativeAudioGetPadEnvelopeRelease(selectedPad)));
    }

    private void refreshFilterInfo() {
        if (filterInfo == null) return;
        filterInfo.setText("Cutoff " + formatCutoff(nativeAudioGetPadFilterCutoff(selectedPad)));
    }

    private void refreshRecordingInfo() {
        final String status = nativeAudioRecordingStatus();
        if (recordingInfo != null) {
            recordingInfo.setText(status);
        }

        if (recordingTelemetry != null) {
            final int frames = nativeAudioGetRecordingFrameCount();
            final int sampleRate = nativeAudioGetRecordingSampleRate();
            final float peak = nativeAudioGetRecordingPeak();
            final double seconds = sampleRate > 0
                    ? ((double) frames / (double) sampleRate)
                    : 0.0;
            recordingTelemetry.setText(String.format(
                    Locale.ROOT,
                    "Duration %.2fs  •  Peak %d%%  •  Frames %d",
                    seconds,
                    Math.round(peak * 100.0f),
                    frames));
        }
    }

    private void refreshSampleWaveform() {
        if (sampleWaveform == null) return;

        final long frames = nativeAudioGetPadSampleFrameCount(
                selectedPad, selectedLayer);
        final int sampleRate = nativeAudioGetPadSampleRate(
                selectedPad, selectedLayer);

        if (frames <= 0) {
            sampleWaveform.setPeaks(null);
            sampleWaveform.setSelection(0f, 1f);
            sampleWaveform.setDurationMs(0f);
            return;
        }

        final float[] peaks = nativeAudioGetPadWaveformPeaks(
                selectedPad, selectedLayer, 768);
        sampleWaveform.setPeaks(peaks);
        final long start = nativeAudioGetPadSampleRegionStart(
                selectedPad, selectedLayer);
        final long end = nativeAudioGetPadSampleRegionEnd(
                selectedPad, selectedLayer);
        sampleWaveform.setSelection(
                start / (float) frames,
                end / (float) frames);
        sampleWaveform.setDurationMs(
                sampleRate > 0
                        ? frames * 1000.0f / sampleRate
                        : 0.0f);
        sampleWaveform.setRecording(false);
    }

    private void startRecordingWaveformUpdates() {
        if (recordingWaveformUpdater == null) {
            recordingWaveformUpdater = new Runnable() {
                @Override
                public void run() {
                    if (!"REC".equals(currentPage) || recordingWaveform == null) {
                        waveformUiHandler.removeCallbacks(this);
                        return;
                    }
                    refreshRecordingWaveform();
                    waveformUiHandler.postDelayed(this, 100);
                }
            };
        }
        waveformUiHandler.removeCallbacks(recordingWaveformUpdater);
        waveformUiHandler.post(recordingWaveformUpdater);
    }

    private void refreshRecordingWaveform() {
        if (recordingWaveform == null) return;

        final String status = nativeAudioRecordingStatus();
        final int frames = nativeAudioGetRecordingFrameCount();
        final int sampleRate = nativeAudioGetRecordingSampleRate();
        final int capacity = Math.max(1, nativeAudioGetRecordingFrameCapacity());

        recordingWaveform.setPeaks(
                nativeAudioGetRecordingWaveformPeaks(512));
        recordingWaveform.setProgress(
                Math.min(1.0f, frames / (float) capacity));
        recordingWaveform.setDurationMs(
                sampleRate > 0
                        ? frames * 1000.0f / sampleRate
                        : 0.0f);
        recordingWaveform.setRecording(
                status.startsWith("Recording active")
                        || status.startsWith("Recording armed"));

        refreshRecordingInfo();
    }


    private void changePadTuning(float delta) {
        String result = nativeAudioSetPadTuning(
                selectedPad, nativeAudioGetPadTuning(selectedPad) + delta);
        setBottomStatus(result);
    }

    private void changePadLevel(float delta) {
        String result = nativeAudioSetPadLevel(
                selectedPad, nativeAudioGetPadLevel(selectedPad) + delta);
        setBottomStatus(result);
    }

    private void setPadPan(float pan) {
        String result = nativeAudioSetPadPan(selectedPad, pan);
        setBottomStatus(result);
    }

    private void nudgeRegionStart(long delta) {
        long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            setBottomStatus("Sample region change failed: no sample assigned");
            return;
        }
        long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        long next = Math.max(0, Math.min(end - 1, start + delta));
        setBottomStatus(nativeAudioSetPadSampleRegion(
                selectedPad, selectedLayer, next, end));
        refreshRegionInfo();
    }

    private void nudgeRegionEnd(long delta) {
        long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            setBottomStatus("Sample region change failed: no sample assigned");
            return;
        }
        long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        long next = Math.min(total, Math.max(start + 1, end + delta));
        setBottomStatus(nativeAudioSetPadSampleRegion(
                selectedPad, selectedLayer, start, next));
        refreshRegionInfo();
    }

    private void resetRegion() {
        long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            setBottomStatus("Sample region change failed: no sample assigned");
            return;
        }
        setBottomStatus(nativeAudioSetPadSampleRegion(
                selectedPad, selectedLayer, 0, total));
        refreshRegionInfo();
    }

    private void cropRegion() {
        setBottomStatus(nativeAudioCropPadSampleRegion(selectedPad, selectedLayer));
        refreshAllInspectorState();
        refreshSampleWaveform();
    }

    private void chop(int count) {
        setBottomStatus(nativeAudioChopPadSampleToPads(
                selectedPad, selectedLayer, count));
        refreshAllInspectorState();
        refreshSampleWaveform();
    }

    private void setEnvelope(float attack, float decay, float sustain, float release) {
        setBottomStatus(nativeAudioSetPadEnvelope(
                selectedPad, attack, decay, sustain, release));
        refreshEnvelopeInfo();
    }

    private void changeEnvelope(float attackDelta, float decayDelta,
                                float sustainDelta, float releaseDelta) {
        float attack = Math.max(0, Math.min(2000,
                nativeAudioGetPadEnvelopeAttack(selectedPad) + attackDelta));
        float decay = Math.max(0, Math.min(2000,
                nativeAudioGetPadEnvelopeDecay(selectedPad) + decayDelta));
        float sustain = Math.max(0, Math.min(1,
                nativeAudioGetPadEnvelopeSustain(selectedPad) + sustainDelta));
        float release = Math.max(0, Math.min(2000,
                nativeAudioGetPadEnvelopeRelease(selectedPad) + releaseDelta));
        setEnvelope(attack, decay, sustain, release);
    }

    private void setFilter(String label) {
        String normalized = label.toLowerCase(Locale.ROOT)
                .replace("khz", "")
                .replace("hz", "")
                .trim();
        float value;
        try {
            value = Float.parseFloat(normalized);
            if (label.toLowerCase(Locale.ROOT).contains("khz")) value *= 1000f;
        } catch (NumberFormatException e) {
            value = 20000f;
        }
        String result = nativeAudioSetPadFilterCutoff(selectedPad, value);
        setBottomStatus(result);
        refreshFilterInfo();
    }

    private void changeLayerGain(float delta) {
        setBottomStatus(nativeAudioSetPadLayerGain(
                selectedPad, selectedLayer,
                nativeAudioGetPadLayerGain(selectedPad, selectedLayer) + delta));
    }

    private void changeLayerTuning(float delta) {
        setBottomStatus(nativeAudioSetPadLayerTuning(
                selectedPad, selectedLayer,
                nativeAudioGetPadLayerTuning(selectedPad, selectedLayer) + delta));
    }

    private void setLayerPan(float pan) {
        setBottomStatus(nativeAudioSetPadLayerPan(
                selectedPad, selectedLayer, pan));
    }

    private void startMonitor() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_MONITOR_AUDIO);
            setBottomStatus("Microphone permission requested for monitor");
            return;
        }
        final String result = nativeAudioStartMonitor();
        setBottomStatus(result);
        setAudioStateFromResult(result);
        refreshRecordingInfo();
    }

    private void openWavPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/wav");
        try {
            startActivityForResult(intent, REQUEST_OPEN_WAV);
        } catch (RuntimeException e) {
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_OPEN_WAV);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_WAV || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null) {
            setBottomStatus("Sample load failed: no file selected");
            return;
        }

        try {
            setBottomStatus("Loading WAV…");
            byte[] bytes = readSampleBytes(uri);

            nativeAudioStop();
            final String loaded = nativeAudioLoadSampleForPadLayer(
                    bytes, selectedPad, selectedLayer);
            final String restarted = nativeAudioStart();

            setAudioStateFromResult(restarted);
            setBottomStatus(loaded + " | " + restarted);
            refreshAllInspectorState();
            refreshSampleWaveform();
        } catch (IOException | IllegalArgumentException e) {
            final String restarted = nativeAudioStart();
            setAudioStateFromResult(restarted);
            setBottomStatus("Sample load failed: "
                    + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + " | " + restarted);
        }
    }

    private byte[] readSampleBytes(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) throw new IOException("could not open selected file");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_SAMPLE_BYTES) {
                    throw new IOException("file is larger than 32 MB");
                }
                output.write(buffer, 0, count);
            }
            if (output.size() == 0) throw new IOException("selected file is empty");
            return output.toByteArray();
        }
    }

    private String loadBundledSample() {
        try (InputStream input = getAssets().open("samples/pad01.wav.b64")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            byte[] wavBytes = android.util.Base64.decode(
                    output.toString(StandardCharsets.UTF_8.name()),
                    android.util.Base64.DEFAULT);
            String fallback = nativeAudioLoadSample(wavBytes);
            String padLayer = nativeAudioLoadSampleForPadLayer(
                    wavBytes, selectedPad, selectedLayer);
            return fallback + " | " + padLayer;
        } catch (IOException | IllegalArgumentException e) {
            return "Sample asset load failed: " + e.getMessage();
        }
    }

    private void setAudioStateFromResult(String result) {
        if (audioState == null) return;

        final boolean active = result != null
                && (result.startsWith("Audio output")
                || result.startsWith("Recording active")
                || result.startsWith("Recording stopped")
                || result.startsWith("Recording armed"));
        final boolean failed = result != null
                && (result.contains("failed")
                || result.contains("Failure")
                || result.contains("error"));

        audioState.setText(active ? "AUDIO ON" : (failed ? "AUDIO ERR" : "AUDIO OFF"));
        audioState.setTextColor(
                active ? ACTIVE : (failed ? DANGER : MUTED));
    }

    private void setBottomStatus(String text) {
        if (bottomStatus != null && text != null) bottomStatus.setText(text);
    }

    private TextView sectionLabelView(String text, ViewGroup.LayoutParams params) {
        TextView view = sectionLabel(text);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView sectionLabel(String text) {
        TextView view = label(text, 11, MUTED);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setPadding(dp(4), 0, dp(4), 0);
        return view;
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private Button topButton(String text) {
        Button b = button(text);
        b.setTextSize(10);
        return b;
    }

    private Button actionButton(String text, View.OnClickListener listener) {
        Button b = button(text);
        b.setTextSize(11);
        b.setOnClickListener(listener);
        return b;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        return b;
    }

    private TextView statusChip(String text, int color) {
        TextView t = label(text, 10, color);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        return t;
    }

    private LinearLayout page() {
        LinearLayout p = column();
        p.setPadding(dp(10), dp(8), dp(10), dp(8));
        return p;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout panel() {
        LinearLayout l = column();
        l.setPadding(dp(10), dp(8), dp(10), dp(8));
        l.setBackground(strokeBackground(SURFACE, LINE, 10));
        return l;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
    }

    private LinearLayout.LayoutParams marginParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        p.topMargin = dp(5);
        p.bottomMargin = dp(5);
        return p;
    }

    private LinearLayout.LayoutParams compactHeight() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
    }

    private GradientDrawable strokeBackground(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), stroke);
        return g;
    }

    private void removeBelow(LinearLayout page, int index) {
        while (page.getChildCount() > index) {
            page.removeViewAt(index);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String formatSigned(float value) {
        return String.format(Locale.ROOT, "%+.2f", value);
    }

    private String formatPan(float pan) {
        if (pan < -0.001f) return "L" + Math.round(-pan * 100);
        if (pan > 0.001f) return "R" + Math.round(pan * 100);
        return "C";
    }

    private String formatCutoff(float value) {
        if (value >= 1000) {
            return String.format(Locale.ROOT, "%.1fkHz", value / 1000.0f);
        }
        return Math.round(value) + "Hz";
    }

    private void runUiAudit() {
        Log.i(TAG, "UI_HIERARCHY_BEGIN");
        String[] expected = {
                "MAIN", "BROWSE", "SAMPLE", "SEQ", "MIX", "REC", "MENU",
                "PLAY", "STOP", "MIDI", "01", "16", "LOAD"
        };

        for (String text : expected) {
            View view = findViewWithExactText(getWindow().getDecorView(), text);
            if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e(TAG, "UI_HIERARCHY_FAILED: " + text);
                return;
            }
        }
        Log.i(TAG, "UI_HIERARCHY_COMPLETE");
        Log.i(TAG, "UI_INTERACTION_BEGIN");

        View pad1 = findViewWithExactText(getWindow().getDecorView(), "01");
        if (pad1 == null || !pad1.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: pad 01");
            return;
        }
        if (selectedPad != 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: selection did not stick");
            return;
        }

        View sample = findViewWithExactText(getWindow().getDecorView(), "SAMPLE");
        if (sample == null || !sample.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: SAMPLE");
            return;
        }

        if (findViewWithExactText(getWindow().getDecorView(), "ENV") == null
                || findViewWithExactText(getWindow().getDecorView(), "FILTER") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Sample waveform editor") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sample waveform editor");
            return;
        }

        View rec = findViewWithExactText(getWindow().getDecorView(), "REC");
        if (rec == null || !rec.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: REC mode");
            return;
        }

        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Recording waveform monitor") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: recording waveform");
            return;
        }

        Log.i(TAG, "UI_INTERACTION_COMPLETE");
    }

    private View findViewWithContentDescription(
            View view, String expectedDescription) {
        CharSequence actual = view.getContentDescription();
        if (expectedDescription.contentEquals(actual)) return view;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = findViewWithContentDescription(
                        group.getChildAt(i), expectedDescription);
                if (match != null) return match;
            }
        }
        return null;
    }

    private View findViewWithExactText(View view, String expectedText) {
        if (view instanceof TextView) {
            CharSequence actual = ((TextView) view).getText();
            if (expectedText.contentEquals(actual)) return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View match = findViewWithExactText(group.getChildAt(i), expectedText);
                if (match != null) return match;
            }
        }
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            setBottomStatus("Microphone permission denied");
            return;
        }

        if (requestCode == REQUEST_RECORD_AUDIO) {
            final String result = nativeAudioStartRecording();
            setBottomStatus(result);
            refreshRecordingInfo();
            return;
        }

        if (requestCode == REQUEST_MONITOR_AUDIO) {
            final String result = nativeAudioStartMonitor();
            setBottomStatus(result);
            refreshRecordingInfo();
            setAudioStateFromResult(result);
        }
    }

    @Override
    public void onDevicesChanged(String description) {
        if (midiState != null) {
            runOnUiThread(() -> {
                if (midiState != null) {
                    midiState.setText(description.contains("MPC Studio")
                            ? "MIDI READY" : "MIDI —");
                }
            });
        }
    }

    @Override
    public void onMidi(String description) {
        Log.d(TAG, "MIDI IN " + description);
    }

    @Override
    public void onConnection(String description) {
        runOnUiThread(() -> {
            boolean connected = description != null
                    && description.startsWith("Connected:");
            midiState.setText(connected ? "MIDI ON" : "MIDI —");
            midiState.setTextColor(connected ? ACTIVE : MUTED);
            setBottomStatus(description);
        });
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyFullscreenWindowPolicy();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        if (recordingWaveformUpdater != null) {
            waveformUiHandler.removeCallbacks(recordingWaveformUpdater);
        }
        startupExecutor.shutdownNow();

        if (!uiOnlySmokeMode) {
            nativeAudioStop();
        }

        if (midiBridge != null) {
            midiBridge.close();
            midiBridge = null;
        }
        super.onDestroy();
    }
}
