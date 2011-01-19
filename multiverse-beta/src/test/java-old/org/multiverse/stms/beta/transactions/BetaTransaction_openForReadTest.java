package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockLevel;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.BetaTranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;


//todo: improved testing of version and value on the tranlocal
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
    public void whenNullRef() {
        BetaTransaction tx = newTransaction();
        BetaTranlocal read = tx.openForRead((BetaLongRef) null, LOCKMODE_NONE);

        assertNull(read);
        assertIsActive(tx);
    }

    @Test
    public void whenUntracked() {
        assumeIsAbleToNotTrackReads();

        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertEquals(version, ref.getVersion());
        assertEquals(100, ref.___weakRead());
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
        assertNotAttached(tx, ref);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertEquals(100, read.value);
        assertFalse(read.hasDepartObligation());
        assertTrue(read.isReadonly());
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertRefHasNoLocks(ref);
        assertAttached(tx, read);
        assertEquals(version, ref.getVersion());
        assertEquals(100, ref.___weakRead());
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(100, read.value);
        assertTrue(read.hasDepartObligation());
        assertTrue(read.isReadonly());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
        assertRefHasNoLocks(ref);
        assertIsActive(tx);
        assertAttached(tx, read);
    }


    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_NONE);
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    // =================== already opened for write =======================

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value = 100;
        BetaTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        BetaTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(write, read);
        assertEquals(100, write.value);
        assertFalse(write.isReadonly());
        assertTrue(write.hasDepartObligation());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version + 1, 101);
        assertAttached(tx, read);
    }


    @Test
    public void whenMultipleOpenForReads() {
        assumeTrue(getTransactionMaxCapacity() >= 3);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        long versionRef1 = ref1.getVersion();
        long versionRef2 = ref2.getVersion();
        long versionRef3 = ref3.getVersion();

        BetaTransaction tx = newTransaction();
        BetaTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);
        BetaTranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);
        BetaTranlocal read3 = tx.openForRead(ref3, LOCKMODE_NONE);

        assertIsActive(tx);
        assertVersionAndValue(ref1, versionRef1, 0);
        assertVersionAndValue(ref1, versionRef2, 0);
        assertVersionAndValue(ref1, versionRef3, 0);

        assertAttached(tx, read1);
        assertAttached(tx, read2);
        assertAttached(tx, read3);
    }


    // ===================  already opened For construction =================

    @Test
    public void whenAlreadyOpenedForConstruction() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        BetaTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isReadonly());
        assertFalse(constructed.hasDepartObligation());
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertVersionAndValue(ref, 0, 0);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndPrivatize() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertEquals(0, constructed.version);
        assertFalse(constructed.isReadonly());
        assertFalse(constructed.hasDepartObligation());
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndEnsure() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        BetaTranlocal read = tx.openForRead(ref, LOCKMODE_WRITE);

        assertSame(constructed, read);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isReadonly());
        assertFalse(constructed.hasDepartObligation());
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertVersionAndValue(ref, 0, 0);
        assertAttached(tx, read);
    }

    // ================= locking ===============


    @Test
    public void locking_whenReadBiasedAndPrivatize() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertEquals(100, read.value);
        assertFalse(read.hasDepartObligation());
        assertTrue(read.isReadonly());
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertVersionAndValue(ref, version, 100);
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void locking_whenReadBiasedAndEnsure() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_WRITE);

        assertEquals(100, read.value);
        assertFalse(read.hasDepartObligation());
        assertTrue(read.isReadonly());

        assertRefHasWriteLock(ref, tx);
        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertVersionAndValue(ref, version, 100);
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void locking_whenReadBiasedAndNoReadTrackingAndPrivate_thenAttached() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertIsActive(tx);
        assertTrue(read.isReadonly());
        assertFalse(read.hasDepartObligation());
        assertEquals(10, read.value);
        assertAttached(tx, read);
        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, tx);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void locking_whenReadBiasedAndNoReadTrackingAndEnsure_thenAttached() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_WRITE);

        assertIsActive(tx);

        assertTrue(read.isReadonly());
        assertFalse(read.hasDepartObligation());
        assertEquals(10, read.value);
        assertSurplus(1, ref);
        assertRefHasWriteLock(ref, tx);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertAttached(tx, read);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void locking_whenUpdateBiasedAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertEquals(100, read.value);
        assertEquals(version, read.version);
        assertTrue(read.hasDepartObligation());
        assertTrue(read.isReadonly());
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void locking_whenUpdateBiasedAndEnsure() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_WRITE);

        assertEquals(100, read.value);
        assertTrue(read.hasDepartObligation());
        assertTrue(read.isReadonly());

        assertIsActive(tx);
        assertAttached(tx, read);

        assertRefHasWriteLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void locking_whenEnsuredByOther_thenReadSuccess() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        BetaLongRefTranlocal read1 = otherTx.openForRead(ref, LOCKMODE_WRITE);

        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        assertNotSame(read1, read2);
        assertTrue(read2.isReadonly());
        assertEquals(version, read2.version);
        assertEquals(100, read2.value);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenAlreadyPrivatizedBySelfAndPrivatize_thenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void locking_whenAlreadyPrivatizedBySelfAndPEnsure_thenRemainsPrivatized() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_WRITE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertVersionAndValue(ref, version, 10);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void locking_whenAlreadyEnsuredBySelfAndPrivatize_thenUpgraded() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_WRITE);
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertVersionAndValue(ref, version, 10);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void locking_whenAlreadyEnsuredBySelfAndEnsure_thenRemainsEnsured() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_WRITE);
        BetaLongRefTranlocal read2 = tx.openForRead(ref, LOCKMODE_WRITE);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertVersionAndValue(ref, version, 0);
        assertRefHasWriteLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }


    @Test
    public void locking_whenConstructedAndPrivatize_thenRemainsPrivatized() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal construction = tx.openForConstruction(ref);

        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        assertSame(construction, read);
        assertVersionAndValue(ref, 0, 0);
        assertIsActive(tx);
        assertAttached(tx, read);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void locking_whenConstructedAndEnsure_thenRemainsPrivatized() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal construction = tx.openForConstruction(ref);

        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_WRITE);

        assertIsActive(tx);
        assertSame(construction, read);
        assertVersionAndValue(ref, 0, 0);
        assertRefHasCommitLock(ref, tx);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read);
    }

    // =============================== pessimistic lock level ===================

    @Test
    public void lockLevel_whenPrivatizeReadsLevelUsed() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertTrue(read.isReadonly());
        assertTrue(read.hasDepartObligation());
        assertAttached(tx, read);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void lockLevel_whenEnsureReadsLevelUsed() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.WriteLockReads);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertTrue(read.isReadonly());
        assertTrue(read.hasDepartObligation());
        assertAttached(tx, read);
        assertVersionAndValue(ref, version, 100);
        assertRefHasWriteLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void lockLevel__whenEnsureWritesLevelUsed() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.WriteLockWrites);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertTrue(read.isReadonly());
        assertTrue(read.hasDepartObligation());
        assertAttached(tx, read);
        assertVersionAndValue(ref, version, 100);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void lockLevel__whenPrivatizeWritesLevelUsed() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockWrites);
        BetaTransaction tx = newTransaction(config);
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(100, read.value);
        assertEquals(version, read.version);
        assertTrue(read.isReadonly());
        assertTrue(read.hasDepartObligation());
        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, read);
        assertVersionAndValue(ref, version, 100);
    }

    // ============= isolation level ============================

    @Test
    @Ignore
    public void isolationLevel_whenInconsistentRead() {

    }


    // ============ consistency ===============================

    @Test
    public void consistency_whenReadConflict() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        BetaTransaction conflictingTx = stm.startDefaultTransaction();
        conflictingTx.openForWrite(ref1, LOCKMODE_NONE).value++;
        conflictingTx.commit();

        try {
            tx.openForRead(ref2, LOCKMODE_NONE);
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
    public void consistency_conflictCounterIsSetAtFirstRead() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void consistency_conflictCounterIsNotSetWhenAlreadyRead() {
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
        assumeTrue(getTransactionMaxCapacity() >= 2);
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
        assertRefHasNoLocks(ref1);
        assertSurplus(0, ref2);
        assertRefHasNoLocks(ref2);
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
    public void consistency_whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads);

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForRead(ref2, LOCKMODE_NONE);
        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
    }

    @Test
    public void consistency_whenAlreadyOpenedForRead_thenNoReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction conflictingTx = stm.startDefaultTransaction();
        BetaLongRefTranlocal conflictingWrite = conflictingTx.openForWrite(ref, LOCKMODE_NONE);
        conflictingWrite.value++;
        conflictingTx.commit();

        long version = ref.getVersion();

        BetaTranlocal read2 = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(read1, read2);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 101);
        assertAttached(tx, read1);
    }

    @Test
    public void consistency_conflictCounterIsOnlySetOnFirstRead() {
        assumeTrue(hasLocalConflictCounter());
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        stm.getGlobalConflictCounter().signalConflict(ref1);

        BetaLongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        BetaLongRefTranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    @Test
    public void consistency_whenNonConflictReadConflict() {
        assumeTrue(hasLocalConflictCounter());
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);
        long oldLocalConflictCounter = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        BetaLongRefTranlocal read2 = tx.openForRead(ref2, LOCKMODE_NONE);

        assertFalse(oldLocalConflictCounter == stm.getGlobalConflictCounter().count());
        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
        assertIsActive(tx);
        assertAttached(tx, read1);
        assertAttached(tx, read2);
    }

    // ============ commute ====================================

    @Test
    public void commute_whenHasCommutingFunctions() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongFunction function = newIncLongFunction(1);
        tx.commute(ref, function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);

        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        assertSame(commuting, tranlocal);
        assertEquals(version, tranlocal.version);
        assertEquals(11, tranlocal.value);
        assertEquals(10, tranlocal.oldValue);
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        assertTranlocalHasNoLock(tranlocal);
        assertTrue(tranlocal.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, tranlocal);

        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLocked() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        BetaTransaction tx = newTransaction();
        LongFunction function = newIncLongFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void commute_whenCommuteConflicts_thenAborted() {
        assumeTrue(doesTransactionSupportCommute());
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
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
        assertRefHasNoLocks(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertRefHasNoLocks(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

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
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_EXCLUSIVE);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLockedByOther_thenReadConflict() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newIncLongFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLocked_thenReadConflict() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_EXCLUSIVE);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newIncLongFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);
    }

    // =============== other states ===============================

    @Test
    public void state_whenAlreadyPrepared_thenPreparedTransactionException() {
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
    public void state_whenAlreadyAborted_thenDeadTransactionException() {
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
    public void state_whenAlreadyCommitted_thenDeadTransactionException() {
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
