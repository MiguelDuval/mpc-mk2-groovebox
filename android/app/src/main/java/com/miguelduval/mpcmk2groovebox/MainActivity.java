package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.provider.OpenableColumns;
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
import java.util.Arrays;
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
    private MpcNavigationController navigationController;
    private MpcShell mpcShell;
    private LinearLayout functionBar;
    private TextView compactSequenceContext;
    private TextView compactTrackContext;
    private TextView compactProgramContext;
    private TextView compactPadContext;
    private TextView compactFocusContext;
    private android.widget.ProgressBar compactPadLevelMeter;
    private TextView compactPadLevelLabel;
    private TextView compactPadPanLabel;
    private TextView compactPadTuneLabel;
    private LinearLayout compactMixerPanel;
    private Button compactMixerToggle;
    private boolean compactMixerVisible = true;
    private Button timingCorrectTopButton;
    private Button metronomeTopButton;
    private Button automationTopButton;
    private final Button[] shortcutButtons =
            new Button[MpcNavigationController.SHORTCUT_COUNT];
    private TextView pageTitle;
    private TextView audioState;
    private TextView midiState;
    private TextView projectState;
    private TextView bottomStatus;
    private TextView hardwareFeedbackView;
    private final int[] hardwareButtonLedStateCache = new int[128];
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
    private WaveformView mainTrackWaveform;
    private WaveformView recordingWaveform;
    private TextView recordingTelemetry;
    private SequenceTimelineView sequenceTimeline;
    private SequenceTimelineView mainArrangementPreview;
    private MpcArrangeView arrangementView;
    private MpcBrowserView browserView;
    private final ArrayList<MpcArrangeView.Lane> arrangementLanes = new ArrayList<>();
    private SequenceOverviewView sequenceOverviewView;
    private SequenceGridView sequenceGridView;
    private FrameLayout mainTrackArrangementHost;
    private Button mainTrackViewButton;
    private Button mainArrangementViewButton;
    private SequenceLauncherView sequenceLauncherView;
    private final Button[] sequenceStepButtons = new Button[16];
    private TextView sequenceStepEventInfo;
    private TextView sequenceTransportView;
    private int sequenceGridStartStep = 0;
    private int sequenceGridVisibleSteps =
            MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS;
    private int sequenceGridStartPad = 0;
    private int sequenceGridVisiblePads = MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS;
    private int sequenceStepPage = 0;
    private int launcherBank = 0;
    private String lastLauncherLedSignature = "";
    private String lastStepEditLedSignature = "";
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
    private TextView gridToolStateView;
    private TextView gridSelectionStateView;
    private final Handler sequenceUiHandler = new Handler(Looper.getMainLooper());
    private Runnable sequenceUiUpdater;
    private static final int STEP_EDIT_PARAMETER_VELOCITY = 0;
    private static final int STEP_EDIT_PARAMETER_PROBABILITY = 1;
    private static final int STEP_EDIT_PARAMETER_RATCHET = 2;
    private static final int STEP_EDIT_PARAMETER_NUDGE = 3;
    private static final int STEP_EDIT_PARAMETER_DURATION = 4;
    private static final int TOUCH_STRIP_MODE_LEVEL = 0;
    private static final int TOUCH_STRIP_MODE_PAN = 1;
    private static final int TOUCH_STRIP_MODE_TUNE = 2;
    private static final int TOUCH_STRIP_MODE_SAMPLE_START = 3;
    private static final int TOUCH_STRIP_MODE_SAMPLE_END = 4;

    private int selectedPad = 0;
    private int selectedLayer = 0;
    private int hardwareFocus = 0;
    private int stepEditParameter = STEP_EDIT_PARAMETER_VELOCITY;
    private int hardwarePadBank = 0;
    private int hardwareTouchStripMode = TOUCH_STRIP_MODE_LEVEL;
    private int hardwareNoteRepeatRateIndex = 2;
    private boolean hardwareEraseActive;
    private boolean hardwareCopyDeleteActive;
    private int hardwareCopyDeleteMode = 0;
    private int hardwareCopySourcePad = -1;
    private int hardwareCopyPadMask = 0;
    private String lastTouchStripLedSignature = "";
    private String lastNoteRepeatDivisionLedSignature = "";
    private int lastTouchStripButtonLedState = -1;
    private int lastEraseLedState = -1;
    private int lastCopyDeleteLedState = -1;
    private int lastPlayLedState = -1;
    private int lastRecordLedState = -1;
    private int lastOverdubLedState = -1;
    private int lastNoteRepeatLedState = -1;
    private int lastLevelLedState = -1;
    private int lastSixteenLevelLedState = -1;
    private int lastMuteLedState = -1;
    private boolean hardwareNoteRepeatActive;
    private String lastLcdSignature = "";
    private long lastHardwareTapNanos = 0L;
    private final long[] hardwareTapIntervalsNanos = new long[4];
    private int hardwareTapIntervalCount = 0;
    private int selectedSequenceStep = -1;
    private boolean hardwareLocateActive;
    private String currentPage = "MAIN";
    private volatile boolean destroyed;
    private volatile boolean startupComplete;
    private boolean uiOnlySmokeMode;
    private boolean uiAuditSmokeMode;
    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor();

    private static native String nativeEngineInfo();
    private static native String nativeAudioLoadSample(byte[] data);
    private static native String nativeAudioLoadSampleForPadLayer(byte[] data, int pad, int layer);
    private static native String nativeAudioSetPadSampleName(
            int pad, int layer, String name);
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
    private static native String nativeAudioGetPadSampleName(int pad, int layer);
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
    private static native void nativeSequenceSetStepEditContext(
            boolean enabled, int page);
    private static native int nativeStepEditParameterNext(int parameter);
    private static native int nativeStepEditParameterDelta(
            int parameter, int gridTicks, int direction, boolean fine);
    private static native String nativeSequenceLaunchPad(
            int bank, int padIndex);
    private static native String nativeSequenceSelect(int sequenceIndex);
    private static native String nativeSequencePrevious();
    private static native String nativeSequenceNext();
    private static native String nativeSequenceCopyPadToPads(int sourcePad, int destinationMask);
    private static native String nativeSequenceDeletePadAssignments(int padMask);
    private static native String nativeSequenceUndo();
    private static native String nativeSequenceRedo();
    private static native boolean nativeSequenceCanUndo();
    private static native boolean nativeSequenceCanRedo();
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
    private static native String nativeSequenceQuantizeSelectedTrack();
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
    private static native String nativeSequenceSetStepDuration(
            int padIndex, int stepIndex, int gridTicks, int durationTicks);
    private static native int nativeSequenceGetRecordMode();
    private static native String nativeSequenceSetRecordMode(int mode);
    private static native int nativeSequenceDrainRecordEvents();
    private static native int nativeSequenceGetTrackCount();
    private static native int nativeSequenceGetSelectedTrack();
    private static native int nativeSequenceGetDrumProgramCount();
    private static native String nativeSequenceGetDrumProgramName(int programIndex);
    private static native int nativeSequenceGetTrackProgramIndex(int trackIndex);
    private static native String nativeSequenceSetTrackProgram(
            int trackIndex, int programIndex);
    private static native String nativeSequenceGetTrackType(int trackIndex);
    private static native String nativeSequenceGetTrackArrangementData(int trackIndex);
    private static native boolean nativeSequenceIsTrackMuted(int trackIndex);
    private static native String nativeSequenceGetTrackProgram(int trackIndex);
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
    private static native String nativeSequenceErasePadAtPlayhead(int padIndex);
    private static native String nativeSequenceSetLocator(int slot);
    private static native long nativeSequenceGetLocator(int slot);
    private static native String nativeSequenceJumpToLocator(int slot);
    private static native String nativeSequenceLocateMoveTicks(long deltaTicks);
    private static native String nativeSequenceMoveToLocateBoundary(int direction);
    private static native String nativeSequenceMoveToPreviousOrNextEvent(int direction);
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

        navigationController = new MpcNavigationController(
                uiState -> runOnUiThread(this::updateMpcShellState));

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

        Arrays.fill(hardwareButtonLedStateCache, -1);
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
                    if ("MAIN".equals(currentPage)) {
                        refreshMainTrackQuickSample();
                    }
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
        mpcShell = new MpcShell(this);
        mpcShell.toolbar().addView(buildTopBar(),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        buildShortcutRail(mpcShell.shortcuts());
        buildCompactContext(mpcShell.contextArea());

        content = mpcShell.workspace();
        functionBar = mpcShell.functionBar();

        hardwareFeedbackView = label("MKII • MAIN • PAD BANK A", 10, ACCENT);
        hardwareFeedbackView.setPadding(dp(12), 0, dp(12), 0);
        hardwareFeedbackView.setGravity(Gravity.CENTER_VERTICAL);
        hardwareFeedbackView.setTypeface(Typeface.DEFAULT_BOLD);
        hardwareFeedbackView.setBackgroundColor(SURFACE);
        hardwareFeedbackView.setContentDescription(
                "MPC Studio MkII controller feedback status");

        bottomStatus = label("Initializing…", 11, MUTED);
        bottomStatus.setPadding(dp(12), 0, dp(12), 0);
        bottomStatus.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.addView(mpcShell.root(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        root.addView(hardwareFeedbackView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));
        root.addView(bottomStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        refreshMpcFunctionBar();
        showMainPage();
        syncHardwareControllerFeedback();
        updateModeRailSelection();
        return root;
    }

    private void buildShortcutRail(LinearLayout rail) {
        rail.removeAllViews();
        MpcUiState.Mode[] modes = navigationController.shortcuts();
        for (int i = 0; i < modes.length; i++) {
            final MpcUiState.Mode mode = modes[i];
            Button shortcut = modeButton(
                    String.format(
                            Locale.ROOT, "%d\n%s",
                            i + 1, mode.label()),
                    mode.name());
            shortcut.setContentDescription("MPC shortcut " + (i + 1) + " " + mode.label());
            shortcut.setTag(mode);
            shortcut.setOnClickListener(v -> navigateToMode(mode));
            shortcutButtons[i] = shortcut;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
            if (i > 0) {
                params.topMargin = dp(3);
            }
            rail.addView(shortcut, params);
        }
    }

    private void buildCompactContext(LinearLayout area) {
        area.removeAllViews();
        area.setContentDescription("MPC shell compact track program context");

        LinearLayout header = row();
        TextView headerLabel = label("CONTEXT", 9, MUTED);
        headerLabel.setTypeface(Typeface.DEFAULT_BOLD);
        headerLabel.setGravity(Gravity.CENTER_VERTICAL);
        headerLabel.setPadding(dp(7), 0, dp(7), 0);
        header.addView(headerLabel, new LinearLayout.LayoutParams(
                0, dp(26), 1));

        compactMixerToggle = actionButton("MIX", v -> {
            compactMixerVisible = !compactMixerVisible;
            applyCompactMixerVisibility();
        });
        compactMixerToggle.setTextSize(9);
        compactMixerToggle.setContentDescription(
                "MPC condensed Mixer Strip show or hide");
        header.addView(compactMixerToggle,
                new LinearLayout.LayoutParams(dp(54), dp(26)));
        area.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        compactSequenceContext = compactContextField(
                "SEQ 01 • 120.0 BPM",
                "MPC shell sequence context",
                v -> showSequenceSelectPage());
        area.addView(compactSequenceContext, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        compactTrackContext = compactContextField(
                "TRACK 01",
                "MPC shell track context",
                v -> showTrackSelectPage());
        area.addView(compactTrackContext, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        compactProgramContext = compactContextField(
                "PROGRAM • —",
                "MPC shell program context",
                v -> showProgramSelectPage());
        area.addView(compactProgramContext, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        TextView channelRule = label("CHANNEL", 8, MUTED);
        channelRule.setTypeface(Typeface.DEFAULT_BOLD);
        channelRule.setPadding(dp(7), dp(3), dp(7), 0);
        area.addView(channelRule, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        compactPadContext = compactContextField(
                "PAD 01 • BANK A",
                "MPC shell pad context",
                null);
        area.addView(compactPadContext, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        compactMixerPanel = column();
        compactMixerPanel.setContentDescription(
                "MPC condensed Mixer Strip");
        TextView mixerHeader = label("MIXER STRIP", 8, MUTED);
        mixerHeader.setTypeface(Typeface.DEFAULT_BOLD);
        mixerHeader.setPadding(dp(7), dp(3), dp(7), 0);
        compactMixerPanel.addView(mixerHeader, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        compactPadLevelMeter = new android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        compactPadLevelMeter.setMax(100);
        compactPadLevelMeter.setProgress(100);
        compactPadLevelMeter.setContentDescription(
                "MPC Main Mixer Strip level meter");
        compactMixerPanel.addView(compactPadLevelMeter, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));

        compactPadLevelLabel = label(
                "LEVEL 100%",
                9,
                TEXT);
        compactPadLevelLabel.setGravity(Gravity.CENTER_VERTICAL);
        compactPadLevelLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadLevelLabel.setContentDescription(
                "MPC Main Mixer Strip level");
        compactMixerPanel.addView(compactPadLevelLabel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        LinearLayout padMixValues = row();

        compactPadPanLabel = label(
                "PAN C",
                9,
                MUTED);
        compactPadPanLabel.setGravity(Gravity.CENTER);
        compactPadPanLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadPanLabel.setContentDescription(
                "MPC Main Mixer Strip pad pan");
        compactPadPanLabel.setBackground(strokeBackground(
                SURFACE_2, LINE, 5));
        padMixValues.addView(compactPadPanLabel, new LinearLayout.LayoutParams(
                0, dp(28), 1));

        compactPadTuneLabel = label(
                "TUNE +0.0",
                9,
                MUTED);
        compactPadTuneLabel.setGravity(Gravity.CENTER);
        compactPadTuneLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadTuneLabel.setContentDescription(
                "MPC Main Mixer Strip pad tuning");
        compactPadTuneLabel.setBackground(strokeBackground(
                SURFACE_2, LINE, 5));
        padMixValues.addView(compactPadTuneLabel, new LinearLayout.LayoutParams(
                0, dp(28), 1));

        compactMixerPanel.addView(padMixValues, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        area.addView(compactMixerPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(98)));

        compactFocusContext = compactContextField(
                "DIAL • NONE",
                "MPC shell dial focus",
                null);
        area.addView(compactFocusContext, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        sequenceOverviewView = new SequenceOverviewView(this);
        sequenceOverviewView.setContentDescription("Sequence playback overview");
        area.addView(sequenceOverviewView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        TextView stateHint = label(
                "SELECTED TRACK",
                8,
                MUTED);
        stateHint.setGravity(Gravity.CENTER_VERTICAL);
        stateHint.setPadding(dp(7), 0, dp(7), 0);
        area.addView(stateHint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        applyCompactMixerVisibility();
    }

    private TextView compactContextField(
            String initialText,
            String contentDescription,
            View.OnClickListener listener) {
        TextView field = label(initialText, 10, TEXT);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(7), 0, dp(7), 0);
        field.setTypeface(Typeface.DEFAULT_BOLD);
        field.setContentDescription(contentDescription);
        field.setBackground(strokeBackground(
                SURFACE_2,
                LINE,
                6));
        if (listener != null) {
            field.setOnClickListener(listener);
            field.setFocusable(true);
            field.setClickable(true);
        }
        return field;
    }

    private void navigateToMode(MpcUiState.Mode mode) {
        switch (mode) {
            case MAIN:
                showMainPage();
                break;
            case TRACK_VIEW:
                showTrackViewPage();
                break;
            case BROWSER:
                showBrowserPage();
                break;
            case GRID:
                showSequenceGridPage();
                break;
            case STEP:
                showSequenceStepPage();
                break;
            case SAMPLER:
                showRecordPage();
                break;
            case SAMPLE_EDIT:
                showSamplePage();
                break;
            case CHANNEL_MIXER:
                navigationController.navigate(MpcUiState.Mode.CHANNEL_MIXER);
                setBottomStatus("CHANNEL MIXER • RESERVED until track-strip mixer backend");
                updateMpcShellState();
                break;
            case PAD_MIXER:
                showMixPage();
                break;
            case ARRANGE:
                showArrangePage();
                break;
            case NEXT_SEQUENCE:
                showSequenceLauncherPage();
                break;
            case MENU:
                showMenuPage();
                break;
            case MIDI_CONTROL:
                showMidiPage();
                break;
            case PREFERENCES:
                showAudioSettingsPage();
                break;
            default:
                navigationController.setActionAvailable(false);
                setBottomStatus(
                        mode.label() + " • RESERVED / UNAVAILABLE");
                updateMpcShellState();
                break;
        }
    }

    private void updateMpcShellState() {
        updateModeRailSelection();
        refreshMpcCompactContext();
        refreshMpcToolbarState();
        refreshMpcFunctionBar();
    }

    private void refreshMpcToolbarState() {
        if (timingCorrectTopButton == null) {
            return;
        }
        final boolean enabled = startupComplete
                && nativeSequenceIsTimingCorrectEnabled();
        timingCorrectTopButton.setText(enabled ? "TC ON" : "TC OFF");
        timingCorrectTopButton.setBackground(strokeBackground(
                enabled ? Color.rgb(74, 124, 88) : SURFACE_2,
                enabled ? ACTIVE : LINE,
                6));

        if (metronomeTopButton != null) {
            metronomeTopButton.setText("METRO");
        }
        if (automationTopButton != null) {
            automationTopButton.setText("AUTO");
        }
    }

    private void applyCompactMixerVisibility() {
        if (compactMixerPanel == null || compactMixerToggle == null) {
            return;
        }

        compactMixerPanel.setVisibility(
                compactMixerVisible ? View.VISIBLE : View.GONE);
        compactMixerToggle.setText(
                compactMixerVisible ? "MIX ON" : "MIX OFF");
        compactMixerToggle.setTextColor(
                compactMixerVisible ? BG : TEXT);
        compactMixerToggle.setBackground(strokeBackground(
                compactMixerVisible ? ACCENT : SURFACE_2,
                compactMixerVisible ? ACCENT : LINE,
                5));
    }

    private void refreshMpcCompactContext() {
        if (navigationController == null
                || compactSequenceContext == null
                || compactTrackContext == null
                || compactProgramContext == null
                || compactPadContext == null
                || compactFocusContext == null) {
            return;
        }

        final boolean nativeStateReady = startupComplete;
        final int sequence = nativeStateReady
                ? Math.max(0, nativeSequenceGetIndex()) + 1 : 1;
        final int trackIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final int track = trackIndex + 1;
        final double tempo = nativeStateReady
                ? nativeSequenceGetTempo() : 120.0;

        compactSequenceContext.setText(String.format(
                Locale.ROOT, "SEQ %02d • %.1f BPM", sequence, tempo));

        if (nativeStateReady) {
            final String trackType = nativeSequenceGetTrackType(trackIndex);
            final boolean armed = nativeSequenceIsSelectedTrackArmed();
            final boolean muted = nativeSequenceIsTrackMuted(trackIndex);
            compactTrackContext.setText(String.format(
                    Locale.ROOT,
                    "TRACK %02d • %s  %s%s",
                    track,
                    trackType,
                    armed ? "REC" : "—",
                    muted ? " • MUTE" : ""));

            final boolean drumProgramContext =
                    "DRUM".equalsIgnoreCase(trackType);
            final String programStatus =
                    nativeSequenceGetTrackProgram(trackIndex);
            compactProgramContext.setText(
                    drumProgramContext
                            ? "PROGRAM • " + normalizeProgramLabel(programStatus)
                            : "PROGRAM • N/A (" + trackType + ")");
            compactProgramContext.setEnabled(drumProgramContext);
            compactProgramContext.setAlpha(drumProgramContext ? 1.0f : 0.48f);
            if (drumProgramContext) {
                compactProgramContext.setOnClickListener(
                        v -> showProgramSelectPage());
            } else {
                compactProgramContext.setOnClickListener(null);
            }
        } else {
            compactTrackContext.setText("TRACK 01 • DRUM");
            compactProgramContext.setText("PROGRAM • —");
            compactProgramContext.setEnabled(true);
            compactProgramContext.setAlpha(1.0f);
            compactProgramContext.setOnClickListener(
                    v -> showProgramSelectPage());
        }

        final char padBank = (char) ('A' + Math.max(
                0,
                Math.min(7, navigationController.state().padBank())));
        compactPadContext.setText(String.format(
                Locale.ROOT, "PAD %02d • BANK %s",
                selectedPad + 1,
                padBank));

        if (compactPadLevelMeter != null && compactPadLevelLabel != null) {
            final int levelPercent = nativeStateReady
                    ? Math.max(0, Math.min(100,
                            Math.round(nativeAudioGetPadLevel(selectedPad) * 100.0f)))
                    : 100;
            compactPadLevelMeter.setProgress(levelPercent);
            compactPadLevelLabel.setText(String.format(
                    Locale.ROOT,
                    "LEVEL %d%%  •  PAD %02d",
                    levelPercent,
                    selectedPad + 1));

            if (compactPadPanLabel != null) {
                final float pan = nativeStateReady
                        ? Math.max(-1.0f, Math.min(1.0f,
                                nativeAudioGetPadPan(selectedPad)))
                        : 0.0f;
                compactPadPanLabel.setText(
                        "PAN " + (Math.abs(pan) < 0.01f
                                ? "C"
                                : String.format(
                                        Locale.ROOT,
                                        "%s%d",
                                        pan < 0.0f ? "L" : "R",
                                        Math.round(Math.abs(pan) * 100.0f))));
            }

            if (compactPadTuneLabel != null) {
                final float tune = nativeStateReady
                        ? Math.max(-24.0f, Math.min(24.0f,
                                nativeAudioGetPadTuning(selectedPad)))
                        : 0.0f;
                compactPadTuneLabel.setText(String.format(
                        Locale.ROOT,
                        "TUNE %+0.1f",
                        tune));
            }
        }

        final MpcUiState state = navigationController.state();
        compactFocusContext.setText(
                "DIAL • " + state.dataDialFocus().name().replace('_', ' ')
                        + (state.subcontext() == MpcUiState.Subcontext.NONE
                                ? "" : " • " + state.subcontext().name().replace('_', ' ')));
        compactFocusContext.setTextColor(
                state.actionAvailable() ? ACCENT : DANGER);
    }

    private void refreshMpcFunctionBar() {
        if (functionBar == null || navigationController == null) {
            return;
        }

        functionBar.removeAllViews();
        final MpcUiState.Mode mode = navigationController.state().mode();
        final int trackCount = startupComplete
                ? nativeSequenceGetTrackCount() : 0;

        if (mode == MpcUiState.Mode.MAIN) {
            addFunction("NEW TRACK", true, v -> addSequenceTrack(0));
            addFunction("REC ARM", trackCount > 0, v -> {
                setBottomStatus(nativeSequenceSetSelectedTrackArmed(
                        !nativeSequenceIsSelectedTrackArmed()));
                syncHardwareTransportLeds();
                showMainPage();
            });
            addFunction("TRACK −", trackCount > 0,
                    v -> selectAdjacentTrack(-1));
            addFunction("TRACK +", trackCount > 0,
                    v -> selectAdjacentTrack(1));
            addFunction("MUTE", trackCount > 0,
                    v -> toggleSelectedTrackMute());
            addFunction("SOLO", false, null);
            return;
        }

        if (mode == MpcUiState.Mode.TRACK_VIEW) {
            addFunction("NEW TRACK", true, v -> addSequenceTrack(0));
            addFunction("REC ARM", trackCount > 0, v -> {
                setBottomStatus(nativeSequenceSetSelectedTrackArmed(
                        !nativeSequenceIsSelectedTrackArmed()));
                syncHardwareTransportLeds();
                showTrackViewPage();
            });
            addFunction("TRACK −", trackCount > 0,
                    v -> selectAdjacentTrack(-1));
            addFunction("TRACK +", trackCount > 0,
                    v -> selectAdjacentTrack(1));
            addFunction("MUTE", trackCount > 0,
                    v -> toggleSelectedTrackMute());
            addFunction("SOLO", false, null);
            return;
        }

        switch (mode) {
            case BROWSER:
                addFunction("LOAD", true, v -> openWavPicker());
                addFunction("UP", true, v -> showBrowserPage());
                addFunction("FAV", false, null);
                addFunction("SEARCH", false, null);
                addFunction("BACK", true, v -> navigateBackFromShell());
                break;
            case ARRANGE:
                addFunction("CUT", false, null);
                addFunction("COPY", false, null);
                addFunction("PASTE", false, null);
                addFunction("DUP", false, null);
                addFunction("GRID", true, v -> showSequenceGridPage());
                addFunction("TRACK VIEW", true, v -> showTrackViewPage());
                break;

            case GRID:
                addFunction("STEP", drumGridAvailable(), v -> {
                    if (drumGridAvailable()) {
                        showSequenceStepPage();
                    }
                });
                addFunction("ZOOM H", drumGridAvailable(), v -> zoomSequenceGridHorizontal(1));
                addFunction("ZOOM V", drumGridAvailable(), v -> zoomSequenceGridVertical(1));
                addFunction("TRACK VIEW", true, v -> showTrackViewPage());
                addFunction("MAIN", true, v -> showMainPage());
                addFunction("BACK", true, v -> navigateBackFromShell());
                break;

            case STEP: {
                final boolean stepAvailable =
                        navigationController.state().actionAvailable();
                final boolean gridAvailable = drumGridAvailable();
                addFunction("PARAM", stepAvailable, v -> {
                    onHardwareAction(
                            MpcStudioMk2SemanticActions.DATA_DIAL_PRESS,
                            0, 0, 0);
                });
                addFunction("−", stepAvailable, v -> {
                    onHardwareAction(
                            MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA,
                            -1, 0, 0);
                });
                addFunction("+", stepAvailable, v -> {
                    onHardwareAction(
                            MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA,
                            1, 0, 0);
                });
                addFunction("NUDGE −", stepAvailable, v -> adjustSelectedStepNudge(-10));
                addFunction("NUDGE +", stepAvailable, v -> adjustSelectedStepNudge(10));
                addFunction("GRID", gridAvailable, v -> {
                    if (gridAvailable) {
                        showSequenceGridPage();
                    }
                });
                break;
            }

            case SAMPLE_EDIT:
            case SAMPLER:
                addFunction("AUDITION", true,
                        v -> selectAndTriggerPad(selectedPad, 112));
                addFunction("EDIT", true,
                        v -> showSamplePage());
                addFunction("SAMPLER", true,
                        v -> showRecordPage());
                addFunction("BROWSER", true,
                        v -> showBrowserPage());
                addFunction("BACK", true, v -> navigateBackFromShell());
                break;
            default:
                addFunction("BACK", true, v -> navigateBackFromShell());
                break;
        }
    }

    private void addFunction(String text, boolean enabled, View.OnClickListener listener) {
        Button b = actionButton(text, listener);
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1.0f : 0.45f);
        functionBar.addView(b, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
    }

    private void navigateBackFromShell() {
        if (navigationController != null && navigationController.back()) {
            navigateToMode(navigationController.state().mode());
            return;
        }
        showMainPage();
    }

    private void selectAdjacentTrack(int delta) {
        final int count = nativeSequenceGetTrackCount();
        if (count <= 0) {
            setBottomStatus("No tracks");
            return;
        }
        int next = nativeSequenceGetSelectedTrack() + delta;
        next %= count;
        if (next < 0) {
            next += count;
        }
        setBottomStatus(nativeSequenceSelectTrack(next));
        navigationController.setSelectedTrack(next);

        final MpcUiState.Mode mode = navigationController.state().mode();
        if (mode == MpcUiState.Mode.MAIN) {
            showMainPage();
        } else if (mode == MpcUiState.Mode.TRACK_VIEW) {
            showTrackViewPage();
        } else {
            showMainPage();
        }
    }

    private void toggleSelectedTrackMute() {
        final int track = nativeSequenceGetSelectedTrack();
        setBottomStatus(nativeSequenceToggleTrackMute(track));

        if ("ARRANGE".equals(currentPage)) {
            loadArrangementLanes();
            refreshArrangeView();
            refreshMpcCompactContext();
            return;
        }

        if ("TRACK_VIEW".equals(currentPage)) {
            showTrackViewPage();
            return;
        }

        refreshSequenceControls();
        refreshMpcCompactContext();
        if ("MAIN".equals(currentPage)) {
            refreshMainArrangementPreview();
        }
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

        pageTitle = label("MAIN", 12, Color.WHITE);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setTypeface(Typeface.DEFAULT_BOLD);
        bar.addView(pageTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.55f));

        sequenceTransportView = label(
                "BAR 001  BEAT 1  TICK 000",
                10,
                TEXT);
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

        timingCorrectTopButton = topButton("TC");
        timingCorrectTopButton.setContentDescription("Timing Correct");
        timingCorrectTopButton.setOnClickListener(v -> {
            if (!startupComplete) {
                setBottomStatus("TIMING CORRECT • waiting for sequencer");
                return;
            }
            setBottomStatus(nativeSequenceSetTimingCorrectEnabled(
                    !nativeSequenceIsTimingCorrectEnabled()));
            refreshMpcToolbarState();
            syncHardwareTransportLeds();
        });
        bar.addView(timingCorrectTopButton,
                new LinearLayout.LayoutParams(dp(58), dp(38)));

        metronomeTopButton = topButton("METRO");
        metronomeTopButton.setContentDescription("Metronome reserved");
        metronomeTopButton.setEnabled(false);
        metronomeTopButton.setAlpha(0.55f);
        bar.addView(metronomeTopButton,
                new LinearLayout.LayoutParams(dp(68), dp(38)));

        automationTopButton = topButton("AUTO");
        automationTopButton.setContentDescription("Automation reserved");
        automationTopButton.setEnabled(false);
        automationTopButton.setAlpha(0.55f);
        bar.addView(automationTopButton,
                new LinearLayout.LayoutParams(dp(58), dp(38)));

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
        if (navigationController == null) {
            return;
        }
        final MpcUiState.Mode active = navigationController.state().mode();
        for (Button button : shortcutButtons) {
            if (button == null) {
                continue;
            }
            final Object tag = button.getTag();
            final boolean selected = tag == active;
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        clearSequenceLauncherLeds();
        currentPage = "MAIN";
        hardwareFocus = 0;
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
        navigationController.setActionAvailable(true);
        pageTitle.setText("MAIN");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        LinearLayout sequenceCard = panel();
        sequenceCard.setContentDescription("Main Mode Sequence card");

        LinearLayout sequenceHeader = row();
        sequenceHeader.addView(sectionLabelView(
                "SEQUENCE",
                new LinearLayout.LayoutParams(0, dp(32), 1)));
        sequenceHeader.addView(actionButton(
                "SEQ SELECT",
                v -> showSequenceSelectPage()),
                new LinearLayout.LayoutParams(dp(104), dp(32)));
        sequenceCard.addView(sequenceHeader);

        TextView sequenceName = label("", 16, TEXT);
        sequenceName.setTypeface(Typeface.DEFAULT_BOLD);
        sequenceName.setContentDescription("Main Mode selected sequence");
        sequenceCard.addView(sequenceName, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        LinearLayout sequenceMetrics = row();
        TextView bpm = mainMetric("BPM");
        TextView bars = mainMetric("BARS");
        TextView timeSig = mainMetric("TIME SIG");
        TextView loop = mainMetric("LOOP");
        TextView start = mainMetric("START");
        TextView end = mainMetric("END");
        sequenceMetrics.addView(bpm, weight());
        sequenceMetrics.addView(bars, weight());
        sequenceMetrics.addView(timeSig, weight());
        sequenceMetrics.addView(loop, weight());
        sequenceMetrics.addView(start, weight());
        sequenceMetrics.addView(end, weight());
        sequenceCard.addView(sequenceMetrics);

        LinearLayout sequenceActions = row();
        sequenceActions.addView(
                actionButton("BPM −", v -> changeSequenceTempo(-1.0)),
                weight());
        sequenceActions.addView(
                actionButton("BPM +", v -> changeSequenceTempo(1.0)),
                weight());
        sequenceActions.addView(
                actionButton("BARS −", v -> changeSequenceBars(-1)),
                weight());
        sequenceActions.addView(
                actionButton("BARS +", v -> changeSequenceBars(1)),
                weight());
        sequenceActions.addView(
                actionButton("LOOP", v -> {
                    setBottomStatus(nativeSequenceSetLoopEnabled(
                            !nativeSequenceIsLoopEnabled()));
                    refreshMainModeState(sequenceName, bpm, bars, timeSig, loop, start, end);
                }),
                weight());
        sequenceCard.addView(sequenceActions, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout trackProgramSection = mainSection();
        trackProgramSection.setContentDescription("Main Mode Track Program section");

        LinearLayout trackProgramHeader = row();
        trackProgramHeader.addView(sectionLabelView(
                "TRACK",
                new LinearLayout.LayoutParams(dp(58), dp(32))));

        TextView trackName = mainField("TRACK");
        trackName.setTypeface(Typeface.DEFAULT_BOLD);
        trackName.setTextSize(14);
        trackName.setContentDescription("Main Mode selected track");
        trackName.setOnClickListener(v -> showTrackSelectPage());
        trackProgramHeader.addView(trackName, new LinearLayout.LayoutParams(
                0, dp(42), 1.35f));

        TextView program = mainField("PROGRAM");
        program.setContentDescription("Main Mode selected program");
        program.setOnClickListener(v -> showProgramSelectPage());
        trackProgramHeader.addView(program, new LinearLayout.LayoutParams(
                0, dp(42), 1.75f));

        trackProgramHeader.addView(actionButton(
                "BROWSER",
                v -> showBrowserPage()),
                new LinearLayout.LayoutParams(dp(84), dp(32)));
        trackProgramSection.addView(trackProgramHeader, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        trackProgramSection.addView(buildMainTrackTypeSelector(),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout trackState = row();
        TextView trackType = mainInfo("TYPE");
        TextView record = mainInfo("REC");
        TextView mute = mainInfo("MUTE");
        trackState.addView(trackType, new LinearLayout.LayoutParams(0, dp(44), 1.25f));
        trackState.addView(record, new LinearLayout.LayoutParams(0, dp(44), 0.85f));
        trackState.addView(mute, new LinearLayout.LayoutParams(0, dp(44), 0.85f));
        trackProgramSection.addView(trackState, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        /*
         * MPC Main keeps Track and Arrangement as sibling views of the same
         * Main-mode lower region. Track View is the default entry; switching
         * views changes presentation only and never changes Navigation mode or
         * the selected Track/Sequence.
         */
        LinearLayout viewSelector = row();
        TextView selectorLabel = sectionLabelView(
                "MAIN VIEW",
                new LinearLayout.LayoutParams(dp(82), dp(34)));
        viewSelector.addView(selectorLabel);

        mainTrackViewButton = actionButton("TRACK", v -> setMainTrackArrangementView(false));
        mainTrackViewButton.setContentDescription("Main Track View selector");
        viewSelector.addView(mainTrackViewButton,
                new LinearLayout.LayoutParams(0, dp(34), 1));

        mainArrangementViewButton = actionButton(
                "ARRANGEMENT",
                v -> setMainTrackArrangementView(true));
        mainArrangementViewButton.setContentDescription("Main Arrangement View selector");
        viewSelector.addView(mainArrangementViewButton,
                new LinearLayout.LayoutParams(0, dp(34), 1));
        trackProgramSection.addView(viewSelector);

        mainTrackArrangementHost = new FrameLayout(this);
        mainTrackArrangementHost.setContentDescription(
                "Main Track and Arrangement workspace");

        LinearLayout trackWorkspace = column();
        trackWorkspace.setContentDescription("Main Mode Track View workspace");
        trackWorkspace.setPadding(dp(6), dp(4), dp(6), dp(4));
        trackWorkspace.setBackground(strokeBackground(SURFACE_2, LINE, 4));

        LinearLayout trackWorkspaceHeader = row();
        trackWorkspaceHeader.addView(sectionLabelView(
                "TRACK VIEW",
                new LinearLayout.LayoutParams(0, dp(30), 1)));

        Button sampleEdit = actionButton("SAMPLE EDIT", v -> showSamplePage());
        trackWorkspaceHeader.addView(sampleEdit,
                new LinearLayout.LayoutParams(dp(104), dp(30)));

        Button trackEdit = actionButton("TRACK EDIT", v -> setBottomStatus(
                "TRACK EDIT • RESERVED / UNAVAILABLE"));
        trackEdit.setEnabled(false);
        trackEdit.setAlpha(0.45f);
        trackWorkspaceHeader.addView(trackEdit,
                new LinearLayout.LayoutParams(dp(90), dp(30)));

        trackWorkspace.addView(trackWorkspaceHeader,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        TextView trackWorkspaceInfo = label(
                "PERFORMANCE • PAD + QUICK SAMPLE",
                9, MUTED);
        trackWorkspaceInfo.setContentDescription("Main Track View guidance");
        trackWorkspace.addView(trackWorkspaceInfo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        /*
         * MPC Main exposes a compact track-state row directly above the
         * Track/Arrangement canvas. Keep the vocabulary recognizable while
         * only exposing values that our backend can state truthfully:
         * Monitor is unavailable, Length is sequence-scoped, Velocity is not
         * currently a track property, and Layer is the real selected sample
         * layer for Drum tracks.
         */
        LinearLayout trackDetailRow = row();
        TextView monitorDetail = mainMetric("MONITOR");
        monitorDetail.setContentDescription("Main Track View monitor state");
        monitorDetail.setText("MONITOR\\n—");
        trackDetailRow.addView(monitorDetail, weight());

        TextView lengthDetail = mainMetric("LENGTH");
        lengthDetail.setContentDescription("Main Track View length mode");
        lengthDetail.setText("LENGTH\\nSEQ");
        trackDetailRow.addView(lengthDetail, weight());

        TextView velocityDetail = mainMetric("VELOCITY");
        velocityDetail.setContentDescription("Main Track View velocity state");
        velocityDetail.setText("VELOCITY\\n—");
        trackDetailRow.addView(velocityDetail, weight());

        TextView layerDetail = mainMetric("LAYER");
        layerDetail.setContentDescription("Main Track View selected layer");
        layerDetail.setText(String.format(
                Locale.ROOT,
                "LAYER\\n%d/8",
                selectedLayer + 1));
        trackDetailRow.addView(layerDetail, weight());

        trackWorkspace.addView(trackDetailRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        LinearLayout quickTrack = row();

        LinearLayout padColumn = column();
        padColumn.setContentDescription("Main Track View performance pad surface");
        padColumn.addView(buildMiniMainPadGrid(),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        quickTrack.addView(padColumn,
                new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 0.52f));

        LinearLayout sampleColumn = column();
        sampleColumn.setPadding(dp(6), 0, 0, 0);
        sampleColumn.setContentDescription("Main Track View quick sample editor");

        LinearLayout sampleHeader = row();
        TextView sampleTitle = label("", 10, TEXT);
        sampleTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sampleTitle.setGravity(Gravity.CENTER_VERTICAL);
        sampleTitle.setContentDescription("Main Track View sample context");
        sampleHeader.addView(sampleTitle,
                new LinearLayout.LayoutParams(0, dp(28), 1));

        Button layerDownButton = actionButton("L−", v -> {
            selectedLayer = Math.max(0, selectedLayer - 1);
            navigationController.setSelectedLayer(selectedLayer);
            refreshMainTrackQuickSample();
        });
        layerDownButton.setContentDescription("Main Track View previous sample layer");
        sampleHeader.addView(layerDownButton,
                new LinearLayout.LayoutParams(dp(48), dp(28)));

        Button layerUpButton = actionButton("L+", v -> {
            selectedLayer = Math.min(7, selectedLayer + 1);
            navigationController.setSelectedLayer(selectedLayer);
            refreshMainTrackQuickSample();
        });
        layerUpButton.setContentDescription("Main Track View next sample layer");
        sampleHeader.addView(layerUpButton,
                new LinearLayout.LayoutParams(dp(48), dp(28)));
        sampleColumn.addView(sampleHeader,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        mainTrackWaveform = new WaveformView(this);
        mainTrackWaveform.setContentDescription(
                "Main Track View quick sample waveform");
        mainTrackWaveform.setEditable(true);
        mainTrackWaveform.setMinimumHeight(dp(110));
        mainTrackWaveform.setOnSelectionCommitListener(
                (startNormalized, endNormalized) -> commitMainTrackWaveformRegion(
                        startNormalized, endNormalized));
        sampleColumn.addView(mainTrackWaveform,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView quickSampleInfo = label("", 9, MUTED);
        quickSampleInfo.setContentDescription(
                "Main Track View quick sample info");
        quickSampleInfo.setGravity(Gravity.CENTER_VERTICAL);
        sampleColumn.addView(quickSampleInfo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        LinearLayout sampleActions = row();
        sampleActions.addView(actionButton(
                "AUDITION",
                v -> selectAndTriggerPad(selectedPadIndexForUi(), 112)),
                new LinearLayout.LayoutParams(0, dp(34), 1));
        sampleActions.addView(actionButton(
                "SAMPLE EDIT",
                v -> showSamplePage()),
                new LinearLayout.LayoutParams(0, dp(34), 1));
        sampleColumn.addView(sampleActions,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        quickTrack.addView(sampleColumn,
                new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 0.48f));

        trackWorkspace.addView(quickTrack,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout trackWorkspaceFooter = row();
        TextView selectedPad = label("", 11, TEXT);
        selectedPad.setTypeface(Typeface.DEFAULT_BOLD);
        selectedPad.setGravity(Gravity.CENTER_VERTICAL);
        selectedPad.setContentDescription("Main Track View selected pad");
        trackWorkspaceFooter.addView(selectedPad,
                new LinearLayout.LayoutParams(0, dp(34), 1));

        trackWorkspaceFooter.addView(actionButton(
                "GRID",
                v -> showSequenceGridPage()),
                new LinearLayout.LayoutParams(dp(68), dp(34)));
        trackWorkspace.addView(trackWorkspaceFooter,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        LinearLayout arrangement = column();
        arrangement.setPadding(dp(6), dp(4), dp(6), dp(4));
        arrangement.setBackground(strokeBackground(SURFACE_2, LINE, 4));
        arrangement.setContentDescription("Main Mode arrangement preview");

        LinearLayout arrangementHeader = row();
        arrangementHeader.addView(sectionLabelView(
                "ARRANGEMENT",
                new LinearLayout.LayoutParams(0, dp(30), 1)));
        arrangementHeader.addView(actionButton(
                "GRID",
                v -> showSequenceGridPage()),
                new LinearLayout.LayoutParams(dp(68), dp(30)));
        arrangement.addView(arrangementHeader);

        TextView arrangementInfo = label(
                "SEQUENCE • selected Track • playhead-aware",
                10, MUTED);
        arrangement.addView(arrangementInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));

        mainArrangementPreview = new SequenceTimelineView(this);
        mainArrangementPreview.setContentDescription("Main Mode arrangement overview");
        arrangement.addView(mainArrangementPreview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView eventSummary = label("TRACK EVENTS • —", 9, MUTED);
        eventSummary.setContentDescription("Main Mode arrangement event summary");
        arrangement.addView(eventSummary, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(18)));

        mainTrackArrangementHost.addView(trackWorkspace,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        mainTrackArrangementHost.addView(arrangement,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        trackProgramSection.addView(mainTrackArrangementHost,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        setMainTrackArrangementView(false);

        page.addView(sequenceCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.27f));
        page.addView(trackProgramSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0.73f));

        content.addView(page);
        refreshMainModeState(sequenceName, bpm, bars, timeSig, loop, start, end);
        refreshMpcToolbarState();
        refreshMainModePadVisuals();
        refreshMainTrackQuickSample();
        updateModeRailSelection();
    }

    private void commitMainTrackWaveformRegion(
            float startNormalized,
            float endNormalized) {
        final long total = nativeAudioGetPadSampleFrameCount(
                selectedPad, selectedLayer);
        if (total <= 0) {
            setBottomStatus("Quick Sample • no sample assigned");
            refreshMainTrackQuickSample();
            return;
        }

        final long start = Math.max(
                0,
                Math.min(total - 1,
                        Math.round(startNormalized * total)));
        final long end = Math.max(
                start + 1,
                Math.min(total,
                        Math.round(endNormalized * total)));
        setBottomStatus(nativeAudioSetPadSampleRegion(
                selectedPad, selectedLayer, start, end));
        refreshMainTrackQuickSample();
    }

    private void refreshMainTrackQuickSample() {
        if (mainTrackWaveform == null || !startupComplete) return;

        final long frames = nativeAudioGetPadSampleFrameCount(
                selectedPad, selectedLayer);
        final int sampleRate = nativeAudioGetPadSampleRate(
                selectedPad, selectedLayer);

        if (frames <= 0) {
            mainTrackWaveform.setPeaks(null);
            mainTrackWaveform.setSelection(0f, 1f);
            mainTrackWaveform.setDurationMs(0f);
        } else {
            mainTrackWaveform.setPeaks(
                    nativeAudioGetPadWaveformPeaks(
                            selectedPad, selectedLayer, 384));
            final long start = nativeAudioGetPadSampleRegionStart(
                    selectedPad, selectedLayer);
            final long end = nativeAudioGetPadSampleRegionEnd(
                    selectedPad, selectedLayer);
            mainTrackWaveform.setSelection(
                    start / (float) frames,
                    end / (float) frames);
            mainTrackWaveform.setDurationMs(
                    sampleRate > 0
                            ? frames * 1000.0f / sampleRate
                            : 0.0f);
        }
        mainTrackWaveform.setRecording(false);

        final String sampleName = startupComplete
                ? nativeAudioGetPadSampleName(selectedPad, selectedLayer)
                : "";
        final String displaySampleName =
                sampleName == null || sampleName.trim().isEmpty()
                        ? "NO NAME"
                        : sampleName.trim();

        final TextView info = findTextByContentDescription(
                content,
                "Main Track View quick sample info");
        if (info != null) {
            if (frames <= 0) {
                info.setText(String.format(
                        Locale.ROOT,
                        "PAD %02d • LAYER %d/8 • NO SAMPLE",
                        selectedPad + 1,
                        selectedLayer + 1));
            } else {
                info.setText(String.format(
                        Locale.ROOT,
                        "%s • %d frames • S %d • E %d",
                        displaySampleName,
                        frames,
                        nativeAudioGetPadSampleRegionStart(
                                selectedPad, selectedLayer),
                        nativeAudioGetPadSampleRegionEnd(
                                selectedPad, selectedLayer)));
            }
        }

        final TextView context = findTextByContentDescription(
                content,
                "Main Track View sample context");
        if (context != null) {
            context.setText(String.format(
                    Locale.ROOT,
                    "SAMPLE • %s • PAD %02d • LAYER %d/8",
                    displaySampleName,
                    selectedPad + 1,
                    selectedLayer + 1));
        }

        final TextView layerDetail = findTextByContentDescription(
                content,
                "Main Track View selected layer");
        if (layerDetail != null) {
            final String trackType = startupComplete
                    ? nativeSequenceGetTrackType(
                            Math.max(0, nativeSequenceGetSelectedTrack()))
                    : "DRUM";
            final boolean drum = "DRUM".equalsIgnoreCase(trackType);
            layerDetail.setText(
                    drum
                            ? String.format(
                                    Locale.ROOT,
                                    "LAYER\\n%d/8",
                                    selectedLayer + 1)
                            : "LAYER\\n—");
        }
    }

    private void setMainTrackArrangementView(boolean arrangementSelected) {
        if (mainTrackArrangementHost == null
                || mainTrackViewButton == null
                || mainArrangementViewButton == null) {
            return;
        }

        final View trackView = mainTrackArrangementHost.getChildAt(0);
        final View arrangementView = mainTrackArrangementHost.getChildAt(1);
        final boolean trackVisible = !arrangementSelected;
        trackView.setVisibility(trackVisible ? View.VISIBLE : View.GONE);
        arrangementView.setVisibility(trackVisible ? View.GONE : View.VISIBLE);

        mainTrackViewButton.setTextColor(trackVisible ? BG : TEXT);
        mainTrackViewButton.setBackground(strokeBackground(
                trackVisible ? ACCENT : SURFACE_2,
                trackVisible ? ACCENT : LINE,
                6));
        mainArrangementViewButton.setTextColor(trackVisible ? TEXT : BG);
        mainArrangementViewButton.setBackground(strokeBackground(
                trackVisible ? SURFACE_2 : ACCENT,
                trackVisible ? LINE : ACCENT,
                6));

        if (trackVisible) {
            final TextView selectedPad =
                    findTextByContentDescription(
                            mainTrackArrangementHost,
                            "Main Track View selected pad");
            if (selectedPad != null) {
                selectedPad.setText(String.format(
                        Locale.ROOT,
                        "PAD %02d • BANK %s • Track %02d",
                        selectedPadIndexForUi() + 1,
                        (char) ('A' + Math.max(
                                0,
                                Math.min(7, navigationController.state().padBank()))),
                        Math.max(0, nativeSequenceGetSelectedTrack()) + 1));
            }
            refreshMainModePadVisuals();
            refreshMainTrackQuickSample();
        } else {
            refreshMainArrangementPreview();
        }
    }

    private int selectedPadIndexForUi() {
        return Math.max(0, Math.min(15, selectedPad));
    }

    private LinearLayout mainSection() {
        LinearLayout section = column();
        section.setPadding(dp(8), dp(6), dp(8), dp(6));
        section.setBackground(strokeBackground(SURFACE, LINE, 4));
        return section;
    }

    private TextView mainMetric(String title) {
        TextView view = label("", 12, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(6), 0, dp(6), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, 4));
        view.setTag(title);
        return view;
    }

    private TextView mainField(String title) {
        TextView view = label("", 14, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(8), 0, dp(8), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, 4));
        view.setTag(title);
        return view;
    }

    private TextView mainInfo(String title) {
        TextView view = label("", 11, TEXT);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(6), 0, dp(6), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        view.setTag(title);
        return view;
    }

    private void refreshMainModeState(
            TextView sequenceName,
            TextView bpm,
            TextView bars,
            TextView timeSig,
            TextView loop,
            TextView start,
            TextView end) {
        final boolean nativeStateReady = startupComplete;
        final int sequenceIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetIndex()) : 0;
        final int trackIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final int trackCount = nativeStateReady
                ? Math.max(1, nativeSequenceGetTrackCount()) : 1;

        sequenceName.setText(String.format(
                Locale.ROOT, "Sequence %02d", sequenceIndex + 1));
        final double tempo = nativeStateReady
                ? nativeSequenceGetTempo() : 120.0;
        final int sequenceBars = nativeStateReady
                ? nativeSequenceGetBars() : 1;
        final int numerator = nativeStateReady
                ? nativeSequenceGetNumerator() : 4;
        final int denominator = nativeStateReady
                ? nativeSequenceGetDenominator() : 4;
        final boolean loopEnabled = nativeStateReady
                && nativeSequenceIsLoopEnabled();
        bpm.setText(String.format(
                Locale.ROOT, "BPM\\n%.1f", tempo));
        bars.setText(String.format(
                Locale.ROOT, "BARS\\n%d", sequenceBars));
        timeSig.setText(String.format(
                Locale.ROOT, "TIME SIG\\n%d/%d",
                numerator, denominator));
        loop.setText(
                "LOOP\\n" + (loopEnabled ? "ON" : "OFF"));
        final int loopStartBar = nativeStateReady
                ? nativeSequenceGetLoopStartBar() : 1;
        final int loopEndBar = nativeStateReady
                ? nativeSequenceGetLoopEndBar() : sequenceBars;
        start.setText(String.format(Locale.ROOT, "START\\nBAR %d", loopStartBar));
        end.setText(String.format(Locale.ROOT, "END\\nBAR %d", loopEndBar));

        trackNameRefresh(trackIndex, trackCount);
        refreshMainTrackTypeVisuals();

        if (mainArrangementPreview != null) {
            mainArrangementPreview.setBarCount(sequenceBars);
            mainArrangementPreview.setLoop(loopStartBar, loopEndBar);
            final long ticksPerBeat = Math.max(
                    1L, Math.round(
                            960.0 * 4.0 / Math.max(1, denominator)));
            final long ticksPerBar = Math.max(
                    ticksPerBeat,
                    ticksPerBeat * Math.max(1, numerator));
            final long positionTicks = nativeStateReady
                    ? nativeSequencePositionTicks() : 0L;
            final float playheadBar = 1.0f
                    + (ticksPerBar > 0
                            ? (float) positionTicks / (float) ticksPerBar
                            : 0.0f);
            mainArrangementPreview.setPlayheadBar(playheadBar);
        }

        final TextView eventSummary = findTextByContentDescription(
                content, "Main Mode arrangement event summary");
        if (eventSummary != null) {
            final String status = nativeStateReady
                    ? nativeSequenceTrackStatus(trackIndex)
                    : "events=0";
            eventSummary.setText(
                    "TRACK EVENTS • " + trackEventSummary(status));
        }
    }

    private String trackEventSummary(String status) {
        if (status == null) return "EVENTS • 0";
        final int marker = status.indexOf("events=");
        if (marker < 0) return "EVENTS • —";
        final int start = marker + "events=".length();
        int end = start;
        while (end < status.length()
                && Character.isDigit(status.charAt(end))) {
            end++;
        }
        return "EVENTS • " + status.substring(start, end);
    }

    private LinearLayout buildMainTrackTypeSelector() {
        LinearLayout row = row();
        row.setContentDescription("Main Mode track type selector");

        final String[] labels = {
                "DRUM", "KEYGROUP", "PLUGIN", "MIDI", "AUDIO", "CV"
        };
        final boolean[] available = {
                true, false, false, false, false, false
        };

        for (int i = 0; i < labels.length; i++) {
            final String type = labels[i];
            final Button b = mainInfoButton(type, "TRACKTYPE_" + type);
            b.setEnabled(available[i]);
            b.setAlpha(available[i] ? 1.0f : 0.45f);
            b.setOnClickListener(v -> setBottomStatus(
                    type + " • TRACK TYPE SELECTION RESERVED"));
            row.addView(b, new LinearLayout.LayoutParams(
                    0, dp(34), 1f));
        }
        return row;
    }

    private Button mainInfoButton(String text, String tag) {
        Button b = actionButton(text, null);
        b.setTag(tag);
        b.setTextSize(9);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        return b;
    }

    private void refreshMainArrangementPreview() {
        if (mainArrangementPreview == null || !startupComplete) {
            return;
        }
        mainArrangementPreview.setBarCount(
                Math.max(1, nativeSequenceGetBars()));
        mainArrangementPreview.setLoop(
                nativeSequenceGetLoopStartBar(),
                nativeSequenceGetLoopEndBar());
        mainArrangementPreview.setPlayheadBar(
                (float) (1.0 + nativeSequencePositionTicks()
                        / Math.max(1.0, getSequenceTicksPerBar())));
    }

    private void refreshMainTrackTypeVisuals() {
        if (content == null) return;
        final View selector = findViewWithContentDescription(
                content, "Main Mode track type selector");
        if (!(selector instanceof ViewGroup)) return;

        String active = "DRUM";
        if (startupComplete) {
            final int index = Math.max(0, nativeSequenceGetSelectedTrack());
            final String backendType = nativeSequenceGetTrackType(index);
            if (backendType != null && !backendType.trim().isEmpty()) {
                active = backendType.trim().toUpperCase(Locale.ROOT);
            }
        }

        final ViewGroup group = (ViewGroup) selector;
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (!(child instanceof Button)) continue;
            Button button = (Button) child;
            final Object tag = button.getTag();
            final String type = tag instanceof String
                    ? ((String) tag).replace("TRACKTYPE_", "") : "";
            final boolean selected = type.equals(active);
            button.setTextColor(selected ? BG : TEXT);
            button.setBackground(strokeBackground(
                    selected ? ACCENT : SURFACE_2,
                    selected ? ACCENT : LINE,
                    5));
        }
    }

    private String normalizeProgramLabel(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "—";
        }
        final String value = status.trim();
        return value.startsWith("PROGRAM • ")
                ? value.substring("PROGRAM • ".length())
                : value;
    }

    private void trackNameRefresh(
            int trackIndex,
            int trackCount) {
        View root = content;
        TextView trackName = findTextByContentDescription(
                root, "Main Mode selected track");
        TextView trackType = findTextByTag(root, "TYPE");
        TextView program = findTextByTag(root, "PROGRAM");
        TextView record = findTextByTag(root, "REC");
        TextView mute = findTextByTag(root, "MUTE");
        if (trackName == null) return;

        final String status = startupComplete && trackIndex < trackCount
                ? nativeSequenceTrackStatus(trackIndex)
                : "Track 01";
        final String backendType = startupComplete
                ? nativeSequenceGetTrackType(trackIndex)
                : "DRUM";
        trackName.setText(String.format(
                Locale.ROOT, "Track %02d  •  %s", trackIndex + 1, status));
        if (trackType != null) trackType.setText(
                "TYPE\\n" + (backendType == null || backendType.isEmpty()
                        ? "—" : backendType));
        if (program != null) {
            final String programStatus = startupComplete
                    ? nativeSequenceGetTrackProgram(trackIndex)
                    : "PROGRAM • NONE";
            program.setText("PROGRAM\\n" + normalizeProgramLabel(programStatus));
        }
        if (record != null) record.setText(
                "REC\\n" + (startupComplete && nativeSequenceIsSelectedTrackArmed()
                        ? "ARM" : "OFF"));
        if (mute != null) mute.setText(
                "MUTE\\n" + (status.toLowerCase(Locale.ROOT).contains("mute")
                        ? "ON" : "OFF"));
    }

    private TextView findTextByContentDescription(View root, String description) {
        if (root == null) return null;
        if (description.contentEquals(root.getContentDescription())
                && root instanceof TextView) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView match = findTextByContentDescription(
                        group.getChildAt(i), description);
                if (match != null) return match;
            }
        }
        return null;
    }

    private TextView findTextByTag(View root, String tag) {
        if (root == null) return null;
        if (tag.equals(root.getTag()) && root instanceof TextView) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView match = findTextByTag(group.getChildAt(i), tag);
                if (match != null) return match;
            }
        }
        return null;
    }

    private View buildMiniMainPadGrid() {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(4);
        for (int displayRow = 0; displayRow < 4; displayRow++) {
            for (int displayColumn = 0; displayColumn < 4; displayColumn++) {
                final int pad = (3 - displayRow) * 4 + displayColumn;
                Button b = button(String.format(Locale.ROOT, "%02d", pad + 1));
                b.setTextSize(11);
                b.setTypeface(Typeface.DEFAULT_BOLD);
                b.setContentDescription("Main Mode pad " + (pad + 1));
                b.setOnClickListener(v -> {
                    selectAndTriggerPad(pad, 112);
                    navigationController.setSelectedPad(pad);
                    refreshMainModePadVisuals();
                });
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = 0;
                lp.columnSpec = GridLayout.spec(displayColumn, 1f);
                lp.rowSpec = GridLayout.spec(displayRow, 1f);
                grid.addView(b, lp);
            }
        }
        return grid;
    }

    private void refreshMainModePadVisuals() {
        View root = content;
        if (root == null) return;
        for (int pad = 0; pad < 16; pad++) {
            View v = findViewWithContentDescription(
                    root, "Main Mode pad " + (pad + 1));
            if (v == null) continue;
            final boolean selected = pad == selectedPad;
            v.setBackground(strokeBackground(
                    selected ? Color.rgb(45, 72, 82) : SURFACE_2,
                    selected ? ACCENT : LINE,
                    6));
        }

        if (mainTrackArrangementHost != null) {
            final TextView selectedPadView = findTextByContentDescription(
                    mainTrackArrangementHost,
                    "Main Track View selected pad");
            if (selectedPadView != null) {
                selectedPadView.setText(String.format(
                        Locale.ROOT,
                        "PAD %02d • BANK %s • Track %02d",
                        selectedPadIndexForUi() + 1,
                        (char) ('A' + Math.max(
                                0,
                                Math.min(7, navigationController.state().padBank()))),
                        Math.max(0, nativeSequenceGetSelectedTrack()) + 1));
            }
        }
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
            navigationController.setSelectedLayer(selectedLayer);
            refreshAllInspectorState();
        });
        Button layerUp = actionButton("LAYER +", v -> {
            selectedLayer = Math.min(7, selectedLayer + 1);
            navigationController.setSelectedLayer(selectedLayer);
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SAMPLE";
        navigationController.navigate(MpcUiState.Mode.SAMPLE_EDIT);
        navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_SELECT);
        navigationController.setDataDialFocus(
                MpcUiState.DataDialFocus.SAMPLE_LAYER);
        pageTitle.setText("SAMPLE EDIT");
        content.removeAllViews();

        LinearLayout page = page();
        LinearLayout header = row();
        header.addView(sectionLabelView("PAD " + (selectedPad + 1)
                + "  •  LAYER " + (selectedLayer + 1) + "/8",
                new LinearLayout.LayoutParams(0, dp(34), 1)));
        header.addView(actionButton("EDIT", v -> {
            navigationController.setSubcontext(
                    MpcUiState.Subcontext.SAMPLE_SELECT);
            showSamplePage();
        }), new LinearLayout.LayoutParams(dp(62), dp(34)));
        header.addView(actionButton("SAMPLER", v -> {
            navigationController.navigate(MpcUiState.Mode.SAMPLER);
            showRecordPage();
        }), new LinearLayout.LayoutParams(dp(82), dp(34)));
        header.addView(actionButton("BROWSER", v -> showBrowserPage()),
                new LinearLayout.LayoutParams(dp(82), dp(34)));
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "REC";
        navigationController.navigate(MpcUiState.Mode.SAMPLER);
        navigationController.setSubcontext(MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
        pageTitle.setText("SAMPLER");
        content.removeAllViews();

        LinearLayout page = page();

        LinearLayout sampleHeader = row();
        sampleHeader.addView(sectionLabelView(
                "SAMPLE WORKSPACE  •  RECORD / MONITOR",
                new LinearLayout.LayoutParams(0, dp(34), 1)));
        sampleHeader.addView(actionButton("EDIT", v -> showSamplePage()),
                new LinearLayout.LayoutParams(dp(62), dp(34)));
        sampleHeader.addView(actionButton("BROWSER", v -> showBrowserPage()),
                new LinearLayout.LayoutParams(dp(82), dp(34)));
        page.addView(sampleHeader);

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

    private void showSequenceSelectPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "MAIN";
        hardwareFocus = 3;
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(MpcUiState.Subcontext.SEQUENCE_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
        navigationController.setActionAvailable(true);
        pageTitle.setText("MAIN • SEQUENCE");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView heading = label(
                "SEQUENCE SELECT  •  DATA DIAL / +/-",
                13, TEXT);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        final int count = startupComplete
                ? Math.max(0, nativeSequenceGetCount()) : 0;
        final int selected = startupComplete
                ? Math.max(0, nativeSequenceGetIndex()) : 0;

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();
        list.setContentDescription("Main Sequence Select list");

        if (count == 0) {
            list.addView(label("NO SEQUENCES", 12, MUTED));
        } else {
            for (int i = 0; i < count; i++) {
                final int index = i;
                final double tempo = nativeSequenceGetIndex() == i
                        ? nativeSequenceGetTempo() : 0.0;
                final String summary = startupComplete
                        ? String.format(
                                Locale.ROOT,
                                "%02d  SEQUENCE %02d  •  %d BAR%s  •  %s",
                                i + 1,
                                i + 1,
                                i == selected
                                        ? nativeSequenceGetBars()
                                        : 0,
                                i == selected
                                        && nativeSequenceGetBars() == 1
                                        ? "" : "S",
                                i == selected
                                        ? String.format(
                                                Locale.ROOT,
                                                "%.1f BPM",
                                                tempo)
                                        : "SELECT")
                        : String.format(Locale.ROOT, "%02d  SEQUENCE %02d",
                                i + 1, i + 1);
                Button button = actionButton(summary, v -> {
                    setBottomStatus(nativeSequenceSelect(index));
                    navigationController.setSelectedSequence(index);
                    showSequenceSelectPage();
                });
                button.setGravity(Gravity.CENTER_VERTICAL);
                button.setPadding(dp(10), 0, dp(10), 0);
                button.setBackground(strokeBackground(
                        i == selected
                                ? Color.rgb(42, 66, 76) : SURFACE_2,
                        i == selected ? ACCENT : LINE,
                        7));
                list.addView(button, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            }
        }

        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton(
                "BACK MAIN",
                v -> showMainPage()), weight());
        footer.addView(actionButton(
                "SEQUENCE EDIT",
                v -> showSequencePage()), weight());
        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        content.addView(page);
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void showTrackSelectPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "MAIN";
        hardwareFocus = 2;
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.setActionAvailable(true);
        pageTitle.setText("MAIN • TRACK");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView heading = label(
                "TRACK SELECT  •  DATA DIAL / +/-",
                13, TEXT);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        final int count = startupComplete
                ? Math.max(0, nativeSequenceGetTrackCount()) : 0;
        final int selected = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = column();
        list.setContentDescription("Main Track Select list");

        if (count == 0) {
            list.addView(label("NO TRACKS", 12, MUTED));
        } else {
            for (int i = 0; i < count; i++) {
                final int trackIndex = i;
                final String status = startupComplete
                        ? nativeSequenceTrackStatus(i)
                        : "DRUM  TRACK " + (i + 1);
                Button button = actionButton(
                        String.format(
                                Locale.ROOT,
                                "%02d  %s  •  %s%s%s",
                                i + 1,
                                status,
                                nativeSequenceGetTrackType(i),
                                i == selected && startupComplete
                                        ? "  • CURRENT" : "",
                                i == selected && startupComplete
                                        && nativeSequenceIsTrackMuted(i)
                                        ? "  • M" : ""),
                        v -> {
                            setBottomStatus(nativeSequenceSelectTrack(trackIndex));
                            navigationController.setSelectedTrack(trackIndex);
                            showTrackSelectPage();
                        });
                button.setGravity(Gravity.CENTER_VERTICAL);
                button.setPadding(dp(10), 0, dp(10), 0);
                button.setBackground(strokeBackground(
                        i == selected
                                ? Color.rgb(42, 66, 76) : SURFACE_2,
                        i == selected ? ACCENT : LINE,
                        7));
                list.addView(button, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            }
        }

        scroll.addView(list);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton(
                "BACK MAIN",
                v -> showMainPage()), weight());
        footer.addView(actionButton(
                "TRACK VIEW",
                v -> showTrackViewPage()), weight());
        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        content.addView(page);
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void showProgramSelectPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);

        currentPage = "MAIN";
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(MpcUiState.Subcontext.PROGRAM_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PROGRAM);
        hardwareFocus = 4;
        pageTitle.setText("MAIN • PROGRAM");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView heading = label(
                String.format(
                        Locale.ROOT,
                        "PROGRAM SELECT  •  TRACK %02d",
                        Math.max(0, nativeSequenceGetSelectedTrack()) + 1),
                13, TEXT);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(heading, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        final int selectedTrack = Math.max(
                0, nativeSequenceGetSelectedTrack());
        final String selectedTrackType = nativeSequenceGetTrackType(selectedTrack);
        final boolean drumTrack =
                "DRUM".equalsIgnoreCase(selectedTrackType);
        navigationController.setActionAvailable(drumTrack);

        final int programCount = drumTrack
                ? Math.max(0, nativeSequenceGetDrumProgramCount())
                : 0;
        final int selectedProgram = drumTrack
                ? nativeSequenceGetTrackProgramIndex(selectedTrack)
                : -1;

        if (!drumTrack) {
            page.addView(label(
                    "PROGRAM SELECT UNAVAILABLE • "
                            + selectedTrackType + " TRACK",
                    12, MUTED));
            page.addView(label(
                    "Select a Drum Track to choose a Drum Program.",
                    11, MUTED));
        } else if (programCount == 0) {
            page.addView(label(
                    "NO DRUM PROGRAMS",
                    12, MUTED));
        } else {
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = column();
            list.setContentDescription("Main Program Select list");
            for (int i = 0; i < programCount; i++) {
                final int programIndex = i;
                Button b = actionButton(
                        String.format(
                                Locale.ROOT,
                                "%02d  %s%s",
                                i + 1,
                                nativeSequenceGetDrumProgramName(i),
                                i == selectedProgram ? "  • CURRENT" : ""),
                        v -> {
                            setBottomStatus(nativeSequenceSetTrackProgram(
                                    selectedTrack,
                                    programIndex));
                            navigationController.setSelectedProgram(
                                    nativeSequenceGetDrumProgramName(programIndex));
                            showProgramSelectPage();
                        });
                b.setGravity(Gravity.CENTER_VERTICAL);
                b.setPadding(dp(10), 0, dp(10), 0);
                b.setBackground(strokeBackground(
                        i == selectedProgram ? Color.rgb(42, 66, 76) : SURFACE_2,
                        i == selectedProgram ? ACCENT : LINE,
                        7));
                list.addView(b, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            }
            scroll.addView(list);
            page.addView(scroll, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }

        LinearLayout footer = row();
        footer.addView(actionButton(
                "BACK MAIN",
                v -> showMainPage()), weight());
        footer.addView(actionButton(
                "PROGRAM EDIT",
                v -> {
                    setBottomStatus(
                            "PROGRAM EDIT • RESERVED until per-program editor");
                }), weight());
        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        content.addView(page);
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void showBrowserPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "BROWSE";
        navigationController.navigate(MpcUiState.Mode.BROWSER);
        navigationController.setSubcontext(MpcUiState.Subcontext.BROWSER);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.BROWSER_ITEM);
        pageTitle.setText("BROWSER");
        content.removeAllViews();

        browserView = new MpcBrowserView(this);
        browserView.setListener(new MpcBrowserView.Listener() {
            @Override
            public void onSectionSelected(String section) {
                navigationController.setBrowser(
                        section, navigationController.state().browserFilter(),
                        navigationController.state().browserSearch());
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.BROWSER_ITEM);
                setBottomStatus("BROWSER • " + section);
            }

            @Override
            public void onFilterSelected(String filter) {
                navigationController.setBrowser(
                        navigationController.state().browserLocation(),
                        filter,
                        browserView.searchQuery());
                setBottomStatus("FILTER • " + filter);
            }

            @Override
            public void onOpenStorage() {
                openWavPicker();
            }

            @Override
            public void onPlayCurrent() {
                selectAndTriggerPad(selectedPad, 112);
                setBottomStatus(String.format(
                        Locale.ROOT,
                        "PLAY CURRENT • PAD %02d / LAYER %02d",
                        selectedPad + 1, selectedLayer + 1));
            }

            @Override
            public void onSearchChanged(String query) {
                navigationController.setBrowser(
                        navigationController.state().browserLocation(),
                        navigationController.state().browserFilter(),
                        query);
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.BROWSER_ITEM);
            }
        });

        LinearLayout page = page();
        page.setPadding(dp(5), dp(4), dp(5), dp(2));
        page.addView(browserView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        final String location = navigationController.state().browserLocation();
        final String filter = navigationController.state().browserFilter();
        final String search = navigationController.state().browserSearch();
        browserView.setLocation(location.isEmpty() ? "INTERNAL" : location);
        browserView.setFilter(filter.isEmpty() ? "ALL" : filter);
        browserView.setSearch(search);

        if (sampleInfo == null) {
            sampleInfo = label("", 12, TEXT);
        }
        if (startupComplete) {
            final long frames = nativeAudioGetPadSampleFrameCount(
                    selectedPad, selectedLayer);
            final String sampleName = nativeAudioGetPadSampleName(
                    selectedPad, selectedLayer);
            browserView.setCurrentSampleName(
                    frames > 0
                            ? (sampleName == null || sampleName.trim().isEmpty()
                                    ? "ASSIGNED"
                                    : sampleName)
                            : "NONE");
        } else {
            browserView.setCurrentSampleName("NONE");
        }
        browserView.setTarget(selectedPad, selectedLayer);

        content.addView(page);
        updateModeRailSelection();
    }

    private void showArrangePage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "ARRANGE";
        navigationController.navigate(MpcUiState.Mode.ARRANGE);
        navigationController.setSubcontext(MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TIMELINE);
        navigationController.setZoomFocus(MpcUiState.ZoomFocus.HORIZONTAL);
        pageTitle.setText("ARRANGE");
        content.removeAllViews();
        arrangementView = new MpcArrangeView(this);
        arrangementView.setListener(new MpcArrangeView.Listener() {
            @Override
            public void onTrackSelected(int trackIndex) {
                setBottomStatus(nativeSequenceSelectTrack(trackIndex));
                navigationController.setSelectedTrack(trackIndex);
                loadArrangementLanes();
                refreshArrangeView();
            }

            @Override
            public void onEventDoubleTapped(int trackIndex, long tick) {
                setBottomStatus(nativeSequenceSelectTrack(trackIndex));
                navigationController.setSelectedTrack(trackIndex);
                showSequenceGridPage();
            }

            @Override
            public void onLoopCommitted(int startBar, int endBar) {
                setBottomStatus(nativeSequenceSetLoopBars(startBar, endBar));
                refreshArrangeView();
            }
        });

        LinearLayout page = page();
        page.setPadding(dp(6), dp(5), dp(6), dp(2));

        LinearLayout header = row();
        header.addView(sectionLabelView(
                "ARRANGEMENT",
                new LinearLayout.LayoutParams(0, dp(34), 1)));
        header.addView(actionButton(
                "ZOOM −",
                v -> {
                    arrangementView.zoomOut();
                    refreshArrangeView();
                }),
                new LinearLayout.LayoutParams(dp(72), dp(34)));
        header.addView(actionButton(
                "ZOOM +",
                v -> {
                    arrangementView.zoomIn();
                    refreshArrangeView();
                }),
                new LinearLayout.LayoutParams(dp(72), dp(34)));
        header.addView(actionButton(
                "RESET",
                v -> {
                    arrangementView.resetZoom();
                    refreshArrangeView();
                }),
                new LinearLayout.LayoutParams(dp(66), dp(34)));
        page.addView(header);

        TextView info = label(
                "LOOP BRACE edits Sequence loop • event placement is read-only in the current pattern model • double-tap an event for Grid",
                9, MUTED);
        info.setContentDescription("Arrangement editing guidance");
        page.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        page.addView(arrangementView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        content.addView(page);
        loadArrangementLanes();
        refreshArrangeView();
        updateModeRailSelection();
    }

    private void loadArrangementLanes() {
        if (arrangementView == null || !startupComplete) {
            return;
        }

        final int trackCount = Math.max(0, nativeSequenceGetTrackCount());
        final int selected = Math.max(0, nativeSequenceGetSelectedTrack());
        arrangementLanes.clear();

        for (int trackIndex = 0; trackIndex < trackCount; trackIndex++) {
            final String status = nativeSequenceTrackStatus(trackIndex);
            final String type = nativeSequenceGetTrackType(trackIndex);
            final String program = nativeSequenceGetTrackProgram(trackIndex)
                    .replace("PROGRAM • ", "");
            final boolean muted = nativeSequenceIsTrackMuted(trackIndex);
            final boolean armed = trackIndex == selected
                    && nativeSequenceIsSelectedTrackArmed();

            String data = nativeSequenceGetTrackArrangementData(trackIndex);
            int separator = data == null ? -1 : data.indexOf('|');
            int lengthTicks = 1;
            String eventData = "";
            if (separator >= 0) {
                try {
                    lengthTicks = Math.max(
                            1, Integer.parseInt(data.substring(0, separator)));
                } catch (NumberFormatException ignored) {
                    lengthTicks = 1;
                }
                eventData = data.substring(separator + 1);
            }

            arrangementLanes.add(new MpcArrangeView.Lane(
                    trackIndex,
                    arrangementTrackName(status, trackIndex),
                    type,
                    program,
                    muted,
                    armed,
                    lengthTicks,
                    MpcArrangeView.decodeEventData(eventData)));
        }

        arrangementView.setLanes(arrangementLanes, selected);
    }

    private String arrangementTrackName(String status, int trackIndex) {
        if (status == null || status.isEmpty()) {
            return "Track " + (trackIndex + 1);
        }
        final int separator = status.indexOf("  |");
        final String withoutEvents = separator >= 0
                ? status.substring(0, separator) : status;
        final int kindSeparator = withoutEvents.indexOf("  ");
        if (kindSeparator >= 0 && kindSeparator + 2 < withoutEvents.length()) {
            return withoutEvents.substring(kindSeparator + 2).trim();
        }
        return withoutEvents.trim();
    }

    private void refreshArrangeView() {
        if (arrangementView == null || !startupComplete) {
            return;
        }

        if (arrangementLanes.isEmpty() && nativeSequenceGetTrackCount() > 0) {
            loadArrangementLanes();
        }

        final int bars = Math.max(1, nativeSequenceGetBars());
        arrangementView.setTimeline(
                bars,
                nativeSequenceGetLoopStartBar(),
                nativeSequenceGetLoopEndBar(),
                nativeSequenceGetNumerator(),
                nativeSequenceGetDenominator(),
                nativeSequencePositionTicks(),
                nativeSequenceIsPlaying());
    }

    private void showTrackViewPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "TRACK_VIEW";
        hardwareFocus = 2;
        navigationController.navigate(MpcUiState.Mode.TRACK_VIEW);
        navigationController.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.setActionAvailable(true);
        pageTitle.setText("TRACK VIEW");
        content.removeAllViews();

        LinearLayout page = page();
        page.setContentDescription("MPC Track View workspace");
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        LinearLayout header = row();
        header.addView(sectionLabelView(
                "SEQUENCE • TRACK VIEW",
                new LinearLayout.LayoutParams(0, dp(34), 1)));
        header.addView(actionButton("PREV", v -> {
            setBottomStatus(nativeSequencePrevious());
            showTrackViewPage();
        }), new LinearLayout.LayoutParams(dp(66), dp(34)));
        header.addView(actionButton("NEXT", v -> {
            setBottomStatus(nativeSequenceNext());
            showTrackViewPage();
        }), new LinearLayout.LayoutParams(dp(66), dp(34)));
        header.addView(actionButton(
                "ARRANGE",
                v -> showArrangePage()),
                new LinearLayout.LayoutParams(dp(90), dp(34)));
        page.addView(header);

        ScrollView scroll = new ScrollView(this);
        LinearLayout strips = column();
        strips.setPadding(0, dp(4), 0, dp(4));
        scroll.addView(strips);

        final int count = startupComplete
                ? nativeSequenceGetTrackCount() : 0;
        final int selected = startupComplete
                ? nativeSequenceGetSelectedTrack() : 0;

        if (count == 0) {
            strips.addView(label(
                    "NO TRACKS • NEW TRACK is available in the shell Function Bar",
                    12, MUTED));
        } else {
            for (int i = 0; i < count; i++) {
                final int trackIndex = i;
                final boolean isSelected = i == selected;
                final boolean muted = nativeSequenceIsTrackMuted(trackIndex);
                final boolean armed = isSelected
                        && nativeSequenceIsSelectedTrackArmed();

                LinearLayout strip = row();
                strip.setPadding(dp(7), dp(4), dp(7), dp(4));
                strip.setGravity(Gravity.CENTER_VERTICAL);
                strip.setContentDescription(
                        "Track View track " + (i + 1));
                strip.setBackground(strokeBackground(
                        isSelected
                                ? Color.rgb(42, 66, 76) : SURFACE_2,
                        isSelected ? ACCENT : LINE,
                        7));
                strip.setOnClickListener(v -> {
                    setBottomStatus(nativeSequenceSelectTrack(trackIndex));
                    navigationController.setSelectedTrack(trackIndex);
                    showTrackViewPage();
                });

                TextView name = label(
                        String.format(
                                Locale.ROOT,
                                "%02d  %s",
                                i + 1,
                                nativeSequenceTrackStatus(trackIndex)),
                        12, TEXT);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                name.setGravity(Gravity.CENTER_VERTICAL);
                strip.addView(name, new LinearLayout.LayoutParams(
                        0, dp(52), 1.75f));

                TextView type = label(
                        nativeSequenceGetTrackType(trackIndex),
                        9, MUTED);
                type.setGravity(Gravity.CENTER_VERTICAL);
                type.setTypeface(Typeface.DEFAULT_BOLD);
                strip.addView(type, new LinearLayout.LayoutParams(
                        0, dp(52), 0.75f));

                String programName = nativeSequenceGetTrackProgram(trackIndex);
                if (programName == null || programName.trim().isEmpty()) {
                    programName = "—";
                } else {
                    programName = programName.replace("PROGRAM • ", "");
                }
                TextView program = label(
                        programName,
                        9, MUTED);
                program.setGravity(Gravity.CENTER_VERTICAL);
                strip.addView(program, new LinearLayout.LayoutParams(
                        0, dp(52), 1.15f));

                TextView events = label(
                        trackEventSummary(nativeSequenceTrackStatus(trackIndex)),
                        9, MUTED);
                events.setGravity(Gravity.CENTER_VERTICAL);
                strip.addView(events, new LinearLayout.LayoutParams(
                        0, dp(52), 0.75f));

                TextView rec = label(
                        armed ? "REC" : "R",
                        9,
                        armed ? DANGER : MUTED);
                rec.setGravity(Gravity.CENTER);
                rec.setTypeface(Typeface.DEFAULT_BOLD);
                rec.setBackground(strokeBackground(
                        armed ? Color.rgb(104, 64, 64) : Color.TRANSPARENT,
                        armed ? DANGER : LINE,
                        6));
                strip.addView(rec, new LinearLayout.LayoutParams(
                        dp(46), dp(40)));

                TextView mute = label(
                        muted ? "MUTE" : "M",
                        9,
                        muted ? ACTIVE : MUTED);
                mute.setGravity(Gravity.CENTER);
                mute.setTypeface(Typeface.DEFAULT_BOLD);
                mute.setBackground(strokeBackground(
                        muted ? Color.rgb(74, 124, 88) : Color.TRANSPARENT,
                        muted ? ACTIVE : LINE,
                        6));
                strip.addView(mute, new LinearLayout.LayoutParams(
                        dp(58), dp(40)));

                TextView solo = label("SOLO", 8, MUTED);
                solo.setGravity(Gravity.CENTER);
                solo.setTypeface(Typeface.DEFAULT_BOLD);
                solo.setAlpha(0.48f);
                solo.setContentDescription(
                        "Track View solo unavailable");
                strip.addView(solo, new LinearLayout.LayoutParams(
                        dp(50), dp(40)));

                strips.addView(strip, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(62)));
            }
        }

        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = label(
                "SELECTED TRACK • use the shell Function Bar for REC ARM / TRACK − / TRACK + / MUTE / SOLO",
                9, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(dp(8), 0, dp(8), 0);
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        content.addView(page);
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void showSequencePage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        navigationController.navigate(MpcUiState.Mode.TRACK_VIEW);
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        navigationController.navigate(MpcUiState.Mode.GRID);
        navigationController.setSubcontext(MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PAD);
        final int gridTrack = Math.max(0, nativeSequenceGetSelectedTrack());
        final String gridTrackType = nativeSequenceGetTrackType(gridTrack);
        final boolean drumGrid = "DRUM".equalsIgnoreCase(gridTrackType);
        navigationController.setActionAvailable(drumGrid
                && nativeSequenceIsGridEditable());
        pageTitle.setText("GRID");
        content.removeAllViews();
        sequenceTimeline = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();
        page.setContentDescription("MPC Grid View workspace");
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        // Grid header: context first, navigation second, no local "page" chrome.
        LinearLayout header = row();
        TextView contextTitle = label("GRID", 13, TEXT);
        contextTitle.setTypeface(Typeface.DEFAULT_BOLD);
        contextTitle.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(contextTitle, new LinearLayout.LayoutParams(dp(58), dp(38)));

        TextView trackContext = label("", 10, MUTED);
        trackContext.setGravity(Gravity.CENTER_VERTICAL);
        trackContext.setPadding(dp(8), 0, dp(8), 0);
        header.addView(trackContext, new LinearLayout.LayoutParams(0, dp(38), 1.7f));

        TextView sequenceContext = label("", 10, MUTED);
        sequenceContext.setGravity(Gravity.CENTER);
        sequenceContext.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        header.addView(sequenceContext, new LinearLayout.LayoutParams(dp(126), dp(34)));

        gridSelectionStateView = label("", 10, TEXT);
        gridSelectionStateView.setGravity(Gravity.CENTER);
        gridSelectionStateView.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        header.addView(gridSelectionStateView, new LinearLayout.LayoutParams(dp(116), dp(34)));

        header.addView(actionButton("◀", v -> moveSequenceGridPage(-1)),
                new LinearLayout.LayoutParams(dp(50), dp(38)));
        header.addView(actionButton("▶", v -> moveSequenceGridPage(1)),
                new LinearLayout.LayoutParams(dp(50), dp(38)));
        page.addView(header);

        // Tool row mirrors the contextual editing concept while only exposing
        // operations that this backend can perform truthfully.
        LinearLayout toolRow = row();
        Button drawTool = actionButton("DRAW", v -> {
            navigationController.setEditorTool(MpcUiState.EditorTool.DRAW);
            refreshGridToolState();
        });
        drawTool.setContentDescription("Grid Draw tool");
        toolRow.addView(drawTool, weight());

        Button eraseTool = actionButton("ERASE", v -> {
            navigationController.setEditorTool(MpcUiState.EditorTool.ERASE);
            refreshGridToolState();
        });
        eraseTool.setContentDescription("Grid Erase tool");
        toolRow.addView(eraseTool, weight());

        Button selectTool = actionButton("SELECT", v -> {
            navigationController.setEditorTool(MpcUiState.EditorTool.SELECT);
            refreshGridToolState();
        });
        selectTool.setContentDescription("Grid Select tool");
        toolRow.addView(selectTool, weight());

        Button magnifyTool = actionButton("MAGNIFY", v -> {
            navigationController.setEditorTool(MpcUiState.EditorTool.MAGNIFY);
            refreshGridToolState();
        });
        magnifyTool.setContentDescription("Grid Navigation tool");
        toolRow.addView(magnifyTool, weight());

        gridToolStateView = label("", 9, ACCENT);
        gridToolStateView.setGravity(Gravity.CENTER);
        toolRow.addView(gridToolStateView, new LinearLayout.LayoutParams(0, dp(42), 1.8f));
        page.addView(toolRow, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        TextView info = label("", 10, MUTED);
        info.setGravity(Gravity.CENTER_VERTICAL);
        info.setPadding(dp(8), 0, dp(8), 0);
        page.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        if (!drumGrid) {
            LinearLayout unavailable = panel();
            unavailable.setContentDescription("MPC Grid View unavailable track type");
            TextView unavailableTitle = label(
                    "GRID • " + gridTrackType + " TRACK",
                    16, TEXT);
            unavailableTitle.setTypeface(Typeface.DEFAULT_BOLD);
            unavailableTitle.setGravity(Gravity.CENTER);
            unavailable.addView(unavailableTitle,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

            TextView unavailableBody = label(
                    "This Track Type requires its dedicated MPC Grid renderer. "
                            + "Drum Grid is not substituted here.",
                    11, MUTED);
            unavailableBody.setGravity(Gravity.CENTER);
            unavailable.addView(unavailableBody,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

            page.addView(unavailable,
                    new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

            content.addView(page);
            refreshMpcCompactContext();
            refreshMpcFunctionBar();
            updateModeRailSelection();
            return;
        }

        sequenceGridView = new SequenceGridView(this);
        sequenceGridView.setContentDescription("MPC Grid View drum event grid");
        sequenceGridView.setListener((padIndex, absoluteStep) -> {
            final int gridTicks = Math.max(1, nativeSequenceGetQuantizeGrid());
            final MpcUiState.EditorTool tool =
                    navigationController.state().editorTool();

            if (tool == MpcUiState.EditorTool.MAGNIFY) {
                setBottomStatus("GRID • use two-finger pinch or hardware ZOOM");
                return;
            }

            if (tool == MpcUiState.EditorTool.SELECT) {
                selectedPad = padIndex;
                navigationController.setSelectedPad(padIndex);
                gridSelectionStateView.setText(
                        String.format(
                                Locale.ROOT,
                                "PAD %02d • STEP %02d",
                                padIndex + 1,
                                absoluteStep + 1));
                refreshMpcCompactContext();
                sequenceGridView.invalidate();
                setBottomStatus(
                        "GRID SELECT • PAD "
                                + (padIndex + 1)
                                + " • STEP "
                                + (absoluteStep + 1));
                return;
            }

            final int[] velocities =
                    nativeSequenceGetGridVelocities(
                            sequenceGridStartStep,
                            gridTicks);
            final int localColumn = absoluteStep - sequenceGridStartStep;
            final int localIndex = padIndex * 16 + localColumn;
            final boolean occupied = localColumn >= 0
                    && localColumn < 16
                    && localIndex >= 0
                    && localIndex < velocities.length
                    && velocities[localIndex] > 0;

            if (tool == MpcUiState.EditorTool.DRAW && occupied) {
                setBottomStatus("GRID DRAW • NOTE ALREADY EXISTS");
            } else if (tool == MpcUiState.EditorTool.ERASE && !occupied) {
                setBottomStatus("GRID ERASE • NO NOTE");
            } else {
                final String result = nativeSequenceToggleGridStep(
                        padIndex, absoluteStep, gridTicks);
                setBottomStatus(result);
            }

            selectedPad = padIndex;
            navigationController.setSelectedPad(padIndex);
            navigationController.setDataDialFocus(
                    tool == MpcUiState.EditorTool.ERASE
                            ? MpcUiState.DataDialFocus.PAD
                            : MpcUiState.DataDialFocus.PAD);
            gridSelectionStateView.setText(
                    String.format(
                            Locale.ROOT,
                            "PAD %02d • STEP %02d",
                            padIndex + 1,
                            absoluteStep + 1));
            refreshSequenceGrid();
            refreshMpcCompactContext();
        });

        sequenceGridView.setViewportListener(
                (firstStep, visibleSteps, firstPad, visiblePads) -> {
                    sequenceGridStartStep = firstStep;
                    sequenceGridVisibleSteps = visibleSteps;
                    sequenceGridStartPad = firstPad;
                    sequenceGridVisiblePads = visiblePads;
                    refreshSequenceGrid();
                });

        page.addView(sequenceGridView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        TextView resolution = label("", 10, TEXT);
        resolution.setGravity(Gravity.CENTER);
        resolution.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        footer.addView(resolution, new LinearLayout.LayoutParams(dp(100), dp(38)));

        TextView editState = label("", 10, TEXT);
        editState.setGravity(Gravity.CENTER);
        editState.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        footer.addView(editState, new LinearLayout.LayoutParams(dp(130), dp(38)));

        TextView range = label("", 10, MUTED);
        range.setGravity(Gravity.CENTER_VERTICAL);
        range.setPadding(dp(8), 0, dp(8), 0);
        footer.addView(range, new LinearLayout.LayoutParams(0, dp(38), 1));

        TextView hint = label(
                "TAP = DRAW/ERASE   •   SELECT = FOCUS   •   MAGNIFY = NAVIGATION",
                9, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        footer.addView(hint, new LinearLayout.LayoutParams(dp(310), dp(38)));
        page.addView(footer);

        content.addView(page);

        final int totalSteps = sequenceGridTotalSteps();
        final int first = sequenceGridStartStep + 1;
        final int last = Math.min(
                totalSteps,
                sequenceGridStartStep + sequenceGridVisibleSteps);
        sequenceContext.setText(String.format(
                Locale.ROOT,
                "SEQ %02d  •  %d/%d",
                nativeSequenceGetIndex() + 1,
                nativeSequenceGetBars(),
                nativeSequenceGetCount()));
        trackContext.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s",
                gridTrack + 1,
                nativeSequenceTrackStatus(gridTrack)));
        range.setText(String.format(
                Locale.ROOT,
                "STEPS %02d–%02d / %02d",
                first, last, totalSteps));
        resolution.setText(sequenceGridLabel(
                nativeSequenceGetQuantizeGrid()));
        editState.setText(
                nativeSequenceIsGridEditable()
                        ? "EDIT READY"
                        : "STOP TO EDIT");
        gridSelectionStateView.setText(
                String.format(
                        Locale.ROOT,
                        "PAD %02d",
                        selectedPad + 1));
        refreshGridToolState();
        refreshSequenceGrid();
        refreshSequencePlayhead();
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private boolean drumGridAvailable() {
        if (!startupComplete) {
            return false;
        }
        final int track = Math.max(0, nativeSequenceGetSelectedTrack());
        return "DRUM".equalsIgnoreCase(nativeSequenceGetTrackType(track));
    }

    private void refreshGridToolState() {
        if (gridToolStateView == null || navigationController == null) {
            return;
        }
        final MpcUiState state = navigationController.state();
        final String tool = state.editorTool().name();
        gridToolStateView.setText(
                "TOOL • " + tool
                        + "  •  DIAL "
                        + state.dataDialFocus().name().replace('_', ' '));
        gridToolStateView.setTextColor(
                state.actionAvailable() ? ACCENT : DANGER);
    }

    private void showSequenceLauncherPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        launcherBank = Math.max(0, nativeSequenceGetIndex() / 16);
        currentPage = "SEQ";
        navigationController.navigate(MpcUiState.Mode.NEXT_SEQUENCE);
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
        clearSequenceLauncherLeds();
        nativeSequenceSetStepEditContext(true, sequenceStepPage);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "SEQ";
        navigationController.navigate(MpcUiState.Mode.STEP);
        navigationController.setSubcontext(MpcUiState.Subcontext.STEP_EDIT);
        final int stepTrack = Math.max(0, nativeSequenceGetSelectedTrack());
        final String stepTrackType = nativeSequenceGetTrackType(stepTrack);
        final boolean stepSupported =
                "DRUM".equalsIgnoreCase(stepTrackType);
        final boolean stepEditorAvailable =
                stepSupported && nativeSequenceIsGridEditable();
        navigationController.setActionAvailable(stepEditorAvailable);
        navigationController.setDataDialFocus(
                MpcUiState.DataDialFocus.STEP);
        pageTitle.setText("STEP");
        content.removeAllViews();
        sequenceTimeline = null;
        sequenceGridView = null;
        sequenceLauncherView = null;
        for (int i = 0; i < sequenceStepButtons.length; i++) {
            sequenceStepButtons[i] = null;
        }

        LinearLayout page = page();
        page.setContentDescription("MPC Step Sequencer workspace");

        if (!stepSupported) {
            TextView unavailable = label(
                    "STEP SEQUENCER • " + stepTrackType + " TRACK",
                    16, TEXT);
            unavailable.setTypeface(Typeface.DEFAULT_BOLD);
            unavailable.setGravity(Gravity.CENTER);
            page.addView(unavailable, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

            TextView message = label(
                    "The current Step backend is Drum Track only. "
                            + "Select a Drum Track to program 16 steps.",
                    11, MUTED);
            message.setGravity(Gravity.CENTER);
            page.addView(message, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

            content.addView(page);
            refreshMpcCompactContext();
            refreshMpcFunctionBar();
            updateModeRailSelection();
            return;
        }

        LinearLayout header = row();
        TextView title = label("STEP", 13, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(dp(58), dp(38)));

        TextView trackContext = label("", 10, MUTED);
        trackContext.setGravity(Gravity.CENTER_VERTICAL);
        trackContext.setPadding(dp(8), 0, dp(8), 0);
        header.addView(trackContext, new LinearLayout.LayoutParams(0, dp(38), 1.6f));

        TextView padInfo = label("", 10, TEXT);
        padInfo.setTypeface(Typeface.DEFAULT_BOLD);
        padInfo.setGravity(Gravity.CENTER);
        padInfo.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        header.addView(padInfo, new LinearLayout.LayoutParams(dp(88), dp(34)));

        header.addView(actionButton("PAD −", v -> {
            selectedPad = Math.max(0, selectedPad - 1);
            navigationController.setSelectedPad(selectedPad);
            refreshSequenceStepPage();
            refreshMpcCompactContext();
        }), new LinearLayout.LayoutParams(dp(62), dp(38)));
        header.addView(actionButton("PAD +", v -> {
            selectedPad = Math.min(15, selectedPad + 1);
            navigationController.setSelectedPad(selectedPad);
            refreshSequenceStepPage();
            refreshMpcCompactContext();
        }), new LinearLayout.LayoutParams(dp(62), dp(38)));
        header.addView(actionButton("◀", v -> moveSequenceStepPage(-1)),
                new LinearLayout.LayoutParams(dp(50), dp(38)));
        header.addView(actionButton("▶", v -> moveSequenceStepPage(1)),
                new LinearLayout.LayoutParams(dp(50), dp(38)));
        page.addView(header);

        sequenceStepEventInfo = label(
                "STEP —  •  VEL —  •  PROB —  •  RAT —  •  NUDGE —",
                10,
                TEXT);
        sequenceStepEventInfo.setGravity(Gravity.CENTER_VERTICAL);
        sequenceStepEventInfo.setPadding(dp(10), 0, dp(10), 0);
        sequenceStepEventInfo.setBackground(strokeBackground(SURFACE_2, LINE, 6));
        sequenceStepEventInfo.setContentDescription(
                "Step Edit event information");
        page.addView(sequenceStepEventInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        TextView toolInfo = label(
                "STEP PARAMETER • Data Dial / +/− edit the focused event field",
                9, MUTED);
        toolInfo.setGravity(Gravity.CENTER_VERTICAL);
        toolInfo.setPadding(dp(10), 0, dp(10), 0);
        page.addView(toolInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

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
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.STEP);
                refreshSequenceStepPage();
                refreshMpcCompactContext();
            });
            button.setOnLongClickListener(v -> {
                selectedSequenceStep =
                        sequenceStepPage * SEQUENCE_GRID_PAGE_STEPS + step;
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.STEP);
                setBottomStatus(
                        "STEP " + (selectedSequenceStep + 1) + " SELECTED");
                refreshSequenceStepPage();
                return true;
            });
            sequenceStepButtons[i] = button;
            steps.addView(button, new LinearLayout.LayoutParams(
                    0, dp(96), 1));
        }
        page.addView(steps, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView hint = label(
                "16 STEPS • TAP = ADD/REMOVE • LONG-PRESS = SELECT • PARAM = CYCLE VEL/PROB/RAT/NUDGE/DUR",
                9, MUTED);
        hint.setGravity(Gravity.CENTER_VERTICAL);
        hint.setPadding(dp(10), 0, dp(10), 0);
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(30)));

        content.addView(page);

        trackContext.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s",
                nativeSequenceGetSelectedTrack() + 1,
                nativeSequenceTrackStatus(
                        nativeSequenceGetSelectedTrack())));
        padInfo.setText("PAD " + String.format(
                Locale.ROOT, "%02d", selectedPad + 1));

        refreshSequenceStepPage();
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void refreshSequenceStepPage() {
        if (sequenceStepButtons[0] == null) return;

        final int count = sequenceStepPageCount();
        sequenceStepPage = Math.max(
                0, Math.min(count - 1, sequenceStepPage));
        nativeSequenceSetStepEditContext(true, sequenceStepPage);

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
                            "STEP %02d  •  VEL %d  •  PROB %d  •  RAT %dx  •  NUDGE %+d  •  DUR %d",
                            selectedSequenceStep + 1,
                            parameters[0],
                            parameters[1],
                            parameters[2],
                            parameters[3],
                            parameters.length >= 5 && parameters[4] > 0
                                    ? parameters[4]
                                    : gridTicks)
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

    private void adjustSelectedStepDuration(int quarterSteps) {
        final int[] parameters = getSelectedStepParameters();
        if (parameters == null || parameters.length < 5 || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }
        final int gridTicks = Math.max(1, nativeSequenceGetQuantizeGrid());
        final int increment = Math.max(1, gridTicks / 4);
        final int current = Math.max(increment, parameters[4] > 0
                ? parameters[4]
                : gridTicks);
        final int value = Math.max(
                increment,
                Math.min(gridTicks * 4, current + quarterSteps * increment));
        setBottomStatus(nativeSequenceSetStepDuration(
                selectedPad,
                selectedSequenceStep,
                gridTicks,
                value));
        refreshSequenceStepPage();
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
        final int count = sequenceStepPageCount();
        sequenceStepPage = Math.max(
                0, Math.min(count - 1, sequenceStepPage + delta));
        nativeSequenceSetStepEditContext(true, sequenceStepPage);
        refreshSequenceStepPage();
    }


    private void syncStepEditPadLeds() {
        if (midiBridge == null || sequenceStepButtons[0] == null) {
            return;
        }

        final int gridTicks = Math.max(1, nativeSequenceGetQuantizeGrid());
        final int page = Math.max(0, sequenceStepPage);
        final int firstStep = page * SEQUENCE_GRID_PAGE_STEPS;
        final int[] velocities = nativeSequenceGetGridVelocities(
                firstStep, gridTicks);
        final long totalSequenceTicks = Math.max(
                gridTicks,
                Math.round(
                        getSequenceTicksPerBar()
                                * Math.max(1, nativeSequenceGetBars())));
        final int totalSteps = (int) Math.max(
                1L,
                (totalSequenceTicks + gridTicks - 1L) / gridTicks);
        final int selectedStep = selectedSequenceStep;
        final int playheadStep = (int) Math.max(
                0L,
                nativeSequencePositionTicks() / gridTicks);

        StringBuilder signature = new StringBuilder();
        signature.append(gridTicks)
                .append(':').append(page)
                .append(':').append(selectedStep)
                .append(':').append(playheadStep)
                .append(':').append(totalSteps);

        final int[] red = new int[16];
        final int[] green = new int[16];
        final int[] blue = new int[16];

        for (int pad = 0; pad < 16; pad++) {
            final int absoluteStep = firstStep + pad;
            final boolean inRange = absoluteStep < totalSteps;
            final boolean active = inRange
                    && velocities != null
                    && velocities.length > selectedPad * 16 + pad
                    && velocities[selectedPad * 16 + pad] > 0;
            final boolean selected = absoluteStep == selectedStep;
            final boolean playhead = absoluteStep == playheadStep;

            if (!inRange) {
                red[pad] = 0;
                green[pad] = 0;
                blue[pad] = 0;
            } else if (selected && playhead) {
                red[pad] = 127;
                green[pad] = 127;
                blue[pad] = 127;
            } else if (playhead) {
                red[pad] = 127;
                green[pad] = active ? 80 : 20;
                blue[pad] = active ? 40 : 20;
            } else if (selected) {
                red[pad] = 127;
                green[pad] = 72;
                blue[pad] = 0;
            } else if (active) {
                red[pad] = 0;
                green[pad] = 72;
                blue[pad] = 18;
            } else {
                red[pad] = 8;
                green[pad] = 8;
                blue[pad] = 8;
            }

            signature.append('|')
                    .append(pad).append('=')
                    .append(red[pad]).append(',')
                    .append(green[pad]).append(',')
                    .append(blue[pad]);
        }

        final String value = signature.toString();
        if (value.equals(lastStepEditLedSignature)) {
            return;
        }

        for (int pad = 0; pad < 16; pad++) {
            final byte[] message = MpcStudioMk2MidiMessages.padRgb(
                    pad, red[pad], green[pad], blue[pad]);
            if (message != null) {
                midiBridge.send(message);
            }
        }

        lastStepEditLedSignature = value;
    }

    private void clearStepEditPadLeds() {
        if (midiBridge == null) {
            lastStepEditLedSignature = "";
            return;
        }
        if ("CLEARED".equals(lastStepEditLedSignature)) {
            stepEditParameter = STEP_EDIT_PARAMETER_VELOCITY;
            return;
        }

        stepEditParameter = STEP_EDIT_PARAMETER_VELOCITY;
        for (int pad = 0; pad < 16; pad++) {
            final byte[] message = MpcStudioMk2MidiMessages.padRgb(
                    pad, 0, 0, 0);
            if (message != null) {
                midiBridge.send(message);
            }
        }
        lastStepEditLedSignature = "CLEARED";
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

    private int sequenceGridTotalSteps() {
        final long gridTicks = Math.max(
                1L, nativeSequenceGetQuantizeGrid());
        final long lengthTicks = Math.max(
                gridTicks,
                Math.round(getSequenceTicksPerBar())
                        * Math.max(1, nativeSequenceGetBars()));
        return (int) Math.max(
                1L,
                (lengthTicks + gridTicks - 1L) / gridTicks);
    }

    private int sequenceStepPageCount() {
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

    private int sequenceGridMaxStartStep() {
        return Math.max(
                0,
                sequenceGridTotalSteps() - sequenceGridVisibleSteps);
    }

    private int sequenceGridMaxStartPad() {
        return Math.max(
                0,
                MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS
                        - sequenceGridVisiblePads);
    }

    private void moveSequenceGridPage(int delta) {
        final int step = Math.max(
                1,
                sequenceGridVisibleSteps);
        sequenceGridStartStep = Math.max(
                0,
                Math.min(
                        sequenceGridMaxStartStep(),
                        sequenceGridStartStep + delta * step));
        refreshSequenceGrid();
    }

    private void refreshGridHeaderState() {
        if (sequenceGridView == null || navigationController == null) {
            return;
        }

        final int totalSteps = sequenceGridTotalSteps();
        final int first = sequenceGridStartStep + 1;
        final int last = Math.min(
                totalSteps,
                sequenceGridStartStep + sequenceGridVisibleSteps);
        final int track = Math.max(0, nativeSequenceGetSelectedTrack());

        if (gridSelectionStateView != null) {
            gridSelectionStateView.setText(String.format(
                    Locale.ROOT,
                    "PAD %02d",
                    selectedPad + 1));
        }

        if (gridToolStateView != null) {
            refreshGridToolState();
        }

        View parent = sequenceGridView.getParent();
        if (parent instanceof LinearLayout) {
            LinearLayout page = (LinearLayout) parent;
            if (page.getChildCount() >= 5
                    && page.getChildAt(0) instanceof LinearLayout
                    && page.getChildAt(1) instanceof LinearLayout
                    && page.getChildAt(2) instanceof TextView
                    && page.getChildAt(4) instanceof LinearLayout) {
                LinearLayout header = (LinearLayout) page.getChildAt(0);
                if (header.getChildCount() >= 2
                        && header.getChildAt(1) instanceof TextView) {
                    ((TextView) header.getChildAt(1)).setText(String.format(
                            Locale.ROOT,
                            "TRACK %02d • %s",
                            track + 1,
                            nativeSequenceTrackStatus(track)));
                }
                TextView info = (TextView) page.getChildAt(2);
                info.setText(String.format(
                        Locale.ROOT,
                        "GRID • %02d–%02d / %02d • Q %s",
                        first, last, totalSteps,
                        sequenceGridLabel(nativeSequenceGetQuantizeGrid())));
            }
        }
    }

    private void refreshSequenceGrid() {
        if (sequenceGridView == null) return;

        final int totalSteps = sequenceGridTotalSteps();
        sequenceGridVisibleSteps = Math.max(
                1,
                Math.min(
                        MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_STEPS,
                        Math.min(sequenceGridVisibleSteps, totalSteps)));
        sequenceGridStartStep = Math.max(
                0,
                Math.min(
                        totalSteps - sequenceGridVisibleSteps,
                        sequenceGridStartStep));
        sequenceGridVisiblePads = Math.max(
                1,
                Math.min(
                        MpcSequenceZoomPolicy.MAX_GRID_VISIBLE_PADS,
                        sequenceGridVisiblePads));
        sequenceGridStartPad = Math.max(
                0,
                Math.min(
                        sequenceGridMaxStartPad(),
                        sequenceGridStartPad));

        final int gridTicks = Math.max(
                1, nativeSequenceGetQuantizeGrid());
        final long absoluteStep = Math.max(
                0L,
                nativeSequencePositionTicks() / gridTicks);
        final int firstStep = sequenceGridStartStep;
        final int localPlayhead =
                absoluteStep >= firstStep
                        && absoluteStep < firstStep + sequenceGridVisibleSteps
                        ? (int) absoluteStep
                        : -1;

        sequenceGridView.setEditable(nativeSequenceIsGridEditable());
        sequenceGridView.setViewport(
                sequenceGridStartStep,
                sequenceGridVisibleSteps,
                totalSteps,
                sequenceGridStartPad,
                sequenceGridVisiblePads);
        sequenceGridView.setState(
                nativeSequenceGetGridVelocities(firstStep, gridTicks),
                localPlayhead,
                selectedPad);
        refreshSequenceGridPageInfo();
    }

    private void refreshSequenceGridPageInfo() {
        if (sequenceGridView == null) return;
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
        final int first = sequenceGridStartStep + 1;
        final int last = Math.min(
                sequenceGridTotalSteps(),
                sequenceGridStartStep + sequenceGridVisibleSteps);
        pageInfo.setText(String.format(
                Locale.ROOT,
                "%02d–%02d/%02d",
                first,
                last,
                sequenceGridTotalSteps()));
    }

    private void zoomSequenceGridHorizontal(int delta) {
        if (sequenceGridView == null) return;
        final int oldVisible = sequenceGridVisibleSteps;
        final int nextVisible = delta > 0
                ? MpcSequenceZoomPolicy.zoomGridStepsIn(oldVisible)
                : MpcSequenceZoomPolicy.zoomGridStepsOut(oldVisible);
        final int totalSteps = sequenceGridTotalSteps();
        final int clampedVisible = Math.max(
                1,
                Math.min(nextVisible, Math.max(1, totalSteps)));
        if (clampedVisible == oldVisible) return;

        final int center = sequenceGridStartStep + oldVisible / 2;
        sequenceGridVisibleSteps = clampedVisible;
        sequenceGridStartStep = Math.max(
                0,
                Math.min(
                        sequenceGridTotalSteps() - sequenceGridVisibleSteps,
                        center - sequenceGridVisibleSteps / 2));
        refreshSequenceGrid();
        setBottomStatus(
                "GRID ZOOM H • "
                        + sequenceGridStartStep + 1
                        + "–"
                        + Math.min(
                                sequenceGridTotalSteps(),
                                sequenceGridStartStep + sequenceGridVisibleSteps));
    }

    private void zoomSequenceGridVertical(int delta) {
        if (sequenceGridView == null) return;
        final int oldVisible = sequenceGridVisiblePads;
        final int nextVisible = delta > 0
                ? MpcSequenceZoomPolicy.zoomGridPadsIn(oldVisible)
                : MpcSequenceZoomPolicy.zoomGridPadsOut(oldVisible);
        if (nextVisible == oldVisible) return;

        final int selectedRow = Math.max(
                0,
                Math.min(15, 15 - selectedPad));
        sequenceGridVisiblePads = nextVisible;
        sequenceGridStartPad = Math.max(
                0,
                Math.min(
                        sequenceGridMaxStartPad(),
                        selectedRow - sequenceGridVisiblePads / 2));
        refreshSequenceGrid();
        setBottomStatus(
                "GRID ZOOM V • PADS "
                        + (16 - sequenceGridStartPad)
                        + "–"
                        + (17 - sequenceGridStartPad - sequenceGridVisiblePads));
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

    private String formatMpcToolbarPosition(long positionTicks) {
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
                Locale.ROOT,
                "BAR %03d  BEAT %d  TICK %03d",
                bar,
                beat,
                tick);
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
                    Math.max(0L, sequenceGridStartStep);
            final int localStep =
                    absoluteStep >= firstStep
                            && absoluteStep < firstStep + sequenceGridVisibleSteps
                            ? (int) absoluteStep
                            : -1;
            sequenceGridView.setState(
                    nativeSequenceGetGridVelocities(
                            (int) firstStep, gridTicks),
                    localStep,
                    selectedPad);
        }

        if (sequenceStepButtons[0] != null) {
            syncStepEditPadLeds();
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

    private String noteRepeatRateLabel(int index) {
        switch (index) {
            case 0: return "1/4";
            case 1: return "1/8";
            case 2: return "1/16";
            case 3: return "1/32";
            case 4: return "1/64";
            case 5: return "1/4T";
            case 6: return "1/8T";
            case 7: return "1/16T";
            default: return "RATE " + index;
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
                        if (sequenceGridView != null
                                && navigationController.state().mode()
                                        == MpcUiState.Mode.GRID) {
                            refreshGridHeaderState();
                        }
                    }
                    if ("ARRANGE".equals(currentPage)) {
                        refreshArrangeView();
                    }
                    if ("MAIN".equals(currentPage)) {
                        refreshMainArrangementPreview();
                    }
                }

                sequenceUiHandler.postDelayed(this, 80);
            }
        };
        sequenceUiHandler.post(sequenceUiUpdater);
    }

    private void refreshSequenceOverview() {
        if (!startupComplete) {
            return;
        }

        if (mpcShell == null) {
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

        if (mpcShell != null) {
            final long ticksPerSequence = Math.max(
                    1L,
                    Math.round(getSequenceTicksPerBar() * Math.max(1, bars)));
            mpcShell.playheadStrip().setState(
                    positionTicks,
                    ticksPerSequence,
                    nativeSequenceIsPlaying());
        }

        syncHardwareLcd();

        refreshMpcToolbarState();

        if (sequenceTransportView != null) {
            final int queuedIndex = nativeSequenceGetQueuedIndex();
            final String queueLabel = queuedIndex >= 0
                    && queuedIndex < sequenceCount
                    ? String.format(Locale.ROOT, " →S%02d", queuedIndex + 1)
                    : "";
            sequenceTransportView.setText(
                    formatMpcToolbarPosition(positionTicks));
        }
    }

    private void stopSequenceUiUpdater() {
        if (sequenceUiUpdater != null) {
            sequenceUiHandler.removeCallbacks(sequenceUiUpdater);
            sequenceUiUpdater = null;
        }
    }

    private void showMixPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "PAD_MIXER";
        navigationController.navigate(MpcUiState.Mode.PAD_MIXER);
        pageTitle.setText("PAD MIXER");
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
                    if (fromUser) {
                        nativeAudioSetPadLevel(p, progress / 100.0f);
                        refreshMpcCompactContext();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar bar) {}
                @Override public void onStopTrackingTouch(SeekBar bar) {}
            });
            strip.addView(level, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

            strip.addView(actionButton("SELECT", v -> {
                selectedPad = p;
                navigationController.setSelectedPad(p);
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        currentPage = "MIDI";
        navigationController.navigate(MpcUiState.Mode.MIDI_CONTROL);
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
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        currentPage = "MENU";
        navigationController.navigate(MpcUiState.Mode.MENU);
        pageTitle.setText("MENU");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView hint = label(
                "MODE MENU  •  4×4 launcher  •  promoted contexts live in the five shortcuts",
                10, MUTED);
        hint.setPadding(dp(6), 0, dp(6), 0);
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(4);

        final MpcModeRegistry.Entry[] entries = MpcModeRegistry.menuEntries();
        for (int i = 0; i < entries.length; i++) {
            final MpcModeRegistry.Entry entry = entries[i];
            final Button b = actionButton(
                    entry.available ? entry.label : entry.label + "\nRESERVED",
                    v -> {
                        if (!entry.available) {
                            navigationController.navigate(MpcUiState.Mode.RESERVED);
                            navigationController.setActionAvailable(false);
                            setBottomStatus(
                                    entry.label + " • RESERVED / UNAVAILABLE");
                            updateMpcShellState();
                            return;
                        }
                        navigateToMode(entry.mode);
                    });
            b.setEnabled(entry.available);
            b.setAlpha(entry.available ? 1.0f : 0.55f);
            b.setGravity(Gravity.CENTER);
            b.setTextSize(11);
            b.setTypeface(Typeface.DEFAULT_BOLD);
            b.setContentDescription(
                    entry.available
                            ? "MPC Menu " + entry.label
                            : "MPC Menu " + entry.label + " reserved");

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = 0;
            lp.columnSpec = GridLayout.spec(i % 4, 1f);
            lp.rowSpec = GridLayout.spec(i / 4, 1f);
            final int margin = dp(3);
            lp.setMargins(margin, margin, margin, margin);
            grid.addView(b, lp);
        }

        page.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout system = row();
        system.addView(actionButton("PREFERENCES", v -> showAudioSettingsPage()),
                weight());
        system.addView(actionButton("MIDI / CONTROL", v -> showMidiPage()),
                weight());
        Button saveProject = actionButton(
                "SAVE / PROJECT",
                null);
        saveProject.setEnabled(false);
        saveProject.setAlpha(0.45f);
        saveProject.setContentDescription(
                "Save and Project reserved");
        system.addView(saveProject, weight());
        system.addView(actionButton(
                "EDIT SHORTCUTS",
                v -> showShortcutConfigPage()),
                weight());
        system.addView(actionButton("BACK", v -> navigateBackFromShell()), weight());
        page.addView(system, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        content.addView(page);
        updateModeRailSelection();
    }
    private void showShortcutConfigPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        navigationController.setSubcontext(MpcUiState.Subcontext.SHORTCUT_CONFIG);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SHORTCUT);
        pageTitle.setText("SHORTCUTS");
        content.removeAllViews();

        final MpcModeRegistry.Entry[] allModes = MpcModeRegistry.menuEntries();
        final ArrayList<MpcModeRegistry.Entry> availableModes = new ArrayList<>();
        for (MpcModeRegistry.Entry entry : allModes) {
            if (entry.available) {
                availableModes.add(entry);
            }
        }

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView header = label(
                "SHORTCUTS • FIVE HIGH-FREQUENCY MODES",
                13, TEXT);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        TextView hint = label(
                "Each slot can promote any implemented context. RESERVED modes stay in Menu.",
                10, MUTED);
        page.addView(hint, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        LinearLayout list = column();
        MpcUiState.Mode[] current = navigationController.shortcuts();
        for (int i = 0; i < MpcNavigationController.SHORTCUT_COUNT; i++) {
            final int slot = i;
            LinearLayout row = row();
            row.setPadding(dp(6), dp(3), dp(6), dp(3));
            row.setBackground(strokeBackground(SURFACE_2, LINE, 7));

            TextView position = label(
                    String.format(Locale.ROOT, "%02d", i + 1),
                    13, ACCENT);
            position.setGravity(Gravity.CENTER);
            position.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(position, new LinearLayout.LayoutParams(
                    dp(42), dp(48)));

            android.widget.Spinner spinner = new android.widget.Spinner(this);
            ArrayList<String> labels = new ArrayList<>();
            int selectedIndex = 0;
            for (int j = 0; j < availableModes.size(); j++) {
                MpcModeRegistry.Entry entry = availableModes.get(j);
                labels.add(entry.label);
                if (entry.mode == current[i]) {
                    selectedIndex = j;
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    labels);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            spinner.setSelection(selectedIndex);
            spinner.setContentDescription(
                    "Shortcut " + (i + 1) + " mode selector");
            spinner.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(
                                android.widget.AdapterView<?> parent,
                                View view,
                                int positionIndex,
                                long id) {
                            if (positionIndex >= 0
                                    && positionIndex < availableModes.size()) {
                                navigationController.setShortcut(
                                        slot,
                                        availableModes.get(positionIndex).mode);
                                setBottomStatus(String.format(
                                        Locale.ROOT,
                                        "SHORTCUT %d • %s",
                                        slot + 1,
                                        availableModes.get(positionIndex).label));
                            }
                        }

                        @Override
                        public void onNothingSelected(
                                android.widget.AdapterView<?> parent) {
                        }
                    });
            row.addView(spinner, new LinearLayout.LayoutParams(
                    0, dp(48), 1));

            Button up = actionButton("▲", v -> {
                if (slot > 0) {
                    navigationController.moveShortcut(slot, slot - 1);
                    showShortcutConfigPage();
                }
            });
            up.setEnabled(i > 0);
            up.setAlpha(i > 0 ? 1.0f : 0.35f);
            row.addView(up, new LinearLayout.LayoutParams(dp(50), dp(44)));

            Button down = actionButton("▼", v -> {
                if (slot < MpcNavigationController.SHORTCUT_COUNT - 1) {
                    navigationController.moveShortcut(slot, slot + 1);
                    showShortcutConfigPage();
                }
            });
            down.setEnabled(i < MpcNavigationController.SHORTCUT_COUNT - 1);
            down.setAlpha(
                    i < MpcNavigationController.SHORTCUT_COUNT - 1
                            ? 1.0f : 0.35f);
            row.addView(down, new LinearLayout.LayoutParams(dp(50), dp(44)));

            list.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        }

        page.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton("RESET DEFAULTS", v -> {
            navigationController.setShortcuts(
                    MpcModeRegistry.defaultShortcuts());
            showShortcutConfigPage();
        }), weight());
        footer.addView(actionButton("BACK TO MENU", v -> showMenuPage()), weight());
        page.addView(footer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44)));

        content.addView(page);
        updateModeRailSelection();
        refreshMpcFunctionBar();
    }


    private void showAudioSettingsPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        currentPage = "AUDIO";
        navigationController.navigate(MpcUiState.Mode.PREFERENCES);
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
        if (navigationController != null) {
            navigationController.setSelectedPad(pad);
        }
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
        refreshMainModePadVisuals();
        refreshMainTrackQuickSample();
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
            final String sampleName = sampleDisplayName(uri);
            if (loaded != null
                    && loaded.startsWith("Pad ")
                    && loaded.contains(" sample loaded")) {
                nativeAudioSetPadSampleName(
                        selectedPad, selectedLayer, sampleName);
            }
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

    private String sampleDisplayName(Uri uri) {
        if (uri == null) return "Imported sample";

        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int nameIndex =
                        cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    final String displayName = cursor.getString(nameIndex);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName.trim();
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // Some document providers do not expose DISPLAY_NAME.
        } finally {
            if (cursor != null) cursor.close();
        }

        final String fallback = uri.getLastPathSegment();
        if (fallback != null && !fallback.trim().isEmpty()) {
            final int colon = fallback.lastIndexOf(':');
            final String value = colon >= 0
                    ? fallback.substring(colon + 1)
                    : fallback;
            if (!value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "Imported sample";
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
        syncHardwareControllerFeedback();
    }

    private void syncHardwareControllerFeedback() {
        final String hardwareContext = navigationController == null
                ? currentPage
                : navigationController.state().mode().name();
        final String context = MpcHardwareFeedbackPolicy.contextLabel(
                hardwareContext,
                hardwareFocus,
                hardwareLocateActive,
                hardwareEraseActive,
                hardwareCopyDeleteActive,
                hardwareCopyDeleteMode,
                hardwareNoteRepeatActive,
                hardwareNoteRepeatRateIndex,
                hardwareTouchStripMode);

        if (hardwareFeedbackView != null) {
            final String axis = MpcHardwareFeedbackPolicy.focusAxis(hardwareFocus);
            final String dial = axis.isEmpty()
                    ? ""
                    : " • DIAL " + axis;
            hardwareFeedbackView.setText(
                    "MKII • " + context
                            + dial
                            + " • BANK " + (char) ('A' + hardwarePadBank % 4)
                            + (hardwarePadBank >= 4 ? "+SHIFT" : ""));
            hardwareFeedbackView.setTextColor(
                    hardwareEraseActive
                            ? DANGER
                            : hardwareCopyDeleteActive
                                    ? ACCENT_2
                                    : hardwareNoteRepeatActive
                                            ? ACTIVE
                                            : ACCENT);
            hardwareFeedbackView.setContentDescription(
                    "MPC Studio MkII: " + context + dial);
        }

        if (midiBridge == null) return;

        // The MkII button LEDs are not hardware-owned; host software drives them.
        // Two-color buttons use color 1 for the primary context and color 2 for
        // Shift/alternate context, matching the reverse-engineered protocol.
        final boolean zoomHorizontal =
                hardwareFocus == 11 || hardwareFocus == 13 || hardwareFocus == 15;
        final boolean zoomVertical =
                hardwareFocus == 12 || hardwareFocus == 14;
        setHardwareButtonLedState(
                66,
                MpcHardwareFeedbackPolicy.dualColor(
                        zoomHorizontal || zoomVertical,
                        zoomVertical));

        setHardwareButtonLedState(
                13,
                MpcHardwareFeedbackPolicy.dualColor(
                        hardwareFocus == 2 || hardwareFocus == 3,
                        hardwareFocus == 3));

        setHardwareButtonLedState(
                14,
                MpcHardwareFeedbackPolicy.dualColor(
                        hardwareFocus == 4 || hardwareFocus == 5,
                        hardwareFocus == 5));

        setHardwareButtonLedState(
                42,
                hardwareFocus == 10
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                33,
                hardwareFocus == 7
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                34,
                hardwareFocus == 8
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                79,
                hardwareFocus == 9
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                15,
                nativeSequenceIsTimingCorrectEnabled()
                        ? MpcHardwareFeedbackPolicy.LED_SINGLE_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);

        for (int i = 0; i < 4; i++) {
            final int cc = 35 + i;
            final boolean selected =
                    hardwarePadBank % 4 == i;
            final boolean shifted = hardwarePadBank >= 4;
            setHardwareButtonLedState(
                    cc,
                    selected
                            ? (shifted
                                    ? MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL
                                    : MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL)
                            : MpcHardwareFeedbackPolicy.LED_OFF);
        }
    }

    private void syncHardwareLcd() {
        if (midiBridge == null) return;

        final String status = bottomStatus == null
                ? ""
                : String.valueOf(bottomStatus.getText());

        final MpcStudioMk2LcdRenderer.State state =
                new MpcStudioMk2LcdRenderer.State(
                        navigationController == null
                                ? currentPage
                                : navigationController.state().mode().name(),
                        Math.max(0, nativeSequenceGetIndex()),
                        Math.max(1, nativeSequenceGetCount()),
                        nativeSequenceGetQueuedIndex(),
                        Math.max(0, nativeSequenceGetSelectedTrack()),
                        Math.max(1, nativeSequenceGetTrackCount()),
                        nativeSequenceGetTempo(),
                        nativeSequenceGetNumerator(),
                        nativeSequenceGetDenominator(),
                        nativeSequencePositionTicks(),
                        nativeSequenceIsPlaying(),
                        nativeSequenceIsSelectedTrackArmed(),
                        nativeSequenceGetRecordMode() == 1,
                        selectedPad,
                        selectedLayer,
                        hardwareTouchStripMode,
                        hardwareNoteRepeatRateIndex,
                        hardwareNoteRepeatActive,
                        hardwareLocateActive,
                        hardwareEraseActive,
                        stepEditParameter,
                        selectedSequenceStep,
                        status);

        final String signature = state.signature();
        if (signature.equals(lastLcdSignature)) return;

        final List<byte[]> messages = MpcStudioMk2LcdRenderer.render(state);
        for (byte[] message : messages) {
            if (message != null) midiBridge.send(message);
        }
        lastLcdSignature = signature;
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
                "MAIN", "BROWSER", "GRID", "SAMPLER", "PAD MIXER",
                "MENU", "PLAY", "STOP", "MIDI", "01", "16", "LOAD"
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

        View browserShortcut = findViewWithExactText(
                getWindow().getDecorView(), "BROWSER");
        if (browserShortcut == null || !browserShortcut.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: BROWSER shortcut");
            return;
        }

        View mainShortcut = findViewWithExactText(
                getWindow().getDecorView(), "MAIN");
        if (mainShortcut == null || !mainShortcut.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: MAIN shortcut return");
            return;
        }

        View pad1 = findViewWithExactText(getWindow().getDecorView(), "01");
        if (pad1 == null || !pad1.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: pad 01");
            return;
        }
        if (selectedPad != 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: selection did not stick");
            return;
        }

        View quickSampleWaveform = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View quick sample waveform");
        View quickSampleInfo = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View quick sample info");
        if (quickSampleWaveform == null
                || quickSampleWaveform.getHeight() <= dp(70)
                || quickSampleInfo == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main quick sample context");
            return;
        }

        View layerUp = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View next sample layer");
        View layerDown = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View previous sample layer");
        if (layerUp == null || layerDown == null
                || !layerUp.performClick()
                || !layerDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main sample layer controls");
            return;
        }

        View mainTrackProgram = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Mode Track Program section");
        if (mainTrackProgram == null || mainTrackProgram.getHeight() <= dp(160)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track Program composition");
            return;
        }

        View compactContext = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC shell compact track program context");
        View compactTrack = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC shell track context");
        View compactProgram = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC shell program context");
        if (compactContext == null
                || compactContext.getWidth() < dp(160)
                || compactContext.getHeight() <= dp(300)
                || compactTrack == null
                || compactProgram == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: persistent compact track/program context");
            return;
        }

        View compactMixerToggleAudit = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC condensed Mixer Strip show or hide");
        View compactMixerPanelAudit = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC condensed Mixer Strip");
        if (compactMixerToggleAudit == null
                || compactMixerPanelAudit == null
                || compactMixerPanelAudit.getVisibility() != View.VISIBLE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: persistent Main Mixer Strip");
            return;
        }
        if (!compactMixerToggleAudit.performClick()
                || compactMixerPanelAudit.getVisibility() != View.GONE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Mixer Strip hide");
            return;
        }
        if (!compactMixerToggleAudit.performClick()
                || compactMixerPanelAudit.getVisibility() != View.VISIBLE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Mixer Strip show");
            return;
        }

        View mainTrackSelector = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Track View selector");
        View mainArrangementSelector = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Arrangement View selector");
        View mainTrackWorkspace = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Mode Track View workspace");
        View mainArrangementWorkspace = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Mode arrangement preview");
        if (mainTrackSelector == null
                || mainArrangementSelector == null
                || mainTrackWorkspace == null
                || mainArrangementWorkspace == null
                || mainTrackWorkspace.getVisibility() != View.VISIBLE
                || mainArrangementWorkspace.getVisibility() != View.GONE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track/Arrangement default view");
            return;
        }

        if (!mainArrangementSelector.performClick()
                || mainArrangementWorkspace.getVisibility() != View.VISIBLE
                || mainTrackWorkspace.getVisibility() != View.GONE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Arrangement switch");
            return;
        }

        if (!mainTrackSelector.performClick()
                || mainTrackWorkspace.getVisibility() != View.VISIBLE
                || mainArrangementWorkspace.getVisibility() != View.GONE) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track switch");
            return;
        }

        View seqSelect = findViewWithExactText(
                getWindow().getDecorView(), "SEQ SELECT");
        if (seqSelect == null || !seqSelect.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main SEQ SELECT");
            return;
        }
        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Main Sequence Select list") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Sequence Select list");
            return;
        }
        View backMain = findViewWithExactText(
                getWindow().getDecorView(), "BACK MAIN");
        if (backMain == null || !backMain.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Sequence Select back");
            return;
        }

        onHardwareAction(
                MpcStudioMk2SemanticActions.TRACK_SELECTION_CONTEXT,
                0, 0, 0);
        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Main Track Select list") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track Select list");
            return;
        }
        backMain = findViewWithExactText(
                getWindow().getDecorView(), "BACK MAIN");
        if (backMain == null || !backMain.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track Select back");
            return;
        }

        onHardwareAction(
                MpcStudioMk2SemanticActions.PROGRAM_SELECTION_CONTEXT,
                0, 0, 0);
        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Main Program Select list") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Program Select list");
            return;
        }
        backMain = findViewWithExactText(
                getWindow().getDecorView(), "BACK MAIN");
        if (backMain == null || !backMain.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Program Select back");
            return;
        }

        View trackView = findViewWithExactText(
                getWindow().getDecorView(), "TRACK VIEW");
        if (trackView == null || !trackView.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: TRACK VIEW");
            return;
        }

        View trackViewWorkspace = findViewWithContentDescription(
                getWindow().getDecorView(), "MPC Track View workspace");
        if (trackViewWorkspace == null
                || trackViewWorkspace.getHeight() <= dp(180)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Track View workspace");
            return;
        }

        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Track View track 1") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Track View track strips");
            return;
        }

        View arrangeFromTrackView = findViewWithExactText(
                getWindow().getDecorView(), "ARRANGE");
        if (arrangeFromTrackView == null
                || !arrangeFromTrackView.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: ARRANGE from Track View");
            return;
        }

        View arrangementSurface = findViewWithContentDescription(
                getWindow().getDecorView(), "MPC linear arrangement editor");
        if (arrangementSurface == null
                || arrangementSurface.getHeight() <= dp(160)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Arrangement surface");
            return;
        }

        View menuAfterArrange = findViewWithExactText(
                getWindow().getDecorView(), "MENU");
        if (menuAfterArrange == null || !menuAfterArrange.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: MENU after ARRANGE");
            return;
        }

        View grid = findViewWithExactText(getWindow().getDecorView(), "GRID");
        if (grid == null || !grid.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: GRID editor");
            return;
        }

        View gridView = findViewWithContentDescription(
                getWindow().getDecorView(), "MPC Grid View drum event grid");
        if (gridView == null || gridView.getHeight() <= dp(120)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sequence grid editor");
            return;
        }
        if (!drumGridAvailable()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: audit track is not Drum");
            return;
        }
        if (findViewWithContentDescription(
                getWindow().getDecorView(), "Grid Draw tool") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Grid Erase tool") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Grid Select tool") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Grid Navigation tool") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Grid tool palette");
            return;
        }

        View step = findViewWithExactText(
                getWindow().getDecorView(), "STEP");
        if (step == null || !step.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: STEP editor");
            return;
        }

        if (!"DRUM".equalsIgnoreCase(
                nativeSequenceGetTrackType(nativeSequenceGetSelectedTrack()))) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Step audit track is not Drum");
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

        onHardwareAction(
                MpcStudioMk2SemanticActions.STEP_EDIT_PAD_SELECTED,
                0, 0, 0);
        if (selectedSequenceStep != 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: hardware step selection");
            return;
        }

        final String eventBeforeHardwareEdit =
                sequenceStepEventInfo.getText().toString();
        onHardwareAction(
                MpcStudioMk2SemanticActions.DATA_DIAL_PRESS,
                0, 0, 0);
        onHardwareAction(
                MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA,
                1, 0, 0);
        final String eventAfterHardwareEdit =
                sequenceStepEventInfo.getText().toString();
        if (eventBeforeHardwareEdit.equals(eventAfterHardwareEdit)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: hardware Step Edit adjustment");
            return;
        }

        View paramButton = findViewWithExactText(
                getWindow().getDecorView(), "PARAM");
        View plusButton = findViewWithExactText(
                getWindow().getDecorView(), "+");
        if (paramButton == null || plusButton == null
                || !paramButton.performClick()
                || !plusButton.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step shell controls");
            return;
        }
        View stepEventView = findViewWithContentDescription(
                getWindow().getDecorView(), "Step Edit event information");
        if (stepEventView == null
                || !(stepEventView instanceof TextView)
                || !((TextView) stepEventView).getText().toString()
                        .contains("STEP 01")) {
            Log.e(TAG, "UI_INTERACTION_FAILED: step event parameters");
            return;
        }

        onHardwareAction(
                MpcStudioMk2SemanticActions.LOCATE_STATE, 1, 0, 0);
        onHardwareAction(
                MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA, -1, 1, 0);
        onHardwareAction(
                MpcStudioMk2SemanticActions.LOCATE_PAD, 0, 1, 0);
        final long locator = nativeSequenceGetLocator(0);
        if (locator < 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: locator set");
            return;
        }
        onHardwareAction(
                MpcStudioMk2SemanticActions.LOCATE_PAD, 0, 0, 0);
        if (nativeSequencePositionTicks() != locator) {
            Log.e(TAG, "UI_INTERACTION_FAILED: locator jump");
            return;
        }
        onHardwareAction(
                MpcStudioMk2SemanticActions.LOCATE_STATE, 0, 0, 0);

        View menuAfterStep = findViewWithExactText(
                getWindow().getDecorView(), "MENU");
        if (menuAfterStep == null || !menuAfterStep.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: MENU after STEP");
            return;
        }

        View launch = findViewWithExactText(
                getWindow().getDecorView(), "NEXT SEQUENCE");
        if (launch == null || !launch.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: NEXT SEQUENCE");
            return;
        }

        View launcher = findViewWithContentDescription(
                getWindow().getDecorView(), "Sequence live launcher");
        if (launcher == null || launcher.getHeight() <= dp(120)) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sequence launcher");
            return;
        }

        View menuAfterLauncher = findViewWithExactText(
                getWindow().getDecorView(), "MENU");
        if (menuAfterLauncher == null || !menuAfterLauncher.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: MENU after launcher");
            return;
        }

        View sample = findViewWithExactText(
                getWindow().getDecorView(), "SAMPLE EDIT");
        if (sample == null || !sample.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: SAMPLE EDIT");
            return;
        }

        if (findViewWithExactText(getWindow().getDecorView(), "ENV") == null
                || findViewWithExactText(getWindow().getDecorView(), "FILTER") == null
                || findViewWithContentDescription(
                        getWindow().getDecorView(), "Sample waveform editor") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: sample waveform editor");
            return;
        }

        View menuAfterSample = findViewWithExactText(
                getWindow().getDecorView(), "MENU");
        if (menuAfterSample == null || !menuAfterSample.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: MENU after SAMPLE EDIT");
            return;
        }

        View rec = findViewWithExactText(
                getWindow().getDecorView(), "SAMPLER");
        if (rec == null || !rec.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: SAMPLER mode");
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
        setHardwareButtonLedState(
                cc,
                on
                        ? MpcHardwareFeedbackPolicy.isTwoColorButton(cc)
                                ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                                : MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
    }

    private void setHardwareButtonLedState(int cc, int state) {
        if (cc < 0 || cc >= hardwareButtonLedStateCache.length) return;
        final int clamped = Math.max(
                MpcHardwareFeedbackPolicy.LED_OFF,
                Math.min(MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL, state));
        if (hardwareButtonLedStateCache[cc] == clamped) return;
        hardwareButtonLedStateCache[cc] = clamped;
        if (midiBridge != null) {
            midiBridge.send(
                    MpcStudioMk2MidiMessages.buttonLed(
                            cc,
                            clamped));
        }
    }

    private void setHardwarePadRgb(int pad, int red, int green, int blue) {
        if (midiBridge == null || pad < 0 || pad >= 16) return;
        final byte[] message = MpcStudioMk2MidiMessages.padRgb(
                pad, red, green, blue);
        if (message != null) {
            midiBridge.send(message);
        }
    }

    private void clearHardwareCopyDeletePadLeds() {
        for (int pad = 0; pad < 16; pad++) {
            setHardwarePadRgb(pad, 0, 0, 0);
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

    private boolean isHardwareLocateExitAction(int actionType) {
        switch (actionType) {
            case MpcStudioMk2SemanticActions.NAVIGATE_MAIN:
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_VIEW:
            case MpcStudioMk2SemanticActions.NAVIGATE_GRID:
            case MpcStudioMk2SemanticActions.NAVIGATE_WAVEFORM:
            case MpcStudioMk2SemanticActions.NAVIGATE_SAMPLE_EDIT:
            case MpcStudioMk2SemanticActions.NAVIGATE_PAD_MIXER:
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_MIXER:
            case MpcStudioMk2SemanticActions.NAVIGATE_SEQUENCE_LAUNCHER:
            case MpcStudioMk2SemanticActions.NAVIGATE_BROWSE:
            case MpcStudioMk2SemanticActions.NAVIGATE_SAMPLER:
            case MpcStudioMk2SemanticActions.NAVIGATE_STEP:
            case MpcStudioMk2SemanticActions.BROWSER_UP:
            case MpcStudioMk2SemanticActions.TRACK_SELECTION_CONTEXT:
            case MpcStudioMk2SemanticActions.SEQUENCE_SELECTION_CONTEXT:
            case MpcStudioMk2SemanticActions.PROGRAM_SELECTION_CONTEXT:
            case MpcStudioMk2SemanticActions.TRACK_TYPE_SELECTION_CONTEXT:
            case MpcStudioMk2SemanticActions.SAMPLE_SELECT_CONTEXT:
            case MpcStudioMk2SemanticActions.SAMPLE_START_CONTEXT:
            case MpcStudioMk2SemanticActions.SAMPLE_END_CONTEXT:
            case MpcStudioMk2SemanticActions.TUNE_CONTEXT:
            case MpcStudioMk2SemanticActions.QUANTIZE:
            case MpcStudioMk2SemanticActions.TIMING_CORRECT_STATE:
            case MpcStudioMk2SemanticActions.ZOOM_CONTEXT:
            case MpcStudioMk2SemanticActions.COPY_CONTEXT:
            case MpcStudioMk2SemanticActions.UNDO:
            case MpcStudioMk2SemanticActions.AUTOMATION_CONTEXT:
                return true;
            default:
                return false;
        }
    }

    private void applyHardwareAction(
            int actionType, int value0, int value1, int value2) {
        if (hardwareLocateActive
                && isHardwareLocateExitAction(actionType)) {
            hardwareLocateActive = false;
            setHardwareButtonLed(70, false);
        }

        switch (actionType) {
            case MpcStudioMk2SemanticActions.NAVIGATE_MAIN:
                hardwareFocus = 0;
                showMainPage();
                setBottomStatus("MAIN");
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_VIEW:
                hardwareFocus = 0;
                showTrackViewPage();
                setBottomStatus("TRACK VIEW");
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
                showTrackSelectPage();
                setBottomStatus("TRACK SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.SEQUENCE_SELECTION_CONTEXT:
                hardwareFocus = 3;
                showSequenceSelectPage();
                setBottomStatus("SEQUENCE SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.PROGRAM_SELECTION_CONTEXT:
                hardwareFocus = 4;
                if (!startupComplete) {
                    setBottomStatus("PROGRAM SELECT • sequencer not ready");
                } else if ("DRUM".equals(nativeSequenceGetTrackType(
                        nativeSequenceGetSelectedTrack()))) {
                    navigationController.setActionAvailable(true);
                    showProgramSelectPage();
                    setBottomStatus(
                            "PROGRAM SELECT • DATA DIAL / +/-");
                } else {
                    navigationController.setActionAvailable(false);
                    setBottomStatus(
                            "PROGRAM SELECT • TRACK TYPE IS NOT DRUM");
                    refreshMpcCompactContext();
                    refreshMpcFunctionBar();
                }
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
                if (hardwareLocateActive) {
                    setBottomStatus(
                            "LOCATE • DATA DIAL = ±1 BEAT • SHIFT = ±1 TICK");
                } else if (hardwareFocus == 4) {
                    showProgramSelectPage();
                    setBottomStatus("PROGRAM SELECT • DATA DIAL / +/-");
                } else if ("SEQ".equals(currentPage) && sequenceStepButtons[0] != null) {
                    stepEditParameter = nativeStepEditParameterNext(
                            stepEditParameter);
                    setBottomStatus(
                            "STEP EDIT • " + stepEditParameterLabel()
                                    + " • DATA DIAL / +/-");
                } else {
                    setBottomStatus("DATA DIAL ENTER");
                }
                return;
            case MpcStudioMk2SemanticActions.PAD_BANK_CHANGED:
                hardwarePadBank = Math.max(0, Math.min(7, value0));
                navigationController.setPadBank(hardwarePadBank);
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
            case MpcStudioMk2SemanticActions.STEP_EDIT_PAD_SELECTED:
                if (!"SEQ".equals(currentPage)
                        || sequenceStepButtons[0] == null) {
                    return;
                }
                selectedSequenceStep = Math.max(0, value0);
                refreshSequenceStepPage();
                setBottomStatus(
                        "STEP " + String.format(
                                Locale.ROOT, "%02d", selectedSequenceStep + 1)
                                + " SELECTED • PAD " + (value1 + 1));
                return;
            case MpcStudioMk2SemanticActions.ERASE_STATE:
                hardwareEraseActive = value0 != 0;
                setHardwareButtonLed(9, hardwareEraseActive);
                setBottomStatus(
                        hardwareEraseActive
                                ? (nativeSequenceIsPlaying()
                                        ? "ERASE ARMED • HOLD + PAD"
                                        : "ERASE • PLAYBACK REQUIRED • HOLD + PAD")
                                : "ERASE OFF");
                return;
            case MpcStudioMk2SemanticActions.ERASE_PAD_TARGET:
                setBottomStatus(nativeSequenceErasePadAtPlayhead(value0));
                refreshSequenceControls();
                if ("SEQ".equals(currentPage) && sequenceStepButtons[0] != null) {
                    refreshSequenceStepPage();
                }
                return;
            case MpcStudioMk2SemanticActions.NOTE_REPEAT_STATE:
                hardwareNoteRepeatActive = value0 != 0;
                setHardwareButtonLed(11, value0 != 0);
                setTouchStripButtonLed(value0 != 0);
                syncHardwareNoteRepeatRateLeds(
                        hardwareNoteRepeatRateIndex, value0 != 0);
                syncHardwareTouchStripModeLeds();
                setBottomStatus(
                        value0 != 0
                                ? "NOTE REPEAT ON • "
                                        + (value1 != 0 ? "LATCHED" : "MOMENTARY")
                                        + " • grid-synchronised"
                                : "NOTE REPEAT OFF");
                return;
            case MpcStudioMk2SemanticActions.NOTE_REPEAT_RATE_CHANGED:
                hardwareNoteRepeatRateIndex = Math.max(0, Math.min(7, value0));
                syncHardwareNoteRepeatRateLeds(
                        hardwareNoteRepeatRateIndex, true);
                setBottomStatus(
                        "NOTE REPEAT RATE • "
                                + noteRepeatRateLabel(value0)
                                + " • "
                                + value1
                                + " ticks");
                return;
            case MpcStudioMk2SemanticActions.TOUCH_STRIP_MODE_CHANGED:
                hardwareTouchStripMode = Math.max(
                        TOUCH_STRIP_MODE_LEVEL,
                        Math.min(TOUCH_STRIP_MODE_SAMPLE_END, value0));
                hardwareFocus = 0;
                syncHardwareTouchStripModeLeds();
                setBottomStatus(
                        "TOUCH STRIP • "
                                + touchStripModeLabel(hardwareTouchStripMode)
                                + " • SLIDE TO CONTROL");
                return;
            case MpcStudioMk2SemanticActions.TOUCH_STRIP_TOUCH_STATE:
                if (value0 == 0) {
                    syncHardwareTouchStripModeLeds();
                }
                setBottomStatus(
                        value0 != 0
                                ? "TOUCH STRIP • "
                                        + touchStripModeLabel(value1)
                                        + " • TOUCH"
                                : "TOUCH STRIP • "
                                        + touchStripModeLabel(value1));
                return;
            case MpcStudioMk2SemanticActions.TOUCH_STRIP_CONFIG_CONTEXT:
                setBottomStatus(
                        "TOUCH STRIP CONFIG • LEVEL / PAN / TUNE / SAMPLE START / SAMPLE END"
                                + " • CURRENT "
                                + touchStripModeLabel(value0));
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
            case MpcStudioMk2SemanticActions.LOCATE_STATE:
                hardwareLocateActive = value0 != 0;
                setHardwareButtonLed(70, hardwareLocateActive);
                setBottomStatus(
                        hardwareLocateActive
                                ? (value1 != 0
                                        ? "LOCATE ON • DATA DIAL=BEAT • SHIFT=FINE"
                                        : "LOCATE ON • DATA DIAL=BEAT")
                                : "LOCATE OFF");
                return;
            case MpcStudioMk2SemanticActions.LOCATE_PAD: {
                if (value0 < 0) {
                    setBottomStatus("LOCATE • unused pad");
                    return;
                }
                final String locateResult = value1 != 0
                        ? nativeSequenceSetLocator(value0)
                        : nativeSequenceJumpToLocator(value0);
                setBottomStatus(locateResult);
                refreshSequencePlayhead();
                refreshSequenceControls();
                return;
            }
            case MpcStudioMk2SemanticActions.TAP_TEMPO:
                handleHardwareTapTempo();
                return;
            case MpcStudioMk2SemanticActions.TOUCH_STRIP_VALUE:
                handleHardwareTouchStrip(value0);
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_SELECT_CONTEXT:
                hardwareFocus = 10;
                navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_SELECT);
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_LAYER);
                showSamplePage();
                setBottomStatus("SAMPLE SELECT • DATA DIAL / +/- changes layer");
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_START_CONTEXT:
                hardwareTouchStripMode = TOUCH_STRIP_MODE_SAMPLE_START;
                navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_START);
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_START);
                syncHardwareTouchStripModeLeds();
                hardwareFocus = 7;
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "SAMPLE START • FINE"
                        : "SAMPLE START");
                return;
            case MpcStudioMk2SemanticActions.SAMPLE_END_CONTEXT:
                hardwareTouchStripMode = TOUCH_STRIP_MODE_SAMPLE_END;
                navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_END);
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_END);
                syncHardwareTouchStripModeLeds();
                hardwareFocus = 8;
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "SAMPLE END • FINE"
                        : "SAMPLE END");
                return;
            case MpcStudioMk2SemanticActions.TUNE_CONTEXT:
                hardwareTouchStripMode = TOUCH_STRIP_MODE_TUNE;
                syncHardwareTouchStripModeLeds();
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
                    setHardwareButtonLedState(
                            15,
                            nativeSequenceIsTimingCorrectEnabled()
                                    ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                                    : MpcHardwareFeedbackPolicy.LED_OFF);
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
                if ("SAMPLE".equals(currentPage) && sampleWaveform != null) {
                    hardwareFocus = value0 != 0 ? 12 : 11;
                    setBottomStatus(value0 != 0
                            ? "ZOOM VERTICAL • DATA DIAL / +/-"
                            : "ZOOM HORIZONTAL • DATA DIAL / +/-");
                } else if ("SEQ".equals(currentPage) && sequenceGridView != null) {
                    hardwareFocus = value0 != 0 ? 14 : 13;
                    setBottomStatus(value0 != 0
                            ? "GRID ZOOM VERTICAL • DATA DIAL / +/-"
                            : "GRID ZOOM HORIZONTAL • DATA DIAL / +/-");
                } else if ("SEQ".equals(currentPage) && sequenceTimeline != null) {
                    if (value0 != 0) {
                        hardwareFocus = 16;
                        setBottomStatus("TIMELINE HAS NO VERTICAL AXIS");
                    } else {
                        hardwareFocus = 15;
                        setBottomStatus("TIMELINE ZOOM HORIZONTAL • DATA DIAL / +/-");
                    }
                } else {
                    hardwareFocus = 0;
                    setBottomStatus(value0 != 0
                            ? "ZOOM VERTICAL • CONTEXT REQUIRED"
                            : "ZOOM HORIZONTAL • CONTEXT REQUIRED");
                }
                return;
            case MpcStudioMk2SemanticActions.COPY_CONTEXT:
                hardwareCopyDeleteActive = value1 != 0;
                hardwareCopyDeleteMode = value0;
                hardwareCopySourcePad = -1;
                hardwareCopyPadMask = 0;
                setHardwareButtonLed(122, hardwareCopyDeleteActive);
                clearHardwareCopyDeletePadLeds();
                if (!hardwareCopyDeleteActive) {
                    setBottomStatus("COPY/DELETE • CANCELLED");
                    return;
                }
                setBottomStatus(
                        value0 == 0
                                ? "COPY PAD • HOLD COPY • SOURCE → DESTINATION(S) → RELEASE"
                                : "DELETE PAD • HOLD SHIFT+COPY • SELECT PAD(S) → RELEASE");
                return;
            case MpcStudioMk2SemanticActions.COPY_PAD_SELECTION:
                if (!hardwareCopyDeleteActive || value1 < 0 || value1 >= 16) {
                    return;
                }
                final boolean selected = value2 != 0;
                if (value0 == 0) {
                    if (hardwareCopySourcePad < 0) {
                        hardwareCopySourcePad = value1;
                        selectedPad = value1;
                        refreshPadSelectionVisuals();
                        setHardwarePadRgb(value1, 127, 72, 0);
                        setBottomStatus(
                                "COPY PAD • SOURCE " + (value1 + 1)
                                        + " • SELECT DESTINATION(S)");
                    } else if (value1 != hardwareCopySourcePad) {
                        final int bit = 1 << value1;
                        hardwareCopyPadMask = selected
                                ? (hardwareCopyPadMask | bit)
                                : (hardwareCopyPadMask & ~bit);
                        setHardwarePadRgb(
                                value1,
                                selected ? 24 : 8,
                                selected ? 96 : 8,
                                selected ? 24 : 8);
                        setBottomStatus(
                                "COPY PAD • FROM " + (hardwareCopySourcePad + 1)
                                        + " • TO " + (value1 + 1)
                                        + (selected ? " ON" : " OFF"));
                    }
                } else {
                    final int bit = 1 << value1;
                    hardwareCopyPadMask = selected
                            ? (hardwareCopyPadMask | bit)
                            : (hardwareCopyPadMask & ~bit);
                    setHardwarePadRgb(
                            value1,
                            selected ? 127 : 8,
                            selected ? 16 : 8,
                            selected ? 16 : 8);
                    setBottomStatus(
                            "DELETE PAD • " + (value1 + 1)
                                    + (selected ? " SELECTED" : " DESELECTED"));
                }
                return;
            case MpcStudioMk2SemanticActions.COPY_PAD_COMMIT: {
                final int mode = value0;
                final int source = hardwareCopySourcePad;
                final int mask = hardwareCopyPadMask;
                hardwareCopyDeleteActive = false;
                hardwareCopySourcePad = -1;
                hardwareCopyPadMask = 0;
                setHardwareButtonLed(122, false);
                clearHardwareCopyDeletePadLeds();

                final String result;
                if (mode == 0) {
                    result = source >= 0 && mask != 0
                            ? nativeSequenceCopyPadToPads(source, mask)
                            : "COPY PAD • cancelled: select source and destination";
                } else {
                    result = mask != 0
                            ? nativeSequenceDeletePadAssignments(mask)
                            : "DELETE PAD • cancelled: select at least one pad";
                }
                setBottomStatus(result);
                refreshAllInspectorState();
                return;
            }
            case MpcStudioMk2SemanticActions.UNDO:
                final String historyResult = value0 != 0
                        ? nativeSequenceRedo()
                        : nativeSequenceUndo();
                setBottomStatus(historyResult);
                setHardwareButtonLed(
                        67,
                        nativeSequenceCanUndo() || nativeSequenceCanRedo());
                refreshAllInspectorState();
                refreshSequenceControls();
                return;
            default:
                setBottomStatus("MIDI CONTROL RESERVED • " + actionType);
        }
    }

    private void handleHardwareDialDelta(int delta, boolean fine) {
        if (delta == 0) return;
        if (hardwareLocateActive) {
            final int denominator = Math.max(1, nativeSequenceGetDenominator());
            final long beatTicks = Math.max(
                    1L, Math.round(960.0 * 4.0 / denominator));
            final long deltaTicks = fine
                    ? delta
                    : delta * beatTicks;
            setBottomStatus(nativeSequenceLocateMoveTicks(deltaTicks));
            refreshSequencePlayhead();
            return;
        }
        if ("SEQ".equals(currentPage) && sequenceStepButtons[0] != null) {
            adjustSelectedStepParameter(delta, fine);
            return;
        }
        if (hardwareFocus == 2) {
            navigationController.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
            final int count = nativeSequenceGetTrackCount();
            if (count <= 0) return;
            int next = nativeSequenceGetSelectedTrack() + delta;
            next %= count;
            if (next < 0) next += count;
            setBottomStatus(nativeSequenceSelectTrack(next));
            navigationController.setSelectedTrack(next);
            if (navigationController.state().mode() == MpcUiState.Mode.MAIN) {
                showTrackSelectPage();
            } else if (navigationController.state().mode() == MpcUiState.Mode.TRACK_VIEW) {
                showTrackViewPage();
            } else {
                showMainPage();
            }
            return;
        }
        if (hardwareFocus == 4) {
            final int track = nativeSequenceGetSelectedTrack();
            if (!"DRUM".equals(nativeSequenceGetTrackType(track))) {
                setBottomStatus("PROGRAM SELECT • DRUM TRACK REQUIRED");
                return;
            }
            final int count = nativeSequenceGetDrumProgramCount();
            if (count <= 0) {
                setBottomStatus("PROGRAM SELECT • NO PROGRAMS");
                return;
            }
            int current = nativeSequenceGetTrackProgramIndex(track);
            if (current < 0) current = 0;
            int next = current + delta;
            next %= count;
            if (next < 0) next += count;
            setBottomStatus(nativeSequenceSetTrackProgram(track, next));
            navigationController.setSelectedProgram(
                    nativeSequenceGetDrumProgramName(next));
            showProgramSelectPage();
            return;
        }
        if (hardwareFocus == 3) {
            navigationController.setSubcontext(MpcUiState.Subcontext.SEQUENCE_SELECT);
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
            setBottomStatus(
                    delta > 0 ? nativeSequenceNext() : nativeSequencePrevious());
            navigationController.setSelectedSequence(nativeSequenceGetIndex());
            if (navigationController.state().mode() == MpcUiState.Mode.MAIN) {
                showSequenceSelectPage();
            } else if (navigationController.state().mode() == MpcUiState.Mode.TRACK_VIEW) {
                showTrackViewPage();
            } else {
                showMainPage();
            }
            return;
        }
        if (hardwareFocus == 11 || hardwareFocus == 12) {
            if (sampleWaveform == null || !"SAMPLE".equals(currentPage)) {
                setBottomStatus("ZOOM • SAMPLE EDIT REQUIRED");
                return;
            }
            if (hardwareFocus == 11) {
                if (delta > 0) sampleWaveform.zoomIn();
                else sampleWaveform.zoomOut();
                setBottomStatus(
                        "ZOOM HORIZONTAL • "
                                + (delta > 0 ? "IN" : "OUT"));
            } else {
                if (delta > 0) sampleWaveform.zoomVerticalIn();
                else sampleWaveform.zoomVerticalOut();
                setBottomStatus(
                        "ZOOM VERTICAL • "
                                + (delta > 0 ? "IN" : "OUT"));
            }
            return;
        }
        if (hardwareFocus == 13 || hardwareFocus == 14) {
            if (sequenceGridView == null || !"SEQ".equals(currentPage)) {
                setBottomStatus("GRID ZOOM • SEQ GRID REQUIRED");
                return;
            }
            if (hardwareFocus == 13) {
                zoomSequenceGridHorizontal(delta);
            } else {
                zoomSequenceGridVertical(delta);
            }
            return;
        }
        if (hardwareFocus == 15) {
            if (sequenceTimeline == null || !"SEQ".equals(currentPage)) {
                setBottomStatus("TIMELINE ZOOM • SEQ REQUIRED");
                return;
            }
            if (delta > 0) sequenceTimeline.zoomIn();
            else sequenceTimeline.zoomOut();
            setBottomStatus(
                    "TIMELINE ZOOM H • "
                            + (delta > 0 ? "IN" : "OUT"));
            return;
        }
        setBottomStatus("DATA DIAL " + (delta > 0 ? "+" : "−") + " • no focused selector");
    }

    private String stepEditParameterLabel() {
        switch (stepEditParameter) {
            case STEP_EDIT_PARAMETER_PROBABILITY:
                return "PROB";
            case STEP_EDIT_PARAMETER_RATCHET:
                return "RAT";
            case STEP_EDIT_PARAMETER_NUDGE:
                return "NUDGE";
            case STEP_EDIT_PARAMETER_DURATION:
                return "DUR";
            case STEP_EDIT_PARAMETER_VELOCITY:
            default:
                return "VEL";
        }
    }

    private void adjustSelectedStepParameter(int direction, boolean fine) {
        if (selectedSequenceStep < 0) {
            setBottomStatus("Select a step first • DATA DIAL ENTER selects parameter");
            return;
        }

        final int[] parameters = getSelectedStepParameters();
        final int minimumParameters =
                stepEditParameter == STEP_EDIT_PARAMETER_NUDGE ? 4
                        : stepEditParameter == STEP_EDIT_PARAMETER_DURATION ? 5
                        : 3;
        if (parameters == null
                || parameters.length < minimumParameters
                || parameters[0] <= 0) {
            setBottomStatus("Select an active step first");
            return;
        }

        final int gridTicks = Math.max(1, nativeSequenceGetQuantizeGrid());
        final int delta = nativeStepEditParameterDelta(
                stepEditParameter, gridTicks, direction, fine);

        switch (stepEditParameter) {
            case STEP_EDIT_PARAMETER_VELOCITY: {
                final int value = Math.max(
                        1, Math.min(127, parameters[0] + delta));
                setBottomStatus(nativeSequenceSetStepVelocity(
                        selectedPad, selectedSequenceStep, gridTicks, value));
                break;
            }
            case STEP_EDIT_PARAMETER_PROBABILITY: {
                final int value = Math.max(
                        0, Math.min(127, parameters[1] + delta));
                setBottomStatus(nativeSequenceSetStepProbability(
                        selectedPad, selectedSequenceStep, gridTicks, value));
                break;
            }
            case STEP_EDIT_PARAMETER_RATCHET: {
                final int value = Math.max(
                        1, Math.min(8, parameters[2] + delta));
                setBottomStatus(nativeSequenceSetStepRatchet(
                        selectedPad, selectedSequenceStep, gridTicks, value));
                break;
            }
            case STEP_EDIT_PARAMETER_NUDGE: {
                setBottomStatus(nativeSequenceSetStepNudge(
                        selectedPad,
                        selectedSequenceStep,
                        gridTicks,
                        Math.max(-960, Math.min(960, parameters[3] + delta))));
                break;
            }
            case STEP_EDIT_PARAMETER_DURATION: {
                final int increment = Math.max(1, Math.abs(delta));
                final int current = Math.max(
                        increment, parameters[4] > 0 ? parameters[4] : gridTicks);
                final int value = Math.max(
                        increment,
                        Math.min(gridTicks * 4, current + delta));
                setBottomStatus(nativeSequenceSetStepDuration(
                        selectedPad, selectedSequenceStep, gridTicks, value));
                break;
            }
            default:
                return;
        }

        refreshSequenceStepPage();
        syncStepEditPadLeds();
        setBottomStatus(
                "STEP EDIT • " + stepEditParameterLabel()
                        + " • " + (direction > 0 ? "+" : "−")
                        + (fine ? " FINE" : ""));
    }

    private void handleHardwareTouchStrip(int value) {
        value = Math.max(0, Math.min(127, value));

        switch (hardwareTouchStripMode) {
            case TOUCH_STRIP_MODE_LEVEL:
                setBottomStatus(nativeAudioSetPadLevel(
                        selectedPad, value / 127.0f));
                refreshAllInspectorState();
                syncHardwareTouchStripValueLeds(value);
                return;
            case TOUCH_STRIP_MODE_PAN: {
                final float pan = (value / 127.0f) * 2.0f - 1.0f;
                setBottomStatus(nativeAudioSetPadPan(selectedPad, pan));
                refreshAllInspectorState();
                syncHardwareTouchStripValueLeds(value);
                return;
            }
            case TOUCH_STRIP_MODE_TUNE: {
                final float semitones = -24.0f + (48.0f * value / 127.0f);
                setBottomStatus(nativeAudioSetPadLayerTuning(
                        selectedPad, selectedLayer, semitones));
                refreshAllInspectorState();
                refreshSampleInfo();
                syncHardwareTouchStripValueLeds(value);
                return;
            }
            case TOUCH_STRIP_MODE_SAMPLE_START:
                handleHardwareTouchStripSampleRegion(value, true);
                return;
            case TOUCH_STRIP_MODE_SAMPLE_END:
                handleHardwareTouchStripSampleRegion(value, false);
                return;
            default:
                syncHardwareTouchStripModeLeds();
                return;
        }
    }

    private void handleHardwareTouchStripSampleRegion(
            int value, boolean start) {
        final long total = nativeAudioGetPadSampleFrameCount(
                selectedPad, selectedLayer);
        if (total <= 1) {
            setBottomStatus("TOUCH STRIP • no sample assigned");
            return;
        }

        final long currentStart = nativeAudioGetPadSampleRegionStart(
                selectedPad, selectedLayer);
        final long currentEnd = nativeAudioGetPadSampleRegionEnd(
                selectedPad, selectedLayer);

        if (start) {
            final long nextStart = Math.min(
                    currentEnd - 1L,
                    Math.round((value / 127.0)
                            * Math.max(0L, currentEnd - 1L)));
            setBottomStatus(nativeAudioSetPadSampleRegion(
                    selectedPad, selectedLayer, nextStart, currentEnd));
        } else {
            final long nextEnd = Math.max(
                    currentStart + 1L,
                    Math.round((value / 127.0)
                            * Math.max(1L, total - currentStart)
                            + currentStart));
            setBottomStatus(nativeAudioSetPadSampleRegion(
                    selectedPad, selectedLayer, currentStart, nextEnd));
        }

        refreshRegionInfo();
        if (sampleWaveform != null) {
            refreshSampleWaveform();
        }
        syncHardwareTouchStripValueLeds(value);
    }

    private String touchStripModeLabel(int mode) {
        switch (mode) {
            case TOUCH_STRIP_MODE_LEVEL: return "LEVEL";
            case TOUCH_STRIP_MODE_PAN: return "PAN";
            case TOUCH_STRIP_MODE_TUNE: return "TUNE";
            case TOUCH_STRIP_MODE_SAMPLE_START: return "SAMPLE START";
            case TOUCH_STRIP_MODE_SAMPLE_END: return "SAMPLE END";
            default: return "LEVEL";
        }
    }

    private void syncHardwareTouchStripValueLeds(int value) {
        if (midiBridge == null) return;
        final int clamped = Math.max(0, Math.min(127, value));
        final int segment = (clamped * 9) / 128;
        final String signature = "V:" + segment;
        if (signature.equals(lastTouchStripLedSignature)) return;
        for (int i = 0; i < 9; i++) {
            final byte[] message =
                    MpcStudioMk2MidiMessages.touchStripLedSegment(
                            i, i == segment ? 127 : 0);
            if (message != null) midiBridge.send(message);
        }
        lastTouchStripLedSignature = signature;
    }

    private void syncHardwareTouchStripModeLeds() {
        if (midiBridge == null) return;
        final int segment = Math.max(
                0, Math.min(8, hardwareTouchStripMode * 2));
        final String signature = "M:" + hardwareTouchStripMode;
        if (signature.equals(lastTouchStripLedSignature)) return;
        for (int i = 0; i < 9; i++) {
            final byte[] message =
                    MpcStudioMk2MidiMessages.touchStripLedSegment(
                            i, i == segment ? 127 : 0);
            if (message != null) midiBridge.send(message);
        }
        lastTouchStripLedSignature = signature;
    }

    private void syncHardwareNoteRepeatRateLeds(
            int rateIndex, boolean active) {
        if (midiBridge == null) return;
        final int clamped = Math.max(0, Math.min(7, rateIndex));
        final String signature = "R:" + (active ? 1 : 0) + ":" + clamped;
        if (signature.equals(lastNoteRepeatDivisionLedSignature)) return;
        for (int i = 0; i < 8; i++) {
            final byte[] message = MpcStudioMk2MidiMessages.noteRepeatLed(
                    i, active && i == clamped ? 127 : 0);
            if (message != null) midiBridge.send(message);
        }
        lastNoteRepeatDivisionLedSignature = signature;
    }

    private void setTouchStripButtonLed(boolean noteRepeatActive) {
        if (midiBridge == null) return;
        final int state = noteRepeatActive ? 4 : 3;
        if (state == lastTouchStripButtonLedState) return;
        midiBridge.send(MpcStudioMk2MidiMessages.buttonLed(0, state));
        lastTouchStripButtonLedState = state;
    }

    private void handleHardwarePlayheadMove(
            int locateMode, int direction, boolean bar) {
        if (locateMode != 0) {
            setBottomStatus(
                    bar
                            ? nativeSequenceMoveToLocateBoundary(direction)
                            : nativeSequenceMoveToPreviousOrNextEvent(direction));
            refreshSequencePlayhead();
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
            if (connected) {
                Arrays.fill(hardwareButtonLedStateCache, -1);
                lastTouchStripLedSignature = "";
                lastNoteRepeatDivisionLedSignature = "";
                lastLcdSignature = "";
                hardwareNoteRepeatActive = false;
                lastTouchStripButtonLedState = -1;
                syncHardwareTouchStripModeLeds();
                syncHardwareNoteRepeatRateLeds(
                        hardwareNoteRepeatRateIndex, false);
                setTouchStripButtonLed(false);
            }
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
        nativeSequenceSetStepEditContext(false, 0);

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