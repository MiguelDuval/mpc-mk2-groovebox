package com.miguelduval.mpcmk2groovebox;

import java.util.Locale;

/**
 * A real local WAV item discovered through Android MediaStore.
 * The URI is plain data so Browser filtering policy is JVM-testable.
 */
final class MpcBrowserFile {
    private final String uri;
    private final String displayName;
    private final String mimeType;
    private final long sizeBytes;
    private final long durationMs;

    MpcBrowserFile(String uri, String displayName, String mimeType,
            long sizeBytes, long durationMs) {
        this.uri = uri == null ? "" : uri;
        this.displayName = displayName == null ? "" : displayName;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.sizeBytes = sizeBytes;
        this.durationMs = durationMs;
    }

    String uri() { return uri; }
    String displayName() { return displayName; }
    String mimeType() { return mimeType; }
    long sizeBytes() { return sizeBytes; }
    long durationMs() { return durationMs; }

    static boolean isWavName(String name) {
        return name != null
                && name.trim().toLowerCase(Locale.ROOT).endsWith(".wav");
    }

    /** Categories without a real local provider intentionally return no rows. */
    boolean matchesBrowserQuery(String section, String location,
            String filter, String query) {
        if (!isWavName(displayName)) return false;
        final String normalizedSection = upper(section);
        final String normalizedLocation = upper(location);
        final String normalizedFilter = upper(filter);
        final boolean supportedPlace =
                "PLACES".equals(normalizedSection)
                        && "INTERNAL".equals(normalizedLocation);
        final boolean supportedContent =
                "CONTENT".equals(normalizedSection)
                        && ("SAMPLES".equals(normalizedLocation)
                                || "MY FILES".equals(normalizedLocation));
        if (!supportedPlace && !supportedContent) return false;
        if (!"ALL".equals(normalizedFilter) && !"SAMPLES".equals(normalizedFilter)) {
            return false;
        }
        final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() || displayName.toLowerCase(Locale.ROOT).contains(needle);
    }

    String metadataLine() {
        final String size = sizeBytes < 0 ? "SIZE UNKNOWN" : formatSize(sizeBytes);
        final String duration = durationMs <= 0
                ? "DURATION UNKNOWN" : formatDuration(durationMs);
        return size + " • " + duration + " • WAV";
    }

    static String formatSize(long bytes) {
        if (bytes < 0) return "SIZE UNKNOWN";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    static String formatDuration(long durationMs) {
        if (durationMs <= 0) return "DURATION UNKNOWN";
        final long seconds = durationMs / 1000L;
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
