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