package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_openForWriteTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean isSupportingCommute();

    public abstract int getTransactionMaxCapacity();

    public abstract boolean hasLocalConflictCounter();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.openForWrite((BetaLongRef) null, LOCKMODE_NONE);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertFalse(write.isReadonly());
        assertFalse(write.hasDepartObligation());

        assertIsActive(tx);
        assertHasUpdates(tx);
        assertAttached(tx, write);

        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertRefHasNoLocks(ref);
        assertSame(ref, write.owner);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertVersionAndValue(ref, version, 100);
    }


    private void assertOpenedForWrite(BetaTransactionalObject ref, BetaTranlocal tranlocal) {
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        //todo:
        //assertSame(ref.___unsafeLoad(), tranlocal.read);
        assertFalse(tranlocal.isReadonly());
    }

    @Test
    public void whenAlreadyOpenedForReadThenUpgraded() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertSame(read, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaTransactionalObject ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        BetaTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write1, write2);
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        BetaTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isReadonly());
        assertFalse(constructed.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    // ================== locking =========================

    @Test
    public void locking_whenUpdateBiasedAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertEquals(100, write.value);

        assertIsActive(tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertRefHasCommitLock(ref,tx);
        assertVersionAndValue(ref, version, 100);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenConstructedAndLock() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;

        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertFalse(write.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    @Test
    public void locking_whenUpdateBiasedAndEnsure() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
        assertRefHasUpdateLock(ref,tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenReadBiasedAndPrivatize() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertEquals(version, write.version);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertFalse(write.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertRefHasCommitLock(ref,tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenOpenForWriteAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenOpenForWriteAndEnsure() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertUpdateBiased(ref);
        assertRefHasUpdateLock(ref,tx);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenAlreadyOpenedForReadAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenAlreadyOpenedForWriteAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        BetaLongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write2, write1);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isReadonly());
        assertTrue(write2.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenAlreadyOpenedForReadAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(read, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenAlreadyOpenedForWriteAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        BetaLongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isReadonly());
        assertTrue(write2.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndPrivatize() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        BetaTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isReadonly());
        assertFalse(constructed.hasDepartObligation());
        assertIsActive(tx);
        assertHasNoUpdates(tx);
        assertAttached(tx, write);
        assertRefHasCommitLock(ref,tx);
        assertVersionAndValue(ref, 0, 0);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
    }

    @Test
    public void locking_whenPrivatizedIsCalledMultipleTimes_ThenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        BetaLongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isReadonly());
        assertTrue(write2.hasDepartObligation());
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenEnsureIsCalledMultipleTimes_ThenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_UPDATE);
        BetaLongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isReadonly());
        assertTrue(write2.hasDepartObligation());
        assertRefHasUpdateLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenAlreadyOpenedAndPrivatizedByOtherAfterOpenedAndPrivatize_thenReadWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, LOCKMODE_COMMIT);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict e) {

        }

        assertRefHasCommitLock(ref,otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void locking_whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        BetaTransaction tx = newTransaction();
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
    }

    // ================== pessimistic lock level =======================

    @Test
    public void lockLevel_whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads);
        BetaTransaction tx = newTransaction(config);

        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertEquals(version, write.version);
        assertEquals(10, write.value);
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void lockLevel__whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockWrites);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertEquals(version, write.version);
        assertEquals(10, write.value);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertVersionAndValue(ref, version, 10);
        assertRefHasCommitLock(ref,tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }


    // ============================= commute =========================

    @Test
    public void commute_whenHasCommutingFunctions() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newIncLongFunction(1);
        tx.commute(ref, function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);

        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(commuting, write);
        assertVersionAndValue(ref, initialVersion, 10);
        assertFalse(write.isCommuting());
        assertFalse(write.isReadonly());
        assertEquals(11, write.value);
        assertRefHasNoLocks(ref);
        assertHasNoCommutingFunctions(write);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void commute_whenHasCommutingFunctionAndLocked_thenReadConflict() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newIncLongFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref,otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, 10);
    }


    @Test
    public void commute_whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(isSupportingCommute());
        assumeTrue(hasLocalConflictCounter());
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads);

        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        BetaLongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }


    @Test
    public void commute_whenCommuteConflicts_thenAborted() {
        assumeTrue(getTransactionMaxCapacity() >= 2);
        assumeTrue(isSupportingCommute());

        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref1, LOCKMODE_NONE);
        LongFunction function = mock(LongFunction.class);
        tx.commute(ref2, function);

        ref1.atomicIncrementAndGet(1);

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertRefHasNoLocks(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, 10);
    }


    @Test
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, 10);
    }

    // ========================= consistency ======================

    @Test
    public void consistency_conflictCounterIsSetAtFirstWrite() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void consistency_conflictCounterIsOnlySetOnFirstRead() {
        assumeTrue(getTransactionMaxCapacity() >= 2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        stm.getGlobalConflictCounter().signalConflict(ref1);

        tx.openForWrite(ref1, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
    }

    @Test
    public void consistency_whenAlreadyOpenedForReadThenNoReadConflictEvenIfUpdatedByOther() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        BetaTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        BetaTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNotNull(write);
        assertFalse(write.isReadonly());
        //todo:
        //assertSame(read, write.read);
        assertSame(ref, write.owner);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void consistency_whenReadConflict() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref1, LOCKMODE_NONE).value++;
        otherTx.openForWrite(ref2, LOCKMODE_NONE).value++;
        otherTx.commit();

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);

        assertRefHasNoLocks(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertRefHasNoLocks(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void consistency_whenLockedByOtherAndNoLockNeeded_thenReadConflict() {
        consistency_whenLockedByOther_thenReadConflict(false);
    }

    @Test
    public void consistency_whenLockedByOtherAndLockNeeded_thenReadConflict() {
        consistency_whenLockedByOther_thenReadConflict(true);
    }

    public void consistency_whenLockedByOther_thenReadConflict(boolean lockNeeded) {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        BetaTransaction tx = newTransaction();
        try {
            tx.openForWrite(ref, lockNeeded ? LOCKMODE_COMMIT : LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasCommitLock(ref,otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void consistency_whenAlreadyOpenedForWriteAndUpdatedByOther_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        BetaTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        BetaTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(write2, write1);
        assertFalse(write2.isReadonly());
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void consistency_conflictCounterIsNotSetWhenAlreadyOpenedForWrite() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref, LOCKMODE_NONE);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void consistency_whenUnrealConflictThenConflictCounterUpdated() {
        assumeTrue(getTransactionMaxCapacity() >= 3);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref1, LOCKMODE_NONE);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForWrite(ref2, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForWrite(ref3, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertIsActive(tx);
    }

    @Test
    public void consistency_whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .init();

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSurplus(1, ref1);
        assertRefHasNoLocks(ref1);
        assertSurplus(0, ref2);
        assertRefHasNoLocks(ref2);
    }

    @Test
    public void consistency_whenNotCommittedBefore_thenReadConflict() {
        BetaTransaction otherTx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(otherTx);

        BetaTransaction tx = newTransaction();

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void consistency_whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(getTransactionMaxCapacity() >= 2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads);

        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        BetaLongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());

        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
    }


    //=========================== readonly =====================

    @Test
    public void readonly_whenReadonly_thenAbortedAndReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        BetaTransaction tx = newTransaction(config);
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void readonly_whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
    }

    // ========================= misc ===============================

    @Test
    public void multipleOpenForWrites() {
        assumeTrue(getTransactionMaxCapacity() >= 3);

        BetaTransactionalObject ref1 = newLongRef(stm);
        BetaTransactionalObject ref2 = newLongRef(stm);
        BetaTransactionalObject ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);
        BetaTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);
        BetaTranlocal write3 = tx.openForWrite(ref3, LOCKMODE_NONE);

        assertIsActive(tx);
        assertOpenedForWrite(ref1, write1);
        assertOpenedForWrite(ref2, write2);
        assertOpenedForWrite(ref3, write3);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertAttached(tx, write3);
        assertHasUpdates(tx);
    }

    // ========================== state ==============================

    @Test
    public void state_whenPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        BetaLongRef ref = newLongRef(stm);
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void state_whenAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void state_whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

}
