package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_prepareTest<T extends GammaTransaction> implements GammaConstants {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    @Test
    @Ignore
    public void whenAbortOnly() {

    }

    @Test
    public void whenContainsRead() {
        whenContainsRead(LockMode.None);
        whenContainsRead(LockMode.Read);
        whenContainsRead(LockMode.Write);
        whenContainsRead(LockMode.Exclusive);
    }

    public void whenContainsRead(LockMode readLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.openForRead(tx, readLockMode.asInt());
        tx.prepare();

        assertIsPrepared(tx);
        assertLockMode(ref, readLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
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
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tx.prepare();

        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void dirtyCheckDisabled_whenDirty_thenLockedForCommit() {
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode.None);
    }

    public void dirtyCheckDisabled_whenDirty_thenLockedForCommit(LockMode writeLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tranlocal.long_value++;
        tx.prepare();

        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void dirtyCheckEnabled_whenNotDirty_thenNothingHappens() {
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckDisabled_whenNotDirty_thenLockedForCommit(LockMode.Exclusive);
    }

    public void dirtyCheckEnabled_whenNotDirty_nothingHappens(LockMode writeLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tx.prepare();

        assertFalse(tranlocal.isDirty());
        assertEquals(writeLockMode.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, writeLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void dirtyCheckEnabled_whenDirty_thenLockedForCommit() {
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.None);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Read);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Write);
        dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode.Exclusive);
    }

    public void dirtyCheckEnabled_whenDirty_thenLockedForCommit(LockMode writeLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);

        GammaTransaction tx = newTransaction(config);
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, writeLockMode.asInt());
        tranlocal.long_value++;
        tx.prepare();

        assertTrue(tranlocal.isDirty());
        assertEquals(LockMode.Exclusive.asInt(), tranlocal.getLockMode());
        assertLockMode(ref, LockMode.Exclusive);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    // =============================== locked by other =============================

    @Test
    public void conflict_dirty_whenReadLockedByOther() {
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
    }

    @Test
    public void conflict_dirty_whenWriteLockedByOther() {
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
    }

    @Test
    public void conflict_dirty_whenExclusiveLockedByOther() {
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
    }

    // ================================ states =====================================

    @Test
    public void whenPreparedAndUnused() {
        T tx = newTransaction();
        tx.prepare();

        tx.prepare();

        assertIsPrepared(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.commit();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}
