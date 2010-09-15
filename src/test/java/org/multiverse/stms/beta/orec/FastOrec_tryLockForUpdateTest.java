package org.multiverse.stms.beta.orec;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FastOrec_tryLockForUpdateTest {

    @Test
    public void whenFree() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertTrue(result);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenFreeAndSurplus() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertTrue(result);
        assertLocked(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertNotProtectedAgainstUpdate(orec);
    }

    @Test
    public void whenLocked() {
        FastOrec orec = new FastOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertFalse(result);
        assertLocked(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
        assertNotProtectedAgainstUpdate(orec);        
    }

    @Test
    public void whenReadBiasedMode() {
        FastOrec orec = makeReadBiased(new FastOrec());

        orec.___arrive(1);
        boolean result = orec.___tryLockAfterNormalArrive(1);

        assertTrue(result);
        assertReadBiased(orec);
        assertLocked(orec);
        assertSurplus(1, orec);
        assertNotProtectedAgainstUpdate(orec);
    }
}
