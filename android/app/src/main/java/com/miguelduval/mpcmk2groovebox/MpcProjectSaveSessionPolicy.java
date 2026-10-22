package com.miguelduval.mpcmk2groovebox;

/**
 * Pure policy for asynchronous Project-save callbacks.
 *
 * A save captures serialized state on the UI thread and performs provider I/O
 * later. The current native Project may change while that I/O runs, so success
 * must be separated from permission to mutate the active session's UI/link.
 */
final class MpcProjectSaveSessionPolicy {
    private MpcProjectSaveSessionPolicy() {}

    static boolean isCurrentSession(long saveGeneration, long activeGeneration) {
        return saveGeneration == activeGeneration;
    }

    static boolean snapshotMatchesCurrentState(
            String snapshotSha256,
            String activeStateSha256) {
        return isSha256(snapshotSha256)
                && isSha256(activeStateSha256)
                && snapshotSha256.equals(activeStateSha256);
    }

    private static boolean isSha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }
}
