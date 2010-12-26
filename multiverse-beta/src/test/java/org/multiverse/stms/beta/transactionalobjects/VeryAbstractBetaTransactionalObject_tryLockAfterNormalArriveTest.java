package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractBetaTransactionalObject_tryLockAfterNormalArriveTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUpdateBiasedAndNoSurplus_thenPanicErrorWhenCommitLockAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___tryLockAfterNormalArrive(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndNoSurplus_thenPanicErrorWhenUpdateLockAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);

        try {
            orec.___tryLockAfterNormalArrive(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFree_thenCommitLockCanBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFree_thenUpdateLockCanBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertUpdateBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFreeAndSurplus_thenCommitLockCanBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndFreeAndSurplus_thenUpdateLockCanBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertHasUpdateLock(orec);
        assertSurplus(2, orec);
        assertUpdateBiased(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenCommitLockCantBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForCommit_thenUpdateLockCantBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasNoUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenCommitLockCantBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    public void whenUpdateBiasedAndLockedForUpdate_thenUpdateLockCantBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    /*
    @Test
    @Ignore
    public void whenReadBiasedAndNoSurplus_thenPanicErrorWhenCommitLockAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec(stm));

        try {
            orec.___tryLockAfterNormalArrive(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndNoSurplus_thenPanicErrorWhenUpdateLockAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec(stm));

        try {
            orec.___tryLockAfterNormalArrive(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertReadonlyCount(0, orec);
        assertSurplus(0, orec);
        assertReadBiased(orec);
        assertHasNoUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndFree_thenCommitLockCanBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec(stm));
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertHasUpdateLock(orec);
        assertHasCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndFree_thenUpdateLockCanBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec(stm));
        orec.___arrive(1);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertTrue(result);
        assertReadonlyCount(0, orec);
        assertSurplus(1, orec);
        assertReadBiased(orec);
        assertHasUpdateLock(orec);
        assertHasNoCommitLock(orec);
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForCommit_thenCommitLockCantBeAcquired() {
        FastOrec orec = makeReadBiased(new FastOrec(stm));

        orec.___tryLockAndArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForCommit_thenUpdateLockCantBeAcquired() {
        FastOrec orec = new FastOrec(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, true);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForUpdate_thenCommitLockCantBeAcquired() {
        FastOrec orec = new FastOrec(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, true);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    }

    @Test
    @Ignore
    public void whenReadBiasedAndLockedForUpdate_thenUpdateLockCantBeAcquired() {
        BetaTransactionalObject orec = newLongRef(stm);
        orec.___arrive(1);
        //conflicting lock
        orec.___tryLockAfterNormalArrive(1, false);

        boolean result = orec.___tryLockAfterNormalArrive(1, false);

        assertFalse(result);
        assertReadonlyCount(0, orec);
        assertHasNoCommitLock(orec);
        assertHasUpdateLock(orec);
        assertEquals(1, orec.___getSurplus());
        assertFalse(orec.___isReadBiased());
    } */

}
