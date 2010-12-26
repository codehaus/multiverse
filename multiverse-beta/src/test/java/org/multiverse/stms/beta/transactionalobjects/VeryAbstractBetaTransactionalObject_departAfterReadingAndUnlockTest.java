package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public class VeryAbstractBetaTransactionalObject_departAfterReadingAndUnlockTest {

    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenMultipleArrivesAndLockedForCommit() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(2);
        orec.___tryLockAndArrive(1, true);

        orec.___departAfterReadingAndUnlock();

        assertSurplus(2, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenMultipleArrivesAndLockedForUpdate() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(2);
        orec.___tryLockAndArrive(1, false);

        orec.___departAfterReadingAndUnlock();

        assertSurplus(2, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenSuccess() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___tryLockAndArrive(1, true);

        orec.___departAfterReadingAndUnlock();
        assertSurplus(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(1, orec);
    }

    @Test
    public void whenLockedAndReadBiased() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___tryLockAndArrive(1, true);

        try {
            orec.___departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(1, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndNoSurplus_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNotLockedAndSurplus_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);

        try {
            orec.___departAfterReadingAndUnlock();
            fail();
        } catch (PanicError expected) {
        }

        assertSurplus(1, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
