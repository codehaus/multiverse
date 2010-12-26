package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
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

public class BetaLongRef_incrementAndGet1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        long amount = 30;
        try {
            ref.incrementAndGet(amount);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenActiveTransactionAvailable() {
        int initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long amount = 20;
        long value = ref.incrementAndGet(amount);
        tx.commit();

        assertEquals(initialValue + amount, value);
        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.incrementAndGet(1);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        try {
            ref.incrementAndGet(20);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        try {
            ref.incrementAndGet(20);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenSuccess() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireWriteLock();
        int amount = 1;
        long result = ref.incrementAndGet(amount);

        assertEquals(initialValue + amount, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireCommitLock();
        long result = ref.incrementAndGet(1);

        assertEquals(11, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenAlreadyPrivatizedByOther_theReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.incrementAndGet(1);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAlreadyEnsuredByOther_thenIncrementSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.getLock().acquireWriteLock(otherTx);

        long result = ref.incrementAndGet(1);
        assertEquals(11, result);

        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(2, ref);
        assertRefHasWriteLock(ref, otherTx);
        assertSame(version, ref.getVersion());
        assertEquals(10, ref.___weakRead());

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertRefHasWriteLock(ref, otherTx);
        assertSame(version, ref.getVersion());
        assertEquals(10, ref.___weakRead());
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long amount = 4;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + amount);
        thread.start();

        sleepMs(500);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long result = ref.incrementAndGet(amount);
        tx.commit();

        joinAll(thread);

        assertEquals(initialValue+amount, result);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
    }
}
