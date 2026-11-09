package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class MpcLevels16PolicyTest {
    @Test
    public void velocityLevelsMatchTheMpcPadOneToPadSixteenLadder() {
        assertEquals(7, MpcLevels16Policy.velocityForPadIndex(0));
        assertEquals(15, MpcLevels16Policy.velocityForPadIndex(1));
        assertEquals(63, MpcLevels16Policy.velocityForPadIndex(7));
        assertEquals(71, MpcLevels16Policy.velocityForPadIndex(8));
        assertEquals(127, MpcLevels16Policy.velocityForPadIndex(15));
    }

    @Test
    public void controllerVelocityMapsBackToThePhysicalSixteenLevelSlot() {
        for (int pad = 0; pad < 16; pad++) {
            assertEquals(pad, MpcLevels16Policy.padIndexForVelocity(
                    MpcLevels16Policy.velocityForPadIndex(pad)));
        }
    }

    @Test
    public void tuneLevelsAreSteppedAroundTheOriginalPitch() {
        assertEquals(-8, MpcLevels16Policy.tuneSemitonesForPadIndex(0));
        assertEquals(-1, MpcLevels16Policy.tuneSemitonesForPadIndex(7));
        assertEquals(0, MpcLevels16Policy.tuneSemitonesForPadIndex(8));
        assertEquals(7, MpcLevels16Policy.tuneSemitonesForPadIndex(15));
    }

    @Test
    public void onlyTypesWithRealTransientNativePathsAreEnabled() {
        assertTrue(MpcLevels16Policy.isSupported(MpcLevels16Policy.Type.VELOCITY));
        assertTrue(MpcLevels16Policy.isSupported(MpcLevels16Policy.Type.TUNE));
        assertTrue(MpcLevels16Policy.isSupported(MpcLevels16Policy.Type.FILTER));
        for (MpcLevels16Policy.Type type : new MpcLevels16Policy.Type[]{
                MpcLevels16Policy.Type.LAYER, MpcLevels16Policy.Type.SLICE,
                MpcLevels16Policy.Type.ARTICULATION, MpcLevels16Policy.Type.ATTACK,
                MpcLevels16Policy.Type.DECAY, MpcLevels16Policy.Type.PROBABILITY,
                MpcLevels16Policy.Type.RATCHET
        }) {
            assertFalse(MpcLevels16Policy.isSupported(type));
        }
    }
}