package org.multiverse.stms.gamma.integration.locks;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class CommitLockTest {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUnlocked(){
         GammaLongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenReadLockAlreadyAcquiredByOther_thenCommitLockNotPossible() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasReadLock(ref, otherTx);
    }

    @Test
    public void whenCommitLockAlreadyAcquiredByOther_thenCommitLockNotPossible() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void whenWriteLockAlreadyAcquiredByOther_thenCommitLockNotPossible() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenCommitLockAcquiredByOther_thenReadNotPossible() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        try {
            ref.get(otherTx);
            fail();
        } catch (ReadWriteConflict expected) {
        }
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenNoProblems() {
        GammaLongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.get(otherTx);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        long result = ref.get(otherTx);
        assertEquals(10, result);
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenWriteSuccessButCommitFails() {
        GammaLongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void whenCommitLockAcquiredByOtherThenWriteNotAllowed() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.set(tx, 100);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void writeLockIsUpgradableToCommitLock() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Exclusive);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenReadLockAcquired_thenUpgradableToCommitLock() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        ref.getLock().acquire(tx, LockMode.Exclusive);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenReadLockAlsoAcquiredByOther_thenNotUpgradableToCommitLock() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);

        try {
            ref.getLock().acquire(tx, LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
    }


    @Test
    public void whenTransactionCommits_thenCommitLockReleased() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenCommitLockRemains() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenCommitLockIsReleased() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommitLockAlreadyIsAcquired_thenReentrantCommitLockIsSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        ref.getLock().acquire(tx, LockMode.Exclusive);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenReadConflict_thenCommitLockFails() {
        GammaLongRef ref = new GammaLongRef(stm, 5);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        try {
            ref.getLock().acquire(tx, LockMode.Exclusive);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }

}
