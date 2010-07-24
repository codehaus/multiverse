package org.multiverse.stms.beta.transactions;

import org.junit.Assert;
import org.junit.Before;
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
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class ArrayBetaTransaction_openForWriteTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenNull_thenNullPointerException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);

        try {
            tx.openForWrite((LongRef) null, false, pool);
            fail();
        } catch (NullPointerException ex) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        BetaTransactionalObject ref = createLongRef(stm, 0);
        Tranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true), 1);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true), 10);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenOverflowing() {
        BetaTransactionalObject ref1 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref2 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref3 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref4 = StmUtils.createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 3);
        tx.openForWrite(ref1, false, pool);
        tx.openForWrite(ref2, false, pool);
        tx.openForWrite(ref3, false, pool);
        try {
            tx.openForWrite(ref4, false, pool);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
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

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
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
    public void whenAlreadyOpenedForConstruction(){
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 1);
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, false, pool);

        Assert.assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        Assert.assertSame(tx,ref.getLockOwner());
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
        Tranlocal write = tx.openForWrite(ref, true, pool);

        Assert.assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.getOrec());
        Assert.assertSame(tx,ref.getLockOwner());
        assertSurplus(1, ref.getOrec());
        assertUpdateBiased(ref.getOrec());
        assertActive(tx);
        assertNull(ref.unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        LongRef ref1 = createLongRef(stm);
        LongRef ref2 = createLongRef(stm);

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm,10);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        ArrayBetaTransaction conflictingTx = new ArrayBetaTransaction(stm,10);
        conflictingTx.openForWrite(ref1, false, pool).value++;
        conflictingTx.openForWrite(ref2, false, pool).value++;
        conflictingTx.commit(pool);

        try {
            tx.openForWrite(ref2, false, pool);
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

    private void assertOpenedForWrite(BetaTransactionalObject ref, Tranlocal tranlocal) {
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(ref.unsafeLoad(), tranlocal.read);
        assertFalse(tranlocal.isCommitted);
    }

    @Test
    public void whenAlreadyOpenedForReadThenUpgraded() {
        LongRef ref = StmUtils.createLongRef(stm);
        Tranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForRead(ref, false, pool);
        Tranlocal tranlocal = tx.openForWrite(ref, false, pool);

        assertOpenedForWrite(ref, tranlocal);
        assertFalse(ref.getOrec().isLocked());
    }

    @Test
    public void whenLocked_thenReadConflict() {
        LongRef ref = StmUtils.createLongRef(stm);

        BetaTransaction tx1 = stm.start();
        tx1.openForRead(ref, true, pool);

        ArrayBetaTransaction tx2 = new ArrayBetaTransaction(stm, 10);
        try {
            tx2.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx2);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaTransactionalObject ref = StmUtils.createLongRef(stm);

        BetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        Tranlocal write1 = tx.openForWrite(ref, false, pool);
        Tranlocal write2 = tx.openForWrite(ref, false, pool);

        assertSame(write1, write2);
        assertActive(tx);
    }

    @Test
    public void multipleOpenForWrites() {
        BetaTransactionalObject ref1 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref2 = StmUtils.createLongRef(stm);
        BetaTransactionalObject ref3 = StmUtils.createLongRef(stm);

        BetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        Tranlocal write1 = tx.openForWrite(ref1, false, pool);
        Tranlocal write2 = tx.openForWrite(ref2, false, pool);
        Tranlocal write3 = tx.openForWrite(ref3, false, pool);

        assertActive(tx);
        assertOpenedForWrite(ref1, write1);
        assertOpenedForWrite(ref2, write2);
        assertOpenedForWrite(ref3, write3);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        ArrayBetaTransaction tx = new ArrayBetaTransaction(config, 10);
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
        ArrayBetaTransaction tx = new ArrayBetaTransaction(config, 10);
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
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.prepare(pool);

        LongRef ref = createLongRef(stm);
        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertPrepared(tx);
    }

    @Test
    public void whenUpdatingReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.openForWrite(ref, false, pool);

        assertSame(committed, ref.unsafeLoad());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort(pool);

        LongRef ref = createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.commit(pool);

        LongRef ref = createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
