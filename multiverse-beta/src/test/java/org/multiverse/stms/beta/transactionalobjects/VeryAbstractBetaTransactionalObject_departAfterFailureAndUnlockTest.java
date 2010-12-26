package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_departAfterFailureAndUnlockTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedNotLocked_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);
        try {
            orec.___departAfterFailureAndUnlock();
            fail();
        } catch (PanicError ex) {
        }

        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        BetaTransactionalObject orec = OrecTestUtils.makeReadBiased(newLongRef(stm));

        try {
            orec.___departAfterFailureAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        long result = orec.___departAfterFailureAndUnlock();
        assertEquals(0, result);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        long result = orec.___departAfterFailureAndUnlock();
        assertEquals(1, result);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
    }
}
