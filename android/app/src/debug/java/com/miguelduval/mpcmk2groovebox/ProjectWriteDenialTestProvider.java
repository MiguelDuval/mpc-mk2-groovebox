package com.miguelduval.mpcmk2groovebox;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Debug-only provider fixture for real ContentResolver output-denial acceptance. */
public final class ProjectWriteDenialTestProvider extends ContentProvider {
    private static final String TAG = "MpcProjectWriteDenyProvider";
    private final Object lock = new Object();
    private File documentFile;
    private int outputAttempts;
    private int denyOutputAttempt = 1;

    @Override public boolean onCreate() {
        final Context context = getContext();
        if (context == null) return false;
        documentFile = new File(context.getFilesDir(), "debug-provider-write-denial.mpcproj");
        return true;
    }

    private Uri documentUri() {
        final Context context = getContext();
        if (context == null) throw new IllegalStateException("Provider context unavailable");
        return new Uri.Builder().scheme("content")
                .authority(context.getPackageName() + ".projectwritefixture")
                .appendPath("project").appendPath("current").build();
    }

    private boolean isDocumentUri(Uri uri) {
        return uri != null && "/project/current".equals(uri.getPath());
    }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if ("seed_document".equals(method)) {
            if (extras == null) throw new IllegalArgumentException("Missing fixture seed arguments");
            final byte[] snapshot = extras.getByteArray("snapshot_bytes");
            if (snapshot == null || snapshot.length == 0) throw new IllegalArgumentException("Empty Project snapshot");
            synchronized (lock) {
                try (FileOutputStream output = new FileOutputStream(documentFile, false)) {
                    output.write(snapshot); output.flush(); output.getFD().sync();
                } catch (IOException error) {
                    throw new IllegalStateException("Could not seed controlled provider document", error);
                }
                outputAttempts = 0;
                denyOutputAttempt = Math.max(1, extras.getInt("deny_output_attempt", 1));
            }
            final Bundle result = new Bundle();
            result.putString("uri", documentUri().toString());
            result.putString("sha256", sha256(snapshot));
            result.putInt("deny_output_attempt", denyOutputAttempt);
            return result;
        }
        if ("get_fixture_state".equals(method)) {
            synchronized (lock) {
                final Bundle result = new Bundle();
                try { result.putString("sha256", sha256File(documentFile)); }
                catch (IOException error) { throw new IllegalStateException("Could not fingerprint fixture", error); }
                result.putInt("output_attempts", outputAttempts);
                result.putInt("deny_output_attempt", denyOutputAttempt);
                return result;
            }
        }
        return super.call(method, arg, extras);
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!isDocumentUri(uri) || documentFile == null) throw new FileNotFoundException("Unknown controlled Project document");
        if (mode != null && mode.contains("w")) {
            final int attempt;
            synchronized (lock) {
                attempt = ++outputAttempts;
                if (attempt == denyOutputAttempt) {
                    Log.i(TAG, "PROJECT_WRITE_DENIAL_PROVIDER_OUTPUT_DENIED attempt=" + attempt);
                    throw new SecurityException("Permission denied by controlled debug document provider on output open attempt " + attempt);
                }
            }
            int flags = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE;
            if (mode.contains("t")) flags |= ParcelFileDescriptor.MODE_TRUNCATE;
            else if (mode.contains("a")) flags |= ParcelFileDescriptor.MODE_APPEND;
            Log.i(TAG, "PROJECT_WRITE_DENIAL_PROVIDER_OUTPUT_ALLOWED attempt=" + attempt + " mode=" + mode);
            return ParcelFileDescriptor.open(documentFile, flags);
        }
        if (mode != null && mode.contains("r")) return ParcelFileDescriptor.open(documentFile, ParcelFileDescriptor.MODE_READ_ONLY);
        throw new FileNotFoundException("Unsupported controlled Project document mode: " + mode);
    }

    @Override public String getType(Uri uri) { return isDocumentUri(uri) ? "application/x-mpcmk2-project" : null; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (!isDocumentUri(uri) || documentFile == null) return null;
        final String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
        final MatrixCursor cursor = new MatrixCursor(columns, 1);
        final Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = "Controlled-Project-Write-Denial.mpcproj";
            else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = documentFile.length();
        }
        cursor.addRow(values);
        return cursor;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException("Not insertable"); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Not deletable"); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { throw new UnsupportedOperationException("Not updateable"); }

    private static String sha256(byte[] bytes) {
        try { return toHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException("SHA-256 unavailable", impossible); }
    }
    private static String sha256File(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file)) {
            final MessageDigest digest;
            try { digest = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException impossible) { throw new IOException("SHA-256 unavailable", impossible); }
            final byte[] buffer = new byte[16 * 1024]; int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
            return toHex(digest.digest());
        }
    }
    private static String toHex(byte[] bytes) {
        final char[] alphabet = "0123456789abcdef".toCharArray();
        final char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) { int value = bytes[i] & 0xff; hex[2*i] = alphabet[value >>> 4]; hex[2*i+1] = alphabet[value & 0x0f]; }
        return new String(hex);
    }
}
