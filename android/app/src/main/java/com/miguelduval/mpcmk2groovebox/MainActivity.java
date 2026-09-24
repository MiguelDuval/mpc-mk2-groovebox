package com.miguelduval.mpcmk2groovebox;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity implements AndroidMidiBridge.Listener {
    static {
        System.loadLibrary("mpcgroovebox");
    }

    private AndroidMidiBridge midiBridge;
    private TextView status;
    private TextView devices;
    private TextView midiLog;

    private static native String nativeEngineInfo();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);

        TextView title = new TextView(this);
        title.setText("MPC Studio MkII Groovebox — Hardware Bring-Up");
        title.setTextSize(20.0f);
        title.setGravity(Gravity.CENTER_VERTICAL);

        status = new TextView(this);
        status.setText("Native: " + nativeEngineInfo());
        status.setTextSize(14.0f);

        devices = new TextView(this);
        devices.setText("Scanning MIDI devices...");
        devices.setTextSize(13.0f);

        Button scan = new Button(this);
        scan.setText("Refresh MIDI Devices");
        scan.setOnClickListener(v -> devices.setText(midiBridge.describeDevices()));

        Button connect = new Button(this);
        connect.setText("Connect MPC Studio MkII");
        connect.setOnClickListener(v -> midiBridge.connectPreferred());

        midiLog = new TextView(this);
        midiLog.setText("MIDI IN:\n");
        midiLog.setTextSize(13.0f);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(midiLog);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(status, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scan, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(connect, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(devices, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f));

        setContentView(root);
        midiBridge = new AndroidMidiBridge(this, this);
    }

    @Override
    protected void onDestroy() {
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
}
