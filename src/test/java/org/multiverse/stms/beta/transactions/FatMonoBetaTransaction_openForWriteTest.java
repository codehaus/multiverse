package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_openForWriteTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenUndefined() {

    }

    @Test
    public void whenNotCommittedBefore_thenReadConflict() {
        BetaTransaction otherTx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(otherTx);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenOverflowing() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForWrite(ref1, false);
        try {
            tx.openForWrite(ref2, false);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasNoCommitLock(ref);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
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
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertHasNoCommitLock(ref);
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
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, true);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
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
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertNotNull(write);
        assertNotSame(read, write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
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
    public void whenConstructedAndLock() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, true);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
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
    public void whenOpenForWriteAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, false);
        LongRefTranlocal write2 = tx.openForWrite(ref, false);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForReadAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
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
    public void whenAlreadyOpenedForWriteAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, true);
        LongRefTranlocal write2 = tx.openForWrite(ref, false);

        assertSame(write2, write1);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
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
    public void whenAlreadyOpenedForReadAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);
        LongRefTranlocal write = tx.openForWrite(ref, true);

        assertNotNull(write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
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
    public void whenAlreadyOpenedForWriteAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, false);
        LongRefTranlocal write2 = tx.openForWrite(ref, true);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
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
    public void whenRepeatedLockThenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write1 = tx.openForWrite(ref, true);
        LongRefTranlocal write2 = tx.openForWrite(ref, true);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
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
    public void whenLockedByOtherAfterOpenedAndLockRequired() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false);

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForWrite(ref, true);

        try {
            tx.openForWrite(ref, true);
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
    public void whenLockedByOtherAndNoLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(false);
    }

    @Test
    public void whenLockedByOtherAndLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(true);
    }

    public void whenLockedByOther_thenReadConflict(boolean lockNeeded) {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        try {
            tx.openForWrite(ref, lockNeeded);
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
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref1, false);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForWrite(ref2, false);
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
    public void whenReadonly_thenAbortedAndReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setReadonly(true));
        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setReadonly(true));
        LongRefTranlocal read = tx.openForRead(ref, false);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        try {
            tx.openForWrite((BetaLongRef) null, true);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenHasCommutingFunctions() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertSame(commuting, write);
        assertSame(committed, write.read);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertEquals(11, write.value);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommutingFunctions(write);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    @Test
    public void whenHasCommutingFunctionAndLocked_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

     @Test
    public void whenCommuteAvailableThatCausesProblems_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, true);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        BetaTransactionalObject ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, true);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        BetaTransactionalObject ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, true);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
