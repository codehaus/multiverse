package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.WriteConflict;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_getAndSet1Test {

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
            ref.getAndSet(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(20);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(10);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(10, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertEquals(20, ref.atomicGet());
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenEnsuredByOther_thenGetAndSetSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.ensure(otherTx);

        long result = ref.getAndSet(20);
        assertEquals(10, result);
        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());

        try {
            tx.commit();
            fail();
        } catch (WriteConflict expected) {

        }

        assertIsAborted(tx);
        assertIsActive(otherTx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrivatizedByOther() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.privatize(otherTx);

        try {
            ref.getAndSet(20);
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {
    }
}
