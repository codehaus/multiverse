package org.multiverse.sensors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StripedCounterTest {

    @Test
    public void test() {
        StripedCounter counter = new StripedCounter(10);
        counter.inc(10);

        assertEquals(10, counter.get());
    }

}
