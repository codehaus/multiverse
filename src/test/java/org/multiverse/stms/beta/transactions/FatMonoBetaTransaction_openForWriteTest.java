package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.*;
import org.multiverse.functions.IncLongFunction;
import org.multiverse.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.refs.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_openForWriteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenNew() {

    }

    @Test
    public void whenNotCommittedBefore_thenReadConflict() {
        BetaTransaction otherTx = stm.startDefaultTransaction();
        LongRef ref = new LongRef(otherTx);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenOverflowing() {
        LongRef ref1 = BetaStmUtils.createLongRef(stm);
        LongRef ref2 = BetaStmUtils.createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref1, false, pool);
        try {
            tx.openForWrite(ref2, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertAborted(tx);
        assertEquals(2, config.getSpeculativeConfig().getMinimalLength());
    }

    @Test
    public void whenReadBiased() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenReadBiasedAndLock() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenUpdateBiased() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.unsafeLoad();
        int oldReadonlyCount = ref.getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
        assertAttached(tx, write);
    }

    @Test
    public void whenConstructedAndLock() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
        assertAttached(tx, write);
    }

    @Test
    public void whenOpenForWriteAndLock() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write2);
    }

    @Test
    public void whenAlreadyOpenedForReadAndLocked() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenAlreadyOpenedForWriteAndLocked() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write1);
    }

    @Test
    public void whenAlreadyOpenedForReadAndLock() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write);
    }

    @Test
    public void whenAlreadyOpenedForWriteAndLock() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write2);
    }

    @Test
    public void whenRepeatedLockThenNoProblem() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
        assertAttached(tx, write2);
    }

    @Test
    public void whenLockedByOtherAfterOpenedAndLockRequired() {
        LongRef ref = createLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
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

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        LongRef ref1 = createReadBiasedLongRef(stm, 100);
        LongRef ref2 = createLongRef(stm);

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setReadTrackingEnabled(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref1, false, pool);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        try {
            tx.openForWrite(ref2, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx);
        assertSurplus(1, ref1);
        assertUnlocked(ref1);
        assertNull(ref1.getLockOwner());
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
        assertNull(ref2.getLockOwner());
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        LongRef ref = createLongRef(stm, 0);
        Tranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true));
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfig(stm).setReadonly(true));
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

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
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        assertSame(tx, ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        BetaTransactionConfig config = new BetaTransactionConfig(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertSame(tx, ref.getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenHasCommutingFunctions() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertSame(commuting, write);
        assertSame(committed, write.read);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertEquals(11, write.value);
        assertNull(ref.lockOwner);
        assertHasNoCommutingFunctions(write);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenHasCommutingFunctionAndLocked_thenReadConflict() {
        LongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {

        }

        assertAborted(tx);
        assertSame(otherTx, ref.lockOwner);
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        LongRef ref = BetaStmUtils.createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare(pool);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
        assertSurplus(0, ref);
        assertNull(ref.getLockOwner());
        assertUnlocked(ref);
        assertSame(committed, ref.unsafeLoad());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort(pool);

        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit(pool);

        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
