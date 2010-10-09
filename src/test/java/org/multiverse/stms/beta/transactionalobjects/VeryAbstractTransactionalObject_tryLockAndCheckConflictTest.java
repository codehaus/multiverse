package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class VeryAbstractTransactionalObject_tryLockAndCheckConflictTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFreeAndPrivatize_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());

        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenFreeAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertReadonlyCount(0, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedBySelfAndPrivatize_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedBySelfAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelfAndPrivatize_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertHasNoUpdateLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelfAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredByOtherAndEnsure_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertFalse(result);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredByOtherAndPrivatize_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertFalse(result);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedByOtherAndEnsure_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertFalse(result);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedByOtherAndPrivatize_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertFalse(result);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenOtherTransactionDidUpdate_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicSet(20);
        long version =ref.getVersion();

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read, true);

        assertFalse(result);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);        
        assertReadonlyCount(0, ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version,20);
    }

    @Test
    public void whenPendingUpdate() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();

        Tranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        //lock it by another thread
        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read2, true);

        assertFalse(result);
        assertHasCommitLock(ref.___getOrec());
        assertSurplus(2, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSame(otherTx, ref.___getLockOwner());
    }

}
