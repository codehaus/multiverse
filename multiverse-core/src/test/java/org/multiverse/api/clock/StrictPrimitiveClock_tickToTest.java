package org.multiverse.api.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class StrictPrimitiveClock_tickToTest {
    
     @Test
    public void whenValueTheSame() {
        StrictPrimitiveClock clock = new StrictPrimitiveClock(100);
        long result = clock.tickTo(100);

        assertEquals(100, result);
        assertEquals(100, clock.getVersion());
    }

    @Test
    public void whenClockAlreadyHasHigherValue(){
         StrictPrimitiveClock clock = new StrictPrimitiveClock(100);

        long result = clock.tickTo(90);
        assertEquals(100, result);
        assertEquals(100, clock.getVersion());
    }

    @Test
    public void whenClockHasSmallerValue(){
         StrictPrimitiveClock clock = new StrictPrimitiveClock(100);

        long result = clock.tickTo(110);
        assertEquals(110, result);
        assertEquals(110, clock.getVersion());
    }
}
