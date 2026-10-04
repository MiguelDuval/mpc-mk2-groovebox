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
    public void shortcutCatalogContainsCanonicalMpcDestinations() {
        MpcModeRegistry.Entry[] entries = MpcModeRegistry.shortcutEntries();
        assertEquals(MpcUiState.Mode.BROWSER, entries[0].mode);
        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, entries[1].mode);
        assertEquals(MpcUiState.Mode.PAD_MIXER, entries[2].mode);
        assertEquals(MpcUiState.Mode.SOUNDS, entries[3].mode);
        assertEquals(MpcUiState.Mode.XYFX, entries[4].mode);
        assertFalse(entries[1].available);
        assertFalse(entries[3].available);
        assertFalse(entries[4].available);
    }

    @Test
    public void unavailableModesAreExplicitlyReserved() {
        assertTrue(MpcModeRegistry.menuEntry(5).available);
        assertEquals(MpcUiState.Mode.TRACK_EDIT, MpcModeRegistry.menuEntry(5).mode);
        assertFalse(MpcModeRegistry.menuEntry(8).available);
        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, MpcModeRegistry.menuEntry(8).mode);
        assertTrue(MpcModeRegistry.menuEntry(9).available);
        assertTrue(MpcModeRegistry.menuEntry(13).available);
        assertEquals(MpcUiState.Mode.ARRANGE, MpcModeRegistry.menuEntry(13).mode);
    }

    @Test
    public void defaultShortcutsAreFiveUsableContexts() {
        MpcUiState.Mode[] shortcuts = MpcModeRegistry.defaultShortcuts();
        assertEquals(5, shortcuts.length);
        assertEquals(MpcUiState.Mode.BROWSER, shortcuts[0]);
        assertEquals(MpcUiState.Mode.TRACK_VIEW, shortcuts[1]);
        assertEquals(MpcUiState.Mode.GRID, shortcuts[2]);
        assertEquals(MpcUiState.Mode.STEP, shortcuts[3]);
        assertEquals(MpcUiState.Mode.PAD_MIXER, shortcuts[4]);

        for (MpcUiState.Mode shortcut : shortcuts) {
            boolean available = false;
            for (MpcModeRegistry.Entry entry : MpcModeRegistry.menuEntries()) {
                if (entry.mode == shortcut) {
                    available = entry.available;
                    break;
                }
            }
            assertTrue("default shortcut must be available: " + shortcut, available);
        }
    }
}
