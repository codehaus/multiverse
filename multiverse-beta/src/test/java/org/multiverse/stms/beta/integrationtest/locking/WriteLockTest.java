package org.multiverse.stms.beta.integrationtest.locking;

import org.junit.Before;
import org.junit.Test;
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
        ref.getLock().acquireCommitLock(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquireCommitLock(tx);
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
        ref.getLock().acquireWriteLock(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().acquireWriteLock(tx);
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
        ref.getLock().acquireWriteLock(otherTx);

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
        ref.getLock().acquireWriteLock(otherTx);

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
        ref.getLock().acquireWriteLock(otherTx);

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
        ref.getLock().acquireWriteLock(otherTx);

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
        ref.getLock().acquireCommitLock(tx);
        ref.getLock().acquireWriteLock(tx);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionCommits_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenEnsureIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenEnsureIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);
        ref.getLock().acquireWriteLock(tx);

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
            ref.getLock().acquireWriteLock(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }
}
