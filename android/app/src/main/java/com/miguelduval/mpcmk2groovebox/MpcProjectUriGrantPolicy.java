package com.miguelduval.mpcmk2groovebox;

import android.content.Intent;

/** Restricts persistent SAF grants to the URI modes actually returned by the provider. */
final class MpcProjectUriGrantPolicy {
    private MpcProjectUriGrantPolicy() {}

    static int persistableModeFlags(int resultFlags) {
        if ((resultFlags & Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) == 0) return 0;
        return resultFlags & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
}
