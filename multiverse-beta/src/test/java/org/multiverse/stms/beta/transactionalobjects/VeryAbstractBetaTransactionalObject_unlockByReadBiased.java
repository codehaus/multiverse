package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public class VeryAbstractBetaTransactionalObject_unlockByReadBiased {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenSurplusAndReadBiasedAndLocked() {
        BetaTransactionalObject orec = OrecTestUtils.makeReadBiased(newLongRef(stm));
        orec.___tryLockAndArrive(1, true);

        orec.___unlockByReadBiased();

        assertHasNoCommitLock(orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertSurplus(1, orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenSurplusAndUpdateBiasedAndLocked_thenPanicError() {

    }

    @Test
    @Ignore
    public void whenNoSurplusAndReadBiasedAndUnlocked_thenPanicError() {

    }

    @Test
    @Ignore
    public void whenNoSurplusAndUpdateBiasedAndUnlocked_thenPanicError() {

    }
}
