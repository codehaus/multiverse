package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_tryLockAndArriveTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedAndAlreadyLockedForCommit() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndUnlocked() {
        BetaTransactionalObject orec = newLongRef(stm);

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        BetaTransactionalObject orec = makeReadBiased(newLongRef(stm));

        int result = orec.___tryLockAndArrive(1, true);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }
}
