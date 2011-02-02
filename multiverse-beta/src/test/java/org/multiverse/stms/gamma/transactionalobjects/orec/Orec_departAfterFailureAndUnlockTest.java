package org.multiverse.stms.gamma.transactionalobjects.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.AbstractGammaObject;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterFailureAndUnlockTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }


    @Test
    public void whenUpdateBiasedNotLocked_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        try {
            orec.departAfterFailureAndUnlock();
            fail();
        } catch (PanicError ex) {
        }

        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.departAfterFailureAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertReadBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndHasMultipleReadLocks() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        long result = orec.departAfterFailureAndUnlock();

        assertEquals(1, result);
        assertLockMode(orec, LOCKMODE_READ);
        assertReadLockCount(orec, 1);
        assertSurplus(orec, 1);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenUpdateBiasedAndHasSingleReadLock() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        long result = orec.departAfterFailureAndUnlock();

        assertEquals(0, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
        assertReadLockCount(orec, 0);
    }

    @Test
    public void whenUpdateBiasedAndHasWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        long result = orec.departAfterFailureAndUnlock();
        assertEquals(0, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
        assertReadLockCount(orec, 0);
    }

    @Test
    public void whenUpdateBiasedAndHasExclusiveLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        long result = orec.departAfterFailureAndUnlock();
        assertEquals(0, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
        assertReadLockCount(orec, 0);
    }
}
