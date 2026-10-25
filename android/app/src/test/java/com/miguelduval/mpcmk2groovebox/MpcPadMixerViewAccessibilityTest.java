package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MpcPadMixerViewAccessibilityTest {
    @Test
    public void panButtonsIdentifyPadControlAndDirection() {
        assertEquals(
                "PAD 01 PAN decrease",
                MpcPadMixerView.controlButtonDescription(
                        0, MpcPadMixerView.ControlFocus.PAN, false));
        assertEquals(
                "PAD 01 PAN increase",
                MpcPadMixerView.controlButtonDescription(
                        0, MpcPadMixerView.ControlFocus.PAN, true));
    }

    @Test
    public void tuningButtonsIdentifyPadControlAndDirection() {
        assertEquals(
                "PAD 16 TUNE decrease",
                MpcPadMixerView.controlButtonDescription(
                        15, MpcPadMixerView.ControlFocus.TUNE, false));
        assertEquals(
                "PAD 16 TUNE increase",
                MpcPadMixerView.controlButtonDescription(
                        15, MpcPadMixerView.ControlFocus.TUNE, true));
    }
}
