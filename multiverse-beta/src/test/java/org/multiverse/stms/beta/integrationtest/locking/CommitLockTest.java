package org.multiverse.stms.beta.integrationtest.locking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class CommitLockTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenPrivatizationIsNotPossible() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Commit);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void whenAlreadyEnsuredByOther_thenPrivatizationIsNotPossible() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Commit);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenPrivatizedThenReadNotAllowed() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        try {
            ref.get(otherTx);
            fail();
        } catch (ReadWriteConflict expected) {
        }
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenNoProblems() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.get(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);

        long result = ref.get(otherTx);
        assertEquals(10, result);
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenWriteSuccessButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

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
    public void whenPrivatizedBuOtherThenWriteNotAllowed() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        BetaTransaction tx = stm.startDefaultTransaction();
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
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Commit);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionCommits_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx,LockMode.Commit);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenPrivatizationIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPrivatizeIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.Commit);

        assertIsActive(tx);
        assertRefHasCommitLock(ref,tx);
    }

    @Test
    public void whenReadConflict_thenPrivatizationFails() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        try {
            ref.getLock().acquire(tx, LockMode.Commit);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }
}
