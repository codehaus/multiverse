package org.multiverse.utils.clock;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class RelaxedPrimitiveClockTest {

    @Test
    public void testConstructor(){
        RelaxedPrimitiveClock clock  = new RelaxedPrimitiveClock();
        assertEquals(0, clock.getVersion());
    }

    @Test
    public void testTick(){
        RelaxedPrimitiveClock clock = new RelaxedPrimitiveClock();
        long old = clock.getVersion();
        long returned = clock.tick();
        assertEquals(old+1, clock.getVersion());
        assertEquals(returned, clock.getVersion());
    }

    @Test
    public void testToString(){
        RelaxedPrimitiveClock clock = new RelaxedPrimitiveClock();
        //make sure that the toString function makes use of the time and not of the dawn.
        clock.tick();

        assertEquals("RelaxedPrimitiveClock(time=1)", clock.toString());
    }
}
