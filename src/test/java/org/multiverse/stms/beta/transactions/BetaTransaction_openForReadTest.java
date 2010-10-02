package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.functions.Functions.newLongIncFunction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_openForReadTest implements BetaStmConstants {
    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean doesTransactionSupportCommute();

    public abstract int getTransactionMaxCapacity();

    protected abstract void assumeIsAbleToNotTrackReads();

    protected abstract boolean hasLocalConflictCounter();

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
        BetaTransaction tx = newTransaction();
        Tranlocal read = tx.openForRead((BetaLongRef) null, LOCKMODE_NONE);

        assertNull(read);
        assertIsActive(tx);
    }


//    @Test
//    public void conflictCounterIsOnlySetOnFirstRead() {
//        BetaLongRef ref1 = newLongRef(stm);
//        BetaLongRef ref2 = newLongRef(stm);
//
//        BetaTransaction tx = new FatArrayBetaTransaction(stm);
//        stm.getGlobalConflictCounter().signalConflict(ref1);
//
//        Tranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);
//
//        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
//
//        Tranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);
//
//        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
//        assertIsActive(tx);
//        assertAttached(tx, read1);
//        assertAttached(tx, read2);
//    }

//    @Test
//      public void whenOverflowing() {
//          BetaLongRef ref1 = newLongRef(stm);
//          BetaLongRef ref2 = newLongRef(stm);
//          BetaLongRef ref3 = newLongRef(stm);
//          BetaLongRef ref4 = newLongRef(stm);
//
//          BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 3);
//          config.init();
//          BetaTransaction tx = new FatArrayBetaTransaction(config);
//          tx.openForRead(ref1, LOCKMODE_NONE);
//          tx.openForRead(ref2, LOCKMODE_NONE);
//          tx.openForRead(ref3, LOCKMODE_NONE);
//          try {
//              tx.openForRead(ref4, LOCKMODE_NONE);
//              fail();
//          } catch (SpeculativeConfigurationError expected) {
//          }
//
//          assertIsAborted(tx);
//          assertEquals(4, config.getSpeculativeConfiguration().minimalLength);
//      }

    @Test
    public void whenUntracked() {
        assumeIsAbleToNotTrackReads();

        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        BetaTransaction tx = newTransaction(config);
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
        BetaTransaction tx = newTransaction(config);
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

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
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
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_NONE);
        LongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
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

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction conflictingTx = stm.startDefaultTransaction();
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

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction conflictingTx = stm.startDefaultTransaction();
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
        BetaTransaction tx = newTransaction();
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
    public void whenAlreadyOpenedForConstructionAndPrivatize() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndEnsure() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal read = tx.openForRead(ref, LOCKMODE_UPDATE);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction other = stm.startDefaultTransaction();
        other.openForRead(ref, LOCKMODE_COMMIT);

        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(other, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenEnsuredByOther_thenReadSuccess() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction other = stm.startDefaultTransaction();
        other.openForRead(ref, LOCKMODE_UPDATE);

        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(other, ref.___getLockOwner());
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertIsActive(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        BetaTransaction conflictingTx = stm.startDefaultTransaction();
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
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyRead() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

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
        assumeTrue(getTransactionMaxCapacity()>=2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        BetaTransaction tx = newTransaction(config);
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
        assumeTrue(getTransactionMaxCapacity()>=3);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();

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
        assumeTrue(getTransactionMaxCapacity() >= 3);

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
    public void whenAlreadyPrivatizedBySelf_thenNoProblem() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_COMMIT);
        LongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertSame(committed, read2);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void whenPrivatizeReadsLevelUsed() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = newTransaction(config);
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
    public void whenPrivatizeWritesLevelUsed() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(committed, read);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
        assertAttached(tx, read);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

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
    public void commute_whenHasCommutingFunctions() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongFunction function = newLongIncFunction(1);
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
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = newTransaction();
        LongFunction function = newLongIncFunction(1);
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
        assumeTrue(doesTransactionSupportCommute());
        assumeTrue(getTransactionMaxCapacity() >= 2);

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
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
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
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
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


    //todo:
    @Test
    public void whenConstructedAndLock() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref);

        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(construction, read);
        assertNull(ref.___unsafeLoad());
        assertHasCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertAttached(tx, read);
    }

    //todo:
    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        stm.getGlobalConflictCounter().signalConflict(ref1);

        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        LongRefTranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }


    //todo:
    @Test
    public void whenNonConflictReadConflict() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);
        long oldLocalConflictCounter = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        LongRefTranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);

        assertFalse(oldLocalConflictCounter == stm.getGlobalConflictCounter().count());
        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLockedByOther_thenReadConflict() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
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

    //todo:
    //todo:

    @Test
    public void whenPrivatized() {

        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(committed, read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read);
    }

    //todo:

    @Test
    public void commute_whenHasCommutingFunctionsAndLocked_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = Functions.newLongIncFunction(1);
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
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
