package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpcProjectSaveSessionPolicyTest {
    private static final String SHA_A =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String SHA_B =
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    @Test
    public void completionMayMutateActiveDocumentOnlyForSameProjectGeneration() {
        assertTrue(MpcProjectSaveSessionPolicy.isCurrentSession(4L, 4L));
        assertFalse(MpcProjectSaveSessionPolicy.isCurrentSession(4L, 5L));
        assertFalse(MpcProjectSaveSessionPolicy.isCurrentSession(4L, 6L));
    }

    @Test
    public void exactSerializedSnapshotDistinguishesNoEditsFromLaterEdits() {
        assertTrue(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState(SHA_A, SHA_A));
        assertFalse(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState(SHA_A, SHA_B));
    }

    @Test
    public void unavailableOrMalformedDigestCanNeverClaimSnapshotIsCurrent() {
        assertFalse(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState(null, SHA_A));
        assertFalse(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState("unavailable", "unavailable"));
        assertFalse(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState(SHA_A, SHA_A.substring(1)));
        assertFalse(MpcProjectSaveSessionPolicy.snapshotMatchesCurrentState(SHA_A.toUpperCase(), SHA_A.toUpperCase()));
    }
}
