package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_incrementAndGet1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.incrementAndGet(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.incrementAndGet(20);
        tx.commit();

        assertEquals(30, value);
        assertIsCommitted(tx);
        assertEquals(30, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        long value = ref.incrementAndGet(20);

        assertEquals(30, value);
        assertEquals(30, ref.atomicGet());
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long value = ref.incrementAndGet(20);

        assertEquals(30, value);
        assertIsCommitted(tx);
        assertEquals(30, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long value = ref.incrementAndGet(20);

        assertEquals(30, value);
        assertIsAborted(tx);
        assertEquals(30, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        long result = ref.incrementAndGet(1);

        assertEquals(11, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        long result = ref.incrementAndGet(1);

        assertEquals(11, result);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenAlreadyPrivatizedByOther_theReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.privatize(otherTx);

        try {
            ref.incrementAndGet(1);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAlreadyEnsuredByOther_thenIncrementSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.ensure(otherTx);

        long result = ref.incrementAndGet(1);
        assertEquals(11, result);

        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(2, ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(1, ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }
}
