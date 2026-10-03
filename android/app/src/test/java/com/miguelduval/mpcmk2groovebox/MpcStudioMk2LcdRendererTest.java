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
    @Test
    public void waveformZoomPolicyChangesAxesIndependentlyAndClamps() {
        float horizontal = MpcZoomPolicy.MAX_HORIZONTAL_SPAN;
        float vertical = MpcZoomPolicy.MIN_VERTICAL_ZOOM;

        horizontal = MpcZoomPolicy.zoomHorizontalIn(horizontal);
        assertTrue(horizontal < MpcZoomPolicy.MAX_HORIZONTAL_SPAN);
        assertTrue(MpcZoomPolicy.zoomHorizontalOut(horizontal)
                > horizontal);

        vertical = MpcZoomPolicy.zoomVerticalIn(vertical);
        assertTrue(vertical > MpcZoomPolicy.MIN_VERTICAL_ZOOM);
        assertTrue(MpcZoomPolicy.zoomVerticalOut(vertical)
                < vertical);

        for (int i = 0; i < 32; i++) {
            horizontal = MpcZoomPolicy.zoomHorizontalIn(horizontal);
            vertical = MpcZoomPolicy.zoomVerticalIn(vertical);
        }

        assertTrue(horizontal >= MpcZoomPolicy.MIN_HORIZONTAL_SPAN);
        assertTrue(vertical <= MpcZoomPolicy.MAX_VERTICAL_ZOOM);

        for (int i = 0; i < 32; i++) {
            horizontal = MpcZoomPolicy.zoomHorizontalOut(horizontal);
            vertical = MpcZoomPolicy.zoomVerticalOut(vertical);
        }

        assertTrue(horizontal <= MpcZoomPolicy.MAX_HORIZONTAL_SPAN);
        assertTrue(vertical >= MpcZoomPolicy.MIN_VERTICAL_ZOOM);
    }
}
