package com.miguelduval.mpcmk2groovebox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
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

    /**
     * Failure origin matters for direct-SAVE availability. A denied readback is
     * not evidence that the provider denied the preceding write.
     */
    enum FailurePhase {
        NONE,
        PRECONDITION,
        PREIMAGE_CAPTURE,
        SNAPSHOT_FINGERPRINT,
        PROVIDER_OUTPUT_OPEN,
        PROVIDER_WRITE,
        PROVIDER_READBACK,
        PROVIDER_READBACK_MISMATCH,
        ROLLBACK_OUTPUT_OPEN,
        ROLLBACK_WRITE,
        ROLLBACK_BACKUP_READ,
        ROLLBACK_VERIFICATION
    }

    static final class Result {
        final boolean success;
        final boolean writeAttempted;
        final boolean previousDocumentPreserved;
        final File recoveryCopy;
        final long previousBytes;
        final long verifiedBytes;
        final String failure;
        final FailurePhase failurePhase;
        final String failureDetail;
        final FailurePhase rollbackFailurePhase;
        final String rollbackFailureDetail;

        private Result(
                boolean success,
                boolean writeAttempted,
                boolean previousDocumentPreserved,
                File recoveryCopy,
                long previousBytes,
                long verifiedBytes,
                String failure,
                FailurePhase failurePhase,
                String failureDetail,
                FailurePhase rollbackFailurePhase,
                String rollbackFailureDetail) {
            this.success = success;
            this.writeAttempted = writeAttempted;
            this.previousDocumentPreserved = previousDocumentPreserved;
            this.recoveryCopy = recoveryCopy;
            this.previousBytes = previousBytes;
            this.verifiedBytes = verifiedBytes;
            this.failure = failure;
            this.failurePhase = failurePhase;
            this.failureDetail = failureDetail;
            this.rollbackFailurePhase = rollbackFailurePhase;
            this.rollbackFailureDetail = rollbackFailureDetail;
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
                    FailurePhase.PRECONDITION, "Invalid Project document or backup configuration",
                    FailurePhase.NONE, null,
                    "Invalid Project document or backup configuration");
        }
        if (replacement.length == 0 || replacement.length > maxBytes) {
            return failed(false, true, null, 0, 0,
                    FailurePhase.PRECONDITION, "Project snapshot is empty or exceeds the size limit",
                    FailurePhase.NONE, null,
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
            final String detail = describe(error);
            return failed(false, true, null, 0, 0,
                    FailurePhase.PREIMAGE_CAPTURE, detail,
                    FailurePhase.NONE, null,
                    "Could not back up destination before writing: " + detail);
        }

        final Fingerprint expected;
        try {
            expected = fingerprintBytes(replacement);
        } catch (IOException error) {
            deleteQuietly(backup);
            final String detail = describe(error);
            return failed(false, true, null, previous.bytes, 0,
                    FailurePhase.SNAPSHOT_FINGERPRINT, detail,
                    FailurePhase.NONE, null,
                    "Could not fingerprint Project snapshot: " + detail);
        }

        // Opening "wt" may truncate a target even when it then throws. Record
        // the attempt before calling the provider, but classify its phase below.
        FailurePhase failurePhase = FailurePhase.NONE;
        String failureDetail = null;
        OutputStream providerStream = null;
        try {
            providerStream = document.openOutputStream();
        } catch (Exception error) {
            // Only an exception thrown while opening the provider output is
            // evidence that this specific access operation was denied.
            failurePhase = FailurePhase.PROVIDER_OUTPUT_OPEN;
            failureDetail = describe(error);
        }
        if (failurePhase == FailurePhase.NONE) {
            if (providerStream == null) {
                failurePhase = FailurePhase.PROVIDER_WRITE;
                failureDetail = "Document provider returned no output stream";
            } else {
                try (OutputStream output = providerStream) {
                    output.write(replacement);
                    output.flush();
                } catch (Exception error) {
                    failurePhase = FailurePhase.PROVIDER_WRITE;
                    failureDetail = describe(error);
                }
            }
        }

        long verifiedBytes = 0;
        if (failurePhase == FailurePhase.NONE) {
            try {
                final Fingerprint persisted = fingerprintDocument(document, maxBytes);
                verifiedBytes = persisted.bytes;
                if (!expected.matches(persisted)) {
                    failurePhase = FailurePhase.PROVIDER_READBACK_MISMATCH;
                    failureDetail = "Provider read-back did not match the saved Project snapshot";
                } else {
                    deleteQuietly(backup);
                    return new Result(true, true, false, null,
                            previous.bytes, persisted.bytes, null,
                            FailurePhase.NONE, null, FailurePhase.NONE, null);
                }
            } catch (Exception error) {
                failurePhase = FailurePhase.PROVIDER_READBACK;
                failureDetail = describe(error);
            }
        }

        final String primaryFailure = failurePhase == FailurePhase.PROVIDER_OUTPUT_OPEN
                || failurePhase == FailurePhase.PROVIDER_WRITE
                ? "Project write failed: " + failureDetail
                : "Project result verification failed: " + failureDetail;

        FailurePhase rollbackFailurePhase = FailurePhase.NONE;
        String rollbackFailureDetail = null;
        String rollbackFailure = null;
        byte[] backupBytes = null;
        // Read and re-verify the private preimage before opening the provider for
        // rollback. This keeps local backup-read failures distinct from an actual
        // provider output failure, which is the only rollback phase that can prove
        // a write-access denial.
        try (InputStream input = new FileInputStream(backup);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final Fingerprint backupFingerprint = copyAndFingerprint(input, output, maxBytes);
            backupBytes = output.toByteArray();
            if (!previous.matches(backupFingerprint)
                    || backupBytes.length != previous.bytes) {
                throw new IOException("Private recovery backup no longer matches the original document");
            }
        } catch (Exception error) {
            rollbackFailurePhase = FailurePhase.ROLLBACK_BACKUP_READ;
            rollbackFailureDetail = describe(error);
            rollbackFailure = rollbackFailureDetail;
        }

        if (rollbackFailurePhase == FailurePhase.NONE) {
            OutputStream rollbackStream = null;
            try {
                rollbackStream = document.openOutputStream();
            } catch (Exception error) {
                rollbackFailurePhase = FailurePhase.ROLLBACK_OUTPUT_OPEN;
                rollbackFailureDetail = describe(error);
                rollbackFailure = rollbackFailureDetail;
            }
            if (rollbackFailurePhase == FailurePhase.NONE) {
                if (rollbackStream == null) {
                    rollbackFailurePhase = FailurePhase.ROLLBACK_WRITE;
                    rollbackFailureDetail = "Document provider returned no rollback output stream";
                    rollbackFailure = rollbackFailureDetail;
                } else {
                    try (OutputStream output = rollbackStream) {
                        output.write(backupBytes);
                        output.flush();
                    } catch (Exception error) {
                        rollbackFailurePhase = FailurePhase.ROLLBACK_WRITE;
                        rollbackFailureDetail = describe(error);
                        rollbackFailure = rollbackFailureDetail;
                    }
                }
            }
        }

        boolean restored = false;
        try {
            final Fingerprint afterRollback = fingerprintDocument(document, maxBytes);
            verifiedBytes = afterRollback.bytes;
            restored = previous.matches(afterRollback);
            if (!restored) {
                final String mismatch = "Provider read-back still differs from the original document";
                rollbackFailure = append(rollbackFailure, mismatch);
                if (rollbackFailurePhase == FailurePhase.NONE) {
                    rollbackFailurePhase = FailurePhase.ROLLBACK_VERIFICATION;
                    rollbackFailureDetail = mismatch;
                }
            }
        } catch (Exception error) {
            final String detail = describe(error);
            rollbackFailure = append(rollbackFailure,
                    "Could not verify rollback: " + detail);
            if (rollbackFailurePhase == FailurePhase.NONE) {
                rollbackFailurePhase = FailurePhase.ROLLBACK_VERIFICATION;
                rollbackFailureDetail = detail;
            }
        }

        if (restored) {
            deleteQuietly(backup);
            return failed(true, true, null, previous.bytes, verifiedBytes,
                    failurePhase, failureDetail, rollbackFailurePhase, rollbackFailureDetail,
                    primaryFailure + "; previous document restored and verified");
        }

        // Preserve the last verified pre-write bytes if rollback cannot be proven.
        // The active in-memory Project is not changed by this file transaction.
        return failed(true, false, backup, previous.bytes, verifiedBytes,
                failurePhase, failureDetail, rollbackFailurePhase, rollbackFailureDetail,
                primaryFailure + "; rollback could not be verified: "
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
            FailurePhase failurePhase,
            String failureDetail,
            FailurePhase rollbackFailurePhase,
            String rollbackFailureDetail,
            String failure) {
        return new Result(false, writeAttempted, previousDocumentPreserved,
                recoveryCopy, previousBytes, verifiedBytes, failure,
                failurePhase, failureDetail, rollbackFailurePhase, rollbackFailureDetail);
    }

    private static void deleteQuietly(File file) {
        if (file != null && file.exists() && !file.delete()) {
            // A leftover private backup is safer than masking the original outcome.
        }
    }
}
