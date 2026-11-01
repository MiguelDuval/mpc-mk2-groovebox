package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
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
    public void backRestoresPriorSubcontextAndFocusWithoutRevertingLiveMusicalState() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setSelectedSequence(2);
        navigation.setSelectedTrack(1);
        navigation.setSelectedPad(4);
        navigation.setSelectedLayer(3);
        navigation.setTransportState(true, true, true, false, false);
        navigation.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);
        navigation.setZoomFocus(MpcUiState.ZoomFocus.NONE);
        navigation.setActionAvailable(true);

        navigation.navigate(MpcUiState.Mode.BROWSER);
        navigation.setSubcontext(MpcUiState.Subcontext.BROWSER);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.BROWSER_ITEM);
        navigation.setSelectedTrack(2);
        navigation.setSelectedPad(5);
        navigation.setSelectedLayer(6);
        navigation.setTransportState(true, false, true, true, false);

        assertTrue(navigation.back());
        assertEquals(MpcUiState.Mode.MAIN, navigation.state().mode());
        assertEquals(MpcUiState.Subcontext.TRACK_SELECT, navigation.state().subcontext());
        assertEquals(MpcUiState.DataDialFocus.TRACK, navigation.state().dataDialFocus());
        assertEquals(MpcUiState.ZoomFocus.NONE, navigation.state().zoomFocus());

        // Navigation return restores presentation context but must not roll back
        // musical selection or transport changes that occurred while away.
        assertEquals(2, navigation.state().selectedSequence());
        assertEquals(2, navigation.state().selectedTrack());
        assertEquals(5, navigation.state().selectedPad());
        assertEquals(6, navigation.state().selectedLayer());
        assertTrue(navigation.state().playing());
        assertFalse(navigation.state().loopEnabled());
        assertTrue(navigation.state().timingCorrect());
        assertTrue(navigation.state().metronome());
        assertTrue(navigation.state().actionAvailable());
    }

    @Test
    public void nestedBackRestoresEachModeSpecificFocusFrame() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setSubcontext(MpcUiState.Subcontext.TRACK_SELECT);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.TRACK);

        navigation.navigate(MpcUiState.Mode.BROWSER);
        navigation.setSubcontext(MpcUiState.Subcontext.BROWSER);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.BROWSER_ITEM);

        navigation.navigate(MpcUiState.Mode.GRID);
        navigation.setSubcontext(MpcUiState.Subcontext.STEP_EDIT);
        navigation.setDataDialFocus(MpcUiState.DataDialFocus.STEP_DURATION);

        assertTrue(navigation.back());
        assertEquals(MpcUiState.Mode.BROWSER, navigation.state().mode());
        assertEquals(MpcUiState.Subcontext.BROWSER, navigation.state().subcontext());
        assertEquals(
                MpcUiState.DataDialFocus.BROWSER_ITEM,
                navigation.state().dataDialFocus());

        assertTrue(navigation.back());
        assertEquals(MpcUiState.Mode.MAIN, navigation.state().mode());
        assertEquals(MpcUiState.Subcontext.TRACK_SELECT, navigation.state().subcontext());
        assertEquals(MpcUiState.DataDialFocus.TRACK, navigation.state().dataDialFocus());
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

    @Test
    public void reservedModeCanBeAssignedToShortcutCatalogSlot() {
        MpcNavigationController navigation = new MpcNavigationController();
        navigation.setShortcut(0, MpcUiState.Mode.CHANNEL_MIXER);

        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, navigation.shortcut(0));
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
        assertEquals(MpcUiState.Mode.XYFX, navigation.shortcut(1));
        assertEquals(MpcUiState.Mode.CHANNEL_MIXER, navigation.shortcut(2));
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
    public void persistedShortcutOrderRoundTripsExactlyFiveSlots() {
        MpcUiState.Mode[] customized = {
                MpcUiState.Mode.GRID,
                MpcUiState.Mode.BROWSER,
                MpcUiState.Mode.NEXT_SEQUENCE,
                MpcUiState.Mode.CHANNEL_MIXER,
                MpcUiState.Mode.SAMPLE_EDIT
        };

        String encoded = MpcNavigationController.encodeShortcuts(customized);
        assertEquals("GRID,BROWSER,NEXT_SEQUENCE,CHANNEL_MIXER,SAMPLE_EDIT", encoded);
        assertArrayEquals(
                customized,
                MpcNavigationController.decodeShortcuts(encoded));
    }

    @Test
    public void controllerConstructorRestoresCustomSlotOrderBeforeShellBuild() {
        MpcUiState.Mode[] customized = {
                MpcUiState.Mode.GRID,
                MpcUiState.Mode.BROWSER,
                MpcUiState.Mode.NEXT_SEQUENCE,
                MpcUiState.Mode.CHANNEL_MIXER,
                MpcUiState.Mode.SAMPLE_EDIT
        };

        MpcNavigationController navigation =
                new MpcNavigationController(null, customized);

        assertArrayEquals(customized, navigation.shortcuts());
    }

    @Test
    public void missingOrCorruptShortcutPreferenceFallsBackToFactoryOrder() {
        MpcUiState.Mode[] defaults = MpcModeRegistry.defaultShortcuts();
        assertArrayEquals(defaults, MpcNavigationController.decodeShortcuts(null));
        assertArrayEquals(defaults, MpcNavigationController.decodeShortcuts(""));
        assertArrayEquals(defaults, MpcNavigationController.decodeShortcuts(
                "BROWSER,GRID"));
        assertArrayEquals(defaults, MpcNavigationController.decodeShortcuts(
                "BROWSER,GRID,NOT_A_MODE,STEP,MAIN"));
        assertArrayEquals(defaults, MpcNavigationController.decodeShortcuts(
                "BROWSER,GRID,RESERVED,STEP,MAIN"));
    }

    @Test
    public void invalidBulkShortcutUpdateDoesNotPartiallyReplaceCurrentSlots() {
        MpcNavigationController navigation = new MpcNavigationController();
        MpcUiState.Mode[] before = navigation.shortcuts();
        boolean rejected = false;
        try {
            navigation.setShortcuts(
                    MpcUiState.Mode.GRID,
                    MpcUiState.Mode.BROWSER,
                    MpcUiState.Mode.RESERVED,
                    MpcUiState.Mode.STEP,
                    MpcUiState.Mode.MAIN);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        assertTrue(rejected);
        assertArrayEquals(before, navigation.shortcuts());
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
