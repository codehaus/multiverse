package org.multiverse.stms.beta.transactions;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MonoBetaTransaction_openForWriteTest {
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
    public void whenNotCommittedBefore_thenReadConflict() {
        LongRef ref = new LongRef();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = createLongRef(stm);
        BetaTransactionalObject ref2 = createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForRead(ref1, false, pool);
        try {
            tx.openForWrite(ref2, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertUnlocked(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenReadBiasedAndLock() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertActive(tx);
        assertSame(tx, ref.getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertLocked(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNull(ref.getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertUnlocked(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertActive(tx);
        assertSame(tx, ref.getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertLocked(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotNull(write);
        assertNotSame(read, write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {        
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx,ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenConstructedAndLock() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx,ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenOpenForWriteAndLock() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, false, pool);
        LongRefTranlocal write2 = tx.openForWrite(ref, false, pool);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForReadAndLocked() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true, pool);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWriteAndLocked() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, true, pool);
        LongRefTranlocal write2 = tx.openForWrite(ref, false, pool);

        assertSame(write2, write1);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForReadAndLock() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertNotNull(write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWriteAndLock() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, false, pool);
        LongRefTranlocal write2 = tx.openForWrite(ref, true, pool);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenRepeatedLockThenNoProblem() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, true, pool);
        LongRefTranlocal write2 = tx.openForWrite(ref, true, pool);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
    }

    @Test
    public void whenLockedByOtherAfterOpenedAndLockRequired() {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        MonoBetaTransaction otherTx = new MonoBetaTransaction(stm);
        otherTx.openForWrite(ref, true, pool);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (ReadConflict e) {

        }

        assertSame(otherTx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenLockedByOtherAndNoLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(false);
    }

    @Test
    public void whenLockedByOtherAndLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(true);
    }

    public void whenLockedByOther_thenReadConflict(boolean lockNeeded) {
        LongRef ref = createLongRef(stm, 100);

        MonoBetaTransaction otherTx = new MonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        try {
            tx.openForWrite(ref, lockNeeded, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertSame(otherTx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        LongRef ref = createLongRef(stm, 0);
        Tranlocal committed = ref.unsafeLoad();

        MonoBetaTransaction tx = new MonoBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true));
        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        LongRef ref = createLongRef(stm, 0);

        MonoBetaTransaction tx = new MonoBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true));
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);

        try {
            tx.openForWrite((LongRef) null, true, pool);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        BetaTransaction tx = new MonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        Assert.assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new MonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        Assert.assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = StmUtils.createLongRef(stm);

        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.abort(pool);

        BetaTransactionalObject ref = StmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        MonoBetaTransaction tx = new MonoBetaTransaction(stm);
        tx.commit(pool);

        BetaTransactionalObject ref = StmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
