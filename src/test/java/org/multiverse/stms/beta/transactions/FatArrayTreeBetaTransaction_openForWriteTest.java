package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
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
public class FatArrayTreeBetaTransaction_openForWriteTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }


    @Test
    public void whenNull_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.openForWrite((BetaLongRef) null, false);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(
                new BetaTransactionConfiguration(stm).setReadonly(true));
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

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(
                new BetaTransactionConfiguration(stm).setReadonly(true));
        tx.openForRead(ref, false);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenInitialOpenForWrite() {
        BetaLongRef ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        Tranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertFalse(write.isCommitted());
        assertSame(committed, write.read);
        assertSame(write.owner, ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, false);
        Tranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNotSame(read, write);
        assertFalse(write.isCommitted());
        assertSame(write.owner, ref);
        assertSame(ref, write.owner);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 0);

        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write1 = tx.openForWrite(ref, false);
        Tranlocal write2 = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertSame(write1, write2);
        assertFalse(write2.isCommitted());
        assertSame(committed, write1.read);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForReadThenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal read = tx.openForRead(ref, false);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, false);
        otherTx.commit();

        Tranlocal write = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertNotNull(write);
        assertFalse(write.isCommitted());
        assertSame(read, write.read);
        assertSame(ref, write.owner);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForWriteThenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        Tranlocal write1 = tx.openForWrite(ref, false);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, false);
        otherTx.commit();

        Tranlocal write2 = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertSame(write2, write1);
        assertFalse(write2.isCommitted());
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void whenLockedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);
        Orec orec = ref.___getOrec();
        orec.___arrive(1);
        orec.___tryLockAfterNormalArrive(1,true);

        BetaTransaction tx1 = stm.startDefaultTransaction();

        try {
            tx1.openForWrite(ref, false);
            fail();
        } catch (ReadConflict expected) {
        }

        assertIsAborted(tx1);
        assertHasCommitLock(orec);
    }

    @Test
    public void whenLockedByOtherAfterOpenedForWrite_thenNoProblems() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        Tranlocal write1 = tx.openForWrite(ref, false);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, true);

        Tranlocal write2 = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertSame(write1, write2);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        assertHasUpdates(tx);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
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
        assertHasUpdates(tx);
    }

    @Test
    public void whenReadConflict() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, false);

        FatArrayTreeBetaTransaction conflictingTx = new FatArrayTreeBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, false).value++;
        conflictingTx.openForWrite(ref2, false).value++;
        conflictingTx.commit();

        try {
            tx.openForWrite(ref2, false);
            fail();
        } catch (ReadConflict expected) {
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
    public void whenAlreadyOpenedForConstruction() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, false);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, false);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());

        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.prepare();

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void commute_whenHasCommutingFunctions() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void commute_whenHasCommutingFunctionsAndLocked() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForRead(ref, true);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, false);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void commute_whenCommuteConflicts_thenAborted() {
        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.openForRead(ref1, false);
        LongFunction function = mock(LongFunction.class);
        tx.commute(ref2, function);

        FatArrayTreeBetaTransaction otherTx = new FatArrayTreeBetaTransaction(stm);
        otherTx.openForWrite(ref1, true).value++;
        otherTx.commit();

        try {
            tx.openForWrite(ref2, false);
            fail();
        } catch (ReadConflict expected) {

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
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
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
    public void commute_whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, true);
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
    public void conflictCounterIsSetAtFirstWrite() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        assertAttached(tx, write);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write1 = tx.openForWrite(ref, false);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref, false);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        assertSame(write1, write2);
        assertAttached(tx, write1);
    }

    @Test
    public void whenUnrealConflictThenConflictCounterUpdated() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write1 = tx.openForWrite(ref1, false);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        LongRefTranlocal write2 = tx.openForWrite(ref2, false);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        LongRefTranlocal write3 = tx.openForWrite(ref3, false);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertAttached(tx, write3);
    }

    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        LongRefTranlocal write1 = tx.openForWrite(ref1, false);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        LongRefTranlocal write2 = tx.openForWrite(ref2, false);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        tx.openForRead(ref1, false);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForWrite(ref2, false);
            fail();
        } catch (ReadConflict expected) {
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
    @Ignore
    public void whenUndefined(){}

    @Test
    public void whenAborted_thenIllegalStateException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.abort();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, true);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenIllegalStateException() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        tx.commit();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, true);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsCommitted(tx);
    }
}
