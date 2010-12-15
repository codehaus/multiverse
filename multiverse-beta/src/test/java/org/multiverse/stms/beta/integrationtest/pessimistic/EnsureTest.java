package org.multiverse.stms.beta.integrationtest.pessimistic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class EnsureTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenEnsureIsNotPossible() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.ensure(tx);
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
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.ensure(tx);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasUpdateLock(ref, otherTx);
    }

    @Test
    public void whenEnsuredByOther_thenReadStillAllowed() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();

        long result = ref.get(tx);

        assertEquals(5, result);
        assertIsActive(tx);
        assertRefHasUpdateLock(ref,otherTx);
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenNoProblems() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        long result = ref.get(tx);

        assertEquals(10, result);
        assertIsActive(tx);
        assertRefHasUpdateLock(ref,otherTx);        
    }

    @Test
    public void whenPreviouslyReadByOtherTransaction_thenWriteSuccessButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasUpdateLock(ref, otherTx);
    }

    @Test
    public void whenEnsuredByOther_thenWriteAllowedButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasUpdateLock(ref, otherTx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenEnsureSuccessful() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.ensure(tx);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionCommits_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenEnsureIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasUpdateLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenEnsureIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenEnsureIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.ensure(tx);

        assertIsActive(tx);
        assertRefHasUpdateLock(ref, tx);
    }

    @Test
    public void whenReadConflict_thenEnsureFails() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        try {
            ref.ensure(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }
}
