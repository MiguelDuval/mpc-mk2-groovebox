package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
import android.media.midi.MidiReceiver;
import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.util.Locale;

public final class AndroidMidiBridge {
    public interface Listener {
        void onDevicesChanged(String description);
        void onMidi(String description);
        void onConnection(String description);
    }

    private final MidiManager midiManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Listener listener;

    private MidiDevice device;
    private MidiDeviceInfo.PortInfo inputPortInfo;
    private MidiDeviceInfo.PortInfo outputPortInfo;
    private android.media.midi.MidiInputPort inputPort;
    private MidiOutputPort outputPort;

    private final MidiManager.DeviceCallback deviceCallback = new MidiManager.DeviceCallback() {
        @Override public void onDeviceAdded(MidiDeviceInfo info) { publishDeviceList(); }

        @Override public void onDeviceRemoved(MidiDeviceInfo info) {
            if (device != null && device.getInfo().getId() == info.getId()) disconnect();
            publishDeviceList();
        }

        @Override public void onDeviceStatusChanged(MidiDeviceInfo status) {
            publishDeviceList();
        }
    };

    private final MidiReceiver receiver = new MidiReceiver() {
        @Override public void onSend(byte[] data, int offset, int count, long timestamp) {
            if (count <= 0) return;

            byte[] message = new byte[count];
            System.arraycopy(data, offset, message, 0, count);

            nativeOnMidi(message, timestamp);
            listener.onMidi(toHex(message));
        }
    };

    public AndroidMidiBridge(Context context, Listener listener) {
        midiManager =
                (MidiManager) context.getApplicationContext()
                        .getSystemService(Context.MIDI_SERVICE);
        this.listener = listener;

        if (midiManager == null) {
            listener.onConnection("Android MIDI service unavailable");
            return;
        }

        midiManager.registerDeviceCallback(deviceCallback, mainHandler);
        publishDeviceList();
    }

    public String describeDevices() {
        if (midiManager == null) return "MIDI service unavailable";

        MidiDeviceInfo[] infos = midiManager.getDevices();
        if (infos.length == 0) return "No MIDI devices detected";

        StringBuilder result = new StringBuilder();
        for (MidiDeviceInfo info : infos) {
            result.append(describeDevice(info)).append('\n');

            for (MidiDeviceInfo.PortInfo port : info.getPorts()) {
                result.append("  - ")
                        .append(port.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT ? "IN " : "OUT ")
                        .append(port.getPortNumber())
                        .append(": ")
                        .append(port.getName())
                        .append('\n');
            }
        }

        return result.toString().trim();
    }

    public void connectPreferred() {
        if (midiManager == null) {
            listener.onConnection("Android MIDI service unavailable");
            return;
        }

        MidiDeviceInfo target = null;

        for (MidiDeviceInfo info : midiManager.getDevices()) {
            if (info.getInputPortCount() == 0 || info.getOutputPortCount() == 0) continue;

            String haystack = describeDevice(info).toLowerCase(Locale.ROOT);
            if (haystack.contains("mpc studio mk2")
                    || haystack.contains("mpc studio mk ii")) {
                target = info;
                break;
            }
        }

        if (target == null) {
            listener.onConnection(
                    "MPC Studio MkII not found. Connect the controller and refresh MIDI devices.");
            return;
        }

        disconnect();

        MidiDeviceInfo selected = target;
        midiManager.openDevice(selected, opened -> {
            if (opened == null) {
                listener.onConnection("Could not open " + describeDevice(selected));
                return;
            }

            device = opened;
            inputPortInfo = choosePort(
                    selected, MidiDeviceInfo.PortInfo.TYPE_INPUT, "public");
            outputPortInfo = choosePort(
                    selected, MidiDeviceInfo.PortInfo.TYPE_OUTPUT, "public");

            if (inputPortInfo == null || outputPortInfo == null) {
                listener.onConnection("Opened MkII, but no usable MIDI ports were found");
                disconnect();
                return;
            }

            inputPort = device.openInputPort(inputPortInfo.getPortNumber());
            outputPort = device.openOutputPort(outputPortInfo.getPortNumber());

            if (inputPort == null || outputPort == null) {
                listener.onConnection("MkII MIDI port open failed");
                disconnect();
                return;
            }

            try {
                outputPort.connect(receiver);
            } catch (RuntimeException e) {
                listener.onConnection("MIDI receive connection failed: " + e.getMessage());
                disconnect();
                return;
            }

            listener.onConnection(
                    "Connected: " + describeDevice(selected)
                            + " | IN=" + inputPortInfo.getName()
                            + " | OUT=" + outputPortInfo.getName());
        }, mainHandler);
    }

    public void send(byte[] message) {
        if (inputPort == null || message == null || message.length == 0) return;

        try {
            inputPort.send(message, 0, message.length);
        } catch (IOException e) {
            listener.onConnection("MIDI send failed: " + e.getMessage());
        }
    }

    public void testPadRed() {
        send(MpcStudioMk2MidiMessages.padRgb(0, 127, 0, 0));
    }

    public void testPadBlue() {
        send(MpcStudioMk2MidiMessages.padRgb(0, 0, 0, 127));
    }

    public void testPadOff() {
        send(MpcStudioMk2MidiMessages.padRgb(0, 0, 0, 0));
    }

    public void testPlayLed() {
        send(MpcStudioMk2MidiMessages.buttonLed(82, 2));
    }

    public void testTouchLed() {
        send(MpcStudioMk2MidiMessages.touchStripLed(57, 127));
    }

    public void disconnect() {
        if (outputPort != null) {
            try { outputPort.close(); } catch (IOException ignored) {}
            outputPort = null;
        }

        if (inputPort != null) {
            try { inputPort.close(); } catch (IOException ignored) {}
            inputPort = null;
        }

        if (device != null) {
            try { device.close(); } catch (IOException ignored) {}
            device = null;
        }

        inputPortInfo = null;
        outputPortInfo = null;
    }

    public void close() {
        disconnect();

        if (midiManager != null) {
            midiManager.unregisterDeviceCallback(deviceCallback);
        }
    }

    private void publishDeviceList() {
        listener.onDevicesChanged(describeDevices());
    }

    private static MidiDeviceInfo.PortInfo choosePort(
            MidiDeviceInfo info, int type, String preferredName) {
        MidiDeviceInfo.PortInfo fallback = null;

        for (MidiDeviceInfo.PortInfo port : info.getPorts()) {
            if (port.getType() != type) continue;

            if (fallback == null) fallback = port;

            String name = port.getName();
            if (name != null
                    && name.toLowerCase(Locale.ROOT).contains(preferredName)) {
                return port;
            }
        }

        return fallback;
    }

    private static String describeDevice(MidiDeviceInfo info) {
        android.os.Bundle props = info.getProperties();

        String manufacturer =
                props.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "");
        String product =
                props.getString(MidiDeviceInfo.PROPERTY_PRODUCT, "");
        String name =
                props.getString(MidiDeviceInfo.PROPERTY_NAME, "");

        return "id=" + info.getId()
                + " name=" + name
                + " manufacturer=" + manufacturer
                + " product=" + product
                + " in=" + info.getInputPortCount()
                + " out=" + info.getOutputPortCount();
    }

    private static String toHex(byte[] data) {
        StringBuilder builder = new StringBuilder(data.length * 3);

        for (byte value : data) {
            builder.append(String.format(Locale.ROOT, "%02X ", value & 0xFF));
        }

        return builder.toString().trim();
    }

    private static native void nativeOnMidi(byte[] data, long timestamp);
}
