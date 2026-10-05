package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpcNavigationControllerTest {
    @Test
    public void defaultShellHasExactlyFiveShortcuts() {
        MpcNavigationController navigation = new MpcNavigationController();
        assertEquals(5, navigation.shortcuts().length);
        assertEquals(MpcUiState.Mode.BROWSER, navigation.shortcut(0));
        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, navigation.shortcut(1));
        assertEquals(MpcUiState.Mode.PAD_MIXER, navigation.shortcut(2));
        assertEquals(MpcUiState.Mode.SOUNDS, navigation.shortcut(3));
        assertEquals(MpcUiState.Mode.XYFX, navigation.shortcut(4));
    }

    @Test
    public void navigationTracksModeWithoutOwningTransport() {
        MpcNavigationController navigation = new MpcNavigationController();
        assertEquals(MpcUiState.Mode.MAIN, navigation.state().mode());

        assertTrue(navigation.navigate(MpcUiState.Mode.GRID));
        assertEquals(MpcUiState.Mode.GRID, navigation.state().mode());
        assertEquals(1, navigation.historyDepth());

        assertTrue(navigation.back());
        assertEquals(MpcUiState.Mode.MAIN, navigation.state().mode());
        assertEquals(0, navigation.historyDepth());
    }

    @Test
    public void contextAndDialFocusAreIndependent() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.navigate(MpcUiState.Mode.STEP);
        navigation.setSubcontext(MpcUiState.Subcontext.STEP_EDIT);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.STEP_DURATION);

        assertEquals(MpcUiState.Subcontext.STEP_EDIT, navigation.state().subcontext());
        assertEquals(
                MpcUiState.DataDialFocus.STEP_DURATION,
                navigation.state().dataDialFocus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reservedModeCannotBePromotedToShortcut() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setShortcut(0, MpcUiState.Mode.CHANNEL_MIXER);
    }

    @Test
    public void individualShortcutCanBeAssignedToAnImplementedMode() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setShortcut(0, MpcUiState.Mode.ARRANGE);

        assertEquals(MpcUiState.Mode.ARRANGE, navigation.shortcut(0));
        assertEquals(5, navigation.shortcuts().length);
    }

    @Test
    public void shortcutsCanBeReorderedWithoutChangingSlotCount() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.moveShortcut(4, 1);

        assertEquals(5, navigation.shortcuts().length);
        assertEquals(MpcUiState.Mode.PAD_MIXER, navigation.shortcut(1));
        assertEquals(MpcUiState.Mode.BROWSER, navigation.shortcut(2));
    }

    @Test
    public void shortcutsAreConfigurableButRemainFiveSlots() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setShortcuts(
                MpcUiState.Mode.MAIN,
                MpcUiState.Mode.TRACK_VIEW,
                MpcUiState.Mode.GRID,
                MpcUiState.Mode.STEP,
                MpcUiState.Mode.NEXT_SEQUENCE);

        assertEquals(MpcUiState.Mode.TRACK_VIEW, navigation.shortcut(1));
        assertEquals(MpcUiState.Mode.NEXT_SEQUENCE, navigation.shortcut(4));
    }

    @Test
    public void legacyPageMappingMatchesMigrationSurface() {
        assertEquals(MpcUiState.Mode.MAIN, MpcUiState.legacyPage("MAIN").mode());
        assertEquals(MpcUiState.Mode.BROWSER, MpcUiState.legacyPage("BROWSE").mode());
        assertEquals(MpcUiState.Mode.SAMPLE_EDIT, MpcUiState.legacyPage("SAMPLE").mode());
        assertEquals(MpcUiState.Mode.SAMPLER, MpcUiState.legacyPage("REC").mode());
        assertEquals(MpcUiState.Mode.TRACK_VIEW, MpcUiState.legacyPage("SEQ").mode());
        assertEquals(MpcUiState.Mode.PAD_MIXER, MpcUiState.legacyPage("MIX").mode());
    }

    @Test
    public void reservedModeIsMarkedUnavailable() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.navigate(MpcUiState.Mode.RESERVED);

        assertFalse(navigation.state().actionAvailable());
        assertEquals(MpcUiState.Mode.RESERVED, navigation.state().mode());
    }
}
