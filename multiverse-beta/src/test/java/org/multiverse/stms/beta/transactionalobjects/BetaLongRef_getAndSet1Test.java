package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.assertSurplus;

public class BetaLongRef_getAndSet1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(initialValue + 2);
        tx.commit();

        assertEquals(initialValue, value);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 2);
    }

    @Test
    public void whenNoChange() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(initialValue);
        tx.commit();

        assertEquals(initialValue, value);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireWriteLock();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
        assertSurplus(1, ref);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireCommitLock();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenEnsuredByOther_thenGetAndSetSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.getLock().acquire(otherTx, LockMode.Write);

        long result = ref.getAndSet(20);
        assertEquals(10, result);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(2, ref);
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertIsActive(otherTx);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(1, ref);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        try {
            ref.getAndSet(20);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long newValue = 20;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, newValue);
        thread.start();

        sleepMs(500);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long result = ref.getAndSet(newValue);
        tx.commit();

        joinAll(thread);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        long newValue = 20;
        try {
            ref.getAndSet(newValue);
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(initialValue + 1);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(initialValue + 1);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}
