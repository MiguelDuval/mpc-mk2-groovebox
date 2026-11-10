package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MpcPadMixerViewAccessibilityTest {
    @Test
    public void panButtonsIdentifyPadControlAndDirection() {
        assertEquals(
                "PAD 01 PAN decrease",
                MpcPadMixerAccessibilityPolicy.controlButtonDescription(0, "PAN", false));
        assertEquals(
                "PAD 01 PAN increase",
                MpcPadMixerAccessibilityPolicy.controlButtonDescription(0, "PAN", true));
    }

    @Test
    public void tuningButtonsIdentifyPadControlAndDirection() {
        assertEquals(
                "PAD 16 TUNE decrease",
                MpcPadMixerAccessibilityPolicy.controlButtonDescription(15, "TUNE", false));
        assertEquals(
                "PAD 16 TUNE increase",
                MpcPadMixerAccessibilityPolicy.controlButtonDescription(15, "TUNE", true));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void unsupportedControlDoesNotCreateAmbiguousLabel() {
        MpcPadMixerAccessibilityPolicy.controlButtonDescription(0, "LEVEL", true);
    }
}
