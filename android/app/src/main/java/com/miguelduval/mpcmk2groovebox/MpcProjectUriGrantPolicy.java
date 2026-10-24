package com.miguelduval.mpcmk2groovebox;

import android.content.Intent;
import java.util.Locale;

/** SAF grant and provider-write-availability policy. */
final class MpcProjectUriGrantPolicy {
    private MpcProjectUriGrantPolicy() {}

    static int persistableModeFlags(int resultFlags) {
        if ((resultFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) return 0;
        return resultFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    static boolean isProviderWriteAccessFailure(String failure) {
        if (failure == null || failure.trim().isEmpty()) return false;
        final String normalized = failure.toLowerCase(Locale.ROOT);
        return normalized.contains("securityexception")
                || normalized.contains("eacces")
                || normalized.contains("eperm")
                || normalized.contains("operation not permitted")
                || normalized.contains("permission denied")
                || normalized.contains("accessdeniedexception")
                || normalized.contains("access denied")
                || normalized.contains("read-only")
                || normalized.contains("readonly")
                || normalized.contains("not writable")
                || normalized.contains("write permission");
    }

    /**
     * Latch direct SAVE as unavailable only when opening a provider output stream
     * is denied. A failure after a stream was returned (write/flush/close) or while
     * reading/verifying describes a different phase and is not proof of open denial.
     */
    static boolean shouldLatchProviderWriteDenial(
            boolean writeAttempted,
            MpcProjectDocumentWriteTransaction.FailurePhase failurePhase,
            String failureDetail,
            MpcProjectDocumentWriteTransaction.FailurePhase rollbackFailurePhase,
            String rollbackFailureDetail) {
        if (!writeAttempted) return false;
        final boolean primaryWriteDenied =
                failurePhase == MpcProjectDocumentWriteTransaction.FailurePhase.PROVIDER_OUTPUT_OPEN
                        && isProviderWriteAccessFailure(failureDetail);
        final boolean rollbackWriteDenied =
                rollbackFailurePhase == MpcProjectDocumentWriteTransaction.FailurePhase.ROLLBACK_OUTPUT_OPEN
                        && isProviderWriteAccessFailure(rollbackFailureDetail);
        return primaryWriteDenied || rollbackWriteDenied;
    }

    static boolean directSaveActionAvailable(
            boolean hasCurrentDocument, boolean writePermissionGranted,
            boolean providerPreviouslyRejectedWrite) {
        // An unlinked SAVE is a Save As entry point. A linked document needs live
        // write permission and must not have explicitly rejected a prior write.
        return !hasCurrentDocument || (writePermissionGranted && !providerPreviouslyRejectedWrite);
    }
}
