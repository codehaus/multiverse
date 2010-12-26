package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_departAfterFailureTest {

    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedAndNoSurplusAndNotLocked_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndNotLocked() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);

        orec.___departAfterFailure();

        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLockedForCommit() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        orec.___departAfterFailure();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedAndSurplusAndLockedForUpdate() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, false);

        orec.___departAfterFailure();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertReadonlyCount(0, orec);
    }


    @Test
    public void whenReadBiasedAndLockedForCommit_thenPanicError() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndNotLocked_thenPanicError() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        try {
            orec.___departAfterFailure();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }
}
