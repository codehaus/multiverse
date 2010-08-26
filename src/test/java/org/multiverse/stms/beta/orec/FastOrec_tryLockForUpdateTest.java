package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_tryLockForUpdateTest {

    @Test
    public void whenFree() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterArrive(1);
        assertTrue(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenFreeAndSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterArrive(1);
        assertTrue(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(2, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterArrive(1);

        boolean result = orec.___tryLockAfterArrive(1);
        assertFalse(result);
        OrecTestUtils.assertLocked(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenReadBiasedMode() {
        FastOrec orec = OrecTestUtils.makeReadBiased(new FastOrec());

        orec.___arrive(1);
        boolean result = orec.___tryLockAfterArrive(1);

        assertTrue(result);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }
}
