package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MpcProjectOpenSessionPolicyTest {
    @Test
    public void currentOpenMayApplyOnlyToTheSessionItStartedFrom() {
        assertTrue(MpcProjectOpenSessionPolicy.canApplyOpenResult(3L, 3L, 8L, 8L));
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(3L, 3L, 8L, 9L));
    }

    @Test
    public void olderOpenRequestCannotReplaceTheNewerSelection() {
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(4L, 5L, 8L, 8L));
        assertTrue(MpcProjectOpenSessionPolicy.canApplyOpenResult(5L, 5L, 8L, 8L));
    }

    @Test
    public void newProjectSessionInvalidatesPendingOpenResult() {
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(7L, 7L, 10L, 11L));
    }

    @Test
    public void bothRequestAndSessionMustMatch() {
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(1L, 2L, 3L, 4L));
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(1L, 1L, 3L, 4L));
        assertFalse(MpcProjectOpenSessionPolicy.canApplyOpenResult(1L, 2L, 3L, 3L));
    }
}
