package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcBrowserFileTest {
    private MpcBrowserFile sample(String name) {
        return new MpcBrowserFile(
                "content://media/external/audio/media/7", name, "audio/wav", 48000L, 1200L);
    }

    @Test
    public void onlyWaveFilesAreEligibleForTheCurrentNativeLoader() {
        assertTrue(MpcBrowserFile.isWavName("Kick.WAV"));
        assertTrue(MpcBrowserFile.isWavName("  snare.wav  "));
        assertFalse(MpcBrowserFile.isWavName("loop.mp3"));
        assertFalse(MpcBrowserFile.isWavName("bass.flac"));
        assertFalse(MpcBrowserFile.isWavName("project.xpj"));
    }

    @Test
    public void localWavResultsRespondToSearchAndSupportedTypeFilters() {
        MpcBrowserFile kick = sample("Kick_Main.wav");
        assertTrue(kick.matchesBrowserQuery("PLACES", "INTERNAL", "ALL", ""));
        assertTrue(kick.matchesBrowserQuery("PLACES", "INTERNAL", "SAMPLES", "kick"));
        assertTrue(kick.matchesBrowserQuery("PLACES", "USER FOLDER", "ALL", "kick"));
        assertTrue(kick.matchesBrowserQuery("PLACES", "USER FOLDER", "SAMPLES", "MAIN"));
        assertFalse(kick.matchesBrowserQuery("PLACES", "USER FOLDER", "PROJECTS", ""));
        assertFalse(kick.matchesBrowserQuery("PLACES", "USER FOLDER", "ALL", "snare"));
        assertTrue(kick.matchesBrowserQuery("CONTENT", "SAMPLES", "SAMPLES", "MAIN"));
        assertTrue(kick.matchesBrowserQuery("CONTENT", "MY FILES", "ALL", "kick"));
        assertFalse(kick.matchesBrowserQuery("PLACES", "INTERNAL", "PROJECTS", ""));
        assertFalse(kick.matchesBrowserQuery("PLACES", "INTERNAL", "ALL", "snare"));
    }

    @Test
    public void unimplementedLocationsNeverPretendToHaveIndexedResults() {
        MpcBrowserFile kick = sample("Kick_Main.wav");
        assertFalse(kick.matchesBrowserQuery("PLACES", "MPC DOCUMENTS", "ALL", ""));
        assertFalse(kick.matchesBrowserQuery("PLACES", "CONNECTED STORAGE", "SAMPLES", ""));
        assertTrue(kick.matchesBrowserQuery("PLACES", "FAVORITE 1", "ALL", ""));
        assertTrue(kick.matchesBrowserQuery("PLACES", "FAVORITE 5", "SAMPLES", "kick"));
        assertFalse(kick.matchesBrowserQuery("PLACES", "FAVORITE 6", "ALL", ""));
        assertFalse(kick.matchesBrowserQuery("CONTENT", "DRUMS", "ALL", ""));
        assertFalse(kick.matchesBrowserQuery("CONTENT", "INSTRUMENTS", "SAMPLES", ""));
        assertFalse(kick.matchesBrowserQuery("CONTENT", "DEMOS", "ALL", ""));
        assertFalse(kick.matchesBrowserQuery("EXPANSIONS", "USER CONTENT", "ALL", ""));
    }
}
