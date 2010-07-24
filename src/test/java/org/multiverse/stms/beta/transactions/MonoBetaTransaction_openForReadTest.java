package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
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
public class MonoBetaTransaction_openForReadTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void whenNew(){

    }

    @Test
    public void whenNull() {
        BetaTransaction tx = new MonoBetaTransaction(stm);
        Tranlocal result = tx.openForRead((LongRef) null, true, pool);
        assertNull(result);
        assertActive(tx);
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = StmUtils.createLongRef(stm);
        LongRef ref2 = StmUtils.createLongRef(stm);

        BetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref1, false, pool);
        try {
            tx.openForRead(ref2, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertAborted(tx);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 10);
        Tranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.getLockOwner());
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref, pool);

        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(construction, read);
        assertNull(ref.unsafeLoad());
        assertLocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx,ref.getLockOwner());
    }

    @Test
    public void whenLockedByOther_thenLockedConflict() {
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        BetaTransaction otherTx = stm.start();
        otherTx.openForRead(ref, true, pool);

        MonoBetaTransaction tx2 = new MonoBetaTransaction(stm);
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
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        LongRef ref = createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
    public void whenPessimisticReadEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new MonoBetaTransaction(config);
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
    public void whenPessimisticWriteEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new MonoBetaTransaction(config);
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
    public void whenPrepared_thenPreparedTransactionException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
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
