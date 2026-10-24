package com.miguelduval.mpcmk2groovebox;

/**
 * Pure policy for asynchronous Project Open operations.
 *
 * Reading a SAF document happens off the UI thread. The bytes may only replace
 * the native Project if the read still belongs to the newest Open request and
 * the active Project session has not been replaced since that read began.
 */
final class MpcProjectOpenSessionPolicy {
    private MpcProjectOpenSessionPolicy() {}

    static boolean canApplyOpenResult(
            long requestGeneration,
            long activeRequestGeneration,
            long startingSessionGeneration,
            long activeSessionGeneration) {
        return requestGeneration == activeRequestGeneration
                && startingSessionGeneration == activeSessionGeneration;
    }
}
