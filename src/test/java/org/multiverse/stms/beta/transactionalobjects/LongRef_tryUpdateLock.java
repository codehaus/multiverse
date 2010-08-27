package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Test;
import org.multiverse.stms.beta.orec.OrecTestUtils;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_tryUpdateLock {

    @Test
    public void whenFree() {
        Ref orec = new Ref();
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertTrue(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenFreeAndSurplus() {
        Ref orec = new Ref();
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertTrue(result);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(2, orec);
        OrecTestUtils.assertUpdateBiased(orec);
    }

    @Test
    public void whenLocked() {
        Ref orec = new Ref();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1);
        assertFalse(result);
        OrecTestUtils.assertLocked(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenReadBiasedMode() {
        Ref orec = OrecTestUtils.makeReadBiased(new Ref());

        orec.___arrive(1);
        boolean result = orec.___tryLockAfterNormalArrive(1);

        assertTrue(result);
        OrecTestUtils.assertReadBiased(orec);
        OrecTestUtils.assertLocked(orec);
        OrecTestUtils.assertSurplus(1, orec);
    }

}
