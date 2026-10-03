package com.miguelduval.mpcmk2groovebox;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MpcArrangeViewTest {
    @Test
    public void decodeEventDataParsesOrderedTickDurationPairs() {
        List<MpcArrangeView.Event> events =
                MpcArrangeView.decodeEventData("0,120;240,480;960,240");
        assertEquals(3, events.size());
        assertEquals(0L, events.get(0).tick);
        assertEquals(120, events.get(0).durationTicks);
        assertEquals(960L, events.get(2).tick);
        assertEquals(240, events.get(2).durationTicks);
    }

    @Test
    public void malformedEntriesAreIgnoredWithoutFailingWholeProjection() {
        List<MpcArrangeView.Event> events =
                MpcArrangeView.decodeEventData("bad;120,abc;240,360");
        assertEquals(1, events.size());
        assertEquals(240L, events.get(0).tick);
        assertTrue(events.get(0).durationTicks > 0);
    }
}
