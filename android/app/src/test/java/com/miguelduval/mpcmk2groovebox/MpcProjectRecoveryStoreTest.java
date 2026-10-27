package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpcProjectRecoveryStoreTest {
    private static final byte[] OLDER = bytes("older verified Project snapshot");
    private static final byte[] NEWER = bytes("newer verified Project snapshot with MIDI and placements");

    @Test
    public void inventoryIsRestrictedToNonEmptyRecoveryFilesAndNewestFirst() throws Exception {
        withDirectories((directory, outside) -> {
            File older = new File(directory, "mpc-project-older.recovery.mpcproj");
            File newer = new File(directory, "mpc-project-newer.recovery.mpcproj");
            File empty = new File(directory, "mpc-project-empty.recovery.mpcproj");
            File unrelated = new File(directory, "ordinary.mpcproj");
            File dirLike = new File(directory, "mpc-project-folder.recovery.mpcproj");
            File outsideFile = new File(outside, "mpc-project-outside.recovery.mpcproj");
            write(older, OLDER); write(newer, NEWER); write(empty, new byte[0]); write(unrelated, NEWER);
            assertTrue(dirLike.mkdir()); write(outsideFile, OLDER);
            assertTrue(older.setLastModified(1_000L)); assertTrue(newer.setLastModified(2_000L));
            List<File> inventory = MpcProjectRecoveryStore.list(directory);
            assertEquals(2, inventory.size());
            assertEquals(newer.getName(), inventory.get(0).getName());
            assertEquals(older.getName(), inventory.get(1).getName());
            assertFalse(MpcProjectRecoveryStore.isRecoveryCopy(empty, directory));
            assertFalse(MpcProjectRecoveryStore.isRecoveryCopy(unrelated, directory));
            assertFalse(MpcProjectRecoveryStore.isRecoveryCopy(dirLike, directory));
            assertFalse(MpcProjectRecoveryStore.isRecoveryCopy(outsideFile, directory));
        });
    }

    @Test
    public void boundedReadReturnsExactBytesWithoutDeletingPrivateOriginal() throws Exception {
        withDirectories((directory, outside) -> {
            File source = new File(directory, "mpc-project-valid.recovery.mpcproj");
            write(source, NEWER);
            assertArrayEquals(NEWER, MpcProjectRecoveryStore.read(source, directory, 1024));
            assertTrue(source.isFile());
            assertArrayEquals(NEWER, Files.readAllBytes(source.toPath()));
        });
    }

    @Test
    public void readRejectsRecoveryFilesOutsidePrivateDirectory() throws Exception {
        withDirectories((directory, outside) -> {
            File source = new File(outside, "mpc-project-outside.recovery.mpcproj");
            write(source, OLDER);
            assertFalse(MpcProjectRecoveryStore.isRecoveryCopy(source, directory));
            assertReadFails(source, directory, 1024);
        });
    }

    @Test
    public void readRejectsEmptyAndOversizedRecoveryCopies() throws Exception {
        withDirectories((directory, outside) -> {
            File empty = new File(directory, "mpc-project-empty.recovery.mpcproj");
            File oversized = new File(directory, "mpc-project-large.recovery.mpcproj");
            write(empty, new byte[0]); write(oversized, NEWER);
            assertReadFails(empty, directory, 1024); assertReadFails(oversized, directory, 4);
        });
    }

    private static void assertReadFails(File file, File directory, int maxBytes) throws Exception {
        try {
            MpcProjectRecoveryStore.read(file, directory, maxBytes);
            throw new AssertionError("Expected recovery read to fail for " + file);
        } catch (IOException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().isEmpty());
        }
    }
    private static void withDirectories(DirectoryAction action) throws Exception {
        File root = Files.createTempDirectory("mpc-project-recovery-store-test").toFile();
        File directory = new File(root, "private"), outside = new File(root, "outside");
        assertTrue(directory.mkdir()); assertTrue(outside.mkdir());
        try { action.run(directory, outside); }
        finally {
            deleteTree(directory); deleteTree(outside);
            if (!root.delete()) throw new IOException("Could not clean temporary recovery root");
        }
    }
    private static void deleteTree(File directory) throws IOException {
        File[] children = directory.listFiles();
        if (children != null) for (File child : children) {
            if (child.isDirectory()) deleteTree(child);
            if (child.exists() && !child.delete()) throw new IOException("Could not clean recovery test entry");
        }
        if (directory.exists() && !directory.delete()) throw new IOException("Could not clean recovery test directory");
    }
    private static void write(File file, byte[] bytes) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
    }
    private static byte[] bytes(String text) {
        try { return text.getBytes("UTF-8"); }
        catch (java.io.UnsupportedEncodingException impossible) { throw new AssertionError(impossible); }
    }
    private interface DirectoryAction { void run(File privateDirectory, File outsideDirectory) throws Exception; }
}
