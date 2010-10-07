package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
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
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.hasDepartObligation);

        assertIsActive(tx);
        assertHasUpdates(tx);
        assertAttached(tx, write);

        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(ref, write.owner);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertHasNoCommitLock(ref);
        assertVersionAndValue(ref, version, 100);
    }


    private void assertOpenedForWrite(BetaTransactionalObject ref, Tranlocal tranlocal) {
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        //todo:
        //assertSame(ref.___unsafeLoad(), tranlocal.read);
        assertFalse(tranlocal.isCommitted);
    }

    @Test
    public void whenAlreadyOpenedForReadThenUpgraded() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertSame(read, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaTransactionalObject ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        Tranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        Tranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write1, write2);
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.hasDepartObligation);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
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
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertEquals(100, write.value);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertVersionAndValue(ref, version, 100);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void locking_whenConstructedAndLock() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
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
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertEquals(100, write.value);
        assertEquals(version, write.version);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.hasDepartObligation);

        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);

        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenOpenForWriteAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertUpdateBiased(ref);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx,ref.___getLockOwner());
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
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertUpdateBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx,ref.___getLockOwner());
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
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write2, write1);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertTrue(write2.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(read, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertTrue(write2.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.hasDepartObligation);

        assertIsActive(tx);
        assertHasNoUpdates(tx);
        assertAttached(tx, write);

        assertHasCommitLock(ref);
        assertVersionAndValue(ref, 0, 0);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0,0);
    }

    @Test
    public void locking_whenPrivatizedIsCalledMultipleTimes_ThenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertTrue(write2.hasDepartObligation);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_UPDATE);
       LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertTrue(write2.hasDepartObligation);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
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

        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void locking_whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

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
    public void pessimisticLockLevel_whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm,10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = newTransaction(config);

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);
        assertEquals(version, write.version);
        assertEquals(10, write.value);

        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);

        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void pessimisticLockLevel__whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertEquals(version, write.version);
        assertEquals(10, write.value);
        assertFalse(write.isCommitted);
        assertTrue(write.hasDepartObligation);

        assertVersionAndValue(ref, version, 10);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
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

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(commuting, write);
        assertVersionAndValue(ref, initialVersion, 10);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertEquals(11, write.value);
        assertNull(ref.___getLockOwner());
        assertHasNoCommutingFunctions(write);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
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
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
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
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

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
        assertNull(ref1.___getLockOwner());
        assertHasNoCommitLock(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertNull(ref2.___getLockOwner());
        assertHasNoCommitLock(ref2);
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
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion,10);
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
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion,10);
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
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNotNull(write);
        assertFalse(write.isCommitted);
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
        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

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

        assertHasNoCommitLock(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertHasNoCommitLock(ref2);
        assertNull(ref2.___getLockOwner());
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
        ref.privatize(otherTx);

        BetaTransaction tx = newTransaction();
        try {
            tx.openForWrite(ref, lockNeeded ? LOCKMODE_COMMIT : LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    @Test
    public void consistency_whenAlreadyOpenedForWriteAndUpdatedByOther_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        Tranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        Tranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(write2, write1);
        assertFalse(write2.isCommitted);
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
        assertHasNoCommitLock(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref2);
        assertHasNoCommitLock(ref2);
        assertNull(ref2.___getLockOwner());
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
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

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
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

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
        Tranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);
        Tranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);
        Tranlocal write3 = tx.openForWrite(ref3, LOCKMODE_NONE);

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
