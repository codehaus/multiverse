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

public class WriteLockTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = (BetaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenEnsureIsNotPossible() {
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
    public void whenAlreadyEnsuredByOther_thenEnsureIsNotPossible() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquire(tx, LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenEnsuredByOther_thenReadStillAllowed() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = stm.startDefaultTransaction();

        long result = ref.get(tx);

        assertEquals(5, result);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenNoProblems() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        long result = ref.get(tx);

        assertEquals(10, result);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenPreviouslyReadByOtherTransaction_thenWriteSuccessButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenEnsuredByOther_thenWriteAllowedButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenEnsureSuccessful() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        ref.getLock().acquire(tx, LockMode.Write);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionCommits_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenEnsureIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenEnsureIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        ref.getLock().acquire(tx, LockMode.Write);

        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenReadConflict_thenEnsureFails() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        try {
            ref.getLock().acquire(tx, LockMode.Write);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }
}
