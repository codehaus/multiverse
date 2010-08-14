package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class UnsafeOrec_tryLockForUpdateTest {

    @Test
    public void whenFree() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);

        boolean result = orec.tryUpdateLock(1);
        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenFreeAndSurplus() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.arrive(1);

        boolean result = orec.tryUpdateLock(1);
        assertTrue(result);
        assertLocked(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        UnsafeOrec orec = new UnsafeOrec();
        orec.arrive(1);
        orec.tryUpdateLock(1);

        boolean result = orec.tryUpdateLock(1);
        assertFalse(result);
        assertLocked(orec);
        assertEquals(1, orec.getSurplus());
        assertFalse(orec.isReadBiased());
    }

    @Test
    public void whenReadBiasedMode() {
        UnsafeOrec orec = makeReadBiased(new UnsafeOrec());

        orec.arrive(1);
        boolean result = orec.tryUpdateLock(1);

        assertTrue(result);
        assertReadBiased(orec);
        assertLocked(orec);
        assertSurplus(1, orec);
    }
}
