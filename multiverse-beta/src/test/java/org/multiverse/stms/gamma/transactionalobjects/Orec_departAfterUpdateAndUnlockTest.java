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
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
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
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndUnlock();

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        AbstractGammaObject orec = new GammaLongRef(stm);
        orec.arrive(1);
        orec.arrive(1);
        orec.arrive(1);
        orec.tryLockAfterNormalArrive(1, LOCKMODE_COMMIT);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.departAfterUpdateAndUnlock();

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        assertLockMode(orec, LOCKMODE_NONE);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
