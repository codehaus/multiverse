package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayTreeBetaTransaction_openForReadTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenNull() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        Tranlocal result = tx.openForRead((LongRef) null, true, pool);
        assertNull(result);
        assertActive(tx);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 10);
        Tranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref.getOrec());
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertSame(ref, read.owner);
        assertTrue(committed.isCommitted);
        assertFalse(committed.isPermanent);
        assertEquals(10, read.value);
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        LongRef ref = createLongRef(stm, 10);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, false, pool);
        LongRefTranlocal read2 = tx.openForRead(ref, false, pool);

        assertSame(read1, read2);
        assertUnlocked(ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertSurplus(1, ref.getOrec());
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm, 10);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.getLockOwner());
    }

    @Test
    public void whenLockedByOther_thenLockedConflict() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction otherTx = stm.start();
        otherTx.openForRead(ref, true, pool);

        ArrayTreeBetaTransaction tx2 = new ArrayTreeBetaTransaction(stm);
        try {
            tx2.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx2);
        assertSame(committed, ref.unsafeLoad());
        assertLocked(ref.getOrec());
        assertReadonlyCount(0, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertSame(otherTx, ref.getLockOwner());
    }

    @Test
    public void whenLock() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(committed, read);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyLockedBySelf_thenNoProblem() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, true, pool);
        LongRefTranlocal read2 = tx.openForRead(ref, true, pool);

        assertActive(tx);
        assertSame(read1, read2);
        assertSame(committed, read2);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenPessimisticRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new ArrayTreeBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        ArrayTreeBetaTransaction conflictingTx = new ArrayTreeBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, false, pool).value++;
        conflictingTx.openForWrite(ref2, false, pool).value++;
        conflictingTx.commit(pool);

        try {
            tx.openForRead(ref2, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);

        assertUnlocked(ref1);
        assertNull(ref1.getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertUnlocked(ref2);
        assertNull(ref2.getLockOwner());
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void whenPessimisticWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new ArrayTreeBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref, pool);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(construction, read);
        assertLocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.getLockOwner());
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenConstructedAndLock() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref, pool);

        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(construction, read);
        assertNull(ref.unsafeLoad());
        assertLocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.getLockOwner());
    }


    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        LongRef ref = StmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort(pool);

        LongRef ref = StmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit(pool);

        LongRef ref = StmUtils.createLongRef(stm);

        try {
            tx.openForRead(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
