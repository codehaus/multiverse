package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterReadingAndUnlockTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void updateBiased_whenSingleReadLock() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 0);
        assertReadLockCount(orec, 0);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void updateBiased_whenMultipleReadLocks() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 2);
        assertReadLockCount(orec, 2);
        assertLockMode(orec, LOCKMODE_READ);
    }

    @Test
    public void updateBiased_whenWriteLock() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 0);
        assertReadLockCount(orec, 0);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void updateBiased_whenExclusiveLock() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 0);
        assertReadLockCount(orec, 0);
        assertLockMode(orec, LOCKMODE_NONE);
    }


    @Test
    public void updateBiased_whenMultipleArrivesAndLockedForCommit() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(2);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 2);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void updateBiased_whenMultipleArrivesAndLockedForWrite() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(2);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.departAfterReadingAndUnlock();

        assertSurplus(orec, 2);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void updateBiased_whenNotLockedAndNoSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 0);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }


    @Test
    public void whenLockedAndReadBiased() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 1);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        try {
            orec.departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 1);
        assertLockMode(orec, LOCKMODE_NONE);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
