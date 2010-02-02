package org.multiverse.utils.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class StrictPrimitiveClockTest {

    @Test
    public void testNoArgConstructor() {
        StrictPrimitiveClock clock = new StrictPrimitiveClock();
        assertEquals(0, clock.getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeClock() {
        new StrictPrimitiveClock(-1);
    }

    @Test
    public void testTick() {
        StrictPrimitiveClock clock = new StrictPrimitiveClock();
        long old = clock.getVersion();
        long returned = clock.tick();
        assertEquals(old + 1, clock.getVersion());
        assertEquals(returned, clock.getVersion());
    }

    @Test
    public void testToString() {
        StrictPrimitiveClock clock = new StrictPrimitiveClock();
        clock.tick();

        assertEquals("StrictPrimitiveClock(time=1)", clock.toString());
    }
}
