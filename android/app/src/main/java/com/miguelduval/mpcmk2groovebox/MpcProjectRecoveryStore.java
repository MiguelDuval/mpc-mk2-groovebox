package com.miguelduval.mpcmk2groovebox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Validates and reads pre-write Project recovery copies from app-private storage. */
final class MpcProjectRecoveryStore {
    static final String FILE_PREFIX = "mpc-project-";
    static final String FILE_SUFFIX = ".recovery.mpcproj";
    private MpcProjectRecoveryStore() {}

    static List<File> list(File privateDirectory) {
        if (privateDirectory == null || !privateDirectory.isDirectory()) return Collections.emptyList();
        File[] entries = privateDirectory.listFiles();
        if (entries == null || entries.length == 0) return Collections.emptyList();
        List<File> result = new ArrayList<>();
        for (File entry : entries) if (isRecoveryCopy(entry, privateDirectory)) result.add(entry);
        Collections.sort(result, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                int recent = Long.compare(b.lastModified(), a.lastModified());
                return recent != 0 ? recent : a.getName().compareTo(b.getName());
            }
        });
        return result;
    }

    static boolean isRecoveryCopy(File candidate, File privateDirectory) {
        if (candidate == null || privateDirectory == null) return false;
        String name = candidate.getName();
        if (!name.startsWith(FILE_PREFIX) || !name.endsWith(FILE_SUFFIX)
                || !candidate.isFile() || !candidate.canRead() || candidate.length() <= 0) return false;
        try {
            File base = privateDirectory.getCanonicalFile();
            File real = candidate.getCanonicalFile();
            return real.getParentFile() != null && base.equals(real.getParentFile())
                    && real.isFile() && real.canRead() && real.length() > 0;
        } catch (IOException error) {
            return false;
        }
    }

    static byte[] read(File candidate, File privateDirectory, int maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IOException("Invalid recovery-copy size limit");
        if (!isRecoveryCopy(candidate, privateDirectory)) {
            throw new IOException("Recovery copy is missing, empty, unreadable, or outside app-private storage");
        }
        long originalLength = candidate.length();
        if (originalLength > maxBytes) throw new IOException("Recovery copy exceeds the " + maxBytes + "-byte safety limit");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[16 * 1024];
        long total = 0;
        try (FileInputStream input = new FileInputStream(candidate)) {
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (count == 0) continue;
                total += count;
                if (total > maxBytes) throw new IOException("Recovery copy exceeds the " + maxBytes + "-byte safety limit");
                output.write(buffer, 0, count);
            }
        }
        byte[] bytes = output.toByteArray();
        if (bytes.length != originalLength) throw new IOException("Recovery copy changed while being read");
        return bytes;
    }
}
