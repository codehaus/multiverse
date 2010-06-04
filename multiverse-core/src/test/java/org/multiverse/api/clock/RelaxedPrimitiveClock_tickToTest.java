package org.multiverse.api.clock;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class RelaxedPrimitiveClock_tickToTest {

    @Test
    public void whenValueTheSame() {
        RelaxedPrimitiveClock clock = new RelaxedPrimitiveClock(100);
        long result = clock.tickTo(100);

        assertEquals(100, result);
        assertEquals(100, clock.getVersion());
    }

    @Test
    public void whenClockAlreadyHasHigherValue(){
         RelaxedPrimitiveClock clock = new RelaxedPrimitiveClock(100);

        long result = clock.tickTo(90);
        assertEquals(100, result);
        assertEquals(100, clock.getVersion());
    }

    @Test
    public void whenClockHasSmallerValue(){
         RelaxedPrimitiveClock clock = new RelaxedPrimitiveClock(100);

        long result = clock.tickTo(110);
        assertEquals(110, result);
        assertEquals(110, clock.getVersion());
    }
}
