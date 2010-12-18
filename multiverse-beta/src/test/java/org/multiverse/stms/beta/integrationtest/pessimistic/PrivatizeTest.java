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

public class PrivatizeTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenPrivatizationIsNotPossible() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.privatize(tx);
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
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.privatize(tx);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasUpdateLock(ref, otherTx);
    }

    @Test
    public void whenPrivatizedThenReadNotAllowed() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

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
        ref.privatize(tx);

        long result = ref.get(otherTx);
        assertEquals(10, result);
    }

    @Test
    public void whenPreviouslyReadByOtherThread_thenWriteSuccessButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

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
        ref.privatize(otherTx);

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
    public void whenAlreadyEnsuredBySelf_thenUpgradeToPrivatizeSuccessful() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.privatize(tx);

        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionCommits_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionIsPrepared_thenPrivatizationIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenTransactionAborts_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.abort();

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPrivatizeIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.privatize(tx);

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
            ref.privatize(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
    }
}