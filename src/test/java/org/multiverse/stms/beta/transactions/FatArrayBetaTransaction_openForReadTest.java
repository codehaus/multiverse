package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
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
public class FatArrayBetaTransaction_openForReadTest {
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
    public void whenNullRef() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal read = tx.openForRead((BetaLongRef) null, LOCKMODE_NONE);

        assertNull(read);
        assertIsActive(tx);
    }

    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        Tranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        Tranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void whenOverflowing() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);
        BetaLongRef ref4 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
        config.init();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);
        tx.openForRead(ref2, LOCKMODE_NONE);
        tx.openForRead(ref3, LOCKMODE_NONE);
        try {
            tx.openForRead(ref4, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
        assertEquals(4, config.getSpeculativeConfiguration().minimalLength);
    }

    @Test
    public void whenUntracked() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(committed, tranlocal);
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
        assertNotAttached(tx, ref);
    }

    @Test
    public void whenReadBiasedAndNoReadTrackingAndLock_thenAttached() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiasedAndPrivatize() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiasedAndEnsure() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_UPDATE);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertTrue(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenUpdateBiasedAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenUpdateBiasedAndEnsure() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_UPDATE);

        assertSame(committed, read);
        assertEquals(100, read.value);
        assertFalse(read.isPermanent);
        assertTrue(read.isCommitted);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = 100;
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, LOCKMODE_NONE);
        conflictingWrite.value++;
        conflictingTx.commit();

        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_NONE);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, LOCKMODE_NONE);
        conflictingWrite.value++;
        conflictingTx.commit();

        Tranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(read1, read2);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertSame(conflictingWrite, ref.___unsafeLoad());
        assertAttached(tx, read1);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndLock() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenLockedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction other = new FatArrayBetaTransaction(stm);
        other.openForRead(ref, LOCKMODE_COMMIT);

        int oldReadonlyCount = ref.___getReadonlyCount();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertIsAborted(tx);
        assertSame(other, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        FatArrayBetaTransaction conflictingTx = new FatArrayBetaTransaction(stm);
        conflictingTx.openForWrite(ref1, LOCKMODE_NONE).value++;
        conflictingTx.commit();

        try {
            tx.openForRead(ref2, LOCKMODE_NONE);
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
    public void conflictCounterIsSetAtFirstRead() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyRead() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForRead(ref, LOCKMODE_NONE);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForRead(ref2, LOCKMODE_NONE);
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
    public void whenUnrealConflictThenConflictCounterUpdated() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForRead(ref1, LOCKMODE_NONE);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForRead(ref2, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForRead(ref3, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertIsActive(tx);
    }

    @Test
    public void whenMultipleOpenForReads() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = new FatArrayBetaTransaction(stm);
        Tranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);
        Tranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);
        Tranlocal read3 = tx.openForRead(ref3, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(read1, ref1.___unsafeLoad());
        assertSame(read2, ref2.___unsafeLoad());
        assertSame(read3, ref3.___unsafeLoad());
        assertAttached(tx, read1);
        assertAttached(tx, read2);
        assertAttached(tx, read3);
    }

    @Test
    public void whenPessimisticRead() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = new FatArrayBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(committed, read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
        assertAttached(tx, read);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForRead(ref2, LOCKMODE_NONE);
        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
    }

    @Test
    public void comute_whenHasCommutingFunctions() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal read = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(commuting, read);
        assertSame(committed, read.read);
        assertFalse(read.isCommuting);
        assertFalse(read.isCommitted);
        assertEquals(11, read.value);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, read);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLocked() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatArrayBetaTransaction otherTx = new FatArrayBetaTransaction(stm);
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
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
    public void commute_whenCommuteConflicts_thenAborted() {
        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 10);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.openForRead(ref1, LOCKMODE_NONE);
        LongFunction function = mock(LongFunction.class);
        tx.commute(ref2, function);

        ref1.atomicIncrementAndGet(1);

        try {
            tx.openForRead(ref2, LOCKMODE_NONE);
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
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
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

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_COMMIT);
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
    public void whenPrepared_thenPreparedException() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
