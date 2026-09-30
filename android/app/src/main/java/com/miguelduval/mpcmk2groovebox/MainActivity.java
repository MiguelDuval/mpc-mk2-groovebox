package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback;
    private Spinner outputDeviceSpinner;
    private Spinner inputDeviceSpinner;
    private Spinner sampleRateSpinner;
    private Spinner bufferSizeSpinner;
    private Spinner sharingModeSpinner;
    private Spinner performanceModeSpinner;
    private TextView audioRoutingDiagnostics;
    private final List<AudioDeviceInfo> outputDevices = new ArrayList<>();
    private final List<AudioDeviceInfo> inputDevices = new ArrayList<>();
    private boolean audioSettingsBinding;
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
    private SequenceTimelineView sequenceTimeline;
    private SequenceOverviewView sequenceOverviewView;
    private SequenceGridView sequenceGridView;
    private SequenceLauncherView sequenceLauncherView;
    private final Button[] sequenceStepButtons = new Button[16];
    private TextView sequenceStepEventInfo;
    private TextView sequenceTransportView;
    private int sequenceGridPage = 0;
    private int sequenceStepPage = 0;
    private int launcherBank = 0;
    private String lastLauncherLedSignature = "";
    private static final int SEQUENCE_GRID_PAGE_STEPS = 16;
    private TextView sequenceStatusView;
    private TextView sequenceTempoView;
    private TextView sequenceBarsView;
    private TextView sequenceTimeSignatureView;
    private TextView sequenceLoopView;
    private TextView sequenceQuantizeView;
    private TextView sequenceSwingView;
    private TextView sequenceTimingCorrectView;
    private TextView sequenceRecordModeView;
    private TextView sequenceTrackInfoView;
    private final Handler sequenceUiHandler = new Handler(Looper.getMainLooper());
    private Runnable sequenceUiUpdater;
    private int selectedPad = 0;
    private int selectedLayer = 0;
    private int hardwareFocus = 0;
    private int hardwarePadBank = 0;
    private int lastPlayLedState = -1;
    private int lastRecordLedState = -1;
    private int lastOverdubLedState = -1;
    private int lastNoteRepeatLedState = -1;
    private int lastLevelLedState = -1;
    private int lastSixteenLevelLedState = -1;
    private int lastMuteLedState = -1;
    private long lastHardwareTapNanos = 0L;
    private final long[] hardwareTapIntervalsNanos = new long[4];
    private int hardwareTapIntervalCount = 0;
    private int selectedSequenceStep = -1;
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
    private static native String nativeAudioConfigureOutput(
            int deviceId, int sampleRate, int bufferSizeFrames,
            boolean exclusive, boolean lowLatency);
    private static native String nativeAudioConfigureInputDevice(int deviceId);
    private static native String nativeAudioTestOutput();
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
    private static native int nativeSequenceGetIndex();
    private static native int nativeSequenceGetCount();
    private static native int nativeSequenceGetQueuedIndex();
    private static native void nativeSequenceSetLauncherContext(
            boolean enabled, int bank);
    private static native String nativeSequenceLaunchPad(
            int bank, int padIndex);
    private static native String nativeSequenceSelect(int sequenceIndex);
    private static native String nativeSequencePrevious();
    private static native String nativeSequenceNext();
    private static native String nativeSequenceAddSequence();
    private static native double nativeSequenceGetTempo();
    private static native String nativeSequenceSetTempo(double tempo);
    private static native int nativeSequenceGetBars();
    private static native String nativeSequenceSetBars(int bars);
    private static native int nativeSequenceGetNumerator();
    private static native int nativeSequenceGetDenominator();
    private static native String nativeSequenceSetTimeSignature(
            int numerator, int denominator);
    private static native boolean nativeSequenceIsLoopEnabled();
    private static native String nativeSequenceSetLoopEnabled(boolean enabled);
    private static native int nativeSequenceGetLoopStartBar();
    private static native int nativeSequenceGetLoopEndBar();
    private static native String nativeSequenceSetLoopBars(int startBar, int endBar);
    private static native int nativeSequenceGetQuantizeGrid();
    private static native String nativeSequenceSetQuantizeGrid(int ticks);
    private static native int nativeSequenceGetSwing();
    private static native String nativeSequenceSetSwing(int percent);
    private static native boolean nativeSequenceIsTimingCorrectEnabled();
    private static native String nativeSequenceSetTimingCorrectEnabled(boolean enabled);
    private static native boolean nativeSequenceIsGridEditable();
    private static native int[] nativeSequenceGetGridVelocities(
            int firstStep, int gridTicks);
    private static native String nativeSequenceToggleGridStep(
            int padIndex, int stepIndex, int gridTicks);
    private static native int[] nativeSequenceGetStepParameters(
            int padIndex, int stepIndex, int gridTicks);
    private static native String nativeSequenceSetStepVelocity(
            int padIndex, int stepIndex, int gridTicks, int velocity);
    private static native String nativeSequenceSetStepProbability(
            int padIndex, int stepIndex, int gridTicks, int probability);
    private static native String nativeSequenceSetStepRatchet(
            int padIndex, int stepIndex, int gridTicks, int ratchet);
    private static native String nativeSequenceSetStepNudge(
            int padIndex, int stepIndex, int gridTicks, int nudgeTicks);
    private static native int nativeSequenceGetRecordMode();
    private static native String nativeSequenceSetRecordMode(int mode);
    private static native int nativeSequenceDrainRecordEvents();
    private static native int nativeSequenceGetTrackCount();
    private static native int nativeSequenceGetSelectedTrack();
    private static native String nativeSequenceSelectTrack(int trackIndex);
    private static native String nativeSequenceAddTrack(int kind);
    private static native String nativeSequenceTrackStatus(int trackIndex);
    private static native boolean nativeSequenceIsSelectedTrackArmed();
    private static native String nativeSequenceSetSelectedTrackArmed(boolean armed);
    private static native String nativeSequenceTogglePadMute(int padIndex);
    private static native String nativeSequenceToggleTrackMute(int trackIndex);
    private static native String nativeSequenceStart();
    private static native String nativeSequenceStop();
    private static native String nativeSequenceReset();
    private static native int nativeSequenceAdvance(long milliseconds);
    private static native String nativeSequenceMovePlayheadTicks(long deltaTicks);
    private static native long nativeSequencePositionTicks();
    private static native boolean nativeSequenceIsPlaying();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String smokeMode = getIntent().getStringExtra(SMOKE_MODE_EXTRA);
        uiOnlySmokeMode = "ui-only".equals(smokeMode);
        uiAuditSmokeMode = "ui-audit".equals(smokeMode);

        applyFullscreenWindowPolicy();

        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            audioDeviceCallback = new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                    refreshAudioDevicesFromSystem();
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    refreshAudioDevicesFromSystem();
                }
            };
            audioManager.registerAudioDeviceCallback(
                    audioDeviceCallback,
                    new Handler(Looper.getMainLooper()));
        }

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
                    startSequenceUiUpdater();
                    refreshSequenceOverview();

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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        sequenceOverviewView = new SequenceOverviewView(this);
        sequenceOverviewView.setContentDescription("Sequence playback overview");
        root.addView(sequenceOverviewView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));

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

        sequenceTransportView = label("S01 001.1.000 120.0", 10, TEXT);
        sequenceTransportView.setGravity(Gravity.CENTER);
        sequenceTransportView.setTypeface(Typeface.DEFAULT_BOLD);
        sequenceTransportView.setContentDescription("Sequence position and tempo");
        bar.addView(sequenceTransportView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.75f));

        audioState = statusChip("AUDIO OFF", MUTED);
        bar.addView(audioState, new LinearLayout.LayoutParams(dp(96), dp(38)));

        midiState = statusChip("MIDI —", MUTED);
        bar.addView(midiState, new LinearLayout.LayoutParams(dp(96), dp(38)));

        Button play = topButton("PLAY");
        play.setOnClickListener(v -> {
            final String result = nativeSequenceStart();
            setAudioStateFromResult(result);
            setBottomStatus(result);
        });
        bar.addView(play, new LinearLayout.LayoutParams(dp(72), dp(38)));

        Button stop = topButton("STOP");
        stop.setOnClickListener(v -> {
            final String sequenceResult = nativeSequenceStop();
            final String audioResult = nativeAudioStop();
            setAudioStateFromResult(audioResult);
            setBottomStatus(sequenceResult + " | " + audioResult);
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
            params.topMargin = i == 0 ? 0 : dp(3);
            params.bottomMargin = i == pages.length - 1 ? 0 : 0;
            rail.addView(button, params);
        }
        return rail;
    }

    private Button modeButton(String text, String page) {
        Button b = button(text);
        b.setTextSize(12);
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
        nativeSequenceSetLauncherContext(false, 0);
        clearSequenceLauncherLeds();
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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

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
                // MPC Studio MkII physical numbering is bottom-left -> top-right:
                // bottom row = Pads 1-4, then 5-8, 9-12, top row = 13-16.
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
        layerRow.addView(layerDown, touchButtonWeight());
        layerRow.addView(layerUp, touchButtonWeight());
        inspector.addView(layerRow, touchRowParams());

        inspector.addView(sectionLabel("QUICK TONE"));

        LinearLayout tone1 = row();
        tone1.addView(actionButton("TUNE −1", v -> changePadTuning(-1)), touchButtonWeight());
        tone1.addView(actionButton("TUNE +1", v -> changePadTuning(1)), touchButtonWeight());
        tone1.addView(actionButton("LEVEL −10", v -> changePadLevel(-0.10f)), touchButtonWeight());
        tone1.addView(actionButton("LEVEL +10", v -> changePadLevel(0.10f)), touchButtonWeight());
        inspector.addView(tone1, touchRowParams());

        LinearLayout tone2 = row();
        tone2.addView(actionButton("PAN L", v -> setPadPan(-1)), touchButtonWeight());
        tone2.addView(actionButton("PAN C", v -> setPadPan(0)), touchButtonWeight());
        tone2.addView(actionButton("PAN R", v -> setPadPan(1)), touchButtonWeight());
        tone2.addView(actionButton("EDIT", v -> showSamplePage()), touchButtonWeight());
        inspector.addView(tone2, touchRowParams());

        regionInfo = label("", 11, MUTED);
        inspector.addView(regionInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        LinearLayout detail = row();
        detail.addView(actionButton("SAMPLE", v -> showSamplePage()), touchButtonWeight());
        detail.addView(actionButton("SEQ", v -> showSequencePage()), touchButtonWeight());
        detail.addView(actionButton("MIX", v -> showMixPage()), touchButtonWeight());
        inspector.addView(detail, touchRowParams());

        return inspector;
    }

    private void showSamplePage() {
        nativeSequenceSetLauncherContext(false, 0);
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
        nativeSequenceSetLauncherContext(false, 0);
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

    private void showBrowserPage() {
        nativeSequenceSetLauncherContext(false, 0);
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
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        pageTitle.setText("SEQUENCER");
        content.removeAllViews();
        sequenceGridView = null;
        sequenceLauncherView = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();

        LinearLayout sequenceChooser = row();
        sequenceChooser.addView(sectionLabel("SEQUENCE / MAIN"),
                new LinearLayout.LayoutParams(0, dp(38), 1));
        sequenceChooser.addView(actionButton("PREV", v -> {
            setBottomStatus(nativeSequencePrevious());
            showSequencePage();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        sequenceChooser.addView(actionButton("NEXT", v -> {
            setBottomStatus(nativeSequenceNext());
            showSequencePage();
        }), new LinearLayout.LayoutParams(dp(70), dp(38)));
        sequenceChooser.addView(actionButton("NEW", v -> {
            setBottomStatus(nativeSequenceAddSequence());
            showSequencePage();
        }), new LinearLayout.LayoutParams(dp(66), dp(38)));
        page.addView(sequenceChooser);

        sequenceStatusView = label("", 13, TEXT);
        sequenceStatusView.setTypeface(Typeface.DEFAULT_BOLD);
        sequenceStatusView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        sequenceStatusView.setGravity(Gravity.CENTER_VERTICAL);
        sequenceStatusView.setPadding(dp(12), 0, dp(12), 0);
        page.addView(sequenceStatusView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        LinearLayout settings = row();
        settings.addView(sequenceValueControl(
                "BPM", "−1", "+1",
                v -> changeSequenceTempo(-1.0),
                v -> changeSequenceTempo(1.0),
                sequenceTempoViewHolder()), sequenceWeight());
        settings.addView(sequenceValueControl(
                "BARS", "−1", "+1",
                v -> changeSequenceBars(-1),
                v -> changeSequenceBars(1),
                sequenceBarsViewHolder()), sequenceWeight());
        settings.addView(sequenceValueControl(
                "TIME", "◀", "▶",
                v -> cycleTimeSignature(-1),
                v -> cycleTimeSignature(1),
                sequenceTimeSignatureViewHolder()), sequenceWeight());
        page.addView(settings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(76)));

        LinearLayout transport = row();
        transport.addView(actionButton("PLAY START", v -> {
            nativeSequenceReset();
            setBottomStatus(nativeSequenceStart());
            refreshSequenceControls();
        }), touchButtonWeight());
        transport.addView(actionButton("PLAY", v -> {
            setBottomStatus(nativeSequenceStart());
            refreshSequenceControls();
        }), touchButtonWeight());
        transport.addView(actionButton("STOP", v -> {
            setBottomStatus(nativeSequenceStop());
            refreshSequenceControls();
        }), touchButtonWeight());
        transport.addView(actionButton("RESET", v -> {
            setBottomStatus(nativeSequenceReset());
            refreshSequenceControls();
        }), touchButtonWeight());
        transport.addView(actionButton("REC ARM", v -> {
            final boolean arm = !nativeSequenceIsSelectedTrackArmed();
            setBottomStatus(nativeSequenceSetSelectedTrackArmed(arm));
            refreshSequenceControls();
        }), touchButtonWeight());
        sequenceRecordModeView = label("", 11, TEXT);
        sequenceRecordModeView.setGravity(Gravity.CENTER);
        sequenceRecordModeView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        transport.addView(sequenceRecordModeView, new LinearLayout.LayoutParams(
                0, dp(46), 1.0f));
        transport.addView(actionButton("REC MODE", v -> {
            final int mode = nativeSequenceGetRecordMode();
            setBottomStatus(nativeSequenceSetRecordMode(mode == 0 ? 1 : 0));
            refreshSequenceControls();
        }), touchButtonWeight());
        page.addView(transport, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        LinearLayout range = row();
        sequenceLoopView = label("", 12, TEXT);
        sequenceLoopView.setGravity(Gravity.CENTER);
        sequenceLoopView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        range.addView(sequenceLoopView, new LinearLayout.LayoutParams(
                0, dp(44), 1.2f));
        range.addView(actionButton("LOOP ON/OFF", v -> {
            setBottomStatus(nativeSequenceSetLoopEnabled(!nativeSequenceIsLoopEnabled()));
            refreshSequenceControls();
        }), new LinearLayout.LayoutParams(0, dp(44), 0.9f));
        range.addView(actionButton("FULL LOOP", v -> {
            final int bars = nativeSequenceGetBars();
            setBottomStatus(nativeSequenceSetLoopBars(1, bars));
            refreshSequenceControls();
        }), new LinearLayout.LayoutParams(0, dp(44), 0.9f));
        page.addView(range);

        sequenceTimeline = new SequenceTimelineView(this);
        sequenceTimeline.setContentDescription("Sequence bar and loop timeline");
        sequenceTimeline.setMinimumHeight(dp(86));
        sequenceTimeline.setOnLoopCommitListener((startBar, endBar) -> {
            setBottomStatus(nativeSequenceSetLoopBars(startBar, endBar));
            refreshSequenceControls();
        });
        page.addView(sequenceTimeline, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(92)));

        LinearLayout tools = row();
        sequenceQuantizeView = label("", 11, TEXT);
        sequenceQuantizeView.setGravity(Gravity.CENTER);
        sequenceQuantizeView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        tools.addView(sequenceQuantizeView, new LinearLayout.LayoutParams(0, dp(42), 1));
        tools.addView(actionButton("Q −", v -> adjustQuantizeGrid(-1)), touchButtonWeight());
        tools.addView(actionButton("Q +", v -> adjustQuantizeGrid(1)), touchButtonWeight());
        sequenceSwingView = label("", 11, TEXT);
        sequenceSwingView.setGravity(Gravity.CENTER);
        sequenceSwingView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        tools.addView(sequenceSwingView, new LinearLayout.LayoutParams(0, dp(42), 1));
        tools.addView(actionButton("SWING −5", v -> changeSequenceSwing(-5)), touchButtonWeight());
        tools.addView(actionButton("SWING +5", v -> changeSequenceSwing(5)), touchButtonWeight());
        page.addView(tools);

        LinearLayout timingTools = row();
        sequenceTimingCorrectView = label("", 11, TEXT);
        sequenceTimingCorrectView.setGravity(Gravity.CENTER);
        sequenceTimingCorrectView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        timingTools.addView(sequenceTimingCorrectView,
                new LinearLayout.LayoutParams(0, dp(42), 1.8f));
        timingTools.addView(actionButton("TC ON/OFF", v -> {
            final String result = nativeSequenceSetTimingCorrectEnabled(
                    !nativeSequenceIsTimingCorrectEnabled());
            setBottomStatus(result);
            refreshSequencePageTools();
        }), touchButtonWeight());
        timingTools.addView(actionButton("QUANTIZE", v -> {
            setBottomStatus(nativeSequenceQuantizeSelectedTrack());
            refreshSequencePageTools();
        }), touchButtonWeight());
        page.addView(timingTools);

        LinearLayout workspace = row();

        LinearLayout trackPanel = panel();
        trackPanel.addView(sectionLabel("TRACKS / SEQUENCE"));
        ScrollView trackScroll = new ScrollView(this);
        LinearLayout trackList = column();
        trackScroll.addView(trackList);
        trackPanel.addView(trackScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout addTracks = row();
        addTracks.addView(actionButton("+ DRUM", v -> addSequenceTrack(0)), touchButtonWeight());
        addTracks.addView(actionButton("+ KEY", v -> addSequenceTrack(1)), touchButtonWeight());
        addTracks.addView(actionButton("+ PLUG", v -> addSequenceTrack(2)), touchButtonWeight());
        addTracks.addView(actionButton("+ MIDI", v -> addSequenceTrack(3)), touchButtonWeight());
        addTracks.addView(actionButton("+ AUDIO", v -> addSequenceTrack(4)), touchButtonWeight());
        trackPanel.addView(addTracks, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        workspace.addView(trackPanel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.40f));

        LinearLayout detailPanel = panel();
        detailPanel.addView(sectionLabel("SELECTED TRACK"));
        sequenceTrackInfoView = label("", 13, TEXT);
        sequenceTrackInfoView.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        sequenceTrackInfoView.setPadding(dp(12), dp(10), dp(12), dp(10));
        sequenceTrackInfoView.setGravity(Gravity.CENTER_VERTICAL);
        detailPanel.addView(sequenceTrackInfoView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        TextView lane = label(
                "EVENT LANE  •  MIDI events belong to the selected track inside this sequence. "
                        + "Sampler, looper, audio and instrument tracks share the same sequence timeline.",
                12, MUTED);
        lane.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        lane.setPadding(dp(12), dp(10), dp(12), dp(10));
        detailPanel.addView(lane, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout editActions = row();
        editActions.addView(actionButton("GRID", v -> showSequenceGridPage()), touchButtonWeight());
        editActions.addView(actionButton("STEP", v -> showSequenceStepPage()), touchButtonWeight());
        editActions.addView(actionButton("LAUNCH", v -> showSequenceLauncherPage()), touchButtonWeight());
        detailPanel.addView(editActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        workspace.addView(detailPanel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.60f));
        page.addView(workspace, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        content.addView(page);
        refreshSequenceTrackList(trackList);
        refreshSequenceControls();

        refreshSequencePageTools();

    }

    private void showSequenceGridPage() {
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        pageTitle.setText("SEQ • GRID");
        content.removeAllViews();
        sequenceTimeline = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();

        LinearLayout header = row();
        header.addView(actionButton("BACK SEQ", v -> showSequencePage()),
                new LinearLayout.LayoutParams(dp(86), dp(38)));
        TextView title = label("GRID / STEP EDIT", 12, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));

        TextView pageInfo = label("", 11, TEXT);
        pageInfo.setTypeface(Typeface.DEFAULT_BOLD);
        pageInfo.setGravity(Gravity.CENTER);
        pageInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        header.addView(pageInfo, new LinearLayout.LayoutParams(dp(132), dp(38)));

        header.addView(actionButton("◀", v -> moveSequenceGridPage(-1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        header.addView(actionButton("▶", v -> moveSequenceGridPage(1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        page.addView(header);

        TextView context = label("", 11, MUTED);
        context.setPadding(dp(10), 0, dp(10), 0);
        context.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(context, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        sequenceGridView = new SequenceGridView(this);
        sequenceGridView.setContentDescription("Sequence 16 by 16 step grid");
        sequenceGridView.setListener((padIndex, pageStep) -> {
            final int gridTicks = nativeSequenceGetQuantizeGrid();
            final int absoluteStep =
                    sequenceGridPage * SEQUENCE_GRID_PAGE_STEPS + pageStep;
            final String result = nativeSequenceToggleGridStep(
                    padIndex, absoluteStep, gridTicks);
            setBottomStatus(result);
            refreshSequenceGrid();
            refreshSequenceControls();
        });
        page.addView(sequenceGridView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        TextView hint = label("TAP = ADD / REMOVE NOTE", 10, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        footer.addView(hint, new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView resolution = label("", 11, TEXT);
        resolution.setGravity(Gravity.CENTER);
        resolution.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        footer.addView(resolution, new LinearLayout.LayoutParams(dp(96), dp(38)));
        TextView editState = label("", 10, TEXT);
        editState.setGravity(Gravity.CENTER);
        editState.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        footer.addView(editState, new LinearLayout.LayoutParams(dp(150), dp(38)));
        page.addView(footer);

        content.addView(page);

        refreshSequenceGrid();
        final int gridTicks = nativeSequenceGetQuantizeGrid();
        resolution.setText(sequenceGridLabel(gridTicks));
        editState.setText(
                nativeSequenceIsGridEditable()
                        ? "EDIT READY"
                        : "DRUM / STOP REQUIRED");
        context.setText(
                "TRACK " + (nativeSequenceGetSelectedTrack() + 1)
                        + "  •  " + nativeSequenceTrackStatus(
                                nativeSequenceGetSelectedTrack()));
        refreshSequencePlayhead();
    }

    private void showSequenceLauncherPage() {
        nativeSequenceSetLauncherContext(false, 0);
        launcherBank = Math.max(0, nativeSequenceGetIndex() / 16);
        currentPage = "SEQ";
        pageTitle.setText("SEQ • LAUNCH");
        content.removeAllViews();
        sequenceTimeline = null;
        sequenceGridView = null;
        sequenceLauncherView = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();

        LinearLayout header = row();
        header.addView(actionButton("BACK SEQ", v -> showSequencePage()),
                new LinearLayout.LayoutParams(dp(86), dp(38)));
        TextView title = label("SEQUENCE LAUNCH", 12, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));
        TextView bankInfo = label("", 11, TEXT);
        bankInfo.setTypeface(Typeface.DEFAULT_BOLD);
        bankInfo.setGravity(Gravity.CENTER);
        bankInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        header.addView(bankInfo, new LinearLayout.LayoutParams(dp(118), dp(38)));
        header.addView(actionButton("◀", v -> moveSequenceLauncherBank(-1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        header.addView(actionButton("▶", v -> moveSequenceLauncherBank(1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        page.addView(header);

        TextView context = label(
                "LIVE MODE  •  TAP SEQUENCE = SELECT / QUEUE  •  ACTIVE = PLAYING  •  ORANGE = QUEUED",
                10, MUTED);
        context.setGravity(Gravity.CENTER_VERTICAL);
        context.setPadding(dp(10), 0, dp(10), 0);
        page.addView(context, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        sequenceLauncherView = new SequenceLauncherView(this);
        sequenceLauncherView.setListener(sequenceIndex -> {
            final int targetBank = sequenceIndex / 16;
            if (sequenceLauncherView != null
                    && targetBank != sequenceLauncherView.getBank()) {
                launcherBank = targetBank;
                nativeSequenceSetLauncherContext(true, launcherBank);
            }
            setBottomStatus(nativeSequenceLaunchPad(
                    targetBank, sequenceIndex % 16));
            refreshSequenceLauncher();
            refreshSequenceControls();
        });
        page.addView(sequenceLauncherView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton("CANCEL QUEUE", v -> {
            final int active = nativeSequenceGetIndex();
            setBottomStatus(nativeSequenceSelect(active));
            refreshSequenceLauncher();
            refreshSequenceControls();
        }), new LinearLayout.LayoutParams(dp(126), dp(44)));
        TextView hint = label(
                "A queued Sequence changes only at the current Sequence loop boundary.",
                10, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(dp(10), 0, dp(10), 0);
        footer.addView(hint, new LinearLayout.LayoutParams(0, dp(44), 1));
        page.addView(footer);

        content.addView(page);

        nativeSequenceSetLauncherContext(true, launcherBank);
        refreshSequenceLauncher();
        refreshSequenceControls();
    }

    private void showSequenceStepPage() {
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        pageTitle.setText("SEQ • STEP");
        content.removeAllViews();
        sequenceTimeline = null;
        sequenceGridView = null;
        sequenceLauncherView = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();

        LinearLayout header = row();
        header.addView(actionButton("BACK SEQ", v -> showSequencePage()),
                new LinearLayout.LayoutParams(dp(86), dp(38)));

        TextView title = label("STEP SEQUENCER", 12, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(38), 1));

        header.addView(actionButton("PAD −", v -> {
            selectedPad = Math.max(0, selectedPad - 1);
            refreshSequenceStepPage();
        }), new LinearLayout.LayoutParams(dp(64), dp(38)));
        TextView padInfo = label("", 11, TEXT);
        padInfo.setTypeface(Typeface.DEFAULT_BOLD);
        padInfo.setGravity(Gravity.CENTER);
        padInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        header.addView(padInfo, new LinearLayout.LayoutParams(dp(78), dp(38)));
        header.addView(actionButton("PAD +", v -> {
            selectedPad = Math.min(15, selectedPad + 1);
            refreshSequenceStepPage();
        }), new LinearLayout.LayoutParams(dp(64), dp(38)));
        header.addView(actionButton("◀", v -> moveSequenceStepPage(-1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        header.addView(actionButton("▶", v -> moveSequenceStepPage(1)),
                new LinearLayout.LayoutParams(dp(52), dp(38)));
        page.addView(header);

        TextView context = label("", 11, MUTED);
        context.setPadding(dp(10), 0, dp(10), 0);
        context.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(context, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        LinearLayout eventBar = row();
        sequenceStepEventInfo = label(
                "STEP —  •  VEL —  •  PROB —  •  RAT —  •  NUDGE —",
                11,
                TEXT);
        sequenceStepEventInfo.setGravity(Gravity.CENTER_VERTICAL);
        sequenceStepEventInfo.setPadding(dp(10), 0, dp(10), 0);
        sequenceStepEventInfo.setBackground(strokeBackground(SURFACE_2, LINE, 8));
        eventBar.addView(sequenceStepEventInfo, new LinearLayout.LayoutParams(
                0, dp(44), 1.25f));
        eventBar.addView(actionButton("VEL −10", v -> adjustSelectedStepVelocity(-10)),
                touchButtonWeight());
        eventBar.addView(actionButton("VEL +10", v -> adjustSelectedStepVelocity(10)),
                touchButtonWeight());
        eventBar.addView(actionButton("PROB −10", v -> adjustSelectedStepProbability(-10)),
                touchButtonWeight());
        eventBar.addView(actionButton("PROB +10", v -> adjustSelectedStepProbability(10)),
                touchButtonWeight());
        eventBar.addView(actionButton("RATCH −", v -> adjustSelectedStepRatchet(-1)),
                touchButtonWeight());
        eventBar.addView(actionButton("RATCH +", v -> adjustSelectedStepRatchet(1)),
                touchButtonWeight());
        page.addView(eventBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        LinearLayout nudgeBar = row();
        TextView nudgeLabel = label("MICRO TIMING", 10, MUTED);
        nudgeLabel.setGravity(Gravity.CENTER_VERTICAL);
        nudgeLabel.setPadding(dp(10), 0, dp(10), 0);
        nudgeBar.addView(nudgeLabel, new LinearLayout.LayoutParams(
                0, dp(48), 1.0f));
        nudgeBar.addView(actionButton(
                "NUDGE −10",
                v -> adjustSelectedStepNudge(-10)),
                touchButtonWeight());
        nudgeBar.addView(actionButton(
                "NUDGE +10",
                v -> adjustSelectedStepNudge(10)),
                touchButtonWeight());
        nudgeBar.addView(actionButton(
                "NUDGE 0",
                v -> setSelectedStepNudge(0)),
                touchButtonWeight());
        page.addView(nudgeBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));

        LinearLayout steps = row();
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            final int step = i;
            Button button = button(String.format(Locale.ROOT, "%02d", i + 1));
            button.setTextSize(12);
            button.setTypeface(Typeface.DEFAULT_BOLD);
            button.setContentDescription(
                    "Pad " + (selectedPad + 1) + " step " + (i + 1));
            button.setOnClickListener(v -> {
                final int gridTicks = Math.max(
                        1, nativeSequenceGetQuantizeGrid());
                final int absoluteStep =
                        sequenceStepPage * SEQUENCE_GRID_PAGE_STEPS + step;
                selectedSequenceStep = absoluteStep;
                final String result = nativeSequenceToggleGridStep(
                        selectedPad, absoluteStep, gridTicks);
                setBottomStatus(result);
                refreshSequenceStepPage();
            });
            button.setOnLongClickListener(v -> {
                selectedSequenceStep =
                        sequenceStepPage * SEQUENCE_GRID_PAGE_STEPS + step;
                setBottomStatus(
                        "Selected step " + (selectedSequenceStep + 1));
                refreshSequenceStepPage();
                return true;
            });
            sequenceStepButtons[i] = button;
            steps.addView(button, new LinearLayout.LayoutParams(
                    0, dp(88), 1));
        }
        page.addView(steps, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = label(
                "16 STEPS  •  LONG-PRESS = SELECT  •  TAP = ADD / REMOVE  •  NUDGE = ±10 TICKS  •  RESET = 0",
                10, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(dp(10), 0, dp(10), 0);
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        content.addView(page);

        refreshSequenceStepPage();
        padInfo.setText("PAD " + String.format(
                Locale.ROOT, "%02d", selectedPad + 1));
        context.setText(
                "STEP " + sequenceGridLabel(nativeSequenceGetQuantizeGrid())
                        + "  •  "
                        + nativeSequenceTrackStatus(
                                nativeSequenceGetSelectedTrack()));
        refreshSequencePlayhead();
    }

    private void refreshSequenceStepPage() {
        if (sequenceStepButtons[0] == null) return;

        final int count = sequenceGridPageCount();
        sequenceStepPage = Math.max(
                0, Math.min(count - 1, sequenceStepPage));

        final int gridTicks = Math.max(
                1, nativeSequenceGetQuantizeGrid());
        final int firstStep =
                sequenceStepPage * SEQUENCE_GRID_PAGE_STEPS;
        final int[] velocities =
                nativeSequenceGetGridVelocities(firstStep, gridTicks);
        final boolean editable = nativeSequenceIsGridEditable();
        final int selectedPageStep = selectedSequenceStep >= firstStep
                && selectedSequenceStep < firstStep + SEQUENCE_GRID_PAGE_STEPS
                ? selectedSequenceStep - firstStep
                : -1;
        final long positionTicks = nativeSequencePositionTicks();
        final long absolutePlayheadStep =
                Math.max(0L, positionTicks / gridTicks);
        final long playheadFirstStep = firstStep;
        final int playheadColumn =
                absolutePlayheadStep >= playheadFirstStep
                        && absolutePlayheadStep
                                < playheadFirstStep + SEQUENCE_GRID_PAGE_STEPS
                        ? (int) (absolutePlayheadStep - playheadFirstStep)
                        : -1;

        for (int i = 0; i < sequenceStepButtons.length; i++) {
            final Button button = sequenceStepButtons[i];
            final int velocity = velocities != null
                    && velocities.length > selectedPad * 16 + i
                    ? velocities[selectedPad * 16 + i]
                    : 0;
            final boolean active = velocity > 0;
            final boolean playhead = playheadColumn == i;
            final boolean selected = selectedPageStep == i;
            button.setText(String.format(Locale.ROOT, "%02d", i + 1));
            button.setContentDescription(
                    "Pad " + (selectedPad + 1) + " step " + (i + 1)
                            + (active ? " on" : " off"));
            button.setTextColor(active ? BG : TEXT);
            button.setBackground(strokeBackground(
                    active ? ACCENT : SURFACE_2,
                    selected ? ACCENT_2 : (playhead ? DANGER : (active ? ACCENT : LINE)),
                    8));
            button.setAlpha(editable ? 1.0f : 0.55f);
        }

        if (sequenceStepEventInfo != null) {
            final int[] parameters = selectedSequenceStep >= 0
                    ? nativeSequenceGetStepParameters(
                            selectedPad, selectedSequenceStep, gridTicks)
                    : null;
            final boolean hasEvent = parameters != null
                    && parameters.length >= 3
                    && parameters[0] > 0;
            sequenceStepEventInfo.setText(hasEvent
                    ? String.format(
                            Locale.ROOT,
                            "STEP %02d  •  VEL %d  •  PROB %d  •  RAT %dx  •  NUDGE %+d",
                            selectedSequenceStep + 1,
                            parameters[0],
                            parameters[1],
                            parameters[2],
                            parameters[3])
                    : selectedSequenceStep >= 0
                            ? String.format(
                                    Locale.ROOT,
                                    "STEP %02d  •  EMPTY",
                                    selectedSequenceStep + 1)
                            : "STEP —  •  VEL —  •  PROB —  •  RAT —  •  NUDGE —");
        }

        final View parent = sequenceStepButtons[0].getParent() instanceof View
                ? (View) sequenceStepButtons[0].getParent()
                : null;
        if (parent instanceof ViewGroup
                && ((ViewGroup) parent).getParent() instanceof ViewGroup) {
            final ViewGroup page = (ViewGroup)
                    ((ViewGroup) parent).getParent();
            if (page.getChildCount() > 0
                    && page.getChildAt(0) instanceof ViewGroup) {
                final ViewGroup header = (ViewGroup) page.getChildAt(0);
                if (header.getChildCount() > 4
                        && header.getChildAt(3) instanceof TextView) {
                    ((TextView) header.getChildAt(3)).setText(
                            "PAD " + String.format(
                                    Locale.ROOT, "%02d", selectedPad + 1));
                }
            }
        }
    }

    private int[] getSelectedStepParameters() {
        if (selectedSequenceStep < 0) return null;
        return nativeSequenceGetStepParameters(
                selectedPad,
                selectedSequenceStep,
                Math.max(1, nativeSequenceGetQuantizeGrid()));
    }

    private void adjustSelectedStepVelocity(int delta) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 3 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int value = Math.max(1, Math.min(127, parameters[0] + delta));
        setBottomStatus(nativeSequenceSetStepVelocity(
                selectedPad,
                selectedSequenceStep,
                Math.max(1, nativeSequenceGetQuantizeGrid()),
                value));
        refreshSequenceStepPage();
    }

    private void adjustSelectedStepProbability(int delta) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 3 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int value = Math.max(0, Math.min(127, parameters[1] + delta));
        setBottomStatus(nativeSequenceSetStepProbability(
                selectedPad,
                selectedSequenceStep,
                Math.max(1, nativeSequenceGetQuantizeGrid()),
                value));
        refreshSequenceStepPage();
    }

    private void adjustSelectedStepNudge(int delta) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 4 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int value = Math.max(-960, Math.min(960, parameters[3] + delta));
        setSelectedStepNudge(value);
    }

    private void setSelectedStepNudge(int value) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 4 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int bounded = Math.max(-960, Math.min(960, value));
        setBottomStatus(nativeSequenceSetStepNudge(
                selectedPad,
                selectedSequenceStep,
                Math.max(1, nativeSequenceGetQuantizeGrid()),
                bounded));
        refreshSequenceStepPage();
    }

    private void adjustSelectedStepRatchet(int delta) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 3 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int value = Math.max(1, Math.min(8, parameters[2] + delta));
        setBottomStatus(nativeSequenceSetStepRatchet(
                selectedPad,
                selectedSequenceStep,
                Math.max(1, nativeSequenceGetQuantizeGrid()),
                value));
        refreshSequenceStepPage();
    }

    private void moveSequenceStepPage(int delta) {
        final int count = sequenceGridPageCount();
        sequenceStepPage = Math.max(
                0, Math.min(count - 1, sequenceStepPage + delta));
        refreshSequenceStepPage();
    }

    private void syncSequenceLauncherLeds(
            int count,
            int activeIndex,
            int queuedIndex,
            int bank) {
        if (midiBridge == null) return;

        final String signature = count + ":" + activeIndex + ":"
                + queuedIndex + ":" + bank;
        if (signature.equals(lastLauncherLedSignature)) return;

        for (int pad = 0; pad < 16; pad++) {
            final int sequenceIndex = bank * 16 + pad;
            int red = 8;
            int green = 8;
            int blue = 8;
            if (sequenceIndex >= count) {
                red = 0;
                green = 0;
                blue = 0;
            } else if (sequenceIndex == activeIndex) {
                red = 0;
                green = 127;
                blue = 24;
            } else if (sequenceIndex == queuedIndex) {
                red = 127;
                green = 72;
                blue = 0;
            }

            final byte[] message =
                    MpcStudioMk2MidiMessages.padRgb(
                            pad, red, green, blue);
            if (message != null) {
                midiBridge.send(message);
            }
        }

        lastLauncherLedSignature = signature;
    }

    private void clearSequenceLauncherLeds() {
        if (midiBridge == null) {
            lastLauncherLedSignature = "";
            return;
        }

        if ("CLEARED".equals(lastLauncherLedSignature)) return;

        for (int pad = 0; pad < 16; pad++) {
            final byte[] message =
                    MpcStudioMk2MidiMessages.padRgb(
                            pad, 0, 0, 0);
            if (message != null) {
                midiBridge.send(message);
            }
        }
        lastLauncherLedSignature = "CLEARED";
    }

    private int sequenceLauncherBankCount() {
        final int count = nativeSequenceGetCount();
        return Math.max(1, (count + 15) / 16);
    }

    private void moveSequenceLauncherBank(int delta) {
        launcherBank = Math.max(
                0,
                Math.min(
                        sequenceLauncherBankCount() - 1,
                        launcherBank + delta));
        nativeSequenceSetLauncherContext(true, launcherBank);
        refreshSequenceLauncher();
    }

    private void refreshSequenceLauncher() {
        if (sequenceLauncherView == null) return;

        final int count = nativeSequenceGetCount();
        launcherBank = Math.max(
                0,
                Math.min(
                        Math.max(0, (count - 1) / 16),
                        launcherBank));
        final int activeIndex = nativeSequenceGetIndex();
        final int queuedIndex = nativeSequenceGetQueuedIndex();
        sequenceLauncherView.setState(
                count,
                activeIndex,
                queuedIndex,
                launcherBank);
        syncSequenceLauncherLeds(
                count, activeIndex, queuedIndex, launcherBank);

        final View parent = sequenceLauncherView.getParent() instanceof View
                ? (View) sequenceLauncherView.getParent()
                : null;
        if (parent instanceof ViewGroup) {
            final ViewGroup page = (ViewGroup) parent;
            if (page.getChildCount() > 0
                    && page.getChildAt(0) instanceof ViewGroup) {
                final ViewGroup header = (ViewGroup) page.getChildAt(0);
                if (header.getChildCount() > 2
                        && header.getChildAt(2) instanceof TextView) {
                    final TextView bankInfo =
                            (TextView) header.getChildAt(2);
                    bankInfo.setText(String.format(
                            Locale.ROOT,
                            "BANK %02d/%02d",
                            launcherBank + 1,
                            sequenceLauncherBankCount()));
                }
            }
        }
    }

    private int sequenceGridPageCount() {
        final long gridTicks = Math.max(
                1L, nativeSequenceGetQuantizeGrid());
        final long lengthTicks = Math.max(
                gridTicks,
                Math.round(getSequenceTicksPerBar())
                        * Math.max(1, nativeSequenceGetBars()));
        final long pageTicks =
                gridTicks * SEQUENCE_GRID_PAGE_STEPS;
        return (int) Math.max(
                1L,
                (lengthTicks + pageTicks - 1L) / pageTicks);
    }

    private void moveSequenceGridPage(int delta) {
        final int count = sequenceGridPageCount();
        sequenceGridPage = Math.max(
                0, Math.min(count - 1, sequenceGridPage + delta));
        refreshSequenceGrid();
    }

    private void refreshSequenceGrid() {
        if (sequenceGridView == null) return;

        final int count = sequenceGridPageCount();
        sequenceGridPage = Math.max(
                0, Math.min(count - 1, sequenceGridPage));

        final int gridTicks = Math.max(
                1, nativeSequenceGetQuantizeGrid());
        final int firstStep = sequenceGridPage * SEQUENCE_GRID_PAGE_STEPS;

        sequenceGridView.setEditable(nativeSequenceIsGridEditable());
        sequenceGridView.setState(
                nativeSequenceGetGridVelocities(firstStep, gridTicks),
                -1);
        refreshSequenceGridPageInfo();
    }

    private void refreshSequenceGridPageInfo() {
        if (sequenceGridView == null) return;
        // The page indicator is part of the grid screen header; redraw is kept
        // local so tapping a cell never reconstructs the whole page.
        final View root = sequenceGridView.getParent() instanceof View
                ? (View) sequenceGridView.getParent()
                : null;
        if (!(root instanceof LinearLayout)) return;

        final LinearLayout page = (LinearLayout) root;
        if (page.getChildCount() < 1
                || !(page.getChildAt(0) instanceof LinearLayout)) return;

        final LinearLayout header = (LinearLayout) page.getChildAt(0);
        if (header.getChildCount() < 3
                || !(header.getChildAt(2) instanceof TextView)) return;

        final TextView pageInfo = (TextView) header.getChildAt(2);
        pageInfo.setText(String.format(
                Locale.ROOT,
                "PAGE %02d/%02d",
                sequenceGridPage + 1,
                sequenceGridPageCount()));
    }

    private TextView sequenceTempoViewHolder() {
        sequenceTempoView = label("", 13, TEXT);
        sequenceTempoView.setGravity(Gravity.CENTER);
        sequenceTempoView.setTypeface(Typeface.DEFAULT_BOLD);
        return sequenceTempoView;
    }

    private TextView sequenceBarsViewHolder() {
        sequenceBarsView = label("", 13, TEXT);
        sequenceBarsView.setGravity(Gravity.CENTER);
        sequenceBarsView.setTypeface(Typeface.DEFAULT_BOLD);
        return sequenceBarsView;
    }

    private TextView sequenceTimeSignatureViewHolder() {
        sequenceTimeSignatureView = label("", 13, TEXT);
        sequenceTimeSignatureView.setGravity(Gravity.CENTER);
        sequenceTimeSignatureView.setTypeface(Typeface.DEFAULT_BOLD);
        return sequenceTimeSignatureView;
    }

    private View sequenceValueControl(
            String title,
            String minus,
            String plus,
            View.OnClickListener onMinus,
            View.OnClickListener onPlus,
            TextView valueView) {
        LinearLayout box = column();
        TextView caption = label(title, 9, MUTED);
        caption.setGravity(Gravity.CENTER);
        box.addView(caption, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));
        LinearLayout line = row();
        line.addView(actionButton(minus, onMinus), touchButtonWeight());
        line.addView(valueView, new LinearLayout.LayoutParams(0, dp(48), 1.4f));
        line.addView(actionButton(plus, onPlus), touchButtonWeight());
        box.addView(line, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return box;
    }

    private LinearLayout.LayoutParams sequenceWeight() {
        return new LinearLayout.LayoutParams(0, dp(76), 1);
    }

    private void refreshSequenceControls() {
        if (sequenceStatusView == null) return;
        final String sequenceStatus = nativeSequenceStatus();
        final int queuedIndex = nativeSequenceGetQueuedIndex();
        if (queuedIndex >= 0) {
            sequenceStatusView.setText(
                    sequenceStatus
                            + "  | QUEUED → SEQ "
                            + (queuedIndex + 1));
        } else {
            sequenceStatusView.setText(sequenceStatus);
        }

        sequenceTempoView.setText(String.format(
                Locale.ROOT, "%.1f", nativeSequenceGetTempo()));
        sequenceBarsView.setText(nativeSequenceGetBars() + " BARS");
        sequenceTimeSignatureView.setText(
                nativeSequenceGetNumerator() + "/" + nativeSequenceGetDenominator());

        final int loopStart = nativeSequenceGetLoopStartBar();
        final int loopEnd = nativeSequenceGetLoopEndBar();
        sequenceLoopView.setText(
                "LOOP " + (nativeSequenceIsLoopEnabled() ? "ON" : "OFF")
                        + "  •  " + loopStart + " → " + loopEnd);

        if (sequenceRecordModeView != null) {
            sequenceRecordModeView.setText(
                    nativeSequenceGetRecordMode() == 0
                            ? "REC REPLACE"
                            : "REC OVERDUB");
        }

        if (sequenceTimeline != null) {
            sequenceTimeline.setBarCount(nativeSequenceGetBars());
            sequenceTimeline.setLoop(loopStart, loopEnd);
            refreshSequencePlayhead();
        }

        syncHardwareTransportLeds();

        if (sequenceTrackInfoView != null) {
            sequenceTrackInfoView.setText(
                    "Track " + (nativeSequenceGetSelectedTrack() + 1)
                            + " / " + nativeSequenceGetTrackCount()
                            + "  •  "
                            + nativeSequenceTrackStatus(
                                    nativeSequenceGetSelectedTrack()));
        }
    }

    private double getSequenceTicksPerBeat() {
        return Math.max(
                1.0,
                960.0 * 4.0 / Math.max(1, nativeSequenceGetDenominator()));
    }

    private double getSequenceTicksPerBar() {
        return Math.max(
                1.0,
                getSequenceTicksPerBeat() * Math.max(1, nativeSequenceGetNumerator()));
    }

    private String formatSequencePosition(long positionTicks) {
        final long ticksPerBeat = Math.max(1L, Math.round(getSequenceTicksPerBeat()));
        final long ticksPerBar = Math.max(
                ticksPerBeat,
                Math.round(getSequenceTicksPerBar()));
        long normalized = Math.max(0L, positionTicks);
        final int bar = (int) (normalized / ticksPerBar) + 1;
        normalized %= ticksPerBar;
        final int beat = (int) (normalized / ticksPerBeat) + 1;
        final int tick = (int) (normalized % ticksPerBeat);
        return String.format(
                Locale.ROOT, "%03d.%d.%03d", bar, beat, tick);
    }

    private void refreshSequencePlayhead() {
        if (!"SEQ".equals(currentPage)) return;

        if (sequenceTimeline != null) {
            final double ticksPerBar = getSequenceTicksPerBar();
            final double bar = 1.0
                    + nativeSequencePositionTicks() / ticksPerBar;
            sequenceTimeline.setPlayheadBar((float) bar);
        }

        if (sequenceGridView != null) {
            final int gridTicks = Math.max(1, nativeSequenceGetQuantizeGrid());
            final long absoluteStep =
                    Math.max(0L, nativeSequencePositionTicks() / gridTicks);
            final long firstStep =
                    (long) sequenceGridPage * SEQUENCE_GRID_PAGE_STEPS;
            final int localStep =
                    absoluteStep >= firstStep
                            && absoluteStep < firstStep + SEQUENCE_GRID_PAGE_STEPS
                            ? (int) (absoluteStep - firstStep)
                            : -1;
            sequenceGridView.setState(
                    nativeSequenceGetGridVelocities(
                            (int) firstStep, gridTicks),
                    localStep);
        }
    }

    private void refreshSequenceTrackList(LinearLayout list) {
        list.removeAllViews();
        final int count = nativeSequenceGetTrackCount();
        final int selected = nativeSequenceGetSelectedTrack();
        for (int i = 0; i < count; i++) {
            final int track = i;
            Button b = actionButton(
                    String.format(
                            Locale.ROOT,
                            "%02d  %s",
                            i + 1,
                            nativeSequenceTrackStatus(i)),
                    v -> {
                        setBottomStatus(nativeSequenceSelectTrack(track));
                        refreshSequenceTrackList(list);
                        refreshSequenceControls();
                    });
            b.setTextSize(10);
            list.addView(b, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
            b.setBackground(strokeBackground(
                    i == selected ? Color.rgb(32, 52, 60) : SURFACE_2,
                    i == selected ? ACCENT : LINE,
                    8));
        }
        if (count == 0) {
            list.addView(label("No tracks", 12, MUTED));
        }
    }

    private void addSequenceTrack(int kind) {
        nativeSequenceStop();
        setBottomStatus(nativeSequenceAddTrack(kind));
        showSequencePage();
    }

    private void changeSequenceTempo(double delta) {
        final double tempo = Math.max(
                20.0, Math.min(300.0, nativeSequenceGetTempo() + delta));
        setBottomStatus(nativeSequenceSetTempo(tempo));
        refreshSequenceControls();
    }

    private void changeSequenceBars(int delta) {
        final int bars = Math.max(
                1, Math.min(128, nativeSequenceGetBars() + delta));
        setBottomStatus(nativeSequenceSetBars(bars));
        refreshSequenceControls();
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
        refreshSequenceControls();
    }

    private void adjustQuantizeGrid(int delta) {
        final int[] values = {60, 120, 240, 480, 960};
        int current = nativeSequenceGetQuantizeGrid();
        int index = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == current) {
                index = i;
                break;
            }
        }
        index = Math.max(0, Math.min(values.length - 1, index + delta));
        setBottomStatus(nativeSequenceSetQuantizeGrid(values[index]));
        refreshSequencePageTools();
    }

    private void refreshSequencePageTools() {
        refreshSequenceControls();
        if (sequenceQuantizeView != null) {
            sequenceQuantizeView.setText(
                    sequenceGridLabel(nativeSequenceGetQuantizeGrid()));
        }
        if (sequenceSwingView != null) {
            sequenceSwingView.setText(
                    "SWING " + nativeSequenceGetSwing() + "%");
        }
        if (sequenceTimingCorrectView != null) {
            sequenceTimingCorrectView.setText(
                    "TIMING CORRECT "
                            + (nativeSequenceIsTimingCorrectEnabled()
                                    ? "ON" : "OFF")
                            + "  •  RECORD GRID "
                            + sequenceGridLabel(nativeSequenceGetQuantizeGrid()));
        }
    }

    private void changeSequenceSwing(int delta) {
        final int value = Math.max(
                0, Math.min(100, nativeSequenceGetSwing() + delta));
        setBottomStatus(nativeSequenceSetSwing(value));
        refreshSequencePageTools();
    }

    private String sequenceGridLabel(int ticks) {
        switch (ticks) {
            case 60: return "Q 1/64";
            case 120: return "Q 1/32";
            case 240: return "Q 1/16";
            case 480: return "Q 1/8";
            case 960: return "Q 1/4";
            default: return "Q " + ticks;
        }
    }

    private void startSequenceUiUpdater() {
        if (sequenceUiUpdater != null || destroyed || uiOnlySmokeMode) {
            return;
        }

        sequenceUiUpdater = new Runnable() {
            @Override public void run() {
                if (destroyed) {
                    return;
                }

                if (startupComplete && nativeSequenceIsPlaying()) {
                    nativeSequenceAdvance(80);
                    nativeSequenceDrainRecordEvents();
                }

                if (startupComplete) {
                    refreshSequenceOverview();
                    if ("SEQ".equals(currentPage)) {
                        refreshSequencePlayhead();
                        refreshSequenceControls();
                        refreshSequenceLauncher();
                    }
                }

                sequenceUiHandler.postDelayed(this, 80);
            }
        };
        sequenceUiHandler.post(sequenceUiUpdater);
    }

    private void refreshSequenceOverview() {
        if (sequenceOverviewView == null || !startupComplete) {
            return;
        }

        final int bars = nativeSequenceGetBars();
        final int loopStart = nativeSequenceGetLoopStartBar();
        final int loopEnd = nativeSequenceGetLoopEndBar();

        final long positionTicks = nativeSequencePositionTicks();
        final double tempo = nativeSequenceGetTempo();
        final int sequenceCount = Math.max(1, nativeSequenceGetCount());
        final int sequenceIndex = Math.max(
                0,
                Math.min(sequenceCount - 1, nativeSequenceGetIndex()));

        sequenceOverviewView.setState(
                sequenceIndex,
                sequenceCount,
                bars,
                loopStart,
                loopEnd,
                nativeSequenceGetNumerator(),
                nativeSequenceGetDenominator(),
                nativeSequenceIsLoopEnabled(),
                positionTicks,
                nativeSequenceIsPlaying());

        if (sequenceTransportView != null) {
            final int queuedIndex = nativeSequenceGetQueuedIndex();
            final String queueLabel = queuedIndex >= 0
                    && queuedIndex < sequenceCount
                    ? String.format(Locale.ROOT, " →S%02d", queuedIndex + 1)
                    : "";
            sequenceTransportView.setText(String.format(
                    Locale.ROOT,
                    "S%02d/%02d%s %s %.1f",
                    sequenceIndex + 1,
                    sequenceCount,
                    queueLabel,
                    formatSequencePosition(positionTicks),
                    tempo));
        }
    }

    private void stopSequenceUiUpdater() {
        if (sequenceUiUpdater != null) {
            sequenceUiHandler.removeCallbacks(sequenceUiUpdater);
            sequenceUiUpdater = null;
        }
    }

    private void showMixPage() {
        nativeSequenceSetLauncherContext(false, 0);
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
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "MENU";
        pageTitle.setText("MENU");
        content.removeAllViews();

        LinearLayout page = page();
        page.addView(sectionLabel("WORK MODES"));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        String[][] items = {
                {"MAIN", "MAIN"}, {"BROWSER", "BROWSE"}, {"SAMPLE", "SAMPLE"}, {"RECORDER", "REC"},
                {"SEQUENCER", "SEQ"}, {"MIXER", "MIX"}, {"AUDIO SETTINGS", "AUDIO"}, {"GRID", "GRID"},
                {"STEP", "STEP"}, {"TRACK EDIT", "TRACK"}, {"PAD MIX", "PAD"}, {"Q-LINK", "QLINK"},
                {"PROJECT", "PROJECT"}
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

        LinearLayout columns = row();

        LinearLayout routing = panel();
        routing.addView(sectionLabel("ROUTING"));

        routing.addView(label("OUTPUT DEVICE", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        outputDeviceSpinner = new Spinner(this);
        routing.addView(outputDeviceSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        routing.addView(label("INPUT DEVICE", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        inputDeviceSpinner = new Spinner(this);
        routing.addView(inputDeviceSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        TextView routingNote = label(
                "Default leaves Android's primary route selected. USB audio devices are shown when Android exposes them as audio endpoints.",
                11, MUTED);
        routingNote.setPadding(dp(4), dp(8), dp(4), dp(8));
        routing.addView(routingNote,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        columns.addView(routing,
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.50f));

        LinearLayout engine = panel();
        engine.addView(sectionLabel("ENGINE"));

        engine.addView(label("SAMPLE RATE", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        sampleRateSpinner = new Spinner(this);
        engine.addView(sampleRateSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        engine.addView(label("BUFFER", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));
        bufferSizeSpinner = new Spinner(this);
        engine.addView(bufferSizeSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        LinearLayout modeRow = row();
        LinearLayout sharingColumn = column();
        sharingColumn.addView(label("SHARING", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        sharingModeSpinner = new Spinner(this);
        sharingColumn.addView(sharingModeSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        modeRow.addView(sharingColumn,
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout performanceColumn = column();
        performanceColumn.addView(label("PERFORMANCE", 11, MUTED),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        performanceModeSpinner = new Spinner(this);
        performanceColumn.addView(performanceModeSpinner,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));
        modeRow.addView(performanceColumn,
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        engine.addView(modeRow);

        TextView engineNote = label(
                "AUTO keeps Android/Oboe free to negotiate the native device rate and burst size.",
                11, MUTED);
        engineNote.setPadding(dp(4), dp(8), dp(4), dp(8));
        engine.addView(engineNote,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        columns.addView(engine,
                new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.MATCH_PARENT, 0.50f));

        page.addView(columns,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout actions = row();
        actions.addView(actionButton("REFRESH DEVICES",
                v -> refreshAudioSettingsPage()), weight());
        actions.addView(actionButton("TEST OUTPUT",
                v -> {
                    final String result = nativeAudioTestOutput();
                    setBottomStatus(result);
                    refreshAudioRoutingDiagnostics();
                }), weight());
        actions.addView(actionButton("APPLY & RESTART",
                v -> applyAudioSettings()), weight());
        page.addView(actions,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        audioRoutingDiagnostics = label("", 10, MUTED);
        audioRoutingDiagnostics.setBackground(
                strokeBackground(SURFACE_2, LINE, 8));
        audioRoutingDiagnostics.setPadding(dp(10), dp(6), dp(10), dp(6));
        page.addView(audioRoutingDiagnostics,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        content.addView(page);
        setupAudioSettingSpinners();
        refreshAudioDevicesFromSystem();
        refreshAudioRoutingDiagnostics();
        updateModeRailSelection();
    }

    private void setupAudioSettingSpinners() {
        audioSettingsBinding = true;
        sampleRateSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AUTO", "44100 Hz", "48000 Hz", "88200 Hz", "96000 Hz"}));
        bufferSizeSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"AUTO", "64", "96", "128", "192", "256", "384", "512", "1024"}));
        sharingModeSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"SHARED", "EXCLUSIVE"}));
        performanceModeSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"LOW LATENCY", "NORMAL"}));

        sampleRateSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {}
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        audioSettingsBinding = false;
    }

    private void refreshAudioSettingsPage() {
        if (!"AUDIO".equals(currentPage)) {
            showAudioSettingsPage();
            return;
        }
        refreshAudioDevicesFromSystem();
        refreshAudioRoutingDiagnostics();
    }

    private void refreshAudioDevicesFromSystem() {
        if (audioManager == null) return;

        final AudioDeviceInfo[] outputs =
                audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        final AudioDeviceInfo[] inputs =
                audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);

        outputDevices.clear();
        inputDevices.clear();
        for (AudioDeviceInfo device : outputs) {
            if (device != null && device.isSink()) outputDevices.add(device);
        }
        for (AudioDeviceInfo device : inputs) {
            if (device != null && device.isSource()) inputDevices.add(device);
        }

        if (!"AUDIO".equals(currentPage)
                || outputDeviceSpinner == null
                || inputDeviceSpinner == null) {
            return;
        }

        runOnUiThread(() -> {
            if (!"AUDIO".equals(currentPage)
                    || outputDeviceSpinner == null
                    || inputDeviceSpinner == null) {
                return;
            }

            final int previousOutputId = selectedOutputDeviceId();
            final int previousInputId = selectedInputDeviceId();

            audioSettingsBinding = true;
            outputDeviceSpinner.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    audioOutputLabels()));
            inputDeviceSpinner.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    audioInputLabels()));
            outputDeviceSpinner.setSelection(
                    indexForDeviceId(outputDevices, previousOutputId));
            inputDeviceSpinner.setSelection(
                    indexForDeviceId(inputDevices, previousInputId));
            audioSettingsBinding = false;
        });
    }

    private List<String> audioOutputLabels() {
        final List<String> result = new ArrayList<>();
        result.add("DEFAULT / Android primary route");
        for (AudioDeviceInfo device : outputDevices) {
            result.add(formatAudioDevice(device));
        }
        return result;
    }

    private List<String> audioInputLabels() {
        final List<String> result = new ArrayList<>();
        result.add("DEFAULT / Android input");
        for (AudioDeviceInfo device : inputDevices) {
            result.add(formatAudioDevice(device));
        }
        return result;
    }

    private String formatAudioDevice(AudioDeviceInfo device) {
        final String name = String.valueOf(device.getProductName());
        final String type = audioDeviceTypeName(device.getType());
        final int[] channels = device.getChannelCounts();
        final int channelCount = channels.length > 0 ? channels[0] : 0;
        return "#" + device.getId() + "  " + type
                + "  " + name
                + (channelCount > 0 ? "  •  " + channelCount + "ch" : "");
    }

    private String audioDeviceTypeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "BUILT-IN SPEAKER";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return "EARPIECE";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "BUILT-IN MIC";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "WIRED HEADSET";
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES: return "WIRED HEADPHONES";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return "BLUETOOTH A2DP";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "BLUETOOTH SCO";
            case AudioDeviceInfo.TYPE_BLE_HEADSET: return "BLE HEADSET";
            case AudioDeviceInfo.TYPE_USB_DEVICE: return "USB AUDIO";
            case AudioDeviceInfo.TYPE_USB_ACCESSORY: return "USB ACCESSORY";
            case AudioDeviceInfo.TYPE_USB_HEADSET: return "USB HEADSET";
            case AudioDeviceInfo.TYPE_HDMI: return "HDMI";
            case AudioDeviceInfo.TYPE_HDMI_ARC: return "HDMI ARC";
            case AudioDeviceInfo.TYPE_HDMI_EARC: return "HDMI EARC";
            case AudioDeviceInfo.TYPE_LINE_ANALOG: return "LINE ANALOG";
            case AudioDeviceInfo.TYPE_LINE_DIGITAL: return "LINE DIGITAL";
            case AudioDeviceInfo.TYPE_AUX_LINE: return "AUX LINE";
            case AudioDeviceInfo.TYPE_DOCK: return "DOCK";
            default: return "TYPE " + type;
        }
    }

    private int indexForDeviceId(List<AudioDeviceInfo> devices, int deviceId) {
        if (deviceId < 0) return 0;
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).getId() == deviceId) return i + 1;
        }
        return 0;
    }

    private int selectedOutputDeviceId() {
        final int position = outputDeviceSpinner == null
                ? 0 : outputDeviceSpinner.getSelectedItemPosition();
        return position <= 0 || position - 1 >= outputDevices.size()
                ? -1 : outputDevices.get(position - 1).getId();
    }

    private int selectedInputDeviceId() {
        final int position = inputDeviceSpinner == null
                ? 0 : inputDeviceSpinner.getSelectedItemPosition();
        return position <= 0 || position - 1 >= inputDevices.size()
                ? -1 : inputDevices.get(position - 1).getId();
    }

    private int selectedSampleRate() {
        if (sampleRateSpinner == null) return 0;
        switch (sampleRateSpinner.getSelectedItemPosition()) {
            case 1: return 44100;
            case 2: return 48000;
            case 3: return 88200;
            case 4: return 96000;
            default: return 0;
        }
    }

    private int selectedBufferSize() {
        if (bufferSizeSpinner == null) return 0;
        switch (bufferSizeSpinner.getSelectedItemPosition()) {
            case 1: return 64;
            case 2: return 96;
            case 3: return 128;
            case 4: return 192;
            case 5: return 256;
            case 6: return 384;
            case 7: return 512;
            case 8: return 1024;
            default: return 0;
        }
    }

    private boolean selectedExclusive() {
        return sharingModeSpinner != null
                && sharingModeSpinner.getSelectedItemPosition() == 1;
    }

    private boolean selectedLowLatency() {
        return performanceModeSpinner == null
                || performanceModeSpinner.getSelectedItemPosition() == 0;
    }

    private void applyAudioSettings() {
        if (audioSettingsBinding) return;

        final String inputResult = nativeAudioConfigureInputDevice(
                selectedInputDeviceId());
        final String outputResult = nativeAudioConfigureOutput(
                selectedOutputDeviceId(),
                selectedSampleRate(),
                selectedBufferSize(),
                selectedExclusive(),
                selectedLowLatency());

        setBottomStatus(inputResult + " | " + outputResult);
        setAudioStateFromResult(outputResult);
        refreshAudioRoutingDiagnostics();
    }

    private void refreshAudioRoutingDiagnostics() {
        if (audioRoutingDiagnostics == null) return;

        final String status = nativeAudioStatus();
        audioRoutingDiagnostics.setText(
                "ROUTE / ENGINE\n" + status
                        + "\nInput device: #"
                        + selectedInputDeviceId());
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
        // Android file providers are inconsistent about the MIME they report
        // for WAV (audio/wav, audio/x-wav, audio/wave, or even octet-stream).
        // Start with the broad MIME contract and let the native WAV decoder
        // perform the final format validation.
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
            // Some vendor pickers reject the extra MIME contract. Retry with
            // the simplest Android document-provider request.
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
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Persistable access is optional; the immediate read still
                    // works with the transient document-provider grant.
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
                || result.startsWith("Output test")
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

    private LinearLayout.LayoutParams touchButtonWeight() {
        return new LinearLayout.LayoutParams(0, dp(48), 1);
    }

    private LinearLayout.LayoutParams touchRowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        p.topMargin = dp(3);
        p.bottomMargin = dp(3);
        return p;
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

        View seq = findViewWithExactText(getWindow().getDecorView(), "SEQ");
        if (seq == null || !seq.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: SEQ mode");
            return;
        }

        View grid = findViewWithExactText(getWindow().getDecorView(), "GRID");
        if (grid == null || !grid.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: GRID editor");
            return;
        }

        View gridView = findViewWithContentDescription(
                getWindow().getDecorView(), "Sequence 16 by 16 step grid");
        if (gridView == null || gridView.getHeight() <= dp(120)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sequence grid editor");
            return;
        }

        View step = findViewWithExactText(
                getWindow().getDecorView(), "STEP");
        if (step == null || !step.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: STEP editor");
            return;
        }

        View stepCell = findViewWithContentDescription(
                getWindow().getDecorView(), "Pad 1 step 1 off");
        if (stepCell == null || stepCell.getWidth() <= 0
                || stepCell.getHeight() <= dp(40)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step editor cells");
            return;
        }

        if (!stepCell.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step toggle");
            return;
        }
        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Pad 1 step 1 on") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step did not turn on");
            return;
        }

        View velocityUp = findViewWithExactText(
                getWindow().getDecorView(), "VEL +10");
        View probabilityDown = findViewWithExactText(
                getWindow().getDecorView(), "PROB −10");
        View ratchetUp = findViewWithExactText(
                getWindow().getDecorView(), "RATCH +");
        if (velocityUp == null || probabilityDown == null || ratchetUp == null
                || !velocityUp.performClick()
                || !probabilityDown.performClick()
                || !ratchetUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step event controls");
            return;
        }
        if (findViewWithExactText(
                getWindow().getDecorView(), "STEP 01  •  VEL 110  •  PROB 117  •  RAT 2x") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step event parameters");
            return;
        }

        View backSeq = findViewWithExactText(
                getWindow().getDecorView(), "BACK SEQ");
        if (backSeq == null || !backSeq.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: return to SEQ");
            return;
        }

        View newSequence = findViewWithExactText(
                getWindow().getDecorView(), "NEW");
        if (newSequence == null || !newSequence.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: NEW sequence");
            return;
        }

        View launch = findViewWithExactText(
                getWindow().getDecorView(), "LAUNCH");
        if (launch == null || !launch.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: LAUNCH mode");
            return;
        }

        View launcher = findViewWithContentDescription(
                getWindow().getDecorView(), "Sequence live launcher");
        if (launcher == null || launcher.getHeight() <= dp(120)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sequence launcher");
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
    public void onSequenceLauncherPad(int padIndex) {
        if (!"SEQ".equals(currentPage)
                || sequenceLauncherView == null
                || padIndex < 0 || padIndex >= 16) {
            return;
        }

        setBottomStatus(nativeSequenceLaunchPad(
                launcherBank, padIndex));
        refreshSequenceLauncher();
        refreshSequenceControls();
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
    public void onHardwareAction(int actionType, int value0, int value1, int value2) {
        applyHardwareAction(actionType, value0, value1, value2);
    }

    private void setHardwareButtonLed(int cc, boolean on) {
        final int state = on ? 2 : 0;
        switch (cc) {
            case 82:
                if (lastPlayLedState == state) return;
                lastPlayLedState = state;
                break;
            case 73:
                if (lastRecordLedState == state) return;
                lastRecordLedState = state;
                break;
            case 80:
                if (lastOverdubLedState == state) return;
                lastOverdubLedState = state;
                break;
            case 11:
                if (lastNoteRepeatLedState == state) return;
                lastNoteRepeatLedState = state;
                break;
            case 39:
                if (lastLevelLedState == state) return;
                lastLevelLedState = state;
                break;
            case 40:
                if (lastSixteenLevelLedState == state) return;
                lastSixteenLevelLedState = state;
                break;
            case 4:
                if (lastMuteLedState == state) return;
                lastMuteLedState = state;
                break;
            default:
                break;
        }

        if (midiBridge != null) {
            midiBridge.send(
                    MpcStudioMk2MidiMessages.buttonLed(
                            cc,
                            state));
        }
    }

    private void syncHardwareTransportLeds() {
        if (midiBridge == null) return;

        final boolean playing = nativeSequenceIsPlaying();
        final boolean armed = nativeSequenceIsSelectedTrackArmed();
        final boolean overdub = nativeSequenceGetRecordMode() == 1;

        setHardwareButtonLed(82, playing);
        setHardwareButtonLed(73, armed && !overdub);
        setHardwareButtonLed(80, armed && overdub);
    }

    private void applyHardwareAction(
            int actionType, int value0, int value1, int value2) {
        switch (actionType) {
            case MpcStudioMk2SemanticActions.NAVIGATE_MAIN:
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_VIEW:
                hardwareFocus = 0;
                showMainPage();
                setBottomStatus(
                        actionType == MpcStudioMk2SemanticActions.NAVIGATE_MAIN
                                ? "MAIN" : "TRACK VIEW");
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_GRID:
                showSequenceGridPage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_WAVEFORM:
            case MpcStudioMk2SemanticActions.NAVIGATE_SAMPLE_EDIT:
                showSamplePage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_PAD_MIXER:
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_MIXER:
                showMixPage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_SEQUENCE_LAUNCHER:
                showSequenceLauncherPage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_BROWSE:
                showBrowserPage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_SAMPLER:
                showRecordPage();
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_STEP:
                showSequenceStepPage();
                return;
            case MpcStudioMk2SemanticActions.BROWSER_UP:
                showBrowserPage();
                setBottomStatus("BROWSER UP");
                return;
            case MpcStudioMk2SemanticActions.TRACK_SELECTION_CONTEXT:
                hardwareFocus = 2;
                showSequencePage();
                setBottomStatus("TRACK SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.SEQUENCE_SELECTION_CONTEXT:
                hardwareFocus = 3;
                showSequencePage();
                setBottomStatus("SEQUENCE SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.PROGRAM_SELECTION_CONTEXT:
                hardwareFocus = 4;
                setBottomStatus("PROGRAM SELECT • reserved");
                return;
            case MpcStudioMk2SemanticActions.TRACK_TYPE_SELECTION_CONTEXT:
                hardwareFocus = 5;
                setBottomStatus("TRACK TYPE • reserved");
                return;
            case MpcStudioMk2SemanticActions.DATA_DIAL_DELTA:
            case MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA:
                handleHardwareDialDelta(value0, value1 != 0);
                return;
            case MpcStudioMk2SemanticActions.DATA_DIAL_PRESS:
                setBottomStatus("DATA DIAL ENTER");
                return;
            case MpcStudioMk2SemanticActions.PAD_BANK_CHANGED:
                hardwarePadBank = Math.max(0, Math.min(7, value0));
                if ("SEQ".equals(currentPage)
                        && pageTitle != null
                        && "SEQ • LAUNCH".equals(pageTitle.getText().toString())) {
                    launcherBank = hardwarePadBank;
                    nativeSequenceSetLauncherContext(true, launcherBank);
                    refreshSequenceLauncher();
                    refreshSequenceControls();
                    setBottomStatus("LAUNCH BANK " + (hardwarePadBank + 1));
                } else {
                    setBottomStatus(
                            "PAD BANK " + (hardwarePadBank + 1)
                                    + " • normal performance uses current 16-pad domain");
                }
                return;
            case MpcStudioMk2SemanticActions.NOTE_REPEAT_STATE:
                setHardwareButtonLed(11, value0 != 0);
                setBottomStatus(
                        value0 != 0
                                ? "NOTE REPEAT ON • "
                                        + (value1 != 0 ? "LATCHED" : "MOMENTARY")
                                        + " • grid-synchronised"
                                : "NOTE REPEAT OFF");
                return;
            case MpcStudioMk2SemanticActions.FULL_LEVEL_STATE:
                setHardwareButtonLed(39, value0 != 0);
                setBottomStatus(value0 != 0 ? "FULL LEVEL ON • 127" : "FULL LEVEL OFF");
                return;
            case MpcStudioMk2SemanticActions.HALF_LEVEL_STATE:
                setHardwareButtonLed(39, value0 != 0);
                setBottomStatus(value0 != 0 ? "HALF LEVEL ON • 64" : "HALF LEVEL OFF");
                return;
            case MpcStudioMk2SemanticActions.SIXTEEN_LEVEL_STATE:
                setHardwareButtonLed(40, value0 != 0);
                if (value0 != 0 && value1 >= 0) {
                    setBottomStatus(
                            "16 LEVEL • VELOCITY • SOURCE PAD " + (value1 + 1));
                } else if (value0 != 0) {
                    setBottomStatus("16 LEVEL unavailable • hit a pad first");
                } else {
                    setBottomStatus("16 LEVEL OFF");
                }
                return;
            case MpcStudioMk2SemanticActions.PAD_MUTE_MODE_STATE:
                setHardwareButtonLed(4, value0 != 0);
                setBottomStatus(value0 != 0 ? "PAD MUTE MODE" : "PAD MUTE MODE OFF");
                return;
            case MpcStudioMk2SemanticActions.TRACK_MUTE_MODE_STATE:
                setHardwareButtonLed(4, value0 != 0);
                setBottomStatus(value0 != 0 ? "TRACK MUTE MODE" : "TRACK MUTE MODE OFF");
                return;
            case MpcStudioMk2SemanticActions.PAD_MUTE_TARGET:
                setBottomStatus(nativeSequenceTogglePadMute(value0));
                return;
            case MpcStudioMk2SemanticActions.TRACK_MUTE_TARGET:
                setBottomStatus(nativeSequenceToggleTrackMute(value0));
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_RECORD:
                setBottomStatus(
                        nativeSequenceSetSelectedTrackArmed(
                                !nativeSequenceIsSelectedTrackArmed()));
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_OVERDUB:
                setBottomStatus(nativeSequenceSetRecordMode(1));
                if (!nativeSequenceIsSelectedTrackArmed()) {
                    setBottomStatus(nativeSequenceSetSelectedTrackArmed(true));
                }
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_STOP:
                setBottomStatus(nativeSequenceStop());
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_PLAY:
                setBottomStatus(nativeSequenceStart());
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_PLAY_START:
                nativeSequenceReset();
                setBottomStatus(nativeSequenceStart());
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.TRANSPORT_RESET:
                setBottomStatus(nativeSequenceReset());
                syncHardwareTransportLeds();
                refreshSequenceControls();
                return;
            case MpcStudioMk2SemanticActions.STEP_LEFT:
                handleHardwarePlayheadMove(value0, -1, false);
                return;
            case MpcStudioMk2SemanticActions.STEP_RIGHT:
                handleHardwarePlayheadMove(value0, 1, false);
                return;
            case MpcStudioMk2SemanticActions.BAR_LEFT:
                handleHardwarePlayheadMove(value0, -1, true);
                return;
            case MpcStudioMk2SemanticActions.BAR_RIGHT:
                handleHardwarePlayheadMove(value0, 1, true);
                return;
            case MpcStudioMk2SemanticActions.TAP_TEMPO:
                handleHardwareTapTempo();
                return;
            case MpcStudioMk2SemanticActions.TOUCH_STRIP_VALUE:
                handleHardwareTouchStrip(value0);
                return;
            case MpcStudioMk2SemanticActions.LOCATE_STATE:
                setBottomStatus(value0 != 0 ? "LOCATE HELD" : "LOCATE OFF");
                return;
            case MpcStudioMk2SemanticActions.LOCATE_PAD:
                setBottomStatus("LOCATE PAD " + (value0 + 1) + " • pending");
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_SELECT_CONTEXT:
                hardwareFocus = 10;
                showSamplePage();
                setBottomStatus("SAMPLE SELECT • DATA DIAL / +/- changes layer");
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_START_CONTEXT:
                hardwareFocus = 7;
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "SAMPLE START • FINE"
                        : "SAMPLE START");
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_END_CONTEXT:
                hardwareFocus = 8;
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "SAMPLE END • FINE"
                        : "SAMPLE END");
                return;
            case MpcStudioMk2SemanticActions.TUNE_CONTEXT:
                hardwareFocus = 9;
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "TUNE • FINE"
                        : "TUNE");
                return;
            case MpcStudioMk2SemanticActions.QUANTIZE:
                if (value0 != 0) {
                    setBottomStatus(
                            "QUANTIZE • selection-aware command pending");
                } else {
                    setBottomStatus(nativeSequenceQuantizeSelectedTrack());
                    refreshSequenceControls();
                }
                return;
            case MpcStudioMk2SemanticActions.TIMING_CORRECT_STATE:
                if (value0 == 1) {
                    setBottomStatus(
                            nativeSequenceSetTimingCorrectEnabled(
                                    !nativeSequenceIsTimingCorrectEnabled()));
                    refreshSequenceControls();
                } else {
                    setBottomStatus(
                            "TIMING CORRECT CONFIG • grid "
                                    + nativeSequenceGetQuantizeGrid()
                                    + " ticks, swing "
                                    + nativeSequenceGetSwing() + "%");
                }
                return;
            case MpcStudioMk2SemanticActions.ZOOM_CONTEXT:
                setBottomStatus(value0 != 0
                        ? "ZOOM VERTICAL CONTEXT" : "ZOOM HORIZONTAL CONTEXT");
                return;
            case MpcStudioMk2SemanticActions.COPY_CONTEXT:
                setBottomStatus(value0 != 0
                        ? "DELETE CONTEXT • target confirmation required"
                        : "COPY CONTEXT");
                return;
            case MpcStudioMk2SemanticActions.UNDO:
                setBottomStatus(value0 != 0
                        ? "REDO • command layer pending"
                        : "UNDO • command layer pending");
                return;
            default:
                setBottomStatus("MIDI CONTROL RESERVED • " + actionType);
        }
    }

    private void handleHardwareDialDelta(int delta, boolean fine) {
        if (delta == 0) return;
        if (hardwareFocus == 2) {
            final int count = nativeSequenceGetTrackCount();
            if (count <= 0) return;
            int next = nativeSequenceGetSelectedTrack() + delta;
            next %= count;
            if (next < 0) next += count;
            setBottomStatus(nativeSequenceSelectTrack(next));
            showSequencePage();
            return;
        }
        if (hardwareFocus == 3) {
            setBottomStatus(
                    delta > 0 ? nativeSequenceNext() : nativeSequencePrevious());
            showSequencePage();
            return;
        }
        setBottomStatus("DATA DIAL " + (delta > 0 ? "+" : "−") + " • no focused selector");
    }

    private void handleHardwareTouchStrip(int value) {
        value = Math.max(0, Math.min(127, value));

        if (hardwareFocus == 7 || hardwareFocus == 8) {
            final long total =
                    nativeAudioGetPadSampleFrameCount(
                            selectedPad, selectedLayer);
            if (total <= 1) {
                setBottomStatus("TOUCH STRIP • no sample assigned");
                return;
            }

            final long start =
                    nativeAudioGetPadSampleRegionStart(
                            selectedPad, selectedLayer);
            final long end =
                    nativeAudioGetPadSampleRegionEnd(
                            selectedPad, selectedLayer);

            if (hardwareFocus == 7) {
                final long nextStart = Math.min(
                        end - 1L,
                        Math.round(
                                (value / 127.0)
                                        * Math.max(0L, end - 1L)));
                setBottomStatus(nativeAudioSetPadSampleRegion(
                        selectedPad, selectedLayer, nextStart, end));
            } else {
                final long nextEnd = Math.max(
                        start + 1L,
                        Math.round(
                                start + (value / 127.0)
                                        * Math.max(
                                                1L,
                                                total - start)));
                setBottomStatus(nativeAudioSetPadSampleRegion(
                        selectedPad, selectedLayer, start, nextEnd));
            }

            refreshRegionInfo();
            if (sampleWaveform != null) {
                refreshSampleWaveform();
            }
            return;
        }

        if (hardwareFocus == 9) {
            final float semitones = -24.0f + (48.0f * value / 127.0f);
            setBottomStatus(nativeAudioSetPadLayerTuning(
                    selectedPad, selectedLayer, semitones));
            refreshAllInspectorState();
            refreshSampleInfo();
            return;
        }

        setBottomStatus("TOUCH STRIP " + value + "/127");
    }

    private void handleHardwarePlayheadMove(
            int locateMode, int direction, boolean bar) {
        if (locateMode != 0) {
            setBottomStatus(
                    bar
                            ? "LOCATE + BAR • pending"
                            : "LOCATE + STEP • pending");
            return;
        }
        if (nativeSequenceIsPlaying()) {
            setBottomStatus("NAVIGATION: stop playback first");
            return;
        }
        final int numerator = nativeSequenceGetNumerator();
        final int denominator = Math.max(1, nativeSequenceGetDenominator());
        final long beatTicks = Math.max(
                1L, Math.round(960.0 * 4.0 / denominator));
        final long deltaTicks = bar
                ? beatTicks * Math.max(1, numerator) * direction
                : (long) nativeSequenceGetQuantizeGrid() * direction;
        setBottomStatus(nativeSequenceMovePlayheadTicks(deltaTicks));
        refreshSequencePlayhead();
    }

    private void handleHardwareTapTempo() {
        final long now = System.nanoTime();
        if (lastHardwareTapNanos <= 0L
                || now - lastHardwareTapNanos > 2_000_000_000L) {
            hardwareTapIntervalCount = 0;
        } else if (hardwareTapIntervalCount < hardwareTapIntervalsNanos.length) {
            hardwareTapIntervalsNanos[hardwareTapIntervalCount++] =
                    now - lastHardwareTapNanos;
        } else {
            System.arraycopy(
                    hardwareTapIntervalsNanos, 1,
                    hardwareTapIntervalsNanos, 0,
                    hardwareTapIntervalsNanos.length - 1);
            hardwareTapIntervalsNanos[hardwareTapIntervalsNanos.length - 1] =
                    now - lastHardwareTapNanos;
        }
        lastHardwareTapNanos = now;
        if (hardwareTapIntervalCount == 0) {
            setBottomStatus("TAP TEMPO • tap again");
            return;
        }
        long sum = 0L;
        for (int i=0; i<hardwareTapIntervalCount; i++) sum += hardwareTapIntervalsNanos[i];
        final double bpm = Math.max(
                20.0,
                Math.min(300.0,
                        60_000_000_000.0
                                / ((double) sum / hardwareTapIntervalCount)));
        setBottomStatus(nativeSequenceSetTempo(bpm));
        refreshSequenceControls();
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
        stopSequenceUiUpdater();

        if (!uiOnlySmokeMode) {
            nativeAudioStop();
        }

        if (midiBridge != null) {
            midiBridge.close();
            midiBridge = null;
        }
        if (audioManager != null && audioDeviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
            audioDeviceCallback = null;
        }
        super.onDestroy();
    }
}
