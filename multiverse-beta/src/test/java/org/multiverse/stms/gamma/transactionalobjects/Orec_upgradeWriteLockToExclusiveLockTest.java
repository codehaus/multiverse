package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.assertLockMode;
import static org.multiverse.stms.gamma.GammaTestUtils.assertSurplus;

public class Orec_upgradeWriteLockToExclusiveLockTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenNotLocked() {
        GammaLongRef ref = new GammaLongRef(stm);

        try {
            ref.upgradeWriteLockToExclusiveLock();
            fail();
        } catch (PanicError expected) {

        }

        assertSurplus(ref, 0);
        assertLockMode(ref, LOCKMODE_NONE);
    }

    @Test
    public void whenReadLocked() {
        GammaLongRef ref = new GammaLongRef(stm);
        ref.tryLockAndArrive(1, LOCKMODE_READ);

        try {
            ref.upgradeWriteLockToExclusiveLock();
            fail();
        } catch (PanicError expected) {

        }

        assertSurplus(ref, 1);
        assertLockMode(ref, LOCKMODE_READ);
    }

    @Test
    public void whenWriteLocked() {
        GammaLongRef ref = new GammaLongRef(stm);
        ref.tryLockAndArrive(1, LOCKMODE_WRITE);

        ref.upgradeWriteLockToExclusiveLock();

        assertSurplus(ref, 1);
        assertLockMode(ref, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void whenExclusiveLocked() {
        GammaLongRef ref = new GammaLongRef(stm);
        ref.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        ref.upgradeWriteLockToExclusiveLock();

        assertSurplus(ref, 1);
        assertLockMode(ref, LOCKMODE_EXCLUSIVE);
    }
}
