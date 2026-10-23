package com.miguelduval.mpcmk2groovebox;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only WAV catalog backed by a user-selected Storage Access Framework tree. */
final class MpcDocumentTreeCatalog {
    private MpcDocumentTreeCatalog() {}

    static String fallbackDisplayName(Uri treeUri) {
        if (treeUri == null) throw new IllegalArgumentException("A document-tree URI is required");
        String id = DocumentsContract.getTreeDocumentId(treeUri);
        if (id == null) id = "";
        int volume = id.lastIndexOf(':');
        if (volume >= 0) id = id.substring(volume + 1);
        int slash = id.lastIndexOf('/');
        if (slash >= 0) id = id.substring(slash + 1);
        id = id.trim();
        return id.isEmpty() ? "USER FOLDER" : id;
    }

    static String queryDisplayName(ContentResolver resolver, Uri treeUri) {
        requireInputs(resolver, treeUri);
        String rootId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId);
        try (Cursor cursor = resolver.query(rootUri,
                new String[]{DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int col = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                if (col >= 0) {
                    String name = cursor.getString(col);
                    if (name != null && !name.trim().isEmpty()) return name.trim();
                }
            }
        }
        return fallbackDisplayName(treeUri);
    }

    /** Direct children only, no recursive scan or synthetic entries. */
    static List<MpcBrowserFile> queryWavFiles(ContentResolver resolver, Uri treeUri) {
        requireInputs(resolver, treeUri);
        String rootId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        List<MpcBrowserFile> files = new ArrayList<>();
        try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
            if (cursor == null) return Collections.emptyList();
            int idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
            int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
            int mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
            int sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
            int modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
            if (idCol < 0 || nameCol < 0)
                throw new IllegalStateException("Document provider omitted child ID or display name");
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameCol);
                if (!MpcBrowserFile.isWavName(name)) continue;
                String mime = mimeCol < 0 || cursor.isNull(mimeCol) ? "" : cursor.getString(mimeCol);
                if ("vnd.android.document/directory".equalsIgnoreCase(mime)) continue;
                String id = cursor.getString(idCol);
                if (id == null || id.trim().isEmpty()) continue;
                long size = sizeCol < 0 || cursor.isNull(sizeCol) ? -1L : cursor.getLong(sizeCol);
                long modifiedAtMs = modifiedCol < 0 || cursor.isNull(modifiedCol)
                        ? -1L : cursor.getLong(modifiedCol);
                Uri child = DocumentsContract.buildDocumentUriUsingTree(treeUri, id);
                files.add(new MpcBrowserFile(child.toString(), name, mime, size, -1L, modifiedAtMs));
            }
        }
        Collections.sort(files, (a,b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        return files;
    }

    private static void requireInputs(ContentResolver resolver, Uri treeUri) {
        if (resolver == null) throw new IllegalArgumentException("ContentResolver is required");
        if (treeUri == null) throw new IllegalArgumentException("A document-tree URI is required");
    }
}
