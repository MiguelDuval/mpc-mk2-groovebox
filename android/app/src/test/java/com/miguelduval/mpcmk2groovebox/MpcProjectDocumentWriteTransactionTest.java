package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MpcProjectDocumentWriteTransactionTest {
    private static final byte[] OLD_DOCUMENT = bytes("valid-old-project-with-midi-state");
    private static final byte[] NEW_DOCUMENT = bytes("new-project-with-midi-and-pattern-placements");

    @Test
    public void successfulWriteMustReadBackExactSnapshotAndRemoveTemporaryBackup() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertTrue(result.success);
            assertTrue(result.writeAttempted);
            assertFalse(result.previousDocumentPreserved);
            assertNull(result.recoveryCopy);
            assertArrayEquals(NEW_DOCUMENT, document.content);
            assertEquals(0, directory.list().length);
        });
    }

    @Test
    public void partialWriteFailureRestoresAndVerifiesExactPreviousDocument() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.partialWriteThenThrow = true;

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertTrue(result.failure.contains("previous document restored and verified"));
            assertNull(result.recoveryCopy);
            assertArrayEquals(OLD_DOCUMENT, document.content);
            assertEquals(0, directory.list().length);
        });
    }

    @Test
    public void silentTruncatedProviderWriteIsDetectedAndRolledBack() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.silentlyTruncateFirstWrite = true;

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.previousDocumentPreserved);
            assertArrayEquals(OLD_DOCUMENT, document.content);
            assertNull(result.recoveryCopy);
        });
    }

    @Test
    public void failureToReadPreimageAbortsBeforeOpeningDestinationForWrite() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.failInputOpenNumber = 1;

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertFalse(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertEquals(0, document.outputOpenCount);
            assertArrayEquals(OLD_DOCUMENT, document.content);
            assertNull(result.recoveryCopy);
            assertEquals(0, directory.list().length);
        });
    }

    @Test
    public void readbackProviderFailureRollsBackAndVerifiesOriginal() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.failInputOpenNumber = 2;

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.PROVIDER_READBACK,
                    result.failurePhase);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.NONE,
                    result.rollbackFailurePhase);
            assertArrayEquals(OLD_DOCUMENT, document.content);
            assertNull(result.recoveryCopy);
        });
    }

    @Test
    public void providerReadbackSecurityExceptionRestoresDestinationWithoutLatchingWriteDenial() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.failInputOpenNumber = 2;
            document.failInputOpenMessage =
                    "SecurityException: permission denied while verifying provider output";

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.PROVIDER_READBACK,
                    result.failurePhase);
            assertTrue(result.failureDetail.contains("SecurityException"));
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.NONE,
                    result.rollbackFailurePhase);
            assertFalse(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                    result.writeAttempted, result.failurePhase, result.failureDetail,
                    result.rollbackFailurePhase, result.rollbackFailureDetail));
            assertArrayEquals(OLD_DOCUMENT, document.content);
            assertNull(result.recoveryCopy);
        });
    }

    @Test
    public void providerWriteSecurityExceptionIsClassifiedAsWriteDenial() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.failOutputOpenNumber = 1;
            document.failOutputOpenMessage =
                    "SecurityException: permission denied during provider write";

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.PROVIDER_WRITE,
                    result.failurePhase);
            assertTrue(result.failureDetail.contains("SecurityException"));
            assertTrue(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                    result.writeAttempted, result.failurePhase, result.failureDetail,
                    result.rollbackFailurePhase, result.rollbackFailureDetail));
            assertArrayEquals(OLD_DOCUMENT, document.content);
        });
    }

    @Test
    public void deniedRollbackWriteIsClassifiedEvenWhenPrimaryFailureWasReadback() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.failInputOpenNumber = 2;
            document.failInputOpenMessage = "IOException: injected verification read failure";
            document.failOutputOpenNumber = 2;
            document.failOutputOpenMessage =
                    "SecurityException: EACCES permission denied during rollback write";

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertFalse(result.previousDocumentPreserved);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.PROVIDER_READBACK,
                    result.failurePhase);
            assertEquals(MpcProjectDocumentWriteTransaction.FailurePhase.ROLLBACK_WRITE,
                    result.rollbackFailurePhase);
            assertTrue(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                    result.writeAttempted, result.failurePhase, result.failureDetail,
                    result.rollbackFailurePhase, result.rollbackFailureDetail));
            assertNotNull(result.recoveryCopy);
            assertArrayEquals(OLD_DOCUMENT, Files.readAllBytes(result.recoveryCopy.toPath()));
        });
    }

    @Test
    public void unverifiedRollbackRetainsByteExactPrivateRecoveryCopy() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);
            document.partialWriteThenThrow = true;
            document.failOutputOpenNumber = 2;

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 1024);

            assertFalse(result.success);
            assertTrue(result.writeAttempted);
            assertFalse(result.previousDocumentPreserved);
            assertNotNull(result.recoveryCopy);
            assertTrue(result.recoveryCopy.isFile());
            assertArrayEquals(OLD_DOCUMENT, Files.readAllBytes(result.recoveryCopy.toPath()));
            assertTrue(result.failure.contains("rollback could not be verified"));
            assertEquals(1, directory.list().length);
        });
    }

    @Test
    public void rejectsOversizedSnapshotBeforeReadingOrWritingDocument() throws Exception {
        withDirectory(directory -> {
            final FakeDocument document = new FakeDocument(OLD_DOCUMENT);

            final MpcProjectDocumentWriteTransaction.Result result =
                    MpcProjectDocumentWriteTransaction.write(document, directory, NEW_DOCUMENT, 4);

            assertFalse(result.success);
            assertFalse(result.writeAttempted);
            assertTrue(result.previousDocumentPreserved);
            assertEquals(0, document.inputOpenCount);
            assertEquals(0, document.outputOpenCount);
            assertArrayEquals(OLD_DOCUMENT, document.content);
        });
    }

    private static void withDirectory(DirectoryAction action) throws Exception {
        final File directory = Files.createTempDirectory("mpc-project-write-test").toFile();
        try {
            action.run(directory);
        } finally {
            final File[] children = directory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!child.delete()) throw new IOException("Could not clean test recovery file");
                }
            }
            if (!directory.delete()) throw new IOException("Could not clean test directory");
        }
    }

    private interface DirectoryAction {
        void run(File directory) throws Exception;
    }

    private static byte[] bytes(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static final class FakeDocument implements MpcProjectDocumentWriteTransaction.DocumentAccess {
        byte[] content;
        int inputOpenCount;
        int outputOpenCount;
        int failInputOpenNumber = -1;
        int failOutputOpenNumber = -1;
        String failInputOpenMessage = "injected provider read failure";
        String failOutputOpenMessage = "injected provider write-open failure";
        boolean partialWriteThenThrow;
        boolean silentlyTruncateFirstWrite;

        FakeDocument(byte[] content) {
            this.content = content.clone();
        }

        @Override
        public InputStream openInputStream() throws IOException {
            inputOpenCount++;
            if (inputOpenCount == failInputOpenNumber) {
                throw new IOException(failInputOpenMessage);
            }
            return new ByteArrayInputStream(content.clone());
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            outputOpenCount++;
            final int attempt = outputOpenCount;
            if (attempt == failOutputOpenNumber) {
                throw new IOException(failOutputOpenMessage);
            }
            // Emulate providers that truncate as soon as "wt" is opened.
            content = new byte[0];
            final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            return new OutputStream() {
                private boolean partialFailureInjected;

                @Override
                public void write(int value) throws IOException {
                    write(new byte[] {(byte) value}, 0, 1);
                }

                @Override
                public void write(byte[] bytes, int offset, int length) throws IOException {
                    if (attempt == 1 && partialWriteThenThrow && !partialFailureInjected) {
                        partialFailureInjected = true;
                        final int partialLength = Math.max(1, length / 2);
                        buffer.write(bytes, offset, partialLength);
                        content = buffer.toByteArray();
                        throw new IOException("injected partial provider write failure");
                    }
                    if (attempt == 1 && silentlyTruncateFirstWrite) {
                        final int partialLength = Math.max(1, length / 2);
                        buffer.write(bytes, offset, Math.min(partialLength, length));
                        content = buffer.toByteArray();
                        return;
                    }
                    buffer.write(bytes, offset, length);
                    content = buffer.toByteArray();
                }

                @Override
                public void close() {
                    content = buffer.toByteArray();
                }
            };
        }
    }
}
