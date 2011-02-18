package org.multiverse.stms.gamma.transactionalobjects.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionMandatoryException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class Lock_acquire0Test {

    private GammaStm stm;
    private boolean readBiased;

    public Lock_acquire0Test(boolean readBiased) {
        this.readBiased = readBiased;
    }

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> configs() {
        return asList(new Boolean[]{true}, new Boolean[]{false});
    }

    public GammaLongRef newLongRef(long initialValue) {
        if (readBiased) {
            return makeReadBiased(new GammaLongRef(stm, initialValue));
        } else {
            return new GammaLongRef(stm, initialValue);
        }
    }

    //todo: conflict detection.

    @Test
    public void whenNullLock() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNullTransaction() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (TransactionMandatoryException expected) {

        }

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeNone() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.None);

        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeRead() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Read);

        assertIsActive(tx);
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeWrite() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Write);

        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeCommit() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Exclusive);

        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenReadLockAndUpgradeToNone() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Read);
        ref.getLock().acquire(LockMode.None);

        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenReadLockedAndUpgradeToWrite() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Read);
        ref.getLock().acquire(LockMode.Write);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenReadLockedAndUpgradeToCommit() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Read);
        ref.getLock().acquire(LockMode.Exclusive);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToNone() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Write);
        ref.getLock().acquire(LockMode.None);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToRead() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Write);
        ref.getLock().acquire(LockMode.Read);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToWrite() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Write);
        ref.getLock().acquire(LockMode.Write);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToCommit() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Write);
        ref.getLock().acquire(LockMode.Exclusive);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenExclusiveLockedAndUpgradeToNone() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Exclusive);
        ref.getLock().acquire(LockMode.None);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenExclusiveLockedAndUpgradeToRead() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Exclusive);
        ref.getLock().acquire(LockMode.Read);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenExclusiveLockedAndUpgradeToWrite() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Exclusive);
        ref.getLock().acquire(LockMode.Write);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenExclusiveLockedAndUpgradeToCommit() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Exclusive);
        ref.getLock().acquire(LockMode.Exclusive);

        assertRefHasExclusiveLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    //todo: other locks.

    // =========================== locked by other ==========================

    @Test
    public void otherLocked_whenOtherHasReadLockedAndNoLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.None);

        //todo: check state of tx with regard to lock
        assertRefHasReadLock(ref, otherTx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherReadLockedAndReadLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.Read);

        assertRefHasReadLock(ref, tx);
        assertRefHasReadLock(ref, otherTx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherReadLockedAndWriteLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasReadLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherReadLockedAndExclusiveLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasReadLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherWriteLockedAndNoLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.getLock().acquire(LockMode.None);

        //todo: check state of tx and locking
        assertRefHasWriteLock(ref, otherTx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherWriteLockedAndReadLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasWriteLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherWriteLockedAndWriteLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasWriteLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherWriteLockedAndExclusiveLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasWriteLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherExclusiveLockedAndNoLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.None);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasExclusiveLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherExclusiveLockedAndReadLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasExclusiveLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherExclusiveLockedAndWriteLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasExclusiveLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherExclusiveLockedAndExclusiveLockAcquired() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.getLock().acquire(LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasExclusiveLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    // ========================== states ====================================

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        try {
            ref.getLock().acquire(LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

}
