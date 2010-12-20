package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class Lock_isLockedForCommitBySelf1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);

        ref.isLockedForCommitBySelf(null);
    }

    @Test
    public void whenFree() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isLockedForCommitBySelf(tx);

        assertFalse(result);
    }

    @Test
    public void whenPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(tx);
        boolean result = ref.isLockedForCommitBySelf(tx);

        assertTrue(result);
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);
        boolean result = ref.isLockedForCommitBySelf(tx);

        assertFalse(result);
    }

    @Test
    public void whenPrivatizedByOther() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isLockedForCommitBySelf(tx);

        assertFalse(result);
    }

    @Test
    public void whenEnsuredByOther() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isLockedForCommitBySelf(tx);

        assertFalse(result);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.isLockedForCommitBySelf(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.isLockedForCommitBySelf(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.isLockedForCommitBySelf(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }
}
