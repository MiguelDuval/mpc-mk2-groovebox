package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcBrowserFileTest {
    private MpcBrowserFile sample(String name) {
        return sample(name, "content://media/external/audio/media/7");
    }

    private MpcBrowserFile sample(String name, String uri) {
        return new MpcBrowserFile(uri, name, "audio/wav", 48000L, 1200L);
    }

    @Test
    public void resultsUseDeterministicCaseInsensitiveDisplayNameOrder() {
        MpcBrowserFile lowerKick = sample(
                "kick.wav", "content://media/external/audio/media/9");
        MpcBrowserFile upperKickSeven = sample(
                "Kick.wav", "content://media/external/audio/media/7");
        MpcBrowserFile bass = sample(
                "Bass.wav", "content://media/external/audio/media/4");
        MpcBrowserFile upperKickTwo = sample(
                "Kick.wav", "content://media/external/audio/media/2");
        MpcBrowserFile drum = sample(
                "drum.wav", "content://media/external/audio/media/1");

        List<MpcBrowserFile> original = new ArrayList<>(Arrays.asList(
                lowerKick, upperKickSeven, bass, upperKickTwo, drum));
        List<MpcBrowserFile> sorted = MpcBrowserFile.sortedByDisplayName(original);

        assertEquals(Arrays.asList(
                "Bass.wav", "drum.wav", "Kick.wav", "Kick.wav", "kick.wav"),
                Arrays.asList(
                        sorted.get(0).displayName(),
                        sorted.get(1).displayName(),
                        sorted.get(2).displayName(),
                        sorted.get(3).displayName(),
                        sorted.get(4).displayName()));
        assertEquals("content://media/external/audio/media/2", sorted.get(2).uri());
        assertEquals("content://media/external/audio/media/7", sorted.get(3).uri());

        // Sorting is a presentation projection and must not reorder provider-owned input.
        assertEquals(lowerKick, original.get(0));
        assertEquals(upperKickSeven, original.get(1));
        assertEquals(bass, original.get(2));
        assertEquals(upperKickTwo, original.get(3));
        assertEquals(drum, original.get(4));
    }

    @Test
    public void sortingNullOrEmptyCatalogProducesAnEmptyResult() {
        assertTrue(MpcBrowserFile.sortedByDisplayName(null).isEmpty());
        assertTrue(MpcBrowserFile.sortedByDisplayName(Collections.emptyList()).isEmpty());
    }

    @Test
    public void fileSizeOptionOnlyHidesSizeAndPreservesOtherMetadata() {
        MpcBrowserFile file = new MpcBrowserFile(
                "content://media/external/audio/media/12",
                "Kick.wav", "audio/wav", 1536L, 1200L);
        assertEquals("1.5 KB • 0:01 • WAV", file.metadataLine(true));
        assertEquals("0:01 • WAV", file.metadataLine(false));

        MpcBrowserFile unknown = new MpcBrowserFile(
                "content://media/external/audio/media/13",
                "Unknown.wav", "audio/wav", -1L, 0L);
        assertEquals("SIZE UNKNOWN • DURATION UNKNOWN • WAV", unknown.metadataLine(true));
        assertEquals("DURATION UNKNOWN • WAV", unknown.metadataLine(false));
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
