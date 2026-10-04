package com.miguelduval.mpcmk2groovebox;

/**
 * Immutable presentation/navigation snapshot for the MPC 3.9-style shell.
 *
 * This is deliberately Android-free beyond being plain Java data. It is not
 * the musical domain model and it never owns audio/realtime objects.
 */
final class MpcUiState {
    enum Mode {
        MAIN("MAIN"),
        TRACK_VIEW("TRACK VIEW"),
        BROWSER("BROWSER"),
        GRID("GRID"),
        STEP("STEP"),
        TRACK_EDIT("TRACK EDIT"),
        SAMPLE_EDIT("SAMPLE EDIT"),
        SAMPLER("SAMPLER"),
        CHANNEL_MIXER("CHANNEL MIXER"),
        PAD_MIXER("PAD MIXER"),
        LEVELS_16("16 LEVELS"),
        PAD_PERFORM("PAD PERFORM"),
        NEXT_SEQUENCE("NEXT SEQUENCE"),
        ARRANGE("ARRANGE"),
        LIST_EDIT("LIST EDIT"),
        PROJECT("PROJECT"),
        MENU("MENU"),
        PREFERENCES("PREFERENCES"),
        MIDI_CONTROL("MIDI / CONTROL"),
        LOOPER("LOOPER"),
        XYFX("XYFX"),
        SOUNDS("SOUNDS"),
        RESERVED("RESERVED");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    enum Subcontext {
        NONE,
        TRACK_SELECT,
        SEQUENCE_SELECT,
        PROGRAM_SELECT,
        TRACK_TYPE_SELECT,
        SAMPLE_SELECT,
        SAMPLE_START,
        SAMPLE_END,
        SEQUENCE_START,
        SEQUENCE_END,
        TUNE,
        STEP_EDIT,
        BROWSER,
        PERFORMANCE,
        SHORTCUT_CONFIG
    }

    enum DataDialFocus {
        NONE,
        SEQUENCE,
        SEQUENCE_START,
        SEQUENCE_END,
        SEQUENCE_BPM,
        SEQUENCE_BARS,
        TRACK,
        PROGRAM,
        TRACK_TYPE,
        PAD,
        SAMPLE_LAYER,
        SAMPLE_START,
        SAMPLE_END,
        TUNE,
        STEP,
        STEP_VELOCITY,
        STEP_PROBABILITY,
        STEP_RATCHET,
        STEP_NUDGE,
        STEP_DURATION,
        BROWSER_ITEM,
        SHORTCUT,
        ZOOM_HORIZONTAL,
        ZOOM_VERTICAL,
        TIMELINE
    }

    enum ZoomFocus {
        NONE,
        HORIZONTAL,
        VERTICAL,
        TIMELINE
    }

    enum EditorTool {
        DRAW,
        ERASE,
        SELECT,
        MAGNIFY
    }

    private final Mode mode;
    private final Subcontext subcontext;
    private final int selectedSequence;
    private final int selectedTrack;
    private final String selectedProgram;
    private final int selectedPad;
    private final int selectedLayer;
    private final int padBank;
    private final DataDialFocus dataDialFocus;
    private final ZoomFocus zoomFocus;
    private final boolean playing;
    private final boolean loopEnabled;
    private final boolean timingCorrect;
    private final boolean metronome;
    private final boolean recordArmed;
    private final boolean muted;
    private final boolean soloed;
    private final String browserLocation;
    private final String browserFilter;
    private final String browserSearch;
    private final EditorTool editorTool;
    private final boolean shiftActive;
    private final boolean alternateActive;
    private final boolean actionAvailable;
    private final boolean compactMixerVisible;
    private final boolean compactMixerPadMode;

    MpcUiState() {
        this(
                Mode.MAIN,
                Subcontext.NONE,
                0,
                0,
                null,
                0,
                0,
                0,
                DataDialFocus.NONE,
                ZoomFocus.NONE,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                "",
                "",
                "",
                EditorTool.DRAW,
                false,
                false,
                true,
                false,
                false);
    }

    private MpcUiState(
            Mode mode,
            Subcontext subcontext,
            int selectedSequence,
            int selectedTrack,
            String selectedProgram,
            int selectedPad,
            int selectedLayer,
            int padBank,
            DataDialFocus dataDialFocus,
            ZoomFocus zoomFocus,
            boolean playing,
            boolean loopEnabled,
            boolean timingCorrect,
            boolean metronome,
            boolean recordArmed,
            boolean muted,
            boolean soloed,
            String browserLocation,
            String browserFilter,
            String browserSearch,
            EditorTool editorTool,
            boolean shiftActive,
            boolean alternateActive,
            boolean actionAvailable,
            boolean compactMixerVisible,
            boolean compactMixerPadMode) {
        this.mode = mode;
        this.subcontext = subcontext;
        this.selectedSequence = Math.max(0, selectedSequence);
        this.selectedTrack = Math.max(0, selectedTrack);
        this.selectedProgram = selectedProgram;
        this.selectedPad = clamp(selectedPad, 0, 15);
        this.selectedLayer = clamp(selectedLayer, 0, 7);
        this.padBank = clamp(padBank, 0, 7);
        this.dataDialFocus = dataDialFocus;
        this.zoomFocus = zoomFocus;
        this.playing = playing;
        this.loopEnabled = loopEnabled;
        this.timingCorrect = timingCorrect;
        this.metronome = metronome;
        this.recordArmed = recordArmed;
        this.muted = muted;
        this.soloed = soloed;
        this.browserLocation = safe(browserLocation);
        this.browserFilter = safe(browserFilter);
        this.browserSearch = safe(browserSearch);
        this.editorTool = editorTool;
        this.shiftActive = shiftActive;
        this.alternateActive = alternateActive;
        this.actionAvailable = actionAvailable;
        this.compactMixerVisible = compactMixerVisible;
        this.compactMixerPadMode = compactMixerPadMode;
    }

    static MpcUiState legacyPage(String page) {
        MpcUiState state = new MpcUiState();
        if (page == null) return state;
        switch (page) {
            case "MAIN":
                return state.withMode(Mode.MAIN);
            case "BROWSE":
                return state.withMode(Mode.BROWSER);
            case "SAMPLE":
                return state.withMode(Mode.SAMPLE_EDIT);
            case "REC":
                return state.withMode(Mode.SAMPLER);
            case "SEQ":
                return state.withMode(Mode.TRACK_VIEW);
            case "MIX":
                return state.withMode(Mode.PAD_MIXER);
            case "MENU":
                return state.withMode(Mode.MENU);
            default:
                return state;
        }
    }

    Mode mode() { return mode; }
    Subcontext subcontext() { return subcontext; }
    int selectedSequence() { return selectedSequence; }
    int selectedTrack() { return selectedTrack; }
    String selectedProgram() { return selectedProgram; }
    int selectedPad() { return selectedPad; }
    int selectedLayer() { return selectedLayer; }
    int padBank() { return padBank; }
    DataDialFocus dataDialFocus() { return dataDialFocus; }
    ZoomFocus zoomFocus() { return zoomFocus; }
    boolean playing() { return playing; }
    boolean loopEnabled() { return loopEnabled; }
    boolean timingCorrect() { return timingCorrect; }
    boolean metronome() { return metronome; }
    boolean recordArmed() { return recordArmed; }
    boolean muted() { return muted; }
    boolean soloed() { return soloed; }
    String browserLocation() { return browserLocation; }
    String browserFilter() { return browserFilter; }
    String browserSearch() { return browserSearch; }
    EditorTool editorTool() { return editorTool; }
    boolean shiftActive() { return shiftActive; }
    boolean alternateActive() { return alternateActive; }
    boolean actionAvailable() { return actionAvailable; }
    boolean compactMixerVisible() { return compactMixerVisible; }
    boolean compactMixerPadMode() { return compactMixerPadMode; }

    MpcUiState withMode(Mode value) {
        return copy(value, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable, compactMixerVisible, compactMixerPadMode);
    }

    MpcUiState withSubcontext(Subcontext value) {
        return copy(mode, value, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withDataDialFocus(DataDialFocus value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, value, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withZoomFocus(ZoomFocus value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, value, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withSelectedSequence(int value) {
        return copy(mode, subcontext, value, selectedTrack, selectedProgram, selectedPad,
                selectedLayer, padBank, dataDialFocus, zoomFocus, playing, loopEnabled,
                timingCorrect, metronome, recordArmed, muted, soloed, browserLocation,
                browserFilter, browserSearch, editorTool, shiftActive, alternateActive,
                actionAvailable);
    }

    MpcUiState withSelectedTrack(int value) {
        return copy(mode, subcontext, selectedSequence, Math.max(0, value),
                selectedProgram, selectedPad, selectedLayer, padBank, dataDialFocus,
                zoomFocus, playing, loopEnabled, timingCorrect, metronome, recordArmed,
                muted, soloed, browserLocation, browserFilter, browserSearch, editorTool,
                shiftActive, alternateActive, actionAvailable);
    }

    MpcUiState withSelectedProgram(String value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, value, selectedPad,
                selectedLayer, padBank, dataDialFocus, zoomFocus, playing, loopEnabled,
                timingCorrect, metronome, recordArmed, muted, soloed, browserLocation,
                browserFilter, browserSearch, editorTool, shiftActive, alternateActive,
                actionAvailable);
    }

    MpcUiState withSelectedPad(int value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                value, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withSelectedLayer(int value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, value, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withPadBank(int value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, value, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withTransport(
            boolean playing,
            boolean loopEnabled,
            boolean timingCorrect,
            boolean metronome,
            boolean recordArmed) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withTrackState(boolean muted, boolean soloed, boolean recordArmed) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withBrowser(String location, String filter, String search) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                location, filter, search, editorTool, shiftActive, alternateActive,
                actionAvailable);
    }

    MpcUiState withEditorTool(EditorTool value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, value, shiftActive,
                alternateActive, actionAvailable);
    }

    MpcUiState withModifiers(boolean shift, boolean alternate) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shift,
                alternate, actionAvailable);
    }

    MpcUiState withActionAvailable(boolean value) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, value, compactMixerVisible, compactMixerPadMode);
    }

    MpcUiState withCompactMixerState(boolean visible, boolean padMode) {
        return copy(mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable, visible, padMode);
    }

    private MpcUiState copy(
            Mode mode,
            Subcontext subcontext,
            int selectedSequence,
            int selectedTrack,
            String selectedProgram,
            int selectedPad,
            int selectedLayer,
            int padBank,
            DataDialFocus dataDialFocus,
            ZoomFocus zoomFocus,
            boolean playing,
            boolean loopEnabled,
            boolean timingCorrect,
            boolean metronome,
            boolean recordArmed,
            boolean muted,
            boolean soloed,
            String browserLocation,
            String browserFilter,
            String browserSearch,
            EditorTool editorTool,
            boolean shiftActive,
            boolean alternateActive,
            boolean actionAvailable,
            boolean compactMixerVisible,
            boolean compactMixerPadMode) {
        return new MpcUiState(
                mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable, compactMixerVisible, compactMixerPadMode);
    }

    private MpcUiState copy(
            Mode mode,
            Subcontext subcontext,
            int selectedSequence,
            int selectedTrack,
            String selectedProgram,
            int selectedPad,
            int selectedLayer,
            int padBank,
            DataDialFocus dataDialFocus,
            ZoomFocus zoomFocus,
            boolean playing,
            boolean loopEnabled,
            boolean timingCorrect,
            boolean metronome,
            boolean recordArmed,
            boolean muted,
            boolean soloed,
            String browserLocation,
            String browserFilter,
            String browserSearch,
            EditorTool editorTool,
            boolean shiftActive,
            boolean alternateActive,
            boolean actionAvailable) {
        return copy(
                mode, subcontext, selectedSequence, selectedTrack, selectedProgram,
                selectedPad, selectedLayer, padBank, dataDialFocus, zoomFocus, playing,
                loopEnabled, timingCorrect, metronome, recordArmed, muted, soloed,
                browserLocation, browserFilter, browserSearch, editorTool, shiftActive,
                alternateActive, actionAvailable, compactMixerVisible, compactMixerPadMode);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
