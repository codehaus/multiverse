package org.multiverse.utils.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class StrictClockTest {

    @Test
    public void testNoArgConstructor() {
        StrictClock clock = new StrictClock();
        assertEquals(0, clock.getVersion());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeClock() {
        new StrictClock(-1);
    }

    @Test
    public void testTick() {
        StrictClock clock = new StrictClock();
        long old = clock.getVersion();
        long returned = clock.tick();
        assertEquals(old + 1, clock.getVersion());
        assertEquals(returned, clock.getVersion());
    }

    @Test
    public void testToString() {
        StrictClock clock = new StrictClock();
        clock.tick();

        assertEquals("StrictClock(time=1)", clock.toString());
    }
}
