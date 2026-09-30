package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.ScrollView;
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
    private static final int TYPE_SAMPLE = Color.rgb(69, 211, 255);
    private static final int TYPE_SYNTH = Color.rgb(188, 124, 255);
    private static final int TYPE_MIDI = Color.rgb(255, 180, 72);
    private static final int TYPE_AUDIO = Color.rgb(63, 207, 117);

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
    private final Button[] modeButtons = new Button[8];
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
    private final Handler transportUiHandler = new Handler(Looper.getMainLooper());
    private Runnable transportUiUpdater;
    private SequencerProgressView sequenceProgressView;
    private TextView sequenceCounterView;
    private TextView sequencePositionView;
    private TextView tempoView;
    private TextView recordModeView;
    private LinearLayout trackList;

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

    private static native String nativeSequenceStatus();
    private static native double nativeSequenceGetTempo();
    private static native String nativeSequenceSetTempo(double tempo);
    private static native int nativeSequenceGetBars();
    private static native String nativeSequenceSetBars(int bars);
    private static native int nativeSequenceGetNumerator();
    private static native int nativeSequenceGetDenominator();
    private static native String nativeSequenceSetTimeSignature(int numerator, int denominator);
    private static native int nativeSequenceGetRecordMode();
    private static native String nativeSequenceSetRecordMode(int mode);
    private static native int nativeSequenceGetTrackCount();
    private static native int nativeSequenceGetSelectedTrack();
    private static native String nativeSequenceSelectTrack(int trackIndex);
    private static native String nativeSequenceAddTrack(int kind);
    private static native String nativeSequenceTrackStatus(int trackIndex);
    private static native boolean nativeSequenceIsTrackMuted(int trackIndex);
    private static native boolean nativeSequenceIsTrackSoloed(int trackIndex);
    private static native String nativeSequenceSetTrackMuted(int trackIndex, boolean muted);
    private static native String nativeSequenceSetTrackSoloed(int trackIndex, boolean soloed);
    private static native boolean nativeSequenceIsSelectedTrackArmed();
    private static native String nativeSequenceSetSelectedTrackArmed(boolean armed);
    private static native String nativeSequenceStart();
    private static native String nativeSequenceStop();
    private static native String nativeSequenceReset();
    private static native int nativeSequenceAdvance(long milliseconds);
    private static native long nativeSequencePositionTicks();
    private static native boolean nativeSequenceIsPlaying();
    private static native int nativeSequenceGetIndex();
    private static native int nativeSequenceGetCount();
    private static native boolean nativeSequenceIsChainEnabled();
    private static native String nativeSequenceSetChainEnabled(boolean enabled);
    private static native String nativeSequenceAddSequence();
    private static native String nativeSequenceNext();
    private static native String nativeSequenceRecordToggle();
    private static native String nativeSequenceCapturePadHit(int pad, int velocity);

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

                    startTransportUiUpdates();
                    refreshTransportUi();

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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        sequenceProgressView = new SequencerProgressView(this);
        sequenceProgressView.setContentDescription("Sequence playback progress");
        root.addView(sequenceProgressView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));

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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        showMainPage();
        updateModeRailSelection();
        return root;
    }

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(5), dp(8), dp(5));
        bar.setBackgroundColor(SURFACE);

        projectState = label("UNTITLED", 12, TEXT);
        projectState.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(projectState, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1.05f));

        pageTitle = label("MAIN", 10, MUTED);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(pageTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.34f));

        sequenceCounterView = statusChip("SEQ 01", TEXT);
        sequenceCounterView.setTextSize(9);
        sequenceCounterView.setContentDescription("Current sequence counter");
        sequenceCounterView.setOnClickListener(v -> {
            final String result = nativeSequenceNext();
            setBottomStatus(result);
            refreshTransportUi();
            refreshTrackGrid();
        });
        bar.addView(sequenceCounterView, new LinearLayout.LayoutParams(dp(62), dp(36)));

        sequencePositionView = statusChip("001.1.000", TEXT);
        sequencePositionView.setTextSize(9);
        bar.addView(sequencePositionView, new LinearLayout.LayoutParams(dp(82), dp(36)));

        tempoView = statusChip("120.0", TEXT);
        tempoView.setTextSize(9);
        bar.addView(tempoView, new LinearLayout.LayoutParams(dp(58), dp(36)));

        Button stop = transportButton("STOP", SURFACE_2, LINE, TEXT);
        stop.setOnClickListener(v -> {
            final String sequenceResult = nativeSequenceStop();
            final String audioResult = nativeAudioStop();
            setAudioStateFromResult(audioResult);
            setBottomStatus(sequenceResult + " | " + audioResult);
            refreshTransportUi();
        });
        bar.addView(stop, new LinearLayout.LayoutParams(dp(54), dp(38)));

        Button play = transportButton("PLAY", ACTIVE, ACTIVE, BG);
        play.setOnClickListener(v -> {
            final String result = nativeSequenceStart();
            setAudioStateFromResult(result);
            setBottomStatus(result);
            refreshTransportUi();
        });
        bar.addView(play, new LinearLayout.LayoutParams(dp(72), dp(40)));

        Button rec = transportButton("REC", DANGER, DANGER, Color.WHITE);
        rec.setContentDescription("Sequence Record");
        rec.setOnClickListener(v -> {
            final String result = nativeSequenceRecordToggle();
            setBottomStatus(result);
            refreshTransportUi();
            refreshTrackGrid();
        });
        bar.addView(rec, new LinearLayout.LayoutParams(dp(72), dp(40)));

        recordModeView = statusChip("REPL", TEXT);
        recordModeView.setTextSize(8);
        recordModeView.setContentDescription("Recording mode");
        recordModeView.setOnClickListener(v -> {
            final int next = nativeSequenceGetRecordMode() == 0 ? 1 : 0;
            setBottomStatus(nativeSequenceSetRecordMode(next));
            refreshTransportUi();
        });
        bar.addView(recordModeView, new LinearLayout.LayoutParams(dp(54), dp(36)));

        audioState = statusChip("AUDIO", MUTED);
        audioState.setTextSize(8);
        bar.addView(audioState, new LinearLayout.LayoutParams(dp(52), dp(34)));

        midiState = statusChip("MIDI", MUTED);
        midiState.setTextSize(8);
        bar.addView(midiState, new LinearLayout.LayoutParams(dp(52), dp(34)));

        Button midi = topButton("MIDI");
        midi.setOnClickListener(v -> showMidiPage());
        bar.addView(midi, new LinearLayout.LayoutParams(dp(48), dp(36)));

        Button menu = topButton("MENU");
        menu.setOnClickListener(v -> showMenuPage());
        bar.addView(menu, new LinearLayout.LayoutParams(dp(58), dp(36)));

        return bar;
    }


    private View buildModeRail() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(dp(6), dp(6), dp(6), dp(6));
        rail.setBackgroundColor(Color.rgb(18, 21, 24));

        String[] pages = {
                "MAIN", "BROWSE", "SAMPLE", "SEQ",
                "TRACKS", "MIX", "REC", "MENU"
        };
        for (int i = 0; i < pages.length; i++) {
            Button button = modeButton(pages[i], pages[i]);
            modeButtons[i] = button;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            params.topMargin = i == 0 ? 0 : dp(3);
            rail.addView(button, params);
        }
        return rail;
    }

    private Button modeButton(String text, String page) {
        Button b = button(text);
        b.setTextSize(11);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(3), 0, dp(3), 0);
        b.setTag(page);
        b.setContentDescription(page + " mode");
        b.setOnClickListener(v -> {
            switch (page) {
                case "MAIN": showMainPage(); break;
                case "BROWSE": showBrowserPage(); break;
                case "SAMPLE": showSamplePage(); break;
                case "SEQ": showSequencePage(); break;
                case "TRACKS": showTrackPage(); break;
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
        page.addView(buildSelectedTrackContext(),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));

        LinearLayout workspace = row();

        LinearLayout padSurface = column();
        padSurface.addView(sectionLabel("PERFORM • 16 PADS"));
        padSurface.addView(buildPadGrid(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout quick = row();
        quick.addView(actionButton("LOAD", v -> openWavPicker()), weight());
        quick.addView(actionButton("SAMPLE", v -> showSamplePage()), weight());
        quick.addView(actionButton("SEQ", v -> showSequencePage()), weight());
        quick.addView(actionButton("TRACKS", v -> showTrackPage()), weight());
        quick.addView(actionButton("REC", v -> showRecordPage()), weight());
        padSurface.addView(quick, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        workspace.addView(padSurface, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.68f));
        workspace.addView(buildInspector(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.32f));

        page.addView(workspace, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        content.addView(page);
        refreshPadSelectionVisuals();
        updateModeRailSelection();
    }

    private View buildSelectedTrackContext() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.HORIZONTAL);
        shell.setGravity(Gravity.CENTER_VERTICAL);
        shell.setPadding(dp(8), dp(4), dp(8), dp(4));
        shell.setBackground(strokeBackground(SURFACE, LINE, 8));

        final int trackCount = nativeSequenceGetTrackCount();
        if (trackCount <= 0) {
            shell.addView(label("NO TRACKS", 11, MUTED), new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            Button tracks = actionButton("TRACKS", v -> showTrackPage());
            shell.addView(tracks, new LinearLayout.LayoutParams(dp(82), dp(42)));
            return shell;
        }

        final int selected = Math.max(0, Math.min(
                trackCount - 1, nativeSequenceGetSelectedTrack()));
        final String raw = nativeSequenceTrackStatus(selected);
        final String type = trackTypeLabel(raw);
        final String name = trackName(raw);

        TextView index = label(String.format(Locale.ROOT, "%02d", selected + 1),
                13, trackTypeColor(type));
        index.setGravity(Gravity.CENTER);
        index.setTypeface(Typeface.DEFAULT_BOLD);
        shell.addView(index, new LinearLayout.LayoutParams(dp(38), dp(42)));

        LinearLayout textBlock = column();
        TextView typeLabel = label(type, 8, trackTypeColor(type));
        typeLabel.setTypeface(Typeface.DEFAULT_BOLD);
        textBlock.addView(typeLabel);
        TextView nameLabel = label(name, 12, TEXT);
        nameLabel.setTypeface(Typeface.DEFAULT_BOLD);
        textBlock.addView(nameLabel);
        shell.addView(textBlock, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        Button mute = actionButton(
                nativeSequenceIsTrackMuted(selected) ? "M*" : "M",
                v -> {
                    setBottomStatus(nativeSequenceSetTrackMuted(
                            selected, !nativeSequenceIsTrackMuted(selected)));
                    showMainPage();
                });
        mute.setTextColor(nativeSequenceIsTrackMuted(selected) ? DANGER : TEXT);
        shell.addView(mute, new LinearLayout.LayoutParams(dp(44), dp(42)));

        Button solo = actionButton(
                nativeSequenceIsTrackSoloed(selected) ? "S*" : "S",
                v -> {
                    setBottomStatus(nativeSequenceSetTrackSoloed(
                            selected, !nativeSequenceIsTrackSoloed(selected)));
                    showMainPage();
                });
        solo.setTextColor(nativeSequenceIsTrackSoloed(selected) ? TYPE_MIDI : TEXT);
        shell.addView(solo, new LinearLayout.LayoutParams(dp(44), dp(42)));

        Button arm = actionButton(
                nativeSequenceIsSelectedTrackArmed() ? "ARM*" : "ARM",
                v -> {
                    final boolean next = !nativeSequenceIsSelectedTrackArmed();
                    setBottomStatus(nativeSequenceSetSelectedTrackArmed(next));
                    showMainPage();
                });
        arm.setTextColor(nativeSequenceIsSelectedTrackArmed() ? DANGER : TEXT);
        shell.addView(arm, new LinearLayout.LayoutParams(dp(60), dp(42)));

        Button tracks = actionButton("ALL", v -> showTrackPage());
        shell.addView(tracks, new LinearLayout.LayoutParams(dp(44), dp(42)));

        return shell;
    }


    private View buildPadGrid() {
        LinearLayout grid = column();
        for (int rowIndex = 0; rowIndex < 4; rowIndex++) {
            LinearLayout row = row();
            for (int col = 0; col < 4; col++) {
                 // MPC Studio MkII physical numbering: bottom row 1-4, then 5-8, 9-12, top row 13-16.
                final int pad = (3 - rowIndex) * 4 + col;
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
                new LinearLayout.LayoutParams(dp(110), dp(34)));
        page.addView(header);

        sampleWaveform = new WaveformView(this);
        sampleWaveform.setContentDescription("Sample waveform editor");
        sampleWaveform.setEditable(true);
        sampleWaveform.setMinimumHeight(dp(144));
        page.addView(sampleWaveform, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        regionInfo = label("", 12, MUTED);
        regionInfo.setPadding(dp(10), 0, dp(10), 0);
        page.addView(regionInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

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
        row1.addView(actionButton("S −1K", v -> nudgeRegionStart(-1000)), weight());
        row1.addView(actionButton("S +1K", v -> nudgeRegionStart(1000)), weight());
        row1.addView(actionButton("E −1K", v -> nudgeRegionEnd(-1000)), weight());
        row1.addView(actionButton("E +1K", v -> nudgeRegionEnd(1000)), weight());
        page.addView(row1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout row2 = row();
        row2.addView(actionButton("FULL", v -> resetRegion()), weight());
        row2.addView(actionButton("CROP", v -> cropRegion()), weight());
        row2.addView(actionButton("CH4", v -> chop(4)), weight());
        row2.addView(actionButton("CH8", v -> chop(8)), weight());
        row2.addView(actionButton("CH16", v -> chop(16)), weight());
        row2.addView(actionButton("PLAY", v -> selectAndTriggerPad(selectedPad, 112)), weight());
        row2.addView(actionButton("ZOOM+", v -> {
            if (sampleWaveform != null) sampleWaveform.zoomIn();
        }), weight());
        row2.addView(actionButton("ZOOM−", v -> {
            if (sampleWaveform != null) sampleWaveform.zoomOut();
        }), weight());
        row2.addView(actionButton("RESET", v -> {
            if (sampleWaveform != null) sampleWaveform.resetZoom();
        }), weight());
        page.addView(row2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));
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

        recordingInfo = label("", 13, TEXT);
        recordingInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        recordingInfo.setPadding(dp(12), 0, dp(12), 0);
        page.addView(recordingInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        recordingWaveform = new WaveformView(this);
        recordingWaveform.setContentDescription("Recording waveform monitor");
        recordingWaveform.setEditable(false);
        recordingWaveform.setRecording(false);
        recordingWaveform.setMinimumHeight(dp(144));
        page.addView(recordingWaveform, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

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
            if (result != null
                    && result.startsWith("Recording assigned")
                    && !result.contains("sampler restart failed")) {
                showSamplePage();
            } else {
                refreshRecordingInfo();
            }
        }), weight());
        page.addView(controls1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        content.addView(page);
        refreshRecordingInfo();
        startRecordingWaveformUpdates();
    }

    private View buildTrackControlPanel() {
        LinearLayout panel = panel();

        LinearLayout header = row();
        header.addView(sectionLabel("TRACKS • SEQUENCE "
                + String.format(Locale.ROOT, "%02d", nativeSequenceGetIndex() + 1)),
                new LinearLayout.LayoutParams(0, dp(32), 1));

        Button seq = actionButton("SEQUENCE", v -> showSequencePage());
        header.addView(seq, new LinearLayout.LayoutParams(dp(92), dp(32)));
        panel.addView(header);

        LinearLayout add = row();
        add.addView(trackTypeButton("+ SAMPLE", 0), weight());
        add.addView(trackTypeButton("+ SYNTH", 1), weight());
        add.addView(trackTypeButton("+ MIDI", 3), weight());
        add.addView(trackTypeButton("+ AUDIO", 4), weight());
        panel.addView(add, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        trackList = column();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(trackList, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        panel.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        refreshTrackGrid();
        return panel;
    }

    private Button trackTypeButton(String text, int kind) {
        Button b = actionButton(text, v -> {
            setBottomStatus(nativeSequenceAddTrack(kind));
            refreshTrackGrid();
            refreshTransportUi();
        });
        final String type = trackTypeLabelFromKind(kind);
        final int color = trackTypeColor(type);
        b.setTextColor(color);
        b.setBackground(strokeBackground(
                Color.rgb(29, 33, 37), color, 8));
        return b;
    }

    private void refreshTrackGrid() {
        if (trackList == null) return;

        trackList.removeAllViews();
        final int count = Math.min(32, nativeSequenceGetTrackCount());
        final int selected = nativeSequenceGetSelectedTrack();

        for (int i = 0; i < count; i++) {
            final int track = i;
            final String raw = nativeSequenceTrackStatus(track);
            final String type = trackTypeLabel(raw);
            final String nameText = trackName(raw);
            final boolean muted = nativeSequenceIsTrackMuted(track);
            final boolean soloed = nativeSequenceIsTrackSoloed(track);
            final boolean armed = track == selected
                    && nativeSequenceIsSelectedTrackArmed();

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(4), dp(6), dp(4));

            final int typeColor = trackTypeColor(type);
            final int fill = selected == track
                    ? Color.rgb(32, 52, 60)
                    : SURFACE_2;
            final int stroke = selected == track ? ACCENT : LINE;
            row.setBackground(strokeBackground(fill, stroke, 7));
            row.setOnClickListener(v -> {
                setBottomStatus(nativeSequenceSelectTrack(track));
                refreshTrackGrid();
                refreshTransportUi();
            });

            TextView number = label(String.format(Locale.ROOT, "%02d", i + 1),
                    12, typeColor);
            number.setGravity(Gravity.CENTER);
            number.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(number, new LinearLayout.LayoutParams(dp(36), dp(48)));

            TextView kind = label(type, 8, typeColor);
            kind.setTypeface(Typeface.DEFAULT_BOLD);
            kind.setGravity(Gravity.CENTER);
            kind.setBackground(strokeBackground(
                    Color.rgb(23, 26, 29), typeColor, 6));
            row.addView(kind, new LinearLayout.LayoutParams(dp(68), dp(32)));

            LinearLayout details = column();
            TextView name = label(nameText, 12, TEXT);
            name.setTypeface(Typeface.DEFAULT_BOLD);
            details.addView(name);

            String meta = raw.contains("events=")
                    ? raw.substring(raw.indexOf("events=")).replace("|", "•").trim()
                    : "Pattern";
            TextView metaView = label(meta, 8, MUTED);
            details.addView(metaView);
            row.addView(details, new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

            Button mute = actionButton(muted ? "M*" : "M", v -> {
                setBottomStatus(nativeSequenceSetTrackMuted(
                        track, !nativeSequenceIsTrackMuted(track)));
                refreshTrackGrid();
            });
            mute.setTextColor(muted ? DANGER : TEXT);
            row.addView(mute, new LinearLayout.LayoutParams(dp(44), dp(46)));

            Button solo = actionButton(soloed ? "S*" : "S", v -> {
                setBottomStatus(nativeSequenceSetTrackSoloed(
                        track, !nativeSequenceIsTrackSoloed(track)));
                refreshTrackGrid();
                refreshPadSelectionVisuals();
            });
            solo.setTextColor(soloed ? TYPE_MIDI : TEXT);
            row.addView(solo, new LinearLayout.LayoutParams(dp(44), dp(46)));

            Button arm = actionButton(armed ? "ARM*" : "ARM", v -> {
                if (track != nativeSequenceGetSelectedTrack()) {
                    setBottomStatus(nativeSequenceSelectTrack(track));
                }
                final boolean next = !nativeSequenceIsSelectedTrackArmed();
                setBottomStatus(nativeSequenceSetSelectedTrackArmed(next));
                refreshTrackGrid();
                refreshTransportUi();
            });
            arm.setTextColor(armed ? DANGER : TEXT);
            row.addView(arm, new LinearLayout.LayoutParams(dp(60), dp(46)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
            lp.bottomMargin = dp(4);
            trackList.addView(row, lp);
        }

        if (count == 0) {
            TextView empty = label("No tracks in this sequence.", 12, MUTED);
            empty.setGravity(Gravity.CENTER);
            trackList.addView(empty, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));
        }
    }

    private String trackName(String rawStatus) {
        if (rawStatus == null) return "Track";
        String left = rawStatus;
        final int separator = left.indexOf('|');
        if (separator >= 0) left = left.substring(0, separator).trim();
        final String[] parts = left.split("\\s+", 2);
        return parts.length > 1 ? parts[1].trim() : left;
    }

    private String trackTypeLabel(String rawStatus) {
        if (rawStatus == null) return "TRACK";
        final String left = rawStatus.split("\\s+", 2)[0].trim().toUpperCase(Locale.ROOT);
        if ("DRUM".equals(left) || "SAMPLE".equals(left)) return "SAMPLE";
        if ("KEYGROUP".equals(left) || "PLUGIN".equals(left)) return "SYNTH";
        if ("MIDI".equals(left)) return "MIDI";
        if ("AUDIO".equals(left)) return "AUDIO";
        return "TRACK";
    }

    private String trackTypeLabelFromKind(int kind) {
        switch (kind) {
            case 0: return "SAMPLE";
            case 1:
            case 2: return "SYNTH";
            case 3: return "MIDI";
            case 4: return "AUDIO";
            default: return "TRACK";
        }
    }

    private int trackTypeColor(String type) {
        switch (type) {
            case "SAMPLE": return TYPE_SAMPLE;
            case "SYNTH": return TYPE_SYNTH;
            case "MIDI": return TYPE_MIDI;
            case "AUDIO": return TYPE_AUDIO;
            default: return MUTED;
        }
    }


    private void changeSequenceTempo(double delta) {
        final double tempo = Math.max(20.0,
                Math.min(300.0, nativeSequenceGetTempo() + delta));
        setBottomStatus(nativeSequenceSetTempo(tempo));
        refreshTransportUi();
    }

    private void changeSequenceBars(int delta) {
        final int bars = Math.max(1,
                Math.min(128, nativeSequenceGetBars() + delta));
        setBottomStatus(nativeSequenceSetBars(bars));
        refreshTransportUi();
    }

    private static final int[][] SEQUENCE_TIME_SIGNATURES = {
            {4, 4}, {3, 4}, {5, 4}, {6, 8}, {7, 8}, {12, 8}
    };

    private void cycleTimeSignature(int direction) {
        final int numerator = nativeSequenceGetNumerator();
        final int denominator = nativeSequenceGetDenominator();
        int current = 0;
        for (int i = 0; i < SEQUENCE_TIME_SIGNATURES.length; i++) {
            if (SEQUENCE_TIME_SIGNATURES[i][0] == numerator
                    && SEQUENCE_TIME_SIGNATURES[i][1] == denominator) {
                current = i;
                break;
            }
        }
        int next = (current + direction) % SEQUENCE_TIME_SIGNATURES.length;
        if (next < 0) next += SEQUENCE_TIME_SIGNATURES.length;
        setBottomStatus(nativeSequenceSetTimeSignature(
                SEQUENCE_TIME_SIGNATURES[next][0],
                SEQUENCE_TIME_SIGNATURES[next][1]));
        refreshTransportUi();
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

    private void showTrackPage() {
        currentPage = "TRACKS";
        pageTitle.setText("TRACKS");
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout title = row();
        title.addView(sectionLabelView("TRACKS • INDEPENDENT OF PADS",
                new LinearLayout.LayoutParams(0, dp(38), 1)));
        title.addView(actionButton("SEQ", v -> showSequencePage()),
                new LinearLayout.LayoutParams(dp(60), dp(36)));
        title.addView(actionButton("MAIN", v -> showMainPage()),
                new LinearLayout.LayoutParams(dp(68), dp(36)));
        page.addView(title);

        page.addView(buildTrackControlPanel(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        content.addView(page);
        refreshTrackGrid();
        refreshTransportUi();
    }

    private void showSequencePage() {
        currentPage = "SEQ";
        pageTitle.setText("SEQUENCER");
        content.removeAllViews();

        LinearLayout page = page();

        LinearLayout header = row();
        header.addView(sectionLabelView("SEQUENCE",
                new LinearLayout.LayoutParams(0, dp(38), 1)));
        header.addView(actionButton("RESET", v -> {
            setBottomStatus(nativeSequenceReset());
            refreshTransportUi();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        header.addView(actionButton("NEXT", v -> {
            setBottomStatus(nativeSequenceNext());
            refreshTransportUi();
            refreshTrackGrid();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        header.addView(actionButton("NEW", v -> {
            setBottomStatus(nativeSequenceAddSequence());
            refreshTransportUi();
            refreshTrackGrid();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        header.addView(actionButton("CHAIN", v -> {
            final String result = nativeSequenceSetChainEnabled(
                    !nativeSequenceIsChainEnabled());
            setBottomStatus(result);
            refreshTransportUi();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        page.addView(header);

        LinearLayout controls = row();
        controls.addView(sequenceValue("TEMPO", String.format(
                Locale.ROOT, "%.1f", nativeSequenceGetTempo()),
                v -> changeSequenceTempo(-1), v -> changeSequenceTempo(1)),
                new LinearLayout.LayoutParams(0, dp(66), 1));
        controls.addView(sequenceValue("BARS",
                String.valueOf(nativeSequenceGetBars()),
                v -> changeSequenceBars(-1), v -> changeSequenceBars(1)),
                new LinearLayout.LayoutParams(0, dp(66), 0.8f));
        controls.addView(sequenceValue("TIME",
                nativeSequenceGetNumerator() + "/" + nativeSequenceGetDenominator(),
                v -> cycleTimeSignature(-1), v -> cycleTimeSignature(1)),
                new LinearLayout.LayoutParams(0, dp(66), 0.9f));

        TextView mode = label(
                nativeSequenceGetRecordMode() == 0
                        ? "REPLACE"
                        : "OVERDUB",
                10, nativeSequenceGetRecordMode() == 0 ? TEXT : DANGER);
        mode.setGravity(Gravity.CENTER);
        mode.setTypeface(Typeface.DEFAULT_BOLD);
        mode.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        mode.setOnClickListener(v -> {
            final int next = nativeSequenceGetRecordMode() == 0 ? 1 : 0;
            setBottomStatus(nativeSequenceSetRecordMode(next));
            showSequencePage();
        });
        controls.addView(mode, new LinearLayout.LayoutParams(
                dp(92), dp(44)));

        Button arm = actionButton(
                nativeSequenceIsSelectedTrackArmed() ? "ARM*" : "ARM",
                v -> {
                    final boolean next = !nativeSequenceIsSelectedTrackArmed();
                    setBottomStatus(nativeSequenceSetSelectedTrackArmed(next));
                    refreshTrackGrid();
                    refreshTransportUi();
                });
        arm.setTextColor(nativeSequenceIsSelectedTrackArmed() ? DANGER : TEXT);
        controls.addView(arm, new LinearLayout.LayoutParams(dp(66), dp(58)));

        page.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));

        page.addView(buildTrackControlPanel(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton("GRID", v -> setBottomStatus(
                "Grid editor context: selected Track / Pattern")), weight());
        footer.addView(actionButton("STEP", v -> setBottomStatus(
                "Step editor context: selected Track / Pattern")), weight());
        footer.addView(actionButton("REC", v -> {
            setBottomStatus(nativeSequenceRecordToggle());
            refreshTransportUi();
            refreshTrackGrid();
        }), weight());
        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        content.addView(page);
        refreshTrackGrid();
        refreshTransportUi();
    }


    private View sequenceValue(
            String title,
            String value,
            View.OnClickListener minus,
            View.OnClickListener plus) {
        LinearLayout box = column();
        TextView caption = label(title, 9, MUTED);
        caption.setGravity(Gravity.CENTER);
        box.addView(caption, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        LinearLayout line = row();
        line.addView(actionButton("−", minus), new LinearLayout.LayoutParams(dp(38), dp(44)));
        TextView v = label(value, 12, TEXT);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        line.addView(v, new LinearLayout.LayoutParams(0, dp(44), 1));
        line.addView(actionButton("+", plus), new LinearLayout.LayoutParams(dp(38), dp(44)));
        box.addView(line);
        return box;
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
                {"SEQUENCER", "SEQ"}, {"TRACKS", "TRACKS"}, {"MIXER", "MIX"},
                {"AUDIO SETTINGS", "AUDIO"}, {"GRID", "GRID"}, {"STEP", "STEP"}, {"TRACK EDIT", "TRACK"},
                {"PAD MIX", "PAD"}, {"Q-LINK", "QLINK"}, {"PROJECT", "PROJECT"}
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
                    case "TRACKS": showTrackPage(); break;
                    case "MIX": showMixPage(); break;
                    case "AUDIO": showAudioSettingsPage(); break;
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

    private void showAudioSettingsPage() {
        currentPage = "AUDIO";
        pageTitle.setText("AUDIO");
        content.removeAllViews();

        LinearLayout page = page();

        TextView route = label(
                "OUTPUT ROUTE\nAndroid system / current native audio route",
                12, TEXT);
        route.setTypeface(Typeface.DEFAULT_BOLD);
        route.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        route.setPadding(dp(12), dp(10), dp(12), dp(10));
        page.addView(route, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66)));

        TextView engine = label(
                "ENGINE\n" + nativeEngineInfo(),
                11, MUTED);
        engine.setBackground(strokeBackground(SURFACE, LINE, 8));
        engine.setPadding(dp(12), dp(10), dp(12), dp(10));
        page.addView(engine, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(74)));

        TextView status = label(
                "STATUS\n" + nativeAudioStatus(),
                11, MUTED);
        status.setBackground(strokeBackground(SURFACE, LINE, 8));
        status.setPadding(dp(12), dp(10), dp(12), dp(10));
        page.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        LinearLayout actions = row();
        actions.addView(actionButton("RESTART AUDIO", v -> {
            final String stop = nativeAudioStop();
            final String start = nativeAudioStart();
            setAudioStateFromResult(start);
            setBottomStatus(stop + " | " + start);
            showAudioSettingsPage();
        }), weight());
        actions.addView(actionButton("AUDITION SELECTED", v ->
                selectAndTriggerPad(selectedPad, 112)), weight());
        actions.addView(actionButton("MAIN", v -> showMainPage()), weight());
        page.addView(actions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        content.addView(page);
        updateModeRailSelection();
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
            final String startResult = nativeAudioStart();
            if (startResult == null
                    || startResult.toLowerCase(Locale.ROOT).contains("failed")) {
                setBottomStatus(startResult == null
                        ? "Audio output unavailable"
                        : startResult);
                return;
            }
            nativeAudioTriggerPad(pad, velocity);
            final String captureResult = nativeSequenceCapturePadHit(pad, velocity);
            if (captureResult != null && captureResult.length() > 0
                    && !captureResult.equals("capture=idle")) {
                setBottomStatus(captureResult);
            }
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

            final boolean selected = i == selectedPad;
            padButtons[i].setTextColor(TEXT);
            padButtons[i].setBackground(strokeBackground(
                    selected ? Color.rgb(32, 52, 60) : SURFACE_2,
                    selected ? ACCENT : LINE, 8));
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

    private void startTransportUiUpdates() {
        if (transportUiUpdater == null) {
            transportUiUpdater = new Runnable() {
                @Override public void run() {
                    if (destroyed) return;

                    if (startupComplete && nativeSequenceIsPlaying()) {
                        nativeSequenceAdvance(80);
                    }

                    refreshTransportUi();
                    transportUiHandler.postDelayed(this, 80);
                }
            };
        }
        transportUiHandler.removeCallbacks(transportUiUpdater);
        transportUiHandler.post(transportUiUpdater);
    }

    private void refreshTransportUi() {
        if (sequenceProgressView == null) return;

        final int count = Math.max(1, nativeSequenceGetCount());
        final int index = Math.max(0, Math.min(count - 1, nativeSequenceGetIndex()));
        final int bars = Math.max(1, nativeSequenceGetBars());
        final int numerator = Math.max(1, nativeSequenceGetNumerator());
        final int denominator = Math.max(1, nativeSequenceGetDenominator());
        final double ticksPerBeat = 960.0 * 4.0 / denominator;
        final double ticksPerBar = ticksPerBeat * numerator;
        final double lengthTicks = Math.max(1.0, bars * ticksPerBar);
        final long position = Math.max(0L, nativeSequencePositionTicks());

        final float progress = (float) Math.max(
                0.0,
                Math.min(1.0, position / lengthTicks));
        sequenceProgressView.setBars(bars);
        sequenceProgressView.setProgress(progress);
        sequenceProgressView.setPlaying(nativeSequenceIsPlaying());

        sequenceCounterView.setText(String.format(
                Locale.ROOT, "SEQ %02d/%02d", index + 1, count));

        final int bar = (int) Math.floor(position / ticksPerBar) + 1;
        final double inBar = position % ticksPerBar;
        final int beat = (int) Math.floor(inBar / ticksPerBeat) + 1;
        final int tick = (int) Math.round(inBar % ticksPerBeat);
        sequencePositionView.setText(String.format(
                Locale.ROOT, "%03d.%d.%03d", bar, beat, tick));

        tempoView.setText(String.format(
                Locale.ROOT, "%.1f", nativeSequenceGetTempo()));

        final boolean overdub = nativeSequenceGetRecordMode() == 1;
        if (recordModeView != null) {
            recordModeView.setText(overdub ? "OVER" : "REPL");
            recordModeView.setTextColor(overdub ? DANGER : TEXT);
            recordModeView.setBackground(strokeBackground(
                    overdub ? Color.rgb(50, 29, 31) : SURFACE_2,
                    overdub ? DANGER : LINE, 7));
        }
        sequencePositionView.setTextColor(
                nativeSequenceIsPlaying() ? TEXT : MUTED);
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
        // Android providers report WAV under several MIME types.
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "audio/wav",
                "audio/x-wav",
                "audio/wave",
                "audio/vnd.wave",
                "application/octet-stream"
        });
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try {
            startActivityForResult(intent, REQUEST_OPEN_WAV);
        } catch (RuntimeException e) {
            Intent fallback = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            fallback.addCategory(Intent.CATEGORY_OPENABLE);
            fallback.setType("audio/*");
            fallback.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(fallback, REQUEST_OPEN_WAV);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_OPEN_WAV || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();
        if (uri == null && data.getClipData() != null
                && data.getClipData().getItemCount() > 0) {
            uri = data.getClipData().getItemAt(0).getUri();
        }
        if (uri == null) {
            setBottomStatus("Sample load failed: no file selected");
            return;
        }

        try {
            if ((data.getFlags() & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) != 0) {
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Immediate access remains valid through the transient document grant.
                }
            }

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
        b.setTextSize(9);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        return b;
    }

    private Button transportButton(String text, int fill, int stroke, int textColor) {
        Button b = button(text);
        b.setTextSize(10);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(textColor);
        b.setBackground(strokeBackground(fill, stroke, 8));
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
        p.setPadding(dp(10), dp(8), dp(10), 0);
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
                "MAIN", "TRACKS", "PLAY", "STOP", "REC", "01", "16", "LOAD"
        };

        for (String text : expected) {
            View view = findViewWithExactText(getWindow().getDecorView(), text);
            if (view == null || view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e(TAG, "UI_HIERARCHY_FAILED: " + text);
                return;
            }
        }

        View tracksMode = findViewWithContentDescription(
                getWindow().getDecorView(), "TRACKS mode");
        if (tracksMode == null || !tracksMode.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: TRACKS mode");
            return;
        }

        View sampleMode = findViewWithContentDescription(
                getWindow().getDecorView(), "SAMPLE mode");
        if (sampleMode == null || !sampleMode.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: SAMPLE mode");
            return;
        }

        View pad1 = findViewWithExactText(getWindow().getDecorView(), "01");
        if (pad1 == null || !pad1.performClick() || selectedPad != 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: pad 01");
            return;
        }

        if (findViewWithExactText(getWindow().getDecorView(), "ENV") == null
                || findViewWithExactText(getWindow().getDecorView(), "FILTER") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Sample waveform editor") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sample waveform editor");
            return;
        }

        View recMode = findViewWithContentDescription(
                getWindow().getDecorView(), "REC mode");
        if (recMode == null || !recMode.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: REC mode");
            return;
        }

        View recordingWaveformView = findViewWithContentDescription(
                getWindow().getDecorView(), "Recording waveform monitor");
        if (recordingWaveformView == null
                || recordingWaveformView.getHeight() < dp(144)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: recording waveform height");
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
                            ? "M·RDY" : "MIDI");
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
            midiState.setText(connected ? "M·ON" : "MIDI");
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

    private static final class SequencerProgressView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float progress;
        private boolean playing;
        private int bars = 4;

        SequencerProgressView(android.content.Context context) {
            super(context);
        }

        void setBars(int value) {
            final int next = Math.max(1, Math.min(128, value));
            if (bars != next) {
                bars = next;
                invalidate();
            }
        }

        void setProgress(float value) {
            final float next = Math.max(0f, Math.min(1f, value));
            if (Math.abs(next - progress) > 0.0005f) {
                progress = next;
                invalidate();
            }
        }

        void setPlaying(boolean value) {
            if (playing != value) {
                playing = value;
                invalidate();
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final float width = getWidth();
            final float height = getHeight();

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(70, 24, 29));
            canvas.drawRect(0, 0, width, height, paint);

            paint.setColor(Color.rgb(109, 38, 44));
            final int markerStep = bars <= 32 ? 1 : 4;
            for (int i = markerStep; i < bars; i += markerStep) {
                final float x = width * (i / (float) bars);
                canvas.drawRect(x, 0, x + dpForView(1), height, paint);
            }

            paint.setColor(DANGER);
            canvas.drawRect(0, height * 0.18f, width * progress, height * 0.82f, paint);

            final float x = Math.max(0f, Math.min(width, width * progress));
            paint.setColor(playing ? Color.WHITE : DANGER);
            canvas.drawRect(
                    Math.max(0f, x - dpForView(1)),
                    0,
                    Math.min(width, x + dpForView(1)),
                    height,
                    paint);
        }

        private float dpForView(int value) {
            return Math.max(1f, value * getResources().getDisplayMetrics().density);
        }
    }


    @Override
    protected void onDestroy() {
        destroyed = true;
        if (recordingWaveformUpdater != null) {
            waveformUiHandler.removeCallbacks(recordingWaveformUpdater);
        }
        if (transportUiUpdater != null) {
            transportUiHandler.removeCallbacks(transportUiUpdater);
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
