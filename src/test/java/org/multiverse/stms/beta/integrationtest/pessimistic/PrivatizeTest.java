package org.multiverse.stms.beta.integrationtest.pessimistic;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

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
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
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
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
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
        } catch (ReadConflict expected) {
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

        BetaTransaction ensureTx = stm.startDefaultTransaction();
        ref.privatize(ensureTx);

        ref.set(tx, 100);

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(ensureTx, ref.___getLockOwner());
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
        } catch (ReadConflict expected) {
        }

        assertIsAborted(tx);
        assertHasCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenUpgradeToPrivatizeSuccessful() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        ref.privatize(tx);

        assertIsActive(tx);
        assertHasCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenTransactionCommits_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.commit();

        assertIsCommitted(tx);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenTransactionIsPrepared_thenPrivatizationIsNotEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.prepare();

        assertIsPrepared(tx);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenTransactionAborts_thenPrivatizationIsEnded() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        tx.abort();

        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenPrivatizeIsReentrant() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        ref.privatize(tx);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasUpdateLock(ref);
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
        } catch (ReadConflict expected) {
        }

        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertIsAborted(tx);
    }
}
