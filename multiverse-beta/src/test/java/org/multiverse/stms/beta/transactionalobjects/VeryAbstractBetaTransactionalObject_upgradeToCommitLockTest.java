package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public class VeryAbstractBetaTransactionalObject_upgradeToCommitLockTest {

    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedAlreadyUpgradedToCommitLock() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenUpdateBiasedUpdateLockAlreadyAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenMultipleSurplusAndUpdateBiasedAlreadyUpgradedToCommitLock() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(3, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenMultipleSurplusAndUpdateBiasedUpdateLockAlreadyAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(3, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenSurplusAndUpdateBiasedNoLockAcquired_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenNoSurplusAndUpdateBiasedNoLockAcquired_thenPanicError() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndNoLocks_thenPanicError() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        try {
            orec.___upgradeToCommitLock();
            fail();
        } catch (PanicError expected) {
        }

        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndAlreadyCommitLock() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___tryLockAndArrive(1, true);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }

    @Test
    public void whenReadBiasedAndAlreadyUpdateLock() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___tryLockAndArrive(1, false);

        orec.___upgradeToCommitLock();

        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
    }
}
