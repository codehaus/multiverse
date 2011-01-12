package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_tryLockAfterNormalArriveTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void updateBiased_acquireCommitLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        assertTrue(result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void updateBiased_acquireCommitLock_whenNoSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);
            fail();
        } catch (PanicError expected) {

        }

        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void updateBiased_acquireCommitLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        orec.arrive(1);
        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireCommitLock_whenCommitLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void updateBiased_acquireCommitLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void updateBiased_acquireWriteLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        assertTrue(result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenNoSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);
            fail();
        } catch (PanicError expected) {

        }

        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenCommitLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void updateBiased_acquireReadLock_whenNoSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);
            fail();
        } catch (PanicError expected) {

        }

        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void updateBiased_acquireReadLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);

        assertTrue(result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    @Test
    public void updateBiased_acquireReadLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireReadLock_whenCommitLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_COMMIT);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);

        assertFalse(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_COMMIT);
    }

    @Test
    public void updateBiased_acquireReadLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        boolean result = orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);

        assertTrue(result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 2);
    }

    // ====================================================================

    @Test
    public void readBiased_acquireCommitLock_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 0);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void readBiased_acquireWriteLock_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_WRITE);
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 0);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }

    @Test
    public void readBiased_acquireReadLock_thenPanicError() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        try {
            orec.tryLockAfterNormalArrive(1, LOCKMODE_READ);
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(orec, 0);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_NONE);
    }
}
