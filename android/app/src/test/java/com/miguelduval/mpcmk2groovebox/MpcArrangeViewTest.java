package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcArrangeViewTest {
    @Test
    public void decodeEventDataParsesOrderedTickDurationPairs() {
        List<MpcArrangeView.Event> events =
                MpcArrangeView.decodeEventData("0,120,36;240,480,38;960,240,42");
        assertEquals(3, events.size());
        assertEquals(0L, events.get(0).tick);
        assertEquals(120, events.get(0).durationTicks);
        assertEquals(36, events.get(0).noteNumber);
        assertEquals(38, events.get(1).noteNumber);
        assertEquals(960L, events.get(2).tick);
        assertEquals(240, events.get(2).durationTicks);
        assertEquals(42, events.get(2).noteNumber);
    }

    @Test
    public void malformedEntriesAreIgnoredWithoutFailingWholeProjection() {
        List<MpcArrangeView.Event> events =
                MpcArrangeView.decodeEventData("bad;120,abc;240,360");
        assertEquals(1, events.size());
        assertEquals(240L, events.get(0).tick);
        assertTrue(events.get(0).durationTicks > 0);
        assertEquals(-1, events.get(0).noteNumber);
    }
}
