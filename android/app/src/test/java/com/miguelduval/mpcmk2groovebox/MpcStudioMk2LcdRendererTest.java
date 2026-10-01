package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class MpcStudioMk2LcdRendererTest {
    @Test
    public void signatureFormatsBooleanStateFlags() {
        final MpcStudioMk2LcdRenderer.State state =
                new MpcStudioMk2LcdRenderer.State(
                        "MAIN",
                        0,
                        4,
                        -1,
                        0,
                        2,
                        120.0,
                        4,
                        4,
                        0L,
                        true,
                        false,
                        true,
                        0,
                        0,
                        0,
                        2,
                        true,
                        true,
                        false,
                        0,
                        -1,
                        "ready");

        final String signature = state.signature();

        assertTrue(signature.contains("|true|false|true|"));
        assertTrue(signature.contains("|NRtrue:2|LOCtrue|ERfalse|"));
    }
}
