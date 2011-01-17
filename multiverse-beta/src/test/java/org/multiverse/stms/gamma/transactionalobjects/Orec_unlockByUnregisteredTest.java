package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_unlockByUnregisteredTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenUpdateBiasedAndNoSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.unlockWhenUnregistered();
            fail();
        } catch (PanicError expected) {

        }

        assertUpdateBiased(orec);
        assertSurplus(orec, 0);
        assertLockMode(orec, LockMode.None);
    }

    @Test
    public void whenUpdateBiasedAndNotLocked_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        try {
            orec.unlockWhenUnregistered();
            fail();
        } catch (PanicError expected) {

        }

        assertUpdateBiased(orec);
        assertSurplus(orec, 1);
        assertLockMode(orec, LockMode.None);
    }

    @Test
    public void readBiased_whenReadLockedAcquiredOnce() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.unlockWhenUnregistered();
        assertLockMode(orec, LockMode.None);
        assertSurplus(orec, 1);
    }

    @Test
    public void readBiased_whenReadLockAcquiredMultipleTimes() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.unlockWhenUnregistered();
        assertLockMode(orec, LockMode.Read);
        assertReadLockCount(orec, 2);
        assertSurplus(orec, 1);
    }

    @Test
    public void readBiased_whenWriteLockAcquired() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.unlockWhenUnregistered();
        assertLockMode(orec, LockMode.None);
        assertSurplus(orec, 1);
    }

    @Test
    public void readBiased_whenCommitLockAcquired() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        orec.unlockWhenUnregistered();

        assertLockMode(orec, LOCKMODE_NONE);
        assertReadBiased(orec);
        assertSurplus(orec, 1);
    }
}
