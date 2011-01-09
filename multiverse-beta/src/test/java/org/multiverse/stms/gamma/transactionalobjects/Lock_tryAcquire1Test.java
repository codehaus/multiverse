package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class Lock_tryAcquire1Test {

    private GammaStm stm;
    private boolean readBiased;

    public Lock_tryAcquire1Test(boolean readBiased){
        this.readBiased = readBiased;
    }

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Parameterized.Parameters
    public static Collection<Boolean[]> configs() {
        return asList(new Boolean[]{true},new Boolean[]{false});
    }

    public GammaLongRef newLongRef(long initialValue){
        if(readBiased){
            return makeReadBiased(new GammaLongRef(stm, initialValue));
        }else{
           return new GammaLongRef(stm, initialValue);
        }
    }

    //todo: conflict detection.

    @Test
    public void whenNullLock() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, null);
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
            ref.getLock().acquire(null, LockMode.Read);
            fail();
        } catch (NullPointerException expected) {

        }

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeNone() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.None);

        assertIsActive(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeRead() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);

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

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);

        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void free_whenLockModeCommit() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenReadLockAndUpgradeToNone() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        ref.getLock().acquire(tx, LockMode.None);

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

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        ref.getLock().acquire(tx, LockMode.Write);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenReadLockedAndUpgradeToCommit() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        ref.getLock().acquire(tx, LockMode.Commit);

        assertRefHasCommitLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToNone() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.None);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToRead() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Read);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToWrite() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Write);

        assertRefHasWriteLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenWriteLockedAndUpgradeToCommit() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Commit);

        assertRefHasCommitLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenCommitLockedAndUpgradeToNone() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.None);

        assertRefHasCommitLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenCommitLockedAndUpgradeToRead() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.Read);

        assertRefHasCommitLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenCommitLockedAndUpgradeToWrite() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.Write);

        assertRefHasCommitLock(ref, tx);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void selfLocked_whenCommitLockedAndUpgradeToCommit() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.Commit);

        assertRefHasCommitLock(ref, tx);
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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.None);

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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);

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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasReadLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherReadLockedAndCommitLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Commit);
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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.None);

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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Read);
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

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasWriteLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherWriteLockedAndCommitLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Commit);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasWriteLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherCommitLockedAndNoLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.None);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasCommitLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherCommitLockedAndReadLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Read);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasCommitLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherCommitLockedAndWriteLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasCommitLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void otherLocked_whenOtherCommitLockedAndCommitLockAcquired() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Commit);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertRefHasCommitLock(ref, otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    // ========================== states ====================================

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        long initialValue = 10;
         GammaLongRef ref = newLongRef(initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.getLock().acquire(tx, LockMode.Read);
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

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.getLock().acquire(tx, LockMode.Read);
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

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.getLock().acquire(tx, LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
