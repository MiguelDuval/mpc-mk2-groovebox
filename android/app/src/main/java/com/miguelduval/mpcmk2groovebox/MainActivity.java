package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.text.InputType;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
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

    private enum MainNumericField {
        BPM,
        BARS,
        LOOP_START,
        LOOP_END
    }

    private interface MainNumericCommitter {
        String commit(String value);
    }

    private static final String TAG = "MpcGroovebox";
    private static final int REQUEST_OPEN_WAV = 1001;
    private static final int REQUEST_RECORD_AUDIO = 1002;
    private static final int REQUEST_MONITOR_AUDIO = 1003;
    private static final int MAX_SAMPLE_BYTES = 32 * 1024 * 1024;
    private static final String SMOKE_MODE_EXTRA = "mpc.groovebox.smoke.mode";
    private static final boolean NATIVE_LIBRARY_LOADED;
    private static final String NATIVE_LIBRARY_ERROR;

    private static final int BG = Color.rgb(14, 16, 18);
    private static final int SURFACE = Color.rgb(25, 29, 33);

    /**
     * MPC-facing shell chrome uses compact rectangular surfaces rather than Android-style rounded cards.
     */
    private static final int MPC_FLAT_RADIUS_DP = 0;
    private static final int SURFACE_2 = Color.rgb(32, 37, 42);
    private static final int LINE = Color.rgb(64, 72, 80);
    private static final int TEXT = Color.rgb(235, 239, 242);
    private static final int MUTED = Color.rgb(156, 166, 174);
    private static final int ACCENT = Color.rgb(69, 211, 255);
    private static final int ACCENT_2 = Color.rgb(255, 180, 72);
    private static final int DANGER = Color.rgb(236, 83, 83);
    // MPC One / MPC3 visual language: vivid transport header over graphite UI.
    // These are presentation constants only; semantic state continues to use
    // the existing domain/UI-state model.
    // MPC3 Toolbar is dark graphite; red is a selection/accent, not the
    // persistent full-width Toolbar background.
    private static final int MPC_TOOLBAR_BG = Color.rgb(17, 19, 22);
    private static final int MPC_TOOLBAR_TEXT = Color.WHITE;
    private static final int MPC_SELECTION_RED = Color.rgb(224, 30, 61);
    private static final int MPC_PANEL = Color.rgb(39, 43, 47);
    private static final int MPC_PANEL_DARK = Color.rgb(28, 31, 34);
    private static final int MPC_PANEL_BORDER = Color.rgb(75, 82, 88);
    private static final int MPC_SELECTED = Color.rgb(235, 42, 68);
    private static final int MPC_TIME_SIGNATURE_HIGHLIGHT = Color.rgb(240, 194, 48);
    private static final int ACTIVE = Color.rgb(63, 207, 117);
    // MPC3 Toolbar geometry: fixed hit-target zones, with only the transport clock expanding.
    private static final int MPC_TOOLBAR_INSET_DP = 6;
    private static final int MPC_TOOLBAR_CONTROL_HEIGHT_DP = 34;
    private static final int MPC_TOOLBAR_GAP_DP = 2;
    private static final int MPC_TOOLBAR_MENU_WIDTH_DP = 38;
    private static final int MPC_TOOLBAR_PROJECT_IDENTITY_WIDTH_DP = 106;
    private static final int MPC_TOOLBAR_PROJECT_BROWSER_WIDTH_DP = 26;
    private static final int MPC_TOOLBAR_TIMING_WIDTH_DP = 60;
    private static final int MPC_TOOLBAR_METRO_WIDTH_DP = 58;
    private static final int MPC_TOOLBAR_AUTO_WIDTH_DP = 48;
    private static final int MPC_TOOLBAR_IO_WIDTH_DP = 40;

    // MPC One Main geometry: dense, edge-tight, and independent of legacy page spacing.
    private static final int MPC_MAIN_CONTENT_GUTTER_DP = 4;
    private static final int MPC_MAIN_SECTION_GAP_DP = 2;
    private static final int MPC_MAIN_FIELD_HEIGHT_DP = 40;
    private static final int MPC_MAIN_METRIC_HEIGHT_DP = 36;
    private static final int MPC_MAIN_TRACK_STATE_HEIGHT_DP = 40;
    private static final int MPC_MAIN_TRACK_HEADER_HEIGHT_DP = 36;
    private static final int MPC_MAIN_TRACK_TYPE_ICON_WIDTH_DP = 38;
    private static final int MPC_MAIN_PROGRAM_HEIGHT_DP = 32;
    private static final float MPC_MAIN_WORKSPACE_WEIGHT = 1.0f;
    private static final int MPC_MAIN_RADIUS_DP = 0;


    static {
        boolean loaded = false;
        String errorMessage = "";
        try {
            System.loadLibrary("mpcgroovebox");
            loaded = true;
        } catch (Throwable error) {
            errorMessage = error.getClass().getSimpleName() + ": "
                    + String.valueOf(error.getMessage());
            Log.e(TAG, "NATIVE_LIBRARY_LOAD_FAILED: " + errorMessage, error);
        }
        NATIVE_LIBRARY_LOADED = loaded;
        NATIVE_LIBRARY_ERROR = errorMessage;
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
    private TextView compactDialContext;
    private SequenceOverviewView compactSequenceOverviewView;
    private TextView compactTrackLevelLabel;
    private TextView compactTrackStateLabel;
    private TextView compactOutputContext;
    private TextView compactOutputLevelLabel;
    private android.widget.ProgressBar compactPadLevelMeter;
    private TextView compactPadLevelLabel;
    private TextView compactPadPanLabel;
    private TextView compactPadTuneLabel;
    private LinearLayout compactContextPanel;
    private LinearLayout compactMixerPanel;
    private Button compactMixerToggle;
    private Button compactMixerStripModeToggle;
    private TextView compactTrackCaption;
    private View compactTrackTabs;
    private TextView compactPadCaption;
    private View compactPadTabs;
    private AlertDialog activeMpcParameterDialog;
    private Button timingCorrectTopButton;
    private Button metronomeTopButton;
    private Button automationTopButton;
    private TextView midiInTopStatus;
    private TextView midiOutTopStatus;
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
    private Button mainTrackSamplePrimaryButton;
    private Button mainTrackSampleActionButton;
    private MpcTrackEditView mainTrackEditView;
    private MpcPadMixerView padMixerView;
    private MpcMainMixerStripView mainMixerStripView;
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
    private TextView mainSequenceNameField;
    private TextView mainSequenceTypeField;
    private TextView mainSequenceBpmField;
    private TextView mainSequenceBarsField;
    private TextView mainSequenceStartField;
    private TextView mainSequenceEndField;
    private TextView mainSequenceTimeSigField;
    private TextView mainSequenceLoopField;
    private TextView mainSequenceTransposeField;
    private TextView mainTrackField;
    private View mainTrackTypeField;
    private TextView mainTrackLayerField;
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
    private static final int HARDWARE_FOCUS_SEQUENCE_START = 17;
    private static final int HARDWARE_FOCUS_SEQUENCE_END = 18;
    private static final int HARDWARE_FOCUS_SEQUENCE_BPM = 19;
    private static final int HARDWARE_FOCUS_SEQUENCE_BARS = 20;

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
    private boolean hardwareFullLevelActive;
    private boolean hardwareHalfLevelActive;
    private boolean hardwareSixteenLevelActive;
    private boolean hardwarePadMuteModeActive;
    private boolean hardwareTrackMuteModeActive;
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

        try {
            applyFullscreenWindowPolicy();
            navigationController = new MpcNavigationController(
                    uiState -> runOnUiThread(this::updateMpcShellState));
        } catch (Throwable error) {
            Log.e(TAG, "EARLY_STARTUP_SETUP_FAILED", error);
            showStartupFailure("Early startup setup failed", error);
            return;
        }

        try {
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
        } catch (Throwable error) {
            Log.e(TAG, "AUDIO_DEVICE_CALLBACK_UNAVAILABLE", error);
            audioManager = null;
            audioDeviceCallback = null;
        }

        Arrays.fill(hardwareButtonLedStateCache, -1);
        try {
            setContentView(buildApplicationShell());
            applyFullscreenWindowPolicy();
        } catch (Throwable error) {
            Log.e(TAG, "UI_SHELL_BUILD_FAILED", error);
            showStartupFailure("UI shell startup failed", error);
            return;
        }
        Log.i(TAG, "UI_READY");

        if (!NATIVE_LIBRARY_LOADED) {
            final String failure = "Native engine unavailable: " + NATIVE_LIBRARY_ERROR;
            final String[] supportedAbis = Build.SUPPORTED_ABIS;
            final String abiText = supportedAbis == null
                    ? "unknown"
                    : Arrays.toString(supportedAbis);
            final String diagnostic = failure + " | ABIs=" + abiText;
            if (bottomStatus != null) {
                bottomStatus.setText(diagnostic);
            }
            Log.e(TAG, "UI_NATIVE_UNAVAILABLE: " + diagnostic);
            if (uiOnlySmokeMode) {
                Log.i(TAG, "UI_ONLY_COMPLETE");
                return;
            }
            return;
        }

        if (uiOnlySmokeMode) {
            bottomStatus.setText("UI-only startup diagnostic");
            Log.i(TAG, "UI_ONLY_COMPLETE");
            return;
        }

        content.postOnAnimation(() -> {
            Log.i(TAG, "STARTUP_BEGIN");
            startupExecutor.execute(() -> {
                try {
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
                        try {
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
                            try {
                                midiBridge = new AndroidMidiBridge(this, this);
                                Log.i(TAG, "MIDI_BRIDGE_END");
                            } catch (Throwable error) {
                                Log.e(TAG, "MIDI_BRIDGE_STARTUP_FAILED", error);
                                setBottomStatus("MIDI unavailable • " + error.getClass().getSimpleName());
                            }
                            Log.i(TAG, "STARTUP_COMPLETE");

                            if (uiAuditSmokeMode) {
                                runUiAudit();
                            }
                        } catch (Throwable error) {
                            Log.e(TAG, "UI_STARTUP_FINALIZATION_FAILED", error);
                            showStartupFailure("UI startup finalization failed", error);
                        }
                    });
                } catch (Throwable error) {
                    Log.e(TAG, "STARTUP_NATIVE_FAILED", error);
                    runOnUiThread(() -> showStartupFailure("Native startup failed", error));
                }
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

        /*
         * The MPC shell is the real instrument surface. Keep it MATCH_PARENT
         * rather than relying on a weighted zero-height child: this avoids
         * device-specific measure passes producing a blank/preview-colored
         * window before the nested shell has a concrete height.
         */
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.addView(mpcShell.root(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        /*
         * These two views are diagnostics, not part of the MPC Main surface.
         * Keep them available only to UI-audit runs and pin them to the bottom
         * as an overlay so they cannot participate in shell measurement.
         */
        hardwareFeedbackView.setVisibility(
                uiAuditSmokeMode ? View.VISIBLE : View.GONE);
        bottomStatus.setVisibility(
                uiAuditSmokeMode ? View.VISIBLE : View.GONE);

        FrameLayout.LayoutParams feedbackLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                uiAuditSmokeMode ? dp(24) : 0,
                Gravity.BOTTOM);
        feedbackLp.bottomMargin = uiAuditSmokeMode ? dp(28) : 0;
        root.addView(hardwareFeedbackView, feedbackLp);

        FrameLayout.LayoutParams statusLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                uiAuditSmokeMode ? dp(28) : 0,
                Gravity.BOTTOM);
        root.addView(bottomStatus, statusLp);

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
            Button shortcut = mpcShortcutButton(
                    mpcShortcutLabel(mode), mode);
            shortcut.setContentDescription("MPC shortcut " + mode.label());
            shortcut.setTextSize(18);
            shortcut.setTypeface(Typeface.DEFAULT_BOLD);
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
        area.setContentDescription("MPC shell mixer strips");

        LinearLayout header = row();
        android.widget.Space headerSpacer = new android.widget.Space(this);
        header.addView(headerSpacer, new LinearLayout.LayoutParams(
                0, dp(24), 1));

        compactMixerToggle = actionButton("◉", v -> {
            final MpcUiState state = navigationController.state();
            navigationController.setCompactMixerState(
                    !state.compactMixerVisible(),
                    state.compactMixerPadMode());
        });
        compactMixerToggle.setTextSize(12);
        compactMixerToggle.setContentDescription(
                "MPC condensed Mixer Strip show or hide");
        header.addView(compactMixerToggle,
                new LinearLayout.LayoutParams(dp(34), dp(24)));

        area.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        // Persistent context: this remains visible when Mixer Strip details are hidden.
        compactContextPanel = column();
        compactContextPanel.setContentDescription(
                "MPC shell compact track program context");
        compactContextPanel.setPadding(dp(2), dp(2), dp(2), dp(2));
        compactContextPanel.setBackgroundColor(MPC_PANEL_DARK);

        compactContextPanel.addView(compactContextCaption("SEQUENCE"),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        compactSequenceContext = compactContextField(
                "SEQ 01\n120.0 BPM",
                "MPC shell sequence context",
                v -> {
                    if (startupComplete) {
                        showSequenceSelectPage();
                    } else {
                        setBottomStatus("SEQUENCE SELECT • waiting for sequencer");
                    }
                });
        compactContextPanel.addView(compactSequenceContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(36)));

        compactTrackCaption = compactContextCaption("TRACK");
        compactContextPanel.addView(compactTrackCaption,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        compactTrackContext = compactContextField(
                "TRACK 01 • DRUM",
                "MPC shell track context",
                v -> focusMainTrackField());
        compactContextPanel.addView(compactTrackContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        compactContextPanel.addView(compactContextCaption("PROGRAM"),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        compactProgramContext = compactContextField(
                "PROGRAM\n—",
                "MPC shell program context",
                v -> {
                    if (!startupComplete) {
                        setBottomStatus("PROGRAM SELECT • waiting for sequencer");
                        return;
                    }
                    final int selectedTrack = Math.max(
                            0, nativeSequenceGetSelectedTrack());
                    if ("DRUM".equalsIgnoreCase(
                            nativeSequenceGetTrackType(selectedTrack))) {
                        showProgramSelectPage();
                    } else {
                        setBottomStatus(
                                "PROGRAM SELECT • TRACK TYPE IS NOT DRUM");
                    }
                });
        compactContextPanel.addView(compactProgramContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        compactPadCaption = compactContextCaption("PAD");
        compactContextPanel.addView(compactPadCaption,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        compactPadContext = compactContextField(
                "PAD 01 • BANK A",
                "MPC shell pad mixer strip",
                null);
        compactContextPanel.addView(compactPadContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(34)));

        compactContextPanel.addView(compactContextCaption("DATA DIAL"),
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));

        compactDialContext = compactContextField(
                "DIAL\nNONE",
                "MPC shell Data Dial focus",
                null);
        compactContextPanel.addView(compactDialContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        compactSequenceOverviewView = new SequenceOverviewView(this);
        compactSequenceOverviewView.setContentDescription(
                "MPC shell sequence overview");
        compactContextPanel.addView(compactSequenceOverviewView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(28)));

        // Mixer detail layer: visibility state applies only to these controls.
        compactMixerPanel = column();
        compactMixerPanel.setContentDescription(
                "MPC condensed Mixer Strip");
        compactMixerPanel.setPadding(dp(2), dp(2), dp(2), dp(2));
        compactMixerPanel.setBackgroundColor(MPC_PANEL);

        LinearLayout trackTabs = buildCompactMixerTabs();
        compactTrackTabs = trackTabs;
        compactMixerPanel.addView(trackTabs,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        compactTrackStateLabel = label(
                "REC OFF • MUTE OFF • SOLO —",
                8,
                MUTED);
        compactTrackStateLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactTrackStateLabel.setGravity(Gravity.CENTER_VERTICAL);
        compactTrackStateLabel.setPadding(dp(4), 0, dp(4), 0);
        compactTrackStateLabel.setContentDescription(
                "MPC shell Track record mute and solo state");
        compactMixerPanel.addView(compactTrackStateLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(25)));

        compactTrackLevelLabel = label("LEVEL —", 8, MUTED);
        compactTrackLevelLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactTrackLevelLabel.setGravity(Gravity.CENTER_VERTICAL);
        compactTrackLevelLabel.setPadding(dp(4), 0, dp(4), 0);
        compactTrackLevelLabel.setContentDescription(
                "MPC shell track level");
        compactMixerPanel.addView(compactTrackLevelLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        compactPadTabs = buildCompactMixerTabs();
        compactMixerPanel.addView(compactPadTabs,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(24)));

        compactPadLevelMeter = new android.widget.ProgressBar(
                this, null, android.R.attr.progressBarStyleHorizontal);
        compactPadLevelMeter.setMax(100);
        compactPadLevelMeter.setProgress(100);
        compactPadLevelMeter.setContentDescription(
                "MPC Main Mixer Strip level meter");
        compactMixerPanel.addView(compactPadLevelMeter,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(16)));

        compactPadLevelLabel = label("LEVEL 100%", 8, TEXT);
        compactPadLevelLabel.setGravity(Gravity.CENTER_VERTICAL);
        compactPadLevelLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadLevelLabel.setContentDescription(
                "MPC Main Mixer Strip level");
        compactMixerPanel.addView(compactPadLevelLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        LinearLayout padMixValues = row();
        compactPadPanLabel = label("PAN C", 8, MUTED);
        compactPadPanLabel.setGravity(Gravity.CENTER);
        compactPadPanLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadPanLabel.setContentDescription(
                "MPC Main Mixer Strip pad pan");
        compactPadPanLabel.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        padMixValues.addView(compactPadPanLabel,
                new LinearLayout.LayoutParams(0, dp(24), 1));

        compactPadTuneLabel = label("TUNE +0.0", 8, MUTED);
        compactPadTuneLabel.setGravity(Gravity.CENTER);
        compactPadTuneLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactPadTuneLabel.setContentDescription(
                "MPC Main Mixer Strip pad tuning");
        compactPadTuneLabel.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        padMixValues.addView(compactPadTuneLabel,
                new LinearLayout.LayoutParams(0, dp(24), 1));

        compactMixerPanel.addView(padMixValues,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(26)));

        compactOutputContext = label("MAIN OUT", 8, MUTED);
        compactOutputContext.setTypeface(Typeface.DEFAULT_BOLD);
        compactOutputContext.setGravity(Gravity.CENTER_VERTICAL);
        compactOutputContext.setPadding(dp(3), dp(3), dp(3), 0);
        compactMixerPanel.addView(compactOutputContext,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));

        compactOutputLevelLabel = label("LEVEL —  •  RESERVED", 8, MUTED);
        compactOutputLevelLabel.setTypeface(Typeface.DEFAULT_BOLD);
        compactOutputLevelLabel.setGravity(Gravity.CENTER_VERTICAL);
        compactOutputLevelLabel.setPadding(dp(4), 0, dp(4), 0);
        compactOutputLevelLabel.setContentDescription(
                "MPC shell main output level reserved");
        compactMixerPanel.addView(compactOutputLevelLabel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(23)));

        area.addView(compactContextPanel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        area.addView(compactMixerPanel,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        /*
         * Fidelity correction: the visible Main left workspace is the MPC
         * XL Mixer Strip region, not a persistent Android-style sequence /
         * track / program / dial context card. The legacy views stay mounted
         * but hidden during migration so their semantic refresh dependencies
         * can be retired safely in a later cleanup pass.
         */
        header.setVisibility(View.GONE);
        compactContextPanel.setVisibility(View.GONE);
        compactMixerPanel.setVisibility(View.GONE);

        mainMixerStripView = new MpcMainMixerStripView(this,
                new MpcMainMixerStripView.Listener() {
                    @Override
                    public boolean isStartupReady() {
                        return startupComplete;
                    }

                    @Override
                    public boolean mixerStripVisible() {
                        return navigationController != null
                                && navigationController.state().compactMixerVisible();
                    }

                    @Override
                    public void toggleTrackMute() {
                        toggleSelectedTrackMute();
                    }

                    @Override
                    public void onMixerStripVisibilityChanged(boolean visible) {
                        final boolean padMode =
                                navigationController != null
                                        && navigationController.state().compactMixerPadMode();
                        navigationController.setCompactMixerState(visible, padMode);
                    }
                });
        area.addView(mainMixerStripView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        applyCompactMixerVisibility();
        applyCompactMixerStripMode();
        refreshMpcMainMixerStripView();
    }

    private boolean compactMixerStripModeAvailable() {
        if (!startupComplete) {
            return false;
        }
        final int trackIndex = Math.max(
                0, nativeSequenceGetSelectedTrack());
        return "DRUM".equalsIgnoreCase(
                nativeSequenceGetTrackType(trackIndex));
    }

    private boolean compactMixerPadModeForDisplay() {
        return navigationController != null
                && navigationController.state().compactMixerPadMode()
                && compactMixerStripModeAvailable();
    }

    private void applyCompactMixerStripMode() {
        if (compactMixerStripModeToggle == null) {
            return;
        }

        final boolean padMode = compactMixerPadModeForDisplay();
        compactMixerStripModeToggle.setText(padMode ? "□  ▦" : "■  ▦");
        compactMixerStripModeToggle.setContentDescription(
                padMode
                        ? "MPC condensed Mixer Strip showing Pad"
                        : "MPC condensed Mixer Strip showing Track");

        if (compactTrackStateLabel != null) {
            compactTrackStateLabel.setVisibility(padMode ? View.GONE : View.VISIBLE);
        }
        if (compactTrackLevelLabel != null) {
            compactTrackLevelLabel.setVisibility(padMode ? View.GONE : View.VISIBLE);
        }
        if (compactTrackTabs != null) {
            compactTrackTabs.setVisibility(padMode ? View.GONE : View.VISIBLE);
        }
        if (compactPadTabs != null) {
            compactPadTabs.setVisibility(padMode ? View.VISIBLE : View.GONE);
        }
        if (compactPadLevelMeter != null) {
            compactPadLevelMeter.setVisibility(padMode ? View.VISIBLE : View.GONE);
        }
        if (compactPadLevelLabel != null) {
            compactPadLevelLabel.setVisibility(padMode ? View.VISIBLE : View.GONE);
        }
        if (compactPadPanLabel != null) {
            compactPadPanLabel.setVisibility(padMode ? View.VISIBLE : View.GONE);
        }
        if (compactPadTuneLabel != null) {
            compactPadTuneLabel.setVisibility(padMode ? View.VISIBLE : View.GONE);
        }
    }

    private TextView compactContextCaption(String text) {
        TextView caption = label(text, 8, MUTED);
        caption.setGravity(Gravity.BOTTOM | Gravity.START);
        caption.setTypeface(Typeface.DEFAULT_BOLD);
        caption.setPadding(dp(3), 0, dp(3), 0);
        caption.setContentDescription("MPC shell context section " + text);
        return caption;
    }

    private TextView compactContextField(
            String initialText,
            String contentDescription,
            View.OnClickListener listener) {
        TextView field = label(initialText, 9, TEXT);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(6), 0, dp(6), 0);
        field.setTypeface(Typeface.DEFAULT_BOLD);
        field.setContentDescription(contentDescription);
        field.setBackground(strokeBackground(
                MPC_PANEL,
                MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));        if (listener != null) {            field.setOnClickListener(listener);            field.setFocusable(true);
            field.setClickable(true);
        }
        return field;
    }

    private void setCompactContextFocus(View view, boolean active) {
        if (view == null) return;
        view.setBackground(strokeBackground(
                SURFACE_2,
                active ? DANGER : LINE,
                MPC_FLAT_RADIUS_DP));
    }

    private String compactDialFocusLabel(MpcUiState.DataDialFocus focus) {
        if (focus == null) return "NONE";
        switch (focus) {
            case SEQUENCE:
                return "SEQUENCE";
            case SEQUENCE_START:
                return "SEQ START";
            case SEQUENCE_END:
                return "SEQ END";
            case SEQUENCE_BPM:
                return "BPM";
            case SEQUENCE_BARS:
                return "BARS";
            case TRACK:
                return "TRACK";
            case PROGRAM:
                return "PROGRAM";
            case TRACK_TYPE:
                return "TRACK TYPE";
            case PAD:
                return "PAD";
            case PAD_MIXER_LEVEL:
                return "MIX LEVEL";
            case PAD_MIXER_PAN:
                return "MIX PAN";
            case PAD_MIXER_TUNE:
                return "MIX TUNE";
            case SAMPLE_LAYER:
                return "LAYER";
            default:
                return focus.name().replace('_', ' ');
        }
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
            case TRACK_EDIT:
                openMainTrackEditContext();
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
                navigationController.navigate(mode);
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
        timingCorrectTopButton.setText(
                enabled
                        ? "TC " + sequenceGridLabel(
                                nativeSequenceGetQuantizeGrid()).replace("Q ", "")
                        : "TC OFF");
        timingCorrectTopButton.setTextColor(MPC_TOOLBAR_TEXT);
        timingCorrectTopButton.setBackground(strokeBackground(
                enabled ? Color.rgb(183, 35, 57) : Color.TRANSPARENT,
                enabled ? Color.WHITE : Color.TRANSPARENT,
                MPC_FLAT_RADIUS_DP));

        if (metronomeTopButton != null) {
            metronomeTopButton.setText("METRO");
        }
        if (automationTopButton != null) {
            automationTopButton.setText("AUTO");
        }

        final boolean midiReady = midiBridge != null;
        updateTopMidiStatus(midiInTopStatus, midiReady);
        updateTopMidiStatus(midiOutTopStatus, midiReady);
    }

    private void applyCompactMixerVisibility() {
        if (compactMixerPanel == null || compactMixerToggle == null) {
            return;
        }

        final boolean visible = navigationController != null
                && navigationController.state().compactMixerVisible();
        compactMixerPanel.setVisibility(
                visible ? View.VISIBLE : View.GONE);
        compactMixerToggle.setText(
                visible ? "◉" : "○");
        compactMixerToggle.setTextSize(12);
        compactMixerToggle.setTextColor(
                visible ? BG : TEXT);
        compactMixerToggle.setBackground(strokeBackground(
                visible ? ACCENT : SURFACE_2,
                visible ? ACCENT : LINE,
                MPC_FLAT_RADIUS_DP));
    }

    private void refreshMpcCompactContext() {
        if (navigationController == null
                || compactSequenceContext == null
                || compactTrackContext == null
                || compactProgramContext == null
                || compactPadContext == null
                || compactDialContext == null
                || compactSequenceOverviewView == null) {
            return;
        }

        final boolean nativeStateReady = startupComplete;
        final int trackIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final int track = trackIndex + 1;
        final int sequenceIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetIndex())
                : 0;
        final int sequenceCount = nativeStateReady
                ? Math.max(1, nativeSequenceGetCount())
                : 1;
        final double tempo = nativeStateReady
                ? nativeSequenceGetTempo()
                : 120.0;

        compactSequenceContext.setText(String.format(
                Locale.ROOT,
                "SEQ %02d\n%.1f BPM",
                Math.min(sequenceCount - 1, sequenceIndex) + 1,
                tempo));

        final MpcUiState shellState = navigationController.state();
        final MpcUiState.DataDialFocus dialFocus = shellState.dataDialFocus();
        final String dialLabel = dialFocus == null
                ? "NONE"
                : dialFocus.name().replace('_', ' ');
        final MpcUiState.Subcontext dialSubcontext = shellState.subcontext();
        final String subcontextLabel = dialSubcontext == null
                ? ""
                : dialSubcontext.name().replace('_', ' ');
        compactDialContext.setText(
                "DIAL\n" + compactDialFocusLabel(dialFocus));
        compactDialContext.setContentDescription(
                "MPC shell Data Dial focus • " + dialLabel
                        + (subcontextLabel.isEmpty()
                                ? ""                                : " • " + subcontextLabel));

        final boolean sequenceFocus =
                dialFocus == MpcUiState.DataDialFocus.SEQUENCE
                        || dialFocus == MpcUiState.DataDialFocus.SEQUENCE_START
                        || dialFocus == MpcUiState.DataDialFocus.SEQUENCE_END
                        || dialFocus == MpcUiState.DataDialFocus.SEQUENCE_BPM
                        || dialFocus == MpcUiState.DataDialFocus.SEQUENCE_BARS;
        final boolean trackFocus =
                dialFocus == MpcUiState.DataDialFocus.TRACK
                        || dialFocus == MpcUiState.DataDialFocus.TRACK_TYPE
                        || dialFocus == MpcUiState.DataDialFocus.SAMPLE_LAYER;
        final boolean programFocus =
                dialFocus == MpcUiState.DataDialFocus.PROGRAM;
        final boolean padFocus =
                dialFocus == MpcUiState.DataDialFocus.PAD;
        setCompactContextFocus(compactSequenceContext, sequenceFocus);
        setCompactContextFocus(compactTrackContext, trackFocus);
        setCompactContextFocus(compactProgramContext, programFocus);
        setCompactContextFocus(compactPadContext, padFocus);
        setCompactContextFocus(
                compactDialContext,
                dialFocus != MpcUiState.DataDialFocus.NONE);

        // In MPC Main the selected channel/track strip is a strong visual
        // anchor. Keep the persistent Track context visibly selected while
        // retaining a white focus border when Data Dial is on Track.
        compactTrackContext.setTextColor(MPC_TOOLBAR_TEXT);
        compactTrackContext.setBackground(strokeBackground(
                MPC_SELECTION_RED,
                trackFocus ? Color.WHITE : Color.rgb(183, 35, 57),
                MPC_FLAT_RADIUS_DP));

        if (nativeStateReady) {
            final String trackType = nativeSequenceGetTrackType(trackIndex);
            final boolean drumTrack = "DRUM".equalsIgnoreCase(trackType);
            if (compactMixerStripModeToggle != null) {
                compactMixerStripModeToggle.setEnabled(drumTrack);
                compactMixerStripModeToggle.setAlpha(drumTrack ? 1.0f : 0.45f);
            }
            final boolean armed = nativeSequenceIsSelectedTrackArmed();
            final boolean muted = nativeSequenceIsTrackMuted(trackIndex);
            compactTrackContext.setText(String.format(
                    Locale.ROOT,
                    "TRACK %02d • %s\nREC %s • MUTE %s",
                    track,
                    trackType,
                    armed ? "ON" : "OFF",
                    muted ? "ON" : "OFF"));

            if (compactTrackStateLabel != null) {
                compactTrackStateLabel.setText(
                        "REC " + (armed ? "ON" : "OFF")
                                + " • MUTE " + (muted ? "ON" : "OFF")
                                + " • SOLO —");
            }

            if (compactProgramContext != null) {
                String program = nativeSequenceGetTrackProgram(trackIndex);
                if (program == null || program.trim().isEmpty()) {
                    program = "—";
                } else {
                    program = program.replace("PROGRAM • ", "").trim();
                }
                compactProgramContext.setText("PROGRAM\n" + program);
                compactProgramContext.setEnabled(drumTrack);
                compactProgramContext.setAlpha(drumTrack ? 1.0f : 0.48f);
                compactProgramContext.setContentDescription(
                        drumTrack
                                ? "MPC shell program context"
                                : "MPC shell program context unavailable");
            }

            if (compactTrackLevelLabel != null) {
                compactTrackLevelLabel.setText(
                        "LEVEL —  •  TRACK MIXER RESERVED");
            }
        } else {
            compactSequenceContext.setText("SEQ 01\n120.0 BPM");
            compactDialContext.setText("DIAL\nNONE");
            compactDialContext.setContentDescription(
                    "MPC shell Data Dial focus • NONE");
            compactTrackContext.setText(
                    "TRACK 01 • DRUM\nREC OFF • MUTE OFF");
            if (compactProgramContext != null) {
                compactProgramContext.setText("PROGRAM\n—");
                compactProgramContext.setEnabled(false);
                compactProgramContext.setAlpha(0.45f);
                compactProgramContext.setContentDescription(
                        "MPC shell program context unavailable");
            }

            if (compactMixerStripModeToggle != null) {
                compactMixerStripModeToggle.setEnabled(false);
                compactMixerStripModeToggle.setAlpha(0.45f);
            }
            compactTrackContext.setText("TRACK 01 • DRUM");
            if (compactTrackStateLabel != null) {
                compactTrackStateLabel.setText("REC OFF • MUTE OFF • SOLO —");
            }
            if (compactTrackLevelLabel != null) {
                compactTrackLevelLabel.setText(
                        "LEVEL —  •  TRACK MIXER RESERVED");
            }
        }

        final char padBank = (char) ('A' + Math.max(
                0, Math.min(7, navigationController.state().padBank())));
        compactPadContext.setText(String.format(
                Locale.ROOT, "PAD %02d • BANK %s",
                selectedPad + 1, padBank));

        if (compactPadLevelMeter != null && compactPadLevelLabel != null) {
            final int levelPercent = nativeStateReady
                    ? Math.max(0, Math.min(100,
                            Math.round(nativeAudioGetPadLevel(selectedPad) * 100.0f)))
                    : 100;
            compactPadLevelMeter.setProgress(levelPercent);
            compactPadLevelLabel.setText(String.format(
                    Locale.ROOT,
                    "LEVEL %d%%",
                    levelPercent));

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
                        "TUNE %+.1f",
                        tune));
            }
        }

        if (compactOutputLevelLabel != null) {
            compactOutputLevelLabel.setText("LEVEL —  •  RESERVED");
        }
        applyCompactMixerStripMode();
        refreshMpcMainMixerStripView();
    }

    private void refreshMpcMainMixerStripView() {
        if (mainMixerStripView == null || navigationController == null) {
            return;
        }

        final boolean ready = startupComplete;
        final int track = ready
                ? Math.max(0, nativeSequenceGetSelectedTrack())
                : 0;
        final String trackType = ready
                ? nativeSequenceGetTrackType(track)
                : "DRUM";
        final String program = ready
                ? nativeSequenceGetTrackProgram(track)
                : "—";
        final boolean padMode = navigationController.state().compactMixerPadMode()
                && "DRUM".equalsIgnoreCase(trackType);
        final int pad = selectedPadIndexForUi();
        final float level = ready ? nativeAudioGetPadLevel(pad) : 1.0f;
        final float pan = ready ? nativeAudioGetPadPan(pad) : 0.0f;
        final String sample = ready
                ? nativeAudioGetPadSampleName(pad, selectedLayer)
                : "NO SAMPLE";
        final boolean muted = ready && nativeSequenceIsTrackMuted(track);

        mainMixerStripView.setState(
                navigationController.state().compactMixerVisible(),
                padMode,
                compactDialFocusLabel(navigationController.state().dataDialFocus()),
                track,
                trackType,
                cleanTrackDisplayName(
                        ready ? nativeSequenceTrackStatus(track) : "Track"),
                program,
                pad,
                level,
                pan,
                sample,
                muted);
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
            addFunction("+ NEW TRACK", true, v -> addSequenceTrack(0));
            addSequenceRecArmFunction(
                    trackCount > 0,
                    () -> {
                        setBottomStatus(nativeSequenceSetSelectedTrackArmed(
                                !nativeSequenceIsSelectedTrackArmed()));
                        syncHardwareTransportLeds();
                        showMainPage();
                    });
            addTrackStepperFunction(trackCount > 0);
            addFunction("MUTE", trackCount > 0,
                    v -> toggleSelectedTrackMute());
            addFunction("SOLO", false, null);
            return;
        }

        if (mode == MpcUiState.Mode.TRACK_VIEW) {
            addFunction("NEW TRACK", true, v -> addSequenceTrack(0));
            addSequenceRecArmFunction(
                    trackCount > 0,
                    () -> {
                        setBottomStatus(nativeSequenceSetSelectedTrackArmed(
                                !nativeSequenceIsSelectedTrackArmed()));
                        syncHardwareTransportLeds();
                        showTrackViewPage();
                    });
            addTrackStepperFunction(trackCount > 0);
            addFunction("MUTE", trackCount > 0,
                    v -> toggleSelectedTrackMute());
            addFunction("SOLO", false, null);
            return;
        }

        if (mode == MpcUiState.Mode.MENU) {
            // Keep all Menu system commands on the global shell Function Bar.
            addFunction("NEW PROJECT", false, null);
            addFunction("SAVE", false, null);
            addFunction("PREFERENCES", true, v -> showAudioSettingsPage());
            addFunction("MIDI / CONTROL", true, v -> showMidiPage());
            addFunction("EDIT SHORTCUTS", true, v -> showShortcutConfigPage());
            addFunction("BACK", true, v -> navigateBackFromShell());
            return;
        }

        if (mode == MpcUiState.Mode.TRACK_EDIT) {
            final boolean drumTrack = startupComplete
                    && "DRUM".equalsIgnoreCase(
                            nativeSequenceGetTrackType(
                                    Math.max(0, nativeSequenceGetSelectedTrack())));
            addFunction("BACK", true, v -> navigateBackFromShell());
            addFunction("AUDITION", drumTrack,
                    v -> selectAndTriggerPad(selectedPadIndexForUi(), 112));
            addFunction("LAYER −", drumTrack, v -> adjustTrackEditLayer(-1));
            addFunction("LAYER +", drumTrack, v -> adjustTrackEditLayer(1));
            addFunction("MAIN", true, v -> showMainPage());
            return;
        }

        switch (mode) {
            case BROWSER:
                // MPC Browser's primary bottom controls are Sample Assign,
                // Audition and Open/Load. Keep the remaining Function Bar
                // slots contextual rather than inventing Browser operations.
                addFunction("SAMPLE ASSIGN", false, null);
                addFunction("AUDITION", true,
                        v -> selectAndTriggerPad(selectedPadIndexForUi(), 112));
                addFunction("LOAD", true, v -> openWavPicker());
                addFunction("UP", true, v -> showBrowserPage());
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

            case PAD_MIXER: {
                final int currentPad = selectedPadIndexForUi();
                addFunction("PAD −", currentPad > 0, v -> {
                    selectedPad = Math.max(0, selectedPad - 1);
                    navigationController.setSelectedPad(selectedPad);
                    navigationController.setDataDialFocus(
                            MpcUiState.DataDialFocus.PAD);
                    showMixPage();
                });
                addFunction("PAD +", currentPad < 15, v -> {
                    selectedPad = Math.min(15, selectedPad + 1);
                    navigationController.setSelectedPad(selectedPad);
                    navigationController.setDataDialFocus(
                            MpcUiState.DataDialFocus.PAD);
                    showMixPage();
                });
                addFunction("TRACK EDIT",
                        startupComplete
                                && "DRUM".equalsIgnoreCase(
                                        nativeSequenceGetTrackType(
                                                Math.max(0, nativeSequenceGetSelectedTrack()))),
                        v -> openMainTrackEditContext());
                addFunction("MAIN", true, v -> showMainPage());
                addFunction("BROWSER", true, v -> showBrowserPage());
                addFunction("BACK", true, v -> navigateBackFromShell());
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

    private void addSequenceRecArmFunction(
            boolean enabled,
            Runnable action) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER_VERTICAL);
        group.setContentDescription("MPC Function Bar REC ARM");
        group.setPadding(dp(2), dp(2), dp(2), dp(2));
        group.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));

        TextView seq = label("SEQ", 8, MUTED);
        seq.setGravity(Gravity.CENTER);
        seq.setTypeface(Typeface.DEFAULT_BOLD);
        seq.setContentDescription("MPC Main sequence record scope");
        group.addView(seq, new LinearLayout.LayoutParams(
                dp(26), ViewGroup.LayoutParams.MATCH_PARENT));

        Button recArm = actionButton("REC ARM", v -> {
            if (action != null) {
                action.run();
            }
        });
        final boolean armed = enabled
                && startupComplete
                && nativeSequenceIsSelectedTrackArmed();
        recArm.setEnabled(enabled);
        recArm.setAlpha(enabled ? 1.0f : 0.45f);
        recArm.setText(armed ? "REC ARM ON" : "REC ARM");
        recArm.setTextColor(armed ? BG : TEXT);
        recArm.setBackground(strokeBackground(
                armed ? ACTIVE : SURFACE_2,
                armed ? ACTIVE : LINE,
                MPC_FLAT_RADIUS_DP));
        recArm.setContentDescription(
                armed ? "MPC Main sequence REC ARM active"
                        : "MPC Main sequence REC ARM");
        group.addView(recArm, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

        functionBar.addView(group, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void addTrackStepperFunction(boolean enabled) {
        LinearLayout group = new LinearLayout(this);
        group.setOrientation(LinearLayout.HORIZONTAL);
        group.setGravity(Gravity.CENTER_VERTICAL);
        group.setContentDescription("MPC Function Bar TRACK previous next");
        group.setPadding(dp(2), dp(2), dp(2), dp(2));
        group.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));

        Button previous = actionButton("−", v -> selectAdjacentTrack(-1));
        previous.setEnabled(enabled);
        previous.setAlpha(enabled ? 1.0f : 0.45f);
        previous.setContentDescription("MPC Main previous track");
        group.addView(previous, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.42f));

        TextView title = label("TRACK", 8, TEXT);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setContentDescription("MPC Main track stepper");
        group.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.9f));

        Button next = actionButton("+", v -> selectAdjacentTrack(1));
        next.setEnabled(enabled);
        next.setAlpha(enabled ? 1.0f : 0.45f);
        next.setContentDescription("MPC Main next track");
        group.addView(next, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.42f));

        functionBar.addView(group, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void addFunction(String text, boolean enabled, View.OnClickListener listener) {
        Button b = actionButton(text, listener);
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1.0f : 0.45f);
        b.setBackground(strokeBackground(SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        b.setTextColor(TEXT);
        b.setBackground(strokeBackground(
                MPC_PANEL_DARK,
                MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
        b.setTypeface(Typeface.DEFAULT_BOLD);
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
            /*
             * Track −/+ is a high-frequency Main action, not a navigation
             * command. Preserve the user's local Track/Arrangement presentation
             * while making the newly selected Track the active Data Dial focus.
             */
            final boolean arrangementSelected =
                    mainTrackArrangementHost != null
                            && mainTrackArrangementHost.getChildCount() > 1
                            && mainTrackArrangementHost.getChildAt(1).getVisibility()
                                    == View.VISIBLE;
            showMainPage();
            setMainTrackArrangementView(arrangementSelected);
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
            navigationController.setSubcontext(
                    MpcUiState.Subcontext.TRACK_SELECT);
            navigationController.setDataDialFocus(
                    MpcUiState.DataDialFocus.TRACK);
            navigationController.setActionAvailable(true);
            refreshMainDataDialFocusVisuals();
        } else if (mode == MpcUiState.Mode.TRACK_VIEW) {
            showTrackViewPage();
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
            navigationController.setSubcontext(
                    MpcUiState.Subcontext.TRACK_SELECT);
            navigationController.setDataDialFocus(
                    MpcUiState.DataDialFocus.TRACK);
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
        bar.setPadding(
                dp(MPC_TOOLBAR_INSET_DP), dp(3),
                dp(MPC_TOOLBAR_INSET_DP), dp(3));
        bar.setBackgroundColor(MPC_TOOLBAR_BG);
        bar.setContentDescription("MPC One Main Toolbar");

        Button menu = topButton("▦");
        menu.setContentDescription("MPC Toolbar Menu");
        menu.setOnClickListener(v -> showMenuPage());
        bar.addView(menu, new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_MENU_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP)));

        projectState = label("PROJECT\nUNTITLED", 9, MPC_TOOLBAR_TEXT);
        projectState.setTypeface(Typeface.DEFAULT_BOLD);
        projectState.setGravity(Gravity.CENTER_VERTICAL);
        projectState.setPadding(dp(6), 0, dp(2), 0);
        projectState.setContentDescription("MPC Project");
        bar.addView(projectState, new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_PROJECT_IDENTITY_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP)));

        // MPC One keeps a direct project/browser affordance next to project
        // identity. It is deliberately a compact entry point rather than a
        // second navigation surface.
        Button projectBrowser = topButton("");
        projectBrowser.setContentDescription("MPC Project Browser");
        projectBrowser.setForeground(new MpcFolderIconDrawable());
        projectBrowser.setOnClickListener(v -> showBrowserPage());
        bar.addView(projectBrowser, new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_PROJECT_BROWSER_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP)));

        // showMainPage() is part of shell construction, so the current-page
        // field must exist before the first page render. Keep it in the MPC
        // toolbar instead of leaving the legacy field uninitialized.
        pageTitle = label("MAIN", 9, MPC_TOOLBAR_TEXT);
        pageTitle.setGravity(Gravity.CENTER);
        pageTitle.setTypeface(Typeface.DEFAULT_BOLD);
        pageTitle.setContentDescription("MPC current page");
        // MPC One's Toolbar is status-oriented; the active page is communicated
        // by the left shortcut/context system, not by a duplicate title chip.
        pageTitle.setVisibility(View.GONE);

        sequenceTransportView = label(
                "BAR  001    BEAT  1    TICK  000",
                9,
                MPC_TOOLBAR_TEXT);
        sequenceTransportView.setGravity(Gravity.CENTER);
        sequenceTransportView.setTypeface(Typeface.DEFAULT_BOLD);
        sequenceTransportView.setContentDescription("Sequence position and tempo");
        bar.addView(sequenceTransportView, new LinearLayout.LayoutParams(
                0,
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP),
                1));

        timingCorrectTopButton = topButton("TC OFF");
        timingCorrectTopButton.setContentDescription("Timing Correct");
        timingCorrectTopButton.setOnClickListener(v -> {
            if (!startupComplete) {
                setBottomStatus("TIMING CORRECT • waiting for sequencer");
                return;
            }
            showTimingCorrectDialog();
        });
        LinearLayout.LayoutParams timingLp = new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_TIMING_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP));
        timingLp.leftMargin = dp(MPC_TOOLBAR_GAP_DP);
        bar.addView(timingCorrectTopButton, timingLp);

        metronomeTopButton = topButton("METRO");
        metronomeTopButton.setContentDescription("Metronome reserved");
        metronomeTopButton.setEnabled(false);
        metronomeTopButton.setAlpha(0.55f);
        LinearLayout.LayoutParams metroLp = new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_METRO_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP));
        metroLp.leftMargin = dp(MPC_TOOLBAR_GAP_DP);
        bar.addView(metronomeTopButton, metroLp);

        automationTopButton = topButton("AUTO");
        automationTopButton.setContentDescription("Automation reserved");
        automationTopButton.setEnabled(false);
        automationTopButton.setAlpha(0.55f);
        LinearLayout.LayoutParams autoLp = new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_AUTO_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP));
        autoLp.leftMargin = dp(MPC_TOOLBAR_GAP_DP);
        bar.addView(automationTopButton, autoLp);

        /*
         * MPC 3.9 keeps the Toolbar status-oriented. Transport remains a
         * hardware-first operation, while the final cells show MIDI In/Out
         * status and open the MIDI monitor/context when tapped.
         */
        midiInTopStatus = topStatusCell("IN");
        midiInTopStatus.setTag("IN");
        midiInTopStatus.setContentDescription("MPC Toolbar MIDI IN");
        midiInTopStatus.setOnClickListener(v -> showMidiPage());
        LinearLayout.LayoutParams midiInLp = new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_IO_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP));
        midiInLp.leftMargin = dp(MPC_TOOLBAR_GAP_DP);
        bar.addView(midiInTopStatus, midiInLp);

        midiOutTopStatus = topStatusCell("OUT");
        midiOutTopStatus.setTag("OUT");
        midiOutTopStatus.setContentDescription("MPC Toolbar MIDI OUT");
        midiOutTopStatus.setOnClickListener(v -> showMidiPage());
        LinearLayout.LayoutParams midiOutLp = new LinearLayout.LayoutParams(
                dp(MPC_TOOLBAR_IO_WIDTH_DP),
                dp(MPC_TOOLBAR_CONTROL_HEIGHT_DP));
        midiOutLp.leftMargin = dp(MPC_TOOLBAR_GAP_DP);
        bar.addView(midiOutTopStatus, midiOutLp);

        // Keep diagnostic state objects alive for existing refresh logic, but do
        // not duplicate them in the MPC-facing toolbar.
        audioState = statusChip("AUDIO OFF", MUTED);
        midiState = statusChip("MIDI —", MUTED);

        return bar;
    }

    private void showTimingCorrectDialog() {
        final LinearLayout root = column();
        root.setContentDescription("Timing Correct dialog");
        root.setPadding(dp(14), dp(6), dp(14), 0);

        final boolean[] enabledState = {
                nativeSequenceIsTimingCorrectEnabled()
        };
        final int[] divisionState = {
                nativeSequenceGetQuantizeGrid()
        };
        final int[] swingState = {
                nativeSequenceGetSwing()
        };

        final TextView enabled = label(
                "ENABLED\n" + (enabledState[0] ? "ON" : "OFF"),
                11,
                TEXT);
        enabled.setTypeface(Typeface.DEFAULT_BOLD);
        enabled.setGravity(Gravity.CENTER_VERTICAL);
        enabled.setPadding(dp(10), 0, dp(10), 0);
        enabled.setBackground(strokeBackground(
                SURFACE_2,
                enabledState[0] ? ACTIVE : LINE,
                5));
        enabled.setContentDescription("Timing Correct enabled field");
        enabled.setOnClickListener(v -> {
            enabledState[0] = !enabledState[0];
            enabled.setText("ENABLED\n" + (enabledState[0] ? "ON" : "OFF"));
            enabled.setBackground(strokeBackground(
                    SURFACE_2,
                    enabledState[0] ? ACTIVE : LINE,
                    5));
        });
        root.addView(enabled,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        final TextView timeDiv = label(
                "TIME DIV\n" + sequenceGridLabel(divisionState[0]),
                11,
                TEXT);
        timeDiv.setTypeface(Typeface.DEFAULT_BOLD);
        timeDiv.setGravity(Gravity.CENTER_VERTICAL);
        timeDiv.setPadding(dp(10), 0, dp(10), 0);
        timeDiv.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        timeDiv.setContentDescription("Timing Correct time division");
        root.addView(timeDiv,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        LinearLayout divisionButtons = row();
        final int[] divisions = {60, 120, 240, 480, 960};
        for (int division : divisions) {
            Button b = actionButton(
                    sequenceGridLabel(division).replace("Q ", ""),
                    v -> {
                        divisionState[0] = division;
                        timeDiv.setText(
                                "TIME DIV\n" + sequenceGridLabel(divisionState[0]));
                    });
            b.setTextSize(9);
            divisionButtons.addView(b, weight());
        }
        root.addView(divisionButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        final TextView swing = label(
                "SWING\n" + swingState[0] + "%",
                11,
                TEXT);
        swing.setTypeface(Typeface.DEFAULT_BOLD);
        swing.setGravity(Gravity.CENTER_VERTICAL);
        swing.setPadding(dp(10), 0, dp(10), 0);
        swing.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        swing.setContentDescription("Timing Correct swing");
        root.addView(swing,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));

        LinearLayout swingButtons = row();
        for (int delta : new int[]{-10, -1, 1, 10}) {
            Button b = actionButton(
                    delta > 0 ? "+" + delta : "−" + Math.abs(delta),
                    v -> {
                        swingState[0] = Math.max(
                                0, Math.min(100, swingState[0] + delta));
                        swing.setText("SWING\n" + swingState[0] + "%");
                    });
            swingButtons.addView(b, weight());
        }
        root.addView(swingButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        TextView reserved = label(
                "TYPE • START\nEVENTS • ALL\nSTRENGTH • RESERVED",
                9,
                MUTED);
        reserved.setPadding(dp(10), dp(4), dp(10), dp(4));
        reserved.setGravity(Gravity.CENTER_VERTICAL);
        reserved.setContentDescription("Timing Correct reserved fields");
        root.addView(reserved,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Timing Correct")
                .setView(root)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("DO IT", (d, which) -> {
                    setBottomStatus(
                            nativeSequenceSetQuantizeGrid(divisionState[0]));
                    setBottomStatus(
                            nativeSequenceSetSwing(swingState[0]));
                    setBottomStatus(
                            nativeSequenceSetTimingCorrectEnabled(enabledState[0]));
                    refreshMpcToolbarState();
                    refreshSequenceControls();
                    setBottomStatus(
                            "TIMING CORRECT • "
                                    + (enabledState[0] ? "ON" : "OFF")
                                    + " • "
                                    + sequenceGridLabel(divisionState[0])
                                    + " • SWING " + swingState[0] + "%");
                })
                .create();
        dialog.show();
        activeMpcParameterDialog = dialog;
    }

    private void showTimeSignatureDialog() {
        final LinearLayout root = column();
        root.setContentDescription("Time Signature dialog");
        root.setPadding(dp(14), dp(6), dp(14), 0);

        final int[] numeratorState = {nativeSequenceGetNumerator()};
        final int[] denominatorState = {nativeSequenceGetDenominator()};

        final TextView current = label(
                "TIME SIGNATURE\n"
                        + numeratorState[0] + "/" + denominatorState[0],
                12,
                TEXT);
        current.setTypeface(Typeface.DEFAULT_BOLD);
        current.setGravity(Gravity.CENTER_VERTICAL);
        current.setPadding(dp(10), 0, dp(10), 0);
        current.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        current.setContentDescription("Time Signature value");
        root.addView(current,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(46)));

        TextView numerator = label(
                "BEATS / BAR\n" + numeratorState[0],
                10,
                TEXT);
        numerator.setGravity(Gravity.CENTER_VERTICAL);
        numerator.setPadding(dp(10), 0, dp(10), 0);
        numerator.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        numerator.setContentDescription("Time Signature numerator");
        root.addView(numerator,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        GridLayout numeratorButtons = new GridLayout(this);
        numeratorButtons.setColumnCount(4);
        numeratorButtons.setRowCount(4);
        for (int value = 1; value <= 16; value++) {
            final int numeratorValue = value;
            Button b = actionButton(String.valueOf(value), v -> {
                numeratorState[0] = numeratorValue;
                current.setText(
                        "TIME SIGNATURE\n"
                                + numeratorState[0] + "/" + denominatorState[0]);
                numerator.setText(
                        "BEATS / BAR\n" + numeratorState[0]);
            });
            b.setTextSize(9);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(34);
            lp.columnSpec = GridLayout.spec((value - 1) % 4, 1f);
            lp.rowSpec = GridLayout.spec((value - 1) / 4);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            numeratorButtons.addView(b, lp);
        }
        root.addView(numeratorButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(148)));

        TextView denominator = label(
                "NOTE VALUE\n" + denominatorState[0],
                10,
                TEXT);
        denominator.setGravity(Gravity.CENTER_VERTICAL);
        denominator.setPadding(dp(10), 0, dp(10), 0);
        denominator.setBackground(strokeBackground(SURFACE_2, LINE, 5));
        denominator.setContentDescription("Time Signature denominator");
        root.addView(denominator,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));

        LinearLayout denominatorButtons = row();
        for (int value : new int[]{4, 8, 16, 32}) {
            final int denominatorValue = value;
            Button b = actionButton(String.valueOf(value), v -> {
                denominatorState[0] = denominatorValue;
                current.setText(
                        "TIME SIGNATURE\n"
                                + numeratorState[0] + "/" + denominatorState[0]);
                denominator.setText(
                        "NOTE VALUE\n" + denominatorState[0]);
            });
            b.setTextSize(9);
            denominatorButtons.addView(b, weight());
        }
        root.addView(denominatorButtons,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Time Signature")
                .setView(root)
                .setNegativeButton("CANCEL", null)                .setPositiveButton("DO IT", (d, which) -> {
                    setBottomStatus(                            nativeSequenceSetTimeSignature(
                                    numeratorState[0], denominatorState[0]));                    refreshMainModeFields();
                    refreshSequenceControls();
                    setBottomStatus(
                            "TIME SIGNATURE • "
                                    + numeratorState[0] + "/"
                                    + denominatorState[0]);
                })
                .create();
        dialog.show();
        activeMpcParameterDialog = dialog;
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

    private Button mpcShortcutButton(String text, MpcUiState.Mode mode) {
        // The label argument remains as an accessibility/fallback vocabulary
        // anchor, but the visible rail uses a deterministic vector drawable
        // so Android font substitution cannot change MPC-style pictography.
        Button b = button("");
        b.setText("");
        b.setTextColor(Color.TRANSPARENT);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setGravity(Gravity.CENTER);
        b.setForeground(new MpcShortcutIconDrawable(mode));
        b.setBackground(strokeBackground(
                BG,
                Color.TRANSPARENT,
                MPC_FLAT_RADIUS_DP));
        b.setTag(mode);
        b.setContentDescription(
                "MPC shortcut " + (mode == null ? "unknown" : mode.label()));
        b.setOnClickListener(v -> navigateToMode(mode));
        return b;
    }

    private LinearLayout buildCompactMixerTabs() {
        LinearLayout tabs = row();
        for (String tab : new String[]{"LVL", "FX", "SEND", "I/O"}) {
            final boolean active = "LVL".equals(tab);
            TextView tabView = label(tab, 7, active ? BG : MUTED);
            tabView.setGravity(Gravity.CENTER);
            tabView.setTypeface(Typeface.DEFAULT_BOLD);
            tabView.setBackground(strokeBackground(
                    active ? DANGER : BG,
                    active ? DANGER : Color.TRANSPARENT,
                    MPC_FLAT_RADIUS_DP));
            tabView.setContentDescription(
                    "MPC Mixer Strip " + tab
                            + (active ? " active" : " unavailable"));
            tabs.addView(tabView,
                    new LinearLayout.LayoutParams(0, dp(22), 1));
        }
        return tabs;
    }

    private String mpcShortcutLabel(MpcUiState.Mode mode) {
        if (mode == null) return "□";
        switch (mode) {
            case MAIN: return "⌂";
            case TRACK_VIEW: return "☷";
            case BROWSER: return "⌕";
            case GRID: return "▦";
            case STEP: return "▥";
            case TRACK_EDIT: return "✎";
            case SAMPLE_EDIT: return "∿";
            case SAMPLER: return "●";
            case CHANNEL_MIXER: return "≡";
            case PAD_MIXER: return "▤";
            case LEVELS_16: return "16";
            case PAD_PERFORM: return "✣";
            case SOUNDS: return "♫";
            case XYFX: return "✣";
            case NEXT_SEQUENCE: return "▶";
            case ARRANGE: return "╬";
            case LIST_EDIT: return "☰";
            case PROJECT: return "P";
            case MENU: return "▦";
            default: return "□";
        }
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
            button.setTextColor(Color.TRANSPARENT);
            button.setSelected(selected);
            button.setBackground(strokeBackground(
                    selected ? MPC_SELECTED : BG,
                    selected ? MPC_SELECTED : Color.TRANSPARENT,
                    MPC_FLAT_RADIUS_DP));
            if (button.getForeground() instanceof MpcShortcutIconDrawable) {
                ((MpcShortcutIconDrawable) button.getForeground()).setSelected(selected);
            }
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
        /*
         * Main is a persistent workspace, not a disposable page. Preserve
         * semantic selection/focus when the shell returns to Main, but never
         * leak editor-only focus (sample zoom, timeline, etc.) into Main.
         * MpcUiState remains the sole owner of the semantic Data Dial focus.
         */
        final MpcUiState.DataDialFocus previousFocus =
                navigationController.state().dataDialFocus();
        final MpcUiState.Subcontext previousSubcontext =
                navigationController.state().subcontext();
        final boolean preserveMainContext =
                isMainWorkspaceDataDialFocus(previousFocus);
        final boolean previousMainArrangementView =
                mainTrackArrangementHost != null
                        && mainTrackArrangementHost.getChildCount() > 1
                        && mainTrackArrangementHost.getChildAt(1).getVisibility()
                                == View.VISIBLE;

        clearStepEditPadLeds();
        if (NATIVE_LIBRARY_LOADED) {
            nativeSequenceSetStepEditContext(false, 0);
            nativeSequenceSetLauncherContext(false, 0);
        }
        clearSequenceLauncherLeds();
        currentPage = "MAIN";
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(
                preserveMainContext
                        ? previousSubcontext
                        : MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(
                preserveMainContext
                        ? previousFocus
                        : MpcUiState.DataDialFocus.NONE);
        navigationController.setActionAvailable(true);
        pageTitle.setText("MAIN");
        content.removeAllViews();

        LinearLayout page = page();
        page.setPadding(dp(MPC_MAIN_CONTENT_GUTTER_DP), dp(2), dp(MPC_MAIN_CONTENT_GUTTER_DP), 0);

        /*
         * Main's Sequence header is a single information band: sequence
         * identity on the left, tempo/source in the center-right, and the
         * project time signature at the far right. Keep high-frequency loop
         * fields in the compact row below instead of creating a secondary
         * "card" just for Time Signature.
         */
        LinearLayout sequenceCard = column();
        sequenceCard.setContentDescription("Main Mode Sequence section");
        sequenceCard.setPadding(dp(4), dp(4), dp(4), dp(MPC_MAIN_SECTION_GAP_DP));
        sequenceCard.setBackground(strokeBackground(
                MPC_PANEL, MPC_PANEL_BORDER, MPC_MAIN_RADIUS_DP));

        LinearLayout sequenceHeader = row();
        sequenceHeader.setContentDescription("Main Mode sequence header");

        TextView sequenceName = mainField("SEQUENCE");
        sequenceName.setTextSize(15);
        sequenceName.setTypeface(Typeface.DEFAULT_BOLD);
        sequenceName.setContentDescription("Main Mode selected sequence");
        sequenceName.setPadding(dp(8), 0, dp(8), 0);
        sequenceName.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.SEQUENCE_SELECT,
                MpcUiState.DataDialFocus.SEQUENCE,
                "SEQUENCE • DATA DIAL / +/-"));
        sequenceHeader.addView(sequenceName,
                new LinearLayout.LayoutParams(0, dp(MPC_MAIN_FIELD_HEIGHT_DP), 1));

        TextView bpm = mainHeaderMetric("BPM");
        bpm.setContentDescription("Main Mode BPM");
        sequenceHeader.addView(bpm,
                new LinearLayout.LayoutParams(dp(82), dp(MPC_MAIN_METRIC_HEIGHT_DP)));

        TextView sequenceType = mainHeaderMetric("SEQ");
        sequenceType.setContentDescription(
                "Main Mode sequence tempo source • SEQ • Global unavailable");
        sequenceHeader.addView(sequenceType,
                new LinearLayout.LayoutParams(dp(46), dp(MPC_MAIN_METRIC_HEIGHT_DP)));

        TextView timeSig = mainHeaderMetric("TIME SIG");
        timeSig.setContentDescription("Main Time Signature field • tap for editor");
        timeSig.setBackground(strokeBackground(
                SURFACE_2,
                MPC_TIME_SIGNATURE_HIGHLIGHT,
                MPC_MAIN_RADIUS_DP));
        sequenceHeader.addView(timeSig,
                new LinearLayout.LayoutParams(dp(56), dp(MPC_MAIN_METRIC_HEIGHT_DP)));

        Button sequenceEdit = mainActionButton("✎", null);
        sequenceEdit.setEnabled(false);
        sequenceEdit.setAlpha(0.42f);
        sequenceEdit.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        sequenceEdit.setContentDescription(
                "Main Sequence Edit/Copy RESERVED until semantic backend exists");
        sequenceHeader.addView(sequenceEdit,
                new LinearLayout.LayoutParams(dp(36), dp(32)));

        sequenceCard.addView(sequenceHeader,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(MPC_MAIN_FIELD_HEIGHT_DP)));

        LinearLayout sequenceFields = row();
        TextView bars = mainMetric("BARS");
        TextView start = mainMetric("START");
        TextView end = mainMetric("END");
        TextView transpose = mainMetric("TRANSPOSE");
        Button loop = mainActionButton("↻", v -> {
            if (!startupComplete) {
                setBottomStatus("LOOP • waiting for sequencer");
                return;
            }
            setBottomStatus(nativeSequenceSetLoopEnabled(
                    !nativeSequenceIsLoopEnabled()));
            refreshMainModeFields();
        });
        loop.setTextSize(15);
        loop.setTypeface(Typeface.DEFAULT_BOLD);
        loop.setContentDescription("Main Sequence Loop button");
        loop.setGravity(Gravity.CENTER);
        sequenceFields.addView(bars, weight());
        sequenceFields.addView(start, weight());
        sequenceFields.addView(end, weight());
        sequenceFields.addView(transpose, weight());
        sequenceFields.addView(loop, new LinearLayout.LayoutParams(dp(48), dp(MPC_MAIN_FIELD_HEIGHT_DP)));
        sequenceCard.addView(sequenceFields,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(MPC_MAIN_FIELD_HEIGHT_DP)));

        LinearLayout trackProgramSection = mainSection();
        trackProgramSection.setContentDescription("Main Mode Track Program section");
        trackProgramSection.setBackground(strokeBackground(
                MPC_PANEL, MPC_PANEL_BORDER, MPC_MAIN_RADIUS_DP));
        trackProgramSection.setPadding(
                dp(4), dp(MPC_MAIN_SECTION_GAP_DP), dp(4), 0);

        /*
         * MPC3 uses one unified Track container. Keep the Main identity band
         * focused on the selected Track itself; Program is contextual state
         * owned by that Track rather than a second visible header field.
         * Program selection remains available through the semantic Program
         * context without consuming Main workspace width.
         */
        /*
         * MPC presents the selected Track and its Track / Arrangement context
         * as one compact header boundary over the shared workspace. Keeping
         * these controls on one row removes an otherwise empty separator row
         * and gives the waveform/performance canvas more vertical room.
         */
        LinearLayout trackContextHeader = row();
        trackContextHeader.setContentDescription(
                "Main Track visual hierarchy • Track / Program / workspace header");
        trackContextHeader.setPadding(dp(4), dp(1), dp(4), dp(1));

        mainTrackTypeField = buildMainTrackTypeIconStrip();
        trackContextHeader.addView(
                mainTrackTypeField,
                new LinearLayout.LayoutParams(
                        dp(MPC_MAIN_TRACK_TYPE_ICON_WIDTH_DP),
                        dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP)));

        TextView trackName = mainField("TRACK");
        trackName.setTypeface(Typeface.DEFAULT_BOLD);
        trackName.setTextSize(13);
        trackName.setContentDescription("Main Mode selected track");
        trackName.setOnClickListener(v -> focusMainTrackField());
        trackContextHeader.addView(trackName,
                new LinearLayout.LayoutParams(0, dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP), 1.0f));

        mainTrackField = trackName;

        Button trackEditHeader = mainActionButton("✎", v -> openMainTrackEditContext());
        trackEditHeader.setTextSize(15);
        trackEditHeader.setContentDescription("Main Track Edit");
        trackEditHeader.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        trackContextHeader.addView(trackEditHeader,
                new LinearLayout.LayoutParams(dp(34), dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP)));

        LinearLayout trackArrangementToggle = row();
        trackArrangementToggle.setContentDescription(
                "Main Track Arrangement segmented control");
        trackArrangementToggle.setPadding(dp(1), dp(1), dp(1), dp(1));
        mainTrackViewButton = mainSectionToggle(
                "TRACK",
                v -> setMainTrackArrangementView(false));
        mainTrackViewButton.setContentDescription("Main Track View header");
        trackArrangementToggle.addView(mainTrackViewButton,
                new LinearLayout.LayoutParams(0, dp(32), 1));

        mainArrangementViewButton = mainSectionToggle(
                "ARRANGEMENT",
                v -> setMainTrackArrangementView(true));
        mainArrangementViewButton.setContentDescription("Main Arrangement View header");
        trackArrangementToggle.addView(mainArrangementViewButton,
                new LinearLayout.LayoutParams(0, dp(32), 1));
        trackContextHeader.addView(trackArrangementToggle,
                new LinearLayout.LayoutParams(dp(150), dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP)));

        trackProgramSection.addView(trackContextHeader,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(MPC_MAIN_TRACK_HEADER_HEIGHT_DP)));

        // MPC One keeps the selected Track's Program visible directly below
        // the Track identity band. This is the same semantic Program context
        // exposed by the persistent left rail, not a second state model.
        LinearLayout programContextRow = row();
        programContextRow.setContentDescription("Main Mode Track Program context");
        programContextRow.setPadding(dp(4), dp(2), dp(4), dp(2));

        Button programCreateButton = mainActionButton("+", null);
        programCreateButton.setTextSize(14);
        programCreateButton.setTypeface(Typeface.DEFAULT_BOLD);
        programCreateButton.setEnabled(false);
        programCreateButton.setAlpha(0.42f);
        programCreateButton.setContentDescription("Main Program create button reserved");
        programCreateButton.setBackground(strokeBackground(
                MPC_PANEL_DARK, MPC_PANEL_BORDER, MPC_FLAT_RADIUS_DP));
        programCreateButton.setGravity(Gravity.CENTER);
        programContextRow.addView(
                programCreateButton,
                new LinearLayout.LayoutParams(dp(28), dp(MPC_MAIN_PROGRAM_HEIGHT_DP)));

        TextView programCaption = label("DRUM PROGRAM", 9, MUTED);
        programCaption.setTypeface(Typeface.DEFAULT_BOLD);
        programCaption.setGravity(Gravity.CENTER_VERTICAL);
        programCaption.setPadding(dp(4), 0, dp(8), 0);
        programContextRow.addView(
                programCaption,
                new LinearLayout.LayoutParams(dp(62), dp(MPC_MAIN_PROGRAM_HEIGHT_DP)));

        Button programField = mainActionButton(
                "—",
                v -> {
                    if (!startupComplete) {
                        setBottomStatus("PROGRAM SELECT • waiting for sequencer");
                        return;
                    }
                    final int selectedTrack = Math.max(
                            0, nativeSequenceGetSelectedTrack());
                    final String trackType = nativeSequenceGetTrackType(selectedTrack);
                    if ("DRUM".equalsIgnoreCase(trackType)) {
                        showProgramSelectPage();
                    } else {
                        setBottomStatus(
                                "PROGRAM SELECT • TRACK TYPE IS NOT DRUM");
                    }
                });
        programField.setContentDescription("Main Mode selected program");
        programField.setGravity(Gravity.CENTER_VERTICAL);
        programField.setTypeface(Typeface.DEFAULT_BOLD);
        programField.setTextSize(11);
        programField.setPadding(dp(8), 0, dp(8), 0);
        programField.setBackground(strokeBackground(
                MPC_PANEL_DARK,
                MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
        programContextRow.addView(
                programField,
                new LinearLayout.LayoutParams(0, dp(MPC_MAIN_PROGRAM_HEIGHT_DP), 1));

        TextView programStatus = label("TRACK-OWNED", 8, MUTED);
        programStatus.setGravity(Gravity.CENTER);
        programStatus.setTypeface(Typeface.DEFAULT_BOLD);
        programStatus.setContentDescription(
                "Main Mode program ownership status");
        programContextRow.addView(
                programStatus,
                new LinearLayout.LayoutParams(dp(78), dp(MPC_MAIN_PROGRAM_HEIGHT_DP)));

        trackProgramSection.addView(
                programContextRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        mainTrackArrangementHost = new FrameLayout(this);
        mainTrackArrangementHost.setContentDescription(
                "Main Track and Arrangement workspace");

        LinearLayout trackWorkspace = column();
        trackWorkspace.setContentDescription("Main Mode Track workspace");
        trackWorkspace.setPadding(dp(6), dp(2), dp(6), dp(4));
        trackWorkspace.setBackgroundColor(BG);

        /*
         * MPC Main exposes the compact Track-state row below the
         * Track/Arrangement canvas. Keep the vocabulary recognizable while
         * only exposing values that our backend can state truthfully:
         * Monitor is unavailable, Length is sequence-scoped, Velocity is not
         * currently a track property, and Layer is the real selected sample
         * layer for Drum tracks.
         */
        LinearLayout trackDetailRow = row();
        TextView monitorDetail = mainMetric("MONITOR");
        monitorDetail.setContentDescription("Main Track View monitor state");
        monitorDetail.setText("MONITOR\n—");
        trackDetailRow.addView(monitorDetail, weight());

        TextView lengthDetail = mainMetric("LENGTH");
        lengthDetail.setContentDescription("Main Track View length mode");
        lengthDetail.setText("LENGTH\nSEQ");
        trackDetailRow.addView(lengthDetail, weight());

        TextView velocityDetail = mainMetric("VELOCITY");
        velocityDetail.setContentDescription("Main Track View velocity state");
        velocityDetail.setText("VELOCITY\n—");
        trackDetailRow.addView(velocityDetail, weight());

        /*
         * Track-state order is fixed by the MPC Main workflow:
         * MONITOR / LENGTH / VELOCITY / LAYER.
         * TRANSPOSE belongs to the Sequence section, not this Track row.
         */
        LinearLayout layerControls = row();
        Button layerDownButton = mainActionButton("−", v -> adjustMainLayer(-1));
        layerDownButton.setTextSize(13);
        layerDownButton.setContentDescription("Main Track View previous sample layer");
        layerControls.addView(layerDownButton,
                new LinearLayout.LayoutParams(dp(24), dp(MPC_MAIN_TRACK_STATE_HEIGHT_DP)));

        TextView layerDetail = mainMetric("LAYER");
        layerDetail.setContentDescription("Main Track View selected layer • tap to focus Layer");
        layerDetail.setOnClickListener(v -> {
            navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_SELECT);
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_LAYER);
            navigationController.setActionAvailable(true);
            setBottomStatus("LAYER • DATA DIAL / +/-");
            refreshMainDataDialFocusVisuals();
        });
        layerDetail.setText(String.format(
                Locale.ROOT,
                "LAYER\n%d/8",
                selectedLayer + 1));
        layerControls.addView(layerDetail,
                new LinearLayout.LayoutParams(0, dp(MPC_MAIN_TRACK_STATE_HEIGHT_DP), 1));

        Button layerUpButton = mainActionButton("+", v -> adjustMainLayer(1));
        layerUpButton.setTextSize(13);
        layerUpButton.setContentDescription("Main Track View next sample layer");
        layerControls.addView(layerUpButton,
                new LinearLayout.LayoutParams(dp(24), dp(MPC_MAIN_TRACK_STATE_HEIGHT_DP)));
        trackDetailRow.addView(layerControls,
                new LinearLayout.LayoutParams(0, dp(MPC_MAIN_TRACK_STATE_HEIGHT_DP), 1));

        compactMixerStripModeToggle = mainActionButton("□  ▦", v -> {
            final MpcUiState state = navigationController.state();
            if (!compactMixerStripModeAvailable()) {
                setBottomStatus("TRACK/PAD CONTEXT • DRUM TRACK REQUIRED");
                return;
            }
            navigationController.setCompactMixerState(
                    state.compactMixerVisible(),
                    !state.compactMixerPadMode());
        });
        compactMixerStripModeToggle.setTextSize(11);
        compactMixerStripModeToggle.setContentDescription(
                "MPC condensed Mixer Strip Track or Pad selector");
        compactMixerStripModeToggle.setGravity(Gravity.CENTER);
        compactMixerStripModeToggle.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        trackDetailRow.addView(compactMixerStripModeToggle,
                new LinearLayout.LayoutParams(dp(46), dp(40)));

        LinearLayout quickTrack = row();

        /*
         * MPC 3.9 Main is controller-first here: the selected Pad is chosen
         * on the physical MPC surface, while the phone Main workspace is the
         * Track/Arrangement waveform surface. Do not embed an Android 4x4 pad
         * grid into the canonical Main composition.
         */
        LinearLayout sampleColumn = column();
        sampleColumn.setPadding(0, 0, 0, 0);
        sampleColumn.setContentDescription("Main Track View quick sample editor • controller-first selected Pad");

        LinearLayout sampleHeader = row();
        TextView sampleTitle = label("", 10, TEXT);
        sampleTitle.setTypeface(Typeface.DEFAULT_BOLD);
        sampleTitle.setGravity(Gravity.CENTER_VERTICAL);
        sampleTitle.setContentDescription("Main Track View sample context");
        sampleHeader.addView(sampleTitle,
                new LinearLayout.LayoutParams(0, dp(28), 1));


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
        mainTrackWaveform.setOnDoubleTapListener(
                this::openMainTrackEditContext);
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
        mainTrackSamplePrimaryButton = mainActionButton(
                "AUDITION",
                v -> selectAndTriggerPad(selectedPadIndexForUi(), 112));
        mainTrackSamplePrimaryButton.setContentDescription(
                "Main Track View sample primary action");
        sampleActions.addView(
                mainTrackSamplePrimaryButton,
                new LinearLayout.LayoutParams(0, dp(34), 1));

        mainTrackSampleActionButton = mainActionButton(
                "BROWSE",
                v -> showBrowserPage());
        mainTrackSampleActionButton.setContentDescription(
                "Main Track View sample secondary action");
        sampleActions.addView(mainTrackSampleActionButton,
                new LinearLayout.LayoutParams(0, dp(34), 1));
        sampleColumn.addView(sampleActions,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        quickTrack.addView(sampleColumn,
                new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.MATCH_PARENT, 0.64f));

        /*
         * MPC Main places the compact Track-state controls immediately above
         * the Track canvas. Layer +/- stays inside this same row so Layer is
         * one coherent high-frequency semantic control.
         */
        trackWorkspace.addView(trackDetailRow,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(MPC_MAIN_TRACK_STATE_HEIGHT_DP)));

        trackWorkspace.addView(quickTrack,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, MPC_MAIN_WORKSPACE_WEIGHT));

        LinearLayout arrangement = column();
        arrangement.setPadding(dp(6), dp(4), dp(6), dp(4));
        arrangement.setBackgroundColor(BG);
        arrangement.setContentDescription("Main Mode arrangement preview");


        TextView arrangementInfo = label(
                "SEQUENCE • selected Track • playhead-aware",
                10, MUTED);
        arrangement.addView(arrangementInfo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(20)));

        mainArrangementPreview = new SequenceTimelineView(this);
        mainArrangementPreview.setContentDescription("Main Mode arrangement overview");
        mainArrangementPreview.setOnDoubleTapListener(
                this::openMainArrangementGridContext);
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
                        ViewGroup.LayoutParams.MATCH_PARENT, 0, MPC_MAIN_WORKSPACE_WEIGHT));
        trackProgramSection.setContentDescription(
                "Main Track visual hierarchy • Track / Program / workspace section");

        // Rebuilding Main must preserve the prior Track/Arrangement presentation.
        setMainTrackArrangementView(previousMainArrangementView);

        /*
         * The Sequence band is content-sized. Its two compact 42dp rows
         * must not consume an arbitrary third of the Main workspace and leave
         * dead vertical space before the Track/Arrangement canvas.
         */
        page.addView(sequenceCard, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        page.addView(trackProgramSection, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        mainSequenceNameField = sequenceName;
        mainSequenceTypeField = sequenceType;
        mainTrackLayerField = findTextByContentDescription(
                trackWorkspace, "Main Track View selected layer");
        mainSequenceBpmField = bpm;
        mainSequenceBarsField = bars;
        mainSequenceStartField = start;
        mainSequenceEndField = end;
        mainSequenceTimeSigField = timeSig;
        mainSequenceLoopField = loop;
        mainSequenceTransposeField = transpose;

        sequenceName.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.SEQUENCE_SELECT,
                MpcUiState.DataDialFocus.SEQUENCE,
                "SEQUENCE • DATA DIAL / +/-"));
        sequenceType.setOnClickListener(v -> setBottomStatus(
                "SEQ TEMPO • Sequence tempo source is fixed to SEQ in the current backend"));
        sequenceType.setEnabled(false);
        sequenceType.setAlpha(0.7f);
        sequenceType.setContentDescription(
                "Main Mode sequence tempo source • SEQ • Global unavailable");
        bpm.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.NONE,
                MpcUiState.DataDialFocus.SEQUENCE_BPM,
                "BPM • DATA DIAL / +/-"));
        bars.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.NONE,
                MpcUiState.DataDialFocus.SEQUENCE_BARS,
                "BARS • DATA DIAL / +/-"));
        start.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.SEQUENCE_START,
                MpcUiState.DataDialFocus.SEQUENCE_START,
                "LOOP START • DATA DIAL / +/-"));
        end.setOnClickListener(v -> focusMainSequenceField(
                MpcUiState.Subcontext.SEQUENCE_END,
                MpcUiState.DataDialFocus.SEQUENCE_END,
                "LOOP END • DATA DIAL / +/-"));

        installMainNumericEntry(bpm, MainNumericField.BPM);
        installMainNumericEntry(bars, MainNumericField.BARS);
        installMainNumericEntry(start, MainNumericField.LOOP_START);
        installMainNumericEntry(end, MainNumericField.LOOP_END);
        timeSig.setContentDescription("Main Time Signature field • tap for editor");
        timeSig.setOnClickListener(v -> {
            if (!startupComplete) {
                setBottomStatus("TIME SIGNATURE • waiting for sequencer");
                return;
            }
            showTimeSignatureDialog();
        });
        transpose.setOnClickListener(v -> setBottomStatus(
                "TRANSPOSE • unavailable in current Sequence backend"));

        content.addView(page);
        refreshMainModeState(
                sequenceName, sequenceType, bpm, bars, timeSig, loop,
                start, end, transpose);
        refreshMainDataDialFocusVisuals();
        refreshMpcToolbarState();
        refreshMainModePadVisuals();
        refreshMainTrackQuickSample();
        refreshMainDataDialFocusVisuals();
        updateModeRailSelection();
    }


    private void installMainNumericEntry(
            View view,
            MainNumericField field) {
        final GestureDetector detector = new GestureDetector(
                this,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDown(MotionEvent event) {
                        return true;
                    }

                    @Override
                    public boolean onDoubleTap(MotionEvent event) {
                        showMpcNumericEntry(field);
                        return true;
                    }
                });
        view.setOnTouchListener((v, event) -> {
            detector.onTouchEvent(event);
            return false;
        });
        final String fieldLabel;
        switch (field) {
            case BPM:
                fieldLabel = "BPM";
                break;
            case BARS:
                fieldLabel = "BARS";
                break;
            case LOOP_START:
                fieldLabel = "LOOP START";
                break;
            case LOOP_END:
                fieldLabel = "LOOP END";
                break;
            default:
                fieldLabel = "NUMERIC";
                break;
        }
        view.setContentDescription(
                "MPC Main " + fieldLabel + " field • double-tap for numeric entry");
    }

    private void showMpcNumericEntry(MainNumericField field) {
        final String title;
        final String initial;
        final boolean decimal;
        final MainNumericCommitter committer;

        switch (field) {
            case BPM:
                title = "BPM";
                initial = String.format(
                        Locale.ROOT, "%.1f", nativeSequenceGetTempo());
                decimal = true;
                committer = value -> {
                    try {
                        final double bpm = Double.parseDouble(value);
                        if (!Double.isFinite(bpm) || bpm < 20.0 || bpm > 300.0) {
                            return "BPM must be between 20 and 300";
                        }
                        return nativeSequenceSetTempo(bpm);
                    } catch (NumberFormatException error) {
                        return "BPM • enter a number";
                    }
                };
                break;
            case BARS:
                title = "BARS";
                initial = String.valueOf(Math.max(1, nativeSequenceGetBars()));
                decimal = false;
                committer = value -> {
                    try {
                        final int bars = Integer.parseInt(value);
                        if (bars < 1) return "BARS must be at least 1";
                        return nativeSequenceSetBars(bars);
                    } catch (NumberFormatException error) {
                        return "BARS • enter a whole number";
                    }
                };
                break;
            case LOOP_START:
                title = "LOOP START";
                initial = String.valueOf(
                        Math.max(1, nativeSequenceGetLoopStartBar()));
                decimal = false;
                committer = value -> {
                    try {
                        final int startBar = Integer.parseInt(value);
                        final int bars = Math.max(1, nativeSequenceGetBars());
                        final int endBar = Math.max(
                                startBar,
                                nativeSequenceGetLoopEndBar());
                        if (startBar < 1 || startBar > bars) {
                            return "LOOP START must be inside the sequence";
                        }
                        if (startBar > endBar) {
                            return "LOOP START cannot exceed END";
                        }
                        return nativeSequenceSetLoopBars(startBar, endBar);
                    } catch (NumberFormatException error) {
                        return "LOOP START • enter a whole number";
                    }
                };
                break;
            case LOOP_END:
                title = "LOOP END";
                initial = String.valueOf(
                        Math.max(1, nativeSequenceGetLoopEndBar()));
                decimal = false;
                committer = value -> {
                    try {
                        final int endBar = Integer.parseInt(value);
                        final int bars = Math.max(1, nativeSequenceGetBars());
                        final int startBar = Math.max(
                                1, nativeSequenceGetLoopStartBar());
                        if (endBar < 1 || endBar > bars) {
                            return "LOOP END must be inside the sequence";
                        }
                        if (endBar < startBar) {
                            return "LOOP END cannot precede START";
                        }
                        return nativeSequenceSetLoopBars(startBar, endBar);
                    } catch (NumberFormatException error) {
                        return "LOOP END • enter a whole number";
                    }
                };
                break;
            default:
                return;
        }

        final LinearLayout dialogRoot = column();
        dialogRoot.setPadding(dp(14), dp(4), dp(14), 0);

        final EditText valueField = new EditText(this);
        valueField.setText(initial);
        valueField.setSelectAllOnFocus(true);
        valueField.setSingleLine(true);
        valueField.setTextSize(24);
        valueField.setTextColor(TEXT);
        valueField.setGravity(Gravity.CENTER);
        valueField.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        valueField.setBackground(
                strokeBackground(SURFACE_2, DANGER, 5));
        dialogRoot.addView(valueField, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        final GridLayout keypad = new GridLayout(this);
        keypad.setColumnCount(3);
        keypad.setRowCount(4);
        keypad.setPadding(0, dp(8), 0, 0);

        final String[] keys = decimal
                ? new String[]{"1", "2", "3", "4", "5", "6",
                        "7", "8", "9", "DEL", "0", "."}
                : new String[]{"1", "2", "3", "4", "5", "6",
                        "7", "8", "9", "DEL", "0", "00"};

        for (String key : keys) {
            Button button = actionButton(key, null);
            button.setTextSize(key.equals("DEL") ? 9 : 16);
            button.setBackground(strokeBackground(
                    SURFACE_2,
                    LINE,
                    5));
            button.setOnClickListener(v -> {
                if ("DEL".equals(key)) {
                    final int end = valueField.getSelectionEnd();
                    final int start = valueField.getSelectionStart();
                    if (start != end) {
                        valueField.getText().delete(
                                Math.min(start, end), Math.max(start, end));
                    } else if (end > 0) {
                        valueField.getText().delete(end - 1, end);
                    }
                    return;
                }
                if ("00".equals(key)) {
                    final int selectionStart = valueField.getSelectionStart();
                    final int selectionEnd = valueField.getSelectionEnd();
                    final int left = Math.min(selectionStart, selectionEnd);
                    final int right = Math.max(selectionStart, selectionEnd);
                    valueField.getText().replace(left, right, "00");
                    return;
                }

                final int selectionStart = valueField.getSelectionStart();
                final int selectionEnd = valueField.getSelectionEnd();
                final int left = Math.min(selectionStart, selectionEnd);
                final int right = Math.max(selectionStart, selectionEnd);
                valueField.getText().replace(
                        left, right, key);
            });

            keypad.addView(button, new GridLayout.LayoutParams(
                    new android.view.ViewGroup.LayoutParams(
                            dp(92), dp(46))));
        }

        dialogRoot.addView(keypad, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(dialogRoot)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("ENTER", null)
                .create();

        dialog.setOnShowListener(ignored -> {
            final Button enter = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            enter.setOnClickListener(v -> {
                final String result = committer.commit(
                        valueField.getText().toString().trim());
                setBottomStatus(result);
                final boolean inputError = result != null
                        && (result.toLowerCase(Locale.ROOT).contains("must")
                        || result.toLowerCase(Locale.ROOT).contains("enter")
                        || result.toLowerCase(Locale.ROOT).contains("cannot")
                        || result.toLowerCase(Locale.ROOT).contains("inside"));
                if (!inputError) {
                    refreshMainModeFields();
                    refreshMainDataDialFocusVisuals();
                    syncHardwareLcd();
                    dialog.dismiss();
                }
            });
        });
        dialog.show();
        valueField.requestFocus();
        dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
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
            mainTrackWaveform.setSelection(                    start / (float) frames,
                    end / (float) frames);            mainTrackWaveform.setDurationMs(
                    sampleRate > 0
                            ? frames * 1000.0f / sampleRate                            : 0.0f);
        }
        mainTrackWaveform.setRecording(false);

        final String sampleName = startupComplete
                ? nativeAudioGetPadSampleName(selectedPad, selectedLayer)
                : "";
        final String displaySampleName =
                sampleName == null || sampleName.trim().isEmpty()
                        ? "NO NAME"
                        : sampleName.trim();

        if (mainTrackSamplePrimaryButton != null
                && mainTrackSampleActionButton != null) {
            if (frames <= 0) {
                // MPC Main's empty-pad state exposes the two loading paths:
                // Browse an existing sample or Record a new one.
                mainTrackSamplePrimaryButton.setText("BROWSE");
                mainTrackSamplePrimaryButton.setOnClickListener(
                        v -> showBrowserPage());
                mainTrackSamplePrimaryButton.setContentDescription(
                        "Main Track View browse samples");

                mainTrackSampleActionButton.setText("RECORD");
                mainTrackSampleActionButton.setOnClickListener(
                        v -> showRecordPage());
                mainTrackSampleActionButton.setContentDescription(
                        "Main Track View record sample");
            } else {
                // Once loaded, the waveform becomes the primary surface;
                // keep Audition first and expose the semantic Sample Edit action.
                mainTrackSamplePrimaryButton.setText("AUDITION");
                mainTrackSamplePrimaryButton.setOnClickListener(
                        v -> selectAndTriggerPad(selectedPadIndexForUi(), 112));
                mainTrackSamplePrimaryButton.setContentDescription(
                        "Main Track View sample primary action");

                mainTrackSampleActionButton.setText("SAMPLE EDIT");
                mainTrackSampleActionButton.setOnClickListener(
                        v -> showSamplePage());
                mainTrackSampleActionButton.setContentDescription(
                        "Main Track View sample edit");
            }
        }

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
                                    "LAYER\n%d/8",
                                    selectedLayer + 1)
                            : "LAYER\n—");
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

        /*
         * MPC presents Track / Arrangement as a compact segmented context
         * control. The active segment is filled with the MPC selection red;
         * the inactive segment remains flat and quiet so the workspace keeps
         * the dominant visual weight.
         */
        mainTrackViewButton.setTextColor(trackVisible ? TEXT : MUTED);
        mainTrackViewButton.setBackground(strokeBackground(
                trackVisible ? MPC_SELECTED : MPC_PANEL_DARK,
                trackVisible ? MPC_SELECTED : MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
        mainArrangementViewButton.setTextColor(trackVisible ? MUTED : TEXT);
        mainArrangementViewButton.setBackground(strokeBackground(
                trackVisible ? MPC_PANEL_DARK : MPC_SELECTED,
                trackVisible ? MPC_PANEL_BORDER : MPC_SELECTED,
                MPC_FLAT_RADIUS_DP));

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

    private void adjustMainLayer(int delta) {
        final int next = Math.max(0, Math.min(7, selectedLayer + delta));
        if (next == selectedLayer) return;
        selectedLayer = next;
        navigationController.setSelectedLayer(selectedLayer);
        navigationController.setSubcontext(MpcUiState.Subcontext.SAMPLE_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_LAYER);
        navigationController.setActionAvailable(true);
        setBottomStatus("LAYER " + (selectedLayer + 1) + "/8");
        refreshMainTrackQuickSample();
        refreshMainDataDialFocusVisuals();
    }

    private int selectedPadIndexForUi() {        return Math.max(0, Math.min(15, selectedPad));
    }

    private void openMainTrackEditContext() {
        final int trackIndex = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final boolean drumTrack = startupComplete
                && "DRUM".equalsIgnoreCase(nativeSequenceGetTrackType(trackIndex));

        navigationController.navigate(MpcUiState.Mode.TRACK_EDIT);
        navigationController.setSubcontext(
                drumTrack
                        ? MpcUiState.Subcontext.SAMPLE_SELECT
                        : MpcUiState.Subcontext.NONE);
        navigationController.setDataDialFocus(
                drumTrack
                        ? MpcUiState.DataDialFocus.SAMPLE_LAYER
                        : MpcUiState.DataDialFocus.NONE);
        navigationController.setActionAvailable(drumTrack);
        currentPage = "TRACK_EDIT";
        pageTitle.setText("TRACK EDIT");
        content.removeAllViews();

        mainTrackEditView = new MpcTrackEditView(
                this,
                new MpcTrackEditView.Listener() {
                    @Override public void onBack() { navigateBackFromShell(); }
                    @Override public void onAudition() {
                        selectAndTriggerPad(selectedPadIndexForUi(), 112);
                    }
                    @Override public void onLayerDelta(int delta) {
                        adjustTrackEditLayer(delta);
                    }
                    @Override public void onLayerGainDelta(float delta) {
                        changeLayerGain(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onLayerTuningDelta(float delta) {
                        changeLayerTuning(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onLayerPan(float pan) {
                        setLayerPan(pan);
                        refreshTrackEditView();
                    }
                    @Override public void onLayerVelocityMinDelta(int delta) {
                        adjustTrackEditVelocityMin(delta);
                    }
                    @Override public void onLayerVelocityMaxDelta(int delta) {
                        adjustTrackEditVelocityMax(delta);
                    }
                    @Override public void onRegionStartDelta(long delta) {
                        nudgeRegionStart(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onRegionEndDelta(long delta) {
                        nudgeRegionEnd(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onRegionSelection(
                            float startNormalized, float endNormalized) {
                        commitMainTrackWaveformRegion(startNormalized, endNormalized);
                        refreshTrackEditView();
                    }
                    @Override public void onPadTuningDelta(float delta) {
                        changePadTuning(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onPadLevelDelta(float delta) {
                        changePadLevel(delta);
                        refreshTrackEditView();
                    }
                    @Override public void onPadPan(float pan) {
                        setPadPan(pan);
                        refreshTrackEditView();
                    }
                    @Override public void onEnvelopeDelta(
                            float attack, float decay,
                            float sustain, float release) {
                        changeEnvelope(attack, decay, sustain, release);
                        refreshTrackEditView();
                    }
                    @Override public void onEnvelopeReset() {
                        setEnvelope(0, 0, 1, 0);
                        refreshTrackEditView();
                    }
                    @Override public void onFilterDelta(float deltaHz) {
                        final float current = nativeAudioGetPadFilterCutoff(selectedPad);
                        final float next = Math.max(
                                20f, Math.min(20000f, current + deltaHz));
                        setBottomStatus(nativeAudioSetPadFilterCutoff(
                                selectedPad, next));
                        refreshTrackEditView();
                    }
                    @Override public void onFilterSet(float cutoffHz) {
                        final float next = Math.max(
                                20f, Math.min(20000f, cutoffHz));
                        setBottomStatus(nativeAudioSetPadFilterCutoff(
                                selectedPad, next));
                        refreshTrackEditView();
                    }
                });

        content.addView(mainTrackEditView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        refreshTrackEditView();
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private MpcTrackEditView.Snapshot buildTrackEditSnapshot() {
        final boolean ready = startupComplete;
        final int trackIndex = ready
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final String trackType = ready
                ? nativeSequenceGetTrackType(trackIndex) : "DRUM";
        final boolean drumTrack = "DRUM".equalsIgnoreCase(trackType);
        final String trackStatus = ready
                ? nativeSequenceTrackStatus(trackIndex) : "Track 01";
        final String programName = ready && drumTrack
                ? normalizeProgramLabel(nativeSequenceGetTrackProgram(trackIndex))
                : "—";

        if (!ready || !drumTrack) {
            return new MpcTrackEditView.Snapshot(
                    ready, drumTrack, trackIndex, trackType, trackStatus,
                    programName, selectedPadIndexForUi(), selectedLayer,
                    "NO SAMPLE", 0L, 0, 0L, 0L,
                    1.0f, 0.0f, 0.0f, 0, 127,
                    0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                    20000.0f, null);
        }

        final long frames = nativeAudioGetPadSampleFrameCount(
                selectedPad, selectedLayer);
        final int sampleRate = nativeAudioGetPadSampleRate(
                selectedPad, selectedLayer);
        final String sampleName = nativeAudioGetPadSampleName(
                selectedPad, selectedLayer);
        final long startFrame = frames > 0
                ? nativeAudioGetPadSampleRegionStart(
                        selectedPad, selectedLayer) : 0L;
        final long endFrame = frames > 0
                ? nativeAudioGetPadSampleRegionEnd(
                        selectedPad, selectedLayer) : 0L;
        final float[] peaks = frames > 0
                ? nativeAudioGetPadWaveformPeaks(
                        selectedPad, selectedLayer, 768) : null;

        return new MpcTrackEditView.Snapshot(
                true, true, trackIndex, trackType, trackStatus, programName,
                selectedPadIndexForUi(), selectedLayer, sampleName,
                frames, sampleRate, startFrame, endFrame,
                nativeAudioGetPadLayerGain(selectedPad, selectedLayer),
                nativeAudioGetPadLayerTuning(selectedPad, selectedLayer),
                nativeAudioGetPadLayerPan(selectedPad, selectedLayer),
                nativeAudioGetPadLayerVelocityMin(selectedPad, selectedLayer),
                nativeAudioGetPadLayerVelocityMax(selectedPad, selectedLayer),
                nativeAudioGetPadTuning(selectedPad),
                nativeAudioGetPadLevel(selectedPad),
                nativeAudioGetPadPan(selectedPad),
                nativeAudioGetPadEnvelopeAttack(selectedPad),
                nativeAudioGetPadEnvelopeDecay(selectedPad),
                nativeAudioGetPadEnvelopeSustain(selectedPad),
                nativeAudioGetPadEnvelopeRelease(selectedPad),
                nativeAudioGetPadFilterCutoff(selectedPad),
                peaks);
    }

    private void refreshTrackEditView() {
        if (mainTrackEditView == null || !"TRACK_EDIT".equals(currentPage)) {
            return;
        }
        mainTrackEditView.bind(buildTrackEditSnapshot());
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
    }

    private void adjustTrackEditLayer(int delta) {
        final int trackIndex = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        if (!startupComplete
                || !"DRUM".equalsIgnoreCase(
                        nativeSequenceGetTrackType(trackIndex))) {
            setBottomStatus("TRACK EDIT • selected Track is not DRUM");
            return;
        }
        final int next = Math.max(0, Math.min(7, selectedLayer + delta));
        if (next == selectedLayer) return;
        selectedLayer = next;
        navigationController.setSelectedLayer(next);
        setBottomStatus("TRACK EDIT • LAYER " + (next + 1) + "/8");
        refreshTrackEditView();
    }

    private void adjustTrackEditVelocityMin(int delta) {
        final int currentMin =
                nativeAudioGetPadLayerVelocityMin(selectedPad, selectedLayer);
        final int currentMax =
                nativeAudioGetPadLayerVelocityMax(selectedPad, selectedLayer);
        final int next = Math.max(0, Math.min(
                currentMax, currentMin + delta));
        setBottomStatus(nativeAudioSetPadLayerVelocityRange(
                selectedPad, selectedLayer, next, currentMax));
        refreshTrackEditView();
    }

    private void adjustTrackEditVelocityMax(int delta) {
        final int currentMin =
                nativeAudioGetPadLayerVelocityMin(selectedPad, selectedLayer);
        final int currentMax =
                nativeAudioGetPadLayerVelocityMax(selectedPad, selectedLayer);
        final int next = Math.max(
                currentMin, Math.min(127, currentMax + delta));
        setBottomStatus(nativeAudioSetPadLayerVelocityRange(
                selectedPad, selectedLayer, currentMin, next));
        refreshTrackEditView();
    }

    private void openMainArrangementGridContext() {
        final int trackIndex = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final String trackType = startupComplete
                ? nativeSequenceGetTrackType(trackIndex) : "DRUM";
        if (!"DRUM".equalsIgnoreCase(trackType)) {
            navigationController.setActionAvailable(false);
            setBottomStatus(
                    "GRID • unavailable for " + trackType + " Track");
            refreshMpcCompactContext();
            refreshMpcFunctionBar();
            return;
        }
        showSequenceGridPage();
    }

    private LinearLayout mainSection() {
        LinearLayout section = column();
        section.setPadding(dp(6), dp(4), dp(6), dp(4));
        section.setBackground(strokeBackground(SURFACE, LINE, MPC_MAIN_RADIUS_DP));
        return section;
    }

    private TextView mainMetric(String title) {
        TextView view = label("", 10, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(5), 0, dp(5), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, MPC_MAIN_RADIUS_DP));
        view.setTag(title);
        return view;
    }

    private Button mainActionButton(String text, View.OnClickListener listener) {
        Button b = actionButton(text, listener);
        b.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_MAIN_RADIUS_DP));
        return b;
    }

    private Button mainSectionToggle(String text, View.OnClickListener listener) {
        Button b = mainActionButton(text, listener);
        b.setTextSize(8);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setAllCaps(true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(2), 0, dp(2), 0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setContentDescription("Main Track Arrangement segmented control • " + text);
        return b;
    }

    private TextView mainHeaderMetric(String title) {
        TextView view = label("", 10, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(4), 0, dp(4), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, MPC_MAIN_RADIUS_DP));
        view.setTag(title);
        return view;
    }

    private TextView mainField(String title) {
        TextView view = label("", 12, TEXT);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setPadding(dp(7), 0, dp(7), 0);
        view.setBackground(strokeBackground(SURFACE_2, LINE, MPC_MAIN_RADIUS_DP));
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

    /*
     * MpcUiState owns semantic Data Dial focus. This integer is only a
     * protocol/rendering projection for existing MkII LED/LCD feedback.
     */
    private int hardwareFocusId() {
        if (navigationController == null) return 0;
        final MpcUiState.DataDialFocus focus =
                navigationController.state().dataDialFocus();
        switch (focus) {
            case TRACK:
                return 2;
            case SEQUENCE:
                return 3;
            case PROGRAM:
                return 4;
            case TRACK_TYPE:
                return 5;
            case SAMPLE_LAYER:
                return 10;
            case SAMPLE_START:
                return 7;
            case SAMPLE_END:
                return 8;
            case TUNE:
                return 9;
            case SEQUENCE_START:
                return HARDWARE_FOCUS_SEQUENCE_START;
            case SEQUENCE_END:
                return HARDWARE_FOCUS_SEQUENCE_END;
            case SEQUENCE_BPM:
                return HARDWARE_FOCUS_SEQUENCE_BPM;
            case SEQUENCE_BARS:
                return HARDWARE_FOCUS_SEQUENCE_BARS;
            case ZOOM_HORIZONTAL:
                if ("SAMPLE".equals(currentPage)) return 11;
                if ("SEQ".equals(currentPage) && sequenceGridView != null) return 13;
                return 11;
            case ZOOM_VERTICAL:
                if ("SAMPLE".equals(currentPage)) return 12;
                if ("SEQ".equals(currentPage) && sequenceGridView != null) return 14;
                if ("SEQ".equals(currentPage) && sequenceTimeline != null) return 16;
                return 12;
            case TIMELINE:
                return 15;
            default:
                return 0;
        }
    }

    private boolean isMainWorkspaceDataDialFocus(
            MpcUiState.DataDialFocus focus) {
        if (focus == null) return false;
        switch (focus) {
            case SEQUENCE:
            case SEQUENCE_START:
            case SEQUENCE_END:
            case SEQUENCE_BPM:
            case SEQUENCE_BARS:
            case TRACK:
            case PROGRAM:
            case TRACK_TYPE:
            case SAMPLE_LAYER:
                return true;
            default:
                return false;
        }
    }

    private void refreshMainDataDialFocusVisuals() {
        if (!"MAIN".equals(currentPage)) return;

        final int focus = hardwareFocusId();
        setMainFieldFocus(mainSequenceNameField, focus == 3);
        setMainFieldFocus(mainSequenceTypeField, false);
        setMainFieldFocus(mainSequenceBpmField, focus == HARDWARE_FOCUS_SEQUENCE_BPM);
        setMainFieldFocus(mainSequenceBarsField, focus == HARDWARE_FOCUS_SEQUENCE_BARS);
        setMainFieldFocus(mainSequenceStartField, focus == HARDWARE_FOCUS_SEQUENCE_START);
        setMainFieldFocus(mainSequenceEndField, focus == HARDWARE_FOCUS_SEQUENCE_END);
        setMainFieldFocus(mainSequenceTransposeField, false);
        setMainFieldFocus(mainSequenceTimeSigField, false);
        if (mainSequenceTimeSigField != null) {
            mainSequenceTimeSigField.setBackground(strokeBackground(
                    SURFACE_2,
                    MPC_TIME_SIGNATURE_HIGHLIGHT,
                    MPC_FLAT_RADIUS_DP));
        }
        setMainFieldFocus(mainSequenceLoopField, false);
        setMainFieldFocus(mainTrackField, focus == 2);
        setMainFieldFocus(mainTrackLayerField, focus == 10);
        if (mainTrackTypeField != null) {
            refreshMainTrackTypeVisuals();
        }
    }

    private void setMainFieldFocus(View view, boolean active) {
        if (view == null) return;
        view.setBackground(strokeBackground(
                SURFACE_2, active ? DANGER : LINE, MPC_FLAT_RADIUS_DP));
    }

    private void focusMainTrackField() {
        /*
         * Main Track is a selection entry point, not merely a passive
         * parameter highlight. Touch and MkII Track-Select resolve through
         * the same 4×4 Main context.
         */
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        showTrackSelectPage();
        setBottomStatus("TRACK SELECT • DATA DIAL / +/-");
    }

    private void focusMainSequenceField(
            MpcUiState.Subcontext subcontext,
            MpcUiState.DataDialFocus focus,
            String message) {
        navigationController.setSubcontext(subcontext);
        navigationController.setDataDialFocus(focus);
        navigationController.setActionAvailable(true);
        setBottomStatus(message);
        refreshMpcCompactContext();
        refreshMainDataDialFocusVisuals();
    }

    private void refreshMainModeFields() {
        if (mainSequenceNameField == null
                || mainSequenceTypeField == null
                || mainSequenceBpmField == null
                || mainSequenceBarsField == null
                || mainSequenceTimeSigField == null
                || mainSequenceLoopField == null
                || mainSequenceStartField == null
                || mainSequenceEndField == null
                || mainSequenceTransposeField == null) {
            return;
        }
        refreshMainModeState(                mainSequenceNameField,
                mainSequenceTypeField,
                mainSequenceBpmField,
                mainSequenceBarsField,
                mainSequenceTimeSigField,
                mainSequenceLoopField,
                mainSequenceStartField,
                mainSequenceEndField,
                mainSequenceTransposeField);
        refreshMainDataDialFocusVisuals();
    }

    private void refreshMainModeState(
            TextView sequenceName,
            TextView sequenceType,
            TextView bpm,
            TextView bars,
            TextView timeSig,
            TextView loop,
            TextView start,
            TextView end,
            TextView transpose) {
        final boolean nativeStateReady = startupComplete;
        final int sequenceIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetIndex()) : 0;
        final int trackIndex = nativeStateReady
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final int trackCount = nativeStateReady
                ? Math.max(1, nativeSequenceGetTrackCount()) : 1;

        sequenceName.setText(String.format(
                Locale.ROOT, "%d  Sequence %02d",
                sequenceIndex + 1, sequenceIndex + 1));
        sequenceName.setBackground(strokeBackground(
                SURFACE_2,
                DANGER,
                MPC_MAIN_RADIUS_DP));
        sequenceName.setContentDescription(
                "Main Mode selected sequence • MPC selected field");
        sequenceType.setText("SEQ");
        transpose.setText("TRANSPOSE\n—");
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
                Locale.ROOT, "%.1f", tempo));
        bars.setText(String.format(
                Locale.ROOT, "BARS\n%d", sequenceBars));
        timeSig.setText(String.format(
                Locale.ROOT, "%d/%d",
                numerator, denominator));
        loop.setText("↻");
        loop.setContentDescription(
                "Main Sequence Loop button • " + (loopEnabled ? "ON" : "OFF"));
        loop.setTextColor(loopEnabled ? BG : TEXT);
        loop.setBackground(strokeBackground(
                loopEnabled ? ACCENT : SURFACE_2,
                loopEnabled ? ACCENT : LINE,
                MPC_FLAT_RADIUS_DP));
        final int loopStartBar = nativeStateReady
                ? nativeSequenceGetLoopStartBar() : 1;
        final int loopEndBar = nativeStateReady
                ? nativeSequenceGetLoopEndBar() : sequenceBars;
        start.setText(String.format(Locale.ROOT, "START\nBAR %d", loopStartBar));
        end.setText(String.format(Locale.ROOT, "END\nBAR %d", loopEndBar));

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

    private LinearLayout buildMainTrackTypeIconStrip() {
        final LinearLayout strip = row();
        strip.setGravity(Gravity.CENTER);
        strip.setPadding(dp(1), dp(1), dp(1), dp(1));
        strip.setContentDescription("Main Mode selected Track Type icon");

        final Button iconButton = new Button(this);
        iconButton.setTag("TRACKTYPE_ICON");
        iconButton.setText("");
        iconButton.setGravity(Gravity.CENTER);
        iconButton.setMinWidth(0);
        iconButton.setMinimumWidth(0);
        iconButton.setMinHeight(0);
        iconButton.setMinimumHeight(0);
        iconButton.setPadding(0, 0, 0, 0);
        iconButton.setBackground(strokeBackground(
                MPC_PANEL_DARK, MPC_PANEL_BORDER, MPC_FLAT_RADIUS_DP));
        iconButton.setOnClickListener(v -> {
            navigationController.setSubcontext(
                    MpcUiState.Subcontext.TRACK_TYPE_SELECT);
            navigationController.setDataDialFocus(
                    MpcUiState.DataDialFocus.TRACK_TYPE);
            navigationController.setActionAvailable(true);
            setBottomStatus(
                    "TRACK TYPE SELECT • DATA DIAL / +/-");
            refreshMainDataDialFocusVisuals();
        });
        strip.addView(
                iconButton,
                new LinearLayout.LayoutParams(dp(34), dp(34)));
        return strip;
    }

    private Button mainInfoButton(String text, String tag) {
        Button b = actionButton(text, null);
        b.setTag(tag);
        b.setTextSize(9);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setGravity(Gravity.CENTER);
        b.setBackground(strokeBackground(
                MPC_PANEL_DARK, MPC_PANEL_BORDER, MPC_FLAT_RADIUS_DP));
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
        if (!(mainTrackTypeField instanceof LinearLayout)) return;

        String active = "DRUM";
        if (startupComplete) {
            final int index = Math.max(0, nativeSequenceGetSelectedTrack());
            final String backendType = nativeSequenceGetTrackType(index);
            if (backendType != null && !backendType.trim().isEmpty()) {
                active = backendType.trim().toUpperCase(Locale.ROOT);
            }
        }

        final LinearLayout strip = (LinearLayout) mainTrackTypeField;
        final boolean focused =
                navigationController != null
                        && navigationController.state().dataDialFocus()
                                == MpcUiState.DataDialFocus.TRACK_TYPE;

        strip.setContentDescription(
                "Main Mode selected Track Type icon • " + active
                        + (focused ? " • DATA DIAL" : ""));
        strip.setBackground(strokeBackground(
                SURFACE_2,
                focused ? MPC_SELECTION_RED : MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));

        if (strip.getChildCount() == 0
                || !(strip.getChildAt(0) instanceof Button)) {
            return;
        }

        final Button iconButton = (Button) strip.getChildAt(0);
        final boolean supported = "DRUM".equalsIgnoreCase(active);
        final MpcTrackTypeIconDrawable icon =
                trackTypeIconDrawable(active);
        icon.setSelected(true);
        icon.setEnabledState(supported);

        iconButton.setForeground(icon);
        iconButton.setEnabled(supported);
        iconButton.setAlpha(supported ? 1.0f : 0.58f);
        iconButton.setContentDescription(
                "Main Track Type " + active
                        + (supported ? " available" : " reserved")
                        + " • tap to select");
        iconButton.setBackground(strokeBackground(
                focused ? MPC_SELECTED : MPC_PANEL_DARK,
                focused ? MPC_SELECTION_RED : MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
    }

    private MpcTrackTypeIconDrawable trackTypeIconDrawable(String active) {
        final String value =
                active == null ? "DRUM" : active.trim().toUpperCase(Locale.ROOT);
        final MpcTrackTypeIconDrawable.Type type;
        switch (value) {
            case "KEYGROUP":
                type = MpcTrackTypeIconDrawable.Type.KEYGROUP;
                break;
            case "PLUGIN":
                type = MpcTrackTypeIconDrawable.Type.PLUGIN;
                break;
            case "MIDI":
                type = MpcTrackTypeIconDrawable.Type.MIDI;
                break;
            case "CLIP":
                type = MpcTrackTypeIconDrawable.Type.CLIP;
                break;
            case "CV":
                type = MpcTrackTypeIconDrawable.Type.CV;
                break;
            default:
                type = MpcTrackTypeIconDrawable.Type.DRUM;
                break;
        }
        return new MpcTrackTypeIconDrawable(type);
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

    private String cleanTrackDisplayName(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "—";
        }
        String displayName = status.trim();
        final int typeSeparator = displayName.indexOf("  ");
        if (typeSeparator >= 0) {
            displayName = displayName.substring(typeSeparator + 2);
        }
        final int metadataSeparator = displayName.indexOf("  |");
        if (metadataSeparator >= 0) {
            displayName = displayName.substring(0, metadataSeparator);
        }
        displayName = displayName.trim();
        return displayName.isEmpty() ? "—" : displayName;
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
        /*
         * MPC Main Track identity is intentionally compact: track number +
         * user-facing name. Native trackStatus also carries event/arm
         * diagnostics; those belong to contextual/status surfaces, not the
         * primary Track field.
         */
        trackName.setText(String.format(
                Locale.ROOT, "%d  %s",
                trackIndex + 1,
                cleanTrackDisplayName(status)));
        if (trackType != null) trackType.setText(
                "TYPE\n" + (backendType == null || backendType.isEmpty()
                        ? "—" : backendType));
        final String programStatus = startupComplete
                ? nativeSequenceGetTrackProgram(trackIndex)
                : "PROGRAM • NONE";
        final String normalizedProgram = normalizeProgramLabel(programStatus);
        if (program != null) {
            program.setText(normalizedProgram);
        }

        final TextView centralProgram = findTextByContentDescription(
                root, "Main Mode selected program");
        if (centralProgram != null) {
            centralProgram.setText(normalizedProgram);
        }

        final TextView programOwnership = findTextByContentDescription(
                root, "Main Mode program ownership status");
        if (programOwnership != null) {
            final boolean drumTrack = "DRUM".equalsIgnoreCase(backendType);
            programOwnership.setText(
                    drumTrack ? "TRACK-OWNED" : "UNAVAILABLE");
            programOwnership.setTextColor(
                    drumTrack ? MUTED : DANGER);
        }
        if (record != null) record.setText(
                "REC\n" + (startupComplete && nativeSequenceIsSelectedTrackArmed()
                        ? "ARM" : "OFF"));
        if (mute != null) mute.setText(
                "MUTE\n" + (status.toLowerCase(Locale.ROOT).contains("mute")
                        ? "ON" : "OFF"));
    }

    private TextView findTextByContentDescription(View root, String description) {
        if (root == null || description == null) return null;

        // Android Views commonly have a null contentDescription. Calling
        // String.contentEquals(null) dereferences the CharSequence internally
        // and crashes with "CharSequence.length() on a null object reference".
        final CharSequence actualDescription = root.getContentDescription();
        if (actualDescription != null
                && description.contentEquals(actualDescription)
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
                b.setPadding(0, 0, 0, 0);
                b.setGravity(Gravity.CENTER);
                b.setBackground(strokeBackground(
                        MPC_PANEL_DARK,
                        MPC_PANEL_BORDER,
                        MPC_FLAT_RADIUS_DP));
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
                lp.rowSpec = GridLayout.spec(displayRow, 1f);                grid.addView(b, lp);
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
                    selected ? MPC_SELECTED : MPC_PANEL_DARK,
                    selected ? MPC_SELECTED : MPC_PANEL_BORDER,
                    MPC_FLAT_RADIUS_DP));
            if (v instanceof TextView) {
                ((TextView) v).setTextColor(
                        selected ? MPC_TOOLBAR_TEXT : TEXT);
            }
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
                        (char) ('A' + Math.max(                                0,
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
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
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
                    showMainPage();
                    navigationController.setSubcontext(
                            MpcUiState.Subcontext.SEQUENCE_SELECT);
                    navigationController.setDataDialFocus(
                            MpcUiState.DataDialFocus.SEQUENCE);
                    navigationController.setActionAvailable(true);
                    refreshMainDataDialFocusVisuals();
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
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.navigate(MpcUiState.Mode.MAIN);
        navigationController.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.setActionAvailable(true);
        pageTitle.setText("MAIN • TRACK");
        content.removeAllViews();

        LinearLayout page = page();
        page.setContentDescription("MPC Main Track Select context");
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        LinearLayout heading = row();
        TextView title = label(
                "TRACK SELECT  •  4×4",
                13, TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        heading.addView(title, new LinearLayout.LayoutParams(
                0, dp(34), 1));

        final int count = startupComplete
                ? Math.max(0, nativeSequenceGetTrackCount()) : 0;
        final int selected = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        TextView countLabel = label(
                String.format(Locale.ROOT, "%02d TRACKS", count),
                9, MUTED);
        countLabel.setGravity(Gravity.CENTER);
        countLabel.setTypeface(Typeface.DEFAULT_BOLD);
        countLabel.setContentDescription("Main Track Select track count");
        heading.addView(countLabel,
                new LinearLayout.LayoutParams(dp(76), dp(34)));
        page.addView(heading);

        ScrollView scroll = new ScrollView(this);
        scroll.setContentDescription("Main Track Select 4x4 grid");
        LinearLayout grid = column();
        grid.setPadding(0, dp(2), 0, dp(2));

        if (count == 0) {
            grid.addView(label("NO TRACKS", 12, MUTED));
        } else {
            for (int rowStart = 0; rowStart < count; rowStart += 4) {
                LinearLayout gridRow = row();
                gridRow.setContentDescription(
                        "Main Track Select row " + (rowStart / 4 + 1));

                for (int column = 0; column < 4; column++) {
                    final int trackIndex = rowStart + column;
                    if (trackIndex >= count) {
                        gridRow.addView(
                                new android.widget.Space(this),
                                new LinearLayout.LayoutParams(
                                        0, dp(68), 1));
                        continue;
                    }

                    final boolean isSelected = trackIndex == selected;
                    final String status = nativeSequenceTrackStatus(trackIndex);
                    final String displayName = cleanTrackDisplayName(status);

                    Button trackButton = actionButton(
                            String.format(
                                    Locale.ROOT,
                                    "%02d\n%s",
                                    trackIndex + 1,
                                    displayName),
                            v -> {
                                setBottomStatus(
                                        nativeSequenceSelectTrack(trackIndex));
                                navigationController.setSelectedTrack(trackIndex);
                                showMainPage();
                                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
                                navigationController.setSubcontext(
                                        MpcUiState.Subcontext.TRACK_SELECT);
                                navigationController.setDataDialFocus(
                                        MpcUiState.DataDialFocus.TRACK);
                                navigationController.setActionAvailable(true);
                                refreshMainDataDialFocusVisuals();
                            });
                    trackButton.setTextSize(9);
                    trackButton.setTypeface(Typeface.DEFAULT_BOLD);
                    trackButton.setGravity(Gravity.CENTER);
                    trackButton.setContentDescription(
                            "Main Track Select track " + (trackIndex + 1));
                    trackButton.setBackground(strokeBackground(
                            isSelected ? SURFACE_2 : BG,
                            isSelected ? DANGER : LINE,
                            MPC_FLAT_RADIUS_DP));
                    gridRow.addView(trackButton,
                            new LinearLayout.LayoutParams(
                                    0, dp(68), 1));
                }

                grid.addView(gridRow, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));
            }
        }

        scroll.addView(grid);
        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout footer = row();
        footer.addView(actionButton(
                "BACK MAIN",
                v -> showMainPage()), weight());
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
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PROGRAM);
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
                            showMainPage();
                            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PROGRAM);
                            navigationController.setSubcontext(
                                    MpcUiState.Subcontext.PROGRAM_SELECT);
                            navigationController.setDataDialFocus(
                                    MpcUiState.DataDialFocus.PROGRAM);
                            navigationController.setActionAvailable(true);
                            refreshMainDataDialFocusVisuals();
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
        page.addView(browserView, new LinearLayout.LayoutParams(                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

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
            return withoutEvents.substring(kindSeparator + 2).trim();        }
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
        currentPage = "TRACK_VIEW";        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.navigate(MpcUiState.Mode.TRACK_VIEW);
        navigationController.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigationController.setActionAvailable(true);
        pageTitle.setText("TRACK VIEW");        content.removeAllViews();

        LinearLayout page = page();
        page.setContentDescription("MPC Track View workspace");
        page.setPadding(dp(6), dp(4), dp(6), dp(2));

        /*
         * MPC Track View uses the focused Track field as the workspace
         * header and a vertical stack of horizontal track strips below it.
         * The global Toolbar already carries the Time Counter, so this
         * workspace header stays dedicated to Track identity/focus.
         */
        LinearLayout header = row();
        header.setContentDescription("MPC Track View focused track header");
        final int headerTrack = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack())
                : 0;
        final int headerTrackCount = startupComplete
                ? Math.max(1, nativeSequenceGetTrackCount())
                : 1;

        String headerIdentity = startupComplete
                ? cleanTrackDisplayName(nativeSequenceTrackStatus(headerTrack))
                : "Track 01";

        TextView selectedTrackContext = label("", 13, TEXT);
        selectedTrackContext.setTypeface(Typeface.DEFAULT_BOLD);
        selectedTrackContext.setGravity(Gravity.CENTER_VERTICAL);
        selectedTrackContext.setPadding(dp(8), 0, dp(6), 0);
        selectedTrackContext.setContentDescription(
                "MPC Track View focused Track field");
        selectedTrackContext.setText(String.format(
                Locale.ROOT,
                "TRACK %02d • %s",
                headerTrack + 1,
                headerIdentity));
        selectedTrackContext.setBackground(strokeBackground(
                SURFACE_2, DANGER, MPC_FLAT_RADIUS_DP));
        selectedTrackContext.setOnClickListener(v -> showTrackSelectPage());
        selectedTrackContext.setFocusable(true);
        selectedTrackContext.setClickable(true);
        header.addView(selectedTrackContext,
                new LinearLayout.LayoutParams(0, dp(36), 1));

        TextView trackCountContext = label(
                String.format(Locale.ROOT, "%02d TRACKS", headerTrackCount),
                9, MUTED);
        trackCountContext.setTypeface(Typeface.DEFAULT_BOLD);
        trackCountContext.setGravity(Gravity.CENTER);
        trackCountContext.setContentDescription(
                "MPC Track View track count");
        header.addView(trackCountContext,
                new LinearLayout.LayoutParams(dp(72), dp(36)));

        Button trackFieldMenu = actionButton("▾", v -> showTrackSelectPage());
        trackFieldMenu.setTextSize(13);
        trackFieldMenu.setContentDescription(
                "MPC Track View open Track selection");
        trackFieldMenu.setBackground(strokeBackground(
                SURFACE_2, LINE, MPC_FLAT_RADIUS_DP));
        header.addView(trackFieldMenu,
                new LinearLayout.LayoutParams(dp(34), dp(34)));
        page.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setContentDescription("MPC Track View track list");
        LinearLayout strips = column();
        strips.setPadding(0, dp(3), 0, dp(3));
        scroll.addView(strips);

        final int count = startupComplete
                ? nativeSequenceGetTrackCount() : 0;
        final int selected = startupComplete
                ? nativeSequenceGetSelectedTrack() : 0;

        if (count == 0) {
            strips.addView(label(
                    "NO TRACKS • NEW TRACK is available in the shell Function Bar",
                    11, MUTED));
        } else {
            for (int i = 0; i < count; i++) {
                final int trackIndex = i;
                final boolean isSelected = i == selected;
                final boolean muted = nativeSequenceIsTrackMuted(trackIndex);
                final boolean armed = isSelected
                        && nativeSequenceIsSelectedTrackArmed();

                LinearLayout strip = column();
                strip.setPadding(dp(5), dp(3), dp(5), dp(3));
                strip.setContentDescription(
                        "Track View track " + (i + 1));
                strip.setBackground(strokeBackground(
                        isSelected
                                ? Color.rgb(42, 36, 39) : SURFACE_2,
                        isSelected ? DANGER : LINE,
                        MPC_FLAT_RADIUS_DP));
                strip.setOnClickListener(v -> {
                    setBottomStatus(nativeSequenceSelectTrack(trackIndex));
                    navigationController.setSelectedTrack(trackIndex);
                    showTrackViewPage();
                });

                LinearLayout identity = row();
                identity.setMinimumHeight(dp(26));

                String trackIdentityStatus =
                        nativeSequenceTrackStatus(trackIndex);
                String trackDisplayName =
                        cleanTrackDisplayName(trackIdentityStatus);

                TextView name = label(
                        String.format(
                                Locale.ROOT,
                                "%02d  %s",
                                i + 1,
                                trackDisplayName),
                        11, TEXT);
                name.setTypeface(Typeface.DEFAULT_BOLD);
                name.setGravity(Gravity.CENTER_VERTICAL);
                identity.addView(name, new LinearLayout.LayoutParams(
                        0, dp(28), 1.65f));

                TextView type = label(
                        nativeSequenceGetTrackType(trackIndex),
                        8, MUTED);
                type.setGravity(Gravity.CENTER_VERTICAL);
                type.setTypeface(Typeface.DEFAULT_BOLD);
                type.setContentDescription(
                        "Track View track " + (i + 1) + " type");
                identity.addView(type, new LinearLayout.LayoutParams(
                        0, dp(28), 0.72f));

                String programName = nativeSequenceGetTrackProgram(trackIndex);
                if (programName == null || programName.trim().isEmpty()) {
                    programName = "—";
                } else {
                    programName = programName.replace("PROGRAM • ", "");
                }
                TextView program = label(programName, 8, MUTED);
                program.setGravity(Gravity.CENTER_VERTICAL);
                program.setContentDescription(
                        "Track View track " + (i + 1) + " program");
                identity.addView(program, new LinearLayout.LayoutParams(
                        0, dp(28), 1.0f));

                TextView events = label(
                        trackEventSummary(nativeSequenceTrackStatus(trackIndex)),
                        8, MUTED);
                events.setGravity(Gravity.CENTER_VERTICAL);
                events.setContentDescription(
                        "Track View track " + (i + 1) + " event summary");
                identity.addView(events, new LinearLayout.LayoutParams(
                        0, dp(28), 0.72f));
                strip.addView(identity);

                LinearLayout controls = row();
                controls.setContentDescription(
                        "Track View track " + (i + 1) + " controls");

                TextView io = trackViewReservedControl(
                        "I/O —",
                        "Track View track I/O unavailable");
                controls.addView(io, new LinearLayout.LayoutParams(
                        0, dp(30), 1.05f));

                TextView range = trackViewReservedControl(
                        "KEY RANGE —",
                        "Track View track key range unavailable");
                controls.addView(range, new LinearLayout.LayoutParams(
                        0, dp(30), 1.05f));

                TextView monitor = trackViewReservedControl(
                        "MONITOR —",
                        "Track View track monitor unavailable");
                controls.addView(monitor, new LinearLayout.LayoutParams(
                        0, dp(30), 0.82f));

                TextView level = trackViewReservedControl(
                        "LEVEL —",
                        "Track View track level unavailable");
                controls.addView(level, new LinearLayout.LayoutParams(
                        0, dp(30), 0.82f));

                TextView pan = trackViewReservedControl(
                        "PAN —",
                        "Track View track pan unavailable");
                controls.addView(pan, new LinearLayout.LayoutParams(
                        0, dp(30), 0.72f));

                Button rec = actionButton(
                        armed ? "REC" : "R",
                        v -> {
                            String selectionResult =
                                    nativeSequenceSelectTrack(trackIndex);
                            navigationController.setSelectedTrack(trackIndex);
                            final boolean arm =
                                    !nativeSequenceIsSelectedTrackArmed();
                            setBottomStatus(
                                    selectionResult + " | "
                                            + nativeSequenceSetSelectedTrackArmed(arm));
                            showTrackViewPage();
                        });
                rec.setTextSize(8);
                rec.setTypeface(Typeface.DEFAULT_BOLD);
                rec.setContentDescription(
                        "Track View track " + (i + 1) + " record arm");
                rec.setBackground(strokeBackground(
                        armed ? Color.rgb(104, 64, 64) : Color.TRANSPARENT,
                        armed ? DANGER : LINE,
                        MPC_FLAT_RADIUS_DP));
                controls.addView(rec, new LinearLayout.LayoutParams(
                        0, dp(30), 0.7f));

                Button muteButton = actionButton(
                        muted ? "MUTE" : "M",
                        v -> {
                            setBottomStatus(
                                    nativeSequenceToggleTrackMute(trackIndex));
                            showTrackViewPage();
                        });
                muteButton.setTextSize(8);
                muteButton.setTypeface(Typeface.DEFAULT_BOLD);
                muteButton.setContentDescription(
                        "Track View track " + (i + 1) + " mute");
                muteButton.setTextColor(muted ? ACTIVE : MUTED);
                muteButton.setBackground(strokeBackground(
                        muted ? Color.rgb(74, 124, 88) : Color.TRANSPARENT,
                        muted ? ACTIVE : LINE,
                        MPC_FLAT_RADIUS_DP));
                controls.addView(muteButton, new LinearLayout.LayoutParams(
                        0, dp(30), 0.78f));

                TextView solo = trackViewReservedControl(
                        "SOLO",
                        "Track View track solo unavailable");
                controls.addView(solo, new LinearLayout.LayoutParams(
                        0, dp(30), 0.82f));

                TextView midiFilter = trackViewReservedControl(
                        "FILTER",
                        "Track View track MIDI filter unavailable");
                controls.addView(midiFilter, new LinearLayout.LayoutParams(
                        0, dp(30), 1.0f));

                strip.addView(controls);
                strips.addView(strip, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));
                if (i < count - 1) {
                    View divider = new View(this);
                    divider.setBackgroundColor(LINE);
                    strips.addView(divider, new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
                }
            }
        }

        page.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        content.addView(page);
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private TextView trackViewReservedControl(
            String text,
            String contentDescription) {
        TextView control = label(text, 8, MUTED);
        control.setGravity(Gravity.CENTER);
        control.setTypeface(Typeface.DEFAULT_BOLD);
        control.setContentDescription(contentDescription);
        control.setAlpha(0.58f);
        control.setBackground(strokeBackground(
                Color.TRANSPARENT,
                LINE,
                MPC_FLAT_RADIUS_DP));
        return control;
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

        TextView toolInfo = label(                "STEP PARAMETER • Data Dial / +/− edit the focused event field",
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
                refreshMpcCompactContext();            });            button.setOnLongClickListener(v -> {
                selectedSequenceStep =
                        sequenceStepPage * SEQUENCE_GRID_PAGE_STEPS + step;
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.STEP);                setBottomStatus(
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

        android.view.ViewParent parent = sequenceGridView.getParent();
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
                localPlayhead);
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
                    localStep);
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
        refreshMainModeFields();
    }

    private void changeSequenceBars(int delta) {
        final int bars = Math.max(
                1, Math.min(128, nativeSequenceGetBars() + delta));
        setBottomStatus(nativeSequenceSetBars(bars));
        refreshSequenceControls();
        refreshMainModeFields();
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
            case 60: return "Q 1/64";            case 120: return "Q 1/32";
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

        if (sequenceOverviewView != null) {
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
        }
        if (compactSequenceOverviewView != null) {
            compactSequenceOverviewView.setState(
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
        }

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
        navigationController.setSubcontext(MpcUiState.Subcontext.PERFORMANCE);
        navigationController.setDataDialFocus(
                MpcUiState.DataDialFocus.PAD_MIXER_LEVEL);
        navigationController.setActionAvailable(true);
        pageTitle.setText("PAD MIXER");
        content.removeAllViews();

        padMixerView = new MpcPadMixerView(
                this,
                new MpcPadMixerView.Listener() {
                    @Override public int selectedPad() {
                        return selectedPadIndexForUi();
                    }

                    @Override public float padLevel(int pad) {
                        return nativeAudioGetPadLevel(pad);
                    }

                    @Override public float padPan(int pad) {
                        return nativeAudioGetPadPan(pad);
                    }

                    @Override public float padTuning(int pad) {
                        return nativeAudioGetPadTuning(pad);
                    }

                    @Override public String padSampleName(int pad) {
                        final String value = nativeAudioGetPadSampleName(
                                pad, selectedLayer);
                        return value == null ? "" : value;
                    }

                    @Override public void onPadSelected(int pad) {
                        selectedPad = Math.max(0, Math.min(15, pad));
                        navigationController.setSelectedPad(selectedPad);
                        navigationController.setSubcontext(
                                MpcUiState.Subcontext.PERFORMANCE);
                        navigationController.setDataDialFocus(
                                MpcUiState.DataDialFocus.PAD_MIXER_LEVEL);
                        navigationController.setActionAvailable(true);
                        padMixerView.setControlFocus(
                                MpcPadMixerView.ControlFocus.LEVEL);
                        setBottomStatus(String.format(
                                Locale.ROOT,
                                "PAD MIXER • PAD %02d • LEVEL • DATA DIAL",
                                selectedPad + 1));
                        refreshMpcCompactContext();
                        syncHardwareControllerFeedback();
                    }

                    @Override public void onControlFocus(
                            int pad,
                            MpcPadMixerView.ControlFocus focus) {
                        selectedPad = Math.max(0, Math.min(15, pad));
                        navigationController.setSelectedPad(selectedPad);
                        navigationController.setSubcontext(
                                MpcUiState.Subcontext.PERFORMANCE);
                        navigationController.setActionAvailable(true);
                        final MpcUiState.DataDialFocus dialFocus;
                        switch (focus) {
                            case PAN:
                                dialFocus = MpcUiState.DataDialFocus.PAD_MIXER_PAN;
                                break;
                            case TUNE:
                                dialFocus = MpcUiState.DataDialFocus.PAD_MIXER_TUNE;
                                break;
                            case LEVEL:
                            default:
                                dialFocus = MpcUiState.DataDialFocus.PAD_MIXER_LEVEL;
                                break;
                        }
                        navigationController.setDataDialFocus(dialFocus);
                        setBottomStatus(String.format(
                                Locale.ROOT,
                                "PAD MIXER • PAD %02d • %s • DATA DIAL",
                                selectedPad + 1,
                                focus == MpcPadMixerView.ControlFocus.PAN
                                        ? "PAN"
                                        : focus == MpcPadMixerView.ControlFocus.TUNE
                                                ? "TUNE"
                                                : "LEVEL"));
                        refreshMpcCompactContext();
                        syncHardwareControllerFeedback();
                    }

                    @Override public void onPadLevelSet(int pad, float value) {
                        final float next = Math.max(0.0f, Math.min(1.0f, value));
                        setBottomStatus(nativeAudioSetPadLevel(pad, next));
                        refreshMpcCompactContext();
                    }

                    @Override public void onPadPanDelta(int pad, float delta) {
                        final float next = Math.max(
                                -1.0f,
                                Math.min(1.0f,
                                        nativeAudioGetPadPan(pad) + delta));
                        setBottomStatus(nativeAudioSetPadPan(pad, next));
                        refreshMpcCompactContext();
                    }

                    @Override public void onPadTuningDelta(int pad, float delta) {
                        final float next =
                                nativeAudioGetPadTuning(pad) + delta;
                        setBottomStatus(nativeAudioSetPadTuning(pad, next));
                        refreshMpcCompactContext();
                    }
                });

        content.addView(padMixerView,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        refreshPadMixerView();
        refreshMpcCompactContext();
        refreshMpcFunctionBar();
        updateModeRailSelection();
    }

    private void refreshPadMixerView() {
        if (padMixerView == null) {
            return;
        }
        final int trackIndex = startupComplete
                ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
        final String trackLabel = String.format(
                Locale.ROOT, "%02d", trackIndex + 1);
        final String programLabel = startupComplete
                ? normalizeProgramLabel(nativeSequenceGetTrackProgram(trackIndex))
                : "PROGRAM —";
        padMixerView.refresh(
                trackLabel,
                programLabel,
                selectedPadIndexForUi());
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
            final Button b = buildMpcMenuTile(entry);

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

        // Menu owns the 4x4 launcher only. System actions are rendered
        // once by the shell Function Bar so there is no nested command footer.
        content.addView(page);
        updateModeRailSelection();
    }
    private Button buildMpcMenuTile(MpcModeRegistry.Entry entry) {
        final String title = entry.available
                ? entry.label
                : entry.label + "\nRESERVED";
        final String glyph = mpcShortcutLabel(entry.mode);

        final Button b = actionButton(
                glyph + "\n" + title,
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

        b.setGravity(Gravity.CENTER);
        b.setTextSize(10);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(4), dp(6), dp(4), dp(4));
        b.setTextColor(entry.available ? TEXT : MUTED);
        b.setAlpha(entry.available ? 1.0f : 0.48f);
        b.setBackground(strokeBackground(
                MPC_PANEL_DARK,
                MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
        b.setContentDescription(
                entry.available
                        ? "MPC Menu " + entry.label
                        : "MPC Menu " + entry.label + " reserved");
        return b;
    }

    private Button styleMpcMenuFooterButton(Button button) {
        button.setTextSize(9);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setTextColor(TEXT);
        button.setBackground(strokeBackground(
                MPC_PANEL,
                MPC_PANEL_BORDER,
                MPC_FLAT_RADIUS_DP));
        return button;
    }

    private void showShortcutConfigPage() {
        clearStepEditPadLeds();
        nativeSequenceSetStepEditContext(false, 0);
        nativeSequenceSetLauncherContext(false, 0);
        navigationController.setSubcontext(MpcUiState.Subcontext.SHORTCUT_CONFIG);
        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SHORTCUT);
        pageTitle.setText("SHORTCUTS");
        content.removeAllViews();

        final MpcModeRegistry.Entry[] shortcutModes =
                MpcModeRegistry.shortcutEntries();

        LinearLayout page = page();
        page.setPadding(dp(8), dp(6), dp(8), dp(2));

        TextView header = label(
                "SHORTCUTS • FIVE HIGH-FREQUENCY MODES",
                13, TEXT);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        TextView hint = label(
                "Each slot mirrors the canonical MPC shortcut set. RESERVED destinations stay visible.",
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
            for (int j = 0; j < shortcutModes.length; j++) {
                MpcModeRegistry.Entry entry = shortcutModes[j];
                labels.add(entry.available
                        ? entry.label
                        : entry.label + " • RESERVED");
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
                                    && positionIndex < shortcutModes.length) {
                                final MpcModeRegistry.Entry entry =
                                        shortcutModes[positionIndex];
                                navigationController.setShortcut(
                                        slot,
                                        entry.mode);
                                setBottomStatus(String.format(
                                        Locale.ROOT,
                                        "SHORTCUT %d • %s",
                                        slot + 1,
                                        entry.label));
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
        refreshTrackEditView();
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
                    seconds,                    Math.round(peak * 100.0f),
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
        final long end = nativeAudioGetPadSampleRegionEnd(                selectedPad, selectedLayer);
        sampleWaveform.setSelection(
                start / (float) frames,                end / (float) frames);
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

    private void showStartupFailure(String prefix, Throwable error) {
        final String detail = prefix + ": "
                + error.getClass().getSimpleName()
                + (error.getMessage() == null ? "" : " • " + error.getMessage());
        Log.e(TAG, detail, error);
        if (bottomStatus != null) {            bottomStatus.setVisibility(View.VISIBLE);
            bottomStatus.setText(detail);
        }
        try {
            new AlertDialog.Builder(this)
                    .setTitle("MPC Groovebox startup error")
                    .setMessage(detail)
                    .setPositiveButton("OK", null)
                    .show();
        } catch (Throwable dialogError) {
            Log.e(TAG, "STARTUP_ERROR_DIALOG_FAILED", dialogError);
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
                hardwareFocusId(),
                hardwareLocateActive,
                hardwareEraseActive,
                hardwareCopyDeleteActive,
                hardwareCopyDeleteMode,
                hardwareNoteRepeatActive,
                hardwareNoteRepeatRateIndex,
                hardwareTouchStripMode);

        if (hardwareFeedbackView != null) {
            final String axis = MpcHardwareFeedbackPolicy.focusAxis(hardwareFocusId());
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
                hardwareFocusId() == 11 || hardwareFocusId() == 13 || hardwareFocusId() == 15;
        final boolean zoomVertical =
                hardwareFocusId() == 12 || hardwareFocusId() == 14;
        syncPersistentHardwareModeLeds();
        setHardwareButtonLedState(
                66,
                MpcHardwareFeedbackPolicy.dualColor(
                        zoomHorizontal || zoomVertical,
                        zoomVertical));

        setHardwareButtonLedState(
                13,
                MpcHardwareFeedbackPolicy.dualColor(
                        hardwareFocusId() == 2 || hardwareFocusId() == 3,
                        hardwareFocusId() == 3));

        setHardwareButtonLedState(
                14,
                MpcHardwareFeedbackPolicy.dualColor(
                        hardwareFocusId() == 4 || hardwareFocusId() == 5,
                        hardwareFocusId() == 5));

        setHardwareButtonLedState(
                42,
                hardwareFocusId() == 10
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                33,
                hardwareFocusId() == 7
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                34,
                hardwareFocusId() == 8
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                79,
                hardwareFocusId() == 9
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

    private void syncPersistentHardwareModeLeds() {
        setHardwareButtonLedState(
                9,
                hardwareEraseActive
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(9)
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                11,
                hardwareNoteRepeatActive
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(11)
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                70,
                hardwareLocateActive
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(70)
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        setHardwareButtonLedState(
                122,
                hardwareCopyDeleteActive
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(122)
                        : MpcHardwareFeedbackPolicy.LED_OFF);
        syncHardwareLevelModeLeds();
        syncHardwareMuteModeLed();
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

    private TextView topStatusCell(String text) {
        TextView v = label(text, 8, MPC_TOOLBAR_TEXT);
        v.setGravity(Gravity.CENTER);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        v.setPadding(dp(2), 0, dp(2), 0);
        v.setBackground(strokeBackground(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                MPC_FLAT_RADIUS_DP));
        return v;
    }

    private void updateTopMidiStatus(TextView view, boolean connected) {
        if (view == null) return;
        view.setText("IN".equals(view.getTag()) ? "IN" : "OUT");
        view.setTextColor(connected ? MPC_TOOLBAR_TEXT : Color.rgb(205, 154, 164));
        view.setContentDescription(
                "MPC Toolbar MIDI "
                        + ("IN".equals(view.getTag()) ? "IN" : "OUT")
                        + (connected ? " ready" : " unavailable"));
    }
    private Button topButton(String text) {
        Button b = button(text);
        b.setTextSize(10);
        b.setTextColor(MPC_TOOLBAR_TEXT);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(strokeBackground(
                Color.TRANSPARENT,
                Color.TRANSPARENT,
                MPC_FLAT_RADIUS_DP));
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
                "GRID", "SAMPLER", "PAD MIXER",
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

        View browserShortcut = findViewWithContentDescription(
                getWindow().getDecorView(), "MPC shortcut BROWSER");
        if (browserShortcut == null || !browserShortcut.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: BROWSER shortcut");
            return;
        }

        View mainShortcut = findViewWithContentDescription(
                getWindow().getDecorView(), "MPC shortcut MAIN");
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

        View bpmField = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main BPM field • double-tap for numeric entry");
        View barsField = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main BARS field • double-tap for numeric entry");
        View loopStartField = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main LOOP START field • double-tap for numeric entry");
        View loopEndField = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main LOOP END field • double-tap for numeric entry");
        if (bpmField == null || barsField == null
                || loopStartField == null || loopEndField == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main numeric entry targets");
            return;
        }

        View timeSignatureField = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Time Signature field • tap for editor");
        if (timeSignatureField == null
                || !timeSignatureField.performClick()
                || activeMpcParameterDialog == null
                || activeMpcParameterDialog.getButton(AlertDialog.BUTTON_NEGATIVE) == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Time Signature dialog");
            return;
        }
        activeMpcParameterDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

        if (timingCorrectTopButton == null
                || !timingCorrectTopButton.performClick()
                || activeMpcParameterDialog == null
                || activeMpcParameterDialog.getButton(AlertDialog.BUTTON_NEGATIVE) == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Timing Correct dialog");
            return;
        }
        activeMpcParameterDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();

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

        View quickSamplePrimary = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View sample primary action");
        View quickSampleBrowse = findViewWithContentDescription(
                getWindow().getDecorView(),
                "Main Track View browse samples");
        if (quickSamplePrimary == null || quickSampleBrowse == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main sample action state");
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

        View recArmAudit = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main sequence REC ARM");
        if (recArmAudit == null || !(recArmAudit instanceof Button)
                || !recArmAudit.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main REC ARM enable");
            return;
        }
        View recArmActiveAudit = findViewWithContentDescription(
                getWindow().getDecorView(),
                "MPC Main sequence REC ARM active");
        if (recArmActiveAudit == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main REC ARM active state");
            return;
        }
        if (!recArmActiveAudit.performClick()
                || findViewWithContentDescription(
                        getWindow().getDecorView(),
                        "MPC Main sequence REC ARM") == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: Main REC ARM disable");
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

        View mainViewSwitcher = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Track / Arrangement view switcher");
        View mainTrackSelector = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Track View header");
        View mainArrangementSelector = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Arrangement View header");
        View mainTrackWorkspace = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Mode Track View workspace");
        View mainArrangementWorkspace = findViewWithContentDescription(
                getWindow().getDecorView(), "Main Mode arrangement preview");
        if (mainViewSwitcher == null
                || mainViewSwitcher.getHeight() < dp(36)
                || mainTrackSelector == null
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
            Log.e(TAG, "UI_INTERACTION_FAILED: Main Track Select back");            return;
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
                getWindow().getDecorView(), "MPC Track View workspace");        if (trackViewWorkspace == null
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
        if (view == null || expectedDescription == null) return null;

        final CharSequence actual = view.getContentDescription();
        if (actual != null && expectedDescription.contentEquals(actual)) {
            return view;
        }

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
        if (view == null || expectedText == null) return null;

        if (view instanceof TextView) {
            final CharSequence actual = ((TextView) view).getText();
            if (actual != null && expectedText.contentEquals(actual)) {
                return view;
            }
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
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(cc)
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

    private void syncHardwareLevelModeLeds() {
        final int levelState = hardwareHalfLevelActive
                ? MpcHardwareFeedbackPolicy.LED_COLOR_2_FULL
                : hardwareFullLevelActive
                        ? MpcHardwareFeedbackPolicy.LED_COLOR_1_FULL
                        : MpcHardwareFeedbackPolicy.LED_OFF;
        setHardwareButtonLedState(39, levelState);
        setHardwareButtonLedState(
                40,
                hardwareSixteenLevelActive
                        ? MpcHardwareFeedbackPolicy.buttonLedOnState(40)
                        : MpcHardwareFeedbackPolicy.LED_OFF);
    }

    private void syncHardwareMuteModeLed() {
        setHardwareButtonLedState(
                4,
                MpcHardwareFeedbackPolicy.dualColor(
                        hardwarePadMuteModeActive || hardwareTrackMuteModeActive,
                        hardwareTrackMuteModeActive));
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
                showMainPage();
                setBottomStatus("MAIN");
                return;
            case MpcStudioMk2SemanticActions.NAVIGATE_TRACK_VIEW:
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
                showTrackSelectPage();
                setBottomStatus("TRACK SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.SEQUENCE_SELECTION_CONTEXT:
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
                showSequenceSelectPage();
                setBottomStatus("SEQUENCE SELECT • DATA DIAL / +/-");
                return;
            case MpcStudioMk2SemanticActions.PROGRAM_SELECTION_CONTEXT:
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PROGRAM);
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK_TYPE);
                setBottomStatus("TRACK TYPE • reserved");
                return;
            case MpcStudioMk2SemanticActions.DATA_DIAL_DELTA:
            case MpcStudioMk2SemanticActions.ADJUST_VALUE_DELTA:
                handleHardwareDialDelta(value0, value1 != 0);
                return;
            case MpcStudioMk2SemanticActions.DATA_DIAL_PRESS:
                if (navigationController.state().mode()
                        == MpcUiState.Mode.PAD_MIXER
                        && padMixerView != null) {
                    cyclePadMixerDialFocus();
                    return;
                }
                if (hardwareLocateActive) {
                    setBottomStatus(
                            "LOCATE • DATA DIAL = ±1 BEAT • SHIFT = ±1 TICK");
                } else if (hardwareFocusId() == 4) {
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
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
                hardwareFullLevelActive = value0 != 0;
                if (hardwareFullLevelActive) hardwareHalfLevelActive = false;
                syncHardwareLevelModeLeds();
                setBottomStatus(value0 != 0 ? "FULL LEVEL ON • 127" : "FULL LEVEL OFF");
                return;
            case MpcStudioMk2SemanticActions.HALF_LEVEL_STATE:
                hardwareHalfLevelActive = value0 != 0;
                if (hardwareHalfLevelActive) hardwareFullLevelActive = false;
                syncHardwareLevelModeLeds();
                setBottomStatus(value0 != 0 ? "HALF LEVEL ON • 64" : "HALF LEVEL OFF");
                return;
            case MpcStudioMk2SemanticActions.SIXTEEN_LEVEL_STATE:
                hardwareSixteenLevelActive = value0 != 0;
                if (hardwareSixteenLevelActive) {
                    hardwareFullLevelActive = false;
                    hardwareHalfLevelActive = false;
                }
                syncHardwareLevelModeLeds();
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
                hardwarePadMuteModeActive = value0 != 0;
                if (hardwarePadMuteModeActive) hardwareTrackMuteModeActive = false;
                syncHardwareMuteModeLed();
                setBottomStatus(value0 != 0 ? "PAD MUTE MODE" : "PAD MUTE MODE OFF");
                return;
            case MpcStudioMk2SemanticActions.TRACK_MUTE_MODE_STATE:
                hardwareTrackMuteModeActive = value0 != 0;
                if (hardwareTrackMuteModeActive) hardwarePadMuteModeActive = false;
                syncHardwareMuteModeLed();
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_LAYER);
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_START);
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
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SAMPLE_END);
                showSamplePage();
                setBottomStatus(value0 != 0
                        ? "SAMPLE END • FINE"
                        : "SAMPLE END");
                return;
            case MpcStudioMk2SemanticActions.TUNE_CONTEXT:
                hardwareTouchStripMode = TOUCH_STRIP_MODE_TUNE;
                syncHardwareTouchStripModeLeds();
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TUNE);
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
                    refreshMpcToolbarState();
                } else {
                    if (startupComplete) {
                        showTimingCorrectDialog();
                    } else {
                        setBottomStatus(
                                "TIMING CORRECT • waiting for sequencer");
                    }
                }
                return;
            case MpcStudioMk2SemanticActions.ZOOM_CONTEXT:
                if ("SAMPLE".equals(currentPage) && sampleWaveform != null) {
                    navigationController.setDataDialFocus(
                            value0 != 0
                                    ? MpcUiState.DataDialFocus.ZOOM_VERTICAL
                                    : MpcUiState.DataDialFocus.ZOOM_HORIZONTAL);
                    setBottomStatus(value0 != 0
                            ? "ZOOM VERTICAL • DATA DIAL / +/-"
                            : "ZOOM HORIZONTAL • DATA DIAL / +/-");
                } else if ("SEQ".equals(currentPage) && sequenceGridView != null) {
                    navigationController.setDataDialFocus(
                            value0 != 0
                                    ? MpcUiState.DataDialFocus.ZOOM_VERTICAL
                                    : MpcUiState.DataDialFocus.ZOOM_HORIZONTAL);
                    setBottomStatus(value0 != 0
                            ? "GRID ZOOM VERTICAL • DATA DIAL / +/-"
                            : "GRID ZOOM HORIZONTAL • DATA DIAL / +/-");
                } else if ("SEQ".equals(currentPage) && sequenceTimeline != null) {
                    if (value0 != 0) {
                        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.ZOOM_VERTICAL);
                        setBottomStatus("TIMELINE HAS NO VERTICAL AXIS");
                    } else {
                        navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TIMELINE);
                        setBottomStatus("TIMELINE ZOOM HORIZONTAL • DATA DIAL / +/-");
                    }
                } else {
                    navigationController.setDataDialFocus(MpcUiState.DataDialFocus.NONE);
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

    private void cyclePadMixerDialFocus() {
        final MpcUiState.DataDialFocus current =
                navigationController.state().dataDialFocus();
        final MpcUiState.DataDialFocus next;
        switch (current) {
            case PAD_MIXER_PAN:
                next = MpcUiState.DataDialFocus.PAD_MIXER_TUNE;
                break;
            case PAD_MIXER_TUNE:
                next = MpcUiState.DataDialFocus.PAD_MIXER_LEVEL;
                break;
            case PAD_MIXER_LEVEL:
            case PAD:
            default:
                next = MpcUiState.DataDialFocus.PAD_MIXER_PAN;
                break;
        }

        navigationController.setDataDialFocus(next);
        navigationController.setSubcontext(
                MpcUiState.Subcontext.PERFORMANCE);
        navigationController.setActionAvailable(true);

        if (padMixerView != null) {
            final MpcPadMixerView.ControlFocus focus =
                    next == MpcUiState.DataDialFocus.PAD_MIXER_PAN
                            ? MpcPadMixerView.ControlFocus.PAN
                            : next == MpcUiState.DataDialFocus.PAD_MIXER_TUNE
                                    ? MpcPadMixerView.ControlFocus.TUNE
                                    : MpcPadMixerView.ControlFocus.LEVEL;
            padMixerView.setControlFocus(focus);
        }

        setBottomStatus(
                "PAD MIXER • "
                        + (next == MpcUiState.DataDialFocus.PAD_MIXER_PAN
                                ? "PAN"
                                : next == MpcUiState.DataDialFocus.PAD_MIXER_TUNE
                                        ? "TUNE"
                                        : "LEVEL")
                        + " • DATA DIAL");
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
        if (navigationController.state().mode()
                == MpcUiState.Mode.PAD_MIXER
                && padMixerView != null) {
            final MpcUiState.DataDialFocus focus =
                    navigationController.state().dataDialFocus();
            switch (focus) {
                case PAD_MIXER_PAN: {
                    final float increment = fine ? 0.01f : 0.05f;
                    final float next = Math.max(
                            -1.0f,
                            Math.min(
                                    1.0f,
                                    nativeAudioGetPadPan(selectedPad)
                                            + delta * increment));
                    setBottomStatus(
                            nativeAudioSetPadPan(selectedPad, next));
                    break;
                }
                case PAD_MIXER_TUNE: {
                    final float increment = fine ? 0.1f : 1.0f;
                    final float next =
                            nativeAudioGetPadTuning(selectedPad)
                                    + delta * increment;
                    setBottomStatus(
                            nativeAudioSetPadTuning(selectedPad, next));
                    break;
                }
                case PAD_MIXER_LEVEL:
                default: {
                    final float increment = fine ? 0.001f : 0.01f;
                    final float next = Math.max(
                            0.0f,
                            Math.min(
                                    1.0f,
                                    nativeAudioGetPadLevel(selectedPad)
                                            + delta * increment));
                    setBottomStatus(
                            nativeAudioSetPadLevel(selectedPad, next));
                    break;
                }
            }
            refreshPadMixerView();
            refreshMpcCompactContext();
            return;
        }
        if (hardwareFocusId() == HARDWARE_FOCUS_SEQUENCE_BPM) {
            changeSequenceTempo(fine ? delta * 0.1 : delta);
            return;
        }
        if (hardwareFocusId() == HARDWARE_FOCUS_SEQUENCE_BARS) {
            changeSequenceBars(delta);
            return;
        }
        if (hardwareFocusId() == HARDWARE_FOCUS_SEQUENCE_START) {
            final int loopEnd = Math.max(
                    1, Math.min(128, nativeSequenceGetLoopEndBar()));
            final int nextStart = Math.max(
                    1, Math.min(loopEnd,
                            nativeSequenceGetLoopStartBar() + delta));
            setBottomStatus(nativeSequenceSetLoopBars(
                    nextStart, loopEnd));
            refreshMainModeFields();
            refreshMpcCompactContext();
            return;
        }
        if (hardwareFocusId() == HARDWARE_FOCUS_SEQUENCE_END) {
            final int bars = Math.max(
                    1, Math.min(128, nativeSequenceGetBars()));
            final int loopStart = Math.max(
                    1, Math.min(bars, nativeSequenceGetLoopStartBar()));
            final int nextEnd = Math.max(
                    loopStart, Math.min(bars,
                            nativeSequenceGetLoopEndBar() + delta));
            setBottomStatus(nativeSequenceSetLoopBars(
                    loopStart, nextEnd));
            refreshMainModeFields();
            refreshMpcCompactContext();
            return;
        }
        if (hardwareFocusId() == 2) {
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
                final boolean arrangementSelected =
                        mainTrackArrangementHost != null
                                && mainTrackArrangementHost.getChildCount() > 1
                                && mainTrackArrangementHost.getChildAt(1).getVisibility()
                                        == View.VISIBLE;
                showMainPage();
                setMainTrackArrangementView(arrangementSelected);
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
                navigationController.setSubcontext(
                        MpcUiState.Subcontext.TRACK_SELECT);
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.TRACK);
                navigationController.setActionAvailable(true);
                refreshMainDataDialFocusVisuals();
            } else if (navigationController.state().mode() == MpcUiState.Mode.TRACK_VIEW) {
                showTrackViewPage();
            } else {
                showMainPage();
            }
            return;
        }
        if (hardwareFocusId() == 4) {
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

            final boolean programSelectPageVisible =
                    pageTitle != null
                            && "MAIN • PROGRAM".equals(
                                    pageTitle.getText().toString());
            if (programSelectPageVisible) {
                showProgramSelectPage();
            } else {
                showMainPage();
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.PROGRAM);
                navigationController.setSubcontext(
                        MpcUiState.Subcontext.PROGRAM_SELECT);
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.PROGRAM);
                navigationController.setActionAvailable(true);
                refreshMainDataDialFocusVisuals();
            }
            return;
        }
        if (hardwareFocusId() == 3) {
            navigationController.setSubcontext(MpcUiState.Subcontext.SEQUENCE_SELECT);
            navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
            setBottomStatus(
                    delta > 0 ? nativeSequenceNext() : nativeSequencePrevious());
            navigationController.setSelectedSequence(nativeSequenceGetIndex());

            final boolean sequenceSelectPageVisible =
                    pageTitle != null
                            && "MAIN • SEQUENCE".equals(
                                    pageTitle.getText().toString());
            if (sequenceSelectPageVisible) {
                showSequenceSelectPage();
            } else if (navigationController.state().mode()
                    == MpcUiState.Mode.TRACK_VIEW) {
                showTrackViewPage();
            } else {
                showMainPage();
                navigationController.setDataDialFocus(MpcUiState.DataDialFocus.SEQUENCE);
                navigationController.setSubcontext(
                        MpcUiState.Subcontext.SEQUENCE_SELECT);
                navigationController.setDataDialFocus(
                        MpcUiState.DataDialFocus.SEQUENCE);
                navigationController.setActionAvailable(true);
                refreshMainDataDialFocusVisuals();
            }
            return;
        }
        if (hardwareFocusId() == 5) {
            setBottomStatus(
                    "TRACK TYPE • DRUM is the only implemented Track Type");
            refreshMainDataDialFocusVisuals();
            return;
        }

        if (hardwareFocusId() == 10) {
            final int trackIndex = startupComplete
                    ? Math.max(0, nativeSequenceGetSelectedTrack()) : 0;
            if (!startupComplete
                    || !"DRUM".equalsIgnoreCase(
                            nativeSequenceGetTrackType(trackIndex))) {
                setBottomStatus("TRACK EDIT • DRUM TRACK REQUIRED");
                return;
            }
            adjustTrackEditLayer(delta);
            return;
        }
        if (hardwareFocusId() == 11 || hardwareFocusId() == 12) {
            if (sampleWaveform == null || !"SAMPLE".equals(currentPage)) {
                setBottomStatus("ZOOM • SAMPLE EDIT REQUIRED");
                return;
            }
            if (hardwareFocusId() == 11) {
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
        if (hardwareFocusId() == 13 || hardwareFocusId() == 14) {
            if (sequenceGridView == null || !"SEQ".equals(currentPage)) {
                setBottomStatus("GRID ZOOM • SEQ GRID REQUIRED");
                return;
            }
            if (hardwareFocusId() == 13) {
                zoomSequenceGridHorizontal(delta);
            } else {
                zoomSequenceGridVertical(delta);
            }
            return;
        }
        if (hardwareFocusId() == 15) {
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
            updateTopMidiStatus(midiInTopStatus, connected);
            updateTopMidiStatus(midiOutTopStatus, connected);
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