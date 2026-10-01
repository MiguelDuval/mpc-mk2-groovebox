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
    public void waveformZoomChangesHorizontalAndVerticalAxesIndependently() {
        final WaveformView view = new WaveformView(
                new android.test.mock.MockContext());

        assertTrue(view.viewportSpanForTest() > 0.99f);
        assertTrue(view.verticalZoomForTest() > 0.99f);

        view.zoomIn();
        final float zoomedSpan = view.viewportSpanForTest();
        assertTrue(zoomedSpan < 0.99f);
        assertTrue(view.verticalZoomForTest() > 0.99f);

        view.zoomVerticalIn();
        final float vertical = view.verticalZoomForTest();
        assertTrue(vertical > 1.0f);
        assertTrue(view.viewportSpanForTest() == zoomedSpan);

        for (int i = 0; i < 32; i++) {
            view.zoomVerticalIn();
            view.zoomIn();
        }
        assertTrue(view.verticalZoomForTest() <= 4.0f);
        assertTrue(view.viewportSpanForTest() >= 0.0625f);

        view.zoomVerticalOut();
        view.zoomOut();
        view.resetZoom();
        assertTrue(view.viewportSpanForTest() > 0.99f);
        assertTrue(view.verticalZoomForTest() > 0.99f);
    }

}
