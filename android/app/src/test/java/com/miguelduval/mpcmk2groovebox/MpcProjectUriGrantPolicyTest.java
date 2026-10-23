package com.miguelduval.mpcmk2groovebox;

import android.content.Intent;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

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
}
