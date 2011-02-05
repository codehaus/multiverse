package org.multiverse.stms.gamma.transactions.fat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.AbortOnlyException;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class FatGammaTransaction_prepareTest<T extends GammaTransaction> implements GammaConstants {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    @Test
    public void conflict_whenArriveByOther() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        long newValue = 1;
        ref.set(tx, newValue);

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setMaximumPoorMansConflictScanLength(0);

        FatVariableLengthGammaTransaction otherTx = new FatVariableLengthGammaTransaction(config);
        ref.get(otherTx);

        long globalConflictCount = stm.globalConflictCounter.count();
        tx.prepare();

        assertIsPrepared(tx);
        assertTrue(tx.commitConflict);
        assertGlobalConflictCount(stm, globalConflictCount);
        assertVersionAndValue(ref, initialVersion , initialValue);
        assertLockMode(ref, LOCKMODE_EXCLUSIVE);
        assertSurplus(ref, 2);
    }

    @Test
    public void whenAbortOnly() {
        long globalConflictCount = stm.globalConflictCounter.count();

        T tx = newTransaction();
        tx.setAbortOnly();

        try {
            tx.prepare();
            fail();
        } catch (AbortOnlyException expected) {
        }

        assertIsAborted(tx);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void whenContainsRead() {
        whenContainsRead(LockMode.None);
        whenContainsRead(LockMode.Read);
        whenContainsRead(LockMode.Write);
        whenContainsRead(LockMode.Exclusive);
    }

    public void whenContainsRead(LockMode readLockMode) {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.openForRead(tx, readLockMode.asInt());
        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertLockMode(ref, readLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    @Ignore
    public void writeSkew() {

    }

    @Test
    @Ignore
    public void whenContainsCommute() {

    }

    @Test
    @Ignore
    public void whenContainsCommuteThatConflicts() {

    }

    @Test
    @Ignore
    public void whenContainsConstructed() {
    }

    // =============================== dirty check =================================

    @Test
    public void dirtyCheckDisabled_whenNotDirty_thenLockedForCommit() {
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.None);
    }

    public void dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode writeLockMode) {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void dirtyCheckDisabled_whenDirty_thenLockedForCommit() {
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.None);
    }

    public void dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode writeLockMode) {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tranlocal.long_value++;
        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void dirtyCheckEnabled_whenNotDirty_thenNothingHappens() {
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Exclusive);
    }

    public void dirtyCheckEnabled_whenNotDirty_nothingHappens(LockMode writeLockMode) {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertFalse(tranlocal.isDirty());
        assertEquals(writeLockMode.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, writeLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void dirtyCheckEnabled_whenDirty_thenLockedForCommit() {
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Exclusive);
    }

    public void dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode writeLockMode) {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tranlocal.long_value++;
        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    // =============================== locked by other =============================

    @Test
    public void conflict_dirty_whenReadLockedByOther() {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_READ);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void conflict_dirty_whenWriteLockedByOther() {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_WRITE);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasWriteLock(ref, otherTx);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void conflict_dirty_whenExclusiveLockedByOther() {
        long globalConflictCount = stm.globalConflictCounter.count();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_EXCLUSIVE);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasExclusiveLock(ref, otherTx);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    // ================================ states =====================================

    @Test
    public void whenPreparedAndUnused() {
        long globalConflictCount = stm.globalConflictCounter.count();

        T tx = newTransaction();
        tx.prepare();

        tx.prepare();

        assertIsPrepared(tx);
        assertFalse(tx.commitConflict);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        long globalConflictCount = stm.globalConflictCounter.count();

        T tx = newTransaction();
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertGlobalConflictCount(stm, globalConflictCount);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        long globalConflictCount = stm.globalConflictCounter.count();

        T tx = newTransaction();
        tx.commit();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertGlobalConflictCount(stm, globalConflictCount);
    }
}
