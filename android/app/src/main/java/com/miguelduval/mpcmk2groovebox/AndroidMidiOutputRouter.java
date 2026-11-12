package com.miguelduval.mpcmk2groovebox;

import android.content.Context;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.os.Handler;
import android.os.Looper;
import java.io.IOException;
import java.util.Locale;

/** Owns the external MIDI output port used by MIDI tracks. */
public final class AndroidMidiOutputRouter implements AutoCloseable {
    public interface Listener {
        void onOutputDevicesChanged(String description);
        void onOutputConnection(String description);
    }

    private final MidiManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private MidiDevice device;
    private MidiInputPort port;
    private int selectedDeviceId = -1;
    private int selectedPortNumber = -1;
    private long generation;
    private boolean closed;

    private final MidiManager.DeviceCallback callback = new MidiManager.DeviceCallback() {
        @Override public void onDeviceAdded(MidiDeviceInfo info) {
            publishDevices();
            if (!closed && info.getId() == selectedDeviceId && port == null) {
                connect(selectedDeviceId, selectedPortNumber);
            }
        }
        @Override public void onDeviceRemoved(MidiDeviceInfo info) {
            if (info.getId() == selectedDeviceId) {
                disconnectInternal(false);
                notifyConnection("MIDI output device disconnected");
            }
            publishDevices();
        }
        @Override public void onDeviceStatusChanged(
                android.media.midi.MidiDeviceStatus status) {
            publishDevices();
        }
    };

    public AndroidMidiOutputRouter(Context context, Listener listener) {
        this.listener = listener;
        manager = (MidiManager) context.getApplicationContext()
                .getSystemService(Context.MIDI_SERVICE);
        if (manager == null) {
            notifyConnection("Android MIDI service unavailable");
        } else {
            manager.registerDeviceCallback(callback, handler);
            publishDevices();
        }
    }

    public boolean isConnected() { return port != null; }

    public void connectPreferred() {
        if (closed || manager == null) {
            notifyConnection("Android MIDI service unavailable");
            return;
        }
        for (MidiDeviceInfo info : manager.getDevices()) {
            if (isMpcStudio(info)) continue;
            for (MidiDeviceInfo.PortInfo p : info.getPorts()) {
                if (p.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT) {
                    connect(info.getId(), p.getPortNumber());
                    return;
                }
            }
        }
        notifyConnection("No MIDI output device available");
    }

    public void connect(int deviceId, int portNumber) {
        if (closed || manager == null) {
            notifyConnection("Android MIDI service unavailable");
            return;
        }
        MidiDeviceInfo info = findDevice(deviceId);
        if (info == null || !hasInputPort(info, portNumber)) {
            notifyConnection("Selected MIDI output port is unavailable");
            return;
        }
        disconnectInternal(false);
        selectedDeviceId = deviceId;
        selectedPortNumber = portNumber;
        final long request = ++generation;
        notifyConnection("Opening MIDI output: " + describe(info));
        manager.openDevice(info, opened -> {
            if (request != generation || closed) {
                closeQuietly(opened);
                return;
            }
            if (opened == null) {
                notifyConnection("Unable to open MIDI output device");
                return;
            }
            MidiInputPort openedPort = opened.openInputPort(portNumber);
            if (openedPort == null) {
                closeQuietly(opened);
                notifyConnection("Unable to open MIDI input port " + portNumber);
                return;
            }
            device = opened;
            port = openedPort;
            notifyConnection("MIDI output connected: " + describe(info)
                    + " port=" + portNumber);
        }, handler);
    }

    public void disconnect() { disconnectInternal(true); }

    private void disconnectInternal(boolean clearSelection) {
        generation++;
        if (port != null) {
            try { port.close(); } catch (IOException ignored) {}
            port = null;
        }
        closeQuietly(device);
        device = null;
        if (clearSelection) {
            selectedDeviceId = -1;
            selectedPortNumber = -1;
        }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        disconnectInternal(true);
        if (manager != null) manager.unregisterDeviceCallback(callback);
    }

    public void sendMidiNote(int trackIndex, int note, int velocity, boolean noteOn) {
        MidiInputPort target = port;
        if (target == null || note < 0 || note > 127
                || velocity < 0 || velocity > 127) return;
        int channel = Math.floorMod(trackIndex, 16);
        byte[] message = new byte[] {
                (byte) ((noteOn ? 0x90 : 0x80) | channel),
                (byte) note, (byte) (noteOn ? velocity : 0)
        };
        try { target.send(message, 0, message.length); }
        catch (IOException e) {
            notifyConnection("MIDI output send failed: " + e.getMessage());
        }
    }

    public void stopAllMidiNotes() {
        MidiInputPort target = port;
        if (target == null) return;
        for (int channel = 0; channel < 16; channel++) {
            sendControlChange(target, channel, 64, 0);
            sendControlChange(target, channel, 123, 0);
        }
    }

    private static void sendControlChange(
            MidiInputPort target, int channel, int controller, int value) {
        byte[] message = new byte[] {
                (byte) (0xB0 | channel), (byte) controller, (byte) value
        };
        try { target.send(message, 0, message.length); }
        catch (IOException ignored) {}
    }

    private MidiDeviceInfo findDevice(int id) {
        if (manager == null) return null;
        for (MidiDeviceInfo info : manager.getDevices()) {
            if (info.getId() == id) return info;
        }
        return null;
    }

    private static boolean hasInputPort(MidiDeviceInfo info, int number) {
        for (MidiDeviceInfo.PortInfo p : info.getPorts()) {
            if (p.getType() == MidiDeviceInfo.PortInfo.TYPE_INPUT
                    && p.getPortNumber() == number) return true;
        }
        return false;
    }

    private void publishDevices() {
        if (listener != null) listener.onOutputDevicesChanged(describeDevices());
    }

    private String describeDevices() {
        if (manager == null) return "MIDI service unavailable";
        MidiDeviceInfo[] infos = manager.getDevices();
        if (infos.length == 0) return "No MIDI devices detected";
        StringBuilder result = new StringBuilder();
        for (MidiDeviceInfo info : infos) {
            if (result.length() > 0) result.append("; ");
            result.append(describe(info));
        }
        return result.toString();
    }

    private void notifyConnection(String text) {
        if (listener != null) listener.onOutputConnection(text);
    }

    private static void closeQuietly(MidiDevice value) {
        if (value == null) return;
        try { value.close(); } catch (IOException ignored) {}
    }

    private static boolean isMpcStudio(MidiDeviceInfo info) {
        android.os.Bundle p = info.getProperties();
        String manufacturer = p.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "");
        String product = p.getString(MidiDeviceInfo.PROPERTY_PRODUCT, "");
        String name = p.getString(MidiDeviceInfo.PROPERTY_NAME, "");
        return manufacturer.toLowerCase(Locale.ROOT).contains("akai professional")
                && (product + " " + name).toLowerCase(Locale.ROOT).contains("mpc studio");
    }

    private static String describe(MidiDeviceInfo info) {
        android.os.Bundle p = info.getProperties();
        return "id=" + info.getId()
                + " name=" + p.getString(MidiDeviceInfo.PROPERTY_NAME, "")
                + " manufacturer=" + p.getString(MidiDeviceInfo.PROPERTY_MANUFACTURER, "")
                + " product=" + p.getString(MidiDeviceInfo.PROPERTY_PRODUCT, "")
                + " inputs=" + info.getInputPortCount()
                + " outputs=" + info.getOutputPortCount();
    }
}
