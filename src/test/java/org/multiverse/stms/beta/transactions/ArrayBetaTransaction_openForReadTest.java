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
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayBetaTransaction_openForReadTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    @Ignore
    public void whenUnstarted(){

    }

    @Test
    public void whenNullRef() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        Tranlocal read = tx.openForRead((LongRef) null, false, pool);

        assertNull(read);
        assertActive(tx);
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);
        LongRef ref3 = createLongRef(stm);
        LongRef ref4 = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 3);
        tx.openForRead(ref1, false, pool);
        tx.openForRead(ref2, false, pool);
        tx.openForRead(ref3, false, pool);
        try {
            tx.openForRead(ref4, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertUnlocked(ref);
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(committed, ref.unsafeLoad());
        assertNull(ref.getLockOwner());
        assertActive(tx);
    }

    @Test
    public void whenReadBiasedAndLock() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertReadBiased(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertSame(tx, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);
        write.value = 100;
        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNoReadConflict() {
        LongRef ref = createLongRef(stm,100);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        ArrayBetaTransaction conflictingTx = new ArrayBetaTransaction(stm,1);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, false,pool);
        conflictingWrite.value++;
        conflictingTx.commit(pool);

        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForRead_thenNoReadConflict() {
        LongRef ref = createLongRef(stm,100);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRefTranlocal read1 = tx.openForRead(ref, false, pool);

        ArrayBetaTransaction conflictingTx = new ArrayBetaTransaction(stm,1);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, false,pool);
        conflictingWrite.value++;
        conflictingTx.commit(pool);

        Tranlocal read2 = tx.openForRead(ref, false, pool);

        assertSame(read1, read2);
        assertUnlocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertSame(conflictingWrite, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForConstruction(){
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, false, pool);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        assertSame(tx,ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndLock(){
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, true, pool);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        assertSame(tx,ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenLockedByOther_thenReadConflict() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction other = new ArrayBetaTransaction(stm, 1);
        other.openForRead(ref, true, pool);

        int oldReadonlyCount = ref.getReadonlyCount();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertLocked(ref.getOrec());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertReadonlyCount(oldReadonlyCount, ref);
        assertAborted(tx);
        assertSame(other, ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 2);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        ArrayBetaTransaction conflictingTx = new ArrayBetaTransaction(stm, 2);
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
    @Ignore
    public void whenUnrealReadConflict() {
    }

    @Test
    public void multipleOpenForReads() {
        BetaTransactionalObject ref1 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref2 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref3 = StmUtils.createLongRef(stm);

        BetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        Tranlocal read1 = tx.openForRead(ref1, false, pool);
        Tranlocal read2 = tx.openForRead(ref2, false, pool);
        Tranlocal read3 = tx.openForRead(ref3, false, pool);

        assertActive(tx);

        assertSame(read1, ref1.unsafeLoad());
        assertSame(read2, ref2.unsafeLoad());
        assertSame(read3, ref3.unsafeLoad());
    }

    @Test
    public void whenPessimisticRead() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new ArrayBetaTransaction(config, 10);
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
    public void whenPessimisticWrite() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new ArrayBetaTransaction(config, 10);
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
    public void whenPrepared_thenPreparedException() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.prepare(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        LongRef ref = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.commit(pool);

        try {
            tx.openForRead(ref, false, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
