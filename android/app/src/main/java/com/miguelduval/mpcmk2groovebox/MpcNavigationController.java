package com.miguelduval.mpcmk2groovebox;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Objects;

/**
 * UI-only navigation state machine. It does not know Android views, audio, MIDI
 * or native engine objects.
 */
final class MpcNavigationController {
    interface Listener {
        void onNavigationStateChanged(MpcUiState state);
    }

    static final int SHORTCUT_COUNT = 5;

    private MpcUiState state = new MpcUiState();
    private final Deque<MpcUiState.Mode> history = new ArrayDeque<>();
    private final MpcUiState.Mode[] shortcuts = new MpcUiState.Mode[SHORTCUT_COUNT];
    private final Listener listener;

    MpcNavigationController() {
        this(null);
    }

    MpcNavigationController(Listener listener) {
        this.listener = listener;
        MpcUiState.Mode[] defaults = MpcModeRegistry.defaultShortcuts();
        System.arraycopy(defaults, 0, shortcuts, 0, SHORTCUT_COUNT);
    }

    MpcUiState state() {
        return state;
    }

    MpcUiState.Mode shortcut(int slot) {
        if (slot < 0 || slot >= SHORTCUT_COUNT) {
            throw new IllegalArgumentException("Shortcut slot out of range: " + slot);
        }
        return shortcuts[slot];
    }

    MpcUiState.Mode[] shortcuts() {
        return Arrays.copyOf(shortcuts, shortcuts.length);
    }

    void setShortcut(int slot, MpcUiState.Mode mode) {
        if (slot < 0 || slot >= SHORTCUT_COUNT) {
            throw new IllegalArgumentException("Shortcut slot out of range: " + slot);
        }
        final MpcUiState.Mode value = Objects.requireNonNull(
                mode, "mode");
        if (!isPromotable(value)) {
            throw new IllegalArgumentException(
                    "Shortcut mode is not implemented: " + value);
        }
        shortcuts[slot] = value;
        notifyListener();
    }

    void moveShortcut(int from, int to) {
        if (from < 0 || from >= SHORTCUT_COUNT
                || to < 0 || to >= SHORTCUT_COUNT) {
            throw new IllegalArgumentException("Shortcut slot out of range");
        }
        if (from == to) return;
        final MpcUiState.Mode moved = shortcuts[from];
        if (from < to) {
            System.arraycopy(
                    shortcuts, from + 1, shortcuts, from, to - from);
        } else {
            System.arraycopy(
                    shortcuts, to, shortcuts, to + 1, from - to);
        }
        shortcuts[to] = moved;
        notifyListener();
    }

    void setShortcuts(MpcUiState.Mode... modes) {
        if (modes == null || modes.length != SHORTCUT_COUNT) {
            throw new IllegalArgumentException("Exactly five shortcuts are required");
        }
        for (int i = 0; i < SHORTCUT_COUNT; i++) {
            final MpcUiState.Mode value = Objects.requireNonNull(
                    modes[i], "shortcut[" + i + "]");
            if (!isPromotable(value)) {
                throw new IllegalArgumentException(
                        "Shortcut mode is not implemented: " + value);
            }
            shortcuts[i] = value;
        }
        notifyListener();
    }

    boolean navigate(MpcUiState.Mode mode) {
        Objects.requireNonNull(mode, "mode");
        if (state.mode() == mode) {
            notifyListener();
            return false;
        }
        history.push(state.mode());
        state = state.withMode(mode).withSubcontext(MpcUiState.Subcontext.NONE)
                .withActionAvailable(mode != MpcUiState.Mode.RESERVED);
        notifyListener();
        return true;
    }

    boolean back() {
        if (history.isEmpty()) {
            return false;
        }
        state = state.withMode(history.pop());
        notifyListener();
        return true;
    }

    void setSubcontext(MpcUiState.Subcontext subcontext) {
        state = state.withSubcontext(Objects.requireNonNull(subcontext));
        notifyListener();
    }

    void setDataDialFocus(MpcUiState.DataDialFocus focus) {
        state = state.withDataDialFocus(Objects.requireNonNull(focus));
        notifyListener();
    }

    void setZoomFocus(MpcUiState.ZoomFocus focus) {
        state = state.withZoomFocus(Objects.requireNonNull(focus));
        notifyListener();
    }

    void setSelectedSequence(int index) {
        state = state.withSelectedSequence(index);
        notifyListener();
    }

    void setSelectedTrack(int index) {
        state = state.withSelectedTrack(index);
        notifyListener();
    }

    void setSelectedProgram(String program) {
        state = state.withSelectedProgram(program);
        notifyListener();
    }

    void setSelectedPad(int pad) {
        state = state.withSelectedPad(pad);
        notifyListener();
    }

    void setSelectedLayer(int layer) {
        state = state.withSelectedLayer(layer);
        notifyListener();
    }

    void setPadBank(int bank) {
        state = state.withPadBank(bank);
        notifyListener();
    }

    void setTransportState(
            boolean playing,
            boolean loopEnabled,
            boolean timingCorrect,
            boolean metronome,
            boolean recordArmed) {
        state = state.withTransport(
                playing, loopEnabled, timingCorrect, metronome, recordArmed);
        notifyListener();
    }

    void setTrackState(boolean muted, boolean soloed, boolean recordArmed) {
        state = state.withTrackState(muted, soloed, recordArmed);
        notifyListener();
    }

    void setBrowser(String location, String filter, String search) {
        state = state.withBrowser(location, filter, search);
        notifyListener();
    }

    void setEditorTool(MpcUiState.EditorTool tool) {
        state = state.withEditorTool(tool);
        notifyListener();
    }

    void setModifiers(boolean shift, boolean alternate) {
        state = state.withModifiers(shift, alternate);
        notifyListener();
    }

    void setActionAvailable(boolean available) {
        state = state.withActionAvailable(available);
        notifyListener();
    }
\n    void setCompactMixerState(boolean visible, boolean padMode) {\n        state = state.withCompactMixerState(visible, padMode);\n        notifyListener();\n    }\n
    private boolean isPromotable(MpcUiState.Mode mode) {
        for (MpcModeRegistry.Entry entry : MpcModeRegistry.menuEntries()) {
            if (entry.mode == mode) {
                return entry.available;
            }
        }
        return false;
    }

    void clearHistory() {
        history.clear();
    }

    int historyDepth() {
        return history.size();
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onNavigationStateChanged(state);
        }
    }
}
