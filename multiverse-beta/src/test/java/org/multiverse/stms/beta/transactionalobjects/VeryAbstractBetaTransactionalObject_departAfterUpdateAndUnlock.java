package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_departAfterUpdateAndUnlock {

    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        globalConflictCounter = stm.globalConflictCounter;
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);

        long oldConflictCount = globalConflictCounter.count();

        try {
            orec.___departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        long oldConflictCount = globalConflictCounter.count();
        try {
            orec.___departAfterUpdateAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertHasNoCommitLock(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenLockedAndNoAdditionalSurplus() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndUnlock();

        assertEquals(0, result);
        assertEquals(oldConflictCount, globalConflictCounter.count());
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenLockedAndAdditionalSurplus() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        long oldConflictCount = globalConflictCounter.count();

        long result = orec.___departAfterUpdateAndUnlock();

        assertEquals(oldConflictCount + 1, globalConflictCounter.count());
        assertEquals(2, result);
        assertHasNoCommitLock(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }
}
