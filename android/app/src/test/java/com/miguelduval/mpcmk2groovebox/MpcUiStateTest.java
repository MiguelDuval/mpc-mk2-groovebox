package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcUiStateTest {
    @Test
    public void mainMixerStripsStartVisibleAndTrackFocused() {
        MpcUiState state = new MpcUiState();
        assertTrue(state.compactMixerVisible());
        assertFalse(state.compactMixerPadMode());
    }

    @Test
    public void compactMixerModeChangePreservesMusicalSelectionState() {
        MpcUiState state = new MpcUiState()
                .withSelectedSequence(2)
                .withSelectedTrack(3)
                .withSelectedPad(7);

        MpcUiState padMixer = state.withCompactMixerState(true, true);

        assertTrue(padMixer.compactMixerVisible());
        assertTrue(padMixer.compactMixerPadMode());
        assertTrue(padMixer.selectedSequence() == 2);
        assertTrue(padMixer.selectedTrack() == 3);
        assertTrue(padMixer.selectedPad() == 7);
    }
}
