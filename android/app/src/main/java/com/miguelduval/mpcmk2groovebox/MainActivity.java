package com.miguelduval.mpcmk2groovebox;

import android.app.Activity;
import android.content.Intent;
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
    private static native String nativeAudioStart();
    private static native String nativeAudioStop();
    private static native String nativeAudioStatus();

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
        root.addView(padGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(audioControls, new LinearLayout.LayoutParams(
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
        } catch (IOException | IllegalArgumentException e) {
            status.setText(
                    "Sample file load failed: "
                            + e.getClass().getSimpleName()
                            + ": "
                            + e.getMessage()
                            + "\nPrevious sample, if any, was kept.");
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

        String[] expectedTexts = {
                "MPC Studio MkII Groovebox — Hardware Bring-Up",
                "Refresh MIDI Devices",
                "Connect MPC Studio MkII",
                "Load WAV Sample",
                "Start Sampler",
                "Pad 1 tuning: +0.00 st",
                "Pad 1 level: 100%",
                "Pad 1 pan: C",
                "Pad 1 sample layer: 1/8"
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
        if (layerDown == null || !layerDown.performClick()) {
            Log.e(TAG, "UI_INTERACTION_FAILED: could not click Layer -");
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
    }

    private void updateLayerStatus() {
        layerStatus.setText("Pad " + (selectedPad + 1)
                + " sample layer: " + (selectedLayer + 1) + "/8");
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
            String result = nativeAudioLoadSample(wavBytes);
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
