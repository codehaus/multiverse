package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
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
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);

        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenFreeAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertReadonlyCount(0, ref);
        assertRefHasUpdateLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedBySelfAndPrivatize_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedBySelfAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelfAndPrivatize_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertTrue(result);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelfAndEnsure_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertTrue(result);
        assertSurplus(1, ref);
        assertRefHasUpdateLock(ref,tx);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredByOtherAndEnsure_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertFalse(result);
        assertRefHasUpdateLock(ref,otherTx);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyEnsuredByOtherAndPrivatize_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertFalse(result);
        assertRefHasUpdateLock(ref,otherTx);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedByOtherAndEnsure_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, false);

        assertFalse(result);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyPrivatizedByOtherAndPrivatize_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaTranlocal write = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write, true);

        assertFalse(result);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenOtherTransactionDidUpdate_thenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicSet(20);
        long version = ref.getVersion();

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read, true);

        assertFalse(result);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version, 20);
    }

    @Test
    public void whenPendingUpdate() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();

        BetaTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        //lock it by another thread
        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read2, true);

        assertFalse(result);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(2, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
    }

}
