package org.multiverse.sensors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StripedCounterTest {

    @Test
    public void testConstruction() {
        StripedCounter counter = new StripedCounter(100);
        assertEquals(0, counter.sum());
    }

    @Test
    public void inc() {
        StripedCounter counter = new StripedCounter(100);
        counter.inc(100, 10);

        assertEquals(10, counter.sum());
    }
}
