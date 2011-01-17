package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_tryLockAndArriveTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void updateBiased_acquireExclusiveLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void updateBiased_acquireExclusiveLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireExclusiveLock_whenExclusiveLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void updateBiased_acquireExclusiveLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void updateBiased_acquireWriteLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenExclusiveLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void updateBiased_acquireWriteLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void updateBiased_acquireReadLock_whenUnlocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    @Test
    public void updateBiased_acquireReadLock_whenWriteLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void updateBiased_acquireReadLock_whenExclusiveLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void updateBiased_acquireReadLock_whenReadLocked() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 2);
    }

    // ====================================================================

    @Test
    public void readBiased_acquireExclusiveLock_whenUnlocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void readBiased_acquireExclusiveLock_whenWriteLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void readBiased_acquireExclusiveLock_whenExclusiveLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void readBiased_acquireExclusiveLock_whenReadLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void readBiased_acquireWriteLock_whenUnlocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void readBiased_acquireWriteLock_whenWriteLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void readBiased_acquireWriteLock_whenExclusiveLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void readBiased_acquireWriteLock_whenReadLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    // =======================================================================

    @Test
    public void readBiased_acquireReadLock_whenUnlocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 1);
    }

    @Test
    public void readBiased_acquireReadLock_whenWriteLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_WRITE);
    }

    @Test
    public void readBiased_acquireReadLock_whenExclusiveLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_EXCLUSIVE);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertLockMode(orec, LOCKMODE_EXCLUSIVE);
    }

    @Test
    public void readBiased_acquireReadLock_whenReadLocked() {
        AbstractGammaObject orec = makeReadBiased(new GammaLongRef(stm));
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        int result = orec.tryLockAndArrive(1, LOCKMODE_READ);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(orec, 1);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertReadLockCount(orec, 2);
    }
}
