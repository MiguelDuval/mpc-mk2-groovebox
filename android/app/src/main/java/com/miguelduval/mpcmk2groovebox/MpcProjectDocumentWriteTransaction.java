package com.miguelduval.mpcmk2groovebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Best-effort rollback transaction for writing a complete Project through SAF.
 *
 * Document providers do not promise that "wt" updates are atomic. Capture and
 * verify the existing document before touching it, verify the completed write
 * by reading it back, and restore the preimage after any ambiguous write result.
 * If the provider cannot be restored and verified, retain the exact preimage in
 * app-private storage rather than deleting the only known-good copy.
 */
final class MpcProjectDocumentWriteTransaction {
    interface DocumentAccess {
        InputStream openInputStream() throws IOException;
        OutputStream openOutputStream() throws IOException;
    }

    static final class Result {
        final boolean success;
        final boolean writeAttempted;
        final boolean previousDocumentPreserved;
        final File recoveryCopy;
        final long previousBytes;
        final long verifiedBytes;
        final String failure;

        private Result(
                boolean success,
                boolean writeAttempted,
                boolean previousDocumentPreserved,
                File recoveryCopy,
                long previousBytes,
                long verifiedBytes,
                String failure) {
            this.success = success;
            this.writeAttempted = writeAttempted;
            this.previousDocumentPreserved = previousDocumentPreserved;
            this.recoveryCopy = recoveryCopy;
            this.previousBytes = previousBytes;
            this.verifiedBytes = verifiedBytes;
            this.failure = failure;
        }
    }

    private static final class Fingerprint {
        final long bytes;
        final String sha256;

        Fingerprint(long bytes, String sha256) {
            this.bytes = bytes;
            this.sha256 = sha256;
        }

        boolean matches(Fingerprint other) {
            return other != null && bytes == other.bytes && sha256.equals(other.sha256);
        }
    }

    private MpcProjectDocumentWriteTransaction() {}

    static Result write(
            DocumentAccess document,
            File recoveryDirectory,
            byte[] replacement,
            int maxBytes) {
        if (document == null || recoveryDirectory == null || replacement == null || maxBytes <= 0) {
            return failed(false, true, null, 0, 0,
                    "Invalid Project document or backup configuration");
        }
        if (replacement.length == 0 || replacement.length > maxBytes) {
            return failed(false, true, null, 0, 0,
                    "Project snapshot is empty or exceeds the size limit");
        }

        File backup = null;
        Fingerprint previous = null;
        try {
            if (!recoveryDirectory.isDirectory()
                    && !recoveryDirectory.mkdirs()
                    && !recoveryDirectory.isDirectory()) {
                throw new IOException("Could not create private recovery directory");
            }
            backup = File.createTempFile(
                    "mpc-project-", ".recovery.mpcproj", recoveryDirectory);
            try (InputStream input = document.openInputStream();
                    OutputStream output = new FileOutputStream(backup, false)) {
                if (input == null) {
                    throw new IOException("Document provider returned no input stream");
                }
                previous = copyAndFingerprint(input, output, maxBytes);
            }
            final Fingerprint verifiedBackup = fingerprintFile(backup, maxBytes);
            if (!previous.matches(verifiedBackup)) {
                throw new IOException("Private backup did not match the existing document");
            }
        } catch (Exception error) {
            deleteQuietly(backup);
            return failed(false, true, null, 0, 0,
                    "Could not back up destination before writing: " + describe(error));
        }

        final Fingerprint expected;
        try {
            expected = fingerprintBytes(replacement);
        } catch (IOException error) {
            deleteQuietly(backup);
            return failed(false, true, null, previous.bytes, 0,
                    "Could not fingerprint Project snapshot: " + describe(error));
        }

        String writeFailure;
        try {
            // Count the attempt before opening: some providers may truncate while
            // opening "wt" and then throw before returning a usable stream.
            final OutputStream providerStream = document.openOutputStream();
            if (providerStream == null) {
                throw new IOException("Document provider returned no output stream");
            }
            try (OutputStream output = providerStream) {
                output.write(replacement);
                output.flush();
            }
            final Fingerprint persisted = fingerprintDocument(document, maxBytes);
            if (!expected.matches(persisted)) {
                throw new IOException("Provider read-back did not match the saved Project snapshot");
            }
            deleteQuietly(backup);
            return new Result(true, true, false, null,
                    previous.bytes, persisted.bytes, null);
        } catch (Exception error) {
            writeFailure = describe(error);
        }

        String rollbackFailure = null;
        try {
            final Fingerprint restoredFromBackup;
            try (InputStream input = new FileInputStream(backup);
                    OutputStream output = document.openOutputStream()) {
                if (output == null) {
                    throw new IOException("Document provider returned no rollback output stream");
                }
                restoredFromBackup = copyAndFingerprint(input, output, maxBytes);
                output.flush();
            }
            if (!previous.matches(restoredFromBackup)) {
                rollbackFailure = "Recovery backup changed while restoring the document";
            }
        } catch (Exception error) {
            rollbackFailure = describe(error);
        }

        boolean restored = false;
        long verifiedBytes = 0;
        try {
            final Fingerprint afterRollback = fingerprintDocument(document, maxBytes);
            verifiedBytes = afterRollback.bytes;
            restored = previous.matches(afterRollback);
            if (!restored && rollbackFailure == null) {
                rollbackFailure = "Provider read-back still differs from the original document";
            }
        } catch (Exception error) {
            rollbackFailure = append(rollbackFailure,
                    "Could not verify rollback: " + describe(error));
        }

        if (restored) {
            deleteQuietly(backup);
            return failed(true, true, null, previous.bytes, verifiedBytes,
                    "Project write failed: " + writeFailure + "; previous document restored and verified");
        }

        // Preserve the last verified pre-write bytes if rollback cannot be proven.
        // The active in-memory Project is not changed by this file transaction.
        return failed(true, false, backup, previous.bytes, verifiedBytes,
                "Project write failed: " + writeFailure
                        + "; rollback could not be verified: "
                        + (rollbackFailure == null ? "unknown provider failure" : rollbackFailure));
    }

    private static Fingerprint fingerprintDocument(DocumentAccess document, int maxBytes)
            throws IOException {
        try (InputStream input = document.openInputStream()) {
            if (input == null) {
                throw new IOException("Document provider returned no verification input stream");
            }
            return copyAndFingerprint(input, null, maxBytes);
        }
    }

    private static Fingerprint fingerprintFile(File file, int maxBytes) throws IOException {
        try (InputStream input = new FileInputStream(file)) {
            return copyAndFingerprint(input, null, maxBytes);
        }
    }

    private static Fingerprint fingerprintBytes(byte[] bytes) throws IOException {
        final MessageDigest digest = newDigest();
        digest.update(bytes);
        return new Fingerprint(bytes.length, toHex(digest.digest()));
    }

    private static Fingerprint copyAndFingerprint(
            InputStream input, OutputStream output, int maxBytes) throws IOException {
        final MessageDigest digest = newDigest();
        final byte[] buffer = new byte[16 * 1024];
        long total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (count == 0) continue;
            total += count;
            if (total > maxBytes) {
                throw new IOException("Project document exceeds the " + maxBytes + "-byte safety limit");
            }
            digest.update(buffer, 0, count);
            if (output != null) output.write(buffer, 0, count);
        }
        return new Fingerprint(total, toHex(digest.digest()));
    }

    private static MessageDigest newDigest() throws IOException {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IOException("SHA-256 is unavailable", impossible);
        }
    }

    private static String toHex(byte[] bytes) {
        final char[] alphabet = "0123456789abcdef".toCharArray();
        final char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            final int value = bytes[i] & 0xff;
            result[i * 2] = alphabet[value >>> 4];
            result[i * 2 + 1] = alphabet[value & 0x0f];
        }
        return new String(result);
    }

    private static String describe(Exception error) {
        final String message = error.getMessage();
        return error.getClass().getSimpleName()
                + (message == null || message.trim().isEmpty() ? "" : ": " + message.trim());
    }

    private static String append(String first, String second) {
        if (first == null || first.isEmpty()) return second;
        return first + "; " + second;
    }

    private static Result failed(
            boolean writeAttempted,
            boolean previousDocumentPreserved,
            File recoveryCopy,
            long previousBytes,
            long verifiedBytes,
            String failure) {
        return new Result(false, writeAttempted, previousDocumentPreserved,
                recoveryCopy, previousBytes, verifiedBytes, failure);
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            // A leftover private backup is safer than masking the original outcome.
        }
    }
}
