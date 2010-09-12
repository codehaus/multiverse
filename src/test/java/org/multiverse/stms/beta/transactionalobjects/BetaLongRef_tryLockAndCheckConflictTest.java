package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.arbitraryUpdate;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_tryLockAndCheckConflictTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenFree() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, false);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, write);

        assertTrue(result);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
    }

    @Test
    public void whenConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = otherTx.openForWrite(ref, false);
        write.value++;
        otherTx.commit();

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read);

        assertFalse(result);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref);
        assertSame(write, ref.___unsafeLoad());
    }

    @Test
    public void whenLockedByOtherAndUpdated() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read = tx.openForRead(ref, false);

        arbitraryUpdate(stm, ref);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction lockingTx = stm.startDefaultTransaction();
        lockingTx.openForWrite(ref, true);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read);
        assertFalse(result);
        assertSame(lockingTx, ref.___getLockOwner());
        assertLocked(ref);
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(1, ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPendingUpdate() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();

        Tranlocal read2 = tx.openForRead(ref, false);

        //lock it by another thread
        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read2);

        assertFalse(result);
        assertLocked(ref.___getOrec());
        assertSurplus(2, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenAlreadyLockedBySelf() {
        BetaLongRef ref = newLongRef(stm, 0);

        //lock it by this thread.
        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, true);

        boolean result = ref.___tryLockAndCheckConflict(tx, 1, read);

        assertTrue(result);
        assertLocked(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
    }
}
