package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.arbitraryUpdate;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class LongRef_tryLockAndCheckConflictTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenFree() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write = tx.openForRead(ref, false, pool);

        boolean result = ref.tryLockAndCheckConflict(tx, 1, write);

        assertTrue(result);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
    }

    @Test
    public void whenConflict() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = otherTx.openForWrite(ref, false, pool);
        write.value++;
        otherTx.commit();

        boolean result = ref.tryLockAndCheckConflict(tx, 1, read);

        assertFalse(result);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref);
        assertSame(write, ref.unsafeLoad());
    }

    @Test
    public void whenLockedByOtherAndUpdated() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        arbitraryUpdate(stm, ref);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransaction lockingTx = stm.startDefaultTransaction();
        lockingTx.openForWrite(ref, true, pool);

        boolean result = ref.tryLockAndCheckConflict(tx, 1, read);
        assertFalse(result);
        assertSame(lockingTx, ref.getLockOwner());
        assertLocked(ref);
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(1, ref);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenPendingUpdate() {
        LongRef ref = createLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();

        Tranlocal read2 = tx.openForRead(ref, false, pool);

        //lock it by another thread
        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true, pool);

        boolean result = ref.tryLockAndCheckConflict(tx, 1, read2);

        assertFalse(result);
        assertLocked(ref.getOrec());
        assertSurplus(2, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(otherTx, ref.getLockOwner());
    }

    @Test
    public void whenAlreadyLockedBySelf() {
        LongRef ref = createLongRef(stm, 0);

        //lock it by this thread.
        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, true, pool);

        boolean result = ref.tryLockAndCheckConflict(tx, 1, read);

        assertTrue(result);
        assertLocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
    }
}
