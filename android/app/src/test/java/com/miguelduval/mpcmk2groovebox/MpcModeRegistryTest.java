package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcModeRegistryTest {
    @Test
    public void menuHasSixteenStableSlots() {
        MpcModeRegistry.Entry[] entries = MpcModeRegistry.menuEntries();
        assertEquals(16, entries.length);
        assertEquals(MpcUiState.Mode.MAIN, entries[0].mode);
        assertEquals(MpcUiState.Mode.PROJECT, entries[15].mode);
    }

    @Test
    public void unavailableModesAreExplicitlyReserved() {
        assertFalse(MpcModeRegistry.menuEntry(5).available);
        assertEquals(MpcUiState.Mode.TRACK_EDIT, MpcModeRegistry.menuEntry(5).mode);
        assertFalse(MpcModeRegistry.menuEntry(8).available);
        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, MpcModeRegistry.menuEntry(8).mode);
        assertTrue(MpcModeRegistry.menuEntry(9).available);
    }

    @Test
    public void defaultShortcutsAreFivePromotedContexts() {
        MpcUiState.Mode[] shortcuts = MpcModeRegistry.defaultShortcuts();
        assertEquals(5, shortcuts.length);
        assertEquals(MpcUiState.Mode.MAIN, shortcuts[0]);
        assertEquals(MpcUiState.Mode.BROWSER, shortcuts[1]);
        assertEquals(MpcUiState.Mode.GRID, shortcuts[2]);
        assertEquals(MpcUiState.Mode.SAMPLER, shortcuts[3]);
        assertEquals(MpcUiState.Mode.PAD_MIXER, shortcuts[4]);
    }
}
