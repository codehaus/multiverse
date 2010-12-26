package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static junit.framework.Assert.assertEquals;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;


/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_arriveTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedNotLockedAndNoSurplus_thenNormalArrive() {
        BetaTransactionalObject orec = newLongRef(stm);
        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndNotLockedAndSurplus_thenNormalArrive() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertUpdateBiased(orec);
        assertSurplus(3, orec);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenLockNotFree() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___tryLockAndArrive(1, true);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenUnregisteredArrive() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___tryLockAndArrive(1, false);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_NORMAL, result);
        assertSurplus(2, orec);
        assertReadonlyCount(0, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenReadBiasedAndLockedForCommit() {
        BetaTransactionalObject orec = OrecTestUtils.makeReadBiased(newLongRef(stm));
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1, true);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_LOCK_NOT_FREE, result);
        assertSurplus(1, orec);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertReadBiased(orec);
    }

    @Test
    public void whenReadBiasedAndNoSurplus() {
        BetaTransactionalObject orec = OrecTestUtils.makeReadBiased(newLongRef(stm));

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenReadBiasedAndSurplus_thenCallIgnored() {
        BetaTransactionalObject orec = OrecTestUtils.makeReadBiased(newLongRef(stm));
        orec.___arrive(1);

        int result = orec.___arrive(1);

        assertEquals(ARRIVE_UNREGISTERED, result);
        assertHasNoCommitLock(orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertReadonlyCount(0, orec);
        assertHasNoUpdateLock(orec);
    }
}
