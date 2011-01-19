package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GlobalConflictCounter;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_departAfterUpdateAndUnlockTest {

    private GlobalConflictCounter globalConflictCounter;
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        globalConflictCounter = stm.globalConflictCounter;
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertReadonlyCount(orec, 0);
        assertUpdateBiased(orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);

        long oldConflictCount = globalConflictCounter.count();
        try {
            orec.departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_EXCLUSIVE);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndUnlock();

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 0);
        assertUpdateBiased(orec);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_EXCLUSIVE);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndUnlock();

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(orec, 2);
        assertUpdateBiased(orec);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenWriteLock_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_WRITE);

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {

        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_WRITE);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(orec, 0);
    }

    @Test
    public void whenReadLockAcquired_thenPanicError() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.tryLockAndArrive(1, LOCKMODE_READ);

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {

        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_READ);
        assertReadLockCount(orec, 1);
        assertSurplus(orec, 1);
        assertUpdateBiased(orec);
        assertReadonlyCount(orec, 0);
    }
}
