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
public class VeryAbstractBetaTransactionalObject_departAfterReadingTest {

    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenNoSurplus_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___departAfterReading();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenMuchSurplus() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        orec.___departAfterReading();

        assertSurplus(1, orec);
        assertReadonlyCount(1, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);

    }

    @Test
    public void whenLockedForCommit() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        orec.___departAfterReading();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenLockedForUpdate() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, false);

        orec.___departAfterReading();

        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit_thenPanicError() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        try {
            orec.___departAfterReading();
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
    public void whenReadBiasedAndUnlocked_thenPanicError() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        try {
            orec.___departAfterReading();
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
