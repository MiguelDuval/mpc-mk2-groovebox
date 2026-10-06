package com.miguelduval.mpcmk2groovebox;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Canonical MPC mode vocabulary used by Menu and shortcuts.
 *
 * This is presentation/navigation metadata only. Availability describes
 * whether the current product has a truthful implementation for the context.
 */
final class MpcModeRegistry {
    static final class Entry {
        final MpcUiState.Mode mode;
        final String label;
        final boolean available;

        Entry(MpcUiState.Mode mode, String label, boolean available) {
            this.mode = mode;
            this.label = label;
            this.available = available;
        }
    }

    private static final Entry[] MENU_GRID = {
            new Entry(MpcUiState.Mode.MAIN, "MAIN", true),
            new Entry(MpcUiState.Mode.TRACK_VIEW, "TRACK VIEW", true),
            new Entry(MpcUiState.Mode.BROWSER, "BROWSER", true),
            new Entry(MpcUiState.Mode.GRID, "GRID", true),

            new Entry(MpcUiState.Mode.STEP, "STEP", true),
            new Entry(MpcUiState.Mode.TRACK_EDIT, "TRACK EDIT", true),
            new Entry(MpcUiState.Mode.SAMPLE_EDIT, "SAMPLE EDIT", true),
            new Entry(MpcUiState.Mode.SAMPLER, "SAMPLER", true),

            new Entry(MpcUiState.Mode.CHANNEL_MIXER, "CHANNEL MIXER", false),
            new Entry(MpcUiState.Mode.PAD_MIXER, "PAD MIXER", true),
            new Entry(MpcUiState.Mode.LEVELS_16, "16 LEVELS", false),
            new Entry(MpcUiState.Mode.PAD_PERFORM, "PAD PERFORM", false),

            new Entry(MpcUiState.Mode.NEXT_SEQUENCE, "NEXT SEQUENCE", true),
            new Entry(MpcUiState.Mode.ARRANGE, "ARRANGE", true),
            new Entry(MpcUiState.Mode.LIST_EDIT, "LIST EDIT", false),
            new Entry(MpcUiState.Mode.PROJECT, "PROJECT", false)
    };

    /*
     * MPC 3.x factory shortcut order is a product-level shell contract:
     * Browser / Channel Mixer / Pad Mixer / Sounds / XY.
     *
     * Availability is independent from shortcut identity. A factory shortcut
     * may currently resolve to a truthful RESERVED/UNAVAILABLE context when
     * our backend is not ready; it must never be silently replaced by a
     * different mode just to make the slot executable.
     */
    private static final MpcUiState.Mode[] DEFAULT_SHORTCUTS = {
            MpcUiState.Mode.BROWSER,
            MpcUiState.Mode.CHANNEL_MIXER,
            MpcUiState.Mode.PAD_MIXER,
            MpcUiState.Mode.SOUNDS,
            MpcUiState.Mode.XYFX
    };

    private MpcModeRegistry() {}

    static Entry menuEntry(int index) {
        return MENU_GRID[Math.max(0, Math.min(MENU_GRID.length - 1, index))];
    }

    static Entry[] menuEntries() {
        return Arrays.copyOf(MENU_GRID, MENU_GRID.length);
    }

    static Entry[] shortcutEntries() {
        final Entry[] promoted = {
                new Entry(MpcUiState.Mode.BROWSER, "BROWSER", true),
                new Entry(MpcUiState.Mode.CHANNEL_MIXER, "CHANNEL MIXER", false),
                new Entry(MpcUiState.Mode.PAD_MIXER, "PAD MIXER", true),
                new Entry(MpcUiState.Mode.SOUNDS, "SOUNDS", false),
                new Entry(MpcUiState.Mode.XYFX, "XY", false)
        };

        final ArrayList<Entry> result = new ArrayList<>();
        for (Entry entry : promoted) {
            result.add(entry);
        }
        for (Entry entry : MENU_GRID) {
            boolean alreadyPresent = false;
            for (Entry selected : result) {
                if (selected.mode == entry.mode) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                result.add(entry);
            }
        }
        return result.toArray(new Entry[0]);
    }

    static MpcUiState.Mode[] defaultShortcuts() {
        return Arrays.copyOf(DEFAULT_SHORTCUTS, DEFAULT_SHORTCUTS.length);
    }
}
