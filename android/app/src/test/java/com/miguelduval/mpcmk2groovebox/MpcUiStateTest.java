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
        assertFalse(state.compactMixerPadMode());
    }
}
