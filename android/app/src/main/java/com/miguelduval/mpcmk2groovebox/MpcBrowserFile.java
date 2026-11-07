package com.miguelduval.mpcmk2groovebox;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private final String relativePath;
    private final long lastModifiedMs;

    MpcBrowserFile(String uri, String displayName, String mimeType,
            long sizeBytes, long durationMs) {
        this(uri, displayName, mimeType, sizeBytes, durationMs, "", -1L);
    }

    MpcBrowserFile(String uri, String displayName, String mimeType,
            long sizeBytes, long durationMs, String relativePath) {
        this(uri, displayName, mimeType, sizeBytes, durationMs, relativePath, -1L);
    }

    MpcBrowserFile(String uri, String displayName, String mimeType,
            long sizeBytes, long durationMs, long lastModifiedMs) {
        this(uri, displayName, mimeType, sizeBytes, durationMs, "", lastModifiedMs);
    }

    MpcBrowserFile(String uri, String displayName, String mimeType,
            long sizeBytes, long durationMs, String relativePath, long lastModifiedMs) {
        this.uri = uri == null ? "" : uri;
        this.displayName = displayName == null ? "" : displayName;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.sizeBytes = sizeBytes;
        this.durationMs = durationMs;
        this.relativePath = relativePath == null ? "" : relativePath.trim();
        this.lastModifiedMs = lastModifiedMs;
    }

    String uri() { return uri; }
    String displayName() { return displayName; }
    String mimeType() { return mimeType; }
    long sizeBytes() { return sizeBytes; }
    long durationMs() { return durationMs; }
    String relativePath() { return relativePath; }
    long lastModifiedMs() { return lastModifiedMs; }

    /**
     * Returns a new list in deterministic, case-insensitive display-name order.
     * Providers may enumerate files in different orders between scans; Browser
     * results must not jump around simply because the provider changed its order.
     * Exact-name and URI tie-breakers make ordering deterministic for duplicates.
     */
    static List<MpcBrowserFile> sortedByDisplayName(List<MpcBrowserFile> files) {
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }
        final List<MpcBrowserFile> sorted = new ArrayList<>(files);
        Collections.sort(sorted, (left, right) -> {
            final int insensitive = left.displayName.compareToIgnoreCase(right.displayName);
            if (insensitive != 0) return insensitive;
            final int exact = left.displayName.compareTo(right.displayName);
            if (exact != 0) return exact;
            return left.uri.compareTo(right.uri);
        });
        return sorted;
    }

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
                        && ("INTERNAL".equals(normalizedLocation)
                                || "USER FOLDER".equals(normalizedLocation)
                                || isFavoriteLocation(normalizedLocation));
        final boolean supportedContent =
                "CONTENT".equals(normalizedSection)
                        && ("SAMPLES".equals(normalizedLocation)
                                || "MY FILES".equals(normalizedLocation));
        final boolean pathBasedContent =
                "CONTENT".equals(normalizedSection)
                        && ("DRUMS".equals(normalizedLocation)
                                || "DEMOS".equals(normalizedLocation))
                        && pathContainsFolderSegment(relativePath, normalizedLocation);
        if (!supportedPlace && !supportedContent && !pathBasedContent) return false;
        if (!"ALL".equals(normalizedFilter) && !"SAMPLES".equals(normalizedFilter)) {
            return false;
        }
        final String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() || displayName.toLowerCase(Locale.ROOT).contains(needle);
    }

    /** Match an exact path component, not a filename substring or similarly named folder. */
    static boolean pathContainsFolderSegment(String path, String folderName) {
        if (path == null || path.trim().isEmpty()
                || folderName == null || folderName.trim().isEmpty()) {
            return false;
        }
        final String expected = folderName.trim();
        final String[] segments = path.replace('\\', '/').split("/");
        for (String segment : segments) {
            if (expected.equalsIgnoreCase(segment.trim())) return true;
        }
        return false;
    }

    String metadataLine() {
        return metadataLine(true, false);
    }

    String metadataLine(boolean showFileSize) {
        return metadataLine(showFileSize, false);
    }

    String metadataLine(boolean showFileSize, boolean showModifiedDate) {
        final StringBuilder line = new StringBuilder();
        if (showFileSize) {
            appendMetadataPart(line, sizeBytes < 0 ? "SIZE UNKNOWN" : formatSize(sizeBytes));
        }
        if (showModifiedDate) {
            appendMetadataPart(line, lastModifiedMs <= 0
                    ? "DATE UNKNOWN" : "MODIFIED " + formatModifiedDate(lastModifiedMs));
        }
        appendMetadataPart(line, durationMs <= 0
                ? "DURATION UNKNOWN" : formatDuration(durationMs));
        appendMetadataPart(line, "WAV");
        return line.toString();
    }

    private static void appendMetadataPart(StringBuilder line, String part) {
        if (line.length() > 0) line.append(" • ");
        line.append(part);
    }

    static String formatModifiedDate(long epochMillis) {
        if (epochMillis <= 0) return "DATE UNKNOWN";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date(epochMillis));
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

    private static boolean isFavoriteLocation(String value) {
        final String normalized = upper(value);
        return "FAVORITE 1".equals(normalized)
                || "FAVORITE 2".equals(normalized)
                || "FAVORITE 3".equals(normalized)
                || "FAVORITE 4".equals(normalized)
                || "FAVORITE 5".equals(normalized);
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
