package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class VeryAbstractTransactionalObject_hasReadConflictTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadAndNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        boolean hasReadConflict = ref.___hasReadConflict(read);

        assertFalse(hasReadConflict);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenWriteAndNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        boolean hasReadConflict = ref.___hasReadConflict(write);

        assertFalse(hasReadConflict);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenPrivatizedBySelf_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenEnsuredBySelf_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_UPDATE);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenUpdatedByOther_thenConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        //conflicting update
        ref.atomicIncrementAndGet(1);

        boolean hasConflict = ref.___hasReadConflict(read);
        assertTrue(hasConflict);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenFresh() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal tranlocal = tx.openForConstruction(ref);

        boolean conflict = ref.___hasReadConflict(tranlocal);

        assertFalse(conflict);
        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenValueChangedByOtherAndPrivatizedAgain() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        //do the conflicting update
        ref.atomicIncrementAndGet(1);

        //privatize it
        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        ref.privatize(otherTx);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertTrue(hasConflict);
        assertSurplus(2, ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenValueChangedByOtherAndEnsuredAgain() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        //do the conflicting update
        ref.atomicIncrementAndGet(1);

        //ensure it.
        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertTrue(hasConflict);
        assertSurplus(2, ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenUpdateInProgressBecauseLockedByOther() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        boolean hasReadConflict = ref.___hasReadConflict(tranlocal);

        assertTrue(hasReadConflict);
    }

    @Test
    public void whenAlsoReadByOther_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal read = otherTx.openForRead(ref, LOCKMODE_NONE);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(2, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenPendingUpdateByOther_thenNoConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, 200);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRefTranlocal read = otherTx.openForRead(ref, LOCKMODE_NONE);

        boolean hasConflict = ref.___hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(2, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    @Ignore
    public void whenReadBiased() {
    }
}
