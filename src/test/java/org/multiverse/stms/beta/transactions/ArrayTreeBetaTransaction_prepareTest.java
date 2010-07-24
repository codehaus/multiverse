package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_prepareTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenUnused() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);

        tx.prepare();

        assertPrepared(tx);
    }

    @Test
    public void whenReadIsConflictedByWrite() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        ArrayTreeBetaTransaction otherTx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        tx.prepare(pool);

        assertPrepared(tx);
        assertCommitted(otherTx);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadIsConflictedByLock_thenPrepareSuccess() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        ArrayTreeBetaTransaction otherTx = new ArrayTreeBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        tx.prepare(pool);

        assertPrepared(tx);
        assertActive(otherTx);
        assertLocked(ref);
        assertSame(otherTx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdate() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.prepare(pool);

        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenUpdateAlreadyLockedBySelf() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);
        tx.prepare(pool);

        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenUpdateAndLockedByOther_thenWriteConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        ArrayTreeBetaTransaction otherTx = new ArrayTreeBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertActive(otherTx);

        assertLocked(ref);
        assertSame(otherTx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenOnlyFresh() {

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        tx.openForConstruction(ref, pool);
        tx.prepare();

        assertPrepared(tx);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenConflictingWrite() {
        LongRef ref = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;

        ArrayTreeBetaTransaction otherTx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = otherTx.openForWrite(ref, false, pool);
        conflictingWrite.value++;
        otherTx.commit();

        try {
            tx.prepare(pool);
            fail();
        } catch (WriteConflict expected) {
        }

        assertAborted(tx);
        assertCommitted(otherTx);

        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenAlreadyPrepared() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value++;
        tx.prepare(pool);

        tx.prepare(pool);
        assertPrepared(tx);

        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        try {
            tx.prepare(pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }
}
