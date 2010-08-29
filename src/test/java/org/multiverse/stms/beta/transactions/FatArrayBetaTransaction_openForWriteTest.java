package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatArrayBetaTransaction_openForWriteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        tx.openForWrite(ref1, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        tx.openForWrite(ref2, false, pool);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
    }

    @Test
    public void whenNull_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForWrite((BetaLongRef) null, false, pool);
            fail();
        } catch (NullPointerException ex) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        BetaTransactionalObject ref = createLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        BetaLongRef ref = createLongRef(stm, 0);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);
        BetaLongRef ref3 = createLongRef(stm);
        BetaLongRef ref4 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForWrite(ref1, false, pool);
        tx.openForWrite(ref2, false, pool);
        tx.openForWrite(ref3, false, pool);
        try {
            tx.openForWrite(ref4, false, pool);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertAborted(tx);
        assertEquals(4, config.getSpeculativeConfig().getMinimalLength());
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNull(ref.___getLockOwner());
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
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenReadBiasedAndLock() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertActive(tx);
        assertSame(tx, ref.___getLockOwner());
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
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNull(ref.___getLockOwner());
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
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdateBiasedAndLock() {
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true, pool);

        assertActive(tx);
        assertSame(tx, ref.___getLockOwner());
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
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = createLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertNotNull(write);
        assertNotSame(read, write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertNull(ref.___getLockOwner());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndLock() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref, pool);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, true, pool);

        assertActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertLocked(ref.___getOrec());
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenReadConflict() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false, pool);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
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
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertUnlocked(ref2);
        assertNull(ref2.___getLockOwner());
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    private void assertOpenedForWrite(BetaTransactionalObject ref, Tranlocal tranlocal) {
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(ref.___unsafeLoad(), tranlocal.read);
        assertFalse(tranlocal.isCommitted);
    }

    @Test
    public void whenAlreadyOpenedForReadThenUpgraded() {
        BetaLongRef ref = BetaStmUtils.createLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref, false, pool);
        Tranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertOpenedForWrite(ref, write);
        assertFalse(ref.___getOrec().___isLocked());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenLocked_thenReadConflict() {
        BetaLongRef ref = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        tx1.openForRead(ref, true, pool);

        FatArrayBetaTransaction tx2 = new FatArrayBetaTransaction(stm);
        try {
            tx2.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {
        }

        assertAborted(tx2);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaTransactionalObject ref = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal write1 = tx.openForWrite(ref, false, pool);
        Tranlocal write2 = tx.openForWrite(ref, false, pool);

        assertSame(write1, write2);
        assertActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void multipleOpenForWrites() {
        BetaTransactionalObject ref1 = BetaStmUtils.createLongRef(stm);
        BetaTransactionalObject ref2 = BetaStmUtils.createLongRef(stm);
        BetaTransactionalObject ref3 = BetaStmUtils.createLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal write1 = tx.openForWrite(ref1, false, pool);
        Tranlocal write2 = tx.openForWrite(ref2, false, pool);
        Tranlocal write3 = tx.openForWrite(ref3, false, pool);

        assertActive(tx);
        assertOpenedForWrite(ref1, write1);
        assertOpenedForWrite(ref2, write2);
        assertOpenedForWrite(ref3, write3);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertAttached(tx, write3);
        assertHasUpdates(tx);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Write);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        BetaLongRef ref = createLongRef(stm);
        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenUpdatingReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForWrite(ref, false, pool);

        assertActive(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertHasUpdates(tx);
    }

    @Test
    public void conflictCounterIsSetAtFirstWrite() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForWrite(ref, false, pool);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertActive(tx);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyOpenedForWrite() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForWrite(ref, false, pool);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForWrite(ref, false, pool);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
    }

    @Test
    public void whenUnrealConflictThenConflictCounterUpdated() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);
        BetaLongRef ref3 = createLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        tx.openForWrite(ref1, false, pool);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForWrite(ref2, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));
        tx.openForWrite(ref3, false, pool);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertActive(tx);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
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
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref2);
        assertUnlocked(ref2);
        assertNull(ref2.___getLockOwner());
    }

    @Test
    public void whenHasCommutingFunctions() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
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
        assertNull(ref.___getLockOwner());
        assertHasNoCommutingFunctions(write);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenHasCommutingFunctionAndLocked_thenReadConflict() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, pool, function);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (ReadConflict expected) {

        }

        assertAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertLocked(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }


    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, false, pool);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(createLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, false, pool);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
        assertActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void whenCommuteAvailableThatCausesProblems_thenAbort() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, pool, function);

        try {
            tx.openForWrite(ref, false, pool);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        BetaLongRef ref = createLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, pool, function);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort(pool);

        BetaLongRef ref = createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit(pool);

        BetaLongRef ref = createLongRef(stm);

        try {
            tx.openForWrite(ref, true, pool);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
    }
}
