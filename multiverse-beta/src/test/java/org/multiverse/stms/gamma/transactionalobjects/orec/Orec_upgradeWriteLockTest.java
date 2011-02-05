package org.multiverse.stms.gamma.transactionalobjects.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertOrecValue;
import static org.multiverse.stms.gamma.GammaTestUtils.assertLockMode;
import static org.multiverse.stms.gamma.GammaTestUtils.assertSurplus;

public class Orec_upgradeWriteLockTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void updateBiased_whenNotLocked() {
        GammaLongRef orec = new GammaLongRef(stm);
        long orecValue = orec.orec;

        try {
            orec.upgradeWriteLock();
            fail();
        } catch (PanicError expected) {

        }

        assertOrecValue(orec, orecValue);
    }

    @Test
    public void updateBiased_whenReadLocked() {
        GammaLongRef orec = new GammaLongRef(stm);
        orec.arriveAndLock(1, LOCKMODE_READ);
        long orecValue = orec.orec;

        try {
            orec.upgradeWriteLock();
            fail();
        } catch (PanicError expected) {
        }

        assertOrecValue(orec, orecValue);
    }

    @Test
    public void updateBiased_whenWriteLocked() {
        GammaLongRef orec = new GammaLongRef(stm);
        orec.arriveAndLock(1, LOCKMODE_WRITE);

        orec.upgradeWriteLock();

        assertSurplus(orec, 1);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void updateBiased_whenExclusiveLocked() {
        GammaLongRef orec = new GammaLongRef(stm);
        orec.arriveAndLock(1, LOCKMODE_EXCLUSIVE);

        orec.upgradeWriteLock();

        assertSurplus(orec, 1);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }
}
