package com.miguelduval.mpcmk2groovebox;

import android.content.Intent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpcProjectUriGrantPolicyTest {
    @Test public void readOnlyProviderGrantPersistsReadWithoutInventingWriteAccess() {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, MpcProjectUriGrantPolicy.persistableModeFlags(flags));
    }
    @Test public void writeOnlyProviderGrantPersistsWriteWithoutInventingReadAccess() {
        int flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        assertEquals(Intent.FLAG_GRANT_WRITE_URI_PERMISSION, MpcProjectUriGrantPolicy.persistableModeFlags(flags));
    }
    @Test public void bothReturnedModesMayBePersisted() {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION;
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                MpcProjectUriGrantPolicy.persistableModeFlags(flags));
    }
    @Test public void transientGrantIsNotPersistable() {
        assertEquals(0, MpcProjectUriGrantPolicy.persistableModeFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
    }
    @Test public void persistableFlagWithoutReadOrWriteModeCreatesNoGrant() {
        assertEquals(0, MpcProjectUriGrantPolicy.persistableModeFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION));
    }
    @Test public void unrelatedIntentFlagsAreNotPassedAsUriModes() {
        int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION;
        assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION, MpcProjectUriGrantPolicy.persistableModeFlags(flags));
    }

    @Test public void unlinkedProjectKeepsSaveAvailableAsSaveAsEntryPoint() {
        assertEquals(true, MpcProjectUriGrantPolicy.directSaveActionAvailable(false, false, false));
    }

    @Test public void linkedWritableDocumentEnablesDirectSave() {
        assertEquals(true, MpcProjectUriGrantPolicy.directSaveActionAvailable(true, true, false));
    }

    @Test public void readOnlyOrRevokedWriteGrantDisablesDirectSave() {
        assertEquals(false, MpcProjectUriGrantPolicy.directSaveActionAvailable(true, false, false));
    }

    @Test public void providerWriteDenialDisablesDirectSaveEvenWhenSystemGrantStillExists() {
        assertEquals(false, MpcProjectUriGrantPolicy.directSaveActionAvailable(true, true, true));
    }

    @Test public void eaccesProviderFailureDisablesDirectSave() {
        assertTrue(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure("ErrnoException: EACCES"));
    }

    @Test public void epermOperationNotPermittedDisablesDirectSave() {
        assertTrue(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure(
                "ErrnoException: EPERM (Operation not permitted)"));
    }

    @Test public void readOnlyFileSystemProviderFailureDisablesDirectSave() {
        assertTrue(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure(
                "ErrnoException: EROFS (Read-only file system)"));
    }

    @Test public void genericProviderTimeoutDoesNotLatchReadOnlyWriteState() {
        assertFalse(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure(
                "IOException: provider timed out while reading the pre-write snapshot"));
    }

    @Test public void nullOrBlankProviderFailureDoesNotLatchWriteDenial() {
        assertFalse(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure(null));
        assertFalse(MpcProjectUriGrantPolicy.isProviderWriteAccessFailure("  "));
    }

    @Test public void preimageReadEaccesDoesNotLatchWriteDenialBeforeAnyWriteAttempt() {
        assertFalse(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                false, "Could not back up destination before writing: ErrnoException: EACCES"));
    }

    @Test public void preimageReadSecurityExceptionDoesNotLatchWriteDenialBeforeAnyWriteAttempt() {
        assertFalse(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                false, "Could not back up destination before writing: SecurityException"));
    }

    @Test public void actualWriteAttemptEaccesLatchesWriteDenial() {
        assertTrue(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                true, "Project write failed: ErrnoException: EACCES (Permission denied)"));
    }

    @Test public void attemptedWriteWithGenericReadbackTimeoutDoesNotLatchWriteDenial() {
        assertFalse(MpcProjectUriGrantPolicy.shouldLatchProviderWriteDenial(
                true, "Project write failed: IOException: provider timed out during verification"));
    }
}
