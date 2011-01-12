package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_incrementAndGet1Test {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
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
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
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
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.incrementAndGet(1);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(getThreadLocalTransaction());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
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
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
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
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenSuccess() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Write);
        int amount = 1;
        long result = ref.incrementAndGet(amount);

        assertEquals(initialValue + amount, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Commit);
        long result = ref.incrementAndGet(1);

        assertEquals(11, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 1);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenAlreadyPrivatizedByOther_theReadConflict() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();

        ref.getLock().acquire(otherTx, LockMode.Commit);

        try {
            ref.incrementAndGet(1);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 1);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAlreadyEnsuredByOther_thenIncrementSucceedsButCommitFails() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();

        ref.getLock().acquire(otherTx, LockMode.Write);

        long result = ref.incrementAndGet(1);
        assertEquals(11, result);

        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 2);
        assertRefHasWriteLock(ref, otherTx);
        assertSame(version, ref.getVersion());
        assertEquals(10, ref.atomicWeakGet());

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, otherTx);
        assertSame(version, ref.getVersion());
        assertEquals(10, ref.atomicWeakGet());
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long amount = 4;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + amount);
        thread.start();

        sleepMs(500);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long result = ref.incrementAndGet(amount);
        tx.commit();

        joinAll(thread);

        assertEquals(initialValue + amount, result);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + amount);
    }
}
