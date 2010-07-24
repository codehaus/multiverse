package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;


/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_arriveTest {

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        boolean result = orec.arrive(1);

        assertTrue(result);
        assertSurplus(1, orec);
        assertUnlocked(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.arrive(1);

        boolean result = orec.arrive(1);

        assertTrue(result);
        assertSurplus(3, orec);
        assertReadonlyCount(0, orec);
        assertUnlocked(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked_thenLockedConflict() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arrive(1);

        assertFalse(result);
        assertSurplus(1, orec);
        assertLocked(orec);
    }

    @Test
    public void whenReadBiasedAndLocked_thenLockConflict() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.arrive(1);

        assertFalse(result);
        assertSurplus(1, orec);
        assertLocked(orec);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());

        boolean result = orec.arrive(1);

        assertTrue(result);
        assertUnlocked(orec);
        assertSurplus(1, orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());
        orec.arrive(1);

        boolean result = orec.arrive(1);

        assertTrue(result);
        assertUnlocked(orec);
        assertSurplus(1, orec);
    }
}
