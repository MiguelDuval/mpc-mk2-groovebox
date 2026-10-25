package com.miguelduval.mpcmk2groovebox;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Queries shared device audio only after the user explicitly requests a scan.
 * Document Provider remains the separate fallback for locations not indexed
 * by MediaStore.
 */
final class MpcLocalAudioCatalog {
    private MpcLocalAudioCatalog() {}

    static List<MpcBrowserFile> queryWavFiles(ContentResolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("ContentResolver is required");
        }

        final Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        // RELATIVE_PATH is available on Android 10+ and lets Browser filter real folders.
        final boolean hasRelativePath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
        final String[] projection = hasRelativePath
                ? new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.MediaColumns.RELATIVE_PATH
                }
                : new String[]{
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DURATION
                };
        final List<MpcBrowserFile> files = new ArrayList<>();
        try (Cursor cursor = resolver.query(
                collection, projection, null, null,
                MediaStore.Audio.Media.DISPLAY_NAME + " COLLATE NOCASE ASC")) {
            if (cursor == null) return Collections.emptyList();

            final int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            final int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            final int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            final int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            final int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            final int relativePathColumn =
                    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH);

            while (cursor.moveToNext()) {
                final String name = cursor.getString(nameColumn);
                // The current native sampler loader accepts WAV; do not present
                // unsupported codecs, projects, patterns or presets as loadable.
                if (!MpcBrowserFile.isWavName(name)) continue;

                final long id = cursor.getLong(idColumn);
                final String mime = cursor.isNull(mimeColumn) ? "" : cursor.getString(mimeColumn);
                final long size = cursor.isNull(sizeColumn) ? -1L : cursor.getLong(sizeColumn);
                final long duration = cursor.isNull(durationColumn)
                        ? -1L : cursor.getLong(durationColumn);
                final String relativePath = relativePathColumn < 0
                        || cursor.isNull(relativePathColumn)
                        ? "" : cursor.getString(relativePathColumn);
                final Uri itemUri = ContentUris.withAppendedId(collection, id);
                files.add(new MpcBrowserFile(
                        itemUri.toString(), name, mime, size, duration, relativePath));
            }
        }
        return files;
    }
}
