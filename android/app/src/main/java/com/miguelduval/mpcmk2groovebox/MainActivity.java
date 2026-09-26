package com.miguelduval.mpcmk2groovebox;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements AndroidMidiBridge.Listener {
    private static final String TAG = "MpcGroovebox";
    private static final int REQUEST_OPEN_WAV = 1001;
    private static final int REQUEST_RECORD_AUDIO = 1002;
    private static final int REQUEST_MONITOR_AUDIO = 1003;
    private static final int MAX_SAMPLE_BYTES = 32 * 1024 * 1024;
    private static final String SMOKE_MODE_EXTRA = "mpc.groovebox.smoke.mode";

    static {
        System.loadLibrary("mpcgroovebox");
    }

    private AndroidMidiBridge midiBridge;
    private TextView status;
    private TextView devices;
    private TextView midiLog;
    private TextView selectedPadStatus;
    private TextView tuningStatus;
    private TextView levelStatus;
    private TextView panStatus;
    private TextView layerStatus;
    private TextView sampleRegionStatus;
    private TextView chopStatus;
    private TextView recordingStatus;
    private TextView recordingThresholdStatus;
    private int selectedPad = 0;
    private int selectedLayer = 0;
    private boolean uiOnlySmokeMode;
    private boolean uiAuditSmokeMode;
    private volatile boolean destroyed;
    private final ExecutorService startupExecutor = Executors.newSingleThreadExecutor();

    private static native String nativeEngineInfo();
    private static native String nativeAudioLoadSample(byte[] data);
    private static native String nativeAudioLoadSampleForPad(byte[] data, int pad);
    private static native String nativeAudioLoadSampleForPadLayer(byte[] data, int pad, int layer);
    private static native String nativeAudioSetPadTuning(int pad, float semitones);
    private static native float nativeAudioGetPadTuning(int pad);
    private static native String nativeAudioSetPadLevel(int pad, float level);
    private static native float nativeAudioGetPadLevel(int pad);
    private static native String nativeAudioSetPadPan(int pad, float pan);
    private static native float nativeAudioGetPadPan(int pad);
    private static native String nativeAudioSetPadSampleRegion(
            int pad, int layer, long startFrame, long endFrame);
    private static native long nativeAudioGetPadSampleRegionStart(int pad, int layer);
    private static native long nativeAudioGetPadSampleRegionEnd(int pad, int layer);
    private static native long nativeAudioGetPadSampleFrameCount(int pad, int layer);
    private static native String nativeAudioChopPadSampleToPads(
            int sourcePad, int sourceLayer, int chopCount);
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

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        String smokeMode = getIntent().getStringExtra(SMOKE_MODE_EXTRA);
        uiOnlySmokeMode = "ui-only".equals(smokeMode);
        uiAuditSmokeMode = "ui-audit".equals(smokeMode);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("MPC Studio MkII Groovebox — Hardware Bring-Up");
        title.setTextSize(20.0f);
        title.setGravity(Gravity.CENTER_VERTICAL);

        status = new TextView(this);
        status.setText("Starting native engine...");
        status.setTextSize(14.0f);

        devices = new TextView(this);
        devices.setText("Scanning MIDI devices...");
        devices.setTextSize(13.0f);

        Button scan = new Button(this);
        scan.setText("Refresh MIDI Devices");
        scan.setOnClickListener(v -> {
            if (midiBridge == null) {
                devices.setText("MIDI bridge is still starting...");
                return;
            }
            devices.setText(midiBridge.describeDevices());
        });

        Button connect = new Button(this);
        connect.setText("Connect MPC Studio MkII");
        connect.setOnClickListener(v -> {
            if (midiBridge == null) {
                status.setText("MIDI bridge is still starting...");
                return;
            }
            midiBridge.connectPreferred();
        });

        selectedPadStatus = new TextView(this);
        selectedPadStatus.setText("Sample target pad: 1");
        selectedPadStatus.setTextSize(13.0f);

        tuningStatus = new TextView(this);
        tuningStatus.setText("Pad 1 tuning: +0.00 st");
        tuningStatus.setTextSize(13.0f);

        LinearLayout tuningControls = new LinearLayout(this);
        tuningControls.setOrientation(LinearLayout.HORIZONTAL);

        Button tuneDown = new Button(this);
        tuneDown.setText("-1 st");
        tuneDown.setOnClickListener(v -> adjustSelectedPadTuning(-1.0f));

        Button tuneReset = new Button(this);
        tuneReset.setText("Reset");
        tuneReset.setOnClickListener(v -> adjustSelectedPadTuning(0.0f, true));

        Button tuneUp = new Button(this);
        tuneUp.setText("+1 st");
        tuneUp.setOnClickListener(v -> adjustSelectedPadTuning(1.0f));

        tuningControls.addView(tuneDown, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tuningControls.addView(tuneReset, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        tuningControls.addView(tuneUp, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        levelStatus = new TextView(this);
        levelStatus.setText("Pad 1 level: 100%");
        levelStatus.setTextSize(13.0f);

        LinearLayout levelControls = new LinearLayout(this);
        levelControls.setOrientation(LinearLayout.HORIZONTAL);

        Button levelDown = new Button(this);
        levelDown.setText("-10%");
        levelDown.setOnClickListener(v -> adjustSelectedPadLevel(-0.10f));

        Button levelReset = new Button(this);
        levelReset.setText("Level Reset");
        levelReset.setOnClickListener(v -> setSelectedPadLevel(1.0f));

        Button levelUp = new Button(this);
        levelUp.setText("+10%");
        levelUp.setOnClickListener(v -> adjustSelectedPadLevel(0.10f));

        levelControls.addView(levelDown, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        levelControls.addView(levelReset, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        levelControls.addView(levelUp, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        panStatus = new TextView(this);
        panStatus.setText("Pad 1 pan: C");
        panStatus.setTextSize(13.0f);

        LinearLayout panControls = new LinearLayout(this);
        panControls.setOrientation(LinearLayout.HORIZONTAL);

        Button panLeft = new Button(this);
        panLeft.setText("Pan L");
        panLeft.setOnClickListener(v -> setSelectedPadPan(-1.0f));

        Button panCenter = new Button(this);
        panCenter.setText("Pan C");
        panCenter.setOnClickListener(v -> setSelectedPadPan(0.0f));

        Button panRight = new Button(this);
        panRight.setText("Pan R");
        panRight.setOnClickListener(v -> setSelectedPadPan(1.0f));

        panControls.addView(panLeft, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panControls.addView(panCenter, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        panControls.addView(panRight, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        layerStatus = new TextView(this);
        layerStatus.setText("Pad 1 sample layer: 1/8");
        layerStatus.setTextSize(13.0f);

        LinearLayout layerControls = new LinearLayout(this);
        layerControls.setOrientation(LinearLayout.HORIZONTAL);

        Button layerDown = new Button(this);
        layerDown.setText("Layer -");
        layerDown.setOnClickListener(v -> adjustSelectedLayer(-1));

        Button layerUp = new Button(this);
        layerUp.setText("Layer +");
        layerUp.setOnClickListener(v -> adjustSelectedLayer(1));

        layerControls.addView(layerDown, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        layerControls.addView(layerUp, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        sampleRegionStatus = new TextView(this);
        sampleRegionStatus.setText("Pad 1 layer 1 sample region: no sample");
        sampleRegionStatus.setTextSize(13.0f);

        LinearLayout sampleRegionControls = new LinearLayout(this);
        sampleRegionControls.setOrientation(LinearLayout.HORIZONTAL);

        Button regionStartDown = new Button(this);
        regionStartDown.setText("Start -");
        regionStartDown.setOnClickListener(v -> nudgeSelectedSampleRegionStart(-1000));

        Button regionStartUp = new Button(this);
        regionStartUp.setText("Start +");
        regionStartUp.setOnClickListener(v -> nudgeSelectedSampleRegionStart(1000));

        Button regionEndDown = new Button(this);
        regionEndDown.setText("End -");
        regionEndDown.setOnClickListener(v -> nudgeSelectedSampleRegionEnd(-1000));

        Button regionEndUp = new Button(this);
        regionEndUp.setText("End +");
        regionEndUp.setOnClickListener(v -> nudgeSelectedSampleRegionEnd(1000));

        Button regionFull = new Button(this);
        regionFull.setText("Full Region");
        regionFull.setOnClickListener(v -> resetSelectedSampleRegion());

        sampleRegionControls.addView(regionStartDown, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sampleRegionControls.addView(regionStartUp, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sampleRegionControls.addView(regionEndDown, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sampleRegionControls.addView(regionEndUp, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        sampleRegionControls.addView(regionFull, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        chopStatus = new TextView(this);
        chopStatus.setText("Chop: select a sample, then choose 4, 8 or 16 slices");
        chopStatus.setTextSize(13.0f);

        LinearLayout chopControls = new LinearLayout(this);
        chopControls.setOrientation(LinearLayout.HORIZONTAL);

        Button chop4 = new Button(this);
        chop4.setText("Chop 4");
        chop4.setOnClickListener(v -> chopSelectedSample(4));

        Button chop8 = new Button(this);
        chop8.setText("Chop 8");
        chop8.setOnClickListener(v -> chopSelectedSample(8));

        Button chop16 = new Button(this);
        chop16.setText("Chop 16");
        chop16.setOnClickListener(v -> chopSelectedSample(16));

        chopControls.addView(chop4, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        chopControls.addView(chop8, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        chopControls.addView(chop16, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout padGrid = new LinearLayout(this);
        padGrid.setOrientation(LinearLayout.VERTICAL);

        for (int row = 0; row < 4; ++row) {
            LinearLayout padRow = new LinearLayout(this);
            padRow.setOrientation(LinearLayout.HORIZONTAL);

            for (int column = 0; column < 4; ++column) {
                final int pad = row * 4 + column;
                Button padButton = new Button(this);
                padButton.setText(String.valueOf(pad + 1));
                padButton.setOnClickListener(v -> selectPad(pad));
                padRow.addView(padButton, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            }

            padGrid.addView(padRow, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        LinearLayout audioControls = new LinearLayout(this);
        audioControls.setOrientation(LinearLayout.HORIZONTAL);

        Button loadSample = new Button(this);
        loadSample.setText("Load WAV Sample");
        loadSample.setOnClickListener(v -> openWavPicker());

        Button audioStart = new Button(this);
        audioStart.setText("Start Sampler");
        audioStart.setOnClickListener(v -> status.setText(nativeAudioStart()));

        Button audioStop = new Button(this);
        audioStop.setText("Stop Audio");
        audioStop.setOnClickListener(v -> status.setText(nativeAudioStop()));

        Button audioStatus = new Button(this);
        audioStatus.setText("Audio Status");
        audioStatus.setOnClickListener(v -> status.setText(nativeAudioStatus()));

        audioControls.addView(loadSample, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        audioControls.addView(audioStart, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        audioControls.addView(audioStop, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        audioControls.addView(audioStatus, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        recordingStatus = new TextView(this);
        recordingStatus.setText("Recording: idle");
        recordingStatus.setTextSize(13.0f);

        recordingThresholdStatus = new TextView(this);
        recordingThresholdStatus.setText("Recording threshold: Off");
        recordingThresholdStatus.setTextSize(13.0f);

        LinearLayout recordingThresholdControls = new LinearLayout(this);
        recordingThresholdControls.setOrientation(LinearLayout.HORIZONTAL);

        Button thresholdOff = new Button(this);
        thresholdOff.setText("Threshold Off");
        thresholdOff.setOnClickListener(v -> setRecordingThreshold(0.0f));

        Button threshold10 = new Button(this);
        threshold10.setText("Threshold 10%");
        threshold10.setOnClickListener(v -> setRecordingThreshold(0.10f));

        Button threshold25 = new Button(this);
        threshold25.setText("Threshold 25%");
        threshold25.setOnClickListener(v -> setRecordingThreshold(0.25f));

        Button threshold50 = new Button(this);
        threshold50.setText("Threshold 50%");
        threshold50.setOnClickListener(v -> setRecordingThreshold(0.50f));

        recordingThresholdControls.addView(thresholdOff, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        recordingThresholdControls.addView(threshold10, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        recordingThresholdControls.addView(threshold25, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        recordingThresholdControls.addView(threshold50, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout recordingControls = new LinearLayout(this);
        recordingControls.setOrientation(LinearLayout.HORIZONTAL);

        Button recordMicrophone = new Button(this);
                recordMicrophone.setText("Record");
        recordMicrophone.setOnClickListener(v -> startRecordingFromUi());

        Button stopRecording = new Button(this);
        stopRecording.setText("Stop Recording");
        stopRecording.setOnClickListener(v -> {
            status.setText(nativeAudioStopRecording());
            refreshRecordingStatus();
        });

        Button recordingStatusButton = new Button(this);
        recordingStatusButton.setText("Recording Status");
        recordingStatusButton.setOnClickListener(v -> refreshRecordingStatus());

        recordingControls.addView(recordMicrophone, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        recordingControls.addView(stopRecording, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        recordingControls.addView(recordingStatusButton, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout monitorControls = new LinearLayout(this);
        monitorControls.setOrientation(LinearLayout.HORIZONTAL);

        Button monitorOn = new Button(this);
        monitorOn.setText("Monitor On");
        monitorOn.setOnClickListener(v -> startMonitorFromUi());

        Button monitorOff = new Button(this);
        monitorOff.setText("Monitor Off");
        monitorOff.setOnClickListener(v -> {
            status.setText(nativeAudioStopMonitor());
            refreshRecordingStatus();
        });

        Button assignRecording = new Button(this);
        assignRecording.setText("Assign Last Recording");
        assignRecording.setOnClickListener(v -> {
            final String result =
                    nativeAudioAssignRecordingToPadLayer(selectedPad, selectedLayer);
            status.setText(result);
            recordingStatus.setText(nativeAudioRecordingStatus());
        });

        monitorControls.addView(monitorOn, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        monitorControls.addView(monitorOff, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        monitorControls.addView(assignRecording, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout diagnostics = new LinearLayout(this);
        diagnostics.setOrientation(LinearLayout.HORIZONTAL);

        Button red = new Button(this);
        red.setText("Pad 1 Red");
        red.setOnClickListener(v -> midiBridge.testPadRed());

        Button blue = new Button(this);
        blue.setText("Pad 1 Blue");
        blue.setOnClickListener(v -> midiBridge.testPadBlue());

        Button off = new Button(this);
        off.setText("Pad 1 Off");
        off.setOnClickListener(v -> midiBridge.testPadOff());

        Button playLed = new Button(this);
        playLed.setText("Play LED");
        playLed.setOnClickListener(v -> midiBridge.testPlayLed());

        Button touchLed = new Button(this);
        touchLed.setText("Touch LED");
        touchLed.setOnClickListener(v -> midiBridge.testTouchLed());

        Button repeatLed = new Button(this);
        repeatLed.setText("Repeat LED");
        repeatLed.setOnClickListener(v -> midiBridge.testNoteRepeatLed());

        Button lcdTest = new Button(this);
        lcdTest.setText("LCD Test");
        lcdTest.setOnClickListener(v -> midiBridge.testLcd());

        LinearLayout diagnosticsRow1 = new LinearLayout(this);
        diagnosticsRow1.setOrientation(LinearLayout.HORIZONTAL);

        diagnosticsRow1.addView(red, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        diagnosticsRow1.addView(blue, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        diagnosticsRow1.addView(off, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        diagnosticsRow1.addView(playLed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        diagnosticsRow1.addView(touchLed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout diagnosticsRow2 = new LinearLayout(this);
        diagnosticsRow2.setOrientation(LinearLayout.HORIZONTAL);
        diagnosticsRow2.addView(repeatLed, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        diagnosticsRow2.addView(lcdTest, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        diagnostics.addView(diagnosticsRow1, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        diagnostics.addView(diagnosticsRow2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        midiLog = new TextView(this);
        midiLog.setText("MIDI IN:\n");
        midiLog.setTextSize(13.0f);

        ScrollView midiScroll = new ScrollView(this);
        midiScroll.setFillViewport(true);
        midiScroll.addView(midiLog);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scan, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(connect, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(selectedPadStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(tuningStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(tuningControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(levelStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(levelControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(panStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(panControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(layerStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(layerControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sampleRegionStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(sampleRegionControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(chopStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(chopControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(padGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(audioControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(recordingStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(recordingThresholdStatus, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(recordingThresholdControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(recordingControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(monitorControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(diagnostics, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(devices, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(midiScroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));

        ScrollView contentScroll = new ScrollView(this);
        contentScroll.setFillViewport(true);
        contentScroll.addView(root);
        setContentView(contentScroll);
        Log.i(TAG, "UI_READY");

        if (uiOnlySmokeMode) {
            status.setText("Startup diagnostic: UI-only; native/MIDI deferred");
            Log.i(TAG, "UI_ONLY_COMPLETE");
            return;
        }

        root.postOnAnimation(() -> {
            Log.i(TAG, "STARTUP_BEGIN");
            startupExecutor.execute(() -> {
                Log.i(TAG, "NATIVE_INFO_BEGIN");
                String engineInfo = nativeEngineInfo();
                Log.i(TAG, "NATIVE_INFO_END");
                Log.i(TAG, "BUNDLED_SAMPLE_BEGIN");
                String sampleResult = loadBundledSample();
                Log.i(TAG, "BUNDLED_SAMPLE_END");

                runOnUiThread(() -> {
                    if (destroyed) {
                        return;
                    }

                    status.setText("Native: " + engineInfo + "\n" + sampleResult);
                    updateSampleRegionStatus();
                    updateRecordingThresholdStatus();
                    Log.i(TAG, "MIDI_BRIDGE_BEGIN");
                    midiBridge = new AndroidMidiBridge(this, this);
                    Log.i(TAG, "MIDI_BRIDGE_END");
                    Log.i(TAG, "STARTUP_COMPLETE");
                    if (uiAuditSmokeMode) {
                        runUiHierarchySmokeCheck();
                    }
                });
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_OPEN_WAV || resultCode != RESULT_OK || data == null) {
            return;
        }

        final Uri uri = data.getData();
        if (uri == null) {
            status.setText("Sample load failed: no file selected");
            return;
        }

        try {
            status.setText("Loading WAV sample...");
            final byte[] wavBytes = readSampleBytes(uri);

            // The native engine currently owns the realtime stream exclusively.
            // Stop it before replacing the immutable sample buffer.
            nativeAudioStop();

            final String result =
                    nativeAudioLoadSampleForPadLayer(wavBytes, selectedPad, selectedLayer);
            status.setText(result);
            updateSampleRegionStatus();
        } catch (IOException | IllegalArgumentException e) {
            status.setText(
                    "Sample file load failed: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
                            + "\nPrevious sample, if any, was kept.");
        }
    }

    private void startRecordingFromUi() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
            status.setText("Microphone permission requested");
            return;
        }

        status.setText(nativeAudioStartRecording());
        refreshRecordingStatus();
    }

    private void startMonitorFromUi() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    REQUEST_MONITOR_AUDIO);
            status.setText("Microphone permission requested for monitor");
            return;
        }

        status.setText(nativeAudioStartMonitor());
        refreshRecordingStatus();
    }

    private void setRecordingThreshold(float threshold) {
        final String result = nativeAudioSetRecordingThreshold(threshold);
        status.setText(result);
        updateRecordingThresholdStatus();
        refreshRecordingStatus();
    }

    private void updateRecordingThresholdStatus() {
        final int percent = Math.round(
                nativeAudioGetRecordingThreshold() * 100.0f);
        if (percent <= 0) {
            recordingThresholdStatus.setText("Recording threshold: Off");
            return;
        }

        recordingThresholdStatus.setText(
                "Recording threshold: " + percent + "%");
    }

    private void refreshRecordingStatus() {
        final String result = nativeAudioRecordingStatus();
        recordingStatus.setText(result);
        status.setText(result);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText(nativeAudioStartRecording());
                refreshRecordingStatus();
            } else {
                status.setText("Microphone permission denied");
                recordingStatus.setText("Recording: permission denied");
            }
            return;
        }

        if (requestCode == REQUEST_MONITOR_AUDIO) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                status.setText(nativeAudioStartMonitor());
                refreshRecordingStatus();
            } else {
                status.setText("Microphone permission denied for monitor");
                recordingStatus.setText("Monitor: permission denied");
            }
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
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

    @Override
    public void onDevicesChanged(String description) {
        runOnUiThread(() -> devices.setText(description));
    }

    @Override
    public void onMidi(String description) {
        runOnUiThread(() -> {
            String updated = midiLog.getText().toString() + description + "\n";
            if (updated.length() > 12000) {
                updated = updated.substring(updated.length() - 12000);
            }
            midiLog.setText(updated);
        });
    }

    @Override
    public void onConnection(String description) {
        runOnUiThread(() -> status.setText(description));
    }

    private void runUiHierarchySmokeCheck() {
        Log.i(TAG, "UI_HIERARCHY_BEGIN");

        final long bundledFrameCount =
                nativeAudioGetPadSampleFrameCount(0, 0);
        String[] expectedTexts = {
                "MPC Studio MkII Groovebox — Hardware Bring-Up",
                "Refresh MIDI Devices",
                "Connect MPC Studio MkII",
                "Load WAV Sample",
                "Start Sampler",
                "Record",
                "Monitor On",
                "Monitor Off",
                "Assign Last Recording",
                "Recording: idle",
                "Pad 1 tuning: +0.00 st",
                "Pad 1 level: 100%",
                "Pad 1 pan: C",
                "Pad 1 sample layer: 1/8",
                "Pad 1 layer 1 sample region: 0-" + bundledFrameCount
                        + " / " + bundledFrameCount + " frames",
                "Chop: select a sample, then choose 4, 8 or 16 slices",
                "Chop 4",
                "Chop 8",
                "Chop 16",
                "Recording threshold: Off",
                "Threshold Off",
                "Threshold 10%",
                "Threshold 25%",
                "Threshold 50%",
                "Start -",
                "Start +",
                "End -",
                "End +",
                "Full Region"
        };

        View root = getWindow().getDecorView();
        for (String expected : expectedTexts) {
            View view = findViewWithExactText(root, expected);
            if (view == null) {
                Log.e(TAG, "UI_HIERARCHY_FAILED: missing text=" + expected);
                return;
            }
            if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                Log.e(TAG, "UI_HIERARCHY_FAILED: zero-size text=" + expected
                        + " width=" + view.getWidth()
                        + " height=" + view.getHeight());
                return;
            }
            Log.i(TAG, "UI_ELEMENT_PRESENT: " + expected
                    + " shown=" + view.isShown()
                    + " width=" + view.getWidth()
                    + " height=" + view.getHeight());
        }

        Log.i(TAG, "UI_HIERARCHY_COMPLETE");
        runUiInteractionSmokeCheck();
    }

    private void runUiInteractionSmokeCheck() {
        Log.i(TAG, "UI_INTERACTION_BEGIN");

        View threshold25 = findViewWithExactText(
                getWindow().getDecorView(), "Threshold 25%");
        if (threshold25 == null || !threshold25.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Threshold 25%");
            return;
        }
        if (!assertUiTextPresent("Recording threshold: 25%")) {
            return;
        }

        View thresholdOff = findViewWithExactText(
                getWindow().getDecorView(), "Threshold Off");
        if (thresholdOff == null || !thresholdOff.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Threshold Off");
            return;
        }
        if (!assertUiTextPresent("Recording threshold: Off")) {
            return;
        }

        View assignRecording = findViewWithExactText(
                getWindow().getDecorView(), "Assign Last Recording");
        if (assignRecording == null || !assignRecording.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Assign Last Recording");
            return;
        }
        if (!assertUiTextPresent("Recording assign failed: no recorded audio")) {
            return;
        }
        Log.i(TAG, "UI_RECORDING_ASSIGN_EMPTY_COMPLETE");

        View padTwo = findViewWithExactText(getWindow().getDecorView(), "2");
        if (padTwo == null || !padTwo.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click pad 2");
            return;
        }

        if (!assertUiTextPresent("Sample target pad: 2")) {
            return;
        }
        if (!assertUiTextPresent("Pad 2 tuning: +0.00 st")) {
            return;
        }
        if (!assertUiTextPresent("Pad 2 level: 100%")) {
            return;
        }
        if (!assertUiTextPresent("Pad 2 pan: C")) {
            return;
        }
        if (!assertUiTextPresent("Pad 2 sample layer: 1/8")) {
            return;
        }

        View layerUp = findViewWithExactText(getWindow().getDecorView(), "Layer +");
        if (layerUp == null || !layerUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer +");
            return;
        }
        if (!assertUiTextPresent("Pad 2 sample layer: 2/8")) {
            return;
        }

        for (int index = 0; index < 6; ++index) {
            if (!layerUp.performClick()) {
                Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer + at upper-range step " + index);
                return;
            }
        }

        if (!assertUiTextPresent("Pad 2 sample layer: 8/8")) {
            return;
        }

        if (!layerUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer + at upper clamp");
            return;
        }

        if (!assertUiTextPresent("Pad 2 sample layer: 8/8")) {
            return;
        }

        View layerDown = findViewWithExactText(getWindow().getDecorView(), "Layer -");
        if (layerDown == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not find Layer -");
            return;
        }

        for (int index = 0; index < 7; ++index) {
            if (!layerDown.performClick()) {
                Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer - at lower-range step " + index);
                return;
            }
        }

        if (!assertUiTextPresent("Pad 2 sample layer: 1/8")) {
            return;
        }

        if (!layerDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer - at lower clamp");
            return;
        }

        if (!assertUiTextPresent("Pad 2 sample layer: 1/8")) {
            return;
        }

        View levelUp = findViewWithExactText(getWindow().getDecorView(), "+10%");
        if (levelUp == null || !levelUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click +10%");
            return;
        }
        if (!assertUiTextPresent("Pad 2 level: 100%")) {
            Log.e(TAG, "UI_INTERACTION_FAILED: expected level clamp at 100%");
            return;
        }

        View levelDown = findViewWithExactText(getWindow().getDecorView(), "-10%");
        if (levelDown == null || !levelDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click -10%");
            return;
        }
        if (!assertUiTextPresent("Pad 2 level: 90%")) {
            return;
        }

        View panRight = findViewWithExactText(getWindow().getDecorView(), "Pan R");
        if (panRight == null || !panRight.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Pan R");
            return;
        }
        if (!assertUiTextPresent("Pad 2 pan: R100")) {
            return;
        }

        View panCenter = findViewWithExactText(getWindow().getDecorView(), "Pan C");
        if (panCenter == null || !panCenter.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Pan C");
            return;
        }
        if (!assertUiTextPresent("Pad 2 pan: C")) {
            return;
        }

        View tuneUp = findViewWithExactText(getWindow().getDecorView(), "+1 st");
        if (tuneUp == null || !tuneUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click +1 st");
            return;
        }

        if (!assertUiTextPresent("Pad 2 tuning: +1.00 st")) {
            return;
        }

        for (int index = 0; index < 23; ++index) {
            if (!tuneUp.performClick()) {
                Log.e(TAG, "UI_INTERACTION_FAILED: could not click +1 st at upper-range step " + index);
                return;
            }
        }

        if (!assertUiTextPresent("Pad 2 tuning: +24.00 st")) {
            return;
        }

        if (!tuneUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click +1 st at upper clamp");
            return;
        }

        if (!assertUiTextPresent("Pad 2 tuning: +24.00 st")) {
            return;
        }

        View tuneDown = findViewWithExactText(getWindow().getDecorView(), "-1 st");
        if (tuneDown == null || !tuneDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click -1 st");
            return;
        }

        for (int index = 0; index < 47; ++index) {
            if (!tuneDown.performClick()) {
                Log.e(TAG, "UI_INTERACTION_FAILED: could not click -1 st at lower-range step " + index);
                return;
            }
        }

        if (!assertUiTextPresent("Pad 2 tuning: -24.00 st")) {
            return;
        }

        if (!tuneDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click -1 st at lower clamp");
            return;
        }

        if (!assertUiTextPresent("Pad 2 tuning: -24.00 st")) {
            return;
        }

        View padOne = findViewWithExactText(getWindow().getDecorView(), "1");
        if (padOne == null || !padOne.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click pad 1");
            return;
        }

        if (!assertUiTextPresent("Sample target pad: 1")) {
            return;
        }
        if (!assertUiTextPresent("Pad 1 tuning: +0.00 st")) {
            return;
        }
        if (!assertUiTextPresent("Pad 1 level: 100%")) {
            return;
        }
        if (!assertUiTextPresent("Pad 1 pan: C")) {
            return;
        }
        if (!assertUiTextPresent("Pad 1 sample layer: 1/8")) {
            return;
        }

        final long padOneTotal = nativeAudioGetPadSampleFrameCount(0, 0);
        if (!assertUiTextPresent(
                "Pad 1 layer 1 sample region: 0-" + padOneTotal
                        + " / " + padOneTotal + " frames")) {
            return;
        }

        View chop4 = findViewWithExactText(
                getWindow().getDecorView(), "Chop 4");
        if (chop4 == null || !chop4.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Chop 4");
            return;
        }
        if (!assertUiTextPresent(
                "Chop complete: Pad 1 layer 1 -> pads 1-4 (4 slices)")) {
            return;
        }

        View startUp = findViewWithExactText(getWindow().getDecorView(), "Start +");
        if (startUp == null || !startUp.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Start +");
            return;
        }
        if (!assertUiTextPresent(
                "Pad 1 layer 1 sample region: 1000-" + padOneTotal
                        + " / " + padOneTotal + " frames")) {
            return;
        }

        View endDown = findViewWithExactText(getWindow().getDecorView(), "End -");
        if (endDown == null || !endDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click End -");
            return;
        }
        if (!assertUiTextPresent(
                "Pad 1 layer 1 sample region: 1000-" + (padOneTotal - 1000)
                        + " / " + padOneTotal + " frames")) {
            return;
        }

        View startDown = findViewWithExactText(getWindow().getDecorView(), "Start -");
        if (startDown == null || !startDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Start -");
            return;
        }
        if (!assertUiTextPresent(
                "Pad 1 layer 1 sample region: 0-" + (padOneTotal - 1000)
                        + " / " + padOneTotal + " frames")) {
            return;
        }

        View fullRegion = findViewWithExactText(getWindow().getDecorView(), "Full Region");
        if (fullRegion == null || !fullRegion.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Full Region");
            return;
        }
        if (!assertUiTextPresent(
                "Pad 1 layer 1 sample region: 0-" + padOneTotal
                        + " / " + padOneTotal + " frames")) {
            return;
        }

        Log.i(TAG, "UI_INTERACTION_COMPLETE");
    }

    private boolean assertUiTextPresent(String expectedText) {
        View view = findViewWithExactText(getWindow().getDecorView(), expectedText);
        if (view == null) {
            Log.e(TAG, "UI_INTERACTION_FAILED: missing text=" + expectedText);
            return false;
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            Log.e(TAG, "UI_INTERACTION_FAILED: zero-size text=" + expectedText
                    + " width=" + view.getWidth()
                    + " height=" + view.getHeight());
            return false;
        }
        Log.i(TAG, "UI_INTERACTION_STATE: " + expectedText);
        return true;
    }

    private View findViewWithExactText(View view, String expectedText) {
        if (view instanceof android.widget.TextView) {
            CharSequence actualText = ((android.widget.TextView) view).getText();
            if (expectedText.contentEquals(actualText)) {
                return view;
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); ++index) {
                View match = findViewWithExactText(group.getChildAt(index), expectedText);
                if (match != null) {
                    return match;
                }
            }
        }

        return null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void selectPad(int pad) {
        if (pad < 0 || pad >= 16) {
            return;
        }

        selectedPad = pad;
        selectedPadStatus.setText("Sample target pad: " + (pad + 1));
        updatePadToneStatus();
        updateLayerStatus();
        updateSampleRegionStatus();
    }

    private void adjustSelectedPadTuning(float delta) {
        adjustSelectedPadTuning(delta, false);
    }

    private void adjustSelectedPadTuning(float delta, boolean reset) {
        float target = reset ? 0.0f : nativeAudioGetPadTuning(selectedPad) + delta;
        String result = nativeAudioSetPadTuning(selectedPad, target);
        status.setText(result);
        updatePadToneStatus();
    }

    private void updateTuningStatus() {
        float tuning = nativeAudioGetPadTuning(selectedPad);
        String sign = tuning >= 0.0f ? "+" : "";
        tuningStatus.setText(
                String.format(java.util.Locale.ROOT, "Pad %d tuning: %s%.2f st",
                        selectedPad + 1, sign, tuning));
    }

    private void adjustSelectedPadLevel(float delta) {
        setSelectedPadLevel(nativeAudioGetPadLevel(selectedPad) + delta);
    }

    private void setSelectedPadLevel(float target) {
        String result = nativeAudioSetPadLevel(selectedPad, target);
        status.setText(result);
        updateLevelStatus();
    }

    private void updateLevelStatus() {
        int percent = Math.round(nativeAudioGetPadLevel(selectedPad) * 100.0f);
        levelStatus.setText("Pad " + (selectedPad + 1) + " level: " + percent + "%");
    }

    private void setSelectedPadPan(float pan) {
        String result = nativeAudioSetPadPan(selectedPad, pan);
        status.setText(result);
        updatePanStatus();
    }

    private void updatePanStatus() {
        float pan = nativeAudioGetPadPan(selectedPad);
        if (pan < -0.001f) {
            panStatus.setText(String.format(java.util.Locale.ROOT,
                    "Pad %d pan: L%d",
                    selectedPad + 1, Math.round(-pan * 100.0f)));
            return;
        }
        if (pan > 0.001f) {
            panStatus.setText(String.format(java.util.Locale.ROOT,
                    "Pad %d pan: R%d",
                    selectedPad + 1, Math.round(pan * 100.0f)));
            return;
        }
        panStatus.setText("Pad " + (selectedPad + 1) + " pan: C");
    }

    private void updatePadToneStatus() {
        updateTuningStatus();
        updateLevelStatus();
        updatePanStatus();
    }

    private void adjustSelectedLayer(int delta) {
        selectedLayer = Math.max(0, Math.min(7, selectedLayer + delta));
        updateLayerStatus();
        updateSampleRegionStatus();
    }

    private void updateLayerStatus() {
        layerStatus.setText("Pad " + (selectedPad + 1)
                + " sample layer: " + (selectedLayer + 1) + "/8");
    }

    private void updateSampleRegionStatus() {
        final long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            sampleRegionStatus.setText(
                    "Pad " + (selectedPad + 1)
                            + " layer " + (selectedLayer + 1)
                            + " sample region: no sample");
            return;
        }

        final long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        final long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        sampleRegionStatus.setText(
                "Pad " + (selectedPad + 1)
                        + " layer " + (selectedLayer + 1)
                        + " sample region: " + start + "-" + end
                        + " / " + total + " frames");
    }

    private void nudgeSelectedSampleRegionStart(long delta) {
        final long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            status.setText("Sample region change failed: no sample assigned");
            return;
        }

        final long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        final long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        final long nextStart =
                Math.max(0L, Math.min(Math.max(0L, end - 1L), start + delta));
        applySelectedSampleRegion(nextStart, end);
    }

    private void nudgeSelectedSampleRegionEnd(long delta) {
        final long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            status.setText("Sample region change failed: no sample assigned");
            return;
        }

        final long start = nativeAudioGetPadSampleRegionStart(selectedPad, selectedLayer);
        final long end = nativeAudioGetPadSampleRegionEnd(selectedPad, selectedLayer);
        final long nextEnd =
                Math.min(total, Math.max(Math.min(total, start + 1L), end + delta));
        applySelectedSampleRegion(start, nextEnd);
    }

    private void applySelectedSampleRegion(long start, long end) {
        final String result =
                nativeAudioSetPadSampleRegion(
                        selectedPad, selectedLayer, start, end);
        status.setText(result);
        updateSampleRegionStatus();
    }

    private void chopSelectedSample(int chopCount) {
        final String result = nativeAudioChopPadSampleToPads(
                selectedPad, selectedLayer, chopCount);
        status.setText(result);
        chopStatus.setText(result);
        updateSampleRegionStatus();
    }

    private void resetSelectedSampleRegion() {
        final long total = nativeAudioGetPadSampleFrameCount(selectedPad, selectedLayer);
        if (total <= 0) {
            status.setText("Sample region change failed: no sample assigned");
            return;
        }

        applySelectedSampleRegion(0L, total);
    }

    private void openWavPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_OPEN_WAV);
    }

    private byte[] readSampleBytes(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri)) {
            if (input == null) {
                throw new IOException("could not open selected file");
            }

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;

            while ((count = input.read(buffer)) != -1) {
                if (output.size() + count > MAX_SAMPLE_BYTES) {
                    throw new IOException("file is larger than 32 MB");
                }

                output.write(buffer, 0, count);
            }

            if (output.size() == 0) {
                throw new IOException("selected file is empty");
            }

            return output.toByteArray();
        }
    }

    private String loadBundledSample() {
        Log.i(TAG, "loadBundledSample:begin");
        try (InputStream input = getAssets().open("samples/pad01.wav.b64")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;

            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }

            String encoded = output.toString(StandardCharsets.UTF_8.name());
            byte[] wavBytes = Base64.decode(encoded, Base64.DEFAULT);
            String fallbackResult = nativeAudioLoadSample(wavBytes);
            String padLayerResult =
                    nativeAudioLoadSampleForPadLayer(wavBytes, 0, 0);
            String result = fallbackResult + " | " + padLayerResult;
            Log.i(TAG, "loadBundledSample:nativeResult=" + result);
            return result;
        } catch (IOException | IllegalArgumentException e) {
            Log.e(TAG, "loadBundledSample:failed", e);
            return "Sample asset load failed: " + e.getMessage();
        } finally {
            Log.i(TAG, "loadBundledSample:end");
        }
    }
}
